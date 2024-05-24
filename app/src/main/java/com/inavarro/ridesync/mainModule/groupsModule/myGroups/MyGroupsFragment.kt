package com.inavarro.ridesync.mainModule.groupsModule.myGroups

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.Message
import com.inavarro.ridesync.databinding.FragmentMyGroupsBinding
import com.inavarro.ridesync.databinding.ItemGroupBinding
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.groupsModule.GroupsFragmentDirections
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


open class MyGroupsFragment : Fragment() {

    private lateinit var mBinding: FragmentMyGroupsBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<Group, GroupHolder>

    inner class GroupHolder(view: View):
        RecyclerView.ViewHolder(view) {
            val binding = ItemGroupBinding.bind(view)

            fun setListener(group: Group) {
                binding.root.setOnClickListener {
                    mBinding.svSearch.setOnQueryTextFocusChangeListener { _, hasFocus ->
                        if (hasFocus) {
                            (activity as? MainActivity)?.hideBottomNav()
                        }
                    }

                    findNavController().navigate(GroupsFragmentDirections.actionGroupsFragmentToChatFragment(group.id!!, group.name!!))
                }

                binding.root.setOnLongClickListener {
                    val builder = AlertDialog.Builder(requireContext())

                    val alertDialog = builder.create()
                    alertDialog.show()

                    builder.setTitle("Confirmación")
                    builder.setMessage("¿Quieres salir del grupo?")
                    builder.setPositiveButton("Aceptar") { _, _ ->
                        leaveGroup(group)
                        alertDialog.dismiss()
                    }
                    builder.setNegativeButton("Cancel") { _, _ ->
                        alertDialog.dismiss()
                    }

                    builder.show()
                    alertDialog.dismiss()

                    true
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentMyGroupsBinding.inflate(layoutInflater)

        setupMyGroupsFragment()

        setupSearchView()

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = LinearLayoutManager(context)

        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .whereArrayContains("users", FirebaseAuth.getInstance().currentUser?.uid!!)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Group>()
            .setQuery(query, Group::class.java)
            .build()

        mFirebaseAdapter = object : FirestoreRecyclerAdapter<Group, GroupHolder>(options) {

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

                    binding.tvGroupName.text = group.name.toString()

                    if (group.lastMessageRef == null) {
                        binding.tvLastMessage.text = "No hay mensajes"
                        binding.tvLastMessageTime.text = ""
                    } else {

                        val ref = group.lastMessageRef.let {
                            FirebaseDatabase
                                .getInstance("https://ridesync-da55c-default-rtdb.europe-west1.firebasedatabase.app/")
                                .reference
                                .child(it.path)
                        }

                        ref.get().addOnSuccessListener {
                            val message: Message? = it.getValue(Message::class.java)
                            if (message != null) {
                                if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                                    if (message.text?.length!! > 20) {
                                        binding.tvLastMessage.text =
                                            "You: ${message.text.substring(0, 15)}..."
                                    } else {
                                        binding.tvLastMessage.text = "You: ${message.text}"
                                    }
                                } else {
                                    if (message.text?.length!! > 20) {
                                        binding.tvLastMessage.text =
                                            "${message.text.substring(0, 15)}..."
                                    } else {
                                        binding.tvLastMessage.text = message.text
                                    }
                                }

                                val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                                val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

                                val dateDayFormat = Timestamp(message.sendTime!!, 0).toDate()
                                val dateHourFormat = Timestamp(message.sendTime!!, 0).toDate()

                                if (dayFormat.format(dateDayFormat) == dayFormat.format(Date())) {
                                    binding.tvLastMessageTime.text =
                                        hourFormat.format(dateHourFormat)
                                } else {
                                    binding.tvLastMessageTime.text = dayFormat.format(dateDayFormat)
                                }
                            } else {
                                binding.tvLastMessage.text = "No hay mensajes"
                                binding.tvLastMessageTime.text = ""
                            }
                        }
                    }

                    if (group.photo != null) {
                        Glide.with(context)
                            .load(group.photo)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .circleCrop()
                            .into(binding.ivGroupImage)
                    } else {
                        Glide.with(context)
                            .load(R.drawable.ic_group)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .circleCrop()
                            .into(binding.ivGroupImage)
                    }
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

                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.rvChats.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirebaseAdapter
        }
    }

    override fun onStart() {
        super.onStart()

        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .whereArrayContains("users", FirebaseAuth.getInstance().currentUser?.uid!!)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)

        val options = FirestoreRecyclerOptions.Builder<Group>()
            .setQuery(query, Group::class.java)
            .build()

        mFirebaseAdapter.updateOptions(options)

        mFirebaseAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()

        mBinding.svSearch.setQuery(null, false)
        mFirebaseAdapter.stopListening()
    }

    private fun setupMyGroupsFragment() {
        mBinding.progressBar.visibility = View.VISIBLE
        ((activity as? MainActivity)?.showBottomNav())
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
                        .whereArrayContains("users", FirebaseAuth.getInstance().currentUser?.uid!!)
                        .orderBy("name")
                        .startAt(newText.lowercase())
                        .endAt(newText.lowercase() + "\uf8ff")

                    val options = FirestoreRecyclerOptions.Builder<Group>()
                        .setQuery(query, Group::class.java)
                        .build()

                    mFirebaseAdapter.updateOptions(options)
                }
                return false
            }
        })
    }

    private fun leaveGroup(group: Group) {
        val groupRef = FirebaseFirestore.getInstance().collection("groups")
        groupRef.document(group.id!!).update(
            "users",
            FieldValue.arrayRemove(FirebaseAuth.getInstance().currentUser?.uid)
        )
            .addOnSuccessListener {
                Toast.makeText(context, "Has abandonado el grupo", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            }

        val userRef = FirebaseFirestore.getInstance().collection("users")
        userRef.document(FirebaseAuth.getInstance().currentUser?.uid!!).update(
            "groups",
            FieldValue.arrayRemove(group.id)
        )
    }

    private fun emptyList() {
        mBinding.ivEmptyList.visibility = if (mFirebaseAdapter.itemCount == 0) View.VISIBLE else View.GONE
        mBinding.tvEmptyList.visibility = if (mFirebaseAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }
}