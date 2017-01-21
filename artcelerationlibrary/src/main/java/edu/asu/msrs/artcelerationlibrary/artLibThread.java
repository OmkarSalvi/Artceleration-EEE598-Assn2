/**
 * This class processes threads which are created from handler method ArtLibIncomingHandler class
 * This class implements runnable interface and run method
 * Processing of threads is based on the FIFO queue
 * Once the head of queue is processed, bitmap is created and passed to the onProcessedTransform
 */

package edu.asu.msrs.artcelerationlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by abhishek on 11/7/2016.
 */

public class artLibThread implements Runnable {

    /**
     * Debug Tag and constructor variables to perform necessary operations
     */
    String TAG = "artLibThread";
    int artLibRequestId;
    int byteArrSize;
    Bundle receivedBundle;
    Bitmap resultBitmap;
    TransformHandler transformHandlerListener;
    /**
     * Concurrent queue which follow FIFO order
     */
    ConcurrentLinkedQueue<Integer> artLibThreadQueue;

    /**
     * @param byteSize: Byte Array received from the thread
     * @param index: Index of the processing transform
     * @param bunData: Data bundle which has ParcelFileDescriptor object
     * @param artQueue: Concurrent Linked Queue which stores the transform request order
     * @param listener: object of the Transform Handler
     */
    artLibThread(int byteSize, int index, Bundle bunData, ConcurrentLinkedQueue<Integer> artQueue, TransformHandler listener ){
        byteArrSize = byteSize;
        artLibRequestId = index;
        receivedBundle = bunData;
        artLibThreadQueue = artQueue;
        transformHandlerListener = listener;
    }

    /**
     * Helper to perform operations on thread
     */
    @Override
    public void run() {

        /**
         * Logging variables for debugging
         */
        Log.d(TAG,"Inside artLib handler: "+ artLibRequestId);
        Log.d(TAG, "Queue head is : "+String.valueOf(artLibThreadQueue.peek()));
        Log.d(TAG, "Queue in thread is "+artLibThreadQueue.toString());
        //req_id = 10;
        while (!artLibThreadQueue.isEmpty() && artLibThreadQueue.peek() != artLibRequestId){
            /**
             * For Testing FIFO Queue
             */
            /*
            int temp;
            Log.d(TAG, "Busy Waiting");
            for(int i=0;i<900000000;i++){
                temp = i+1;
            }
            Log.d(TAG, " Waiting finished");
            req_id = artLibThreadQueue.peek();
            */
        }
        Log.d(TAG, "Result Size: "+byteArrSize);
        byte[] resultBuffer = new byte[byteArrSize];
        Bundle resultServiceDataBundle = receivedBundle;
        ParcelFileDescriptor resultPFD = (ParcelFileDescriptor)resultServiceDataBundle.getParcelable("objPFD");
        //Log.d(TAG, "Result File descriptor: "+resultPFD);
        ParcelFileDescriptor.AutoCloseInputStream resultInput = new ParcelFileDescriptor.AutoCloseInputStream(resultPFD);
        try {
            //Storing the byte array received in the buffer
            resultInput.read(resultBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * Create Bitmap using the stored byte array
         */
        BitmapFactory.Options bitOptions = new BitmapFactory.Options();
        resultBitmap = BitmapFactory.decodeByteArray(resultBuffer, 0, resultBuffer.length, bitOptions);
        /**
         * For future use
         * Helper to save processed image in the gallery
         * //createProcessedBitmap(resultBitmap);
         */

        /**
         * Removing the request id of processed transform from the queue
         */
        artLibThreadQueue.poll();
        Log.d(TAG, "After removing from queue: "+artLibThreadQueue.toString());
        if(resultBitmap == null)
            Log.d(TAG, "result bitmap is null");
        transformHandlerListener.onTransformProcessed(resultBitmap);
    }
}
