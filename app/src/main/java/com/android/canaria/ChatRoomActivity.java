package com.android.canaria;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_room);

        roomInfo_textView = (TextView)findViewById(R.id.chat_roomInfo_textView);
        msgInput_editText = (EditText) findViewById(R.id.chat_message_editText);
        sendMsg_btn = (Button)findViewById(R.id.chat_send_btn);


        //메시지 보내기 버튼을 클릭하면 -> 서버로 메시지를 보낸다
        sendMsg_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "onClick");

                String message_input = msgInput_editText.getText().toString();
                sendMsg("msg/"+message_input);
            }
        });


        //현재 사용자의 정보와 방 정보를 가져온다
        userId = Function.getString(getApplicationContext(), "user_id");
        username = Function.getString(getApplicationContext(), "username");
        roomName = getIntent().getStringExtra("roomName");
        roomId = getIntent().getIntExtra("roomId", 10000);


        //리사이클러뷰 초기화
        rcv = (RecyclerView)findViewById(R.id.chat_message_rcv);
        linearLayoutManager = new LinearLayoutManager(this);
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        messageItemList = new ArrayList<>();
        adapter = new MessageAdapter(messageItemList, this);
        rcv.setAdapter(adapter);


        //클라이언트 소켓 연결
        clientSocketThread = new ClientSocket();
        clientSocketThread.start();
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
                sendMsg("closeRoom");
                socket.close();
            }
            clientSocketThread.quit();
        }catch (Exception e){
            Log.d(TAG, "socket closing error: "+e);
        }

    }



    class ClientSocket extends Thread{

        boolean isSocketAlive = true;

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

                //입장알림 - 서버에 보내는 신호/ 사용자 id /username
                sendMsg("connect/" + userId + "/" +username+"/"+roomId+"/"+roomName);

            } catch (Exception e) {
                Log.d(TAG, "socket connection error: "+e);
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
                        case "serverMsg":

                            content = line_array[1];

                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    messageItemList.add(new MessageItem("", content));
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