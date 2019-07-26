package com.android.canaria.login;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.MainActivity;
import com.android.canaria.R;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.CredentialRequest;
import com.google.android.gms.auth.api.credentials.CredentialRequestResponse;
import com.google.android.gms.auth.api.credentials.Credentials;
import com.google.android.gms.auth.api.credentials.CredentialsClient;
import com.google.android.gms.auth.api.credentials.IdentityProviders;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

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

public class SignInActivity extends AppCompatActivity {

    Button signin_with_email_btn;

    LinearLayout signin_with_email_linearLayout;

    TextView signin_btn, signup_btn, email_warning_textView, password_warning_textView;

    EditText email_editText, password_editText;

    String email_input, password_input;

    private String TAG = "signin.class";

    /*credential 변수*/
    private int RC_SAVE = 2000;
    private int RC_READ = 1000;
    CredentialsClient mCredentialsClient;
    CredentialRequest mCredentialRequest;



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);


        signin_btn = (TextView)findViewById(R.id.signin_cofirm_btn);
        signup_btn = (TextView)findViewById(R.id.signin_register_btn);

        signin_with_email_btn = (Button)findViewById(R.id.signin_with_email_btn);
        signin_with_email_linearLayout = (LinearLayout)findViewById(R.id.signin_with_email_linearLayout);

        email_editText = (EditText)findViewById(R.id.signin_email_editText);
        password_editText = (EditText)findViewById(R.id.signin_password_editText);

        email_warning_textView = (TextView)findViewById(R.id.signin_email_warning_textView);
        password_warning_textView = (TextView)findViewById(R.id.signin_password_warning_textView);


        //이메일로 로그인하기 버튼을 누르면 -> 로그인 양식이 나타난다 + smart lock에 저장된 id 목록이 나타난다
        signin_with_email_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                signin_with_email_linearLayout.setVisibility(View.VISIBLE);

                getCredentials();
            }
        });



        //회원가입 버튼을 누르면 -> 회원가입 화면으로 전환
        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(getApplicationContext(), SignUpActivity.class);
                startActivity(intent);
            }
        });




        //확인 버튼을 누르면
        signin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("tag", "signin_btn is clicked");

                //사용자가 입력한 값을 확인한다 -> 문제가 있는 칸 밑에 warning 메시지를 띄워준다
                boolean isContentValid = false;

                //사용자가 입력한 텍스트를 가져온다
                email_input = email_editText.getText().toString();
                password_input = password_editText.getText().toString();



                //이메일: 채워졌는지, 올바른 양식인지 확인(정규표현식 사용)
                if(email_input.length()==0){
                    email_warning_textView.setVisibility(View.VISIBLE);
                    email_warning_textView.setText("Enter your email");
                    isContentValid=false;

                }else{//사용자가 이메일을 입력했다면

                    //올바른 양식인지 확인
                    if(Patterns.EMAIL_ADDRESS.matcher(email_input).matches()){
                        isContentValid = true;
                    }else{
                        email_warning_textView.setVisibility(View.VISIBLE);
                        email_warning_textView.setText("Enter a valid email address");
                        isContentValid = false;
                    }

                }

                //첫번째 패스워드: 입력값이 있는지 확인(정규표현식 확인x)
                if(password_input.length()==0){
                    password_warning_textView.setVisibility(View.VISIBLE);
                    password_warning_textView.setText("Enter your password");
                    isContentValid=false;

                }else{ //패스워드를 입력했다면

                    isContentValid = true;
                }


                if(isContentValid){
                    //서버로 입력값을 보낸다

                    try{

                        SignInActivity.SendPost sendPost = new SignInActivity.SendPost();
                        sendPost.execute(email_input, password_input);

                    }catch (Exception e){
                        Log.d("tag", this.getClass().getName()+" Error: "+e);
                    }

                }

            }
        });



        email_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                email_warning_textView.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        password_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                password_warning_textView.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }




    //구글 smart lock에서 사용자 이메일 불러오기 ------------------------------------
    private void getCredentials(){
        Log.d("tag", "getCredentials");

        mCredentialsClient = Credentials.getClient(this);

        mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes(IdentityProviders.GOOGLE, IdentityProviders.TWITTER)
                .build();


        mCredentialsClient.request(mCredentialRequest).addOnCompleteListener(
                new OnCompleteListener<CredentialRequestResponse>() {
                    @Override
                    public void onComplete(@NonNull Task<CredentialRequestResponse> task) {

                        if (task.isSuccessful()) { //저장된 계정이 한 개일 때만 이 코드 사용
                            Log.d("tag", "credential is retrieved");
                            // See "Handle successful credential requests"
//                            onCredentialRetrieved(task.getResult().getCredential());
                            return;
                        }

                        //저장된 계정이 복수일 때는 위의 코드로 결과를 가져올 수 없다(사용자의 선택이 필요)
                        //아래 코드를 사용한다
                        Exception e = task.getException();
                        if (e instanceof ResolvableApiException) {
                            // This is most likely the case where the user has multiple saved
                            // credentials and needs to pick one. This requires showing UI to
                            // resolve the read request.
                            ResolvableApiException rae = (ResolvableApiException) e;
                            resolveResult(rae, RC_READ);
                        } else if (e instanceof ApiException) {
                            // The user must create an account or sign in manually.
                            Log.e(TAG, "Unsuccessful credential request.", e);

                            ApiException ae = (ApiException) e;
                            int code = ae.getStatusCode();
                            // ...
                        }

                        // See "Handle unsuccessful and incomplete credential requests"
                        // ...
                    }
                });

    }


    private void resolveResult(ResolvableApiException rae, int requestCode) {
        try {
            rae.startResolutionForResult(this, requestCode);
//            mIsResolving = true;
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Failed to send resolution.", e);
//            hideProgress();
        }
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_READ) {
            if (resultCode == RESULT_OK) {

                com.google.android.gms.auth.api.credentials.Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
                onCredentialRetrieved(credential);
            } else {
                Log.e("tag", "Credential Read: NOT OK");
//                Toast.makeText(this, "Credential Read Failed", Toast.LENGTH_SHORT).show();
            }
        }

        if (requestCode == RC_SAVE) {
            if (resultCode == RESULT_OK) {
                Log.d(TAG, "SAVE: OK");
                Toast.makeText(this, "Credentials saved", Toast.LENGTH_SHORT).show();
            } else {
                Log.e(TAG, "SAVE: Canceled by user");
            }
        }

    }


    //사용자의 구글 id와 이름을 가져오는 메소드
    private void onCredentialRetrieved(com.google.android.gms.auth.api.credentials.Credential credential) {
        Log.d("tag","Credential is selected");

        String accountType = credential.getAccountType();
//        Log.d("tag","accountType="+accountType);
        Log.d("tag","id="+credential.getId()+" / name="+credential.getName());
        String email_saved = credential.getId();
        String password_saved = credential.getPassword();

        email_editText.setText(email_saved);
        password_editText.setText(password_saved);

    }

    //구글 smart lock에서 사용자 이메일 불러오기 끝 ------------------------------------







    class SendPost extends AsyncTask<String, Void, String> {



        ProgressDialog dialog = new ProgressDialog(SignInActivity.this);

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

            //-> 서버로 세 개의 사용자 정보를 보낸다(닉넴, 이메일, 패스워드)
            Log.d("tag","signin) doInBackground. param1="+strings[0]+"/param2="+strings[1]);

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.180.107.44/register.php");



            //POST 방식에서 사용된다
            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

            try {
                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
                nameValues.add(new BasicNameValuePair(
                        "sign_in", URLDecoder.decode("1", "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "email", URLDecoder.decode(strings[0], "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "password", URLDecoder.decode(strings[1], "UTF-8")));

                //HttpPost에 넘길 값을들 Set해주기
                post.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));

            } catch (UnsupportedEncodingException ex) {
                Log.d("tag", ex.toString());
            }

            try {
                //설정한 URL을 실행시키기 -> 응답을 받음
                HttpResponse response = client.execute(post);
                //통신 값을 받은 Log 생성. (200이 나오는지 확인할 것~) 200이 나오면 통신이 잘 되었다는 뜻!
                Log.i("tag", "signin) response.getStatusCode:" + response.getStatusLine().getStatusCode());



                HttpEntity entity = response.getEntity();

                if(entity !=null){
                    Log.d("tag", "signin) Response length:"+entity.getContentLength());

                    // 콘텐츠를 읽어들임.
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));


                    while ((response_line = reader.readLine()) != null) {
                        // 콘텐츠 내용
                        Log.d("tag", "signin) response: "+response_line);
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

            Log.d("tag","signin) onPostExecute. param="+s);
            dialog.dismiss();


            String result = "";
            String user_id = "";
            String username = "";
            try{

                JSONObject result_object = new JSONObject(s);

                result = result_object.getString("result");
                user_id = result_object.getString("id");
                username = result_object.getString("username");

                Log.d("tag","signin) result="+result+"/user_id="+user_id+"/username="+username);

            }
            catch (JSONException e){

                Log.d("tag","error: "+e);
            }



            if(result.equals("success")){//결과가 '성공'이면

                //사용자의 정보를 저장한다
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("user_id", user_id);
                editor.putString("username", username);
                editor.putString("email", email_input);
                editor.commit();


                //메인 화면으로 전환한다
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);

                finish();

            }else if(s.equals("none")){//존재하지 않는 계정일 경우

                //이미 존재하는 이메일이라고 띄워준다
                email_warning_textView.setVisibility(View.VISIBLE);
                email_warning_textView.setText("Account does not exist");

            }else if(s.equals("password")){//패스워드가 틀렸을 때

                //이미 존재하는 이메일이라고 띄워준다
                password_warning_textView.setVisibility(View.VISIBLE);
                password_warning_textView.setText("Password is incorrect");

            }else if(s.equals("activation")){//sms 인증이 되어 있지 않을 경우


                final AlertDialog.Builder builder = new AlertDialog.Builder(SignInActivity.this);
                builder.setTitle("SMS verification is needed");
                builder.setMessage("You need to verify your account to continue.");
                builder.setPositiveButton("Verify now",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                //sms 인증 화면으로 전환한다
                                Intent intent = new Intent(getApplicationContext(), SmsVerificationActivity.class);
                                intent.putExtra("email", email_input);
                                startActivity(intent);
                                finish();

                            }
                        });
                builder.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                dialog.dismiss();
                            }
                        });
                builder.show();


            }else{

                Log.d("tag", "response:"+s);
                Toast.makeText(SignInActivity.this, "Error", Toast.LENGTH_SHORT).show();
            }

        }
    }


}
