package ru.crew.motley.dere.photo

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.os.Looper
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*

interface LocationProviderCallback {
    fun onPermissionRequest(requestPermission: Int)
    fun onResolutionRequired(ex: ResolvableApiException)
}


class LocationProvider(
        private val context: Context,
        private val providerCallback: LocationProviderCallback,
        private val locationCallback: LocationCallback) {

    companion object {
        val TAG: String = LocationProvider::class.java.simpleName
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val REQUEST_CHECK_SETTINGS = 0x1
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 1000
        const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = UPDATE_INTERVAL_IN_MILLISECONDS / 2
    }

    private var mFusedLocationClient: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context)

    private var mSettingsClient: SettingsClient = LocationServices.getSettingsClient(context)

    private val locationRequest by lazy { createLocationRequest() }

    private val mLocationSettingsRequest by lazy { createLocationSettingsRequest() }

    private var mRequestingLocationUpdates: Boolean = false

    private fun createLocationRequest() =
            LocationRequest.create().apply {
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                interval = UPDATE_INTERVAL_IN_MILLISECONDS
                fastestInterval = FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
//                fastestInterval = 1000
            }


    private fun createLocationSettingsRequest() =
            LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                    .build()


    private fun startLocationUpdates() {
        mSettingsClient.checkLocationSettings(mLocationSettingsRequest)
                .addOnSuccessListener { _ ->
                    Log.i(TAG, "All location settings are satisfied.")
                    try {
                        mFusedLocationClient.requestLocationUpdates(
                                locationRequest,
                                locationCallback,
                                Looper.myLooper())
                    } catch (ex: SecurityException) {
                        Log.e(TAG, "", ex)
                    }
                }
                .addOnFailureListener {
                    val statusCode = (it as ApiException).statusCode
                    when (statusCode) {
                        LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                            Log.i(TAG, "Location settings are not satisfied. Attempting to upgrade " + "location settings ")
                            try {
                                val rae = it as ResolvableApiException
                                providerCallback.onResolutionRequired(rae)
                            } catch (sie: IntentSender.SendIntentException) {
                                Log.i(TAG, "PendingIntent unable to execute request.")
                            }

                        }
                        LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                            val errorMessage = "Location settings are inadequate, and cannot be " + "fixed here. Fix in Settings."
                            Log.e(TAG, errorMessage)
                            Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            mRequestingLocationUpdates = false
                        }
                    }
                }
    }

    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            Log.d(TAG, "stopLocationUpdates: updates never requested, cancel-op.")
            return
        }
        mFusedLocationClient.removeLocationUpdates(locationCallback)
                .addOnCompleteListener { mRequestingLocationUpdates = false }
    }

    fun onCreate() {
        mRequestingLocationUpdates = false
    }

    fun onResume() {
        if (checkPermissions() && !mRequestingLocationUpdates) {
            startLocationUpdates()
        } else if (!checkPermissions()) {
            requestPermissions()
        }
    }

    fun onTakePicture() {
        onCreate()
        onResume()
    }

    fun onPause() {
        mRequestingLocationUpdates = true
        stopLocationUpdates()
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION)
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        providerCallback.onPermissionRequest(REQUEST_PERMISSIONS_REQUEST_CODE)
    }

    fun onPermissionGranted() {
        Log.i(TAG, "onRequestPermissionResult")
        startLocationUpdates()
    }
}