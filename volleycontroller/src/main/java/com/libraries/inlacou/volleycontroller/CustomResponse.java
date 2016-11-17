package com.libraries.inlacou.volleycontroller;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.Map;

/**
 * Created by inlacou on 17/11/16.
 */

public class CustomResponse {

	private int statusCode;
	private boolean notModified;
	private Map<String, String> headers;
	private String data;
	private long networkTimeMs;
	private Cache.Entry chacheHeaders;

	public CustomResponse(NetworkResponse response) {
		try {
			data = new String(response.data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		chacheHeaders = HttpHeaderParser.parseCacheHeaders(response);
		headers = response.headers;
		networkTimeMs = response.networkTimeMs;
		notModified = response.notModified;
		statusCode = response.statusCode;
	}

	public int getStatusCode() {
		return statusCode;
	}

	public boolean isNotModified() {
		return notModified;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public String getData() {
		return data;
	}

	public long getNetworkTimeMs() {
		return networkTimeMs;
	}

	public Cache.Entry getChacheHeaders() {
		return chacheHeaders;
	}
}
