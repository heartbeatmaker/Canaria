package com.android.canaria;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.drawable.RoundedBitmapDrawable;
import android.support.v4.graphics.drawable.RoundedBitmapDrawableFactory;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.android.canaria.connect_to_server.HttpRequest;
import com.android.canaria.db.DBHelper;
import com.android.canaria.recyclerView.FriendListAdapter;
import com.android.canaria.recyclerView.FriendListItem;
import com.android.canaria.view.CollageView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.BitmapImageViewTarget;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.target.ViewTarget;
import com.bumptech.glide.request.transition.Transition;
import com.stfalcon.multiimageview.MultiImageView;


import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class Function {

    public static String domain = "http://15.164.193.65";
    public static String dbName = "canaria.db";
    public static int dbVersion = 1;
    public static int activeRoomId = 0;




    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/my_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }






    /**
     * Gets timestamp in millis and converts it to HH:mm (e.g. 16:44).
     */
    public static String formatTime(long timeInMillis){

        SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
        return dateFormat.format(timeInMillis);
    }




    public static void displayCollageImages(final Context context, final int roomId, final String fileName_string, final CollageView collageView){

        final String[] fileName_split = fileName_string.split(";");

        List<String> urls_list = new ArrayList<String>();

        //채팅 화면에 띄워주는 이미지 = 썸네일. 썸네일 폴더에서 가져온다
        for (int i=0; i<fileName_split.length; i++){

            String url = domain+"/images/"+roomId+"_thumb/"+fileName_split[i];
            urls_list.add(url);
        }

        Log.d("이미지", "displayCollageImages) fileName_split="+ Arrays.asList(fileName_split));
        Log.d("이미지", "displayCollageImages) urls_list="+ urls_list);

        collageView
                .photoMargin(0)
                .photoPadding(0)
//                .backgroundColor(Color.GRAY)
//                .photoFrameColor(Color.GRAY)
                .useFirstAsHeader(false) // makes first photo fit device widtdh and use full line
                .defaultPhotosForLine(3) // sets default photos number for line of photos (can be changed by program at runtime)
//                .iconSelector(context, getResources().getDimensionPixelSize(R.dimen.icon_size)) (or use 0 as size to wrap content)
//                .useCards(true) // adds cardview backgrounds to all photos
//                .maxWidth(60) // will resize images if their side is bigger than number
//                .placeHolder(R.drawable.bird_icon) //adds placeholder resource
//                .headerForm(CollageView.ImageForm.IMAGE_FORM_SQUARE) // sets form of image for header (if useFirstAsHeader == true)
//                .photosForm(CollageView.ImageForm.IMAGE_FORM_HALF_HEIGHT) //sets form of image for other photos
                .loadPhotos(urls_list); // here you can use Array/List of photo urls or array of resource ids

        collageView.setOnPhotoClickListener(new CollageView.OnPhotoClickListener() {
            @Override
            public void onPhotoClick(int position) {
//                Toast.makeText(context, "position="+position, Toast.LENGTH_SHORT).show();

                //해당 이미지의 url 을 전달한다
                Intent intent = new Intent(context, ImageActivity.class);
                String url = domain+"/images/"+roomId+"/"+fileName_split[position];

                //roomId, filename_string, position, 이 사진의 url
                intent.putExtra("room_id", roomId);
                intent.putExtra("filename_string", fileName_string);
                intent.putExtra("position", position);
                intent.putExtra("url", url);

                context.startActivity(intent);
            }
        });
    }



    public static void displayRoomProfileImage(Context context, int roomId, final CollageView collageView){

        //db에서 이 방 참여자 목록을 가져온다 - id만 string 형태로 잇는다 (4명만 있으면 된다)
        //php 서버로 보낸다(이를 처리하는 서버 코드를 작성한다) -> 각 참여자의 프로필 사진 url을 응답받는다
        //collageView에 glide로 프로필 사진을 넣는다



        //1. db에서 이 방 참여자 목록을 가져온다 - id만 string 형태로 잇는다 (4명만 있으면 된다)
        DBHelper dbHelper = new DBHelper(context, Function.dbName, null, Function.dbVersion);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT members FROM chat_rooms WHERE room_id='" + roomId + "';", null);
        cursor.moveToFirst();
        String memberInfo = cursor.getString(0);

        int count = 0;
        String memberInfo_string = "";
        String[] memberInfo_split = memberInfo.split(";");
        for(int i=0; i<memberInfo_split.length; i++){

            if(i%2 == 0){ //i가 짝수일 때: id
                int member_id = Integer.valueOf(memberInfo_split[i]);

                //자신의 id는 포함하지 않는다
                if(member_id != Integer.valueOf(getString(context, "user_id"))){
                    memberInfo_string += member_id +";";
                    count += 1;
                }

            }

            if(count == 4){ //4명의 id만 필요하다. 방목록에는 최대 4인의 프로필 사진이 들어간다
                break;
            }
        }

        //마지막 ';' 제거
        memberInfo_string = memberInfo_string.substring(0, memberInfo_string.length()-1);



        //2. php 서버로 보낸다 -> 각 참여자의 프로필 사진 url을 응답받는다
        ContentValues data = new ContentValues();
        data.put("user_id_group", memberInfo_string);

        try {
            //String response 를 jsonArray 로 파싱한다
            final String response = new HttpRequest("image.php", data).execute().get();
            JSONArray jsonArray = new JSONArray(response);


            List<String> urls_list = new ArrayList<String>();
            //3. collageView에 glide로 프로필 사진을 넣는다
            for(int k=0; k<jsonArray.length(); k++){

                String url = domain+"/uploads/"+jsonArray.getString(k);
                urls_list.add(url);

                Log.d("프로필", "url="+url);

                RequestOptions options = new RequestOptions().placeholder(R.drawable.user);

            }


            collageView
//                    .useCards(true)
                    .photoMargin(0)
                    .photoPadding(3)
                    .placeHolder(R.drawable.bird)
                    .useFirstAsHeader(false) // makes first photo fit device widtdh and use full line
                    .defaultPhotosForLine(2) // sets default photos number for line of photos (can be changed by program at runtime)
                    .loadPhotos(urls_list); // here you can use Array/List of photo urls or array of resource ids

        } catch (Exception e) {
            Log.d("tag", "Error: "+e);
        }


    }






    /**
     * Crops image into a circle that fits within the ImageView.
     */
    public static void displayRoundImageFromUrl(final Context context, final String fileName, final ImageView imageView) {

//        if(!fileName.equals("null")){ //서버에서 프로필 사진을 찾을 때, 파일이 없으면 "null"이라고 반환하도록 설정해놓음

            String url = "http://15.164.193.65/uploads/"+fileName;

            RequestOptions options = new RequestOptions()
                    .placeholder(R.drawable.user)
                    .centerCrop()
                    .dontAnimate();

            Glide.with(context)
                    .asBitmap()
                    .load(url)
                    .apply(options)
                    .into(new BitmapImageViewTarget(imageView) {
                        @Override
                        protected void setResource(Bitmap resource) {
                            RoundedBitmapDrawable circularBitmapDrawable =
                                    RoundedBitmapDrawableFactory.create(context.getResources(), resource);
                            circularBitmapDrawable.setCircular(true);
                            imageView.setImageDrawable(circularBitmapDrawable);
                        }
                    });
        }

//    }



    public static void displayResizedImage(final Context context, String url, ImageView imageView,
                                           final int position, final boolean isVideoThumbnail, final boolean isSender, final String filename, final int roomId){

        RequestOptions options = new RequestOptions()
                .fitCenter();
//                .override(width, height);

        final ProgressBar progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleLarge);
        progressBar.setIndeterminate(true);
        progressBar.getIndeterminateDrawable().setColorFilter(Color.BLUE, PorterDuff.Mode.MULTIPLY); //색 변경

        Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(options)
                .listener(new RequestListener<Bitmap>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
                        progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {

                        progressBar.setVisibility(View.GONE);

                        Log.d("이미지", "onResourceReady");
                        Log.d("이미지", "isSender="+isSender+" && isVideoThumbnail="+isVideoThumbnail);


                        /*비디오 파일 처리를 위한 코드*/
                        //사진 전송자인지, 이 파일이 썸네일인지 검사한다
//                        if(isVideoThumbnail){
//
//                            //이거아님~~~ 이렇게 추출하면 안됨 _ 가 다 사라짐
//                            String[] name_split = filename.split("\\.");
//                            //확장자 없는 이름을 추출
//                            String file_name_without_extension = "";
//                            for(int m=0; m<name_split.length-1; m++){
//                                file_name_without_extension += name_split[m];
//                            }
//                            String[] origin_name_split = file_name_without_extension.split("_");
//                            String origin_name_without_extension = "";
//                            for(int m=2; m<origin_name_split.length; m++){
//                                origin_name_without_extension += origin_name_split[m];
//                            }
//
//                            String video_filename = origin_name_without_extension+".mp4";
//                            Log.d("이미지", "원본 mp4파일 이름="+video_filename);
//
//
//                            if(isSender){
//
//
//                                //이미 동영상이 업로드 되었는지 검사한다
//                                SharedPreferences pref = context.getSharedPreferences("video", Context.MODE_PRIVATE);
//                                SharedPreferences.Editor editor = pref.edit();
//
//                                if(pref.contains(String.valueOf(roomId))){
//
//                                    try{
//                                        String s = pref.getString(String.valueOf(roomId), "no data");
//                                        JSONArray room_array = new JSONArray(s);
//                                        for(int i=0; i<room_array.length(); i++){
//
//                                            if(room_array.getString(i).equals(video_filename)){ //이미 동영상이 존재하면
//
//                                                //동영상 재생 view를 띄워주라고 알림 발송
//
//                                            }else{//존재하지 않으면
//
//                                                //새로 저장한다
//                                                room_array.put(video_filename);
//                                                editor.putString(String.valueOf(roomId), room_array.toString());
//                                                editor.commit();
//
//                                                //동영상을 업로드하라는 신호를 보낸다!!!!!
//                                                Intent intent = new Intent("video_upload");
//                                                intent.putExtra("message", "upload");
//                                                intent.putExtra("position", position);
//                                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//                                            }
//                                        }
//
//                                    }catch (Exception e){
//                                        StringWriter sw = new StringWriter();
//                                        e.printStackTrace(new PrintWriter(sw));
//                                        String ex = sw.toString();
//
//                                        Log.d("이미지",ex);
//
//                                    }
//
//                                }else{ //이 방에 해당하는 값이 없을 경우
//
//                                    //jsonArray를 만들어서 이 방 이름으로 저장한다
//                                    JSONArray room_array = new JSONArray();
//                                    room_array.put(video_filename);
//                                    editor.putString(String.valueOf(roomId), room_array.toString());
//                                    editor.commit();
//
//                                    //동영상을 업로드하라는 신호를 보낸다!!!
//
//                                }
//
//
//                            }else{
//
//                            }


//
//                            //압축 파일을 저장하는 디렉토리에 해당 동영상이 존재하는지 검사한다
//                            String destination_directory = Environment.getExternalStorageDirectory().toString() + "/Canaria/videos/"+video_filename;
//                            File video_file = new File(destination_directory);
//
//
//                            //해당 파일이 존재하지 않는 경우 -> 동영상을 다운로드 할 수 있는 뷰를 만들라고 신호를 보낸다
//                            if (!video_file.exists()) {
//                                Log.d("이미지", "압축된 동영상이 디렉토리에 존재하지 않음");
//
//                                Intent intent = new Intent("video_upload");
//                                intent.putExtra("position", position);
//                                LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//
//                            }

//                        }

                        return false;
                    }
                })
                .into(imageView);

    }


    public static String getUserImage_url(int userId) {

        String userImage_url = "null";
        ContentValues data = new ContentValues();
        data.put("sender_id", userId);

        try {
            //sender_id를 이용하여 서버에서 해당 사용자의 프로필 사진 이름을 가져온다
            userImage_url = new HttpRequest("image.php", data).execute().get();
        } catch (Exception e) {
            Log.d("tag", "Error: "+e);
        }


        return userImage_url;
    }


    //현재 날짜, 시간을 구하기
    public static String getCurrentTime(){

        //초 단위로 표시해야함. 그래야 db에서 데이터 가져올 때 가장 최신 데이터를 가져올 수 있다
        //분 단위로 표시했을 때 문제: 8시 31분에 해당하는 4개의 데이터가 있다면, 거기서 가장 오래된 데이터를 가져옴(최신x)
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd aaa hh:mm:ss");
        String time = format.format(System.currentTimeMillis());
        return time;
    }


    public static void setString(Context context, String key, String value){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString(key, value);
        editor.commit();
    }


    public static String getString(Context context, String key){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(key, "null");
    }


    public static boolean getBoolean(Context context, String key){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getBoolean(key, true);
    }


    public static void setBoolean(Context context, String key, boolean value){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = pref.edit();
        editor.putBoolean(key, value);
        editor.commit();
    }


    public static void getAllPrefData(Context context){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> allEntries = pref.getAll();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("noti", "Shared Pref 에 있는 데이터 전부출력: "+entry.getKey() + ": " + entry.getValue().toString());
        }
    }


    public static boolean isForegroundActivity(Context context, Class<?> cls) {
        if(cls == null)
            return false;

        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> info = activityManager.getRunningTasks(1);
        ActivityManager.RunningTaskInfo running = info.get(0);
        ComponentName componentName = running.topActivity;

        return cls.getName().equals(componentName.getClassName());
    }



    public static String sendGetRequest(Activity activity, String _url, ContentValues _params) {

        String TAG = "tag "+activity.getClass().getSimpleName();

        Log.d(TAG, "sendGetRequest method");

        HttpURLConnection urlConn = null;

        // URL 뒤에 붙여서 보낼 파라미터.
        StringBuffer sbParams = new StringBuffer();

        /**
         * 1. StringBuffer에 파라미터 연결
         * */
        // 보낼 데이터가 없으면 파라미터를 비운다.
        if (_params == null)
            sbParams.append("");
            // 보낼 데이터가 있으면 파라미터를 채운다.
        else {
            // 파라미터가 2개 이상이면 파라미터 연결에 &가 필요하므로 스위칭할 변수 생성.
            boolean isAnd = false;
            // 파라미터 키와 값.
            String key;
            String value;

            for (Map.Entry<String, ?> element : _params.valueSet()) {
                key = element.getKey();
                value = element.getValue().toString();

                // 파라미터가 두개 이상일때, 파라미터 사이에 &를 붙인다.
                if (isAnd)
                    sbParams.append("&");

                sbParams.append(key).append("=").append(value);

                // 파라미터가 2개 이상이면 isAnd를 true로 바꾸고 다음 루프부터 &를 붙인다.
                if (!isAnd){
                    if (_params.size() >= 2)
                        isAnd = true;
                }

            }

            Log.d(TAG, "parameter:"+sbParams);
        }

        /**
         * 2. HttpURLConnection을 통해 web의 데이터를 가져온다.
         * */
        try {
            URL url = new URL(_url);
            urlConn = (HttpURLConnection) url.openConnection();

            // [2-1]. urlConn 설정.
            urlConn.setReadTimeout(10000);
            urlConn.setConnectTimeout(15000);
            urlConn.setRequestMethod("GET"); // URL 요청에 대한 메소드 설정 : GET/POST.
            urlConn.setDoOutput(true);
            urlConn.setDoInput(true);
            urlConn.setRequestProperty("Accept-Charset", "utf-8"); // Accept-Charset 설정.
//            urlConn.setRequestProperty("Context_Type", "application/x-www-form-urlencoded");

            // [2-2]. parameter 전달
            PrintWriter pw = new PrintWriter(new OutputStreamWriter(urlConn.getOutputStream()));
            pw.write(sbParams.toString());
            pw.flush(); // 출력 스트림을 flush. 버퍼링 된 모든 출력 바이트를 강제 실행.
            pw.close(); // 출력 스트림을 닫고 모든 시스템 자원을 해제.

            // [2-3]. 연결 요청 확인.
            Log.d(TAG, "responseCode = "+urlConn.getResponseCode());
            // 실패 시 null을 리턴하고 메서드를 종료.
            if (urlConn.getResponseCode() != HttpURLConnection.HTTP_OK)
                return null;

            // [2-4]. 읽어온 결과물 리턴.
            // 요청한 URL의 출력물을 BufferedReader로 받는다.
            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConn.getInputStream(), "UTF-8"));

            // 출력물의 라인과 그 합에 대한 변수.
            String line;
            String page = "";

            // 라인을 받아와 합친다.
            while ((line = reader.readLine()) != null) {
                page += line;
            }

            Log.d(TAG, "result = "+page);
            return page;

        } catch (MalformedURLException e) { // for URL.
            Log.d(TAG, "error: = "+e);
            e.printStackTrace();
        } catch (IOException e) { // for openConnection().
            Log.d(TAG, "error: = "+e);
            e.printStackTrace();
        } finally {
            if (urlConn != null)
                urlConn.disconnect();
        }
        return null;
    }






    //쿠키 생성
//            CookieStore cookieStore = new BasicCookieStore();
//            BasicClientCookie cookie = new BasicClientCookie("session_id", "value");
//
//
//            //지금으로부터 7일 후의 날짜를 구한다
//            Calendar cal = Calendar.getInstance();
//            cal.setTime(new Date());
//            cal.add(Calendar.DATE, 7);
//            Date exDate = cal.getTime();
//
//
//            cookie.setDomain(Function.domain);
//            cookie.setPath("/");
//            cookie.setExpiryDate(exDate);
//            cookieStore.addCookie(cookie);
//            ((DefaultHttpClient) client).setCookieStore(cookieStore);
    //








    //쿠키에 저장된 session id를 확인
//    List<Cookie> cookies = ((DefaultHttpClient)client).getCookieStore().getCookies();
//
//                if (!cookies.isEmpty()) {
//        for (int i = 0; i < cookies.size(); i++) {
//            // cookie = cookies.get(i);
//            String cookieString = cookies.get(i).getName() + " = "
//                    + cookies.get(i).getValue();
//            Log.d("tag", "cookie "+i+": "+cookieString);
//
//
//            if(cookies.get(i).getName().equals("PHPSESSID")){
//
//                String key = cookies.get(i).getName();
//                String value = cookies.get(i).getValue();
//                String domain = cookies.get(i).getDomain();
//                String path = cookies.get(i).getPath();
////                            String expiry_date = cookies.get(i).getExpiryDate().toString();
//
//                Log.d("tag", "key="+key+"/value="+value+"/domain="+domain+"/path="+path);
//
//                SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//                SharedPreferences.Editor editor = pref.edit();
//                editor.putString("key", key);
//                editor.putString("value", value);
//                editor.putString("domain", domain);
//                editor.putString("path", path);
////                            editor.putString("expiry_date", expiry_date);
//                editor.commit();
//
//            }
//        }
//    }else{
//        Log.d("tag", "cookie is empty");
//    }
    //






//    public String getSessionId(){
//
//        String session_id = "null";
//
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
//
//        if(prefs.getString("JSESSIONID", "") != null){
//            session_id = prefs.getString("JSESSIONID", "null");
//        }
//
//        return session_id;
//    }


//    public void updateCookie(){
//
//        CookieStore cookieStore = ((DefaultHttpClient)httpClient).getCookieStore();
//        List<Cookie> cookieList = cookieStore.getCookies();
//
//        String key = "";
//        String value = "";
//
//        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
//
//        if(!prefs.getString("key", "").equals("")){
//            key = prefs.getString("key", "null");
//            value = prefs.getString("value", "");
//            Log.d("tag"+getClass().getName(), "saved session_id:"+key+"="+value);
//
//        }else{
//            Log.d("tag"+getClass().getName(), "preference is null");
//        }
//
//
//        if(cookieList.size() == 0 && key != null){
//
//            BasicClientCookie cookie = new BasicClientCookie(key, value);
//            cookie.setDomain(Function.domain);
//            cookie.setPath("/");
//            cookieStore.addCookie(cookie);
//
//        }
//
//
//    }


}
