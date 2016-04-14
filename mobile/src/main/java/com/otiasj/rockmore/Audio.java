package com.otiasj.rockmore;

/**
 * Created by juliensaito on 4/12/16.
 */

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.util.Log;

/**
 * Audio class
 * <p/>
 * Handles an audio thread which plays a defined signal.
 *
 * @author Cyriaque Skrapits
 */
public class Audio {

    public static final float DO = 65.4064f;
    public static final float RE = 73.4162f;
    public static final float MI = 82.4069f;
    public static final float FA = 87.3071f;
    public static final float SOL = 97.9989f;
    public static final float LA = 110;
    public static final float SI = 123.471f;
    public static final float DO2 = 130.813f;

    public static final float[] range= {DO, RE, MI, FA, SOL, LA, SI, DO2};

    private final static int SAMPLE_RATE = 44100;
    private static final String TAG = Audio.class.getCanonicalName();

    private final static int BUFFER_SIZE_2 = 8000;

    private AudioTrack track;
    private Thread thread;
    private float frequency = DO;
    private float amplitude;

    private volatile boolean running;

    public Audio() {
        track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, BUFFER_SIZE_2, AudioTrack.MODE_STREAM);
    }

    public void start() {
        Log.v(TAG, "start");
        running = true;
        track.play();

        // Audio thread
        thread = new Thread(new Runnable() {
            byte[] buffer;

            @Override
            public void run() {
                while (running) {
                    buffer = makeBuffer(frequency);
                    track.write(buffer, 0, buffer.length);
                    track.setVolume(amplitude);
                }
            }
        });
        thread.start();
    }

    private byte[] makeBuffer(float frequency) {
        double duration = 10;                // seconds
        int sampleRate = 8000;              // a number

        double dnumSamples = duration * sampleRate;
        dnumSamples = Math.ceil(dnumSamples);
        int numSamples = (int) dnumSamples;
        double sample[] = new double[numSamples];
        byte generatedSnd[] = new byte[2 * numSamples];

        for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
            sample[i] = Math.sin(frequency * 2 * Math.PI * i / (sampleRate));
        }

        for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
            sample[i] = Math.sin(frequency * 2 * Math.PI * i / (sampleRate));
        }

        int idx = 0;
        int i;

        int ramp = numSamples / 20;                                    // Amplitude ramp as a percent of sample count
        for (i = 0; i < ramp; ++i) {                                     // Ramp amplitude up (to avoid clicks)
            double dVal = sample[i];
            // Ramp up to maximum
            final short val = (short) ((dVal * 32767 * i / ramp));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }


        for (i = i; i < numSamples; ++i) {                        // Max amplitude for most of the samples
            double dVal = sample[i];
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        //for (i = i; i < numSamples; ++i) {                               // Ramp amplitude down
        //    double dVal = sample[i];
        //    // Ramp down to zero
        //    final short val = (short) ((dVal * 32767 * (numSamples - i) / ramp));
        //    // in 16 bit wav PCM, first byte is the low order byte
        //    generatedSnd[idx++] = (byte) (val & 0x00ff);
        //    generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        //}
        return generatedSnd;
    }

    /**
     * Update the frequency and the amplitude.
     *
     * @param amplitude Signal amplitude
     */
    public void update(float amplitude) {
        this.amplitude = amplitude;
    }

    public void play() {
        byte[] buffer = makeBuffer(frequency);
        track.write(buffer, 0, buffer.length);
    }

    /**
     * Stops the signal.
     */
    public void stop() {
        System.out.println("Audio::stop()");

        running = false; // Stop the thread.
        track.stop(); // Stop writing to the audio track.
    }

    public float setFrequency(int tone) {
        frequency = range[tone];
        return frequency;
    }

    // http://stackoverflow.com/questions/2413426/playing-an-arbitrary-tone-with-android
    boolean playNote() {
        double duration = 1;                // seconds
        double freqOfTone = 1000;           // hz
        int sampleRate = 8000;              // a number

        double dnumSamples = duration * sampleRate;
        dnumSamples = Math.ceil(dnumSamples);
        int numSamples = (int) dnumSamples;
        double sample[] = new double[numSamples];
        byte generatedSnd[] = new byte[2 * numSamples];


        for (int i = 0; i < numSamples; ++i) {      // Fill the sample array
            sample[i] = Math.sin(freqOfTone * 2 * Math.PI * i / (sampleRate));
        }

        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalized.
        // convert to 16 bit pcm sound array
        // assumes the sample buffer is normalised.
        int idx = 0;
        int i = 0;

        int ramp = numSamples / 20;                                    // Amplitude ramp as a percent of sample count


        for (i = 0; i < ramp; ++i) {                                     // Ramp amplitude up (to avoid clicks)
            double dVal = sample[i];
            // Ramp up to maximum
            final short val = (short) ((dVal * 32767 * i / ramp));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }


        for (i = i; i < numSamples - ramp; ++i) {                        // Max amplitude for most of the samples
            double dVal = sample[i];
            // scale to maximum amplitude
            final short val = (short) ((dVal * 32767));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        for (i = i; i < numSamples; ++i) {                               // Ramp amplitude down
            double dVal = sample[i];
            // Ramp down to zero
            final short val = (short) ((dVal * 32767 * (numSamples - i) / ramp));
            // in 16 bit wav PCM, first byte is the low order byte
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }

        AudioTrack audioTrack = null;                                   // Get audio track
        try {
            audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
                    sampleRate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, (int) numSamples * 2,
                    AudioTrack.MODE_STATIC);
            audioTrack.write(generatedSnd, 0, generatedSnd.length);     // Load the track
            audioTrack.play();                                          // Play the track
        } catch (Exception e) {
            return false;
        }

        int x = 0;
        do {                                                     // Montior playback to find when done
            if (audioTrack != null)
                x = audioTrack.getPlaybackHeadPosition();
            else
                x = numSamples;
        } while (x < numSamples);

        if (audioTrack != null) audioTrack.release();
        return false;
    }
}
