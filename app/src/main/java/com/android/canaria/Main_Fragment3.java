package com.android.canaria;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.connect_to_server.HttpRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import id.zelory.compressor.Compressor;

import static android.app.Activity.RESULT_OK;

/*더보기 fragment*/

public class Main_Fragment3 extends Fragment {

    ImageView profileImage_imageView;
    Button pikachu_detector_btn;
    String mCurrentPhotoPath; //사진파일을 저장할 경로. 재사용을 위해 전역변수로 선언한다
    Bitmap rotatedBitmap;
    private static final int PICK_IMAGE_REQUEST = 2;
    static final int REQUEST_TAKE_PHOTO = 3;
    private static final int REQUEST_IMAGE_CROP = 4;
    Uri photoUri, albumUri;


    TextView username_textView, email_textView;
    String user_id, username, user_email;
    Switch alarm_switch;

    String previous_fileName;
    String random_face_filename;
    String random_image_path;

    int serverResponseCode = 0;
    ProgressDialog dialog = null;
    String upLoadServerUri = "http://15.164.193.65/fileUpload.php";//서버컴퓨터의 ip주소

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment3, container, false);

        user_id = Function.getString(getContext(), "user_id");
        username = Function.getString(getContext(), "username");
        user_email = Function.getString(getContext(), "email");
        profileImage_imageView = (ImageView)view.findViewById(R.id.main_fragment3_profile_imageView);

        pikachu_detector_btn = (Button)view.findViewById(R.id.myPage_pikachu_detector_btn);


        String profileImage_name = Function.getString(getActivity(), "profileImage");
        if(!profileImage_name.equals("null")){
            String profileImage_path = "http://15.164.193.65/uploads/"+profileImage_name;
            Log.d("image", profileImage_path);

//            RequestOptions options = new RequestOptions()
//                    .diskCacheStrategy(DiskCacheStrategy.NONE);

            Glide.with(getActivity()).asBitmap().load(profileImage_path)
                    .into(profileImage_imageView); //asBitmap은 왜 넣는거지?
        }


        username_textView = (TextView)view.findViewById(R.id.main_fragment3_username_textView);
        email_textView = (TextView)view.findViewById(R.id.main_fragment3_email_textView);
        username_textView.setText(username);
        email_textView.setText(user_email);

        //스위치 버튼
        alarm_switch = (Switch)view.findViewById(R.id.main_fragment3_receiveAlarm_switch);

        //스위치 on/off -> 알림메시지 수신 여부를 결정
        switchAction();

        return view;
    }


    private void switchAction(){

        boolean isChecked = Function.getBoolean(getContext(), "alarm"); //저장된 스위치 on/off상태 불러옴
        alarm_switch.setChecked(isChecked); //스위치 on/off 초기상태 지정


        //스위치 클릭 리스너
        alarm_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                //스위치 ON/OFF에 따라 boolean값 변화
                if (isChecked) {
                    Function.setBoolean(getContext(),"alarm", true);
                    Toast.makeText(getActivity(), "Notification is on", Toast.LENGTH_SHORT).show();
                }else{
                    Function.setBoolean(getContext(),"alarm", false);
                    Toast.makeText(getActivity(), "Notification is off", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }



    @Override
    public void onResume() {
        super.onResume();


        isCameraPermissionChecked(); //권한 허용 되었는지 확인
        requestCameraPermission(); //권한 없으면 -> 요청하기

        if(isCameraPermissionChecked()) { //권한 허용 되어 있어야, 사진 어떻게 가져올 것인지 선택하는 다이얼로그 띄워줌

            //프로필 사진을 누르면
            profileImage_imageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {


                    //카메라로 사진찍기 or 갤러리에서 사진 가져오기 선택
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    final View view = LayoutInflater.from(getActivity()).inflate(R.layout.camera_dialog, null, false);
                    builder.setView(view);

                    final ProgressBar progressBar = (ProgressBar)view.findViewById(R.id.dialog_progressBar);
                    progressBar.setIndeterminate(true);
                    progressBar.getIndeterminateDrawable().setColorFilter(Color.WHITE, PorterDuff.Mode.MULTIPLY); //색 변경

                    final ImageView profile_imageView = (ImageView)view.findViewById(R.id.profile_imageView);
                    final String profileImage_path = "http://15.164.193.65/uploads/"+Function.getString(getActivity(), "profileImage");

                    Glide.with(view).asBitmap().load(profileImage_path)
                            .listener(new RequestListener<Bitmap>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                                    progressBar.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                                    progressBar.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(profile_imageView);

                    final Button camera = (Button) view.findViewById(R.id.camera_btn);
                    final Button gallery = (Button) view.findViewById(R.id.gallery_btn);
                    final Button random_face_btn = (Button) view.findViewById(R.id.random_btn);
                    final Button set_btn = (Button)view.findViewById(R.id.set_btn);

                    final AlertDialog dialog = builder.create();


                    //방금 누른 random face를 사용자가 set 하면
                    set_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            //다이얼로그 바깥 화면의 프로필 이미지를 변경한다
                            random_image_path = "http://15.164.193.65/uploads/"+random_face_filename;
                            Glide.with(view).asBitmap().load(random_image_path).into(profileImage_imageView);

                            //새로운 사진 파일 이름을 shared preference 에 저장
                            Function.setString(getActivity(), "profileImage", random_face_filename);


                            Log.d("image", "사용자가 고른 얼굴사진을 프로필사진으로 저장하라고 서버에 알려줘야 함");
                            //이 사진을 사용자의 프로필사진으로 저장하라고 서버에 알려줘야 한다
                            ContentValues data = new ContentValues();
                            data.put("set_profile", "Y");
                            data.put("face_filename", random_face_filename);
                            data.put("user_id", user_id);
                            data.put("username", username);

                            //result로 받는 것: 검색된 사용자의 닉네임, 사진, id
                            String response = "";

                            try {
                                response = new HttpRequest("image.php", data).execute().get();
                            } catch (Exception e) {
                                Log.d("image", "Error: "+e);
                            }

                            Log.d("image", "서버의 응담 = "+response);
                        }
                    });


                    //random face 버튼을 누르면 -> 얼굴 사진이 랜덤으로 화면에 뜬다
                    random_face_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            set_btn.setVisibility(View.VISIBLE);

                            int rand_number = (int)(Math.random()*82) +1 ;
                            random_face_filename = "face_"+rand_number+".jpg";
                            random_image_path = "http://15.164.193.65/uploads/"+random_face_filename;
                            Glide.with(view).asBitmap().load(random_image_path).into(profile_imageView);
                        }
                    });


                    camera.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //카메라 버튼을 클릭했을 때 일어나는 일

                            dispatchTakePictureIntent();

                            dialog.dismiss();
                        }
                    });

                    gallery.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //갤러리 버튼을 눌렀을 때 일어나는 일
                            callGallery();

                            dialog.dismiss();
                        }
                    });

                    dialog.show();

                }
            });


            //버튼을 누르면 -> 피카츄 디텍터 화면이 나타난다
            pikachu_detector_btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(getActivity(), PikachuDetectorActivity.class);
                    startActivity(intent);

                }
            });


        }
    }



    //갤러리 열고 사진 가져오기
    public void callGallery(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*"); //타입을 바꿔서 video 나 audio 를 가져올 수 있다
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("image", "onActivityResult");
        Log.d("image", "requestCode="+requestCode+" / resultCode="+resultCode+" / data="+data);

        //직접 찍은 사진의 경로를 받아옴
        if(requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK){
            try { //getBitmap 오류 방지

                //사용자가 방금 찍은 사진은 photoUri 에 저장되어 있다

                try{

                    File albumFile = null;
                    albumFile = createImageFile();
                    albumUri = Uri.fromFile(albumFile);
                    Log.d("image", "photoUri: "+photoUri); // content uri= 사용자가 방금 찍은 사진이 저장된 곳
                    Log.d("image","albumUri: "+albumUri); // file uri = 사진을 crop 해서 저장할 곳
                    cropImage();

                }catch(Exception e){
                    e.printStackTrace();
                }

            }catch (Exception ex){

            }
        }

        //갤러리에서 선택한 사진 받아옴
        else if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK){

            try{

                File albumFile = null;
                albumFile = createImageFile();
                photoUri = data.getData(); //content URI
                albumUri = Uri.fromFile(albumFile); // file URI
                Log.d("image", "photoUri(=방금 가져온 data의 contentUri) : "+photoUri);
                Log.d("image","albumUri(=새로 만든 file의 fileUri) : "+albumUri);

//                getRealPath(photoUri);
//                if (isDownloadsDocument(photoUri)) {
//
//                    final String id = DocumentsContract.getDocumentId(photoUri);
//                    final Uri contentUri = ContentUris.withAppendedId(
//                            Uri.parse("content://downloads/all_downloads"), Long.valueOf(id));
//
//                    String realPath = getDataColumn(contentUri, null, null);
//                    Log.d("image", "realPath="+realPath);
//                }

                cropImage();

            }catch(Exception e){
                e.printStackTrace();
            }

        }

        //크롭한 사진
        else if(requestCode == REQUEST_IMAGE_CROP && resultCode == RESULT_OK) {

            galleryAddPic();
            profileImage_imageView.setImageURI(albumUri);

            String[] imagePath_split = mCurrentPhotoPath.split("/");
            String fileName = imagePath_split[imagePath_split.length-1];
            Log.d("image", "fileName="+fileName);


            //기존 사진 파일의 이름을 가져온다
            previous_fileName = Function.getString(getActivity(), "profileImage");
            //새로운 사진 파일 이름을 shared preference 에 저장
            Function.setString(getActivity(), "profileImage", fileName);

            //사진을 서버에 업로드
            dialog = ProgressDialog.show(getActivity(), "", "Uploading file...", true);

            new Thread(new Runnable() {

                public void run() {

                    Log.d("image", "Uploading file...");
                    uploadFile(mCurrentPhotoPath);
                }

            }).start();
        }
    }




    public String getRealPath(Uri uri){
        if (isDownloadsDocument(uri)) {
            Log.d("image", "isDownloadsDocument = true");

            final String id = DocumentsContract.getDocumentId(uri);
            Log.d("image", "id = "+id);


            String[] contentUriPrefixesToTry = new String[]{
//                            "content://downloads/public_downloads",
                    "content://downloads/my_downloads",
                    "content://downloads/all_downloads"
            };

            for (String contentUriPrefix : contentUriPrefixesToTry) {
                Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.valueOf(id));
                try {
                    String realPath = getDataColumn(contentUri, null, null);
                    if (realPath != null) {

                        Log.d("image", "realPath="+realPath);
                        return realPath;
                    }
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String ex = sw.toString();

                    Log.d("image",ex);
                }
            }


        }
        return null;
    }


    public String getDataColumn(Uri uri, String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };

        try {
            cursor = getContext().getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }



    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }



    public int uploadFile(String sourceFileUri) {

        String fileName = sourceFileUri;

        HttpURLConnection conn = null;
        DataOutputStream dos = null;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";

        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File sourceFile = new File(sourceFileUri);


        //파일 압축
        try{
            sourceFile = new Compressor(getContext())
                    .setQuality(10)
                    .compressToFile(sourceFile);
        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d("image",ex);
        }


        if (!sourceFile.isFile()) {

            dialog.dismiss();
            Log.d("image", "Source File not exist");

            return 0;

        }else{

            try {
                // open a URL connection to the Servlet
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(upLoadServerUri);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("uploaded_file", fileName);

                dos = new DataOutputStream(conn.getOutputStream());

                //id를 보낸다
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"previous_fileName\"\r\n\r\n" + previous_fileName +lineEnd);

                //이미지 파일을 보낸다
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + fileName + "\"" + lineEnd);

                dos.writeBytes(lineEnd);


                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                // read file and write it into form...
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {

                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                }

                // send multipart form data necesssary after file data...
                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                // Responses from the server (code and message)
                serverResponseCode = conn.getResponseCode();
                String serverResponseMessage = conn.getResponseMessage();

                Log.d("image", "HTTP Response is : "
                        + serverResponseMessage + ": " + serverResponseCode);

                if(serverResponseCode == 200){

                    Log.d("image", "File is successfully uploaded.");

                }

                //close the streams //
                fileInputStream.close();
                dos.flush();
                dos.close();

            } catch (MalformedURLException ex) {
                dialog.dismiss();
                ex.printStackTrace();

                Log.d("image", "error: " + ex.getMessage(), ex);

            } catch (Exception e) {

                dialog.dismiss();
                e.printStackTrace();

                Log.d("image", "error: " + e.getMessage(), e);

            }
            dialog.dismiss();
            return serverResponseCode;


        } // End else block

    }











/*
* * <안드로이드에서 파일을 저장할 수 있는 경로>
* 1. 내부저장소(Internal Storage) : 앱 데이터가 저장되는 영역
* 2. 외부저장소(External Storage) : 사진, 비디오, 데이터를 저장하는 영역 = SD카드
	* 외부저장소 = 공용 영역 + 각 앱의 고유 영역
	* 각 앱의 고유 영역은 앱이 삭제될 때 같이 삭제된다
	* 공용 영역에는 사진, 비디오, 기타 파일등이 저장된다. 앱의 삭제와 무관하다
	* 고유 영역이라 할지라도 다른 앱에서 데이터에 접근하는 것이 가능하다
	* 데이터가 저장되는 주요 경로를, 메서드를 이용하여 간편하게 얻을 수 있다
 */

    //이미지파일 만들기
    private File createImageFile() throws IOException{

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = user_id +"_"+ username +"_"+ timeStamp + "_";

        //여러 앱이 공용으로 사용할 수 있는 저장공간. 그림파일이 저장되는 디렉토리 경로를 불러온다
        File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES); // = /mnt/sdcard/Pictures

        //이미지를 서버로 전송하기 전에, 임시로 캐쉬폴더에 저장한다
        //createTempFile : 캐쉬 디렉토리에 파일을 생성해주는 메소드
        File image = File.createTempFile(
                imageFileName, //접두어 prefix
                ".jpg", //접미어 suffix
                storageDir //파일을 저장할 폴더 directory
        );

        Log.d("image", "createImageFile) image="+image);

        mCurrentPhotoPath = image.getAbsolutePath(); //이건 왜 하는거지?
        Log.d("image", "createImageFile) mCurrentPhotoPath="+mCurrentPhotoPath);

        return image; //이미지 파일을 return
    }



    public void cropImage(){
        Log.d("image", "cropImage()");

        Intent cropIntent = new Intent("com.android.camera.action.CROP");
        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.setDataAndType(photoUri, "image/*"); //원본 사진 경로
        cropIntent.putExtra("aspectX",0);
        cropIntent.putExtra("aspectY",0);
        cropIntent.putExtra("output", albumUri); //crop 한 사진을 저장할 곳
        startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);

    }




    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(takePictureIntent.resolveActivity(getActivity().getPackageManager())!=null){
            File photoFile = null;

            try{
                photoFile = createImageFile(); //이미지 파일을 생성
                Log.d("image", "camera) photoFile="+photoFile);

            }catch (IOException e){

                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d("image",ex);

            }

            if(photoFile!=null){

                //파일을 다른 앱과 공유하기 위해서는, content URI를 생성해야 한다.
                //contentURI 생성하는 법: 새 파일을 만든다 -> 그 파일을 getUriForFile() 로 넘긴다 -> 이 uri를 인텐트로 다른 앱에 넘길 수 있다
                photoUri = FileProvider.getUriForFile(getActivity(), "com.android.canaria.fileprovider", photoFile);
                Log.d("image", "camera) photoUri="+photoUri);

                //사진을 찍어서 이 파일에 저장하라고, 안드로이드에게 말한다
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                //되돌아오는 사진 데이터를 onActivityResult 에서 받으면 된다
                startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }


    //미디어스캐너에게, uri에 해당하는 파일을 스캔하고, 미디어 라이브러리에 파일을 추가하라는 신호를 보낸다
    private void galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        File f= new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        Log.d("image", "galleryAddPic) contentUri="+contentUri);

        mediaScanIntent.setData(contentUri);
        getActivity().sendBroadcast(mediaScanIntent);
    }




    /*------권한요청------*/

    public boolean isCameraPermissionChecked() { //카메라 권한 있는지 확인

        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(getContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        else {
            return true;
        }
    }

    //카메라 권한 없으면 요청하기
    public void requestCameraPermission() {

        if (isCameraPermissionChecked() == false) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                new AlertDialog.Builder(getContext()).setTitle("알림").setMessage("저장소 권한이 거부되었습니다. 설정에서 해당 권한을 직접 허용하십시오.")
                        .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).setCancelable(false).create().show();
            }
            else {
                ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        }
        else {
        }

    }




}
