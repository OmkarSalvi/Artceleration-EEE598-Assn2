package edu.asu.msrs.artcelerationlibrary;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;

/**
 * Created by abhishek on 11/7/2016.
 */

public class GaussianBlurTransform implements Runnable {

    String TAG = "GaussianBlurTransform";
    int byteArraySize;
    Bundle serviceDataBundle;
    int req;
    Messenger messengerGBT;
    GaussianBlurTransform(int size, Bundle bund, int request, Messenger gbt){
        byteArraySize = size;
        serviceDataBundle = bund;
        req = request;
        messengerGBT = gbt;
    }
    @Override
    public void run() {

        //byteArraySize = objMessGBT.arg1;
        Log.d(TAG, "Passed size from Library: "+byteArraySize);
        byte[] buffer = new byte[byteArraySize];
        ParcelFileDescriptor pfd = (ParcelFileDescriptor)serviceDataBundle.get("libPFD");
        ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
        try {
            input.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Log.d(TAG, "here"+pfd);
        BitmapFactory.Options options = new BitmapFactory.Options();
        Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
        Log.d(TAG, String.valueOf(bmp.getByteCount()));
        int what;
        try {
            MemoryFile objMemoryFile = new MemoryFile("MemoryFileObject",buffer.length);
            objMemoryFile.writeBytes(buffer,0,0, buffer.length);
            ParcelFileDescriptor objPFD = MemoryFileUtil.getParcelFileDescriptor(objMemoryFile);
            what = 100;
            Bundle bunData = new Bundle();
            bunData.putParcelable("objPFD",objPFD);

            Message newMsg = Message.obtain(null, what, byteArraySize,req);
            Log.d(TAG, "objMessage.replyTo: " + messengerGBT);
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
