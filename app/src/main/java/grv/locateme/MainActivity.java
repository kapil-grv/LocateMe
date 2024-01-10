package grv.locateme;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.graphics.drawable.IconCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.mapbox.geojson.Point;
import com.mapbox.maps.CameraOptions;
import com.mapbox.maps.MapView;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements Serializable {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private FusedLocationProviderClient fusedLocationClient;
    private ActivityResultLauncher<Intent> notificationSettingsLauncher;
    private TextView locationTextView;
    private LocationBoundary selectedEntry;
    private double currentLatitude = 0.0;
    private double currentLongitude = 0.0;
    private MapView mapView;
    private ProgressDialog progressDialog;
    private static List<LocationBoundary> locationBoundaries;

    private boolean startedLocationUpdateService = false;

    // TODO: Consider calling
    //    * Pages:
    //      - Home
    //         - Recent pings (Name, Status, Time)
    //      - Tasks
    //          - New task
    //              - Recipient phone number with international code (+919008314988)
    //              - Notification message (Text to be shared with recipient)
    //              - Notification interval in hours (24 hours by default)
    //              - Notification frequency
    //                  - Repeat count (number of times)
    //                  - Repeat duration (Repeat check after n minutes)
    //          - Current tasks (List of current tasks, with on click edit button. Edit button will be similar to new task page, but will refilled values based on the task selected and option to modify and re-save that task)
    //      - Message delivery notification
    //      - Analytics (need login)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Get all locations
        locationBoundaries = LocationUtils.getAllLocationBoundaries(this);

        // Initialize the ProgressDialog
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Loading Map. Waiting for Location ...");
        progressDialog.setCancelable(false); // Set to false if you don't want users to cancel

        // Request location and SMS permissions
        requestPermissions();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        locationTextView = findViewById(R.id.locationTextView);
        mapView = findViewById(R.id.mapView);

        Button getLocationButton = findViewById(R.id.getLocationButton);
        getLocationButton.setOnClickListener(view -> Executors.newSingleThreadExecutor().execute(new LocationTask()));

    }

    // Method to set the selected entry
    private void setSelectedEntry(LocationBoundary entry) {
        selectedEntry = entry;
    }

    // Method to handle item selection, call this when an entry is selected
    private void onItemSelected(LocationBoundary entry) {
        Log.i("MainActivity", "Item selected: " + entry.getName());
        setSelectedEntry(entry);

        // Call editEntry directly when an entry is clicked
        editEntry(selectedEntry);
    }

    private void listAllEntries() {
        // Retrieve all entries (modify this based on your data structure)
        List<LocationBoundary> allEntries = LocationUtils.getAllLocationBoundaries(this);;

        // Use a dialog or start a new activity to display the list
        // For simplicity, let's use a dialog in this example
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select an Entry");

        // Convert entry names to an array (modify this based on your data structure)
        String[] entryNames = new String[allEntries.size()];
        for (int i = 0; i < allEntries.size(); i++) {
            // Assuming your LocationBoundary class has a method like getName()
            entryNames[i] = allEntries.get(i).getName();
        }

        builder.setItems(entryNames, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Handle item click
                LocationBoundary selectedEntry = allEntries.get(which);
                Log.i("Editing entry", selectedEntry.toString());
                onItemSelected(selectedEntry);
            }
        });

        builder.show();
    }

    public void showEntries(View view) {
        // Show a list of all entries
        listAllEntries();
    }

    public void createEntry(View view) {
        Log.i("Creation request", "New Entry");
        switchToLocationEntryActivity(false, null);
    }

    public void editEntry(LocationBoundary entry) {
        // Assuming you have a selectedEntry variable representing the entry to edit
        if (entry != null) {
            switchToLocationEntryActivity(true, entry);
        }
    }

    private void switchToLocationEntryActivity(boolean editMode, LocationBoundary existingEntry) {
        Intent intent = new Intent(this, LocationEntryActivity.class);
        intent.putExtra("EDIT_MODE", editMode);

        if (!startedLocationUpdateService) {
            startLocationUpdateService();
        }

        if (editMode) {
            Log.i("Edit mode", "Activated for " + existingEntry.toString());
            // Use consistent keys when putting extras
            intent.putExtra("NAME", existingEntry.getName());
            intent.putExtra("PHONE_NUMBER", existingEntry.getPhoneNumber());
            intent.putExtra("MESSAGE", existingEntry.getMessage());
            intent.putExtra("LATITUDE", existingEntry.getLatitude());
            intent.putExtra("LONGITUDE", existingEntry.getLongitude());
            intent.putExtra("BOUNDARY_RADIUS", existingEntry.getBoundaryRadius());
            intent.putExtra("FREQUENCY", existingEntry.getFrequency());
        } else {
            intent.putExtra("LATITUDE", currentLatitude);
            intent.putExtra("LONGITUDE", currentLongitude);
        }

        startActivity(intent);
    }

    private void requestPermissions() {
        String[] permissions = {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.SEND_SMS,
                Manifest.permission.VIBRATE, // Replace POST_NOTIFICATIONS with VIBRATE
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.POST_NOTIFICATIONS
        };

        // Check if any of the permissions are not granted, then request them
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                Log.i("Requesting Permission", String.valueOf(ActivityCompat.checkSelfPermission(this, permission)));
                ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST_CODE);
                break;  // Request only once if any permission is not granted
            }
        }
    }

    private void startLocationUpdateService() {

        // Start the location update service with the MainActivity instance
        Intent serviceIntent = new Intent(this, LocationUpdateService.class);
        startForegroundService(serviceIntent);
        startedLocationUpdateService = true;
    }

    public void requestLocation() {
        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Update UI elements here
                    progressDialog.show(); // Show the progress dialog on the main thread
                }
            });

            // Create a location request
            LocationRequest locationRequest = new LocationRequest.Builder(0).setMinUpdateDistanceMeters(5).build();

            LocationCallback locationCallback = new LocationCallback() {

                @Override
                public void onLocationResult(@NonNull LocationResult locationResult) {

                    if (locationResult.getLocations().size() > 0) {
                        Location location = locationResult.getLocations().get(0);

                        updateLocationTextView(location.getLatitude(), location.getLongitude());
                        currentLatitude = location.getLatitude();
                        currentLongitude = location.getLongitude();

                        // Create a CameraPosition with the current location and desired zoom level
                        mapView.getMapboxMap().setCamera(
                                new CameraOptions.Builder()
                                        .center(Point.fromLngLat(currentLongitude, currentLatitude))
                                        .pitch(0.0)
                                        .zoom(16.0)
                                        .bearing(0.0)
                                        .build()
                        );

                        // Dismiss the progress dialog after loading is complete
                        if (progressDialog.isShowing()) {
                            progressDialog.dismiss();
                        }

                        Log.i("LocationUpdateService", "Updating coordinates to " + location.getLatitude() +"|"+ location.getLongitude());
                    }
                }
            };

            // Request location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    private void updateLocationTextView(double latitude, double longitude) {
        locationTextView.setText(String.format("Latitude: %s\nLongitude: %s", latitude, longitude));
    }

    private class LocationTask implements Runnable {
        @Override
        public void run() {
            Executors.newSingleThreadExecutor().execute(MainActivity.this::requestLocation);
        }
    }
}
