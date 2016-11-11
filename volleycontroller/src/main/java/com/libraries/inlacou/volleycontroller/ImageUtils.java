package com.libraries.inlacou.volleycontroller;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.ByteArrayOutputStream;

/**
 * Created by inlacou on 11/11/16.
 */
public class ImageUtils {
	/**
	 * Turn bitmap into byte array.
	 *
	 * @param bitmap data
	 * @return byte array
	 */
	public static byte[] getFileDataFromBitmap(Context context, Bitmap bitmap) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}
}
