package org.phillyopen.mytracks.cyclephilly;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Spinner;

import java.util.Map;

/**
 * Created by groberts619 on 1/30/2017.
 */

public class UserNotes extends Activity {
    public final static int PREF_NOTES = 1;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usernotes);

        SharedPreferences settings = getSharedPreferences("PREFS", 0);
        Map<String, ?> prefs = settings.getAll();
        for (Map.Entry<String, ?> p : prefs.entrySet()) {
            int key = Integer.parseInt(p.getKey());
            // CharSequence value = (CharSequence) p.getValue();

            switch (key) {
                case PREF_NOTES:
                    ((Spinner) findViewById(R.id.usernotesSpinner))
                            .setSelection(((Integer) p.getValue()).intValue());
            }
        }
    }

    private void savePreferences() {
        // Save user preferences. We need an Editor object to
        // make changes. All objects are from android.context.Context
        final SharedPreferences settings = getSharedPreferences("PREFS", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putInt("" + PREF_NOTES, ((Spinner) findViewById(R.id.usernotesSpinner))
                .getSelectedItemPosition());
    }
}
