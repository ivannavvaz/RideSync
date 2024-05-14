package com.inavarro.ridesync.mainModule.profileModule

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.view.GravityCompat
import androidx.databinding.adapters.ViewGroupBindingAdapter.setListener
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.NavigationUI.setupActionBarWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.inavarro.ridesync.R
import com.inavarro.ridesync.authModule.loginModule.LoginActivity
import com.inavarro.ridesync.common.entities.Photo
import com.inavarro.ridesync.databinding.FragmentProfileBinding
import com.inavarro.ridesync.databinding.ItemPhotoBinding
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.groupsModule.searchGroups.SearchGroupsFragment
import java.util.Locale

class ProfileFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mBinding: FragmentProfileBinding

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mStorageReference: StorageReference
    private lateinit var mFirestoreReference: CollectionReference

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<Photo, PhotoHolder>

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    inner class PhotoHolder(view: View):
        RecyclerView.ViewHolder(view) {
            val binding = ItemPhotoBinding.bind(view)

            fun setListener(photo: Photo) {
                binding.root.setOnLongClickListener {
                    onLongClick(photo)
                    true
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentProfileBinding.inflate(layoutInflater)

        mAuth = FirebaseAuth.getInstance()

        setupProfileFragment()

        setupNavigationView()

        setupProfile()

        mBinding.fabUploadImage.setOnClickListener {
            openGallery()
        }

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = GridLayoutManager(context, 2)

        mStorageReference = FirebaseStorage.getInstance()
            .reference.child("photos")
            .child(FirebaseAuth.getInstance().currentUser!!.uid)

        mFirestoreReference = FirebaseFirestore.getInstance()
            .collection("users")
            .document(mAuth.currentUser?.uid!!)
            .collection("photos")

        val options = FirestoreRecyclerOptions.Builder<Photo>().setQuery(mFirestoreReference, Photo::class.java).build()

        mFirebaseAdapter = object : FirestoreRecyclerAdapter<Photo, PhotoHolder>(options) {

            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext).inflate(R.layout.item_photo, parent, false)

                return PhotoHolder(view)
            }

            override fun onBindViewHolder(holder: PhotoHolder, position: Int, model: Photo) {
                val photo = getItem(position)

                with(holder) {
                    setListener(photo)

                    Glide.with(mContext)
                        .load(photo.photoUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(binding.ivPhoto)
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChanged() {
                super.onDataChanged()

                notifyDataSetChanged()

                emptyList()
            }

            override fun onError(e: FirebaseFirestoreException) {
                super.onError(e)

                Toast.makeText(mContext, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.rvMyTrips.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
        }

        emptyList()
    }

    override fun onStart() {
        super.onStart()

        mFirebaseAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()

        mFirebaseAdapter.stopListening()
        mBinding.drawerLayout.closeDrawer(GravityCompat.START)
    }

    private fun setupProfileFragment() {
        (activity as? MainActivity)?.hideFragmentContainerViewActivity()
        (activity as? MainActivity)?.showBottomNav()
    }

    private fun setupNavigationView() {
        mBinding.navigationView.setNavigationItemSelectedListener(this)

        ActionBarDrawerToggle(requireActivity(),
            mBinding.drawerLayout,
            mBinding.toolbar,
            R.string.open_drawer,
            R.string.close_drawer).apply {
            mBinding.drawerLayout.addDrawerListener(this)
            syncState()
        }

        mBinding.toolbar.setNavigationOnClickListener {
            mBinding.drawerLayout.openDrawer(GravityCompat.START)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_logout -> {
                signOut()
            }
            R.id.nav_edit_profile -> {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            }
        }

        mBinding.drawerLayout.closeDrawer(GravityCompat.START)

        return false
    }

    private fun setupProfile() {
        mBinding.tvEmail.text = getEmail()
        mBinding.tvUserName.text = getUserName()

        val photoUrl = getPhotoUrl()
        if (photoUrl != null) {
            loadPhotoProfile(photoUrl)
        }
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
            // First letter in uppercase
            user.displayName
            //.split(" ")?.joinToString(" ") { it.lowercase(Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() } }
        } else "anonymous"
    }

    private fun getPhotoUrl(): Uri? {
        val user = mAuth.currentUser
        return user?.photoUrl
    }

    private fun loadPhotoProfile(photoUrl: Uri?) {
        Glide.with(requireContext())
            .load(photoUrl)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .circleCrop()
            .into(mBinding.ivProfile)
    }

    private fun onLongClick(photo: Photo) {
        val builder = AlertDialog.Builder(requireContext())

        val alertDialog = builder.create()
        alertDialog.show()

        builder.setTitle("Confirmacion")
        builder.setMessage("¿Quieres eliminar la foto?")
        builder.setPositiveButton("Eliminar") { _, _ ->
            deletePhoto(photo)
            alertDialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { _, _ ->
            alertDialog.dismiss()
        }

        alertDialog.dismiss()
        builder.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startForResult.launch(intent)
    }

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            if ((result.data != null)) {
                uploadImage(result.data?.data)
            }
        }
    }

    private fun uploadImage(photoSelectUri: Uri?) {
        val id = mFirestoreReference.document().id

        val storageReference = mStorageReference.child(id)

        if (photoSelectUri != null) {
            storageReference.putFile(photoSelectUri)
                .addOnSuccessListener {
                    Snackbar.make(
                        mBinding.root,
                        "Imagen publicada",
                        Snackbar.LENGTH_SHORT).show()

                    // Save image in firestore
                    it.storage.downloadUrl.addOnSuccessListener { uri ->
                        savePhoto(id, uri.toString())
                    }
                }
                .addOnFailureListener {
                    Snackbar.make(
                        mBinding.root,
                        "No se ha podido subir, inténtalo más tarde",
                        Snackbar.LENGTH_SHORT).show()
                }
        }
    }

    private fun savePhoto(id: String, uri: String) {
        val photo = Photo(id, uri)
        mFirestoreReference.document(id).set(photo)
    }

    private fun deletePhoto(photo: Photo) {
        val photoRef = mStorageReference.child(photo.id)

        photoRef.delete()
            .addOnSuccessListener {
                mFirestoreReference.document(photo.id).delete()
            }
            .addOnFailureListener {
                Snackbar.make(
                    mBinding.root,
                    "No se ha podido borrar, inténtalo más tarde",
                    Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun emptyList() {
        mBinding.ivEmptyList.visibility = if (mFirebaseAdapter.itemCount == 0) View.VISIBLE else View.GONE
        mBinding.tvEmptyList.visibility = if (mFirebaseAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun signOut() {
        mAuth.signOut()
        startActivity(Intent(requireContext(), LoginActivity::class.java))
        requireActivity().finish()
    }
}