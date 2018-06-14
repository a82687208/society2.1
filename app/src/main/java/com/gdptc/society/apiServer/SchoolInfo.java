package com.gdptc.society.apiServer;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Administrator on 2018/1/6/006.
 */

public class SchoolInfo implements Parcelable {
    private String addr;
    private String brief_introduction;
    private String code;
    private long college_id;
    private String college_nature;
    private boolean edu_directly;
    private String email;
    private String level;
    private String logo;
    private String member_ship;
    private String name;
    private String official_website;
    private String old_name;
    private String property;
    private String province;
    private String ranking;
    private String ranking_collegetype;
    private String recruit_tel;
    private String recruit_website;
    private String tuition_fee;
    private String type;

    public SchoolInfo() {}

    protected SchoolInfo(Parcel in) {
        addr = in.readString();
        brief_introduction = in.readString();
        code = in.readString();
        college_id = in.readLong();
        college_nature = in.readString();
        edu_directly = in.readByte() != 0;
        email = in.readString();
        level = in.readString();
        logo = in.readString();
        member_ship = in.readString();
        name = in.readString();
        official_website = in.readString();
        old_name = in.readString();
        property = in.readString();
        province = in.readString();
        ranking = in.readString();
        ranking_collegetype = in.readString();
        recruit_tel = in.readString();
        recruit_website = in.readString();
        tuition_fee = in.readString();
        type = in.readString();
    }

    public static final Creator<SchoolInfo> CREATOR = new Creator<SchoolInfo>() {
        @Override
        public SchoolInfo createFromParcel(Parcel in) {
            return new SchoolInfo(in);
        }

        @Override
        public SchoolInfo[] newArray(int size) {
            return new SchoolInfo[size];
        }
    };

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }

    public String getBrief_introduction() {
        return brief_introduction;
    }

    public void setBrief_introduction(String brief_introduction) {
        this.brief_introduction = brief_introduction;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public long getCollege_id() {
        return college_id;
    }

    public void setCollege_id(long college_id) {
        this.college_id = college_id;
    }

    public String getCollege_nature() {
        return college_nature;
    }

    public void setCollege_nature(String college_nature) {
        this.college_nature = college_nature;
    }

    public boolean isEdu_directly() {
        return edu_directly;
    }

    public void setEdu_directly(boolean edu_directly) {
        this.edu_directly = edu_directly;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getLogo() {
        return logo;
    }

    public void setLogo(String logo) {
        this.logo = logo;
    }

    public String getMember_ship() {
        return member_ship;
    }

    public void setMember_ship(String member_ship) {
        this.member_ship = member_ship;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOfficial_website() {
        return official_website;
    }

    public void setOfficial_website(String official_website) {
        this.official_website = official_website;
    }

    public String getOld_name() {
        return old_name;
    }

    public void setOld_name(String old_name) {
        this.old_name = old_name;
    }

    public String getProperty() {
        return property;
    }

    public void setProperty(String property) {
        this.property = property;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getRanking() {
        return ranking;
    }

    public void setRanking(String ranking) {
        this.ranking = ranking;
    }

    public String getRanking_collegetype() {
        return ranking_collegetype;
    }

    public void setRanking_collegetype(String ranking_collegetype) {
        this.ranking_collegetype = ranking_collegetype;
    }

    public String getRecruit_tel() {
        return recruit_tel;
    }

    public void setRecruit_tel(String recruit_tel) {
        this.recruit_tel = recruit_tel;
    }

    public String getRecruit_website() {
        return recruit_website;
    }

    public void setRecruit_website(String recruit_website) {
        this.recruit_website = recruit_website;
    }

    public String getTuition_fee() {
        return tuition_fee;
    }

    public void setTuition_fee(String tuition_fee) {
        this.tuition_fee = tuition_fee;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(addr);
        dest.writeString(brief_introduction);
        dest.writeString(code);
        dest.writeLong(college_id);
        dest.writeString(college_nature);
        dest.writeByte((byte) (edu_directly ? 1 : 0));
        dest.writeString(email);
        dest.writeString(level);
        dest.writeString(logo);
        dest.writeString(member_ship);
        dest.writeString(name);
        dest.writeString(official_website);
        dest.writeString(old_name);
        dest.writeString(property);
        dest.writeString(province);
        dest.writeString(ranking);
        dest.writeString(ranking_collegetype);
        dest.writeString(recruit_tel);
        dest.writeString(recruit_website);
        dest.writeString(tuition_fee);
        dest.writeString(type);
    }
}
