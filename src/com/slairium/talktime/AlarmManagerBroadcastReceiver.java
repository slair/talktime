package com.slairium.talktime;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.Random;

import fc.cron.CronExpression;

//~ import android.Manifest;
//~ import android.content.pm.PackageManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.AudioAttributes;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class AlarmManagerBroadcastReceiver extends BroadcastReceiver {

	private static final int MORNING_START	= 3;
	private static final int DAY_START		= 9;
	private static final int EVENING_START	= 18;
	private static final int NIGHT_START		= 22;
	private static final String SND_FOLDER	= "/phonedata/sndtime/";

	private static final String CRONTAB_FOLDER	= "/phonedata/";
	private static final String FN_CRONTAB		= "crontab.txt";
	private static final String FN_SETTINGS		= "settings.txt";

	private boolean use_NOTIFY = false;
	private int SetAlarmMode = 2;
	// 1 - setExactAndAllowWhileIdle
	// 2 - setAlarmClock

	private static MainActivity mact;

	NotificationManager nm;
	AudioManager audioManager;

	TimeZone original = TimeZone.getDefault();
	ZoneId zoneId = TimeZone.getDefault().toZoneId();

	String sdcard_folder = "/storage/emulated/0";
	//~ String sdcard_folder = "/storage/8057-1BE9";

	@Override
	public void onReceive(Context context, Intent intent) {
		PowerManager pm = (PowerManager) context.getSystemService(
			Context.POWER_SERVICE);

		//~ nm = (NotificationManager) context.getSystemService(
			//~ Context.NOTIFICATION_SERVICE);

		PowerManager.WakeLock wl = pm.newWakeLock(
			PowerManager.PARTIAL_WAKE_LOCK, "YOUR TAG");
		//Acquire the lock
		wl.acquire();

		say_time(context, null);
		setup_next(context);

		//Release the lock
		wl.release();
	}

	public void _setExactAndAllowWhileIdle(Context context, long start_time) {
		AlarmManager am = (AlarmManager) context.getSystemService(
			Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, start_time, pi);
	}

	public void _setAlarmClock(Context context, long start_time){
		AlarmManager am = (AlarmManager) context.getSystemService(
			Context.ALARM_SERVICE);
		Intent intent = new Intent(context, AlarmManagerBroadcastReceiver.class);
		PendingIntent pi = PendingIntent.getBroadcast(context, 0, intent, 0);
		am.setAlarmClock(new AlarmManager.AlarmClockInfo(start_time, pi), pi);
	}

	public void mylog(String msg) {
		Log.d("SLAIRIUM", msg);
	}

	public void play_file(MediaPlayer mp, String file_path) {
		mylog("Playing: " + file_path);
		while (mp.isPlaying()) {
			SystemClock.sleep(100);
		}
		mp.reset();
		try {
			mp.setDataSource(file_path);
		} catch (IllegalArgumentException e) {
			mylog("Exception: " + Log.getStackTraceString(e));
		} catch (SecurityException e) {
			mylog("Exception: " + Log.getStackTraceString(e));
		} catch (IllegalStateException e) {
			mylog("Exception: " + Log.getStackTraceString(e));
		} catch (IOException e) {
			mylog("Exception: " + Log.getStackTraceString(e));
		}
		try {
			mp.prepare();
		} catch (IllegalStateException e) {
			mylog("Exception: " + Log.getStackTraceString(e));
		} catch (IOException e) {
			mylog("Exception: " + Log.getStackTraceString(e));
		}
		mp.start();
	}

	public boolean find_snd(List<String> sndfiles, String sndname
		, String sndext) {

		for (int i = 0; i < sndfiles.size(); i++) {
			String snd = sndfiles.get(i);
			if (snd.startsWith(sndname) && snd.endsWith(sndext)) {
				return true;
			}
		}
		mylog("Not found! sndname=" + sndname + ", sndext=" + sndext);
		return false;
	}

	public List<String> get_snd_startswith(List<String> sndfiles, String sw) {
		List<String> res = new ArrayList<String>();
		for (int i = 0; i < sndfiles.size(); i++) {
			String snd = sndfiles.get(i);
			if (snd.startsWith(sw)) {
				res.add(snd);
			}
		}
		return res;
	}

	private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener =
    new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    // Фокус предоставлен.
                    // Например, был входящий звонок и фокус у нас отняли.
                    // Звонок закончился, фокус выдали опять
                    // и мы продолжили воспроизведение.
                    //~ mediaSessionCallback.onPlay();
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    // Фокус отняли, потому что какому-то приложению надо
                    // коротко "крякнуть".
                    // Например, проиграть звук уведомления или навигатору сказать
                    // "Через 50 метров поворот направо".
                    // В этой ситуации нам разрешено не останавливать вопроизведение,
                    // но надо снизить громкость.
                    // Приложение не обязано именно снижать громкость,
                    // можно встать на паузу, что мы здесь и делаем.
                    //~ mediaSessionCallback.onPause();
                    break;
                default:
                    // Фокус совсем отняли.
                    //~ mediaSessionCallback.onPause();
                    break;
            }
        }
    };

	public void say_time(Context context, MainActivity ma) {

		if (ma != null) {
			mact = ma;
		}
		mylog("Started.");
		mylog("sdcard_folder = " + sdcard_folder);
		Calendar c = Calendar.getInstance();
		int day_of_week = c.get(Calendar.DAY_OF_WEEK);
		int hour_of_day = c.get(Calendar.HOUR_OF_DAY);
		int minute = c.get(Calendar.MINUTE);
		int seconds = c.get(Calendar.SECOND);

		String hour_of_day_string, minute_string, seconds_string;
		if (hour_of_day<10) {
			hour_of_day_string = "0" + hour_of_day;
		} else {
			hour_of_day_string = "" + hour_of_day;
		}
		if (minute<10) {
			minute_string = "0" + minute;
		} else {
			minute_string = "" + minute;
		}
		if (seconds<10) {
			seconds_string = "0" + seconds;
		} else {
			seconds_string = "" + seconds;
		}

		String bn;
		if (hour_of_day >= NIGHT_START || hour_of_day <= MORNING_START) {
			bn = "bn-night";
		} else if (hour_of_day >= MORNING_START && hour_of_day < DAY_START) {
			bn = "bn-morning";
		} else if (hour_of_day >= DAY_START && hour_of_day < EVENING_START) {
			bn = "bn-day";
		} else if (hour_of_day >= EVENING_START && hour_of_day < NIGHT_START) {
			bn = "bn-evening";
		} else {
			bn = "not-found";
		}

		String my_files_folder = sdcard_folder + SND_FOLDER;

		File[] files;

		List<String> sndfiles = new ArrayList<String>();

		File file = new File(my_files_folder);
		if (file.isDirectory()) {
			files = file.listFiles();
			for (File f : files) {
				if (f.isFile()) {
					try {
						String lcname = f.getName().toLowerCase();
						if (lcname.endsWith(".ogg") || lcname.endsWith(".mp3")) {
							sndfiles.add(f.getName());
						}
					} catch(Exception e){
						mylog("Exception: " + Log.getStackTraceString(e));
					}
				}
			}
		} else {
			mylog("ERROR!!! No sound files found in '" + my_files_folder + "'!");
		}

		MediaPlayer mp = new MediaPlayer();

		nm = (NotificationManager)
			context.getSystemService(Context.NOTIFICATION_SERVICE);
		audioManager = (AudioManager)
			context.getSystemService(Context.AUDIO_SERVICE);

		int audioFocusResult = audioManager.requestAudioFocus(
			audioFocusChangeListener, AudioManager.STREAM_MUSIC,
			AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
			//~ AudioManager.AUDIOFOCUS_GAIN);
			//~ AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

		mylog("audioFocusResult = " + audioFocusResult);
		if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
			return;

		mylog("use_NOTIFY = " + use_NOTIFY);
		if (use_NOTIFY) {
			mylog("CREATING NOTIFICATION");
			String NOTIFICATION_CHANNEL_ID = "TT_CH";
			NotificationChannel notificationChannel = new NotificationChannel(
				NOTIFICATION_CHANNEL_ID, "My Notifications"
				, NotificationManager.IMPORTANCE_DEFAULT);
			// Configure the notification channel.
			notificationChannel.setDescription("Channel description");
			notificationChannel.enableLights(true);
			//~ notificationChannel.setLightColor(Color.RED);
			//~ notificationChannel.setVibrationPattern(
				//~ new long[]{0, 1000, 500, 1000});
			//~ notificationChannel.enableVibration(true);
			nm.createNotificationChannel(notificationChannel);

			Notification notif = new Notification.Builder(context
				, NOTIFICATION_CHANNEL_ID)
				.setContentTitle("ContentTitle")
				.setContentText("ContentText")
				.setSmallIcon(R.drawable.app_icon)
				//~ .setLargeIcon(aBitmap)
				.build();

			//~ Notification notif = new Notification(R.drawable.app_icon
				//~ , "Text in status bar", System.currentTimeMillis());
			Intent intent = new Intent(mact, MainActivity.class);
			//~ intent.putExtra(MainActivity.FILE_NAME, "somefile");
			PendingIntent pIntent = PendingIntent.getActivity(mact, 0,
				intent, 0);
			//~ notif.setLatestEventInfo(mact, "Notification's title"
				//~ , "Notification's text", pIntent);
			notif.flags |= Notification.FLAG_AUTO_CANCEL;
			nm.notify(1, notif);
		}

		//~ mp.setAudioStreamType(AudioManager.STREAM_RING);

		//~ CONTENT_TYPE_MOVIE
		//~ CONTENT_TYPE_SONIFICATION
		//~ CONTENT_TYPE_SPEECH
		//~ CONTENT_TYPE_UNKNOWN
		//~ USAGE_ALARM
		//~ USAGE_ASSISTANCE_ACCESSIBILITY
		//~ USAGE_ASSISTANCE_NAVIGATION_GUIDANCE
		//~ USAGE_ASSISTANCE_SONIFICATION
		//~ USAGE_ASSISTANT
		//~ USAGE_GAME
		//~ USAGE_MEDIA
		//~ USAGE_NOTIFICATION
		//~ USAGE_NOTIFICATION_COMMUNICATION_DELAYED
		//~ USAGE_NOTIFICATION_COMMUNICATION_INSTANT
		//~ USAGE_NOTIFICATION_COMMUNICATION_REQUEST
		//~ USAGE_NOTIFICATION_EVENT
		//~ USAGE_NOTIFICATION_RINGTONE
		//~ USAGE_UNKNOWN
		//~ USAGE_VOICE_COMMUNICATION
		//~ USAGE_VOICE_COMMUNICATION_SIGNALLING
		AudioAttributes aa1 = new AudioAttributes.Builder()
			.setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
			.setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
			.build();
		mp.setAudioAttributes(aa1);

		String sndext;
		Random rnd = new Random();
		//~ int i = rnd.nextInt(100);
		//~ mylog("i=" + i);
		//~ if (i > 50) {
			//~ sndext = ".mp3";
		//~ } else {
			sndext = ".ogg";
		//~ }

		List<String> play_list;

		mylog("Need to play : " + my_files_folder + bn + ".*");
		play_list = get_snd_startswith(sndfiles, bn);
		if (play_list.size() > 0) {
			mylog("Found " + play_list.size() + " items.");
			int idx = rnd.nextInt(play_list.size());
			String play_file = play_list.get(idx);
			play_file(mp, my_files_folder + play_file);
		} else {
			mylog("!!! Not found : " + my_files_folder + bn + ".*");
		}
		play_list.clear();

		mylog("Need to play : " + my_files_folder + hour_of_day_string
			+ minute_string + ".*");
		play_list = get_snd_startswith(sndfiles, hour_of_day_string
			+ minute_string + ".");
		if (play_list.size() > 0) {
			mylog("Found " + play_list.size() + " items.");
			int idx = rnd.nextInt(play_list.size());
			String full_play_file = play_list.get(idx);
			play_file(mp, my_files_folder + full_play_file);
		} else {
			mylog("Not found : " + my_files_folder + hour_of_day_string
				+ minute_string + ".*");

			play_list = get_snd_startswith(sndfiles, "h" + hour_of_day_string
				+ ".");
			mylog("Found " + play_list.size() + " items.");
			int idx = rnd.nextInt(play_list.size());
			String hour_play_file = play_list.get(idx);
			play_file(mp, my_files_folder + hour_play_file);
			String minute_play_file = hour_play_file.replace("h"
				+ hour_of_day_string + ".", "m" + minute_string + ".");
			play_file(mp, my_files_folder + minute_play_file);
		}
		play_list.clear();

		while (mp.isPlaying()) {
			SystemClock.sleep(100);
		}
		audioManager.abandonAudioFocus(audioFocusChangeListener);
	}

	public void setup_next(Context context) {
		CronExpression cronExpr;
		ZonedDateTime after;
		ZonedDateTime min_after = ZonedDateTime
			.of(2012, 4, 10, 13, 0, 1, 0, zoneId);
		long am_start;
		long min_am_now = Long.MAX_VALUE;
		long min_am_start = Long.MAX_VALUE;
		ZonedDateTime now = ZonedDateTime.now();
		long now_long = now.toInstant().toEpochMilli();

		File f = new File(sdcard_folder + CRONTAB_FOLDER, FN_CRONTAB);
		if(f.exists() && !f.isDirectory()) {
			mylog(sdcard_folder+CRONTAB_FOLDER+FN_CRONTAB+" exists");
			try
			{
				String filename = sdcard_folder + CRONTAB_FOLDER + FN_CRONTAB;
				FileReader fr = new FileReader(filename);
				BufferedReader reader = new BufferedReader(fr);
				// считаем сначала первую строку
				String line = reader.readLine();
				while (line != null) {
					line = line.trim();
					//~ mylog(line);
					if (line != "") {
						if (line.charAt(0) != '#') {
							cronExpr = new CronExpression(line);
							after = cronExpr.nextTimeAfter(now);
							am_start = after.toInstant().toEpochMilli();
							if (min_am_now > (am_start - now_long)) {
								min_am_now = am_start - now_long;
								min_am_start = am_start;
								min_after = after;
							}
							mylog(line + "	-	" + after + "	-	"
								+ am_start);
						}
					}
					// считываем остальные строки в цикле
					line = reader.readLine();
				}
			}
			catch(IOException ioe)
			{
				ioe.printStackTrace();
			}
			//~ min_am_start = min_am_start - 14400;
			//~ min_am_start = min_am_start - 60000;
			mylog("setup am	-	" + min_after + "	-	"
				+ (min_am_start - now_long) / 60000);
			switch (SetAlarmMode)
			{
				 case 1:
					mylog("Using setExactAndAllowWhileIdle.");
					this._setExactAndAllowWhileIdle(context, min_am_start);
				 break;

				 case 2:
					mylog("Using setAlarmClock.");
					this._setAlarmClock(context, min_am_start);
				 break;

				 default:
					mylog("No SetAlarmMode defined!")
				 ;
			}
		}
		mylog("Finished.");
	}
}
