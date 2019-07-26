package com.android.canaria.recyclerView;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.canaria.R;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {

    private static final String TAG = "MsgRecyclerViewAdapter";

    private ArrayList<MessageItem> mItemArrayList;
    private Context mContext;


    public MessageAdapter(ArrayList<MessageItem> mItemArrayList, Context mContext) {
        this.mItemArrayList = mItemArrayList;
        this.mContext = mContext;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int position) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.message_item, parent, false);

        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }


    @Override
    public void onBindViewHolder(@NonNull MessageAdapter.ViewHolder viewHolder, int position) {

        final MessageItem item = mItemArrayList.get(position);

        final String sender_username = item.getSenderUsername();
        final String message = item.getMessage();

        viewHolder.sender_textView.setText(sender_username);
        viewHolder.message_textView.setText(message);
    }

    @Override
    public int getItemCount() {
        return mItemArrayList.size();
    }




    public class ViewHolder extends RecyclerView.ViewHolder {

        TextView sender_textView, message_textView;
        RelativeLayout parentLayout;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.sender_textView = itemView.findViewById(R.id.messageItem_sender_textView);
            this.message_textView = itemView.findViewById(R.id.messageItem_message_textView);
            this.parentLayout = itemView.findViewById(R.id.messageItem_relativeLayout);

        }


    }


}

