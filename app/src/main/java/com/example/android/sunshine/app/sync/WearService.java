package com.example.android.sunshine.app.sync;

import android.app.IntentService;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by Johnny on 1/14/2016.
 * Following Guide at http://catinean.com/2015/03/28/creating-a-watch-face-with-android-wear-api-part-2/
 * "Sending Data to the Data Layer Api"
 */
public class WearService extends IntentService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener  {

    public static String UPDATE_WEAR = "UPDATE_WEAR";
    public static String TAG = WearService.class.getSimpleName();

    //Keys used in DataMap Bundle
    public static String KEY_PATH = "/wear";
    public static String KEY_WEATHER_ID = "KEY_WEATHER_ID";
    public static String KEY_MAX_TEMP = "KEY_MAX_TEMP";
    public static String KEY_LOW_TEMP = "KEY_LOW_TEMP";

    private GoogleApiClient mGoogleApiClient;

    public WearService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null && intent.getExtras() != null) {
            mGoogleApiClient = new GoogleApiClient.Builder(WearService.this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();

            mGoogleApiClient.connect();
        }
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {
        //begin building query
        String location = Utility.getPreferredLocation(this);
        Uri weatherUri = WeatherContract.WeatherEntry
                .buildWeatherLocationWithDate(location, System.currentTimeMillis());
        Cursor cursor = getContentResolver().query(
                weatherUri,
                new String[] {WeatherContract.WeatherEntry.COLUMN_WEATHER_ID,
                WeatherContract.WeatherEntry.COLUMN_MAX_TEMP,
                WeatherContract.WeatherEntry.COLUMN_MIN_TEMP},
                null,null,null);

        if (cursor.moveToFirst()){
            int weatherId = cursor.getInt(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_WEATHER_ID));
            String maxTemp = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MAX_TEMP));
            String minTemp = cursor.getString(cursor.getColumnIndex(WeatherContract.WeatherEntry.COLUMN_MIN_TEMP));

            //set path where data can be accessed and set queried data in DataMap Bundle
            PutDataMapRequest dataMapRequest = PutDataMapRequest.create(KEY_PATH);
            dataMapRequest.getDataMap().putInt(KEY_WEATHER_ID, weatherId);
            dataMapRequest.getDataMap().putString(KEY_MAX_TEMP, maxTemp);
            dataMapRequest.getDataMap().putString(KEY_LOW_TEMP, minTemp);
            //set the data to be delivered
            PendingResult<DataApi.DataItemResult> pendingResult =
                    Wearable.DataApi.putDataItem(mGoogleApiClient, dataMapRequest.asPutDataRequest());
        }
        cursor.close();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mGoogleApiClient.disconnect();
    }
}
