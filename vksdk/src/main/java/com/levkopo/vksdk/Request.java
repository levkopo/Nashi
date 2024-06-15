package com.levkopo.vksdk;
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
	private VKSdk.RequestListener request;
	
	public Request(VKSdk.RequestListener request){
		this.request = request;
	}

	@Override
	protected void onPostExecute(String res) {
		super.onPostExecute(res);
		if(res!=null)
		Log.wtf("Nashi", res);
		
		try {
			JSONObject result = new JSONObject(res);
			if (result != null && !result.isNull("response")) {
				VKSdk.VKResponse response = new VKSdk.VKResponse();
				response.json = result;
				request.onComplete(response);
			}else if(result!=null&&!result.isNull("error")&&result.get("error") instanceof JSONObject){
				request.onError(VKError.bind(result.optJSONObject("error")));
			}else if(result!=null&&!result.isNull("error")&&result.get("error") instanceof String){
				VKError err = new VKError();
				err.error_code = -1;
				err.error_msg = result.getString("error");
				err.json = result;
				request.onError(err);
			}else if(result!=null){
				VKSdk.VKResponse response = new VKSdk.VKResponse();
				response.json = result;
				request.onComplete(response);
			}else{
				request.onError(null);
			}
		} catch (Exception e) {
			request.onError(null);
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
		} finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}    
		}
		return null;
	}
	
}
