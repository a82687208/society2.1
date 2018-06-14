package com.gdptc.society.tools;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by hasee on 2017/10/31.
 */

public class TimeUtil {
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public String getLocalTime() {
        return dateFormat.format(Calendar.getInstance().getTime());
    }

    public long getTimeMill(String time) throws ParseException {
        return dateFormat.parse(time).getTime();
    }

    public String getTimeTxt(Date date) {
        return dateFormat.format(date);
    }

    public Date getTimeTxt(String time) {
        try {
            return dateFormat.parse(time);
        }
        catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean isToday(Calendar dataTime, Calendar localTime) {
        return isThisYear(dataTime, localTime) && isThisMonth(dataTime, localTime)
                && dataTime.get(Calendar.DAY_OF_MONTH) == localTime.get(Calendar.DAY_OF_MONTH);
    }

    public boolean isThisMonth(Calendar dataTime, Calendar localTime) {
        return dataTime.get(Calendar.MONTH) == localTime.get(Calendar.MONTH);
    }

    public boolean isThisYear(Calendar dataTime, Calendar localTime) {
        return dataTime.get(Calendar.YEAR) == localTime.get(Calendar.YEAR);
    }

    public String getTimeTxt(Calendar dataTime, String timeStr) {
        Calendar calendar = Calendar.getInstance();
        return getTimeTxt(dataTime, calendar, timeStr);
    }

    public String getTimeTxt(Calendar dataTime, Calendar localTime, String timeStr) {
        if (isThisYear(dataTime, localTime))
            timeStr = timeStr.substring(timeStr.indexOf("-") + 1, timeStr.length());
        else
            return timeStr;
        if (isToday(dataTime, localTime))
           return timeStr.substring(timeStr.lastIndexOf(" ") + 1, timeStr.lastIndexOf(":"));
        else {
            localTime.add(Calendar.DAY_OF_MONTH, -1);
            return isToday(dataTime, localTime) ? "昨天 " + timeStr.substring(timeStr.lastIndexOf(" ") + 1,
                    timeStr.lastIndexOf(":")) : timeStr.substring(0, timeStr.lastIndexOf(":"));
        }
    }

    public String getTimeFilterTxt(String timeStr) {
        if (timeStr == null)
            return null;
        return timeStr.substring(0, timeStr.lastIndexOf(":"));
    }
}
