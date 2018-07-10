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
	private Cache.Entry cacheHeaders;
	private String code;

	public CustomResponse(){}

	public CustomResponse(NetworkResponse response) {
		try {
			data = new String(response.data, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		cacheHeaders = HttpHeaderParser.parseCacheHeaders(response);
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
		return cacheHeaders;
	}
	
	public String getCode() {
		return code;
	}
	
	public void setCode(String code) {
		this.code = code;
	}
	
	@Override
	public String toString() {
		return "{ \"statusCode\": " + statusCode +
				", \"notModified\": " + notModified +
				", \"headers\": " + headers +
				", \"data\": " + data +
				", \"networkTimeMs\": " + networkTimeMs +
				", \"cacheHeaders\": " + cacheHeaders +
				" }";
	}
}
