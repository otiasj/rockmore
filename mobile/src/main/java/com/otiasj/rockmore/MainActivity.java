package com.otiasj.rockmore;

import android.hardware.Sensor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends AppCompatActivity implements SensorListener, DataApi.DataListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String WEARABLE_DATA_PATH = "/wearable_data";
    private static final String TAG = MainActivity.class.getCanonicalName();
    private GoogleApiClient mGoogleApiClient;

    private Audio audio;
    private Graph graph;
    private Sensors sensors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        graph = new Graph((LineChart) findViewById(R.id.chart1));
        sensors = new Sensors(this, this);
        audio = new Audio();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        audio.stop();
        sensors.stop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        audio.start();
        sensors.start();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Connected to Google Api Service");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(final int i) {
        Log.v(TAG, "connection suspended");
    }

    @Override
    protected void onStop() {
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                final DataMapItem mapItem = DataMapItem.fromDataItem(event.getDataItem());
                final DataMap items = mapItem.getDataMap();
                int tone = items.getInt("tone");
                Log.d(TAG, "tone " + tone);
                audio.setFrequency(tone);
            }
        }
    }

    private float amplitude;
    @Override
    public void onSensorChanged(int sensorType, float value) {
        if (sensorType == Sensor.TYPE_LIGHT) {
            amplitude = 1.0f - (value / 100.0f);
        }
        audio.update(amplitude);
        graph.addEntry(0f, 0f, value);
    }

    @Override
    public void onConnectionFailed(@NonNull final ConnectionResult connectionResult) {
        Log.v(TAG, "connection failed");
    }

}
