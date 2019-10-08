package com.android.canaria;

import android.app.Activity;
import android.content.ContentValues;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.connect_to_server.HttpRequest;
import com.android.canaria.recyclerView.FriendListItem;
import com.bumptech.glide.Glide;

import org.json.JSONArray;
import org.json.JSONObject;


    /*원래 구조: FriendFinderActivity 안에 2개의 fragment가 있음
    (qr코드로 찾기=FriendFinder_Fragment1 & 이메일로 검색=FriendFinder_Fragment2)
    qr코드로 찾기 Fragment 안에는 2개의 fragment가 있음
    (스캔하기=FriendFinder_Fragment1_ScanFragment $ 내qr코드=FriendFinder_Fragment1_MyCode)


    qr코드 기능 없이 이메일 검색 기능만 활성화함
    */

public class FriendFinder_Fragment2 extends Fragment {

    EditText search_editText;
    TextView result_textView, result_small_textView;
    ImageView friend_profileImage_imageView;
    Button addBtn;
    ImageButton searchBtn;
    JSONArray friendList_arr;

    String user_id, email;
    int friend_id;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_friend_finder_fragment2, container, false);


        //이 회원의 기본 정보를 불러온다
        user_id = Function.getString(getContext(), "user_id");
        email = Function.getString(getContext(), "email");


        searchBtn = (ImageButton)view.findViewById(R.id.findFriend_search_imageBtn);

        result_textView = (TextView)view.findViewById(R.id.findFriend_result_textView);
        result_small_textView = (TextView)view.findViewById(R.id.findFriend_result_small_textView);
        friend_profileImage_imageView = (ImageView)view.findViewById(R.id.findFriend_profileImage);
        addBtn = (Button)view.findViewById(R.id.findFriend_add_button);

        result_textView.setVisibility(View.GONE);
        result_small_textView.setVisibility(View.GONE);
        friend_profileImage_imageView.setVisibility(View.GONE);
        addBtn.setVisibility(View.GONE);

        search_editText = (EditText)view.findViewById(R.id.findFriend_search_editText);
        search_editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent keyEvent) {
                if(actionId == EditorInfo.IME_ACTION_SEARCH){

                    //execute our method for searching
                    if(search_editText.getText().toString() != null){
                        String userInput = search_editText.getText().toString();
                        search(userInput);
                        hideSoftKeyboard();
                    }
                }
                return false;
            }
        });

        searchBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(search_editText.getText().toString() != null){
                    String userInput = search_editText.getText().toString();
                    search(userInput);
                    hideSoftKeyboard();
                }
            }
        });

        return view;
    }





    private void search(final String input){

        //확인내용:
        /*
        * 1. 본인일 때
        * 2. 이미 친구 목록에 있을 때
        * 3. 새로운 친구일 때(친구 목록에 x)
        * 4. 존재하지 않는 회원일 때
        * */

        //본인의 email 을 검색했을 때
        if(input.equals(email)){
                friend_profileImage_imageView.setVisibility(View.GONE);
                addBtn.setVisibility(View.GONE);
                result_small_textView.setVisibility(View.GONE);

                result_textView.setVisibility(View.VISIBLE);
                result_textView.setText("That's you!");

        }else{
        //서버에 요청을 보낸다
        //이 사용자의 id와 input값을 보낸다

            ContentValues data = new ContentValues();
            data.put("search_user", "Y");
            data.put("user_id", user_id);
            data.put("input_email", input);

            //result로 받는 것: 검색된 사용자의 닉네임, 사진, id
            String response = "";

            try {
                response = new HttpRequest("management.php", data).execute().get();
            } catch (Exception e) {
                Log.d("tag", "Error: "+e);
            }

            Log.d("tag", "result="+response);

            String result = "";
//            friend_id;
            String friend_username = "";
            String friend_profileImage = "";
            String url = "http://15.164.193.65/uploads_thumb/";
            try{

                JSONObject result_object = new JSONObject(response);

                result = result_object.getString("result");
                friend_id = result_object.getInt("friend_id");
                friend_username = result_object.getString("friend_username");
                friend_profileImage = result_object.getString("friend_profileImage");
                url += friend_profileImage;

            }catch (Exception e){
                Log.d("tag"+this.getClass().getName(), " Error: "+e);
            }


            switch (result){
                case "exists": //이미 친구 목록에 있을 때

                    result_small_textView.setVisibility(View.VISIBLE);
                    friend_profileImage_imageView.setVisibility(View.VISIBLE);
                    result_textView.setVisibility(View.VISIBLE);
                    addBtn.setVisibility(View.GONE);

                    //친구의 사진을 보여준다
                    Glide.with(getActivity()).load(url).into(friend_profileImage_imageView);
                    result_textView.setText(friend_username);
                    result_small_textView.setText("is your friend!");
                    break;
                case "none": //존재하지 않는 사용자일 때

                    friend_profileImage_imageView.setVisibility(View.GONE);
                    addBtn.setVisibility(View.GONE);

                    //존재하지 않는 회원이라고 표시해주기
                    result_textView.setVisibility(View.VISIBLE);
                    result_small_textView.setVisibility(View.VISIBLE);

                    result_textView.setText("User not found");
                    result_small_textView.setText("Please check the Email and try again.");

                    break;
                case "new": //친구가 아닌 새로운 사용자일 때

                    result_small_textView.setVisibility(View.GONE);

                    friend_profileImage_imageView.setVisibility(View.VISIBLE);
                    result_textView.setVisibility(View.VISIBLE);
                    addBtn.setVisibility(View.VISIBLE);

                    //친구의 사진을 보여준다
                    Glide.with(getActivity()).load(url).into(friend_profileImage_imageView);
                    result_textView.setText(friend_username);


                    //Add 버튼을 누르면 -> 친구추가
                    final String finalFriend_username = friend_username;
                    addBtn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            addFriend(friend_id);
                            addBtn.setVisibility(View.GONE);
                            //친구를 추가한다(저장하기)
                            Main_Fragment1.friendItemList.add(0, new FriendListItem(finalFriend_username, friend_id));
                        }
                    });

                    break;
                case "null":

                    friend_profileImage_imageView.setVisibility(View.GONE);
                    addBtn.setVisibility(View.GONE);

                    //오류가 발생했다고 표시해주기
                    result_textView.setVisibility(View.VISIBLE);
                    result_small_textView.setVisibility(View.VISIBLE);

                    result_textView.setText("Error");
                    result_small_textView.setText("Sorry, please try again.");

                    break;
            }

        }

    }



    //친구 목록에 id를 추가하는 메소드
    public void addFriend(int friend_id){

        ContentValues data = new ContentValues();
        data.put("add_friend", "Y");
        data.put("user_id", user_id);
        data.put("friend_id", friend_id);

        String response = "";

        try {
            response = new HttpRequest("management.php", data).execute().get();
        } catch (Exception e) {
            Log.d("tag", "Error: "+e);
        }

        if(response.equals("success")){
            Toast.makeText(getActivity(), "Added", Toast.LENGTH_SHORT).show();

        }else{ //오류가 발생했다고 표시해주기
            friend_profileImage_imageView.setVisibility(View.GONE);
            addBtn.setVisibility(View.GONE);

            result_textView.setVisibility(View.VISIBLE);
            result_small_textView.setVisibility(View.VISIBLE);

            result_textView.setText("Error");
            result_small_textView.setText("Sorry, please try again.");
        }

    }



    //검색 시 키보드를 숨긴다
    private void hideSoftKeyboard(){
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(search_editText.getWindowToken(),0);
    }




}
