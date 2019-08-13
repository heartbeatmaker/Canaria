package com.android.canaria;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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
import com.github.chrisbanes.photoview.PhotoView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class SlideAdapter extends PagerAdapter {

    private ArrayList<String> url_list;
    private LayoutInflater inflater;
    private Context context;

    private DBHelper dbHelper;
    private SQLiteDatabase db;


    public SlideAdapter(Context context, ArrayList<String> url_list){
        this.context = context;
        this.url_list = url_list;

        dbHelper = new DBHelper(context, Function.dbName, null, Function.dbVersion);
        db = dbHelper.getWritableDatabase();
    }


    @Override
    public int getCount() {
        return url_list.size();
    }

    @Override
    public boolean isViewFromObject(@NotNull View view, @NotNull Object object) {
        return view == ((RelativeLayout) object);
    }

    @NotNull
    @Override
    public Object instantiateItem(@NotNull ViewGroup container, int position) {
        inflater = (LayoutInflater)context.getSystemService
                (Context.LAYOUT_INFLATER_SERVICE);
        View v = inflater.inflate(R.layout.slider, container, false);

        PhotoView photoView = (PhotoView) v.findViewById(R.id.slider_photoView);
        TextView textView = (TextView)v.findViewById(R.id.slider_textView);

        Glide.with(context).asBitmap().load(url_list.get(position)).into(photoView);

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
