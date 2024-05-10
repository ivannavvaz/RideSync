package com.inavarro.ridesync.mainModule.infoChatModule.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.ItemUserBinding

class InfoChatListAdapter():
    ListAdapter<User, RecyclerView.ViewHolder>(UserDiffCallback()) {

        private lateinit var context: Context

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val binding = ItemUserBinding.bind(view)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            context = parent.context

            val view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false)

            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val user = getItem(position)

            with(holder as ViewHolder) {
                binding.tvUserName.text = user.username

                // Get profile photo
                if (user.profilePhoto != null) {
                    Glide.with(context)
                        .load(user.profilePhoto)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .centerCrop()
                        .circleCrop()
                        .into(binding.ivPhotoProfile)
                } else {
                    binding.ivPhotoProfile.setImageResource(R.drawable.ic_person)
                }

                if (position == itemCount - 1) {
                    binding.divider.visibility = View.GONE
                } else {
                    binding.divider.visibility = View.VISIBLE
                }
            }
        }

        class UserDiffCallback : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
                return oldItem == newItem
            }
        }
}
