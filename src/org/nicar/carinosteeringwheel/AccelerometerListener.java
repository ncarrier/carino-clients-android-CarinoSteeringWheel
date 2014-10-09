package org.nicar.carinosteeringwheel;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.widget.ProgressBar;

public class AccelerometerListener implements SensorEventListener {
	private static final String TAG = SteeringWheel.TAG;

	private float gravity[];
	private CarinoListener carinoSelector;
	private ProgressBar bar1;
	private ProgressBar bar2;
	private ProgressBar bar3;

	public AccelerometerListener(CarinoListener carinoSelector,
			ProgressBar bar1, ProgressBar bar2, ProgressBar bar3) {
		this.carinoSelector = carinoSelector;
		this.bar1 = bar1;
		this.bar2 = bar2;
		this.bar3 = bar3;
		gravity = new float[3];
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		float alpha = (float) 0.8;
		float norm;
		byte x;
		byte y;
		byte z;
		String msgString;

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
		x = (byte) (50 * (gravity[0] / norm) + 50);
		y = (byte) (50 * (gravity[1] / norm) + 50);
		z = (byte) (50 * (gravity[2] / norm) + 50);

		bar1.setProgress((int) x);
		bar2.setProgress((int) y);
		bar3.setProgress((int) z);

		msgString = "x = " + x + ", y = " + y + ", z = " + z + "\n";
		this.carinoSelector.postMessage(msgString.getBytes());
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		/* nothing to do */
	}

}
