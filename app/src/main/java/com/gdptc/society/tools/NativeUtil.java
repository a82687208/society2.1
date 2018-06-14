/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gdptc.society.tools;

import android.graphics.Bitmap;
import android.os.Build;

public class NativeUtil {
	private static final int DEFAULT_QUALITY = 80;

	public static void compressBitmap(Bitmap bit, String fileName,
			boolean optimize) {
		compressBitmap(bit, DEFAULT_QUALITY, fileName, optimize);

	}

	public static void compressBitmap(Bitmap bit, int quality, String fileName,
			boolean optimize) {
			compressBitmap(bit, bit.getWidth(), bit.getHeight(), quality,
				fileName.getBytes(), optimize);
	}

	private static native String compressBitmap(Bitmap bit, int w, int h,
			int quality, byte[] fileNameBytes, boolean optimize);

	static {
		if (Build.CPU_ABI.equals("arm64-v8a") || Build.CPU_ABI.equals("armeabi-v7a"))
			System.loadLibrary("jpegTurbo_neon");
		else
			System.loadLibrary("jpegbither");

		System.loadLibrary("bitherjni");
	}

}
