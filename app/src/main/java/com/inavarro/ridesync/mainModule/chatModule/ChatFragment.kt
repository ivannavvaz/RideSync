package com.inavarro.ridesync.mainModule.chatModule

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
import java.util.Locale

class ChatFragment : Fragment() {

    private lateinit var mBinding: FragmentChatBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirebaseRecyclerAdapter<Message, ChatFragment.MessageHolder>

    inner class MessageHolder(view: View):
        RecyclerView.ViewHolder(view) {
        val binding = ItemMessageBinding.bind(view)

        fun setListener(message: Message) {
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentChatBinding.inflate(layoutInflater)

        (activity as? MainActivity)?.hideBottomNav()

        mBinding.tvName.text = arguments?.getString("nameGroup").toString().replaceFirstChar { it.uppercase() }

        mBinding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        mBinding.etMessage.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                mBinding.rvMessages.scrollToPosition(mFirebaseAdapter.itemCount - 1)
            }
        }

        mBinding.ivSend.setOnClickListener {
            if (mBinding.etMessage.text.toString().isNotEmpty()) {
                val message = Message(
                    mBinding.etMessage.text.toString(),
                    FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown",
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
                    setListener(message)

                    binding.messageTextView.text = message.text
                    binding.messengerTextView.text = message.name

                    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val date = Timestamp(message.sendTime!!, 0).toDate()
                    binding.dateTextView.text = hourFormat.format(date)

                    if (message.name == FirebaseAuth.getInstance().currentUser?.displayName) {
                        binding.messageTextView.setBackgroundResource(R.drawable.rounded_message_blue)
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
}