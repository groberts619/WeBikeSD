package org.phillyopen.mytracks.cyclephilly;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


/**
 * Created by groberts619 on 1/30/2017.
 */

public class UserNotes extends Activity implements AdapterView.OnItemClickListener {
    ListView listView;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usernotes);

        UserNotes.this.setTitle("WeBikeSD - Report Issue");

        listView = (ListView) findViewById(R.id.notesList);
        listView.setOnItemClickListener(this);
    }
    @Override
    public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
        setContentView(R.layout.usernotesdetails);

        // Added email to San Diego Street Services when clicking on ListView item
        Intent myIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto","street_service@sandiego.gov", null));
        myIntent.putExtra(Intent.EXTRA_SUBJECT, "Report " + ((TextView) view).getText());
        startActivity(Intent.createChooser(myIntent, "Send email..."));

        // Toast for ListView Items (optional)
        /*Toast.makeText(getApplicationContext(), ((TextView) view).getText(),
                Toast.LENGTH_SHORT).show();*/
    }
}
