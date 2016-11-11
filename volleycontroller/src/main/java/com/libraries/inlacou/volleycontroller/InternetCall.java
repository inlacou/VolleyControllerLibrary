package com.libraries.inlacou.volleycontroller;

import com.android.volley.Request;

import java.util.Iterator;
import java.util.Map;

/**
 * Created by inlacou on 10/09/14.
 */
public class InternetCall {

	private static final String DEBUG_TAG = InternetCall.class.getName();
	private Request request;
	private String code;
	private VolleyController.IOCallbacks callback;
	private String url;
	private Map<String, String> params;
	private Map<String, String> headers;
	private String rawBody;

	public InternetCall(Request request, String code, VolleyController.IOCallbacks callback, String url, Map<String, String> headers, Map<String, String> params, String rawBody) {
		setUrl(url);
		setRequest(request);
		setCode(code);
		setCallback(callback);
		setParams(params);
		setHeaders(headers);
		setRawBody(rawBody);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getRawBody() {
		return rawBody;
	}

	public void setRawBody(String rawBody) {
		this.rawBody = rawBody;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Map getParams(){
		return params;
	}

	public Request getRequest() {
		return request;
	}

	private void setRequest(Request request) {
		this.request = request;
	}

	public VolleyController.IOCallbacks getCallback() {
		return callback;
	}

	private void setCallback(VolleyController.IOCallbacks callback) {
		this.callback = callback;
	}

	public String getCode() {
		return code;
	}

	private void setCode(String code) {
		this.code = code;
	}

	public String replaceAccessToken(String oldAccessToken, String newAccessToken) {
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

		if(rawBody!=null) {
			rawBody = rawBody.replace(oldAccessToken, newAccessToken);
		}

		return url;
	}
}
