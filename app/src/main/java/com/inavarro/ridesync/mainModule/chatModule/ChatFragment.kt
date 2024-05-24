package com.inavarro.ridesync.mainModule.chatModule

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Message
import com.inavarro.ridesync.common.entities.MessagesRecyclerViewItem
import com.inavarro.ridesync.databinding.FragmentChatBinding
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.chatModule.adapters.MessagesListAdapter

class ChatFragment : Fragment() {

    private lateinit var mBinding: FragmentChatBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mListAdapter: MessagesListAdapter
    private lateinit var mLinearlayout: LinearLayoutManager
    private lateinit var mItems: MutableList<MessagesRecyclerViewItem>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentChatBinding.inflate(layoutInflater)

        setupChatFragment()

        mBinding.progressBar.visibility = View.VISIBLE

        setupRecyclerView()

        mItems = mutableListOf()

        getMessages()

        mBinding.tvName.text = arguments?.getString("nameGroup").toString()

        val groupId = arguments?.getString("idGroup")
        val groupRef = FirebaseFirestore.getInstance().collection("groups").document(groupId!!)
        groupRef.get().addOnSuccessListener {
            if (it.exists()) {
                if (it.getString("photo") != null) {
                    Glide.with(requireContext())
                        .load(it.getString("photo"))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(mBinding.ivPhotoGroup)
                } else {
                    Glide.with(requireContext())
                        .load(R.drawable.ic_group)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(mBinding.ivPhotoGroup)
                }
            }
        }

        mBinding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        mBinding.ivInfo.setOnClickListener {
            val bundle = Bundle()
            bundle.putString("idGroup", arguments?.getString("idGroup"))
            findNavController().navigate(R.id.action_chatFragment_to_infoGroupFragment, bundle)
        }

        mBinding.ivSend.setOnClickListener {
            if (mBinding.etMessage.text.toString().isNotEmpty()) {
                sendMessage(mBinding.etMessage.text.toString())
            }
        }

        return mBinding.root
    }

    private fun setupChatFragment(){
        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun setupRecyclerView() {
        mListAdapter = MessagesListAdapter()

        mLinearlayout = LinearLayoutManager(context)
        mLinearlayout.orientation = LinearLayoutManager.VERTICAL

        mBinding.rvMessages.apply {
            setHasFixedSize(true)
            layoutManager = mLinearlayout
            adapter = mListAdapter
        }
    }

    private fun getMessages() {
        val query = FirebaseDatabase
            .getInstance("https://ridesync-da55c-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("groups")
            .child(arguments?.getString("idGroup")!!)
            .child("messages")

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mItems.clear()
                for (message in snapshot.children) {
                    val message = message.getValue(Message::class.java)
                    if (message != null) {
                        val messageWithType: MessagesRecyclerViewItem
                        if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                            messageWithType = MessagesRecyclerViewItem.TransmitterMessage(message.text, message.senderId, message.sendTime)
                        } else {
                            messageWithType = MessagesRecyclerViewItem.ReceiverMessage(message.text, message.senderId, message.sendTime)
                        }
                        mItems.add(messageWithType)
                    }
                }
                mListAdapter.submitList(mItems)
                mBinding.progressBar.visibility = View.GONE
                mBinding.rvMessages.scrollToPosition(mItems.size - 1)
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun sendMessage(message: String) {
        val message = Message(
            message,
            FirebaseAuth.getInstance().currentUser?.uid ?: "Unknown",
            Timestamp.now().seconds
        )

        val ref = FirebaseDatabase
            .getInstance("https://ridesync-da55c-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("groups")
            .child(arguments?.getString("idGroup")!!)
            .child("messages")
            .push()

        ref.setValue(message)
            .addOnSuccessListener {
                mBinding.etMessage.setText("")
            }
            .addOnFailureListener {
                Toast.makeText(context, it.message, Toast.LENGTH_SHORT).show()
            }

        val db = FirebaseFirestore.getInstance()
        db.collection("groups")
            .document(arguments?.getString("idGroup")!!)
            .update("lastMessageRef", db.collection("groups").document(arguments?.getString("idGroup")!!).collection("messages").document(ref.key!!))

        db.collection("groups")
            .document(arguments?.getString("idGroup")!!)
            .update("lastMessageTime", Timestamp.now().seconds)
    }
}