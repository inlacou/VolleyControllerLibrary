package com.libraries.inlacou.volleycontroller;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;

/**
 * Created by inlacou on 17/11/16.
 */

public class CustomRequest extends Request {

	public CustomRequest(int method, String url, Response.ErrorListener listener) {
		super(method, url, listener);
	}

	@Override
	protected Response parseNetworkResponse(NetworkResponse response) {
		return null;
	}

	@Override
	protected void deliverResponse(Object response) {
		//Do nothing, we will override it
	}

	@Override
	public int compareTo(Object o) {
		return 0;
	}
}
