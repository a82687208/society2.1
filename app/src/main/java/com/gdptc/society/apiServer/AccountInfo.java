package com.gdptc.society.apiServer;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Administrator on 2018/6/11/011.
 */

public class AccountInfo implements Parcelable {
    private String phone;
    private long userPic;
    private String name;
    private String sex;
    private long schoolId;
    private String schoolName;
    private boolean online;
    private boolean isAdmin;
    private long stuId;
    private long societyId;

    public AccountInfo() {}

    protected AccountInfo(Parcel in) {
        phone = in.readString();
        userPic = in.readLong();
        name = in.readString();
        sex = in.readString();
        schoolId = in.readLong();
        schoolName = in.readString();
        online = in.readByte() != 0;
        isAdmin = in.readByte() != 0;
        stuId = in.readLong();
        societyId = in.readLong();
    }

    public static AccountInfo copy(AccountInfo accountInfo) {
        if (accountInfo == null)
            return null;

        AccountInfo accountInfo1 = new AccountInfo();
        accountInfo1.setPhone(accountInfo.getPhone());
        accountInfo1.setUserPic(accountInfo.getUserPic());
        accountInfo1.setName(accountInfo.getName());
        accountInfo1.setSex(accountInfo.getSex());
        accountInfo1.setSchoolId(accountInfo.getSchoolId());
        accountInfo1.setSchoolName(accountInfo.getSchoolName());
        accountInfo1.setOnline(accountInfo.isOnline());
        accountInfo1.setAdmin(accountInfo.isAdmin());
        accountInfo1.setStuId(accountInfo.getStuId());

        return accountInfo1;
    }

    public static final Creator<AccountInfo> CREATOR = new Creator<AccountInfo>() {
        @Override
        public AccountInfo createFromParcel(Parcel in) {
            return new AccountInfo(in);
        }

        @Override
        public AccountInfo[] newArray(int size) {
            return new AccountInfo[size];
        }
    };

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public long getUserPic() {
        return userPic;
    }

    public void setUserPic(long userPic) {
        this.userPic = userPic;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(long schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public boolean isAdmin() {
        return isAdmin;
    }

    public void setAdmin(boolean admin) {
        isAdmin = admin;
    }

    public long getStuId() {
        return stuId;
    }

    public void setStuId(long stuId) {
        this.stuId = stuId;
    }

    public void setSocietyId(long societyId) {
        this.societyId = societyId;
    }

    public long getSocietyId() {
        return societyId;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(phone);
        dest.writeLong(userPic);
        dest.writeString(name);
        dest.writeString(sex);
        dest.writeLong(schoolId);
        dest.writeString(schoolName);
        dest.writeByte((byte) (online ? 1 : 0));
        dest.writeByte((byte) (isAdmin ? 1 : 0));
        dest.writeLong(stuId);
        dest.writeLong(societyId);
    }
}
