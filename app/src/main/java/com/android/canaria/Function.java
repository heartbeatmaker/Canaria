package com.android.canaria;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import java.util.List;

import cz.msebera.android.httpclient.client.CookieStore;
import cz.msebera.android.httpclient.cookie.Cookie;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.impl.cookie.BasicClientCookie;

public class Function {

    public static String domain = "http://54.180.107.44";


    public static String getString(Context context, String key){
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        return pref.getString(key, "null");
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
