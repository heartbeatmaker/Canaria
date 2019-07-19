package com.android.canaria;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.ViewHolder> {

    private static final String TAG = "MsgRecyclerViewAdapter";

    private ArrayList<FriendListItem> mItemArrayList;
    private Context mContext;

    public FriendListAdapter(ArrayList<FriendListItem> mItemArrayList, Context mContext) {
        this.mItemArrayList = mItemArrayList;
        this.mContext = mContext;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.friendlist_layout, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull FriendListAdapter.ViewHolder viewHolder, int position) {

        final FriendListItem item = mItemArrayList.get(position);

//        viewHolder.profileImage_imageView.setImageBitmap(item.getProfileImage());
        viewHolder.friendName_textView.setText(item.getFriendName());
        viewHolder.friendId_textView.setText(item.getFriendId());

        viewHolder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(mContext, "item clicked", Toast.LENGTH_SHORT).show();
            }
        });

    }

    @Override
    public int getItemCount() {
        return mItemArrayList.size();
    }



    public class ViewHolder extends RecyclerView.ViewHolder {

//        ImageView profileImage_imageView;
        TextView friendName_textView, friendId_textView;
        RelativeLayout parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
//            this.roomImage_imageView = itemView.findViewById(R.id.friendList_profileImage);
            this.friendName_textView = itemView.findViewById(R.id.friendList_name);
            this.friendId_textView = itemView.findViewById(R.id.friendList_friendId_textView);
            this.parentLayout = itemView.findViewById(R.id.friendList_relativeLayout);
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
