package com.conreo.couchytv;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.widget.Button;

/** Consistent brand surface for compact remote-friendly choice dialogs. */
public final class OwnDialogBuilder extends AlertDialog.Builder {
    public OwnDialogBuilder(Context context) {
        super(context);
    }

    @Override
    public AlertDialog create() {
        AlertDialog dialog = super.create();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new OwnBackdrop());
        return dialog;
    }

    @Override
    public AlertDialog show() {
        AlertDialog dialog = super.show();
        if (dialog.getWindow() != null) dialog.getWindow().setBackgroundDrawable(new OwnBackdrop());
        for (int id :
                new int[] {
                    AlertDialog.BUTTON_POSITIVE,
                    AlertDialog.BUTTON_NEGATIVE,
                    AlertDialog.BUTTON_NEUTRAL
                }) {
            Button b = dialog.getButton(id);
            if (b != null) {
                b.setTextColor(0xFF8DD5FF);
                b.setAllCaps(false);
            }
        }
        return dialog;
    }
}
