package com.example.kbe_wallpaper.livewallpaper;

import java.io.File;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.widget.Toast;

import com.example.kbe_wallpaper.R;


public class Preferences extends PreferenceActivity {
	public static final String ADDRESS_URL = "com.example.kbe_wallpaper.livewallpaper";
	private SharedPreferences prefs;
	static File storageDir;
	private Preference button, timePreference;
	static Bitmap[] photos;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.prefs);
		prefs = getSharedPreferences("com.example.kbe_wallpaper",Context.MODE_PRIVATE);
		
		button = (Preference)findPreference("button");
		button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener(){
			public boolean onPreferenceClick(Preference arg0){
				Intent intent = new Intent(getApplicationContext(), com.example.kbe_wallpaper.album.AlbumListActivity.class);
				startActivity(intent);
				finish();
				return true;
			}
		});
		timePreference = getPreferenceScreen().findPreference("timeBetweenSlides");
		timePreference.setOnPreferenceChangeListener(numberCheckListener);
	}
	
	
	Preference.OnPreferenceChangeListener numberCheckListener = new OnPreferenceChangeListener(){
		public boolean onPreferenceChange(Preference preference, Object newValue){
			if(newValue != null && newValue.toString().length() > 0 && newValue.toString().matches("\\d*")){
				prefs.edit().putString("timeBetweenSlides", newValue.toString());
				return true;
			}
			Toast.makeText(getApplicationContext(), "Invalid Input", Toast.LENGTH_SHORT).show();
			return false;
		}
	};
	
}
