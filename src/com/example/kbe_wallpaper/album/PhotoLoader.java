package com.example.kbe_wallpaper.album;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.IntentService;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class PhotoLoader extends IntentService {
	private JSONObject jsonOPhotos;
	private JSONArray jsonAPhotos;
	File storageDir;
	private FileOutputStream fOut;
	private Intent receivedIntent;
	Handler handler;
	public final static String DOWNLOAD_COMPLETE="com.example.wallpaper.album.PhotoLoader.DOWNLOAD_COMPLETE";
	public final static String IMAGES_PATH = "com.example.kbe_wallpaper.album.PhotoLoader";

	public PhotoLoader() {
		super("PhotoLoader");
		handler = new Handler();
		// TODO Auto-generated constructor stub
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		// TODO Auto-generated method stub
		receivedIntent = intent;
		String address = receivedIntent.getStringExtra(com.example.kbe_wallpaper.album.AlbumListActivity.IMAGE_ADDRESS);
		jsonOPhotos = loadAllPhotosInAlbum(address);
		String[] photoSources = null;
		storageDir = Environment.getExternalStorageDirectory().getAbsoluteFile();
		String directory = storageDir.getPath();
		directory = directory + File.separator + "kbe_wallpaper_album";
		storageDir = new File(directory);
		boolean newFolder = storageDir.mkdirs();
		if(!newFolder){
			File[] existingFiles = storageDir.listFiles();
			for(File file: existingFiles){
				file.delete();
				storageDir = new File(directory);
			}
		}
		try{
			jsonAPhotos = jsonOPhotos.getJSONArray("data");
			int numKeys = jsonAPhotos.length();
			photoSources = new String[numKeys];
			for(int i = 0; i < numKeys; i++){
				JSONObject photoInfo = jsonAPhotos.getJSONObject(i);
				String source = photoInfo.getString("source");
				photoSources[i] = source;
			}
			
			
		}
		catch(Exception e){
			Log.e("tag2", e.toString());
		}
		
		int i = 1;
		for(String photoAddress: photoSources){
			try{
				File file = new File(storageDir + File.separator + "Picture" + i + ".jpg");
				fOut = new FileOutputStream(file);
				URL imageURL = new URL(photoAddress);
				Bitmap image = BitmapFactory.decodeStream(imageURL.openStream());
				image.compress(Bitmap.CompressFormat.JPEG, 50, fOut);
				fOut.flush();
				fOut.close();
				image.recycle();
				image = null;
				i++;

			}
			catch(IOException e){
				Log.e("tag3", e.toString());
			}
		}
		try{
			fOut.close();
		}
		catch(IOException e){
			Log.e("tag4", e.toString());
		}
		handler.post(new Runnable(){
			public void run(){
				Toast.makeText(getApplicationContext(), "Donwload finished!", Toast.LENGTH_SHORT).show();
				Intent intent = new Intent(DOWNLOAD_COMPLETE);
				intent.setAction("PhotosLoadedinSD");
				intent.putExtra(IMAGES_PATH, storageDir.toString());
				getApplicationContext().sendBroadcast(intent);
			}
		});
		

	}
	
	private JSONObject loadAllPhotosInAlbum(String serverAddress){
		HttpClient client = new DefaultHttpClient();
		try{
			HttpGet request = new HttpGet(serverAddress);
			HttpResponse response = client.execute(request);
			String result = "";
			HttpEntity responseEntity = response.getEntity();
			InputStream input = responseEntity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			StringBuilder str = new StringBuilder();
			String line = null;
			while((line = reader.readLine())!= null){
				str.append(line);
			}
			input.close();
			result = str.toString();
			jsonOPhotos= new JSONObject(result);
			return jsonOPhotos;
			
		}
		catch(Exception e){
			Log.e("tag1",e.toString());
		}
		return null;
	}

}
