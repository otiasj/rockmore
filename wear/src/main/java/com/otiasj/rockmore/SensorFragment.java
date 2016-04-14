package com.otiasj.rockmore;

import android.app.Fragment;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataMap;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SensorFragment extends Fragment implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 1.1f;
    private static final int SHAKE_WAIT_TIME_MS = 250;
    private static final float ROTATION_THRESHOLD = 2.0f;
    private static final int ROTATION_WAIT_TIME_MS = 100;
    private static final String TAG = SensorFragment.class.getCanonicalName();
    private static final float NS2S = 1.0f / 1000000000.0f;
    private static final int IGNORE_FLIPS_AFTER_FLIP = 2500;
    private static final int SAMPLING_INTERVAL = 200;//60;
    private static final int MINIMAL_STACK_SIZE_TO_FLIP = 2; // Shouldn't be lower than 2
    private static final float FLIP_RADIANS = (float) Math.toRadians(140);
    private static final int STACK_MAX_SIZE = 38;
    private static final String[] toneRange = {"DO 1", "RE", "MI", "FA", "SOL", "LA", "SI", "DO 2"};
    private final float[] deltaRotationVector = new float[4];
    private View mView;
    private TextView mTextTitle;
    private TextView mTextValues;
    private SensorManager mSensorManager;
    private Sensor mSensor;
    private int mSensorType;
    private long mShakeTime = 0;
    private long mRotationTime = 0;
    private GoogleApiClient googleClient;
    private float timestamp;
    private List<Float> stack1 = new ArrayList<Float>();
    private long lastAdd = 0;
    private long lastFlip = 0;
    private int currentTone = 4;

    public static SensorFragment newInstance(int sensorType) {
        SensorFragment f = new SensorFragment();

        // Supply sensorType as an argument
        Bundle args = new Bundle();
        args.putInt("sensorType", sensorType);
        f.setArguments(args);

        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mSensorType = args.getInt("sensorType");
        }

        mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(mSensorType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        mView = inflater.inflate(R.layout.sensor, container, false);

        mTextTitle = (TextView) mView.findViewById(R.id.text_title);
        //mTextTitle.setText();
        mTextValues = (TextView) mView.findViewById(R.id.text_values);

        return mView;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_GAME);
        googleClient = ((WearMainActivity) getActivity()).getGoogleClient();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        googleClient = null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // If sensor is unreliable, then just return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            return;
        }

        //mTextValues.setText(
        //        "x = " + Float.toString(event.values[0]) + "\n" +
        //                "y = " + Float.toString(event.values[1]) + "\n" +
        //                "z = " + Float.toString(event.values[2]) + "\n"
        //);

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            detectShake(event);
            mTextValues.setText("tilt forward\nor back\nto change tone > ");
        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            detectRotation(event);
            rotationRateAroundXChanged((float) event.values[0]);
            mTextValues.setText(toneRange[currentTone]);
            mTextValues.setTextSize(20);
        }
    }

    private void sendEvent(final int eventType) {
        String WEARABLE_DATA_PATH = "/wearable_data";
        DataMap dataMap = new DataMap();
        dataMap.putLong("time", new Date().getTime());
        dataMap.putInt("tone", eventType);

        if (googleClient != null) {
            new SendToDataLayerThread(googleClient, WEARABLE_DATA_PATH, dataMap).start();
        }
    }

    private void sendMessage(final SensorEvent event) {
        String WEARABLE_DATA_PATH = "/wearable_data";
        DataMap dataMap = new DataMap();
        dataMap.putLong("time", new Date().getTime());
        dataMap.putFloat("x", event.values[0]);
        dataMap.putFloat("y", event.values[1]);
        dataMap.putFloat("z", event.values[2]);

        if (googleClient != null) {
            new SendToDataLayerThread(googleClient, WEARABLE_DATA_PATH, dataMap).start();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // References:
    //  - http://jasonmcreynolds.com/?p=388
    //  - http://code.tutsplus.com/tutorials/using-the-accelerometer-on-android--mobile-22125
    private void detectShake(SensorEvent event) {
        long now = System.currentTimeMillis();

        if ((now - mShakeTime) > SHAKE_WAIT_TIME_MS) {
            mShakeTime = now;

            float gX = event.values[0] / SensorManager.GRAVITY_EARTH;
            float gY = event.values[1] / SensorManager.GRAVITY_EARTH;
            float gZ = event.values[2] / SensorManager.GRAVITY_EARTH;

            // gForce will be close to 1 when there is no movement
            double gForce = Math.sqrt(gX * gX + gY * gY + gZ * gZ);

            // Change background color if gForce exceeds threshold;
            // otherwise, reset the color
            if (gForce > SHAKE_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
                currentTone = 0;
            } else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }

    private void rotationRateAroundXChanged(float rotationRateAroundX) {
        long currentTime = System.currentTimeMillis();

        if (lastFlip != 0 && (currentTime - lastFlip) < IGNORE_FLIPS_AFTER_FLIP) {
            return;
        }

        if ((currentTime - lastAdd) >= SAMPLING_INTERVAL) {
            if (Math.abs(rotationRateAroundX) > 0.3) { // Smaller values are unimportant. They can make only mess.
                addToStack(rotationRateAroundX);
                updateAngle();
            }
        }
    }

    private void updateAngle() {
        int stackSize = stack1.size();
        if (stackSize < MINIMAL_STACK_SIZE_TO_FLIP) return;
        float approximateAngleSummary = 0;
        float val;

        for (int i = 0; i < stackSize; i++) {
            //val = Math.abs(stack1.get(i).floatValue());
            val = stack1.get(i).floatValue();
            // "+ Math.pow(val/4.58, 2) )" don't have a sense. Simply it works better with it.
            approximateAngleSummary += ((val + Math.pow(val / 4.58, 2)) / 1000) * SAMPLING_INTERVAL;

            if (approximateAngleSummary >= FLIP_RADIANS) {
                triggerFlipDetected();
                clearStack();
                return;
            }
        }

        //int tone = (int) (approximateAngleSummary * 10);
        if (approximateAngleSummary > 0) {
            currentTone++;
        } else {
            currentTone--;
        }
        if (currentTone >= 8) {
            currentTone = 7;
        }
        if (currentTone < 0) {
            currentTone = 0;
        }
        Log.e(TAG, "ANGLE = " + approximateAngleSummary + " -> current " + currentTone);
        sendEvent(currentTone);
    }

    private void clearStack() {
        stack1.clear();
    }

    private void addToStack(float val) {
        lastAdd = System.currentTimeMillis();
        int stackSize = stack1.size();
        if (stackSize > 0 && ((stack1.get(stackSize - 1) > 0 ? 1 : -1) != (val > 0 ? 1 : -1) || stackSize > STACK_MAX_SIZE)) {
            clearStack();
        }
        stack1.add(val);
    }

    private void triggerFlipDetected() {
        lastFlip = System.currentTimeMillis();
        Log.e(TAG, "Flip!");
    }

    private void detectRotation(SensorEvent event) {
        long now = System.currentTimeMillis();
        if ((now - mRotationTime) > ROTATION_WAIT_TIME_MS) {
            mRotationTime = now;

            // Change background color if rate of rotation around any
            // axis and in any direction exceeds threshold;
            // otherwise, reset the color
            if (Math.abs(event.values[0]) > ROTATION_THRESHOLD ||
                    Math.abs(event.values[1]) > ROTATION_THRESHOLD ||
                    Math.abs(event.values[2]) > ROTATION_THRESHOLD) {
                mView.setBackgroundColor(Color.rgb(0, 100, 0));
                currentTone = 7;
            } else {
                mView.setBackgroundColor(Color.BLACK);
            }
        }
    }
}
