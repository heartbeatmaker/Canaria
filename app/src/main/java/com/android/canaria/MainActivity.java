package com.android.canaria;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
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

    Context context;
    String user_id, username, email;

    ActionBar actionBar;


    private FragmentManager fragmentManager = getSupportFragmentManager();

    // bottom navigation view 에서 각각의 메뉴를 누르면 나타나는 프래그먼트
    private Main_Fragment1 fragment1 = new Main_Fragment1();
    private Main_Fragment2 fragment2 = new Main_Fragment2();
    private Main_Fragment3 fragment3 = new Main_Fragment3();


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_actions, menu);

        return super.onCreateOptionsMenu(menu);

    }

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
                Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                startActivity(intent);

                break;

            case R.id.action_add_friends: //친구찾기 옵션을 눌렀을 때

                Intent intent_add_friends = new Intent(getApplicationContext(), FriendFinderActivity.class);
                startActivity(intent_add_friends);

                break;


        }


        return super.onOptionsItemSelected(item);
    }

    private void openFragment(final Fragment fragment){
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.main_frameLayout, fragment);
        transaction.addToBackStack(null);
        transaction.commit();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        openFragment(fragment1);

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
                                openFragment(fragment1);

                                return true;

                            case R.id.menuitem_bottombar_chat:

                                actionBar.setTitle("Chat");
                                openFragment(fragment2);

                                return true;

                            case R.id.menuitem_bottombar_more:

                                actionBar.setTitle("More");
                                openFragment(fragment3);

                                return true;
                        }
                        return false;
                    }
                });




//        MainActivity.SendPost sendPost = new MainActivity.SendPost();
//        sendPost.execute();

    }





    class SendPost extends AsyncTask<String, Void, String> {



        ProgressDialog dialog = new ProgressDialog(MainActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("tag","onPreExecute");

            dialog.setMessage("Processing..");
            dialog.show();
        }

        @Override
        protected String doInBackground(String... strings) {


            String response_line = "";

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.180.107.44/register.php");



            //POST 방식에서 사용된다
            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

            try {
                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
                nameValues.add(new BasicNameValuePair(
                        "main", URLDecoder.decode("1", "UTF-8")));
//                nameValues.add(new BasicNameValuePair(
//                        "email", URLDecoder.decode(strings[0], "UTF-8")));
//                nameValues.add(new BasicNameValuePair(
//                        "password", URLDecoder.decode(strings[1], "UTF-8")));

                //HttpPost에 넘길 값을들 Set해주기
                post.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));

            } catch (UnsupportedEncodingException ex) {
                Log.d("tag", ex.toString());
            }

            try {
                //설정한 URL을 실행시키기 -> 응답을 받음
                HttpResponse response = httpClient.execute(post);
                //통신 값을 받은 Log 생성. (200이 나오는지 확인할 것~) 200이 나오면 통신이 잘 되었다는 뜻!
                Log.i("tag"+getClass().getName(), "response.getStatusCode:" + response.getStatusLine().getStatusCode());



//                HttpEntity entity = response.getEntity();
//
//                if(entity !=null){
//                    Log.d("tag", "signin) Response length:"+entity.getContentLength());
//
//                    // 콘텐츠를 읽어들임.
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
//
//
//                    while ((response_line = reader.readLine()) != null) {
//                        // 콘텐츠 내용
//                        Log.d("tag", "signin) response: "+response_line);
//                        return response_line;
//                    }
//                }
//
//                //Ensures that the entity content is fully consumed and the content stream, if exists, is closed.
//                EntityUtils.consume(entity);

                post.releaseConnection();


            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            Log.d("tag","signin) onPostExecute. param="+s);
            dialog.dismiss();



        }
    }
}
