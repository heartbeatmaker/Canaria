package com.android.canaria;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.Image;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewFlipper;

import com.android.canaria.db.DBHelper;
import com.android.canaria.recyclerView.FriendListItem;
import com.android.canaria.recyclerView.RoomListItem;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class ImageActivity extends AppCompatActivity {

    /*
    *
    * 채팅창에서 주고받은 이미지를 클릭했을 때, 해당 이미지 크게 보여주는 화면
    * */

    ActionBar actionBar;

//    ArrayList<String> url_list;
    ArrayList<SlideImageItem> imageItemArrayList;


    SlideAdapter slideAdapter;
    ViewPager viewPager;

    DBHelper dbHelper;
    SQLiteDatabase db;

    Handler handler = new Handler();

    String filename_string;
    int room_id;


//    int swipe_direction;


    boolean isRightSideLoaded = false;
    boolean isLeftSideLoaded = false;

    boolean isFirstImage = false;
    boolean isLastImage = false;

    boolean beforeInit = false;
    int position; //다중이미지 내 해당 사진의 인덱스

    String video_file_path;

//    ArrayList<Integer> loaded_db_id_arrayList; //chat_logs 테이블에서, url_list에 이미 load 된 row의 db id를 담아놓는 곳

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

//        loaded_db_id_arrayList = new ArrayList<>();

        //DB를 연다
        dbHelper = new DBHelper(ImageActivity.this, Function.dbName, null, Function.dbVersion);
        db = dbHelper.getWritableDatabase();

        //스와이핑에 필요한 url을 담는 리스트
//        url_list = new ArrayList<>();
        imageItemArrayList = new ArrayList<>();


        //이전화면 (채팅화면) 에서 보낸 값을 받는다
        String data_type = getIntent().getStringExtra("type");

        if(data_type.equals("image")){ //이미지를 띄우는 것이라면

            Log.d("스와이프", "데이터 타입: 이미지");

            filename_string = getIntent().getStringExtra("filename_string");
            position = getIntent().getIntExtra("position", 0);
            room_id = getIntent().getIntExtra("room_id", 0);

            Log.d("스와이프", "filename_string="+filename_string+" " +
                    "/ position="+position+" / room_id="+room_id);


            //string 으로 이어져 있는 여러 개의 파일 이름을, 개별로 분할한다 -> url 형태로 만든다 -> 아이템 list 에 담는다
            String[] filename_split = filename_string.split(";");
            for(int i=0; i<filename_split.length; i++){

                String filename = filename_split[i];
                String url = Function.domain+"/images/"+room_id+"/"+filename;
//                url_list.add(i, url);

//                String type, int room_id, String filename, String url, String video_path
                imageItemArrayList.add(new SlideImageItem(data_type, room_id, filename, url, ""));
            }


        }else if(data_type.equals("video")){ //비디오의 썸네일을 띄우는 것이라면

            Log.d("스와이프", "데이터 타입: 비디오 썸네일");
            position = 0; //동영상 썸네일: 다중이미지로 띄우지 않는다. 무조건 개별사진이므로, position = 0

            filename_string = getIntent().getStringExtra("thumbnail_filename");
            room_id = getIntent().getIntExtra("room_id", 0);

            String thumbnail_url = Function.domain+"/images/"+room_id+"/"+filename_string;

            video_file_path = getIntent().getStringExtra("video_path");

            //썸네일 이미지를 url_list 에 담는다
//            url_list.add(url);
            imageItemArrayList.add(new SlideImageItem(data_type, room_id, filename_string, thumbnail_url, video_file_path));
        }

        Log.d("스와이프", "이 화면을 열었을 때, 최초의 imageItemArrayList="+imageItemArrayList);


        /* 사진 스와이핑을 위한 뷰페이저를 초기화하는 곳 */
        viewPager = (ViewPager) findViewById(R.id.view);

        //맨 처음 띄워주는 사진이 다중이미지 중에서 첫번째 or 마지막 이미지일 때 => 이전 or 다음에 띄워줄 사진이 없다
        //-> db에서 새로운 데이터를 가져온다
        //-> 그 다음에 어댑터를 연결한다(loadMoreImage 메소드 안에 어댑터 연결하는 부분이 있다)

        if(imageItemArrayList.size() == 1){ //단독 이미지일 때
            Log.d("스와이프", "단독 이미지임");


            Log.d("스와이프", "이전, 이후 데이터를 다 가져올 것임");
            //이전, 이후 데이터를 다 가져온다
            int position_updated = loadAllImages();

            Log.d("스와이프", "추가된 이전 데이터 개수 = 이 사진의 현재 포지션 = "+position_updated);

            //어댑터를 연결한다
            slideAdapter = new SlideAdapter(this, imageItemArrayList);
            viewPager.setAdapter(slideAdapter);

            //많은 사진 중에서 아까 사용자가 클릭한 사진이 바로 뜰 수 있도록, 현재 아이템을 설정한다
            viewPager.setCurrentItem(position_updated);
            Log.d("스와이프", "어댑터 연결하고, 포지션 설정함");


        } else if(imageItemArrayList.size()>1){ //다중이미지일 때
            Log.d("스와이프", "다중 이미지임");


            if(position == 0 || position >= imageItemArrayList.size()-1){ //다중이미지 중에서 첫번째 or 마지막 이미지일 때

                beforeInit = true;
                loadMoreImage(position);

                if(isFirstImage || isLastImage){ //사용자가 채팅화면에서 클릭한 이 사진이, 이 채팅방에서 주고받은 최초의 or 마지막 사진일 때

                    Log.d("스와이프", "사용자가 채팅화면에서 클릭한 이 사진이, 이 채팅방에서 주고받은 최초의 or 마지막 사진임");

                    slideAdapter = new SlideAdapter(this, imageItemArrayList);
                    viewPager.setAdapter(slideAdapter);

                    //많은 사진 중에서 아까 사용자가 클릭한 사진이 바로 뜰 수 있도록, 현재 아이템을 설정한다
                    viewPager.setCurrentItem(position);
                }

            }else{//첫번째 or 마지막 이미지가 아닐 때: 아직 앞뒤로 띄워줄 수 있는 이미지가 있다. 어댑터를 연결한다
                slideAdapter = new SlideAdapter(this, imageItemArrayList);
                viewPager.setAdapter(slideAdapter);

                //많은 사진 중에서 아까 사용자가 클릭한 사진이 바로 뜰 수 있도록, 현재 아이템을 설정한다
                viewPager.setCurrentItem(position);
            }

        }



        //사용자의 스와이핑을 감지하는 곳
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i1) { }


            //스크롤이 다 된 상태
            @Override
            public void onPageSelected(int index) {

                //앞뒤로 띄워줄 데이터가 없을 경우 -> db에서 새로운 데이터를 가져온다
                if(index ==0 || index == imageItemArrayList.size()-1){

                    loadMoreImage(index);
                }
            }

            @Override
            public void onPageScrollStateChanged(int i) { }
        });



        //상단 액션바 설정
        actionBar = getSupportActionBar();
        actionBar.show();
        actionBar.setTitle("");

        //뒤로가기 버튼 - Parent 액티비티가 무엇인지 manifest에 선언해줘야 함(-- 이 방식 사용x)
        //아래 onOptionsItemSelected 에 홈버튼 클릭시 발생하는 이벤트를 지정해줌
        actionBar.setDisplayHomeAsUpEnabled(true);


    }



    //현재 화면에 보이는 이미지 이전 or 이후에 이 채팅방에서 주고받은 사진이 있는지 확인하고, 가져오는 메소드
    public void loadMoreImage(int currentIndex){

        //데이터가 추가되기 전, url_list 의 사이즈
        final int origin_size = imageItemArrayList.size();

        //리스트에 추가된 이미지의 개수를 담는 변수
        int result_count = 0;


        //현재 사진이 db의 chat_logs 테이블에서 어떤 id를 가지고 있는지 확인한다
        int current_db_id = 0;
        //roomId와 image name이 일치하는 row를 찾는다
        Cursor cursor = db.rawQuery
                ("SELECT * FROM chat_logs WHERE room_id='" + room_id + "' AND image_name= '"+filename_string+"';", null);
        while (cursor.moveToNext()) {
            current_db_id = cursor.getInt(0);
        }
        Log.d("스와이프", "current_db_id="+current_db_id);


        //url_list 에서, 현재 사진이 가장 앞쪽에 위치한 데이터라면(= 사용자가 왼쪽으로 스와이프 했을 때 띄워줄 이전 데이터가 없음)
        if(currentIndex == 0){
            Log.d("스와이프", "currentIndex == 0. 이전 데이터가 떨어짐");

            if(!isLeftSideLoaded){ //이전에 왼쪽 데이터를 받아온 적이 있는지 검사. 데이터는 양 쪽이 한번씩, 최초로 한번만 가져와야 한다

                Log.d("스와이프", "왼쪽 데이터가 로드된 적이 없음. 지금부터 시작");

//                swipe_direction = 0;// 왼쪽으로 스와이프 했다는 표시

                //db에서 이전 데이터를 더 가져온다
                //최근 순서대로 가져와야함(order by time desc)
                Cursor cursor2 = db.rawQuery
                        ("SELECT * FROM chat_logs WHERE id<'"+current_db_id+"' AND room_id='" + room_id + "' AND image_name != 'N' ORDER BY time DESC;", null);

                //결과 개수를 확인
                result_count = cursor2.getCount();
                if(result_count > 0){
                    Log.d("스와이프", "db에 이전 데이터가 있음. 가져오면 됨");

                    while (cursor2.moveToNext()) {

                        //이전에 주고받은 이미지의 이름을 가져온다
                        int db_id = cursor2.getInt(0);
                        String filename_string = cursor2.getString(5);


                        //여러 파일의 이름이 String 으로 이어져 있다. 개별 이름으로 분리한다
                        String[] filename_split = filename_string.split(";");

                        if(filename_split.length == 1){ //단독 이미지일 경우

                            String url = Function.domain+"/images/"+room_id+"/"+filename_string;

                            //썸네일 이미지인지 일반 이미지인지 확인한다
                            String[] filename_split_2 = filename_string.split("_");

                            if(filename_split_2[1].equals("video")){

                                //가장 최근 사진부터 추가해야 하므로, 인덱스를 거꾸로 놓는다
                                imageItemArrayList.add(0, new SlideImageItem("video", room_id, filename_string, url, ""));

                            }else{

                                String video_path = cursor2.getString(8);
                                imageItemArrayList.add(0, new SlideImageItem("image", room_id, filename_string, url, video_path));
                            }


                        }else{ //다중이미지일 경우

                            //가장 최근 이미지부터 추가해야 하므로, 인덱스를 거꾸로 놓는다
                            for(int i=filename_split.length-1; i>=0; i--){

                                String filename = filename_split[i];
                                String url = Function.domain+"/images/"+room_id+"/"+filename;

//                String type, int room_id, String filename, String url, String video_path
                                imageItemArrayList.add(0, new SlideImageItem("image", room_id, filename, url, ""));
                            }

                        }


                        Log.d("스와이프", "이전데이터 가져옴. db_id="+db_id+" / filename_string="+filename_string);
                    }

                    Log.d("스와이프", "이전데이터 업데이트 완료");

                    //어댑터를 새것으로 교체한다
                    slideAdapter = new SlideAdapter(getApplicationContext(), imageItemArrayList);
                    slideAdapter.notifyDataSetChanged();
                    viewPager.setAdapter(slideAdapter);
                    viewPager.setCurrentItem(imageItemArrayList.size()-origin_size);


//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//
//
//                            if(beforeInit){
//                                slideAdapter = new SlideAdapter(ImageActivity.this, url_list);
//                                viewPager.setAdapter(slideAdapter);
//                                viewPager.setCurrentItem(url_list.size()-origin_size);
//                                Log.d("스와이프", "1");
//
//                                beforeInit = false;
//                            }else{
//                                slideAdapter.notifyDataSetChanged();
//                                viewPager.setCurrentItem(url_list.size()-origin_size);
//
//                                Log.d("스와이프", "2");
//                            }
//
//                        }
//                    });


                }else{ //이전 데이터가 없을 때 = 이 사진이 이 채팅방에서 주고받은 최초의 사진일 때

                    Log.d("스와이프", "db에 이전 데이터가 없음");
                    isFirstImage = true;

                }

                isLeftSideLoaded = true;

            }else{
                Log.d("스와이프", "왼쪽은 이미 load 됨. db를 검색하지 않음");
            }


        }else if(currentIndex >= imageItemArrayList.size() -1){ //이 사진 이후에 띄워줄 데이터가 없는 경우
            Log.d("스와이프", "currentIndex == url_list.size() -1. 이후 데이터가 떨어짐");

            if(!isRightSideLoaded){
                Log.d("스와이프", "오른쪽 데이터가 로드된 적이 없음. 지금부터 시작");

//                swipe_direction = 1;

                //db에서 이후 데이터를 더 가져온다
                //오래된 순서대로 가져와야함
                Cursor cursor3 = db.rawQuery
                        ("SELECT * FROM chat_logs WHERE id>'"+current_db_id+"' AND room_id='" + room_id + "' AND image_name != 'N' ORDER BY time;", null);

                result_count = cursor3.getCount();

                if(result_count>0){

                    while (cursor3.moveToNext()) {

                        //이 사진 이후로 주고받은 이미지의 이름을 가져온다
                        int db_id = cursor3.getInt(0);
                        String filename_string = cursor3.getString(5);


                        //여러 파일의 이름이 String 으로 이어져 있다. 개별 이름으로 분리한다
                        String[] filename_split = filename_string.split(";");

                        if(filename_split.length == 1){ //단독 이미지일 경우

                            String url = Function.domain+"/images/"+room_id+"/"+filename_string;

                            //썸네일 이미지인지 일반 이미지인지 확인한다
                            String[] filename_split_2 = filename_string.split("_");

                            if(filename_split_2[1].equals("video")){

                                //현재 데이터의 뒤에 새로운 데이터를 추가한다
                                imageItemArrayList.add(new SlideImageItem("video", room_id, filename_string, url, ""));

                            }else{

                                String video_path = cursor3.getString(8);
                                imageItemArrayList.add(new SlideImageItem("image", room_id, filename_string, url, video_path));
                            }


                        }else{ //다중이미지일 경우

                            for(int i=0; i<filename_split.length; i++){

                                String filename = filename_split[i];
                                String url = Function.domain+"/images/"+room_id+"/"+filename;

//                String type, int room_id, String filename, String url, String video_path
                                imageItemArrayList.add(new SlideImageItem("image", room_id, filename, url, ""));
                            }

                        }


                        Log.d("스와이프", "이후 데이터 가져옴. db_id="+db_id+" / filename_string="+filename_string);
                    }

                    Log.d("스와이프", "이후 데이터 업데이트 완료");


                    //어댑터를 새것으로 교체한다
                    slideAdapter = new SlideAdapter(getApplicationContext(), imageItemArrayList);
                    slideAdapter.notifyDataSetChanged();
                    viewPager.setAdapter(slideAdapter);
                    viewPager.setCurrentItem(origin_size-1);

//                    handler.post(new Runnable() {
//                        @Override
//                        public void run() {
//
//                            if(beforeInit){
//                                slideAdapter = new SlideAdapter(ImageActivity.this, url_list);
//                                viewPager.setAdapter(slideAdapter);
//                                viewPager.setCurrentItem(origin_size-1);
//                                Log.d("스와이프", "3");
//
//                                beforeInit = false;
//                            }else{
//                                slideAdapter.notifyDataSetChanged();
//                                viewPager.setCurrentItem(origin_size-1);
//                                Log.d("스와이프", "4");
//                            }
//                        }
//                    });


                }else{//이후 데이터가 없을 때 = 이 사진이 이 채팅방에서 주고받은 마지막 사진일 때

                    Log.d("스와이프", "db에 이후 데이터가 없음");
                    isLastImage = true;
                }

                isRightSideLoaded = true;

            }else{
                Log.d("스와이프", "오른쪽은 이미 load 됨. db를 검색하지 않음");
            }

        }

    }




    public int loadAllImages(){

        Log.d("스와이프", "loadAllImages()");

        //데이터가 추가되기 전, url_list 의 사이즈
        final int origin_size = imageItemArrayList.size();

        Log.d("스와이프", "데이터가 추가되기 전, list 사이즈 = "+origin_size);

        //db에서 찾은 row의 개수를 담는 변수
        int result_count = 0;

        int added_image_count_left = 0;


        Log.d("스와이프", "room_id="+room_id+" / filename_string = "+filename_string+" 현재 사진이 db의 chat_logs 테이블에서 어떤 id를 가지고 있는지 확인한다");
        //현재 사진이 db의 chat_logs 테이블에서 어떤 id를 가지고 있는지 확인한다
        int current_db_id = 0;
        //roomId와 image name이 일치하는 row를 찾는다
        Cursor cursor = db.rawQuery
                ("SELECT * FROM chat_logs WHERE room_id='" + room_id + "' AND image_name= '"+filename_string+"';", null);
        while (cursor.moveToNext()) {
            current_db_id = cursor.getInt(0);
        }
        Log.d("스와이프", "current_db_id="+current_db_id);




        //db에서 이전 데이터를 더 가져온다
        //최근 순서대로 가져와야함(order by time desc)
        Cursor cursor2 = db.rawQuery
                ("SELECT * FROM chat_logs WHERE id<'"+current_db_id+"' AND room_id='" + room_id + "' AND image_name != 'N' ORDER BY time DESC;", null);

        //결과 개수를 확인
        result_count = cursor2.getCount();
        if(result_count > 0){
            Log.d("스와이프", "db에 이전 데이터가 있음. 가져오면 됨");

            while (cursor2.moveToNext()) {

                //이전에 주고받은 이미지의 이름을 가져온다
                int db_id = cursor2.getInt(0);
                String filename_string = cursor2.getString(5);


                //여러 파일의 이름이 String 으로 이어져 있다. 개별 이름으로 분리한다
                String[] filename_split = filename_string.split(";");


                if(filename_split.length == 1){ //단독 이미지일 경우

                    Log.d("스와이프", "단독 이미지임. 썸네일인지 일반 이미지인지 확인");

                    added_image_count_left +=1;

                    String url = Function.domain+"/images/"+room_id+"/"+filename_string;

                    //썸네일 이미지인지 일반 이미지인지 확인한다
                    String[] filename_split_2 = filename_string.split("_");

                    if(filename_split_2[1].equals("video")){
                        Log.d("스와이프", "썸네일임");

                        imageItemArrayList.add(0, new SlideImageItem("video", room_id, filename_string, url, ""));

                    }else{
                        Log.d("스와이프", "일반 이미지임");

                        String video_path = cursor2.getString(8);
                        imageItemArrayList.add(0, new SlideImageItem("image", room_id, filename_string, url, video_path));
                    }


                }else{ //다중이미지일 경우

                    Log.d("스와이프", "다중 이미지임");

                    //가장 최근 사진부터 추가해야 하므로, 인덱스를 거꾸로 놓는다
                    for(int i=filename_split.length-1; i>=0; i--){

                        String filename = filename_split[i];
                        String url = Function.domain+"/images/"+room_id+"/"+filename;

//                String type, int room_id, String filename, String url, String video_path
                        imageItemArrayList.add(0, new SlideImageItem("image", room_id, filename, url, ""));

                        added_image_count_left +=1;
                    }

                }

                Log.d("스와이프", "이전데이터 가져옴. db_id="+db_id+" / filename_string="+filename_string);
            }

            Log.d("스와이프", "이전데이터 업데이트 완료. 왼쪽 데이터 몇개 추가 했음? "+added_image_count_left+" 개");


        }else{ //이전 데이터가 없을 때 = 이 사진이 이 채팅방에서 주고받은 최초의 사진일 때

            Log.d("스와이프", "db에 이전 데이터가 없음");
        }

        isLeftSideLoaded = true;

        



        //db에서 이후 데이터를 더 가져온다
        //오래된 순서대로 가져와야함
        Cursor cursor3 = db.rawQuery
                ("SELECT * FROM chat_logs WHERE id>'"+current_db_id+"' AND room_id='" + room_id + "' AND image_name != 'N' ORDER BY time;", null);

        result_count = cursor3.getCount();

        if(result_count>0){

            while (cursor3.moveToNext()) {

                //이 사진 이후로 주고받은 이미지의 이름을 가져온다
                int db_id = cursor3.getInt(0);
                String filename_string = cursor3.getString(5);

                //현재 데이터의 뒤에 새로운 데이터를 추가한다
                String[] filename_split = filename_string.split(";");


                if(filename_split.length == 1){ //단독 이미지일 경우

                    String url = Function.domain+"/images/"+room_id+"/"+filename_string;

                    //썸네일 이미지인지 일반 이미지인지 확인한다
                    String[] filename_split_2 = filename_string.split("_");

                    if(filename_split_2[1].equals("video")){

                        //현재 데이터의 뒤에 새로운 데이터를 추가한다
                        imageItemArrayList.add(new SlideImageItem("video", room_id, filename_string, url, ""));

                    }else{

                        String video_path = cursor3.getString(8);
                        imageItemArrayList.add(new SlideImageItem("image", room_id, filename_string, url, video_path));
                    }


                }else{ //다중이미지일 경우

                    for(int i=0; i<filename_split.length; i++){

                        String filename = filename_split[i];
                        String url = Function.domain+"/images/"+room_id+"/"+filename;

//                String type, int room_id, String filename, String url, String video_path
                        imageItemArrayList.add(new SlideImageItem("image", room_id, filename, url, ""));
                    }

                }
            }

            Log.d("스와이프", "이후 데이터 업데이트 완료");



        }else{//이후 데이터가 없을 때 = 이 사진이 이 채팅방에서 주고받은 마지막 사진일 때

            Log.d("스와이프", "db에 이후 데이터가 없음");
        }

        isRightSideLoaded = true;


        //왼쪽에 추가된 이미지이름 개수 = 최초에 불러온 이미지의 updated 된 position
        return added_image_count_left;
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
//                progressBar.setVisibility(View.VISIBLE);

                int current_position = viewPager.getCurrentItem();
                String type = imageItemArrayList.get(current_position).getType();

                if(type.equals("image")){

                    new ImageDownload().execute(imageItemArrayList.get(viewPager.getCurrentItem()).getUrl(), "image");

                }else if(type.equals("video")){

                    String video_path = imageItemArrayList.get(viewPager.getCurrentItem()).getVideo_path();


                    //동영상 파일의 서버 경로를 가져온다
                    Cursor cursor = db.rawQuery
                            ("SELECT video_path_server FROM chat_logs WHERE video_path='"+video_path+"';", null);

                    cursor.moveToFirst();
                    String video_server_url = cursor.getString(0);

                    //로컬 경로가 있으면
                    if(video_server_url != null){
                        Toast.makeText(this, "Downloading..", Toast.LENGTH_SHORT).show();
                        new ImageDownload().execute(video_server_url, "video");

                    }else{
                        Toast.makeText(this, "Download failed: cannot find url", Toast.LENGTH_SHORT).show();
                    }

                }

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
        private final String MY_FOLDER = "/Canaria_download"; //내가 원하는 저장 경로(폴더 이름)
        String path_final = "";


        @Override protected String doInBackground(String... params) {


            //웹 서버 쪽 파일이 있는 경로
            String fileUrl = params[0];
            String dataType = params[1];


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

            if(dataType.equals("image")){
                path_final = pathname + ".jpg";
            }else if(dataType.equals("video")){
                path_final = pathname + ".mp4";
            }

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

//            progressBar.setVisibility(View.GONE);

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
