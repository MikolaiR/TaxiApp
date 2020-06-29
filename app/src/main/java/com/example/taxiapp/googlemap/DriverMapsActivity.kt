package com.example.taxiapp.googlemap

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
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.taxiapp.BuildConfig
import com.example.taxiapp.R
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.snackbar.Snackbar
import java.text.DateFormat
import java.util.*

class DriverMapsActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        const val CHECK_SETTINGS_CODE = 111
        const val REQUEST_LOCATION_PERMISSION = 222
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient //новый класс который служит для определения местоположения
    private lateinit var settingsClient: SettingsClient  //доступ к настройкам
    private lateinit var locationRequest: LocationRequest // сохранения данных запроса к fusedLocationClient
    private lateinit var locationSettingsRequest: LocationSettingsRequest //определение настроек девайса пользователя
    private lateinit var locationCallback: LocationCallback // события определения местоположения
    private lateinit var currentLocation: Location // в нем хранятся высота и широта пользователя
    private var isLocationUpdateActive: Boolean =
        false  //будет проверять активно ли обновление местоположения

    private lateinit var mMap: GoogleMap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        settingsClient = LocationServices.getSettingsClient(this)

        //методы для построения locationRequest
        buildLocationRequest()
        //buildLocationCallBack()
        buildLocationSettingsRequest()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add a marker in Sydney and move the camera
        val driverLocation = LatLng(-34.0, 151.0)
        mMap.addMarker(MarkerOptions().position(driverLocation).title("Driver"))
        mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation))
    }
    private fun stopLocationUpdate() {
        //останавливаем определение местоположения
        fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener {
            isLocationUpdateActive = false
        }
    }

    private fun startLocationUpdates() {
        settingsClient.checkLocationSettings(locationSettingsRequest)
            .addOnSuccessListener(this@DriverMapsActivity,
                object : OnSuccessListener<LocationSettingsResponse> {
                    override fun onSuccess(p0: LocationSettingsResponse?) {
                        if (ActivityCompat.checkSelfPermission(
                                this@DriverMapsActivity,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            ) !=
                            PackageManager.PERMISSION_GRANTED &&
                            ActivityCompat
                                .checkSelfPermission(
                                    this@DriverMapsActivity,
                                    Manifest.permission
                                        .ACCESS_COARSE_LOCATION
                                ) !=
                            PackageManager.PERMISSION_GRANTED
                        ) { return
                        }
                        buildLocationCallBack()
                        fusedLocationClient.requestLocationUpdates(
                            locationRequest,
                            locationCallback,
                            Looper.myLooper()
                        )
                    }
                    //в случаи неудачи
                }).addOnFailureListener(this, OnFailureListener {
                when ((it as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> try {
                        val resolvableApiException: ResolvableApiException =
                            it as ResolvableApiException
                        //передаю данные в метод onActivityResult
                        resolvableApiException.startResolutionForResult(
                            this@DriverMapsActivity,
                            CHECK_SETTINGS_CODE
                        )
                    } catch (sie: IntentSender.SendIntentException) {
                        sie.printStackTrace()
                    }
                    //в случаи когда настройки нужно вводить вручную
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val message = "Adjust location settings on your device"
                        Toast.makeText(this@DriverMapsActivity, message, Toast.LENGTH_LONG).show()
                        isLocationUpdateActive = false
                       /* todo
                       startLocationUpdateButton.isEnabled = true
                        stopLocationUpdateButton.isEnabled = false*/
                    }
                }
                updateLocationUi()
            })
    }

    // обрабатываю ошибку
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHECK_SETTINGS_CODE -> {
                when (resultCode) {
                    //пользователь дал согласие
                    Activity.RESULT_OK -> {
                        Log.d(
                            "MainActivity",
                            "User has agreed to change location settings"
                        )
                        startLocationUpdates()
                    }
                    // не дал согласие
                    Activity.RESULT_CANCELED -> {
                        Log.d(
                            "MainActivity",
                            "User has not agreed to change location settings"
                        )
                        isLocationUpdateActive = false
                        /*todo
                        startLocationUpdateButton.isEnabled = true
                        stopLocationUpdateButton.isEnabled = false*/
                        updateLocationUi()
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

    //запрашиваю текущее местоположение
    private fun buildLocationCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)
                currentLocation = p0.lastLocation
                Log.i("buildLocationCallBack", "$currentLocation")
                updateLocationUi()
            }
        }
    }

    //обновляем интерфейс
    private fun updateLocationUi() {
        currentLocation.let {
            val driverLocation = LatLng(currentLocation.latitude,currentLocation.longitude)
            mMap.moveCamera(CameraUpdateFactory.newLatLng(driverLocation))
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12F))
            mMap.addMarker(MarkerOptions().position(driverLocation).title("Driver"))
        }
    }

    //настройки запросов местоположения
    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.apply {
            interval = 5000
            fastestInterval = 3000
            priority =
                LocationRequest.PRIORITY_HIGH_ACCURACY //преоритет сигнала с высокой точьность
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
    // в случаи паузы останавливает определение местоположения для экономии батареи
    override fun onPause() {
        super.onPause()
        stopLocationUpdate()
    }

    private fun requestLocationPermission() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (shouldProvideRationale) {
            //снэкбар из материал дизайна implementation 'com.google.android.material:material:1.1.0'
            //запрашиваем у пользователя разрешение с объяснением
            showSnackBar("Location permission is needed for app functionality", "OK",
                View.OnClickListener {
                    ActivityCompat.requestPermissions(
                        this@DriverMapsActivity,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                })
        } else {
            //запрашиваем у пользователя разрешение без объяснения
            ActivityCompat.requestPermissions(
                this@DriverMapsActivity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    //снэкбар из материал дизайна implementation 'com.google.android.material:material:1.1.0'
    //выезжает сообщение в низу экрана с кнопкой ок
    private fun showSnackBar(mainText: String, action: String, listener: View.OnClickListener) {
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE)
            .setAction(action, listener).show()
    }

    // после того как разрешение будет получено обрабатываем ответ
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
                //в случаи невозможности подтвердить пользователю будет предложено перейти в настройки
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

    // проверка разрешения использования локации
    private fun checkLocationPermission(): Boolean {
        val permissionState =
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }
}