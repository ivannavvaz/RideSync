package com.inavarro.ridesync.mainModule.chatsModule

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.Filter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.Message
import com.inavarro.ridesync.databinding.FragmentChatsBinding
import com.inavarro.ridesync.databinding.ItemGroupBinding
import com.inavarro.ridesync.mainModule.MainActivity
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale


open class ChatsFragment : Fragment() {

    private lateinit var mBinding: FragmentChatsBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirestoreAdapter: FirestoreRecyclerAdapter<Group, GroupHolder>

    inner class GroupHolder(view: View):
        RecyclerView.ViewHolder(view) {
            val binding = ItemGroupBinding.bind(view)

            fun setListener(group: Group) {
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentChatsBinding.inflate(layoutInflater)

        setupSearchView()

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = LinearLayoutManager(context)

        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Group>()
            .setQuery(query, Group::class.java)
            .build()

        mFirestoreAdapter = object : FirestoreRecyclerAdapter<Group, GroupHolder>(options) {

            private lateinit var context: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
                context = parent.context

                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_group, parent, false)

                return GroupHolder(view)
            }

            override fun onBindViewHolder(holder: GroupHolder, position: Int, model: Group) {
                val group = getItem(position)

                with(holder) {
                    setListener(group)

                    binding.tvGroupName.text = group.name.toString().replaceFirstChar { it.uppercase() }

                    val ref = group.lastMessageRef?.let {
                        FirebaseDatabase
                            .getInstance("https://ridesync-da55c-default-rtdb.europe-west1.firebasedatabase.app/")
                            .reference
                            .child(it.path)
                    }

                    ref?.get()?.addOnSuccessListener {
                        val message: Message? = it.getValue(Message::class.java)
                        if (message != null) {
                            if (message.name == FirebaseAuth.getInstance().currentUser?.displayName) {
                                    if (message.text?.length!! > 20) {
                                        binding.tvLastMessage.text = "You: ${message.text?.substring(0, 20)}..."
                                    } else {
                                        binding.tvLastMessage.text = "You: ${message.text}"
                                    }
                            } else  {
                                if (message.text?.length!! > 20) {
                                    binding.tvLastMessage.text = "${message.text?.substring(0, 20)}..."
                                } else {
                                    binding.tvLastMessage.text = message.text
                                }
                            }

                            val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                            val dateDayFormat = Timestamp(message.sendTime!!, 0).toDate()
                            val dateHourFormat = Timestamp(message.sendTime!!, 0).toDate()

                            if (dayFormat.format(dateDayFormat) == dayFormat.format(Date())) {
                                binding.tvLastMessageTime.text = hourFormat.format(dateHourFormat)
                            } else {
                                binding.tvLastMessageTime.text = dayFormat.format(dateDayFormat)
                            }
                        }
                    }

                    binding.root.setOnClickListener {

                        mBinding.svSearch.setOnQueryTextFocusChangeListener { _, hasFocus ->
                            if (hasFocus) {
                                (activity as? MainActivity)?.hideBottomNav()
                            }
                        }

                        findNavController().navigate(ChatsFragmentDirections.actionChatsFragmentToChatFragment(group.id!!, group.name!!))
                    }
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

        mBinding.rvChats.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirestoreAdapter
        }
    }

    override fun onResume() {
        super.onResume()

        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Group>()
            .setQuery(query, Group::class.java)
            .build()

        mFirestoreAdapter.updateOptions(options)
    }

    override fun onStart() {
        super.onStart()

        (activity as? MainActivity)?.showBottomNav()
        mFirestoreAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()

        mBinding.svSearch.setQuery(null, false)
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
                    val query = FirebaseFirestore.getInstance().collection("groups")
                        .orderBy("name")
                        .startAt(newText.lowercase())
                        .endAt(newText.lowercase() + "\uf8ff")

                    val options = FirestoreRecyclerOptions.Builder<Group>()
                        .setQuery(query, Group::class.java)
                        .build()

                    mFirestoreAdapter.updateOptions(options)
                }
                return false
            }
        })
    }
}