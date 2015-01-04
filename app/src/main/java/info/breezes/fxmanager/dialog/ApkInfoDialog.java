package info.breezes.fxmanager.dialog;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;

import info.breezes.ComputerUnitUtils;
import info.breezes.fxmanager.R;
import info.breezes.fxmanager.model.MediaItem;

/**
 * apk detail information dialog
 * Created by Qiao on 2015/1/1.
 */
public class ApkInfoDialog {
    public static void showApkInfoDialog(final Context context, final MediaItem item) {
        PackageManager pm = context.getPackageManager();
        PackageInfo info = pm.getPackageArchiveInfo(item.path, 0);
        if (info == null) {
            return;
        }
        ApplicationInfo appInfo = info.applicationInfo;
        appInfo.sourceDir = item.path;
        appInfo.publicSourceDir = item.path;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(item.title);
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        @SuppressLint("InflateParams")
        View view = layoutInflater.inflate(R.layout.dialog_apk_info, null);
        ((ImageView) view.findViewById(R.id.icon)).setImageDrawable(pm.getApplicationIcon(appInfo));
        ((TextView) view.findViewById(R.id.title)).setText(pm.getApplicationLabel(appInfo));
        ((TextView) view.findViewById(R.id.version)).setText(info.versionName);
        ((TextView) view.findViewById(R.id.size)).setText(ComputerUnitUtils.toReadFriendly(item.length));
        ((TextView) view.findViewById(R.id.packageName)).setText(info.packageName);
        try {
            PackageInfo pInfo = pm.getPackageInfo(info.packageName, 0);
            ((TextView) view.findViewById(R.id.installVersion)).setText(String.format("已安装(%s)", pInfo.versionName));
        } catch (PackageManager.NameNotFoundException e) {
            ((TextView) view.findViewById(R.id.installVersion)).setText("未安装");
        }
        builder.setView(view);
        builder.setPositiveButton("安装", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setAction(android.content.Intent.ACTION_VIEW);
                Uri uri = Uri.fromFile(new File(item.path));
                intent.setDataAndType(uri, MimeTypeMap.getSingleton().getMimeTypeFromExtension("apk"));
                context.startActivity(intent);
            }
        });
        builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
