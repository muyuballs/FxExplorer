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