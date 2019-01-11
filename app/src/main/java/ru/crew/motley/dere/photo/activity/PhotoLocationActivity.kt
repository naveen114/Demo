package ru.crew.motley.dere.photo.activity

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.Window
import android.view.WindowManager
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_photo_location.*
import kotlinx.android.synthetic.main.fragment_photo2.*
import ru.crew.motley.dere.R
import ru.crew.motley.dere.photo.LocationProvider
import ru.crew.motley.dere.photo.LocationProviderCallback
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper


class PhotoLocationActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {

        private const val ZOOM = 15.0f

        private const val EXTRA_LON = "longitude"
        private const val EXTRA_LAT = "latitude"
        private const val EXTRA_CUR_LON = "currentLongitude"
        private const val EXTRA_CUR_LAT = "currentLatitude"

        fun getIntent(
                context: Context?,
                lat: Double, lon: Double,
                currentLat: Double?,
                currentLon: Double?) =
                Intent(context, PhotoLocationActivity::class.java).apply {
                    putExtra(EXTRA_LAT, lat)
                    putExtra(EXTRA_LON, lon)
                    currentLat?.let { putExtra(EXTRA_CUR_LAT, currentLat) }
                    currentLon?.let { putExtra(EXTRA_CUR_LON, currentLon) }
                }
    }

    private val locationProvider by lazy {
        LocationProvider(
                this,
                createProviderCallback(),
                createLocationCallback())
    }
    private var currentLocation: Location? = null
    private lateinit var photoLatLon: LatLng
    private var googleMap: GoogleMap? = null
    private var photoMarker: MarkerOptions? = null
    private var meMarker: MarkerOptions? = null
//    private var circleOptions: CircleOptions? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_photo_location)
        (map as SupportMapFragment).getMapAsync(this)
    }

    override fun onResume() {
        super.onResume()
        locationProvider.onResume()
    }

    override fun onPause() {
        super.onPause()
        locationProvider.onPause()
    }


    override fun onMapReady(googleMap: GoogleMap) {
        val lat = intent.getDoubleExtra(EXTRA_LAT, -1.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, -1.0)
        photoLatLon = LatLng(lat, lon)
        this.googleMap = googleMap
        photoMarker = MarkerOptions().position(photoLatLon).title("Photo was taken here")
        googleMap.addMarker(photoMarker).showInfoWindow()
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.moveCamera(CameraUpdateFactory.newLatLng(photoLatLon))
        googleMap.setOnMapClickListener {

        }
        openMaps.setOnClickListener {
            val intent = Intent(android.content.Intent.ACTION_VIEW, Uri.parse(
                    "http://maps.google.com/maps?q=loc:${photoLatLon.latitude}, ${photoLatLon.longitude}"))
            startActivity(intent)
        }
    }

    private fun createProviderCallback() =
            object : LocationProviderCallback {
                override fun onPermissionRequest(requestPermission: Int) {
//                    requestPermissions(arrayOf(ACCESS_FINE_LOCATION), LocationProvider.REQUEST_PERMISSIONS_REQUEST_CODE)
                }

                override fun onResolutionRequired(ex: ResolvableApiException) {
                    this@PhotoLocationActivity.let {
                        ex.startResolutionForResult(it, LocationProvider.REQUEST_CHECK_SETTINGS)
                    }
                }
            }

    private fun createLocationCallback() =
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    if (googleMap == null) {
                        return
                    }
                    updateMarkers(locationResult)
                    if (currentLocation == null) {
                        val bounds = LatLngBounds.Builder()
                                .include(meMarker?.position)
                                .include(photoMarker?.position)
                                .build()
                        googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
                    }
                    currentLocation = locationResult.lastLocation
                    setDirectionsListener(locationResult.lastLocation)
                }
            }

    private fun updateMarkers(locationResult: LocationResult) {
        googleMap?.clear()
        meMarker = MarkerOptions()
                .position(LatLng(
                        locationResult.lastLocation?.latitude!!,
                        locationResult.lastLocation?.longitude!!))
                .title("You are here")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
        googleMap?.addMarker(meMarker)?.showInfoWindow()
        val m = googleMap?.addMarker(photoMarker)
        m?.showInfoWindow()
    }

    private fun setDirectionsListener(location: Location) {
        openDirections.setOnClickListener {
            val intent = Intent(
                    android.content.Intent.ACTION_VIEW,
                    Uri.parse(
                            "http://maps.google.com/maps?" +
                                    "saddr=${location.latitude}, ${location.longitude}" +
                                    "&daddr=${photoLatLon.latitude}, ${photoLatLon.longitude}"))
            startActivity(intent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            LocationProvider.REQUEST_CHECK_SETTINGS -> {
                if (resultCode != Activity.RESULT_OK) {
                    locationProvider.onPause()
                    val bounds = LatLngBounds.Builder()
                            .include(photoMarker?.position)
                            .build()
                    googleMap?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 200))
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase))
    }

}