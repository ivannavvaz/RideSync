package com.inavarro.ridesync.mainModule.searchModule

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.tabs.TabLayoutMediator
import com.inavarro.ridesync.R
import com.inavarro.ridesync.databinding.FragmentSearchBinding
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class SearchFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mBinding: FragmentSearchBinding

    private lateinit var mGoogleMap: GoogleMap

    private var mAllowUbication: Boolean = false

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

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
        val supportMapFragment = childFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

        val sharedPref =
            requireActivity().getSharedPreferences("settings_preferences", Context.MODE_PRIVATE)
        mAllowUbication = sharedPref.getBoolean("allowUbication", false)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        if (ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )
            return
        }
        enableUserLocation()
    }

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enableUserLocation()
            } else {
                Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableUserLocation() {
        if (mAllowUbication) {
            mGoogleMap.isMyLocationEnabled = true
            mGoogleMap.isMyLocationEnabled = true
            mFusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
/*
                    searchViewModel.getLatAndLong().observe(viewLifecycleOwner) { locations ->
                        locations?.forEach { latLongIdPairs ->
                            val markerLatLng = LatLng(latLongIdPairs.second, latLongIdPairs.third)
                            val customMarker = BitmapDescriptorFactory.fromBitmap(resizeBitmap())
                            val marker = mGoogleMap.addMarker(
                                MarkerOptions().position(markerLatLng).icon(customMarker)
                            )
                            marker?.tag = latLongIdPairs.first

                        }
                        */
                }
            }
        } else {
            Toast.makeText(requireContext(), "Ubication not allowed", Toast.LENGTH_SHORT).show()
        }
/*
        mMap.setOnMarkerClickListener { marker ->
            val args = Bundle()
            args.putString("propertyId", marker.tag as String)
            findNavController().navigate(R.id.navigation_mini_property, args)
            false
        }

 */

    }
}