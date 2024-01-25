package grv.locateme;

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
//    private EditText phoneNumberEditText;
    private EditText emailEditText;
    private EditText messageEditText;
    private EditText latitudeEditText;
    private EditText longitudeEditText;
    private EditText boundaryRadiusEditText;
    private EditText frequencyEditText;
    private Button saveButton;
    private Button deleteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_entry);

        nameEditText = findViewById(R.id.editTextName);
//        phoneNumberEditText = findViewById(R.id.editTextPhoneNumber);
        emailEditText = findViewById(R.id.editTextEmail);
        messageEditText = findViewById(R.id.editTextMessage);
        latitudeEditText = findViewById(R.id.editTextLatitude);
        longitudeEditText = findViewById(R.id.editTextLongitude);
        boundaryRadiusEditText = findViewById(R.id.editTextBoundaryRadius);
        frequencyEditText = findViewById(R.id.editTextFrequency);
        saveButton = findViewById(R.id.buttonSave);

        saveButton.setOnClickListener(view -> saveLocationEntry());

        // Initialize the delete button
        deleteButton = findViewById(R.id.buttonDelete);
        deleteButton.setOnClickListener(view -> deleteLocationEntry());

        // Check if in edit mode
        Intent intent = getIntent();

        // Check if the intent has extras
        if (intent != null && intent.getExtras() != null) {
            Bundle extras = intent.getExtras();

            // Iterate through all extras and print key-value pairs
            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                if (value != null) {
                    Log.d("Intent Extra", key + ": " + value);
                }
            }
        }

        if (intent != null && intent.getBooleanExtra("EDIT_MODE", false)) {
            // Load existing entry details for editing
            Log.i("Edit Mode", String.valueOf(true));
            loadExistingEntry(intent);
        } else if (intent != null) {
            // Handle the button click event for creating an entry
            latitudeEditText.setText(String.valueOf(intent.getDoubleExtra("LATITUDE", 0.0)));
            longitudeEditText.setText(String.valueOf(intent.getDoubleExtra("LONGITUDE", 0.0)));
            frequencyEditText.setText(String.valueOf(intent.getIntExtra("FREQUENCY", 720)));
        }
    }

    private void loadExistingEntry(Intent intent) {

        Log.i("Editing", Objects.requireNonNull(intent.getStringExtra("NAME")));

        String name = intent.getStringExtra("NAME");
//        String phoneNumber = intent.getStringExtra("PHONE_NUMBER");
        String email = intent.getStringExtra("EMAIL");
        String message = intent.getStringExtra("MESSAGE");
        double latitude = intent.getDoubleExtra("LATITUDE", 0.0);
        double longitude = intent.getDoubleExtra("LONGITUDE", 0.0);
        int boundaryRadius = intent.getIntExtra("BOUNDARY_RADIUS", 100);
        int frequency = intent.getIntExtra("FREQUENCY", 720);

        // Populate UI with existing entry details
        nameEditText.setText(name);
        nameEditText.setEnabled(false);  // Disable editing of the name field
//        phoneNumberEditText.setText(phoneNumber);
        emailEditText.setText(email);
        messageEditText.setText(message);
        latitudeEditText.setText(String.valueOf(latitude));
        longitudeEditText.setText(String.valueOf(longitude));
        boundaryRadiusEditText.setText(String.valueOf(boundaryRadius));
        frequencyEditText.setText(String.valueOf(frequency));

        // Change button text for edit mode
        saveButton.setText(R.string.update);

        // Make the delete button visible when an entry is being edited
        deleteButton.setVisibility(View.VISIBLE);
    }

    private void saveLocationEntry() {

        Log.i("Saving entry", "Test");

        String name = nameEditText.getText().toString();
//        String phoneNumber = phoneNumberEditText.getText().toString();
        String message = messageEditText.getText().toString();
        String email = emailEditText.getText().toString();

        double latitude = LocationUtils.parseAndValidateDouble(latitudeEditText.getText().toString(), "Invalid Latitude", this);
        double longitude = LocationUtils.parseAndValidateDouble(longitudeEditText.getText().toString(), "Invalid Longitude", this);
        int boundaryRadius = LocationUtils.parseAndValidateInt(boundaryRadiusEditText.getText().toString(), "Invalid Boundary Radius", this);
        int frequency = LocationUtils.parseAndValidateInt(frequencyEditText.getText().toString(), "Invalid Frequency", this);

        // Check if an entry with the same name already exists
        LocationBoundary existingEntry = LocationUtils.getLocationBoundaryByName(name, LocationUtils.getAllLocationBoundaries(this));

        if (existingEntry != null) {

            Log.i("Entry exists", existingEntry.toString());
            Log.i("frequency", String.valueOf(frequency));

            // Update the existing entry
            // existingEntry.setPhoneNumber(phoneNumber);
            existingEntry.setEmail(email);
            existingEntry.setMessage(message);
            existingEntry.setBoundaryRadius(boundaryRadius);
            existingEntry.setFrequency(frequency);
            LocationUtils.updateLocationBoundary(this, existingEntry);
        } else {
            // Create a new entry
            LocationBoundary locationBoundary = new LocationBoundary(name, "NA", message, latitude, longitude, boundaryRadius, frequency, email);

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
