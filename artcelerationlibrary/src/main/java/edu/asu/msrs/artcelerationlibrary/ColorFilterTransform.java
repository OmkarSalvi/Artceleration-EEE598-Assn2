/**
 * This class performs the Color Filter Transform requested from service
 * It implements the runnable interface because we create a thread of this class to compute the image transform
 */
package edu.asu.msrs.artcelerationlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Created by salvi on 11/23/2016.
 */

public class ColorFilterTransform implements Runnable{

    // Debug tag for the ColorFilterTransform class
    String TAG = "ColorFilterTransform";
    /**
     * Defining variables to access data passed to thread from service
     */
    int byteArraySize;
    Bundle serviceDataBundle;
    int req;
    Messenger messengerGBT;

    public int[] intArgs = new int[24];

    ColorFilterTransform(int size, Bundle bund, int request, Messenger gbt){
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
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor) serviceDataBundle.get("libPFD");
        intArgs = (int[]) serviceDataBundle.get("intArray");

        /**
         * Storing the byte array received in the buffer
         */
        ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        try {
            input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //decoding the byte array into input bitmap
        BitmapFactory.Options options0 = new BitmapFactory.Options();
        Bitmap Inbmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options0);

        /**
         * calling native method
         */
        Bitmap outImage = NativeClass.colorfilterndk(Inbmp, intArgs);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        outImage.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();

        int what;
        try {
            /**
             * Binding data to message object
             */
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject",byteArray.length);
            objMemoryFile.writeBytes(byteArray,0,0, byteArray.length);
            ParcelFileDescriptor objPFD = MemoryFileUtil.getParcelFileDescriptor(objMemoryFile);
            what = 100;
            Bundle bunData = new Bundle();
            bunData.putParcelable("objPFD",objPFD);
            Message newMsg = Message.obtain(null, what, byteArray.length,req);
            /**
             * Setting the messenger object to library messenger
             */
            Messenger mClient =  messengerGBT;
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
