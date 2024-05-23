package com.inavarro.ridesync.mainModule.addUsersGroupModule.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.ItemUserAddedBinding
import com.inavarro.ridesync.mainModule.infoChatModule.adapters.InfoChatListAdapter

class AddedUsersListAdapter(private val usersList: ArrayList<User>):
    ListAdapter<User, RecyclerView.ViewHolder>(UserDiffCallback()){

        private lateinit var context: Context

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding = ItemUserAddedBinding.bind(view)

            @SuppressLint("NotifyDataSetChanged")
            fun setListener(user: User) {
                binding.ivClose.setOnClickListener {
                    usersList.remove(user)

                    submitList(usersList)
                    notifyDataSetChanged()
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            context = parent.context

            val view = LayoutInflater.from(context).inflate(R.layout.item_user_added, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val user = getItem(position)

            with(holder as ViewHolder) {
                setListener(user)

                binding.tvNameUser.text = user.username

                if (user.profilePhoto != null) {
                    Glide.with(context)
                        .load(user.profilePhoto)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .circleCrop()
                        .into(binding.ivPhotoUser)
                } else {
                    binding.ivPhotoUser.setImageResource(R.drawable.ic_person)
                }
            }
        }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.username == newItem.username && oldItem.profilePhoto == newItem.profilePhoto
        }
    }
}