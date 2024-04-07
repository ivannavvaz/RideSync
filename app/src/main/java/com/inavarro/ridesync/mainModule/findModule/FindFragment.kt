package com.inavarro.ridesync.mainModule.findModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Activity
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.Message
import com.inavarro.ridesync.databinding.FragmentFindBinding
import com.inavarro.ridesync.databinding.ItemActivityForFindBinding
import com.inavarro.ridesync.databinding.ItemGroupBinding
import com.inavarro.ridesync.databinding.ItemGroupForFindBinding
import com.inavarro.ridesync.mainModule.chatsModule.ChatsFragment

class FindFragment : Fragment() {

    private lateinit var mBinding: FragmentFindBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirestoreAdapter: FirestoreRecyclerAdapter<Any, FindHolder>

    private lateinit var mItems: MutableList<Any>

    inner class FindHolder(view: View):
        RecyclerView.ViewHolder(view) {
        val bindingGroup = ItemGroupForFindBinding.bind(view)
        val bindingActity = ItemActivityForFindBinding.bind(view)

        fun setListener(group: Group) {
        }

        fun setListener(activity: Activity) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentFindBinding.inflate(layoutInflater)
        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = LinearLayoutManager(context)

        val queryGroups = FirebaseFirestore.getInstance().collection("groups")

        val queryActivities = FirebaseFirestore.getInstance().collection("activities")

        val optionsGroups = FirestoreRecyclerOptions.Builder<Group>()
            .setQuery(queryGroups, Group::class.java)
            .build()

        val optionsActivities = FirestoreRecyclerOptions.Builder<Activity>()
            .setQuery(queryActivities, Activity::class.java)
            .build()

        mItems = mutableListOf<Any>()
        
    }
}