package info.breezes.fxmanager;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.google.zxing.Result;
import com.google.zxing.client.android.camera.CameraConfigurationUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import info.breezes.fxmanager.countly.CountlyActivity;
import info.breezes.fxmanager.qrcode.QrBitmapDecoder;
import info.breezes.toolkit.log.Log;
import info.breezes.toolkit.ui.LayoutViewHelper;
import info.breezes.toolkit.ui.annotation.LayoutView;


public class ScannerActivity extends CountlyActivity implements SurfaceHolder.Callback {

    public static final String SCAN_STRING_RESULT = "scan_string_result";
    public static final String SCAN_BINARY_RESULT = "scan_binary_result";
    public static final String SCAN_RESULT_FORMAT = "scan_result_format";

    @LayoutView(R.id.surfaceView)
    private SurfaceView surfaceView;
    @LayoutView(R.id.imageView2)
    private ImageView imageView2;
    private SurfaceHolder surfaceHolder;

    private Camera camera;
    private Camera.Parameters parameters;
    private boolean isSupportFlash = true;
    private boolean isSupportAutoFocus = true;
    private AutoFocusCallBack autoFocusCallback;
    private PreviewCallBack previewCallback;
    private boolean focused = false;
    private QrBitmapDecoder decoder;
    private int catchWidth;
    private int catchHeight;

    private Timer autoFocus;
    private Timer handFocus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_scan);
        LayoutViewHelper.initLayout(this);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        ViewGroup.LayoutParams lp = surfaceView.getLayoutParams();
        catchHeight = lp.height;
        catchWidth = lp.width;
        surfaceHolder.setFixedSize(catchWidth, catchHeight);
        surfaceHolder.setKeepScreenOn(true);
        decoder = new QrBitmapDecoder();
    }

    @Override
    protected void onResume() {
        super.onResume();
        camera = Camera.open();
        parameters = camera.getParameters();
        DisplayMetrics metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metric);
        if (metric.widthPixels < metric.heightPixels)// 横竖屏问题
        {
            camera.setDisplayOrientation(90);
        } else {
            camera.setDisplayOrientation(0);
        }
        CameraConfigurationUtils.setBestPreviewFPS(parameters);
        Log.d(null, "Display Metric:" + metric.widthPixels + "x" + metric.heightPixels);
        Point p = CameraConfigurationUtils.findBestPreviewSizeValue(parameters, new Point(metric.widthPixels, metric.heightPixels));
        setBestPreviewSize(p, metric, parameters);
        List<String> focusMode = parameters.getSupportedFocusModes();

        // 自动对焦优先
        if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        } else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        } else if (focusMode.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);
        } else {
            isSupportAutoFocus = false;
        }
        parameters.setPreviewFormat(ImageFormat.NV21);                                //仅支持NV21
        camera.setParameters(parameters);
        previewCallback = new PreviewCallBack();
        camera.setPreviewCallback(previewCallback);
        if (isSupportAutoFocus) {
            autoFocusCallback = new AutoFocusCallBack();
            autoFocus = new Timer();
            autoFocus.schedule(new AutoFocusTimer(), 1000, 1500);        //自动对焦相机启动5秒后开始，每5秒对焦一次
        } else {
            handFocus = new Timer();
            handFocus.schedule(new NotSupportAutoFocusTimer(), 1000, 1500);        //手动对焦相机启动3秒后开始，每3秒解析一次
        }
    }

    private void setBestPreviewSize(Point p, DisplayMetrics metric, Camera.Parameters parameters) {
        List<Camera.Size> sizes = parameters.getSupportedPreviewSizes();
        if (sizes != null && sizes.size() > 0) {
            Collections.sort(sizes, new Comparator<Camera.Size>() {
                @Override
                public int compare(Camera.Size lhs, Camera.Size rhs) {
                    return rhs.height * rhs.width - lhs.width * lhs.height;
                }
            });
            Camera.Size bestSize = sizes.get(0);
            for (Camera.Size s : sizes) {
                if (s.width < metric.widthPixels || s.height < metric.heightPixels) {
                    break;
                }
                bestSize = s;
            }
            Log.d(null, "Set Preview Size:" + bestSize.width + "x" + bestSize.height);
            ViewGroup.LayoutParams params = surfaceView.getLayoutParams();
            params.width = bestSize.height;
            params.height = bestSize.width;
            surfaceView.setLayoutParams(params);
            parameters.setPreviewSize(bestSize.width, bestSize.height);
            return;
        }
        parameters.setPreviewSize(p.x, p.y);
    }

    @Override
    protected void onPause() {
        if (autoFocus != null) {
            autoFocus.cancel();
        }
        if (handFocus != null) {
            handFocus.cancel();
        }
        if (camera != null) {
            camera.stopPreview();
            camera.release();
        }
        super.onPause();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            camera.setPreviewDisplay(holder);
            camera.startPreview();
        } catch (IOException e) {
            Log.e(null, "SurfaceCreated", e);
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }

    class AutoFocusCallBack implements Camera.AutoFocusCallback {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            focused = true;
        }
    }

    class PreviewCallBack implements Camera.PreviewCallback {
        Rect rect = new Rect(0, 0, parameters.getPreviewSize().width, parameters.getPreviewSize().height);

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (!focused) {
                return;
            }
            YuvImage img = new YuvImage(data, ImageFormat.NV21, parameters.getPreviewSize().width, parameters.getPreviewSize().height, null);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            if (img.compressToJpeg(rect, 100, byteArrayOutputStream)) {
                Bitmap bm = BitmapFactory.decodeByteArray(byteArrayOutputStream.toByteArray(), 0, byteArrayOutputStream.size());
                int borderSize = getSize(402);
                Log.d(null, "BorderSize:" + borderSize);
                final Bitmap cropBitmap = Bitmap.createBitmap(bm, (bm.getWidth() - borderSize) / 2, (bm.getHeight() - borderSize) / 2, borderSize, borderSize);
                imageView2.post(new Runnable() {
                    @Override
                    public void run() {
                        imageView2.setImageBitmap(cropBitmap);
                    }
                });
                bm.recycle();
                Result result = decoder.decode(cropBitmap);
                //cropBitmap.recycle();
                if (result != null) {
                    if (isSupportAutoFocus) {
                        autoFocus.cancel();
                    } else {
                        handFocus.cancel();
                    }
                    Intent intent = new Intent();
                    intent.putExtra(SCAN_STRING_RESULT, result.getText());
                    intent.putExtra(SCAN_BINARY_RESULT, result.getRawBytes());
                    intent.putExtra(SCAN_RESULT_FORMAT, result.getBarcodeFormat());
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            }
            camera.startPreview();
            focused = false;
        }

        private int getSize(int i) {
            return Math.round( i);
        }
    }

    class AutoFocusTimer extends TimerTask {
        @Override
        public void run() {
            Log.d("SCANNER", "autoFocus.");
            camera.autoFocus(autoFocusCallback);
        }
    }

    class NotSupportAutoFocusTimer extends TimerTask {
        @Override
        public void run() {
            focused = true;
        }
    }
}
