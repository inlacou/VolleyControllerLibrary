package com.libraries.inlacou.volleycontroller

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object NukeSSLCerts {
	fun nuke() {
		try {
			val trustAllCerts = arrayOf<TrustManager>(
					object : X509TrustManager {
						override fun getAcceptedIssuers(): Array<X509Certificate?> {
							return arrayOfNulls(0)
						}
						
						override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
						override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
					}
			)
			val sc = SSLContext.getInstance("SSL")
			sc.init(null, trustAllCerts, SecureRandom())
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.socketFactory)
			HttpsURLConnection.setDefaultHostnameVerifier { arg0, arg1 -> true }
		} catch (e: Exception) {
		}
	}
}