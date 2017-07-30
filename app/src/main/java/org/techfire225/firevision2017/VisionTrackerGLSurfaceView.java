package org.techfire225.firevision2017;

import android.content.Context;
import android.hardware.camera2.CaptureRequest;
import android.util.AttributeSet;
import android.util.Log;

import org.opencv.android.BetterCamera2Renderer;
import org.opencv.android.BetterCameraGLSurfaceView;

import java.util.HashMap;


public class VisionTrackerGLSurfaceView extends BetterCameraGLSurfaceView implements BetterCameraGLSurfaceView.CameraTextureListener {

    // Physical goal height in inches to the middle of the target
    // height to bottom + (width/2)
    static final double topGoalHeight = (7*12)+4;

    // Physical camera height and pitch in inches and degrees
    static final double cameraHeight = 23.5;
    static final double cameraPitch = 41.5; // 41.5

    static final int kHeight = 480;
    static final int kWidth = 640;


    int frameCounter;
    long lastNanoTime;


    MjpgServer mjpg;
    static BetterCamera2Renderer.Settings getCameraSettings() {
        BetterCamera2Renderer.Settings settings = new BetterCamera2Renderer.Settings();
        settings.height = kHeight;
        settings.width = kWidth;
        settings.camera_settings = new HashMap<>();
        settings.camera_settings.put(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_OFF);
        settings.camera_settings.put(CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE, CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE_OFF);
        settings.camera_settings.put(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_OFF);
        settings.camera_settings.put(CaptureRequest.SENSOR_EXPOSURE_TIME, 1900000L);
        settings.camera_settings.put(CaptureRequest.LENS_FOCUS_DISTANCE, .2f);
        return settings;
    }

    public VisionTrackerGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs, getCameraSettings());
        setCameraTextureListener(this);
        mjpg = MjpgServer.getInstance();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {

    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public boolean onCameraTexture(int texIn, int texOut, int width, int height, long image_timestamp) {
        frameCounter++;
        if (frameCounter >= 30) {
            final int fps = (int) (frameCounter * 1e9 / (System.nanoTime() - lastNanoTime));
            Log.i("Vision", "onCameraTexture() FPS: " + fps);
            frameCounter = 0;
            lastNanoTime = System.nanoTime();
        }

        Native.Target target = new Native.Target();

        Native.processImage(texOut, width, height, target);

        mjpg.publish(target.imagePtr);
        if ( MainActivity.server != null ) {

            if ( target.found ) {
                long age = System.nanoTime() - image_timestamp;

                target.topCentroidX += 5;
                double topAngle = Math.toDegrees(Math.atan((target.topCentroidX - (width / 2.0)) / getFocalLengthPixels()));
                double topAngleVert = Math.toDegrees(Math.atan(((height-target.topCentroidY) - (height / 2.0)) / getFocalLengthPixels()));
                double topDistance = (topGoalHeight-cameraHeight) / Math.tan(Math.toRadians(topAngleVert+cameraPitch));

                MainActivity.server.publish(new VisionMessage(true, age, topAngle, topDistance));
            }
            else
                MainActivity.server.publish(new VisionMessage(false, 0,0,0));
        }


        return true;
    }
}
