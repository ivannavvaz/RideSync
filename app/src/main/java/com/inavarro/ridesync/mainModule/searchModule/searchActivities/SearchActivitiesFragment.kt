package com.inavarro.ridesync.mainModule.searchModule.searchActivities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Activity
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.databinding.FragmentSearchActivitiesBinding
import com.inavarro.ridesync.databinding.ItemActivityForSearchBinding
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.searchModule.searchGroups.SearchGroupsFragment
import java.text.SimpleDateFormat
import java.util.Locale

class SearchActivitiesFragment : Fragment() {

    private lateinit var mBinding: FragmentSearchActivitiesBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirestoreAdapter: FirestoreRecyclerAdapter<Activity, ActivityHolder>

    inner class ActivityHolder(view: View):
        RecyclerView.ViewHolder(view) {
            val binding = ItemActivityForSearchBinding.bind(view)

            fun setListener(activity: Activity) {
            }
        }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.bottom = verticalSpaceHeight
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentSearchActivitiesBinding.inflate(layoutInflater)

        setupSearchView()

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = LinearLayoutManager(context)

        val query = FirebaseFirestore.getInstance()
            .collection("activities")

        val options = FirestoreRecyclerOptions.Builder<Activity>()
            .setQuery(query, Activity::class.java)
            .build()

        mFirestoreAdapter = object : FirestoreRecyclerAdapter<Activity, ActivityHolder>(options) {

            private lateinit var context: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ActivityHolder {
                context = parent.context

                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_activity_for_search, parent, false)

                return ActivityHolder(view)
            }

            override fun onBindViewHolder(holder: ActivityHolder, position: Int, model: Activity) {
                val activity = getItem(position)

                with(holder) {
                    setListener(activity)

                    binding.tvActivityTitle.text = activity.title
                    binding.tvDescription.text = activity.description

                    val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.getDefault())
                    val dateDateTimeFormat = Timestamp(activity.date!!, 0).toDate()
                    binding.tvDate.text = dateTimeFormat.format(dateDateTimeFormat)

                    binding.tvLocation.text = activity.location
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChanged() {
                super.onDataChanged()

                notifyDataSetChanged()
            }

            override fun onError(e: FirebaseFirestoreException) {
                super.onError(e)

                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        val dividerItemDecoration = VerticalSpaceItemDecoration(16)
        mBinding.rvActivities.addItemDecoration(dividerItemDecoration)

        mBinding.rvActivities.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirestoreAdapter
        }
    }

    override fun onStart() {
        super.onStart()

        mFirestoreAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()

        mFirestoreAdapter.stopListening()
    }

    private fun setupSearchView() {
        mBinding.svSearch.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (activity as? MainActivity)?.hideBottomNav()
            } else {
                (activity as? MainActivity)?.showBottomNav()
            }
        }

        mBinding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    val query = FirebaseFirestore.getInstance().collection("activities")
                        .orderBy("title")
                        .startAt(newText.lowercase())
                        .endAt(newText.lowercase() + "\uf8ff")

                    val options = FirestoreRecyclerOptions.Builder<Activity>()
                        .setQuery(query, Activity::class.java)
                        .build()

                    mFirestoreAdapter.updateOptions(options)
                }
                return false
            }
        })
    }
}