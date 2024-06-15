package com.levkopo.requestlib;
import android.os.AsyncTask;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.BufferedReader;
import android.util.Log;

public class Request extends AsyncTask <String, String, String>
{
	private RequestListener request;

	public Request(RequestListener request){
		this.request = request;
	}

	@Override
	protected void onPostExecute(String res) {
		super.onPostExecute(res);
		if(res!=null)
			Log.wtf("Nashi", res);

		try {
			JSONObject result = new JSONObject(res);
			request.onResponse(result);
		} catch (Exception e) {
			request.onError(res);
			e.printStackTrace();
		}
	}

	@Override
	protected String doInBackground(String[] p1) {
		URL url;
		HttpURLConnection urlConnection = null;
		try {
			url = new URL(p1[0]);

			urlConnection = (HttpURLConnection) url
                .openConnection();

			InputStream in = urlConnection.getErrorStream()==null ?
				urlConnection.getInputStream() : urlConnection.getErrorStream();
			InputStreamReader isw = new InputStreamReader(in);

			BufferedReader reader = new BufferedReader(isw);
			StringBuilder sb = new StringBuilder();

			String line = null;
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
			return sb.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return e.getMessage();
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}    
		}
	}

}
