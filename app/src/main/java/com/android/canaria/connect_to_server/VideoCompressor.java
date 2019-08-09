package com.android.canaria.connect_to_server;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.iceteck.silicompressorr.SiliCompressor;

import java.io.File;
import java.net.URISyntaxException;


public class VideoCompressor extends AsyncTask<String, String, String> {

    Context mContext;

    public VideoCompressor(Context context) {
        mContext = context;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
//            imageView.setImageDrawable(ContextCompat.getDrawable(mContext, R.drawable.ic_photo_camera_white_48px));
//            compressionMsg.setVisibility(View.VISIBLE);
//            picDescription.setVisibility(View.GONE);
    }

    @Override
    protected String doInBackground(String... paths) {
        Log.i("이미지", "doInBackground");
        String filePath = null;
        try {

            //비트레이트: 3000k
            //내 폰으로 찍은 영상 기준, 약 27퍼센트 수준으로 용량을 낮춤
            filePath = SiliCompressor.with(mContext).compressVideo(paths[0], paths[1], 720, 480, 3000000);

        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return filePath;

    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);


    }

    @Override
    protected void onPostExecute(String compressedFilePath) {
        super.onPostExecute(compressedFilePath);
        File imageFile = new File(compressedFilePath);
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
        Log.i("이미지", "onPostExecute");
        Log.i("이미지", "파일 용량="+value);
        Log.i("이미지", "compressedFilePath: " + compressedFilePath);
        mContext.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(new File(compressedFilePath))));
    }
}
