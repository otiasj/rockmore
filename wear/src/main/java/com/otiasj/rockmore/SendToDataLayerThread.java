package com.otiasj.rockmore;

import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

/**
 * Created by juliensaito on 4/10/16.
 */
class SendToDataLayerThread extends Thread {
    private final static String TAG = SendToDataLayerThread.class.getCanonicalName();
    private final String path;
    private final DataMap dataMap;
    private final GoogleApiClient googleApiClient;

    // Constructor for sending data objects to the data layer
    SendToDataLayerThread(final GoogleApiClient googleClient, String p, DataMap data) {
        path = p;
        dataMap = data;
        googleApiClient = googleClient;
    }

    public void run() {
        if (googleApiClient != null) {
            // Construct a DataRequest and send over the data layer
            PutDataMapRequest putDMR = PutDataMapRequest.create(path);
            putDMR.getDataMap().putAll(dataMap);
            PutDataRequest request = putDMR.asPutDataRequest();
            DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient, request).await();

            if (result.getStatus().isSuccess()) {
                Log.v(TAG, "DataMap: " + dataMap + " sent successfully to data layer ");
            } else {
                Log.e(TAG, "ERROR: failed to send DataMap to data layer");
            }
        } else {
            Log.e(TAG, "ERROR: googleApiClient is null");
        }
    }
}