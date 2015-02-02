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

import android.os.AsyncTask;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import info.breezes.fxapi.MediaItem;
import info.breezes.toolkit.log.Log;

public class MediaItemUtil {
    public static int delete(boolean fall, List<MediaItem> items) {
        int deleted = 0;
        for (MediaItem item : items) {
            File file = new File(item.path);
            if (!file.delete() && !fall) {
                break;
            }
            deleted++;
        }
        return deleted;
    }

    public static boolean rename(MediaItem item, String newName) {
        File file = new File(item.path);
        return file.renameTo(new File(file.getParent() + File.separator + newName));
    }

    public static void compress(final String outFile, final OnProgressChangeListener listener, final List<MediaItem> items) {
        new AsyncTask<Void, String, Boolean>() {
            @Override
            protected void onPreExecute() {
                listener.onPreExecute();
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                listener.onPostExecute(aBoolean);
            }

            @Override
            protected void onProgressUpdate(String... values) {
                listener.onProgressChanged(values[0], 0, 0);
            }

            @Override
            protected Boolean doInBackground(Void... params) {
                FileOutputStream fos;
                try {
                    fos = new FileOutputStream(outFile);
                    ZipOutputStream zos = new ZipOutputStream(fos);
                    for (MediaItem item : items) {
                        File f = new File(item.path);
                        if (f.isFile()) {
                            zipFile("/", f, zos);
                        } else if (f.isDirectory()) {
                            zipDir("/", zos, f);
                        }
                    }
                    zos.close();
                    return true;
                } catch (Exception e) {
                    File file = new File(outFile);
                    if (file.exists()) {
                        //noinspection ResultOfMethodCallIgnored
                        file.delete();
                    }
                    Log.e(null, e.getMessage(), e);
                }
                return false;
            }

            private void zipFile(String path, File f, ZipOutputStream zos) throws IOException {
                publishProgress(f.getName());
                ZipEntry entry = new ZipEntry(path + f.getName());
                zos.putNextEntry(entry);
                copyToZip(f, zos);
                zos.closeEntry();
            }

            private void zipDir(String s, ZipOutputStream zos, File root) throws IOException {
                publishProgress(root.getName());
                File[] files = root.listFiles();
                String cDir = s + root.getName() + "/";
                for (File f : files) {
                    if (f.isFile()) {
                        zipFile(cDir, f, zos);
                    } else if (f.isDirectory()) {
                        zipDir(cDir, zos, f);
                    }
                }
            }

            private void copyToZip(File f, ZipOutputStream zos) throws IOException {
                FileInputStream inputStream = new FileInputStream(f);
                try {
                    byte[] buf = new byte[1 << 20];
                    int c;
                    while ((c = inputStream.read(buf)) != -1) {
                        if (c > 0) {
                            zos.write(buf, 0, c);
                        }
                    }
                } finally {
                    safeClose(inputStream);
                }
            }

            private void safeClose(Closeable closeable) {
                try {
                    if (closeable != null) {
                        closeable.close();
                    }
                } catch (Exception exp) {
                    Log.w(null, exp.getMessage(), exp);
                }
            }
        }.execute();
    }

    public interface OnProgressChangeListener {
        public void onPreExecute();

        public void onProgressChanged(String file, long max, long current);

        public void onPostExecute(boolean success);
    }
}

