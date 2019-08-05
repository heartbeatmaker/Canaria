package com.android.canaria.recyclerView;

import android.content.ContentValues;
import android.util.Log;

import com.android.canaria.connect_to_server.HttpRequest;

public class FriendListItem {


    private String userImage_url;
    private String friendName;
    private int friendId;


    public FriendListItem(String friendName, int friendId) {
        this.friendName = friendName;
        this.friendId = friendId;
    }


    public String getUserImage_url() {

        ContentValues data = new ContentValues();
        data.put("sender_id", friendId);

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

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public int getFriendId() {
        return friendId;
    }

    public void setFriendId(int friendId) {
        this.friendId = friendId;
    }


}
