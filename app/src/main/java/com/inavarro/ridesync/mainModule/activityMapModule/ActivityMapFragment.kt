package com.inavarro.ridesync.mainModule.activityMapModule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.common.entities.Activity
import com.inavarro.ridesync.databinding.FragmentActivityMapBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ActivityMapFragment : Fragment() {

    private lateinit var mBinding: FragmentActivityMapBinding

    private lateinit var mActivity: Activity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentActivityMapBinding.inflate(layoutInflater)

        getSelectedActivity()

        mBinding.background.setOnClickListener {
            // Close fragment
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        }

        mBinding.ivMap.setOnClickListener {
            openMap()
        }

        return mBinding.root
    }

    private fun getSelectedActivity() {
        val id = arguments?.getString("id")

        // Get activities from Firestore
        val db = FirebaseFirestore.getInstance()
          db.collection("activities")
                .document(id!!)
                .get()
                .addOnSuccessListener { document ->
                 if (document.exists()) {
                     // Get activity
                      mActivity = document.toObject(Activity::class.java)!!
                      setActivity(mActivity)
                 } else {
                     // Show error
                     Snackbar.make(mBinding.root, "No such activity", Snackbar.LENGTH_SHORT).show()
                 }
                }
                .addOnFailureListener { exception ->
                 Snackbar.make(mBinding.root, "Error getting activity: $exception", Snackbar.LENGTH_SHORT).show()
                }
    }

    private fun setActivity(activity: Activity) {
        mBinding.tvTitle.text = activity.title
        mBinding.tvDescription.text = activity.description

        if (activity.date != null) {
            showDate()
            // Format date
            val date = activity.date.toDate()
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            mBinding.tvDate.text = dateFormat.format(date)
        }

        if (activity.image != null) {
            Glide.with(requireContext()).load(activity.image).into(mBinding.ivActivity)
        }
    }

    private fun openMap() {
        val uri = Uri.parse("geo:0,0?q=${mActivity.location?.latitude},${mActivity.location?.longitude}(${mActivity.title})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }

    private fun showDate() {
        mBinding.tvDate.visibility = View.VISIBLE
        mBinding.ivDate.visibility = View.VISIBLE
    }
}