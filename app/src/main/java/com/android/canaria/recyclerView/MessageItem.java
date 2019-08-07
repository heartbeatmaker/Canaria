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

    public MessageItem(int sender_id, String senderUsername, String message, int room_id, String image_name, long timeMillis) {
        this.room_id = room_id;
        this.sender_id = sender_id;
        this.senderUsername = senderUsername;
        this.message = message;
        this.timeMillis = timeMillis;
        this.image_name = image_name;
    }



    public String getImage_name() {
        return image_name;
    }

    public String getImage_url() {

        image_url = Function.domain+"/images/"+room_id+"/"+image_name;

        return image_url;
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
