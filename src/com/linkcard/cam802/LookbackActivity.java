package com.linkcard.cam802;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;


import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingProgressListener;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

public class LookbackActivity extends Activity{
	private GridView mLookbackGridView;
	
	private List<String> mMediaFilesList = new ArrayList<String>();
	private MediaFilesListAdapter mMediaFilesListAdapter;
	
	private DisplayImageOptions mDisplayImageOptions;
	
	private PopupWindow mPopupWindow;
	private RelativeLayout mLookbackLayout;
	private TextView mPopText;
	private Button mBtnPopDelete;
	private Button mBtnPopDeleteAll;
	private Button mBtnPopCancel;
	
	private TextView mLookbackTextViewEmpty;
	
	private int mMediaFileCurItem = 0;
	
	private MyFileAsyncTask mMyFileAsyncTask = null;
	private String mFileDir = android.os.Environment.getExternalStorageDirectory() + "/cam802";
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_lookback);
		
		mLookbackLayout = (RelativeLayout)findViewById(R.id.mLookbackLayout);
		
		mLookbackTextViewEmpty = (TextView)findViewById(R.id.mLookbackTextViewEmpty);
		
		mDisplayImageOptions = new DisplayImageOptions.Builder()
		.showImageOnLoading(R.drawable.ic_stub)
		.showImageForEmptyUri(R.drawable.ic_empty)
		.showImageOnFail(R.drawable.ic_error)
		.cacheInMemory(true)
		.cacheOnDisk(true)
		.considerExifParams(true)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();
		
		
		mLookbackGridView = (GridView)findViewById(R.id.mLookbackGridView);
		mMediaFilesListAdapter = new MediaFilesListAdapter(this);
		mLookbackGridView.setAdapter(mMediaFilesListAdapter);
		
		LayoutInflater inflater = LayoutInflater.from(this);
		View popView = inflater.inflate(R.layout.activity_pop, null);
		mPopText = (TextView)(popView.findViewById(R.id.mPopText));
		mBtnPopDelete = (Button)(popView.findViewById(R.id.mPopDelete));
		mBtnPopDelete.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				
				String file = mMediaFilesList.get(mMediaFileCurItem).substring("file://".length());
				File f = new File(file);
				if (f==null || !f.exists()){
					return;
				}
				
				f.delete();
				updateFileList();
				mPopupWindow.dismiss();
			}
		});
		
		mBtnPopDeleteAll = (Button)(popView.findViewById(R.id.mPopDeleteAll));
		mBtnPopDeleteAll.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				File f = new File(mFileDir);
				if (f==null || !f.exists()){
					return;
				}
				
				if (f.isFile()){
					f.delete();
				}else if (f.isDirectory()){
					File[] subfile = f.listFiles();
					for (int i=0; i<subfile.length;i++){
						if (subfile[i].isFile()){
							subfile[i].delete();
						}						
					}
				}
				
				mMediaFilesList.clear();
				mMediaFilesListAdapter.notifyDataSetChanged();
				mPopupWindow.dismiss();
				mLookbackTextViewEmpty.setVisibility(View.VISIBLE);
			}
		});
		
		mBtnPopCancel = (Button)(popView.findViewById(R.id.mPopCancel));
		mBtnPopCancel.setOnClickListener(new OnClickListener() {			
			@Override
			public void onClick(View v) {
				mPopupWindow.dismiss();
			}
		});
		
		mPopupWindow = new PopupWindow(popView, LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, false); 
		mPopupWindow.setBackgroundDrawable(new BitmapDrawable()); 
		mPopupWindow.setOutsideTouchable(true); 
		mPopupWindow.setFocusable(true);
		
		mLookbackGridView.setOnItemClickListener(new OnItemClickListener() {
			@Override
            public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
				mMediaFileCurItem = position;
				String filepath = mMediaFilesList.get(position);
				if (filepath.endsWith(".jpg")){
					openImageFileIntent(filepath);
				}else if (filepath.endsWith(".mp4")){
					openVideoFileIntent(filepath);
				}
            }			
		});
		
		mLookbackGridView.setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
            public boolean onItemLongClick(AdapterView<?> arg0, View view, int position, long arg3) {
				mMediaFileCurItem = position;
				int start = mMediaFilesList.get(position).lastIndexOf("/");
				String filepath = mMediaFilesList.get(position).substring(start+1);
				mPopText.setText(filepath);
				mPopupWindow.showAtLocation(mLookbackLayout, Gravity.CENTER, 0, 0);
	            return true;
            }
		});
		
		updateFileList();
	}
	
	private void updateFileList(){
		mMyFileAsyncTask = new MyFileAsyncTask();
		mMyFileAsyncTask.execute(mFileDir);
	}
	
	
	public void openImageFileIntent( String param ) {  
        Intent intent = new Intent(Intent.ACTION_VIEW);  
        Uri uri = Uri.parse(param);  
        intent.setDataAndType(uri, "image/*");    
		LookbackActivity.this.startActivity(intent);
    }  
	
    public void openVideoFileIntent( String param ) {  
        Intent intent = new Intent(Intent.ACTION_VIEW);  
        Uri uri = Uri.parse(param);  
        intent.setDataAndType(uri, "video/*");    
		LookbackActivity.this.startActivity(intent); 
    }
	
	public class MediaFilesListAdapter extends BaseAdapter {

		private LayoutInflater inflater;

		MediaFilesListAdapter(Context context) {
			inflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mMediaFilesList.size();
		}

		@Override
		public Object getItem(int position) {
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final ViewHolder holder;
			View view = convertView;
			if (view == null) {
				view = inflater.inflate(R.layout.item_grid_image, parent, false);
				holder = new ViewHolder();
				assert view != null;
				holder.imageView = (ImageView) view.findViewById(R.id.image);
				holder.progressBar = (ProgressBar) view.findViewById(R.id.progress);
				view.setTag(holder);
			} else {
				holder = (ViewHolder) view.getTag();
			}

			ImageLoader.getInstance()
					.displayImage(mMediaFilesList.get(position), holder.imageView, mDisplayImageOptions, new SimpleImageLoadingListener() {
						@Override
						public void onLoadingStarted(String imageUri, View view) {
							holder.progressBar.setProgress(0);
							holder.progressBar.setVisibility(View.VISIBLE);
						}

						@Override
						public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
							/*閺嶈宓侀崥搴ｇ磻閸掋倖鏌囬弬鍥︽缁鐎烽敍灞炬▔缁�桨绗夐崥宀�畱闁挎瑨顕ら崶鐐垼*/
							if (imageUri != null && imageUri.endsWith(".mp4")){
								// 鐠佸墽鐤嗙憴鍡涱暥閻ㄥ嫰鏁婄拠顖氭禈閺嶏拷	
								Bitmap bitmap2 = ((BitmapDrawable) getResources().getDrawable(R.drawable.video)).getBitmap();
								holder.imageView.setImageBitmap(bitmap2);
							}
							holder.progressBar.setVisibility(View.GONE);
						}

						@Override
						public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
							/*閺嶈宓侀崥搴ｇ磻閸掋倖鏌囬弬鍥︽缁鐎烽敍灞炬▔缁�桨绗夐崥宀�畱闁挎瑨顕ら崶鐐垼*/
							if (imageUri != null && imageUri.endsWith(".mp4")){								
								Bitmap bitmap2 = ((BitmapDrawable) getResources().getDrawable(R.drawable.media_play_new_normal)).getBitmap();
								Bitmap tmp = drawLogo(loadedImage, bitmap2);
								holder.imageView.setImageBitmap(tmp);
							}
							holder.progressBar.setVisibility(View.GONE);
						}
					}, new ImageLoadingProgressListener() {
						@Override
						public void onProgressUpdate(String imageUri, View view, int current, int total) {
							holder.progressBar.setProgress(Math.round(100.0f * current / total));
						}
					});

			return view;
		}
	}

	static class ViewHolder {
		ImageView imageView;
		ProgressBar progressBar;
	}
	
	
	private class MyFileAsyncTask extends AsyncTask<String, Integer, List<String>>{

		@Override
        protected List<String> doInBackground(String... param) {
	        if (param==null || param.length==0){
	        	return null;
	        }
	        
	        File dir = new File(param[0]);
	        if (dir==null || !dir.exists()){
	        	return null;
	        }
	        
	        File[] subdir = dir.listFiles();
	        if (subdir==null || subdir.length==0){
	        	return null;
	        }
	        
	        mMediaFilesList.clear();
	        for (int i=0; i<subdir.length; i++){
	        	File subfile = subdir[i];
	        	if (subfile != null && subfile.exists() && !subfile.isDirectory()){
	        		mMediaFilesList.add("file://" + subfile.getAbsolutePath());
	        	}
	        }
	        
	        return mMediaFilesList;
        }

		@Override
        protected void onPostExecute(List<String> result) {	        
			mMediaFilesListAdapter.notifyDataSetChanged();
			if (result != null && result.size() > 0){
				mLookbackTextViewEmpty.setVisibility(View.INVISIBLE);
			}else{
				mLookbackTextViewEmpty.setVisibility(View.VISIBLE);
			}
        }
		
	}
	
	private static final String TAG = "DBG";
	private void LOGD(String msg){
		Log.d(TAG, msg);
	}
	
	private Bitmap drawLogo(Bitmap bg, Bitmap logo) {
		if (!bg.isMutable()) {
			bg = bg.copy(Bitmap.Config.ARGB_8888, true);
		}

		Paint p = new Paint();
		p.setStyle(Paint.Style.STROKE);
		p.setAlpha(200);
		
		Canvas canvas = new Canvas(bg);
		//Rect rect = new Rect(0, (bg.getHeight() - logo.getHeight()), bg.getWidth(), bg.getHeight());
		Rect rect = new Rect((bg.getWidth()-logo.getWidth())/2, (bg.getHeight()-logo.getHeight())/2, (bg.getWidth()+logo.getWidth())/2, (bg.getHeight()+logo.getHeight())/2);
		// canvas.drawBitmap(logo, null, rect,p);
		canvas.drawBitmap(logo, null, rect, null);
		canvas.save(Canvas.ALL_SAVE_FLAG);
		canvas.restore();
		return bg;
	}

}
