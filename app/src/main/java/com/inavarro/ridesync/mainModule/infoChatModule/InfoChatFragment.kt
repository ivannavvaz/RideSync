package com.inavarro.ridesync.mainModule.infoChatModule

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentInfoChatBinding
import com.inavarro.ridesync.databinding.ItemUserBinding
import com.inavarro.ridesync.mainModule.infoChatModule.adapters.InfoChatListAdapter

class InfoChatFragment : Fragment() {

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

        mBinding.ivBack.setOnClickListener {
            findNavController().navigateUp()
        }

        setupRecyclerView()

        setupGroup()

        mBinding.btnExitGroup.setOnClickListener {
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

        return mBinding.root
    }

    private fun setupGroup() {
        val idGroup = arguments?.getString("idGroup")
        val query = FirebaseFirestore.getInstance().collection("groups").document(idGroup!!)
        query.get().addOnSuccessListener {
            mGroup = it.toObject(Group::class.java)!!
            mBinding.tvGroupName.text = mGroup.name.toString().replaceFirstChar { it.uppercase() }

            if (mGroup.description != null) {
                mBinding.tvDescription.text = mGroup.description
            } else {
                mBinding.tvDescription.text = "No hay descripción"
            }

            getMembers()
        }
    }

    private fun setupRecyclerView() {
        mInfoChatListAdapter = InfoChatListAdapter()

        mLayoutManager = LinearLayoutManager(this.context)

        mBinding.rvMembers.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mInfoChatListAdapter
        }
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