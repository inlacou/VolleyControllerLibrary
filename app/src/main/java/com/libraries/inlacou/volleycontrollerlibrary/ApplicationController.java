package com.libraries.inlacou.volleycontrollerlibrary;

import android.app.Application;
import android.support.annotation.NonNull;
import android.widget.Toast;

import com.android.volley.VolleyError;
import com.libraries.inlacou.volleycontroller.CustomResponse;
import com.libraries.inlacou.volleycontroller.InternetCall;
import com.libraries.inlacou.volleycontroller.VolleyController;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

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
		VolleyController.INSTANCE.init(this, true, new VolleyController.LogicCallbacks() {
			
			@Override
			public void onRefreshTokenExpired(@NotNull VolleyError volleyError, @Nullable String code) {
			
			}
			
			@Override
			public void onRefreshTokenInvalid(@NotNull VolleyError volleyError, @Nullable String code) {
			
			}
			
			@NotNull
			@Override
			public InternetCall doRefreshToken(@Nullable List<? extends VolleyController.IOCallbacks> ioCallbacks) {
				return null;
			}
			
			@Override
			public void setTokens(@NotNull JSONObject jsonObject) {
			
			}
			
			@NotNull
			@Override
			public String getCharset() {
				return null;
			}
			
			@Nullable
			@Override
			public String getAuthTokenExpiredMessage() {
				return null;
			}
			
			@Nullable
			@Override
			public String getRefreshTokenExpiredMessage() {
				return null;
			}
			
			@Nullable
			@Override
			public String getRefreshTokenInvalidMessage() {
				return null;
			}
			
			@NotNull
			@Override
			public String getAuthToken() {
				return null;
			}
			
			@NotNull
			@Override
			public String getRefreshToken() {
				return null;
			}
		});
		VolleyController.INSTANCE.addInterceptor(new InternetCall.Interceptor() {
			@Override
			public void intercept(@NonNull InternetCall internetCall) {
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
