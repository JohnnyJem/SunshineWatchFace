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

package com.example.wear;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

import java.util.concurrent.TimeUnit;

/**
 * Digital watch face with seconds. In ambient mode, the seconds aren't displayed. On devices with
 * low-bit ambient mode, the text is drawn without anti-aliasing in ambient mode.
 */
    public class SimpleWatchFaceService extends CanvasWatchFaceService {
    private final String TAG = this.getClass().getSimpleName();

    @Override
    public Engine onCreateEngine() {
        return new SimpleEngine();
    }

    private class SimpleEngine extends Engine  implements
            GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{

        private final long TICK_PERIOD_MILLIS = TimeUnit.SECONDS.toMillis(1);
        private Handler timeTick; //using handler b/c we want it to be executed on the mainthread
        private SimpleWatchFace watchFace;

        //Keys used in DataMap Bundle
        public final String KEY_PATH = "/wear";
        public final String KEY_WEATHER_ID = "KEY_WEATHER_ID";
        public final String KEY_MAX_TEMP = "KEY_MAX_TEMP";
        public final String KEY_LOW_TEMP = "KEY_LOW_TEMP";

        private String maxTemp = "";
        private String minTemp = "";

        GoogleApiClient mGoogleApiClient;

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);



            //define custom watchface style
            setWatchFaceStyle(new WatchFaceStyle.Builder(SimpleWatchFaceService.this)
                    .setCardPeekMode(WatchFaceStyle.PEEK_MODE_SHORT) // first card peeked and shown on the watch will have a small height
                    .setAmbientPeekMode(WatchFaceStyle.AMBIENT_PEEK_MODE_HIDDEN) //in ambient mode, no peek card will be visible
                    .setBackgroundVisibility(WatchFaceStyle.BACKGROUND_VISIBILITY_INTERRUPTIVE) //if interruptive notification peek card will be shown briefly
                    .setShowSystemUiTime(false) // false b/c we will draw it on the canvas ourselves
                    .build());

            mGoogleApiClient = new GoogleApiClient.Builder(SimpleWatchFaceService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            timeTick = new Handler(Looper.myLooper());
            startTimerIfNecessary(); // if the watch is visible and not in ambient mode
            watchFace = SimpleWatchFace.newInstance(SimpleWatchFaceService.this);
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            super.onDraw(canvas, bounds);
            watchFace.draw(canvas, bounds, maxTemp, minTemp);
        }

        private void startTimerIfNecessary() {
            timeTick.removeCallbacks(timeRunnable);
            if (isVisible() && !isInAmbientMode()) {
                timeTick.post(timeRunnable);
            }
        }

        private final Runnable timeRunnable = new Runnable() {
            @Override
            public void run() {
                onSecondTick();
                if (isVisible() && !isInAmbientMode()) {
                    timeTick.postDelayed(this, TICK_PERIOD_MILLIS);
                }
            }
        };

        private void onSecondTick() {
            invalidateIfNecessary();
        }

        private void invalidateIfNecessary() {
            if (isVisible() && !isInAmbientMode()) {
                invalidate();
            }
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            if (visible) {
                mGoogleApiClient.connect();
            } else {
                if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
                    Wearable.DataApi.removeListener(mGoogleApiClient, onDataChangedListener);
                    mGoogleApiClient.disconnect();
                }
            }
            startTimerIfNecessary();
        }


        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);

            watchFace.setAntiAlias(!inAmbientMode);
            watchFace.setColor(inAmbientMode ? Color.GRAY : Color.parseColor(SimpleWatchFaceUtil.DATE_TEXT_COLOR));
            watchFace.setShowSeconds(!isInAmbientMode());
            invalidate();
            startTimerIfNecessary();
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();
            invalidate();
        }

        @Override
        public void onDestroy() {
            timeTick.removeCallbacks(timeRunnable);
            if(mGoogleApiClient!=null && mGoogleApiClient.isConnected()){
                Wearable.DataApi.removeListener(mGoogleApiClient, onDataChangedListener);
                mGoogleApiClient.disconnect();
            }
            super.onDestroy();
        }

        @Override
        public void onConnected(@Nullable Bundle bundle) {
            Wearable.DataApi.addListener(mGoogleApiClient, onDataChangedListener);
            Wearable.DataApi.getDataItems(mGoogleApiClient).setResultCallback(onConnectedResultCallback);
            Log.d(TAG, "onConnected: Connected to Wear Api");
            //updateConfigDataItemAndUiOnStartup();
        }

        /*
        * The following methods for receiving configuration were implemented with the help of
        * Andrei Catinean's guide
        * found at http://catinean.com/2015/03/28/creating-a-watch-face-with-android-wear-api-part-2/
        * #HANDLING THE RECEIVED CONFIGURATION IN THE SIMPLEWATCHFACESERVICE*/
        private final DataApi.DataListener onDataChangedListener = new DataApi.DataListener(){
            @Override
            public void onDataChanged(DataEventBuffer dataEventBuffer) {
                for (DataEvent dataEvent : dataEventBuffer) {
                    DataItem dataItem = dataEvent.getDataItem();
                    if (dataEvent.getType() != DataEvent.TYPE_CHANGED) {
                        DataItem item = dataEvent.getDataItem();
                        if (dataItem.getUri().getPath().equals(KEY_PATH)) {
                            DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                            maxTemp = dataMap.getString(KEY_MAX_TEMP);
                            minTemp = dataMap.getString(KEY_LOW_TEMP);
                            Log.d(TAG, "onDataChanged: "+ maxTemp + " " + minTemp);
                        }
                    }
                }
                dataEventBuffer.release();
                invalidateIfNecessary();
            }
        };

        private final ResultCallback<DataItemBuffer> onConnectedResultCallback = new ResultCallback<DataItemBuffer>() {
            @Override
            public void onResult(DataItemBuffer dataItems) {
                for (DataItem item : dataItems) {
                    processConfigurationFor(item);
                }

                dataItems.release();
                invalidateIfNecessary();
            }
        };

        private void processConfigurationFor(DataItem item) {
            if (KEY_PATH.equals(item.getUri().getPath())) {
                DataMap dataMap = DataMapItem.fromDataItem(item).getDataMap();
                if (dataMap.containsKey(KEY_LOW_TEMP)) {
                   minTemp = dataMap.getString(KEY_LOW_TEMP);
                }
                if (dataMap.containsKey(KEY_MAX_TEMP)) {
                  maxTemp= dataMap.getString(KEY_MAX_TEMP);
                }
                Log.d(TAG, "processConfigurationFor: "+ maxTemp + " " + minTemp);
            }
        }








        @Override
        public void onConnectionSuspended(int i) {

        }

        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        }
    }


}
