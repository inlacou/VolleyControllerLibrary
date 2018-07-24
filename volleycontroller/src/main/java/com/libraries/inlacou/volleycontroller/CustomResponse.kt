package com.libraries.inlacou.volleycontroller

import com.android.volley.Cache
import com.android.volley.NetworkResponse
import com.android.volley.toolbox.HttpHeaderParser

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * Created by inlacou on 17/11/16.
 */
class CustomResponse(response: NetworkResponse) {

	val statusCode: Int
	val isNotModified: Boolean
	val headers: Map<String, String>
	var data: String? = null
		private set
	val networkTimeMs: Long
	val chacheHeaders: Cache.Entry
	var code: String? = null

	init {
		try {
			data = String(response.data, Charset.forName(VolleyController.logicCallbacks.charset))
		} catch (e: UnsupportedEncodingException) {
			e.printStackTrace()
		}
		chacheHeaders = HttpHeaderParser.parseCacheHeaders(response)
		headers = response.headers
		networkTimeMs = response.networkTimeMs
		isNotModified = response.notModified
		statusCode = response.statusCode
	}

	override fun toString(): String {
		return "{ \"statusCode\": " + statusCode +
				", \"notModified\": " + isNotModified +
				", \"headers\": " + headers +
				", \"data\": " + data +
				", \"networkTimeMs\": " + networkTimeMs +
				", \"cacheHeaders\": " + chacheHeaders +
				" }"
	}
}
