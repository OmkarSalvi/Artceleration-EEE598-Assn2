/**
 * Library to perform necessary operations invoked from application
 * Library will perform various operations such as getTransform, registerHandler, requestTransform
 * RequestTransform will pass all data created from arguments such as index, bitmap, etc. to service
 * Service will create thread for creation of transformed image in corresponding transform class
 */
package edu.asu.msrs.artcelerationlibrary;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Created by rlikamwa on 10/2/2016.
 */

public class ArtLib {

    String TAG = "ArtLib";
    private TransformHandler artlistener;
    private Activity objActivity;
    private Bitmap resultBitmap = null;
    private boolean boolBound;
    public static int globalRequestId = 0;
    ConcurrentLinkedQueue<Integer> artLibQueue = new ConcurrentLinkedQueue<Integer>();
    /**
     * Variables to check file permissions for API 23+
     */
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] ARRAY_PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    /**
     * Create messenger object for Library and Service
     */
    private Messenger objServiceMessenger;
    final Messenger artLibMessenger = new Messenger(new ArtLibIncomingHandler());

    /**
     * Constructor to initialize activity
     *
     * @param activity
     */
    public ArtLib(Activity activity){
        objActivity = activity;
        startUp();
    }

    /**
     * For future use
     * Helper to request file permissions at run time
     * Helper will prompt user to get storage permissions on device
     *
     * @param activity : object of current Activity
     */
    public static void checkPermissions(Activity activity) {
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, ARRAY_PERMISSIONS, REQUEST_EXTERNAL_STORAGE);
        }
    }

    /**
     * Handler class to handle response messages from service named MyArtTransService
     */
    class ArtLibIncomingHandler extends Handler{
        /**
         * Helper to handle messages received from service
         * @param resultMsg: Message object received from service MyArtTransService
         */
        @Override
        public void handleMessage(Message resultMsg){

            /**
             * Create object of artLibThread class to perform operations on the message received from service
             * Create thread for every message and perform transform
             */
            artLibThread objartLibThread = new artLibThread(resultMsg.arg1, resultMsg.arg2, resultMsg.getData(), artLibQueue, artlistener);
            new Thread(objartLibThread).start();

        }
    }

    /**
     * Helper to create and store media file in storage directory
     *
     * @param bitmapImage : Bitmap created using byte array obtained from service
     */
    private void createProcessedBitmap(Bitmap bitmapImage) {
        File objFile = createFilename();
        if (objFile == null) {
            Log.d(TAG, "No file created. Try again!!");
            return;
        }
        Log.d(TAG,"Filename: "+objFile.getAbsolutePath());
        //checkPermissions(objActivity);
        try {
            OutputStream out = null;
            out = new FileOutputStream(objFile);
            bitmapImage.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.close();
        } catch (FileNotFoundException e) {
            Log.d(TAG, "File Not Found Exception: " + e.getMessage());
        } catch (IOException e) {
            Log.d(TAG, "IO Exception: " + e.getMessage());
        }
    }

    /**
     * Helper to create the file in storage directory of device
     *
     * @return filename : File object depending on timestamp
     */
    private File createFilename(){
        File galleryDir = new File(Environment.getExternalStorageDirectory().toString());
        if (! galleryDir.exists()){
            if (! galleryDir.mkdirs()){
                return null;
            }
        }
        String createTime = new SimpleDateFormat("dd_MM_HH_mm").format(new Date());
        File objFile;
        String mImageName="IMG_"+ createTime +".jpg";
        Log.d(TAG,mImageName);
        objFile = new File(galleryDir.getPath() + File.separator + mImageName);
        return objFile;
    }


    /**
     * Helper to bind the service to the current activity
     * access the service variables using objServiceMessenger
     */
    ServiceConnection objServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            objServiceMessenger = new Messenger(service);
            boolBound = true;
            Log.d(TAG,"Service Connected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            objServiceMessenger = null;
            boolBound = false;
            Log.d(TAG,"Service disconnected");
        }
    };

    /**
     * Helper to bind service with the activity
     */
    public void startUp(){
        objActivity.bindService(new Intent(objActivity, MyArtTransService.class), objServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Helper to create string transform array
     *
     * @return Transform: String array which defines provided transform
     */
    public String[] getTransformsArray(){
        String[] transforms = {"Gaussian Blur", "Unsharp Mask", "Color Filter", "Edge detection", "ASCII transform"};
        return transforms;
    }

    /**
     * Helper to get transformtest array
     * @return TransformTest: Transform array which defines index, integer argument and float argument
     */
    public TransformTest[] getTestsArray(){
        TransformTest[] transforms = new TransformTest[5];
        transforms[0]=new TransformTest(0, new int[]{1}, new float[]{10.0f});
        transforms[1]=new TransformTest(1, new int[]{11,22,33}, new float[]{10.0f, 2.0f, 0.3f});
        transforms[2]=new TransformTest(2, new int[]{32,128,64,160,128,192,192,255,0,128,64,160,128,192,192,255,0,128,64,160,128,192,192,255}, new float[]{0.5f, 0.6f, 0.3f});
        transforms[3]=new TransformTest(3, new int[]{61,72,29}, new float[]{0.4f, 0.3f, 0.8f});
        transforms[4]=new TransformTest(4, new int[]{41,82,35}, new float[]{0.7f, 0.2f, 0.5f});
        return transforms;
    }

    public void registerHandler(TransformHandler artlistener){
        this.artlistener=artlistener;

    }

    public boolean requestTransform(Bitmap img, int index, int[] intArgs, float[] floatArgs){

        /**
         * Creating byte array using received bitmap
         */
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] byteStreamArray = byteStream.toByteArray();
        Log.d(TAG,"length is :"+String.valueOf(byteStreamArray.length));
        Log.d(TAG, "int intArgs : "+intArgs[0]);
        Log.d(TAG, "int floatArgs : "+floatArgs[0]);
        try {
            /**
             * Binding data to message object using memory file object
             */
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject",byteStreamArray.length);
            objMemoryFile.writeBytes(byteStreamArray,0,0, byteStreamArray.length);
            ParcelFileDescriptor libPFD = MemoryFileUtil.getParcelFileDescriptor(objMemoryFile);
            int what = index;
            Bundle bundleData = new Bundle();
            bundleData.putParcelable("libPFD",libPFD);
            bundleData.putIntArray("intArray",intArgs );
            bundleData.putFloatArray("floatArray", floatArgs);
            Message newMsg = Message.obtain(null, what, byteStreamArray.length, globalRequestId);
            /**
             * Setting the messenger object to library messenger
             */
            newMsg.replyTo = artLibMessenger;
            /**
             * Adding bundle data to message object
             */
            newMsg.setData(bundleData);
            artLibQueue.add(globalRequestId);
            Log.d(TAG,"globalRequestId: "+globalRequestId);
            try {
                objServiceMessenger.send(newMsg);
                Log.d(TAG, "Queue before sending: "+artLibQueue.toString());
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        /**
         * Incrementing the global counter for transform request
         */
        globalRequestId++;
        return true;
    }

}
