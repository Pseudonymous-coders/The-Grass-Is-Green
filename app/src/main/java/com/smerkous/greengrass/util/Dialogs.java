package com.smerkous.greengrass.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import static com.smerkous.greengrass.MainActivity.Log;

public class Dialogs {
    public interface ClickListener {
        void onContinue();
    }

    public static void ErrorDialog(final Context context, final String message, final ClickListener clickListener) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message);
            builder.setNeutralButton("Continue", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Log("Okay pressed");
                    clickListener.onContinue();
                }
            });
            builder.show();
        } catch(Throwable ignored) {}
    }
}
