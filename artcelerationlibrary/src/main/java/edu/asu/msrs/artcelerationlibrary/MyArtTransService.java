/**
 * This service creates different transform threads on the basis of transform type
 * For checkpoint-1, we have just created one transform class i.e GaussianBlurTransform
 * GaussianBlurTransform will process all threads
 */

package edu.asu.msrs.artcelerationlibrary;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;

/*This service will operate depending on messenegr*/

public class MyArtTransService extends Service {

    public MyArtTransService() {
    }
    String TAG = "MyArtTransService";
    /**
     * Options to select transform
     */
    static final int OPTION_0 = 0;
    static final int OPTION_1 = 1;
    static final int OPTION_2 = 2;
    static final int OPTION_3 = 3;
    static final int OPTION_4 = 4;

    /**
     * Messenger object to Handle messages that come in from library
     */
    final Messenger objMessenger = new Messenger(new MyArtTransServiceHandler());

    /**
     * Handler class to implement operations on the message received from the Art library
     */
    class MyArtTransServiceHandler extends Handler{
        /**
         * Helper to handle messages received from library
         * @param objMessage: Message object received from Library
         */
        @Override
        public void handleMessage(Message objMessage){
            /**
             * Logging message variables for debugging
             */
            Log.d(TAG,"MyArtTransServiceHandler handleMessage" + objMessage.what);
            Log.d(TAG,"Length inside Service: "+ objMessage.arg1);
            Log.d(TAG,"objmessage : "+ objMessage.what);
            /**
             * Creation of threads depending on transform types i.e. index
             * For example, if the index passed is 0 then GaussianBlurTransform is implemented by creating thread of same class
             * For checkpoint-1, only GaussianBlurTransform is implemented
             * Different classes will be created for various transform defined in the transform array in library
             */
            switch(objMessage.what){
                case OPTION_0:
                    Log.d(TAG, "OPTION_0");
                    GaussianBlurTransform objGBT0 = new GaussianBlurTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo);
                    new Thread(objGBT0).start();
                    break;
                case OPTION_1:
                    Log.d(TAG, "OPTION_1");
                    GaussianBlurTransform objGBT1 = new GaussianBlurTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo);
                    UnsharpMaskTransform objUSM1 = new UnsharpMaskTransform(objMessage.arg1, objMessage.getData(), objMessage.arg2, objMessage.replyTo, objGBT1);
                    new Thread(objUSM1).start();
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
                    Log.d(TAG, "Invalid Transform!!");
                    break;
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        /**
         * Return the communication channel to the service when someone binds
         */
        return objMessenger.getBinder();
    }
}

