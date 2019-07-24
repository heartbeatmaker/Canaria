package com.android.canaria;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class SelectFriendsAdapter extends RecyclerView.Adapter<SelectFriendsAdapter.ViewHolder> {

    private static final String TAG = "MsgRecyclerViewAdapter";

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
    public void onBindViewHolder(@NonNull SelectFriendsAdapter.ViewHolder viewHolder, int position) {

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
            viewHolder.checkBox.setChecked(false);
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
            this.selectFriendsActivity = selectFriendsActivity;
            this.checkBox = (CheckBox)itemView.findViewById(R.id.selectFriends_checkBox);
            parentLayout.setOnLongClickListener(selectFriendsActivity);
        }

        @Override
        public void onClick(View v) {

            selectFriendsActivity.prepareSelection(v, getAdapterPosition());
        }
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
