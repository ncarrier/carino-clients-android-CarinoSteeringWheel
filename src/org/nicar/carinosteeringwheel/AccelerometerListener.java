package org.nicar.carinosteeringwheel;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;
import android.widget.ProgressBar;

public class AccelerometerListener extends Thread implements
		SensorEventListener {
	private float gravity[];
	private CarinoListener carinoSelector;
	private ProgressBar bar1;
	private ProgressBar bar2;
	private ProgressBar bar3;

	private final static double UI_REFRESH_TIME = 50.;
	public static final String TAG = "CarinoSteeringWheel";

	private long previous_ui_refresh_timestamp = 0;

	private SensorManager mSensorManager;

	private Sensor mAccelerometer;

	private Object mLock;
	private boolean mRegister;
	private boolean mRunning;

	public AccelerometerListener(CarinoListener carinoSelector,
			ProgressBar bar1, ProgressBar bar2, ProgressBar bar3,
			SensorManager sensorManager) {
		super();

		this.carinoSelector = carinoSelector;
		this.bar1 = bar1;
		this.bar2 = bar2;
		this.bar3 = bar3;
		gravity = new float[3];
		mSensorManager = sensorManager;
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mLock = new Object();
		this.mRegister = false;
		this.mRunning = true;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float alpha = 0.8f;
		float norm;
		float x;
		float y;
		float z;
		long ts;
		int max = bar1.getMax() / 2;

		ts = System.currentTimeMillis();

		/* lowpass filter the gravity vector to filter sudden movements */
		gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

		/* normalize the gravity vector */
		norm = (float) Math.sqrt(Math.pow(gravity[0], 2)
				+ Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));
		x = gravity[0] / norm;
		y = gravity[1] / norm;
		z = gravity[2] / norm;

		if (ts - previous_ui_refresh_timestamp > UI_REFRESH_TIME) {
			previous_ui_refresh_timestamp = ts;
			bar1.setProgress((int) ((x + 1f) * max));
			bar2.setProgress((int) ((y + 1f) * max));
			bar3.setProgress((int) ((z + 1f) * max));
		}

		// Log.d(TAG, "direction = " + y + ", speed = " + y + "\n");

		this.carinoSelector.setDirectionAndSpeed(2 * y, 3 * z - 2);
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		/* nothing to do */
	}

	@Override
	public void run() { 
		synchronized (mLock) {
			do {
				if (this.mRegister) {
					Log.d(TAG, "register");
					mSensorManager.registerListener(this, mAccelerometer,
							SensorManager.SENSOR_DELAY_GAME);
				} else {
					Log.d(TAG, "unregister");
					mSensorManager.unregisterListener(this);
				}
				try {
					this.mLock.wait();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} while (this.mRunning);
		}
	}

	public void register() {
		synchronized (mLock) {
			this.mRegister = true;
			mLock.notify();
		}
	}

	public void unregister() {
		synchronized (mLock) {
			this.mRegister = false;
			mLock.notify();
		}
	}

	public void end() {
		synchronized (mLock) {
			this.mRunning = false;
			mLock.notify();
		}
	}
}
