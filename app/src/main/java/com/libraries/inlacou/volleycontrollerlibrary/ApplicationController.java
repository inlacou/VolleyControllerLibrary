package com.libraries.inlacou.volleycontrollerlibrary;

import android.app.Application;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.libraries.inlacou.volleycontroller.CustomResponse;
import com.libraries.inlacou.volleycontroller.InternetCall;
import com.libraries.inlacou.volleycontroller.VolleyController;

import org.json.JSONObject;

import java.util.ArrayList;

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
		VolleyController.getInstance().init(this, true, new VolleyController.LogicCallbacks() {

			@Override
			public void setTokens(JSONObject jsonObject) {
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
			public InternetCall doRefreshToken(ArrayList<VolleyController.IOCallbacks> ioCallbacks) {
				return null;
			}
			
			@Override
			public void onRefreshTokenInvalid(VolleyError volleyError, String code) {
			
			}
			
			@Override
			public void onRefreshTokenExpired(VolleyError volleyError, String code) {
			
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
		VolleyController.getInstance().addInterceptor(new InternetCall.Interceptor() {
			@Override
			public void intercept(InternetCall internetCall) {
				internetCall
						.putHeader("deviceId", "5")
						.putParam("alwaysParam", "Hey! :D")
						.addCallback(new VolleyController.IOCallbacks() {
							@Override
							public void onResponse(CustomResponse response, String code) {
								try {
									Toast.makeText(ApplicationController.this, response.getData().substring(0, 20) + "...", Toast.LENGTH_SHORT).show();
								}catch (IndexOutOfBoundsException ioobe){
									Toast.makeText(ApplicationController.this, response.getData().substring(0, response.getData().length()), Toast.LENGTH_SHORT).show();
								}
							}
							
							@Override
							public void onResponseError(VolleyError error, String code) {
								
							}
						});
			}
		});
	}
}
