package com.android.canaria;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.android.canaria.db.DBHelper;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SlideAdapter extends PagerAdapter {

//    private ArrayList<String> url_list;
    private ArrayList<SlideImageItem> itemArrayList;
    private LayoutInflater inflater;
    private Context context;

    private DBHelper dbHelper;
    private SQLiteDatabase db;


    public SlideAdapter(Context context, ArrayList<SlideImageItem> itemArrayList){
        this.context = context;
        this.itemArrayList = itemArrayList;

        dbHelper = new DBHelper(context, Function.dbName, null, Function.dbVersion);
        db = dbHelper.getWritableDatabase();
    }


    @Override
    public int getCount() {
        return itemArrayList.size();
    }

    @Override
    public boolean isViewFromObject(@NotNull View view, @NotNull Object object) {
        return view == ((RelativeLayout) object);
    }

    @NotNull
    @Override
    public Object instantiateItem(@NotNull ViewGroup container, final int position) {
        inflater = (LayoutInflater)context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.slider, container, false);

        PhotoView photoView = (PhotoView) v.findViewById(R.id.slider_photoView);
//        TextView textView = (TextView)v.findViewById(R.id.slider_textView);
        ImageView btn_imageView = (ImageView)v.findViewById(R.id.slider_btn_imageView);


        Glide.with(context).asBitmap().load(itemArrayList.get(position).getUrl()).listener(new RequestListener<Bitmap>() {
            @Override
            public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                return false;
            }

            @Override
            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
                return false;
            }
        }).into(photoView);


        if(itemArrayList.get(position).getType().equals("video")){ //이 이미지 = 비디오 파일의 썸네일

            //플레이 버튼 이미지를 화면에 보여준다
            btn_imageView.setVisibility(View.VISIBLE);

            photoView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String video_path = itemArrayList.get(position).getVideo_path();

                    //이미지뷰를 클릭하면, 동영상을 띄워줄 수 있는 앱을 연다
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    Uri uri = Uri.parse(video_path);
                    intent.setDataAndType(uri, "video/*");
                    if(intent.resolveActivity(context.getPackageManager()) != null) {
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(intent);
                    }

                }
            });

        }else{

            //플레이 버튼 이미지를 화면에서 없앤다
            btn_imageView.setVisibility(View.INVISIBLE);
        }


//        String text = (position + 1) + " / " + url_list.size();
//        textView.setText(text);
        container.addView(v);
        return v;
    }


    @Override
    public void destroyItem(@NotNull ViewGroup container, int position, @NotNull Object object) {
        container.invalidate();
    }




}
