/*
 * Copyright 2015. Qiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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