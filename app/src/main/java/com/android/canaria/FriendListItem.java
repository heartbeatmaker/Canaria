package com.android.canaria;

public class FriendListItem {

//    private Bitmap profileImage;
    private String friendName;


    public FriendListItem(String friendName) {
        this.friendName = friendName;
//        this.roomImage = roomImage;
    }


//    public Bitmap getProfileImage() {
//        return profileImage;
//    }
//
//    public void setProfileImage(Bitmap profileImage) {
//        this.profileImage = profileImage;
//    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }


}
