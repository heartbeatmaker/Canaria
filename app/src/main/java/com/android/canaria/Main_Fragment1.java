package com.android.canaria;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

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

/*친구목록 fragment*/

public class Main_Fragment1 extends Fragment{

    RecyclerView rcv;
    ArrayList<FriendListItem> friendItemList;
    FriendListAdapter adapter;
    LinearLayoutManager linearLayoutManager;

    String TAG = "tag "+this.getClass().getSimpleName();

    String user_id;
    boolean noMoreItem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.main_fragment1, container, false);

        rcv = (RecyclerView)view.findViewById(R.id.main_fragment1_rcv);
        linearLayoutManager = new LinearLayoutManager(getActivity());
        rcv.setHasFixedSize(true);
        rcv.setLayoutManager(linearLayoutManager);
        friendItemList = new ArrayList<>();
        adapter = new FriendListAdapter(friendItemList, getActivity());
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

        user_id = Function.getString(getContext(), "user_id");
        loadFirstPage();

        return view;
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
            Toast.makeText(getContext(), "no more item", Toast.LENGTH_SHORT).show();
        }
    }


    class SendPost extends AsyncTask<Object, Void, String> {

        ProgressDialog dialog = new ProgressDialog(getContext());

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
            HttpPost post = new HttpPost("http://54.180.107.44/management.php");

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
                    Log.d(TAG,"friendInfo_array = "+friendInfo_array);

                    for(int i=0; i<friendInfo_array.length(); i++){
                        JSONObject individual_friendInfo_object = (JSONObject)friendInfo_array.get(i);
//                        Log.d(TAG,i+"번째 friendInfo_object = "+individual_friendInfo_object);

                        String friend_id = (String)individual_friendInfo_object.get("friend_id");
                        String friend_username = (String)individual_friendInfo_object.get("friend_username");
//            String friend_profileImage = (String)individual_friendInfo_object.get("friend_profileImage");

//                        Log.d(TAG,i+"번째 친구의 id = "+friend_id+" / name = "+friend_username);

                        friendItemList.add(new FriendListItem(friend_username, friend_id));
                        adapter.notifyDataSetChanged();

                    }

                }else if(s.equals("zero")){ //친구 목록이 비어있을 때

                    Log.d(TAG,"This user has no friend");
                    Toast.makeText(getContext(), "You have no friend.", Toast.LENGTH_SHORT).show();
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
