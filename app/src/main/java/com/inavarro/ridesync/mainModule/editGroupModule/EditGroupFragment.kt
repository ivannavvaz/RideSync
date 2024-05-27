package com.inavarro.ridesync.mainModule.editGroupModule

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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentEditGroupBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class EditGroupFragment : Fragment() {

    private lateinit var mBinding: FragmentEditGroupBinding

    private var mPhotoGroupUri: Uri? = null

    private var mPhotoGroupChanged = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentEditGroupBinding.inflate(layoutInflater)

        setupEditGroupFragment()

        setupToolBar()

        setupGroup()

        mBinding.btnEditPhotoGroup.setOnClickListener {
            openGallery()
        }

        return mBinding.root
    }

    private fun setupEditGroupFragment() {
        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)

        query.get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java)

            if (user?.premium == true) {
                mBinding.llPrivateGroup.visibility = View.VISIBLE
            }
        }
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        mBinding.toolBar.title = "Editar grupo"
        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        mBinding.ivCheck.setOnClickListener {
            // Hide the keyboard
            val imm = requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(mBinding.root.windowToken, 0)

            val groupName = mBinding.etGroupName.text.toString()
            val groupDescription = mBinding.etGroupDescription.text.toString()
            val groupPrivate = mBinding.swPrivateGroup.isChecked

            if (validateGroupName(groupName) && validateGroupDescription(groupDescription)) {
                updateGroup(groupName, groupDescription, mPhotoGroupUri, groupPrivate)
            }
        }
    }

    private fun setupGroup() {
        val idGroup = arguments?.getString("idGroup")

        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(idGroup!!)

        query.get().addOnSuccessListener { document ->
            val group = document.toObject(Group::class.java)

            mBinding.etGroupName.setText(group?.name)
            mBinding.etGroupDescription.setText(group?.description)
            mBinding.swPrivateGroup.isChecked = group?.private!!

            if (group.photo != null) {
                loadPhoto(group.photo.toUri())
            }
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
                mPhotoGroupChanged = true
                loadPhoto(result.data?.data)
            }
        }
    }

    private fun loadPhoto(photoUri: Uri?) {
        if (photoUri != null) {
            mPhotoGroupUri = photoUri

            Glide.with(requireContext())
                .load(photoUri)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .circleCrop()
                .into(mBinding.ivPhotoGroup)
        }
    }

    private fun validateGroupName(groupName: String): Boolean {
        val maxLength = 20

        return when {
            groupName.isEmpty() -> {
                Snackbar.make(mBinding.root, "El nombre del grupo no puede estar vacío", Snackbar.LENGTH_SHORT).show()
                false
            }
            groupName.length > maxLength -> {
                Snackbar.make(mBinding.root, "El nombre del grupo no puede exceder los $maxLength caracteres", Snackbar.LENGTH_SHORT).show()
                false
            }
            !groupName.matches(Regex("^[a-zA-Z0-9]+$")) -> {
                Snackbar.make(mBinding.root, "El nombre del grupo solo puede contener letras y números", Snackbar.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun validateGroupDescription(description: String): Boolean {
        val maxLength = 150

        return when {
            description.isEmpty() -> {
                Snackbar.make(mBinding.root, "La descripción del grupo no puede estar vacía", Snackbar.LENGTH_SHORT).show()
                false
            }
            description.length > maxLength -> {
                Snackbar.make(mBinding.root, "La descripción del grupo no puede exceder los $maxLength caracteres", Snackbar.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun updateGroup(groupName: String, groupDescription: String, photoUri: Uri?, groupPrivate: Boolean) {
        val idGroup = arguments?.getString("idGroup")

        if (mPhotoGroupChanged && photoUri != null) {
            uploadPhotoGroup(idGroup!!, groupName, groupDescription, photoUri, groupPrivate)
        } else {
            updateGroupDetails(idGroup!!, groupName, groupDescription, null, groupPrivate)
        }
    }

    private fun uploadPhotoGroup(idGroup: String, groupName: String, groupDescription: String, photoUri: Uri, groupPrivate: Boolean) {
        val storageRef = FirebaseStorage
            .getInstance()
            .reference
            .child("groupPhotos")
            .child(idGroup)

        lifecycleScope.launch {
            storageRef.putFile(photoUri)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        updateGroupDetails(idGroup, groupName, groupDescription, uri, groupPrivate)
                    }
                }
                .await()
        }
    }

    private fun updateGroupDetails(idGroup: String, groupName: String, groupDescription: String, photoUri: Uri?, groupPrivate: Boolean) {
        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(idGroup)

        val groupData = mutableMapOf(
            "name" to groupName,
            "description" to groupDescription,
            "private" to groupPrivate
        )

        if (photoUri != null) {
            groupData["photo"] = photoUri.toString()
        }

        query.update(groupData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Grupo actualizado",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }.addOnFailureListener {
                Snackbar.make(mBinding.root, "Error al actualizar el grupo", Snackbar.LENGTH_SHORT)
                    .show()
            }
    }
}