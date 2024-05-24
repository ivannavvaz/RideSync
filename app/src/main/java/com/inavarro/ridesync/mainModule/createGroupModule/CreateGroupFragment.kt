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
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
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

        mBinding.btnEditPhotoGroup.setOnClickListener {
            openGallery()
        }

        mBinding.btnCancel.setOnClickListener {
            findNavController().navigate(R.id.action_createGroupFragment_to_GroupsFragment)
        }

        mBinding.btnAccept.setOnClickListener {
            val groupName = mBinding.etGroupName.text.toString().trim()
            val description = mBinding.etGroupDescription.text.toString().trim()

            if (validateGroupName(groupName) && validateGroupDescription(description)) {
                createGroup(groupName, description, mBinding.swPrivateGroup.isChecked)
            }
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
                mBinding.swPrivateGroup.visibility = View.VISIBLE
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
        val bundle = arguments
        mAddedUsersIdList = bundle!!.getStringArrayList("usersIdList") as ArrayList<String>
        mAddedUsersIdList.add(FirebaseAuth.getInstance().currentUser!!.uid)

        Log.d("CreateGroupFragment", mAddedUsersIdList.toString())
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startForResult.launch(intent)
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
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

    private fun createGroup(groupName: String, description: String, isPrivate: Boolean) {

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
                uploadPhotoGroup(groupRef.id)

                Toast.makeText(requireContext(), "Grupo creado correctamente", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_createGroupFragment_to_GroupsFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al crear el grupo", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadPhotoGroup(groupRefId: String) {
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