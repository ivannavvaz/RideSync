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
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentEditProfileBinding
import com.inavarro.ridesync.mainModule.MainActivity
import java.util.Locale

class EditProfileFragment : Fragment() {

    private lateinit var mBinding: FragmentEditProfileBinding

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mUser: User

    private var mPhotoProfileUri: Uri? = null

    private var mPhotoProfileChanged = false

    private lateinit var mStorageReference: StorageReference

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentEditProfileBinding.inflate(inflater, container, false)

        mAuth = FirebaseAuth.getInstance()

        setupEditProfileFragment()

        setupToolBar()

        setupProfile()

        mBinding.btnEditImageProfile.setOnClickListener {
            openGallery()
        }

        mStorageReference = FirebaseStorage.getInstance().reference.child("profilePhotos")

        return mBinding.root
    }

    private fun setupEditProfileFragment(){
        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolbar)

        mBinding.toolbar.title = "Editar perfil"
        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            val imm = requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mBinding.root.windowToken, 0)

            if (validateUserChanges()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Descartar cambios")
                    .setMessage("¿Estás seguro de que quieres descartar los cambios?")
                    .setPositiveButton("Descartar") { _, _ ->
                        findNavController().navigateUp()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                findNavController().navigateUp()
            }
        }

        mBinding.ivCheck.setOnClickListener {
            val imm = requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mBinding.root.windowToken, 0)

            if (validateUserChanges()) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Guardar cambios")
                    .setMessage("¿Estás seguro de que quieres guardar los cambios?")
                    .setPositiveButton("Guardar") { _, _ ->
                        updateProfile()
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun setupProfile() {
        val userRef = FirebaseFirestore.getInstance().collection("users").document(mAuth.currentUser?.uid!!)

        userRef.get().addOnSuccessListener { document ->
            mUser = document.toObject(User::class.java)!!

            mBinding.etFullName.setText(mUser.fullName)
            mBinding.swPublicProfile.isChecked = mUser.publicProfile!!

            if (mUser.profilePhoto != null) {
                loadPhotoProfile(mUser.profilePhoto!!.toUri())
            }

        }
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

    private fun validateUserChanges(): Boolean {
        return !(mUser.fullName == mBinding.etFullName.text.toString().trim().lowercase() &&
                mUser.publicProfile == mBinding.swPublicProfile.isChecked &&
                !mPhotoProfileChanged)
    }

    private fun updateProfile() {
        if (validateFullName()) {
            val user = mAuth.currentUser

            // Update user in Firestore
            val userRef = FirebaseFirestore.getInstance().collection("users").document(user?.uid!!)

            if (mUser.fullName != mBinding.etFullName.text.toString().trim().lowercase()) {
                userRef.update("fullName", mBinding.etFullName.text.toString().trim().lowercase())
            }

            if (mUser.publicProfile != mBinding.swPublicProfile.isChecked) {
                userRef.update("publicProfile", mBinding.swPublicProfile.isChecked)
            }

            if (mPhotoProfileChanged) {
                // Up image in Firebase Storage
                val photoRef = mStorageReference.child(user.uid)
                photoRef.putFile(mPhotoProfileUri!!)
                    .addOnSuccessListener { taskSnapshot ->
                        taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                            userRef.update("profilePhoto", uri)
                        }
                    }

                // Update user in Firebase Auth
                val profileUpdates = userProfileChangeRequest {
                    photoUri = mPhotoProfileUri
                }

                user.updateProfile(profileUpdates)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            findNavController().navigateUp()

                            Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                findNavController().navigateUp()

                Toast.makeText(requireContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show()
            }
        } else {
            Snackbar.make(mBinding.root, "Nombre completo inválido.", Snackbar.LENGTH_SHORT).show()
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
}