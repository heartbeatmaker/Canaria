package com.android.canaria;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.connect_to_server.MainService;
import com.android.canaria.db.DBHelper;
import com.android.canaria.recyclerView.MessageItem;
import com.android.canaria.recyclerView.RoomListAdapter;
import com.android.canaria.recyclerView.RoomListItem;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;


/*채팅방 목록 fragment*/

public class Main_Fragment2 extends Fragment implements View.OnCreateContextMenuListener {

    String TAG = "tag "+this.getClass().getSimpleName();

    RecyclerView rcv;
    public static ArrayList<RoomListItem> roomItemList;
    RoomListAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    String user_id;
    boolean noMoreItem;

    DBHelper dbHelper;
    SQLiteDatabase db;
    int results_per_page = 10;

    Handler handler;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        handler = new Handler();
    }

    @Override
    public void onResume() {
        super.onResume();

        Log.d(TAG, "onResume");

        LocalBroadcastManager.getInstance(getContext()).registerReceiver(messageReceiver, new IntentFilter("roomList_event"));

        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(messageReceiver);
    }



    @Override
    public boolean onContextItemSelected(final MenuItem item) {

        switch (item.getItemId()){

            case 121: //방이름 수정

                //item.getGroupId()= 아이템의 position. 어댑터 클래스에서 메뉴아이템 만들 때 그렇게 설정해놓음
                final int position = item.getGroupId();
                String roomName = roomItemList.get(position).getRoomName();
                final int roomId = roomItemList.get(position).getRoomId();


                //방 이름 수정하는 다이얼로그가 나타남
                final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.editroom_dialog, null, false);
                builder.setView(view);

                final EditText roomName_editText = (EditText)view.findViewById(R.id.editRoom_dialog_editText);
                roomName_editText.setHint(roomItemList.get(position).getRoomName());
                Button confirm_btn = (Button)view.findViewById(R.id.editRoom_dialog_confirm_btn);
                Button cancel_btn = (Button)view.findViewById(R.id.editRoom_dialog_cancel_btn);

                final AlertDialog dialog = builder.create();

                confirm_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //확인 버튼을 누르면

                        String new_roomName = roomName_editText.getText().toString();

                        //roomInfo 업데이트
                        DBHelper dbHelper = new DBHelper(getContext(), Function.dbName, null, Function.dbVersion);
                        dbHelper.open();
                        dbHelper.room_updateName(roomId, new_roomName);

                        //방 목록 업데이트
                        roomItemList.get(position).setRoomName(new_roomName);
                        adapter.notifyItemChanged(position);

                        dialog.dismiss();
                    }
                });

                cancel_btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //취소 버튼을 누르면

                        dialog.dismiss();
                    }
                });

                dialog.show();


                return true;
            case 122: //방 나가기
                final int item_position = item.getGroupId();
                final int item_roomId = roomItemList.get(item_position).getRoomId();


                //진짜 방을 나갈 것인지 물어보는 다이얼로그가 뜬다
                AlertDialog.Builder builder1 = new AlertDialog.Builder(getContext());
                builder1.setTitle("Are you sure to leave this room?").setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).setPositiveButton("Leave", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                        //나가기 버튼을 누르면

                        //1. 방 목록에서 이 방 삭제, 리사이클러뷰 업데이트
                        roomItemList.remove(item_position);
                        adapter.notifyItemRemoved(item_position);

                        //2. sqlite 에서 이 방 데이터 삭제
                        db.execSQL("DELETE FROM chat_rooms WHERE room_id='" + item_roomId + "';");

                        //3. 서버에 메시지 전달: leave/방 id
                        sendMsg("leave/"+item_roomId);

                        dialog.dismiss();
                    }
                }).setCancelable(false).show();

                return true;

            default:
                return super.onContextItemSelected(item);
        }
    }


    void sendMsg(String msg){
        Intent intent = new Intent(getActivity(), MainService.class);
        intent.putExtra("message", msg);

        getActivity().startService(intent);
    }


    private BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("message");
            Log.d(TAG, "Receiver) message received: " + message);

            final String[] message_array = message.split("/");
            String signal = message_array[0];

            switch (signal){
                case "inserted":

                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "adapter.notifyDataSetChanged()");

                            adapter.notifyDataSetChanged();
                            rcv.scrollToPosition(0);
                        }
                    });
                    break;

            }


        }
    };


    //UI 리소스 초기화
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.main_fragment2, container, false);

        dbHelper = new DBHelper(getContext(), Function.dbName, null, Function.dbVersion);
        db = dbHelper.getWritableDatabase();

        //String tableName, String room_name, String members, String updateTime
//        for(int i=0; i<33; i++){
//            dbHelper.insert("chat_rooms", "room"+i, "me;you", Function.getCurrentTime());
//        }
//
//        String result = dbHelper.getResult_table_chatRooms();
//        Log.d(TAG, "result="+result);



        rcv = (RecyclerView)view.findViewById(R.id.main_fragment2_rcv);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        roomItemList = new ArrayList<>();
        adapter = new RoomListAdapter(roomItemList, getActivity());
        rcv.setAdapter(adapter);

        user_id = Function.getString(getContext(), "user_id");

        loadAllItem();
//        adapter.notifyDataSetChanged();
//        loadItem(0);

//        rcvScroll(); -- 페이징 - 나중에 하기

        return view;
    }



    public void rcvScroll(){
        rcv.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                int lastItemPosition = linearLayoutManager.findLastCompletelyVisibleItemPosition();
                int itemTotalCount = adapter.getItemCount();

//                Log.d(TAG, "onScrolled. lastVisibleItemPosition = "+lastItemPosition+" / itemTotalCount = "+itemTotalCount);

                //리스트의 마지막에 도달했을 때 -> 다음 페이지 로드
                if (dy > 0 && lastItemPosition == (itemTotalCount - 1)) {
                    Log.d(TAG, "last item. lastVisibleItemPosition = "+lastItemPosition+" Loading more item");
                    loadItem(lastItemPosition);

                }
            }
        });

    }


    //메인화면의 fragment는 show/hide를 반복한다.
    //show에서 adapter를 refresh 해준다 (서비스에서 item 추가되었을 경우 대비)
//    @Override
//    public void setUserVisibleHint(boolean isVisibleToUser) {
//        super.setUserVisibleHint(isVisibleToUser);
//        if (isVisibleToUser) {
//            Log.d(TAG, "fragment2 is visible. notifyDataSetChanged() is called");
//            adapter.notifyDataSetChanged();
//        } else {
//            Log.d(TAG, "fragment2 is invisible");
//        }
//    }



    public void loadAllItem(){
        Cursor cursor = db.rawQuery("SELECT * FROM chat_rooms", null);
        while (cursor.moveToNext()) {

            int room_id = cursor.getInt(0);
            String room_name = cursor.getString(1);
            String updateTime = cursor.getString(2);
            String members = cursor.getString(3);

            String[] members_array = members.split(";");
            String recentMessage = "";
            int unreadMsgCount = 0;

            try{

                unreadMsgCount = dbHelper.get_unreadMsgCount(room_id);
                recentMessage = dbHelper.get_recentMessage(room_id);

                if(recentMessage == null){
                    recentMessage = "";
                }
            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);
            }


            if(room_name != null){
                roomItemList.add(0, new RoomListItem(room_name, members_array.length/2,
                        recentMessage, updateTime, room_id, unreadMsgCount));

            }
        }
    }


    public void loadItem(int lastItemPosition){
        //다음에 받아야 할 페이지를 적어서 서버에 데이터 요청

        int first_item_position = lastItemPosition +1;

        long count = DatabaseUtils.queryNumEntries(db, "chat_rooms");

        //방의 개수를 조회한다
        int number_of_rooms = (int)count;

        //페이지마다 몇번째 행부터 데이터를 출력할 지
        int number_of_pages = (int)Math.ceil(number_of_rooms/results_per_page);

        int page = (int)(Math.floor(first_item_position/results_per_page))+1;
        int start_from = (page - 1)*results_per_page;

        Log.d(TAG, "numberOfRooms="+number_of_rooms+" firstItemPosition="+first_item_position+" page="+page+" startFrom="+start_from);

        if(!noMoreItem){

            Cursor cursor = db.rawQuery("SELECT * FROM chat_rooms ORDER BY datetime(updateTime) LIMIT "+start_from+" OFFSET "+results_per_page, null);
            while (cursor.moveToNext()) {

                int room_id = cursor.getInt(0);
                String room_name = cursor.getString(1);
                String updateTime = cursor.getString(2);
                String members = cursor.getString(3);

                String[] members_array = members.split(";");

                String recentMessage = dbHelper.get_recentMessage(room_id);

                if(recentMessage == null){
                    recentMessage = "";
                }
                int unreadMsgCount = dbHelper.get_unreadMsgCount(room_id);

                if(room_name != null){
                    roomItemList.add(0, new RoomListItem(room_name, members_array.length/2,
                            recentMessage, updateTime, room_id, unreadMsgCount));

                }else{
                    noMoreItem = true;
                    Log.d(TAG, "NO MORE ITEM");
                }

            }

            rcv.post(new Runnable() {
                public void run() {
                    adapter.notifyDataSetChanged();
                }
            });
//            new Main_Fragment2.SendPost().execute(user_id, nextPage_firstItemPosition);
        }else{
            Log.d(TAG, "loadMoreItem) no more item");
            Toast.makeText(getContext(), "no more item", Toast.LENGTH_SHORT).show();
        }
    }



    @Override
    public void onPause() {
        super.onPause();

        Log.d(TAG, "onPause");
    }

    //    class SendPost extends AsyncTask<Object, Void, String> {
//
//        ProgressDialog dialog = new ProgressDialog(getContext());
//
//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            Log.d(TAG,"onPreExecute");
//
//            dialog.setMessage("Processing..");
//            dialog.show();
//        }
//
//        @Override
//        protected String doInBackground(Object... objects) {
//
//            String user_id = (String)objects[0];
//            int nextPage_firstItemPosition = (int)objects[1];
//
//
//            String response_line = "";
//
//            HttpClient client = new DefaultHttpClient();
//            HttpPost post = new HttpPost("http://15.164.193.65/management.php");
//
//            //POST 방식에서 사용된다
//            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();
//
//            try {
//                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
//                nameValues.add(new BasicNameValuePair(
//                        "get_roomList", URLDecoder.decode("y", "UTF-8")));
//                nameValues.add(new BasicNameValuePair(
//                        "user_id", URLDecoder.decode(user_id, "UTF-8")));
//                nameValues.add(new BasicNameValuePair(
//                        "first_item_position", URLDecoder.decode(String.valueOf(nextPage_firstItemPosition), "UTF-8")));
//
//
//                //HttpPost에 넘길 값을들 Set해주기
//                post.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));
//
//            } catch (UnsupportedEncodingException ex) {
//                StringWriter sw = new StringWriter();
//                ex.printStackTrace(new PrintWriter(sw));
//                String exx = sw.toString();
//
//                Log.d(TAG,exx);
//            }
//
//            try {
//                //설정한 URL을 실행시키기 -> 응답을 받음
//                HttpResponse response = client.execute(post);
//                //통신 값을 받은 Log 생성. (200이 나오는지 확인할 것~) 200이 나오면 통신이 잘 되었다는 뜻!
//                Log.i(TAG, "response.getStatusCode:" + response.getStatusLine().getStatusCode());
//
//                HttpEntity entity = response.getEntity();
//
//                if(entity !=null){
//                    Log.d(TAG, "Response length:"+entity.getContentLength());
//
//                    // 콘텐츠를 읽어들임.
//                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
//
//                    while ((response_line = reader.readLine()) != null) {
//                        // 콘텐츠 내용
//                        Log.d(TAG, "response: "+response_line);
//                        return response_line;
//                    }
//                }
//
//                //Ensures that the entity content is fully consumed and the content stream, if exists, is closed.
//                EntityUtils.consume(entity);
//
//                post.releaseConnection();
//
//
//            } catch (ClientProtocolException e) {
//                StringWriter sw = new StringWriter();
//                e.printStackTrace(new PrintWriter(sw));
//                String ex = sw.toString();
//
//                Log.d(TAG,ex);
//            } catch (MalformedURLException e) {
//                StringWriter sw = new StringWriter();
//                e.printStackTrace(new PrintWriter(sw));
//                String ex = sw.toString();
//
//                Log.d(TAG,ex);
//            } catch (IOException e) {
//                StringWriter sw = new StringWriter();
//                e.printStackTrace(new PrintWriter(sw));
//                String ex = sw.toString();
//
//                Log.d(TAG,ex);
//            }
//
//            return null;
//        }
//
//
//        @Override
//        protected void onPostExecute(String s) {
//            super.onPostExecute(s);
//
//            dialog.dismiss();
//            Log.d(TAG,"onPostExecute");
//
//
//            try{
//
//                JSONObject result_object = new JSONObject(s);
//                String result = result_object.getString("result");
//                Log.d(TAG,"result="+result);
//
//                noMoreItem = result_object.getBoolean("no_more_item");
//
//                if(result.equals("success")){//결과가 '성공'이면
//
//                    //jsonArray 구조로 전달된 방정보를 파싱한다
//                    Object rooms_Jobject = result_object.get("rooms_id_array");
//                    JSONArray rooms_Jarray = (JSONArray)rooms_Jobject;
//                    Log.d(TAG,"rooms_Jarray = "+rooms_Jarray);
//
//                    for (int i=0;i<rooms_Jarray.length();i++){
//                        String room_id = rooms_Jarray.getString(i);
//
//                        roomItemList.add(0, new RoomListItem("room"+room_id, 5,
//                                "hahaha", "12:00", Integer.valueOf(room_id)));
//                        adapter.notifyDataSetChanged();
//                    }
//
//                }else if(s.equals("zero")){ //방 목록이 비어있을 때
//
//                    Log.d(TAG,"This user has joined no room");
////                    Toast.makeText(getContext(), "no room.", Toast.LENGTH_SHORT).show();
//                }else{
//                    Log.d(TAG,"Error: failed to retrieve room data");
//
////                    Toast.makeText(getContext(), "Error: failed to retrieve room data.", Toast.LENGTH_SHORT).show();
//                }
//
//
//            }catch (Exception e){
//                StringWriter sw = new StringWriter();
//                e.printStackTrace(new PrintWriter(sw));
//                String ex = sw.toString();
//
//                Log.d(TAG,ex);
//            }
//
//
//        }
//    }

}
