package info.breezes.fxmanager.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import info.breezes.DigestUtils;
import info.breezes.fxmanager.R;
import info.breezes.fxmanager.model.MediaItem;
import info.breezes.toolkit.ui.Toast;

/**
 * single file hash code dialog
 * Created by Qiao on 2015/1/1.
 */
public class HashInfoDialog {
    private final Context context;

    public HashInfoDialog(Context context) {
        this.context = context;
    }

    private View.OnClickListener copyToClipboardListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            TextView tv = (TextView) v;
            ClipboardManager clipboardManager = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboardManager.setPrimaryClip(ClipData.newPlainText(String.valueOf(tv.getTag()), tv.getText()));
            Toast.showText(context, String.format("%s值已经复制到剪贴板", tv.getTag()));
        }
    };

    public void showHashDialog(MediaItem mediaItem) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(mediaItem.title);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = layoutInflater.inflate(R.layout.dialog_hash_info, null);
        TextView md5TextView = (TextView) view.findViewById(R.id.hash_md5);
        TextView sha1TextView = (TextView) view.findViewById(R.id.hash_sha1);
        md5TextView.setTag("MD5");
        sha1TextView.setTag("SHA1");
        md5TextView.setOnClickListener(copyToClipboardListener);
        sha1TextView.setOnClickListener(copyToClipboardListener);
        asyncComputeHash("MD5", mediaItem, md5TextView, (ProgressBar) view.findViewById(R.id.progressBar));
        asyncComputeHash("SHA1", mediaItem, sha1TextView, (ProgressBar) view.findViewById(R.id.progressBar2));
        builder.setView(view);
        builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }

    private void asyncComputeHash(final String type, final MediaItem mediaItem, final TextView textView, final ProgressBar progressBar) {
        new AsyncTask<Void, Void, Void>() {
            private String msg;
            private String hash;

            @Override
            protected void onPreExecute() {
                if (progressBar != null) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setIndeterminate(true);
                }
            }

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    switch (type) {
                        case "MD5":
                            hash = DigestUtils.md5File(mediaItem.path);
                            break;
                        case "SHA1":
                            hash = DigestUtils.sha1File(mediaItem.path);
                            break;
                        default:
                            msg = "不支持的Hash算法(" + type + ")";
                    }
                } catch (Exception exp) {
                    msg = exp.getMessage();
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                if (progressBar != null) {
                    progressBar.setVisibility(View.GONE);
                    progressBar.setIndeterminate(false);
                }
                if (!TextUtils.isEmpty(msg)) {
                    Toast.showText(context, msg);
                }
                if (hash != null) {
                    textView.setText(hash);
                }
            }
        }.execute();
    }
}
