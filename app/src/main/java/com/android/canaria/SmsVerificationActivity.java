package com.android.canaria;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class SmsVerificationActivity extends AppCompatActivity {

    EditText phoneNumber_editText;
    TextView countryCode_textView, requestCode_btn;

    String zipCode, email;



    public String getCountryZipCode(){
        String CountryID="";
        String CountryZipCode="";

        TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso
        CountryID= manager.getSimCountryIso().toUpperCase();
        String[] rl=this.getResources().getStringArray(R.array.CountryCodes);
        for(int i=0;i<rl.length;i++){
            String[] g=rl[i].split(",");
            if(g[1].trim().equals(CountryID.trim())){
                CountryZipCode=g[0];
                break;
            }
        }
        return CountryZipCode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_verification);

        phoneNumber_editText = (EditText)findViewById(R.id.sms_phoneNumber_editText);
        requestCode_btn = (TextView)findViewById(R.id.sms_send_btn);
        countryCode_textView = (TextView)findViewById(R.id.sms_countryCode_textView);

        zipCode = getCountryZipCode();
        countryCode_textView.setText("+"+zipCode);

        Intent intent = getIntent();

        if(intent.hasExtra("email")){
            email = intent.getStringExtra("email");
        }


        requestCode_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String phone_number = zipCode + phoneNumber_editText.getText().toString();
                Log.d("tag","phone_number="+phone_number);

                //올바른 번호를 입력했는지 확인해야 한다 - 입력했다고 가정

                try{

                    //서버로 사용자 이메일과 번호를 전송한다
                    String response_fromServer;
                    SmsVerificationActivity.SendPost sendPost = new SmsVerificationActivity.SendPost();
                    response_fromServer = sendPost.execute(email, phone_number).get();

                    //get() : retrieve your result once the work on the thread is done.
                    //get() 메소드는 AsyncTask가 실행되는 동안 UI 쓰레드를 block 시킨다
                    Log.d("tag","sms: response_fromServer="+response_fromServer);


                    String[] resArray = response_fromServer.split(";");
                    String result = resArray[0];
                    String code_sent = resArray[1];

                    if(result.equals("success")){//결과가 '성공'이면

                        //코드 입력 화면으로 넘어간다(이메일, 코드를 인텐트로 보낸다)
                        Toast.makeText(SmsVerificationActivity.this, "Message has been sent", Toast.LENGTH_SHORT).show();
                        Intent i = new Intent(getApplicationContext(), SmsVerification_SecondActivity.class);
                        i.putExtra("email", email);
                        i.putExtra("code_sent", code_sent);
                        startActivity(i);

                    }else{//이외의 결과이면

                        //다시 시도하라고 메시지를 띄워준다
                        Toast.makeText(SmsVerificationActivity.this, "Please try again", Toast.LENGTH_SHORT).show();
                    }

                }catch (Exception e){

                }


            }
        });

    }



    class SendPost extends AsyncTask<String, Void, String> {


        @Override
        protected String doInBackground(String... strings) {

            String response_line = "";

            //-> 서버로 사용자의 이메일과 폰번호를 보낸다
            Log.d("tag","sms: doInBackground. param1="+strings[0]+"/param2="+strings[1]);

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.180.107.44/sms.php");

            //POST 방식에서 사용된다
            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

            try {
                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
                nameValues.add(new BasicNameValuePair(
                        "email", URLDecoder.decode(strings[0], "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "phone_number", URLDecoder.decode(strings[1], "UTF-8")));

                //HttpPost에 넘길 값을들 Set해주기
                post.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));

            } catch (UnsupportedEncodingException ex) {
                Log.d("tag", ex.toString());
            }

            try {
                //설정한 URL을 실행시키기 -> 응답을 받음
                HttpResponse response = client.execute(post);
                //통신 값을 받은 Log 생성. (200이 나오는지 확인할 것~) 200이 나오면 통신이 잘 되었다는 뜻!
                Log.i("tag", "sms: response.getStatusCode:" + response.getStatusLine().getStatusCode());

                HttpEntity entity = response.getEntity();

                if(entity !=null){
                    Log.d("tag", "sms: Response length:"+entity.getContentLength());

                    // 콘텐츠를 읽어들임.
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));


                    while ((response_line = reader.readLine()) != null) {
                        // 콘텐츠 내용
                        Log.d("tag", "sms: response: "+response_line);
                        return response_line;
                    }
                }

                //Ensures that the entity content is fully consumed and the content stream, if exists, is closed.
                EntityUtils.consume(entity);

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

            Log.d("tag","onPostExecute. param="+s);

        }
    }
}
