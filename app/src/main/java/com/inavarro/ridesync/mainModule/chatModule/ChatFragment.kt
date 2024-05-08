package com.inavarro.ridesync.mainModule.chatModule

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Message
import com.inavarro.ridesync.databinding.FragmentChatBinding
import com.inavarro.ridesync.databinding.ItemMessageBinding
import com.inavarro.ridesync.mainModule.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatFragment : Fragment() {

    private lateinit var mBinding: FragmentChatBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Message, ChatFragment.MessageHolder>

    inner class MessageHolder(view: View):
        RecyclerView.ViewHolder(view) {
        val binding = ItemMessageBinding.bind(view)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentChatBinding.inflate(layoutInflater)

        (activity as? MainActivity)?.hideBottomNav()

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
            findNavController().navigate(R.id.action_chatFragment_to_infoChatFragment, bundle)
        }

        mBinding.etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                mBinding.rvMessages.scrollToPosition(mFirebaseAdapter.itemCount - 1)
            }
        }

        mBinding.ivSend.setOnClickListener {
            if (mBinding.etMessage.text.toString().isNotEmpty()) {
                sendMessage(mBinding.etMessage.text.toString())
            }
        }

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = LinearLayoutManager(context)

        val query = FirebaseDatabase
            .getInstance("https://ridesync-da55c-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("groups")
            .child(arguments?.getString("idGroup")!!)
            .child("messages")

        val options = FirebaseRecyclerOptions.Builder<Message>()
            .setQuery(query, Message::class.java)
            .build()

        mFirebaseAdapter = object : FirebaseRecyclerAdapter<Message, MessageHolder>(options) {

            private lateinit var context: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
                context = parent.context

                val view = LayoutInflater.from(context).inflate(R.layout.item_message, parent, false)

                return MessageHolder(view)
            }

            override fun onBindViewHolder(holder: MessageHolder, position: Int, model: Message) {
                val message = getItem(position)

                with(holder) {
                    binding.messageTextView.text = message.text

                    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dayHourFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                    val dateDayFormat = Timestamp(message.sendTime!!, 0).toDate()
                    val date = Timestamp(message.sendTime!!, 0).toDate()

                    if (dayFormat.format(dateDayFormat) == dayFormat.format(Date())) {
                        binding.dateTextView.text = hourFormat.format(date)
                    } else {
                        binding.dateTextView.text = dayHourFormat.format(date)
                    }

                    if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                        binding.messageTextView.setBackgroundResource(R.drawable.rounded_message_blue)
                    } else {
                        binding.messageTextView.setBackgroundResource(R.drawable.rounded_message_gray)
                    }

                    val ref = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(message.senderId!!)

                    ref.get().addOnSuccessListener {
                        if (it.exists()) {
                            binding.messengerTextView.text = it.getString("username")
                            if (it.getString("profilePhoto") != null) {
                                Glide.with(context)
                                    .load(it.getString("profilePhoto"))
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .circleCrop()
                                    .into(binding.messengerImageView)
                            } else {
                                Glide.with(context)
                                    .load(R.drawable.ic_person)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .circleCrop()
                                    .into(binding.messengerImageView)
                            }
                        }
                    }
                }
            }

            override fun onDataChanged() {
                super.onDataChanged()

                mBinding.rvMessages.scrollToPosition(mFirebaseAdapter.itemCount - 1)
                notifyDataSetChanged()
            }

            override fun onError(error: DatabaseError) {
                super.onError(error)

                Toast.makeText(context, error.message, Toast.LENGTH_SHORT).show()
            }
        }

        mBinding.rvMessages.apply {
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

    private fun sendMessage(message: String) {
        val message = Message(
            mBinding.etMessage.text.toString(),
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