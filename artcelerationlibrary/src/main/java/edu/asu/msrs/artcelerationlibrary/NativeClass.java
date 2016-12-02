/**
 * This is a java class which contains the declaration of all native methods.
 * Using this class, we are calling the native methods inside our android code.
 */
package edu.asu.msrs.artcelerationlibrary;

import android.graphics.Bitmap;

/**
 * Created by salvi on 11/28/2016.
 */

public class NativeClass {
    public native static String getMessageFromJNI();
    public native static Bitmap colorfilterndk(Bitmap bmp, int arr[]);
    public native static Bitmap motionblurneon(Bitmap bmp, int temp0, int temp1);
    public native static Bitmap rotateBitmapCcw90(Bitmap bitmap);
}
