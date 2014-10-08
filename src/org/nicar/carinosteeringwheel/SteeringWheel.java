package org.nicar.carinosteeringwheel;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.nicar.carinosteeringwheel.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ProgressBar;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class SteeringWheel extends Activity implements SensorEventListener {
	public static final String TAG = "CarinoSteeringWheel";

	/**
	 * Whether or not the system UI should be auto-hidden after
	 * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
	 */
	private static final boolean AUTO_HIDE = true;

	/**
	 * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
	 * user interaction before hiding the system UI.
	 */
	private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

	/**
	 * If set, will toggle the system UI visibility upon interaction. Otherwise,
	 * will show the system UI visibility upon interaction.
	 */
	private static final boolean TOGGLE_ON_CLICK = true;

	/**
	 * The flags to pass to {@link SystemUiHider#getInstance}.
	 */
	private static final int HIDER_FLAGS = SystemUiHider.FLAG_HIDE_NAVIGATION;

	/**
	 * The instance of the {@link SystemUiHider} for this activity.
	 */
	private SystemUiHider mSystemUiHider;

	private Sensor mAccelerometer;
	private SensorManager mSensorManager;
	private float gravity[];

	private CarinoListener carinoSelector;

	protected static String readGateway() throws IOException {
		Process process = new ProcessBuilder().command("/system/bin/getprop")
				.redirectErrorStream(true).start();
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					process.getInputStream()));

			String line;
			while ((line = in.readLine()) != null) {
				String property[] = line.replaceAll("\\Q[\\E|\\Q]\\E", "")
						.split(":");
				if (property[0].matches("net\\Q.\\E.*\\Q.\\Egw"))
					return property[1];
			}
		} finally {
			process.destroy();
		}

		return null;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		gravity = new float[3];

		setContentView(R.layout.activity_steering_wheel);

		final View controlsView = findViewById(R.id.fullscreen_content_controls);

		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager
				.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

		/*
		 * TODO if the car is the access point, we could use the gateway as the
		 * car's address -> rather use DHCP information
		 */
		try {
			Log.e(TAG, "gateway is " + SteeringWheel.readGateway());
		} catch (IOException e3) {
			// TODO Auto-generated catch block
			e3.printStackTrace();
		}

		// Set up an instance of SystemUiHider to control the system UI for
		// this activity.
		mSystemUiHider = SystemUiHider.getInstance(this, controlsView,
				HIDER_FLAGS);
		mSystemUiHider.setup();
		mSystemUiHider
				.setOnVisibilityChangeListener(new SystemUiHider.OnVisibilityChangeListener() {
					// Cached values.
					int mControlsHeight;
					int mShortAnimTime;

					@Override
					@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
					public void onVisibilityChange(boolean visible) {
						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
							// If the ViewPropertyAnimator API is available
							// (Honeycomb MR2 and later), use it to animate the
							// in-layout UI controls at the bottom of the
							// screen.
							if (mControlsHeight == 0) {
								mControlsHeight = controlsView.getHeight();
							}
							if (mShortAnimTime == 0) {
								mShortAnimTime = getResources().getInteger(
										android.R.integer.config_shortAnimTime);
							}
							controlsView
									.animate()
									.translationY(visible ? 0 : mControlsHeight)
									.setDuration(mShortAnimTime);
						} else {
							// If the ViewPropertyAnimator APIs aren't
							// available, simply show or hide the in-layout UI
							// controls.
							controlsView.setVisibility(visible ? View.VISIBLE
									: View.GONE);
						}

						if (visible && AUTO_HIDE) {
							// Schedule a hide().
							delayedHide(AUTO_HIDE_DELAY_MILLIS);
						}
					}
				});

		// Set up the user interaction to manually show or hide the system UI.
		controlsView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if (TOGGLE_ON_CLICK) {
					mSystemUiHider.toggle();
				} else {
					mSystemUiHider.show();
				}
			}
		});

		// Upon interacting with UI controls, delay any scheduled hide()
		// operations to prevent the jarring behavior of controls going away
		// while interacting with the UI.
		findViewById(R.id.dummy_button).setOnTouchListener(
				mDelayHideTouchListener);

		carinoSelector = new CarinoListener();

		carinoSelector.start();
	}

	@Override
	protected void onResume() {
		super.onResume();

		mSensorManager.registerListener(this, mAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);
	}

	@Override
	protected void onPause() {
		super.onPause();

		mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

		// Trigger the initial hide() shortly after the activity has been
		// created, to briefly hint to the user that UI controls
		// are available.
		delayedHide(100);
	}

	/**
	 * Touch listener to use for in-layout UI controls to delay hiding the
	 * system UI. This is to prevent the jarring behavior of controls going away
	 * while interacting with activity UI.
	 */
	View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
		@Override
		public boolean onTouch(View view, MotionEvent motionEvent) {
			if (AUTO_HIDE) {
				delayedHide(AUTO_HIDE_DELAY_MILLIS);
			}
			return false;
		}
	};

	Handler mHideHandler = new Handler();
	Runnable mHideRunnable = new Runnable() {
		@Override
		public void run() {
			mSystemUiHider.hide();
		}
	};

	/**
	 * Schedules a call to hide() in [delay] milliseconds, canceling any
	 * previously scheduled calls.
	 */
	private void delayedHide(int delayMillis) {
		mHideHandler.removeCallbacks(mHideRunnable);
		mHideHandler.postDelayed(mHideRunnable, delayMillis);
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

		ProgressBar bar1 = (ProgressBar) findViewById(R.id.progressBar1);
		ProgressBar bar2 = (ProgressBar) findViewById(R.id.progressBar2);
		ProgressBar bar3 = (ProgressBar) findViewById(R.id.progressBar3);

		bar1.setProgress((int) x);
		bar2.setProgress((int) y);
		bar3.setProgress((int) z);

		msgString = "x = " + x + ", y = " + y + ", z = " + z + "\n";
		this.carinoSelector.postMessage(msgString.getBytes());
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub

	}
}
