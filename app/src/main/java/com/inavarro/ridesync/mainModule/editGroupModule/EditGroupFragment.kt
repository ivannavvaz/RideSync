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
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
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

        setupToolBar()

        setupGroup()

        mBinding.btnEditPhotoGroup.setOnClickListener {
            openGallery()
        }

        mBinding.btnCancel.setOnClickListener {
            findNavController().navigateUp()
        }

        mBinding.btnAccept.setOnClickListener {
            val groupName = mBinding.etGroupName.text.toString()
            val groupDescription = mBinding.etGroupDescription.text.toString()

            if (validateGroupName(groupName) && validateGroupDescription(groupDescription)) {
                updateGroup(groupName, groupDescription, mPhotoGroupUri)
            }
        }

        return mBinding.root
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        mBinding.toolBar.title = "Editar grupo"
        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
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

            if (group?.photo != null) {
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
                Toast.makeText(requireContext(), "El nombre del grupo no puede estar vacío", Toast.LENGTH_SHORT).show()
                false
            }
            groupName.length > maxLength -> {
                Toast.makeText(requireContext(), "El nombre del grupo no puede exceder los $maxLength caracteres", Toast.LENGTH_SHORT).show()
                false
            }
            !groupName.matches(Regex("^[a-zA-Z0-9]+$")) -> {
                Toast.makeText(requireContext(), "El nombre del grupo solo puede contener letras y números", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun validateGroupDescription(description: String): Boolean {
        val maxLength = 150

        return when {
            description.isEmpty() -> {
                Toast.makeText(requireContext(), "La descripción del grupo no puede estar vacía", Toast.LENGTH_SHORT).show()
                false
            }
            description.length > maxLength -> {
                Toast.makeText(requireContext(), "La descripción del grupo no puede exceder los $maxLength caracteres", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun updateGroup(groupName: String, groupDescription: String, photoUri: Uri?) {
        val idGroup = arguments?.getString("idGroup")

        if (mPhotoGroupChanged && photoUri != null) {
            uploadPhotoGroup(idGroup!!, groupName, groupDescription, photoUri)
        } else {
            updateGroupDetails(idGroup!!, groupName, groupDescription, null)
        }
    }

    private fun uploadPhotoGroup(idGroup: String, groupName: String, groupDescription: String, photoUri: Uri) {
        val storageRef = FirebaseStorage
            .getInstance()
            .reference
            .child("groupPhotos")
            .child(idGroup)

        lifecycleScope.launch {
            storageRef.putFile(photoUri)
                .addOnSuccessListener { taskSnapshot ->
                    taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                        updateGroupDetails(idGroup, groupName, groupDescription, uri)
                    }
                }
                .await()
        }
    }

    private fun updateGroupDetails(idGroup: String, groupName: String, groupDescription: String, photoUri: Uri?) {
        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(idGroup)

        val groupData = mutableMapOf(
            "name" to groupName,
            "description" to groupDescription
        )

        if (photoUri != null) {
            groupData["photo"] = photoUri.toString()
        }

        query.update(groupData as Map<String, Any>)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Grupo actualizado correctamente",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigateUp()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Error al actualizar el grupo", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}