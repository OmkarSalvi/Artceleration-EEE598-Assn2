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

    static final int OPTION_0 = 0;
    static final int OPTION_1 = 1;
    static final int OPTION_2 = 2;
    static final int OPTION_3 = 3;
    static final int OPTION_4 = 4;

    /**
     * Messenger object to Handle messages that come in from library
     */
    final Messenger objMessenger = new Messenger(new MyArtTransServiceHandler());

    class MyArtTransServiceHandler extends Handler{
        @Override
        public void handleMessage(Message objMessage){
            Log.d(TAG,"MyArtTransServiceHandler handleMessage" + objMessage.what);
            Log.d(TAG,"Length inside : "+ objMessage.arg1);
            Log.d(TAG,"objmessage : "+ objMessage.what);
            switch(objMessage.what){
                case OPTION_0:
                    Log.d(TAG, "OPTION_0");
                    GaussianBlurTransform objGBT0 = new GaussianBlurTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo);
                    new Thread(objGBT0).start();
                    break;
                case OPTION_1:
                    Log.d(TAG, "OPTION_1");
                    GaussianBlurTransform objGBT1 = new GaussianBlurTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo);
                    new Thread(objGBT1).start();
                    break;
                case OPTION_2:
                    Log.d(TAG, "OPTION_2");
                    GaussianBlurTransform objGBT2 = new GaussianBlurTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo);
                    new Thread(objGBT2).start();
                    break;
                case OPTION_3:
                    Log.d(TAG, "OPTION_3");
                    GaussianBlurTransform objGBT3 = new GaussianBlurTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo);
                    new Thread(objGBT3).start();
                    break;
                case OPTION_4:
                    Log.d(TAG, "OPTION_4");
                    GaussianBlurTransform objGBT4 = new GaussianBlurTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo);
                    new Thread(objGBT4).start();
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

