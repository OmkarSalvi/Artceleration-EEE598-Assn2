/**
 * This is C++ code which contains the implementations of all the native functions and its helper functions.
 * This is the implementation of native library
 * This file has implementation for color filter and motion blur using NDK
 */
#include<edu_asu_msrs_artcelerationlibrary_NativeClass.h>
#include <jni.h>
#include <android/log.h>
#include <stdio.h>
#include <android/bitmap.h>
#include <cstring>
#include <unistd.h>
#include<arm_neon.h>
#include <string>
#include <sstream>
#include<exception>

using namespace std;

#define  LOG_TAG    "Applog"
#define  LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG,LOG_TAG,__VA_ARGS__)
#define  LOGE(...)  __android_log_print(ANDROID_LOG_ERROR,LOG_TAG,__VA_ARGS__)
#define  LOGI(...)  __android_log_print(ANDROID_LOG_INFO,LOG_TAG,__VA_ARGS__)

/**
 * Test function to send a string from native library
 */
    JNIEXPORT jstring
    JNICALL Java_edu_asu_msrs_artcelerationlibrary_NativeClass_getMessageFromJNI
            (JNIEnv *env, jclass obj) {
        LOGD("reading bitmap info...");
        return env->NewStringUTF("This is omkar from JNI");
    }


/**
 * method to restrict pixel values between 0 and 255
 * @param value: integer variable representing the pixel value
 * @return integer pixel value after clamping the input value between 0 and 255
 */
static int rgb_clamp(int value) {
    if(value > 255) {
        return 255;
    }
    if(value < 0) {
        return 0;
    }
    return value;
}


/**
 * method to implement color filter on each color channel
 * @param input : input value of the pixel color
 * @param color : the color of the pixel -> 0 : red, 8 : green, 16 : blue
 *                this is used to get correct index inside argument integer array
 * @param intArgs[] : integer array which holds the mapping points for all 3 color channels
 * @return integer value which is final value of the color channel after transform
 */
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

/**
 * method which iterarte through all the pixels inside the bitmap. It separates the color channels from the pixel and request the color filter transform
 * @param info : object which stores the information about bitmap
 * @param pixels : pointer to the pixels in the bitmap
 * @param intArgs[] : integer array which holds the mapping points for all 3 color channels
 * @return void
 */
static void transformcolors(AndroidBitmapInfo* info, void* pixels, jint intArgs[]){
    int xx, yy, red, green, blue, alpha;
    uint32_t* line;

    //iterate through all pixels column wise
    for(yy = 0; yy < info->height; yy++){
        line = (uint32_t*)pixels;
        for(xx =0; xx < info->width; xx++){

            //extract the RGB values from the pixel
            alpha = (int) ((line[xx] & 0xFF000000) >> 24);
            red = (int) ((line[xx] & 0x00FF0000) >> 16);
            green = (int)((line[xx] & 0x0000FF00) >> 8);
            blue = (int) (line[xx] & 0x00000FF );

            //request color transform for each color channel of a pixel
            red = convertRange(red, 0 , intArgs);
            green = convertRange(green, 8 , intArgs);
            blue = convertRange(blue, 16 , intArgs);

            // set the new pixel back in bitmap
            line[xx] =
                    ((alpha << 24) & 0xFF000000) |
                    ((red << 16) & 0x00FF0000) |
                    ((green << 8) & 0x0000FF00) |
                    (blue & 0x000000FF);
        }

        pixels = (char*)pixels + info->stride;
    }
}

/**
 * Native method which is called to initiate the color filter transform
 * @param env :  the JNI interface pointer. A pointer to a structure storing all JNI function pointers
 * @param obj : Java class object or NULL if an error occurs
 * @param bitmap : bitmap object of the input image
 * @param arr : integer array which holds the mapping points for all 3 color channels
 * @return void
 */
JNIEXPORT jobject JNICALL Java_edu_asu_msrs_artcelerationlibrary_NativeClass_colorfilterndk(JNIEnv * env, jobject  obj, jobject bitmap, jintArray arr)
{
    LOGD("reading bitmap info...");
    AndroidBitmapInfo  info;
    int ret, i;
    void* pixels;

    //fill out the AndroidBitmapInfo struct for given bitmap
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return bitmap;
    }
    //verifying the format of the pixels stored in bitmap
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return bitmap;
    }

    //obtain the pixel values from the bitamp and attempt to lock the pixel address.
    // Locking will ensure that the memory for the pixels will not move until the unlockPixels call.
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }

    // declarations for storing int argument array
    jint *c_array;
    jint j = 0;

    // get a pointer to the int argument array
    c_array = env->GetIntArrayElements(arr, 0);

    // do some exception checking
    if (c_array == NULL) {
        LOGD("array is NULL");
        return bitmap; /* exception occurred */
    }


    //calling method transformcolors to start color filter transform
    transformcolors(&info,pixels, c_array);

    //Call to balance a successful call to AndroidBitmap_lockPixels.
    // after this time the address of the pixels should no longer be used.
    AndroidBitmap_unlockPixels(env, bitmap);

    //return bitmap which is transformed using color filter
    return bitmap;
}


/**
 * This method is for implementing motion blur using NEON.
 * But it is not working as expected
 */
static void getVerticalneon(AndroidBitmapInfo* info, void* pixels, int rad){
    int rows = 2*rad+1;
    int H = info->height;//source bitmap # of rows
    int W = info->width;//source bitmap # of columns
    LOGD("Height = %d | Width = %d | rows = %d", H, W, rows );

    int xx, yy;
    uint32_t* line, *red, *green, *blue, *alpha;
    uint32x4_t redMask = vdupq_n_u32 (0x00FF0000);
    uint32x4_t greenMask = vdupq_n_u32 (0x0000FF00);
    uint32x4_t blueMask = vdupq_n_u32 (0x000000FF);
    uint32x4_t alphaMask = vdupq_n_u32 (0xFF000000);

    for(yy = 0; yy < info->height; yy++) {
        line = (uint32_t *) pixels;
        for(xx =0; xx < info->width; xx+=4) {
            //LOGD("iteration %d started", xx);
            /**
             * 4 pixels loaded into 3 registers at same time
             * rgb[0] -> alpha
             * rgb[1] -> red
             * rgb[2] -> green
             * rgb[3] -> blue
             */
            uint32x4x4_t rgb = vld4q_u32(line);

            /**
             * Logical And with corresponding mask for each color
             */
            rgb.val[0] = vandq_u32(rgb.val[0], alphaMask);
            rgb.val[1] = vandq_u32(rgb.val[1], redMask);
            rgb.val[2] = vandq_u32(rgb.val[2], greenMask);
            rgb.val[3] = vandq_u32(rgb.val[3], blueMask);

            /**
             * right shift
             */
            //rgb.val[0] = vshrq_n_u32(rgb.val[0], 24);
            rgb.val[1] = vshrq_n_u32(rgb.val[1], 16);
            rgb.val[2] = vshrq_n_u32(rgb.val[2], 8);

            /*
            //storing color info in different arrays
            vst1q_u32(red, rgb.val[1]);
            vst1q_u32(green, rgb.val[2]);
            vst1q_u32(blue, rgb.val[3]);

            int rsum=0, gsum=0,bsum=0;
            rsum = *red + *(red+8) + *(red+16) + *(red+24);
            gsum = *green + *(green+8) + *(green+16) + *(green+24);
            bsum = *blue + *(blue+8) + *(blue+16) + *(blue+24);
            rsum = rgb_clamp(rsum);
            gsum = rgb_clamp(gsum);
            bsum = rgb_clamp (bsum);

            uint32x4_t redAvg = vdupq_n_u32 (rsum/4);
            uint32x4_t greenAvg = vdupq_n_u32 (gsum/4);
            uint32x4_t blueAvg = vdupq_n_u32 (bsum/4);

            redAvg = vshlq_n_u32(redAvg, 16);
            greenAvg = vshlq_n_u32(greenAvg, 8);

            uint32x4_t temp1;
            temp1 = vorrq_u32(rgb.val[0], redAvg);
            temp1 = vorrq_u32(temp1, greenAvg);
            temp1 = vorrq_u32(temp1, blueAvg);
            //vst1q_u32(line, temp1);
            //LOGD("finish");

             */
            //left shift

            rgb.val[1] = vshlq_n_u32(rgb.val[1], 16);
            rgb.val[2] = vshlq_n_u32(rgb.val[2], 8);


            //Logical And

            rgb.val[1] = vandq_u32(rgb.val[1], redMask);
            rgb.val[2] = vandq_u32(rgb.val[2], greenMask);
            rgb.val[3] = vandq_u32(rgb.val[3], blueMask);

            //Logical OR

            uint32x4_t temp;
            temp = vorrq_u32(rgb.val[0], rgb.val[1]);
            temp = vorrq_u32(temp, rgb.val[2]);
            temp = vorrq_u32(temp, rgb.val[3]);
            vst1q_u32(line, temp);
            //vaddq_u32()// addition of 2 uint32x4_t

            line += 4;// go to 5th pixel (i.e. add # pixels * size of pixel)
            //if(yy == 1065){
              //  LOGD("iteration %d ended", xx);
            //}
            //LOGD("iteration %d ended", xx);
        }

        pixels = (char*)pixels + (info->stride);
    }

    LOGD("Finished with MotionBlur");
        //return OutBmp;
    }

/**
 * This method computes the horizontal blur transform of the input bitmap
 * @param info :  object which stores the information about bitmap
 * @param pixels : pointer to the pixels in the bitmap
 * @param rad : radius for the average calculation
 * @return void
 */
static void getHorizontal(AndroidBitmapInfo* info, void* pixels, int rad){

    int rows = 2*rad+1;
    int H = info->height;//source bitmap # of rows
    int W = info->width;//source bitmap # of columns
    //LOGD("Height = %d | Width = %d | rows = %d | stride = %d", H, W, rows, info->stride);

    uint32_t* out_pixels = (uint32_t*)pixels;

    int xx, yy, red, green, blue, alpha, x, y;
    uint32_t* line;
    int *red_row = new int [W*H];int *green_row= new int [W*H];int *blue_row= new int [W*H]; int *alpha_row= new int [W*H];

    //iterate through all pixels row wise
    for(yy = 0; yy < info->height; yy++){
        line = (uint32_t*)pixels;
        for(xx =0; xx < info->width; xx++){
            //extract the ARGB values from the pixel
            alpha = (int) ((line[xx] & 0xFF000000) >> 24);
            red = (int) ((line[xx] & 0x00FF0000) >> 16);
            green = (int)((line[xx] & 0x0000FF00) >> 8);
            blue = (int) (line[xx] & 0x00000FF );

            //storing ARGB values in separate arrays
            *(alpha_row+yy*info->stride/4+xx) = alpha;
            *(red_row+yy*info->stride/4+xx) = red;
            *(green_row+yy*info->stride/4+xx) = green;
            *(blue_row+yy*info->stride/4+xx) = blue;

        }

        pixels = (char*)pixels + info->stride;
    }
    LOGD("done reading");
    int *qR = new int [W*H] ; int *qG= new int [W*H]; int *qB= new int [W*H];
    pixels = (void*) out_pixels;
    //Computing average in each row for given radius
    for(x=0; x<H; x++) {
        for (y = 0; y < W; y++) {
            qR[x*W+y] = 0;
            qG[x*W+y] = 0;
            qB[x*W+y] = 0;
            for (int r = -rad; r <= rad; r++) {
                if ((y + r) >= 0 && (y + r) < W) {
                    qR[x*W+y] += red_row[(x*W)+y+r];//*(red_row+((x + r)*4)+y);
                    qG[x*W+y] += green_row[(x*W)+y+r];//*(green_row+(x*W)+y+r);
                    qB[x*W+y] += blue_row[(x*W)+y+r];//*(blue_row+(x*W)+y+r);
                }
            }
            qR[x*W+y] /= rows;
            qG[x*W+y] /= rows;
            qB[x*W+y] /= rows;
        }
    }
    LOGD("done compute");
    //Storing computed values back into the original pixel location in bitmap
    for(yy = 0; yy < info->height; yy++){
        line = (uint32_t*)out_pixels;
        for(xx =0; xx < info->width; xx++){
            // set the new pixel back in bitmap
            line[xx] =
                    (((*(alpha_row+yy*info->stride/4+xx)) << 24) & 0xFF000000) |
                    ((qR[yy*info->stride/4+xx] << 16) & 0x00FF0000) |
                    ((qG[yy*info->stride/4+xx] << 8) & 0x0000FF00) |
                    (qB[yy*info->stride/4+xx] & 0x000000FF);
        }

        out_pixels = out_pixels + info->stride/4;
    }
    LOGD("Finished with MotionBlur");
    }

/**
 * This method computes the vertical blur transform of the input bitmap
 * @param info :  object which stores the information about bitmap
 * @param pixels : pointer to the pixels in the bitmap
 * @param rad : radius for the average calculation
 * @return void
 */
static void getVertical(AndroidBitmapInfo* info, void* pixels, int rad){

    int rows = 2*rad+1;
    int H = info->height;//source bitmap # of rows
    int W = info->width;//source bitmap # of columns
    LOGD("Height = %d | Width = %d | rows = %d | stride = %d", H, W, rows, info->stride);

    uint32_t* out_pixels = (uint32_t*)pixels;

    int xx, yy, red, green, blue, alpha, x, y;
    uint32_t* line;
    int *red_row = new int [W*H];int *green_row= new int [W*H];int *blue_row= new int [W*H]; int *alpha_row= new int [W*H];

    //iterate through all pixels row wise
    for(yy = 0; yy < info->height; yy++){
        line = (uint32_t*)pixels;
        for(xx =0; xx < info->width; xx++){
            //extract the ARGB values from the pixel
            alpha = (int) ((line[xx] & 0xFF000000) >> 24);
            red = (int) ((line[xx] & 0x00FF0000) >> 16);
            green = (int)((line[xx] & 0x0000FF00) >> 8);
            blue = (int) (line[xx] & 0x00000FF );

            //storing ARGB values in separate arrays
            *(alpha_row+yy*info->stride/4+xx) = alpha;
            *(red_row+yy*info->stride/4+xx) = red;
            *(green_row+yy*info->stride/4+xx) = green;
            *(blue_row+yy*info->stride/4+xx) = blue;

        }

        pixels = (char*)pixels + info->stride;
    }
    LOGD("done reading");
    int *qR = new int [W*H] ; int *qG= new int [W*H]; int *qB= new int [W*H];
    pixels = (void*) out_pixels;
    //Computing average in each column for given radius
    for(y=0; y<W; y++) {
        for (x = 0; x < H; x++) {
            qR[x*W+y] = 0;
            qG[x*W+y] = 0;
            qB[x*W+y] = 0;
            for (int r = -rad; r <= rad; r++) {
                if ((x + r) >= 0 && (x + r) < H) {
                    qR[x*W+y] += red_row[((x + r)*W)+y];//*(red_row+((x + r)*4)+y);
                    qG[x*W+y] += *(green_row+((x + r)*W)+y);
                    qB[x*W+y] += *(blue_row+((x + r)*W)+y);
                }
            }
            qR[x*W+y] /= rows;
            qG[x*W+y] /= rows;
            qB[x*W+y] /= rows;
        }
    }
    LOGD("done compute");
    //Storing computed values back into the original pixel location in bitmap
    for(yy = 0; yy < info->height; yy++){
        line = (uint32_t*)out_pixels;
        for(xx =0; xx < info->width; xx++){
            // set the new pixel back in bitmap
            line[xx] =
                    (((*(alpha_row+yy*info->stride/4+xx)) << 24) & 0xFF000000) |
                    ((qR[yy*info->stride/4+xx] << 16) & 0x00FF0000) |
                    ((qG[yy*info->stride/4+xx] << 8) & 0x0000FF00) |
                    (qB[yy*info->stride/4+xx] & 0x000000FF);
        }

        out_pixels = out_pixels + info->stride/4;
    }
    LOGD("Finished with MotionBlur");
}

/**
 * Native method which is called to initiate the motion blur transform
 * @param env :  the JNI interface pointer. A pointer to a structure storing all JNI function pointers
 * @param obj : Java class object or NULL if an error occurs
 * @param bitmap : bitmap object of the input image
 * @param a0 : integer which represents the direction of blur
 *              0 = Horizontal ; 1 = Vertical
 * @param a1 : integer which holds the radius for transform
 * @return void
 */
JNIEXPORT jobject JNICALL Java_edu_asu_msrs_artcelerationlibrary_NativeClass_motionblurndk(JNIEnv * env, jobject  obj, jobject bitmap, jint a0, jint a1)
{
    LOGD("reading bitmap info...");
    AndroidBitmapInfo  info;
    int ret, i;
    void* pixels;

    //fill out the AndroidBitmapInfo struct for given bitmap
    if ((ret = AndroidBitmap_getInfo(env, bitmap, &info)) < 0) {
        LOGE("AndroidBitmap_getInfo() failed ! error=%d", ret);
        return bitmap;
    }

    //verifying the format of the pixels stored in bitmap
    if (info.format != ANDROID_BITMAP_FORMAT_RGBA_8888) {
        LOGE("Bitmap format is not RGBA_8888 !");
        return bitmap;
    }

    //obtain the pixel values from the bitamp and attempt to lock the pixel address.
    // Locking will ensure that the memory for the pixels will not move until the unlockPixels call.
    if ((ret = AndroidBitmap_lockPixels(env, bitmap, &pixels)) < 0) {
        LOGE("AndroidBitmap_lockPixels() failed ! error=%d", ret);
    }
    LOGD("int args : %d %d ",a0, a1);
    LOGD("pixels in jni : %d %p ",*(uint32_t*)pixels, &pixels);
    if(a0 == 0){
        //calling method to do Horizontal blur
        getHorizontal(&info, pixels, a1);
    }else if(a0 == 1){
        //calling method to do Vertical blur
        getVertical(&info, pixels, a1);
    }
    LOGD("pixels in jni : %d %p ",*(uint32_t*)pixels, &pixels);
    //Call to balance a successful call to AndroidBitmap_lockPixels.
    // after this time the address of the pixels should no longer be used.
    AndroidBitmap_unlockPixels(env, bitmap);

    //return bitmap which is transformed using motion blur
    return bitmap;
}

