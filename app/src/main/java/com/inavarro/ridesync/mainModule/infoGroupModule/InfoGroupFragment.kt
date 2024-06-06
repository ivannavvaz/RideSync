package com.inavarro.ridesync.mainModule.infoGroupModule

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
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
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentInfoGroupBinding
import com.inavarro.ridesync.mainModule.infoGroupModule.adapters.InfoGroupListAdapter
import com.inavarro.ridesync.mainModule.infoGroupModule.adapters.OnClickListener

class InfoGroupFragment : Fragment(), MenuProvider, OnClickListener {

    private lateinit var mBinding: FragmentInfoGroupBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mInfoGroupListAdapter: InfoGroupListAdapter

    private lateinit var mGroup: Group

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentInfoGroupBinding.inflate(layoutInflater)

        mBinding.progressBar.visibility = View.VISIBLE

        setupToolBar()

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

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.option_menu_item, menu)

        val itemExitGroup = menu.findItem(R.id.action_exit_group)
        val itemAddUsers = menu.findItem(R.id.action_add_users)
        val itemEditGroup = menu.findItem(R.id.action_edit_group)
        val itemDeleteGroup = menu.findItem(R.id.action_delete_group)

        // Hide or show the menu items depending on the user's role in the group
        if (::mGroup.isInitialized) {
            itemExitGroup.isVisible = mGroup.admin != FirebaseAuth.getInstance().currentUser?.uid
            itemAddUsers.isVisible = mGroup.admin == FirebaseAuth.getInstance().currentUser?.uid
            itemEditGroup.isVisible = mGroup.admin == FirebaseAuth.getInstance().currentUser?.uid
            itemDeleteGroup.isVisible = mGroup.admin == FirebaseAuth.getInstance().currentUser?.uid
        } else {
            itemExitGroup.isVisible = false
        }
    }

    override fun onMenuItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        when (item.itemId) {
            R.id.action_exit_group -> {
                // Show a confirmation dialog before leaving the group
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmación")
                    .setMessage("¿Quieres salir del grupo?")
                    .setPositiveButton("Aceptar") { _, _ ->
                        leaveGroup(mGroup)
                        findNavController().navigate(R.id.action_infoGroupFragment_to_GroupsFragment)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
            R.id.action_add_users -> {
                findNavController().navigate(InfoGroupFragmentDirections.actionInfoGroupFragmentToAddUsersGroupFragment(mGroup.id!!, true))
            }
            R.id.action_edit_group -> {
                findNavController().navigate(InfoGroupFragmentDirections.actionInfoGroupFragmentToEditGroupFragment(mGroup.id!!))
            }
            R.id.action_delete_group -> {
                // Show a confirmation dialog before deleting the group
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmación")
                    .setMessage("¿Quieres eliminar el grupo?")
                    .setPositiveButton("Aceptar") { _, _ ->
                        deleteGroup(mGroup)
                        findNavController().navigate(R.id.action_infoGroupFragment_to_GroupsFragment)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
        return false
    }

    private fun setupGroup() {
        // Get the group id from the arguments
        val idGroup = arguments?.getString("idGroup")

        // Get the group from Firestore
        val query = FirebaseFirestore.getInstance().collection("groups").document(idGroup!!)
        query.get().addOnSuccessListener {
            // Set the group data to the view
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

            requireActivity().invalidateOptionsMenu()

            setupRecyclerView()

            getMembers()
        }
    }

    private fun setupRecyclerView() {
        mInfoGroupListAdapter = InfoGroupListAdapter(this, mGroup)

        mLayoutManager = LinearLayoutManager(this.context)

        mBinding.rvMembers.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mInfoGroupListAdapter
        }
    }

    override fun onClick(userEntity: User) {
        // Open the user's profile
        if (userEntity.id != FirebaseAuth.getInstance().currentUser?.uid) {
            findNavController().navigate(
                InfoGroupFragmentDirections.actionInfoGroupFragmentToViewProfileFragment(
                    userEntity.id!!
                )
            )
        }
    }

    override fun onLongClick(userEntity: User) {
        // Show a confirmation dialog before removing the user from the group
        if (userEntity.id != FirebaseAuth.getInstance().currentUser?.uid) {
            if (mGroup.admin == FirebaseAuth.getInstance().currentUser?.uid) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Confirmación")
                    .setMessage("¿Quieres eliminar a ${userEntity.username}?")
                    .setPositiveButton("Aceptar") { _, _ ->
                        removeUser(userEntity)
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()
            }
        }
    }

    private fun getMembers() {
        // Create an empty list of users
        val users = mutableListOf<User>()
        mInfoGroupListAdapter.submitList(users)

        // Listen for changes in the group's users list
        FirebaseFirestore.getInstance().collection("groups").document(mGroup.id!!)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val group = snapshot.toObject(Group::class.java)
                    updateUsersList(group!!.users!!, users)
                }
            }
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun updateUsersList(userIds: List<String>, users: MutableList<User>) {
        // Clear the current users list
        users.clear()

        // Fetch the updated users
        userIds.forEach { userId ->
            FirebaseFirestore.getInstance().collection("users").document(userId).get()
                .addOnSuccessListener { it ->
                    val user = it.toObject(User::class.java)
                    users.add(user!!)
                    users.sortBy { it.username }
                    mInfoGroupListAdapter.notifyDataSetChanged()
                    mBinding.progressBar.visibility = View.GONE
                }
        }
    }

    private fun leaveGroup(group: Group) {
        // Remove the user from the group
        val groupRef = FirebaseFirestore.getInstance().collection("groups")
        groupRef.document(group.id!!).update(
            "users",
            FieldValue.arrayRemove(FirebaseAuth.getInstance().currentUser?.uid)
        )
            .addOnSuccessListener {
                Toast.makeText(context, "Has abandonado el grupo", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(mBinding.root, "Error al abandonar el grupo", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun removeUser(user: User) {
        // Remove the user from the group
        val groupRef = FirebaseFirestore.getInstance().collection("groups")
        groupRef.document(mGroup.id!!).update(
            "users",
            FieldValue.arrayRemove(user.id)
        )
            .addOnSuccessListener {
                Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Snackbar.make(mBinding.root, "Error al eliminar usuario", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun deleteGroup(group: Group) {
        // Delete the group from Firestore
        val groupRef = FirebaseFirestore.getInstance().collection("groups")
        groupRef.document(group.id!!).delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Grupo eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                view?.let { Snackbar.make(it, "Error al abandonar el grupo", Snackbar.LENGTH_SHORT).show() }
            }

        if (mGroup.photo != null) {
            val photoRef = FirebaseStorage
                .getInstance()
                .reference
                .child("groupPhotos")
                .child(group.id)

            photoRef.delete()
        }

        if (mGroup.lastMessageRef != null) {
            val messageRef = FirebaseDatabase
                .getInstance("https://ridesync-da55c-default-rtdb.europe-west1.firebasedatabase.app/")
                .reference
                .child("groups")
                .child(group.id)

            messageRef.removeValue()
        }
    }
}