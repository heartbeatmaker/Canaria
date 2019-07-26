package com.android.canaria.login;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
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
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.regex.Pattern;

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

public class SignUpActivity extends AppCompatActivity {

    Button signup_with_email_btn;

    LinearLayout signup_with_email_linearLayout;

    String username, email, password, password_forConfirmation;

    EditText username_editText, email_editText, password_editText, password2_editText;
    TextView signup_btn, signin_btn;

    TextView username_warning, email_warning, password_warning, password2_warning;

    String username_input, email_input, password_input;

    private String TAG = "signup.class";

    /*credential 변수*/
    private int RC_READ = 1000;
//    private int RC_SAVE = 2000;
    CredentialsClient mCredentialsClient;
    CredentialRequest mCredentialRequest;


    private void automaticLogin(){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        //로그인상태가 true이면 -> 바로 메인화면으로 전환
        if(!pref.getString("user_id", "null").equals("null")){
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_up);

        Log.d(TAG,"oncreate");

        automaticLogin();//이미 로그인 상태면, 메인화면으로 전환한다

        smsPermissionCheck(); //sms 수신 권한을 받지 않으면, 메시지를 읽어올 수 없다


        //로그인상태가 true이면 -> 바로 메인화면으로 전환
//        isLogin();


        signup_with_email_linearLayout = (LinearLayout)findViewById(R.id.signup_with_email_linearLayout);

        //입력창
        username_editText = (EditText)findViewById(R.id.signup_username_editText);
        email_editText = (EditText)findViewById(R.id.signup_email_editText);
        password_editText = (EditText)findViewById(R.id.signup_password_editText);
        password2_editText= (EditText)findViewById(R.id.signup_confirm_password_editText);


        //사용자가 입력한 값에 문제가 있을 경우, 메시지를 띄워주는 뷰 - 현재 GONE 상태
        username_warning = (TextView)findViewById(R.id.signup_username_warning_textView);
        email_warning = (TextView)findViewById(R.id.signup_email_warning_textView);
        password_warning = (TextView)findViewById(R.id.signup_password_warning_textView);
        password2_warning = (TextView)findViewById(R.id.signup_password2_warning_textView);


        signup_with_email_btn = (Button)findViewById(R.id.signup_with_email_btn);
        signup_btn = (TextView)findViewById(R.id.signup_cofirm_btn);
        signin_btn = (TextView) findViewById(R.id.signup_alreadyHaveAccount_textView);



        signup_with_email_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //이메일 가입 양식을 보여준다
                signup_with_email_linearLayout.setVisibility(View.VISIBLE);

                getCredentials(); //구글 스마트락에 저장된 이메일을 가져온다

            }
        });


        signin_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("tag","signin_btn clicked");

                Intent intent = new Intent(getApplicationContext(), SignInActivity.class);
                startActivity(intent);
            }
        });


        //사용자가 입력한 텍스트를 가져온다
        username = username_editText.getText().toString();
        email = email_editText.getText().toString();
        password = password_editText.getText().toString();
        password_forConfirmation = password2_editText.getText().toString();


        //확인 버튼을 누르면
        signup_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Log.d("tag", "clicked");

               //사용자가 입력한 값을 확인한다 -> 문제가 있는 칸 밑에 warning 메시지를 띄워준다
                boolean isContentValid = false;


                username_input = username_editText.getText().toString();
                email_input = email_editText.getText().toString();
                password_input = password_editText.getText().toString();
                String password2_input = password2_editText.getText().toString();

                //닉네임: 채워졌는지 확인
                if(username_input.length()==0){
                    username_warning.setVisibility(View.VISIBLE);
                    username_warning.setText("Enter your name");
                    isContentValid=false;
                    Log.d(TAG, "username is invalid");
                }else{
                    isContentValid = true;
                    Log.d(TAG, "username is valid");
                }


                //이메일: 채워졌는지, 올바른 양식인지 확인(정규표현식 사용)
                if(email_input.length()==0){
                    email_warning.setVisibility(View.VISIBLE);
                    email_warning.setText("Enter your email");
                    isContentValid=false;

                }else{//사용자가 이메일을 입력했다면

                    //올바른 양식인지 확인
                    if(Patterns.EMAIL_ADDRESS.matcher(email_input).matches()){
                        isContentValid = true;
                    }else{
                        email_warning.setVisibility(View.VISIBLE);
                        email_warning.setText("Enter a valid email address");
                        isContentValid = false;
                    }

                }

                //첫번째 패스워드: 채워졌는지 + 올바른 양식인지 확인(정규표현식 사용)
                if(password_input.length()==0){
                    password_warning.setVisibility(View.VISIBLE);
                    password_warning.setText("Enter your password");
                    isContentValid=false;

                }else{ //첫번째 패스워드를 입력했다면

                    //올바른 양식인지 확인 - 6자 이상
                    if(Pattern.matches("^(?=.*[A-Za-z])(?=.*\\d)(?=.*[$@$!%*#?&])[A-Za-z\\d$@$!%*#?&]{6,20}$", password_input)){
                        isContentValid = true;
                    }else{
                        password_warning.setVisibility(View.VISIBLE);
                        password_warning.setText("Passwords must be 6 to 20 characters of alphabets and numbers");
                        isContentValid = false;
                    }

                    //두번째 패스워드 확인: 채워졌는지 + 첫번째 패스워드와 일치하는지 검사한다
                    if(password2_input.length()==0){
                        password2_warning.setVisibility(View.VISIBLE);
                        password2_warning.setText("Type your password again");
                        isContentValid=false;
                    }else{
                        if(password_input.equals(password2_input)){
                            isContentValid = true;
                        }else{
                            password2_warning.setVisibility(View.VISIBLE);
                            password2_warning.setText("Passwords must match");
                            isContentValid = false;
                        }
                    }
                }

                Log.d("tag","isContentValid="+isContentValid);
                if(isContentValid){
                    //서버로 입력값을 보낸다
                    Log.d("tag", "content is valid");

                    try{

                        SendPost sendPost = new SendPost();
                        sendPost.execute(username_input, email_input, password_input);

                        //execute(~~~).get() : retrieve your result once the work on the thread is done.
                        //get() 메소드는 AsyncTask가 실행되는 동안 UI 쓰레드를 block 시킨다. 쓰레드 실행 중에 ui 변경이 불가 (ex.로딩바)
                        //따라서 get()을 사용하지 않는다. 서버와 통신 후 처리는 onPostExecute()에서 한다

                    }catch (Exception e){
                        Log.d("tag", this.getClass().getName()+" Error: "+e);
                    }

                }

            }
        });



        //사용자가 입력하는 동안에는 warning 창을 숨겨놓는다
        username_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                username_warning.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        email_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                email_warning.setVisibility(View.GONE);
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
                password_warning.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        password2_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                password2_warning.setVisibility(View.GONE);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });


    }



    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == 1) {
            for (int i = 0; i < permissions.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, permissions[i] + " granted.", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, permissions[i] + " denied.", Toast.LENGTH_LONG).show();
                }
            }
        }
    }


    //sms 수신권한 확인. 이거 있어야 smsReceiver가 문자 감지할 수 있음
    public void smsPermissionCheck(){

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS);
        if(permissionCheck == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "SMS 수신권한 있음", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "SMS 수신권한 없음", Toast.LENGTH_SHORT).show();
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.RECEIVE_SMS)){
                Toast.makeText(this, "SMS 권한 설정이 필요함", Toast.LENGTH_SHORT).show();
            } else {
                // 권한이 할당되지 않았으면 해당 권한을 요청
                ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.RECEIVE_SMS},1);
            }
        }
    }



    //구글 smart lock에서 사용자 이메일 불러오기 ------------------------------------
    private void getCredentials(){
        Log.d("tag", "getCredentials");

        mCredentialsClient = Credentials.getClient(this);

        mCredentialRequest = new CredentialRequest.Builder()
                .setPasswordLoginSupported(true)
                .setAccountTypes("CANARIA")
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


//        if (requestCode == RC_SAVE) {
//            if (resultCode == RESULT_OK) {
//                Log.d(TAG, "SAVE: OK");
//                Toast.makeText(this, "Credentials saved", Toast.LENGTH_SHORT).show();
//            } else {
//                Log.e(TAG, "SAVE: Canceled by user");
//            }
//        }

    }


    //사용자의 구글 id와 이름을 가져오는 메소드
    private void onCredentialRetrieved(com.google.android.gms.auth.api.credentials.Credential credential) {
        Log.d("tag","Credential is selected");

        String accountType = credential.getAccountType();
//        Log.d("tag","accountType="+accountType);
        Log.d("tag","id="+credential.getId()+" / name="+credential.getName());
        email_input = credential.getId();
        username_input = credential.getName();

        email_editText.setText(email_input);
        username_editText.setText(username_input);

//        if (accountType == null) {
//
//            // Sign the user in with information from the Credential.
////            signInWithPassword(credential.getId(), credential.getPassword());
//            Log.d("tag","id="+credential.getId()+" / password="+credential.getPassword());
//
//        } else if (accountType.equals(IdentityProviders.GOOGLE)) {
//            // The user has previously signed in with Google Sign-In. Silently
//            // sign in the user with the same ID.
//            // See https://developers.google.com/identity/sign-in/android/
//            GoogleSignInOptions gso =
//                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                            .requestEmail()
//                            .build();
//
//            GoogleSignInClient signInClient = GoogleSignIn.getClient(this, gso);
//            Task<GoogleSignInAccount> task = signInClient.silentSignIn();
//
//        }
    }

    //구글 smart lock에서 사용자 이메일 불러오기 끝 ------------------------------------




    class SendPost extends AsyncTask<String, Void, String>{

        ProgressDialog dialog = new ProgressDialog(SignUpActivity.this);

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
            Log.d("tag","doInBackground. param1="+strings[0]+"/param2="+strings[1]+"/param3="+strings[2]);

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://54.180.107.44/register.php");

            //POST 방식에서 사용된다
            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

            try {
                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
                nameValues.add(new BasicNameValuePair(
                        "sign_up", URLDecoder.decode("1", "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "username", URLDecoder.decode(strings[0], "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "email", URLDecoder.decode(strings[1], "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "password", URLDecoder.decode(strings[2], "UTF-8")));

                //HttpPost에 넘길 값을들 Set해주기
                post.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));

            } catch (UnsupportedEncodingException ex) {
                Log.d("tag", ex.toString());
            }

            try {
                //설정한 URL을 실행시키기 -> 응답을 받음
                HttpResponse response = client.execute(post);
                //통신 값을 받은 Log 생성. (200이 나오는지 확인할 것~) 200이 나오면 통신이 잘 되었다는 뜻!
                Log.i("tag", "response.getStatusCode:" + response.getStatusLine().getStatusCode());

                HttpEntity entity = response.getEntity();

                if(entity !=null){
                    Log.d("tag", "Response length:"+entity.getContentLength());

                    // 콘텐츠를 읽어들임.
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));


                    while ((response_line = reader.readLine()) != null) {
                        // 콘텐츠 내용
                        Log.d("tag", "response: "+response_line);
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

            dialog.dismiss();
            Log.d("tag","onPostExecute. param="+s);

            String result = "";
            String user_id = "";
            try{
                String[] resArray = s.split(";");
                result = resArray[0];
                user_id = resArray[1];
            }catch (Exception e){

                Log.d("tag", this.getClass().getName()+" Error: "+e);
            }


            if(result.equals("success")){//결과가 '성공'이면

                //사용자의 정보를 저장한다
                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("user_id", user_id);
                editor.putString("username", username);
                editor.putString("email", email_input);
                editor.commit();


                //sms 코드 인증 화면으로 전환한다
                Intent intent = new Intent(getApplicationContext(), SmsVerificationActivity.class);
                intent.putExtra("email", email_input);
                startActivity(intent);
                finish();

            }else if(s.equals("exists")){//결과가 '이미 존재함'이면

                //이미 존재하는 이메일이라고 띄워준다
                email_warning.setVisibility(View.VISIBLE);
                email_warning.setText("Email already exists");
            }

        }
    }



}
