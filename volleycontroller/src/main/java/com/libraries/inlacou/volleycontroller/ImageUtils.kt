package com.libraries.inlacou.volleycontroller

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.io.IOException

/**
 * Created by inlacou on 11/11/16.
 */
object ImageUtils {
	/**
	 * Turn bitmap into byte array.
	 *
	 * @param bitmap data
	 * @return byte array
	 */
	@JvmStatic
	fun getFileDataFromBitmap(bitmap: Bitmap): ByteArray {
		val byteArrayOutputStream = ByteArrayOutputStream()
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
		return byteArrayOutputStream.toByteArray()
	}
	
	@JvmStatic
	@Throws(IOException::class)
	fun getBitmapFromPath(filename: String?): Bitmap {
		val fis = FileInputStream(filename)
		val bitmap = BitmapFactory.decodeStream(fis)
		fis.close()
		return bitmap
	}
}