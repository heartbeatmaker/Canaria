package com.android.canaria;

import android.content.ContentValues;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.android.canaria.connect_to_server.HttpRequest;
import com.android.canaria.recyclerView.MessageAdapter;
import com.android.canaria.recyclerView.MessageItem;
import com.android.canaria.recyclerView.RoomListItem;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;

public class ChatRoomActivity extends AppCompatActivity {

    TextView roomInfo_textView;
    EditText msgInput_editText;
    Button sendMsg_btn;

    String userId, username, roomName;
    int roomId;

    String TAG = "tag "+this.getClass().getSimpleName();


    //클라이언트 소켓 관련 변수
    public static final String ServerIP = "54.180.107.44";
    Socket socket;
    BufferedWriter bufferedWriter;
    BufferedReader bufferedReader;


    //리사이클러뷰 변수
    RecyclerView rcv;
    ArrayList<MessageItem> messageItemList;
    MessageAdapter adapter;
    LinearLayoutManager linearLayoutManager;


    Handler handler = new Handler();

    ClientSocket clientSocketThread;

    boolean isNewRoom = false;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        Log.d(TAG, "onCreate");

        roomInfo_textView = (TextView)findViewById(R.id.chat_roomInfo_textView);
        msgInput_editText = (EditText) findViewById(R.id.chat_message_editText);
        sendMsg_btn = (Button)findViewById(R.id.chat_send_btn);


        //현재 사용자의 정보를 가져온다
        userId = Function.getString(getApplicationContext(), "user_id");
        username = Function.getString(getApplicationContext(), "username");


        try{
            String isNewRoom_string = getIntent().getStringExtra("isNewRoom");
            if(isNewRoom_string != null && isNewRoom_string.length() > 0){
                isNewRoom = true;
            }
        }catch (Exception e){
            Log.d(TAG, "intent error");
            e.printStackTrace();
        }


        if(isNewRoom){ //새로 만든 방이라면
            //이전 액티비티에서 전달한 '친구 정보'를 받아온다
            Log.d(TAG, "is new room");

            String friendInfo_string = getIntent().getStringExtra("friendInfo_jsonArray");

            //클라이언트 소켓 연결
            clientSocketThread = new ClientSocket(isNewRoom, friendInfo_string);
            clientSocketThread.start();

            try{
                JSONArray friendInfo_jsonArray = new JSONArray(friendInfo_string);
                Log.d(TAG, "jsonArray="+friendInfo_jsonArray);

            }catch (Exception e){
                Log.d(TAG, "jsonArray parsing error");
                e.printStackTrace();
            }

        }else{ //기존에 참여하던 방에 다시 들어온 것이라면

            // 방 정보를 가져온다
            roomName = getIntent().getStringExtra("roomName");
            roomId = getIntent().getIntExtra("roomId", 10000);

            //클라이언트 소켓 연결
            //기존 방에 다시 들어갈 때는, friendInfo가 필요 없다
            clientSocketThread = new ClientSocket(isNewRoom, "not needed");
            clientSocketThread.start();
        }


        //리사이클러뷰 초기화
        rcv = (RecyclerView)findViewById(R.id.chat_message_rcv);
        linearLayoutManager = new LinearLayoutManager(this);
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        messageItemList = new ArrayList<>();
        adapter = new MessageAdapter(messageItemList, this);
        rcv.setAdapter(adapter);


        //메시지 보내기 버튼을 클릭하면 -> 서버로 메시지를 보낸다
        sendMsg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");

                String message_input = msgInput_editText.getText().toString();
                sendMsg("msg/"+message_input);
                msgInput_editText.setText("");
            }
        });


    }



    // 서버에 메시지 보내기
    // 네트워크에 연결할 때는 서브 쓰레드를 이용해야 한다
    void sendMsg(final String msg){

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


    //이 액티비티가 종료되면 -> 소켓 종료 + 소켓 담당 쓰레드 중지
    @Override
    protected void onDestroy(){
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try{
            if(socket != null){
//                sendMsg("closeRoom");
                socket.close();
            }
            clientSocketThread.quit();
        }catch (Exception e){
            Log.d(TAG, "socket closing error: "+e);
        }

    }



    class ClientSocket extends Thread{

        boolean isSocketAlive = true;
        boolean isNewRoom;
        String friendInfo;

        public ClientSocket(boolean isNewRoom, String friendInfo){
            this.isNewRoom = isNewRoom;
            this.friendInfo = friendInfo;
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

                if(isNewRoom){ //새로운 방을 개설할 때

                    String friendInfo_string = "";

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

                    // 개설자(=현 사용자) 정보/방 참여자 정보(jsonArray)
                    sendMsg("new_room/" + userId + "/" +username+"/"+friendInfo_string);

                }else{ //기존 방에 입장할 때

                    //입장알림
                    sendMsg("enter/" + userId + "/" +username+"/"+roomId+"/"+roomName);
                }

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

                    //메시지를 sqlite 에 저장한다
                    switch(signal){
                        case "room_created":
                            roomId = Integer.valueOf(line_array[1]);

                            //참여방 목록에 room id를 저장해야 한다
                            ContentValues data = new ContentValues();
                            data.put("save_room_id", "Y");
                            data.put("user_id", userId);
                            data.put("room_id", roomId);

                            //result로 받는 것: 검색된 사용자의 닉네임, 사진, id
                            String response = "";

                            try {
                                response = new HttpRequest(getApplicationContext(), "chat.php", data).execute().get();
                            } catch (Exception e) {
                                Log.d("tag", "Error: "+e);
                            }

                            Log.d(TAG, "http response="+response);
                            if(response.equals("success")){

                                //방 아이템 추가
                                Main_Fragment2.roomItemList.add(0, new RoomListItem("room"+roomId, 5,
                                        "", Function.getCurrentTime(), roomId, 0));
                                Log.d(TAG, "Item is added to ChatRoomList");
                            }

                            break;
                        case "serverMsg":

                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(0, new MessageItem("", content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;
                        case "join": //누군가 방에 새로 가입했을 때

                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem("", content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;
                        case "myJoin": //내가 방에 새로 가입했을 때

                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem("", content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;
                        case "return"://누군가 방으로 돌아왔을 때

                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem("", content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;
                        case "myReturn"://내가 방으로 돌아왔을 때

                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem("", content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;
                        case "roomInfo":

                            // 방이름 (참여인원) 참여자1, 참여자2, 참여자3
                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    roomInfo_textView.setText(content);
                                }
                            });

                            break;
                        case "msg": //다른 사람이 보낸 메시지 알림
                            //메시지 보낸사람 정보 + 메시지 내용
                            String sender_id = line_array[1];
                            final String sender_username = line_array[2];
                            content = line_array[3];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem(sender_username, content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;
                        case "myMsg": //내가 보낸 메시지 알림
                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem("[Me]", content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;
                        case "inactive"://방 참여자중 누군가 방을 닫고 메시지를 읽지 않는 상태일 때
                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem("", content));
                                    adapter.notifyItemInserted(adapter.getItemCount()-1);
                                }
                            });

                            break;



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
}
