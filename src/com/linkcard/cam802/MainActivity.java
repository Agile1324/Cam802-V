package com.linkcard.cam802;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.MeasureSpec;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.linkcard.cam802.ControlCmdHelper.ControlCmdListener;
import com.linkcard.media.LinkVideoCore;
import com.linkcard.media.LinkVideoView;
import com.linkcard.media.LinkVideoView.LinkVideoViewListener;
import com.topeet.serialtest.ServiceTest;
import com.topeet.serialtest.SocThread;
import com.topeet.serialtest.serial;

public class MainActivity extends Activity implements OnClickListener, LinkVideoViewListener {
	
	serial com3 = new serial();
	
	/**************************/
	//SocThread数据传输相关
	private String TAG = "===Client===";
	private TextView tvInfo = null;
	Handler mhandler;
	Handler mhandlerSend;
	boolean isRun = true;
	EditText edtsendms;
	Button btnsend;
	SharedPreferences sp;
	Button btnSetting;
	Socket socket;
	PrintWriter out;
	BufferedReader in;
	SocThread socketThread;
	
	/*************************/

	private TextView mTxtRecorderTimer;
	private ImageButton mBtnRecordMobile, mBtnPlayPause;
	private ImageButton mBtnSnapshot, mBtnWifiOnOff, mBtnFileBrowser, mBtnSetting;
	
	private ProgressBar mPB;
	
	private serial mSerial;

	private LinkVideoCore linkStream;
	private LinkVideoView mVideoView;

	private boolean isPausing = false;
	private boolean isRecording = false;

	private boolean isStreaming = false;
	
	private boolean isForceExit = false;
	
	private boolean isInput = false; //用于输入流循环
	
	private Intent startIntent;


	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main2);
		
		/****** Service后台程序 暂时未启用
		startIntent = new Intent(MainActivity.this, ServiceTest.class);
		startService(startIntent);//启动服务
		Log.d("ServiceText", "Intent onCreate");
		*/
		
		linkStream = new LinkVideoCore();
		linkStream.sysinit();//系统初始化
		
		tvInfo = (TextView) findViewById(R.id.tvInfo);
		
		mTxtRecorderTimer = (TextView) findViewById(R.id.mRecorderTimer);
		
		mBtnRecordMobile = (ImageButton) findViewById(R.id.mBtnRecordMobile);
		mBtnPlayPause = (ImageButton) findViewById(R.id.mBtnPlayPause);
		mBtnSnapshot = (ImageButton) findViewById(R.id.mBtnSnapshot);
		mBtnWifiOnOff = (ImageButton) findViewById(R.id.mBtnWifiOnOff);
		mBtnFileBrowser = (ImageButton) findViewById(R.id.mBtnFileBrowser);
		mBtnSetting = (ImageButton) findViewById(R.id.mBtnSetting);
		
		mBtnRecordMobile.setOnClickListener(this);
		mBtnPlayPause.setOnClickListener(this);
		mBtnSnapshot.setOnClickListener(this);
		mBtnWifiOnOff.setOnClickListener(this);
		mBtnFileBrowser.setOnClickListener(this);
		mBtnSetting.setOnClickListener(this);

		mPB = (ProgressBar) findViewById(R.id.mPB);

		enableViews(false);

		mVideoView = (LinkVideoView) findViewById(R.id.mVideoView);
		mVideoView.setLinkVideoViewListener(this);
		
		tvInfo.setBackgroundColor(Color.argb(0, 0, 0, 0));//背景透明
		
		checkValidation();
		
		
		//创建异步任务
		mhandler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				try {
					if (msg.obj != null) {
						String s = msg.obj.toString();
						if (s.trim().length() > 0) {
							//Log.i(TAG, "mhandler接收到obj=" + s);
							//Log.i(TAG, "开始更新UI");
							tvInfo.setText( s +"档");
							Log.i(TAG, "更新UI完毕" + s);
						} else {
							Log.i(TAG, "没有数据返回不更新");
						}
					}
				} catch (Exception ee) {
					Log.i(TAG, "加载过程出现异常");
					ee.printStackTrace();
				}
			}
		};startSocket();
	}

	public void startSocket() {
		socketThread = new SocThread(mhandler);
		socketThread.start();
	}
	
	public void stopSocket(){
		socketThread.isRun = false;
		socketThread.close();
		socketThread = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (!isStreaming) {
			mVideoView.startPlayback();
			//mPB.setVisibility(View.VISIBLE);
		}
		enableViews(true);
		isStreaming = true;
	}

	@Override
	public void onBackPressed() {
		final CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity.this);
		final CustomDialog dialog;
		builder.setTitle("Warning");
		if (isRecording) {
			builder.setMessage("Do you want to stop recording and disconnect with device ? ");
			builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					isForceExit = true;
					if (mVideoView != null) {
						mVideoView.stopRecord();
						mVideoView.stopPlayback();
						dialog.dismiss();
						MainActivity.this.finish();
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
						MainActivity.this.finish();
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
			mBtnPlayPause.setImageResource(R.drawable.btn_play);
		} else {
			toastMsg("Stream is resumed.");
			mBtnPlayPause.setImageResource(R.drawable.pause);
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
		case R.id.mBtnPlayPause:
			doPlay();
			break;
		case R.id.mBtnSnapshot:
			doTakePicture();
			break;
		case R.id.mBtnRecordMobile:
			doRecording();
			break;
		case R.id.mBtnFileBrowser:
			onThumbPreviewClick();
			break;
		case R.id.mBtnSetting:
			doSetting();
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
				CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity.this);
				builder.setTitle("Warning");
				builder.setMessage("Failed to connect with your device.");
				builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.dismiss();
						MainActivity.this.finish();
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
				mBtnRecordMobile.setEnabled(enabled);
				mBtnPlayPause.setEnabled(enabled);
				mBtnSetting.setEnabled(enabled);
				mBtnSnapshot.setEnabled(enabled);
				mBtnFileBrowser.setEnabled(enabled);
				mBtnWifiOnOff.setEnabled(false);
//				if (enabled) {
//					mPB.setVisibility(View.GONE);
//					mBtnWifiOnOff.setImageResource(R.drawable.wifi_off);
//				}
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
			//String filename = Environment.getExternalStorageDirectory().getAbsolutePath();
			long time = System.currentTimeMillis();
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
			Date d1 = new Date(time);
			String date = format.format(d1);
			filename = filename + ("/" + "cam802/video_" + date + ".mp4");
			ret = mVideoView.startRecord(filename);
//			if (ret == 0) {/* success */
				isRecording = true;
				/* set record status */
				updateRecordStatus(true);
//			} else {
//				showMsgBox("Warning", "Failed to start recording.");
//			}
		} else {
			ret = mVideoView.stopRecord();
//			if (ret == 0) {/* success */
				isRecording = false;
				updateRecordStatus(false);
//			}
		}

	}

	private void doCaptureScreen() {
		View rootLayout = findViewById(R.id.mRelativeLayoutMain);
		String filename = Environment.getExternalStorageDirectory().getAbsolutePath();
		long time = System.currentTimeMillis();
		SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
		Date d1 = new Date(time);
		String date = format.format(d1);
		filename = filename + ("/" + "cam802/screenshot_" + date + ".jpg");

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

		filename = filename + ("/" + "cam802/img_" + date + ".jpg");

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
				CustomDialog.Builder builder = new CustomDialog.Builder(MainActivity.this);
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
				Toast t = Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT);
				t.setGravity(Gravity.CENTER, 0, 0);
				t.show();
			}
		});
	}

	private static final String LAUNCH_PACKAGENAME = "com.linkcard.cam802";
	private static final String LAUNCH_CLASS_THUMB_PREVIEW = "com.linkcard.cam802.LookbackActivity";

	private void onThumbPreviewClick() {
		Intent launchIntentPreview = new Intent();
		launchIntentPreview.setComponent(new ComponentName(LAUNCH_PACKAGENAME, LAUNCH_CLASS_THUMB_PREVIEW));
		launchIntentPreview.setAction(Intent.ACTION_MAIN);
		launchIntentPreview.addCategory(Intent.CATEGORY_LAUNCHER);
	
		startActivitySafety(MainActivity.this, launchIntentPreview);
		
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
	
	private ControlCmdHelper mControlCmdHelper = new ControlCmdHelper();
	private CustomDialog mCustomDialog;
	private void doSetting(){
        //final CustomDialog mCustomDialog;
		CustomDialog.Builder builder = new CustomDialog.Builder(this);
		builder.setTitle("Settings");
		//builder.setMessage(R.string.str_input_new_wifi_name);
		
		LayoutInflater inflater = LayoutInflater.from(this);
		final View textEntryView = inflater.inflate(R.layout.cus_alert_setting, null);
//		final EditText edtInput=(EditText)textEntryView.findViewById(R.id.edtInput);
//		edtInput.setHint(R.string.str_input_new_wifi_name);
		
		final TextView wifiSSIDText = (EditText)textEntryView.findViewById(R.id.mSettingTextWiFiSSID);
		final TextView wifiSSIDBtn = (Button)textEntryView.findViewById(R.id.mSettingBtnWiFiSSID);
		final TextView wifiPWDText = (EditText)textEntryView.findViewById(R.id.mSettingTextWiFiPWD);
		final TextView wifiPWDBtn = (Button)textEntryView.findViewById(R.id.mSettingBtnWiFiPWD);
		//final TextView wifiFactoryReset = (Button)textEntryView.findViewById(R.id.mSettingBtnFactoryReset);
		
		wifiSSIDBtn.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View arg0) {
				if (wifiSSIDText == null || wifiSSIDText.getText().toString().length() == 0){
					return;
				}
				
				wifiSSIDText.setEnabled(false);
				wifiPWDText.setEnabled(false);
				wifiSSIDBtn.setEnabled(false);
				wifiPWDBtn.setEnabled(false);
				
	            mControlCmdHelper.sendCmd(ControlCmdHelper.CONTROL_CMD_SET_WIFI_SSID + wifiSSIDText.getText().toString().trim(), new ControlCmdListener(){
					@Override
                    public void onFailure(int type) {
						showMsgBox("Error", "Failed to set wifi ssid.");	
						//mCustomDialog.dismiss();
						wifiSSIDText.setEnabled(true);
						wifiPWDText.setEnabled(true);
						wifiSSIDBtn.setEnabled(true);
						wifiPWDBtn.setEnabled(true);
                    }

					@Override
                    public void onSuccess(Object obj) {
						WifiSSIDCmdInfo info  = (WifiSSIDCmdInfo)obj;
						if (info==null || info.getCode()!=ControlCmdHelper.CONTROL_CMD_CODE_SUCCESS){
							showMsgBox("Error", "Failed to set wifi ssid.");
							//mCustomDialog.dismiss();
							wifiSSIDText.setEnabled(true);
							wifiPWDText.setEnabled(true);
							wifiSSIDBtn.setEnabled(true);
							wifiPWDBtn.setEnabled(true);
							return;
						}
						mCustomDialog.dismiss();
						showMsgBox("Success", "Successfully set wifi ssid.");	
						wifiSSIDText.setEnabled(true);
						wifiPWDText.setEnabled(true);
						wifiSSIDBtn.setEnabled(true);
						wifiPWDBtn.setEnabled(true);
                    }	            	
	            }, WifiSSIDCmdInfo.class);
            }			
		});
		
		wifiPWDBtn.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View arg0) {
				if (wifiPWDText == null || wifiPWDText.getText().toString().length() == 0){
					return;
				}
				
				wifiSSIDText.setEnabled(false);
				wifiPWDText.setEnabled(false);
				wifiSSIDBtn.setEnabled(false);
				wifiPWDBtn.setEnabled(false);
				
	            mControlCmdHelper.sendCmd(ControlCmdHelper.CONTROL_CMD_SET_WIFI_PWD + wifiPWDText.getText().toString().trim(), new ControlCmdListener(){
					@Override
                    public void onFailure(int type) {
						showMsgBox("Error", "Failed to set wifi pwd.");	
						//mCustomDialog.dismiss();
						wifiSSIDText.setEnabled(true);
						wifiPWDText.setEnabled(true);
						wifiSSIDBtn.setEnabled(true);
						wifiPWDBtn.setEnabled(true);
                    }

					@Override
                    public void onSuccess(Object obj) {
						WifiSSIDCmdInfo info  = (WifiSSIDCmdInfo)obj;
						if (info==null || info.getCode()!=ControlCmdHelper.CONTROL_CMD_CODE_SUCCESS){
							showMsgBox("Error", "Failed to set wifi pwd.");
							//mCustomDialog.dismiss();
							wifiSSIDText.setEnabled(true);
							wifiPWDText.setEnabled(true);
							wifiSSIDBtn.setEnabled(true);
							wifiPWDBtn.setEnabled(true);
							return;
						}
						mCustomDialog.dismiss();
						showMsgBox("Success", "Successfully set wifi pwd.");	
						wifiSSIDText.setEnabled(true);
						wifiPWDText.setEnabled(true);
						wifiSSIDBtn.setEnabled(true);
						wifiPWDBtn.setEnabled(true);
                    }	            	
	            }, WifiPwdCmdInfo.class);
			}
		});
		
		
		builder.setContentView(textEntryView);
		builder.setPositiveButton("Factory Reset", new DialogInterface.OnClickListener(){
			@Override
			public void onClick(DialogInterface arg0, int arg1) {				
				wifiSSIDText.setEnabled(false);
				wifiPWDText.setEnabled(false);
				wifiSSIDBtn.setEnabled(false);
				wifiPWDBtn.setEnabled(false);
				
	            mControlCmdHelper.sendCmd(ControlCmdHelper.CONTROL_CMD_FACTORY_DEFAULT, new ControlCmdListener(){
					@Override
                    public void onFailure(int type) {
						showMsgBox("Error", "Failed to factory reset.");	
						//mCustomDialog.dismiss();
						wifiSSIDText.setEnabled(true);
						wifiPWDText.setEnabled(true);
						wifiSSIDBtn.setEnabled(true);
						wifiPWDBtn.setEnabled(true);
                    }

					@Override
                    public void onSuccess(Object obj) {
						WifiSSIDCmdInfo info  = (WifiSSIDCmdInfo)obj;
						if (info==null || info.getCode()!=ControlCmdHelper.CONTROL_CMD_CODE_SUCCESS){
							showMsgBox("Error", "Failed to factory reset.");
							//mCustomDialog.dismiss();
							wifiSSIDText.setEnabled(true);
							wifiPWDText.setEnabled(true);
							wifiSSIDBtn.setEnabled(true);
							wifiPWDBtn.setEnabled(true);
							return;
						}
						mCustomDialog.dismiss();
						showMsgBox("Success", "Successfully factory reset.");	
						wifiSSIDText.setEnabled(true);
						wifiPWDText.setEnabled(true);
						wifiSSIDBtn.setEnabled(true);
						wifiPWDBtn.setEnabled(true);
                    }	            	
	            }, FactoryDefaultCmdInfo.class);
			}		
		});
		
		mCustomDialog = builder.create();
		mCustomDialog.show();
	}
	
	private void checkValidation(){
		new Thread(new Runnable(){
			@Override
			public void run() {
				while(!isForceExit){
					runOnUiThread(new Runnable() {
						@Override
						public void run() {
							doRequestWiFiStatus();
						}						
					});
					
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
		}).start();
	}
	
	private void doRequestWiFiStatus(){
		if (isForceExit){
			return;
		}
		
        mControlCmdHelper.sendCmd(ControlCmdHelper.CONTROL_CMD_VERSION, new ControlCmdListener(){
			@Override
            public void onFailure(int type) {
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						mBtnWifiOnOff.setImageResource(R.drawable.wifi_disabled);
					}					
				});
            }

			@Override
            public void onSuccess(Object obj) {
				runOnUiThread(new Runnable(){
					@Override
					public void run() {
						mBtnWifiOnOff.setImageResource(R.drawable.wifi_enabled);
					}					
				});
            }	            	
        }, VersionCmdInfo.class);
	}
	
	@Override
	protected void onStop() {
		// TODO Auto-generated method stub
		super.onStop();
		stopSocket();
	}
	
	@Override
	protected void onDestroy(){
		super.onDestroy();
	    stopService(startIntent);
	    Log.d("ServiceTest", "Service 已结束");
	}
	
}


