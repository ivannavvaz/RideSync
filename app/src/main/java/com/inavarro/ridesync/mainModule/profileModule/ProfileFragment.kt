package com.inavarro.ridesync.mainModule.profileModule

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.inavarro.ridesync.R
import com.inavarro.ridesync.authModule.loginModule.LoginActivity
import com.inavarro.ridesync.databinding.FragmentProfileBinding
import com.inavarro.ridesync.mainModule.MainActivity

class ProfileFragment : Fragment() {

    private lateinit var mBinding: FragmentProfileBinding
    private lateinit var mAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentProfileBinding.inflate(layoutInflater)

        mAuth = FirebaseAuth.getInstance()

        mBinding.tvEmail.text = getEmail()
        mBinding.tvUserName.text = getUserName()

        mBinding.btnLogOut.setOnClickListener {
            signOut()
        }

        return mBinding.root
    }

    private fun signOut() {
        mAuth.signOut()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }

    private fun getEmail(): String? {
        val user = mAuth.currentUser
        return if (user != null) {
            user.email
        } else "anonymous"
    }

    private fun getUserName(): String? {
        val user = mAuth.currentUser
        return if (user != null) {
            user.displayName
        } else "anonymous"
    }

    private fun getPhotoUrl(): String? {
        val user = mAuth.currentUser
        return user?.photoUrl?.toString()
    }
}