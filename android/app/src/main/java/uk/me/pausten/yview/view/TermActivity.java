package uk.me.pausten.yview.view;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import java.io.IOException;

import uk.me.pausten.yview.R;
import uk.me.pausten.yview.controller.TermRxListener;
import uk.me.pausten.yview.controller.WyTermSession;
import uk.me.pausten.yview.model.Constants;


public class TermActivity extends Activity implements TermRxListener, OnEditorActionListener {
    public static final int UPDATE_TEXTAREA = 1;
    WyTermSession wyTermSession;
    EditText cmdField;
    TextView textArea;
    Handler uiHandler;
    private Boolean exit = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_term);

        wyTermSession = new WyTermSession();
        wyTermSession.setWyTermAddress(getIntent().getStringExtra(Constants.TERM_ACTIVITY_YTERM_ADDRESS));
        wyTermSession.addTermRxListener(this);
        wyTermSession.start();

        textArea = (TextView) findViewById(R.id.textArea);
        textArea.setText("");

        cmdField = (EditText) findViewById(R.id.cmdField);
        cmdField.setFocusableInTouchMode(true);
        cmdField.requestFocus();

        //Setup handler when user enters a command to be sent out the WyTerm unit
        cmdField.setOnEditorActionListener(this);

        // Defines a Handler object that's attached to the UI thread
        uiHandler = new Handler(Looper.getMainLooper()) {

            /*
            * handleMessage() defines the operations to perform when
            * the Handler receives a new Message to process.
            */
            @Override
            public void handleMessage(Message inputMessage) {
                CharSequence charSeq = (CharSequence) inputMessage.obj;
                updateTextArea(charSeq);

            }
        };
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_term, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Called just before the activity is destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        wyTermSession.removeAllTermRxListener();
        wyTermSession.shutDown();
    }

    /*
    * @brief When data is received from the WyTerm unit this method is called to process the data.
    * @param rxBuffer      A byte array containing the data received on the socket fronm the WyTerm unit.
    * @param rxByteCount   The number of bytes received.
    */
    public void processData(byte rxBuffer[], int rxByteCount) {
        //Notify the main thread that the UI should display the data we received from the WyTerm unit.
        CharSequence charSeq = new String(rxBuffer, 0, rxByteCount);

        Message completeMessage = uiHandler.obtainMessage(TermActivity.UPDATE_TEXTAREA, charSeq);
        completeMessage.sendToTarget();

    }

    /**
     * Called when data is received from the WyTerm unit from the main thread in order to update the UI
     *
     * @param charSeq A String containing the data received from the WyTerm unit.
     */
    private void updateTextArea(CharSequence charSeq) {

        textArea.append(charSeq);

        //If we have to much text in the buffer
        if (textArea.getText().length() > Constants.MAX_TEXT_VIEW_BUFFER_SIZE) {
            //Remove the first half of the buffer in terms of lines of text
            int lineCount = textArea.getLineCount();
            int newFirstLinePos = textArea.getLayout().getLineStart(lineCount / 2);
            textArea.setText(textArea.getText().toString().substring(newFirstLinePos));
        }

        //Get the line of text at the end of the text area. Make some assumption
        //as to whether the user is entering a password. If so make the input field
        //hide the user input
        int lastLineStartPos = textArea.getLayout().getLineStart(textArea.getLineCount() - 1);
        String lastLine = textArea.getText().toString().substring(lastLineStartPos);
        if (lastLine.toLowerCase().contains(("password: "))) {
            cmdField.setTransformationMethod(new PasswordTransformationMethod());
        } else {
            cmdField.setTransformationMethod(null);
        }
        //Scroll the end of the text into view
        final ScrollView scrollview = (ScrollView) findViewById(R.id.scrollView);
        scrollview.post(new Runnable() {
            @Override
            public void run() {
                scrollview.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    /**
     * Called when the user enters a command to send out over the WyTerm unit
     *
     * @param v        The Generator of this event
     * @param actionId The ID of the event
     * @param event    The event
     * @return true as we always handle these events
     */
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        if( !wyTermSession.isConnected() ) {
            Toast.makeText(this, "Connection dropped.", Toast.LENGTH_SHORT).show();
            finish(); // finish activity
        }
        String textToSend = cmdField.getText().toString();
        try {
            wyTermSession.sendData((textToSend + "\r").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
        //Clear the command and show the keyboard again
        //The user can press back if they want the keyboard to disapear
        cmdField.setText("");
        cmdField.requestFocus();
        return true;
    }

    /**
     * \brief if back button pressed then tell user if they press back again withing 3 seconds then the
     * application will close.
     */
    @Override
    public void onBackPressed() {

        if (exit) {
            finish(); // finish activity
        } else {
            Toast.makeText(this, "Press Back again to close terminal", Toast.LENGTH_SHORT).show();
            exit = true;

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);

        }

    }
}
