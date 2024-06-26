package com.inavarro.ridesync.mainModule.viewProfileModule

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Photo
import com.inavarro.ridesync.databinding.FragmentViewProfileBinding
import com.inavarro.ridesync.databinding.ItemPhotoBinding
import java.util.Locale

class ViewProfileFragment : Fragment() {

    private lateinit var mBinding: FragmentViewProfileBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<Photo, ViewProfileFragment.PhotoHolder>

    private lateinit var mStorageReference: StorageReference

    private lateinit var mFirestoreReference: CollectionReference

    inner class PhotoHolder(view: View):
        RecyclerView.ViewHolder(view) {
        val binding = ItemPhotoBinding.bind(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentViewProfileBinding.inflate(layoutInflater)

        setupToolBar()

        setupProfile()

        return mBinding.root
    }

    override fun onStart() {
        super.onStart()

        if (this::mFirebaseAdapter.isInitialized) {
            mFirebaseAdapter.startListening()
        }
    }

    override fun onStop() {
        super.onStop()

        if (this::mFirebaseAdapter.isInitialized) {
            mFirebaseAdapter.stopListening()
        }
    }

    private fun setupToolBar() {
        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupRecyclerView() {
        mLayoutManager = GridLayoutManager(context, 2)

        // Storage
        mStorageReference = FirebaseStorage.getInstance()
            .reference.child("photos")
            .child(arguments?.getString("idUser")!!)

        // Firestore
        mFirestoreReference = FirebaseFirestore.getInstance()
            .collection("users")
            .document(arguments?.getString("idUser")!!)
            .collection("photos")

        val options = FirestoreRecyclerOptions.Builder<Photo>().setQuery(mFirestoreReference, Photo::class.java).build()

        mFirebaseAdapter = object : FirestoreRecyclerAdapter<Photo, ViewProfileFragment.PhotoHolder>(options) {

            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewProfileFragment.PhotoHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext).inflate(R.layout.item_photo, parent, false)

                return PhotoHolder(view)
            }

            override fun onBindViewHolder(holder: PhotoHolder, position: Int, model: Photo) {
                val photo = getItem(position)

                with(holder) {

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

        mFirebaseAdapter.startListening()
    }

    private fun setupProfile() {
        // Get user data
        val user = FirebaseFirestore.getInstance().collection("users").document(arguments?.getString("idUser")!!)

        user.get().addOnSuccessListener {
            if (it.exists()) {
                // Set user data
                val username = it.getString("username")
                val name = it.getString("fullName")?.split(" ")?.joinToString(" ") { it.lowercase(
                    Locale.ROOT).replaceFirstChar { if (it.isLowerCase()) it.titlecase(
                    Locale.ROOT) else it.toString() } }
                val photo = it.getString("profilePhoto")

                mBinding.tvUserName.text = username
                mBinding.tvFullname.text = name

                if (photo != null) {
                    Glide.with(requireContext())
                        .load(photo)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .circleCrop()
                        .into(mBinding.ivProfile)
                } else {
                    mBinding.ivProfile.setImageResource(R.drawable.ic_person)
                }

                // Check if user has public profile
                if (it.getBoolean("publicProfile") == false) {
                    mBinding.ivPrivateProfile.visibility = View.VISIBLE
                    mBinding.tvPrivateProfile.visibility = View.VISIBLE
                } else {
                    mBinding.ivPrivateProfile.visibility = View.GONE
                    mBinding.tvPrivateProfile.visibility = View.GONE
                    setupRecyclerView()
                }
            }
        }
    }

    private fun emptyList() {
        mBinding.ivEmptyList.visibility = if (mFirebaseAdapter.itemCount == 0) View.VISIBLE else View.GONE
        mBinding.tvEmptyList.visibility = if (mFirebaseAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }
}