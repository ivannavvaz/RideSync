package com.inavarro.ridesync.mainModule.chatModule.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Message
import com.inavarro.ridesync.common.entities.MessagesRecyclerViewItem
import com.inavarro.ridesync.databinding.ItemReceiverMessageBinding
import com.inavarro.ridesync.databinding.ItemTransmitterMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MessagesListAdapter: ListAdapter<MessagesRecyclerViewItem, RecyclerView.ViewHolder>(DiffCallBack()){

    private lateinit var context: Context

    sealed class MessageHolder(view: View): RecyclerView.ViewHolder(view) {
        class ReceiverMessageHolder(val binding: ItemReceiverMessageBinding) : MessageHolder(binding.root)
        class TransmitterMessageHolder(val binding: ItemTransmitterMessageBinding) : MessageHolder(binding.root)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageHolder {
        context = parent.context

        return when(viewType) {
            R.layout.item_receiver_message -> {
                val binding = ItemReceiverMessageBinding.inflate(LayoutInflater.from(context), parent, false)
                MessageHolder.ReceiverMessageHolder(binding)
            }
            R.layout.item_transmitter_message -> {
                val binding = ItemTransmitterMessageBinding.inflate(LayoutInflater.from(context), parent, false)
                MessageHolder.TransmitterMessageHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)

        when (holder) {
            is MessageHolder.ReceiverMessageHolder -> {
                with (holder) {
                    val message = item as MessagesRecyclerViewItem.ReceiverMessage

                    binding.messageTextView.text = message.text

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

                    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dayHourFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                    val dateDayFormat = Timestamp(message.sendTime!!, 0).toDate()
                    val date = Timestamp(message.sendTime, 0).toDate()

                    if (dayFormat.format(dateDayFormat) == dayFormat.format(Date())) {
                        binding.dateTextView.text = hourFormat.format(date)
                    } else {
                        binding.dateTextView.text = dayHourFormat.format(date)
                    }
                }
            }

            is MessageHolder.TransmitterMessageHolder -> {
                with (holder) {
                    val message = item as MessagesRecyclerViewItem.TransmitterMessage

                    binding.messageTextView.text = message.text

                    val ref = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(message.senderId!!)

                    ref.get().addOnSuccessListener {
                        if (it.exists()) {
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

                    val hourFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val dayFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    val dayHourFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

                    val dateDayFormat = Timestamp(message.sendTime!!, 0).toDate()
                    val date = Timestamp(message.sendTime, 0).toDate()

                    if (dayFormat.format(dateDayFormat) == dayFormat.format(Date())) {
                        binding.dateTextView.text = hourFormat.format(date)
                    } else {
                        binding.dateTextView.text = dayHourFormat.format(date)
                    }
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when(getItem(position)) {
            is MessagesRecyclerViewItem.ReceiverMessage -> R.layout.item_receiver_message
            is MessagesRecyclerViewItem.TransmitterMessage -> R.layout.item_transmitter_message
        }
    }

    class DiffCallBack: DiffUtil.ItemCallback<MessagesRecyclerViewItem>() {
        override fun areItemsTheSame(oldItem: MessagesRecyclerViewItem, newItem: MessagesRecyclerViewItem): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: MessagesRecyclerViewItem, newItem: MessagesRecyclerViewItem): Boolean {
            return oldItem == newItem
        }
    }
}