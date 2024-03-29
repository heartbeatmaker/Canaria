package com.android.canaria.connect_to_server;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.android.canaria.ChatActivity;
import com.android.canaria.Function;
import com.android.canaria.MainActivity;
import com.android.canaria.Main_Fragment2;
import com.android.canaria.NoInternetActivity;
import com.android.canaria.PikachuDetectorActivity;
import com.android.canaria.R;
import com.android.canaria.db.DBHelper;
import com.android.canaria.recyclerView.RoomListItem;

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

    DBHelper dbHelper;
//    SQLiteDatabase db;

    public MainService(Context context){
        super();
        this.context = context;
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


        //db 연결
        dbHelper = new DBHelper(getApplicationContext(), Function.dbName, null, Function.dbVersion);
        dbHelper.open();

    }


    //startService(intent) 때마다 onStartCommand 가 호출된다
        //액티비티의 onResume()같은 역할
        //서비스 실행중에 startService 가 호출되면, 서비스의 onCreate()가 호출되는게 아니라 onStartCommand()부터 시작한다
        //intent를 통해서 액티비티로부터 데이터를 전달받는다
        @Override
        public int onStartCommand (Intent intent,int flags, int startId){
        super.onStartCommand(intent, flags, startId);
            Log.d(TAG, "MainService - onStartCommand()");

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

        int status = NetworkStatus.getConnectivityStatus(getApplicationContext());
        if(status != NetworkStatus.TYPE_NOT_CONNECTED){
            Intent broadcastIntent = new Intent(this, ServiceStopReceiver.class);
            sendBroadcast(broadcastIntent);
        }

//        setAlarmTimer();
//        stopTimerTask();


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

//        private int roomId;


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
                        Log.d(TAG, "sendMsg() error");
                        StringWriter sw = new StringWriter();
                        e.printStackTrace(new PrintWriter(sw));
                        String ex = sw.toString();

                        Log.d(TAG,ex);
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

        private void sendMsgToMain(String message){
            Log.d(TAG, "Broadcasting message");
            Intent intent = new Intent("roomList_event");
            intent.putExtra("message", message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }

        private void sendMsgToPikachuDetector(String message){
            Log.d(TAG, "Broadcasting message");
            Intent intent = new Intent("pikachu_event");
            intent.putExtra("message", message);
            LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
        }





        @Override
        public void run() {
            try {

                socket = new Socket("15.164.193.65", 8000);
                Log.d(TAG, "connected to chat server");

                InputStream is = socket.getInputStream();
                OutputStream os = socket.getOutputStream();
                InputStreamReader isr = new InputStreamReader(is);
                bufferedReader = new BufferedReader(isr); //서버로부터 메시지를 읽어들이는 객체

                OutputStreamWriter osw = new OutputStreamWriter(os);
                bufferedWriter = new BufferedWriter(osw); //서버에 메시지를 쓰는 객체

                //서버에 연결 메시지 전송
                //참여중인 방 id를 가져온다
                String myRoom_id = "";
                Cursor cursor = dbHelper.db.rawQuery("SELECT room_id FROM chat_rooms", null);
                while (cursor.moveToNext()) {
                    myRoom_id += cursor.getString(0) +";";
                }
                myRoom_id = myRoom_id.substring(0, myRoom_id.length()-1);

                sendMsgToServer("connect/"+user_id+"/"+username+"/"+myRoom_id);

            } catch (Exception e) {
                Log.d(TAG, "socket connection error");
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);

            }

            try {
                //클라이언트의 메인 쓰레드는 서버로부터 데이터 읽어들이는 것만 반복
                while(isSocketAlive) {

                    //서버에서 데이터를 보낼 때, 데이터를 '/'을 기준으로 한 문장으로 구성해서 보냄
                    //맨 앞 문자열: 클라이언트에게 보내는 신호(어떤 행동을 해라)
                    //그다음부터는 화면에 띄워줄 데이터
                    String line = bufferedReader.readLine();
                    Log.d("방나감", "readMsg(). message: "+line);

                    String[] line_array = line.split("/");
                    String signal = line_array[0];

                final String content;

                boolean isChatForeground = false;
                boolean isMainForeground = false;
                boolean isPikaForeground = false;

                isChatForeground = Function.isForegroundActivity(getApplicationContext(), ChatActivity.class);
                isMainForeground = Function.isForegroundActivity(getApplicationContext(), MainActivity.class);
                isPikaForeground = Function.isForegroundActivity(getApplicationContext(), PikachuDetectorActivity.class);

                switch(signal){

                    //피카츄 사진이 다 분석되었다는 알림
                    //pikachu_output/success or fail/filename
                    case "pikachu_output":

                        //해당 화면을 보고 있을 때에만 결과를 띄워준다
                        if(isPikaForeground){
                            sendMsgToPikachuDetector(line);
                        }

                        break;
                    //방이 만들어졌다는 알림
                    //room_created/roomId/방이름/인원/memberInfo(참여자id;username..)
                    case "room_created":
                        int roomId = Integer.valueOf(line_array[1]);
                        String roomName = line_array[2];
                        int number_of_members = Integer.valueOf(line_array[3]);
                        String memberInfo_string = line_array[4];


                        //방 정보를 내부 저장소에 저장한다
                        String currentTime = Function.getCurrentTime();
                        //서버가 정해준 room id를 직접 저장해야 한다. auto increment(x)
                        dbHelper.insert_chatRooms(roomId, roomName, memberInfo_string, currentTime);

//                        String result = dbHelper.getResult_table_chatRooms();
//                        Log.d(TAG, "saved new room. result="+result);

                        //현재 클라이언트 = 방 개설자일때. 이 때 사용자의 foreground activity는 무조건 ChatActivity
                        int room_master_id = Integer.valueOf(memberInfo_string.split(";")[0]);
                        if(room_master_id == user_id){
                            Log.d(TAG, "this client created the room");

                            //채팅화면으로 방 id를 보낸다
                            sendMsgToChat("room_created/"+roomId);

                            //방목록에 아이템을 추가한다
                            Main_Fragment2.roomItemList.add(0, new RoomListItem(roomName, number_of_members,
                                    "", currentTime, roomId, 0));

                            //ChatActivity에 방 정보를 전달한다
                            String msg_roomInfo = "";
                            if(number_of_members == 2){ //1대1 채팅의 경우: 방 인원을 발송하지 않는다
                                msg_roomInfo = "roomInfo/"+roomName+"/"+memberInfo_string;
                            }else{
                                msg_roomInfo = "roomInfo/"+roomName+" ("+number_of_members+")"+"/"+memberInfo_string;
                            }
                            sendMsgToChat(msg_roomInfo);

                        }else{ //현재 클라이언트 = 초대된 사람일 때. 이 때 사용자의 foreground activity는 불명. 확인해야 한다
                            Log.d(TAG, "this client is invited to the room");

                            try{
                                // 방목록에 아이템을 추가한다
                                // 앱이 background에 있을 경우: onResume()에서 datasetChanged()가 호출된다
                                Main_Fragment2.roomItemList.add(0, new RoomListItem(roomName, number_of_members,
                                        "", currentTime, roomId, 0));
                            }catch (Exception e){
                                //앱이 꺼져있는데 서비스만 돌고 있을 경우, 오류가 날 수 있다
                                StringWriter sw = new StringWriter();
                                e.printStackTrace(new PrintWriter(sw));
                                String ex = sw.toString();

                                Log.d(TAG,ex);
                            }

                            if(isMainForeground){
                                Log.d(TAG, "MainActivity is at foreground");

                                //방목록 어댑터를 업데이트 하라고 메시지를 보낸다
                                sendMsgToMain("inserted/");
                            }
                        }

                        break;

                    //이 사용자가 기존에 있던 방에 다시 들어갔을 때, 서버로부터 방 정보를 수신받는다
                    //이 사용자한테만 오는 알림이다
                    case "roomInfo":

                        // 방이름 (참여인원) 참여자1, 참여자2, 참여자3
                        content = line_array[1];

                        if(isChatForeground){ //예기치않게 ChatActivity 가 꺼졌을 경우를 대비, 예외처리

                            sendMsgToChat(line); //signal이 포함된 전체 메시지를 보낸다
                            //챗화면에 메시지 띄워주기
                        }

                        break;

                    case "msg": //메시지 알림(내가 보낸 메시지 + 남이 보낸 메시지 + 서버 메시지)
//                        msg/roomId/sender_id/sender_username/message

                        int roomId_msg = Integer.valueOf(line_array[1]);
                        int sender_id = Integer.valueOf(line_array[2]);
                        final String sender_username = line_array[3];
                        String message = line_array[4];


                        //이미지 or 비디오 파일을 담은 메시지인지 확인한다
                        //image!-!파일이름1;파일이름2;파일이름3
                        //video!-!thumbnail_filename!-!video_filename
                        boolean isImage = false;
                        String filename_string = "";

                        //비디오일 경우 필요한 변수
                        boolean isVideo = false;
                        String thumbnail_filename = "";
                        String video_filename = "";

                        int number_of_files = 0; //사진 개수
                        try{
                            String[] text_array = message.split("!-!");
                            if(text_array[0].equals("image")){

                                isImage = true;

                                //파일이름을 가져온다. 파일이름1;파일이름2;파일이름3..
                                filename_string = text_array[1];

                                //사진 개수
                                number_of_files = filename_string.split(";").length;

                                //푸쉬알람에 띄워줄 내용을 지정한다
                                if(number_of_files == 1){
                                    message = number_of_files +" Photo";
                                }else{
                                    message = number_of_files +" Photos";
                                }

                            }else if(text_array[0].equals("video")){
//                                msg/roomId/id/username/video!-!thumbnail_filename!-!video_filename

                                isVideo = true;

                                thumbnail_filename = text_array[1];
                                video_filename = text_array[2];

                                //푸쉬알람에 띄워줄 내용을 지정한다
                                message = "1 Video";
                            }



                        }catch (Exception e){
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            Log.d(TAG,ex);
                        }

                        String curTime_msg = Function.getCurrentTime();//현재 시각

                        int unreadMsgCount = dbHelper.get_unreadMsgCount(roomId_msg); //사용자가 이 방에서 안 읽은 메시지 개수(원래 개수)

                        int isRead = 0; // 1=true, 0=false 사용자가 이 메시지를 읽었는지 안 읽었는지 표시

                        try{
                            //공통: 메시지 저장, roomItemList 업데이트(insert 알림x)

                            //1. 메시지를 내부 저장소에 저장한다

                            //사용자가 지금 채팅방에서 이 메시지를 바로 읽을 수 있는 경우 -> 이 메시지를 읽은 것으로 처리
                            if(isChatForeground && ChatActivity.roomId == roomId_msg){
                                isRead = 1;

                            }else{ //그렇지 않은 모든 경우 -> 이 메시지는 읽지 않은 것으로 처리 / 이 방에서 사용자가 안 읽은 메시지 개수++

                                //서버메시지는 읽음처리의 대상이 아님. 이미 읽은 것으로 저장한다
                                if(sender_id == 0 && sender_username.equals("server")){
                                    isRead = 1;

                                }else if(sender_id != user_id){ //내가 보낸 메시지가 아닐 경우
                                    //푸쉬 알람을 띄워준다
                                    //푸쉬알람 띄우는 조건
                                    // 1. 지금 해당 채팅방을 보고 있지 않으면서
                                    // 2. 서버메시지도, 내가 보낸 메시지도 아닐 경우 (남이 보낸 메시지일 경우)

                                    showNotification(sender_username, message, roomId_msg);

                                }


                                if(isImage){
                                    unreadMsgCount += number_of_files;
                                }else{
                                    unreadMsgCount += 1;
                                }
                            }


                            //메시지 저장
                            long curTime_long = System.currentTimeMillis();
                            if(isImage){ //이미지일 경우

                                //string 형태로 이어져있는 파일 이름을 그대로 저장한다
                                // : 다중이미지 1개 = 메시지 1개 원칙
                                //filename_string = 파일이름1;파일이름2;파일이름3..
                                dbHelper.insert_chatLogs(roomId_msg, sender_id, sender_username, message, filename_string, curTime_long, isRead);

                            }else if(isVideo){ //비디오일 경우

                                String video_path_server = Function.domain+"/images/"+roomId_msg+"/"+video_filename;
                                dbHelper.insert_chatLogs_with_videoServePath(roomId_msg, sender_id, sender_username, message, thumbnail_filename, curTime_long, isRead, video_path_server);

                            }else{ //텍스트 메시지일 경우
                                dbHelper.insert_chatLogs(roomId_msg, sender_id, sender_username, message, "N", curTime_long, isRead);
                            }


//                            String result_msg = dbHelper.getResult_table_chatLogs();
//                            Log.d(TAG, "chat_logs table="+result_msg);


                            //2. roomList를 업데이트한다 -- 서버메시지 제외
                            if(sender_id == 0 && sender_username.equals("server")){ }
                            else{
                                //2-1. sqlite 에서 방 정보를 불러온다
                                String roomInfo = dbHelper.get_chatRoomInfo(roomId_msg);
                                String [] roomInfo_array = roomInfo.split("/");
                                String roomName_msg = roomInfo_array[1];
                                String memberInfo = roomInfo_array[3];
                                String[] memberInfo_array = memberInfo.split(";");
                                int number_of_members_msg = memberInfo_array.length/2;


                                //2-2. 맨 위에 아이템을 추가하고, 기존 아이템을 삭제한다
                                Main_Fragment2.roomItemList.add(0, new RoomListItem(roomName_msg, number_of_members_msg,
                                        message, curTime_msg, roomId_msg, unreadMsgCount));

                                for(int i=Main_Fragment2.roomItemList.size()-1; i>0; i--){
                                    RoomListItem item = Main_Fragment2.roomItemList.get(i);
                                    if(item.getRoomId() == roomId_msg){
                                        Main_Fragment2.roomItemList.remove(i);
                                        Log.d(TAG, i+" item is removed from roomItemList");
                                    }
                                }


                                //db에 방 정보를 업데이트한다(updateTime)
                                dbHelper.room_updateTime(roomId_msg, curTime_msg);
                            }


                        }catch (Exception e){
                            //앱이 꺼져있는데, 서비스가 돌면서 roomItemList를 업데이트 하면 오류가 날 수 있다
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            Log.d(TAG,ex);
                        }


                        //사용자가 현재 채팅화면을 보고 있을 때
                        if(isChatForeground){

                            //메인화면의 roomItemList를 업데이트한다 - 위에서 완료
                            //insert 메시지는 보내지 않는다. 이 채팅화면이 종료되면, 방목록화면이 onResume() 되면서 adapter가 refresh된다

                            //지금 보고있는 채팅방 = 메시지가 발신된 채팅방일 때
                            //(이 메시지 = 이 방에서 보낸 메시지)
                            if(ChatActivity.roomId == roomId_msg){

                                //채팅 액티비티로 메시지를 전달한다
                                sendMsgToChat(line);

                                //푸쉬 알람을 띄우면 안 된다

                            }else{ //채팅방이 일치하지 않을 때
                                // (이 메시지 = 다른 방에서 보낸 메시지)

                                //채팅 화면으로 메시지를 전달해서는 안 된다

                            }

                        }else if(isMainForeground){ //사용자가 메인화면을 보고 있다면

                            //방목록 어댑터를 업데이트 하라고 메시지를 보낸다
                            sendMsgToMain("inserted/");


                            /*
                             * 1. Fragment2(방목록)을 보고 있을 때
                             * -> roomItemList에서 roomId가 동일한 것을 찾는다. 최신 메시지와 시각을 업데이트한다. 아이템 순서를 맨위로 끌어올린다
                             * -> 방목록 어댑터를 업데이트 하라고 메시지를 보낸다. sendMsgToMain("inserted/");
                             *
                             * 2. 나머지 1,3을 보고 있을 때
                             * -> 위와 동일하다. roomItemList업데이트 시켜줘야 하고
                             * fragment 이동으로는 리사이클러뷰 업데이트가 안 되므로, 동일하게 insert 메시지를 보낸다
                             * */

                        }

                        break;

                    case "new_member": //참여하던 방에 누가 초대되었다는 메시지

                        int roomId_newMember = Integer.valueOf(line_array[1]);
                        String invited_memberInfo = line_array[2];
                        String[] invited_memberInfo_array = invited_memberInfo.split(";");
                        int numberOfInvitedMembers = invited_memberInfo_array.length/2;


                        /*-------1. db 업데이트(방 정보)--------*/
                        String members_string = "";
                        String roomName_origin = "";
                        Cursor cursor = dbHelper.db.rawQuery("SELECT room_name, members FROM chat_rooms WHERE room_id='" + roomId_newMember + "';", null);
                        while (cursor.moveToNext()) {
                            roomName_origin = cursor.getString(0);
                            members_string = cursor.getString(1);
                        }

                        String[] memberInfo_array = members_string.split(";");
                        int numberOfMembers_origin = memberInfo_array.length/2; //기존 채팅 참여자 수

                        String members_updated = members_string+";"+invited_memberInfo;


                        String roomName_updated = roomName_origin; //원래 단체채팅인 경우: 기존이름(group chat이나 사용자 지정 이름)에서 변하지 않는다

                        if(numberOfMembers_origin == 2){ //원래 1대1 채팅인데 거기에 사람을 초대했다면
                            roomName_updated = "Group chat"; //방 이름 = 그룹챗

                        }else if(numberOfMembers_origin == 1 && numberOfInvitedMembers == 1){ //혼자 있는 방에 1명을 추가했다면
                            roomName_updated = invited_memberInfo_array[1]; //방 이름 = 상대방 이름

                        }else if(numberOfMembers_origin == 1 && numberOfInvitedMembers > 1){ //혼자 있는 방에 여러 명을 추가했다면
                            roomName_updated = "Group chat"; //방 이름 = 그룹챗
                        }

                        Log.d("초대", "MainService) db에 저장하기 직전. roomName_updated="+roomName_updated);
                        //db 업데이트(방이름, 멤버정보)
                        dbHelper.db.execSQL("UPDATE chat_rooms SET room_name = '"+roomName_updated+"', members='" + members_updated + "' WHERE room_id='" + roomId_newMember + "';");

                        //방 정보가 잘 업데이트 되었는지 확인
                        dbHelper.get_chatRoomInfo(roomId_newMember);


                        /*-------2. 방 목록 화면 업데이트--------*/
                        //방목록 아이템을 업데이트한다(roomName, 인원)

                        int total_numberOfMembers = numberOfMembers_origin+numberOfInvitedMembers;
                        try{
                            for(RoomListItem room : Main_Fragment2.roomItemList){
                                if(room.getRoomId() == roomId_newMember){

                                    room.setRoomName(roomName_updated);
                                    room.setNumberOfMembers(total_numberOfMembers);
                                }
                            }
                        }catch (Exception e){
                            //앱이 꺼져있는데, 서비스가 돌면서 roomItemList를 업데이트 하면 오류가 날 수 있다
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            Log.d(TAG,ex);
                        }




                        //사용자가 현재 채팅화면을 보고 있을 때
                        if(isChatForeground){

                            //메인화면의 roomItemList를 업데이트한다 - 위에서 완료
                            //insert 메시지는 보내지 않는다. 이 채팅화면이 종료되면, 방목록화면이 onResume() 되면서 adapter가 refresh된다


                            //지금 보고있는 채팅방 = 메시지가 발신된 채팅방일 때
                            //(이 메시지 = 이 방에서 보낸 메시지)
                            if(ChatActivity.roomId == roomId_newMember){
                                /*-------3. 채팅화면 제목 업데이트--------*/
                                /*-------4. 채팅화면의 멤버목록 업데이트--------*/


                                //ChatActivity에 방 정보를 전달한다
                                String msg_roomInfo = "";
                                if(total_numberOfMembers == 2){ //1대1 채팅의 경우: 방 인원을 발송하지 않는다
                                    msg_roomInfo = "roomInfo_plus/"+roomName_updated+"/"+invited_memberInfo;
                                }else{
                                    msg_roomInfo = "roomInfo_plus/"+roomName_updated+" ("+total_numberOfMembers+")"+"/"+invited_memberInfo;
                                }
                                Log.d("초대", "MainService) 채팅화면으로 roomInfo 보내기 직전. roomInfo_plus:"+msg_roomInfo);
                                sendMsgToChat(msg_roomInfo);


                            }else{ //채팅방이 일치하지 않을 때
                                // (이 메시지 = 다른 방과 관련된 메시지)

                                //채팅 화면으로 메시지를 전달해서는 안 된다
                            }

                        }else if(isMainForeground){ //사용자가 메인화면을 보고 있다면

                            //방목록 어댑터를 업데이트 하라고 메시지를 보낸다
                            sendMsgToMain("inserted/");

                        }



                        break;

                    case "member_out": //참여하던 방에서 누가 나갔다는 메시지
                        //member_out/방id/떠난사람 id

                        int out_roomId = Integer.valueOf(line_array[1]);
                        int out_memberId = Integer.valueOf(line_array[2]);

                        Log.d("방나감", "누군가 방을 나갔다고 함. 나간사람 id = "+out_memberId+" / 방 id = "+out_roomId);

                        /*-------1. db 업데이트(방 정보)--------*/
                        String out_members_string = "";
                        String out_roomName_origin = "";
                        Cursor out_cursor = dbHelper.db.rawQuery("SELECT room_name, members FROM chat_rooms WHERE room_id='" + out_roomId + "';", null);
                        while (out_cursor.moveToNext()) {
                            out_roomName_origin = out_cursor.getString(0);
                            out_members_string = out_cursor.getString(1);
                        }

                        String[] out_memberInfo_array = out_members_string.split(";");
                        int out_numberOfMembers_origin = out_memberInfo_array.length/2; //기존 채팅 참여자 수

                        //원래 있던 사용자를 멤버정보에서 삭제해야함 (id로 찾음 -> id와 닉넴을 동시 삭제)
                        int index = 10000;
                        for(int i=0; i<out_memberInfo_array.length; i++){

                            if(i%2==0){ //참여자 목록에서 나간사람의 id를 찾는다
                                if(Integer.valueOf(out_memberInfo_array[i]) == out_memberId){
                                    index = i;
                                    break;
                                }
                            }
                        }

                        String out_members_updated = "";
                        for(int i=0; i<out_memberInfo_array.length; i++){

                            if(i==index || i==index+1){ }else{
                                out_members_updated += out_memberInfo_array[i]+";";
                            }
                        }

                        //마지막 ';' 제거
                        out_members_updated = out_members_updated.substring(0, out_members_updated.length()-1);

                        Log.d("방나감", "memberInfo 업데이트: "+out_members_string+" -> "+out_members_updated);


                        String out_roomName_updated = out_roomName_origin; //원래 단체채팅인 경우: 기존이름(group chat이나 사용자 지정 이름)에서 변하지 않는다

                        if(out_numberOfMembers_origin == 2){ //원래 1대1 채팅인데 거기서 상대방이 나갔다면 -> 현재 1명
                            out_roomName_updated = "No users"; //방 이름 = No users
                        }else if(out_numberOfMembers_origin == 3){ //3명짜리 단체채팅에서 한 명 나간 상황 -> 현재 2명

                            //이름을 지정한 적이 있다면 그 이름 그대로 남겨둠

                            //지정한 적 없을 경우 -> 채팅방 이름을 상대방의 이름으로 변경
                            if(out_roomName_origin.equals("Group chat")){

                                String[] out_memberInfo_updated = out_members_updated.split(";");
                                for(int i=0; i<out_memberInfo_updated.length; i++){

                                    if(i%2==1){ //사용자 2명중에서, 자신의 이름과 다른 이름 = 상대방 이름 = 방 이름
                                        if(!out_memberInfo_updated[i].equals(username)){
                                            out_roomName_updated = out_memberInfo_updated[i];
                                            break;
                                        }
                                    }
                                }

                            }
                        }

                        Log.d("방나감", "db에 저장하기 직전. roomName: "+out_roomName_origin+" -> "+out_roomName_updated);

                        //db 업데이트(방이름, 멤버정보)
                        dbHelper.db.execSQL("UPDATE chat_rooms SET room_name = '"+out_roomName_updated+"', members='" + out_members_updated + "' WHERE room_id='" + out_roomId + "';");

                        //방 정보가 잘 업데이트 되었는지 확인
                        dbHelper.get_chatRoomInfo(out_roomId);


                        /*-------2. 방 목록 화면 업데이트--------*/
                        //방목록 아이템을 업데이트한다(roomName, 인원)

                        int out_total_numberOfMembers = out_numberOfMembers_origin - 1;
                        try{
                            for(RoomListItem room : Main_Fragment2.roomItemList){
                                if(room.getRoomId() == out_roomId){

                                    room.setRoomName(out_roomName_updated);
                                    //방 인원이 3명 미만이면, 화면에 보여주지 않음 - RoomListAdapter에서 처리
                                    room.setNumberOfMembers(out_total_numberOfMembers);
                                }
                            }
                        }catch (Exception e){
                            //앱이 꺼져있는데, 서비스가 돌면서 roomItemList를 업데이트 하면 오류가 날 수 있다
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            Log.d(TAG,ex);
                        }



                        //사용자가 현재 채팅화면을 보고 있을 때
                        if(isChatForeground){

                            //메인화면의 roomItemList를 업데이트한다 - 위에서 완료
                            //insert 메시지는 보내지 않는다. 이 채팅화면이 종료되면, 방목록화면이 onResume() 되면서 adapter가 refresh된다


                            //지금 보고있는 채팅방 = 메시지가 발신된 채팅방일 때
                            //(이 메시지 = 이 방에서 보낸 메시지)
                            if(ChatActivity.roomId == out_roomId){
                                /*-------3. 채팅화면 제목 업데이트--------*/
                                /*-------4. 채팅화면의 멤버목록 업데이트--------*/


                                //ChatActivity에 방 정보를 전달한다
                                String out_msg_roomInfo = "";
                                if(out_total_numberOfMembers <= 2){ //1대1 채팅, 혼자방의 경우: 방 인원을 발송하지 않는다

                                    //roomInfo_minus/방이름/나간사람id
                                    out_msg_roomInfo = "roomInfo_minus/"+out_roomName_updated+"/"+out_memberId;
                                }else{
                                    //roomInfo_minus/방이름 (인원)/나간사람id
                                    out_msg_roomInfo = "roomInfo_minus/"+out_roomName_updated+" ("+out_total_numberOfMembers+")"+"/"+out_memberId;
                                }
                                Log.d("방나감", "MainService) 채팅화면으로 roomInfo 보내기 직전. roomInfo_plus:"+out_msg_roomInfo);
                                sendMsgToChat(out_msg_roomInfo);


                            }else{ //채팅방이 일치하지 않을 때
                                // (이 메시지 = 다른 방과 관련된 메시지)

                                //채팅 화면으로 메시지를 전달해서는 안 된다
                            }

                        }else if(isMainForeground){ //사용자가 메인화면을 보고 있다면

                            //방목록 어댑터를 업데이트 하라고 메시지를 보낸다
                            sendMsgToMain("inserted/");

                        }



                        break;

                    case "invited": //이 사용자가 어떤 방에 초대되었다는 메시지
                        //invited/방id/방이름/memberInfo

                        int roomId_invited = Integer.valueOf(line_array[1]);
                        String roomName_invited = line_array[2];
                        String allMemberInfo_string = line_array[3];

                        String[] allMemberInfo_array = allMemberInfo_string.split(";");
                        int numberOfAllMembers = allMemberInfo_array.length/2;

                        Log.d(TAG, "the client is invited to "+roomName_invited);

                        //방 정보를 내부 저장소에 저장한다
                        String currentTime_invited = Function.getCurrentTime();
                        //서버가 정해준 room id를 직접 저장해야 한다. auto increment(x)
                        dbHelper.insert_chatRooms(roomId_invited, roomName_invited, allMemberInfo_string, currentTime_invited);


                        //사용자의 foreground activity는 불명. 확인해야 한다
                        try{
                            // 방목록에 아이템을 추가한다
                            // Fragment2의 onResume()에서 datasetChanged()가 호출된다
                            Main_Fragment2.roomItemList.add(0, new RoomListItem(roomName_invited, numberOfAllMembers,
                                    "", currentTime_invited, roomId_invited, 0));
                        }catch (Exception e){
                            //앱이 꺼져있는데 서비스만 돌고 있을 경우, 오류가 날 수 있다
                            StringWriter sw = new StringWriter();
                            e.printStackTrace(new PrintWriter(sw));
                            String ex = sw.toString();

                            Log.d(TAG,ex);
                        }

                        if(isMainForeground){
                            Log.d(TAG, "MainActivity is at foreground");

                            //방목록 어댑터를 업데이트 하라고 메시지를 보낸다
                            sendMsgToMain("inserted/");
                        }

                        //채팅 화면이 foreground에 있다면, 다른 방을 보고 있는 것이다

                        break;
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


    public void showNotification(String sender_username, String message, int roomId){

        Function.getAllPrefData(getApplicationContext());
        try{

            if(Function.getBoolean(getApplicationContext(), "alarm")){

                createChannel(this);

                NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this,"channelID")
                        .setContentTitle("Canaria")
                        .setContentText(sender_username +" : "+message)
                        .setSmallIcon(R.drawable.bird_icon)
                        .setAutoCancel(true);

                Intent openThePageIntent = new Intent(this, ChatActivity.class);
                openThePageIntent.putExtra("isNewRoom", "N"); //기존 방에 입장한다는 표시
                openThePageIntent.putExtra("roomId", roomId);
                openThePageIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pendingIntent = PendingIntent.getActivity(this, (int)(System.currentTimeMillis()/1000), openThePageIntent, PendingIntent.FLAG_ONE_SHOT);

                notificationBuilder.setContentIntent(pendingIntent);

                int notificationId = (int)(System.currentTimeMillis()/1000);
                NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.notify(notificationId, notificationBuilder.build());
            }
        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d(TAG, ex);
        }


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
