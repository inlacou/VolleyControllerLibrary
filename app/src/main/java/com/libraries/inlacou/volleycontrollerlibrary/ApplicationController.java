package com.libraries.inlacou.volleycontrollerlibrary;

import android.app.Application;
import android.support.annotation.NonNull;
import android.util.Log;
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

import kotlin.Unit;
import kotlin.jvm.functions.Function2;
import timber.log.Timber;

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
		
		Timber.plant(new Timber.DebugTree());
		
		VolleyController.INSTANCE.init(this, true, new VolleyController.LogicCallbacks() {
			@NotNull
			@Override
			public InternetCall doRefreshToken(@NotNull List<? extends Function2<? super CustomResponse, ? super String, Unit>> successCb, @NotNull List<? extends Function2<? super VolleyError, ? super String, Unit>> errorCb) {
				return null;
			}
			
			@Override
			public void onRefreshTokenExpired(@NotNull VolleyError volleyError, @Nullable String code) {
			
			}
			
			@Override
			public void onRefreshTokenInvalid(@NotNull VolleyError volleyError, @Nullable String code) {
			
			}
			
			@Override
			public void setTokens(@NotNull JSONObject jsonObject) {
			
			}
			
			@NotNull
			@Override
			public String getCharset() {
				return VolleyController.CharSetNames.UTF_8.toString();
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
				return "authtoken";
			}
			
			@NotNull
			@Override
			public String getRefreshToken() {
				return "refreshtoken";
			}
		});
		VolleyController.INSTANCE.addInterceptor(new InternetCall.Interceptor() {
			@Override
			public void intercept(@NonNull InternetCall internetCall) {
				internetCall
						.putHeader("deviceId", "5")
						.putParam("alwaysParam", "Hey! :D")
						.addSuccessCallback(new Function2<CustomResponse, String, Unit>() {
							@Override
							public Unit invoke(CustomResponse response, String code) {
								try {
									Toast.makeText(ApplicationController.this, response.getData().substring(0, 20) + "...", Toast.LENGTH_SHORT).show();
								}catch (IndexOutOfBoundsException ioobe){
									Toast.makeText(ApplicationController.this, response.getData().substring(0, response.getData().length()), Toast.LENGTH_SHORT).show();
								}
								return null;
							}
						})
						.addErrorCallback(new Function2<VolleyError, String, Unit>() {
							@Override
							public Unit invoke(VolleyError error, String code) {
								Log.d(DEBUG_TAG, "Code: " + code + " | VolleyError: " + error);
								return null;
							}
						});
			}
		});
	}
}
