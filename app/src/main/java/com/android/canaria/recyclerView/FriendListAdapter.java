package com.android.canaria.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.Function;
import com.android.canaria.R;
import com.android.canaria.UserProfileActivity;

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

        final FriendListItem friendItem = mItemArrayList.get(position);

        ((FriendListAdapter.ViewHolder) viewHolder).bind(friendItem);


        //친구목록에서 아이템을 선택하면 친구의 프로필 화면이 나타난다
        viewHolder.parentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            Intent intent = new Intent(mContext, UserProfileActivity.class);
            intent.putExtra("friend_id", friendItem.getFriendId());
            intent.putExtra("friend_username", friendItem.getFriendName());
            mContext.startActivity(intent);

            }
        });


        viewHolder.parentLayout.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                //친구 해제 기능 넣기

                Toast.makeText(mContext, "item long clicked", Toast.LENGTH_SHORT).show();

                return false;
            }
        });


    }

    @Override
    public int getItemCount() {
        return mItemArrayList.size();
    }




    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView profileImage_imageView;
        TextView friendName_textView, friendId_textView;
        RelativeLayout parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            this.profileImage_imageView = itemView.findViewById(R.id.friendList_profileImage);
            this.friendName_textView = itemView.findViewById(R.id.friendList_name);
            this.friendId_textView = itemView.findViewById(R.id.friendList_friendId_textView);
            this.parentLayout = itemView.findViewById(R.id.friendList_relativeLayout);
//            parentLayout.setOnLongClickListener(readMessageActivity);
        }


        void bind(FriendListItem friend) {
            friendName_textView.setText(friend.getFriendName());

            friendName_textView.setText(friend.getFriendName());

            // Insert the profile image from the URL into the ImageView.
            Function.displayRoundImageFromUrl(mContext, friend.getUserImage_url(), profileImage_imageView);
        }

    }


}
