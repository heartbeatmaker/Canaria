package com.android.canaria.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.canaria.ChatActivity;
import com.android.canaria.R;

import java.util.ArrayList;

public class RoomListAdapter extends RecyclerView.Adapter<RoomListAdapter.ViewHolder> {

    private static final String TAG = "MsgRecyclerViewAdapter";

    private ArrayList<RoomListItem> mItemArrayList;
    private Context mContext;

    public RoomListAdapter(ArrayList<RoomListItem> mItemArrayList, Context mContext) {
        this.mItemArrayList = mItemArrayList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.roomlist_layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull RoomListAdapter.ViewHolder viewHolder, int position) {

        final RoomListItem item = mItemArrayList.get(position);

//        viewHolder.roomImage_imageView.setImageBitmap(item.getRoomImage());
        viewHolder.roomName_textView.setText(item.getRoomName());
        viewHolder.numberOfMembers_textView.setText(String.valueOf(item.getNumberOfMembers()));
        viewHolder.recentMessage_textView.setText(item.getRecentMessage());
        viewHolder.updatedTime_textView.setText(item.getUpdatedTime());
        viewHolder.roomId_textView.setText(Integer.toString(item.getRoomId()));

        if(item.getUnreadMsgCount() > 0){
            viewHolder.unreadMsg_textView.setVisibility(View.VISIBLE);
            viewHolder.unreadMsg_textView.setText(Integer.toString(item.getUnreadMsgCount()));
        }else{
            viewHolder.unreadMsg_textView.setVisibility(View.INVISIBLE);
        }


        //사용자가 채팅방 아이템을 클릭하면, 채팅 화면을 실행한다
        //room name 과 id를 인텐트로 전달한다
        viewHolder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(mContext, ChatActivity.class);
                intent.putExtra("isNewRoom", "N"); //기존 방에 입장한다는 표시
                intent.putExtra("roomId", item.getRoomId());
                intent.putExtra("roomName", item.getRoomName());
                mContext.startActivity(intent);
            }
        });

    }

    @Override
    public int getItemCount() {
        return mItemArrayList.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder {

//        ImageView roomImage_imageView;
        TextView roomName_textView, numberOfMembers_textView, recentMessage_textView, updatedTime_textView, roomId_textView, unreadMsg_textView;
        RelativeLayout parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            this.roomImage_imageView = itemView.findViewById(R.id.roomList_roomImage);
            this.roomName_textView = itemView.findViewById(R.id.roomList_roomName);
            this.numberOfMembers_textView = itemView.findViewById(R.id.roomList_numberOfMembers);
            this.recentMessage_textView = itemView.findViewById(R.id.roomList_recentMessage);
            this.updatedTime_textView = itemView.findViewById(R.id.roomList_messageTime);
            this.roomId_textView = itemView.findViewById(R.id.roomList_roomId);
            this.parentLayout = itemView.findViewById(R.id.roomList_relativeLayout);
            this.unreadMsg_textView = itemView.findViewById(R.id.roomList_unreadMsgCount);

//            parentLayout.setOnLongClickListener(readMessageActivity);
        }


    }

    public void updateAdapter(ArrayList<RoomListItem> list){ //리스트에 담긴 항목을 삭제한다

        for(RoomListItem message : list){
            mItemArrayList.remove(message);
        }
        notifyDataSetChanged();
    }

}
