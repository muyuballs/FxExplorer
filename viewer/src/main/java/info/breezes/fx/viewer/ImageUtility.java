package info.breezes.fx.viewer;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.NinePatchDrawable;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

public class ImageUtility {
    public static Bitmap decodeFile(File f) {
        try {
            //decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(new FileInputStream(f), null, o);
            //Find the correct scale value. It should be the power of 2.
            final int REQUIRED_SIZE = 70;

            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if (width_tmp / 2 < REQUIRED_SIZE || height_tmp / 2 < REQUIRED_SIZE)
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale++;
            }

            //decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            return BitmapFactory.decodeStream(new FileInputStream(f), null, o2);
        } catch (FileNotFoundException e) {
            Log.e("ImageUtility", "Failed to decode file", e);
        }
        return null;
    }

    public static Bitmap getImageWithFilePathAndSize(String path, int width, int height) {
        if (TextUtils.isEmpty(path)) {
            return null;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(path, options);
        options.inSampleSize = calculateInSampleSize(options, width, height);
        Log.d("ImageUtility", "SampleSize:" + options.inSampleSize);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(path, options);
    }

    public static Bitmap getImageWithResourceAndSize(Resources resources, int resourceId, int width, int height) {
        if (resourceId <= 0) {
            return null;
        }
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(resources, resourceId, options);
        options.inSampleSize = calculateInSampleSize70Precents(options, width, height);
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(resources, resourceId, options);
    }

    public static Bitmap cutCircleBitmap(Bitmap bitmap) {
        return getRoundedCornerBitmap(bitmap, 0);
    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap, int pixels) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(output);
        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        paint.setAntiAlias(true);
        paint.setColor(color);
        canvas.drawARGB(0, 0, 0, 0);
        if (pixels != 0) {
            final RectF rectF = new RectF(rect);
            final float roundPx = pixels;
            canvas.drawRoundRect(rectF, roundPx, roundPx, paint);
        } else {
            canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        }
        paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);
        return output;
    }

    public static Bitmap getNinepatchBitmap(int id, int x, int y, Context context) {
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        byte[] chunk = bitmap.getNinePatchChunk();
        NinePatchDrawable np_drawable = new NinePatchDrawable(context.getResources(), bitmap, chunk, new Rect(), null);
        np_drawable.setBounds(0, 0, x, y);
        Bitmap output_bitmap = Bitmap.createBitmap(x, y, Config.ARGB_8888);
        Canvas canvas = new Canvas(output_bitmap);
        np_drawable.draw(canvas);
        return output_bitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        int width_tmp = options.outWidth, height_tmp = options.outHeight;
        return Math.max(1, Math.max(width_tmp / reqWidth, height_tmp / reqHeight));
    }

    private static int calculateInSampleSize70Precents(BitmapFactory.Options o, int reqWidth, int reqHeight) {
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        return Math.max(1, Math.max(width_tmp / reqWidth, height_tmp / reqHeight));
    }

    public static int calculateInSampleSize(int outWidth, int outHeight, int reqWidth, int reqHeight) {
        return Math.max(1, Math.max(outWidth / reqWidth, outHeight / reqHeight));
    }
}
