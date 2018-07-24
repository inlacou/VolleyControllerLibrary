package com.libraries.inlacou.volleycontroller;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;

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
	public static byte[] getFileDataFromBitmap(Bitmap bitmap) {
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}

	public static Bitmap getBitmapFromPath(String filename) throws IOException {
		FileInputStream is = new FileInputStream(filename);
		Bitmap bitmap = BitmapFactory.decodeStream(is);
		is.close();
		return bitmap;
	}
}
