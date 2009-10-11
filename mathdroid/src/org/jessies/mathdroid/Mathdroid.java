package org.jessies.mathdroid;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.util.Log;
import android.view.*;
import android.widget.*;
import java.util.HashMap;
import org.jessies.calc.Calculator;
import org.jessies.calc.CalculatorError;
import org.jessies.calc.CalculatorPlotData;
import org.jessies.calc.CalculatorPlotter;
import org.jessies.calc.UnitsConverter;

public class Mathdroid extends Activity implements CalculatorPlotter, TextView.OnEditorActionListener, View.OnClickListener {
    private static final String TAG = "Mathdroid";
    
    // Constants identifying the options menu items.
    private static final int OPTIONS_MENU_CLEAR = 0;
    private static final int OPTIONS_MENU_HELP  = 1;
    
    // Constants for the transcript context menu items.
    private static final int CONTEXT_MENU_COPY_LAST = 0;
    private static final int CONTEXT_MENU_COPY_ALL  = 1;
    
    // Constants identifying dialogs.
    private static final int DIALOG_PLOT = 0;
    
    private Calculator calculator;
    
    private CalculatorPlotData plotData;
    
    private final HashMap<Integer, String> buttonMap = new HashMap<Integer, String>();
    
    // Called when the activity is first created.
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        calculator = new Calculator();
        calculator.setPlotter(this);
        
        setContentView(R.layout.main);
        
        final EditText queryView = (EditText) findViewById(R.id.q);
        queryView.setOnEditorActionListener(this);
        
        initButtonMap();
        
        initButtonClickListener(R.id.clear);
        initButtonClickListener(R.id.del);
        initButtonClickListener(R.id.exe);
        initButtonClickListener(R.id.less);
        initButtonClickListener(R.id.more);
        
        for (int id : buttonMap.keySet()) {
            initButtonClickListener(id);
        }
        
        registerForContextMenu(transcriptView());
        
        try {
            loadState();
        } catch (Exception ex) {
            // This shouldn't happen, but two users have claimed that Mathdroid "force closes" on start.
            // Something going wrong in loadState seems the least unlikely culprit.
            // Without something like this, it's impossible to recover from bad state.
            ex.printStackTrace();
        }
    }
    
    private void initButtonClickListener(int id) {
        // Not all buttons will be present in all layouts.
        final View button = findViewById(id);
        if (button != null) {
            button.setOnClickListener(this);
        }
    }
    
    private void initButtonMap() {
        buttonMap.put(R.id.acos,   "acos()");
        buttonMap.put(R.id.ans,    "Ans");
        buttonMap.put(R.id.asin,   "asin()");
        buttonMap.put(R.id.atan,   "atan()");
        buttonMap.put(R.id.close,  ")");
        buttonMap.put(R.id.comma,  ",");
        buttonMap.put(R.id.cos,    "cos()");
        buttonMap.put(R.id.digit0, "0");
        buttonMap.put(R.id.digit1, "1");
        buttonMap.put(R.id.digit2, "2");
        buttonMap.put(R.id.digit3, "3");
        buttonMap.put(R.id.digit4, "4");
        buttonMap.put(R.id.digit5, "5");
        buttonMap.put(R.id.digit6, "6");
        buttonMap.put(R.id.digit7, "7");
        buttonMap.put(R.id.digit8, "8");
        buttonMap.put(R.id.digit9, "9");
        buttonMap.put(R.id.divide, "\u00f7");
        buttonMap.put(R.id.dot,    ".");
        buttonMap.put(R.id.e,      "e");
        buttonMap.put(R.id.eng,    "E");
        buttonMap.put(R.id.log10,  "Log10()");
        buttonMap.put(R.id.logE,   "LogE()");
        buttonMap.put(R.id.log,    "Log()");
        buttonMap.put(R.id.minus,  "-");
        buttonMap.put(R.id.open,   "(");
        buttonMap.put(R.id.pi,     "\u03c0");
        buttonMap.put(R.id.pling,  "!");
        buttonMap.put(R.id.plus,   "+");
        buttonMap.put(R.id.pow,    "^");
        buttonMap.put(R.id.sin,    "sin()");
        buttonMap.put(R.id.sqrt,   "\u221a");
        buttonMap.put(R.id.tan,    "tan()");
        buttonMap.put(R.id.times,  "\u00d7");
        buttonMap.put(R.id.x,      "x");
    }
    
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, OPTIONS_MENU_CLEAR, 0, "Clear").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        menu.add(0, OPTIONS_MENU_HELP,  0, "Help").setIcon(android.R.drawable.ic_menu_help);
        return super.onCreateOptionsMenu(menu);
    }
    
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case OPTIONS_MENU_CLEAR:
            clear((EditText) findViewById(R.id.q));
            return true;
        case OPTIONS_MENU_HELP:
            showHelp();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }
    
    @Override protected Dialog onCreateDialog(int id) {
        switch (id) {
        case DIALOG_PLOT:
            return createPlotDialog();
        default:
            return null;
        }
    }
    
    @Override protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case DIALOG_PLOT:
            final PlotView plotView = (PlotView) dialog.findViewById(R.id.plot);
            plotView.preparePlot(calculator, plotData);
            //plotData = null;
            return;
        default:
            return;
        }
    }
    
    private Dialog createPlotDialog() {
        final LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final View layout = inflater.inflate(R.layout.plot, (ViewGroup) findViewById(R.id.plot));
        
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        builder.setCancelable(true);
        
        final Dialog dialog = builder.create();
        return dialog;
    }
    
    @Override public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        if (transcriptView().length() == 0) {
            // If there's no transcript, there's nothing to copy, so no reason to show a menu.
            return;
        }
        
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle("History");
        menu.add(0, CONTEXT_MENU_COPY_LAST, 0, "Copy last");
        menu.add(0, CONTEXT_MENU_COPY_ALL,  0, "Copy all");
    }
    
    public boolean onContextItemSelected(MenuItem item) {
        final int id = item.getItemId();
        switch (id) {
        case CONTEXT_MENU_COPY_LAST:
        case CONTEXT_MENU_COPY_ALL:
            return copyHistoryToClipboard(id);
        default:
            return super.onContextItemSelected(item);
        }
    }
    
    private boolean copyHistoryToClipboard(int id) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        // We work directly on the text history.
        // This would all be simpler if we kept some kind of structured history.
        final Editable transcript = transcriptView().getEditableText();
        int start = 0;
        int end = transcript.length();
        
        if (end == 0) {
            
        }
        
        if (id == CONTEXT_MENU_COPY_LAST) {
            // Output is of the form:
            // "sin(pi/2)\n"
            // " = 1" // No trailing newline until the next history entry appears!
            // So we need to go back two newlines...
            start = TextUtils.lastIndexOf(transcript, '\n', end);
            if (start == -1) {
                return true;
            }
            start = TextUtils.lastIndexOf(transcript, '\n', start - 1);
            if (start == -1) {
                start = 0; // This is the first entry, so there's no newline before it.
            } else {
                // We don't actually want to copy the newline.
                ++start;
            }
        }
        clipboard.setText(transcript.subSequence(start, end));
        return true;
    }
    
    @Override protected void onPause() {
        super.onPause();
        saveState();
    }
    
    public void onClick(View view) {
        final EditText queryView = (EditText) findViewById(R.id.q);
        final int id = view.getId();
        switch (id) {
        case R.id.clear:
            clear(queryView);
            break;
        case R.id.del:
            del(queryView);
            break;
        case R.id.exe:
            exe(queryView);
            break;
        case R.id.more:
        case R.id.less:
            ((ViewFlipper) findViewById(R.id.flipper)).showNext();
            break;
        default:
            buttonPressed(queryView, id);
        }
    }
    
    // Invoked if the user hits the physical "return" key while the EditText has focus.
    public boolean onEditorAction(TextView queryView, int actionId, KeyEvent event) {
        if (event != null && event.getAction() == KeyEvent.ACTION_UP) {
            // We already handled the ACTION_DOWN event, and don't want to repeat the work.
            return true;
        }
        exe((EditText) queryView);
        return true;
    }
    
    private void buttonPressed(EditText queryView, int id) {
        // Insert the new text (by replacing the entire content, which is our only option).
        // If there's a selection, we overwrite it.
        final String newText = buttonMap.get(id);
        final String existingText = queryView.getText().toString();
        final int startOffset = queryView.getSelectionStart();
        final int endOffset = queryView.getSelectionEnd();
        queryView.setText(existingText.substring(0, startOffset) + newText + existingText.substring(endOffset));
        // Put the caret back in the right place.
        int newCaretOffset;
        if (newText.length() > 1 && newText.endsWith(")")) {
            // For functions, we automatically insert both parentheses, so we need to move the caret back between them.
            newCaretOffset = startOffset + newText.length() - 1;
        } else {
            newCaretOffset = startOffset + newText.length();
        }
        queryView.setSelection(newCaretOffset, newCaretOffset);
    }
    
    private void clear(EditText queryView) {
        queryView.setText("");
        transcriptView().setText("");
    }
    
    private void del(EditText queryView) {
        int startOffset = queryView.getSelectionStart();
        int endOffset = queryView.getSelectionEnd();
        final String existingText = queryView.getText().toString();
        if (existingText.length() == 0) {
            return;
        }
        if (startOffset == endOffset) {
            // Remove the character before the caret.
            --startOffset;
            queryView.setText(existingText.substring(0, startOffset) + existingText.substring(endOffset));
        } else {
            // Remove the selection.
            queryView.setText(existingText.substring(0, startOffset) + existingText.substring(endOffset));
        }
        queryView.setSelection(startOffset, startOffset);
    }
    
    private void exe(EditText queryView) {
        final String queryText = queryView.getText().toString().trim();
        if (queryText.length() == 0) {
            // FIXME: report an error?
            return;
        }
        queryView.selectAll();
        
        final String answerText = computeAnswer(queryText);
        final Editable transcript = transcriptView().getEditableText();
        if (transcript.length() > 0) {
            // We add the newline between question/answer pairs when we add the next pair, so we don't waste space on a blank line.
            transcript.append("\n");
        }
        final int inputStart = transcript.length();
        transcript.append(queryText);
        final int inputEnd = transcript.length();
        transcript.append("\n");
        transcript.append(" = ");
        transcript.append(answerText);
        highlightInput(transcript, inputStart, inputEnd);
        
        scrollToBottomOfTranscript();
    }
    
    private void highlightInput(Spannable text, int start, int end) {
        text.setSpan(new ForegroundColorSpan(0xffcdaa7d), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        text.setSpan(new StyleSpan(Typeface.BOLD), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    
    private void scrollToBottomOfTranscript() {
        final TextView transcriptView = transcriptView();
        final android.graphics.Rect bounds = new android.graphics.Rect();
        final int y = transcriptView.getLineBounds(transcriptView.getLineCount() - 1, bounds);
        
        final ScrollView transcriptScrollView = (ScrollView) findViewById(R.id.transcript_scroll_view);
        transcriptScrollView.smoothScrollTo(0, bounds.bottom);
    }
    
    private String computeAnswer(String query) {
        try {
            String answer = null;
            /*
             * // Convert bases.
             * NumberDecoder numberDecoder = new NumberDecoder(query);
             * if (numberDecoder.isValid()) {
             * for (String item : numberDecoder.toStrings()) {
             * model.addElement(item);
             * }
             * }
             */
            if (answer == null) {
                // Convert units.
                answer = UnitsConverter.convert(query);
            }
            if (answer == null) {
                // Evaluate mathematical expressions.
                answer = calculator.evaluate(query);
            }
            if (answer == null) {
                answer = "Dunno, mate.";
            }
            return answer;
        } catch (CalculatorError ex) {
            return "Error: " + ex.getMessage();
        } catch (Exception ex) {
            ex.printStackTrace();
            return "What do you mean?";
        }
    }
    
    private void loadState() {
        final SharedPreferences state = getPreferences(MODE_PRIVATE);
        final int version = state.getInt("version", 0);
        if (version != 2) {
            // We've never been run before, or the last run was an incompatible version.
            return;
        }
        
        final EditText queryView = (EditText) findViewById(R.id.q);
        final String oldQuery = state.getString("query", "");
        queryView.setText(oldQuery);
        
        final TextView transcriptView = transcriptView();
        final String oldTranscript = state.getString("transcript", "");
        transcriptView.setText(oldTranscript);
        
        // We usually style the text as it's appended, but we don't store that information.
        final Editable transcript = transcriptView().getEditableText();
        int start = 0;
        int end;
        while (start != -1 && (end = TextUtils.indexOf(transcript, "\n =", start)) != -1) {
            highlightInput(transcript, start, end);
            start = TextUtils.indexOf(transcript, "\n", end + 1);
        }
        
        // We can't scroll to the bottom of the transcript until the text has been laid out.
        // This method runs *before* we're visible, so we need to wait.
        transcriptView.post(new Runnable() {
            public void run() {
                scrollToBottomOfTranscript();
            }
        });
        
        final String serializedPlotData = state.getString("plotData", "");
        if (serializedPlotData.length() > 0) {
            plotData = CalculatorPlotData.fromString(serializedPlotData);
        }
    }
    
    private void saveState() {
        final EditText queryView = (EditText) findViewById(R.id.q);
        
        final SharedPreferences.Editor state = getPreferences(MODE_PRIVATE).edit();
        state.putInt("version", 2);
        state.putString("query", queryView.getText().toString());
        state.putString("transcript", transcriptView().getText().toString());
        state.putString("plotData", (plotData != null) ? plotData.toString() : "");
        state.commit();
    }
    
    private TextView transcriptView() {
        return (TextView) findViewById(R.id.transcript);
    }
    
    private void showHelp() {
        startActivity(new Intent(this, MathdroidHelp.class));
    }
    
    public void showPlot(CalculatorPlotData plotData) {
        this.plotData = plotData;
        showDialog(DIALOG_PLOT);
    }
}