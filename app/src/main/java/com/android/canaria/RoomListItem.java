package com.android.canaria;

import android.graphics.Bitmap;

public class RoomListItem {

//    private Bitmap roomImage;
    private String roomName, recentMessage, updatedTime;
    private int numberOfMembers;


    public RoomListItem(String roomName, int numberOfMembers, String recentMessage, String updatedTime) {
//        this.roomImage = roomImage;
        this.roomName = roomName;
        this.recentMessage = recentMessage;
        this.updatedTime = updatedTime;
        this.numberOfMembers = numberOfMembers;
    }


//    public Bitmap getRoomImage() {
//        return roomImage;
//    }
//
//    public void setRoomImage(Bitmap roomImage) {
//        this.roomImage = roomImage;
//    }

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
