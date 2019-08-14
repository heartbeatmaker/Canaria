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
import android.widget.ProgressBar;
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
    private static final int VIEW_TYPE_VIDEO_SENT = 3;
    private static final int VIEW_TYPE_MESSAGE_RECEIVED = 4;
    private static final int VIEW_TYPE_IMAGE_RECEIVED = 5;
    private static final int VIEW_TYPE_VIDEO_RECEIVED = 6;
    private static final int VIEW_TYPE_MESSAGE_SERVER = 7;

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
                }else{ //이미지 or 썸네일 이미지를 포함하고 있을 때

                    String image_name = message.getImage_name();

//                    썸네일 파일 이름 = 날짜_video_index_원래이름.jpg or (1).jpg
                    String[] image_name_split = image_name.split("_");

                    if(image_name_split[1].equals("video")){ //이 이미지 = 비디오 파일의 썸네일

                        Log.d("msg", "VIEW_TYPE_IMAGE_SENT");
                        return VIEW_TYPE_VIDEO_SENT;

                    }else{//이 이미지 = 일반 이미지

                        Log.d("msg", "VIEW_TYPE_IMAGE_SENT");
                        return VIEW_TYPE_IMAGE_SENT;

                    }

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
            case VIEW_TYPE_VIDEO_SENT:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_video_sent, parent, false);
                return new SentVideoHolder(view);
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
            case VIEW_TYPE_VIDEO_SENT:
                ((SentVideoHolder) holder).bind(message);
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

            //message.getImage_name() = 파일이름1;파일이름2;파일이름3... 다중이미지의 경우 이렇게 파일이름이 string 형태로 이어져 있다
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

            //message.getImage_name() = 파일이름1;파일이름2;파일이름3... 다중이미지의 경우 이렇게 파일이름이 string 형태로 이어져 있다
            Function.displayCollageImages(mContext, message.getRoom_id(), message.getImage_name(), received_image_collageView);

            // Format the stored timestamp into a readable String using method.
            received_time_textView.setText(Function.formatTime(message.getTimeMillis()));
            received_username_textView.setText(message.getSenderUsername());

            // Insert the profile image from the URL into the ImageView.
            Function.displayRoundImageFromUrl(mContext, message.getUserImage_url(), received_profileImage_imageView);

        }

    }


    private class SentVideoHolder extends RecyclerView.ViewHolder {
        ImageView sent_video_thumbnail_imageView, sent_video_playBtn_imageView;
        ProgressBar sent_video_progressBar;
        TextView sent_video_textView, sent_video_time_textView;

        SentVideoHolder(View itemView) {
            super(itemView);

            sent_video_thumbnail_imageView = itemView.findViewById(R.id.sent_video_imageView);
            sent_video_playBtn_imageView = itemView.findViewById(R.id.sent_video_playBtn_imageView);
            sent_video_progressBar = itemView.findViewById(R.id.sent_video_progressBar);
            sent_video_textView = itemView.findViewById(R.id.sent_video_textView);
            sent_video_time_textView = itemView.findViewById(R.id.sent_video_time_textView);
        }

        void bind(final MessageItem message) {

            //썸네일 이미지를 띄운다
            Glide.with(mContext).asBitmap().load(message.getThumbImage_url()).into(sent_video_thumbnail_imageView);

            //메시지 보낸 시각 표시
            sent_video_time_textView.setText(Function.formatTime(message.getTimeMillis()));

            //프로그레스 바를 보여준다
            sent_video_progressBar.setVisibility(View.VISIBLE);

            //처음 띄워줄 때: 재생버튼을 숨긴다
            sent_video_playBtn_imageView.setVisibility(View.GONE);
            sent_video_textView.setText("Encoding");

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

