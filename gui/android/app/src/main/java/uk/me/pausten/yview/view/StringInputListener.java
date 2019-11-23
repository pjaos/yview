package uk.me.pausten.yview.view;

import android.app.AlertDialog;

public interface StringInputListener {
    public void positiveInput(String title, String prompt, String input);
    public void negativeInput(String title, String prompt);
}