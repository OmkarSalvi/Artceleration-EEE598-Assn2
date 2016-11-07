package edu.asu.msrs.artcelerationlibrary;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.MemoryFile;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
/*This service will operate depending on messenegr*/

public class MyArtTransService extends Service {

    public MyArtTransService() {
    }
    String TAG = "MyArtTransService";

    static final int OPTION_1 = 1;
    static final int OPTION_2 = 2;
    static final int OPTION_3 = 3;
    static final int OPTION_4 = 4;
    static final int OPTION_5 = 5;

    /**
     * Messenger object to Handle messages that come in from library
     */
    final Messenger objMessenger = new Messenger(new MyArtTransServiceHandler());

    class MyArtTransServiceHandler extends Handler{
        @Override
        public void handleMessage(Message objMessage){
            Log.d(TAG,"MyArtTransServiceHandler handleMessage" + objMessage.what);

            switch(objMessage.what){
                case OPTION_1:
                    int byteArraySize = objMessage.arg1;
                    Log.d(TAG, "Passed size from Library: "+byteArraySize);
                    byte[] buffer = new byte[byteArraySize];
                    Bundle serviceDataBundle = objMessage.getData();
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
                        Message newMsg = Message.obtain(null, what, byteArraySize + 100,0);
                        Log.d(TAG, "objMessage.replyTo: " + objMessage.replyTo);
                        Messenger mClient =  objMessage.replyTo;
                        newMsg.setData(bunData);
                        try {
                            mClient.send(newMsg);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                case OPTION_2:
                    Log.d(TAG, "Case 2:");
                    break;
                case OPTION_3:
                    break;
                case OPTION_4:
                    break;
                case OPTION_5:
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        /**
         * Return the communication channel to the service
         * returning when someone binds
         */
        return objMessenger.getBinder();
    }
}
