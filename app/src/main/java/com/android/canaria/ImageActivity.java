package com.android.canaria;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImageActivity extends AppCompatActivity {

    /*
    *
    * 채팅창에서 주고받은 이미지를 클릭했을 때, 해당 이미지 크게 보여주는 화면
    * */

    ActionBar actionBar;
    String url;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        PhotoView photoView = (PhotoView) findViewById(R.id.imageActivity_photoView);
        progressBar = (ProgressBar)findViewById(R.id.imageActivity_progressBar);
        progressBar.setIndeterminate(true);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY); //색 변경
        progressBar.setVisibility(View.GONE);


        //상단 액션바 설정
        actionBar = getSupportActionBar();
        actionBar.show();
        actionBar.setTitle("");

        //뒤로가기 버튼 - Parent 액티비티가 무엇인지 manifest에 선언해줘야 함(-- 이 방식 사용x)
        //아래 onOptionsItemSelected 에 홈버튼 클릭시 발생하는 이벤트를 지정해줌
        actionBar.setDisplayHomeAsUpEnabled(true);

        url = getIntent().getStringExtra("url");
        Glide.with(this).asBitmap().load(url)
                .into(photoView);

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_image, menu);

        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()){
            case android.R.id.home: //뒤로가기 버튼을 누르면 이 화면을 종료한다
                finish();
                return true;
            case R.id.action_download:
                progressBar.setVisibility(View.VISIBLE);
                new ImageDownload().execute(url);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        Log.d("실험", "imageActivity onDestroy");
    }




    private class ImageDownload extends AsyncTask<String, Void, String> {

        private String fileName;
        private final String MY_FOLDER = "/Canaria"; //내가 원하는 저장 경로(폴더 이름)
        String path_final = "";


        @Override protected String doInBackground(String... params) {


            //웹 서버 쪽 파일이 있는 경로
            String fileUrl = params[0];


            //다운로드 경로를 지정
            //Environment.getExternalStorageDirector() : 저장공간의 기본경로를 가져옴. 기기마다 저장공간에 대한 경로 명이 상이함
            String savePath = Environment.getExternalStorageDirectory().toString() + MY_FOLDER;
            File dir = new File(savePath);


            //위에서 지정한 디렉토리가 존재하지 않을 경우, 새로 생성한다
            if (!dir.exists()) {
                dir.mkdirs();

                /*
                * mkdirs(): 원하는 경로의 상위 폴더가 없으면 상위 폴더까지 생성
                * mkdir(): 지정 폴더만 생성
                * */
            }


            //파일 이름 : Canaria_날짜_시간
            Date day = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
            fileName = "Canaria_"+String.valueOf(sdf.format(day));


            //다운로드 폴더에 동일한 파일명이 존재하는지 확인
            String pathname = savePath + "/" + fileName;
            int fix_index = 0;
            int flag = 0;
            while(flag == 0){

                fix_index++;

                if (new File(pathname).exists()) { //중복파일이 있으면

                    pathname = pathname+"("+fix_index+")";
                    flag = 0;

                } else { //중복파일이 없으면
                    flag = 1;
                }

            }


            path_final = pathname + ".jpg";
            try {

                URL imgUrl = new URL(fileUrl);

                //서버와 접속하는 클라이언트 객체 생성
                HttpURLConnection conn = (HttpURLConnection)imgUrl.openConnection();
                int len = conn.getContentLength();
                byte[] tmpByte = new byte[len];

                //입력 스트림을 구한다
                InputStream is = conn.getInputStream();
                File file = new File(path_final);

                //파일 저장 스트림 생성
                FileOutputStream fos = new FileOutputStream(file);
                int read;

                int response = conn.getResponseCode();
                if(response == HttpURLConnection.HTTP_OK){

                }else{ //전송 실패 시, 쓰레드 종료
                    return "failed";
                }

                //입력 스트림을 파일로 저장
                for (;;) {
                    read = is.read(tmpByte);
                    if (read <= 0) {
                        break;
                    }
                    fos.write(tmpByte, 0, read); //file 생성
                }
                is.close();
                fos.close();
                conn.disconnect();

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d("다운",ex);
                return "failed";
            }

            return "succeeded";
        }


        @Override protected void onPostExecute(String s) {
            super.onPostExecute(s);

            progressBar.setVisibility(View.GONE);

            if(s.equals("succeeded")){
                //이미지 스캔해서 갤러리 업데이트
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path_final))));

                //다운로드 완료 메시지를 띄워준다
                Toast.makeText(ImageActivity.this, "Downloaded: "+path_final, Toast.LENGTH_SHORT).show();

            }else{
                //다운로드 실패 메시지를 띄워준다
                Toast.makeText(ImageActivity.this, "Download failed", Toast.LENGTH_SHORT).show();
            }


            //저장한 이미지 열기
//            Intent i = new Intent(Intent.ACTION_VIEW);
//            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            String targetDir = Environment.getExternalStorageDirectory().toString() + SAVE_FOLDER;
//            File file = new File(targetDir + "/" + fileName + ".jpg");
//
//            //type 지정 (이미지)
//            i.setDataAndType(Uri.fromFile(file), "image/*");
//            getApplicationContext().startActivity(i);


        }


    }



}
