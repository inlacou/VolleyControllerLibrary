package com.libraries.inlacou.volleycontroller;

import android.content.Context;

import com.android.volley.Request;

import java.util.Map;

/**
 * Created by inlacou on 10/09/14.
 */
public class InternetCall {

	private static final String DEBUG_TAG = "InternetCall";
	private Request request;
	private String code;
	private VolleyController.IOCallbacks callback;
	private Map<String, String> map;
	private Context context;

	public InternetCall(Request request, String code, VolleyController.IOCallbacks callback, Map<String, String> map) {
		setRequest(request);
		setCode(code);
		setCallback(callback);
		setMap(map);
	}

	public void setMap(Map<String, String> map) {
		this.map = map;
	}

	public Map getMap(){
		return map;
	}

	public InternetCall(Request request, String code, VolleyController.IOCallbacks callback, Context context) {
		setRequest(request);
		setCode(code);
		setCallback(callback);
		setContext(context);
	}

	public Request getRequest() {
		return request;
	}

	public void setRequest(Request request) {
		this.request = request;
	}

	public VolleyController.IOCallbacks getCallback() {
		return callback;
	}

	public void setCallback(VolleyController.IOCallbacks callback) {
		this.callback = callback;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Context getContext() {
		return context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

    public static String replaceAccessToken(String url, String newAccessToken) {
        String result = url;
        if(url.contains("access_token=")) {
            int pos1 = url.indexOf("access_token=") + "access_token=".length();
            int pos2 = url.indexOf("&", url.indexOf("accessToken="));
            try {
                result = url.replace(url.substring(pos1, pos2), newAccessToken);
            } catch (StringIndexOutOfBoundsException sioobe) {
                result = url.replace(url.substring(pos1, url.length()), newAccessToken);
            }
        }
        return result;
    }
}
