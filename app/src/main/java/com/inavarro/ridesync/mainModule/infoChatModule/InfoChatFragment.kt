package com.inavarro.ridesync.mainModule.infoChatModule

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentInfoChatBinding
import com.inavarro.ridesync.databinding.ItemUserBinding
import com.inavarro.ridesync.mainModule.infoChatModule.adapters.InfoChatListAdapter
import com.inavarro.ridesync.mainModule.infoChatModule.adapters.OnClickListener

class InfoChatFragment : Fragment(), MenuProvider, OnClickListener {

    private lateinit var mBinding: FragmentInfoChatBinding

    private lateinit var mGroup: Group

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mInfoChatListAdapter: InfoChatListAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentInfoChatBinding.inflate(layoutInflater)

        mBinding.progressBar.visibility = View.VISIBLE

        setupToolBar()

        setupRecyclerView()

        setupGroup()

        return mBinding.root
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        mBinding.toolBar.title = "Información del grupo"
        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        val menuHost: MenuHost = requireActivity()
        menuHost.addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.option_menu_item, menu)
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_exit_group -> {
                val builder = AlertDialog.Builder(requireContext())

                val alertDialog = builder.create()
                alertDialog.show()

                builder.setTitle("Confirmación")
                builder.setMessage("¿Quieres salir del grupo?")
                builder.setPositiveButton("Aceptar") { _, _ ->
                    leaveGroup(mGroup)
                    findNavController().navigate(R.id.action_infoChatFragment_to_GroupsFragment)
                    alertDialog.dismiss()
                }
                builder.setNegativeButton("Cancel") { _, _ ->
                    alertDialog.dismiss()
                }

                builder.show()
                alertDialog.dismiss()
            }
        }
        return false
    }

    private fun setupGroup() {
        val idGroup = arguments?.getString("idGroup")
        val query = FirebaseFirestore.getInstance().collection("groups").document(idGroup!!)
        query.get().addOnSuccessListener {
            mGroup = it.toObject(Group::class.java)!!

            if (mGroup.photo != null) {
                Glide.with(requireContext())
                    .load(mGroup.photo)
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

            mBinding.tvGroupName.text = mGroup.name.toString()

            if (mGroup.description != null) {
                mBinding.tvDescription.text = mGroup.description
            } else {
                mBinding.tvDescription.text = "No hay descripción"
            }

            getMembers()
        }
    }

    private fun setupRecyclerView() {
        mInfoChatListAdapter = InfoChatListAdapter(this)

        mLayoutManager = LinearLayoutManager(this.context)

        mBinding.rvMembers.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mInfoChatListAdapter
        }
    }

    override fun onClick(user: User) {
        findNavController().navigate(
            InfoChatFragmentDirections.actionInfoChatFragmentToViewProfileFragment(
                user.id!!
            )
        )
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getMembers() {
        val users = mutableListOf<User>()
        mInfoChatListAdapter.submitList(users)

        mGroup.users?.forEach { userId ->
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener {
                    val user = it.toObject(User::class.java)
                    users.add(user!!)
                    mInfoChatListAdapter.notifyDataSetChanged()
                    mBinding.progressBar.visibility = View.GONE
                }
        }
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
    }
}