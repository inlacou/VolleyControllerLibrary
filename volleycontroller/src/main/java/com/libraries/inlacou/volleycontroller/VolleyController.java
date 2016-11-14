package com.libraries.inlacou.volleycontroller;

import android.app.Application;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by inlacou on 25/11/14.
 */
public class VolleyController {

	private static final String JSON_POST_UPDATE_ACCESS_TOKEN = "network_logic_json_post_update_access_token";
	private static final String DEBUG_TAG = VolleyController.class.getName();

	private static ArrayList<InternetCall> temporaryCallQueue = new ArrayList<InternetCall>();
    private static boolean updatingToken = false;
	private static VolleyController ourInstance = new VolleyController();
	private Context context;
	private RequestQueue mRequestQueue, mSecondaryRequestQueue;
	private LogicCallbacks logicCallbacks;
	private String errorMessage;
	private ArrayList<Interceptor> interceptors;
	
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

	public void addInterceptor(Interceptor interceptor){
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

	private void onCall(int method, String url, String code, Request request, IOCallbacks IOCallbacks, Map<String, String> headers, Map<String, String> params, String rawBody){
		onCall(method, url, code, request, IOCallbacks, headers, params, rawBody, true);
	}

	private void onCall(int method, String url, String code, Request request, IOCallbacks IOCallbacks, Map<String, String> headers, Map<String, String> params, String rawBody, boolean primaryRequestQueue){
		String methodString = methodToString(method);

		Log.d(DEBUG_TAG + ".onCall." + methodString, "Request para la " + (primaryRequestQueue ? "primera" : "segunda") + " requestQueue creada con codigo: " + code);
		Log.d(DEBUG_TAG+".onCall."+methodString+"", "Making "+methodString+" call to url: " + url);
		logMap(headers, "header", methodString);

		request.setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        RequestQueue mRequestQueue;
        if(primaryRequestQueue) {
            Log.d(DEBUG_TAG, "primaryRequestQueue");
            mRequestQueue = getRequestQueue();
        }else{
            Log.d(DEBUG_TAG, "secondaryRequestQueue");
            mRequestQueue = this.getSecondaryRequestQueue();
        }
        if(!code.equalsIgnoreCase(JSON_POST_UPDATE_ACCESS_TOKEN)) {
            temporaryCallQueue.add(new InternetCall(request, code, IOCallbacks, url, headers, params, rawBody));
        }
        mRequestQueue.add(request);
	}

	private void logMap(Map<String, String> map, String type, String method) {
		Log.d(DEBUG_TAG+".onCall."+method+"", "Map(" + type + ") = " + map);
		if(map!=null) {
			for (String s : map.keySet()) {
				Log.d(DEBUG_TAG + ".onCall." + method + "", type + " parameter " + s + ": " + map.get(s));
			}
		}
	}

	private void onResponseFinal(String response, IOCallbacks IOCallbacks, String code, String metodo){
		Log.d(DEBUG_TAG+"."+metodo+".onStringResponse", "Code: " + code);
		Log.d(DEBUG_TAG + "." + metodo + ".onStringResponse", "Method: " + metodo);
		Log.d(DEBUG_TAG + "." + metodo + ".onStringResponse", "Response: " + response);
		if(IOCallbacks !=null) {
            IOCallbacks.onResponse(response, code);
            removeFromTemporaryList(code);
        }
	}

	private void onResponse(String response, IOCallbacks IOCallbacks, String code, int method){
		String metodo = methodToString(method);
		Log.d(DEBUG_TAG+"."+metodo+".onStringResponse", "Code: " + code);
		Log.d(DEBUG_TAG+"."+metodo+".onStringResponse", "StatusCode: " + code);
		Log.d(DEBUG_TAG + "." + metodo + ".onStringResponse", "Response: " + response);
		if(IOCallbacks !=null) {
            if(code.equalsIgnoreCase(JSON_POST_UPDATE_ACCESS_TOKEN)){
                Log.d(DEBUG_TAG+"."+metodo+".onJsonResponse", "Recibida la respuesta al codigo " + JSON_POST_UPDATE_ACCESS_TOKEN +
                        ", updating tokens. | " + response);
	            //Save old authToken
	            String oldAccessToken = logicCallbacks.getAuthToken();
                //Read answer
                try {
                    JSONObject jsonObject = new JSONObject(response);
	                //Save new tokens
	                logicCallbacks.setTokens(jsonObject.getString("token"), jsonObject.getString("refresh_token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Get new authToken
                String accessToken = logicCallbacks.getAuthToken();
                Log.d(DEBUG_TAG+"."+metodo+".onJsonResponse", "Continuando llamadas almacenadas. Numero: " + temporaryCallQueue.size());

                for(int i = 0; i<temporaryCallQueue.size(); i++){
                    doCall(temporaryCallQueue.get(i), oldAccessToken, accessToken, metodo);
                }
                updatingToken=false;
                getRequestQueue().start();
            } else {
				onResponseFinal(response, IOCallbacks, code, metodo);
            }
        }
	}

    private void doCall(final InternetCall iCall, String oldAccessToken, String accessToken, final String metodo){
        RequestQueue rq = getRequestQueue();
        Request r = iCall.getRequest();
	    rq.add(r instanceof StringRequest ?
			    new StringRequest(r.getMethod(), iCall.replaceAccessToken(oldAccessToken, accessToken),
					    new Response.Listener<String>() {
						    @Override
						    public void onResponse(String s) {
							    onResponseFinal(s, iCall.getCallback(), iCall.getCode(), metodo);
						    }
					    }, new Response.ErrorListener() {
				    @Override
				    public void onErrorResponse(VolleyError volleyError) {
					    onResponseError(volleyError, iCall.getCallback(), iCall.getCode(), metodo);
				    }
			    }){
				    @Override
				    protected Response<String> parseNetworkResponse(NetworkResponse response) {
					    try {
						    String utf8String = new String(response.data, "UTF-8");
						    return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
					    } catch (UnsupportedEncodingException e) {
						    e.printStackTrace();
					    }
					    return super.parseNetworkResponse(response);
				    }

				    @Override
				    public Map<String, String> getHeaders() throws AuthFailureError {
					    if(iCall.getHeaders()!=null){
						    Map<String, String> headers = iCall.getHeaders();
						    for (Map.Entry<String, String> entry : headers.entrySet())
						    {
							    Log.v(DEBUG_TAG + "." + metodo, "header -> " + entry.getKey() + ": " + entry.getValue());
						    }
						    return headers;
					    }
					    return super.getHeaders();
				    }

				    @Override
				    protected Map<String, String> getParams() {
					    if(iCall.getParams()!=null){
						    Map<String, String> headers = iCall.getParams();
						    for (Map.Entry<String, String> entry : headers.entrySet())
						    {
							    Log.v(DEBUG_TAG + "." + metodo, "params -> " + entry.getKey() + ": " + entry.getValue());
						    }
						    return iCall.getParams();
					    }
					    return getParams();
				    }

				    @Override
				    public byte[] getBody() throws AuthFailureError {
					    if(iCall.getRawBody()!=null) {
						    Log.v(DEBUG_TAG + "." + metodo, "body -> " + iCall.getRawBody());
						    return iCall.getRawBody().getBytes();
					    }
					    return super.getBody();
				    }

				    @Override
				    public String getBodyContentType() {
					    if(iCall.getRawBody()!=null){
						    Log.v(DEBUG_TAG + "." + metodo, "content-type -> " + ContentType.JSON.toString());
						    return ContentType.JSON.toString();
					    }
					    return super.getBodyContentType();
				    }
			    } :
			    new JsonObjectRequest(r.getMethod(), iCall.replaceAccessToken(oldAccessToken, accessToken), null,
					    new Response.Listener<JSONObject>() {
						    @Override
						    public void onResponse(JSONObject jsonObject) {
							    VolleyController.this.onResponse(jsonObject, iCall.getCallback(), iCall.getCode(), metodo);
						    }
					    }, new Response.ErrorListener() {
				    @Override
				    public void onErrorResponse(VolleyError volleyError) {
					    onResponseError(volleyError, iCall.getCallback(), iCall.getCode(), metodo);
				    }
			    }){
				    @Override
				    public Map<String, String> getHeaders() throws AuthFailureError {
					    if(iCall.getHeaders()!=null){
						    Map<String, String> headers = iCall.getHeaders();
						    for (Map.Entry<String, String> entry : headers.entrySet())
						    {
							    Log.v(DEBUG_TAG + "." + metodo, "header -> " + entry.getKey() + ": " + entry.getValue());
						    }
						    return headers;
					    }
					    return super.getHeaders();
				    }

				    @Override
				    protected Map<String, String> getParams() {
					    if(iCall.getParams()!=null){
						    Map<String, String> headers = iCall.getParams();
						    for (Map.Entry<String, String> entry : headers.entrySet())
						    {
							    Log.v(DEBUG_TAG + "." + metodo, "params -> " + entry.getKey() + ": " + entry.getValue());
						    }
						    return iCall.getParams();
					    }
					    return getParams();
				    }

				    @Override
				    public byte[] getBody() {
					    if(iCall.getRawBody()!=null) {
						    Log.v(DEBUG_TAG + "." + metodo, "body -> " + iCall.getRawBody());
						    return iCall.getRawBody().getBytes();
					    }
					    return super.getBody();
				    }

				    @Override
				    public String getBodyContentType() {
					    if(iCall.getRawBody()!=null){
						    Log.v(DEBUG_TAG + "." + metodo, "content-type -> " + ContentType.JSON.toString());
						    return ContentType.JSON.toString();
					    }
					    return super.getBodyContentType();
				    }
			    });
    }

	private void onResponse(JSONObject jsonObject, IOCallbacks IOCallbacks, String code, int method){
		final String metodo = methodToString(method);
        onResponse(jsonObject, IOCallbacks, code, metodo);
	}

	private void onResponse(JSONObject jsonObject, IOCallbacks IOCallbacks, String code, String metodo){
		Log.d(DEBUG_TAG+"."+metodo+".onJsonResponse", "Code: " + code);
		Log.d(DEBUG_TAG + "." + metodo + ".onJSONResponse", "Response: " + jsonObject.toString());
        if(IOCallbacks !=null) {
            IOCallbacks.onResponse(jsonObject, code);
            removeFromTemporaryList(code);
        }
	}

	private void onResponseError(VolleyError volleyError, IOCallbacks IOCallbacks, String code, int method){
		String metodo = methodToString(method);
		onResponseError(volleyError, IOCallbacks, code, metodo);
	}

	private void onResponseError(VolleyError volleyError, IOCallbacks IOCallbacks, String code, String metodo){
		if(volleyError.networkResponse!=null){
			Log.d(DEBUG_TAG+"."+metodo+".onResponseError", "StatusCode: "+volleyError.networkResponse.statusCode);
			try {
				Log.d(DEBUG_TAG + "."+metodo+".onResponseError", "Message: " + new String(volleyError.networkResponse.data, "UTF-8"));
				Log.d(DEBUG_TAG + "."+metodo+".onResponseError", "StatusCode: " + volleyError.networkResponse.statusCode);
				if(volleyError.networkResponse.statusCode==401 &&
                        (new String(volleyError.networkResponse.data, "UTF-8").contains("The access token provided has expired.")
								|| new String(volleyError.networkResponse.data, "UTF-8").contains("The access token provided is invalid.")
								|| new String(volleyError.networkResponse.data, "UTF-8").contains("UnauthorizedError: jwt expired")
								|| new String(volleyError.networkResponse.data, "UTF-8").contains(logicCallbacks.getAuthTokenExpiredMessage()))
                        ) {
					Log.d(DEBUG_TAG + "."+metodo+".onResponseError", "Detectado un error 401, token caducado.");
                    retry(code, IOCallbacks);
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
		if(IOCallbacks !=null) IOCallbacks.onResponseError(volleyError, code);
	}

    private void retry(String code, IOCallbacks IOCallbacks) throws Exception {
        Log.d(DEBUG_TAG + ".retry", "En retry, desde una llamada con codigo: " + code + ".");
        Log.d(DEBUG_TAG + ".retry", "Estamos ya refrescando el token? " + (updatingToken ? "Si." : "No."));

        RequestQueue mRequestQueue = getRequestQueue();

        if(!updatingToken) {
            updatingToken=true;
            Log.d(DEBUG_TAG + ".retry", "Paramos la request queue principal");
            mRequestQueue.stop();
            logicCallbacks.doRefreshToken(IOCallbacks);
        }
    }

	public void doPost(final String url, final Map<String, String> header, final Map<String, String> params, final String rawBody, final String code, final IOCallbacks IOCallbacks) {
        doPost(url, header, params, rawBody, code, IOCallbacks, true);
    }

    private void doPost(final String url, final Map<String, String> headers, final Map<String, String> params, final String rawBody, final String code, final IOCallbacks IOCallbacks, boolean primaryRequestQueue){
	    for (int i=0; i<interceptors.size(); i++){
		    interceptors.get(i).intercept(url, headers, params, rawBody);
	    }
	    StringRequest postRequest = new StringRequest(Request.Method.POST, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.POST);
					}
				}
		) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				if(headers!=null){
					for (Map.Entry<String, String> entry : headers.entrySet())
					{
						Log.v(DEBUG_TAG + "." + "POST", "header -> " + entry.getKey() + ": " + entry.getValue());
					}
					return headers;
				}
				return super.getHeaders();
			}

			@Override
			protected Map<String, String> getParams() {
				if(params!=null){
					Map<String, String> headers = params;
					for (Map.Entry<String, String> entry : headers.entrySet())
					{
						Log.v(DEBUG_TAG + "." + "POST", "params -> " + entry.getKey() + ": " + entry.getValue());
					}
					return params;
				}
				return getParams();
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				if(rawBody!=null) {
					Log.v(DEBUG_TAG + "." + "POST", "body -> " + rawBody);
					return rawBody.getBytes();
				}
				return super.getBody();
			}

			@Override
			public String getBodyContentType() {
				if(rawBody!=null){
					Log.v(DEBUG_TAG + "." + "POST", "content-type -> " + ContentType.JSON.toString());
					return ContentType.JSON.toString();
				}
				return super.getBodyContentType();
			}
        };
		onCall(Request.Method.POST, url, code, postRequest, IOCallbacks, headers, params, rawBody, primaryRequestQueue);
	}

	public void doDelete(String url, final Map<String, String> headers, final Map<String, String> params, final String rawBody, final String code, final IOCallbacks IOCallbacks){
		for (int i=0; i<interceptors.size(); i++){
			interceptors.get(i).intercept(url, headers, params, rawBody);
		}
		StringRequest request = new StringRequest(Request.Method.DELETE, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.DELETE);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.DELETE);
					}
				}
		){
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				if(headers!=null){
					for (Map.Entry<String, String> entry : headers.entrySet())
					{
						Log.v(DEBUG_TAG + "." + "POST", "header -> " + entry.getKey() + ": " + entry.getValue());
					}
					return headers;
				}
				return super.getHeaders();
			}

			@Override
			protected Map<String, String> getParams() {
				if(params!=null){
					Map<String, String> headers = params;
					for (Map.Entry<String, String> entry : headers.entrySet())
					{
						Log.v(DEBUG_TAG + "." + "POST", "params -> " + entry.getKey() + ": " + entry.getValue());
					}
					return params;
				}
				return getParams();
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				if(rawBody!=null) {
					Log.v(DEBUG_TAG + "." + "POST", "body -> " + rawBody);
					return rawBody.getBytes();
				}
				return super.getBody();
			}

			@Override
			public String getBodyContentType() {
				if(rawBody!=null){
					Log.v(DEBUG_TAG + "." + "POST", "content-type -> " + ContentType.JSON.toString());
					return ContentType.JSON.toString();
				}
				return super.getBodyContentType();
			}
		};
		onCall(Request.Method.DELETE, url, code, request, IOCallbacks, headers, params, rawBody);
	}

	public void doPut(String url, final Map<String, String> headers, final Map<String, String> params, final String rawBody, final String code, final IOCallbacks IOCallbacks){
		for (int i=0; i<interceptors.size(); i++){
			interceptors.get(i).intercept(url, headers, params, rawBody);
		}

		StringRequest request = new StringRequest(Request.Method.PUT, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.PUT);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.PUT);
					}
				}
		) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				if(headers!=null){
					for (Map.Entry<String, String> entry : headers.entrySet())
					{
						Log.v(DEBUG_TAG + "." + "POST", "header -> " + entry.getKey() + ": " + entry.getValue());
					}
					return headers;
				}
				return super.getHeaders();
			}

			@Override
			protected Map<String, String> getParams() {
				if(params!=null){
					Map<String, String> headers = params;
					for (Map.Entry<String, String> entry : headers.entrySet())
					{
						Log.v(DEBUG_TAG + "." + "POST", "params -> " + entry.getKey() + ": " + entry.getValue());
					}
					return params;
				}
				return getParams();
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				if(rawBody!=null) {
					Log.v(DEBUG_TAG + "." + "POST", "body -> " + rawBody);
					return rawBody.getBytes();
				}
				return super.getBody();
			}

			@Override
			public String getBodyContentType() {
				if(rawBody!=null){
					Log.v(DEBUG_TAG + "." + "POST", "content-type -> " + ContentType.JSON.toString());
					return ContentType.JSON.toString();
				}
				return super.getBodyContentType();
			}
		};
		onCall(Request.Method.PUT, url, code, request, IOCallbacks, headers, params, rawBody);
	}

	public void doGet(String url, final Map<String, String> headers, final String code, final IOCallbacks IOCallbacks){
		logMap(headers, "headers pre", "get");
		for (int i=0; i<interceptors.size(); i++){
			interceptors.get(i).intercept(url, headers, null, null);
		}
		logMap(headers, "headers post", "get");
		StringRequest request = new StringRequest(Request.Method.GET, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.GET);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.GET);
					}
				}
		){
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public String getBodyContentType() {
				Log.d(DEBUG_TAG, "getBodyContentType: " + super.getBodyContentType());
				return super.getBodyContentType();
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				if(headers!=null){
					return headers;
				}
				return super.getHeaders();
			}
		};
		onCall(Request.Method.GET, url, code, request, IOCallbacks, headers, null, null);
	}

	public void doJsonGet(final String url, final String code, final IOCallbacks IOCallbacks){
		doJsonGet(url, code, IOCallbacks, true);
	}

	public void doJsonGet(final String url, final String code, final IOCallbacks IOCallbacks, boolean primaryRequestQueue){
		JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
				new Response.Listener<JSONObject>(){
					@Override
					public void onResponse(JSONObject jsonObject) {
						VolleyController.this.onResponse(jsonObject, IOCallbacks, code, Request.Method.GET);
					}
				},
				new Response.ErrorListener() {
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.GET);
					}
				}
		){
			@Override
			public String getBodyContentType() {
				Log.d(DEBUG_TAG, "getBodyContentType: " + super.getBodyContentType());
				return super.getBodyContentType();
			}
		};

		onCall(Request.Method.GET, url, code, request, IOCallbacks, null, null, null, primaryRequestQueue);
	}

	public void doPostMultipart(final String url, Map<String, String> headers, final Map<String, String> params, final Bitmap bitmap, final String format, final String code, final IOCallbacks callbacks, boolean primaryRequestQueue){
		for (int i=0; i<interceptors.size(); i++){
			interceptors.get(i).intercept(url, headers, params, null);
		}
		VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new Response.Listener<NetworkResponse>() {
			@Override
			public void onResponse(NetworkResponse response) {
				Log.v(DEBUG_TAG, "networkResponse.statusCode" + response.statusCode);
				Log.v(DEBUG_TAG, "networkResponse.data" + new String(response.data));
				Log.v(DEBUG_TAG, "networkResponse.networkTimeMs" + response.networkTimeMs);
				Log.v(DEBUG_TAG, "networkResponse.headers" + response.headers);
				VolleyController.this.onResponse(new String(response.data), callbacks, code, Request.Method.POST);
			}
		}, new Response.ErrorListener() {
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
	}

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

	/*public void doPost(String url, final ContentType contentType, final String rawBody, final Map<String, String> headers, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.POST, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.POST);
					}
				}
		) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public String getBodyContentType() {
				return contentType.toString();
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				Map<String, String> map = headers;
				for (Map.Entry<String, String> entry : map.entrySet())
				{
					Log.d(DEBUG_TAG + ".doPost", "header -> " + entry.getKey() + ": " + entry.getValue());
				}
				return headers;
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				Log.d(DEBUG_TAG + ".doPost", "body -> " + rawBody);
				return rawBody.getBytes();
			}
		};
		onCall(Request.Method.POST, url, code, IOCallbacks, request, null);
	}

	public void doPost(final String url, final ContentType contentType, final String raw, final String code, final IOCallbacks IOCallbacks){
		StringRequest postRequest = new StringRequest(Request.Method.POST, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.POST);
					}
				}
		) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public String getBodyContentType()
			{
				return contentType.toString();
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				return raw.getBytes();
			}
		};
		onCall(Request.Method.POST, url, code, IOCallbacks, postRequest, null);
	}*/

	/*public void doDeleteRawJSON(String url, final ContentType contentType, final String raw, final Map<String, String> headers, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.DELETE, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.DELETE);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.DELETE);
					}
				}
		) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public String getBodyContentType() {
				return contentType.toString();
			}

			@Override
			public Map<String, String> getHeaders() throws AuthFailureError {
				return headers;
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				return raw.getBytes();
			}
		};
		onCall(Request.Method.DELETE, url, code, IOCallbacks, request, null);
	}

	public void doDeleteRawXml(final String url, final ContentType contentType, final String raw, final String code, final IOCallbacks IOCallbacks){
		StringRequest postRequest = new StringRequest(Request.Method.DELETE, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.DELETE);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.DELETE);
					}
				}
		) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public String getBodyContentType()
			{
				return contentType.toString();
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				return raw.getBytes();
			}
		};
		onCall(Request.Method.DELETE, url, code, IOCallbacks, postRequest, null);
	}

	public void doPutRawXml(final String url, final ContentType contentType, final String raw, final String code, final IOCallbacks IOCallbacks){
		StringRequest putRequest = new StringRequest(Request.Method.PUT, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						VolleyController.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
					}
				},
				new Response.ErrorListener()
				{
					@Override
					public void onErrorResponse(VolleyError volleyError) {
						onResponseError(volleyError, IOCallbacks, code, Request.Method.POST);
					}
				}
		) {
			@Override
			protected Response<String> parseNetworkResponse(NetworkResponse response) {
				try {
					String utf8String = new String(response.data, "UTF-8");
					return Response.success(utf8String, HttpHeaderParser.parseCacheHeaders(response));
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
				return super.parseNetworkResponse(response);
			}

			@Override
			public String getBodyContentType()
			{
				return contentType.toString();
			}

			@Override
			public byte[] getBody() throws AuthFailureError {
				return raw.getBytes();
			}
		};
		onCall(Request.Method.PUT, url, code, IOCallbacks, putRequest, null);
	}*/

	public interface IOCallbacks {
		/**
		 *
		 * @param json
		 * @param code
		 */
		void onResponse(JSONObject json, String code);

		/**
		 *
		 * @param response
		 * @param code
		 */
		void onResponse(String response, String code);

		/**
		 *
		 * @param error
		 * @param code
		 */
		void onResponseError(VolleyError error, String code);
	}

	public interface LogicCallbacks {
		void setTokens(String authToken, String refreshToken);
		String getRefreshToken();
		String getAuthToken();
		void doRefreshToken(IOCallbacks ioCallbacks);
		void onRefreshTokenInvalid();
		void onRefreshTokenExpired();
		String getRefreshTokenInvalidMessage();
		String getRefreshTokenExpiredMessage();
		String getAuthTokenExpiredMessage();
	}

	public interface Interceptor {
		/**
		 *
		 * @param url
		 * @param headers
		 * @param params
		 * @param rawBody
		 */
		void intercept(String url, Map<String, String> headers, Map<String, String> params, String rawBody);
	}

	public enum ContentType {
		TEXT, TEXT_PLAIN, JSON, JAVASCRIPT, XML_APPLICATION, XML_TEXT, HTML;

		public String toString(){
			switch (this){
				case TEXT:{
					return "text";
				}
				case TEXT_PLAIN:{
					return "text/plain";
				}
				case JSON:{
					return "application/json";
				}
				case JAVASCRIPT:{
					return "application/javascript";
				}
				case XML_APPLICATION:{
					return "application/xml";
				}
				case XML_TEXT:{
					return "text/xml";
				}
				case HTML:{
					return "text/html";
				}
				default:{
					return "";
				}
			}
		}
	}

}
