package com.android.canaria;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.app.Activity.RESULT_OK;

/*더보기 fragment*/

public class Main_Fragment3 extends Fragment {

    ImageView profileImage_imageView;
    String mCurrentPhotoPath;
    Bitmap rotatedBitmap;
    private static final int PICK_IMAGE_REQUEST = 2;
    static final int REQUEST_TAKE_PHOTO = 3;
    private static final int REQUEST_IMAGE_CROP = 4;
    Uri photoUri, albumUri;


    TextView username_textView, email_textView;
    String user_id, username, user_email;
    Switch alarm_switch;


    int serverResponseCode = 0;
    ProgressDialog dialog = null;
    String upLoadServerUri = "http://54.180.107.44/fileUpload.php";//서버컴퓨터의 ip주소

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment3, container, false);

        user_id = Function.getString(getContext(), "user_id");
        username = Function.getString(getContext(), "username");
        user_email = Function.getString(getContext(), "email");

        String profileImage_path = "http://54.180.107.44/uploads/"+Function.getString(getActivity(), "profileImage");
        Log.d("image", profileImage_path);
        profileImage_imageView = (ImageView)view.findViewById(R.id.main_fragment3_profile_imageView);
        Glide.with(getActivity()).asBitmap().load(profileImage_path).into(profileImage_imageView); //asBitmap은 왜 넣는거지?

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

            //화면 하단의 카메라 사진버튼을 누르면
            profileImage_imageView.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    //카메라로 사진찍기 or 갤러리에서 사진 가져오기 선택
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    View view = LayoutInflater.from(getActivity()).inflate(R.layout.camera_dialog, null, false);
                    builder.setView(view);

                    ImageView profile_imageView = (ImageView)view.findViewById(R.id.profile_imageView);
                    String profileImage_path = "http://54.180.107.44/uploads/"+Function.getString(getActivity(), "profileImage");
                    Glide.with(view).asBitmap().load(profileImage_path).into(profile_imageView);

                    final Button camera = (Button) view.findViewById(R.id.camera_btn);
                    final Button gallery = (Button) view.findViewById(R.id.gallery_btn);
                    Button cancel_btn = (Button)view.findViewById(R.id.dialog_cancel_btn);

                    final AlertDialog dialog = builder.create();

                    cancel_btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {

                            dialog.dismiss();
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
        }
    }



    //갤러리 열고 사진 가져오기
    public void callGallery(){
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }



    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //직접 찍은 사진의 경로를 받아옴
        if(requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK){
            try { //getBitmap 오류 방지
                File file = new File(mCurrentPhotoPath);
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getActivity().getContentResolver(), Uri.fromFile(file));
                if (bitmap != null) {
                    ExifInterface ei = new ExifInterface(mCurrentPhotoPath);
                    int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);

                    rotatedBitmap = null;
                    switch(orientation){
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotatedBitmap = rotateImage(bitmap,90);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotatedBitmap = rotateImage(bitmap,180);
                            break;

                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotatedBitmap = rotateImage(bitmap,270);
                            break;

                        case ExifInterface.ORIENTATION_NORMAL:
                        default:
                            rotatedBitmap = bitmap;
                    }

                    profileImage_imageView.setImageBitmap(rotatedBitmap);

                    //사진 경로 저장
//                    setData("profileImagePath", mCurrentPhotoPath);
                }
            }catch (IOException ex){

            }
        }

        //갤러리에서 선택한 사진 받아옴
        else if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK){

            try{

                File albumFile = null;
                albumFile = createImageFile();
                photoUri = data.getData();
                albumUri = Uri.fromFile(albumFile); // 해당 경로에 저장
                Log.d("image", "photoUri: "+photoUri);
                Log.d("image","albumUri: "+albumUri);
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

            //사진 파일 이름을 shared preference 에 저장
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

                Log.i("image", "HTTP Response is : "
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

        Intent cropIntent = new Intent("com.android.camera.action.CROP");

        cropIntent.setFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        cropIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.setDataAndType(photoUri, "image/*");
        cropIntent.putExtra("aspectX",0);
        cropIntent.putExtra("aspectY",0);
        cropIntent.putExtra("output", albumUri);
        startActivityForResult(cropIntent, REQUEST_IMAGE_CROP);
    }




    private void dispatchTakePictureIntent(){
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        if(takePictureIntent.resolveActivity(getActivity().getPackageManager())!=null){
            File photoFile = null;

            try{
                photoFile = createImageFile(); //이미지 파일을 생성
            }catch (IOException ex){

            }

            if(photoFile!=null){

                Uri photoURI = FileProvider.getUriForFile(getActivity(), "com.android.canaria.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,photoURI);
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


    private Bitmap rotateImage(Bitmap source, float angle){
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source,0,0,source.getWidth(), source.getHeight(),matrix,true);
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
