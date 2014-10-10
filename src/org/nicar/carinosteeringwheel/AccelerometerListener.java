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
		float alpha = (float) 0.8;
		float norm;
		int x;
		int y;
		int z;
		String msgString;
		long ts;

		ts = System.currentTimeMillis();

		/* lowpass filter the gravity vector to filter.udden movements */
		gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
		gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
		gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

		/*
		 * normalize the gravity vector and rescale it so that every component
		 * fits the range of it's progress bar.
		 */
		norm = (float) Math.sqrt(Math.pow(gravity[0], 2)
				+ Math.pow(gravity[1], 2) + Math.pow(gravity[2], 2));
		double max = bar1.getMax() / 2;
		double k = max / norm;
		x = (int) (k * gravity[0] + max);
		y = (int) (k * gravity[1] + max);
		z = (int) (k * gravity[2] + max);

		if (ts - previous_ui_refresh_timestamp > UI_REFRESH_TIME) {
			previous_ui_refresh_timestamp = ts;
			bar1.setProgress(x);
			bar2.setProgress(y);
			bar3.setProgress(z);
		}

		msgString = "x = " + x + ", y = " + y + ", z = " + z + "\n";
		this.carinoSelector.postMessage(msgString.getBytes());
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
							SensorManager.SENSOR_DELAY_UI);
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
