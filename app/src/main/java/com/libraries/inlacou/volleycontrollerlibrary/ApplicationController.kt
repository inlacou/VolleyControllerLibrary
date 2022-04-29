package com.libraries.inlacou.volleycontrollerlibrary

import android.app.Application

import com.android.volley.VolleyError
import com.libraries.inlacou.volleycontroller.InternetCall
import com.libraries.inlacou.volleycontroller.VcResponse
import com.libraries.inlacou.volleycontroller.VolleyController
import org.json.JSONObject

import timber.log.Timber

/**
 * Created by inlacou on 14/11/16.
 */
class ApplicationController : Application() {

	override fun onCreate() {
		super.onCreate()

		Timber.plant(Timber.DebugTree())

		val data: String? = null

		Timber.e("data.isEmpty(): ${data?.isEmpty()}")
		Timber.e("data.isEmpty()==true: ${data?.isEmpty()==true}")
		Timber.e("data.isEmpty()==false: ${data?.isEmpty()==false}")
		Timber.e("data.isEmpty()==null: ${data?.isEmpty()==null}")

		VolleyController.init(context = this, logicCallbacks = object : VolleyController.LogicCallbacks {

			override val charset: String get() = VolleyController.CharSetNames.UTF_8.toString()
			override val authTokenExpiredMessage: String? get() = null
			override val refreshTokenExpiredMessage: String? get() = null
			override val refreshTokenInvalidMessage: String? get() = null
			override val authToken: String get() = "authtoken"
			override val refreshToken: String get() = "refreshtoken"
			override fun doRefreshToken(successCb: List<Function2<VcResponse, String, Unit>>, errorCb: List<Function2<VolleyError, String, Unit>>): InternetCall {
				return InternetCall()
			}
			override fun onRefreshTokenExpired(volleyError: VolleyError, code: String?) {}
			override fun onRefreshTokenInvalid(volleyError: VolleyError, code: String?) {}
			override fun setTokens(jsonObject: JSONObject) {}
		}, nukeSSLCerts = false, log = true)
		/*VolleyController.addInterceptor(object : InternetCall.Interceptor {
			override fun intercept(internetCall: InternetCall) {
				internetCall
						.putHeader("deviceId", "5")
						.addSuccessCallback { response, code ->
							try {
								Toast.makeText(this@ApplicationController, response.data!!.substring(0, 20) + "...", Toast.LENGTH_SHORT).show()
							} catch (ioobe: IndexOutOfBoundsException) {
								Toast.makeText(this@ApplicationController, response.data!!.substring(0, response.data!!.length), Toast.LENGTH_SHORT).show()
							}
						}
			}
		})*/
	}

	companion object {
		val instance = ApplicationController()
	}
}
