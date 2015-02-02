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

package info.breezes.fxmanager.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;

import info.breezes.ComputerUnitUtils;
import info.breezes.fxapi.MediaItem;
import info.breezes.fxapi.MediaProvider;
import info.breezes.fxapi.countly.CountlyEvent;
import info.breezes.fxapi.countly.CountlyUtils;
import info.breezes.fxmanager.R;
import info.breezes.toolkit.ui.Toast;

/**
 * file detail information dialog
 * Created by Qiao on 2015/1/1.
 */
public class FileInfoDialog {
    public static void showSingleFileInfoDialog(final Context context, final MediaItem mediaItem, MediaProvider mediaProvider, boolean showHash) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mediaItem.title);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = layoutInflater.inflate(R.layout.dialog_file_info, null);
        asyncLoadFileIcon(context, mediaProvider, (ImageView) view.findViewById(R.id.icon), mediaItem);
        ((TextView) view.findViewById(R.id.path)).setText(mediaItem.path);
        String mime = mediaProvider.getMimeType(mediaItem);
        if (mime != null && mime.startsWith("image/")) {
            Rect size = mediaProvider.getImageSize(mediaItem);
            ((TextView) view.findViewById(R.id.type)).setText(String.format("%s (%d x %d)", mime, size.width(), size.height()));
        } else {
            ((TextView) view.findViewById(R.id.type)).setText(TextUtils.isEmpty(mime) ? context.getString(R.string.unkown) : mime);
        }
        ((TextView) view.findViewById(R.id.size)).setText(ComputerUnitUtils.toReadFriendly(mediaItem.length));
        ((TextView) view.findViewById(R.id.modifyTime)).setText(DateUtils.formatDateTime(context, mediaItem.lastModify, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE));

        builder.setView(view);
        if (showHash) {
            builder.setNeutralButton(context.getString(R.string.show_hash_code), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CountlyUtils.addEvent(CountlyEvent.SHOW_HASH, "");
                    new HashInfoDialog(context).showHashDialog(mediaItem);
                }
            });
        }
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public static void showSingleFolderInfoDialog(final Context context, final MediaItem mediaItem, MediaProvider mediaProvider) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mediaItem.title);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = layoutInflater.inflate(R.layout.dialog_folder_info, null);
        asyncLoadFileIcon(context, mediaProvider, (ImageView) view.findViewById(R.id.icon), mediaItem);
        ((TextView) view.findViewById(R.id.path)).setText(mediaItem.path);
        asyncComputeFileSize(context, mediaItem, (TextView) view.findViewById(R.id.type), (TextView) view.findViewById(R.id.size), (ProgressBar) view.findViewById(R.id.progressBar3));
        ((TextView) view.findViewById(R.id.modifyTime)).setText(DateUtils.formatDateTime(context, mediaItem.lastModify, DateUtils.FORMAT_SHOW_TIME | DateUtils.FORMAT_SHOW_DATE));

        builder.setView(view);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private static void asyncComputeFileSize(final Context context, final MediaItem mediaItem, final TextView detailTextView, final TextView sizeTextView, final ProgressBar progressBar) {
        new AsyncTask<Void, Void, Void>() {
            private String size;
            private String msg;
            private long fileCount;
            private long folderCount;

            @Override
            protected void onPreExecute() {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                }
            }

            @Override
            protected void onProgressUpdate(Void... values) {
                detailTextView.setText(String.format(context.getString(R.string.dialog_msg_folder_detial), fileCount, folderCount));
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    File root = new File(mediaItem.path);
                    size = ComputerUnitUtils.toReadFriendly(computeSize(root));
                } catch (Exception exp) {
                    msg = exp.getMessage();
                }
                return null;
            }

            private long computeSize(File root) {
                long size = 0;
                if (root.isFile()) {
                    fileCount++;
                    size = root.length();
                } else {
                    folderCount++;
                    File fs[] = root.listFiles();
                    for (File f : fs) {
                        size += computeSize(f);
                    }
                }
                publishProgress();
                return size;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (!TextUtils.isEmpty(msg)) {
                    Toast.showText(context, msg);
                }
                detailTextView.setText(String.format(context.getString(R.string.dialog_msg_folder_detial), fileCount, folderCount));
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                    progressBar.setIndeterminate(false);
                }
                if (sizeTextView != null) {
                    sizeTextView.setText(size);
                }
            }

        }.execute();
    }

    private static void asyncLoadFileIcon(final Context context, final MediaProvider mediaProvider, final ImageView imageView, final MediaItem mediaItem) {
        new AsyncTask<Void, Void, Void>() {
            private Drawable icon;
            private String msg;

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    icon = mediaProvider.loadMediaIcon(mediaItem);
                } catch (Exception exp) {
                    msg = exp.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (!TextUtils.isEmpty(msg)) {
                    Toast.showText(context, msg);
                }
                if (icon != null) {
                    imageView.setImageDrawable(icon);
                }
            }
        }.execute();
    }
}
