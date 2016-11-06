package edu.asu.msrs.artcelerationlibrary;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

//import edu.asu.msrs.artcelerationlibrary.MyArtTransService;
/**
 * Created by rlikamwa on 10/2/2016.
 */

public class ArtLib {
    String TAG = "ArtLib";
    private TransformHandler artlistener;
    private Activity objActivity;
    public ArtLib(Activity activity){
        objActivity = activity;
        startUp();
    }
    private Messenger objMess;
    private boolean objBound;
    ServiceConnection objeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            objMess = new Messenger(service);
            objBound = true;

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            objMess = null;
            objBound = false;
        }
    };

    public void startUp(){
        objActivity.bindService(new Intent(objActivity, MyArtTransService.class), objeServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public String[] getTransformsArray(){
        String[] transforms = {"Gaussian Blur", "Neon edges", "Color Filter"};
        return transforms;
    }

    public TransformTest[] getTestsArray(){
        TransformTest[] transforms = new TransformTest[3];
        transforms[0]=new TransformTest(0, new int[]{1,2,3}, new float[]{0.1f, 0.2f, 0.3f});
        transforms[1]=new TransformTest(1, new int[]{11,22,33}, new float[]{0.3f, 0.2f, 0.3f});
        transforms[2]=new TransformTest(2, new int[]{51,42,33}, new float[]{0.5f, 0.6f, 0.3f});

        return transforms;
    }

    public void registerHandler(TransformHandler artlistener){
        this.artlistener=artlistener;
    }

    public boolean requestTransform(Bitmap img, int index, int[] intArgs, float[] floatArgs){
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        img.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        byte[] byteStreamArray = byteStream.toByteArray();
        Log.d(TAG,String.valueOf(byteStreamArray.length));
        try {
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject",byteStreamArray.length);
            objMemoryFile.writeBytes(byteStreamArray,0,0, byteStreamArray.length);

            ParcelFileDescriptor pfd = MemoryFileUtil.getParcelFileDescriptor(objMemoryFile);
            int what = MyArtTransService.MSG_1;
            Bundle bundleData = new Bundle();
            bundleData.putParcelable("pfd",pfd);

            Message newMsg = Message.obtain(null, what, byteStreamArray.length,0);

            newMsg.setData(bundleData);
        try {
            objMess.send(newMsg);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

}
