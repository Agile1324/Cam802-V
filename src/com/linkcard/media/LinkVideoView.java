package com.linkcard.media;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class LinkVideoView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

	private static final String TAG = "DBG";

	public static final int MSG_TYPE_VIDEO_START = 100;
	public static final int MSG_TYPE_VIDEO_STOP = 101;
	public static final int MSG_TYPE_VIDEO_SIZE = 102;
	public static final int MSG_TYPE_ERROR_OPEN = 200;
	public static final int MSG_TYPE_ERROR_DECODE = 201;
	public static final int MSG_TYPE_ERROR_CONVERT = 202;
	
	public static final int  E_SYS_NOERR  = (0);
	public static final int  E_SYS_NOTINIT =  (-2);  //没有初始化ok
	public static final int  E_SYS_NOFRMDATA =  (-3);  //buffer里面没有帧了
	public static final int  E_SYS_BADFRAME = (-4);  //解码出来的图像是错误的
	public static final int  E_SYS_NOFRAME = (-5);  //没有解码出正确的视频帧
	public static final int  E_SYS_UNKOWNERR = (-6);
	public static final int  E_SYS_ERRBITMAP = (-7);

	private Rect mPreviewWindow;
	private boolean mIsRunning = true;
	private SurfaceHolder mSurfaceHolder;
	private Bitmap mPreviewBitmap;
	private String mTakePictureFilename = null;
	private boolean mIsTakePicture = false;
	private boolean isPaused = false;
	
	public int getmVideoWidth() {
		return mVideoWidth;
	}

	public void setmVideoWidth(int mVideoWidth) {
		this.mVideoWidth = mVideoWidth;
	}

	public int getmVideoHeight() {
		return mVideoHeight;
	}
	
	public void setmVideoHeight(int mVideoHeight) {
		this.mVideoHeight = mVideoHeight;
	}

	private int  mVideoWidth  = 1920;
	private int  mVideoHeight = 1080;

	private LinkVideoViewListener mLinkVideoViewListener;


	public LinkVideoView(Context context) {
		super(context);
		Log.d(TAG, "CameraPreview 1...");
		init();
	}

	public LinkVideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		Log.d(TAG, "CameraPreview 2...");
		init();
	}

	public LinkVideoView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		Log.d(TAG, "CameraPreview 3...");
		init();
	}

	private Bitmap drawLogo(Bitmap bg, Bitmap logo) {
		if (!bg.isMutable()) {
			bg = bg.copy(Bitmap.Config.ARGB_8888, true);
		}

		Paint p = new Paint();
		p.setStyle(Paint.Style.STROKE);
		p.setAlpha(200);

		Canvas canvas = new Canvas(bg);
		Rect rect = new Rect(0, (bg.getHeight() - logo.getHeight()), bg.getWidth(), bg.getHeight());
		// canvas.drawBitmap(logo, null, rect,p);
		canvas.drawBitmap(logo, null, rect, null);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();
		return bg;
	}

	@Override
	public void run() 
	{
		Log.d(TAG, "run...");
		while (mIsRunning) 
		{
			/**************************************************************************
			 * 
			 *  这里这个很重要，因为是一个循环，所以所有的jni消息通过这里返回上来。而不需要另外再搞个通知了。
			 * 
			 **************************************************************************/
			int ret = getVideoFrame(mPreviewBitmap);
			if (E_SYS_ERRBITMAP == ret) 
			{
				mVideoWidth  = getVideoWidth();
				mVideoHeight = getVideoHeight();
				mPreviewBitmap.recycle();
				mPreviewBitmap = null;
				mPreviewBitmap = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Bitmap.Config.RGB_565);
				continue;
			}
			else if(E_SYS_NOTINIT == ret) 
			{
				///可能是没有连接上，弹出对话框，或者显示背景图片的其他信息
				continue;
			}
			else if((E_SYS_NOFRMDATA == ret) || (E_SYS_BADFRAME == ret) || (E_SYS_NOFRAME == ret) || (E_SYS_UNKOWNERR == ret) )
			{
				///其他解码相关出错了，就不处理了，获取下一帧图片好了
				continue;
			}			
			else //(E_SYS_NOERR == ret) 
			{
				///继续跟新显示
			}	
			
			// draw logo
			if (mIsLogoShow) {
				drawLogo(mPreviewBitmap, mBitmapLogo);
			}

			/* take picture */
			if (mIsTakePicture) {
				doTakePicture(mTakePictureFilename, mPreviewBitmap);
			}

			// show video
			if (!isPaused)
			{
				Canvas canvas = mSurfaceHolder.lockCanvas();
				if (canvas != null) {
					canvas.drawBitmap(mPreviewBitmap, null, mPreviewWindow, null);
					mSurfaceHolder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int winWidth, int winHeight) {
		Log.d(TAG, "surfaceChanged...");
		int width, height, dw, dh;
		if (winWidth * 3 / 4 <= winHeight) {
			dw = 0;
			dh = (winHeight - winWidth * 3 / 4) / 2;
			width = dw + winWidth - 1;
			height = dh + winWidth * 3 / 4 - 1;
		} else {
			dw = (winWidth - winHeight * 4 / 3) / 2;
			dh = 0;
			width = dw + winHeight * 4 / 3 - 1;
			height = dh + winHeight - 1;
		}

		// if (winWidth > winHeight){
		// mPreviewWindow = new Rect(0, 0, winWidth, winHeight);
		// }else{
		// mPreviewWindow = new Rect(dw, dh, width, height);
		// }

		Log.d(TAG, "winWidth " + winWidth + ", winHeight" + winHeight + ", width " + width + ", height " + height
		        + ", w/h=" + width / height);
		mPreviewWindow = new Rect(dw, dh, width, height);

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		Log.d(TAG, "surfaceCreated...");
		mIsRunning = true;
		(new Thread(this)).start();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		Log.d(TAG, "surfaceDestroyed...");
		mIsRunning = false;
		stopPlayback();
	}

	private void init() {
		this.setFocusable(true);
		mSurfaceHolder = this.getHolder();
		mSurfaceHolder.addCallback(this);
		mPreviewBitmap = Bitmap.createBitmap(mVideoWidth, mVideoHeight, Bitmap.Config.RGB_565);
	}

	private Bitmap mBitmapLogo;
	private boolean mIsLogoShow = false;

	public void enableLogo(Bitmap bitmap) {
		mIsLogoShow = true;
		mBitmapLogo = bitmap;
	}

	public void disableLogo() {
		mIsLogoShow = false;
		mBitmapLogo = null;
	}

	/**
	 * 
	 */
	private void doTakePicture(String filename, Bitmap bitmap) {
		String prefix = filename.substring(0, filename.lastIndexOf('/'));
		if (prefix == null || bitmap == null) {
			mIsTakePicture = false;
			return;
		}

		File f = new File(prefix);
		if (!f.exists()) {
			f.mkdirs();
		}

		File file = new File(filename);

		try {
			file.createNewFile();
			FileOutputStream out = new FileOutputStream(file);
			bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mIsTakePicture = false;
			return;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mIsTakePicture = false;
			return;
		}

		mIsTakePicture = false;
	}

	public boolean takePicture(String filename) {
		if (!mIsRunning || mIsTakePicture || filename == null) {
			return false;
		}
		mTakePictureFilename = filename;
		mIsTakePicture = true;
		return true;
	}

	private native int getVideoFrame(Bitmap bitmap);
	
	private native int getVideoWidth();
	private native int getVideoHeight();
	
	public native int startPlayback();

	public native int stopPlayback();

	public native int startRecord(String filename);

	public native int stopRecord();

	public boolean pausePlayback() 
	{
		if (!mIsRunning || isPaused) {
			return false;
		}
		isPaused = true;
		return true;
	}

	public boolean resumePlayback() {
		if (!mIsRunning || !isPaused) {
			return false;
		}
		isPaused = false;
		return true;
	}

	public void setLinkVideoViewListener(LinkVideoViewListener listener) {
		mLinkVideoViewListener = listener;
	}

	public interface LinkVideoViewListener {
		public void onVideoEvent(int type, int p1, int p2);
	}

}
