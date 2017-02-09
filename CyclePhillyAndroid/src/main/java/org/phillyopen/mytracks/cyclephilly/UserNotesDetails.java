package org.phillyopen.mytracks.cyclephilly;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

/**
 * Created by groberts619 on 2/7/2017.
 */

public class UserNotesDetails extends Activity implements View.OnClickListener {
    private static final int RESULT_LOAD_IMAGE = 1;

    ImageView imageToUpload;
    Button btnUploadImage;
    EditText detailsToUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.usernotesdetails);

        imageToUpload = (ImageView) findViewById(R.id.imageToUpload);
        btnUploadImage = (Button) findViewById(R.id.btnUploadImage);
        detailsToUpload = (EditText) findViewById(R.id.detailsToUpload);

        imageToUpload.setOnClickListener(this);
        btnUploadImage.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()){
            case R.id.imageToUpload:
                Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, RESULT_LOAD_IMAGE);
                break;
            case R.id.btnUploadImage:

                break;
            case R.id.detailsToUpload:

                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            imageToUpload.setImageURI(selectedImage);
        }
    }
}
