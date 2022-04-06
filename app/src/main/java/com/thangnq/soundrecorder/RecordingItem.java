package com.thangnq.soundrecorder;

import android.os.Parcel;
import android.os.Parcelable;

public class RecordingItem implements Parcelable {
    private String Name;
    private String filePath;
    private int id;
    private int length;
    private long time;

    public RecordingItem(){

    }

    public RecordingItem(String name, String filePath, int id, int length, long time) {
        Name = name;
        this.filePath = filePath;
        this.id = id;
        this.length = length;
        this.time = time;
    }

    public RecordingItem(String name, String filePath, int length, long time) {
        Name = name;
        this.filePath = filePath;
        this.length = length;
        this.time = time;
    }

    public RecordingItem(Parcel in){
        Name = in.readString();
        filePath = in.readString();
        id = in.readInt();
        length = in.readInt();
        time = in.readLong();
    }

    public static final Creator<RecordingItem> CREATOR = new Creator<RecordingItem>() {
        @Override
        public RecordingItem createFromParcel(Parcel in) {
            return new RecordingItem(in);
        }

        @Override
        public RecordingItem[] newArray(int size) {
            return new RecordingItem[size];
        }
    };

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(Name);
        parcel.writeString(filePath);
        parcel.writeInt(id);
        parcel.writeInt(length);
        parcel.writeLong(time);
    }
}
