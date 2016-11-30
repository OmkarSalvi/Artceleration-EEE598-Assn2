#include<edu_asu_msrs_artcelerationlibrary_NativeClass.h>
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <android/bitmap.h>
#include <cstring>
#include <unistd.h>


#define  LOG_TAG    "Applog"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

    JNIEXPORT jstring
    JNICALL Java_edu_asu_msrs_artcelerationlibrary_NativeClass_getMessageFromJNI
            (JNIEnv *env, jclass obj) {
        LOGD("reading bitmap info...");
        return env->NewStringUTF("This is omkar from JNI");
    }


JNIEXPORT jobject JNICALL Java_edu_asu_msrs_artcelerationlibrary_NativeClass_rotateBitmapCcw90(JNIEnv * env, jobject obj, jobject bitmap)
{
    //
    //getting bitmap info:
    //
    LOGD("reading bitmap info...");
    AndroidBitmapInfo info;
    int ret;
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0)
    {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return NULL;
    }
    LOGD("width:%d height:%d stride:%d", info.width, info.height, info.stride);
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888)
    {
        LOGE("Bitmap format is not RGBA_8888!");
        return NULL;
    }
    //
    //read pixels of bitmap into native memory :
    //
    LOGD("reading bitmap pixels...");
    void* bitmapPixels;
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &bitmapPixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return NULL;
    }
    uint32_t* src = (uint32_t*) bitmapPixels;
    uint32_t* tempPixels = new uint32_t[info.height * info.width];
    int stride = info.stride;
    int pixelsCount = info.height * info.width;
    memcpy(tempPixels, src, sizeof(uint32_t) * pixelsCount);
    AndroidBitmap_unlockPixels(env, bitmap);
    //
    //recycle bitmap - using bitmap.recycle()
    //
    LOGD("recycling bitmap...");
    jclass bitmapCls = env->GetObjectClass(bitmap);
    jmethodID recycleFunction = env->GetMethodID(bitmapCls, "recycle", "()V");
    if (recycleFunction == 0)
    {
        LOGE("error recycling!");
        return NULL;
    }
    env->CallVoidMethod(bitmap, recycleFunction);
    //
    //creating a new bitmap to put the pixels into it - using Bitmap Bitmap.createBitmap (int width, int height, Bitmap.Config config) :
    //
    LOGD("creating new bitmap...");
    jmethodID createBitmapFunction = env->GetStaticMethodID(bitmapCls, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
    jstring configName = env->NewStringUTF("ARGB_8888");
    jclass bitmapConfigClass = env->FindClass("android/graphics/Bitmap$Config");
    jmethodID valueOfBitmapConfigFunction = env->GetStaticMethodID(bitmapConfigClass, "valueOf", "(Ljava/lang/String;)Landroid/graphics/Bitmap$Config;");
    jobject bitmapConfig = env->CallStaticObjectMethod(bitmapConfigClass, valueOfBitmapConfigFunction, configName);
    jobject newBitmap = env->CallStaticObjectMethod(bitmapCls, createBitmapFunction, info.height, info.width, bitmapConfig);
    //
    // putting the pixels into the new bitmap:
    //
    if ((ret = AndroidBitmap_lockPixels(env, newBitmap, &bitmapPixels)) < 0)
    {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
        return NULL;
    }
    uint32_t* newBitmapPixels = (uint32_t*) bitmapPixels;
    int whereToPut = 0;
    for (int x = info.width - 1; x >= 0; --x)
        for (int y = 0; y < info.height; ++y)
        {
            uint32_t pixel = tempPixels[info.width * y + x];
            newBitmapPixels[whereToPut++] = pixel;
        }
    AndroidBitmap_unlockPixels(env, newBitmap);
    //
    // freeing the native memory used to store the pixels
    //
    delete[] tempPixels;
    return newBitmap;
}

static int rgb_clamp(int value) {
    if(value > 255) {
        return 255;
    }
    if(value < 0) {
        return 0;
    }
    return value;
}

//static int* intArgs;//int array of arguments

static int convertRange(int input, int color, jint intArgs[]){
    /**
     * confining the input value in 0 to 255
     */
    input = rgb_clamp(input);

    int slope = 0, constant = 0, output = 0;

    /**
     * Calculating the output value with color filter
     */
    if(0 <= input && input <= intArgs[color] && intArgs[color] != 0){
        slope = (intArgs[color+1]/intArgs[color]);
        output = slope*input;
    }else if(intArgs[color] < input && input <= intArgs[color+2]){
        slope = ((intArgs[color+3] - intArgs[color+1])/(intArgs[color+2] - intArgs[color]));
        constant = intArgs[color+3] - (slope*intArgs[color+2]);
        output = slope*input + constant;
    }else if(intArgs[color+2] < input && input <= intArgs[color+4]){
        slope = ((intArgs[color+5] - intArgs[color+3])/(intArgs[color+4] - intArgs[color+2]));
        constant = intArgs[color+5] - (slope*intArgs[color+4]);
        output = slope*input + constant;
    }else if(intArgs[color+4] < input && input <= intArgs[color+6]){
        slope = ((intArgs[color+7] - intArgs[5])/(intArgs[color+6] - intArgs[color+4]));
        constant = intArgs[color+7] - (slope*intArgs[color+6]);
        output = slope*input + constant;
    }else if(intArgs[color+6] < input && input <= 255){
        slope = ((255 - intArgs[color+7])/(255 - intArgs[color+6]));
        output = slope*input;
    }
    /**
     * confining the output value in 0 to 255
     */
    output = rgb_clamp(output);
    return output;
}

static void transformcolors(AndroidBitmapInfo* info, void* pixels, float brightnessValue, jint intArgs[]){
    int xx, yy, red, green, blue, alpha;
    uint32_t* line;

    for(yy = 0; yy < info->height; yy++){
        line = (uint32_t*)pixels;
        for(xx =0; xx < info->width; xx++){

            //extract the RGB values from the pixel
            alpha = (int) ((line[xx] & 0xFF000000) >> 24);
            red = (int) ((line[xx] & 0x00FF0000) >> 16);
            green = (int)((line[xx] & 0x0000FF00) >> 8);
            blue = (int) (line[xx] & 0x00000FF );

            red = convertRange(red, 0 , intArgs);
            green = convertRange(green, 8 , intArgs);
            blue = convertRange(blue, 16 , intArgs);

            // set the new pixel back in
            line[xx] =
                    ((alpha << 24) & 0xFF000000) |
                    ((red << 16) & 0x00FF0000) |
                    ((green << 8) & 0x0000FF00) |
                    (blue & 0x000000FF);
        }

        pixels = (char*)pixels + info->stride;
    }
}


JNIEXPORT jobject JNICALL Java_edu_asu_msrs_artcelerationlibrary_NativeClass_colorfilterndk(JNIEnv * env, jobject  obj, jobject bitmap, jfloat brightnessValue, jintArray arr)
{
    LOGD("reading bitmap info...");
    AndroidBitmapInfo  info;
    int ret, i;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return bitmap;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return bitmap;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    /*
    LOGD("int args in 1st : ");
    jsize len = env->GetArrayLength(env, arr);
    jint *body = env->GetIntArrayElements(env, arr, 0);
    for (i=0; i<len; i++) {
        intArgs[i]=body[i];
        LOGD("%d | %d",body[i], intArgs[i]);
    }*/

    // initializations, declarations, etc
    jint *c_array;
    jint j = 0;

    // get a pointer to the array
    c_array = env->GetIntArrayElements(arr, 0);

    // do some exception checking
    if (c_array == NULL) {
        LOGD("array is NULL");
        return bitmap; /* exception occurred */
    }


    // release the memory so java can have it again
    //env->ReleaseIntArrayElements(env, arr, c_array);

    //LOGD("int args in 1st : %d %d %d %d",arr[0],arr[1], arr[2], arr[3]);
    //intArgs = arr;
    transformcolors(&info,pixels, brightnessValue, c_array);

    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;
}

static void getVertical(AndroidBitmapInfo* info, void* pixels, int rad){
    int rows = 2*rad+1;
    int H = info->height;//source bitmap # of rows
    int W = info->width;//source bitmap # of columns
    LOGD("Height = %d | Width = %d | rows = %d", H, W, rows );

    /*
    // color information
    int Alpha[][] = new int[W][H], Red = new int[W][H], Green = new int[W][H], Blue = new int[W][H] ;

        for(int x=0; x<W; x++){
            for(int y=0; y<H;y++){
                int eachpixel = bmp.getPixel(x,y);
                Alpha[x][y] = Color.alpha(eachpixel);
                Red[x][y] = Color.red(eachpixel);
                Green[x][y] = Color.green(eachpixel);
                Blue[x][y] = Color.blue(eachpixel);
            }
        }

        int qR[][] = new int[W][H];
        int qG[][] = new int[W][H];
        int qB[][] = new int[W][H];

        for(int x=0; x<W; x++){
            for(int y=0; y<H; y++){
                qR[x][y] = 0; qG[x][y] = 0; qB[x][y] = 0;
                for(int r = -rad ; r<= rad; r++){
                    if((y + r) >= 0 && (y + r) < H ){
                        qR[x][y] += Red[x][y + r];
                        qG[x][y] += Green[x][y + r];
                        qB[x][y] += Blue[x][y + r];
                    }
                }
            }
        }
        Log.d(TAG,"Vertical calculation finished");

        Bitmap OutBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        */
        /**
         * setting individual pixels of Final output bitmap
         */
    /*
        for(int x =0 ; x< OutBmp.getWidth(); x++){
            for(int y=0; y < OutBmp.getHeight(); y++){
                int color = Color.argb(Alpha[x][y], qR[x][y]/rows, qG[x][y]/rows, qB[x][y]/rows);
                OutBmp.setPixel(x, y, color);
            }
        }*/
    LOGD("Finished with MotionBlur");
        //return OutBmp;
    }


static void getHorizontal(AndroidBitmapInfo* info, void* pixels, int rad){

    int rows = 2*rad+1;
    int H = info->height;//source bitmap # of rows
    int W = info->width;//source bitmap # of columns
    LOGD("Height = %d | Width = %d | rows = %d", H, W, rows);

    /*
    //Initializing pixel arrays
    int Alpha[][] = new int[W][H], Red = new int[W][H], Green = new int[W][H], Blue = new int[W][H] ;
        int x,y;
        // Extracting individual RGB pixel from original image
        for(x=0; x<W; x++){
            for(y=0; y<H;y++){
                Alpha[x][y] = Color.alpha(bmp.getPixel(x,y));
                Red[x][y] = Color.red(bmp.getPixel(x,y));
                Green[x][y] = Color.green(bmp.getPixel(x,y));
                Blue[x][y] = Color.blue(bmp.getPixel(x,y));
            }
        }

        int qR[][] = new int [W][H];
        int qG[][] = new int[W][H];
        int qB[][] = new int[W][H];

        for(x=0; x<W; x++){
            for(y=0; y<H; y++){
                qR[x][y] = 0; qG[x][y] = 0; qB[x][y] = 0;
                for(int r = -rad ; r<= rad; r++){
                    if((x + r) >= 0 && (x + r) < W ){
                        qR[x][y] += Red[x + r][y];
                        qG[x][y] += Green[x + r][y];
                        qB[x][y] += Blue[x + r][y];
                    }
                }
            }
        }
        Log.d(TAG,"Horizontal calculation finished");


        Bitmap OutBmp = bmp.copy(Bitmap.Config.ARGB_8888, true);
        */
        /**
         * setting individual pixels of Final output bitmap
         */
    /*
        for(x =0 ; x< OutBmp.getWidth(); x++){
            for(y=0; y < OutBmp.getHeight(); y++){
                int color = Color.argb(Alpha[x][y], qR[x][y]/rows, qG[x][y]/rows, qB[x][y]/rows);
                OutBmp.setPixel(x, y, color);
            }
        }*/
    LOGD("Finished with MotionBlur");
        //return OutBmp;
    }

JNIEXPORT jobject JNICALL Java_edu_asu_msrs_artcelerationlibrary_NativeClass_motionblurneon(JNIEnv * env, jobject  obj, jobject bitmap, jint a0, jint a1)
{
    LOGD("reading bitmap info...");
    AndroidBitmapInfo  info;
    int ret, i;
    void* pixels;

    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return bitmap;
    }
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return bitmap;
    }

    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    LOGD("int args : %d %d ",a0, a1);

    if(a0 == 0){
        getHorizontal(&info, pixels, a1);
    }else if(a0 == 1){
        getVertical(&info, pixels, a1);
    }

    AndroidBitmap_unlockPixels(env, bitmap);

    return bitmap;
}

