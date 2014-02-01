package com.example.kbe_wallpaper.livewallpaper;



import java.io.File;
import java.io.FileInputStream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.service.wallpaper.WallpaperService;
import android.util.Log;
import android.view.GestureDetector;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class MyWallpaperService extends WallpaperService {

	@Override
	public Engine onCreateEngine() {
		return new wallpaperEngine();
	}
	
	
	private class wallpaperEngine extends Engine{
		
		private final Handler handler = new Handler();
		private Runnable slideRunner;
		private Runnable effectRunner;
		public BroadcastReceiver receiver;
		
		private Bitmap currentPhoto;
		private Bitmap[] photos;
		
		private int indexPhoto = 0;
		private int numOfPhotos;
		
		private SharedPreferences prefs;
		private int millisecondsBetweenSlides;
		private boolean touchOn;
		
		private Rect src;
		private RectF dst;
		private boolean visible = true;
		private boolean firstRun = true;
		private GestureDetector flingDetector;
		private String storagePath = "";
		private int nth = 1;
		private int randomDecider;
		
		private static final int NEXT_SLIDE = 2;
		private static final int PREVIOUS_SLIDE = 0;
		private static final int millisecondsBetweenEffects = 50;
		
		public wallpaperEngine(){
			prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
			flingDetector = new GestureDetector(getApplicationContext(), new SimpleOnGestureListener(){
				public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
					if(touchOn && visible){
						float x1 = e1.getX();
						float x2 = e2.getX();
						float difference = x1 - x2;
						nth = 1;
						if(difference > 0){
							changeSlide(NEXT_SLIDE);
						}
						else{
							changeSlide(PREVIOUS_SLIDE);
						}
					}
					return true;
					
				}
			});
			PreferenceManager.setDefaultValues(getApplicationContext(), com.example.kbe_wallpaper.R.xml.prefs, false);
			millisecondsBetweenSlides = Integer.valueOf(prefs.getString("timeBetweenSlides","5")) * 1000;
			touchOn = prefs.getBoolean("touch", true);
			storagePath = prefs.getString("storagePath", "");
			
			receiver = new BroadcastReceiver(){
				public void onReceive(Context context, Intent intent){
					String action = intent.getAction();
					if(action.equals("PhotosLoadedinSD")){
						storagePath = intent.getStringExtra(com.example.kbe_wallpaper.album.PhotoLoader.IMAGES_PATH);
						prefs.edit().putString("storagePath", storagePath);
						File storageDir = new File(storagePath);
						numOfPhotos = storageDir.listFiles().length - 1;
						slideRunner = new Runnable(){
							public void run(){
								changeSlide(NEXT_SLIDE);
							}
						};
						effectRunner = new Runnable(){
							public void run(){
								effect();
							}
						};
						startAsync(indexPhoto);
					}
				}
			};
			IntentFilter filter = new IntentFilter(com.example.kbe_wallpaper.album.PhotoLoader.DOWNLOAD_COMPLETE);
			filter.addAction("PhotosLoadedinSD");
			registerReceiver(receiver, filter);

			
		}
		@Override
		public void onTouchEvent(MotionEvent event) {
			if(flingDetector.onTouchEvent(event) != true){
				super.onTouchEvent(event);
			}
		}
		

		public void onVisibilityChanged(boolean visible){
			this.visible = visible;
			if(visible){
				if(isPreview() && firstRun){
					startAsync(indexPhoto);
				}
				handler.post(slideRunner);
			}
			else{
				handler.removeCallbacks(slideRunner);
			}
		}
		

		public void onSurfaceDestroyed(SurfaceHolder holder){
			super.onSurfaceDestroyed(holder);
			this.visible=false;
			handler.removeCallbacks(slideRunner);
		}
		
		
	
		private void changeSlide(int value){
			if(firstRun){
				currentPhoto = photos[0];
			}
			else{
				currentPhoto = photos[value];
			}
			millisecondsBetweenSlides = Integer.valueOf(prefs.getString("timeBetweenSlides","5")) * 1000;
			touchOn = prefs.getBoolean("touch", true);
			if(value == NEXT_SLIDE){
				indexPhoto++;
				if(indexPhoto > numOfPhotos){
					indexPhoto = 0;
				}
			}
			else{
				indexPhoto--;
				if(indexPhoto < 0){
					indexPhoto = numOfPhotos;
				}
			}
			startAsync(indexPhoto);
			randomDecider = (int)(Math.round(Math.random() * 3));
			handler.removeCallbacks(slideRunner);
			if(visible){
				handler.postDelayed(effectRunner, millisecondsBetweenEffects);
			}
			
		}
		
		private void effect(){
			SurfaceHolder holder = getSurfaceHolder();
			Canvas canvas = null;
			try{
				if(visible){
					canvas = holder.lockCanvas();
					canvas.drawColor(Color.BLACK);
					int width = currentPhoto.getWidth() / 2 + (nth * currentPhoto.getWidth())/(2* (millisecondsBetweenSlides/millisecondsBetweenEffects));
					int height = currentPhoto.getHeight() / 2 + (nth * currentPhoto.getHeight())/(2* (millisecondsBetweenSlides/millisecondsBetweenEffects));
					
					
					int startX, endX, startY, endY;
					switch(randomDecider){
					case 0: startX = 0;
							startY = 0;
							endX = width;
							endY = height;
							break;
					case 1: endX = currentPhoto.getWidth();
							startX = endX - width;
							startY = 0;
							endY = height;
							break;
					case 2: startX = 0;
							endX = width;
							endY = currentPhoto.getHeight();
							startY = endY - height;
							break;
					case 3: endX = currentPhoto.getWidth();
							startX = endX - width;
							endY = currentPhoto.getHeight();
							startY = endY - height;
							break;
					default: startX = endX = startY = endY = 0;
					}
					int dWidth = currentPhoto.getWidth();
					int dHeight = currentPhoto.getHeight();
					int centerWidth = canvas.getWidth() / 2;
					int centerHeight = canvas.getHeight() / 2;
					int dStartX = centerWidth - dWidth / 2;
					int dEndX = centerWidth + dWidth / 2;
					int dStartY = centerHeight - dHeight / 2;
					int dEndY = centerHeight + dHeight / 2;
					src = new Rect(startX,startY, endX, endY);
					dst = new RectF(dStartX,dStartY, dEndX, dEndY);
					canvas.drawBitmap(currentPhoto, src, dst, null);
					nth++;
				}
			}
			finally{
				if(canvas != null){
					holder.unlockCanvasAndPost(canvas);
				}
			}
			handler.removeCallbacks(effectRunner);
			
			if(nth > millisecondsBetweenSlides / millisecondsBetweenEffects){
				nth = 1;
				handler.post(slideRunner);
			}
			else{
				handler.postDelayed(effectRunner, millisecondsBetweenEffects);
			}
		}
		
		@Override
		public void onDestroy() {
			unregisterReceiver(receiver);
			super.onDestroy();
		}
		
		protected void startAsync(int index){
			new LoadImagesFromSD(this).execute(index);
		}
		protected void initPhotos(Bitmap[] listOfPhotos){
			photos = listOfPhotos;
			if(firstRun){
				handler.postDelayed(slideRunner, millisecondsBetweenSlides);
			}
			firstRun = false;
		}
	}
	
	
	
	protected Bitmap[] loadImages(int i){
		File storageDir = Environment.getExternalStorageDirectory().getAbsoluteFile();
		String directory = storageDir.getPath();
		directory = directory + File.separator + "kbe_wallpaper_album";
		storageDir = new File(directory);
		File[] listOfFiles = storageDir.listFiles();
		Bitmap[] listOfPhotos = new Bitmap[3];
		int i2 = i;
		int i1 = i - 1;
		if(i1 < 0){
			i1 = listOfFiles.length - 1;
		}
		int i3 = i + 1;
		if(i3 > listOfFiles.length - 1){
			i3 = 0;
		}
		int[] indices = {i1, i2, i3};
		int k = 0;
		for(int index: indices){
			try{
				FileInputStream fIn = new FileInputStream(listOfFiles[index]);
				Bitmap image = BitmapFactory.decodeStream(fIn);
				listOfPhotos[k] = image;
				fIn.close();
				k++;
			}
			catch(Exception e){
				Log.e("tag ", e.toString());
			}
		}
		
		return listOfPhotos;
	}
	
	
	private class LoadImagesFromSD extends AsyncTask<Integer, Void, Bitmap[]>{
		public Engine engine;
		public LoadImagesFromSD(Engine engine) {
			this.engine = engine;
		}

		protected Bitmap[] doInBackground(Integer...params){
			return loadImages(params[0]);
		}
		
		protected void onPostExecute(Bitmap[] listOfPhotos){
			((wallpaperEngine) engine).initPhotos(listOfPhotos);
		}
	}
}
