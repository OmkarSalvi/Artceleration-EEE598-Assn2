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
     * method to be used in UnsharpMask Transform
     * @param radius
     * @param sigma
     * @param bitmap
     * @return
     */
    public Bitmap doGaussianBlur(int radius, float sigma, Bitmap bitmap){

        Log.d("UnsharpMaskTransform", "doGaussianBlur started");

        int rows = 2*radius+1; // half range is from 0 to r thus r+1
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

        Log.d("UnsharpMaskTransform","N = "+ N+" | M = "+M );

        // color information
        int[][] Alpha = new int[M][N], Red = new int[M][N], Green = new int[M][N], Blue = new int[M][N] ;

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
        //q(x,y) = G(‐r)*p(x‐r,y), + ... + G(0)*p(x,y),+ ... + G(r)*p(x+r,y)
        for(int x=0; x<M; x++){
            for(int y=0; y<N; y++){
                qR[x][y] = 0; qG[x][y] = 0; qB[x][y] = 0;
                //Log.d(TAG,"x = "+ x+" | y = "+y );
                for(int r = -radius ; r<= radius; r++){
                    int index = (x + r) ;//int index = ((x + r)*N)+y ;
                    if(index >= 0 && index < M ){
                        qR[x][y] += G_matrix[r+radius] * Red[index][y];
                        qG[x][y] += G_matrix[r+radius] * Green[index][y];
                        qB[x][y] += G_matrix[r+radius] * Blue[index][y];
                    }
                }
            }
        }
        Log.d("UnsharpMaskTransform","q calculation finished");

        int[][] PR = new int[M][N];
        int[][] PG = new int[M][N];
        int[][] PB = new int[M][N];
        //P(x,y) = G(‐r)*q(x,y‐r), + ... + G(0)*q(x,y),+ ... + G(r)*q(x,y+r)
        for(int x=0; x<M; x++){
            for(int y=0; y<N; y++){
                PR[x][y] = 0; PG[x][y] = 0; PB[x][y] = 0;
                for(int r = -radius ; r<= radius; r++){
                    if(((y + r) >=0) && ((y + r) < N)){
                        PR[x][y] += G_matrix[r+radius] * qR[x][y + r];
                        PG[x][y] += G_matrix[r+radius] * qG[x][y + r];
                        PB[x][y] += G_matrix[r+radius] * qB[x][y + r];
                    }
                }
            }
        }

        Log.d("UnsharpMaskTransform","P calculation finished");
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
        Log.d("UnsharpMaskTransform","finished with doGaussianBlur");
        return OutBmp;
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

        /**
         * we are computing only 1st half values for Gaussian weight vector G(k) i.e index = 0 to r
         * This is because remaining half values are same as 1st half values.
         */

        int rows = 2*radius+1; // half range is from 0 to r thus r+1
        float[] G_matrix = new float[rows];
        float PiSigma2 = (float) (2*Math.PI*sigma*sigma);
        float sqrtPiSigma2 = (float)Math.sqrt(PiSigma2);
        float sigmasqr2 = (float)2*sigma*sigma;

        for(int i=0; i<=radius; i++){
            float d = (i-radius) * (i-radius);
            G_matrix[i] = (float) Math.exp(-d/sigmasqr2) / sqrtPiSigma2;
            G_matrix[2*radius-i] = G_matrix[i];
        }

        Log.d(TAG, Arrays.toString(G_matrix));


        BitmapFactory.Options options0 = new BitmapFactory.Options();
        Bitmap bmp0 = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options0);
        //Bitmap OutBmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options0);

        Bitmap OutBmp = bmp0.copy(Bitmap.Config.ARGB_8888, true);

        int N = bmp0.getHeight();//source bitmap # of rows
        int M = bmp0.getWidth();//source bitmap # of columns
        int[] pixels = new int[M*N];
        bmp0.getPixels(pixels, 0, M, 0, 0, M, N);

        Log.d(TAG,"N = "+ N+" | M = "+M );
        //Log.d(TAG, Arrays.toString(pixels));
        Log.d(TAG,"size of pixels = "+pixels.length);

        int size = pixels.length;
        // color information
        //int[] Alpha = new int[size], Red = new int[size], Green = new int[size], Blue = new int[size] ;
        int[][] Alpha = new int[M][N], Red = new int[M][N], Green = new int[M][N], Blue = new int[M][N] ;
        /*
        // scan through all pixels
        for (int x = 0; x < size; x++) {
            // get pixel color
            int eachpixel = pixels[x];
            Alpha[x] = Color.alpha(eachpixel);
            Red[x] = Color.red(eachpixel);
            Green[x] = Color.green(eachpixel);
            Blue[x] = Color.blue(eachpixel);

            // set new pixel color to output bitmap
            //bmOut.setPixel(x, y, Color.argb(A, gray, gray, gray));
        }
        */

        for(int x=0; x<M; x++){
            for(int y=0; y<N;y++){
                int eachpixel = bmp0.getPixel(x,y);
                Alpha[x][y] = Color.alpha(eachpixel);
                Red[x][y] = Color.red(eachpixel);
                Green[x][y] = Color.green(eachpixel);
                Blue[x][y] = Color.blue(eachpixel);
            }
        }
        //Log.d(TAG, Arrays.toString(Red));
        //Log.d(TAG, Arrays.toString(Green));
        //Log.d(TAG, Arrays.toString(Blue));

        float[][] qR = new float[M][N];
        float[][] qG = new float[M][N];
        float[][] qB = new float[M][N];
        //q(x,y) = G(‐r)*p(x‐r,y), + ... + G(0)*p(x,y),+ ... + G(r)*p(x+r,y)
        for(int x=0; x<M; x++){
            for(int y=0; y<N; y++){
                qR[x][y] = 0; qG[x][y] = 0; qB[x][y] = 0;
                //Log.d(TAG,"x = "+ x+" | y = "+y );
                for(int r = -radius ; r<= radius; r++){
                    int index = (x + r) ;//int index = ((x + r)*N)+y ;
                    if(index >= 0 && index < M ){
                        qR[x][y] += G_matrix[r+radius] * Red[index][y];
                        qG[x][y] += G_matrix[r+radius] * Green[index][y];
                        qB[x][y] += G_matrix[r+radius] * Blue[index][y];
                    }else{
                        //Log.d(TAG,"index of pixel = "+ (((x + r)*M)+y));
                    }


                    /*
                    if(r!=radius){
                        q[x][y] += G_matrix[r] * pixels[((x + r - radius)*M)+y];
                        q[x][y] += G_matrix[r] * pixels[((x + r + radius)*M)+y]; //q[x][y] += G_matrix[2 * radius - r] * pixels[((x + r + radius)*M)+y];
                    }else{
                        q[x][y] += G_matrix[radius] * pixels[(x*M)+y];
                    }
                    */
                }
            }
        }

        Log.d(TAG, ""+qR[0][0]);//Arrays.toString(qR));
        Log.d(TAG, ""+qG[0][0]);//Arrays.toString(qG));
        Log.d(TAG, ""+qB[0][0]);//Arrays.toString(qB));


        int[][] PR = new int[M][N];
        int[][] PG = new int[M][N];
        int[][] PB = new int[M][N];
        //P(x,y) = G(‐r)*q(x,y‐r), + ... + G(0)*q(x,y),+ ... + G(r)*q(x,y+r)
        for(int x=0; x<M; x++){
            for(int y=0; y<N; y++){
                PR[x][y] = 0; PG[x][y] = 0; PB[x][y] = 0;
                for(int r = -radius ; r<= radius; r++){
                    if(((y + r) >=0) && ((y + r) < N)){
                        PR[x][y] += G_matrix[r+radius] * qR[x][y + r];
                        PG[x][y] += G_matrix[r+radius] * qG[x][y + r];
                        PB[x][y] += G_matrix[r+radius] * qB[x][y + r];
                    }else {
                        //Log.d(TAG,"index of q | x = "+ x + "; y ="+(y+r));
                    }
                    /*
                    if(r!=radius){
                        P[x][y] += G_matrix[r] * q[x][y + r - radius];
                        P[x][y] += G_matrix[r] * q[x][y + r + radius]; //P[x][y] += G_matrix[2 * radius - r] * q[x][y + r + radius];
                    }else{
                        P[x][y] += G_matrix[radius] * q[x][y];
                    }*/
                }
            }
        }

        Log.d(TAG, ""+PR[0][0]);//Arrays.toString(PR));
        Log.d(TAG, ""+PG[0][0]);//Arrays.toString(PG));
        Log.d(TAG, ""+PB[0][0]);//Arrays.toString(PB));
        Log.d(TAG, ""+Alpha[0]);

        /**
         * setting individual pixels of Final output bitmap
         */
        for(int x =0 ; x< OutBmp.getWidth(); x++){
            for(int y=0; y < OutBmp.getHeight(); y++){
                int color = Color.argb(Alpha[x][y], PR[x][y], PG[x][y], PB[x][y]);
                //Log.d(TAG, ""+ color);
                OutBmp.setPixel(x, y, color);
            }
        }

        Log.d(TAG, "output W : "+OutBmp.getWidth()+" | H: "+OutBmp.getHeight());
        //int outWidth = OutBmp.getWidth();
        //int outHeight = OutBmp.getHeight();

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        OutBmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        //Log.d(TAG, Arrays.toString(byteArray));

        /*
        int OutSize = OutBmp.getRowBytes() * OutBmp.getHeight();
        ByteBuffer byteBuffer = ByteBuffer.allocate(OutSize);
        OutBmp.copyPixelsToBuffer(byteBuffer);
        byte[] byteArray = byteBuffer.array();
        Log.d(TAG, Arrays.toString(byteArray));
        */


        /*
        int[] outPixels = new int[outWidth * outHeight];
        int outPixelsIndex = 0;

        for (int i = 0; i < outWidth; i++)
        {
            for (int j = 0; j < outHeight; j++)
            {
                outPixels[outPixelsIndex] = P[i][j];
                outPixelsIndex ++;
            }
        }

        Log.d(TAG, Arrays.toString(outPixels));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        for(int i=0; i < outPixels.length; ++i)
        {
            try {
                dos.writeInt(outPixels[i]);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        byte[] transformed = baos.toByteArray();

        //Log.d(TAG, Arrays.toString(transformed));

        Bitmap outBmp = Bitmap.createBitmap(outPixels, outWidth, outHeight, Bitmap.Config.ARGB_8888);
        if(outBmp == null)
            Log.d(TAG, "result bitmap is null");
        */

        /**
         * For future use
         * Create Bitmap using the stored byte array
         */
        //BitmapFactory.Options options = new BitmapFactory.Options();
        //Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
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
