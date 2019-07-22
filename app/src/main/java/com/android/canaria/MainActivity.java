package com.android.canaria;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.util.Output;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.client.BasicCookieStore;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.conn.DefaultHttpClientConnectionOperator;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class MainActivity extends AppCompatActivity {

    String TAG = "tag "+this.getClass().getSimpleName();

    Context context;
    String user_id, username, email;

    ActionBar actionBar;

    private FragmentManager fragmentManager = getSupportFragmentManager();

    // bottom navigation view 에서 각각의 메뉴를 누르면 나타나는 프래그먼트
    private Main_Fragment1 fragment1 = new Main_Fragment1();
    private Main_Fragment2 fragment2 = new Main_Fragment2();
    private Main_Fragment3 fragment3 = new Main_Fragment3();

    Fragment active_fragment = fragment1;


    //클라이언트 소켓 관련 변수
    public static final String ServerIP = "54.180.107.44";
    Socket socket;
    BufferedWriter bufferedWriter;
    BufferedReader bufferedReader;



    public void initSocketClient() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("54.180.107.44", 8000);
                    Log.d(TAG, "connected to chat server");

                    InputStream is = socket.getInputStream();
                    OutputStream os = socket.getOutputStream();
                    InputStreamReader isr = new InputStreamReader(is);
                    bufferedReader = new BufferedReader(isr); //서버로부터 메시지를 읽어들이는 객체

                    OutputStreamWriter osw = new OutputStreamWriter(os);
                    bufferedWriter = new BufferedWriter(osw); //서버에 메시지를 쓰는 객체

                    //입장알림 - 서버에 보내는 신호/ 사용자 이름
                    sendMsg("enter/" + user_id);

                } catch (Exception e) {
                    Log.d(TAG, "socket connection error: "+e);
                }

                try {
                    Log.d(TAG, "trying to read message");
                    //클라이언트의 메인 쓰레드는 서버로부터 데이터 읽어들이는 것만 반복
                    while(true) {

                        //서버에서 데이터를 보낼 때, 데이터를 '/'을 기준으로 한 문장으로 구성해서 보냄
                        //맨 앞 문자열: 클라이언트에게 보내는 신호(어떤 행동을 해라)
                        //그다음부터는 화면에 띄워줄 데이터
                        String line = bufferedReader.readLine();
                        Log.d(TAG, "readMsg(). message: "+line); //동작하지 않음

                    }
                }catch(IOException e) {
                    Log.d(TAG, "socket reader error: "+e);
                }

            }
        }).start();
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initSocketClient();

        //상단 액션바 설정
        actionBar = getSupportActionBar();
        actionBar.show();
        actionBar.setTitle("Friends");

        context = getApplicationContext();

        //이 회원의 기본 정보를 불러온다
        user_id = Function.getString(context, "user_id");
        username = Function.getString(context, "username");
        email = Function.getString(context, "email");

        Log.d("tag", "main) user_id="+user_id+"/username="+username+"/email="+email);

        //첫 화면 지정
//        openFragment(fragment1);

        fragmentManager.beginTransaction().add(R.id.main_frameLayout, fragment3, "3").hide(fragment3).commit();
        fragmentManager.beginTransaction().add(R.id.main_frameLayout, fragment2, "2").hide(fragment2).commit();
        fragmentManager.beginTransaction().add(R.id.main_frameLayout,fragment1, "1").commit();


        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.main_bottom_navigation);

        //하단 네비게이션 뷰 클릭 리스너
        bottomNavigationView.setOnNavigationItemSelectedListener(
                new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {


                        // 어떤 메뉴 아이템이 터치되었는지 확인합니다.
                        switch (item.getItemId()) {

                            case R.id.menuitem_bottombar_friends:

                                actionBar.setTitle("Friends");
                                fragmentManager.beginTransaction().hide(active_fragment).show(fragment1).commit();
                                active_fragment = fragment1;

                                return true;

                            case R.id.menuitem_bottombar_chat:

                                actionBar.setTitle("Chat");
                                fragmentManager.beginTransaction().hide(active_fragment).show(fragment2).commit();
                                active_fragment = fragment2;

                                return true;

                            case R.id.menuitem_bottombar_more:

                                actionBar.setTitle("More");
                                fragmentManager.beginTransaction().hide(active_fragment).show(fragment3).commit();
                                active_fragment = fragment3;

                                return true;
                        }
                        return false;
                    }
                });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_actions, menu);

        return super.onCreateOptionsMenu(menu);

    }

    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.settings_logout: //로그아웃 옵션을 눌렀을 때

                //저장된 사용자 정보를 지운다
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.remove("user_id");
                editor.remove("username");
                editor.remove("email");
                editor.commit();


                //로그인 화면으로 전환
                finishAffinity(); //모든 액티비티를 종료한다
                startActivity(new Intent(getApplicationContext(), SignInActivity.class));

                break;

            case R.id.action_add_friends: //친구찾기 옵션을 눌렀을 때

                Intent intent_add_friends = new Intent(getApplicationContext(), FriendFinderActivity.class);
                startActivity(intent_add_friends);

                break;

        }

        return super.onOptionsItemSelected(item);
    }



    //사용하지 않음
    private void openFragment(final Fragment fragment){
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_frameLayout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


    /////////////////////////////////////////////////////////////
    //클라이언트 소켓 통신하는 부분



    void sendMsg(String msg) throws Exception {

        try{
            Log.d(TAG, "sendMsg() to server. message:"+msg);
            bufferedWriter.write(msg + "\n");
            bufferedWriter.flush();
        } catch(Exception e){
            Log.d(TAG, "sendMsg() error: "+e);
        }
    }




}
