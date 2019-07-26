package com.android.canaria.login;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.android.canaria.connect_to_server.RedirectActivity;

/*sms 수신을 감지하는 클래스*/

/*주의: 정적으로 브로드캐스트리시버를 만들면(=매니페스트에 등록), 해당 기기에 수신되는 모든 문자를 감지하게 된다.
-> 에러 발생 + 자원낭비. 반드시 동적으로 구성해야 한다 */
public class SmsReceiver{

    final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";
    private BroadcastReceiver broadcastReceiver = null;


    public void registerReceiver(Context context){

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SMS_RECEIVED);

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String lAction = intent.getAction();
                if( lAction.equals(SMS_RECEIVED) ) {
                    Log.d("tag", "smsReceiver: onReceive");

                    Bundle bundle = intent.getExtras();
                    Object[] msg = (Object[]) bundle.get("pdus");
                    SmsMessage[] smsMsg = new SmsMessage[msg.length];

                    for(int i=0; i< msg.length; i++) {
                        smsMsg[i] = SmsMessage.createFromPdu((byte[])msg[i]);
                    }

                    //Data curDate = new Data(smsMsg[0].getTimestampMillis());

                    String msgContent = smsMsg[0].getMessageBody().toString();
                    Log.d("tag", "onReceive: messageContent="+msgContent);


                    try{

                        String[] msgArray = msgContent.split("<");

                        String code_raw = msgArray[1];
                        String code = code_raw.split(">")[0];

                        Log.d("tag", "onReceive: code_received="+code);

                        Intent i = new Intent(context, RedirectActivity.class);
                        i.putExtra("code_received", code);
                        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(i);

                    }catch (Exception e){//카나리아가 아닌 다른 곳에서 받은 문자 내용을 분석하면, 에러가 발생함
                        Log.d("tag", "smsReceiver: error during processing received message: "+e);
                    }

                }

            }
        };

        //브로드캐스트 리시버 등록
        context.registerReceiver(broadcastReceiver, intentFilter);

    }


    //동적으로 해제
    public void unregisterReceiver(Context context){

        if(broadcastReceiver != null){

            Log.d("tag","smsReceiver is unregistered");

            context.unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

    }

}
