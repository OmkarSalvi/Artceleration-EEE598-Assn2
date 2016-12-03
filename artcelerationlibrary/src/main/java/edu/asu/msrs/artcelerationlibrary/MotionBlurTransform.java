/**
 * This class performs the Motion blur transform requested from service
 * It implements the runnable interface because we create a thread of this class to compute the image transform
 */
package edu.asu.msrs.artcelerationlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;


/**
 * Created by abhishek on 11/7/2016.
 */

public class MotionBlurTransform implements Runnable {

    // Debug tag for the GaussianBlurTransform class
    String TAG = "MotionBlurTransform";
    /**
     * Defining variables to access data passed to thread from service
     */
    int byteArraySize;
    Bundle serviceDataBundle;
    int req;
    Messenger messengerGBT;
    static final int OPTION_0 = 0;
    static final int OPTION_1 = 1;


    /**
     * Constructor to initialize class variables
     *
     * @param size:    byte Array received from the thread
     * @param bund:    Data bundle which has ParcelFileDescriptor object
     * @param request: Index of the processing transform
     * @param gbt:     Messenger object to processed transform to the
     */
    MotionBlurTransform(int size, Bundle bund, int request, Messenger gbt) {
        byteArraySize = size;
        serviceDataBundle = bund;
        req = request;
        messengerGBT = gbt;
    }

    /**
     * Load native library
     */
    static {
        System.loadLibrary("MyLibs");
    }

    @Override
    public void run() {
        /**
         * Unbinding data from the messenger object and bundle
         */
        Log.d(TAG, "Passed size to thread from service: " + byteArraySize);
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor) serviceDataBundle.get("libPFD");
        int[] intArgs = (int[]) serviceDataBundle.get("intArray");

        /**
         * Setting the radius and sigma values for gaussian transform
         */
        int OPTION = intArgs[0];
        int Radius = intArgs[1];

        Log.d(TAG, "Radius : " + Radius);

        /**
         * Storing the byte array received in the buffer
         */
        ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        try {
            input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap recvBmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, opts);

        /**
         * calling native method
         */
        Bitmap aftermotionblur = NativeClass.motionblurndk(recvBmp, OPTION, Radius);


        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        //MotionBlurBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        aftermotionblur.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] byteStreamArray = byteStream.toByteArray();

        Log.d(TAG, "MotionBlurBitmap length is :" + String.valueOf(byteStreamArray.length));

        int what;
        try {
            /**
             * Binding data to message object
             */
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject", byteStreamArray.length);
            objMemoryFile.writeBytes(byteStreamArray, 0, 0, byteStreamArray.length);
            ParcelFileDescriptor objPFD = MemoryFileUtil.getParcelFileDescriptor(objMemoryFile);
            what = 100;
            Bundle bunData = new Bundle();
            bunData.putParcelable("objPFD", objPFD);
            Message newMsg = Message.obtain(null, what, byteArraySize, req);
            /**
             * Setting the messenger object to library messenger
             */
            Messenger mClient = messengerGBT;
            newMsg.setData(bunData);
            try {
                mClient.send(newMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
