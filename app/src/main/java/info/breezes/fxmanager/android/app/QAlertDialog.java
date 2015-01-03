package info.breezes.fxmanager.android.app;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;

public class QAlertDialog {

    static class QButtonHandler extends Handler {
        private static final int MSG_DISMISS_DIALOG = 1;
        private final boolean autoDismiss;

        private WeakReference<DialogInterface> mDialog;

        public QButtonHandler(DialogInterface dialog,boolean autoDismiss) {
            mDialog = new WeakReference<DialogInterface>(dialog);
            this.autoDismiss=autoDismiss;
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DialogInterface.BUTTON_POSITIVE:
                case DialogInterface.BUTTON_NEGATIVE:
                case DialogInterface.BUTTON_NEUTRAL:
                    ((DialogInterface.OnClickListener) msg.obj).onClick(mDialog.get(), msg.what);
                    break;

                case MSG_DISMISS_DIALOG:
                    if(autoDismiss) {
                        ((DialogInterface) msg.obj).dismiss();
                    }
            }
        }
    }

   public static void setAutoDismiss(AlertDialog dialog,boolean autoDismiss){
       try {
           Field mAlert=AlertDialog.class.getDeclaredField("mAlert");
           mAlert.setAccessible(true);
           Field mHandler=mAlert.getType().getDeclaredField("mHandler");
           mHandler.setAccessible(true);
           mHandler.set(mAlert.get(dialog),new QButtonHandler(dialog,autoDismiss));
       } catch (NoSuchFieldException e) {
           e.printStackTrace();
       } catch (IllegalAccessException e) {
           e.printStackTrace();
       }
   }

}