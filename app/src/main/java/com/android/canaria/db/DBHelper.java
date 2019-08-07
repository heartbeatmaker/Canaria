package com.android.canaria.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DBHelper extends SQLiteOpenHelper {

    String TAG = "tag "+this.getClass().getSimpleName();
    public SQLiteDatabase db;

    // DBHelper 생성자로 관리할 DB 이름과 버전 정보를 받음
    public DBHelper(Context context, String db_name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, db_name, factory, version);


    }

    public void open(){
        db = getReadableDatabase();
    }

    // DB를 새로 생성할 때 호출되는 함수
    // getWritableDatabase()에서 호출된다
    @Override
    public void onCreate(SQLiteDatabase db) {
        // 새로운 테이블 생성

        //방정보 테이블: id, 방이름, 업데이트 시각, 참여자 -- room_id는 auto increment가 아니다. 서버에서 준 id를 직접 저장한다
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_rooms (room_id INTEGER PRIMARY KEY, room_name TEXT, updateTime TEXT, members TEXT);");

        //채팅내용 테이블: id, 방id, 보낸사람 id, 보낸사람 username, 메시지내용, 이미지파일 이름, 보낸시각, 읽었는지
        db.execSQL("CREATE TABLE IF NOT EXISTS chat_logs (id INTEGER PRIMARY KEY AUTOINCREMENT, room_id INTEGER, sender_id INTEGER, sender_username TEXT, message TEXT, image_name TEXT, time TEXT, isRead INTEGER);");
    }



    // DB 업그레이드를 위해 버전이 변경될 때 호출되는 함수
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    //방정보 테이블: id, 방이름, 업데이트 시각, 참여자
    public void insert_chatRooms(int room_id, String room_name, String members, String updateTime) {
        // 읽고 쓰기가 가능하게 DB 열기
        long last_inserted_id;

        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        ContentValues contentValues = new ContentValues();
        contentValues.put("room_id", room_id);
        contentValues.put("room_name", room_name);
        contentValues.put("members", members);
        contentValues.put("updateTime", updateTime);

        last_inserted_id = db.insert("chat_rooms", null, contentValues);

//        db.execSQL("INSERT INTO "+tableName+" VALUES(null, '" + room_name + "', " + members + ", '" + updateTime + "');");
//        db.close();

        Log.d(TAG, "chat_rooms ITEM IS INSERTED. ID="+(int)last_inserted_id);
    }


    //채팅내용 테이블: id, 방id, 보낸사람 id, 보낸사람 username, 메시지내용, 보낸시각
    public void insert_chatLogs(int room_id, int sender_id, String sender_username, String message, String image_name, long time, int isRead) {
        // 읽고 쓰기가 가능하게 DB 열기
        long last_inserted_id;

        SQLiteDatabase db = getWritableDatabase();
        // DB에 입력한 값으로 행 추가
        ContentValues contentValues = new ContentValues();
        contentValues.put("room_id", room_id);
        contentValues.put("sender_id", sender_id);
        contentValues.put("sender_username", sender_username);
        contentValues.put("message", message);
        contentValues.put("image_name", image_name);
        contentValues.put("time", String.valueOf(time));
        contentValues.put("isRead", isRead);

        last_inserted_id = db.insert("chat_logs", null, contentValues);

        Log.d(TAG, "chat_logs ITEM IS INSERTED. ID="+(int)last_inserted_id);
    }


    public void room_updateTime(int roomId, String updateTime) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행의 가격 정보 수정
        db.execSQL("UPDATE chat_rooms SET updateTime='" + updateTime + "' WHERE room_id='" + roomId + "';");
//        db.close();
    }


    public void room_updateName(int roomId, String room_name) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행의 가격 정보 수정
        db.execSQL("UPDATE chat_rooms SET room_name='" + room_name + "' WHERE room_id='" + roomId + "';");
//        db.close();
    }




    public void delete(String tableName, int room_id) {
        SQLiteDatabase db = getWritableDatabase();
        // 입력한 항목과 일치하는 행 삭제
        db.execSQL("DELETE FROM "+tableName+" WHERE id='" + room_id + "';");
        db.close();
    }


    public String getResult_table_chatRooms() {
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        String result = "";

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT * FROM chat_rooms", null);
        while (cursor.moveToNext()) {
            result += cursor.getInt(0) //id
                    + " / "
                    + cursor.getString(1) //room name
                    + " / "
                    + cursor.getString(2) //updateTime
                    + " / "
                    + cursor.getString(3) //members
                    + "\n";
        }//id는 어디서 볼 수 있음?

        return result;
    }


    //채팅내용 테이블: id, 방id, 보낸사람 id, 보낸사람 username, 메시지내용, 보낸시각
    public String getResult_table_chatLogs() {
        // 읽기가 가능하게 DB 열기
        SQLiteDatabase db = getWritableDatabase();
        String result = "";

        // DB에 있는 데이터를 쉽게 처리하기 위해 Cursor를 사용하여 테이블에 있는 모든 데이터 출력
        Cursor cursor = db.rawQuery("SELECT * FROM chat_logs", null);
        while (cursor.moveToNext()) {
            result += cursor.getInt(0) //id
                    + " / "
                    + cursor.getInt(1) //room id
                    + " / "
                    + cursor.getInt(2) //sender id
                    + " / "
                    + cursor.getString(3) //sender username
                    + " / "
                    + cursor.getString(4) //message
                    + " / "
                    + cursor.getString(5) //time
                    + " / "
                    + cursor.getString(6) //isRead
                    + "\n";
        }

        return result;
    }



    public String get_chatRoomInfo(int roomId){
        SQLiteDatabase db = getReadableDatabase();
        String result = "";

        Log.d("초대", "get_chatRoomInfo. roomId = "+roomId);

        Cursor cursor = db.rawQuery("SELECT * FROM chat_rooms WHERE room_id='" + roomId + "';", null);
        while (cursor.moveToNext()) {
            result += cursor.getInt(0) //id
                    + " / "
                    + cursor.getString(1) //room name
                    + " / "
                    + cursor.getString(2) //updateTime
                    + " / "
                    + cursor.getString(3); //members

        }
        Log.d("초대", "chatRoomInfo_saved = "+result);

        return result;
    }


    //특정 방에서 사용자가 안 읽은 메시지 개수를 가져온다
    public int get_unreadMsgCount(int roomId){
        SQLiteDatabase db = getReadableDatabase();
        int count = 0;
        Log.d(TAG, "get_unreadMsgCount. roomId = "+roomId);

        Cursor cursor = db.rawQuery("SELECT count(*) FROM chat_logs WHERE room_id='" + roomId + "' AND isRead=0;", null);
        cursor.moveToFirst();
        count = cursor.getInt(0);

        Log.d(TAG, "unreadMsgCount of room "+roomId+" = "+count);

        return count;
    }


    //각 채팅방 메시지중에 가장 최신 메시지를 가져온다
    public String get_recentMessage(int roomId){
        SQLiteDatabase db = getReadableDatabase();
        String result = "";
        Log.d("msg", "get_recentMessage. roomId = "+roomId);

        Cursor cursor = db.rawQuery("SELECT message FROM chat_logs WHERE room_id='" + roomId + "' ORDER BY time DESC LIMIT 1;", null);
        while (cursor.moveToNext()) {
            result = cursor.getString(0);
        }
        Log.d("msg", "recent message of room "+roomId+" = "+result);

        return result;
    }


    //방마다 저장된 채팅 메시지를 가져온다
    public String get_savedMessages(int roomId){
        SQLiteDatabase db = getReadableDatabase();
        String result = "";
        Log.d(TAG, "get_recentMessage. roomId = "+roomId);

        //채팅내용 테이블: id, 방id, 보낸사람 id, 보낸사람 username, 메시지내용, 보낸시각
        Cursor cursor = db.rawQuery("SELECT * FROM chat_logs WHERE room_id='" + roomId + "' ORDER BY time DESC;", null);
        while (cursor.moveToNext()) {
            result += cursor.getInt(0) //id
                    + " / "
                    + cursor.getInt(1) //room id
                    + " / "
                    + cursor.getInt(2) //sender id
                    + " / "
                    + cursor.getString(3) //sender username
                    + " / "
                    + cursor.getString(4) //message
                    + " / "
                    + cursor.getString(5) //time
                    + " / "
                    + cursor.getString(6) //isRead
                    + "\n";
        }
        Log.d(TAG, "chatRoomInfo_saved = "+result);

        return result;
    }

}
