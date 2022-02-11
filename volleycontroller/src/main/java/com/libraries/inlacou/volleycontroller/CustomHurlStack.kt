package com.libraries.inlacou.volleycontroller

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Header
import com.android.volley.Request
import com.android.volley.toolbox.BaseHttpStack
import com.android.volley.toolbox.HttpResponse
import timber.log.Timber
import java.io.DataOutputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLSocketFactory

/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ /** A [BaseHttpStack] based on [HttpURLConnection].  */
class CustomHurlStack
/**
 * @param mUrlRewriter Rewriter to use for request URLs
 * @param mSslSocketFactory SSL factory to use for HTTPS connections
 */
private constructor(private val mUrlRewriter: UrlRewriter?, private val mSslSocketFactory: SSLSocketFactory? =  /* sslSocketFactory = */null) : BaseHttpStack() {
	/** An interface for transforming URLs before use.  */
	interface UrlRewriter {
		/**
		 * Returns a URL to use instead of the provided one, or null to indicate this URL should not
		 * be used at all.
		 */
		fun rewriteUrl(originalUrl: String?): String?
	}
	
	constructor() : this( /* urlRewriter = */null) {}
	
	@Throws(IOException::class, AuthFailureError::class)
	override fun executeRequest(request: Request<*>, additionalHeaders: Map<String, String>): HttpResponse {
		var url = request.url
		val map = HashMap<String, String>()
		map.putAll(request.headers)
		map.putAll(additionalHeaders)
		if (mUrlRewriter != null) {
			val rewritten = mUrlRewriter.rewriteUrl(url)
					?: throw IOException("URL blocked by rewriter: $url")
			url = rewritten
		}
		val parsedUrl = URL(url)
		val connection = openConnection(parsedUrl, request)
		var keepConnectionOpen = false
		return try {
			for (headerName in map.keys) {
				connection.addRequestProperty(headerName, map[headerName])
			}
			setConnectionParametersForRequest(connection, request)
			// Initialize HttpResponse with data from the HttpURLConnection.
			val responseCode = connection.responseCode
			if (responseCode == -1) {
				// -1 is returned by getResponseCode() if the response code could not be retrieved.
				// Signal to the caller that something was wrong with the connection.
				throw IOException("Could not retrieve response code from HttpUrlConnection.")
			}
			if (!hasResponseBody(request.method, responseCode)) {
				return HttpResponse(responseCode, convertHeaders(connection.headerFields))
			}
			
			// Need to keep the connection open until the stream is consumed by the caller. Wrap the
			// stream such that close() will disconnect the connection.
			keepConnectionOpen = true
			HttpResponse(
					responseCode,
					convertHeaders(connection.headerFields),
					connection.contentLength,
					UrlConnectionInputStream(connection))
		} finally {
			if (!keepConnectionOpen) {
				connection.disconnect()
			}
		}
	}
	
	/**
	 * Wrapper for a [HttpURLConnection]'s InputStream which disconnects the connection on
	 * stream close.
	 */
	internal class UrlConnectionInputStream(private val mConnection: HttpURLConnection) : FilterInputStream(inputStreamFromConnection(mConnection)) {
		@Throws(IOException::class)
		override fun close() {
			super.close()
			mConnection.disconnect()
		}
		
	}
	
	/** Create an [HttpURLConnection] for the specified `url`.  */
	@Throws(IOException::class)
	private fun createConnection(url: URL): HttpURLConnection {
		val connection = url.openConnection() as HttpURLConnection
		
		// Workaround for the M release HttpURLConnection not observing the
		// HttpURLConnection.setFollowRedirects() property.
		// https://code.google.com/p/android/issues/detail?id=194495
		connection.instanceFollowRedirects = HttpURLConnection.getFollowRedirects()
		return connection
	}
	
	/**
	 * Opens an [HttpURLConnection] with parameters.
	 *
	 * @param url
	 * @return an open connection
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun openConnection(url: URL, request: Request<*>): HttpURLConnection {
		val connection = createConnection(url)
		val timeoutMs = request.timeoutMs
		connection.connectTimeout = timeoutMs
		connection.readTimeout = timeoutMs
		connection.useCaches = false
		connection.doInput = true
		
		// use caller-provided custom SslSocketFactory, if any, for HTTPS
		if ("https" == url.protocol && mSslSocketFactory != null) {
			(connection as HttpsURLConnection).sslSocketFactory = mSslSocketFactory
		}
		return connection
	}
	
	companion object {
		private const val HTTP_CONTINUE = 100
		private fun convertHeaders(responseHeaders: Map<String?, List<String>>): List<Header> {
			val headerList: MutableList<Header> = ArrayList(responseHeaders.size)
			for ((key, value1) in responseHeaders) {
				// HttpUrlConnection includes the status line as a header with a null key; omit it here
				// since it's not really a header and the rest of Volley assumes non-null keys.
				if (key != null) {
					for (value in value1) {
						headerList.add(Header(key, value))
					}
				}
			}
			return headerList
		}
		
		/**
		 * Checks if a response message contains a body.
		 *
		 * @see [RFC 7230 section 3.3](https://tools.ietf.org/html/rfc7230.section-3.3)
		 *
		 * @param requestMethod request method
		 * @param responseCode response status code
		 * @return whether the response has a body
		 */
		private fun hasResponseBody(requestMethod: Int, responseCode: Int): Boolean {
			return (requestMethod != Request.Method.HEAD && !(HTTP_CONTINUE <= responseCode && responseCode < HttpURLConnection.HTTP_OK)
					&& responseCode != HttpURLConnection.HTTP_NO_CONTENT && responseCode != HttpURLConnection.HTTP_NOT_MODIFIED)
		}
		
		/**
		 * Initializes an [InputStream] from the given [HttpURLConnection].
		 *
		 * @param connection
		 * @return an HttpEntity populated with data from `connection`.
		 */
		private fun inputStreamFromConnection(connection: HttpURLConnection): InputStream {
			return try {
				connection.inputStream
			} catch (ioe: IOException) {
				connection.errorStream
			}
		}
		
		@Throws(IOException::class, AuthFailureError::class)
		private fun setConnectionParametersForRequest(connection: HttpURLConnection, request: Request<*>) {
			if(VolleyController.log) Timber.d("setConnectionParametersForRequest: ${request.method}")
			when (request.method) {
				Request.Method.DEPRECATED_GET_OR_POST -> {
					// This is the deprecated way that needs to be handled for backwards compatibility.
					// If the request's post body is null, then the assumption is that the request is
					// GET.  Otherwise, it is assumed that the request is a POST.
					val postBody = request.postBody
					if (postBody != null) {
						if(VolleyController.log) Timber.w("postBody!=null so setting request method to POST")
						connection.requestMethod = "POST"
						addBody(connection, request, postBody)
					}
					if(VolleyController.log) Timber.d("setRequestMethod DEPRECATED_GET_OR_POST")
				}
				Request.Method.GET -> {
					// Not necessary to set the request method because connection defaults to GET but
					// being explicit here.
					connection.requestMethod = "GET"
					if(VolleyController.log) Timber.d("setRequestMethod GET")
					//Adding a body makes it a POST for google or PokeApi
					//addBodyIfExists(connection, request)
				}
				Request.Method.DELETE -> {
					connection.requestMethod = "DELETE"
					if(VolleyController.log) Timber.d("setRequestMethod DELETE")
					addBodyIfExists(connection, request)
				}
				Request.Method.POST -> {
					connection.requestMethod = "POST"
					if(VolleyController.log) Timber.d("setRequestMethod POST")
					addBodyIfExists(connection, request)
				}
				Request.Method.PUT -> {
					connection.requestMethod = "PUT"
					if(VolleyController.log) Timber.d("setRequestMethod PUT")
					addBodyIfExists(connection, request)
				}
				Request.Method.HEAD -> connection.requestMethod = "HEAD"
				Request.Method.OPTIONS -> connection.requestMethod = "OPTIONS"
				Request.Method.TRACE -> connection.requestMethod = "TRACE"
				Request.Method.PATCH -> {
					connection.requestMethod = "PATCH"
					addBodyIfExists(connection, request)
				}
				else -> throw IllegalStateException("Unknown method type.")
			}
		}
		
		@Throws(IOException::class, AuthFailureError::class)
		private fun addBodyIfExists(connection: HttpURLConnection, request: Request<*>) {
			val body = request.body ?: byteArrayOf()
			if (body != null ) {
				if(VolleyController.log) Timber.d("addBodyIfExists | ${body.map { it }}")
				addBody(connection, request, body)
			}
		}
		
		@Throws(IOException::class)
		private fun addBody(connection: HttpURLConnection, request: Request<*>, body: ByteArray) {
			// Prepare output. There is no need to set Content-Length explicitly,
			// since this is handled by HttpURLConnection using the size of the prepared
			// output stream.
			connection.doOutput = true
			connection.addRequestProperty("Content-Type", request.bodyContentType)
			val out = DataOutputStream(connection.outputStream)
			out.write(body)
			out.close()
		}
	}
}