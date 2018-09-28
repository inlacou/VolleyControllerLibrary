package com.libraries.inlacou.volleycontroller

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class InternetCallTest {
	@Test
	@Throws(Exception::class)
	fun url_encoding() {
		val call = InternetCall()
		call.setUrl("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11 dd")
		Assert.assertEquals("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11%20dd", call.url)
		call.setUrl("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11 dd", true)
		Assert.assertEquals("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11%20dd", call.url)
		call.setUrl("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11 dd?name=hola primo&surname=que tal")
		Assert.assertEquals("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11%20dd?name=hola%20primo&surname=que%20tal", call.url)
		call.setUrl("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11 dd", false)
		Assert.assertEquals("https://bigshowi-api-pre.herokuapp.com/api/vehiculos/matricula/11 dd", call.url)
	}
}