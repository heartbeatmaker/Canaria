package com.android.canaria;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.canaria.connect_to_server.MainService;
import com.android.canaria.recyclerView.MessageAdapter;
import com.android.canaria.recyclerView.MessageItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {
    TextView roomInfo_textView;
    EditText msgInput_editText;
    Button sendMsg_btn;

    int userId;
    String username, roomName;
    public static int roomId;

    String TAG = "tag "+this.getClass().getSimpleName();

    //리사이클러뷰 변수
    RecyclerView rcv;
    ArrayList<MessageItem> messageItemList;
    MessageAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    Handler handler;
    boolean isNewRoom = false;

    int msg_sender_id;
    String msg_sender_username;
    String msg_text;

    String[] message_array;

    String myMsg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        handler = new Handler();

        Log.d(TAG, "onCreate");

        roomInfo_textView = (TextView)findViewById(R.id.chat_roomInfo_textView);
        msgInput_editText = (EditText) findViewById(R.id.chat_message_editText);
        sendMsg_btn = (Button)findViewById(R.id.chat_send_btn);


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

            // 방 정보를 가져온다
            roomName = getIntent().getStringExtra("roomName");
            roomId = getIntent().getIntExtra("roomId", 10000);

            //서비스에 메시지 전달
            sendMsg("return/"+roomId+"/"+roomName);

            //저장된 메시지를 가져온다
            
        }


        //리사이클러뷰 초기화
        rcv = (RecyclerView)findViewById(R.id.chat_message_rcv);
        linearLayoutManager = new LinearLayoutManager(this);
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        messageItemList = new ArrayList<>();
        adapter = new MessageAdapter(messageItemList, this);
        rcv.setAdapter(adapter);


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

                    handler.post(new Runnable() {
                        @Override
                        public void run() {

                            roomInfo_textView.setText(roomInfo_atTitleBar);
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
                                if(msg_sender_id == 0 && msg_sender_username.equals("server")){ //서버 메시지
                                    Log.d(TAG, "adding server message..");
                                    messageItemList.add(new MessageItem("", msg_text));

                                }else if(msg_sender_id == userId){//내가 보낸 메시지일때
                                    messageItemList.add(new MessageItem("[Me]", msg_text));

                                }else{//다른 사람이 보낸 메시지
                                    messageItemList.add(new MessageItem(msg_sender_username, msg_text));
                                }
                                adapter.notifyItemInserted(adapter.getItemCount()-1);
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

}