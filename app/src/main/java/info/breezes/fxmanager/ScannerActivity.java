package info.breezes.fxmanager;

import android.content.Context;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.IOException;

import info.breezes.fxmanager.countly.CountlyActivity;
import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.ui.LayoutViewHelper;
import info.breezes.toolkit.ui.Toast;
import info.breezes.toolkit.ui.annotation.LayoutView;


public class ScannerActivity extends CountlyActivity implements Camera.AutoFocusCallback, SurfaceHolder.Callback {

    @LayoutView(R.id.surfaceView)
    private SurfaceView surfaceView;
    private Camera camera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);
        LayoutViewHelper.initLayout(this);
        surfaceView.getHolder().addCallback(this);
    }

    private void openCamera() {
        if (Camera.getNumberOfCameras() > 0) {
            camera = Camera.open();
            Camera.Parameters parameters = camera.getParameters();
            CameraConfigurationUtils.setBestExposure(parameters, false);
            CameraConfigurationUtils.setBestPreviewFPS(parameters);
            CameraConfigurationUtils.setFocus(parameters, true, false, false);
            CameraConfigurationUtils.setFocusArea(parameters);
            Point screenSize = new Point(getResources().getDisplayMetrics().heightPixels, getResources().getDisplayMetrics().widthPixels);
            Point point = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, screenSize);
            parameters.setPreviewSize(point.x, point.y);
            camera.setParameters(parameters);
            camera.setDisplayOrientation(90);
            camera.startPreview();
            camera.autoFocus(this);
        } else {
            Toast.showText(this, "没有找到照相机");
        }
    }

    private void releaseCamera() {
        if (camera != null) {
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    @Override
    public void onAutoFocus(boolean success, final Camera camera) {
        Log.d(null, "focus:" + success);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
            camera.autoFocus(this);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        if (camera != null) {
            camera.stopPreview();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        openCamera();
    }

    @Override
    protected void onPause() {
        releaseCamera();
        super.onPause();
    }

}
