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

public class LinkVideoCore 
{
	private static final String TAG = "DBG";

	static {
		System.loadLibrary("linkcardplayer");
		Log.d(TAG, "liblinkcardplayer.so");
	}
	
	public native int sysinit();

	public native int sysuninit();
}
