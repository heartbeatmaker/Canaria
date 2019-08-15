package com.android.canaria;

public class SlideImageItem {


    String type;
    int room_id;
    String filename;
    String url;
    String video_path;

    public SlideImageItem(String type, int room_id, String filename, String url, String video_path) {
        this.type = type;
        this.room_id = room_id;
        this.filename = filename;
        this.url = url;
        this.video_path = video_path;
    }



    public String getType() {
        return type;
    }

    public int getRoom_id() {
        return room_id;
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }

    public String getVideo_path() {
        return video_path;
    }


}
