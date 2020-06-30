package com.example.taxiapp.activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.taxiapp.BuildConfig
import com.example.taxiapp.R
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class PassengerMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val CHECK_SETTINGS_CODE = 111
        const val REQUEST_LOCATION_PERMISSION = 222
    }

    private lateinit var settingsButton:Button
    private lateinit var signOutButton: Button

    private lateinit var auth: FirebaseAuth
    private lateinit var currentPassenger : FirebaseUser

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var settingsClient: SettingsClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationSettingsRequest: LocationSettingsRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var currentLocation: Location
    private var isLocationUpdateActive: Boolean = false

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_passenger_maps)

        settingsButton = findViewById(R.id.settingsButton)
        signOutButton = findViewById(R.id.signOutButton)

        auth = FirebaseAuth.getInstance()
        auth.currentUser?.let {
            currentPassenger = it
        }

        signOutButton.setOnClickListener {
            auth.signOut()
            signOutDriver()
            val intent = Intent(this, ChoseModeActivity::class.java)
            //user don`t return this is activity
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        buildLocationRequest()
        buildLocationSettingsRequest()
    }

    private fun signOutDriver() {
        val passengerUserId = currentPassenger.uid
        val passenger = Firebase.database.reference.child("passenger")
        val geoFire = GeoFire(passenger)
        geoFire.removeLocation(passengerUserId)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in Sydney and move the camera
        val passengerLocation = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(passengerLocation).title("Passenger"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation))
    }

    private fun stopLocationUpdate() {
        //todo not correct work stop update location
        if (isLocationUpdateActive){
            isLocationUpdateActive = false
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun startLocationUpdates() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(this@PassengerMapsActivity,
                object : OnSuccessListener<LocationSettingsResponse> {
                    override fun onSuccess(p0: LocationSettingsResponse?) {
                        if (ActivityCompat.checkSelfPermission(
                                this@PassengerMapsActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat
                                .checkSelfPermission(
                                    this@PassengerMapsActivity,
                                    Manifest.permission
                                        .ACCESS_COARSE_LOCATION
                                ) !=
                            PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        buildLocationCallBack()
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.myLooper())
                    }
                }).addOnFailureListener(this) {
                when ((it as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvableApiException: ResolvableApiException =
                            it as ResolvableApiException
                        //pass in onActivityResult
                        resolvableApiException.startResolutionForResult(
                            this@PassengerMapsActivity,
                            CHECK_SETTINGS_CODE
                        )
                    } catch (sie: IntentSender.SendIntentException) {
                        sie.printStackTrace()
                    }
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val message = "Adjust location settings on your device"
                        Toast.makeText(this@PassengerMapsActivity, message, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHECK_SETTINGS_CODE -> {
                when (resultCode) {
                    Activity.RESULT_OK -> {
                        Log.d(
                            "MainActivity",
                            "User has agreed to change location settings"
                        )
                        startLocationUpdates()
                    }
                    Activity.RESULT_CANCELED -> {
                        Log.d(
                            "MainActivity",
                            "User has not agreed to change location settings"
                        )
                    }
                }
            }
        }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)
        locationSettingsRequest = builder.build()
    }

    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                currentLocation = p0.lastLocation
                updateLocationUi()
            }
        }
    }

    private fun updateLocationUi() {
        isLocationUpdateActive = true
        currentLocation.let {
            val passengerLocation = LatLng(it.latitude, it.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(passengerLocation))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12F))
            mMap.addMarker(MarkerOptions().position(passengerLocation).title("Passenger location"))
            val passengerUserId = currentPassenger.uid
            val passenger = Firebase.database.reference.child("passenger")
            val geoFire = GeoFire(passenger)
            geoFire.setLocation(passengerUserId, GeoLocation(it.latitude,it.longitude))
        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.apply {
            interval = 5000
            fastestInterval = 3000
            priority =
                LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }

    override fun onResume() {
        super.onResume()
        if (checkLocationPermission()) {
            startLocationUpdates()
        } else {
            requestLocationPermission()
        }
    }

    override fun onStop() {
        super.onStop()
        stopLocationUpdate()
    }

    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (shouldProvideRationale) {
            showSnackBar("Location permission is needed for app functionality", "OK",
                View.OnClickListener {
                    ActivityCompat.requestPermissions(
                        this@PassengerMapsActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                })
        } else {
            ActivityCompat.requestPermissions(
                this@PassengerMapsActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    private fun showSnackBar(mainText: String, action: String, listener: View.OnClickListener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE)
            .setAction(action, listener).show()
    }

    // after request for permission I process the answer
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.isEmpty()) {
                Log.d("onRequestPermissionsRes", "Request was cancelled")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (isLocationUpdateActive) {
                    startLocationUpdates()
                }
            } else {
                //in cases of impossibility to confirm the user will be asked to go to settings
                showSnackBar("Turn on location on settings", "Settings", View.OnClickListener {
                    val intent = Intent()
                    val uri = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                    intent.apply {
                        action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = uri
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    startActivity(intent)
                })
            }
        }
    }

    private fun checkLocationPermission(): Boolean {
        val permissionState =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }
}