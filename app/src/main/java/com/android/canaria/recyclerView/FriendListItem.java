package com.android.canaria.recyclerView;

public class FriendListItem {

//    private Bitmap profileImage;
    private String friendName;
    private String friendId;


    public FriendListItem(String friendName, String friendId) {
        this.friendName = friendName;
        this.friendId = friendId;
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

    public String getFriendId() {
        return friendId;
    }

    public void setFriendId(String friendId) {
        this.friendId = friendId;
    }


}
