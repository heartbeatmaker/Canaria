package com.android.canaria.recyclerView;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import com.android.canaria.Function;
import com.android.canaria.MainActivity;
import com.android.canaria.connect_to_server.HttpRequest;

public class MessageItem {


    private String userImage_url;
    private int sender_id;
    private String senderUsername;
    private String message;
    private int room_id;
    private String image_url;
    private long timeMillis;

    private String image_name;

    private String video_path; //로컬 경로 : 자기가 보낸 동영상의 압축파일이 저장되는 곳 + 남한테 받은 동영상을 다운받았을 때 파일이 저장되는 곳
    private String video_path_server; //서버 경로 : 서버가 동영상을 저장한 곳

    private int db_id; //db의 chat_logs 테이블에서 이 메시지가 지닌 id. SentVideo 일 때만 적용된다. 받은 메시지 아이템에는 db_id 값이 항상 0이다

    public MessageItem(int sender_id, String senderUsername, String message, int room_id, String image_name, long timeMillis,
                       int db_id, String video_path, String video_path_server) {
        this.room_id = room_id;
        this.sender_id = sender_id;
        this.senderUsername = senderUsername;
        this.message = message;
        this.timeMillis = timeMillis;
        this.image_name = image_name;
        this.video_path = video_path;
        this.db_id = db_id;
        this.video_path_server = video_path_server;
    }



    public String getVideo_server_path() {
        return video_path_server;
    }

    public int getDb_id() {
        return db_id;
    }

    public void setDb_id(int db_id) {
        this.db_id = db_id;
    }

    public String getVideo_path() {
        return video_path;
    }

    public void setVideo_file_path(String video_file_path) {
        this.video_path = video_file_path;
    }


    public String getImage_name() {
        return image_name;
    }

    public String getThumbImage_url() {

        image_url = Function.domain+"/images/"+room_id+"_thumb/"+image_name;

        return image_url;
    }


    public boolean isVideoThumbnail(){
        String[] image_name_split = image_name.split("_");
        if(image_name_split[1].equals("thumb")){
            return true;
        }else{
            return false;
        }

    }

    public String getUserImage_url() {

        ContentValues data = new ContentValues();
        data.put("sender_id", sender_id);

        try {
            //sender_id를 이용하여 서버에서 해당 사용자의 프로필 사진 이름을 가져온다
            userImage_url = new HttpRequest("image.php", data).execute().get();
        } catch (Exception e) {
            Log.d("tag", "Error: "+e);
        }

        Log.d("tag", "result="+userImage_url);

        return userImage_url;
    }


    public int getRoom_id(){
        return this.room_id;
    }

    public void setUserImage_url(String userImage_url) {
        this.userImage_url = userImage_url;
    }

    public int getSender_id() {
        return sender_id;
    }

    public void setSender_id(int sender_id) {
        this.sender_id = sender_id;
    }

    public String getSenderUsername() {
        return senderUsername;
    }

    public void setSenderUsername(String senderUsername) {
        this.senderUsername = senderUsername;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimeMillis() {
        return timeMillis;
    }

    public void setTimeMillis(long timeMillis) {
        this.timeMillis = timeMillis;
    }




}
