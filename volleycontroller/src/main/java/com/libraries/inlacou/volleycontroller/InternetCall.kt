package com.libraries.inlacou.volleycontroller

import com.android.volley.*
import com.android.volley.Response.success
import com.libraries.inlacou.volleycontroller.multipart.DataPart
import com.libraries.inlacou.volleycontroller.multipart.VolleyMultipartRequest
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException

/**
 * Created by inlacou on 10/09/14.
 * Last updated by inlacou on 24/10/18.
 */
class InternetCall {
	var method: Method = Method.GET
		private set
	var code: String = ""
		private set
	var url: String? = null
		private set
	var headers: MutableMap<String, String> = mutableMapOf()
		private set
	var params: MutableMap<String, String> = mutableMapOf()
		private set
	var rawBody: String = ""
		private set
	var fileKey: String? = null
		private set
	var file: File? = null
		private set
	var retryPolicy: DefaultRetryPolicy? = null
		private set
	var interceptors: MutableList<Interceptor> = mutableListOf()
		private set
	var successCallbacks: MutableList<((response: VcResponse, code: String) -> Unit)> = mutableListOf()
		private set
	var errorCallbacks: MutableList<((error: VolleyError, code: String) -> Unit)> = mutableListOf()
		private set
	var cancelTag: Any? = null
		private set
	var allowLocationRedirect: Boolean = true
		private set

	init {
		setRetryPolicy(DefaultRetryPolicy(10000, DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT))
	}

	fun setUrl(url: String, urlEncodeSpaces: Boolean = true): InternetCall {
		this.url = if(urlEncodeSpaces){
			url.replace(" ", "%20")
		}else{
			url
		}
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

	fun setRawBody(json: JSONObject): InternetCall {
		putHeader("Content-type", VolleyController.ContentType.JSON.toString())
		this.rawBody = json.toString()
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

	fun addSuccessCallback(callback: ((item: VcResponse, code: String) -> Unit)): InternetCall {
		this.successCallbacks.add(callback)
		return this
	}

	fun addErrorCallback(callback: ((error: VolleyError, code: String) -> Unit)): InternetCall {
		this.errorCallbacks.add(callback)
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

	/**
	 * Applies any interceptor present
	 * Should be called before build()
	 */
	fun applyInterceptors(){
		interceptors.forEach { it.intercept(this) }
	}

	fun build(listener: Response.Listener<VcResponse>, errorListener: Response.ErrorListener): Request<*> {
		val request: Request<*> = if (file == null) {
			object : CustomRequest(this.method.value(), url, errorListener) {
				override fun deliverResponse(response: Any) {
					listener.onResponse(response as VcResponse)
				}

				override fun parseNetworkResponse(response: NetworkResponse): Response<VcResponse> {
					val newCustomResponse = VcResponse(response)
					newCustomResponse.code = code
					//we set here the response (the object received by deliverResponse);
					return success(newCustomResponse, newCustomResponse.chacheHeaders)
				}

				@Throws(AuthFailureError::class)
				override fun getHeaders(): Map<String, String>? {
					return this@InternetCall.headers
				}

				override fun getParams(): Map<String, String>? {
					return this@InternetCall.params
				}

				@Throws(AuthFailureError::class)
				override fun getBody(): ByteArray {
					val body = this@InternetCall.rawBody
					if (body.isNotEmpty()) {
						return body.toByteArray()
					}
					return super.getBody() ?: "".toByteArray()
				}
			}
		} else {
			object : VolleyMultipartRequest(this.method.value(), url, errorListener) {
				override fun deliverResponse(response: Any) {
					listener.onResponse(response as VcResponse)
				}

				override fun parseNetworkResponse(response: NetworkResponse): Response<VcResponse> {
					val newCustomResponse = VcResponse(response)
					newCustomResponse.code = code
					//we set here the response (the object received by deliverResponse);
					return success(newCustomResponse, newCustomResponse.chacheHeaders)
				}

				override fun getParams(): Map<String, String>? {
					return this@InternetCall.params
				}

				override fun getByteData(): Map<String, DataPart>? {
					val byteData = mutableMapOf<String, DataPart>()
					// file name could found file base or direct access from real path
					// for now just get bitmap data from ImageView
					try {
						fileKey?.let { byteData[it] = DataPart(file?.name + "." + file?.format, ImageUtils.getFileDataFromBitmap(ImageUtils.getBitmapFromPath(file?.location)), file?.type.toString() + "/" + file?.format) }
					} catch (e: IOException) {
						e.printStackTrace()
					}

					byteData.forEach { Timber.v("${this@InternetCall.method} byteData -> ${it.key}: ${it.value}") }

					return byteData
				}
			}
		}
		request.retryPolicy = retryPolicy
		if (cancelTag != null) request.tag = cancelTag
		return request
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
		this.params.clear()
		rawBody = ""
		this.params.putAll(params)
		return this
	}

	fun addParams(params: MutableMap<String, String>): InternetCall {
		rawBody = ""
		this.params.putAll(params)
		return this
	}

	fun setCancelTag(tag: Any): InternetCall {
		this.cancelTag = tag
		return this
	}

	fun get(){
		setMethod(Method.GET)
	}

	fun post(){
		setMethod(Method.POST)
	}

	fun put(){
		setMethod(Method.PUT)
	}

	fun delete(){
		setMethod(Method.DELETE)
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

	fun toPostmanString(): String{
		var result = ""

		result += "Method: $method\n"
		result += "Code: $code\n"
		result += "URL: $url\n"

		if(headers.isNotEmpty()){
			result += "headers:\n"
			headers.forEach { result += "\t${it.key}: ${it.value}\n" }
		}else{
			result += "headers: none\n"
		}

		if(params.isNotEmpty()){
			result += "params:\n"
			params.forEach { result += "\t${it.key}: ${it.value}\n" }
		}else{
			result += "params: none\n"
		}

		if(rawBody.isNotEmpty()) {
			result += "body:\n"
			result += rawBody
		}else{
			result += "body: none\n"
		}

		file?.let {
			result += "file: ${file?.location}\n"
		}

		return result
	}

	interface Interceptor {
		/**
		 * @param internetCall
		 */
		fun intercept(internetCall: InternetCall)
	}
}
