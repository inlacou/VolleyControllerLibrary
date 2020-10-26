package com.libraries.inlacou.volleycontroller

import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.Response

/**
 * Created by inlacou on 17/11/16.
 */
open class CustomRequest(method: Int, url: String?, listener: Response.ErrorListener?) : Request<VcResponse>(method, url, listener) {
	override fun parseNetworkResponse(response: NetworkResponse): Response<VcResponse>? {
		return null
	}
	
	override fun deliverResponse(response: VcResponse) {
		//Do nothing, we will override it
	}
}