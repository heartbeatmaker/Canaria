package com.android.canaria.login;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.R;

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

public class SmsVerificationActivity extends AppCompatActivity {

    EditText phoneNumber_editText;
    TextView requestCode_btn, warning_textView;
    Spinner countryCode_spinner;

    String zipCode, email, phone_number_modified;

    ArrayList<String> arrayList;
    ArrayAdapter<String> arrayAdapter;

    public void getCountryZipCode(){
        String CountryID="";
        String CountryZipCode="";

        TelephonyManager manager = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        //getNetworkCountryIso
        CountryID= manager.getSimCountryIso().toUpperCase();
        String[] rl=this.getResources().getStringArray(R.array.CountryCodes); //zipCode와 국가이름이 같이 있는 형태

        arrayList = new ArrayList<>();

        int index = 0; //현재 사용자가 있는 국가의 zipCode position
        for(int i=0;i<rl.length;i++){
            String[] g=rl[i].split(",");

            arrayList.add(g[0].trim()); //arrayList에 zipCode만 담는다

            if(g[1].trim().equals(CountryID.trim())){
                index = i;
                CountryZipCode=g[0];
                Log.d("tag", "countryZipCode="+CountryZipCode);
            }
        }


        arrayAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_spinner_item, arrayList);
        arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        countryCode_spinner.setAdapter(arrayAdapter);
        countryCode_spinner.setSelection(index); //spinner 기본값 = 현재 사용자가 있는 국가의 zipCode


        countryCode_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                //사용자가 선택한 zipCode를 가져온다
                zipCode = arrayList.get(position);
                Log.d("tag", "selected zipCode= "+zipCode);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        for(int i=0;i<rl.length;i++){
            String[] g=rl[i].split(",");
            if(g[1].trim().equals(CountryID.trim())){
                CountryZipCode=g[0];
                break;
            }
        }
//        return CountryZipCode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_verification);

        phoneNumber_editText = (EditText)findViewById(R.id.sms_phoneNumber_editText);
        requestCode_btn = (TextView)findViewById(R.id.sms_send_btn);
        countryCode_spinner = (Spinner)findViewById(R.id.sms_countryCode_spinner);
        warning_textView = (TextView)findViewById(R.id.sms_code_warning_textView);

        getCountryZipCode();

//        countryCode_textView.setText("+"+zipCode);

        Intent intent = getIntent();

        if(intent.hasExtra("email")){
            email = intent.getStringExtra("email");
        }


        requestCode_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String phone_number_noZipCode = phoneNumber_editText.getText().toString();
                Log.d("tag","phone_number="+phone_number_noZipCode);

                //올바른 번호를 입력했는지 확인해야 한다
                String regEx = "(01[016789])(\\d{3,4})(\\d{4})";
                if(!Pattern.matches(regEx, phone_number_noZipCode)){ //잘못된 형식의 번호를 입력했을 경우

                    Log.d("tag","phone_number is invalid");

                    //다시 입력하라고 경고메시지를 띄워준다
                    warning_textView.setVisibility(View.VISIBLE);
                    warning_textView.setText("Phone number is invalid");

                }else{ //올바른 번호를 입력했을 경우 -> 서버로 사용자 이메일과 번호를 전송한다

                    Log.d("tag","phone_number is valid");

                    phone_number_modified = phone_number_noZipCode.substring(1); //010 에서 맨 앞 0을 잘라낸다

                    try{

                        SmsVerificationActivity.SendPost sendPost = new SmsVerificationActivity.SendPost();
                        sendPost.execute(email, zipCode, phone_number_modified);

                    }catch (Exception e){
                        Log.d("tag",this.getClass().getName()+ " error :"+e);
                    }

                }


            }
        });



        phoneNumber_editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                warning_textView.setText("");
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

    }



    class SendPost extends AsyncTask<String, Void, String> {


        ProgressDialog dialog = new ProgressDialog(SmsVerificationActivity.this);

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
            Log.d("tag","sms: doInBackground. param0="+strings[0]+"/param1="+strings[1]+"/param2="+strings[2]);

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://15.164.193.65/sms.php");

            //POST 방식에서 사용된다
            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

            try {
                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
                nameValues.add(new BasicNameValuePair(
                        "email", URLDecoder.decode(strings[0], "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "zip_code", URLDecoder.decode(strings[1], "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "phone_number", URLDecoder.decode(strings[2], "UTF-8")));

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
            dialog.dismiss();

            String result = "";
            String code_sent = "";
            try{
                String[] resArray = s.split(";");
                result = resArray[0];
                code_sent = resArray[1];
            }catch (Exception e){

                Log.d("tag", this.getClass().getName()+" Error: "+e);
            }

            if(result.equals("success")){//결과가 '성공'이면

                //코드 입력 화면으로 넘어간다(이메일, 폰번호, 코드를 인텐트로 보낸다)
                //인증 완료 시 폰번호를 사용자의 db에 저장하도록 하기 위해, 폰번호도 함께 보낸다
                Toast.makeText(SmsVerificationActivity.this, "Message has been sent", Toast.LENGTH_SHORT).show();
                Intent i = new Intent(getApplicationContext(), SmsVerification_SecondActivity.class);
                i.putExtra("email", email);
                i.putExtra("phone_number", phone_number_modified); // 앞의 0을 제거한 10자리 숫자
                i.putExtra("code_sent", code_sent);
                startActivity(i);

            }else if(result.equals("exists")){//이미 존재하는 번호이면

                //다시 입력하라고 경고메시지를 띄워준다
                warning_textView.setVisibility(View.VISIBLE);
                warning_textView.setText("The number already exists");

            }


        }
    }
}
