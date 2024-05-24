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

    private lateinit var mGroup: Group

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mInfoGroupListAdapter: InfoGroupListAdapter

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

    override fun onCreateMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.option_menu_item, menu)

        val itemExitGroup = menu.findItem(R.id.action_exit_group)
        val itemEditGroup = menu.findItem(R.id.action_edit_group)
        val itemDeleteGroup = menu.findItem(R.id.action_delete_group)

        if (::mGroup.isInitialized) {
            itemExitGroup.isVisible = mGroup.admin != FirebaseAuth.getInstance().currentUser?.uid
            itemEditGroup.isVisible = mGroup.admin == FirebaseAuth.getInstance().currentUser?.uid
            itemDeleteGroup.isVisible = mGroup.admin == FirebaseAuth.getInstance().currentUser?.uid
        } else {
            itemExitGroup.isVisible = false
        }
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
                    findNavController().navigate(R.id.action_infoGroupFragment_to_GroupsFragment)
                    alertDialog.dismiss()
                }
                builder.setNegativeButton("Cancel") { _, _ ->
                    alertDialog.dismiss()
                }

                builder.show()
                alertDialog.dismiss()
            }
            R.id.action_edit_group -> {
                findNavController().navigate(InfoGroupFragmentDirections.actionInfoGroupFragmentToEditGroupFragment())
            }
            R.id.action_delete_group -> {
                val builder = AlertDialog.Builder(requireContext())

                val alertDialog = builder.create()
                alertDialog.show()

                builder.setTitle("Confirmación")
                builder.setMessage("¿Quieres eliminar el grupo?")
                builder.setPositiveButton("Aceptar") { _, _ ->
                    deleteGroup(mGroup)
                    findNavController().navigate(R.id.action_infoGroupFragment_to_GroupsFragment)
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
        if (userEntity.id != FirebaseAuth.getInstance().currentUser?.uid) {
            findNavController().navigate(
                InfoGroupFragmentDirections.actionInfoGroupFragmentToViewProfileFragment(
                    userEntity.id!!
                )
            )
        }
    }

    override fun onLongClick(userEntity: User) {
        if (userEntity.id != FirebaseAuth.getInstance().currentUser?.uid) {
            if (mGroup.admin == FirebaseAuth.getInstance().currentUser?.uid) {
                val builder = AlertDialog.Builder(requireContext())

                val alertDialog = builder.create()
                alertDialog.show()

                builder.setTitle("Confirmación")
                builder.setMessage("¿Quieres eliminar a ${userEntity.username}?")
                builder.setPositiveButton("Aceptar") { _, _ ->
                    removeUser(userEntity)
                    alertDialog.dismiss()
                }
                builder.setNegativeButton("Cancel") { _, _ ->
                    alertDialog.dismiss()
                }

                builder.show()
                alertDialog.dismiss()
            }
        }
    }

    private fun getMembers() {
        val users = mutableListOf<User>()
        mInfoGroupListAdapter.submitList(users)

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

    private fun removeUser(user: User) {
        val groupRef = FirebaseFirestore.getInstance().collection("groups")
        groupRef.document(mGroup.id!!).update(
            "users",
            FieldValue.arrayRemove(user.id)
        )
            .addOnSuccessListener {
                Toast.makeText(context, "Usuario eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteGroup(group: Group) {
        val groupRef = FirebaseFirestore.getInstance().collection("groups")
        groupRef.document(group.id!!).delete()
            .addOnSuccessListener {
                Toast.makeText(context, "Grupo eliminado", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show()
            }

        val messageRef = FirebaseDatabase
            .getInstance("https://ridesync-da55c-default-rtdb.europe-west1.firebasedatabase.app/")
            .reference
            .child("groups")
            .child(group.id)

        messageRef.removeValue()

        val photoRef = FirebaseStorage
            .getInstance()
            .reference
            .child("groupPhotos")
            .child(group.id)

        photoRef.delete()
    }
}