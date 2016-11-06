package edu.asu.msrs.artcelerationlibrary;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
/*This service will operate depending on messenegr*/

public class MyArtTransService extends Service {
    public MyArtTransService() {
    }
    String TAG = "MyArtTransService";
    static final int MSG_1 = 1;
    static final int MSG_2 = 2;

    class MyArtTransServiceHandler extends Handler{
        @Override
        public void handleMessage(Message objMessage){
            Log.d(TAG,"inside funcHandleMesage" + objMessage.what);
            switch(objMessage.what){
                case MSG_1:
                    int res = objMessage.arg1 + objMessage.arg2;
                    Log.d(TAG, "1----"+res);
                    Bundle serviceDataBundle = objMessage.getData();
                    ParcelFileDescriptor pfd = (ParcelFileDescriptor)serviceDataBundle.get("pfd");
                    //FileInputStream fis = new FileInputStream(pfd);
                    Log.d(TAG, "here"+pfd);
                    break;
                case MSG_2:
                    Log.d(TAG, "2----");
                    Log.d(TAG,"Testing branch");
                    break;
                default:
                    break;
            }
        }

    }
    /* messenger object to Handle messages that come in */
    final Messenger objMessenger = new Messenger(new MyArtTransServiceHandler());
    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        //returning when someone binds
        //throw new UnsupportedOperationException("Not yet implemented");
        return objMessenger.getBinder();
    }
}
