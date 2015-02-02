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

package info.breezes.fxmanager;

import android.content.Context;
import android.os.*;
import android.text.TextUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Process;

/**
 * execute linux shell cmd
 * Created by Qiao on 2015/1/1.
 */
public class ShellUtil {
    public static byte[] exec(String workDir, String[] evnp, String... cmd) {
        try {
            Process process = Runtime.getRuntime().exec(cmd, evnp, TextUtils.isEmpty(workDir) ? null : new File(workDir));
            InputStream inputStream = process.getInputStream();
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buf = new byte[8096];
            int c = inputStream.read(buf);
            while (c != -1) {
                byteArrayOutputStream.write(buf, 0, c);
                c = inputStream.read(buf);
            }
            return byteArrayOutputStream.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        }
    }
}
