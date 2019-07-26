package com.android.canaria.connect_to_server;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;


// 이 broadcastReceiver가 호출되는 상황은 다음과 같다
// : SensorService가 onDestroy()될 때, 휴대폰이 재부팅될 때(manifest에 등록되어 있음)
public class ServiceStopReceiver extends BroadcastReceiver {

    String TAG = "tag "+this.getClass().getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {

        try {
            Log.d(TAG, "ServiceStopReceiver 클래스, onReceive 들어옴");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

                //fake 서비스를 실행시킨다(진짜 서비스는 fake 서비스 안에서 실행된다)
                Intent in = new Intent(context, RestartService.class);
                context.startForegroundService(in);
                Log.d(TAG, "context.startForegroundService");
            } else {
                Intent in = new Intent(context, MainService.class);
                context.startService(in);
            }
        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d(TAG,ex);
        }
    }



}
