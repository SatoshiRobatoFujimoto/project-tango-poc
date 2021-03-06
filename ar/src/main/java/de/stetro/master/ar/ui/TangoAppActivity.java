package de.stetro.master.ar.ui;


import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;

import com.google.atap.tangoservice.Tango;
import com.google.atap.tangoservice.TangoCameraIntrinsics;
import com.google.atap.tangoservice.TangoConfig;
import com.google.atap.tangoservice.TangoCoordinateFramePair;
import com.google.atap.tangoservice.TangoEvent;
import com.google.atap.tangoservice.TangoPoseData;
import com.google.atap.tangoservice.TangoXyzIjData;
import com.projecttango.rajawali.ar.TangoRajawaliView;

import org.rajawali3d.util.RajLog;

import java.util.ArrayList;

import de.greenrobot.event.EventBus;
import de.stetro.master.ar.rendering.event.SceneUpdateEvent;
import de.stetro.master.ar.rendering.interactive.InteractionGame;
import de.stetro.master.ar.rendering.td.TDGame;
import de.stetro.master.ar.util.PointCloudManager;

public abstract class TangoAppActivity extends BaseActivity implements View.OnTouchListener {
    protected TangoRajawaliView glView;
    protected Tango tango;
    protected boolean isPermissionGranted;
    protected boolean isConnected;
    protected TangoConfig config;
    protected PointCloudManager pointCloudManager;
    protected TDGame renderer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        RajLog.setDebugEnabled(true);

        glView = new TangoRajawaliView(this);
        glView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);

        tango = new Tango(this);

        config = tango.getConfig(TangoConfig.CONFIG_TYPE_DEFAULT);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_LOWLATENCYIMUINTEGRATION, true);
        config.putBoolean(TangoConfig.KEY_BOOLEAN_DEPTH, true);

        int maxDepthPoints = config.getInt("max_point_cloud_elements");
        pointCloudManager = new PointCloudManager(maxDepthPoints);
        renderer = new TDGame(this, pointCloudManager);
        glView.setEGLContextClientVersion(2);
        glView.setSurfaceRenderer(renderer);
        glView.setOnTouchListener(this);
    }

    protected void startAugmentedReality() {
        if (!isConnected) {
            glView.connectToTangoCamera(tango, TangoCameraIntrinsics.TANGO_CAMERA_COLOR);

            tango.connect(config);
            isConnected = true;
            final ArrayList<TangoCoordinateFramePair> framePairs = new ArrayList<>();
            final TangoCoordinateFramePair frames_of_reference = new TangoCoordinateFramePair();
            frames_of_reference.baseFrame = TangoPoseData.COORDINATE_FRAME_START_OF_SERVICE;
            frames_of_reference.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;

            tango.connectListener(framePairs, new Tango.OnTangoUpdateListener() {
                @Override
                public void onPoseAvailable(TangoPoseData pose) {
                }

                @Override
                public void onFrameAvailable(int cameraId) {
                    if (cameraId == TangoCameraIntrinsics.TANGO_CAMERA_COLOR) {
                        glView.onFrameAvailable();
                    }
                }

                @Override
                public void onXyzIjAvailable(TangoXyzIjData xyzIj) {
                    synchronized (PointCloudManager.mPointCloudLock) {
                        SceneUpdateEvent e = new SceneUpdateEvent();
                        e.setPointCloundPointsCount(xyzIj.xyzCount);
                        e.setOctTreePointCloudPointsCount(renderer.getOctTreePointCloudPointsCount());
                        EventBus.getDefault().post(e);
                        TangoPoseData pointCloudPose = tango.getPoseAtTime(xyzIj.timestamp, frames_of_reference);
                        pointCloudManager.updateCallbackBufferAndSwap(xyzIj.xyz, xyzIj.xyzCount, xyzIj.timestamp, pointCloudPose);
                    }
                }

                @Override
                public void onTangoEvent(TangoEvent event) {
                }
            });
            setupExtrinsics();
        }
    }

    private void setupExtrinsics() {
        // Create Camera to IMU Transform
        TangoCoordinateFramePair framePair = new TangoCoordinateFramePair();
        framePair.baseFrame = TangoPoseData.COORDINATE_FRAME_IMU;
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_COLOR;
        TangoPoseData imuTrgbPose = tango.getPoseAtTime(0.0, framePair);

        // Create Device to IMU Transform
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_DEVICE;
        TangoPoseData imuTdevicePose = tango.getPoseAtTime(0.0, framePair);

        // Create Depth camera to IMU Transform
        framePair.targetFrame = TangoPoseData.COORDINATE_FRAME_CAMERA_DEPTH;
        TangoPoseData imuTdepthPose = tango.getPoseAtTime(0.0, framePair);

        renderer.setupExtrinsics(imuTdevicePose, imuTrgbPose, imuTdepthPose);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (isConnected) {
            glView.disconnectCamera();
            tango.disconnect();
            isConnected = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isConnected && isPermissionGranted) {
            startAugmentedReality();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        renderer.onTouchEvent(event);
        return super.onTouchEvent(event);
    }

}
