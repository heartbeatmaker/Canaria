package com.android.canaria;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.ContactsContract;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.connect_to_server.MainService;
import com.android.canaria.connect_to_server.VideoCompressor;
import com.android.canaria.db.DBHelper;
import com.android.canaria.recyclerView.FriendListAdapter;
import com.android.canaria.recyclerView.FriendListItem;
import com.android.canaria.recyclerView.MessageAdapter;
import com.android.canaria.recyclerView.MessageItem;
import com.android.canaria.recyclerView.RoomListItem;
import com.esafirm.imagepicker.features.ImagePicker;
import com.esafirm.imagepicker.features.ReturnMode;
import com.esafirm.imagepicker.model.Image;
import com.iceteck.silicompressorr.SiliCompressor;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import cz.msebera.android.httpclient.entity.mime.content.ContentBody;
import cz.msebera.android.httpclient.entity.mime.content.FileBody;
import id.zelory.compressor.Compressor;
import io.reactivex.schedulers.Schedulers;
import me.nereo.multi_image_selector.MultiImageSelector;
import me.nereo.multi_image_selector.MultiImageSelectorActivity;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Retrofit;

import static javax.xml.transform.OutputKeys.MEDIA_TYPE;

public class ChatActivity extends AppCompatActivity{

    EditText msgInput_editText;
    Button sendMsg_btn;
    LinearLayout input_linearLayout;
    RelativeLayout pickAction_relativeLayout;
    ImageButton plus_btn, gallery_imageBtn, takePic_imageBtn, cancel_btn, videoBtn;

    int userId;
    String username, roomName;
    public static int roomId;

    ActionBar actionBar;
    MenuItem menuItem;

    String TAG = "tag "+this.getClass().getSimpleName();

    //메시지 리사이클러뷰 변수
    RecyclerView rcv;
    ArrayList<MessageItem> messageItemList;
    MessageAdapter adapter;
    LinearLayoutManager linearLayoutManager;


    //참여자 목록 리사이클러뷰
    RecyclerView members_rcv;
    ArrayList<FriendListItem> memberList;
    FriendListAdapter members_adapter;
    LinearLayoutManager members_linearLayoutManager;

    DrawerLayout drawer;

    Handler handler;
    boolean isNewRoom = false;

    int msg_sender_id, msg_roomId;
    String msg_sender_username;
    String msg_text, msg_filename_string;

    String[] message_array;

    private static final int INVITATION_REQUEST = 1000;
    private static final int PICK_IMAGE_REQUEST = 2;
    private static final int PICK_VIDEO_REQUEST = 5;

    boolean isPlusBtnActive = false;

    String upLoadServerUri = "http://15.164.193.65/multi_fileUpload.php";//서버컴퓨터의 ip주소

    DBHelper dbHelper;

    HashMap<String, String> video_hash;
    ArrayList<String> video_path_list;
//    public static ArrayList<Integer> uploading_video_db_id_list = new ArrayList<>();
    int video_count;

    String dataType;
    String video_path_server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        handler = new Handler();

        dbHelper = new DBHelper(getApplicationContext(), Function.dbName, null, Function.dbVersion);
        dbHelper.open();

        Log.d(TAG, "onCreate");


        msgInput_editText = (EditText) findViewById(R.id.chat_message_editText);
        sendMsg_btn = (Button)findViewById(R.id.chat_send_btn);
        plus_btn = (ImageButton)findViewById(R.id.chat_plus_btn);
        input_linearLayout = (LinearLayout)findViewById(R.id.chat_input_linearLayout);
        pickAction_relativeLayout = (RelativeLayout) findViewById(R.id.chat_pickAction_relativeLayout);
        gallery_imageBtn = (ImageButton)findViewById(R.id.chat_galleryBtn);
        takePic_imageBtn = (ImageButton)findViewById(R.id.chat_cameraBtn);
        videoBtn = (ImageButton)findViewById(R.id.chat_videoBtn);
        cancel_btn = (ImageButton)findViewById(R.id.chat_cancelBtn);


        //메시지 리사이클러뷰 초기화
        rcv = (RecyclerView)findViewById(R.id.chat_message_rcv);
        linearLayoutManager = new LinearLayoutManager(this);
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        messageItemList = new ArrayList<>();
        adapter = new MessageAdapter(messageItemList, this);
        rcv.setAdapter(adapter);


        //(drawerLayout에 띄워주는) 참여자 리사이클러뷰 초기화
        members_rcv = (RecyclerView)findViewById(R.id.chatRoom_members_rcv);
        members_linearLayoutManager = new LinearLayoutManager(this);
        members_rcv.setHasFixedSize(true);
        members_rcv.setLayoutManager(members_linearLayoutManager);
        memberList = new ArrayList<>();
        members_adapter = new FriendListAdapter(memberList, this);
        members_rcv.setAdapter(members_adapter);


        actionBar = getSupportActionBar();
        actionBar.show();


        //현재 사용자의 정보를 가져온다
        userId = Integer.valueOf(Function.getString(getApplicationContext(), "user_id"));
        username = Function.getString(getApplicationContext(), "username");


        try{
            String isNewRoom_string = getIntent().getStringExtra("isNewRoom");
            if(isNewRoom_string.equals("Y")){
                isNewRoom = true;
            }else{
                isNewRoom = false;
            }
        }catch (Exception e){
            Log.d(TAG, "intent error");
            e.printStackTrace();
        }


        if(isNewRoom){ //새로 만든 방이라면
            //이전 액티비티에서 전달한 '친구 정보'를 받아온다
            Log.d(TAG, "is new room");

            String friendInfo = getIntent().getStringExtra("friendInfo_jsonArray");

            //java 서버에서 json을 못 읽는다. 참여자들 정보를 한 문장으로 구성한다
            //친구1id;친구1username;친구2id;친구2username..
            String friendInfo_string = "";
            try{
                JSONArray jsonArray = new JSONArray(friendInfo);
                for(int i=0; i<jsonArray.length(); i++){
                    JSONObject friendObject = new JSONObject();
                    friendObject = (JSONObject)jsonArray.get(i);

                    String friend_id = friendObject.getString("id");
                    String friend_username = friendObject.getString("username");

                    if(i == 0){
                        friendInfo_string = friend_id + ";" +friend_username;
                    }else{
                        friendInfo_string += ";"+friend_id + ";" +friend_username;
                    }
                }

                //서비스에 메시지 전달
                sendMsg("new_room/"+friendInfo_string);

            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);
            }


        }else{ //기존에 참여하던 방에 다시 들어온 것이라면

            roomId = getIntent().getIntExtra("roomId", 10000); // 이전 화면에서 전달한 방 id

            //저장된 메시지를 가져와서 화면에 띄워준다
            getSavedMsg(roomId);

        }



        //메시지 보내기 버튼을 클릭하면 -> 서비스로 메시지를 보낸다
        sendMsg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");

                String message_input = msgInput_editText.getText().toString();
                sendMsg("msg/"+roomId+"/"+message_input);
                msgInput_editText.setText("");
            }
        });


        //친구 초대 버튼
        Button invite_btn = (Button)findViewById(R.id.chatRoom_invite_btn);
        invite_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String existing_members_string = "";

                for(int i=0; i<memberList.size(); i++){
                    existing_members_string += memberList.get(i).getFriendId() + ";";
                }

                existing_members_string = existing_members_string.substring(0, existing_members_string.length()-1);

                //초대할 친구를 고르는 화면으로 넘어간다
                //현재 참여자는 목록에 보여주면 안되기 때문에, 현재 참여자들의 id를 인텐트로 보낸다
                Intent intent_select_friends = new Intent(getApplicationContext(), SelectFriendsActivity.class);
                intent_select_friends.putExtra("invitation", "Y");
                intent_select_friends.putExtra("existing_members", existing_members_string);
                Log.d("invitation", "invite 버튼 누름. existing_members="+existing_members_string);
                startActivityForResult(intent_select_friends, INVITATION_REQUEST);

                drawer.closeDrawer(GravityCompat.END);


            }
        });




        plus_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isPlusBtnActive = true;
                input_linearLayout.setVisibility(View.INVISIBLE);
                pickAction_relativeLayout.setVisibility(View.VISIBLE);
            }
        });


        gallery_imageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                https://github.com/esafirm/android-image-picker 라이브러리 주소

                ImagePicker.create(ChatActivity.this)
//                        .returnMode(ReturnMode.ALL) // set whether pick and / or camera action should return immediate result or not.
                        .folderMode(false) // folder mode (false by default)
                        .toolbarFolderTitle("Folder") // folder selection title
                        .toolbarImageTitle("Tap to select") // image selection title
                        .toolbarArrowColor(Color.WHITE) // Toolbar 'up' arrow color
                        .includeVideo(true) // Show video on image picker
//                        .single() // single mode
                        .multi() // multi mode (default mode)
                        .limit(9) // max images can be selected (99 by default)
                        .showCamera(true) // show camera or not (true by default)
                        .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
//                        .origin(images) // original selected images, used in multi mode
//                        .exclude(images) // exclude anything that in image.getPath()
//                        .excludeFiles(files) // same as exclude but using ArrayList<File>
//                        .theme(R.style.CustomImagePickerTheme) // must inherit ef_BaseTheme. please refer to sample
                        .enableLog(false) // disabling log
//                        .imageLoader(new GrayscaleImageLoder()) // custom image loader, must be serializeable
                        .start(PICK_IMAGE_REQUEST); // start image picker activity with request code


                //뷰 안보이게 처리
                pickAction_relativeLayout.setVisibility(View.INVISIBLE);
                input_linearLayout.setVisibility(View.VISIBLE);
                isPlusBtnActive = false;
            }
        });



        videoBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ImagePicker.create(ChatActivity.this)
//                        .returnMode(ReturnMode.ALL) // set whether pick and / or camera action should return immediate result or not.
                        .folderMode(true) // folder mode (false by default)
                        .toolbarFolderTitle("Folder") // folder selection title
                        .toolbarImageTitle("Tap to select") // image selection title
                        .toolbarArrowColor(Color.WHITE) // Toolbar 'up' arrow color
                        .includeVideo(true) // Show video on image picker
//                        .single() // single mode
                        .multi() // multi mode (default mode)
                        .limit(10) // max images can be selected (99 by default)
                        .showCamera(true) // show camera or not (true by default)
                        .imageDirectory("Camera") // directory name for captured image  ("Camera" folder by default)
//                        .origin(images) // original selected images, used in multi mode
//                        .exclude(images) // exclude anything that in image.getPath()
//                        .excludeFiles(files) // same as exclude but using ArrayList<File>
//                        .theme(R.style.CustomImagePickerTheme) // must inherit ef_BaseTheme. please refer to sample
                        .enableLog(false) // disabling log
//                        .imageLoader(new GrayscaleImageLoder()) // custom image loader, must be serializeable
                        .start(PICK_VIDEO_REQUEST); // start image picker activity with request code


                //뷰 안보이게 처리
                pickAction_relativeLayout.setVisibility(View.INVISIBLE);
                input_linearLayout.setVisibility(View.VISIBLE);
                isPlusBtnActive = false;


            }
        });



        cancel_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPlusBtnActive){
                    pickAction_relativeLayout.setVisibility(View.INVISIBLE);
                    input_linearLayout.setVisibility(View.VISIBLE);
                    isPlusBtnActive = false;
                }
            }
        });

    }



    @Override
    public void onBackPressed() {

        if(isPlusBtnActive){
            pickAction_relativeLayout.setVisibility(View.INVISIBLE);
            input_linearLayout.setVisibility(View.VISIBLE);
            isPlusBtnActive = false;

        }else{
            super.onBackPressed();
            finish();
        }
    }





    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == INVITATION_REQUEST && resultCode == RESULT_OK){

            Log.d("invitation", "onActivityForResult");

            String friendInfo = data.getStringExtra("friendInfo_jsonArray");

            //java 서버에서 json을 못 읽는다. 참여자들 정보를 한 문장으로 구성한다
            //친구1id;친구1username;친구2id;친구2username..
            String friendInfo_string = "";
            try{
                JSONArray jsonArray = new JSONArray(friendInfo);
                for(int i=0; i<jsonArray.length(); i++){
                    JSONObject friendObject = new JSONObject();
                    friendObject = (JSONObject)jsonArray.get(i);

                    String friend_id = friendObject.getString("id");
                    String friend_username = friendObject.getString("username");

                    if(i == 0){
                        friendInfo_string = friend_id + ";" +friend_username;
                    }else{
                        friendInfo_string += ";"+friend_id + ";" +friend_username;
                    }
                }

                Log.d("invitation", "초대할 친구 info = "+friendInfo_string);

                //서비스에 메시지 전달
                sendMsg("invite/"+roomId+"/"+friendInfo_string);

            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);
            }

        }else if(requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK){


            //변수 초기화
            video_hash = new HashMap<>();
            video_path_list = new ArrayList<>();


            final OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(40, TimeUnit.SECONDS)
                    .build();

            MultipartBody.Builder mRequestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            try{

                List<Image> images_list = ImagePicker.getImages(data);
                int count = images_list.size();

                if(count > 0){ //파일이 1개 이상 선택되었을 때만 업로드한다

                    //방 id, 파일이 몇 개인지 전달한다
                    mRequestBody.addFormDataPart("room_id", String.valueOf(roomId));
                    mRequestBody.addFormDataPart("count", String.valueOf(count));

                    Log.d("이미지", "방 id ="+roomId);
                    Log.d("이미지", "선택된 파일 개수 ="+count);

                    video_count = 0;
                    for(int i = 0; i < count; i++){

                        String path = images_list.get(i).getPath();
                        Log.d("이미지", i+"번째 파일의 path="+path);

                        File file = null;

//                    MediaType MEDIA_TYPE = path.get(0).endsWith("png") ?
//                            MediaType.parse("image/png") : MediaType.parse("image/jpeg");

                        String[] path_split = path.split("/");
                        String file_name = path_split[path_split.length-1];
                        Log.d("이미지", i+"번째 파일의 file_name="+file_name);


                        //확장자를 추출한다
                        String[] file_name_split = file_name.split("\\.");

                        String extension = file_name_split[file_name_split.length-1];
                        Log.d("이미지", i+"번째 파일의 확장자="+extension);

                        //이미지와 동영상을 구분한다
                        if(extension.equals("jpg")){ //이미지일 때
                            Log.d("이미지", i+"번째 파일은 이미지임");

                            file = new File(path);
                            //이미지 파일 압축
                            file = new Compressor(this)
                                    .setQuality(30)
                                    .compressToFile(file);

                            Log.d("이미지", i+"번째 파일을 압축함");

                        }else if(extension.equals("mp4")){ //동영상일 때
                            Log.d("이미지", i+"번째 파일은 동영상임");

                            //동영상마다 인덱스를 지정하여, 동영상 경로를 리스트에 담는다
                            video_path_list.add(video_count, path);


                            //파일 크기를 검사한다
                            File videoFile = new File(path);
                            float length = videoFile.length() / 1024f; // Size in KB
                            String value;
                            float size;
                            if (length >= 1024){

                                size = (int) Math.ceil(length / 1024f);
                                value = size + " MB";

                                //파일 크기가 50MB를 넘으면, 업로드하지 않는다
                                if(size > 50){
                                    Toast.makeText(this, "Cannot upload a file with the size over 50MB.", Toast.LENGTH_SHORT).show();
                                    continue;
                                }
                            }else{
                                value = length + " KB";
                            }
                            Log.i("이미지", i+"번째 파일의 용량="+value);



                            // 동영상의 썸네일 이미지를 추출한다
                            //비트맵으로 추출한 썸네일을 임시경로에 저장한다
                            Log.i("이미지", i+"번째 파일(동영상)의 썸네일을 추출함");
                            Bitmap bitmap = ThumbnailUtils.createVideoThumbnail(path, MediaStore.Video.Thumbnails.FULL_SCREEN_KIND);
                            Bitmap thumbnail_bitmap = ThumbnailUtils.extractThumbnail(bitmap, 360, 480);


                            //썸네일 파일 이름 = video_원래이름.jpg
                            String thumbnail_filename = "video_"+video_count+"_"+file_name.substring(0, file_name.length()-4)+".jpg";
                            Log.i("이미지", i+"번째 파일(동영상)의 썸네일 이미지 이름="+thumbnail_filename);



                            //파일 생성
                            File thumbnail_file = createImageFile_thumbnail(thumbnail_filename);

                            FileOutputStream out = new FileOutputStream(thumbnail_file);
                            // 넘겨 받은 bitmap을 jpeg(손실압축)으로 저장한다
                            thumbnail_bitmap.compress(Bitmap.CompressFormat.JPEG, 70 , out);
                            out.close();

                            //file_name = 썸네일 이름
                            file_name = thumbnail_filename;

                            //file = 썸네일 파일
                            String thumbnail_path = thumbnail_file.getAbsolutePath(); //썸네일 파일 경로
                            file = new File(thumbnail_path);


                            video_count++;
                        }



                        //최종 이미지 파일의 크기를 검사한다

                        float length = file.length() / 1024f; // Size in KB
                        String value;
                        float size;
                        if (length >= 1024){

                            size = (int) Math.ceil(length / 1024f);
                            value = size + " MB";

                            //파일 크기가 50MB를 넘으면, 업로드하지 않는다
                            if(size > 50){
                                Toast.makeText(this, "Cannot upload a file with the size over 50MB.", Toast.LENGTH_SHORT).show();
                                continue;
                            }
                        }
                        else
                            value = length + " KB";

                        Log.i("이미지", i+"번째 파일(동영상)의 최종 크기="+value);


                        RequestBody imageBody = RequestBody.create(MultipartBody.FORM, file);
                        //key, 서버가 저장할 file 이름, 이미지파일
                        mRequestBody.addFormDataPart("image"+i, file_name, imageBody);
                    }




                    RequestBody rb = mRequestBody.build();

                    final Request request = new Request.Builder()
                            .url(upLoadServerUri)
                            .post(rb)
                            .build();



                    new Thread(new Runnable() {

                        public void run() {

                            String responseMsg;
                            try {

                                //서버에 요청을 보낸다
                                Response mResponse = mOkHttpClient.newCall(request).execute();
                                if (!mResponse.isSuccessful()) throw new IOException();

                                //서버로부터의 응답
                                responseMsg = mResponse.body().string();
                                Log.d("이미지", "response msg = "+responseMsg);


//                    mOkHttpClient.newCall(request).enqueue(new Callback() {
//                        @Override
//                        public void onFailure(Call call, IOException e) {
//
//                        }
//
//                        @Override
//                        public void onResponse(Call call, Response response) throws IOException {
//                            Log.d("이미지", "onResponse: " + response.body().string());
//                        }
//                    });


                            } catch (IOException e) {
                                responseMsg = "time out";

                                StringWriter sw = new StringWriter();
                                e.printStackTrace(new PrintWriter(sw));
                                String ex = sw.toString();

                                Log.d("이미지",ex);
                            }

                            try{

                                //json 형태로 받은 응답을 파싱한다
                                JSONObject result_object = new JSONObject(responseMsg);
                                final JSONArray success_array = (JSONArray) result_object.get("success_data");
                                JSONArray fail_array = (JSONArray)result_object.get("fail_data");

                                Log.d("이미지", "success_array = "+success_array);
                                Log.d("이미지", "fail_array = "+fail_array);


                                //업로드 실패한 파일이 있다면, 사용자에게 그 사실을 알려준다
                                String imageName_failed = "";
                                if(fail_array.length()>0){
                                    for(int k=0; k<fail_array.length(); k++){
                                        imageName_failed += fail_array.get(k) + ",";
                                    }
                                    //마지막 , 제거
                                    imageName_failed = imageName_failed.substring(0, imageName_failed.length()-1);

                                    //토스트메시지로 파일이름을 보여준다
                                    Toast.makeText(ChatActivity.this, "Failed to upload "+imageName_failed, Toast.LENGTH_SHORT).show();
                                }


                                final long curTime = System.currentTimeMillis();
                                if(success_array.length() == 0){ //업로드 성공한 이미지가 없을 경우

                                }else{//업로드 성공한 이미지가 있을 경우 -> 업로드 성공한 파일 이름을 db에 저장한다

                                    Log.d("이미지", "서버에 업로드 성공한 이미지가 있음");

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {


                                            //jsonArray 에 있던 파일 이름을 string 으로 이어서 붙인다
                                            //파일이름1;파일이름2;파일이름3...
                                            String image_filename_string = "";
                                            try{
                                                for(int k=0; k<success_array.length(); k++){

                                                    String filename = success_array.getString(k);
                                                    Log.d("이미지", k+"번째 filename="+filename);

                                                    //서버에서 받은 썸네일 파일 이름 = 날짜_video_index_원래이름.jpg or (1).jpg
                                                    //video_list 의 index와 썸네일 이름의 index가 일치하는지 검사한다
                                                    //일치하면 -> hashmap에 담는다

                                                    String[] filename_split = filename.split("_");
                                                    Log.d("이미지", "이 파일이 동영상인지 검사. filename_split[1]="+filename_split[1]);

                                                    if(filename_split[1].equals("video")){ //이 파일이 비디오라면
                                                        Log.d("이미지", k+"번째 파일은 동영상 썸네일임");


                                                        //인덱스를 가져온다
                                                        int index_extracted = Integer.valueOf(filename_split[2]);
                                                        Log.d("이미지", k+"번째 파일(동영상 썸네일)의 index="+index_extracted);

                                                        Log.d("이미지", k+"번째 파일(동영상 썸네일)의 원본 동영상 path="+video_path_list.get(index_extracted));

                                                        //key=썸네일 이름, value=원본 동영상 파일 경로
                                                        video_hash.put(filename, video_path_list.get(index_extracted));

                                                    }else{ //이 파일이 사진이라면
                                                        Log.d("이미지", k+"번째 파일은 사진임");

                                                        image_filename_string += filename + ";";
                                                    }

                                                }

                                            }catch (Exception e){
                                                StringWriter sw = new StringWriter();
                                                e.printStackTrace(new PrintWriter(sw));
                                                String ex = sw.toString();

                                                Log.d(TAG,ex);
                                            }


                                            //이미지 파일이 존재하는 경우
                                            if(!image_filename_string.equals("")){
                                                //마지막 ; 제거
                                                Log.d("이미지", "이미지 파일의 이름을 string으로 엮음. image_filename_string="+image_filename_string);
                                                image_filename_string = image_filename_string.substring(0, image_filename_string.length()-1);


                                                //---------------이미지 파일을 처리하는 부분---------------

                                                //1. 이미지 파일의 이름을, db에 메시지 형태로 저장한다

                                                //메시지 내용: 'Photo'라고 저장한다. 방목록이나 푸쉬 메시지에서 띄워줄 내용
                                                String message = "";
                                                int number_of_files = success_array.length()-video_hash.size(); //파일 개수
                                                if(number_of_files == 1){
                                                    message = number_of_files + " Photo";
                                                }else if(number_of_files >1){
                                                    message = number_of_files + " Photos";
                                                }

                                                dbHelper.insert_chatLogs(roomId, userId, username, message, image_filename_string, curTime, 1);



                                                //2. 채팅 방목록 업데이트
                                                //2-1. sqlite 에서 방 정보를 불러온다
                                                String roomInfo = dbHelper.get_chatRoomInfo(roomId);
                                                String [] roomInfo_array = roomInfo.split("/");
                                                String roomName_msg = roomInfo_array[1];
                                                String memberInfo = roomInfo_array[3];
                                                String[] memberInfo_array = memberInfo.split(";");
                                                int number_of_members_msg = memberInfo_array.length/2;

                                                //2-2. 목록 맨 위에 아이템을 추가하고, 기존 아이템을 삭제한다
                                                Main_Fragment2.roomItemList.add(0, new RoomListItem(roomName_msg, number_of_members_msg,
                                                        message, Function.getCurrentTime(), roomId, 0));

                                                for(int i=Main_Fragment2.roomItemList.size()-1; i>0; i--){
                                                    RoomListItem item = Main_Fragment2.roomItemList.get(i);
                                                    if(item.getRoomId() == roomId){
                                                        Main_Fragment2.roomItemList.remove(i);
                                                        Log.d(TAG, i+" item is removed from roomItemList");
                                                    }
                                                }


                                                //3. 메시지 리사이클러뷰에 아이템을 추가한다
                                                messageItemList.add(new MessageItem(userId, username, "", roomId, image_filename_string, curTime, 0, "", ""));

                                                //메시지 리사이클러뷰 업데이트 - 아래 동영상 부분과 겹친다
//                                            adapter.notifyDataSetChanged();
//                                            rcv.scrollToPosition(messageItemList.size()-1);


                                                //4. 채팅 서버에 메시지를 보낸다
                                                sendMsg("msg_image/"+roomId+"/"+image_filename_string);



                                            }else{
                                                Log.d("이미지", "image_filename_string 이 빈 값임");
                                            }


                                            //--------------------------------이미지 처리 끝---------------------------------




                                            //---------------------동영상 파일을 처리하는 부분----------------------------


                                            if(video_hash.size()>0){ //동영상 썸네일이 있을 경우에만 아래 코드를 실행한다
                                                Log.d("이미지", "화면에 띄울 썸네일 파일이 있음. 메시지 화면에 추가하기 시작");

                                                Set set = video_hash.keySet();

                                                Iterator iterator = set.iterator();
                                                while(iterator.hasNext()){

                                                    //hashmap 에서 key 와 value 를 가져온다
                                                    //key=썸네일 이름, value=원본 동영상 파일 경로
                                                    String thumbnail_image_name = (String)iterator.next();
                                                    String origin_video_path = video_hash.get(thumbnail_image_name);

                                                    Log.d("이미지", "썸네일 이미지 이름 = "+thumbnail_image_name);
                                                    Log.d("이미지", "원본 동영상의 저장경로 = "+origin_video_path);


                                                    //1. db에 메시지 형태로 저장한다
                                                    //@@@@@ video_path 를 "yet"이라고 저장한다 @@@@
                                                    //db id를 받아온다
                                                    int db_id = dbHelper.insert_chatLogs_with_videoPath(roomId, userId, username, "1 Video", thumbnail_image_name, curTime, 1,
                                                            "yet");

                                                    Log.d("이미지", "썸네일 이미지를 db에 저장함. inserted_db_id = "+db_id);



                                                    //2. 메시지 리사이클러뷰에 아이템을 추가한다 (썸네일 이미지를 화면에 띄운다)
                                                    MessageItem messageItem = new MessageItem(userId, username, "", roomId, thumbnail_image_name, curTime, db_id, origin_video_path, "");

                                                    //원본 동영상 파일의 주소를 set 한다
//                                                    messageItem.setVideo_file_path(origin_video_path);

                                                    //이 메시지의 db id도 set 한다
//                                                    messageItem.setDb_id(db_id);

                                                    messageItemList.add(messageItem);

                                                    Log.d("이미지", "메시지 리사이클러뷰에 아이템을 추가함");

//                                                    uploading_video_db_id_list.add(db_id); //업로드 중인 동영상 목록에 이 동영상의 db id를 추가한다

                                                }

                                            }

                                            //--------------------------------동영상 처리 끝---------------------------------


                                            //메시지 리사이클러뷰 업데이트
                                            adapter.notifyDataSetChanged();
                                            rcv.scrollToPosition(messageItemList.size()-1);
                                        }
                                    });

                                }


                            }catch (Exception e){
                                StringWriter sw = new StringWriter();
                                e.printStackTrace(new PrintWriter(sw));
                                String ex = sw.toString();

                                Log.d(TAG,ex);
                            }


                        }

                    }).start();


                }


            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d("이미지",ex);
            }

        }

    }



    private String getRealPathFromURI(Uri contentUri) {
        if (contentUri.getPath().startsWith("/storage")) {
            return contentUri.getPath();
        }

        String id = DocumentsContract.getDocumentId(contentUri).split(":")[1];
        String[] columns = { MediaStore.Files.FileColumns.DATA };
        String selection = MediaStore.Files.FileColumns._ID + " = " + id;
        Cursor cursor = getContentResolver().query(MediaStore.Files.getContentUri("external"), columns, selection, null, null);

        try {
            int columnIndex = cursor.getColumnIndex(columns[0]);
            if (cursor.moveToFirst()) {
                return cursor.getString(columnIndex);
            }
        } finally {
            cursor.close();
        } return null;
    }









    //이 방의 참여자 정보를 가져와서 화면에 띄워주는 메소드
    private void getMemberInfo(int roomId){

        Log.d("초대", "getMemberInfo");

        /*-------------방 정보를 가져온다--------------*/
        //db 연결
        DBHelper dbHelper = new DBHelper(getApplicationContext(), Function.dbName, null, Function.dbVersion);
        dbHelper.open();

        Cursor _cursor = dbHelper.db.rawQuery("SELECT members FROM chat_rooms WHERE room_id='" + roomId + "';", null);
        _cursor.moveToFirst();
        String memberInfo_string = _cursor.getString(0);
        /*-------------------------------------------*/
        Log.d("초대", "memberInfo_string="+memberInfo_string);

        String[] memberInfo_array = memberInfo_string.split(";");

        int member_id = 0;
        String member_username = "";
        for(int i=0; i<memberInfo_array.length; i++){

            if(i%2 == 0){ //i가 짝수일 때: id
                member_id = Integer.valueOf(memberInfo_array[i]);
            }else{ //i가 홀수일 때: username
                member_username = memberInfo_array[i];
                Log.d("초대", "리스트에 추가 직전. id:"+member_id+", username:"+member_username);
                memberList.add(new FriendListItem(member_username, member_id));
            }
        }
        members_rcv.scrollToPosition(0);
    }



    //저장된 메시지를 가져와서 화면에 띄워주는 메소드
    private void getSavedMsg(int roomId){

        /*-------------방 정보를 가져온다--------------*/
        //db 연결
        DBHelper dbHelper = new DBHelper(getApplicationContext(), Function.dbName, null, Function.dbVersion);
        dbHelper.open();

        Cursor _cursor = dbHelper.db.rawQuery("SELECT room_name FROM chat_rooms WHERE room_id='" + roomId + "';", null);
        _cursor.moveToFirst();
        String roomName = _cursor.getString(0);
        /*-------------------------------------------*/


        //서비스에 메시지 전달
        sendMsg("return/"+roomId+"/"+roomName);


        try{

            Cursor cursor0 = dbHelper.db.rawQuery("SELECT count(*) FROM chat_logs WHERE room_id='" + roomId + "' AND isRead=1;", null);
            cursor0.moveToFirst();
            int readMsgCount = cursor0.getInt(0);
            //안 읽은 메시지가 몇 개인지 확인한다
            Cursor cursor = dbHelper.db.rawQuery("SELECT count(*) FROM chat_logs WHERE room_id='" + roomId + "' AND isRead=0;", null);
            cursor.moveToFirst();
            int unreadMsgCount = cursor.getInt(0);

            Log.d(TAG, "unread message count="+unreadMsgCount);


            boolean isFirstUnreadMsg = true;
            int focus_index = 0;
            //저장된 메시지를 오래된 순서대로 화면에 띄워준다
            //채팅내용 테이블: id, 방id, 보낸사람 id, 보낸사람 username, 메시지내용, 이미지파일 이름, 보낸시각, 읽었는지
            Cursor cursor2 = dbHelper.db.rawQuery("SELECT * FROM chat_logs WHERE room_id='" + roomId + "' ORDER BY time;", null);
            while (cursor2.moveToNext()) {

                int message_id = cursor2.getInt(0);
                int room_id = cursor2.getInt(1);
                int sender_id = cursor2.getInt(2);
                String sender_username = cursor2.getString(3);
                String message = cursor2.getString(4);
                String image_name = cursor2.getString(5);
                long time = Long.valueOf(cursor2.getString(6));
                int isRead = cursor2.getInt(7);

                //동영상을 띄울 때 필요한 값
                String video_path = cursor2.getString(8); //내가 보낸 동영상일 때, 받은 동영상인데 다운받았을 때 -> 무조건 있어야 함
                String video_path_server = cursor2.getString(9);//내가 받은 동영상일 때 -> 무조건 있어야 함

                    Log.d(TAG,"id: "+message_id+" / room id: "+room_id+" / sender id: "+sender_id+" / sender_name : "+sender_username
                            +" / message: "+message+"/ image name: "+image_name+" / time: "+time+" / isRead: "+isRead+" / video_path: "+video_path+" / video_path_server: "+video_path_server);


                //이미지의 경우, 메시지가 Photo로 저장되어 있다
                // 텍스트메시지 내용이 있으면 어댑터에서 이미지 뷰가 gone 처리 된다. 메시지 내용을 없애야 한다
                if(!image_name.equals("N") && image_name.length()>10){
                    message = "";
                }

                if(unreadMsgCount > 0){ //안읽은 메시지가 있을 때
                    if(isRead == 0){ //안읽은 메시지 중에서
                        if(isFirstUnreadMsg){ //가장 오래된 메시지일 때

                            Log.d(TAG, "가장 오래된 안읽은 메시지임");

                            isFirstUnreadMsg = false;


                            if(unreadMsgCount>10){ //안읽은 메시지가 10개 초과: 안읽은 메시지가 한 화면을 넘길 때
                                focus_index = messageItemList.size(); //안읽은 메시지 중 가장 오래된 메시지에 스크롤 focus를 맞춘다

                                if(readMsgCount > 10){//읽은 메시지가 10개 초과일때
                                    //"여기서부터 안 읽었다"라고 메시지 위에 표시해준다
                                    messageItemList.add(new MessageItem(0, "server", "You haven't read messages from here.", room_id, "N", time, message_id, video_path, video_path_server));
                                    messageItemList.add(new MessageItem(sender_id, sender_username,message, room_id, image_name, time, message_id, video_path, video_path_server));

                                }else{ //읽은 메시지가 10개 이하일 때(주고받은 메시지 자체가 적을 때)
                                    //안읽음 표시를 하지 않는다
                                    messageItemList.add(new MessageItem(sender_id, sender_username,message, room_id, image_name, time, message_id, video_path, video_path_server));
                                }
                            }else{ //안읽은 메시지 개수가 10개 이하일때
                                messageItemList.add(new MessageItem(sender_id, sender_username,message, room_id, image_name, time, message_id, video_path, video_path_server));
                            }

                        }else{ //나머지 안읽은 메시지 -> 메시지를 화면에 표시한다
                            messageItemList.add(new MessageItem(sender_id, sender_username,message, room_id, image_name, time, message_id, video_path, video_path_server));
                        }

                    }else{ //이미 읽은 메시지일 때 -> 메시지를 화면에 표시한다
                        messageItemList.add(new MessageItem(sender_id, sender_username,message, room_id, image_name, time, message_id, video_path, video_path_server));
                    }
                }else{ //안읽은 메시지가 없을 때 -> 메시지를 화면에 표시한다

                    messageItemList.add(new MessageItem(sender_id, sender_username,message, room_id, image_name, time, message_id, video_path, video_path_server));
                }


                /*
                 * 페이징 해야함(최근 메시지 nn개씩 가져오기. 어디까지 가져왔는지 메시지 id를 변수에 넣어놓기)
                 * */
            }

            if(focus_index == 0){
                focus_index = messageItemList.size()-1;
            }

            rcv.scrollToPosition(focus_index);



            if(unreadMsgCount > 0){
                //안 읽은 메시지가 있다면 -> 이것을 읽은 메시지로 db를 업데이트한다
                Cursor cursor3 = dbHelper.db.rawQuery("SELECT id FROM chat_logs WHERE room_id='" + roomId + "' AND isRead=0;", null);
                while (cursor3.moveToNext()) {

                    int message_id = cursor3.getInt(0);
                    dbHelper.db.execSQL("UPDATE chat_logs SET isRead=1 WHERE id='" + message_id + "';");
                }


                //방목록 아이템을 업데이트한다(안읽은 메시지 개수를 0으로 바꾼다)
                for(RoomListItem room : Main_Fragment2.roomItemList){
                    if(room.getRoomId() == roomId){
                        room.setUnreadMsgCount(0);
                    }
                }

            }


        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d(TAG,ex);
        }

    }






    //이미지파일 만들기
    private File createImageFile(String extension) throws IOException{

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = userId +"_"+ username +"_"+ timeStamp + "_";

        //여러 앱이 공용으로 사용할 수 있는 저장공간. 그림파일이 저장되는 디렉토리 경로를 불러온다
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // = /mnt/sdcard/Pictures

        //이미지를 서버로 전송하기 전에, 임시로 캐쉬폴더에 저장한다
        //createTempFile : 캐쉬 디렉토리에 파일을 생성해주는 메소드
        File image = File.createTempFile(
                imageFileName, //접두어 prefix
                "."+extension, //접미어 suffix = 확장자
                storageDir //파일을 저장할 폴더 directory
        );

        return image; //이미지 파일을 return
    }



    //이미지파일 만들기
    private File createImageFile_thumbnail(String video_name) throws IOException{

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "thumb_"+video_name;

        //여러 앱이 공용으로 사용할 수 있는 저장공간. 그림파일이 저장되는 디렉토리 경로를 불러온다
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES); // = /mnt/sdcard/Pictures

        //이미지를 서버로 전송하기 전에, 임시로 캐쉬폴더에 저장한다
        //createTempFile : 캐쉬 디렉토리에 파일을 생성해주는 메소드
        File image = File.createTempFile(
                imageFileName, //접두어 prefix
                ".jpg", //접미어 suffix = 확장자
                storageDir //파일을 저장할 폴더 directory
        );

        return image; //이미지 파일을 return
    }






    //사용자가 a 채팅방에서 채팅을 하고 있는데, b 채팅방에서 메시지가 온다(푸쉬 알람으로 뜸) 이것을 클릭할 경우, b 채팅방으로 넘어가야 한다
    //= 채팅 화면이 켜 있는 상태로, 기존 채팅방 정보를 지우고 다른 채팅방 데이터를 띄워줘야 하는 상황
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d("intent", "@@@@@@@@@@@onNewIntent");

        //변수 초기화(메시지 목록, 참여자 목록)
        messageItemList.clear();
        adapter.notifyDataSetChanged();
        isNewRoom = false;
        memberList.clear();
        members_adapter.notifyDataSetChanged();

        roomId = intent.getIntExtra("roomId", 10000); // 이전 화면에서 전달한 방 id
        Log.d("intent", "roomid="+roomId);

        //바뀐 채팅방의 메시지 데이터를 띄워준다
        getSavedMsg(roomId);
        adapter.notifyDataSetChanged();

    }


    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("chat_event"));
        LocalBroadcastManager.getInstance(this).registerReceiver(videoMessageReceiver, new IntentFilter("video_upload"));
    }


    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(videoMessageReceiver);
    }



    private BroadcastReceiver videoMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int position = intent.getIntExtra("position", 10000);
            Log.d("이미지", "동영상이 업로드되지 않은 썸네일 이미지가 화면에 떴다는 알림. position="+position);

        }
    };



    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "Receiver) message received: " + message);

            message_array = message.split("/");
            String signal = message_array[0];

            switch (signal){
                case "room_created": //방이 최초로 만들어졌을 때, 방 id를 전달받는다
                    roomId = Integer.valueOf(message_array[1]);

                    break;
                case "roomInfo": //방 최초생성 시 or 기존 방에 들어왔을 때, 방 정보를 전달받는다

                    final String roomInfo_atTitleBar = message_array[1];
                    String memberInfo_string = message_array[2]; //참여자 정보(id;username;id;username..형식). 나중에 drawerLayout에 띄워줄 것


                    String[] memberInfo_array = memberInfo_string.split(";");


                    //피초대인들의 id와 username을 따로 모아서 arrayList에 저장한다
                    final ArrayList<Integer> memberId_list = new ArrayList<>();
                    final ArrayList<String > memberUsername_list = new ArrayList<>();

                    int member_id = 10000;
                    String member_username = "";

                    for(int i=0; i<memberInfo_array.length; i++){
                        if(i%2==0){
                            member_id = Integer.valueOf(memberInfo_array[i]);
                            memberId_list.add(member_id);
                        }else{
                                member_username = memberInfo_array[i];
                                memberUsername_list.add(member_username);
                        }
                    }


                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            for(int k=0; k<memberId_list.size(); k++){
                                String member_username = memberUsername_list.get(k);
                                int member_id = memberId_list.get(k);

                                memberList.add(0, new FriendListItem(member_username, member_id));
                            }
                            members_adapter.notifyDataSetChanged();


                            actionBar.setTitle(roomInfo_atTitleBar);
//                            roomInfo_textView.setText(roomInfo_atTitleBar);
                        }
                    });
                    break;

                case "roomInfo_plus": //채팅 참여자가 추가되었을 때

                    final String roomInfoPlus_atTitleBar = message_array[1];
                    String invited_memberInfo_string = message_array[2]; //참여자 정보(id;username;id;username..형식). 나중에 drawerLayout에 띄워줄 것

                    String[] invited_memberInfo_array = invited_memberInfo_string.split(";");


                    //피초대인들의 id와 username을 따로 모아서 arrayList에 저장한다
                    final ArrayList<Integer> invited_memberId_list = new ArrayList<>();
                    final ArrayList<String > invited_memberUsername_list = new ArrayList<>();

                    int invited_member_id = 10000;
                    String invited_member_username = "";

                    for(int i=0; i<invited_memberInfo_array.length; i++){
                        if(i%2==0){
                            invited_member_id = Integer.valueOf(invited_memberInfo_array[i]);
                            invited_memberId_list.add(invited_member_id);
                        }else{
                            invited_member_username = invited_memberInfo_array[i];
                            invited_memberUsername_list.add(invited_member_username);
                        }
                    }


                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            for(int k=0; k<invited_memberId_list.size(); k++){
                                String member_username = invited_memberUsername_list.get(k);
                                int member_id = invited_memberId_list.get(k);

                                memberList.add(new FriendListItem(member_username, member_id));
                            }
                            members_adapter.notifyDataSetChanged();

                            actionBar.setTitle(roomInfoPlus_atTitleBar);
                        }
                    });

                    break;

                case "msg":
//                    [텍스트] msg/roomId/sender_id/sender_username/message
//                    [이미지] msg/roomId/sender_id/sender_username/image!-!파일이름1;파일이름2;파일이름3
//                    [동영상] msg/roomId/sender_id/sender_username/video!-!썸네일이름!-!동영상이름

                    msg_roomId = Integer.valueOf(message_array[1]);
                    msg_sender_id = Integer.valueOf(message_array[2]);
                    msg_sender_username = message_array[3];
                    msg_text = message_array[4];


                    //데이터 타입을 구분한다. 텍스트 or 이미지 or 동영상
                    msg_filename_string = "";
                    dataType = "";

                    String thumbnail_filename = "";
                    String video_filename = "";
                    try{
                        String[] text_array = msg_text.split("!-!");

                        if(text_array[0].equals("image")){
                            msg_filename_string = text_array[1];
                            dataType = "image";

                        }else if(text_array[0].equals("video")){
                            thumbnail_filename = text_array[1];
                            video_filename = text_array[2];

                            msg_filename_string = thumbnail_filename;
                            video_path_server = Function.domain+"/images/"+roomId+"/"+video_filename;

                            dataType = "video";

                        }else{
                            dataType = "text";
                        }


                    }catch (Exception e){
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        String ex = sw.toString();

                        Log.d(TAG,ex);
                    }

                    if(msg_roomId == roomId){ //이 예외처리는 이미 서비스에서 했음. 재확인용

                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                long curTime = System.currentTimeMillis();

                                switch (dataType){
                                    case "text":

                                        messageItemList.add(new MessageItem(msg_sender_id, msg_sender_username, msg_text,
                                                msg_roomId, "N", curTime, 0, "", ""));
                                        break;

                                    case "image":

                                        messageItemList.add(new MessageItem(msg_sender_id, msg_sender_username, msg_text,
                                                msg_roomId, msg_filename_string, curTime, 0, "", ""));
                                        break;

                                    case "video":

                                        //@@받은 동영상의 경우, 아직 다운받지 않았으므로 로컬 video_path 가 없다
                                        //전달받은 동영상 경로를, 서버경로 변수에 할당한다
                                        MessageItem messageItem = new MessageItem(msg_sender_id, msg_sender_username, msg_text,
                                                msg_roomId, msg_filename_string, curTime, 0, "", video_path_server);

                                        messageItemList.add(messageItem);
                                        break;
                                }


                                adapter.notifyItemInserted(adapter.getItemCount()-1);
                                rcv.scrollToPosition(messageItemList.size()-1);
                            }
                        });
                    }

                    break;

            }


        }
    };




    void sendMsg(String msg){
        Intent intent = new Intent(getApplicationContext(), MainService.class);
        intent.putExtra("message", msg);

        startService(intent);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_chatroom_actions, menu);

        menuItem = menu.findItem(R.id.chatRoom_action_menu);

        return super.onCreateOptionsMenu(menu);
    }



    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.chatRoom_action_menu: //'메뉴보기' 옵션을 눌렀을 때 -> drawer layout 을 열고 닫음

                View drawerView = (View)findViewById(R.id.chatRoom_drawer_relativeLayout);
                drawer = (DrawerLayout)findViewById(R.id.chatRoom_drawerLayout);

                if(drawer.isDrawerOpen(GravityCompat.END)){ //닫기
                    drawer.closeDrawer(GravityCompat.END);
                }else{
                    drawer.openDrawer(drawerView); //열기
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }








}
