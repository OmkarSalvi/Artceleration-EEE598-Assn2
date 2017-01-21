/**
 * This class performs the GaussianBlur Transform requested from service
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
import java.nio.ByteBuffer;
import java.util.Arrays;


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

    /**
     * method to implemet gaussian blur Transform
     * @param radius : radius to be used
     * @param sigma : standard deviation to be used for computations
     * @param bitmap : input bitmap
     * @return final bitmap after transformation
     */
    public Bitmap doGaussianBlur(int radius, float sigma, Bitmap bitmap){

        Log.d(TAG, "doGaussianBlur started");

        int rows = 2*radius+1;
        float[] G_matrix = new float[rows];
        float PiSigma2 = (float) (2*Math.PI*sigma*sigma);
        float sqrtPiSigma2 = (float)Math.sqrt(PiSigma2);
        float sigmasqr2 = (float)2*sigma*sigma;

        for(int i=0; i<=radius; i++){
            float d = (i-radius) * (i-radius);
            G_matrix[i] = (float) Math.exp(-d/sigmasqr2) / sqrtPiSigma2;
            G_matrix[2*radius-i] = G_matrix[i];
        }

        int N = bitmap.getHeight();//source bitmap # of rows
        int M = bitmap.getWidth();//source bitmap # of columns

        Log.d(TAG,"N = "+ N+" | M = "+M );

        // color information matrices
        int[][] Alpha = new int[M][N], Red = new int[M][N], Green = new int[M][N], Blue = new int[M][N] ;

        //extract each color information from each pixel
        for(int x=0; x<M; x++){
            for(int y=0; y<N;y++){
                int eachpixel = bitmap.getPixel(x,y);
                Alpha[x][y] = Color.alpha(eachpixel);
                Red[x][y] = Color.red(eachpixel);
                Green[x][y] = Color.green(eachpixel);
                Blue[x][y] = Color.blue(eachpixel);
            }
        }

        float[][] qR = new float[M][N];
        float[][] qG = new float[M][N];
        float[][] qB = new float[M][N];
        //Implementation of the equation -
        // q(x,y) = G(‐r)*p(x‐r,y), + ... + G(0)*p(x,y),+ ... + G(r)*p(x+r,y)
        for(int x=0; x<M; x++){
            for(int y=0; y<N; y++){
                qR[x][y] = 0; qG[x][y] = 0; qB[x][y] = 0;
                //Log.d(TAG,"x = "+ x+" | y = "+y );
                for(int r = -radius ; r<= radius; r++){
                    int index = (x + r) ;
                    if(index >= 0 && index < M ){
                        qR[x][y] += G_matrix[r+radius] * Red[index][y];
                        qG[x][y] += G_matrix[r+radius] * Green[index][y];
                        qB[x][y] += G_matrix[r+radius] * Blue[index][y];
                    }
                }
            }
        }
        Log.d(TAG,"q calculation finished");

        int[][] PR = new int[M][N];
        int[][] PG = new int[M][N];
        int[][] PB = new int[M][N];
        //Implementation of the equation -
        //P(x,y) = G(‐r)*q(x,y‐r), + ... + G(0)*q(x,y),+ ... + G(r)*q(x,y+r)
        for(int x=0; x<M; x++){
            for(int y=0; y<N; y++){
                PR[x][y] = 0; PG[x][y] = 0; PB[x][y] = 0;
                for(int r = -radius ; r<= radius; r++){
                    if(((y + r) >=0) && ((y + r) < N)){
                        PR[x][y] += G_matrix[r+radius] * qR[x][y + r];
                        PG[x][y] += G_matrix[r+radius] * qG[x][y + r];
                        PB[x][y] += G_matrix[r+radius] * qB[x][y + r];

                        PR[x][y] = rgb_clamp(PR[x][y]);
                        PG[x][y] = rgb_clamp(PG[x][y]);
                        PB[x][y] = rgb_clamp(PB[x][y]);

                    }
                }
            }
        }

        Log.d(TAG,"P calculation finished");
        Bitmap OutBmp = bitmap.copy(Bitmap.Config.ARGB_8888, true);

        /**
         * setting individual pixels of Final output bitmap
         */
        for(int x =0 ; x< OutBmp.getWidth(); x++){
            for(int y=0; y < OutBmp.getHeight(); y++){
                int color = Color.argb(Alpha[x][y], PR[x][y], PG[x][y], PB[x][y]);
                OutBmp.setPixel(x, y, color);
            }
        }
        Log.d(TAG,"finished with doGaussianBlur");
        return OutBmp;
    }

    @Override
    public void run() {
        /**
         * Unbinding data from the messenger object and bundle
         */
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor)serviceDataBundle.get("libPFD");
        int[] intArgs = (int[]) serviceDataBundle.get("intArray");
        float[] floatArgs = (float[]) serviceDataBundle.get("floatArray");

        /**
         * Setting the radius and sigma values for gaussian transform
         */
        int radius = intArgs[0];
        float sigma = floatArgs[0];
        Log.d(TAG, "radius : "+radius);
        Log.d(TAG, "sigma : "+sigma);
        /**
         * Storing the byte array received in the buffer
         */
        ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        try {
            input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }

        BitmapFactory.Options options1 = new BitmapFactory.Options();
        Bitmap Inbmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options1);

        //call method which will do all the computations for gaussian blur
        Bitmap OutBmp = doGaussianBlur(radius, sigma, Inbmp);

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
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
