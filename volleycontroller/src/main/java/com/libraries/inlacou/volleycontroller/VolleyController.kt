package com.libraries.inlacou.volleycontroller

import android.app.Application
import com.android.volley.Request
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
	private val DEBUG_TAG = VolleyController::class.java.simpleName

	private val temporaryCallQueue = mutableListOf<InternetCall>()
	private var updatingToken = false

	lateinit var requestQueue: RequestQueue
	lateinit var secondaryRequestQueue: RequestQueue
	private lateinit var logicCallbacks: LogicCallbacks
	private lateinit var errorMessage: String
	private var interceptors = mutableListOf<InternetCall.Interceptor>()

	fun init(application: Application, nukeSSLCerts: Boolean, logicCallbacks: LogicCallbacks) {
		if (nukeSSLCerts) {
			NukeSSLCerts.nuke()
		}
		errorMessage = application.getString(R.string.network_error)
		this.logicCallbacks = logicCallbacks

		//InputStream keystore = getResources().openRawResource(R.raw.boletus); //For SSH
		requestQueue = Volley.newRequestQueue(application, CustomHurlStack()
				//, new ExtHttpClientStack(new SslHttpClient(keystore, "ss64kdn4", 443)) //For SSH
		)

		//InputStream keystore = getResources().openRawResource(R.raw.boletus); //For SSH
		secondaryRequestQueue = Volley.newRequestQueue(application, CustomHurlStack()
				//, new ExtHttpClientStack(new SslHttpClient(keystore, "ss64kdn4", 443)) //For SSH
		)
	}

	/**
	 *
	 * @param interceptor
	 */
	fun addInterceptor(interceptor: InternetCall.Interceptor) {
		interceptors.add(interceptor)
	}

	/*@Throws(IOException::class)
	fun convertInputStreamToString(inputStream: InputStream): String {
		val bufferedReader = BufferedReader(InputStreamReader(inputStream))
		var line = " "
		var result = " "
		while ((line = bufferedReader.readLine()) != null) {
			result += line
		}
		inputStream.close()
		return result
	}*/

	private fun methodToString(method: Int?): String {
		if (method == null) {
			return "method-null"
		}
		return when (method) {
			Request.Method.GET -> "get"
			Request.Method.POST -> "post"
			Request.Method.PUT -> "put"
			Request.Method.DELETE -> "delete"
			Request.Method.PATCH -> "patch"
			Request.Method.HEAD -> "head"
			Request.Method.OPTIONS -> "options"
			Request.Method.TRACE -> "trace"
			else -> "method-unknown"
		}
	}

	private fun removeFromTemporaryList(code: String?) {
		val i = temporaryCallQueue.size
		for (c in 0 until i) {
			if (temporaryCallQueue[c].getCode()!!.equals(code!!, ignoreCase = true)) {
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

		val mRequestQueue: RequestQueue?
		if (primaryRequestQueue) {
			mRequestQueue = requestQueue
		} else {
			mRequestQueue = secondaryRequestQueue
		}
		if (iCall.getCode() != null && !iCall.getCode()!!.equals(JSON_POST_UPDATE_ACCESS_TOKEN, ignoreCase = true)) {
			temporaryCallQueue.add(iCall)
		}
		iCall.prebuild()

		Timber.d(DEBUG_TAG + ".onCall." + iCall.getMethod() + "." + iCall.getCode() + " url: " + iCall.getUrl() + " | requestQueue " + if (primaryRequestQueue) "primary" else "secondary")
		logMap(iCall.getHeaders(), "header", iCall.getMethod().toString(), iCall.getCode())
		logMap(iCall.getParams(), "params", iCall.getMethod().toString(), iCall.getCode())
		Timber.d(DEBUG_TAG + ".onCall." + iCall.getMethod() + "." + iCall.getCode() + " Rawbody: " + iCall.getRawBody())

		mRequestQueue.add(iCall.build(Response.Listener { s -> this@VolleyController.onResponse(s, iCall.callbacks, iCall.getCode(), iCall.getMethod(), iCall.isAllowLocationRedirect()) }, Response.ErrorListener { volleyError -> this@VolleyController.onResponseError(volleyError, iCall.callbacks, iCall.getCode(), iCall.getMethod().toString()) }))
	}

	private fun logMap(map: Map<String, String>?, type: String, method: String, code: String?) {
		if (map != null) {
			if (map.isEmpty()) Timber.d("$DEBUG_TAG.onCall.$method.$code Map($type) = $map")
			for (s in map.keys) {
				Timber.d(DEBUG_TAG + ".onCall." + method + "." + code + " " + type + " parameter " + s + ": " + map[s])
			}
		} else {
			Timber.d("$DEBUG_TAG.onCall.$method.$code Map($type) = null")
		}
	}

	private fun onResponseFinal(response: CustomResponse, ioCallbacks: List<IOCallbacks>, code: String?, method: InternetCall.Method, allowLocationRedirect: Boolean) {
		Timber.d("$DEBUG_TAG.$method.onResponseFinal.$code | Method: $method| CustomResponse: $response")
		response.headers["Location"].let { locationHeader ->
			if (allowLocationRedirect && locationHeader!=null && !locationHeader.isEmpty()) {
				val call = InternetCall()
				call.setUrl(locationHeader)
				call.setMethod(InternetCall.Method.GET)
				call.setCode(code!!)
				ioCallbacks.forEach { call.addCallback(it) }
				onCall(call)
			} else {
				ioCallbacks.forEach { it.onResponse(response, code) }
				removeFromTemporaryList(code)
			}
		}

	}

	private fun onResponse(response: CustomResponse, ioCallbacks: List<IOCallbacks>?, code: String?, method: InternetCall.Method, allowLocationRedirect: Boolean) {
		Timber.d("$DEBUG_TAG.$method.onResponse.$code | CustomResponse: $response")
		if (code != null && code.trim { it <= ' ' }.equals(JSON_POST_UPDATE_ACCESS_TOKEN.trim { it <= ' ' }, ignoreCase = true)) {
			Timber.d("$DEBUG_TAG.$method.onResponse.$code | Recibida la respuesta al codigo $JSON_POST_UPDATE_ACCESS_TOKEN, updating tokens.")
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
			Timber.d(DEBUG_TAG + "." + method + ".onResponse | Continuando las " + temporaryCallQueue.size + " llamadas almacenadas.")

			for (i in temporaryCallQueue.indices) {
				doCallReplaceTokens(temporaryCallQueue[i], oldAccessToken, accessToken, method.toString())
			}
			updatingToken = false
			requestQueue.start()
		} else {
			if (ioCallbacks != null) {
				onResponseFinal(response, ioCallbacks, code, method, allowLocationRedirect)
			}
		}
	}

	private fun doCallReplaceTokens(iCall: InternetCall, oldAccessToken: String, accessToken: String, metodo: String) {
		requestQueue.add(iCall.replaceAccessToken(oldAccessToken, accessToken)
				.prebuild().build(Response.Listener { s -> this@VolleyController.onResponseFinal(s, iCall.callbacks, iCall.getCode(), iCall.getMethod(), iCall.isAllowLocationRedirect()) }, Response.ErrorListener { volleyError -> this@VolleyController.onResponseError(volleyError, iCall.callbacks, iCall.getCode(), metodo) }))
	}

	private fun onResponseError(volleyError: VolleyError, ioCallbacks: List<IOCallbacks>?, code: String?, metodo: String) {
		if (volleyError.networkResponse != null) {
			Timber.w(DEBUG_TAG + "." + metodo + ".onResponseError." + code + "| StatusCode: " + volleyError.networkResponse.statusCode)
			try {
				Timber.w(DEBUG_TAG + "." + metodo + ".onResponseError." + code + " | Message: " + String(volleyError.networkResponse.data, Charset.forName(logicCallbacks.charset)))
				if (volleyError.networkResponse.statusCode == 401) {
					Timber.w("$DEBUG_TAG.$metodo.onResponseError.$code | Detectado un error 401, UNAUTHORIZED.")
					val jsonObject = JSONObject(getMessage(volleyError))
					logicCallbacks.refreshTokenInvalidMessage.let { refreshTokenInvalidMessage ->
						logicCallbacks.refreshTokenExpiredMessage.let { refreshTokenExpiredMessage ->
							logicCallbacks.authTokenExpiredMessage.let { authTokenExpiredMessage ->
								if (refreshTokenInvalidMessage!=null && refreshTokenInvalidMessage.isNotEmpty() && jsonObject.toString().contains(refreshTokenInvalidMessage)) {
									logicCallbacks.onRefreshTokenInvalid(volleyError, code)
								}
								if (refreshTokenExpiredMessage!=null && refreshTokenExpiredMessage.isNotEmpty() && jsonObject.toString().contains(refreshTokenExpiredMessage)) {
									logicCallbacks.onRefreshTokenExpired(volleyError, code)
								} else if (jsonObject.toString().contains("The access token provided has expired.")
										|| jsonObject.toString().contains("The access token provided is invalid.")
										|| jsonObject.toString().contains("UnauthorizedError: jwt expired")
										|| (authTokenExpiredMessage!=null
												&& authTokenExpiredMessage.isNotEmpty()
												&& jsonObject.toString().contains(authTokenExpiredMessage
										))) {
									retry(code, ioCallbacks)
									return
								}
							}
						}
					}

				}
			} catch (e: Exception) {
				e.printStackTrace()
			}

		} else {
			Timber.w("$DEBUG_TAG.$metodo.onResponseError.$code networkResponse==null")
		}
		if (ioCallbacks != null) {
			for (i in ioCallbacks.indices) {
				ioCallbacks[i].onResponseError(volleyError, code)
			}
		}
	}

	private fun retry(code: String?, ioCallbacks: List<IOCallbacks>?) {
		Timber.d(DEBUG_TAG + ".retry | En retry, desde una llamada con codigo: " + code + ". Estamos ya refrescando el token? " + if (updatingToken) "Si." else "No.")

		if (!updatingToken) {
			updatingToken = true
			Timber.d("$DEBUG_TAG.retry | Paramos la request queue principal y procedemos a refrescar el token")
			requestQueue.stop()
			cancelAllPrimaryQueue()
			onCall(logicCallbacks.doRefreshToken(ioCallbacks).setCode(JSON_POST_UPDATE_ACCESS_TOKEN), false)
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
			protected Map<String, String> getParams() {
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

	fun cancelAllPrimaryQueue() {
		val filter = RequestQueue.RequestFilter { true }
		requestQueue.cancelAll(filter)
	}

	fun cancelAllSecondaryQueue() {
		val filter = RequestQueue.RequestFilter { true }
		secondaryRequestQueue.cancelAll(filter)
	}

	fun cancelAll() {
		cancelAllPrimaryQueue()
		cancelAllSecondaryQueue()
	}

	interface IOCallbacks {
		/**
		 *
		 * @param response
		 * @param code
		 */
		fun onResponse(response: CustomResponse, code: String?)

		/**
		 *
		 * @param error
		 * @param code
		 */
		fun onResponseError(error: VolleyError, code: String?)
	}

	interface LogicCallbacks {

		val refreshToken: String

		val authToken: String

		val refreshTokenInvalidMessage: String?

		val refreshTokenExpiredMessage: String?

		val authTokenExpiredMessage: String?

		val charset: String

		fun setTokens(jsonObject: JSONObject)

		fun doRefreshToken(ioCallbacks: List<IOCallbacks>?): InternetCall

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
	fun getStatusCode(error: VolleyError): Int {
		return try {
			error.networkResponse.statusCode
		} catch (e: NullPointerException) {
			e.printStackTrace()
			-1
		}

	}

	@JvmOverloads
	fun getMessage(error: VolleyError, charset: String = logicCallbacks.charset): String {
		return try {
			String(error.networkResponse.data, Charset.forName(charset))
		} catch (e: UnsupportedEncodingException) {
			e.printStackTrace()
			errorMessage
		} catch (npe: NullPointerException) {
			npe.printStackTrace()
			errorMessage
		}
	}

}
