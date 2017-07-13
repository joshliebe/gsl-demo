package com.joshliebe.gsldemo;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;

import com.braintreepayments.api.dropin.DropInRequest;
import com.braintreepayments.api.dropin.DropInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.Manifest;
import android.view.View;
import android.widget.Toast;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String SANDBOX_TOKENIZATION_KEY = "sandbox_tmxhyf7d_dcpspy2brwdjr3qn";
    private static final String ORDER_NODE = "Order";
    private static final int DROP_IN_REQUEST_CODE = 567;

    private static final long REQUEST_INTERVAL = 1000L;
    private static final float ZOOM_LEVEL = 18f;
    private static final int LOCATION_REQUEST_CODE = 123;

    private GoogleMap googleMap;
    private GoogleApiClient googleApiClient;
    private Location lastLocation;
    private Marker currentLocationMarker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        //Check that the user has given permission to access their location
        checkLocationPermission();
        findViewById(R.id.order_flowers).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Show the PayPal/card "drop-in" at the bottom of the screen
                startActivityForResult(getDropInRequest().getIntent(MapsActivity.this), DROP_IN_REQUEST_CODE);
            }
        });
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.googleMap = googleMap;
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            setUpGoogleApiClient();
            // Draw an indication of the user's current location on the map
            this.googleMap.setMyLocationEnabled(true);
        }
    }

    /**
     * Called when a connect request is successfully completed using setUpGoogleApiClient().
     */
    @Override
    public void onConnected(@Nullable Bundle bundle) {
        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(REQUEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Request location updates from the Google API client
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, this);
        }
    }

    /**
     * Called when the location of the user has changed.
     */
    @Override
    public void onLocationChanged(Location location) {
        // Store the current location of the user
        lastLocation = location;
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        if(currentLocationMarker == null) {
            // Move the camera to the user's current location on the first location update
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, ZOOM_LEVEL));
        }
        replaceMarker(latLng);
    }

    /**
     * Processes the result of the Braintree transaction (PayPal or card).
     */
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == DROP_IN_REQUEST_CODE) {
                DropInResult result = data.getParcelableExtra(DropInResult.EXTRA_DROP_IN_RESULT);
                // Send the result to Firebase
                sendData(result);
                // Show a message that the transaction was successful
                Toast.makeText(this, R.string.payment_succesful, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Processes the result of the location permission request.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if(requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    if (googleApiClient == null) {
                        setUpGoogleApiClient();
                    }
                    googleMap.setMyLocationEnabled(true);
                }
            } else {
                // Show a message that the position has not been granted
                Toast.makeText(this, R.string.permission_not_granted, Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Checks whether the permission to use location has been granted.
     * If it has not been granted, the user will be prompted to allow location access.
     */
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST_CODE);
        }
    }

    /**
     * Sets up the Google API client to use the location services API and relevant callbacks.
     */
    private synchronized void setUpGoogleApiClient() {
        googleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        googleApiClient.connect();
    }

    /**
     * Adds a marker to the current position.
     */
    private void replaceMarker(LatLng latLng) {
        // Remove the previous marker
        if (currentLocationMarker != null) {
            currentLocationMarker.remove();
        }
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        // Add a marker indicating the user's current position to the Google Map
        currentLocationMarker = googleMap.addMarker(markerOptions);
    }

    /**
     * Creates the PayPal/card drop-in request.
     */
    private DropInRequest getDropInRequest() {
        return new DropInRequest()
                // Use the Braintree sandbox for dev/demo purposes
                .clientToken(SANDBOX_TOKENIZATION_KEY)
                .requestThreeDSecureVerification(true)
                .collectDeviceData(true);
    }

    /**
     * Sends location and payment data to Firebase.
     */
    private void sendData(DropInResult result) {
        // Get the Firebase node to write the data to
        DatabaseReference node = FirebaseDatabase.getInstance().getReference().child(ORDER_NODE).push();
        // Write an entry containing the location and payment data of the user to the Firebase node
        node.setValue(new Order(result, lastLocation.getLatitude(), lastLocation.getLongitude()));
    }

    @Override
    public void onConnectionSuspended(int i) {
        // Not implemented
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        // Not implemented
    }
}