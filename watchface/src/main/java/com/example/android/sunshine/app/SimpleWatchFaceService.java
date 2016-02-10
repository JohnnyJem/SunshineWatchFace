/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.sunshine.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


public class SimpleWatchFaceService extends CanvasWatchFaceService {
    private static String TAG = SimpleWatchFaceService.class.getSimpleName();

    private static final long INTERACTIVE_UPDATE_RATE_MS = TimeUnit.SECONDS.toMillis(1); //Used to update seconds once per second.
    private static final int UPDATE_TIME = 0; //Id for the handler used to update watch in interactive mode.

    private static final Typeface SANS_SERIF_NORMAL = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL);
    private static final Typeface SANS_SERIF_BOLD = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);

    @Override
    public Engine onCreateEngine() {
        return new Engine();
    }

    private class Engine extends CanvasWatchFaceService.Engine implements DataApi.DataListener,
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

        private static final String WEATHER_PATH = "/weather";
        private static final String WEATHER_INFO_PATH = "/weather-info";
        private static final String KEY_UUID = "uuid"; //needed to ensure a unique bundle is detected each time
        private static final String KEY_HIGH = "high";
        private static final String KEY_LOW = "low";
        private static final String KEY_WEATHER_ID = "weatherId";

        GoogleApiClient mGoogleApiClient = new GoogleApiClient.Builder(SimpleWatchFaceService.this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Wearable.API)
                .build();

        //Paints to draw
        Paint timePaint;
        Paint datePaint;
        Paint tempPaint;
        Paint linePaint;
        String mWeatherHigh;
        String mWeatherLow;
        Bitmap mWeatherIcon;

        private Calendar mCalendar;
        private static final String DATE_FORMAT = "%s, %s %d %d";
        private static final String BASE_24H_TIME_FORMAT = "%02d:%02d";
        private static final String BASE_12H_TIME_FORMAT = "%d:%02d";
        private static final String SECONDS_FORMAT = ":%02d";

        boolean mAmbient; // if true device is in ambient mode
        boolean mLowBitAmbient; // controls anti aliasing if lowBitAmbient mode detected. False = no AA.
        boolean shouldShowSeconds = true; //used to include seconds in textTime String.
        boolean mRegisteredTimeZoneReceiver = false;

        final Handler mUpdateTimeHandler = new EngineHandler(this);

        final BroadcastReceiver mTimeZoneReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                mCalendar.setTimeZone(TimeZone.getDefault());
                long now = System.currentTimeMillis();
                mCalendar.setTimeInMillis(now);
            }
        };


        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);

            setWatchFaceStyle(new WatchFaceStyle.Builder(SimpleWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT)
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(false)
                    .build());

            Resources resources = SimpleWatchFaceService.this.getResources();
            mCalendar = Calendar.getInstance();

            timePaint = createTextPaint(Color.WHITE, resources.getDimension(R.dimen.time_size), SANS_SERIF_BOLD);
            datePaint = createTextPaint(Color.WHITE, resources.getDimension(R.dimen.date_size), SANS_SERIF_NORMAL);
            tempPaint = createTextPaint(Color.WHITE, resources.getDimension(R.dimen.temp_size), SANS_SERIF_NORMAL);

            linePaint = new Paint();
            linePaint.setColor(Color.WHITE);
            linePaint.setAntiAlias(true);
        }

        @Override
        public void onDestroy() {
            mUpdateTimeHandler.removeMessages(UPDATE_TIME);
            super.onDestroy();
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            Resources resources = getResources();

            //if in ambient make the background black. If not draw background light blue.
            if (!mAmbient) {
                canvas.drawColor(resources.getColor(R.color.background));
            }else {
                canvas.drawColor(Color.BLACK);
            }

            // Here we use the systems Calendar to set time as
            //  H:MM in interactive mode. With optional H:MM:SS variables provided for future inclusion.
            long currentTimeMillis = System.currentTimeMillis();
            mCalendar.setTimeInMillis(currentTimeMillis);
            boolean is24Hour = DateFormat.is24HourFormat(SimpleWatchFaceService.this);
            int minute = mCalendar.get(Calendar.MINUTE);
            int second = mCalendar.get(Calendar.SECOND);
            int amPm  = mCalendar.get(Calendar.AM_PM);

            //secondsText String and amPM string are included for optional future addition.
            String secondsText = String.format(SECONDS_FORMAT, second);
            String amPmText = SimpleWatchFaceUtil.getAmPmString(resources, amPm);

            String timeText = getTimeTextString(is24Hour, minute, second, amPm);

            // Construct dateText String using data from our Calendar.
            String dayOfWeekString   = SimpleWatchFaceUtil.getDayString(resources, mCalendar.get(Calendar.DAY_OF_WEEK));
            int dayOfMonth = mCalendar.get(Calendar.DAY_OF_MONTH);
            String monthOfYearString = SimpleWatchFaceUtil.getMonthString(resources, mCalendar.get(Calendar.MONTH));
            int year = mCalendar.get(Calendar.YEAR);
            String dateText = String.format(DATE_FORMAT, dayOfWeekString, monthOfYearString, dayOfMonth, year);

            Log.d(TAG,"" + timeText + " " + dateText);


            //Draw timePaint
            float timeXOffset = computeXOffset(timeText, timePaint, bounds);
            float timeYOffset = computeTimeYOffset(timeText, timePaint, bounds);
            canvas.drawText(timeText, timeXOffset, timeYOffset, timePaint);

            //Draw datePaint
            float dateXOffset = computeXOffset(dateText, datePaint, bounds);
            float dateYOffset = computeDateYOffset(dateText, datePaint);
            canvas.drawText(dateText, dateXOffset, timeYOffset + dateYOffset , datePaint);

            //Draw temperature data if provided
            if (mWeatherHigh!=null && mWeatherLow !=null) {
                // Temperature Char = + (char) 0x00B0
                String tempText = mWeatherHigh + " " + mWeatherLow;
                float tempXOffset = computeXOffset(tempText, tempPaint, bounds);
                float tempYOffset = computeTempYOffset(tempText, tempPaint);

                canvas.drawText(tempText, tempXOffset + 25, timeYOffset + dateYOffset + tempYOffset, tempPaint);

                //draw weather icon if not im ambient mode
                if (mWeatherIcon !=null && !mAmbient){
                    Log.d(TAG, "mWeatherIcon not null " + mWeatherIcon.getWidth() + " " + mWeatherIcon.getHeight() );
                    float weatherXOffset = tempXOffset - mWeatherIcon.getWidth() + 15;
                    float weatherYOffset = timeYOffset + dateYOffset + (tempYOffset/2) - 5;
                    canvas.drawBitmap(mWeatherIcon, weatherXOffset, weatherYOffset, null);
                }

                //draw line seperator if not in ambient mode
                if (!mAmbient) {
                    float startX = tempXOffset;
                    float stopX = tempXOffset + tempPaint.measureText(tempText);
                    float startY = timeYOffset + dateYOffset + 20;
                    float stopY = startY;

                    canvas.drawLine(startX, startY, stopX, stopY, linePaint);
                }
            }
        }



        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            //make sure to disconnect/reconnect api's, listeners, receivers etc.. when visibility changes
            if (visible) {
                mGoogleApiClient.connect();
                registerReceiver();

                mCalendar.setTimeZone(TimeZone.getDefault());
                long currentTimeMillis = System.currentTimeMillis();
                mCalendar.setTimeInMillis(currentTimeMillis);
            } else {
                unregisterReceiver();
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, this);
                    mGoogleApiClient.disconnect();
                }
            }
            updateTimer();
        }

        private void registerReceiver() {
            if (mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = true;
            IntentFilter filter = new IntentFilter(Intent.ACTION_TIMEZONE_CHANGED);
            SimpleWatchFaceService.this.registerReceiver(mTimeZoneReceiver, filter);
        }

        private void unregisterReceiver() {
            if (!mRegisteredTimeZoneReceiver) {
                return;
            }
            mRegisteredTimeZoneReceiver = false;
            SimpleWatchFaceService.this.unregisterReceiver(mTimeZoneReceiver);
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            //check if our device is running/capable of LowBitAmbient mode
            mLowBitAmbient = properties.getBoolean(PROPERTY_LOW_BIT_AMBIENT, false);
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            if (mAmbient != inAmbientMode) {
                mAmbient = inAmbientMode; //update whether we are in ambient mode
                setAntiAlias(!inAmbientMode); //if in ambient mode remove antialiasing as it uses additional resources
                setColor(inAmbientMode ? Color.GRAY : Color.WHITE);
                setShowSeconds(!isInAmbientMode());
                invalidate(); //draw immediately
            }
            updateTimer();
        }

        public String getTimeTextString(boolean is24Hour, int minute, int second, int amPm){
            String timeText;
            if (is24Hour) {
                int hour = mCalendar.get(Calendar.HOUR_OF_DAY);

                timeText = String.format(BASE_24H_TIME_FORMAT, hour, minute);
            } else {
                int hour = mCalendar.get(Calendar.HOUR);
                if (hour == 0) {
                    hour = 12;
                }
                //Can add support for seconds and amPM string in this if/else
                if(!shouldShowSeconds) {
                    timeText = String.format(BASE_12H_TIME_FORMAT, hour, minute);
                }else {
                    //adding seconds text optional.doesn't look good.may be added in future.
                    timeText = String.format(BASE_12H_TIME_FORMAT, hour, minute);
                }
            }
            return timeText;
        }

        //The Following are methods we can use to manipulate paint objects

        private Paint createTextPaint(int textColor, float textSize, Typeface typeface) {
            Paint paint = new Paint();
            paint.setColor(textColor);
            paint.setTextSize(textSize);
            paint.setTypeface(typeface);
            paint.setAntiAlias(true);
            return paint;
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
            return centerY + (textHeight / 2.0f)-85.0f;//original 70f
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
            tempPaint.setAntiAlias(antiAlias);
        }

        public void setColor(int color) {
            timePaint.setColor(color);
            datePaint.setColor(color);
            tempPaint.setColor(color);
            linePaint.setColor(color);
        }

        public void setShowSeconds(boolean showSeconds) {
            shouldShowSeconds = showSeconds;
        }

        //Start timer if visible
        private void updateTimer() {
            mUpdateTimeHandler.removeMessages(UPDATE_TIME);
            if (isTimerVisible()) {
                mUpdateTimeHandler.sendEmptyMessage(UPDATE_TIME);
            }
        }

        //If timer is visible returns true to start it.
        private boolean isTimerVisible() {
            return isVisible() && !isInAmbientMode();
        }

        //updates time if time is visible
        private void handleUpdateTimeMessage() {
            invalidate(); //draw immediately
            if (isTimerVisible()) {
                long timeMs = System.currentTimeMillis();
                long delayMs = INTERACTIVE_UPDATE_RATE_MS - (timeMs % INTERACTIVE_UPDATE_RATE_MS);
                mUpdateTimeHandler.sendEmptyMessageDelayed(UPDATE_TIME, delayMs);
            }
        }


        //The following methods are used to interface with Googles Wearable Data Api

        public void requestWeatherInfo() {
            PutDataMapRequest putDataMapRequest = PutDataMapRequest.create(WEATHER_PATH);
            putDataMapRequest.getDataMap().putString(KEY_UUID, UUID.randomUUID().toString());
            PutDataRequest request = putDataMapRequest.asPutDataRequest();

            Wearable.DataApi.putDataItem(mGoogleApiClient, request)
                    .setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                        @Override
                        public void onResult(DataApi.DataItemResult dataItemResult) {
                            if (!dataItemResult.getStatus().isSuccess()) {
                                Log.d(TAG, "Failed asking phone for weather data");
                            } else {
                                Log.d(TAG, "Successfully asked for weather data");
                            }
                        }
                    });
        }

        @Override
        public void onConnected(Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, Engine.this);
            requestWeatherInfo();
        }

        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onDataChanged(DataEventBuffer dataEvents) {

            for (DataEvent dataEvent : dataEvents) {
                //If our data has changed (and we ensure it changes thanks to random UUID also included)
                //We check our datamap bundle.
                if (dataEvent.getType() == DataEvent.TYPE_CHANGED) {
                    DataMap dataMap = DataMapItem.fromDataItem(dataEvent.getDataItem()).getDataMap();
                    String path = dataEvent.getDataItem().getUri().getPath();
                    Log.d(TAG, path);

                    //Check if our DataMap Bundle contains the needed info in the path.
                    if (path.equals(WEATHER_INFO_PATH)) {
                        if (dataMap.containsKey(KEY_HIGH)) {
                            mWeatherHigh = dataMap.getString(KEY_HIGH);
                            Log.d(TAG, "Temp High = " + mWeatherHigh);
                        }
                        if (dataMap.containsKey(KEY_LOW)) {
                            mWeatherLow = dataMap.getString(KEY_LOW);
                            Log.d(TAG, "Temp Low = " + mWeatherLow);
                        }
                        if (dataMap.containsKey(KEY_WEATHER_ID)) {
                            int weatherId = dataMap.getInt(KEY_WEATHER_ID);
                            Log.d(TAG, " weatherID = " + weatherId);

                            //Create a bitmap according to retrieved weatherID
                            // using SimpleWatchFaceUtil helper method.
                            Drawable b = getResources().getDrawable(SimpleWatchFaceUtil.getWeatherBitmapIconID(weatherId));
                            Bitmap icon = ((BitmapDrawable) b).getBitmap();
                            float scaledWidth = (tempPaint.getTextSize() / icon.getHeight()) * icon.getWidth() + 5;
                            Log.d(TAG, "tempPaintTextSize = " + Float.toString(tempPaint.getTextSize()));

                            //Scale bitmap accordingly
                            mWeatherIcon = Bitmap.createScaledBitmap(icon, (int) scaledWidth, (int) tempPaint.getTextSize() + 5, true);
                        }
                        invalidate();
                    }
                }
            }
        }

        @Override
        public void onConnectionFailed(ConnectionResult connectionResult) {

        }
    }

    /*Note to myself: Helper class to handle secheduled messages.
    * "Static inner classes do not hold an implicit reference to their outer class,
    * so the activity will not be leaked"
    * from http://www.androiddesignpatterns.com/2013/01/inner-class-handler-memory-leak.html
    * */
    private static class EngineHandler extends Handler {
        /*Not to self:
        *"To fix the memory leak that occurs when we instantiate the anonymous Runnable class,
        * we make the variable a static field of the class
        * (since static instances of anonymous classes do not hold an
        * implicit reference to their outer class)"
        * */
        private final WeakReference<Engine> mWeakReference;

        public EngineHandler(Engine reference) {
            mWeakReference = new WeakReference<>(reference);
        }

        @Override
        public void handleMessage(Message msg) {
            Engine engine = mWeakReference.get();
            if (engine != null) {
                switch (msg.what) {
                    case UPDATE_TIME:
                        engine.handleUpdateTimeMessage();
                        break;
                }
            }
        }
    }
}
