// -*- coding: utf-8 -*-
package com.slairium.talktime;

import android.app.Activity;
import android.os.Bundle;
import android.Manifest;

import android.content.pm.PackageManager;

public class MainActivity extends Activity {

	private static final int REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE = 77;

	private AlarmManagerBroadcastReceiver alarm;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		int permissionStatus = MainActivity.this.getApplicationContext()
			.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE);

		if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
			MainActivity.this.requestPermissions(new String[] {
					Manifest.permission.READ_EXTERNAL_STORAGE,
					Manifest.permission.WRITE_EXTERNAL_STORAGE
				}, REQUEST_CODE_PERMISSION_READ_EXTERNAL_STORAGE);
		}

		alarm = new AlarmManagerBroadcastReceiver();
		alarm.say_time(MainActivity.this.getApplicationContext()
			, MainActivity.this);
		alarm.setup_next(MainActivity.this.getApplicationContext());
	}
}
