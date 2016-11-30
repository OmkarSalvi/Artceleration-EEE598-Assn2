/**
 * This class performs the necessary transform requested from service
 * For now, the same image is returned to the library to verify communication process
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
     * @param size: byte Array received from the thread
     * @param bund: Data bundle which has ParcelFileDescriptor object
     * @param request: Index of the processing transform
     * @param gbt: Messenger object to processed transform to the
     */
    MotionBlurTransform(int size, Bundle bund, int request, Messenger gbt){
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
        Log.d(TAG, "Passed size to thread from service: "+byteArraySize);
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor)serviceDataBundle.get("libPFD");
        int[] intArgs = (int[]) serviceDataBundle.get("intArray");
        float[] floatArgs = (float[]) serviceDataBundle.get("floatArray");

        /**
         * Setting the radius and sigma values for gaussian transform
         */
        int OPTION = intArgs[0];
        int Radius = intArgs[1];

        Log.d(TAG, "Radius : "+Radius);

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
         * we are computing only 1st half values for Gaussian weight vector G(k) i.e index = 0 to r
         * This is because remaining half values are same as 1st half values.
         */
        int rows = 2*Radius+1;

        BitmapFactory.Options opts = new BitmapFactory.Options();
        Bitmap recvBmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, opts);
        Bitmap MotionBlurBitmap;

        //Bitmap aftermotionblur = NativeClass.motionblurneon(recvBmp, OPTION, Radius);


        switch (OPTION) {
            case OPTION_0:
                MotionBlurBitmap = getHorizontal(recvBmp, Radius, rows);
                break;
            case OPTION_1:
                MotionBlurBitmap = getVertical(recvBmp, Radius, rows);
                break;
            default:
                MotionBlurBitmap = recvBmp;
                Log.d(TAG, "Please select valid operation!!");
                break;
        }

        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        MotionBlurBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        //aftermotionblur.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] byteStreamArray = byteStream.toByteArray();

        Log.d(TAG,"MotionBlurBitmap length is :"+String.valueOf(byteStreamArray.length));

        int what;
        try {
            /**
             * Binding data to message object
             */
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject",byteStreamArray.length);
            objMemoryFile.writeBytes(byteStreamArray,0,0, byteStreamArray.length);
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

    private Bitmap getVertical(Bitmap bmp, int rad, int rows){

        int H = bmp.getHeight();//source bitmap # of rows
        int W = bmp.getWidth();//source bitmap # of columns
        Log.d(TAG,"N = "+ H+" | M = "+W );

        // color information
        int[][] Alpha = new int[W][H], Red = new int[W][H], Green = new int[W][H], Blue = new int[W][H] ;

        for(int x=0; x<W; x++){
            for(int y=0; y<H;y++){
                int eachpixel = bmp.getPixel(x,y);
                Alpha[x][y] = Color.alpha(eachpixel);
                Red[x][y] = Color.red(eachpixel);
                Green[x][y] = Color.green(eachpixel);
                Blue[x][y] = Color.blue(eachpixel);
            }
        }

        int[][] qR = new int[W][H];
        int[][] qG = new int[W][H];
        int[][] qB = new int[W][H];

        for(int x=0; x<W; x++){
            for(int y=0; y<H; y++){
                qR[x][y] = 0; qG[x][y] = 0; qB[x][y] = 0;
                for(int r = -rad ; r<= rad; r++){
                    if((y + r) >= 0 && (y + r) < H ){
                        qR[x][y] += Red[x][y + r];
                        qG[x][y] += Green[x][y + r];
                        qB[x][y] += Blue[x][y + r];
                    }
                }
            }
        }
        Log.d(TAG,"Vertical calculation finished");

        Bitmap OutBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        /**
         * setting individual pixels of Final output bitmap
         */
        for(int x =0 ; x< OutBmp.getWidth(); x++){
            for(int y=0; y < OutBmp.getHeight(); y++){
                int color = Color.argb(Alpha[x][y], qR[x][y]/rows, qG[x][y]/rows, qB[x][y]/rows);
                OutBmp.setPixel(x, y, color);
            }
        }
        Log.d(TAG,"Finished with MotionBlur");
        return OutBmp;
    }


    private Bitmap getHorizontal(Bitmap bmp, int rad, int rows){

        int H = bmp.getHeight();//source bitmap # of rows
        int W = bmp.getWidth();//source bitmap # of columns
        Log.d(TAG,"Height = "+ H+" | Width = "+W );

        //Initializing pixel arrays
        int[][] Alpha = new int[W][H], Red = new int[W][H], Green = new int[W][H], Blue = new int[W][H] ;
        int x,y;
        // Extracting individual RGB pixel from original image
        for(x=0; x<W; x++){
            for(y=0; y<H;y++){
                Alpha[x][y] = Color.alpha(bmp.getPixel(x,y));
                Red[x][y] = Color.red(bmp.getPixel(x,y));
                Green[x][y] = Color.green(bmp.getPixel(x,y));
                Blue[x][y] = Color.blue(bmp.getPixel(x,y));
            }
        }

        int[][] qR = new int [W][H];
        int[][] qG = new int[W][H];
        int[][] qB = new int[W][H];

        for(x=0; x<W; x++){
            for(y=0; y<H; y++){
                qR[x][y] = 0; qG[x][y] = 0; qB[x][y] = 0;
                for(int r = -rad ; r<= rad; r++){
                    if((x + r) >= 0 && (x + r) < W ){
                        qR[x][y] += Red[x + r][y];
                        qG[x][y] += Green[x + r][y];
                        qB[x][y] += Blue[x + r][y];
                    }
                }
            }
        }
        Log.d(TAG,"Horizontal calculation finished");


        Bitmap OutBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        /**
         * setting individual pixels of Final output bitmap
         */
        for(x =0 ; x< OutBmp.getWidth(); x++){
            for(y=0; y < OutBmp.getHeight(); y++){
                int color = Color.argb(Alpha[x][y], qR[x][y]/rows, qG[x][y]/rows, qB[x][y]/rows);
                OutBmp.setPixel(x, y, color);
            }
        }
        Log.d(TAG,"Finished with MotionBlur");
        return OutBmp;
    }
}
