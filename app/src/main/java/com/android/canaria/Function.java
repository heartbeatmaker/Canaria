package com.android.canaria;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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

    public static String domain = "http://54.180.107.44";
    public static String dbName = "canaria.db";
    public static int dbVersion = 1;
    public static int activeRoomId = 0;


    //현재 날짜, 시간을 구하기
    public static String getCurrentTime(){

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd aaa hh:mm");
        String time = format.format(System.currentTimeMillis());
        return time;
    }



    public static String getString(Context context, String key){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(key, "null");
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
