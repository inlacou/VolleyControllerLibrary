package com.libraries.inlacou.volleycontroller

import com.android.volley.Cache
import com.android.volley.NetworkResponse
import com.android.volley.toolbox.HttpHeaderParser

import java.io.UnsupportedEncodingException
import java.nio.charset.Charset

/**
 * Created by inlacou on 17/11/16.
 * Last updated by inlacou on 24/10/18.
 */
class VcResponse(response: NetworkResponse) {

	val statusCode: Int
	val isNotModified: Boolean
	val headers: Map<String, String>
	val data: String?
	val networkTimeMs: Long
	val chacheHeaders: Cache.Entry?
	var code: String? = null

	init {
		data = try {
			String(response.data, Charset.forName(VolleyController.logicCallbacks.charset))
		} catch (e: UnsupportedEncodingException) {
			e.printStackTrace()
			null
		}
		chacheHeaders = HttpHeaderParser.parseCacheHeaders(response)
		headers = response.headers ?: mapOf()
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
