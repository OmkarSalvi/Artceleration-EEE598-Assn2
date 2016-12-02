/**
 * This class performs the Color Filter Transform requested from service
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

    /**
     *
     * @param input input value for a certain color in pixel
     * @param color color corresponding to this value { 0 = red; 8 = green; 16 = blue}
     *              It helps in finding correct indices in int Array
     * @return output value of the color
     */
    public int convertRange(int input, int color){
        /**
         * confining the input value in 0 to 255
         */
        if(input < 0)
            input = 0;
        if(input > 255)
            input = 255;

        int slope = 0, constant = 0, output = 0;

        /**
         * Calculating the output value with color filter
         */
        if(0 <= input && input <= intArgs[color] && intArgs[color] != 0){
            slope = (intArgs[color+1]/intArgs[color]);
            output = slope*input;
        }else if(intArgs[color] < input && input <= intArgs[color+2]){
            slope = ((intArgs[color+3] - intArgs[color+1])/(intArgs[color+2] - intArgs[color]));
            constant = intArgs[color+3] - (slope*intArgs[color+2]);
            output = slope*input + constant;
        }else if(intArgs[color+2] < input && input <= intArgs[color+4]){
            slope = ((intArgs[color+5] - intArgs[color+3])/(intArgs[color+4] - intArgs[color+2]));
            constant = intArgs[color+5] - (slope*intArgs[color+4]);
            output = slope*input + constant;
        }else if(intArgs[color+4] < input && input <= intArgs[color+6]){
            slope = ((intArgs[color+7] - intArgs[5])/(intArgs[color+6] - intArgs[color+4]));
            constant = intArgs[color+7] - (slope*intArgs[color+6]);
            output = slope*input + constant;
        }else if(intArgs[color+6] < input && input <= 255){
            slope = ((255 - intArgs[color+7])/(255 - intArgs[color+6]));
            output = slope*input;
        }
        /**
         * confining the output value in 0 to 255
         */
        if(output < 0)
            output = 0;
        if(output > 255)
            output = 255;
        return output;
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



        /**
         * decoding the byte array into input bitmap
         * creating mutable output bitmap from input bitmap
         */
        BitmapFactory.Options options0 = new BitmapFactory.Options();
        Bitmap Inbmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options0);
        //Bitmap OutBmp = Inbmp.copy(Bitmap.Config.ARGB_8888, true);

        /**
         * calling native method
         */
        Bitmap outImage = NativeClass.colorfilterndk(Inbmp, intArgs);

        /*
        int N = Inbmp.getHeight();//source bitmap # of rows
        int M = Inbmp.getWidth();//source bitmap # of columns

        int outRed =0, outGrreen =0, outBlue =0;
        for(int x=0; x<M; x++){
            for(int y=0; y<N;y++){

                int eachpixel = Inbmp.getPixel(x,y);
                int Alpha = Color.alpha(eachpixel);
                int Red = Color.red(eachpixel);
                int Green = Color.green(eachpixel);
                int Blue = Color.blue(eachpixel);

                outRed = convertRange(Red, 0 );
                outGrreen = convertRange(Green, 8 );
                outBlue = convertRange(Blue, 16 );

                OutBmp.setPixel(x, y, Color.argb(Alpha, outRed, outGrreen, outBlue));
            }
        }
           */

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //OutBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
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
