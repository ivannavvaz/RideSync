package com.inavarro.ridesync.mainModule.editProfileModule

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.databinding.FragmentEditProfileBinding
import com.inavarro.ridesync.mainModule.MainActivity
import java.util.Locale

class EditProfileFragment : Fragment() {

    private lateinit var mBinding: FragmentEditProfileBinding

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mFullName: String

    private var mPhotoProfileUri: Uri? = null

    private var mPhotoProfileChanged = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentEditProfileBinding.inflate(inflater, container, false)

        mAuth = FirebaseAuth.getInstance()

        (activity as? MainActivity)?.hideBottomNav()

        setupProfile()

        mBinding.ivBack.setOnClickListener {
            goBack()
        }

        mBinding.btnEditImageProfile.setOnClickListener {
            openGallery()
        }

        mBinding.btnCancel.setOnClickListener {
            goBack()
        }

        mBinding.btnAccept.setOnClickListener {
            updateProfile()
        }

        return mBinding.root
    }

    private fun setupProfile() {
        getFullName()
        loadPhotoProfile(getPhotoUrl())
    }

    private fun loadPhotoProfile(photoUrl: Uri?) {
        if (photoUrl != null) {
            mPhotoProfileUri = photoUrl

            Glide.with(requireContext())
                .load(photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(mBinding.ivProfile)
        }
    }

    private fun getFullName() {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(mAuth.currentUser?.uid!!)
        userRef.get().addOnSuccessListener { document ->
            if (document != null) {
                mFullName = document.getString("fullName")!!.split(" ").joinToString(" ") { it.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT) else it.toString() } }

                mBinding.etFullName.setText(mFullName)
            }
        }
    }

    private fun getPhotoUrl(): Uri? {
        val user = mAuth.currentUser
        return if (user?.photoUrl == null) {
            null
        } else {
            user.photoUrl
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startForResult.launch(intent)
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            if ((result.data != null)) {
                mPhotoProfileChanged = true
                loadPhotoProfile(result.data?.data)
            }
        }
    }

    private fun updateProfile() {
        if (validateFullName()) {
            val user = mAuth.currentUser

            // Update user in Firestore
            val userRef = FirebaseFirestore.getInstance().collection("users").document(user?.uid!!)

            if (mFullName != mBinding.etFullName.text.toString().trim().lowercase()) {
                userRef.update("fullName", mBinding.etFullName.text.toString().trim().lowercase())
            }

            if (mPhotoProfileChanged) {
                userRef.update("image", mPhotoProfileUri.toString())
            }

            // Update user in Firebase Auth
            if (mPhotoProfileChanged) {
                val profileUpdates = userProfileChangeRequest {
                    if (mPhotoProfileUri != null) {
                        photoUri = mPhotoProfileUri
                    }
                }

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            goBack()
                        }
                    }
            } else {
                goBack()
            }
        } else {
            Toast.makeText(this.context, "Nombre completo inválido.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun validateFullName(): Boolean {
        val userName = mBinding.etFullName.text.toString().trim()

        return if (userName.isEmpty()) {
            false
        } else {
            userName.matches(Regex("^[a-zA-Z ]+\$"))
        }
    }

    private fun goBack() {
        findNavController().navigateUp()
        (activity as? MainActivity)?.showBottomNav()
    }
}