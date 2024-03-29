package com.android.canaria;


import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.recyclerView.FriendListItem;
import com.android.canaria.recyclerView.SelectFriendsAdapter;

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

/*채팅방 만들기, 친구 초대하기 버튼을 눌렀을 때 나타나는 친구목록 화면*/

public class SelectFriendsActivity extends AppCompatActivity {

    //다중선택을 위한 변수
    ArrayList<FriendListItem> selection_list = new ArrayList<>(); //지울 아이템
    ArrayList<Integer> selected_position = new ArrayList<>(); //지우려고 선택한 아이템의 위치
    int counter = 0;
    Toolbar toolbar;
    public boolean is_in_action_mode = false; // 다른 패키지 클래스인 selectFriendsAdapter에서 이 변수에 접근해야 함 -> public으로 바꿈

    //리사이클러뷰 기본 변수
    RecyclerView rcv;
    ArrayList<FriendListItem> friendItemList;
    SelectFriendsAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    String TAG = "tag "+this.getClass().getSimpleName();

    String user_id;
    boolean noMoreItem;

    TextView counter_text_view, selectedFriends_textView;

    Context context = SelectFriendsActivity.this;

    boolean isForInvitation = false;
    String existing_members;


    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.menu_select,menu);
        return true;
    }



//    @Override
//    public boolean onLongClick(View v) {
//
//        toolbar.getMenu().clear(); //길게누르면 -> 툴바 clear
//        toolbar.inflateMenu(R.menu.menu_select);
//        counter_text_view.setVisibility(View.VISIBLE);
//        is_in_action_mode = true;
//        adapter.notifyDataSetChanged();//이건 왜 하는거지?
//
//        return true;
//    }



    public void prepareSelection(View view, final int position) {
        Log.d(TAG,"prepareSelection");
        if (((CheckBox)view).isChecked()) { //체크 됐을 때
            selection_list.add(friendItemList.get(position));
            selected_position.add(position); //해당 아이템의 위치를 리스트에 담는다
            counter = counter + 1;
            updateCounter(counter);

            Log.d(TAG,position+"을 고름");
            Log.d(TAG,position+"을 selectedList에 담음: "+selected_position);
        } else { //isChecked()=false 일 때 (uncheck 됐을 때)
            selection_list.remove(friendItemList.get(position));
//            selected_position.removeIf(p -> p.equals(position)); //해당 아이템의 위치값을 리스트에서 제거한다
            counter = counter - 1;
            updateCounter(counter);
            try {
                for(int i=selected_position.size()-1; i>=0; i--){
                    if(position == selected_position.get(i)){
                        selected_position.remove(i);
                    }
                }

                Log.d(TAG, position + "을 삭제 취소함");
                Log.d(TAG, position + "을 selectedList에서 지움: " + selected_position);
            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);
            }
        }

    }


    public void updateCounter(int counter){
        if(counter == 0){
            counter_text_view.setText("0 selected");
        }
        else{
            selectedFriends_textView.setVisibility(View.VISIBLE);

            //선택한 친구의 username을 가져온다 화면에 보여준다
            String friend_username = "";
            for(int i=0; i<selection_list.size(); i++){
                if(i==0){
                    friend_username = selection_list.get(i).getFriendName();
                }else{
                    friend_username += ", " + selection_list.get(i).getFriendName();
                }
            }
            selectedFriends_textView.setText(friend_username);

            if(counter == 1){
                counter_text_view.setText(counter+" friend selected");
            }else{
                counter_text_view.setText(counter+" friends selected");
            }
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item){
        if(item.getItemId() == R.id.item_ok){ //ok 아이콘을 누르면

            JSONArray friendInfo_array = new JSONArray();
            //선택한 친구의 id와 username을 가져온다 -> 채팅 액티비티로 넘긴다
            for(int i=0; i<selection_list.size(); i++){
                int friend_id = selection_list.get(i).getFriendId();
                String friend_username = selection_list.get(i).getFriendName();

                try{
                    JSONObject friendInfo = new JSONObject();
                    friendInfo.put("id", friend_id);
                    friendInfo.put("username", friend_username);

                    friendInfo_array.put(friendInfo);
                }catch (Exception e){
                    Log.d(TAG, "JSONError");
                    e.printStackTrace();
                }

            }

            Log.d(TAG, "friendInfo_array="+friendInfo_array);

            adapter.updateAdapter(selection_list); //selection_list에 있는 아이템을 지움
            clearActionMode(); //액션바, 각종 변수를 초기화
            selected_position.clear();


            if(isForInvitation){ //초대 시

                Log.d("invitation", "SelectFriendsActivity) 초대할 사람을 고르고, ok 버튼 누름. friendInfo_array="+friendInfo_array);
                Intent resultIntent = new Intent();
                resultIntent.putExtra("friendInfo_jsonArray", friendInfo_array.toString());
                setResult(RESULT_OK,resultIntent);
                finish();

            }else{ //방을 새로 만들 때
                Intent intent = new Intent(this, ChatActivity.class);
                intent.putExtra("isNewRoom", "Y");
                intent.putExtra("friendInfo_jsonArray", friendInfo_array.toString());
                startActivity(intent);
                finish();
            }

        }
        return super.onOptionsItemSelected(item);
    }

    public void clearActionMode(){
        is_in_action_mode = false;
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.item_menu);
        counter_text_view.setVisibility(View.GONE);
        selectedFriends_textView.setVisibility(View.GONE);

        //변수 초기화
        counter_text_view.setText("0 item selected");
        selectedFriends_textView.setText("");
        counter = 0;
        selection_list.clear();
        adapter.itemStateArray.clear(); //checkBox 가 선택됐는지 여부 확인하는 array. 초기화해줘야 한다
    }

    //action mode에서 뒤로가기 눌렀을 때, 해당 액티비티 자체를 벗어남. 이 문제를 해결하기 위한 메소드
    @Override
    public void onBackPressed(){
//        if(is_in_action_mode){
//            clearActionMode();
//            selected_position.clear();
//            adapter.notifyDataSetChanged();
//        }
//        else{
            super.onBackPressed();
            finish();
//        }
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friends);


        try{
            Log.d("invitation", "SelectFriendsActivity) onCreate()");

            String invitation_signal = getIntent().getStringExtra("invitation");
            if(invitation_signal != null){
                isForInvitation = true;
                existing_members = getIntent().getStringExtra("existing_members"); //기존 참여자들의 id를 받아온다
            }else{
                isForInvitation = false;
            }
        }catch (Exception e){
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String ex = sw.toString();

            Log.d("invitation",ex);
        }


        //다중선택을 위한 리소스 초기화: 툴바를 숨겨놓는다
        toolbar =(Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        counter_text_view = (TextView)findViewById(R.id.counter_textView);
        is_in_action_mode = true;
        selectedFriends_textView = (TextView)findViewById(R.id.selectFriends_selectedFriends_textView);
        selectedFriends_textView.setVisibility(View.GONE);


        //initialize recyclerView
        rcv = (RecyclerView)findViewById(R.id.selectFriends_rcv);
        linearLayoutManager = new LinearLayoutManager(this);
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        friendItemList = new ArrayList<>();
        adapter = new SelectFriendsAdapter(friendItemList,this);
        rcv.setAdapter(adapter);

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
                    loadMoreItem(lastItemPosition);
//                    mAdapter.showLoading();
                }
            }
        });


//        for(int k=0; k<30; k++){
//            friendItemList.add(0, new FriendListItem("Huck Finn "+k, "1"));
//        }

        user_id = Function.getString(this, "user_id");
        loadFirstPage();
    }

    public void loadFirstPage(){
        new SendPost().execute(user_id, 0); //해당 유저의 친구목록중에, 1페이지(0~9번째 친구) 데이터를 가져온다
    }

    public void loadMoreItem(int lastItemPosition){
        //다음에 받아야 할 페이지를 적어서 서버에 데이터 요청

        if(!noMoreItem){
            int nextPage_firstItemPosition = lastItemPosition +1;
            new SendPost().execute(user_id, nextPage_firstItemPosition);
        }else{
            Toast.makeText(this, "no more item", Toast.LENGTH_SHORT).show();
        }
    }



    class SendPost extends AsyncTask<Object, Void, String> {

        ProgressDialog dialog = new ProgressDialog(SelectFriendsActivity.this);

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d(TAG,"onPreExecute");

            dialog.setMessage("Processing..");
            dialog.show();
        }

        @Override
        protected String doInBackground(Object... objects) {

            String user_id = (String)objects[0];
            int nextPage_firstItemPosition = (int)objects[1];


            String response_line = "";

            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost("http://15.164.193.65/management.php");

            //POST 방식에서 사용된다
            ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

            try {
                //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.
                nameValues.add(new BasicNameValuePair(
                        "get_friendList", URLDecoder.decode("y", "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "user_id", URLDecoder.decode(user_id, "UTF-8")));
                nameValues.add(new BasicNameValuePair(
                        "first_item_position", URLDecoder.decode(String.valueOf(nextPage_firstItemPosition), "UTF-8")));


                //HttpPost에 넘길 값을들 Set해주기
                post.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));

            } catch (UnsupportedEncodingException ex) {
                Log.d(TAG, ex.toString());
            }

            try {
                //설정한 URL을 실행시키기 -> 응답을 받음
                HttpResponse response = client.execute(post);
                //통신 값을 받은 Log 생성. (200이 나오는지 확인할 것~) 200이 나오면 통신이 잘 되었다는 뜻!
                Log.i(TAG, "response.getStatusCode:" + response.getStatusLine().getStatusCode());

                HttpEntity entity = response.getEntity();

                if(entity !=null){
                    Log.d(TAG, "Response length:"+entity.getContentLength());

                    // 콘텐츠를 읽어들임.
                    BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));

                    while ((response_line = reader.readLine()) != null) {
                        // 콘텐츠 내용
                        Log.d(TAG, "response: "+response_line);
                        return response_line;
                    }
                }

                //Ensures that the entity content is fully consumed and the content stream, if exists, is closed.
                EntityUtils.consume(entity);

                post.releaseConnection();


            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return null;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            dialog.dismiss();
            Log.d(TAG,"onPostExecute");


            try{

                JSONObject result_object = new JSONObject(s);
                String result = result_object.getString("result");
                Log.d(TAG,"result="+result);

                noMoreItem = result_object.getBoolean("no_more_item");

                if(result.equals("success")){//결과가 '성공'이면

                    //jsonArray 구조로 전달된 친구정보를 파싱한다
                    Object friendInfo_object = result_object.get("friendInfo");
                    JSONArray friendInfo_array = (JSONArray)friendInfo_object;
//                    Log.d(TAG,"friendInfo_array = "+friendInfo_array);

                    for(int i=0; i<friendInfo_array.length(); i++){
                        JSONObject individual_friendInfo_object = (JSONObject)friendInfo_array.get(i);
//                        Log.d(TAG,i+"번째 friendInfo_object = "+individual_friendInfo_object);

                        int friend_id = Integer.valueOf((String)individual_friendInfo_object.get("friend_id"));
                        String friend_username = (String)individual_friendInfo_object.get("friend_username");
//            String friend_profileImage = (String)individual_friendInfo_object.get("friend_profileImage");

//                        Log.d(TAG,i+"번째 친구의 id = "+friend_id+" / name = "+friend_username);


                        if(isForInvitation){ //기존 방에 새로운 사용자를 초대하는 상황일 때
                            String[] existing_members_array = existing_members.split(";");
                            boolean isAlreadyMember = false;
                            for(int j=0; j<existing_members_array.length; j++){
                                int existing_member_id = Integer.valueOf(existing_members_array[j]);

                                if(existing_member_id == friend_id){
                                    isAlreadyMember = true;
                                }
                            }

                            //기존 참여자가 아닌 친구만 목록에 추가한다
                            if(!isAlreadyMember){
                                friendItemList.add(new FriendListItem(friend_username, friend_id));
                            }

                        }else{ //새로운 방을 만드는 상황일 때 - 모든 친구를 목록에 추가한다
                            friendItemList.add(new FriendListItem(friend_username, friend_id));
                        }
                    }
                    adapter.notifyDataSetChanged();

                }else if(s.equals("zero")){ //친구 목록이 비어있을 때

                    Log.d(TAG,"This user has no friend");
                    Toast.makeText(SelectFriendsActivity.this, "You have no friend.", Toast.LENGTH_SHORT).show();
                }else{
                    Log.d(TAG,"Error: failed to retrieve data");

//                    Toast.makeText(getContext(), "Error: failed to retrieve data.", Toast.LENGTH_SHORT).show();
                }


            }catch (Exception e){
                Log.d(TAG, "Error: "+e);
            }


        }
    }

}
