/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package io.popcorntime.androidtv;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Point;
import android.media.MediaMetadataRetriever;
import android.os.Build;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A collection of utility methods, all static.
 */
public class Utils {
    private static final String TAG = "Utils";
    public interface MediaDimensions {
        double MEDIA_HEIGHT = 0.95;
        double MEDIA_WIDTH = 0.95;
        double MEDIA_TOP_MARGIN = 0.025;
        double MEDIA_RIGHT_MARGIN = 0.025;
        double MEDIA_BOTTOM_MARGIN = 0.025;
        double MEDIA_LEFT_MARGIN = 0.025;
    }

    /*
     * Making sure public utility methods remain static
     */
    private Utils() {
    }

    /**
     * Returns the screen/display size
     */
    public static Point getDisplaySize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;
        return size;
    }

    /**
     * Shows a (long) toast
     */
    public static void showToast(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Shows a (long) toast.
     */
    public static void showToast(Context context, int resourceId) {
        Toast.makeText(context, context.getString(resourceId), Toast.LENGTH_LONG).show();
    }

    public static int convertDpToPixel(Context ctx, int dp) {
        float density = ctx.getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }


    /**
     * Example for handling resizing content for overscan.  Typically you won't need to resize
     * when using the Leanback support library.
     */
    public void overScan(Activity activity, VideoView videoView) {
        DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int w = (int) (metrics.widthPixels * MediaDimensions.MEDIA_WIDTH);
        int h = (int) (metrics.heightPixels * MediaDimensions.MEDIA_HEIGHT);
        int marginLeft = (int) (metrics.widthPixels * MediaDimensions.MEDIA_LEFT_MARGIN);
        int marginTop = (int) (metrics.heightPixels * MediaDimensions.MEDIA_TOP_MARGIN);
        int marginRight = (int) (metrics.widthPixels * MediaDimensions.MEDIA_RIGHT_MARGIN);
        int marginBottom = (int) (metrics.heightPixels * MediaDimensions.MEDIA_BOTTOM_MARGIN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        videoView.setLayoutParams(lp);
    }


    public static long getDuration(String videoUrl) {
        Pattern google = Pattern.compile("googlevideo.com");
        Matcher m = google.matcher(videoUrl);
        if (m.find()) {
            Pattern dur = Pattern.compile("(?<=dur=)([0-9]+)");
            Matcher m2 = dur.matcher(videoUrl);
            if (m2.find()) {
                String sDuration = m2.group(0);
                Double dDuration = Double.parseDouble(sDuration) * 1000;
                Long lDuration = dDuration.longValue();
                return lDuration;
            }

        }

        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                mmr.setDataSource(videoUrl);//, new HashMap<String, String>());
            } catch (Exception e) {
                Log.e(TAG, "Can't get media duration of url " + videoUrl);
//                System.out.println(e);
//                System.out.println(videoUrl);
            }

        } else {
            mmr.setDataSource(videoUrl);
        }
        return Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
    }
    public static String getMimeType(String url)
    {
        String extension = url.substring(url.lastIndexOf("."));
        String mimeTypeMap = MimeTypeMap.getFileExtensionFromUrl(extension);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(mimeTypeMap);
        return mimeType;
    }
    public static URI getImageURI(String imageUrl) {
        try {
            URI imageURI= new URI(imageUrl);
            return imageURI;
        } catch (URISyntaxException e) {
            Log.e(TAG, "Can't get image URI for " + imageUrl +".");
            return null;
        }

    }

    public static void srt2vtt(String srtFilePath) {
        if (srtFilePath == null || srtFilePath == "") {
            Log.e(TAG, "Invalid srt file path.");
            return;
        }

        String inputPath = srtFilePath;
        String outputPath;
        outputPath = inputPath.replaceFirst(".srt$", ".vtt");
        System.out.println(outputPath);

        FileInputStream in = null;
        FileOutputStream out = null;
        BufferedReader reader = null;
        BufferedWriter writer = null;

        try {
            in = new FileInputStream(inputPath);
            out = new FileOutputStream(outputPath);
            reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            out.write("WEBVTT".getBytes());
            out.write("\r\n".getBytes());
            out.write("\r\n".getBytes());

            String lineIn = reader.readLine();
            String lineOut;
            while(lineIn != null){

                // remove cue marker
                if (lineIn.matches("^\\d+$")) {
                    lineOut = lineIn;
                }
                // replace , with . in front of mils
                else if (lineIn.matches("^[\\d\\s:,]+-->[\\d\\s:,]+$"))
                    lineOut = lineIn.replaceAll(",", ".");
                else
                    lineOut = lineIn;

                byte[] b = lineOut.getBytes(StandardCharsets.UTF_8);
                out.write(b);
                out.write("\r\n".getBytes());
//                writer.append(lineOut).append("\r\n");

                lineIn = reader.readLine();
            }



        } catch(Exception e) {

        } finally {
            try {
                if (reader != null)
                    reader.close();
                in.close();
                out.close();
            } catch (IOException ex) {
//                Logger.getLogger(BufferedReaderExample.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }
}
