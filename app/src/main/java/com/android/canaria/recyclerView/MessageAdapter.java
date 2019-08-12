package com.android.canaria.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.canaria.Function;
import com.android.canaria.ImageActivity;
import com.android.canaria.R;
import com.android.canaria.UserProfileActivity;
import com.android.canaria.db.DBHelper;
import com.android.canaria.view.CollageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;


import java.util.ArrayList;

public class MessageAdapter extends RecyclerView.Adapter {

    private static final String TAG = "MsgRecyclerViewAdapter";

    private ArrayList<MessageItem> mItemArrayList;
    private Context mContext;

    private int userId;

    private static final int VIEW_TYPE_MESSAGE_SENT = 1;
    private static final int VIEW_TYPE_IMAGE_SENT = 2;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 3;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 4;
    private static final int VIEW_TYPE_MESSAGE_SERVER = 5;

    public MessageAdapter(ArrayList<MessageItem> mItemArrayList, Context mContext) {
        this.mItemArrayList = mItemArrayList;
        this.mContext = mContext;

        userId = Integer.valueOf(Function.getString(mContext, "user_id"));
    }


    // Determines the appropriate ViewType according to the sender of the message.
    @Override
    public int getItemViewType(int position) {
        MessageItem message = (MessageItem) mItemArrayList.get(position);

//        Log.d("msg", "sender_id="+message.getSender_id()+" / sender_username="+message.getSenderUsername());

        //서버메시지
        if (message.getSender_id()==0 && message.getSenderUsername().equals("server")) {
            Log.d("msg", "server msg");
            // If server sent the message or etc(ex. dateTime indicator)
            return VIEW_TYPE_MESSAGE_SERVER;
        }else{

            if(message.getSender_id() == userId){ //내가보낸 메시지
                // If the current user is the sender of the message

                if(message.getImage_name().equals("N") || message.getImage_name().equals("")){
                    Log.d("msg", "VIEW_TYPE_MESSAGE_SENT");
                    return VIEW_TYPE_MESSAGE_SENT;
                }else{
                    Log.d("msg", "VIEW_TYPE_IMAGE_SENT");
                    return VIEW_TYPE_IMAGE_SENT;
                }

            }else{ //받은 메시지
                // If some other user sent the message

                if(message.getImage_name().equals("N") || message.getImage_name().equals("")){
                    Log.d("msg", "VIEW_TYPE_MESSAGE_RECEIVED");
                    return VIEW_TYPE_MESSAGE_RECEIVED;
                }else{
                    Log.d("msg", "VIEW_TYPE_IMAGE_RECEIVED");
                    return VIEW_TYPE_IMAGE_RECEIVED;
                }

            }
        }
    }


    // Inflates the appropriate layout according to the ViewType.
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;

        switch (viewType){
            case VIEW_TYPE_MESSAGE_SERVER:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_notification, parent, false);
                return new ServerMessageHolder(view);
            case VIEW_TYPE_MESSAGE_SENT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_sent, parent, false);
                return new SentMessageHolder(view);
            case VIEW_TYPE_IMAGE_SENT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_image_sent, parent, false);
                return new SentImageHolder(view);
            case VIEW_TYPE_MESSAGE_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_received, parent, false);
                return new ReceivedMessageHolder(view);
            case VIEW_TYPE_IMAGE_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_image_received, parent, false);
                return new ReceivedImageHolder(view);
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
            case VIEW_TYPE_IMAGE_SENT:
                ((SentImageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_IMAGE_RECEIVED:
                ((ReceivedImageHolder) holder).bind(message);
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

        void bind(final MessageItem message) {
            sent_message_textView.setText(message.getMessage());
            sent_time_textView.setText(Function.formatTime(message.getTimeMillis()));
        }

    }


    private class SentImageHolder extends RecyclerView.ViewHolder {
        CollageView sent_image_collageView;
        TextView sent_time_textView;

        SentImageHolder(View itemView) {
            super(itemView);

            sent_image_collageView = (CollageView)itemView.findViewById(R.id.sent_image_collageView);
            sent_time_textView = (TextView) itemView.findViewById(R.id.sent_image_time_textView);
        }

        void bind(final MessageItem message) {

            Function.displayCollageImages(mContext, message.getRoom_id(), message.getImage_name(), sent_image_collageView);

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


        void bind(final MessageItem message) {
            received_message_textView.setText(message.getMessage());

            // Format the stored timestamp into a readable String using method.
            received_time_textView.setText(Function.formatTime(message.getTimeMillis()));
            received_username_textView.setText(message.getSenderUsername());

            // Insert the profile image from the URL into the ImageView.
            Function.displayRoundImageFromUrl(mContext, message.getUserImage_url(), received_profileImage_imageView);

        }

    }



    private class ReceivedImageHolder extends RecyclerView.ViewHolder {
        TextView received_time_textView, received_username_textView;
        ImageView received_profileImage_imageView;
        CollageView received_image_collageView;

        ReceivedImageHolder(View itemView) {
            super(itemView);
            received_image_collageView = (CollageView) itemView.findViewById(R.id.received_image_collageView);
            received_time_textView = (TextView) itemView.findViewById(R.id.received_image_time_textView);
            received_username_textView = (TextView) itemView.findViewById(R.id.received_image_username_textView);
            received_profileImage_imageView = (ImageView) itemView.findViewById(R.id.received_image_profileImage_imageView);
        }


        void bind(final MessageItem message) {

            Function.displayCollageImages(mContext, message.getRoom_id(), message.getImage_name(), received_image_collageView);

            // Format the stored timestamp into a readable String using method.
            received_time_textView.setText(Function.formatTime(message.getTimeMillis()));
            received_username_textView.setText(message.getSenderUsername());

            // Insert the profile image from the URL into the ImageView.
            Function.displayRoundImageFromUrl(mContext, message.getUserImage_url(), received_profileImage_imageView);

            received_image_collageView.setOnPhotoClickListener(new CollageView.OnPhotoClickListener() {
                @Override
                public void onPhotoClick(int position) {

                    Toast.makeText(mContext, "position"+position, Toast.LENGTH_SHORT).show();

                }
            });

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

