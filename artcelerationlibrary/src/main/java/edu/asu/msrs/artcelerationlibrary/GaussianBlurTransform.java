/**
 * This class performs the necessary transform requested from service
 * For now, the same image is returned to the library to verify communication process
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
import android.util.Log;

import java.io.IOException;

/**
 * Created by abhishek on 11/7/2016.
 */

public class GaussianBlurTransform implements Runnable {

    // Debug tag for the GaussianBlurTransform class
    String TAG = "GaussianBlurTransform";
    /**
     * Defining variables to access data passed to thread from service
     */
    int byteArraySize;
    Bundle serviceDataBundle;
    int req;
    Messenger messengerGBT;

    /**
     * Constructor to initialize class variables
     *
     * @param size: byte Array received from the thread
     * @param bund: Data bundle which has ParcelFileDescriptor object
     * @param request: Index of the processing transform
     * @param gbt: Messenger object to processed transform to the
     */
    GaussianBlurTransform(int size, Bundle bund, int request, Messenger gbt){
        byteArraySize = size;
        serviceDataBundle = bund;
        req = request;
        messengerGBT = gbt;
    }
    @Override
    public void run() {
        /**
         * Unbinding data from the messenger object and bundle
         */
        Log.d(TAG, "Passed size to thread from service: "+byteArraySize);
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor)serviceDataBundle.get("libPFD");
        /**
         * Storing the byte array received in the buffer
         */
        ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        try {
            input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * For future use
         * Create Bitmap using the stored byte array
         */
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
        int what;
        try {
            /**
             * Binding data to message object
             */
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject",buffer.length);
            objMemoryFile.writeBytes(buffer,0,0, buffer.length);
            ParcelFileDescriptor objPFD = MemoryFileUtil.getParcelFileDescriptor(objMemoryFile);
            what = 100;
            Bundle bunData = new Bundle();
            bunData.putParcelable("objPFD",objPFD);
            Message newMsg = Message.obtain(null, what, byteArraySize,req);
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
