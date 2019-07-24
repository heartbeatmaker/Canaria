package com.android.canaria;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class UserProfileActivity extends AppCompatActivity {

    ImageView friend_profileImage_imageView;
    TextView friend_username_textView;
    Button startChat_btn;

    String user_id, username, friend_id, friend_username;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        friend_profileImage_imageView = (ImageView)findViewById(R.id.userProfile_friendImage_imageView);
        friend_username_textView = (TextView)findViewById(R.id.userProfile_friend_username);
        startChat_btn = (Button)findViewById(R.id.userProfile_startChat_btn);


        //이전화면에서 보낸 친구의 정보를 변수에 할당한다
        friend_id = getIntent().getStringExtra("friend_id");
        friend_username = getIntent().getStringExtra("friend_username");

        //현 사용자의 정보를 가져온다
        user_id = Function.getString(getApplicationContext(), "user_id");
        username = Function.getString(getApplicationContext(), "username");

        friend_username_textView.setText(friend_username);
    }
}
