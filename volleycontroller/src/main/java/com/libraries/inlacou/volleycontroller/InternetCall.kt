package com.libraries.inlacou.volleycontroller

import android.util.Log
import com.android.volley.*
import com.libraries.inlacou.volleycontroller.multipart.DataPart
import com.libraries.inlacou.volleycontroller.multipart.VolleyMultipartRequest
import java.io.IOException

/**
 * Created by inlacou on 10/09/14.
 */
class InternetCall {
	var method: Method = Method.GET
		private set
	var code: String? = null
		private set
	var url: String? = null
		private set
	var params: MutableMap<String, String> = mutableMapOf()
		private set
	var headers: MutableMap<String, String> = mutableMapOf()
		private set
	var rawBody: String = ""
		private set
	var retryPolicy: DefaultRetryPolicy? = null
		private set
	var interceptors: MutableList<Interceptor> = mutableListOf()
		private set
	var file: File? = null
		private set
	var callbacks: MutableList<VolleyController.IOCallbacks> = mutableListOf()
		private set
	var fileKey: String? = null
		private set
	var cancelTag: Any? = null
		private set
	var allowLocationRedirect: Boolean = true
		private set

	init {
		setRetryPolicy(DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
	}

	fun setUrl(url: String): InternetCall {
		this.url = url
		return this
	}

	fun setFile(key: String, file: File): InternetCall {
		this.fileKey = key
		this.file = file
		return this
	}

	fun setRawBody(rawBody: String): InternetCall {
		this.rawBody = rawBody
		return this
	}

	fun setHeaders(headers: MutableMap<String, String>): InternetCall {
		this.headers = headers
		return this
	}

	fun setParams(params: MutableMap<String, String>): InternetCall {
		rawBody = ""
		this.params = params
		return this
	}

	fun addCallback(callback: VolleyController.IOCallbacks): InternetCall {
		this.callbacks.add(callback)
		return this
	}

	fun setAllowLocationRedirect(b: Boolean): InternetCall {
		allowLocationRedirect = b
		return this
	}

	fun setCode(code: String): InternetCall {
		this.code = code
		return this
	}

	fun setMethod(method: Method): InternetCall {
		this.method = method
		return this
	}

	fun replaceAccessToken(oldAccessToken: String, newAccessToken: String): InternetCall {
		url?.let { setUrl(it.replace(oldAccessToken, newAccessToken)) }

		headers.forEach { if(it.value.contains(oldAccessToken)) headers[it.key] = it.value.replace(oldAccessToken, newAccessToken) }
		params.forEach { if(it.value.contains(oldAccessToken)) headers[it.key] = it.value.replace(oldAccessToken, newAccessToken) }

		if (!rawBody.isEmpty() && rawBody.contains(oldAccessToken)) {
			rawBody = rawBody.replace(oldAccessToken, newAccessToken)
		}

		return this
	}

	fun prebuild(): InternetCall {
		headers.clear()
		params.clear()
		rawBody = ""
		interceptors.forEach { it.intercept(this) }
		return this
	}

	fun build(listener: com.android.volley.Response.Listener<CustomResponse>, errorListener: com.android.volley.Response.ErrorListener): Request<*> {
		if (file == null) {
			val request = object : CustomRequest(this.method.value(), url, errorListener) {
				override fun deliverResponse(response: Any) {
					listener.onResponse(response as CustomResponse)
				}

				override fun parseNetworkResponse(response: NetworkResponse): Response<CustomResponse> {
					val newCustomResponse = CustomResponse(response)
					newCustomResponse.code = code
					//we set here the response (the object received by deliverResponse);
					return com.android.volley.Response.success(newCustomResponse, newCustomResponse.chacheHeaders)
				}

				@Throws(AuthFailureError::class)
				override fun getHeaders(): Map<String, String>? {
					this@InternetCall.headers.forEach { Log.v(DEBUG_TAG + "." + this@InternetCall.method , " header -> ${it.key}: ${it.value}") }
					return this@InternetCall.headers
				}

				override fun getParams(): Map<String, String>? {
					this@InternetCall.params.forEach { Log.v(DEBUG_TAG + "." + this@InternetCall.method , " params -> ${it.key}: ${it.value}") }
					return this@InternetCall.params
				}

				@Throws(AuthFailureError::class)
				override fun getBody(): ByteArray {
					val body = this@InternetCall.rawBody
					if (!body.isEmpty()) {
						Log.v(DEBUG_TAG + "." + this@InternetCall.method, "body -> $body")
						return body.toByteArray()
					}
					return super.getBody()
				}
			}
			request.retryPolicy = retryPolicy
			if (cancelTag != null) request.tag = cancelTag
			return request
		} else {
			val request = object : VolleyMultipartRequest(this.method.value(), url, errorListener) {
				override fun deliverResponse(response: Any) {
					listener.onResponse(response as CustomResponse)
				}

				override fun parseNetworkResponse(response: NetworkResponse): Response<CustomResponse> {
					val newCustomResponse = CustomResponse(response)
					newCustomResponse.code = code
					//we set here the response (the object received by deliverResponse);
					return com.android.volley.Response.success(newCustomResponse, newCustomResponse.chacheHeaders)
				}

				override fun getParams(): Map<String, String>? {
					return this@InternetCall.params
				}

				override fun getByteData(): Map<String, DataPart>? {
					val params = mutableMapOf<String, DataPart>()
					// file name could found file base or direct access from real path
					// for now just get bitmap data from ImageView
					try {
						fileKey?.let { params[it] = DataPart(file?.name + "." + file?.format, ImageUtils.getFileDataFromBitmap(ImageUtils.getBitmapFromPath(file?.location)), file?.type.toString() + "/" + file?.format) }
					} catch (e: IOException) {
						e.printStackTrace()
					}

					return params
				}
			}
			request.retryPolicy = retryPolicy
			if (cancelTag != null) request.tag = cancelTag
			return request
		}
	}

	fun setRetryPolicy(retryPolicy: DefaultRetryPolicy): InternetCall {
		this.retryPolicy = retryPolicy
		return this
	}

	fun addInterceptors(interceptors: List<Interceptor>): InternetCall {
		this.interceptors.addAll(interceptors)
		return this
	}

	fun putInterceptors(interceptors: List<Interceptor>): InternetCall {
		this.interceptors.clear()
		this.interceptors.addAll(interceptors)
		return this
	}

	/**
	 * Alias for putInterceptors
	 */
	fun setInterceptors(interceptors: List<Interceptor>): InternetCall {
		return putInterceptors(interceptors)
	}

	fun addInterceptor(interceptor: Interceptor?): InternetCall {
		if (interceptor == null) {
			return this
		}
		this.interceptors.add(interceptor)
		return this
	}

	fun putHeader(key: String, value: String): InternetCall {
		headers[key] = value
		return this
	}

	fun putParam(key: String, value: String?): InternetCall {
		if (value == null) {
			return this
		}
		rawBody = ""
		params[key] = value
		return this
	}

	fun putHeaders(headers: Map<String, String>?): InternetCall {
		if (headers == null) {
			return this
		}
		this.headers.putAll(headers)
		return this
	}

	fun putParams(params: MutableMap<String, String>): InternetCall {
		params.clear()
		rawBody = ""
		this.params.putAll(params)
		return this
	}

	fun setCancelTag(tag: Any): InternetCall {
		this.cancelTag = tag
		return this
	}


	enum class Method {
		GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS, TRACE;

		internal fun value(): Int {
			return when (this) {
				GET -> Request.Method.GET
				POST -> Request.Method.POST
				PUT -> Request.Method.PUT
				DELETE -> Request.Method.DELETE
				PATCH -> Request.Method.PATCH
				HEAD -> Request.Method.HEAD
				OPTIONS -> Request.Method.OPTIONS
				TRACE -> Request.Method.TRACE
			}
		}

		override fun toString(): String {
			return when (this) {
				GET -> "GET"
				POST -> "POST"
				PUT -> "PUT"
				DELETE -> "DELETE"
				PATCH -> "PATCH"
				HEAD -> "HEAD"
				OPTIONS -> "OPTIONS"
				TRACE -> "TRACE"
			}
		}
	}

	interface Interceptor {
		/**
		 * @param internetCall
		 */
		fun intercept(internetCall: InternetCall)
	}

	companion object {
		private val DEBUG_TAG = InternetCall::class.java.name
	}
}
