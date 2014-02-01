package com.example.kbe_wallpaper.album;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.model.GraphUser;
import com.facebook.widget.LoginButton;

public class AlbumListActivity extends Activity {

	static final String IMAGE_ADDRESS = "com.example.kbe_wallpaper.album.AlbumListActivity";
	
	private ListView listView;
	private String authToken;
	private String address = "https://graph.facebook.com/me/albums?access_token=";
	private JSONObject jsonOAlbums;
	private JSONArray jsonAAlbums;
	private LoginButton fbButton;
	private String[] albumIds;
	private Session currentSession;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(com.example.kbe_wallpaper.R.layout.activity_album_list);
		
		fbButton = (LoginButton)findViewById(com.example.kbe_wallpaper.R.id.login);
		ArrayList<String> permissions = new ArrayList<String>();
		permissions.add("user_photos");
		fbButton.setReadPermissions(permissions);
		listView = (ListView)findViewById(com.example.kbe_wallpaper.R.id.list);
		
		currentSession = Session.getActiveSession();
		if(currentSession != null){
			fbButton.setVisibility(View.GONE);
			authToken = currentSession.getAccessToken();
		}
		if (authToken == null){
			fbButton.setVisibility(View.VISIBLE);
			fbButton.setSessionStatusCallback(new Session.StatusCallback() {
				public void call(Session session, SessionState state, Exception exception){
					if (session.isOpened()) {
			          Request.executeMeRequestAsync(session, new Request.GraphUserCallback() {
			            public void onCompleted(GraphUser user, Response response) {
			            	fbButton.setVisibility(View.GONE);
			            }
			          });
			          authToken = session.getAccessToken();
			        }
			       
				}
			});
		}
		address = address + authToken;
        new ServerAccess().execute(address);
	}
	

	public void onActivityResult(int requestCode, int resultCode, Intent data){
		super.onActivityResult(requestCode, resultCode, data);
		Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
	}
	
	private JSONObject loadAlbumFromNetwork(String serverAddress){
		HttpClient client = new DefaultHttpClient();
		try{
			HttpGet request = new HttpGet(serverAddress);
			HttpResponse response = client.execute(request);
			String result = "";
			HttpEntity responseEntity = response.getEntity();
			InputStream stream = responseEntity.getContent();
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			StringBuilder str = new StringBuilder();
			String line = null;
			while((line = reader.readLine())!= null){
				str.append(line);
			}
			stream.close();
			result = str.toString();
			jsonOAlbums= new JSONObject(result);
			return jsonOAlbums;
			
		}
		catch(Exception e){
			Log.e("tag1",e.toString());
		}
		return null;
	}
	


	private class ServerAccess extends AsyncTask<String, Void, JSONObject>{
		protected JSONObject doInBackground(String...params){
			return loadAlbumFromNetwork(params[0]);
		}
		
		protected void onPostExecute(JSONObject code){
			String[] albumNames = null;
			albumIds = null;
			try{
				jsonAAlbums = code.getJSONArray("data");
				int numKeys = jsonAAlbums.length();
				albumNames = new String[numKeys];
				albumIds = new String[numKeys];
				for(int i = 0; i < numKeys; i++){
					JSONObject albumInfo = jsonAAlbums.getJSONObject(i);
					String name = albumInfo.getString("name");
					String id = albumInfo.getString("id");
					albumNames[i] = name;
					albumIds[i] = id;
				}
			}
			catch(Exception e){
				Log.e("tag2", e.toString());
			}
			
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(AlbumListActivity.this, android.R.layout.simple_list_item_1, albumNames);
			listView.setAdapter(adapter);
			listView.setVisibility(View.VISIBLE);
			listView.setOnItemClickListener(new OnItemClickListener(){
				public void onItemClick(AdapterView<?>parent, View view, int position, long id){
					int itemPosition = position;
					String albumId = albumIds[itemPosition];
					address = "https://graph.facebook.com/" + albumId + "/photos?access_token=" + authToken;
					Intent intent = new Intent(getApplicationContext(), PhotoLoader.class);
					intent.putExtra(IMAGE_ADDRESS, address);
					Toast.makeText(getApplicationContext(), "Downloading photos from album...", Toast.LENGTH_SHORT).show();
					startService(intent);
				}
			});
			
			
		}
	}

	

}
