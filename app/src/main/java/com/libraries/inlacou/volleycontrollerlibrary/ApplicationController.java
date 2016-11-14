package com.libraries.inlacou.volleycontrollerlibrary;

import android.app.Application;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.libraries.inlacou.volleycontroller.VolleyController;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by inlacou on 14/11/16.
 */
public class ApplicationController extends Application {

	private static final String DEBUG_TAG = ApplicationController.class.getName();
	private static ApplicationController ourInstance = new ApplicationController();
	
	public static ApplicationController getInstance() {
		return ourInstance;
	}
	
	public ApplicationController() {
	}

	@Override
	public void onCreate() {
		super.onCreate();
		VolleyController.getInstance().init(this, new VolleyController.LogicCallbacks() {

			@Override
			public void setTokens(String authToken, String refreshToken) {

			}

			@Override
			public String getRefreshToken() {
				return null;
			}

			@Override
			public String getAuthToken() {
				return null;
			}

			@Override
			public void doRefreshToken(VolleyController.IOCallbacks ioCallbacks) {

			}

			@Override
			public void onRefreshTokenInvalid() {

			}

			@Override
			public void onRefreshTokenExpired() {

			}

			@Override
			public String getRefreshTokenInvalidMessage() {
				return null;
			}

			@Override
			public String getRefreshTokenExpiredMessage() {
				return null;
			}

			@Override
			public String getAuthTokenExpiredMessage() {
				return null;
			}
		});
		VolleyController.getInstance().addInterceptor(new VolleyController.Interceptor() {
			@Override
			public void intercept(String url, Map<String, String> headers, Map<String, String> params, String rawBody) {
				if(headers==null) headers = new HashMap<>();
				headers.put("deviceId", "5");
			}
		});
	}
}
