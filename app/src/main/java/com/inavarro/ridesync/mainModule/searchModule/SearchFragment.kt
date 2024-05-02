package com.inavarro.ridesync.mainModule.searchModule

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
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
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.material.tabs.TabLayoutMediator
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentSearchBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mBinding: FragmentSearchBinding

    private lateinit var mGoogleMap: GoogleMap


    companion object {
        const val REQUEST_CODE_LOCATION = 0
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentSearchBinding.inflate(layoutInflater)

        setupMap()

        return mBinding.root
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        enableLocation()

        moveCameraSpain()
        addMarker(LatLng(40.5, -3.7), "España")

        val query = FirebaseFirestore.getInstance().collection("activities")

        query.get().addOnSuccessListener { result ->
            for (document in result) {
                val location = document.getGeoPoint("location")
                val title = document.getString("title")
                if (location != null && title != null) {
                    addMarker(LatLng(location.latitude, location.longitude), title)
                }
            }
        }
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun enableLocation() {
        if (!::mGoogleMap.isInitialized) return
        if (isLocationPermissionGranted()) {
            mGoogleMap.isMyLocationEnabled = true
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
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
        if (!::mGoogleMap.isInitialized) return
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

    private fun addMarker(latLng: LatLng, title: String) {
        mGoogleMap.addMarker(
            MarkerOptions()
                .position(latLng)
                .title(title)
                .icon(bitmapDescriptorFromVector(requireContext(), R.drawable.ic_location))
        )
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