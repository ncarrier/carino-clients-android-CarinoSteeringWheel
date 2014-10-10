package org.nicar.carinosteeringwheel;

import android.app.Activity;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

public class SteeringWheel extends Activity {
	public static final String TAG = "CarinoSteeringWheel";

	private CarinoListener carinoSelector;
	private AccelerometerListener accelerometerListener;
	private static final int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN;
	private View decorView;
	private final static long UI_RESTORE_DELAY = 3000;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		View.OnSystemUiVisibilityChangeListener visChangeListener;

		super.onCreate(savedInstanceState);

		Log.d(TAG, "CarinoSteeringWheel onCreate");

		setContentView(R.layout.activity_steering_wheel);

		decorView = getWindow().getDecorView();

		carinoSelector = new CarinoListener();
		carinoSelector.start();
		carinoSelector.setPriority(Thread.NORM_PRIORITY - 1);
		accelerometerListener = new AccelerometerListener(carinoSelector,
				(ProgressBar) findViewById(R.id.progressBar1),
				(ProgressBar) findViewById(R.id.progressBar2),
				(ProgressBar) findViewById(R.id.progressBar3),
				(SensorManager) getSystemService(SENSOR_SERVICE));
		accelerometerListener.setPriority(Thread.NORM_PRIORITY - 1);
		accelerometerListener.start();

		visChangeListener = new View.OnSystemUiVisibilityChangeListener() {
			private Runnable visibilityRestorer = new Runnable() {
				public void run() {
					decorView.setSystemUiVisibility(uiOptions);
				}
			};

			@Override
			public void onSystemUiVisibilityChange(int visibility) {
				if ((uiOptions & visibility) != uiOptions)
					decorView.postDelayed(visibilityRestorer, UI_RESTORE_DELAY);
			}
		};
		decorView.setOnSystemUiVisibilityChangeListener(visChangeListener);
	}

	@Override
	protected void onResume() {
		super.onResume();

		decorView.setSystemUiVisibility(uiOptions);
		accelerometerListener.register();
	}

	@Override
	protected void onPause() {
		super.onPause();

		accelerometerListener.unregister();
	}
}
