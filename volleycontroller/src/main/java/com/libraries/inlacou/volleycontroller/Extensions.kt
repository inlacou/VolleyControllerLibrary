package com.libraries.inlacou.volleycontroller

import com.android.volley.VolleyError
import java.io.UnsupportedEncodingException

val VolleyError?.errorMessageISO_8859_1: String
	get() {
		return try {
			VolleyController.getMessage(this!!, VolleyController.CharSetNames.ISO_8859_1.toString())
		} catch (e: UnsupportedEncodingException) {
			"unknown"
		} catch (e: NullPointerException) {
			"unknown"
		} catch (e: KotlinNullPointerException) {
			"unknown"
		} catch (e: Exception) {
			"unknown"
		}
	}

val VolleyError?.errorMessageUTF_8: String
	get() {
		return try {
			VolleyController.getMessage(this!!, VolleyController.CharSetNames.UTF_8.toString())
		} catch (e: UnsupportedEncodingException) {
			"unknown"
		} catch (e: NullPointerException) {
			"unknown"
		} catch (e: KotlinNullPointerException) {
			"unknown"
		} catch (e: Exception) {
			"unknown"
		}
	}

val VolleyError?.statusCode: Int
	get() = this?.networkResponse?.statusCode ?: -1
