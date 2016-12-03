/**
 * This class performs the sobel edge transform requested from service
 * It implements the runnable interface because we create a thread of this class to compute the image transform
 * The transformed image is obtained depending on the parameter sent from the service
 * If the option(a0)= 0, then image is tranfromed in horizontal direction
 * If the option(a0)= 1, then image is tranfromed in vertical direction
 * If the option(a0)= 2, then image is tranfromed in both X and Y direction
 */
package edu.asu.msrs.artcelerationlibrary;

import android.annotation.SuppressLint;
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
 * Created by abhishek on 11/7/2016.
 */

public class SobelEdgeFilterTransform implements Runnable {

    // Debug tag for the GaussianBlurTransform class
    String TAG = "SobelEdgeFilterTransform";
    /**
     * Defining variables to access data passed to thread from service
     */
    int byteArraySize;
    Bundle serviceDataBundle;
    int req;
    Messenger messengerGBT;
    /**
     * Static variables to define the option sent from library
     */
    static final int OPTION_0 = 0;
    static final int OPTION_1 = 1;
    static final int OPTION_2 = 2;
    /**
     * Filters used to highlight horizontal and vertical edges
     */
    int[][] SobelX = new int[][]{
            {-1, 0, 1},
            {-2, 0, 2},
            {-1, 0, 1}
    };
    int[][] SobelY = new int[][]{
            {-1, -2, -1},
            {0, 0, 0},
            {1, 2, 1}
    };

    /**
     * Constructor to initialize class variables
     *
     * @param size:    byte Array received from the thread
     * @param bund:    Data bundle which has ParcelFileDescriptor object
     * @param request: Index of the processing transform
     * @param gbt:     Messenger object to processed transform to the
     */
    SobelEdgeFilterTransform(int size, Bundle bund, int request, Messenger gbt) {
        byteArraySize = size;
        serviceDataBundle = bund;
        req = request;
        messengerGBT = gbt;
    }

    @SuppressLint("LongLogTag")
    @Override
    public void run() {
        /**
         * Unbinding data from the messenger object and bundle
         */
        Log.d(TAG, "Passed size to thread from service: " + byteArraySize);
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor) serviceDataBundle.get("libPFD");
        int[] intArgs = (int[]) serviceDataBundle.get("intArray");
        float[] floatArgs = (float[]) serviceDataBundle.get("floatArray");

        /**
         * Setting the radius and sigma values for gaussian transform
         */
        int OPTION = intArgs[0];
        float sigma = floatArgs[0];
        //Log.d(TAG, "SelectedOperation : " + OPTION);

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
        Bitmap srcImg = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, opts);
        int eachPixel, alpha, red, blue, green, srcImgWidth, srcImgHeight, x, y;
        srcImgWidth = srcImg.getWidth();
        srcImgHeight = srcImg.getHeight();
        /**
         * Create a grayscale brightness image
         */
        Bitmap grayBmp = srcImg.copy(Bitmap.Config.ARGB_8888, true);
        /**
         * Scan through every single pixel
         */
        for (x = 0; x < srcImgWidth; x++) {
            for (y = 0; y < srcImgHeight; y++) {

                /**
                 * Get one pixel color
                 * Retrieve color of all channels
                 * Compute the overall gradient magnitude
                 */
                eachPixel = srcImg.getPixel(x, y);
                alpha = Color.alpha(eachPixel);
                red = Color.red(eachPixel);
                green = Color.green(eachPixel);
                blue = Color.blue(eachPixel);
                red = (int) ((0.2989 * red));
                green = (int) ((0.5870 * green));
                blue = (int) ((0.1140 * blue));
                int temp = red + green +blue;
                grayBmp.setPixel(x, y, Color.argb(255, temp, temp, temp));
            }
        }

        Bitmap sobelBitmap;
        switch (OPTION) {
            case OPTION_0:
                sobelBitmap = getGrad(grayBmp, OPTION);
                break;
            case OPTION_1:
                sobelBitmap = getGrad(grayBmp, OPTION);
                break;
            case OPTION_2:
                sobelBitmap = getGrad(grayBmp, OPTION);
                break;
            default:
                sobelBitmap = srcImg;
                Log.d(TAG, "Please select valid operation!!");
                break;
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        sobelBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] byteStreamArray = byteStream.toByteArray();
        Log.d(TAG,"SobelBmp length is :"+String.valueOf(byteStreamArray.length));
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
            Message newMsg = Message.obtain(null, what, byteStreamArray.length, req);
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

    @SuppressLint("LongLogTag")
    public Bitmap getGrad(Bitmap grayBmp, int bitOption){

        int srcGrayImgWidth, srcGrayImgHeight, gradX = 0, gradY = 0, grad = 0;
        srcGrayImgWidth = grayBmp.getWidth();
        srcGrayImgHeight = grayBmp.getHeight();
        Bitmap sobelBmp = grayBmp.copy(Bitmap.Config.ARGB_8888, true);
        /**
         * If a0 = 0, the output image channels should contain Grx.
         * If a0 = 1, the output image channels should contain Gry.
         * If a0 = 2, the output image channels should contain Gr.
         *
         */
        for (int x = 1; x < srcGrayImgWidth - 2; x++) {
            for (int y = 1; y < srcGrayImgHeight - 2; y++) {
                /**
                 * If the transform is obtained in X direction
                 */
                if(bitOption != 1 ){
                    gradX = (-1 * Color.green(grayBmp.getPixel(x - 1, y - 1)))+ (-2*Color.green(grayBmp.getPixel(x , y - 1))) + (-1*Color.green(grayBmp.getPixel(x + 1, y - 1))) +
                            (Color.green(grayBmp.getPixel(x - 1, y + 1))) + (2*Color.green(grayBmp.getPixel(x , y + 1)))+(Color.green(grayBmp.getPixel(x + 1, y + 1)));
                    grad = gradX;
                }
                /**
                 * If the transform is obtained in Y direction
                 */
                if(bitOption != 0 ){
                    gradY = (-1 * Color.green(grayBmp.getPixel(x - 1, y - 1)))  + Color.green(grayBmp.getPixel(x + 1, y - 1)) +
                            (-2 * Color.green(grayBmp.getPixel(x - 1, y )))  + ( 2* Color.green(grayBmp.getPixel(x + 1, y ))) +
                            (-1 * Color.green(grayBmp.getPixel(x - 1, y +1)))  + (Color.green(grayBmp.getPixel(x + 1, y+1 )));
                    grad = gradY;
                }
                /**
                 * If the transform is obtained in both directions
                 */
                if(bitOption == 2){
                    grad = (int) Math.sqrt((gradX * gradX)+(gradY * gradY));
                }
                //Log.d(TAG, "Final gradient value: "+String.valueOf(grad));
                /**
                 * Taking only the absolute values in order to reduce yellow pixels
                 */
                grad=Math.abs(grad);
                sobelBmp.setPixel(x,y,Color.argb(255, grad, grad, grad));
            }
        }
        return sobelBmp;
    }
}
