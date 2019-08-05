package com.android.canaria;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

public class UserProfileActivity extends AppCompatActivity {

    ImageView imageView;
    TextView friend_username_textView;
    Button startChat_btn;

    String user_id, username, friend_username;
    int friend_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        imageView = (ImageView)findViewById(R.id.profile_image_imageView);
        friend_username_textView = (TextView)findViewById(R.id.userProfile_friend_username);
        startChat_btn = (Button)findViewById(R.id.userProfile_startChat_btn);


        //이전화면에서 보낸 친구의 정보를 변수에 할당한다
        friend_id = getIntent().getIntExtra("friend_id", 10000);
        friend_username = getIntent().getStringExtra("friend_username");

        //현 사용자의 정보를 가져온다
        user_id = Function.getString(getApplicationContext(), "user_id");
        username = Function.getString(getApplicationContext(), "username");

        friend_username_textView.setText(friend_username);

        final ProgressBar progressBar = (ProgressBar)findViewById(R.id.profile_progressBar);
        progressBar.setIndeterminate(true);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY); //색 변경

        String profileImage_name = Function.getUserImage_url(friend_id);

        if(profileImage_name.equals("null")){

            imageView.setBackgroundResource(R.drawable.user2);
            progressBar.setVisibility(View.INVISIBLE);

        }else{

            String profileImage_path = "http://15.164.193.65/uploads/"+profileImage_name;

            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.ic_person_black_24dp);

            Glide.with(this).asBitmap().load(profileImage_path)
                    .apply(options)
                    .listener(new RequestListener<Bitmap>() {
                        @Override
                        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(imageView);
        }

    }
}
