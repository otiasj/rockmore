package com.otiasj.rockmore;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.preference.PreferenceManager;

/**
 * Created by juliensaito on 4/12/16.
 */
public class Sensors {
    private static SensorListener sensorsListener;
    private static SensorManager sensorManager;
    private static SensorEventListener eventListener;
    private static Sensor light;
    //private static Sensor magnetic;
    private static float calibrateForce = 0.0f;
    private static float calibrateLight = 0.0f;

    public Sensors(Context context, SensorListener listener) {
        // Check if the luminosity sensor has been chosen.
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        //boolean useLuminositySensor = settings.getBoolean("sensors_luminosity", false);

        sensorsListener = listener;
        sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        //magnetic = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        light = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);//useLuminositySensor ? Sensor.TYPE_LIGHT : Sensor.TYPE_PROXIMITY);

        initListener();
    }

    public void start() {
        //sensorManager.registerListener(eventListener, magnetic, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(eventListener, light, SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stop() {
        sensorManager.unregisterListener(eventListener);
    }


    /**
     * Starts the sensors listener.
     */
    private void initListener() {
        eventListener = new SensorEventListener() {

            @Override
            public void onSensorChanged(SensorEvent event) {
                calibrate(event);
            }

            private void calibrate(SensorEvent event) {
                float x, y, z;

                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                    x = Math.abs(event.values[0]);
                    y = Math.abs(event.values[1]);
                    z = Math.abs(event.values[2]);

                    // Magnetic field force formula.
                    float force = (float) Math.sqrt((x * x) + (y * y) + (z * z));

                    if (calibrateForce == 0.0f) // If calibrateForce = 0 --> get initial value
                        calibrateForce = force;

                    float value = getCalibrateValue(force, true);
                    sensorsListener.onSensorChanged(Sensor.TYPE_MAGNETIC_FIELD, value);
                }
                if (event.sensor.getType() == Sensor.TYPE_LIGHT) {
                    x = event.values[0];

                    if (calibrateLight == 0.0f) // If calibrateLight = 0 --> get initial value
                        calibrateLight = x;

                    float value = getCalibrateValue(x, false);
                    sensorsListener.onSensorChanged(Sensor.TYPE_LIGHT, value);
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // TODO Auto-generated method stub

            }
        };
    }

    /**
     * By reseting these variables the event 'onSensorChanged' will get
     * the initial value of the sensors at the moment.
     */
    public void resetSensors() {
        //
        calibrateLight = 0.0f;
        calibrateForce = 0.0f;
    }


    private static float getCalibrateValue(float sensorValue, boolean bool) {
        float value;

        // If bool is true, magnetic sensor.
        if (bool) {
            // Calibrate the magnetic sensor.
            value = 103.0f - (sensorValue / calibrateForce) * 3.5f;

        } else {
            // Calibrate the light sensor.
            value = (sensorValue / calibrateLight) * 100.0f;
        }

        // Values must be between 0 and 100.
        if (value > 100.0f)
            return 100.0f;
        if (value <= 0.0f)
            return 0.0f;
        else
            return value;
    }
}