/**
 * This class performs the necessary transform requested from service
 * For now, the same image is returned to the library to verify communication process
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
    static final int OPTION_0 = 0;
    static final int OPTION_1 = 1;
    static final int OPTION_2 = 2;

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
        Log.d(TAG, "selectedOperation : " + OPTION);
        Log.d(TAG, "sigma : " + sigma);
        //OPTION = 0;

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

        int[][] matrixSobelX = new int[][]{
                {1, 0, -1},
                {2, 0, -2},
                {1, 0, -1}
        };
        int[][] matrixSobelY = new int[][]{
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };

        int eachPixel, alpha, red, blue, green, srcImgWidth, srcImgHeight, x, y,gray;
        srcImgWidth = srcImg.getWidth();
        srcImgHeight = srcImg.getHeight();
        Bitmap grayBmp = srcImg.copy(Bitmap.Config.ARGB_8888, true);

        // scan through every single pixel
        for (x = 0; x < srcImgWidth; x++) {
            for (y = 0; y < srcImgHeight; y++) {
                // get one pixel color
                eachPixel = srcImg.getPixel(x, y);
                // retrieve color of all channels
                alpha = Color.alpha(eachPixel);
                red = Color.red(eachPixel);
                green = Color.green(eachPixel);
                blue = Color.blue(eachPixel);
                // take conversion up to one single value
                // gray = (int) ((0.2989 * red) + (0.5870 * green) + (0.1140 * blue));

                red = (int) ((0.2989 * red));
                green = (int) ((0.5870 * green));
                blue = (int) ((0.1140 * blue));

                int var = red+green+blue;//this is changed
                /// set new pixel color to output bitmap

                //Log.d(TAG, "gray: "+String.valueOf(gray));
                //grayBmp.setPixel(x, y, Color.argb(255, gray, gray, gray));
                grayBmp.setPixel(x, y, Color.argb(255, var, var, var));//this is changed

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
        //grayBmp.compress(Bitmap.CompressFormat.PNG, 100, byteStream);

        byte[] byteStreamArray = byteStream.toByteArray();

        Log.d(TAG,"sobelBmp length is :"+String.valueOf(byteStreamArray.length));

        /**
         * For future use
         * Create Bitmap using the stored byte array
         */
        /*
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
        */
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

    public Bitmap getGrad(Bitmap grayBmp, int bitOption){

        int srcGrayImgWidth, srcGrayImgHeight, gradX = 0, gradY = 0, grad = 0;

        srcGrayImgWidth = grayBmp.getWidth();
        srcGrayImgHeight = grayBmp.getHeight();
        Bitmap sobelBmp = grayBmp.copy(Bitmap.Config.ARGB_8888, true);

        for (int x = 1; x < srcGrayImgWidth - 2; x++) {
            for (int y = 1; y < srcGrayImgHeight - 2; y++) {

                if(bitOption != 1 ){
                    gradX = (-1 * Color.green(grayBmp.getPixel(x - 1, y - 1)))+ (-2*Color.green(grayBmp.getPixel(x , y - 1))) + (-1*Color.green(grayBmp.getPixel(x + 1, y - 1))) +
                            (Color.green(grayBmp.getPixel(x - 1, y + 1))) + (2*Color.green(grayBmp.getPixel(x , y + 1)))+(Color.green(grayBmp.getPixel(x + 1, y + 1)));
                    grad = gradX;
                }

                if(bitOption != 0 ){
                    gradY = (-1 * Color.green(grayBmp.getPixel(x - 1, y - 1)))  + Color.green(grayBmp.getPixel(x + 1, y - 1)) +
                            (-2 * Color.green(grayBmp.getPixel(x - 1, y )))  + ( 2* Color.green(grayBmp.getPixel(x + 1, y ))) +
                            (-1 * Color.green(grayBmp.getPixel(x - 1, y +1)))  + (Color.green(grayBmp.getPixel(x + 1, y+1 )));
                    grad = gradY;
                }

                if(bitOption == 2){
                    grad = (int) Math.sqrt((gradX * gradX)+(gradY * gradY));
                }
                //Log.d(TAG, "x:"+x +" y:"+y+" "+String.valueOf(gradX));

               /*
               if(grad < 0){
                    grad = 0;
               }
               if(grad > 255){
                    grad = 255;
                }
                */
                //Log.d(TAG, "x:"+x +" y:"+y+" "+String.valueOf(grad));

                sobelBmp.setPixel(x,y,Color.argb(255, grad, grad, grad));
            }
        }
        return sobelBmp;
    }
}
