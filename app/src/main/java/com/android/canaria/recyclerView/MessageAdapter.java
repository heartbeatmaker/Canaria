package com.android.canaria.recyclerView;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.Image;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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

import com.android.canaria.ChatActivity;
import com.android.canaria.Function;
import com.android.canaria.ImageActivity;
import com.android.canaria.Main_Fragment2;
import com.android.canaria.R;
import com.android.canaria.UserProfileActivity;
import com.android.canaria.connect_to_server.CountingRequestBody;
import com.android.canaria.connect_to_server.MainService;
import com.android.canaria.db.DBHelper;
import com.android.canaria.view.CollageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.Target;
import com.dinuscxj.progressbar.CircleProgressBar;
import com.iceteck.silicompressorr.SiliCompressor;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

    DBHelper dbHelper;
    SQLiteDatabase db;

    String compressed_filePath = "";

    VideoUploader videoUploader;


//    private static final int CPU_COUNT = Runtime.getRuntime().availableProcessors();
//    private static final int CORE_POOL_SIZE = CPU_COUNT + 1;
//    private static final int MAXIMUM_POOL_SIZE = CPU_COUNT * 2 + 1;
//    private static final int KEEP_ALIVE = 1;
//
//    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
//        private final AtomicInteger mCount = new AtomicInteger(1);
//
//        public Thread newThread(Runnable r) {
//            return new Thread(r, "AsyncTask #" + mCount.getAndIncrement());
//        }
//    };
//
//    private static final BlockingQueue<Runnable> sPoolWorkQueue =
//            new LinkedBlockingQueue<Runnable>(128);
//
//    public static final Executor executor
//            = new ThreadPoolExecutor(CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE,
//            TimeUnit.SECONDS, sPoolWorkQueue, sThreadFactory);



    public MessageAdapter(ArrayList<MessageItem> mItemArrayList, Context mContext) {
        this.mItemArrayList = mItemArrayList;
        this.mContext = mContext;

        userId = Integer.valueOf(Function.getString(mContext, "user_id"));

        //DB를 연다
        dbHelper = new DBHelper(mContext, Function.dbName, null, Function.dbVersion);
        db = dbHelper.getWritableDatabase();
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

                        Log.d("msg", "VIEW_TYPE_VIDEO_SENT");
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

                }else{//이미지 or 썸네일 이미지를 포함하고 있을 때

                    String image_name = message.getImage_name();

//                    썸네일 파일 이름 = 날짜_video_index_원래이름.jpg or (1).jpg
                    String[] image_name_split = image_name.split("_");

                    if(image_name_split[1].equals("video")){ //이 이미지 = 비디오 파일의 썸네일

                        Log.d("msg", "VIEW_TYPE_VIDEO_RECEIVED");
                        return VIEW_TYPE_VIDEO_RECEIVED;

                    }else{//이 이미지 = 일반 이미지

                        Log.d("msg", "VIEW_TYPE_IMAGE_RECEIVED");
                        return VIEW_TYPE_IMAGE_RECEIVED;

                    }
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
            case VIEW_TYPE_VIDEO_RECEIVED:
                view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.message_item_image_received, parent, false);
                return new ReceivedVideoHolder(view);
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
                ((SentVideoHolder) holder).setIsRecyclable(false);
                ((SentVideoHolder) holder).bind(message);
                break;
            case VIEW_TYPE_MESSAGE_RECEIVED:
                ((ReceivedMessageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_IMAGE_RECEIVED:
                ((ReceivedImageHolder) holder).bind(message);
                break;
            case VIEW_TYPE_VIDEO_RECEIVED:
                ((ReceivedVideoHolder) holder).setIsRecyclable(false);
                ((ReceivedVideoHolder) holder).bind(message);
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



    private class ReceivedVideoHolder extends RecyclerView.ViewHolder {
        TextView received_time_textView, received_username_textView;
        ImageView received_profileImage_imageView, received_video_thumbnail_imageView;


        //동영상의 다운로드 상태에 따라 바뀌는 변수 - 디폴트: GONE(숨겨져 있다)
        ImageView received_video_playBtn_imageView;
        CircleProgressBar received_video_circleProgressBar;
        TextView received_video_textView;

        ReceivedVideoHolder(View itemView) {
            super(itemView);

            received_time_textView = (TextView) itemView.findViewById(R.id.received_image_time_textView);
            received_username_textView = (TextView) itemView.findViewById(R.id.received_image_username_textView);
            received_profileImage_imageView = (ImageView) itemView.findViewById(R.id.received_image_profileImage_imageView);

            received_video_thumbnail_imageView = itemView.findViewById(R.id.received_video_imageView);
            received_video_playBtn_imageView = itemView.findViewById(R.id.received_video_playBtn_imageView);
            received_video_circleProgressBar = itemView.findViewById(R.id.received_video_circleProgressBar);
            received_video_textView = itemView.findViewById(R.id.received_video_textView);
        }


        void bind(final MessageItem message) {

            final int room_id = message.getRoom_id();
//            final int db_id = message.getDb_id(); -- 이건 SentVideoHolder 에서만 적용된다. 받은 메시지 아이템에는 db_id 값이 항상 0이다
//            final String video_file_path = message.getVideo_path(); -- 동영상의 로컬경로. 아직 동영상을 다운받지 않았다면, 이 값이 비어있다
            final String video_server_path = message.getVideo_server_path(); // 동영상의 서버경로. 받은 메시지라면 이 값이 반드시 저장되어 있다


            /*동영상 관련 정보를 띄워주는 뷰*/

            //썸네일 이미지를 띄운다
            Glide.with(mContext)
                    .asBitmap()
                    .load(message.getThumbImage_url())
                    .listener(new RequestListener<Bitmap>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                    return false;
                }


                //이미지 업로드가 완료되었을 때
                @Override
                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {

                    Log.d("이미지", "보낸 동영상) 썸네일이 화면에 뜸");

                    //이 동영상이 이미 서버에 업로드 되어 있는지 확인한다 -- video_path 컬럼에 값이 있는지 검사한다

                    Cursor cursor = db.rawQuery
                            ("SELECT video_path FROM chat_logs WHERE room_id='"+room_id+"' AND video_server_path='" + video_server_path +"';", null);

                    cursor.moveToFirst();

                    // db에 저장된 동영상의 로컬 경로를 가져온다
                    String video_path = cursor.getString(0);

                    Log.d("이미지", "받은 동영상) db에 저장된 video_path = "+video_path);

                    if(video_path.length() > 10){//로컬 경로가 있음 = 이미 영상을 다운 받았음

                        Log.d("이미지", "받은 동영상) 사용자가 이 동영상을 이미 다운 받았음");

                        //재생버튼을 보여준다
                        received_video_playBtn_imageView.setVisibility(View.VISIBLE);

                        //재생 시간을 보여준다
                        received_video_textView.setVisibility(View.VISIBLE);
                        received_video_textView.setText(getDuration(video_path));

                        Log.d("이미지", "받은 동영상) 재생버튼과 재생시간이 썸네일 위에 보여야됨");

                    }else{//로컬 경로가 없음 = 영상을 다운받은 적 없음
                        Log.d("이미지", "받은 동영상) 사용자가 이 동영상을 아직 다운받지 않았음");

                        //다운로드 버튼을 보여준다
                        received_video_playBtn_imageView.setImageResource(R.drawable.ic_play_for_work_black_24dp);
                        received_video_playBtn_imageView.setVisibility(View.VISIBLE);
                    }

                    return false;
                }
            }).into(received_video_thumbnail_imageView);


            received_video_thumbnail_imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //이 동영상이 이미 서버에 업로드 되어 있는지 확인한다 -- video_path 컬럼에 값이 있는지 검사한다

                    Cursor cursor = db.rawQuery
                            ("SELECT id, video_path FROM chat_logs WHERE room_id='"+room_id+"' AND video_server_path='" + video_server_path +"';", null);

                    cursor.moveToFirst();

                    // db에 저장된 id, 동영상의 로컬 경로를 가져온다
                    int db_id = cursor.getInt(0);
                    String video_path = cursor.getString(1);

                    if(video_path.length() > 10){//로컬 경로가 있음 = 이미 영상을 다운 받았음

                        //클릭하면 크게보는 화면이 뜨도록 만든다
                        //원본 동영상의 path 를 전달한다

                        //해당 이미지의 url 을 전달한다
                        Intent intent = new Intent(mContext, ImageActivity.class);

                        intent.putExtra("type", "video"); //데이터 타입을 알려준다. 이미지 or 동영상
                        intent.putExtra("room_id", room_id);
                        intent.putExtra("thumbnail_filename", message.getImage_name());
                        intent.putExtra("video_path", video_path);

                        mContext.startActivity(intent);

                    }else{//로컬 경로가 없음 = 영상을 다운받은 적 없음

                        //다운로드 쓰레드를 시작한다
                        VideoDownloader videoDownloader = new VideoDownloader(db_id, received_video_circleProgressBar,received_video_playBtn_imageView, received_video_textView);
                        videoDownloader.execute();

                    }

                }
            });


            /*기본 정보를 띄워주는 뷰*/
            // Format the stored timestamp into a readable String using method.
            //메시지를 받은 시각, 보낸사람의 이름을 표시
            received_time_textView.setText(Function.formatTime(message.getTimeMillis()));
            received_username_textView.setText(message.getSenderUsername());

            // Insert the profile image from the URL into the ImageView.
            //메시지 보낸 사람의 프로필사진을 표시
            Function.displayRoundImageFromUrl(mContext, message.getUserImage_url(), received_profileImage_imageView);

        }

    }




    private class SentVideoHolder extends RecyclerView.ViewHolder {

        ImageView sent_video_thumbnail_imageView;
        TextView sent_video_time_textView;

        //동영상의 업로드 상태에 따라 바뀌는 변수 - 디폴트: GONE(숨겨져 있다)
        ImageView sent_video_playBtn_imageView;
        CircleProgressBar sent_video_circleProgressBar;
        ProgressBar sent_video_progressBar;
        TextView sent_video_textView;


        SentVideoHolder(View itemView) {
            super(itemView);

            sent_video_thumbnail_imageView = itemView.findViewById(R.id.sent_video_imageView);
            sent_video_playBtn_imageView = itemView.findViewById(R.id.sent_video_playBtn_imageView);
            sent_video_circleProgressBar = itemView.findViewById(R.id.sent_video_circleProgressBar);
            sent_video_progressBar = itemView.findViewById(R.id.sent_video_progressBar);
            sent_video_textView = itemView.findViewById(R.id.sent_video_textView);
            sent_video_time_textView = itemView.findViewById(R.id.sent_video_time_textView);

        }

        void bind(final MessageItem message) {

            final int room_id = message.getRoom_id();
            final int db_id = message.getDb_id();
            final String video_file_path = message.getVideo_path();


            //썸네일 이미지를 띄운다
            Glide.with(mContext)
                    .asBitmap()
                    .load(message.getThumbImage_url())
                    .listener(new RequestListener<Bitmap>() {
                @Override
                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                    return false;
                }


                //이미지 업로드가 완료되었을 때
                @Override
                public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {

                    Log.d("이미지", "보낸 동영상) 썸네일이 화면에 뜸");

                    //이 동영상이 이미 서버에 업로드 되어 있는지 확인한다
                    Cursor cursor = db.rawQuery
                            ("SELECT video_path FROM chat_logs WHERE id='" + db_id +"';", null);

                    cursor.moveToFirst();
                    String video_path = cursor.getString(0);

                    Log.d("이미지", "보낸 동영상) db에 저장된 video_path = "+video_path);

                    if(video_path.equals("yet")){ //이 썸네일의 원본 동영상은 서버에 업로드 되지 않았음

                        Log.d("이미지", "보낸 동영상) 이 썸네일의 원본 동영상은 서버에 업로드 되지 않았음");

                        //동영상 압축 -> 업로드 쓰레드를 순차적으로 진행한다
                        //업로드 쓰레드는 videoCompressor 클래스 안에서 실행된다
                        VideoCompressor videoCompressor = new VideoCompressor(mContext, video_file_path, sent_video_progressBar, sent_video_textView,
                                sent_video_circleProgressBar, sent_video_playBtn_imageView, room_id, db_id);


                        //압축 -> 업로드 절차는 순차적으로 실행되어야 한다. 다만 여러개의 동영상 업로드 절차는 병렬로 실행되어야 한다!!
                        videoCompressor.execute();


                    }else if(video_path.length() > 10){//이 썸네일의 원본 동영상은 이미 서버에 업로드 되었음

                        Log.d("이미지", "보낸 동영상) 이 썸네일의 원본 동영상은 이미 서버에 업로드 되었음");

                        //재생버튼을 보여준다
                        sent_video_playBtn_imageView.setVisibility(View.VISIBLE);

                        //재생 시간을 보여준다
                        sent_video_textView.setVisibility(View.VISIBLE);
                        sent_video_textView.setText(getDuration(video_file_path));

                        Log.d("이미지", "보낸 동영상) 재생버튼과 재생시간이 썸네일 위에 보여야됨");
                    }

                    return false;
                }
            }).into(sent_video_thumbnail_imageView);


            sent_video_thumbnail_imageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //이 동영상이 이미 서버에 업로드 되어 있는지 확인한다
                    Cursor cursor = db.rawQuery
                            ("SELECT video_path FROM chat_logs WHERE id='" + db_id +"';", null);

                    cursor.moveToFirst();
                    String video_path = cursor.getString(0);


                    if(video_path.length() > 10){//이 썸네일의 원본 동영상은 이미 서버에 업로드 되었음

                        //클릭하면 크게보는 화면이 뜨도록 만든다
                        //원본 동영상의 path 를 전달한다

                        //해당 이미지의 url 을 전달한다
                        Intent intent = new Intent(mContext, ImageActivity.class);

                        intent.putExtra("type", "video"); //데이터 타입을 알려준다. 이미지 or 동영상
                        intent.putExtra("room_id", room_id);
                        intent.putExtra("thumbnail_filename", message.getImage_name());
                        intent.putExtra("video_path", video_file_path);

                        mContext.startActivity(intent);


                    }else{//이 썸네일의 원본 동영상은 서버에 업로드 되지 않았음 -- 업로드 중이거나, 업로드 실패한 것

                        //클릭이벤트가 발생하지 않도록 한다

                    }


                }
            });



            //메시지 보낸 시각 표시
            sent_video_time_textView.setText(Function.formatTime(message.getTimeMillis()));

//            //프로그레스 바를 보여준다
//            sent_video_progressBar.setVisibility(View.VISIBLE);
//
//            //처음 띄워줄 때: 재생버튼을 숨긴다
//            sent_video_playBtn_imageView.setVisibility(View.GONE);
//            sent_video_textView.setText("Encoding");

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






    //동영상 파일을 압축하는 클래스
    class VideoCompressor extends AsyncTask<Void, String, String> {

        Context mContext;
        String origin_video_file_path; //원본 동영상 파일이 저장되어 있는 로컬 경로

        //Environment.getExternalStorageDirector() : 저장공간의 기본경로를 가져옴. 기기마다 저장공간에 대한 경로 명이 상이함
        String MY_FOLDER = "/Canaria"; //내가 원하는 저장 경로(폴더 이름)
        String destination_directory = Environment.getExternalStorageDirectory().toString() + MY_FOLDER;


        //파일 압축하는 동안 사용자에게 보여주는 정보를 담는 곳
        ProgressBar progressBar;
        TextView encoding_info_textView;


        //업로드 쓰레드에서 필요한 변수
        CircleProgressBar circleProgressBar;
        ImageView playBtn_imageView;
        int room_id, message_db_id;


        public VideoCompressor(Context context, String origin_video_file_path, ProgressBar progressBar, TextView encoding_info_textView
        , CircleProgressBar circleProgressBar,
                               ImageView playBtn_imageView, int room_id, int message_db_id) {
            this.mContext = context;
            this.origin_video_file_path = origin_video_file_path;

            this.progressBar = progressBar;
            this.encoding_info_textView = encoding_info_textView;

            this.circleProgressBar = circleProgressBar;
            this.playBtn_imageView = playBtn_imageView;
            this.room_id = room_id;
            this.message_db_id = message_db_id;

            Log.d("이미지", "Video compressor) 원본 동영상의 path = "+origin_video_file_path);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("이미지", "Video compressor) onPreExecute()");

            //화면에 프로그레스바를 띄우고, 압축중이라는 메시지를 보여준다
            progressBar.setVisibility(View.VISIBLE);
            encoding_info_textView.setVisibility(View.VISIBLE);
            encoding_info_textView.setText("Encoding..");

            Log.d("이미지", "Video compressor) 화면에 프로그레스바, 압축중 메시지가 보여야됨");
        }

        @Override
        protected String doInBackground(Void... voids) {
            Log.d("이미지", "Video compressor) doInBackground");


            String compressed_filePath = null;
            try {

                Log.d("이미지", "Video compressor) 동영상 압축 시작");

                //비트레이트: 3000k
                //내 폰으로 찍은 영상 기준, 약 27퍼센트 수준으로 용량을 낮춤
                compressed_filePath = SiliCompressor.with(mContext).compressVideo(origin_video_file_path, destination_directory, 720, 480, 3000000);

                Log.d("이미지", "Video compressor) 동영상 압축 완료");

            } catch (URISyntaxException e) {
                e.printStackTrace();
            }

            return compressed_filePath;

        }


        @Override
        protected void onPostExecute(String compressedFilePath) {
            super.onPostExecute(compressedFilePath);


            Log.d("이미지", "Video compressor) onPostExecute() 압축된 파일의 경로 = "+compressedFilePath);

            compressed_filePath = compressedFilePath;

            //압축 완료된 파일
            File imageFile = new File(compressedFilePath);

            //용량 검사
            float length = imageFile.length() / 1024f; // Size in KB
            String value;
            if (length >= 1024)
                value = length / 1024f + " MB";
            else
                value = length + " KB";
//            String text = String.format(Locale.US, "%s\nName: %s\nSize: %s", getString(R.string.video_compression_complete), imageFile.getName(), value);
//            compressionMsg.setVisibility(View.GONE);
//            picDescription.setVisibility(View.VISIBLE);
//            picDescription.setText(text);
            Log.d("이미지", "Video compressor) 압축 파일 용량 = "+value);


            //썸네일 위에 떠 있던 프로그레스바를 숨기고, 압축중이라는 메시지를 없앤다
            progressBar.setVisibility(View.GONE);
            encoding_info_textView.setText("");

            Log.d("이미지", "Video compressor) 썸네일 위에 프로그레스바, 압축중 메시지가 없어야 됨");


            VideoUploader videoUploader = new VideoUploader(mContext, compressed_filePath, circleProgressBar, progressBar,
                    encoding_info_textView, playBtn_imageView, room_id, message_db_id);

            //업로드 쓰레드를 실행시킨다
            videoUploader.execute();

        }
    }



    private String getDuration(String path){

        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        retriever.setDataSource(path);
        String time = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long timeInmillisec = Long.parseLong( time );
        long duration = timeInmillisec / 1000;
        long hours = duration / 3600;
        long minutes = (duration - hours * 3600) / 60;
        long seconds = duration - (hours * 3600 + minutes * 60);

        retriever.release();

        return minutes + " : " + seconds;
    }




    //압축된 동영상 파일을 서버에 업로드 하는 클래스
    class VideoUploader extends AsyncTask<Void, Object, String> {

        Context mContext;
        String compressed_video_file_path; //압축된 동영상 파일이 저장되어 있는 로컬 경로
        int room_id;


        //파일을 업로드 하는 동안 사용자에게 보여주는 정보를 담는 곳
        CircleProgressBar circleProgressBar; //업로드 경과를 표시해줌
        ProgressBar progressBar; //업로드 후 서버의 응답을 기다림
        TextView upload_info_textView;
        ImageView playBtn_imageView;

        int message_db_id;

        String upLoadServerUri = "http://15.164.193.65/multi_fileUpload.php";//서버컴퓨터의 ip주소


        public VideoUploader(Context context, String origin_video_file_path, CircleProgressBar circleProgressBar,
                             ProgressBar progressBar, TextView encoding_info_textView, ImageView playBtn_imageView, int room_id, int message_db_id) {
            this.mContext = context;
            this.compressed_video_file_path = origin_video_file_path;

            this.circleProgressBar = circleProgressBar;
            this.progressBar = progressBar;
            this.upload_info_textView = encoding_info_textView;
            this.playBtn_imageView = playBtn_imageView;

            this.room_id = room_id;
            this.message_db_id = message_db_id;

            Log.d("이미지", "Video uploader 생성자) 압축한 동영상을 서버에 업로드 할 것임. video_file_path="+compressed_video_file_path);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Log.d("이미지", "Video uploader) onPreExecute()");


            //화면에 원형 프로그레스바를 띄우고, 0% 업로드 되었음을 알려준다
            circleProgressBar.setVisibility(View.VISIBLE);
            upload_info_textView.setText("Uploading..");

            Log.d("이미지", "Video uploader) 화면에 원형 프로그레스바가 보여야 함. 그 안에 0 이라고 써있어야 함");
        }

        @Override
        protected String doInBackground(Void... voids) {
            Log.d("이미지", "Video uploader) doInBackground");


            File file = new File(compressed_video_file_path);

            //동영상 파일의 이름을 추출한다
            String[] path_split = compressed_video_file_path.split("/");
            String file_name = path_split[path_split.length-1];
            Log.d("이미지", "Video uploader) 동영상의 filename = "+file_name);


            OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(40, TimeUnit.SECONDS)
                    .build();

            MultipartBody.Builder mRequestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM);

            mRequestBody.addFormDataPart("room_id", String.valueOf(room_id));
            mRequestBody.addFormDataPart("count", String.valueOf(1));

            RequestBody imageBody = RequestBody.create(MultipartBody.FORM, file);
            //key, 서버가 저장할 file 이름, 이미지파일
            //다중이미지 업로드 할 때, image 뒤의 인덱스로 각각의 이미지를 구분한다
            //동영상을 올릴 때는 파일이 한 개 이기 때문에, 0으로 하드코딩 해 놓는다
            mRequestBody.addFormDataPart("image"+0, file_name, imageBody);


            RequestBody requestBody = mRequestBody.build();

            Log.d("이미지", "Video uploader) Request body를 만듦");


            // Decorate the request body to keep track of the upload progress
            CountingRequestBody countingBody = new CountingRequestBody(requestBody,
                    new CountingRequestBody.Listener() {

                        @Override
                        public void onRequestProgress(long bytesWritten, long contentLength) {
                            float percentage = 100f * bytesWritten / contentLength;

                            Log.d("이미지", "Video uploader) 업로드 percentage 가 화면에 떠야함. percentage = "+ percentage);

                            //화면에 업로드 퍼센트를 띄워준다
                            publishProgress("upload", (int)percentage);
                        }
                    });


            final Request request = new Request.Builder()
                    .url(upLoadServerUri)
                    .post(countingBody)
                    .build();


            String responseMsg = "";
            try {

                //서버에 요청을 보낸다
                Response mResponse = mOkHttpClient.newCall(request).execute();
                if (!mResponse.isSuccessful()) throw new IOException();

                //서버로부터의 응답
                responseMsg = mResponse.body().string();
                Log.d("이미지", "Video uploader) 서버에 업로드 요청 보냄. response msg = " + responseMsg);

            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d("이미지",ex);
            }


            //업로드 완료 후 작업을 수행중이라고 화면에 띄운다
            publishProgress("post_upload");
            Log.d("이미지", "Video uploader) post_upload. 썸네일 위에 프로그레스바, processing 이라는 메시지가 보여야됨");


            //응답 메시지(json)를 파싱한다
            //동영상이 서버에 무슨 이름으로 저장되어 있는지 알아야 한다(그래야 다른 사용자들한테 보냄)
            String returned_filename = "";
            String result = "";
            try{

                if(responseMsg.equals("")){

                    return "fail";

                }else{


                    JSONObject result_object = new JSONObject(responseMsg);
                    final JSONArray success_array = (JSONArray) result_object.get("success_data");
                    JSONArray fail_array = (JSONArray)result_object.get("fail_data");

                    Log.d("이미지", "Video uploader) 동영상 업로드 후 success_array = "+success_array);
                    Log.d("이미지", "Video uploader) 동영상 업로드 후 fail_array = "+fail_array);


                    //업로드 성공 시
                    if(success_array.length()>0){
                        result = "success";

                        //1. 서버가 저장한 파일의 이름을 받아온다
                        returned_filename = (String)success_array.get(0);
                        Log.d("이미지", "Video uploader) 서버가 저장한 동영상 파일의 이름 = "+returned_filename);


                        String video_url_server = Function.domain+"/images/"+room_id+"/"+returned_filename;

                        //2. db에 저장된 메시지 정보를 업데이트한다. video_path = 압축 동영상 파일의 path
                        db.execSQL("UPDATE chat_logs SET video_path='"+compressed_video_file_path+"', video_path_server='"+video_url_server+"' WHERE id='" + message_db_id + "';");

                        Log.d("이미지", "Video uploader) db를 업데이트 함. video_path 수정");

//
//                        if(ChatActivity.uploading_video_db_id_list.contains(message_db_id)){
//
//                            ChatActivity.uploading_video_db_id_list.remove(new Integer(message_db_id));
//
//                            Log.d("이미지", "Video uploader) ChatActivity 의 uploading_video_db_id_list에서 이 동영상을 제거함");
//                        }else{
//                            Log.d("이미지", "Video uploader) ChatActivity 의 uploading_video_db_id_list에 이 동영상이 없음. 원래 있어야 함");
//                        }


                        //3. 채팅 서버로 메시지를 발송한다
                        //msg_video/room_id/썸네일 파일 이름/동영상 파일 이름

                        //db에서 썸네일 파일 이름을 가져온다
                        Cursor cursor = db.rawQuery
                                ("SELECT image_name FROM chat_logs WHERE id='" + message_db_id +"';", null);

                        cursor.moveToFirst();
                        String thumbnail_filename = cursor.getString(0);

                        String msg = "msg_video/"+room_id+"/"+thumbnail_filename+"/"+returned_filename;

                        Intent intent = new Intent(mContext, MainService.class);
                        intent.putExtra("message", msg);
                        mContext.startService(intent);

                        Log.d("이미지", "Video uploader) 채팅 서버로 메시지 발송함. 자바 서버에 메시지 도착했는지 확인");


                        //4. 채팅방 목록을 업데이트한다

                        //4-1. sqlite 에서 방 정보를 불러온다
                        String roomInfo = dbHelper.get_chatRoomInfo(room_id);
                        String [] roomInfo_array = roomInfo.split("/");
                        String roomName_msg = roomInfo_array[1];
                        String memberInfo = roomInfo_array[3];
                        String[] memberInfo_array = memberInfo.split(";");
                        int number_of_members_msg = memberInfo_array.length/2;


                        //4-2. 목록 맨 위에 아이템을 추가하고, 기존 아이템을 삭제한다
                        Main_Fragment2.roomItemList.add(0, new RoomListItem(roomName_msg, number_of_members_msg,
                                "1 Video", Function.getCurrentTime(), room_id, 0));

                        for(int i=Main_Fragment2.roomItemList.size()-1; i>0; i--){
                            RoomListItem item = Main_Fragment2.roomItemList.get(i);
                            if(item.getRoomId() == room_id){
                                Main_Fragment2.roomItemList.remove(i);
                                Log.d(TAG, i+" item is removed from roomItemList");
                            }
                        }

                        Log.d("이미지", "Video uploader) 채팅방 목록 업데이트 함. 진짜 되었는지 확인");


                    }


                    //업로드 실패 시
                    if(fail_array.length()>0){
                        result = "fail";
                    }


                }


            }catch (Exception e){
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d(TAG,ex);
            }


            return result;

        }


        @Override
        protected void onProgressUpdate(Object... objects) {
            super.onProgressUpdate(objects);

            String operation = (String)objects[0]; //현재 해야 할 작업

            if(operation.equals("upload")){ //서버로 파일을 업로드 중일 때

                int percentage = (int)objects[1];
                circleProgressBar.setProgress(percentage);

            }else if(operation.equals("post_upload")){ //업로드 완료 후의 작업을 수행할 때

                //circleProgressBar를 없애고, progressBar를 보여준다
                //작업중이라는 메시지를 보여준다
                upload_info_textView.setText("Processing..");
                circleProgressBar.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);

            }

        }


        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            Log.d("이미지", "Video uploader) onPostExecute");

            //썸네일 위에 떠 있던 프로그레스바를 숨긴다
            progressBar.setVisibility(View.GONE);

            if(result.equals("success")){ //업로드 성공 시

                //재생버튼과 재생시간을 띄운다
                playBtn_imageView.setVisibility(View.VISIBLE);

                String duration = getDuration(compressed_video_file_path);
                upload_info_textView.setText(duration);

                Log.d("이미지", "Video uploader) 업로드 성공 후 마지막 처리. 썸네일 위에 재생버튼, 재생시간이 보여야됨");

            }else{ //업로드 실패 시

                //실패를 뜻하는 그림을 띄워준다
                playBtn_imageView.setImageResource(0); //이미지뷰를 비움
                playBtn_imageView.setImageResource(R.drawable.ic_warning_black_24dp);
                playBtn_imageView.setVisibility(View.VISIBLE);

                //실패 메시지를 띄워준다
                upload_info_textView.setText("Upload Failed");

                Log.d("이미지", "Video uploader) 업로드 실패 후 마지막 처리. 썸네일 위에 warning 아이콘, upload failed 메시지가 보여야됨");

            }



        }

    }




    //서버에 있는 동영상을 다운받는 클래스
    class VideoDownloader extends AsyncTask<String, String, String> {

        CircleProgressBar circleProgressBar;
        ImageView playBtn_imageView;
        TextView videoInfo_textView;

        int db_id;
        private String fileName;
        private final String MY_FOLDER = "/Canaria"; //내가 원하는 저장 경로(폴더 이름)
        String path_final = "";

        public VideoDownloader(int db_id, CircleProgressBar circleProgressBar, ImageView playBtn_imageView, TextView videoInfo_textView){
            this.db_id = db_id;
            this.circleProgressBar = circleProgressBar;
            this.playBtn_imageView = playBtn_imageView;
            this.videoInfo_textView = videoInfo_textView;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            videoInfo_textView.setVisibility(View.VISIBLE);
            videoInfo_textView.setText("Downloading..");
            circleProgressBar.setVisibility(View.VISIBLE);

            Log.d("이미지", "VideoDownloader) onPreExecute() 프로그레스바, 다운로드 메시지가 보여야 함");
        }

        @Override protected String doInBackground(String... params) {


            //웹 서버 쪽 파일이 있는 경로
            String fileUrl = params[0];
            Log.d("이미지", "VideoDownloader) doInBackground. fileUrl="+fileUrl);


            //다운로드 경로를 지정
            //Environment.getExternalStorageDirector() : 저장공간의 기본경로를 가져옴. 기기마다 저장공간에 대한 경로 명이 상이함
            String savePath = Environment.getExternalStorageDirectory().toString() + MY_FOLDER;
            File dir = new File(savePath);


            //위에서 지정한 디렉토리가 존재하지 않을 경우, 새로 생성한다
            if (!dir.exists()) {
                dir.mkdirs();
                Log.d("이미지", "VideoDownloader) 로컬 다운로드 폴더가 존재하지 않음. 생성함");
                /*
                 * mkdirs(): 원하는 경로의 상위 폴더가 없으면 상위 폴더까지 생성
                 * mkdir(): 지정 폴더만 생성
                 * */
            }


            //파일 이름 : Canaria_날짜_시간
            Date day = new Date();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA);
            fileName = "Canaria_"+String.valueOf(sdf.format(day));


            //다운로드 폴더에 동일한 파일명이 존재하는지 확인
            String pathname = savePath + "/" + fileName;
            int fix_index = 0;
            int flag = 0;
            while(flag == 0){

                fix_index++;

                if (new File(pathname).exists()) { //중복파일이 있으면

                    pathname = pathname+"("+fix_index+")";
                    flag = 0;

                    Log.d("이미지", "VideoDownloader) 다운로드 폴더에 중복파일이 있음");

                } else { //중복파일이 없으면
                    flag = 1;

                    Log.d("이미지", "VideoDownloader) 다운로드 폴더에 중복파일이 없음");
                }

            }


            path_final = pathname + ".mp4";
            Log.d("이미지", "VideoDownloader) 최종 파일 이름 = "+path_final);


            try {

                URL imgUrl = new URL(fileUrl);

                //서버와 접속하는 클라이언트 객체 생성
                HttpURLConnection conn = (HttpURLConnection)imgUrl.openConnection();
                int len = conn.getContentLength();
                byte[] tmpByte = new byte[len];

                //입력 스트림을 구한다
                InputStream is = conn.getInputStream();
                File file = new File(path_final);

                //파일 저장 스트림 생성
                FileOutputStream fos = new FileOutputStream(file);
                int read;

                int response = conn.getResponseCode();
                if(response == HttpURLConnection.HTTP_OK){

                }else{ //전송 실패 시, 쓰레드 종료
                    return "failed";
                }

                long total = 0;
                //입력 스트림을 파일로 저장
                for (;;) {
                    read = is.read(tmpByte);
                    if (read <= 0) {
                        break;
                    }
                    total += read;
                    publishProgress(""+(int)((total*100)/len));

                    fos.write(tmpByte, 0, read); //file 생성
                }
                is.close();
                fos.close();
                conn.disconnect();

            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String ex = sw.toString();

                Log.d("이미지",ex);
                return "failed";
            }

            return "succeeded";
        }


        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);

            circleProgressBar.setProgress(Integer.parseInt(values[0]));

            Log.d("이미지", "VideoDownloader) 다운로드 progress를 표시함. 현재: "+Integer.parseInt(values[0])+" %");
        }

        @Override protected void onPostExecute(String s) {
            super.onPostExecute(s);
            Log.d("이미지", "VideoDownloader) onPostExecute");

            if(s.equals("succeeded")){ //다운로드 성공 시

                Log.d("이미지", "VideoDownloader) 다운로드 성공");

                //db에 저장된 메시지 정보를 업데이트한다 - video_path 컬럼에 경로를 저장한다
                db.execSQL("UPDATE chat_logs SET video_path='"+path_final+"' WHERE id='" + db_id + "';");

                Log.d("이미지", "VideoDownloader) db를 업데이트 함. video_path 수정");


                //프로그레스 바를 없앤다
                circleProgressBar.setVisibility(View.GONE);

                //재생버튼과 재생 시간을 보여준다
                playBtn_imageView.setVisibility(View.VISIBLE);

                String duration = getDuration(path_final);
                videoInfo_textView.setText(duration);

                Log.d("이미지", "VideoDownloader) 다운로드 성공 후 마지막 처리. 썸네일 위에 재생버튼, 재생시간이 보여야됨");

                //이미지 스캔해서 갤러리 업데이트 -- 이걸 안 하면 어떻게 됨?
//            context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(path_final))));


            }else{ //다운로드 실패 시

                Log.d("이미지", "VideoDownloader) 다운로드 실패");

                //프로그레스바를 없앤다
                circleProgressBar.setVisibility(View.GONE);

                //실패를 뜻하는 그림을 띄워준다
                playBtn_imageView.setImageResource(0); //이미지뷰를 비움
                playBtn_imageView.setImageResource(R.drawable.ic_warning_black_24dp);
                playBtn_imageView.setVisibility(View.VISIBLE);

                //실패 메시지를 띄워준다
                videoInfo_textView.setText("Download Failed");

                Log.d("이미지", "VideoDownloader) 다운로드 실패 후 마지막 처리. 썸네일 위에 warning 아이콘, upload failed 메시지가 보여야됨");
            }


        }

    }

}

