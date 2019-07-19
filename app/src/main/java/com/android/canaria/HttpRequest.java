package com.android.canaria;

import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Map;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.ClientProtocolException;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.util.EntityUtils;

public class HttpRequest extends AsyncTask<Void, Void, String> {

    Context context;
    String path;
    ContentValues _params;
    String TAG = "tag "+this.getClass().getSimpleName();
//    ProgressDialog dialog = new ProgressDialog(context);


    public HttpRequest(Context context, String path, ContentValues _params){
        this.context = context;
        this.path = path;
        this._params = _params;
    }

//    @Override
//    protected void onPreExecute() {
//        super.onPreExecute();
//        Log.d(TAG,"onPreExecute");
//
//        dialog.setMessage("Processing..");
//        dialog.show();
//    }

    @Override
    protected String doInBackground(Void... voids) {

        String response_line = "";

        Log.d(TAG,"doInBackground");

        HttpClient client = new DefaultHttpClient();
        HttpPost post = new HttpPost("http://54.180.107.44/"+path);

        //POST 방식에서 사용된다
        ArrayList<NameValuePair> nameValues = new ArrayList<NameValuePair>();

        try {
            //Post방식으로 넘길 값들을 각각 지정을 해주어야 한다.

            String key;
            String value;
            for (Map.Entry<String, ?> element : _params.valueSet()) {
                key = element.getKey();
                value = element.getValue().toString();

                nameValues.add(new BasicNameValuePair(
                        key, URLDecoder.decode(value, "UTF-8")));
            }

            //HttpPost에 넘길 값을들 Set해주기
            post.setEntity(new UrlEncodedFormEntity(nameValues, "UTF-8"));

        } catch (UnsupportedEncodingException ex) {
            Log.d(TAG, "error: "+ex.toString());
        }

        try {
            //설정한 URL을 실행시키기 -> 응답을 받음
            HttpResponse response = client.execute(post);
            //통신 값을 받은 Log 생성. (200이 나오는지 확인할 것~) 200이 나오면 통신이 잘 되었다는 뜻!
            Log.i(TAG, "response.getStatusCode:" + response.getStatusLine().getStatusCode());

            HttpEntity entity = response.getEntity();

            if(entity !=null){
                Log.d(TAG, "Response length:"+entity.getContentLength());

                // 콘텐츠를 읽어들임.
                BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));


                while ((response_line = reader.readLine()) != null) {
                    // 콘텐츠 내용
                    Log.d(TAG, "response: "+response_line);
                    return response_line;
                }
            }

            //Ensures that the entity content is fully consumed and the content stream, if exists, is closed.
            EntityUtils.consume(entity);

            post.releaseConnection();


        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


//    @Override
//    protected void onPostExecute(String s) {
//        super.onPostExecute(s);
//
//        dialog.dismiss();
//        Log.d(TAG,"onPostExecute");
//    }

}
