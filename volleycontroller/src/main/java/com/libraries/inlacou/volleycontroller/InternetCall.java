package com.libraries.inlacou.volleycontroller;

import android.content.Context;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import timber.log.Timber;

/**
 * Created by inlacou on 10/09/14.
 */
public class InternetCall {

	private static final String DEBUG_TAG = InternetCall.class.getName();
	private Method method;
	private String code;
	private String url;
	private Map<String, String> params;
	private Map<String, String> headers;
	private String rawBody;
	private DefaultRetryPolicy retryPolicy;
	private ArrayList<Interceptor> interceptors;
	private File file;
	private ArrayList<VolleyController.IOCallbacks> callbacks;
	private String fileKey;
	private Object cancelTag;
	private Boolean allowLocationRedirect = true;

	public InternetCall() {
		interceptors = new ArrayList<>();
		params = new HashMap<>();
		headers = new HashMap<>();
		method = Method.GET;
		rawBody = "";
		setRetryPolicy(new DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
	}

	public String getUrl() {
		return url;
	}

	public InternetCall setUrl(String url) {
		this.url = url;
		return this;
	}

	public File getFile() {
		return file;
	}

	public InternetCall setFile(String key, File file) {
		this.fileKey = key;
		this.file = file;
		return this;
	}

	public String getRawBody() {
		return rawBody;
	}

	public InternetCall setRawBody(String rawBody) {
		this.rawBody = rawBody;
		return this;
	}

	public InternetCall setHeaders(Map<String, String> headers) {
		this.headers = headers;
		return this;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public InternetCall setParams(Map<String, String> params) {
		rawBody = "";
		this.params = params;
		return this;
	}

	public Map getParams(){
		return params;
	}

	public ArrayList<VolleyController.IOCallbacks> getCallbacks() {
		return callbacks;
	}

	public InternetCall addCallback(VolleyController.IOCallbacks callback) {
		if(callbacks==null){
			callbacks = new ArrayList<>();
		}
		this.callbacks.add(callback);
		return this;
	}

	public boolean isAllowLocationRedirect(){
		return allowLocationRedirect;
	}
	
	public InternetCall setAllowLocationRedirect(Boolean b){
		allowLocationRedirect = b;
		return this;
	}
	
	public String getCode() {
		return code;
	}

	public InternetCall setCode(String code) {
		this.code = code;
		return this;
	}

	public Method getMethod() {
		return method;
	}

	public InternetCall setMethod(Method method) {
		this.method = method;
		return this;
	}

	public InternetCall replaceAccessToken(String oldAccessToken, String newAccessToken) {
		if(url!=null){
			setUrl(url.replace(oldAccessToken, newAccessToken));
		}

		Iterator it;
		if(headers!=null) {
			it = headers.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				if(pair.getValue().toString().contains(oldAccessToken)){
					headers.put(pair.getKey().toString(), pair.getValue().toString().replace(oldAccessToken, newAccessToken));
				}
			}
		}

		if(params!=null) {
			it = params.entrySet().iterator();
			while (it.hasNext()) {
				Map.Entry pair = (Map.Entry) it.next();
				if(pair.getValue().toString().contains(oldAccessToken)){
					params.put(pair.getKey().toString(), pair.getValue().toString().replace(oldAccessToken, newAccessToken));
				}
			}
		}

		if(rawBody!=null && !rawBody.isEmpty() && rawBody.contains(oldAccessToken)) {
			rawBody = rawBody.replace(oldAccessToken, newAccessToken);
		}

		return this;
	}

	public InternetCall prebuild(){
		while(params.values().remove(null));
		while(headers.values().remove(null));
		if(rawBody==null){
			rawBody = "";
		}
		for(int i=0; i<interceptors.size(); i++){
			interceptors.get(i).intercept(this);
		}
		return this;
	}
	
	public Request build(final Context context, final com.android.volley.Response.Listener<CustomResponse> listener, com.android.volley.Response.ErrorListener errorListener) {
		if(file==null) {
			Request request = new CustomRequest(this.getMethod().value(), getUrl(), errorListener) {
				@Override
				protected void deliverResponse(Object response) {
					listener.onResponse((CustomResponse) response);
				}

				@Override
				protected Response<CustomResponse> parseNetworkResponse(NetworkResponse response) {
					CustomResponse newCustomResponse = new CustomResponse(response);
					newCustomResponse.setCode(code);
					//we set here the response (the object received by deliverResponse);
					return com.android.volley.Response.success(newCustomResponse, newCustomResponse.getChacheHeaders());
				}

				@Override
				public Map<String, String> getHeaders() throws AuthFailureError {
					if (InternetCall.this.getHeaders() != null) {
						Map<String, String> headers = InternetCall.this.getHeaders();
						for (Map.Entry<String, String> entry : headers.entrySet()) {
							Log.v(DEBUG_TAG + "." + InternetCall.this.getMethod(), "header -> " + entry.getKey() + ": " + entry.getValue());
						}
						return InternetCall.this.headers;
					}
					return super.getHeaders();
				}

				@Override
				protected Map<String, String> getParams() {
					if (InternetCall.this.getParams() != null) {
						Map<String, String> headers = InternetCall.this.getParams();
						for (Map.Entry<String, String> entry : headers.entrySet()) {
							Log.v(DEBUG_TAG + "." + InternetCall.this.getMethod(), "params -> " + entry.getKey() + ": " + entry.getValue());
						}
						return InternetCall.this.getParams();
					}
					try {
						return super.getParams();
					} catch (AuthFailureError authFailureError) {
						authFailureError.printStackTrace();
					}
					return new HashMap<>();
				}

				@Override
				public byte[] getBody() throws AuthFailureError {
					String body = InternetCall.this.getRawBody();
					if (body != null && !body.isEmpty()) {
						Log.v(DEBUG_TAG + "." + InternetCall.this.getMethod(), "body -> " + body);
						return body.getBytes();
					}
					return super.getBody();
				}
			};
			request.setRetryPolicy(retryPolicy);
			if(cancelTag!=null) request.setTag(cancelTag);
			return request;
		}else{
			Request request = new VolleyMultipartRequest(this.getMethod().value(), getUrl(), errorListener) {
				@Override
				protected void deliverResponse(Object response) {
					listener.onResponse((CustomResponse) response);
				}

				@Override
				protected Response<CustomResponse> parseNetworkResponse(NetworkResponse response) {
					CustomResponse newCustomResponse = new CustomResponse(response);
					newCustomResponse.setCode(code);
					//we set here the response (the object received by deliverResponse);
					return com.android.volley.Response.success(newCustomResponse, newCustomResponse.getChacheHeaders());
				}

				@Override
				protected Map<String, String> getParams() {
					return params;
				}

				@Override
				protected Map<String, DataPart> getByteData() {
					Map<String, DataPart> params = new HashMap<>();
					// file name could found file base or direct access from real path
					// for now just get bitmap data from ImageView
					try {
						params.put(fileKey, new DataPart(file.getName()+"."+file.getFormat(), ImageUtils.getFileDataFromBitmap(context, ImageUtils.getBitmapFromPath(file.getLocation())), file.getType().toString()+"/"+file.getFormat()));
					} catch (IOException e) {
						e.printStackTrace();
					}
					return params;
				}
			};
			request.setRetryPolicy(retryPolicy);
			if(cancelTag!=null) request.setTag(cancelTag);
			return request;
		}
	}

	public InternetCall setRetryPolicy(DefaultRetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		return this;
	}

	public InternetCall setInterceptors(final ArrayList<Interceptor> interceptors){
		this.interceptors = interceptors;
		return this;
	}

	public InternetCall addInterceptors(final ArrayList<Interceptor> interceptors) {
		if(this.interceptors==null){
			this.interceptors = new ArrayList<>();
		}
		if(interceptors==null){
			return this;
		}
		this.interceptors = new ArrayList<Interceptor>(){{ addAll(interceptors); addAll(InternetCall.this.interceptors); }};
		return this;
	}

	public InternetCall addInterceptor(Interceptor interceptor){
		if(this.interceptors==null){
			this.interceptors = new ArrayList<>();
		}
		if(interceptor==null){
			return this;
		}
		this.interceptors.add(interceptor);
		return this;
	}

	public InternetCall putHeader(String key, String value) {
		if(headers==null){
			headers = new HashMap<>();
		}
		headers.put(key, value);
		return this;
	}

	public InternetCall putParam(String key, String value) {
		if(value==null){
			return this;
		}
		rawBody = "";
		if(params==null){
			params = new HashMap<>();
		}
		params.put(key, value);
		return this;
	}

	public InternetCall putHeaders(Map<String, String> headers) {
		if(this.headers==null){
			this.headers = new HashMap<>();
		}
		if(headers==null){
			return this;
		}
		this.headers.putAll(headers);
		return this;
	}

	public InternetCall putParams(Map<String, String> params) {
		while(params.values().remove(null));
		rawBody = "";
		if(this.params==null){
			this.params = new HashMap<>();
		}
		this.params.putAll(params);
		return this;
	}

	public Object getCancelTag() {
		return cancelTag;
	}

	public InternetCall setCancelTag(Object tag){
		this.cancelTag = tag;
		return this;
	}


	public enum Method{
		GET, POST, PUT, DELETE;

		int value(){
			switch (this){
				case GET:
				default:
					return Request.Method.GET;
				case POST:
					return Request.Method.POST;
				case PUT:
					return Request.Method.PUT;
				case DELETE:
					return Request.Method.DELETE;
			}
		}

		@Override
		public String toString() {
			switch (this){
				case GET:
					return "GET";
				case POST:
					return "POST";
				case PUT:
					return "PUT";
				case DELETE:
					return "DELETE";
				default:
					return "(default)GET";
			}
		}
	}

	public interface Interceptor {
		/**
		 *
		 * @param internetCall
		 * @return modified internetCall
		 */
		void intercept(InternetCall internetCall);
	}
}
