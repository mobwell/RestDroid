package net.eunainter.r2std2oid;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.concurrent.TimeUnit;

public class R2stD2oidNew extends AsyncTask<RequestR2D2, Void, ResponseR2D2> {
	HandleObservers observers;
	Timer mTimeout;

	int mRequestTimeout	= 5000;
	final String TAG = "R2stD2oid";

	private Object mLock = new Object();

	private static DefaultHttpClient httpClient;
	private static CookieStore mCookie = null;
	private static HttpContext localContext;

	private static HandleRequests handlerReq;



	public R2stD2oidNew() {
		observers = new HandleObservers();
		handlerReq = new HandleRequests();

/*		mTimeout =new Timer();
		mTimeout.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
//				Log.i("R2D2: TIMER", "Time to timeout: " + mRequestTimeout);
				if (mRequestTimeout > 0)
					mRequestTimeout -= 1000;
				else {

					pauseExecution();
					this.cancel();
				}
			}
		}, 1000, 1000);*/
	}

	public void addObserver(RestObserver rObserver) {
		this.observers.addObserver(rObserver);
	}

	public void pauseExecution() {
		Log.i("R2D2: TIMER", "Timed out!");
		observers.timeoutObservers();
		this.cancel(true);
	}

	@Override
	protected ResponseR2D2 doInBackground(RequestR2D2... requests) {

		try {
			Log.i("R2D2", "Sending req to: " + requests[0].getUrl());

            ResponseR2D2 resp = downloadUrl(requests[0]);
			return resp;
		} catch (IOException e) {
			Log.e("ERREQUEST", e.getClass().getName());
			return  new ResponseR2D2(ResponseR2D2.STATUS_BAD_REQUEST, "Error processing request");
		}
	}

	// onPostExecute displays the results of the AsyncTask.
	@Override
	protected void onPostExecute(ResponseR2D2 result) {
		//        textView.setText(result);
		//		System.out.println("El result: " + result);
		try {
			this.observers.notifyObservers(result);
		}
		catch (Exception e) {
			Log.e("RSTD2DERR", (e.getLocalizedMessage() == null) ? e.getClass().getName() : (e.getLocalizedMessage()));
		}
	}

	public ResponseR2D2 downloadUrl(RequestR2D2 myRequest) throws IOException {
		observers.progressObservers();

		InputStream is = null;
		// Only display the first 500 characters of the retrieved
		// web page content.
		String result ="";
		int len = 33000;

		int idRequest = myRequest.getId();


		int status = 200;
		String messageSend = "";

		try {		
			HttpResponse httpResponse = null;

			/**
			 * Always gives priority to hardcoded string entity
			 */
			JSONObject jsonObj = (myRequest.getJson() == null || myRequest.getJson().length() == 0) 
					? myRequest.createJson()
							: myRequest.getJson();



					//					(myRequest.getJson() == null ||
					//					myRequest.getJson().length() == 0) ?  myRequest.createJson() : myRequest.getJson();
					//			String st2send = myRequest.getStringEntity();

					if (myRequest.getPublishMethod() == RequestR2D2.POST) {
						URL url = new URL(myRequest.getUrl());
						List<NameValuePair> params = myRequest.getNameValueJson();
						/*
						params.put("name", "Freddie the Fish");
						params.put("email", "fishie@seamail.example.com");
						params.put("reply_to_thread", 10394);
						params.put("message", "Shark attacks in Botany Bay have gotten out of control. We need more defensive dolphins to protect the schools here, but Mayor Porpoise is too busy stuffing his snout with lobsters. He's so shellfish.");
*/
						StringBuilder postData = new StringBuilder();
						for (NameValuePair param : params) {
							if (postData.length() != 0) postData.append('&');
							postData.append(URLEncoder.encode(param.getName(), "UTF-8"));
							postData.append('=');
							postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
						}
						byte[] postDataBytes = postData.toString().getBytes("UTF-8");

						HttpURLConnection conn = (HttpURLConnection)url.openConnection();
						conn.setRequestMethod("POST");
						conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
						conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
						conn.setDoOutput(true);
						conn.getOutputStream().write(postDataBytes);

						Reader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
						StringBuilder stb = new StringBuilder("");
						for ( int c = in.read(); c != -1; c = in.read() )
							stb.append((char) c);

						Log.i("RestDroid", stb.toString());
						messageSend = stb.toString();

						// GET
					} else {
						/*
						 * Builds the url to send
						 */
						Uri.Builder bld = Uri.parse(myRequest.getUrl()).buildUpon();
						List<NameValuePair> params = myRequest.getNameValueJson();
						for (int i=0; i < params.size(); i++)
							bld.appendQueryParameter(params.get(i).getName(), params.get(i).getValue());
						String urltosend = bld.toString();

						if (urltosend == null)
							return null;

						HttpGet request = new HttpGet(urltosend);

						httpResponse = R2stD2oidNew.getHttpClient().execute(request, localContext);

						try {
							//				HttpResponse
							HttpEntity httpEntity = httpResponse.getEntity();

							if (httpEntity != null) {
								//						InputStream ist = httpEntity.getContent();

								messageSend = EntityUtils.toString(httpEntity, HTTP.UTF_8);

								//						messageSend = readIt(ist);

								//					Log.i(TAG, "Result: " + result);
							}
						} catch (ClientProtocolException e) {
							Log.e(TAG, "ClientProtocolException: " + e);
							status = ResponseR2D2.STATUS_CLIENTPROTOCOL;
							messageSend = e.getLocalizedMessage();
						} catch (IOException e) {
							Log.e(TAG, "IOException: " + e);

							status = ResponseR2D2.STATUS_IOEXCEPTION;
							messageSend = e.getLocalizedMessage();
						}
						catch (Exception e) {
							//				if (status =)
							Log.e(TAG, "General Exception: " + e);

							status = ResponseR2D2.STATUS_GENERAL_ERROR;
							messageSend = e.getLocalizedMessage();
						}
					}

					if (messageSend != null && !messageSend.isEmpty()) {
						ResponseR2D2 response = new ResponseR2D2(status, messageSend);
						response.setId(idRequest);

						return response;
					}
		} finally {
			if (httpClient != null) {
				ClientConnectionManager cmanager = R2stD2oidNew.getHttpClient().getConnectionManager();

				if (cmanager != null)
					cmanager.closeIdleConnections(3,TimeUnit.SECONDS);
			}
		}

		return null;
	}

	/** 
	 * Reads an InputStream and converts it to a String.
	 * (Deprecated)
	 * @param stream
	 * @return
	 * @throws IOException
	 * @throws UnsupportedEncodingException
	 */
	public String readIt(InputStream stream) throws IOException, UnsupportedEncodingException {
		Reader reader = null;
		reader = new InputStreamReader(stream, "UTF-8");
		char[] buffer = new char[33000];
		reader.read(buffer);
		return new String(buffer);
	}

	public static DefaultHttpClient getHttpClient() {
		/*
		if (httpClient == null) {
			httpClient = new DefaultHttpClient();
			if (mCookie == null) {

				// Create local HTTP context
				localContext = new BasicHttpContext();
				// Bind custom cookie store to the local context
				localContext.setAttribute(ClientContext.COOKIE_STORE, mCookie);


				mCookie = httpClient.getCookieStore();
				httpClient.setCookieStore(mCookie);
			}
//			httpClient.setCookieStore(mCookie);

		}*/
		//		if (httpClient == null) {
		httpClient = new DefaultHttpClient();
		httpClient.getParams().setParameter(CoreProtocolPNames.HTTP_ELEMENT_CHARSET,
				"utf-8");

		if (mCookie == null) {

			// Create local HTTP context
			localContext = new BasicHttpContext();
			// Bind custom cookie store to the local context
			localContext.setAttribute(ClientContext.COOKIE_STORE, mCookie);


			mCookie = httpClient.getCookieStore();
		}
		httpClient.setCookieStore(mCookie);

		//		}
		return httpClient;
	}

	public int getRequestTimeout() {
		return mRequestTimeout;
	}

	public void setRequestTimeout(int mRequestTimeout) {
		this.mRequestTimeout = mRequestTimeout;
	}
	
	
}
