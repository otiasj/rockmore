package com.otiasj.rockmore;

/**
 * Created by juliensaito on 4/12/16.
 */
public interface SensorListener {
    void onSensorChanged(final int typeMagneticField, final float value);
}
