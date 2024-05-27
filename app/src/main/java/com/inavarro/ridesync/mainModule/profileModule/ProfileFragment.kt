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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
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

class ProfileFragment : Fragment(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var mBinding: FragmentProfileBinding

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<Photo, PhotoHolder>

    private lateinit var mStorageReference: StorageReference

    private lateinit var mFirestoreReference: CollectionReference

    inner class PhotoHolder(view: View):
        RecyclerView.ViewHolder(view) {
            val binding = ItemPhotoBinding.bind(view)

            fun setListener(photo: Photo) {
                binding.root.setOnLongClickListener {
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

                mBinding.progressBar.visibility = View.GONE
                emptyList()
            }

            override fun onError(e: FirebaseFirestoreException) {
                super.onError(e)

                Snackbar.make(mBinding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }

        mBinding.rvMyTrips.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
        }
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
        mBinding.progressBar.visibility = View.VISIBLE
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
            R.id.nav_edit_profile -> {
                findNavController().navigate(R.id.action_profileFragment_to_editProfileFragment)
            }
            R.id.nav_share -> {
                openShare()
            }
            R.id.nav_about -> {
                findNavController().navigate(R.id.action_profileFragment_to_aboutFragment)
            }
            R.id.nav_logout -> {
                signOut()
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

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startForResult.launch(intent)
    }

    private fun openShare() {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, "¡Mira mi perfil en RideSync! ${mAuth.currentUser?.displayName}")
        startActivity(Intent.createChooser(intent, "Compartir"))
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
                    Toast.makeText(
                        requireContext(),
                        "Imagen publicada",
                        Toast.LENGTH_SHORT).show()

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

                Toast.makeText(
                    requireContext(),
                    "Imagen eliminada",
                    Toast.LENGTH_SHORT).show()
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