package info.breezes.fx.viewer;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.*;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created by Qiao on 2014/9/22.
 */
public class BigImageView extends SurfaceView implements SurfaceHolder.Callback {

    public BigImageView(Context context) {
        super(context);
        init();
    }


    public BigImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BigImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private boolean alive;
    private String path;
    private Paint paint;
    private SurfaceHolder holder;
    private RenderThread renderThread;
    private BitmapRegionDecoder regionDecoder;
    private ProgressDialog dialog;
    private Handler handler;

    private void init() {
        getHolder().addCallback(this);
        handler = new Handler(Looper.getMainLooper());
        dialog = createDialog();
    }

    public void setResource(String path) {
        this.path = path;
    }

    private ProgressDialog createDialog() {
        ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setMessage("Loading .....");
        dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        return dialog;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        this.holder = holder;
        paint = new Paint();
        paint.setColor(Color.WHITE);
        paint.setTextSize(24.0f);
        alive = true;
        renderThread = new RenderThread();
        renderThread.start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        alive = false;
        renderThread.interrupt();
        dialog.dismiss();
        regionDecoder.recycle();
    }


    private void loadImage() {
        Log.d("BigImageView", "Create RegionDecoder .");
        try {
            regionDecoder = BitmapRegionDecoder.newInstance(new FileInputStream(new File(path)).getFD(), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.d("BigImageView", "End Create .");
    }

    private static final float DOUBLE_POINT_DISTANCE = 10.0f;
    private float downX;
    private float downY;
    private Matrix mMatrix = new Matrix();
    private int touchMode = 0;
    private float oldDist = 0;
    private PointF mid = new PointF();

    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                touchMode = 1;
                downX = event.getRawX();
                downY = event.getRawY();
                mMatrix.set(matrix);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                oldDist = spacing(event);
                if (oldDist >= DOUBLE_POINT_DISTANCE) {
                    touchMode = 2;
                    midPoint(mid, event);
                    mMatrix.set(matrix);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (touchMode == 1) {
                    updateTrans(mMatrix, event.getRawX() - downX, event.getRawY() - downY);
                } else if (touchMode == 2) {
                    float newDist = spacing(event);
                    float scale = newDist / oldDist;
                    if (scale > 1.01 || scale < 0.99) {
                        updateScale(mMatrix, scale, mid.x, mid.y);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                touchMode = 0;
                break;
            case MotionEvent.ACTION_POINTER_UP:
                touchMode = 0;
                break;
        }
        return true;
    }

    // 取手势中心点
    private static void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    // 触碰两点间距离
    private static float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        if (x < 0) {
            x = -x;
        }
        float y = event.getY(0) - event.getY(1);
        if (y < 0) {
            y = -y;
        }
        return FloatMath.sqrt(x * x + y * y);
    }

    private void updateScale(Matrix src, float scale, float v, float v1) {
        matrix.reset();
        matrix.set(src);
        matrix.postScale(scale, scale, v, v1);
    }

    private void updateTrans(Matrix src, float v, float v1) {
        matrix.reset();
        matrix.set(src);
        matrix.postTranslate(v, v1);
    }

    private float normalScale = 1;
    private Matrix matrix = new Matrix();


    private boolean quality = false;

    public void quality() {
        quality = true;
    }

    class RenderThread extends Thread {
        @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
        public void run() {
            setName("BIG IMAGE VIEW RENDER THREAD");
            handler.post(new Runnable() {
                @Override
                public void run() {
                    dialog.show();
                }
            });
            //创建RegionDecoder
            loadImage();
            //内容大小
            Area viewPort = new Area(0, 0, regionDecoder.getWidth(), regionDecoder.getHeight());
            //显示区域大小
            Area visibleArea = new Area(0, 0, getWidth(), getHeight());
            //模糊背景
            Bitmap foggyBitmap = createSampleBitmap(viewPort, visibleArea);
            Bitmap qualityBitmap = null;
            Rect qualityRect = new Rect();
            initMatrix(matrix, normalScale, viewPort, visibleArea);
            dialog.dismiss();
            long i = 0;
            while (alive) {
                try {
                    long st = System.currentTimeMillis();
                    Canvas canvas = holder.lockCanvas(null);
                    if (canvas != null) {
                        int sc = canvas.save();
                        //清屏
                        canvas.drawColor(Color.BLACK);
                        //绘制模糊背景
                        canvas.drawBitmap(foggyBitmap, matrix, null);
                        RectF rect0 = visibleArea.toRectF();
                        RectF rect2 = visibleArea.toRectF();
                        matrix.mapRect(rect0);
                        RectF rectF = new RectF(0 - rect0.left, 0 - rect0.top, 0 - rect0.left + rect2.width(), 0 - rect0.top + rect2.height());

                        if (quality) {
                            qualityBitmap = createQualityBitmap(rectF, qualityRect);
                            quality = false;
                        }

                        if (qualityBitmap != null) {
                            drawQualityBitmap(canvas, qualityBitmap, qualityRect);
                        }

                        float y = 0;
//                        y = drawText(canvas, "DRC:" + i++, 0, y + 5, paint);
//                        y = drawText(canvas, "MEM:" + ComputerUnitUtils.toReadFriendly(Runtime.getRuntime().totalMemory()), 0, y + 5, paint);
//                        y = drawText(canvas, "MATRIX:" + matrix.toShortString(), 0, y + 5, paint);
//                        y = drawText(canvas, "RECT0:" + rect0.toShortString(), 0, y + 5, paint);
//                        y = drawText(canvas, "RECT1:" + rectF.toShortString(), 0, y + 5, paint);
//                        drawText(canvas, "RECT2:" + qualityRect.toShortString(), 0, y + 5, paint);
                        canvas.restoreToCount(sc);
                        holder.unlockCanvasAndPost(canvas);
                        st = System.currentTimeMillis() - st;
                        if (st < 30) {
                            Thread.sleep(30 - st);
                        }
                    } else {
                        break;
                    }
                } catch (InterruptedException e) {
                    break;
                }
            }
        }

        private void drawQualityBitmap(Canvas canvas, Bitmap bitmap, Rect qualityRect) {
            canvas.drawBitmap(bitmap, new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight()), qualityRect, null);
        }

        private Bitmap createQualityBitmap(RectF qualityRect, Rect distRect) {
            Rect rect = new Rect();
            rect.left = qualityRect.left > 0 ? (int) qualityRect.left : 0;
            rect.top = qualityRect.top > 0 ? (int) qualityRect.top : 0;
            rect.right = (int) qualityRect.right;
            rect.bottom = (int) qualityRect.bottom;
            distRect.left = qualityRect.left > 0 ? 0 : (int) -qualityRect.left;
            distRect.top = qualityRect.top > 0 ? 0 : (int) -qualityRect.top;
            distRect.right = distRect.left + rect.width();
            distRect.bottom = distRect.top + rect.height();
            return regionDecoder.decodeRegion(rect, null);
        }

        private float drawText(Canvas canvas, String s, int x, float y, Paint paint) {
            if (s == null || paint == null || canvas == null) {
                return y;
            }
            Paint.FontMetrics fm = paint.getFontMetrics();
            y = y + (float) Math.ceil(fm.descent - fm.ascent);
            canvas.drawText(s, x, y, paint);
            return y;
        }

        private void initMatrix(Matrix matrix, float scale, Area foggyRect, Area target) {
            matrix.postScale(1.0f / scale, 1.0f / scale);
            matrix.postTranslate((target.width - foggyRect.width) / 2, (target.height - foggyRect.height) / 2);
        }

        private Bitmap createSampleBitmap(Area viewPort, Area outArea) {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPurgeable = true;
            options.inInputShareable = true;
            options.inSampleSize = ImageUtility.calculateInSampleSize(viewPort.width, viewPort.height, outArea.width, outArea.height);
            normalScale = 1.0f / options.inSampleSize;
            return regionDecoder.decodeRegion(viewPort.toRect(), options);
        }


        private void recycle(Bitmap qualityBitmap) {
            if (qualityBitmap != null) {
                qualityBitmap.recycle();
            }
        }
    }

    class Area {
        public int left;
        public int top;
        public int width;
        public int height;

        public Area(int left, int top, int width, int height) {
            this.left = left;
            this.top = top;
            this.width = width;
            this.height = height;
        }

        public Rect toRect() {
            return new Rect(left, top, left + width, top + height);
        }

        public RectF toRectF() {
            return new RectF(left, top, left + width, top + height);
        }
    }
}


