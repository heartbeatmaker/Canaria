package com.android.canaria.connect_to_server;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;

//눈속임용 서비스 -- https://forest71.tistory.com/185 참고
//백그라운드 서비스는 더이상 지원되지 않는다. ForegroundService만 가능
//근데 foregroundService는 사용자의 눈에 보인다(notification으로 떠 있음)
//따라서 foregroundService를 실행하자마자, 작업을 원하는 service를 실행하고 이 foregroundService를 중지시킨다
public class RestartService extends Service {

    String TAG = "tag "+this.getClass().getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "RestartService - onCreate()");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        try{
            Log.d(TAG, "RestartService - onStartCommand");

            //알람 채널을 만듦
            createChannel(RestartService.this);

            //알람 세팅
            Notification notification = new NotificationCompat.Builder(RestartService.this,"channelID")
                    .setContentTitle("Canaria is active")
                    .setContentText("Service ing")
                    .build();

            //startForegroundService를 호출한 뒤, 5초 내에 startForeground를 실행하지 않으면 서비스가 중지된다
            startForeground(9, notification);

            //업무를 처리할 진짜 서비스를 실행한다
            Intent in = new Intent(this, MainService.class);
            startService(in);
            Log.d(TAG, "RestartService - SensorService를 실행시킴");

            //가짜 서비스를 중지시킨다(이 과정이 빠르게 진행되기 때문에, 눈에 보이지 않는다)
            stopForeground(true);
            Log.d(TAG, "RestartService - stopForeground(true);");
            stopSelf();
            Log.d(TAG, "RestartService - stopSelf()");

        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d(TAG,ex);
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    public void createChannel(Context context){
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("channelID","name", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Description");
        notificationManager.createNotificationChannel(channel);
    }
}
