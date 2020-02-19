package uk.me.pausten.yview.view;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;

import android.app.Activity;
import android.text.InputType;
import android.widget.Toast;
import android.widget.EditText;

import uk.me.pausten.yview.R;
import uk.me.pausten.yview.model.Constants;

/**
 * @brief A helper class for displaying dialogs.
 */
public class Dialogs {

    /**
     * @brief This class allows toast messages to be displayed in the UI thread by passing the attributes
     *        required.
     */
    private static class Toaster implements Runnable {
        private static Toast toastMessage;
        private Activity activity;
        private String message;

        public Toaster(Activity activity, String message) {
            this.activity=activity;
            this.message = message;
        }

        @Override
        public void run() {
            //Remove any previous message that maybe being displayed to the user.
            if( toastMessage != null ) {
                toastMessage.cancel();
            }
            toastMessage = Toast.makeText(activity.getApplicationContext(), message, Toast.LENGTH_SHORT);
            toastMessage.show();
        }
    }

    public static AlertDialog OKDialog(Activity activity, String title, String message, OnClickListener onClickListener ) {

        AlertDialog builder = new AlertDialog.Builder(activity)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK", onClickListener)
                .show();

        return builder;
    }

    public static AlertDialog OptionsDialog(Activity activity, String title, String options[], OnClickListener onClickListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title).setItems(options, onClickListener);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

        return alertDialog;
    }


    public static AlertDialog showInputDialog(Activity activity, String title, String prompt, String currentValue, boolean password, StringInputListener stringInputListener) {
        final StringInputListener sil = stringInputListener;
        final String _title = title;
        final String _prompt = prompt;
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(title);
        builder.setMessage(prompt);
        final EditText input = new EditText(activity);
        if( password ) {
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        }
        if( currentValue != null ) {
            input.setText(currentValue);
        }
        builder.setView(input);
        builder.setPositiveButton("OK", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                sil.positiveInput(_title, _prompt, input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                sil.negativeInput(_title, _prompt);
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        return alertDialog;
    }

    /**
     * @brief Allow a brieft Toast message to be displayed to the user.
     * @param activity The activity that gives context to the message.
     *                 If this is null then no message is displayed.
     * @param message The text of the message to be displayed.
     */
    public static void Toast(Activity activity, String message) {
        if( activity != null ) {
            activity.runOnUiThread( new Toaster(activity, message) );
        }
    }
}
