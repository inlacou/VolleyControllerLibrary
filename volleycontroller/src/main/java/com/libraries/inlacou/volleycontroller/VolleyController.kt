package com.libraries.inlacou.volleycontroller

import android.app.Application
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.Volley
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * Created by inlacou on 25/11/14.
 */
object VolleyController {
	const val JSON_POST_UPDATE_ACCESS_TOKEN = "network_logic_json_post_update_access_token"

	private val temporaryCallQueue = mutableListOf<InternetCall>()
	private var updatingToken = false

	private lateinit var requestQueue: RequestQueue
	private lateinit var secondaryRequestQueue: RequestQueue
	internal lateinit var logicCallbacks: LogicCallbacks
	private lateinit var defaultErrorMessage: String
	private var interceptors = mutableListOf<InternetCall.Interceptor>()

	fun init(application: Application, nukeSSLCerts: Boolean, logicCallbacks: LogicCallbacks) {
		Timber.d("init started")
		if (nukeSSLCerts) {
			NukeSSLCerts.nuke()
		}
		this.logicCallbacks = logicCallbacks
		defaultErrorMessage = application.getString(R.string.network_error)

		//InputStream keystore = getResources().openRawResource(R.raw.boletus); //For SSH
		requestQueue = Volley.newRequestQueue(application//, CustomHurlStack()
				//, new ExtHttpClientStack(new SslHttpClient(keystore, "ss64kdn4", 443)) //For SSH
		)

		//InputStream keystore = getResources().openRawResource(R.raw.boletus); //For SSH
		secondaryRequestQueue = Volley.newRequestQueue(application//, CustomHurlStack()
				//, new ExtHttpClientStack(new SslHttpClient(keystore, "ss64kdn4", 443)) //For SSH
		)
		Timber.d("init finished")
	}

	/**
	 *
	 * @param interceptor
	 */
	fun addInterceptor(interceptor: InternetCall.Interceptor) {
		Timber.d("new interceptor added")
		interceptors.add(interceptor)
	}

	private fun removeFromTemporaryList(code: String?) {
		val i = temporaryCallQueue.size
		for (c in 0 until i) {
			if (temporaryCallQueue[c].code.equals(code!!, ignoreCase = true)) {
				temporaryCallQueue.removeAt(c)
				return
			}
		}
	}

	fun onCall(iCall: InternetCall) {
		onCall(iCall, true)
	}

	private fun onCall(iCall: InternetCall, primaryRequestQueue: Boolean) {
		iCall.addInterceptors(interceptors)

		val mRequestQueue: RequestQueue = if (primaryRequestQueue) {
			requestQueue
		} else {
			secondaryRequestQueue
		}
		if (!iCall.code.equals(JSON_POST_UPDATE_ACCESS_TOKEN, ignoreCase = true)) {
			temporaryCallQueue.add(iCall)
		}

		iCall.applyInterceptors()
		Timber.d("making call:\n${iCall.toPostmanString()}")
		mRequestQueue.add(iCall.build(
				Response.Listener { resp: VcResponse -> this@VolleyController.onResponse(resp, iCall.successCallbacks, iCall.errorCallbacks, iCall.code, iCall.method, iCall.allowLocationRedirect) },
				Response.ErrorListener { volleyError -> this@VolleyController.onResponseError(volleyError, iCall.successCallbacks, iCall.errorCallbacks, iCall.code, iCall.method.toString()) }))
	}

	private fun onResponseFinal(response: VcResponse, successCb: List<((item: VcResponse, code: String) -> Unit)>, errorCb: List<((item: VolleyError, code: String) -> Unit)>, code: String, method: InternetCall.Method, allowLocationRedirect: Boolean) {
		response.headers["Location"].let { locationHeader ->
			if (allowLocationRedirect && locationHeader!=null && !locationHeader.isEmpty()) {
				val call = InternetCall()
				call.setUrl(locationHeader)
				call.setMethod(InternetCall.Method.GET)
				call.setCode("${code}_redirected_by_location_header")
				successCb.forEach { call.addSuccessCallback(it) }
				errorCb.forEach { call.addErrorCallback(it) }
				onCall(call)
			} else {
				successCb.forEach { it.invoke(response, code) }
				removeFromTemporaryList(code)
			}
		}

	}

	private fun onResponse(response: VcResponse, successCb: List<((item: VcResponse, code: String) -> Unit)>, errorCb: List<((item: VolleyError, code: String) -> Unit)>, code: String, method: InternetCall.Method, allowLocationRedirect: Boolean) {
		Timber.d("$method.onResponse.$code | CustomResponse: $response")
		if (code.trim().equals(JSON_POST_UPDATE_ACCESS_TOKEN.trim(), ignoreCase = true)) {
			Timber.d("$method.onResponse.$code | Recibida la respuesta al codigo $JSON_POST_UPDATE_ACCESS_TOKEN, updating tokens.")
			//Save old authToken
			val oldAccessToken = logicCallbacks.authToken
			//Read answer
			try {
				val jsonObject = JSONObject(response.data)
				//Save new tokens
				logicCallbacks.setTokens(jsonObject)
			} catch (e: JSONException) {
				e.printStackTrace()
			}

			//Get new authToken
			val accessToken = logicCallbacks.authToken
			Timber.d("$method.onResponse | Continuando las " + temporaryCallQueue.size + " llamadas almacenadas.")

			for (i in temporaryCallQueue.indices) {
				doCallReplaceTokens(temporaryCallQueue[i], oldAccessToken, accessToken, method.toString())
			}
			updatingToken = false
			requestQueue.start()
		} else {
			onResponseFinal(response, successCb, errorCb, code, method, allowLocationRedirect)
		}
	}

	private fun doCallReplaceTokens(iCall: InternetCall, oldAccessToken: String, accessToken: String, metodo: String) {
		iCall.applyInterceptors()
		requestQueue.add(iCall.replaceAccessToken(oldAccessToken, accessToken).build(
						Response.Listener { resp: VcResponse -> this@VolleyController.onResponse(resp, iCall.successCallbacks, iCall.errorCallbacks, iCall.code, iCall.method, iCall.allowLocationRedirect) },
						Response.ErrorListener { volleyError -> this@VolleyController.onResponseError(volleyError, iCall.successCallbacks, iCall.errorCallbacks, iCall.code, metodo) }))
	}

	private fun onResponseError(volleyError: VolleyError, successCb: List<((item: VcResponse, code: String) -> Unit)>, errorCb: List<((item: VolleyError, code: String) -> Unit)>, code: String, metodo: String) {
		if (volleyError.networkResponse != null) {
			Timber.w("$metodo.onResponseError.$code" +
					"\nStatusCode: ${volleyError.networkResponse.statusCode}" +
					"\nMessage:" +
					"\n${volleyError.errorMessage}")
			if (code.trim { it <= ' ' }.equals(JSON_POST_UPDATE_ACCESS_TOKEN.trim { it <= ' ' }, ignoreCase = true)) {
				Timber.d("$metodo.onResponseError.$code | Recibida la respuesta al codigo $JSON_POST_UPDATE_ACCESS_TOKEN, updating tokens.")
				//There was an error updating access token
				updatingToken = false
				//Restart queue, but do not retry calls
				requestQueue.start()
			}
			if (volleyError.networkResponse.statusCode == 401) {
				Timber.w("$metodo.onResponseError.$code | Detectado un error 401, UNAUTHORIZED.")
				val errorMessage = getErrorMsg(volleyError)
				logicCallbacks.refreshTokenInvalidMessage.let { refreshTokenInvalidMessage ->
					logicCallbacks.refreshTokenExpiredMessage.let { refreshTokenExpiredMessage ->
						logicCallbacks.authTokenExpiredMessage.let { authTokenExpiredMessage ->
							if (refreshTokenInvalidMessage!=null && refreshTokenInvalidMessage.isNotEmpty() && errorMessage.contains(refreshTokenInvalidMessage)) {
								logicCallbacks.onRefreshTokenInvalid(volleyError, code)
							}
							if (refreshTokenExpiredMessage!=null && refreshTokenExpiredMessage.isNotEmpty() && errorMessage.contains(refreshTokenExpiredMessage)) {
								logicCallbacks.onRefreshTokenExpired(volleyError, code)
							} else if (errorMessage.contains("The access token provided has expired.")
									|| errorMessage.contains("The access token provided is invalid.")
									|| errorMessage.contains("UnauthorizedError: jwt expired")
									|| (authTokenExpiredMessage!=null
											&& authTokenExpiredMessage.isNotEmpty()
											&& errorMessage.contains(authTokenExpiredMessage)
											)
							) {
								retry(code, successCb, errorCb)
								return
							}
						}
					}
				}
			}
		} else {
			Timber.w("$metodo.onResponseError.$code networkResponse==null")
		}
		errorCb.forEach { it.invoke(volleyError, code) }
	}

	private fun retry(code: String?, successCb: List<((item: VcResponse, code: String) -> Unit)>, errorCb: List<((item: VolleyError, code: String) -> Unit)>) {
		Timber.d("retry | En retry, desde una llamada con codigo: " + code + ". Estamos ya refrescando el token? " + if (updatingToken) "Si." else "No.")

		if (!updatingToken) {
			updatingToken = true
			Timber.d("retry | Paramos la request queue principal y procedemos a refrescar el token")
			requestQueue.stop()
			cancelAllPrimaryQueue()
			onCall(logicCallbacks.doRefreshToken(successCb, errorCb).setCode(JSON_POST_UPDATE_ACCESS_TOKEN), false)
		}
	}

	//TODO multipart
	/*public void doPostMultipart(final String url, Map<String, String> headers, final Map<String, String> params, final Bitmap bitmap, final String format, final String code, final IOCallbacks callbacks, boolean primaryRequestQueue){
		for (int i=0; i<interceptors.size(); i++){
			interceptors.get(i).intercept(url, headers, params, null);
		}
		VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url, new CustomResponse.Listener<NetworkResponse>() {
			@Override
			public void onResponse(NetworkResponse response) {
				Log.v(DEBUG_TAG, "networkResponse.statusCode" + response.statusCode);
				Log.v(DEBUG_TAG, "networkResponse.data" + new String(response.data));
				Log.v(DEBUG_TAG, "networkResponse.networkTimeMs" + response.networkTimeMs);
				Log.v(DEBUG_TAG, "networkResponse.headers" + response.headers);
				VolleyController.this.onResponse(new String(response.data), callbacks, code, Request.Method.POST);
			}
		}, new CustomResponse.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				NetworkResponse networkResponse = error.networkResponse;
				String errorMessage = "Unknown error";
				if (networkResponse == null) {
					if (error.getClass().equals(TimeoutError.class)) {
						errorMessage = "Request timeout";
					} else if (error.getClass().equals(NoConnectionError.class)) {
						errorMessage = "Failed to connect server";
					}
				}
				Log.i("Error", errorMessage);
				error.printStackTrace();

				VolleyController.this.onResponseError(error, callbacks, code, Request.Method.POST);
			}
		}) {
			@Override
			protected Map<String, String> params {
				return params;
			}

			@Override
			protected Map<String, DataPart> getByteData() {
				Map<String, DataPart> params = new HashMap<>();
				// file name could found file base or direct access from real path
				// for now just get bitmap data from ImageView
				params.put("files", new DataPart("file_avatar."+format, ImageUtils.getFileDataFromBitmap(VolleyController.this.context, bitmap), "image/"+format));
				return params;
			}
		};
		onCall(Request.Method.POST, url, code, multipartRequest, callbacks, headers, params, null, primaryRequestQueue);
	}*/

	fun cancelRequest(tag: Any?) {
		if (tag == null) {
			return
		}
		val filter = RequestQueue.RequestFilter { request ->
			if (request.tag == null) {
				false
			} else {
				request.tag == tag
			}
		}
		requestQueue.cancelAll(filter)
		secondaryRequestQueue.cancelAll(filter)
	}

	private fun cancelAllPrimaryQueue() {
		val filter = RequestQueue.RequestFilter { true }
		requestQueue.cancelAll(filter)
	}

	private fun cancelAllSecondaryQueue() {
		val filter = RequestQueue.RequestFilter { true }
		secondaryRequestQueue.cancelAll(filter)
	}

	fun cancelAll() {
		cancelAllPrimaryQueue()
		cancelAllSecondaryQueue()
	}

	interface LogicCallbacks {

		val refreshToken: String

		val authToken: String

		val refreshTokenInvalidMessage: String?

		val refreshTokenExpiredMessage: String?

		val authTokenExpiredMessage: String?

		val charset: String

		fun setTokens(jsonObject: JSONObject)

		fun doRefreshToken(successCb: List<((item: VcResponse, code: String) -> Unit)>, errorCb: List<((item: VolleyError, code: String) -> Unit)>): InternetCall

		fun onRefreshTokenInvalid(volleyError: VolleyError, code: String?)

		fun onRefreshTokenExpired(volleyError: VolleyError, code: String?)
	}


	enum class ContentType {
		TEXT, TEXT_PLAIN, JSON, JAVASCRIPT, XML_APPLICATION, XML_TEXT, HTML;

		override fun toString(): String {
			return when (this) {
				TEXT -> "text"
				TEXT_PLAIN -> "text/plain"
				JSON -> "application/json"
				JAVASCRIPT -> "application/javascript"
				XML_APPLICATION -> "application/xml"
				XML_TEXT -> "text/xml"
				HTML -> "text/html"
			}
		}
	}

	enum class CharSetNames {
		ISO_8859_1, UTF_8;

		override fun toString(): String {
			return when (this) {
				ISO_8859_1 -> "ISO-8859-1"
				UTF_8 -> "UTF-8"
			}
		}
	}

	//Utilities
	@JvmOverloads
	internal fun getErrorMsg(error: VolleyError, charset: String = logicCallbacks.charset): String {
		return try {
			String(error.networkResponse.data, Charset.forName(charset))
		} catch (e: UnsupportedEncodingException) {
			e.printStackTrace()
			defaultErrorMessage
		} catch (npe: NullPointerException) {
			npe.printStackTrace()
			defaultErrorMessage
		}
	}

}
