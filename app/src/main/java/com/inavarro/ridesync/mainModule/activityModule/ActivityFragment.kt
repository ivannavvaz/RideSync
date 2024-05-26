package com.inavarro.ridesync.mainModule.activityModule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Activity
import com.inavarro.ridesync.databinding.FragmentActivityBinding
import com.inavarro.ridesync.mainModule.MainActivity
import java.text.SimpleDateFormat
import java.util.Locale

class ActivityFragment : Fragment() {

    private lateinit var mBinding: FragmentActivityBinding

    private lateinit var mActivity: Activity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentActivityBinding.inflate(inflater, container, false)

        setupActivityFragment()

        setupToolBar()

        setupActivity()

        mBinding.ivMap.setOnClickListener {
            openMap()
        }

        return mBinding.root
    }

    private fun setupActivityFragment(){
        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolbar)

        mBinding.toolbar.title = ""
        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupActivity(){
        val idActivity = arguments?.getString("idActivity")

        val activityRef = FirebaseFirestore.getInstance().collection("activities").document(idActivity!!)

        activityRef.get().addOnSuccessListener { document ->
            val activity = document.toObject(Activity::class.java)
            mActivity = activity!!

            mBinding.tvTitle.text = activity.title
            mBinding.tvDescription.text = activity.description
            mBinding.tvAddress.text = activity.address

            when (activity.type) {
                "route" -> mBinding.tvType.text = "Ruta"
                "restaurant" -> mBinding.tvType.text = "Restaurante"
                "meeting" -> mBinding.tvType.text = "Reunión"
                "career" -> mBinding.tvType.text = "Carrera"
                "event" -> mBinding.tvType.text = "Evento"
                "other" -> mBinding.tvType.text = "Otro"
            }

            if (activity.date != null) {
                val date = activity.date.toDate()
                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                mBinding.tvDate.text = dateFormat.format(date)
            } else {
                mBinding.clDate.visibility = View.GONE
            }

            Glide.with(requireContext())
                .load(activity.image)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(mBinding.ivImage)
        }
    }

    private fun openMap(){
        val uri = Uri.parse("geo:0,0?q=${mActivity.location?.latitude},${mActivity.location?.longitude}(${mActivity.title})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}