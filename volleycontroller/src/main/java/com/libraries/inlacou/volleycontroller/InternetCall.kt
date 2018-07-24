package com.libraries.inlacou.volleycontroller

import android.content.Context
import android.util.Log

import com.android.volley.AuthFailureError
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response
import com.libraries.inlacou.volleycontroller.multipart.DataPart
import com.libraries.inlacou.volleycontroller.multipart.VolleyMultipartRequest

import java.io.IOException
import java.util.ArrayList
import java.util.HashMap

import timber.log.Timber

/**
 * Created by inlacou on 10/09/14.
 */
class InternetCall {
	private var method: Method = Method.GET
	private var code: String? = null
	private var url: String? = null
	private var params: MutableMap<String, String> = mutableMapOf()
	private var headers: MutableMap<String, String> = mutableMapOf()
	private var rawBody: String = ""
	private var retryPolicy: DefaultRetryPolicy? = null
	private var interceptors: MutableList<Interceptor> = mutableListOf()
	var file: File? = null
		private set
	var callbacks: MutableList<VolleyController.IOCallbacks>? = null
		private set
	private var fileKey: String? = null
	private var cancelTag: Any? = null
	private var allowLocationRedirect: Boolean = true

	init {
		setRetryPolicy(DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
	}

	fun getUrl(): String? {
		return url
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

	fun getRawBody(): String? {
		return rawBody
	}

	fun setRawBody(rawBody: String): InternetCall {
		this.rawBody = rawBody
		return this
	}

	fun setHeaders(headers: MutableMap<String, String>): InternetCall {
		this.headers = headers
		return this
	}

	fun getHeaders(): Map<String, String> {
		return headers
	}

	fun setParams(params: MutableMap<String, String>): InternetCall {
		rawBody = ""
		this.params = params
		return this
	}

	fun getParams(): Map<String, String> {
		return params
	}

	fun addCallback(callback: VolleyController.IOCallbacks): InternetCall {
		if (callbacks == null) {
			callbacks = mutableListOf()
		}
		this.callbacks?.add(callback)
		return this
	}

	fun isAllowLocationRedirect(): Boolean {
		return allowLocationRedirect
	}

	fun setAllowLocationRedirect(b: Boolean): InternetCall {
		allowLocationRedirect = b
		return this
	}

	fun getCode(): String? {
		return code
	}

	fun setCode(code: String): InternetCall {
		this.code = code
		return this
	}

	fun getMethod(): Method {
		return method
	}

	fun setMethod(method: Method): InternetCall {
		this.method = method
		return this
	}

	fun replaceAccessToken(oldAccessToken: String, newAccessToken: String): InternetCall {
		url?.let { setUrl(it.replace(oldAccessToken, newAccessToken)) }

		headers.forEach { key, value -> if(value.contains(oldAccessToken)) headers[key] = value.replace(oldAccessToken, newAccessToken) }
		params.forEach { key, value -> if(value.contains(oldAccessToken)) headers[key] = value.replace(oldAccessToken, newAccessToken) }

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

	fun build(context: Context, listener: com.android.volley.Response.Listener<CustomResponse>, errorListener: com.android.volley.Response.ErrorListener): Request<*> {
		if (file == null) {
			val request = object : CustomRequest(this.getMethod().value(), getUrl(), errorListener) {
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
					val headers = this@InternetCall.getHeaders()
					headers.forEach { key, value -> Log.v(DEBUG_TAG + "." + this@InternetCall.getMethod(), "header -> $key: $value") }
					for ((key, value) in headers) {
						Timber.v(DEBUG_TAG + "." + this@InternetCall.getMethod() + " header -> $key: $value")
					}
					return this@InternetCall.headers
				}

				override fun getParams(): Map<String, String>? {
					val params = this@InternetCall.getParams()
					for ((key, value) in params) {
						Log.v(DEBUG_TAG + "." + this@InternetCall.getMethod(), "params -> $key: $value")
					}
					return params
				}

				@Throws(AuthFailureError::class)
				override fun getBody(): ByteArray {
					val body = this@InternetCall.getRawBody()
					if (body != null && !body.isEmpty()) {
						Log.v(DEBUG_TAG + "." + this@InternetCall.getMethod(), "body -> $body")
						return body.toByteArray()
					}
					return super.getBody()
				}
			}
			request.retryPolicy = retryPolicy
			if (cancelTag != null) request.tag = cancelTag
			return request
		} else {
			val request = object : VolleyMultipartRequest(this.getMethod().value(), getUrl(), errorListener) {
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
						fileKey?.let { params[it] = DataPart(file?.name + "." + file?.format, ImageUtils.getFileDataFromBitmap(context, ImageUtils.getBitmapFromPath(file?.location)), file?.type.toString() + "/" + file?.format) }
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

	fun setInterceptors(interceptors: ArrayList<Interceptor>): InternetCall {
		this.interceptors = interceptors
		return this
	}

	fun addInterceptors(interceptors: ArrayList<Interceptor>?): InternetCall {
		if (interceptors == null) {
			return this
		}
		this.interceptors = object : ArrayList<Interceptor>() {
			init {
				addAll(interceptors)
				addAll(this@InternetCall.interceptors)
			}
		}
		return this
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

	fun getCancelTag(): Any? {
		return cancelTag
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
