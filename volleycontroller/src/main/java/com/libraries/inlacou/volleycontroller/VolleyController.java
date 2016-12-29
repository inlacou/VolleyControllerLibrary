package com.libraries.inlacou.volleycontroller;

import android.app.Application;
import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by inlacou on 25/11/14.
 */
public class VolleyController {

	public static final String JSON_POST_UPDATE_ACCESS_TOKEN = "network_logic_json_post_update_access_token";
	private static final String DEBUG_TAG = VolleyController.class.getName();

	private static ArrayList<InternetCall> temporaryCallQueue = new ArrayList<InternetCall>();
	private static boolean updatingToken = false;
	private static VolleyController ourInstance = new VolleyController();
	private Context context;
	private RequestQueue mRequestQueue, mSecondaryRequestQueue;
	private LogicCallbacks logicCallbacks;
	private String errorMessage;
	private ArrayList<InternetCall.Interceptor> interceptors;

	public static VolleyController getInstance() {
		return ourInstance;
	}

	private VolleyController() {
	}

	public void init(Application application, LogicCallbacks logicCallbacks){
		errorMessage = application.getString(R.string.network_error);
		this.logicCallbacks = logicCallbacks;
		this.context = application;
		interceptors = new ArrayList<>();
		if (mRequestQueue == null) {
			//InputStream keystore = getResources().openRawResource(R.raw.boletus); //For SSH

			mRequestQueue = Volley.newRequestQueue(application
					//, new ExtHttpClientStack(new SslHttpClient(keystore, "ss64kdn4", 443)) //For SSH
			);
		}
		if (mSecondaryRequestQueue == null) {
			//InputStream keystore = getResources().openRawResource(R.raw.boletus); //For SSH

			mSecondaryRequestQueue = Volley.newRequestQueue(application
					//, new ExtHttpClientStack(new SslHttpClient(keystore, "ss64kdn4", 443)) //For SSH
			);
		}
	}

	/**
	 *
	 * @param interceptor
	 */
	public void addInterceptor(InternetCall.Interceptor interceptor){
		interceptors.add(interceptor);
	}

	public RequestQueue getRequestQueue(){
		// lazy initialize the request queue, the queue instance will be
		// created when it is accessed for the first time
		return mRequestQueue;
	}


	public RequestQueue getSecondaryRequestQueue(){
		// lazy initialize the request queue, the queue instance will be
		// created when it is accessed for the first time
		return mSecondaryRequestQueue;
	}

	public String convertInputStreamToString(InputStream inputStream) throws IOException {
		BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
		String line = " ";
		String result = " ";
		while ((line= bufferedReader.readLine())!=null){
			result += line;
		}
		inputStream.close();
		return result;
	}

	private String methodToString(Integer method){
		if(method==null){
			return "method-null";
		}
		switch (method){
			case Request.Method.PUT:
				return "put";
			case Request.Method.GET:
				return "get";
			case Request.Method.DELETE:
				return "delete";
			case Request.Method.POST:
				return "post";
			default:
				return "method-unknown";
		}
	}

	private void removeFromTemporaryList(String code){
		int i = temporaryCallQueue.size();
		for (int c = 0; c<i; c++){
			if(temporaryCallQueue.get(c).getCode().equalsIgnoreCase(code)){
				temporaryCallQueue.remove(c);
				return;
			}
		}
	}

	public void onCall(final InternetCall iCall){
		onCall(iCall, false);
	}

	private void onCall(final InternetCall iCall, boolean primaryRequestQueue){
		if(iCall==null) {
			return;
		}
		iCall.addInterceptors(interceptors);
		Log.d(DEBUG_TAG + ".onCall." + iCall.getMethod(), "Request para la " + (primaryRequestQueue ? "primera" : "segunda") + " requestQueue creada con codigo: " + iCall.getCode());
		Log.d(DEBUG_TAG + ".onCall."+iCall.getMethod()+"", "Making "+iCall.getMethod()+" call to url: " + iCall.getUrl());
		logMap(iCall.getHeaders(), "header", iCall.getMethod().toString());
		logMap(iCall.getParams(), "params", iCall.getMethod().toString());
		Log.d(DEBUG_TAG+".onCall."+iCall.getMethod()+"", "Rawbody: "+iCall.getRawBody());

		RequestQueue mRequestQueue;
		if(primaryRequestQueue) {
			Log.d(DEBUG_TAG, "primaryRequestQueue");
			mRequestQueue = getRequestQueue();
		}else{
			Log.d(DEBUG_TAG, "secondaryRequestQueue");
			mRequestQueue = this.getSecondaryRequestQueue();
		}
		if(iCall.getCode()!=null && !iCall.getCode().equalsIgnoreCase(JSON_POST_UPDATE_ACCESS_TOKEN)) {
			temporaryCallQueue.add(iCall);
		}
		mRequestQueue.add(iCall.build(context, new Response.Listener<CustomResponse>() {
			@Override
			public void onResponse(CustomResponse s) {
				VolleyController.this.onResponse(s, iCall.getCallbacks(), iCall.getCode(), iCall.getMethod());
			}
		}, new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError volleyError) {
				VolleyController.this.onResponseError(volleyError, iCall.getCallbacks(), iCall.getCode(), iCall.getMethod().toString());
			}
		}));
	}

	private void logMap(Map<String, String> map, String type, String method) {
		Log.d(DEBUG_TAG+".onCall."+method+"", "Map(" + type + ") = " + map);
		if(map!=null) {
			for (String s : map.keySet()) {
				Log.d(DEBUG_TAG + ".onCall." + method + "", type + " parameter " + s + ": " + map.get(s));
			}
		}
	}

	private void onResponseFinal(CustomResponse response, ArrayList<IOCallbacks> ioCallbacks, String code, InternetCall.Method method){
		Log.d(DEBUG_TAG+"."+method+".onStringResponse", "Code: " + code);
		Log.d(DEBUG_TAG + "." + method + ".onStringResponse", "Method: " + method);
		Log.d(DEBUG_TAG + "." + method + ".onStringResponse", "CustomResponse: " + response);
		if(ioCallbacks!=null) {
			for (int i=0; i<ioCallbacks.size(); i++){
				if(ioCallbacks.get(i)!=null) ioCallbacks.get(i).onResponse(response, code);
			}
			removeFromTemporaryList(code);
		}
	}

	private void onResponse(CustomResponse response, ArrayList<IOCallbacks> ioCallbacks, String code, InternetCall.Method method){
		Log.d(DEBUG_TAG+"."+method+".onStringResponse", "Code: " + code);
		Log.d(DEBUG_TAG+"."+method+".onStringResponse", "StatusCode: " + code);
		Log.d(DEBUG_TAG + "." + method + ".onStringResponse", "CustomResponse: " + response);
		if(ioCallbacks!=null) {
			if(code!=null && code.equalsIgnoreCase(JSON_POST_UPDATE_ACCESS_TOKEN)){
				Log.d(DEBUG_TAG+"."+method+".onJsonResponse", "Recibida la respuesta al codigo " + JSON_POST_UPDATE_ACCESS_TOKEN +
						", updating tokens. | " + response);
				//Save old authToken
				String oldAccessToken = logicCallbacks.getAuthToken();
				//Read answer
				try {
					JSONObject jsonObject = new JSONObject(response.getData());
					//Save new tokens
					logicCallbacks.setTokens(jsonObject);
				} catch (JSONException e) {
					e.printStackTrace();
				}
				//Get new authToken
				String accessToken = logicCallbacks.getAuthToken();
				Log.d(DEBUG_TAG+"."+method+".onJsonResponse", "Continuando llamadas almacenadas. Numero: " + temporaryCallQueue.size());

				for(int i = 0; i<temporaryCallQueue.size(); i++){
					doCall(temporaryCallQueue.get(i), oldAccessToken, accessToken, method.toString());
				}
				updatingToken=false;
				getRequestQueue().start();
			} else {
				onResponseFinal(response, ioCallbacks, code, method);
			}
		}
	}

	private void doCall(final InternetCall iCall, String oldAccessToken, String accessToken, final String metodo){
		RequestQueue rq = getRequestQueue();
		rq.add(iCall.replaceAccessToken(oldAccessToken, accessToken)
				.build(context, new Response.Listener<CustomResponse>() {
					@Override
					public void onResponse(CustomResponse s) {
						VolleyController.this.onResponseFinal(s, iCall.getCallbacks(), iCall.getCode(), iCall.getMethod());
					}
				}, new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						VolleyController.this.onResponseError(volleyError, iCall.getCallbacks(), iCall.getCode(), metodo);
					}
				}));
	}

	private void onResponseError(VolleyError volleyError, ArrayList<IOCallbacks> ioCallbacks, String code, String metodo){
		if(volleyError.networkResponse!=null){
			Log.d(DEBUG_TAG+"."+metodo+".onResponseError", "StatusCode: "+volleyError.networkResponse.statusCode);
			try {
				Log.d(DEBUG_TAG + "."+metodo+".onResponseError", "Message: " + new String(volleyError.networkResponse.data, "UTF-8"));
				Log.d(DEBUG_TAG + "."+metodo+".onResponseError", "StatusCode: " + volleyError.networkResponse.statusCode);
				if(volleyError.networkResponse.statusCode==401 &&
						(new String(volleyError.networkResponse.data, "UTF-8").contains("The access token provided has expired.")
								|| new String(volleyError.networkResponse.data, "UTF-8").contains("The access token provided is invalid.")
								|| new String(volleyError.networkResponse.data, "UTF-8").contains("UnauthorizedError: jwt expired")
								|| (logicCallbacks.getAuthTokenExpiredMessage()!=null && !logicCallbacks.getAuthTokenExpiredMessage().isEmpty() && new String(volleyError.networkResponse.data, "UTF-8").contains(logicCallbacks.getAuthTokenExpiredMessage())))
						) {
					Log.d(DEBUG_TAG + "."+metodo+".onResponseError", "Detectado un error 401, token caducado.");
					retry(code, ioCallbacks);
					return;
				}if(volleyError.networkResponse.statusCode==400) {
					Log.v(DEBUG_TAG + "."+metodo+".onResponseError", "Detectado un error 400, refresh-token posiblemente caducado.");
					try{
						JSONObject jsonObject = new JSONObject(getMessage(volleyError));
						if(logicCallbacks.getRefreshTokenInvalidMessage()!=null && !logicCallbacks.getRefreshTokenInvalidMessage().isEmpty() && jsonObject.toString().contains(logicCallbacks.getRefreshTokenInvalidMessage())){
							logicCallbacks.onRefreshTokenInvalid();
						}if(logicCallbacks.getRefreshTokenExpiredMessage()!=null && !logicCallbacks.getRefreshTokenExpiredMessage().isEmpty() && jsonObject.toString().contains(logicCallbacks.getRefreshTokenExpiredMessage())){
							logicCallbacks.onRefreshTokenExpired();
						}
					}catch (JSONException jsone){
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
				return;
			}
		}else{
			Log.d(DEBUG_TAG+"."+metodo+".onResponseError", "networkResponse==null");
		}
		if(ioCallbacks !=null) {
			for (int i=0; i<ioCallbacks.size(); i++){
				if(ioCallbacks.get(i)!=null) ioCallbacks.get(i).onResponseError(volleyError, code);
			}
		}
	}

	private void retry(String code, ArrayList<IOCallbacks> ioCallbacks) throws Exception {
		Log.d(DEBUG_TAG + ".retry", "En retry, desde una llamada con codigo: " + code + ".");
		Log.d(DEBUG_TAG + ".retry", "Estamos ya refrescando el token? " + (updatingToken ? "Si." : "No."));

		RequestQueue mRequestQueue = getRequestQueue();

		if(!updatingToken) {
			updatingToken=true;
			Log.d(DEBUG_TAG + ".retry", "Paramos la request queue principal");
			mRequestQueue.stop();
			VolleyController.getInstance().onCall(logicCallbacks.doRefreshToken(ioCallbacks).setCode(JSON_POST_UPDATE_ACCESS_TOKEN));
		}
	}

	//TODO multipart
	/*public void doPostMultipart(final String url, Map<String, String> headers, final Map<String, String> params, final Bitmap bitmap, final String format, final String code, final IOCallbacks callbacks, boolean primaryRequestQueue){
		for (int i=0; i<interceptors.size(); i++){
			interceptors.get(i).intercept(url, headers, params, null);
		}
		VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new CustomResponse.Listener<NetworkResponse>() {
			@Override
			public void onResponse(NetworkResponse response) {
				Log.v(DEBUG_TAG, "networkResponse.statusCode" + response.statusCode);
				Log.v(DEBUG_TAG, "networkResponse.data" + new String(response.data));
				Log.v(DEBUG_TAG, "networkResponse.networkTimeMs" + response.networkTimeMs);
				Log.v(DEBUG_TAG, "networkResponse.headers" + response.headers);
				VolleyController.this.onResponse(new String(response.data), callbacks, code, Request.Method.POST);
			}
		}, new CustomResponse.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				NetworkResponse networkResponse = error.networkResponse;
				String errorMessage = "Unknown error";
				if (networkResponse == null) {
					if (error.getClass().equals(TimeoutError.class)) {
						errorMessage = "Request timeout";
					} else if (error.getClass().equals(NoConnectionError.class)) {
						errorMessage = "Failed to connect server";
					}
				}
				Log.i("Error", errorMessage);
				error.printStackTrace();

				VolleyController.this.onResponseError(error, callbacks, code, Request.Method.POST);
			}
		}) {
			@Override
			protected Map<String, String> getParams() {
				return params;
			}

			@Override
			protected Map<String, DataPart> getByteData() {
				Map<String, DataPart> params = new HashMap<>();
				// file name could found file base or direct access from real path
				// for now just get bitmap data from ImageView
				params.put("files", new DataPart("file_avatar."+format, ImageUtils.getFileDataFromBitmap(VolleyController.this.context, bitmap), "image/"+format));
				return params;
			}
		};
		onCall(Request.Method.POST, url, code, multipartRequest, callbacks, headers, params, null, primaryRequestQueue);
	}*/

	public int getStatusCode(VolleyError error){
		try {
			return error.networkResponse.statusCode;
		} catch (NullPointerException e) {
			e.printStackTrace();
			return -1;
		}
	}

	public String getMessage(VolleyError error){
		try {
			return new String(error.networkResponse.data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return errorMessage;
		} catch (NullPointerException npe){
			npe.printStackTrace();
			return errorMessage;
		}
	}

	public void cancelRequest(InternetCall internetCall) {
		getRequestQueue().cancelAll(internetCall.getCode());
	}

	public interface IOCallbacks {
		/**
		 *
		 * @param response
		 * @param code
		 */
		void onResponse(CustomResponse response, String code);

		/**
		 *
		 * @param error
		 * @param code
		 */
		void onResponseError(VolleyError error, String code);
	}

	public interface LogicCallbacks {
		void setTokens(JSONObject jsonObject);

		String getRefreshToken();

		String getAuthToken();

		InternetCall doRefreshToken(ArrayList<IOCallbacks> ioCallbacks);

		void onRefreshTokenInvalid();

		void onRefreshTokenExpired();

		String getRefreshTokenInvalidMessage();

		String getRefreshTokenExpiredMessage();

		String getAuthTokenExpiredMessage();
	}


	public enum ContentType {
		TEXT, TEXT_PLAIN, JSON, JAVASCRIPT, XML_APPLICATION, XML_TEXT, HTML;

		public String toString() {
			switch (this) {
				case TEXT: {
					return "text";
				}
				case TEXT_PLAIN: {
					return "text/plain";
				}
				case JSON: {
					return "application/json";
				}
				case JAVASCRIPT: {
					return "application/javascript";
				}
				case XML_APPLICATION: {
					return "application/xml";
				}
				case XML_TEXT: {
					return "text/xml";
				}
				case HTML: {
					return "text/html";
				}
				default: {
					return "";
				}
			}
		}
	}
}
