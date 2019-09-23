package com.android.canaria;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.connect_to_server.MainService;
import com.android.canaria.recyclerView.FriendListItem;
import com.android.canaria.recyclerView.MessageItem;

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
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import id.zelory.compressor.Compressor;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PikachuDetectorActivity extends AppCompatActivity {

    ImageView imageView;
    Button button;
    TextView output_textView;

    private static final int PICK_IMAGE_REQUEST = 2;
    static final int REQUEST_TAKE_PHOTO = 3;
    private static final int REQUEST_IMAGE_CROP = 4;
    Uri photoUri, albumUri;
    String user_id, username;


    String mCurrentPhotoPath; //사진파일을 저장할 경로. 재사용을 위해 전역변수로 선언한다

    String previous_fileName;

    int serverResponseCode = 0;
    ProgressDialog dialog = null;
    String upLoadServerUri = "http://15.164.193.65/pikachu_fileUpload.php";//서버컴퓨터의 ip주소
    String pikachu_image_dir = "http://15.164.193.65/pikachu_images/";

    boolean isDetectionMode = false;

    String filename_origin; //원본 사진의 이름을 기억한다


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pikachu_detector);

        imageView = (ImageView)findViewById(R.id.pikachu_imageView);
        button = (Button)findViewById(R.id.pikachu_select_image_btn);
        output_textView = (TextView)findViewById(R.id.pikachu_output_textView);

        user_id = Function.getString(this, "user_id");
        username = Function.getString(this, "username");

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isDetectionMode){
                    Log.d("image", "Detect btn clicked");
                    isDetectionMode = false;

                    //채팅 서버로 메시지를 보낸다
                    sendMsg("pikachu/"+filename_origin);
                    Log.d("image", "Sent a message to ChatServer");

                }else {
                    callGallery();
                }

            }
        });


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


        //갤러리에서 선택한 사진 받아옴
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK){

            try{

                File albumFile = null;
                albumFile = createImageFile();
                photoUri = data.getData(); //content URI
                albumUri = Uri.fromFile(albumFile); // file URI
                Log.d("image", "photoUri(=방금 가져온 data의 contentUri) : "+photoUri);
                Log.d("image","albumUri(=새로 만든 file의 fileUri) : "+albumUri);

                cropImage();

            }catch(Exception e){
                e.printStackTrace();
            }

        }

        //크롭한 사진
        else if(requestCode == REQUEST_IMAGE_CROP && resultCode == RESULT_OK) {

            galleryAddPic();
            imageView.setImageURI(albumUri);
            button.setText("Start Detection");
            isDetectionMode = true;

            String[] imagePath_split = mCurrentPhotoPath.split("/");
            String fileName = imagePath_split[imagePath_split.length-1];
            Log.d("image", "fileName="+fileName);

            new Thread(new Runnable() {

                public void run() {

                    Log.d("image", "Uploading file...");
                    uploadFile(mCurrentPhotoPath);
                }

            }).start();
        }
    }



    public int uploadFile(String sourceFileUri) {

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
                conn.setRequestProperty("uploaded_file", sourceFileUri);

                dos = new DataOutputStream(conn.getOutputStream());

                Log.d("image", "http 요청 user id="+user_id);

                //id를 보낸다
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"user_id\"\r\n\r\n" + user_id +lineEnd);

                //이미지 파일을 보낸다
                dos.writeBytes(twoHyphens + boundary + lineEnd);

                dos.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + sourceFileUri + "\"" + lineEnd);

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

                ex.printStackTrace();

                Log.d("image", "error: " + ex.getMessage(), ex);

            } catch (Exception e) {

                e.printStackTrace();

                Log.d("image", "error: " + e.getMessage(), e);

            }
            return serverResponseCode;


        } // End else block

    }


    void sendMsg(String msg){
        Intent intent = new Intent(getApplicationContext(), MainService.class);
        intent.putExtra("message", msg);

        startService(intent);
    }


    //미디어스캐너에게, uri에 해당하는 파일을 스캔하고, 미디어 라이브러리에 파일을 추가하라는 신호를 보낸다
    private void galleryAddPic(){
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        File f= new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        Log.d("image", "galleryAddPic) contentUri="+contentUri);

        mediaScanIntent.setData(contentUri);
        sendBroadcast(mediaScanIntent);
    }


    //이미지파일 만들기
    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("dd-HH-mm-ss").format(new Date());
        String imageFileName = user_id +"_"+ timeStamp;

        //여러 앱이 공용으로 사용할 수 있는 저장공간. 그림파일이 저장되는 디렉토리 경로를 불러온다
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // = /mnt/sdcard/Pictures

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

        String[] filename_split = mCurrentPhotoPath.split("/");
        filename_origin = filename_split[filename_split.length-1];

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



    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("pikachu_event"));
    }


    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }


    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d("image", "Receiver) message received: " + message);

            String[] message_array = message.split("/");
            String signal = message_array[0];

            switch (signal){

                //이미지가 분석되었다는 알림
                //pikachu_output/success or fail/filename
                case "pikachu_output":

                    String success_or_fail = message_array[1];
                    String filename = message_array[2];

                    //이미지분석이 정상적으로 완료되었을 경우
                    if(success_or_fail.equals("success")){

                        String[] filename_split = filename.split("_");
                        int number_of_pikachu = Integer.valueOf(filename_split[1]);

                        output_textView.setText("Found "+ number_of_pikachu +" Pikachu");

                    }else{ //이미지 분석 중 오류가 났을 경우

                        output_textView.setText("An error occurred. Please try again later");

                    }

                    break;
            }


        }
    };




}
