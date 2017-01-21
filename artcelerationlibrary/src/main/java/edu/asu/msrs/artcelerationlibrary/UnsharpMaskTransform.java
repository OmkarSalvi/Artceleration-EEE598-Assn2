/**
 * This class performs the Unsharp Mask Transform requested from service
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
import java.io.IOException;

/**
 * Created by salvi on 11/19/2016.
 */

public class UnsharpMaskTransform implements Runnable {

    // Debug tag for the GaussianBlurTransform class
    String TAG = "UnsharpMaskTransform";
    /**
     * Defining variables to access data passed to thread from service
     */
    int byteArraySize;
    Bundle serviceDataBundle;
    int req;
    Messenger messengerGBT;
    GaussianBlurTransform GBT;

    /**
     * Constructor to initialize class variables
     *
     * @param size: byte Array received from the thread
     * @param bund: Data bundle which has ParcelFileDescriptor object
     * @param request: Index of the processing transform
     * @param gbt: Messenger object to processed transform to the
     */
    UnsharpMaskTransform(int size, Bundle bund, int request, Messenger gbt, GaussianBlurTransform objGBT){
        byteArraySize = size;
        serviceDataBundle = bund;
        req = request;
        messengerGBT = gbt;
        GBT = objGBT;
    }

    /**
     * method to restrict pixel values between 0 and 255
     * @param value: integer variable representing the pixel value
     * @return integer pixel value after clamping the input value between 0 and 255
     */
    static int rgb_clamp(int value) {
        if(value > 255) {
            return 255;
        }
        if(value < 0) {
            return 0;
        }
        return value;
    }

    @Override
    public void run() {
        /**
         * Unbinding data from the messenger object and bundle
         */
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor) serviceDataBundle.get("libPFD");
        float[] floatArgs = (float[]) serviceDataBundle.get("floatArray");

        /**
         * Storing the byte array received in the buffer
         */
        ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        try {
            input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        BitmapFactory.Options options0 = new BitmapFactory.Options();
        Bitmap Inbmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options0);

        /**
         * Setting the f0 and f1 values for gaussian transform
         */
        float f0 = floatArgs[0];
        float f1 = floatArgs[1];
        Log.d(TAG, "f0 : "+f0);
        Log.d(TAG, "f1 : "+f1);

        float sigma = f0;
        int radius = (int)(6*f0);
        Log.d(TAG,"radius = "+ radius+" | sigma = "+sigma );

        //Compute Gaussian Blur Transform of the input image
        Bitmap Outbmp = GBT.doGaussianBlur(radius, sigma, Inbmp);

        int N = Inbmp.getHeight();//source bitmap # of rows
        int M = Inbmp.getWidth();//source bitmap # of columns
        Log.d(TAG,"N = "+ N+" | M = "+M );

        // color information
        int[][] OutAlpha = new int[M][N], OutRed = new int[M][N], OutGreen = new int[M][N], OutBlue = new int[M][N] ;

        for(int x=0; x<M; x++){
            for(int y=0; y<N;y++){
                int Ineachpixel = Inbmp.getPixel(x,y);

                int Outeachpixel = Outbmp.getPixel(x,y);
                /**
                 * generate s = (p‐q)*f1
                 * Add the result to the original image to generate the result P = s + p
                 */
                OutAlpha[x][y] = Color.alpha(Outeachpixel);
                OutRed[x][y] = (int)(Color.red(Ineachpixel) + ((Color.red(Outeachpixel) - Color.red(Ineachpixel))*f1));
                OutGreen[x][y] = (int)(Color.green(Ineachpixel) + ((Color.green(Outeachpixel) - Color.green(Ineachpixel))*f1));
                OutBlue[x][y] = (int)(Color.blue(Ineachpixel) + ((Color.blue(Outeachpixel) - Color.blue(Ineachpixel))*f1));

                OutRed[x][y] = rgb_clamp(OutRed[x][y]);
                OutGreen[x][y] = rgb_clamp(OutGreen[x][y]);
                OutBlue[x][y] = rgb_clamp(OutBlue[x][y]);

            }
        }

        /**
         * setting individual pixels of Final output bitmap
         */
        for(int x =0 ; x< Outbmp.getWidth(); x++){
            for(int y=0; y < Outbmp.getHeight(); y++){
                int color = Color.argb(OutAlpha[x][y], OutRed[x][y], OutGreen[x][y], OutBlue[x][y]);
                Outbmp.setPixel(x, y, color);
            }
        }

        Log.d(TAG,"done with unsharpMask Transform");
        ByteArrayOutputStream stream1 = new ByteArrayOutputStream();
        Outbmp.compress(Bitmap.CompressFormat.PNG, 100, stream1);
        byte[] byteArray1 = stream1.toByteArray();

        try {
            /**
             * Binding data to message object
             */
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject",byteArray1.length);
            objMemoryFile.writeBytes(byteArray1,0,0, byteArray1.length);
            ParcelFileDescriptor objPFD1 = MemoryFileUtil.getParcelFileDescriptor(objMemoryFile);
            int what = 100;
            Bundle bunData = new Bundle();
            bunData.putParcelable("objPFD",objPFD1);
            Message newMsg = Message.obtain(null, what, byteArray1.length,req);
            /**
             * Setting the messenger object to library messenger
             */
            Messenger mClient =  messengerGBT;
            newMsg.setData(bunData);
            try {
                Log.d(TAG,"sending the message...");
                mClient.send(newMsg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
