package com.android.canaria.recyclerView;

import android.content.Context;
import android.content.Intent;
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

import com.android.canaria.Function;
import com.android.canaria.ImageActivity;
import com.android.canaria.R;
import com.android.canaria.UserProfileActivity;
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
        ImageView sent_image_imageView;
        FrameLayout sent_image_frameLayout;
        LinearLayout sent_video_linearLayout;

        SentMessageHolder(View itemView) {
            super(itemView);

            sent_message_textView = (TextView) itemView.findViewById(R.id.sent_message_textView);
            sent_time_textView = (TextView) itemView.findViewById(R.id.sent_time_textView);
            sent_image_imageView = (ImageView) itemView.findViewById(R.id.sent_image_imageView);

            sent_image_frameLayout = (FrameLayout)itemView.findViewById(R.id.sent_image_frameLayout);
            sent_video_linearLayout = (LinearLayout)itemView.findViewById(R.id.sent_video_info_linearLayout);
        }

        void bind(final MessageItem message) {

            sent_video_linearLayout.setVisibility(View.GONE);

            //메시징 시각을 표시한다 - 공통
            sent_time_textView.setText(Function.formatTime(message.getTimeMillis()));

            //메시지 내용이 텍스트인지 이미지인지 확인한다
            String image_name = message.getImage_name();
            if(image_name.equals("N") || image_name.equals("")){//텍스트를 보냈을 경우

                //이미지뷰를 숨기고, 텍스트뷰에 텍스트를 넣는다
                sent_message_textView.setVisibility(View.VISIBLE);
                sent_image_frameLayout.setVisibility(View.GONE);
                sent_message_textView.setText(message.getMessage());

            }else{//이미지를 보냈을 경우

                //텍스트뷰를 숨기고, 이미지뷰에 이미지를 넣는다
                sent_message_textView.setVisibility(View.GONE);
                sent_image_frameLayout.setVisibility(View.VISIBLE);
                sent_image_frameLayout.layout(0,0,0,0);//이미지뷰의 레이아웃을 초기화
                sent_image_imageView.layout(0,0,0,0); //이미지뷰의 레이아웃을 초기화
//              layout(left, top, right, bottom) :  Assign a size and position to a view

//                Context context, String url, ImageView imageView,int width, int height
                Function.displayResizedImage(mContext, message.getImage_url(), sent_image_imageView);
            }


            //이미지를 클릭하면 -> 큰 이미지 화면이 나타난다
            sent_image_frameLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //해당 이미지의 url 을 전달한다
                    Intent intent = new Intent(mContext, ImageActivity.class);
                    intent.putExtra("url", message.getImage_url());
                    mContext.startActivity(intent);

                }
            });

        }

    }


    private class ReceivedMessageHolder extends RecyclerView.ViewHolder {
        TextView received_message_textView, received_time_textView, received_username_textView;
        ImageView received_profileImage_imageView, received_image_imageView;
        FrameLayout received_image_frameLayout;
        LinearLayout received_video_linearLayout;

        ReceivedMessageHolder(View itemView) {
            super(itemView);
            received_message_textView = (TextView) itemView.findViewById(R.id.received_message_textView);
            received_time_textView = (TextView) itemView.findViewById(R.id.received_time_textView);
            received_username_textView = (TextView) itemView.findViewById(R.id.received_username_textView);
            received_profileImage_imageView = (ImageView) itemView.findViewById(R.id.received_profileImage_imageView);
            received_image_imageView = (ImageView) itemView.findViewById(R.id.received_image_imageView);

            received_image_frameLayout = (FrameLayout)itemView.findViewById(R.id.received_image_frameLayout);
            received_video_linearLayout = (LinearLayout)itemView.findViewById(R.id.received_video_linearLayout);
        }


        void bind(final MessageItem message) {

            received_video_linearLayout.setVisibility(View.GONE);

            //메시징 시각, 수신자 이름, 수신자 프로필사진을 각각의 뷰에 넣는다 - 공통
            received_time_textView.setText(Function.formatTime(message.getTimeMillis()));
            received_username_textView.setText(message.getSenderUsername());
            Function.displayRoundImageFromUrl(mContext, message.getUserImage_url(), received_profileImage_imageView);

            //메시지 내용이 텍스트인지 이미지인지 확인한다
            String image_name = message.getImage_name();
            if(image_name.equals("N") || image_name.equals("")){//텍스트 메시지를 받았을 경우

                //이미지뷰를 숨기고, 텍스트뷰에 텍스트를 넣는다
                received_message_textView.setVisibility(View.VISIBLE);
                received_image_frameLayout.setVisibility(View.GONE);
                received_message_textView.setText(message.getMessage());

            }else{//이미지를 받았을 경우

                //텍스트뷰를 숨기고, 이미지뷰에 이미지를 넣는다
                received_message_textView.setVisibility(View.GONE);
                received_image_frameLayout.setVisibility(View.VISIBLE);
                received_image_frameLayout.layout(0,0,0,0); //이미지뷰의 레이아웃을 초기화
                received_image_imageView.layout(0,0,0,0); //이미지뷰의 레이아웃을 초기화
//              layout(left, top, right, bottom) :  Assign a size and position to a view

//                Context context, String url, ImageView imageView,int width, int height
                Function.displayResizedImage(mContext, message.getImage_url(), received_image_imageView);


                /*
                <received_image_imageView.layout(0,0,0,0); 쓴 이유>
                * The list items are being reused, and when you scroll down you bind a larger image which makes the ImageView layout taller (wrap_content).
                * When you scroll back up Glide sees that taller size and tries to load an image that has that size.
                * wrap_content only works if the didn't have a layout before, otherwise Glide reads the laid-out width/height and uses that as the target size.
                * I usually recommend a holder.iv.layout(0,0,0,0) to reset the size of the view/target and behave as if the list item was just inflated.
                *
                * */
            }



            //다른 사람의 프로필 사진을 클릭하면 -> 프로필 화면이 나타난다
            received_profileImage_imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(mContext, UserProfileActivity.class);
                    intent.putExtra("friend_id", message.getSender_id());
                    intent.putExtra("friend_username", message.getSenderUsername());
                    mContext.startActivity(intent);

                }
            });



            //이미지를 클릭하면 -> 큰 이미지 화면이 나타난다
            received_image_frameLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //해당 이미지의 url 을 전달한다
                    Intent intent = new Intent(mContext, ImageActivity.class);
                    intent.putExtra("url", message.getImage_url());
                    mContext.startActivity(intent);

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

