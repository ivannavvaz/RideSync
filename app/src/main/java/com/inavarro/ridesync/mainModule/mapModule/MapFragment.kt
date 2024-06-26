package com.inavarro.ridesync.mainModule.mapModule

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentMapBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.activityMapModule.ActivityMapFragment

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mBinding: FragmentMapBinding

    private lateinit var mGoogleMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentMapBinding.inflate(layoutInflater)

        setupMap()

        setupSearchFragment()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        return mBinding.root
    }

    private fun setupSearchFragment(){
        ((activity as MainActivity).showFragmentContainerViewActivity())
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        enableLocation()

        // Move the camera to the current location if the permission is granted
        if (isLocationPermissionGranted()) {
            moveCameraToCurrentLocation()
        } else {
            moveCameraSpain()
        }

        val query = FirebaseFirestore.getInstance().collection("activities")

        query.get().addOnSuccessListener { result ->
            for (document in result) {
                val location = document.getGeoPoint("location")
                val id = document.id
                val date = document.getTimestamp("date")

                if (location != null) {
                    if (date != null && date.toDate().time > System.currentTimeMillis()){
                        addMarker(LatLng(location.latitude, location.longitude), id, document.getString("type")!!)
                    } else {
                        addMarker(
                            LatLng(location.latitude, location.longitude),
                            id,
                            document.getString("type")!!
                        )
                    }
                }
            }
        }

        mGoogleMap.setOnMarkerClickListener { marker ->
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 10f))

            // Load the activity fragment
            val activityMapFragment = ActivityMapFragment()
            val bundle = Bundle()
            bundle.putString("id", marker.title)
            activityMapFragment.arguments = bundle
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerViewActivity, activityMapFragment)
                .addToBackStack(null)
                .commit()

            true
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableLocation() {
        // Check if the map is initialized
        if (!::mGoogleMap.isInitialized) return

        // Check if the permission is granted
        if (isLocationPermissionGranted()) {
            mGoogleMap.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        // Check if the permission should be requested
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            Toast.makeText(
                requireContext(),
                "Permisos de ubicación necesarios para mostrar la ubicación actual.",
                Toast.LENGTH_SHORT
            ).show()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_CODE_LOCATION
            )
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_LOCATION -> {
                // Check if the permission was granted
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mGoogleMap.isMyLocationEnabled = true
                } else {
                    Toast.makeText(
                        requireContext(),
                        "No se activaron los permisos de ubicación.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check if the map is initialized
        if (!::mGoogleMap.isInitialized) return

        // Check if the permission is granted
        if (!isLocationPermissionGranted()) {
            mGoogleMap.isMyLocationEnabled = false
            Toast.makeText(
                requireContext(),
                "No se activaron los permisos de ubicación.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun moveCameraSpain() {
        val latLng = LatLng(40.5, -3.7)
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 5f))
    }

    @SuppressLint("MissingPermission")
    private fun moveCameraToCurrentLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 10f))
                }
            }
    }

    private fun addMarker(latLng: LatLng, id: String, type: String) {
        when (type) {
            "meeting" -> {
                mGoogleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(id)
                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_groups))
                )
            }
            "restaurant" -> {
                mGoogleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(id)
                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_fastfood))
                )
            }
            "route" -> {
                mGoogleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(id)
                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_route))
                )
            }
            else -> {
                mGoogleMap.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title(id)
                        .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_location))
                )
            }
        }
    }

    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)
        vectorDrawable!!.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)

        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)

        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }
}