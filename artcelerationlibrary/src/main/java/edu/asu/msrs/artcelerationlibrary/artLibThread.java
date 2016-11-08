package edu.asu.msrs.artcelerationlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.IOException;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by abhishek on 11/7/2016.
 */

public class artLibThread implements Runnable {

    String TAG = "artLibThread";
    int artLibRequestId;
    int byteArrSize;
    Bundle receivedBundle;
    Bitmap resultBitmap;
    ConcurrentLinkedQueue<Integer> artLibThreadQueue;
    TransformHandler transformHandlerListener;

    artLibThread(int argument1, int index, Bundle bunData, ConcurrentLinkedQueue<Integer> artQueue, TransformHandler listener ){
        byteArrSize = argument1;
        artLibRequestId = index;
        receivedBundle = bunData;
        artLibThreadQueue = artQueue;
        transformHandlerListener = listener;
    }
    @Override
    public void run() {

        int req_id = artLibRequestId;
        Log.d(TAG,"Inside artLib handler: "+ req_id);
        Log.d(TAG, "Queue head is : "+String.valueOf(artLibThreadQueue.peek()));
        Log.d(TAG, "Queue in thread is "+artLibThreadQueue.toString());
        //req_id = 10;
        while (!artLibThreadQueue.isEmpty() && artLibThreadQueue.peek() != req_id){
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


        int resultSize = byteArrSize;
        Log.d(TAG, "Result Size: "+resultSize);
        byte[] resultBuffer = new byte[resultSize];
        Bundle resultServiceDataBundle = receivedBundle;
        ParcelFileDescriptor resultPFD = (ParcelFileDescriptor)resultServiceDataBundle.getParcelable("objPFD");
        Log.d(TAG, "Result File descriptor: "+resultPFD);
        ParcelFileDescriptor.AutoCloseInputStream resultInput = new ParcelFileDescriptor.AutoCloseInputStream(resultPFD);
        try {
            resultInput.read(resultBuffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options bitOptions = new BitmapFactory.Options();
        resultBitmap = BitmapFactory.decodeByteArray(resultBuffer, 0, resultBuffer.length, bitOptions);
        //createProcessedBitmap(resultBitmap);
        artLibThreadQueue.poll();
        Log.d(TAG, "After receiving  : "+artLibThreadQueue.toString());
        transformHandlerListener.onTransformProcessed(resultBitmap);
    }
}
