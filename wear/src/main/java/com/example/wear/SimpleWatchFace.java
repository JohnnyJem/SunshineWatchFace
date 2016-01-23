package com.example.wear;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.format.Time;

import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Helper class from http://catinean.com/2015/03/07/creating-a-watch-face-with-android-wear-api/
 */

public class SimpleWatchFace {

    private static final String TIME_FORMAT_WITHOUT_SECONDS = "%02d:%02d";
    private static final String DATE_FORMAT = "%02d %02d %d";

    private final Paint timePaint;
    private final Paint datePaint;
    private final Paint tempPaint;
    private final Time time;

    private boolean shouldShowSeconds = true;

    public static SimpleWatchFace newInstance(Context context) {
        Paint timePaint = new Paint();
        timePaint.setColor(Color.WHITE);
        timePaint.setTextSize(context.getResources().getDimension(R.dimen.time_size));
        timePaint.setAntiAlias(true);

        Paint datePaint = new Paint();
        datePaint.setColor(Color.parseColor(SimpleWatchFaceUtil.DATE_TEXT_COLOR));
        datePaint.setTextSize(context.getResources().getDimension(R.dimen.date_size));
        datePaint.setAntiAlias(true);

        Paint tempPaint = new Paint();
        tempPaint.setColor(Color.WHITE);
        tempPaint.setTextSize(context.getResources().getDimension(R.dimen.temp_size));

        return new SimpleWatchFace(timePaint, datePaint, tempPaint, new Time());
    }

    SimpleWatchFace(Paint timePaint, Paint datePaint, Paint tempPaint, Time time) {
        this.timePaint = timePaint;
        this.datePaint = datePaint;
        this.tempPaint = tempPaint;
        this.time = time;
    }

    public void draw(Canvas canvas, Rect bounds, String maxTemp, String minTemp) {
        time.setToNow();
        canvas.drawColor(Color.parseColor(SimpleWatchFaceUtil.BACKGROUND_COLOR));

        String timeText = String.format(TIME_FORMAT_WITHOUT_SECONDS, time.hour, time.minute);
        float timeXOffset = computeXOffset(timeText, timePaint, bounds);
        float timeYOffset = computeTimeYOffset(timeText, timePaint, bounds);
        canvas.drawText(timeText, timeXOffset, timeYOffset, timePaint);


        String myDate = String.format(DATE_FORMAT, (time.month + 1), time.monthDay, time.year);
        String dateText = convertDate(myDate);
        float dateXOffset = computeXOffset(dateText, datePaint, bounds);
        float dateYOffset = computeDateYOffset(dateText, datePaint);
        canvas.drawText(dateText, dateXOffset, timeYOffset + dateYOffset, datePaint);

        if (maxTemp!=null && minTemp !=null) {
            String tempText = maxTemp + (char) 0x00B0 + minTemp + (char) 0x00B0;
            float tempXOffset = computeXOffset(tempText, tempPaint, bounds);
            float tempYOffset = computeTempYOffset(tempText, tempPaint);
            canvas.drawText(tempText, tempXOffset, timeYOffset + dateYOffset + tempYOffset, tempPaint);
        }
    }



    private String convertDate(String date) {
        try {
            SimpleDateFormat format = new SimpleDateFormat("MM dd yyyy");
            Date d = format.parse(date);
            SimpleDateFormat serverFormat = new SimpleDateFormat("EEE, MMM dd yyyy");
            return serverFormat.format(d).toUpperCase();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private float computeXOffset(String text, Paint paint, Rect watchBounds) {
        float centerX = watchBounds.exactCenterX();
        float timeLength = paint.measureText(text);
        return centerX - (timeLength / 2.0f);
    }

    private float computeTimeYOffset(String timeText, Paint timePaint, Rect watchBounds) {
        float centerY = watchBounds.exactCenterY();
        Rect textBounds = new Rect();
        timePaint.getTextBounds(timeText, 0, timeText.length(), textBounds);
        int textHeight = textBounds.height();
        return centerY + (textHeight / 2.0f)-70.0f;
    }

    private float computeDateYOffset(String dateText, Paint datePaint) {
        Rect textBounds = new Rect();
        datePaint.getTextBounds(dateText, 0, dateText.length(), textBounds);
        return textBounds.height() + 20.0f;
    }

    private float computeTempYOffset(String tempText, Paint tempPaint) {
        Rect textBounds = new Rect();
        tempPaint.getTextBounds(tempText, 0, tempText.length(), textBounds);
        return textBounds.height() + 40.0f;
    }

    public void setAntiAlias(boolean antiAlias) {
        timePaint.setAntiAlias(antiAlias);
        datePaint.setAntiAlias(antiAlias);
    }

    public void setColor(int color) {
        timePaint.setColor(color);
        datePaint.setColor(color);

    }

    public void setShowSeconds(boolean showSeconds) {
        shouldShowSeconds = showSeconds;
    }


}
