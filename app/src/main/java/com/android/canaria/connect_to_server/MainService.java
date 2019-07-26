package com.android.canaria.connect_to_server;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.android.canaria.ChatActivity;
import com.android.canaria.Function;
import com.android.canaria.MainActivity;
import com.android.canaria.db.DBHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;


//죽지않는 서비스

public class MainService extends Service {

    String TAG = "tag "+this.getClass().getSimpleName();

    public int counter = 0;
    Context context;

    int user_id;
    String username;

    ClientSocket clientSocket;
    Socket socket;

//    DBHelper dbHelper;
//    SQLiteDatabase db;

    public MainService(Context applicationContext){
        super();
        context = applicationContext;
        Log.d(TAG, "MainService.class 생성자");

    }

    public MainService(){};


    @Override
    public void onCreate() {
        super.onCreate();

        //현재 사용자의 정보를 가져온다
        user_id = Integer.valueOf(Function.getString(getApplicationContext(), "user_id"));
        username = Function.getString(getApplicationContext(), "username");

        //클라이언트 소켓 연결
        clientSocket = new ClientSocket();
        clientSocket.start();


    }


    //startService(intent) 때마다 onStartCommand 가 호출된다
        //액티비티의 onResume()같은 역할
        //서비스 실행중에 startService 가 호출되면, 서비스의 onCreate()가 호출되는게 아니라 onStartCommand()부터 시작한다
        //intent를 통해서 액티비티로부터 데이터를 전달받는다
        @Override
        public int onStartCommand (Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
            Log.d(TAG, "MainService - onStartCommand()");

//        int id = dbHelper.insert("chat_rooms", "room", "me;you;us", Function.getCurrentTime());
//        Log.d(TAG, "inserted id="+id);
//        String result = dbHelper.getResult_table_chatRooms();
//        Log.d(TAG, "result="+result);

        try {

            //액티비티에서 전달받은 메시지를, 서버에 보낸다
            if(intent.hasExtra("message")) {
                String message = intent.getStringExtra("message");
                Log.d(TAG, "Received a message from Activity: "+message);

                clientSocket.sendMsgToServer(message);
            }
//            startTimer();

        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d(TAG, ex);
        }
        return START_STICKY;
    }




    @Override
    public void onDestroy() {
        super.onDestroy();

        //앱이 꺼지면서 서비스가 종료될 때 -> broadcastReceiver로 인텐트 보냄
        //메인화면이 onDestroy()될 때 서비스를 종료하도록 설정했다
        Log.d(TAG, "MainService - onDestroy()");

        Intent broadcastIntent = new Intent(this, ServiceStopReceiver.class);
        sendBroadcast(broadcastIntent);

//        setAlarmTimer();
        stopTimerTask();


        try{
            if(socket != null){
//                sendMsg("closeRoom");
                socket.close();
            }
            clientSocket.quit();
        }catch (Exception e){
            Log.d(TAG, "socket closing error: "+e);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }





    class ClientSocket extends Thread{


        private boolean isSocketAlive = true;
        private boolean isNewRoom;
        private String friendInfo;

        private BufferedWriter bufferedWriter;
        private BufferedReader bufferedReader;

        private int roomId;


        // 서버에 메시지 보내기
        // 네트워크에 연결할 때는 서브 쓰레드를 이용해야 한다
        public void sendMsgToServer(final String msg){

            Log.d(TAG, "sendMsg() to server. message:"+msg);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "sendMsg()");

                    try{
                        bufferedWriter.write(msg + "\n");
                        bufferedWriter.flush();
                    }catch (Exception e){
                        Log.d(TAG, "sendMsg() error: "+e);
                    }

                }
            }).start();

        }


        private void sendMsgToChat(String message){
            Log.d(TAG, "Broadcasting message to chat activity");
            Intent intent = new Intent("chat_event");
            intent.putExtra("message", message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        private void sendMsgToMain(){
            Log.d(TAG, "Broadcasting message");
            Intent intent = new Intent("main_event");
            intent.putExtra("message", "This is my first message!");
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }





        @Override
        public void run() {
            try {

                socket = new Socket("54.180.107.44", 8000);
                Log.d(TAG, "connected to chat server");

                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                InputStreamReader isr = new InputStreamReader(is);
                bufferedReader = new BufferedReader(isr); //서버로부터 메시지를 읽어들이는 객체

                OutputStreamWriter osw = new OutputStreamWriter(os);
                bufferedWriter = new BufferedWriter(osw); //서버에 메시지를 쓰는 객체

                //서버에 연결 메시지 전송
                sendMsgToServer("connect/"+user_id+"/"+username);

            } catch (Exception e) {
                Log.d(TAG, "socket connection error");
                e.printStackTrace();
            }

            try {
                //클라이언트의 메인 쓰레드는 서버로부터 데이터 읽어들이는 것만 반복
                while(isSocketAlive) {

                    //서버에서 데이터를 보낼 때, 데이터를 '/'을 기준으로 한 문장으로 구성해서 보냄
                    //맨 앞 문자열: 클라이언트에게 보내는 신호(어떤 행동을 해라)
                    //그다음부터는 화면에 띄워줄 데이터
                    String line = bufferedReader.readLine();
                    Log.d(TAG, "readMsg(). message: "+line);

                    String[] line_array = line.split("/");
                    String signal = line_array[0];

                final String content;

                boolean isChatForeground = false;
                boolean isMainForeground = false;

                isChatForeground = Function.isForegroundActivity(getApplicationContext(), ChatActivity.class);
                isMainForeground = Function.isForegroundActivity(getApplicationContext(), MainActivity.class);

                //메시지를 sqlite 에 저장한다
                switch(signal){

                    //방이 만들어졌다는 알림
                    case "room_created":
                        roomId = Integer.valueOf(line_array[1]);

                        //db 연결
                        DBHelper dbHelper = new DBHelper(getApplicationContext(), Function.dbName, null, Function.dbVersion);
//                        SQLiteDatabase db = dbHelper.getWritableDatabase();

                        //방 정보를 내부 저장소에 저장한다
                        dbHelper.insert("chat_rooms", "room "+roomId, "me;you", Function.getCurrentTime());

                        String result = dbHelper.getResult_table_chatRooms();
                        Log.d(TAG, "saved new room. result="+result);

                        //room_created/roomid -> 모든 사용자가 저장까지는 완료.

                        if(isMainForeground){
                            //리사이클러뷰 업데이트 하라고 전달 - reload?
                        }

                        //roomInfo/방정보
                        //serverMsg/a room is created

                        break;
                    case "serverMsg":

                        content = line_array[1];

                        //@@@@@그냥 보내면 안됨. activeRoom 확인해야함 - 그래야 '방나누기'임
                        //my_room_created와 room_created를 나눌것!!!!!
                        if(isChatForeground){
                            sendMsgToChat(line); //signal이 포함된 전체 메시지를 보낸다
                            //챗화면에 메시지 띄워주기
                        }

//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(0, new MessageItem("", content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });

                        break;
                    case "join": //누군가 방에 새로 가입했을 때

                        content = line_array[1];

//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(new MessageItem("", content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });

                        break;
//                    case "myJoin": //내가 방에 새로 가입했을 때
//
//                        content = line_array[1];
//
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(new MessageItem("", content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });
//
//                        break;
//                    case "return"://누군가 방으로 돌아왔을 때
//
//                        content = line_array[1];
//
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(new MessageItem("", content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });
//
//                        break;
//                    case "myReturn"://내가 방으로 돌아왔을 때
//
//                        content = line_array[1];
//
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(new MessageItem("", content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });
//
//                        break;
                    case "roomInfo":

                        // 방이름 (참여인원) 참여자1, 참여자2, 참여자3
                        content = line_array[1];

                        if(isChatForeground){
                            sendMsgToChat(line); //signal이 포함된 전체 메시지를 보낸다
                            //챗화면에 메시지 띄워주기
                        }


//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                roomInfo_textView.setText(content);
//                            }
//                        });

                        break;
                    case "msg": //다른 사람이 보낸 메시지 알림
                        //메시지 보낸사람 정보 + 메시지 내용
                        String sender_id = line_array[1];
                        final String sender_username = line_array[2];
                        content = line_array[3];

//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(new MessageItem(sender_username, content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });

                        break;
                    case "myMsg": //내가 보낸 메시지 알림
                        content = line_array[1];

//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(new MessageItem("[Me]", content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });

                        break;
//                    case "inactive"://방 참여자중 누군가 방을 닫고 메시지를 읽지 않는 상태일 때
//                        content = line_array[1];
//
//                        handler.post(new Runnable() {
//                            @Override
//                            public void run() {
//                                messageItemList.add(new MessageItem("", content));
//                                adapter.notifyItemInserted(adapter.getItemCount()-1);
//                            }
//                        });
//
//                        break;
//
//
//
//                        case "out"://방 참여자중 누군가 방을 나갔을 때
//                            String friend_id = line_array[1];
//                            final String friend_username = line_array[2];
//
//                            handler.post(new Runnable() {
//                                @Override
//                                public void run() {
//                                    messageItemList.add(new MessageItem("", friend_username+" has left the room."));
//                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
//                                }
//                            });
//
//                            break;
                }


                }
            }catch(IOException e) {
                Log.d(TAG, "socket reader error: "+e);
            }
        }

        public void quit(){
            Log.d(TAG, "client thread, quit()");
            isSocketAlive = false;
        }


    }






    //--------------서비스 작동 테스트용 타이머---------------//
    private Timer timer;
    private TimerTask timerTask;
    long oldTime = 0;
    public void startTimer(){
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 1000);
    }

    public void initializeTimerTask(){
        timerTask = new TimerTask() {
            @Override
            public void run() {
                Log.d(TAG, "MainService, 타이머 안 - counter: "+(counter++));
            }
        };
    }

    public void stopTimerTask(){
        if(timer != null){
            timer.cancel();
            timer = null;
        }
    }
    //-------------------------------------------------------//




    protected void setAlarmTimer(){
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.add(Calendar.SECOND, 1);
        Intent intent = new Intent(this, ServiceStopReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(this, 0,intent,0);

        AlarmManager mAlarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        mAlarmManager.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), sender);
    }


//    public void showNotification(String heading, String description){
//
//        createChannel(this);
//
//        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"channelID")
//                .setContentTitle(heading)
//                .setContentText(description)
//                .setAutoCancel(true);
//
//        Intent openThePageIntent = new Intent(this, CheckGoals.class);
//        openThePageIntent.putExtra("preferenceName", preferenceName);
//        openThePageIntent.putExtra("isClicked", true);//해당 메시지를 클릭해서 읽었는지 확인하기 위한 용도. true/false 값은 상관없음
//        openThePageIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
//
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, (int)(System.currentTimeMillis()/1000), openThePageIntent, PendingIntent.FLAG_ONE_SHOT);
//
//        notificationBuilder.setContentIntent(pendingIntent);
//
//        int notificationId = (int)(System.currentTimeMillis()/1000);
//        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
//        notificationManager.notify(notificationId, notificationBuilder.build());
//    }

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
