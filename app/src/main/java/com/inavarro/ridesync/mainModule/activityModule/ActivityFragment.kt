package com.inavarro.ridesync.mainModule.activityModule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Activity
import com.inavarro.ridesync.databinding.FragmentActivityBinding

class ActivityFragment : Fragment() {

    private lateinit var mBinding: FragmentActivityBinding

    private lateinit var mActivity: Activity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentActivityBinding.inflate(layoutInflater)

        // Get id activity
        val id = arguments?.getString("id")
        getActivities(id)

        mBinding.background.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction().remove(this).commit()
        }

        mBinding.ivMap.setOnClickListener {
            openMap()
        }

        return mBinding.root
    }

    private fun getActivities(Id: String?) {
        // Get activities from Firestore
        val db = FirebaseFirestore.getInstance()
          db.collection("activities")
                .document(Id!!)
                .get()
                .addOnSuccessListener { document ->
                 if (document.exists()) {
                      mActivity = document.toObject(Activity::class.java)!!
                      setActivity(mActivity)
                 } else {
                      Toast.makeText(requireContext(), "No such activity", Toast.LENGTH_SHORT).show()
                 }
                }
                .addOnFailureListener { exception ->
                 Toast.makeText(requireContext(), "Error getting activity: $exception", Toast.LENGTH_SHORT).show()
                }
    }

    private fun setActivity(activity: Activity) {
        mBinding.tvTitle.text = activity.title
        mBinding.tvDescription.text = activity.description
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
}