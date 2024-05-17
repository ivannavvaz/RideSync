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
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.activityModule.ActivityFragment

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

        setupSearchFragment()

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

        moveCameraSpain()

        val query = FirebaseFirestore.getInstance().collection("activities")

        query.get().addOnSuccessListener { result ->
            for (document in result) {
                val location = document.getGeoPoint("location")
                val id = document.id
                if (location != null) {
                    addMarker(LatLng(location.latitude, location.longitude), id, document.getString("type")!!)
                }
            }
        }

        mGoogleMap.setOnMarkerClickListener { marker ->
            mGoogleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(marker.position, 10f))

            // Load the activity fragment
            val fragment = ActivityFragment()
            val bundle = Bundle()
            bundle.putString("id", marker.title)
            fragment.arguments = bundle
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainerViewActivity, fragment)
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