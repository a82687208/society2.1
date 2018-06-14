package com.gdptc.society.apiServer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2017/9/20/020.
 */

public class ImgInfo implements Parcelable {
    public String id;
    public String path;
    public String bucketName;
    public boolean selector = false;

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(path);
        dest.writeString(bucketName);
        dest.writeByte((byte) (selector ? 1 : 0));
    }

    public static final Creator<ImgInfo> CREATOR = new Creator<ImgInfo>() {
        public ImgInfo createFromParcel(Parcel in)
        {
            return new ImgInfo(in);
        }

        public ImgInfo[] newArray(int size)
        {
            return new ImgInfo[size];
        }
    };

    private ImgInfo(Parcel in) {
        id = in.readString();
        path = in.readString();
        bucketName = in.readString();
        selector = in.readByte() != 0;
    }

    public ImgInfo() {}
}
