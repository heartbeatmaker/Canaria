package com.android.canaria.recyclerView;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.canaria.Function;
import com.android.canaria.R;

import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter {

    private static final String TAG = "MsgRecyclerViewAdapter";

    private ArrayList<MessageItem> mItemArrayList;
    private Context mContext;

    private int userId;

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 2;
    private static final int VIEW_TYPE_MESSAGE_SERVER = 3;

    public MessageAdapter(ArrayList<MessageItem> mItemArrayList, Context mContext) {
        this.mItemArrayList = mItemArrayList;
        this.mContext = mContext;

        userId = Integer.valueOf(Function.getString(mContext, "user_id"));
    }


    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        MessageItem message = (MessageItem) mItemArrayList.get(position);

        Log.d("msg", "sender_id="+message.getSender_id()+" / sender_username="+message.getSenderUsername());

        if(message.getSender_id() == userId){
            Log.d("msg", "my msg");
            // If the current user is the sender of the message
            return VIEW_TYPE_MESSAGE_SENT;

        }else if (message.getSender_id()==0 && message.getSenderUsername().equals("server")) {
            Log.d("msg", "server msg");
            // If server sent the message or etc(ex. dateTime indicator)
            return VIEW_TYPE_MESSAGE_SERVER;
        }else{
            Log.d("msg", "general");
            // If some other user sent the message
            return VIEW_TYPE_MESSAGE_RECEIVED;
        }
    }


    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        if (viewType == VIEW_TYPE_MESSAGE_SENT) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_item_sent, parent, false);
            return new SentMessageHolder(view);

        } else if (viewType == VIEW_TYPE_MESSAGE_RECEIVED) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_item_received, parent, false);
            return new ReceivedMessageHolder(view);

        } else if (viewType == VIEW_TYPE_MESSAGE_SERVER) {
            view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.message_item_notification, parent, false);
            return new ServerMessageHolder(view);
        }

        return null;
    }



    // Passes the message object to a ViewHolder so that the contents can be bound to UI.
    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        MessageItem message = (MessageItem) mItemArrayList.get(position);

        switch (holder.getItemViewType()) {
            case VIEW_TYPE_MESSAGE_SENT:
                ((SentMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_SERVER:
                ((ServerMessageHolder) holder).bind(message);
                break;
        }
    }


    @Override
    public int getItemCount() {
        return mItemArrayList.size();
    }




    private class SentMessageHolder extends RecyclerView.ViewHolder {
        TextView sent_message_textView, sent_time_textView;

        SentMessageHolder(View itemView) {
            super(itemView);

            sent_message_textView = (TextView) itemView.findViewById(R.id.sent_message_textView);
            sent_time_textView = (TextView) itemView.findViewById(R.id.sent_time_textView);
        }

        void bind(MessageItem message) {
            sent_message_textView.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.
            sent_time_textView.setText(Function.formatTime(message.getTimeMillis()));

        }

    }


    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView received_message_textView, received_time_textView, received_username_textView;
        ImageView received_profileImage_imageView;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            received_message_textView = (TextView) itemView.findViewById(R.id.received_message_textView);
            received_time_textView = (TextView) itemView.findViewById(R.id.received_time_textView);
            received_username_textView = (TextView) itemView.findViewById(R.id.received_username_textView);
            received_profileImage_imageView = (ImageView) itemView.findViewById(R.id.received_profileImage_imageView);
        }


        void bind(MessageItem message) {
            received_message_textView.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.
            received_time_textView.setText(Function.formatTime(message.getTimeMillis()));
            received_username_textView.setText(message.getSenderUsername());

            // Insert the profile image from the URL into the ImageView.
            Function.displayRoundImageFromUrl(mContext, message.getUserImage_url(), received_profileImage_imageView);
        }

    }


    private class ServerMessageHolder extends RecyclerView.ViewHolder {
        TextView message_textView;

        ServerMessageHolder(View itemView) {
            super(itemView);

            message_textView = (TextView) itemView.findViewById(R.id.messageItem_serverMsg_textView);
        }

        void bind(MessageItem message) {
            message_textView.setText(message.getMessage());
        }

    }


}

