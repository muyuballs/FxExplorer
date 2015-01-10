package info.breezes.fxmanager.qrcode;

import android.graphics.Bitmap;
import com.google.zxing.*;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.Hashtable;

/**
 * QrCode Decoder
 * Created by Qiao on 2014/9/5.
 */
public class QrBitmapDecoder {

    private Hashtable<DecodeHintType, String> hints;
    private QRCodeReader reader;

    public QrBitmapDecoder() {
        hints = new Hashtable<>();
        hints.put(DecodeHintType.CHARACTER_SET, "utf-8");
        reader = new QRCodeReader();
    }

    public Result decode(Bitmap bm) {
        int[] data = new int[bm.getHeight() * bm.getWidth()];
        bm.getPixels(data, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight());
        RGBLuminanceSource source = new RGBLuminanceSource(bm.getWidth(), bm.getHeight(), data);
        BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(source));
        try {
            return reader.decode(bb, hints);
        } catch (ChecksumException e) {
            e.printStackTrace();
        } catch (NotFoundException e) {
            e.printStackTrace();
        } catch (FormatException e) {
            e.printStackTrace();
        }
        return null;
    }
}