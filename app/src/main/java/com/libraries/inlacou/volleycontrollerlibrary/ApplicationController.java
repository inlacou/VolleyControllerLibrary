package com.libraries.inlacou.volleycontrollerlibrary;

import android.app.Application;

import com.libraries.inlacou.volleycontroller.InternetCall;
import com.libraries.inlacou.volleycontroller.VolleyController;

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
		VolleyController.getInstance().addInterceptor(new InternetCall.Interceptor() {
			@Override
			public void intercept(InternetCall internetCall) {
				internetCall
						.putHeader("deviceId", "5")
						.putParam("alwaysParam", "Hey! :D");
			}
		});
	}
}
