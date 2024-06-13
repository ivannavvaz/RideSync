package com.inavarro.ridesync.mainModule.premiumModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentPremiumBinding

class PremiumFragment : Fragment() {

    private lateinit var mBinding: FragmentPremiumBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentPremiumBinding.inflate(inflater, container, false)

        setupPremiumFragment()

        setupToolBar()

        mBinding.btnBuyPremium.setOnClickListener {
            getPremium()
        }

        mBinding.btnCancelPremium.setOnClickListener {
            cancelPremium()
        }

        return mBinding.root
    }

    private fun setupPremiumFragment() {

        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)

        query.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val user = document.toObject(User::class.java)

                if (user?.premium!!) {
                    mBinding.btnBuyPremium.visibility = View.GONE
                    mBinding.btnCancelPremium.visibility = View.VISIBLE
                } else {
                    mBinding.btnBuyPremium.visibility = View.VISIBLE
                    mBinding.btnCancelPremium.visibility = View.GONE
                }
            }
        }
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolbar)

        mBinding.toolbar.title = "Premium"
        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun getPremium() {
        // change premium in firestore
        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .update("premium", true)

        query.addOnSuccessListener {
            mBinding.btnBuyPremium.visibility = View.GONE
            mBinding.btnCancelPremium.visibility = View.VISIBLE
        }
    }

    private fun cancelPremium() {
        // change premium in firestore
        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.uid!!)
            .update("premium", false)

        query.addOnSuccessListener {
            mBinding.btnBuyPremium.visibility = View.VISIBLE
            mBinding.btnCancelPremium.visibility = View.GONE
        }
    }
}