package com.android.canaria.recyclerView;

import android.graphics.Bitmap;
import android.widget.Switch;

public class RoomListItem {

//    private Bitmap roomImage;
    private String roomName;
    private String recentMessage;
    String updatedTime;

    private int roomId;
    private int numberOfMembers;
    private int unreadMsgCount;


    public RoomListItem(String roomName, int numberOfMembers, String recentMessage, String updatedTime, int roomId, int unreadMsgCount) {
//        this.roomImage = roomImage;
        this.roomName = roomName;
        this.recentMessage = recentMessage;
        this.updatedTime = updatedTime;
        this.numberOfMembers = numberOfMembers;
        this.roomId = roomId;
        this.unreadMsgCount = unreadMsgCount;
    }


//    public Bitmap getRoomImage() {
//        return roomImage;
//    }
//
//    public void setRoomImage(Bitmap roomImage) {
//        this.roomImage = roomImage;
//    }

    public int getUnreadMsgCount() {
        return unreadMsgCount;
    }

    public void setUnreadMsgCount(int unreadMsgCount) {
        this.unreadMsgCount = unreadMsgCount;
    }

    public int getRoomId() {
        return roomId;
    }

    public void setRoomId(int roomId) {
        this.roomId = roomId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getRecentMessage() {
        return recentMessage;
    }

    public void setRecentMessage(String recentMessage) {
        this.recentMessage = recentMessage;
    }

    public String getUpdatedTime() {
        return updatedTime;
    }

    public void setUpdatedTime(String updatedTime) {
        this.updatedTime = updatedTime;
    }

    public int getNumberOfMembers() {
        return numberOfMembers;
    }

    public void setNumberOfMembers(int numberOfMembers) {
        this.numberOfMembers = numberOfMembers;
    }





}
