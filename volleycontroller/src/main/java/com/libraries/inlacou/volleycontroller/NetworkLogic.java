package com.libraries.inlacou.volleycontroller;

import android.app.Application;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
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
import java.util.Map;

/**
 * Created by inlacou on 25/11/14.
 */
public class NetworkLogic {

	private static final String JSON_POST_UPDATE_ACCESS_TOKEN = "network_logic_json_post_update_access_token";
	private static final String DEBUG_TAG = NetworkLogic.class.getName();

	private static ArrayList<InternetCall> temporaryCallQueue = new ArrayList<InternetCall>();
    private static boolean updatingToken = false;
	private static NetworkLogic ourInstance = new NetworkLogic();
	private RequestQueue mRequestQueue, mSecondaryRequestQueue;
	private LogicCallbacks logicCallbacks;
	private String errorMessage;

	public static NetworkLogic getInstance() {
		return ourInstance;
	}

	private NetworkLogic() {
	}

	public void init(Application application, LogicCallbacks logicCallbacks){
		errorMessage = application.getString(R.string.network_error);
		this.logicCallbacks = logicCallbacks;
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

	private void onCall(int method, String url, String code, IOCallbacks IOCallbacks, Request request, Map<String, String> map){
		onCall(method, url, code, request, IOCallbacks, map, true);
	}

	private void onCall(int method, String url, String code, Request request, IOCallbacks IOCallbacks, Map<String, String> map, boolean primaryRequestQueue){
		String metodo = methodToString(method);

		Log.d(DEBUG_TAG + ".onCall." + metodo, "Request para la " + (primaryRequestQueue ? "primera" : "segunda") + " requestQueue creada con codigo: " + code);
		Log.d(DEBUG_TAG+".onCall."+metodo+"", "Making "+metodo+" call to url: " + url);
		Log.d(DEBUG_TAG+".onCall."+metodo+"", "Mapa = " + map);
		if(map!=null) {
			for (String s : map.keySet()) {
				Log.d(DEBUG_TAG + ".onCall." + metodo + "", "Post parameter " + s + ": " + map.get(s));
			}
		}
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
            temporaryCallQueue.add(new InternetCall(request, code, IOCallbacks, map));
        }
        mRequestQueue.add(request);
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
                //Leemos la respuesta
                try {
                    JSONObject jsonObject = new JSONObject(response);
	                logicCallbacks.setTokens(jsonObject.getString("token"), jsonObject.getString("refresh_token"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                //Obtenemos el nuevo authtoken
                String accessToken = logicCallbacks.getAuthToken();
                Log.d(DEBUG_TAG+"."+metodo+".onJsonResponse", "Continuando llamadas almacenadas. Numero: " + temporaryCallQueue.size());

                for(int i = 0; i<temporaryCallQueue.size(); i++){
                    doCall(temporaryCallQueue.get(i), accessToken, metodo);
                }
                updatingToken=false;
                getRequestQueue().start();
            } else {
				onResponseFinal(response, IOCallbacks, code, metodo);
            }
        }
	}

    private void doCall(final InternetCall iCall, String accessToken, final String metodo){
        RequestQueue rq = getRequestQueue();
        Request r = iCall.getRequest();
        if(iCall.getMap()!=null){
            rq.add(new StringRequest(r.getMethod(), InternetCall.replaceAccessToken(r.getUrl(), accessToken),
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
            }) {
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
                protected Map<String, String> getParams()
                {
                    return iCall.getMap();
                }
            });
        }else {
            rq.add(r instanceof StringRequest ?
					new StringRequest(r.getMethod(), InternetCall.replaceAccessToken(r.getUrl(), accessToken),
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
					} :
					new JsonObjectRequest(r.getMethod(), InternetCall.replaceAccessToken(r.getUrl(), accessToken), null,
							new Response.Listener<JSONObject>() {
								@Override
								public void onResponse(JSONObject jsonObject) {
									NetworkLogic.this.onResponse(jsonObject, iCall.getCallback(), iCall.getCode(), metodo);
								}
							}, new Response.ErrorListener() {
						@Override
						public void onErrorResponse(VolleyError volleyError) {
							onResponseError(volleyError, iCall.getCallback(), iCall.getCode(), metodo);
						}
					}
					));
        }
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
								|| new String(volleyError.networkResponse.data, "UTF-8").contains("UnauthorizedError: jwt expired"))
                        ) {
					Log.d(DEBUG_TAG + "."+metodo+".onResponseError", "Detectado un error 401, token caducado.");
                    retry(code, IOCallbacks);
                    return;
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

	public void doPost(final String url, final Map<String, String> map, final String code, final IOCallbacks IOCallbacks) {
        doPost(url, map, code, IOCallbacks, true);
    }

    public void doPost(final String url, final Map<String, String> map, final String code, final IOCallbacks IOCallbacks, boolean primaryRequestQueue){
		StringRequest postRequest = new StringRequest(Request.Method.POST, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
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
            protected Map<String, String> getPostParams() throws AuthFailureError {
                return map;
            }

            @Override
            protected Map<String, String> getParams() throws AuthFailureError {
                return map;
            }
        };
		onCall(Request.Method.POST, url, code, postRequest, IOCallbacks, map, primaryRequestQueue);
	}

	public void doDelete(String url, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.DELETE, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.DELETE);
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
		};
		onCall(Request.Method.DELETE, url, code, IOCallbacks, request, null);
	}

	public void doDelete(String url, Map<String, String> headers, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.DELETE, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.DELETE);
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
				return super.getHeaders();
			}
		};
		onCall(Request.Method.DELETE, url, code, IOCallbacks, request, null);
	}

	public void doPut(String url, final Map<String, String> map, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.PUT, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.PUT);
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
			protected Map<String, String> getParams()
			{
				return map;
			}
		};
		onCall(Request.Method.PUT, url, code, IOCallbacks, request, map);
	}

	public void doGet(String url, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.GET, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.GET);
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
		};
		onCall(Request.Method.GET, url, code, IOCallbacks, request, null);
	}

	public void doJsonGet(final String url, final String code, final IOCallbacks IOCallbacks){
		doJsonGet(url, code, IOCallbacks, true);
	}

	public void doJsonGet(final String url, final String code, final IOCallbacks IOCallbacks, boolean primaryRequestQueue){
		JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, url, null,
				new Response.Listener<JSONObject>(){
					@Override
					public void onResponse(JSONObject jsonObject) {
						NetworkLogic.this.onResponse(jsonObject, IOCallbacks, code, Request.Method.GET);
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

		onCall(Request.Method.GET, url, code, request, IOCallbacks, null, primaryRequestQueue);
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

	public void doPost(String url, final ContentType contentType, final String rawBody, final Map<String, String> headers, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.POST, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
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
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
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
	}

	public void doDeleteRawJSON(String url, final ContentType contentType, final String raw, final Map<String, String> headers, final String code, final IOCallbacks IOCallbacks){
		StringRequest request = new StringRequest(Request.Method.DELETE, url,
				new Response.Listener<String>()
				{
					@Override
					public void onResponse(String response) {
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.DELETE);
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
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.DELETE);
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
						NetworkLogic.this.onResponse(response, IOCallbacks, code, Request.Method.POST);
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
	}

	public interface IOCallbacks {
		void onResponse(JSONObject json, String code);
		void onResponse(String response, String code);
		void onResponseError(VolleyError error, String code);
	}

	public interface LogicCallbacks {
		void setTokens(String authToken, String refreshToken);
		String getRefreshToken();
		String getAuthToken();
		void doRefreshToken(IOCallbacks ioCallbacks);
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
