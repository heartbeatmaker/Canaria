package com.android.canaria;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ChatRoomActivity extends AppCompatActivity {

    String user_id, username, friend_username, friend_userId;


    /*1대1 채팅이라고 가정*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        Context context = getApplicationContext();

        //친구의 id와 내 id를 가져온다
        friend_userId = getIntent().getStringExtra("friend_userId");
        friend_username = getIntent().getStringExtra("friend_username");

        user_id = Function.getString(context, "user_id");
        username = Function.getString(context, "username");


    }
}
