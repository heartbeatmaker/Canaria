package com.android.canaria;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;


//getItemCount(아이템 몇 개?) -> onCreateViewHolder(아이템 틀 잡기) -> ViewHolder(틀 안의 세부항목 세팅) -> onBindViewHolder(정보넣기) 순으로 호출
public class SelectFriendsAdapter extends RecyclerView.Adapter<SelectFriendsAdapter.ViewHolder> {

    private static final String TAG = "MsgRecyclerViewAdapter";

    // checkBox 가 선택됐는지 여부를 확인하는 array
    // 문제점: 뷰를 리사이클하면 checkBox 의 상태가 초기화된다
    // 해결책: 체크박스가 onClick 되면, 각 아이템의 check 여부를 boolean으로 이 array에 저장한다
    // -> 리사이클 후 onBindView에서 view를 bind할때 원래 check 상태를 넣어준다
    public SparseBooleanArray itemStateArray= new SparseBooleanArray();

    private ArrayList<FriendListItem> mItemArrayList;
    private Context mContext;
    SelectFriendsActivity selectFriendsActivity;


    public SelectFriendsAdapter(ArrayList<FriendListItem> mItemArrayList, Context mContext) {
        this.mItemArrayList = mItemArrayList;
        this.mContext = mContext;
        selectFriendsActivity = (SelectFriendsActivity) mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.select_friends_layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view, selectFriendsActivity);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull final SelectFriendsAdapter.ViewHolder viewHolder, int position) {

        final FriendListItem item = mItemArrayList.get(position);

        final String friend_username = item.getFriendName();
        final String friend_userId = item.getFriendId();

//        viewHolder.profileImage_imageView.setImageBitmap(item.getProfileImage());
        viewHolder.friendName_textView.setText(friend_username);
        viewHolder.friendId_textView.setText(friend_userId);


        //액션모드일때만 체크박스가 보이도록
        if(!selectFriendsActivity.is_in_action_mode){
            viewHolder.checkBox.setVisibility(View.GONE);
        }
        else{
            viewHolder.checkBox.setVisibility(View.VISIBLE);

            //원래 check 된 상태라면, check 표시를 해준다
            if (!itemStateArray.get(position, false)) {
                viewHolder.checkBox.setChecked(false);
            }else {
                viewHolder.checkBox.setChecked(true);
            }

            //viewHolder.bind(position); 위의 코드를 viewHolder 에서 메소드화 한 것


        }

    }


    @Override
    public int getItemCount() {
        return mItemArrayList.size();
    }




    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

//        ImageView profileImage_imageView;
        TextView friendName_textView, friendId_textView;
        RelativeLayout parentLayout;
        SelectFriendsActivity selectFriendsActivity;
        CheckBox checkBox;

        public ViewHolder(@NonNull View itemView, SelectFriendsActivity selectFriendsActivity) {
            super(itemView);
//            this.roomImage_imageView = itemView.findViewById(R.id.friendList_profileImage);
            this.friendName_textView = itemView.findViewById(R.id.selectFriends_name_textView);
            this.friendId_textView = itemView.findViewById(R.id.selectFriends_friendId_textView);
            this.parentLayout = itemView.findViewById(R.id.selectFriends_relativeLayout);

            //아래: 다중선택 하는데 필요
            this.selectFriendsActivity = selectFriendsActivity;
            this.checkBox = (CheckBox)itemView.findViewById(R.id.selectFriends_checkBox);
            parentLayout.setOnLongClickListener(selectFriendsActivity);
            checkBox.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {


            selectFriendsActivity.prepareSelection(v, getAdapterPosition());

            //check state 저장
            int adapterPosition = getAdapterPosition();
            if (!itemStateArray.get(adapterPosition, false)) {
                itemStateArray.put(adapterPosition, true);
            }
            else  {
                itemStateArray.put(adapterPosition, false);
            }
        }


//        void bind(int position) {
//            // use the sparse boolean array to check
//            if (!itemStateArray.get(position, false)) {
//                checkBox.setChecked(false);
//            }else {
//                checkBox.setChecked(true);
//            }
//        }
    }



    //SelectFriendsActivity.class 에서 쓰는 메소드
    //새 방 생성 or 친구 초대를 위해 친구 목록에서 다중선택 할 때 필요함
    public void updateAdapter(ArrayList<FriendListItem> list){ //리스트에 담긴 항목을 삭제한다

        for(FriendListItem friend : list){
            mItemArrayList.remove(friend);
        }
        notifyDataSetChanged();
    }


}
