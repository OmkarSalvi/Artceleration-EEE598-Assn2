package edu.asu.msrs.artcelerationlibrary;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
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
                    int res = objMessage.arg1;
                    Log.d(TAG, "passed size is "+res);
                    byte[] buffer = new byte[res];
                    Bundle serviceDataBundle = objMessage.getData();
                    ParcelFileDescriptor pfd = (ParcelFileDescriptor)serviceDataBundle.get("pfd");
                    FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());

                    ParcelFileDescriptor.AutoCloseInputStream input = new ParcelFileDescriptor.AutoCloseInputStream(pfd);
                    try {
                        input.read(buffer);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        Log.d(TAG, "-#-"+fis.read() +"#"+buffer.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.d(TAG, "here"+pfd);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    Bitmap bmp = BitmapFactory.decodeByteArray(buffer, 0, buffer.length, options);
                    Log.d(TAG, String.valueOf(bmp.getByteCount()));
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
