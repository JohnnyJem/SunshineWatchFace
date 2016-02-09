package com.example.android.sunshine.app;

import android.content.res.Resources;
import android.support.annotation.NonNull;
import java.util.Calendar;


public final class SimpleWatchFaceUtil {
    //The following methods are adapted from our mobile "app" module's Utility.class

    public static String getAmPmString(Resources resources, int amPm) {
        return amPm == Calendar.AM ? resources.getString(R.string.am) : resources.getString(R.string.pm);
    }

    @NonNull // used to indicate that a given parameter (or return value) can not be null.
    public static String getDayString(Resources resources, int day) {
        int dayOfWeekString;
        switch (day) {
            case Calendar.SUNDAY:
                dayOfWeekString = R.string.sunday;
                break;
            case Calendar.MONDAY:
                dayOfWeekString = R.string.monday;
                break;
            case Calendar.TUESDAY:
                dayOfWeekString = R.string.tuesday;
                break;
            case Calendar.WEDNESDAY:
                dayOfWeekString = R.string.wednesday;
                break;
            case Calendar.THURSDAY:
                dayOfWeekString = R.string.thursday;
                break;
            case Calendar.FRIDAY:
                dayOfWeekString = R.string.friday;
                break;
            case Calendar.SATURDAY:
                dayOfWeekString = R.string.saturday;
                break;
            default:
                dayOfWeekString = -1;
        }

        if (dayOfWeekString != -1) {
            return resources.getString(dayOfWeekString);
        }

        return "";
    }

    @NonNull // used to indicate that a given parameter (or return value) can not be null.
    public static String getMonthString(Resources resources, int monthOfYear) {
        int monthOfYearString;
        switch(monthOfYear) {
            case Calendar.JANUARY:
                monthOfYearString = R.string.january;
                break;
            case Calendar.FEBRUARY:
                monthOfYearString = R.string.february;
                break;
            case Calendar.MARCH:
                monthOfYearString = R.string.march;
                break;
            case Calendar.APRIL:
                monthOfYearString = R.string.april;
                break;
            case Calendar.MAY:
                monthOfYearString = R.string.may;
                break;
            case Calendar.JUNE:
                monthOfYearString = R.string.june;
                break;
            case Calendar.JULY:
                monthOfYearString = R.string.july;
                break;
            case Calendar.AUGUST:
                monthOfYearString = R.string.august;
                break;
            case Calendar.SEPTEMBER:
                monthOfYearString = R.string.september;
                break;
            case Calendar.OCTOBER:
                monthOfYearString = R.string.october;
                break;
            case Calendar.NOVEMBER:
                monthOfYearString = R.string.november;
                break;
            case Calendar.DECEMBER:
                monthOfYearString = R.string.december;
                break;
            default:
                monthOfYearString = -1;
        }

        if (monthOfYearString != -1) {
            return resources.getString(monthOfYearString);
        }

        return "";
    }


    //Returns drawable id appropriate for given weather condition
    public static int getWeatherBitmapIconID(int weatherId) {

        // See http://bugs.openweathermap.org/projects/api/wiki/Weather_Condition_Codes
        // for info on data ranges used to determine appropriate icon in this switch statement
        if (weatherId >= 200 && weatherId <= 232) {
            return R.drawable.ic_storm;
        } else if (weatherId >= 300 && weatherId <= 321) {
            return R.drawable.ic_light_rain;
        } else if (weatherId >= 500 && weatherId <= 504) {
            return R.drawable.ic_rain;
        } else if (weatherId == 511) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 520 && weatherId <= 531) {
            return R.drawable.ic_rain;
        } else if (weatherId >= 600 && weatherId <= 622) {
            return R.drawable.ic_snow;
        } else if (weatherId >= 701 && weatherId <= 761) {
            return R.drawable.ic_fog;
        } else if (weatherId == 761 || weatherId == 781) {
            return R.drawable.ic_storm;
        } else if (weatherId == 800) {
            return R.drawable.ic_clear;
        } else if (weatherId == 801) {
            return R.drawable.ic_light_clouds;
        } else if (weatherId >= 802 && weatherId <= 804) {
            return R.drawable.ic_cloudy;
        }
        return -1;
    }

}
