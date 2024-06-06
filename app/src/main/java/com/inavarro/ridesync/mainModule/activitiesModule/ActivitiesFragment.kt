package com.inavarro.ridesync.mainModule.activitiesModule

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Activity
import com.inavarro.ridesync.databinding.FragmentActivitiesBinding
import com.inavarro.ridesync.databinding.ItemActivityBinding
import com.inavarro.ridesync.mainModule.MainActivity

class ActivitiesFragment : Fragment() {

    private lateinit var mBinding: FragmentActivitiesBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<Activity, ActivityHolder>

    private lateinit var mQuery: Query

    private lateinit var mSharedPreferences: SharedPreferences

    inner class ActivityHolder(view: View) : RecyclerView.ViewHolder(view){
        val binding = ItemActivityBinding.bind(view)

        fun setListener(activity: Activity){
            binding.root.setOnClickListener {
                openActivity(activity.id!!)
            }

            binding.btnActivity.setOnClickListener {
                openActivity(activity.id!!)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentActivitiesBinding.inflate(layoutInflater)

        setupActivitiesFragment()

        setupChipsClicks()

        mSharedPreferences = requireActivity().getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = GridLayoutManager(context, 2)

        // Select the chip that was selected before
        val chipSelected = mSharedPreferences.getInt("activitiesChipSelected", R.id.chipAll)

        // Query the activities depending on the selected chip
        when (chipSelected){
            R.id.chipAll -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
            }
            R.id.chipRoutes -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
                    .whereEqualTo("type", "route")
            }
            R.id.chipRestaurants -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
                    .whereEqualTo("type", "restaurant")
            }
            R.id.chipMettings -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
                    .whereEqualTo("type", "meeting")
            }
            R.id.chipCareer -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
                    .whereEqualTo("type", "career")
            }
            R.id.chipEvents -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
                    .whereEqualTo("type", "event")
            }
            R.id.chipOthers -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
                    .whereEqualTo("type", "other")
            }
            else -> {
                mQuery = FirebaseFirestore.getInstance().collection("activities")
            }
        }

        // Check if the query has items
        mQuery.get().addOnSuccessListener {
            val numItems = it.size()
            emptyList(numItems)
        }

        val options = FirestoreRecyclerOptions.Builder<Activity>()
            .setQuery(mQuery, Activity::class.java)
            .build()

        mFirebaseAdapter = object : FirestoreRecyclerAdapter<Activity, ActivityHolder>(options){

            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext).inflate(
                        R.layout.item_activity,
                        parent,
                        false
                )

                return ActivityHolder(view)
            }

            override fun onBindViewHolder(holder: ActivityHolder, position: Int, model: Activity) {
                val activity = getItem(position)

                with(holder){
                    setListener(activity)

                    binding.tvName.text = activity.title

                    val type = activity.type
                    when(type){
                        "route" -> binding.tvType.text = "Ruta"
                        "restaurant" -> binding.tvType.text = "Restaurante"
                        "meeting" -> binding.tvType.text = "Reunión"
                        "career" -> binding.tvType.text = "Carrera"
                        "event" -> binding.tvType.text = "Evento"
                        "other" -> binding.tvType.text = "Otro"
                    }

                    binding.tvCity.text = activity.city

                    if (activity.image != null){
                        Glide.with(mContext)
                            .load(activity.image)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.ivPhoto)
                    } else {
                        binding.ivPhoto.setImageResource(R.drawable.ic_no_photography)
                    }
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChanged() {
                super.onDataChanged()

                notifyDataSetChanged()

                mBinding.progressBar.visibility = View.GONE
            }

            override fun onError(e: FirebaseFirestoreException) {
                super.onError(e)

                Snackbar.make(mBinding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }

        mBinding.rvActivities.apply {
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
    }

    override fun onResume() {
        super.onResume()

        scrollToAndSelectSelectedChip()
    }

    private fun scrollToAndSelectSelectedChip() {
        val selectedChip = mBinding.chipGroup.findViewById<Chip>(mSharedPreferences.getInt("activitiesChipSelected", R.id.chipAll))

        selectedChip.isChecked = true

        mBinding.horizontalScrollView.post {
            // Calculate the scroll position of the selected chip
            val scrollX = selectedChip.left - (mBinding.horizontalScrollView.width / 2) + (selectedChip.width / 2)

            // Scroll to the selected chip
            mBinding.horizontalScrollView.smoothScrollTo(scrollX, 0)
        }
    }

    private fun setupActivitiesFragment(){
        (activity as MainActivity).showBottomNav()
        (activity as MainActivity).hideFragmentContainerViewActivity()

        mBinding.progressBar.visibility = View.VISIBLE
    }

    // Set the click listeners for the chips
    private fun setupChipsClicks() {
        mBinding.chipAll.setOnClickListener {
            mSharedPreferences.edit().putInt("activitiesChipSelected", R.id.chipAll).apply()

            val query = FirebaseFirestore.getInstance().collection("activities")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Activity>()
                .setQuery(query, Activity::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
        mBinding.chipRoutes.setOnClickListener {
            mSharedPreferences.edit().putInt("activitiesChipSelected", R.id.chipRoutes).apply()

            val query = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("type", "route")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Activity>()
                .setQuery(query, Activity::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
        mBinding.chipRestaurants.setOnClickListener {
            mSharedPreferences.edit().putInt("activitiesChipSelected", R.id.chipRestaurants).apply()

            val query = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("type", "restaurant")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Activity>()
                .setQuery(query, Activity::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
        mBinding.chipMettings.setOnClickListener {
            mSharedPreferences.edit().putInt("activitiesChipSelected", R.id.chipMettings).apply()

            val query = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("type", "meeting")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Activity>()
                .setQuery(query, Activity::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
        mBinding.chipCareer.setOnClickListener {
            mSharedPreferences.edit().putInt("activitiesChipSelected", R.id.chipCareer).apply()

            val query = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("type", "career")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Activity>()
                .setQuery(query, Activity::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
        mBinding.chipEvents.setOnClickListener {
            mSharedPreferences.edit().putInt("activitiesChipSelected",  R.id.chipEvents).apply()

            val query = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("type", "event")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Activity>()
                .setQuery(query, Activity::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
        mBinding.chipOthers.setOnClickListener {
            mSharedPreferences.edit().putInt("activitiesChipSelected", R.id.chipOthers).apply()

            val query = FirebaseFirestore.getInstance().collection("activities")
                .whereEqualTo("type", "other")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Activity>()
                .setQuery(query, Activity::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
    }

    private fun emptyList(itemCount: Int = 0) {
        // Show the empty list message
        mBinding.ivEmptyList.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
        mBinding.tvEmptyList.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
    }

    private fun openActivity(activityId: String) {
        findNavController().navigate(
            ActivitiesFragmentDirections.actionActivitiesFragmentToActivityFragment(activityId)
        )
    }
}