package com.udacity.project4.locationreminders.savereminder.selectreminderlocation


import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.material.snackbar.Snackbar
import com.udacity.project4.BuildConfig
import com.udacity.project4.R
import com.udacity.project4.base.BaseFragment
import com.udacity.project4.databinding.FragmentSelectLocationBinding
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.utils.setDisplayHomeAsUpEnabled
import org.koin.android.ext.android.inject
import java.io.FileNotFoundException

class SelectLocationFragment : BaseFragment(),OnMapReadyCallback,GoogleMap.OnMapLongClickListener,GoogleMap.OnPoiClickListener {
    companion object {
        const val TAG = "SelectLocationFragment"
        const val REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE = 1001
    }

    //Use Koin to get the view model of the SaveReminder
    override val _viewModel: SaveReminderViewModel by inject()
    private lateinit var binding: FragmentSelectLocationBinding

    private lateinit var fusedLocationProvider: FusedLocationProviderClient
    private var lastKnownLocation: Location? = null
    private var selectedLatLong: LatLng? = null
    private var selectedPointOfInterest: PointOfInterest? = null
    private lateinit var myMap: GoogleMap

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        binding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_select_location, container, false)

        binding.viewModel = _viewModel
        binding.lifecycleOwner = this

        setHasOptionsMenu(true)
        setDisplayHomeAsUpEnabled(true)

        fusedLocationProvider = FusedLocationProviderClient(requireActivity())


        val mapFragment = childFragmentManager.findFragmentById(R.id.myMap) as SupportMapFragment
        mapFragment.getMapAsync(this)

//        add the map setup implementation

//         zoom to the user location after taking his permission
//         add style to the map
//         put a marker to location that the user selected


//        call this function after the user confirms on the selected location


        return binding.root
    }

    override fun onStart() {
        super.onStart()
        _viewModel.locationIsConfirmed.observe(viewLifecycleOwner, androidx.lifecycle.Observer {
            if (it) {
                onLocationSelected()
            }
        })
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.map_options, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        //  Change the map type based on the user's selection.
        R.id.normal_map -> {
            myMap.mapType = GoogleMap.MAP_TYPE_NORMAL
            true
        }
        R.id.hybrid_map -> {
            myMap.mapType = GoogleMap.MAP_TYPE_HYBRID
            true
        }
        R.id.satellite_map -> {
            myMap.mapType = GoogleMap.MAP_TYPE_SATELLITE
            true
        }
        R.id.terrain_map -> {
            myMap.mapType = GoogleMap.MAP_TYPE_TERRAIN
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {

        // super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (isForegroundPermissionEnabled()) {
            enableMyLocation()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)){
                // _viewModel.showSnackBar.value = "Access to your location is required.";
                Snackbar.make(requireView(), "Access to your location is required.", Snackbar.LENGTH_LONG)
                    .setAction("Enable Location") {
                        requestForgroundPermissions()
                    }
                    .show()
            } else {
                Snackbar.make(requireView(), "Locations permissions were denied.", Snackbar.LENGTH_LONG)
                    .setAction("Change Permissions") {
                        startActivity(Intent().apply {
                            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                            data = Uri.fromParts("package", BuildConfig.APPLICATION_ID, null)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        })
                    }
                    .show()
            }
        }
    }

    protected fun isForegroundPermissionEnabled(): Boolean {
        return (PackageManager.PERMISSION_GRANTED ==
                ContextCompat.checkSelfPermission(requireActivity(),
                    Manifest.permission.ACCESS_FINE_LOCATION))
    }

    protected fun requestForgroundPermissions() {
        var permissionsArray = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)
        requestPermissions(permissionsArray, REQUEST_FOREGROUND_ONLY_PERMISSIONS_REQUEST_CODE)
    }

    @SuppressLint("MissingPermission")
    private fun enableMyLocation() {
        // Use base fragment method to check whether foreground and background location permissions are granted
        if (isForegroundPermissionEnabled()) {
            myMap.setMyLocationEnabled(true)
            val locationResult = fusedLocationProvider.lastLocation
            locationResult.addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    // Set the map's camera position to the current location of the device.
                    lastKnownLocation = task.result
                    if (lastKnownLocation != null) {
                        selectedLatLong = LatLng(lastKnownLocation!!.latitude,
                            lastKnownLocation!!.longitude)
                        val title = getString(R.string.custom_location)
                        selectedPointOfInterest = PointOfInterest(selectedLatLong, "myLocation", title)
                        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLatLong, 16f))
                        if (selectedLatLong != null) {
                            val marker = myMap.addMarker(
                                MarkerOptions()
                                    .position(selectedLatLong as LatLng)
                                    .title(title)
                            )
                            marker.showInfoWindow()
                        }
                    }
                }
            }

        }
        else {
            // Use BaseFragment method to request foreground and background permissions
            requestForgroundPermissions()
        }
    }


    override fun onMapReady(map: GoogleMap) {
        myMap = map

        enableMyLocation()
        setMapStyle(map)
        myMap.setOnMapLongClickListener(this)
        myMap.setOnPoiClickListener(this)
    }

    private fun setMapStyle(map: GoogleMap) {
        try {
            var success = map.setMapStyle(
                MapStyleOptions.loadRawResourceStyle(
                    requireActivity(),
                    R.raw.map_style
                )
            )

            if (!success) {
                Log.e(TAG, "Style parsing failed.")
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "Unable to parse map styles. R.raw.map_style cannot be found.")
        }
    }

    override fun onMapLongClick(latLng: LatLng) {
        myMap.clear()
        selectedLatLong = latLng
        val title = getString(R.string.custom_location)
        selectedPointOfInterest = PointOfInterest(selectedLatLong as LatLng, "myId", title)
        val marker = myMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
        )
        marker.showInfoWindow()
    }

    override fun onPoiClick(pointOfInterest: PointOfInterest) {
        myMap.clear()
        selectedPointOfInterest = pointOfInterest
        selectedLatLong = pointOfInterest.latLng
        val poiMarker = myMap.addMarker(MarkerOptions()
            .position(pointOfInterest.latLng)
            .title(pointOfInterest.name)
        )
        poiMarker.showInfoWindow()
    }

    //         When the user confirms on the selected location,
    //         send back the selected location details to the view model
    //         and navigate back to the previous fragment to save the reminder and add the geofence
    private fun onLocationSelected() {
        if (isForegroundPermissionEnabled()) {
            if (selectedLatLong != null && selectedPointOfInterest != null) {
                _viewModel.confirmLocation(selectedLatLong as LatLng, selectedPointOfInterest as PointOfInterest)
            }
        } else {
            requestForgroundPermissions()
        }
    }

}
