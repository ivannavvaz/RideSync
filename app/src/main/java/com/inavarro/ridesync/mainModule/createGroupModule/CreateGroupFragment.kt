package com.inavarro.ridesync.mainModule.createGroupModule

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentCreateGroupBinding
import com.inavarro.ridesync.mainModule.MainActivity

class CreateGroupFragment : Fragment() {

    private lateinit var mBinding: FragmentCreateGroupBinding

    private var mAddedUsersIdList: ArrayList<String> = ArrayList()

    private var mPhotoUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentCreateGroupBinding.inflate(layoutInflater)

        setupCreateGroupFragment()

        setupToolBar()

        setupAddedUsersList()

        setupTextFields()

        mBinding.btnEditPhotoGroup.setOnClickListener {
            openGallery()
        }

        mBinding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_createGroupFragment_to_GroupsFragment)
        }

        mBinding.btnAccept.setOnClickListener {
            createGroup()
        }

        return mBinding.root
    }

    private fun setupCreateGroupFragment() {
        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)

        query.get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java)

            if (user?.premium == true) {
                mBinding.llPrivateGroup.visibility = View.VISIBLE
                mBinding.ivInfo.visibility = View.GONE
                mBinding.tvInfo.visibility = View.GONE
            }
        }
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        mBinding.toolBar.title = "Crear grupo"
        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupAddedUsersList() {
        // Get the list of users added to the group
        val bundle = arguments
        mAddedUsersIdList = bundle!!.getStringArrayList("usersIdList") as ArrayList<String>
        mAddedUsersIdList.add(FirebaseAuth.getInstance().currentUser!!.uid)
    }

    private fun setupTextFields() {
        with(mBinding) {
            etGroupName.addTextChangedListener {
                validateFields(tilGroupName)
            }
            etGroupDescription.addTextChangedListener {
                validateFields(tilGroupDescription)
            }
        }
    }

    private fun validateFields(vararg textFields: TextInputLayout): Boolean {
        var isValid = true

        for (textField in textFields) {
            if (textField.editText?.text.toString().isEmpty()) {
                textField.error = "Campo requerido."
                isValid = false
            } else {
                textField.error = null
            }
        }

        return isValid
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startForResult.launch(intent)
    }

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                if ((result.data != null)) {
                    loadPhoto(result.data?.data)
                }
            }
        }

    private fun loadPhoto(photoUri: Uri?) {
        if (photoUri != null) {
            mPhotoUri = photoUri

            Glide.with(requireContext())
                .load(photoUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(mBinding.ivPhotoGroup)
        }
    }

    private fun validateGroupName(groupName: String): Boolean {
        val maxLength = 20

        // Validate the group name
        return when {
            // If the group name is longer than the maximum length, show a message
            groupName.length > maxLength -> {
                Snackbar.make(
                    mBinding.root,
                    "El nombre del grupo no puede exceder los $maxLength caracteres",
                    Snackbar.LENGTH_SHORT
                ).show()
                false
            }

            // If the group name contains special characters, show a message
            !groupName.matches(Regex("^[a-zA-Z0-9]+$")) -> {
                Snackbar.make(
                    mBinding.root,
                    "El nombre del grupo solo puede contener letras y números",
                    Snackbar.LENGTH_SHORT
                ).show()
                false
            }

            else -> true
        }
    }

    private fun validateGroupDescription(description: String): Boolean {
        val maxLength = 150

        // Validate the group description
        return when {
            // If the group description is longer than the maximum length, show a message
            description.length > maxLength -> {
                Snackbar.make(
                    mBinding.root,
                    "La descripción del grupo no puede exceder los $maxLength caracteres",
                    Snackbar.LENGTH_SHORT
                ).show()
                false
            }

            else -> true
        }
    }

    private fun createGroup() {

        val groupName = mBinding.etGroupName.text.toString().trim()
        val description = mBinding.etGroupDescription.text.toString().trim()
        val isPrivate = mBinding.swPrivateGroup.isChecked

        if (validateFields(mBinding.tilGroupName, mBinding.tilGroupDescription)) {
            // Validate the group name and description
            if (validateGroupName(groupName) && validateGroupDescription(description)) {
                // Create the group in Firestore
                val groupRef = FirebaseFirestore.getInstance().collection("groups").document()

                val group = Group(
                    groupRef.id,
                    groupName,
                    description,
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    mAddedUsersIdList,
                    null,
                    null,
                    null,
                    isPrivate,
                )

                groupRef.set(group)
                    .addOnSuccessListener {
                        updateUsersGroups(groupRef.id)
                        uploadPhotoGroup(groupRef.id)

                        Toast.makeText(
                            requireContext(),
                            "Grupo creado correctamente",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                        findNavController().navigate(R.id.action_createGroupFragment_to_GroupsFragment)
                    }
                    .addOnFailureListener {
                        Snackbar.make(
                            mBinding.root,
                            "Error al crear el grupo",
                            Snackbar.LENGTH_SHORT
                        )
                            .show()
                    }
            }
        } else {
            Snackbar.make(
                mBinding.root,
                "Completa todos los campos",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateUsersGroups(groupId: String) {
        // Add the group to the list of groups of each user
        mAddedUsersIdList.forEach { userId ->
            FirebaseFirestore.getInstance().collection("users").document(userId)
                .update("groups", FieldValue.arrayUnion(groupId))
        }
    }

    private fun uploadPhotoGroup(groupRefId: String) {
        // Upload the group photo to Firebase Storage
        if (mPhotoUri != null) {
            val storageRef = FirebaseStorage
                .getInstance()
                .reference
                .child("groupPhotos")
                .child(groupRefId)

            storageRef.putFile(mPhotoUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        FirebaseFirestore.getInstance().collection("groups").document(groupRefId)
                            .update("photo", uri.toString())
                    }
                }
        }
    }
}