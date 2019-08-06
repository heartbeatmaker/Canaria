package com.android.canaria;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.canaria.connect_to_server.MainService;
import com.android.canaria.db.DBHelper;
import com.android.canaria.recyclerView.FriendListAdapter;
import com.android.canaria.recyclerView.FriendListItem;
import com.android.canaria.recyclerView.MessageAdapter;
import com.android.canaria.recyclerView.MessageItem;
import com.android.canaria.recyclerView.RoomListItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    EditText msgInput_editText;
    Button sendMsg_btn;

    int userId;
    String username, roomName;
    public static int roomId;

    ActionBar actionBar;
    MenuItem menuItem;

    String TAG = "tag "+this.getClass().getSimpleName();

    //메시지 리사이클러뷰 변수
    RecyclerView rcv;
    ArrayList<MessageItem> messageItemList;
    MessageAdapter adapter;
    LinearLayoutManager linearLayoutManager;


    //참여자 목록 리사이클러뷰
    RecyclerView members_rcv;
    ArrayList<FriendListItem> memberList;
    FriendListAdapter members_adapter;
    LinearLayoutManager members_linearLayoutManager;

    DrawerLayout drawer;

    Handler handler;
    boolean isNewRoom = false;

    int msg_sender_id;
    String msg_sender_username;
    String msg_text;

    String[] message_array;

    String myMsg;
    private static final int INVITATION_REQUEST = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        handler = new Handler();

        Log.d(TAG, "onCreate");

        msgInput_editText = (EditText) findViewById(R.id.chat_message_editText);
        sendMsg_btn = (Button)findViewById(R.id.chat_send_btn);

        //메시지 리사이클러뷰 초기화
        rcv = (RecyclerView)findViewById(R.id.chat_message_rcv);
        linearLayoutManager = new LinearLayoutManager(this);
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        messageItemList = new ArrayList<>();
        adapter = new MessageAdapter(messageItemList, this);
        rcv.setAdapter(adapter);


        //(drawerLayout에 띄워주는) 참여자 리사이클러뷰 초기화
        members_rcv = (RecyclerView)findViewById(R.id.chatRoom_members_rcv);
        members_linearLayoutManager = new LinearLayoutManager(this);
        members_rcv.setHasFixedSize(true);
        members_rcv.setLayoutManager(members_linearLayoutManager);
        memberList = new ArrayList<>();
        members_adapter = new FriendListAdapter(memberList, this);
        members_rcv.setAdapter(members_adapter);


        actionBar = getSupportActionBar();
        actionBar.show();


        //현재 사용자의 정보를 가져온다
        userId = Integer.valueOf(Function.getString(getApplicationContext(), "user_id"));
        username = Function.getString(getApplicationContext(), "username");


        try{
            String isNewRoom_string = getIntent().getStringExtra("isNewRoom");
            if(isNewRoom_string.equals("Y")){
                isNewRoom = true;
            }else{
                isNewRoom = false;
            }
        }catch (Exception e){
            Log.d(TAG, "intent error");
            e.printStackTrace();
        }


        if(isNewRoom){ //새로 만든 방이라면
            //이전 액티비티에서 전달한 '친구 정보'를 받아온다
            Log.d(TAG, "is new room");

            String friendInfo = getIntent().getStringExtra("friendInfo_jsonArray");

            //java 서버에서 json을 못 읽는다. 참여자들 정보를 한 문장으로 구성한다
            //친구1id;친구1username;친구2id;친구2username..
            String friendInfo_string = "";
            try{
                JSONArray jsonArray = new JSONArray(friendInfo);
                for(int i=0; i<jsonArray.length(); i++){
                    JSONObject friendObject = new JSONObject();
                    friendObject = (JSONObject)jsonArray.get(i);

                    String friend_id = friendObject.getString("id");
                    String friend_username = friendObject.getString("username");

                    if(i == 0){
                        friendInfo_string = friend_id + ";" +friend_username;
                    }else{
                        friendInfo_string += ";"+friend_id + ";" +friend_username;
                    }
                }

                //서비스에 메시지 전달
                sendMsg("new_room/"+friendInfo_string);

            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);
            }


        }else{ //기존에 참여하던 방에 다시 들어온 것이라면

            roomId = getIntent().getIntExtra("roomId", 10000); // 이전 화면에서 전달한 방 id

            //저장된 메시지를 가져와서 화면에 띄워준다
            getSavedMsg(roomId);

        }



        //메시지 보내기 버튼을 클릭하면 -> 서비스로 메시지를 보낸다
        sendMsg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");

                String message_input = msgInput_editText.getText().toString();
                sendMsg("msg/"+roomId+"/"+message_input);
                msgInput_editText.setText("");
            }
        });


        //친구 초대 버튼
        Button invite_btn = (Button)findViewById(R.id.chatRoom_invite_btn);
        invite_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String existing_members_string = "";

                for(int i=0; i<memberList.size(); i++){
                    existing_members_string += memberList.get(i).getFriendId() + ";";
                }

                existing_members_string = existing_members_string.substring(0, existing_members_string.length()-1);

                //초대할 친구를 고르는 화면으로 넘어간다
                //현재 참여자는 목록에 보여주면 안되기 때문에, 현재 참여자들의 id를 인텐트로 보낸다
                Intent intent_select_friends = new Intent(getApplicationContext(), SelectFriendsActivity.class);
                intent_select_friends.putExtra("invitation", "Y");
                intent_select_friends.putExtra("existing_members", existing_members_string);
                Log.d("invitation", "invite 버튼 누름. existing_members="+existing_members_string);
                startActivityForResult(intent_select_friends, INVITATION_REQUEST);

                drawer.closeDrawer(GravityCompat.END);


            }
        });


    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == INVITATION_REQUEST && resultCode == RESULT_OK){

            Log.d("invitation", "onActivityForResult");

            String friendInfo = data.getStringExtra("friendInfo_jsonArray");

            //java 서버에서 json을 못 읽는다. 참여자들 정보를 한 문장으로 구성한다
            //친구1id;친구1username;친구2id;친구2username..
            String friendInfo_string = "";
            try{
                JSONArray jsonArray = new JSONArray(friendInfo);
                for(int i=0; i<jsonArray.length(); i++){
                    JSONObject friendObject = new JSONObject();
                    friendObject = (JSONObject)jsonArray.get(i);

                    String friend_id = friendObject.getString("id");
                    String friend_username = friendObject.getString("username");

                    if(i == 0){
                        friendInfo_string = friend_id + ";" +friend_username;
                    }else{
                        friendInfo_string += ";"+friend_id + ";" +friend_username;
                    }
                }

                Log.d("invitation", "초대할 친구 info = "+friendInfo_string);

                //서비스에 메시지 전달
                sendMsg("invite/"+roomId+"/"+friendInfo_string);

            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);
            }

        }
    }



    //이 방의 참여자 정보를 가져와서 화면에 띄워주는 메소드
    private void getMemberInfo(int roomId){

        Log.d("초대", "getMemberInfo");

        /*-------------방 정보를 가져온다--------------*/
        //db 연결
        DBHelper dbHelper = new DBHelper(getApplicationContext(), Function.dbName, null, Function.dbVersion);
        dbHelper.open();

        Cursor _cursor = dbHelper.db.rawQuery("SELECT members FROM chat_rooms WHERE room_id='" + roomId + "';", null);
        _cursor.moveToFirst();
        String memberInfo_string = _cursor.getString(0);
        /*-------------------------------------------*/
        Log.d("초대", "memberInfo_string="+memberInfo_string);

        String[] memberInfo_array = memberInfo_string.split(";");

        int member_id = 0;
        String member_username = "";
        for(int i=0; i<memberInfo_array.length; i++){

            if(i%2 == 0){ //i가 짝수일 때: id
                member_id = Integer.valueOf(memberInfo_array[i]);
            }else{ //i가 홀수일 때: username
                member_username = memberInfo_array[i];
                Log.d("초대", "리스트에 추가 직전. id:"+member_id+", username:"+member_username);
                memberList.add(new FriendListItem(member_username, member_id));
            }
        }
        members_rcv.scrollToPosition(0);
    }



    //저장된 메시지를 가져와서 화면에 띄워주는 메소드
    private void getSavedMsg(int roomId){

        /*-------------방 정보를 가져온다--------------*/
        //db 연결
        DBHelper dbHelper = new DBHelper(getApplicationContext(), Function.dbName, null, Function.dbVersion);
        dbHelper.open();

        Cursor _cursor = dbHelper.db.rawQuery("SELECT room_name FROM chat_rooms WHERE room_id='" + roomId + "';", null);
        _cursor.moveToFirst();
        String roomName = _cursor.getString(0);
        /*-------------------------------------------*/


        //서비스에 메시지 전달
        sendMsg("return/"+roomId+"/"+roomName);


        try{

            Cursor cursor0 = dbHelper.db.rawQuery("SELECT count(*) FROM chat_logs WHERE room_id='" + roomId + "' AND isRead=1;", null);
            cursor0.moveToFirst();
            int readMsgCount = cursor0.getInt(0);
            //안 읽은 메시지가 몇 개인지 확인한다
            Cursor cursor = dbHelper.db.rawQuery("SELECT count(*) FROM chat_logs WHERE room_id='" + roomId + "' AND isRead=0;", null);
            cursor.moveToFirst();
            int unreadMsgCount = cursor.getInt(0);

            Log.d(TAG, "unread message count="+unreadMsgCount);


            boolean isFirstUnreadMsg = true;
            int focus_index = 0;
            //저장된 메시지를 오래된 순서대로 화면에 띄워준다
            //채팅내용 테이블: id, 방id, 보낸사람 id, 보낸사람 username, 메시지내용, 보낸시각
            Cursor cursor2 = dbHelper.db.rawQuery("SELECT * FROM chat_logs WHERE room_id='" + roomId + "' ORDER BY time;", null);
            while (cursor2.moveToNext()) {

                int message_id = cursor2.getInt(0);
                int sender_id = cursor2.getInt(2);
                String sender_username = cursor2.getString(3);
                String message = cursor2.getString(4);
                long time = Long.valueOf(cursor2.getString(5));
                int isRead = cursor2.getInt(6);

                    Log.d(TAG,"id: "+message_id+" / sender id: "+sender_id+" / sender_name : "+sender_username
                            +" / message: "+message+" / time: "+time+" / isRead: "+isRead);


                if(unreadMsgCount > 0){ //안읽은 메시지가 있을 때
                    if(isRead == 0){ //안읽은 메시지 중에서
                        if(isFirstUnreadMsg){ //가장 오래된 메시지일 때

                            Log.d(TAG, "가장 오래된 안읽은 메시지임");

                            isFirstUnreadMsg = false;


                            if(unreadMsgCount>10){ //안읽은 메시지가 10개 초과: 안읽은 메시지가 한 화면을 넘길 때
                                focus_index = messageItemList.size(); //안읽은 메시지 중 가장 오래된 메시지에 스크롤 focus를 맞춘다

                                if(readMsgCount > 10){//읽은 메시지가 10개 초과일때
                                    //"여기서부터 안 읽었다"라고 메시지 위에 표시해준다
                                    messageItemList.add(new MessageItem(0, "server", "You haven't read messages from here.", time));
                                    messageItemList.add(new MessageItem(sender_id, sender_username,message, time));

                                }else{ //읽은 메시지가 10개 이하일 때(주고받은 메시지 자체가 적을 때)
                                    //안읽음 표시를 하지 않는다
                                    messageItemList.add(new MessageItem(sender_id, sender_username,message, time));
                                }
                            }else{ //안읽은 메시지 개수가 10개 이하일때
                                messageItemList.add(new MessageItem(sender_id, sender_username,message, time));
                            }

                        }else{ //나머지 안읽은 메시지 -> 메시지를 화면에 표시한다
                            messageItemList.add(new MessageItem(sender_id, sender_username,message, time));
                        }

                    }else{ //이미 읽은 메시지일 때 -> 메시지를 화면에 표시한다
                        messageItemList.add(new MessageItem(sender_id, sender_username,message, time));
                    }
                }else{ //안읽은 메시지가 없을 때 -> 메시지를 화면에 표시한다

                    messageItemList.add(new MessageItem(sender_id, sender_username,message, time));
                }


                /*
                 * 페이징 해야함(최근 메시지 nn개씩 가져오기. 어디까지 가져왔는지 메시지 id를 변수에 넣어놓기)
                 * */
            }

            if(focus_index == 0){
                focus_index = messageItemList.size()-1;
            }

            rcv.scrollToPosition(focus_index);



            if(unreadMsgCount > 0){
                //안 읽은 메시지가 있다면 -> 이것을 읽은 메시지로 db를 업데이트한다
                Cursor cursor3 = dbHelper.db.rawQuery("SELECT id FROM chat_logs WHERE room_id='" + roomId + "' AND isRead=0;", null);
                while (cursor3.moveToNext()) {

                    int message_id = cursor3.getInt(0);
                    dbHelper.db.execSQL("UPDATE chat_logs SET isRead=1 WHERE id='" + message_id + "';");
                }


                //방목록 아이템을 업데이트한다(안읽은 메시지 개수를 0으로 바꾼다)
                for(RoomListItem room : Main_Fragment2.roomItemList){
                    if(room.getRoomId() == roomId){
                        room.setUnreadMsgCount(0);
                    }
                }

            }


        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d(TAG,ex);
        }

    }



    //사용자가 a 채팅방에서 채팅을 하고 있는데, b 채팅방에서 메시지가 온다(푸쉬 알람으로 뜸) 이것을 클릭할 경우, b 채팅방으로 넘어가야 한다
    //= 채팅 화면이 켜 있는 상태로, 기존 채팅방 정보를 지우고 다른 채팅방 데이터를 띄워줘야 하는 상황
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        Log.d("intent", "@@@@@@@@@@@onNewIntent");

        //변수 초기화(메시지 목록, 참여자 목록)
        messageItemList.clear();
        adapter.notifyDataSetChanged();
        isNewRoom = false;
        memberList.clear();
        members_adapter.notifyDataSetChanged();

        roomId = intent.getIntExtra("roomId", 10000); // 이전 화면에서 전달한 방 id
        Log.d("intent", "roomid="+roomId);

        //바뀐 채팅방의 메시지 데이터를 띄워준다
        getSavedMsg(roomId);
        adapter.notifyDataSetChanged();

    }


    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("chat_event"));
    }


    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
    }


    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "Receiver) message received: " + message);

            message_array = message.split("/");
            String signal = message_array[0];

            switch (signal){
                case "room_created": //방이 최초로 만들어졌을 때, 방 id를 전달받는다
                    roomId = Integer.valueOf(message_array[1]);

                    break;
                case "roomInfo": //방 최초생성 시 or 기존 방에 들어왔을 때, 방 정보를 전달받는다

                    final String roomInfo_atTitleBar = message_array[1];
                    String memberInfo_string = message_array[2]; //참여자 정보(id;username;id;username..형식). 나중에 drawerLayout에 띄워줄 것

                    String[] memberInfo_array = memberInfo_string.split(";");


                    //피초대인들의 id와 username을 따로 모아서 arrayList에 저장한다
                    final ArrayList<Integer> memberId_list = new ArrayList<>();
                    final ArrayList<String > memberUsername_list = new ArrayList<>();

                    int member_id = 10000;
                    String member_username = "";

                    for(int i=0; i<memberInfo_array.length; i++){
                        if(i%2==0){
                            member_id = Integer.valueOf(memberInfo_array[i]);
                            memberId_list.add(member_id);
                        }else{
                                member_username = memberInfo_array[i];
                                memberUsername_list.add(member_username);
                        }
                    }


                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            for(int k=0; k<memberId_list.size(); k++){
                                String member_username = memberUsername_list.get(k);
                                int member_id = memberId_list.get(k);

                                memberList.add(0, new FriendListItem(member_username, member_id));
                            }
                            members_adapter.notifyDataSetChanged();


                            actionBar.setTitle(roomInfo_atTitleBar);
//                            roomInfo_textView.setText(roomInfo_atTitleBar);
                        }
                    });
                    break;

                case "roomInfo_plus": //채팅 참여자가 추가되었을 때

                    final String roomInfoPlus_atTitleBar = message_array[1];
                    String invited_memberInfo_string = message_array[2]; //참여자 정보(id;username;id;username..형식). 나중에 drawerLayout에 띄워줄 것

                    String[] invited_memberInfo_array = invited_memberInfo_string.split(";");


                    //피초대인들의 id와 username을 따로 모아서 arrayList에 저장한다
                    final ArrayList<Integer> invited_memberId_list = new ArrayList<>();
                    final ArrayList<String > invited_memberUsername_list = new ArrayList<>();

                    int invited_member_id = 10000;
                    String invited_member_username = "";

                    for(int i=0; i<invited_memberInfo_array.length; i++){
                        if(i%2==0){
                            invited_member_id = Integer.valueOf(invited_memberInfo_array[i]);
                            invited_memberId_list.add(invited_member_id);
                        }else{
                            invited_member_username = invited_memberInfo_array[i];
                            invited_memberUsername_list.add(invited_member_username);
                        }
                    }


                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            for(int k=0; k<invited_memberId_list.size(); k++){
                                String member_username = invited_memberUsername_list.get(k);
                                int member_id = invited_memberId_list.get(k);

                                memberList.add(new FriendListItem(member_username, member_id));
                            }
                            members_adapter.notifyDataSetChanged();

                            actionBar.setTitle(roomInfoPlus_atTitleBar);
                        }
                    });

                    break;

                case "msg":
//                    msg/roomId/sender_id/sender_username/message

                    int roomId_msg = Integer.valueOf(message_array[1]);
                    msg_sender_id = Integer.valueOf(message_array[2]);
                    msg_sender_username = message_array[3];
                    msg_text = message_array[4];

                    if(roomId_msg == roomId){ //이 예외처리는 이미 서비스에서 했음. 재확인용

                        handler.post(new Runnable() {
                            @Override
                            public void run() {

                                //서버에서 보낸 알림 메시지와 일반 사용자가 보낸 메시지를 구분한다
//                                if(msg_sender_id == 0 && msg_sender_username.equals("server")){ //서버 메시지
//                                    Log.d(TAG, "adding server message..");
//                                    messageItemList.add(new MessageItem("", msg_text));
//
//                                }else if(msg_sender_id == userId){//내가 보낸 메시지일때
//                                    messageItemList.add(new MessageItem("[Me]", msg_text));
//
//                                }else{//다른 사람이 보낸 메시지
//                                    messageItemList.add(new MessageItem(msg_sender_username, msg_text));
//                                }
                                messageItemList.add(new MessageItem(msg_sender_id, msg_sender_username, msg_text, System.currentTimeMillis()));
                                adapter.notifyItemInserted(adapter.getItemCount()-1);
                                rcv.scrollToPosition(messageItemList.size()-1);
                            }
                        });
                    }

                    break;

            }


        }
    };




    void sendMsg(String msg){
        Intent intent = new Intent(getApplicationContext(), MainService.class);
        intent.putExtra("message", msg);

        startService(intent);
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.actionbar_chatroom_actions, menu);

        menuItem = menu.findItem(R.id.chatRoom_action_menu);

        return super.onCreateOptionsMenu(menu);
    }



    @SuppressLint("NewApi")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()){
            case R.id.chatRoom_action_menu: //'메뉴보기' 옵션을 눌렀을 때 -> drawer layout 을 열고 닫음

                View drawerView = (View)findViewById(R.id.chatRoom_drawer_relativeLayout);
                drawer = (DrawerLayout)findViewById(R.id.chatRoom_drawerLayout);

                if(drawer.isDrawerOpen(GravityCompat.END)){ //닫기
                    drawer.closeDrawer(GravityCompat.END);
                }else{
                    drawer.openDrawer(drawerView); //열기
                }

                break;
        }

        return super.onOptionsItemSelected(item);
    }


}
