package capspatial.locateme;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Objects;

public class LocationEntryActivity extends AppCompatActivity {

    private EditText nameEditText;
    private EditText phoneNumberEditText;
    private EditText messageEditText;
    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private EditText boundaryRadiusEditText;
    private Button saveButton;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_entry);

        nameEditText = findViewById(R.id.editTextName);
        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber);
        messageEditText = findViewById(R.id.editTextMessage);
        latitudeEditText = findViewById(R.id.editTextLatitude);
        longitudeEditText = findViewById(R.id.editTextLongitude);
        boundaryRadiusEditText = findViewById(R.id.editTextBoundaryRadius);
        saveButton = findViewById(R.id.buttonSave);

        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveLocationEntry();
            }
        });

        // Initialize the delete button
        deleteButton = findViewById(R.id.buttonDelete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteLocationEntry();
            }
        });

        // Check if in edit mode
        Intent intent = getIntent();

        // Check if the intent has extras
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();

            // Iterate through all extras and print key-value pairs
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                Log.d("Intent Extra", key + ": " + value.toString());
            }
        }

        if (intent.getBooleanExtra("EDIT_MODE", false)) {
            // Load existing entry details for editing
            Log.i("Edit Mode", String.valueOf(true));
            loadExistingEntry(intent);
        } else {
            // Handle the button click event for creating an entry
            latitudeEditText.setText(String.valueOf(intent.getDoubleExtra("LATITUDE", 0.0)));
            longitudeEditText.setText(String.valueOf(intent.getDoubleExtra("LONGITUDE", 0.0)));
        }

    }

    private void loadExistingEntry(Intent intent) {

        Log.i("Editing", String.valueOf(intent.getIntExtra("BOUNDARY_RADIUS", 0)));

        String name = intent.getStringExtra("NAME");
        String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
        String message = intent.getStringExtra("MESSAGE");
        double latitude = intent.getDoubleExtra("LATITUDE", 0.0);
        double longitude = intent.getDoubleExtra("LONGITUDE", 0.0);
        int boundaryRadius = intent.getIntExtra("BOUNDARY_RADIUS", 100);

        // Populate UI with existing entry details
        nameEditText.setText(name);
        phoneNumberEditText.setText(phoneNumber);
        messageEditText.setText(message);
        latitudeEditText.setText(String.valueOf(latitude));
        longitudeEditText.setText(String.valueOf(longitude));
        boundaryRadiusEditText.setText(String.valueOf(boundaryRadius));

        // Change button text for edit mode
        saveButton.setText("Update");

        // Make the delete button visible when an entry is being edited
        deleteButton.setVisibility(View.VISIBLE);
    }

    private void saveLocationEntry() {

        Log.i("Saving entry", "Test");

        String name = nameEditText.getText().toString();
        String phoneNumber = phoneNumberEditText.getText().toString();
        String message = messageEditText.getText().toString();
        double latitude = Double.parseDouble(latitudeEditText.getText().toString());
        double longitude = Double.parseDouble(longitudeEditText.getText().toString());
        int boundaryRadius = Integer.parseInt(boundaryRadiusEditText.getText().toString());

        // Check if an entry with the same name already exists
        LocationBoundary existingEntry = LocationUtils.getLocationBoundaryByName(name, LocationUtils.getAllLocationBoundaries(this));

        if (existingEntry != null) {

            Log.i("Entry exists", existingEntry.toString());
            Log.i("boundaryRadius", String.valueOf(boundaryRadius));
            // Update the existing entry
            existingEntry.setName(name);
            existingEntry.setPhoneNumber(phoneNumber);
            existingEntry.setMessage(message);
            existingEntry.setBoundaryRadius(boundaryRadius);
            LocationUtils.updateLocationBoundary(this, existingEntry);
        } else {
            // Create a new entry
            LocationBoundary locationBoundary = new LocationBoundary(name, phoneNumber, message, latitude, longitude, boundaryRadius);

            // Save the location entry to a list or database
            LocationUtils.addLocationBoundary(this, locationBoundary);
        }

        // Optionally, you can navigate back to the main activity or perform other actions
        finish();
    }

    private void deleteLocationEntry() {
        String name = nameEditText.getText().toString();

        // Use the name or another identifier to locate and delete the entry
        LocationUtils.deleteLocationBoundaryByName(this, name);

        // Optionally, you can navigate back to the main activity or perform other actions
        finish();
    }

}
