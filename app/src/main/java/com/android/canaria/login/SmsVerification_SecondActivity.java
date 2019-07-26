package com.android.canaria.login;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.MainActivity;
import com.android.canaria.R;
import com.google.android.gms.auth.api.credentials.CredentialsClient;

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

public class SmsVerification_SecondActivity extends AppCompatActivity {

    EditText code_editText;
    TextView verify_btn, warning_textView, back_btn;

    String email, code_sent, code_received, phone_number;

    SmsReceiver smsReceiver = new SmsReceiver();


    private String TAG = "Sms_SecondActivity.class";

    /*credential 변수*/
    private int RC_SAVE = 2000;
    CredentialsClient mCredentialsClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_verification2);

        //broadcastReceiver 실행
        smsReceiver.registerReceiver(getApplicationContext());


        code_editText = (EditText)findViewById(R.id.sms_verify_editText);
        verify_btn = (TextView)findViewById(R.id.sms_verify_btn);
        warning_textView = (TextView)findViewById(R.id.sms_warning_textView);
        back_btn = (TextView)findViewById(R.id.sms_back_btn);

        Intent intent = getIntent();
        if(intent.hasExtra("code_sent")){
            code_sent = intent.getStringExtra("code_sent");
            email = intent.getStringExtra("email");
            phone_number = intent.getStringExtra("phone_number");

            Log.d("tag","SecondActivity) code_sent="+code_sent+" / email="+email);
        }



        //보낸것과 받은 것이 일치하는지 확인해야함
        //일치하면 -> 메인화면으로 넘어감 + 이 사용자를 activate 시킨다(db에 기록)


        verify_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String input = code_editText.getText().toString();

                if(input.equals(code_sent)){ //코드가 일치하면 -> 메인화면으로 이동한다
                    Toast.makeText(SmsVerification_SecondActivity.this, "Code matches.", Toast.LENGTH_SHORT).show();

                    try{

                        //서버로 사용자 이메일과 폰번호를 전송한다
                        String response_fromServer;
                        SmsVerification_SecondActivity.SendPost sendPost = new SmsVerification_SecondActivity.SendPost();
                        sendPost.execute(email, phone_number);


                    }catch (Exception e){
                        Log.d("tag", e.toString());
                    }

                }else{
                    //코드가 일치하지 않을 경우 -> 이전 화면으로 돌아가는 버튼을 보여줌
                    warning_textView.setVisibility(View.VISIBLE);
                    warning_textView.setText("Code doesn't match. Please try again.");

                    back_btn.setVisibility(View.VISIBLE);
                }
            }
        });


        //이전 화면으로 돌아가는 버튼
        back_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent goBack = new Intent(getApplicationContext(), SmsVerificationActivity.class);
                goBack.putExtra("email", email);
                startActivity(goBack);
                finish();
            }
        });

    }


    //이 액티비티가 onCreate() 된 이후에 sms 코드를 전송받는다
    //따라서 smsReceiver가 보낸 intent는 onCreate()에서 받을 수 없다
    //onNewIntent()는 onResume() 직전에 실행된다
    //onNewIntent() -> onResume()
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if(intent.hasExtra("code_received")){
            code_received = intent.getStringExtra("code_received");
            Log.d("tag","SecondActivity) onNewIntent: code_received="+code_received);
            code_editText.setText(code_received);

        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        Log.d("tag", "SecondActivity) onResume()");

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        //broadcastReceiver 해제
        smsReceiver.unregisterReceiver(getApplicationContext());
    }






    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);


        if (requestCode == RC_SAVE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "SAVE: OK");
                Toast.makeText(this, "Credentials saved", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
            }
        }

    }



    class SendPost extends AsyncTask<String, Void, String> {


        ProgressDialog dialog = new ProgressDialog(SmsVerification_SecondActivity.this);

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

            //-> 서버로 사용자의 이메일과 폰번호를 보낸다
            Log.d("tag","sms2: doInBackground. param="+strings[0]);

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.180.107.44/activate.php");

            //POST 방식에서 사용된다
            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

            try {
                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
                nameValues.add(new BasicNameValuePair(
                        "activate", URLDecoder.decode("1", "UTF-8")));
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
                Log.i("tag", "sms2: response.getStatusCode:" + response.getStatusLine().getStatusCode());

                HttpEntity entity = response.getEntity();

                if(entity !=null){
                    Log.d("tag", "sms2: Response length:"+entity.getContentLength());

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
            dialog.dismiss();

            Log.d("tag", "onPostExecute result: "+s);

            if(s.equals("success")){//결과가 '성공'이면

//                //smart lock에 해당 아이디를 저장한다
//
//                //Credential 저장
//                Credential credential = new Credential.Builder(email)
//                        .setPassword("abc").setAccountType("CANARIA")
//                        // Important: only store passwords in this field.
//                        // Android autofill uses this value to complete
//                        // sign-in forms, so repurposing this field will
//                        // likely cause errors.
//                        .build();
//
//
//                mCredentialsClient = Credentials.getClient(SmsVerification_SecondActivity.this);
//
//                mCredentialsClient.save(credential).addOnCompleteListener(
//                        new OnCompleteListener() {
//                            @Override
//                            public void onComplete(@NonNull Task task) {
//                                if (task.isSuccessful()) {
//                                    Log.d(TAG, "SAVE: OK");
//                                    Toast.makeText(SmsVerification_SecondActivity.this, "Credentials saved", Toast.LENGTH_SHORT).show();
//                                    return;
//                                }
//
//                                Exception e = task.getException();
//                                if (e instanceof ResolvableApiException) {
//                                    // Try to resolve the save request. This will prompt the user if
//                                    // the credential is new.
//                                    ResolvableApiException rae = (ResolvableApiException) e;
//                                    try {
//                                        rae.startResolutionForResult(SmsVerification_SecondActivity.this, RC_SAVE);
//                                    } catch (IntentSender.SendIntentException ex) {
//                                        // Could not resolve the request
//                                        Log.e(TAG, "Failed to send resolution.", ex);
//                                        Toast.makeText(SmsVerification_SecondActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
//                                    }
//                                } else {
//                                    // Request has no resolution
//                                    Toast.makeText(SmsVerification_SecondActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
//                                }
//                            }
//                        });





                //메인 화면으로 넘어간다
                Intent toMainActivity = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(toMainActivity);
                finish();

            }else{//이외의 결과면

                //다시 시도하라고 메시지를 띄워준다
                warning_textView.setVisibility(View.VISIBLE);
                warning_textView.setText("Error: Please try again.");
            }


        }
    }
}
