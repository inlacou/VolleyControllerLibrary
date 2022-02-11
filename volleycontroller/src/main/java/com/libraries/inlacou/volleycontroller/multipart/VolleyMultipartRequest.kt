package com.libraries.inlacou.volleycontroller.multipart

import com.android.volley.*
import kotlin.Throws
import com.libraries.inlacou.volleycontroller.multipart.DataPart
import com.libraries.inlacou.volleycontroller.VcResponse
import com.android.volley.toolbox.HttpHeaderParser
import com.libraries.inlacou.volleycontroller.VolleyController
import timber.log.Timber
import com.libraries.inlacou.volleycontroller.multipart.VolleyMultipartRequest
import java.io.*
import java.lang.Exception
import java.lang.RuntimeException
import kotlin.math.min

/**
 * Custom request to make multipart header and upload file.
 *
 * Sketch Project Studio
 * Created by Angga on 27/04/2016 12.05.
 */
abstract class VolleyMultipartRequest : Request<Any?> {
	private val twoHyphens = "--"
	private val lineEnd = "\r\n"
	private val boundary = "apiclient-" + System.currentTimeMillis()
	private var mErrorListener: Response.ErrorListener
	private var mHeaders: Map<String, String>? = null

	/**
	 * Default constructor with predefined header and post method.
	 *
	 * @param url           request destination
	 * @param headers       predefined custom header
	 * @param errorListener on error http or library timeout
	 */
	constructor(
		url: String?, headers: Map<String, String>?, errorListener: Response.ErrorListener
	) : super(Method.POST, url, errorListener) {
		this.mErrorListener = errorListener
		mHeaders = headers
	}

	/**
	 * Constructor with option method and default header configuration.
	 *
	 * @param method        method for now accept POST and GET only
	 * @param url           request destination
	 * @param errorListener on error event handler
	 */
	constructor(
		method: Int, url: String?, errorListener: Response.ErrorListener
	) : super(method, url, errorListener) {
		this.mErrorListener = errorListener
	}

	@Throws(AuthFailureError::class)
	override fun getHeaders(): Map<String, String> {
		return if (mHeaders != null) mHeaders!! else super.getHeaders()
	}

	override fun getBodyContentType(): String {
		return "multipart/form-data;boundary=$boundary"
	}

	@Throws(AuthFailureError::class)
	override fun getBody(): ByteArray? {
		val bos = ByteArrayOutputStream()
		val dos = DataOutputStream(bos)
		try {			// populate text payload
			val params = params
			if (params != null && params.isNotEmpty()) {
				textParse(dos, params, paramsEncoding)
			}

			// populate data byte payload
			val data = getByteData()
			if (data != null && data.isNotEmpty()) {
				dataParse(dos, data)
			}

			// close multipart form data after text and file data
			dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd)
			return bos.toByteArray()
		} catch (e: IOException) {
			e.printStackTrace()
		}
		return null
	}

	/**
	 * Custom method handle data payload.
	 *
	 * @return Map data part label with data byte
	 * @throws AuthFailureError
	 */
	protected abstract fun getByteData(): Map<String, DataPart>?

	override fun parseNetworkResponse(response: NetworkResponse): Response<Any?> {
		return try {
			val customResponse = VcResponse(response)
			Response.success<Any>(
				customResponse, HttpHeaderParser.parseCacheHeaders(response)
			)
		} catch (e: Exception) {
			Response.error(ParseError(e))
		}
	}

	/*override fun parseNetworkResponse(response: NetworkResponse): Response<VcResponse>? {
		return try {
			val customResponse = VcResponse(response)
			Response.success<VcResponse>(
				customResponse, HttpHeaderParser.parseCacheHeaders(response)
			)
		} catch (e: Exception) {
			Response.error(ParseError(e))
		}
	}*/

	override fun deliverResponse(response: Any?) {		//Do nothing, we will override it
	}

	override fun deliverError(error: VolleyError) {
		mErrorListener.onErrorResponse(error)
	}

	/**
	 * Parse string map into data output stream by key and value.
	 *
	 * @param dataOutputStream data output stream handle string parsing
	 * @param params           string inputs collection
	 * @param encoding         encode the inputs, default UTF-8
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun textParse(
		dataOutputStream: DataOutputStream,
		params: Map<String, String>,
		encoding: String
	) {
		if (VolleyController.log) Timber.d("textParse")
		try {
			for ((key, value) in params) {
				buildTextPart(dataOutputStream, key, value)
			}
		} catch (uee: UnsupportedEncodingException) {
			throw RuntimeException("Encoding not supported: $encoding", uee)
		}
	}

	/**
	 * Parse data into data output stream.
	 *
	 * @param dataOutputStream data output stream handle file attachment
	 * @param data             loop through data
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun dataParse(dataOutputStream: DataOutputStream, data: Map<String, DataPart>) {
		for ((key, value) in data) {
			buildDataPart(dataOutputStream, value, key)
		}
	}

	/**
	 * Write string data into header and data output stream.
	 *
	 * @param dataOutputStream data output stream handle string parsing
	 * @param parameterName    name of input
	 * @param parameterValue   value of input
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun buildTextPart(
		dataOutputStream: DataOutputStream,
		parameterName: String,
		parameterValue: String
	) {
		if (VolleyController.log) Timber.d("buildTextPart $parameterName -> $parameterValue")
		dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd)
		dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"$parameterName\"$lineEnd")		//dataOutputStream.writeBytes("Content-Type: text/plain; charset=UTF-8" + lineEnd);
		dataOutputStream.writeBytes(lineEnd)
		dataOutputStream.writeBytes(parameterValue + lineEnd)
	}

	/**
	 * Write data file into header and data output stream.
	 *
	 * @param dataOutputStream data output stream handle data parsing
	 * @param dataFile         data byte as DataPart from collection
	 * @param inputName        name of data input
	 * @throws IOException
	 */
	@Throws(IOException::class)
	private fun buildDataPart(
		dataOutputStream: DataOutputStream,
		dataFile: DataPart,
		inputName: String
	) {
		dataOutputStream.writeBytes(twoHyphens + boundary + lineEnd)
		dataOutputStream.writeBytes(
			"Content-Disposition: form-data; name=\"" + inputName + "\"; filename=\"" + dataFile.fileName + "\"" + lineEnd
		)
		if (dataFile.type!=null && dataFile.type.trim { it <= ' ' }.isNotEmpty()) {
			dataOutputStream.writeBytes("Content-Type: " + dataFile.type + lineEnd)
		}
		dataOutputStream.writeBytes(lineEnd)
		val fileInputStream = ByteArrayInputStream(dataFile.content)
		var bytesAvailable = fileInputStream.available()
		val maxBufferSize = 1024 * 1024
		var bufferSize = min(bytesAvailable, maxBufferSize)
		val buffer = ByteArray(bufferSize)
		var bytesRead = fileInputStream.read(buffer, 0, bufferSize)
		while (bytesRead > 0) {
			dataOutputStream.write(buffer, 0, bufferSize)
			bytesAvailable = fileInputStream.available()
			bufferSize = Math.min(bytesAvailable, maxBufferSize)
			bytesRead = fileInputStream.read(buffer, 0, bufferSize)
		}
		dataOutputStream.writeBytes(lineEnd)
	}
}