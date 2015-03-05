/*
 * This file is part of CarinoSteeringWheel.
 *
 *  CarinoSteeringWheel is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  CarinoSteeringWheel is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CarinoSteeringWheel.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 Nicolas CARRIER <carrier dot nicolas0 at gmail dot com>
 */
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
