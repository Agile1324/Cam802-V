package com.linkcard.cam802;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.linkcard.media.LinkVideoCore;
import com.linkcard.media.LinkVideoView;
import com.linkcard.media.LinkVideoView.LinkVideoViewListener;

public class MainActivity2 extends Activity implements OnClickListener, LinkVideoViewListener {

	private TextView mTxtRecorderTimer;
	private ImageButton mBtnPlay;
	private ImageButton mBtnTakePicture;
	private ImageButton mBtnRecord;
	private ImageButton mBtnScreenSnapshot;
	private ProgressBar mPB;

	private LinkVideoCore linkStream;
	private LinkVideoView mVideoView;

	private boolean isPausing = false;
	private boolean isRecording = false;

	private boolean isStreaming = false;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mTxtRecorderTimer = (TextView) findViewById(R.id.mRecorderTimer);
		mBtnPlay = (ImageButton) findViewById(R.id.mBtnPlay);
		mBtnTakePicture = (ImageButton) findViewById(R.id.mBtnTakePicture);
		mBtnRecord = (ImageButton) findViewById(R.id.mBtnRecord);
		mBtnScreenSnapshot = (ImageButton) findViewById(R.id.mBtnScreenSnapshot);
		mBtnPlay.setOnClickListener(this);
		mBtnTakePicture.setOnClickListener(this);
		mBtnTakePicture.setOnLongClickListener(new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				onThumbPreviewClick();
				return true;
			}
		});
		mBtnRecord.setOnClickListener(this);
		mBtnScreenSnapshot.setOnClickListener(this);

		mPB = (ProgressBar) findViewById(R.id.mPB);

		enableViews(false);

		mVideoView = (LinkVideoView) findViewById(R.id.mVideoView);
		mVideoView.setLinkVideoViewListener(this);
		// mVideoView.startPlayback();
	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		if (!isStreaming) {
			mVideoView.startPlayback();
			mPB.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onBackPressed() {
		 if (!isStreaming){
			 return;
		 }
		 
		final CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity2.this);
		final CustomDialog dialog;
		builder.setTitle("Warning");
		if (isRecording) {
			builder.setMessage("Do you want to stop recording and disconnect with device ? ");
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mVideoView != null) {
						mVideoView.stopRecord();
						mVideoView.stopPlayback();
						dialog.dismiss();
						MainActivity2.this.finish();
					}

				}
			});
		} else {
			builder.setMessage("Do you want to disconnect with device ? ");
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					if (mVideoView != null) {
						mVideoView.stopPlayback();
						dialog.dismiss();
						MainActivity2.this.finish();
					}

				}
			});
		}

		builder.setNegativeButton("Cancel", null);
		dialog = builder.create();
		dialog.show();
	}

	/* update play button status */
	private void updatePlayStatus(boolean pausing) {
		if (pausing) {
			toastMsg("Stream is paused.");
			mBtnPlay.setImageResource(R.drawable.btn_play);
		} else {
			toastMsg("Stream is resumed.");
			mBtnPlay.setImageResource(R.drawable.pause);
		}
	}

	private Timer mRecordTimer;
	private int mRecordedSecond = 0;

	private void updateRecordStatus(boolean recording) {
		if (recording) {
			mRecordTimer = new Timer();
			mTxtRecorderTimer.setVisibility(View.VISIBLE);
			mRecordTimer.schedule(new TimerTask() {
				@Override
				public void run() {
					int hour = mRecordedSecond / 3600;
					int min = mRecordedSecond % 3600 / 60;
					int sec = mRecordedSecond % 60;
					final String hourString = String.format("%02d", hour);
					final String minString = String.format("%02d", min);
					final String secString = String.format("%02d", sec);
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							mTxtRecorderTimer.setText(hourString + ":" + minString + ":" + secString);
						}
					});
					mRecordedSecond++;
				}
			}, 0, 1000);
		} else {
			mTxtRecorderTimer.setVisibility(View.INVISIBLE);
			mRecordedSecond = 0;
			mTxtRecorderTimer.setText("00:00");
			if (mRecordTimer != null) {
				mRecordTimer.cancel();
			}
		}
	}

	public void onClick(View v) {
		int ret = 0;
		switch (v.getId()) {
		case R.id.mBtnPlay:
			doPlay();
			break;
		case R.id.mBtnTakePicture:
			doTakePicture();
			break;
		case R.id.mBtnRecord:
			doRecording();
			break;
		case R.id.mBtnScreenSnapshot:
			doCaptureScreen();
			break;
		}
	}

	@Override
	public void onVideoEvent(int type, int p1, int p2) {
		switch (type) {
		case LinkVideoView.MSG_TYPE_VIDEO_START:
			enableViews(true);
			isStreaming = true;
			break;
		case LinkVideoView.MSG_TYPE_VIDEO_STOP:
			enableViews(false);
			isStreaming = false;
			// showMsgBox("Warning", "Disconnected with your device.");
			break;
		case LinkVideoView.MSG_TYPE_ERROR_OPEN:
			enableViews(false);
			isStreaming = false;
			// showMsgBox("Warning", "Failed to connect with your device.");
			doOpenError();
			break;
		}
	}

	private void doOpenError() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity2.this);
				builder.setTitle("Warning");
				builder.setMessage("Failed to connect with your device.");
				builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						MainActivity2.this.finish();
					}
				});
				CustomDialog dialog = builder.create();
				dialog.show();
			}
		});
	}

	private void enableViews(final boolean enabled) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				mBtnPlay.setEnabled(enabled);
				mBtnTakePicture.setEnabled(enabled);
				mBtnRecord.setEnabled(enabled);
				mBtnScreenSnapshot.setEnabled(enabled);
				if (enabled) {
					mPB.setVisibility(View.GONE);
					mBtnPlay.setImageResource(R.drawable.pause);
				}
			}
		});
	}

	private void doPlay() {
		if (!isPausing) {/* pause */
			if (mVideoView.pausePlayback()) {/* success */
				isPausing = true;
				/* update play status */
				updatePlayStatus(true);
			}
		} else {/* resume */
			if (mVideoView.resumePlayback()) {/* success */
				isPausing = false;
				/* update play status */
				updatePlayStatus(false);
			}
		}
	}

	private void doRecording() {
		int ret = 0;
		if (!isRecording) {
			String filename = Environment.getExternalStorageDirectory().getAbsolutePath();
			long time = System.currentTimeMillis();
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			Date d1 = new Date(time);
			String date = format.format(d1);
			filename = filename + ("/" + "VideoFishing/video_" + date + ".mp4");
			ret = mVideoView.startRecord(filename);
			if (ret == 0) {/* success */
				isRecording = true;
				/* set record status */
				updateRecordStatus(true);
			} else {
				showMsgBox("Warning", "Failed to start recording.");
			}
		} else {
			ret = mVideoView.stopRecord();
			if (ret == 0) {/* success */
				isRecording = false;
				updateRecordStatus(false);
			}
		}

	}

	private void doCaptureScreen() {
		View rootLayout = findViewById(R.id.mRelativeLayoutMain);
		String filename = Environment.getExternalStorageDirectory().getAbsolutePath();
		long time = System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		Date d1 = new Date(time);
		String date = format.format(d1);
		filename = filename + ("/" + "VideoFishing/screenshot_" + date + ".jpg");

		Bitmap b = Bitmap.createBitmap(rootLayout.getWidth(), rootLayout.getHeight(), Bitmap.Config.ARGB_8888);
		Canvas c = new Canvas(b);
		// c.drawBitmap(mVideoView.getVideoFrame(), 0, 0, null);
		rootLayout.draw(c);
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(filename);
			if (fos != null) {
				b.compress(Bitmap.CompressFormat.JPEG, 90, fos);
				fos.close();
				toastMsg("Successful Capturing, file path is " + filename);
			}
		} catch (Exception e) {
			e.printStackTrace();
			toastMsg("Failed to capture screen. ");
		}
	}

	private void doTakePicture() {
		String filename = Environment.getExternalStorageDirectory().getAbsolutePath();

		long time = System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		Date d1 = new Date(time);
		String date = format.format(d1);

		filename = filename + ("/" + "VideoFishing/img_" + date + ".jpg");

		if (mVideoView.takePicture(filename)) {
			toastMsg("Successfully to take picture, file path is " + filename);
		} else {
			toastMsg("Failed to take picture");
		}
	}

	private void showMsgBox(final String title, final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity2.this);
				builder.setTitle(title);
				builder.setMessage(msg);
				builder.setPositiveButton("Close", null);
				CustomDialog dialog = builder.create();
				dialog.show();
			}
		});
	}

	private void toastMsg(final String msg) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast t = Toast.makeText(MainActivity2.this, msg, Toast.LENGTH_SHORT);
				t.setGravity(Gravity.CENTER, 0, 0);
				t.show();
			}
		});
	}

	private static final String LAUNCH_PACKAGENAME = "com.linkcard.videofishing";
	private static final String LAUNCH_CLASS_THUMB_PREVIEW = "com.linkcard.videofishing.LookbackActivity";

	private void onThumbPreviewClick() {
		Intent launchIntentPreview = new Intent();
		launchIntentPreview.setComponent(new ComponentName(LAUNCH_PACKAGENAME, LAUNCH_CLASS_THUMB_PREVIEW));
		launchIntentPreview.setAction(Intent.ACTION_MAIN);
		launchIntentPreview.addCategory(Intent.CATEGORY_LAUNCHER);
	
		startActivitySafety(MainActivity2.this, launchIntentPreview);
		
	}

	private static void startActivitySafety(Activity a, Intent intent) {
		try {
			if (intent == null) {
				return;
			}
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			a.startActivity(intent);
		} catch (ActivityNotFoundException e) {

		} catch (SecurityException e) {

		}
	}
	

}
