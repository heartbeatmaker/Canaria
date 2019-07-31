package com.android.canaria;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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

import com.android.canaria.connect_to_server.MainService;
import com.android.canaria.db.DBHelper;
import com.android.canaria.login.SignInActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    String TAG = "tag "+this.getClass().getSimpleName();


    MainService mainService;
    Intent serviceIntent;
    Context context;

    String user_id, username, email;

    ActionBar actionBar;

    private FragmentManager fragmentManager = getSupportFragmentManager();

    // bottom navigation view 에서 각각의 메뉴를 누르면 나타나는 프래그먼트
    private Main_Fragment1 fragment1 = new Main_Fragment1();
    private Main_Fragment2 fragment2 = new Main_Fragment2();
    private Main_Fragment3 fragment3 = new Main_Fragment3();

    public Fragment active_fragment = fragment1;
    public static int active_fragment_int = 1;

    MenuItem addFriend_menuItem, addRoom_menuItem;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


//        initSocketClient();
        context = getApplicationContext();

        mainService = new MainService(getApplicationContext());
        serviceIntent = new Intent(MainActivity.this, MainService.class);

        //지금 서비스가 실행되고 있지 않다면 -> 서비스를 실행한다
        if(!isMyServiceRunning(mainService.getClass())){
            startService(serviceIntent);
        }



        //상단 액션바 설정
        actionBar = getSupportActionBar();
        actionBar.show();
        actionBar.setTitle("Friends");

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
                                active_fragment_int = 1;

                                return true;

                            case R.id.menuitem_bottombar_chat:

                                actionBar.setTitle("Chat");
                                fragmentManager.beginTransaction().hide(active_fragment).show(fragment2).commit();
                                active_fragment = fragment2;
                                active_fragment_int = 2;

                                return true;

                            case R.id.menuitem_bottombar_more:

                                actionBar.setTitle("More");
                                fragmentManager.beginTransaction().hide(active_fragment).show(fragment3).commit();
                                active_fragment = fragment3;
                                active_fragment_int = 3;

                                return true;
                        }
                        return false;
                    }
                });


    }



    private boolean isMyServiceRunning(Class<?> serviceClass) {

        ActivityManager manager = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE)){
            if(serviceClass.getName().equals(service.service.getClassName())){
                Log.d(TAG, "isMyServiceRunning? "+true);
                return true;
            }
        }
        Log.d(TAG, "isMyServiceRunning? "+false);
        return false;
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            stopService(serviceIntent);
        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d(TAG,ex);
        }
//        new Disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_actions, menu);

        addFriend_menuItem = menu.findItem(R.id.action_add_friends);
        addRoom_menuItem = menu.findItem(R.id.action_add_room);

        return super.onCreateOptionsMenu(menu);
    }



    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.logout: //로그아웃 옵션을 눌렀을 때

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
            case R.id.action_add_room: //방만들기 옵션을 눌렀을 때

                Intent intent_select_friends = new Intent(getApplicationContext(), SelectFriendsActivity.class);
                startActivity(intent_select_friends);

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



//    void sendMsg(String msg) throws Exception {
//
//        try{
//            Log.d(TAG, "sendMsg() to server. message:"+msg);
//            bufferedWriter.write(msg + "\n");
//            bufferedWriter.flush();
//        } catch(Exception e){
//            Log.d(TAG, "sendMsg() error: "+e);
//        }
//    }


//    class Disconnect extends Thread{
//        public void run(){
//
//            try{
//                if(socket != null){
//                    socket.close();
//                    Log.d(TAG, "socket closed");
//                }
//
//            }catch (Exception e){
//                Log.d(TAG, "disconnect error: "+e);
//            }
//
//        }
//    }



}
