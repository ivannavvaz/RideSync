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
import androidx.appcompat.app.AlertDialog
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

    private lateinit var mGroup: Group

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
        // Get current user to check if it is premium
        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .document(FirebaseAuth.getInstance().currentUser!!.uid)

        query.get().addOnSuccessListener { document ->
            val user = document.toObject(User::class.java)

            // Show private group switch if user is premium
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
            hideKeyboard()

            if (validateGroupChange()) {
                // Show dialog to confirm discarding changes
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
            hideKeyboard()

            if (validateGroupChange()) {
                // Show dialog to confirm saving changes
                AlertDialog.Builder(requireContext())
                    .setTitle("Guardar cambios")
                    .setMessage("¿Estás seguro de que quieres guardar los cambios?")
                    .setPositiveButton("Guardar") { _, _ ->
                        val groupName = mBinding.etGroupName.text.toString()
                        val groupDescription = mBinding.etGroupDescription.text.toString()
                        val groupPrivate = mBinding.swPrivateGroup.isChecked

                        if (validateGroupName(groupName) && validateGroupDescription(groupDescription)) {
                            updateGroup(groupName, groupDescription, mPhotoGroupUri, groupPrivate)
                        }
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            } else {
                findNavController().navigateUp()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = requireActivity().getSystemService(AppCompatActivity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(mBinding.root.windowToken, 0)
    }

    private fun setupGroup() {
        // Get the group ID from the arguments
        val idGroup = arguments?.getString("idGroup")

        // Get the group details from Firestore
        val groupRef = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(idGroup!!)

        groupRef.get().addOnSuccessListener { document ->
            mGroup = document.toObject(Group::class.java)!!

            mBinding.etGroupName.setText(mGroup.name)
            mBinding.etGroupDescription.setText(mGroup.description)
            mBinding.swPrivateGroup.isChecked = mGroup.private!!

            if (mGroup.photo != null) {
                loadPhoto(mGroup.photo!!.toUri())
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

    private fun validateGroupChange(): Boolean {
        // Check if any group details have changed
        return !(mGroup.name == mBinding.etGroupName.text.toString() &&
                mGroup.description == mBinding.etGroupDescription.text.toString() &&
                mGroup.private == mBinding.swPrivateGroup.isChecked &&
                !mPhotoGroupChanged)
    }

    private fun validateGroupName(groupName: String): Boolean {
        val maxLength = 20

        // Validate group name
        return when {
            // Check if group name is empty
            groupName.isEmpty() -> {
                Snackbar.make(mBinding.root, "El nombre del grupo no puede estar vacío", Snackbar.LENGTH_SHORT).show()
                false
            }

            // Check if group name exceeds the maximum length
            groupName.length > maxLength -> {
                Snackbar.make(mBinding.root, "El nombre del grupo no puede exceder los $maxLength caracteres", Snackbar.LENGTH_SHORT).show()
                false
            }

            // Check if group name contains special characters
            !groupName.matches(Regex("^[a-zA-Z0-9]+$")) -> {
                Snackbar.make(mBinding.root, "El nombre del grupo solo puede contener letras y números", Snackbar.LENGTH_SHORT).show()
                false
            }

            else -> true
        }
    }

    private fun validateGroupDescription(description: String): Boolean {
        val maxLength = 150

        // Validate group description
        return when {
            // Check if group description is empty
            description.isEmpty() -> {
                Snackbar.make(mBinding.root, "La descripción del grupo no puede estar vacía", Snackbar.LENGTH_SHORT).show()
                false
            }

            // Check if group description exceeds the maximum length
            description.length > maxLength -> {
                Snackbar.make(mBinding.root, "La descripción del grupo no puede exceder los $maxLength caracteres", Snackbar.LENGTH_SHORT).show()
                false
            }

            else -> true
        }
    }

    private fun updateGroup(groupName: String, groupDescription: String, photoUri: Uri?, groupPrivate: Boolean) {
        // Get the group ID from the arguments
        val idGroup = arguments?.getString("idGroup")

        // Check if the group photo has changed
        if (mPhotoGroupChanged && photoUri != null) {
            uploadPhotoGroup(idGroup!!, groupName, groupDescription, photoUri, groupPrivate)
        } else {
            updateGroupDetails(idGroup!!, groupName, groupDescription, null, groupPrivate)
        }
    }

    private fun uploadPhotoGroup(idGroup: String, groupName: String, groupDescription: String, photoUri: Uri, groupPrivate: Boolean) {
        // Upload the group photo to Firebase Storage
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
        // Update the group details in Firestore
        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .document(idGroup)

        // Create a map with the group data
        val groupData = mutableMapOf(
            "name" to groupName,
            "description" to groupDescription,
            "private" to groupPrivate
        )

        // Add the group photo to the group data
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