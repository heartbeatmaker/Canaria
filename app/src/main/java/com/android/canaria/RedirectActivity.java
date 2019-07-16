package com.android.canaria;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;


/*SmsReceiver -> sms 코드입력 액티비티로 리다이렉트하는 액티비티이다
* 액티비티가 없으면, singletop과 cleartop을 곧장 선언할 수 없다고 한다(확인 요망)
* 그래서 smsReceiver에서 이 액티비티로 한 번 거쳐서 가는 것임
* 이 액티비티는 레이아웃이 없음. 없어도 된다고 함*/
public class RedirectActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("tag", "RedirectActivity.class");

        Intent intent = getIntent();
        intent.setClass(RedirectActivity.this, SmsVerification_SecondActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP |Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }
}
