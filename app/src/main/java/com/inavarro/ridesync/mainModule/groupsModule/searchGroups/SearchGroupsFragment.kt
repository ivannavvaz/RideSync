package com.inavarro.ridesync.mainModule.groupsModule.searchGroups

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentSearchGroupsBinding
import com.inavarro.ridesync.databinding.ItemGroupForSearchBinding
import com.inavarro.ridesync.mainModule.MainActivity

class SearchGroupsFragment : Fragment() {

    private lateinit var mBinding: FragmentSearchGroupsBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirestoreAdapter: FirestoreRecyclerAdapter<Group, GroupHolder>

    inner class GroupHolder(view: View):
        RecyclerView.ViewHolder(view) {
        val binding = ItemGroupForSearchBinding.bind(view)

        fun setListener(group: Group) {
            binding.root.setOnClickListener {
                val userRef = FirebaseFirestore.getInstance().collection("users")
                    .document(FirebaseAuth.getInstance().currentUser?.uid!!)
                    .get()

                userRef.addOnSuccessListener {
                    val user = it.toObject(User::class.java)

                    if (group.private == true && user?.premium == false) {
                        Toast.makeText(context, "Grupo privado", Toast.LENGTH_SHORT).show()
                    } else if (group.admin == FirebaseAuth.getInstance().currentUser?.uid) {
                        Toast.makeText(context, "Eres el administrador", Toast.LENGTH_SHORT).show()
                    } else {
                        if (group.users != null && group.users.contains(FirebaseAuth.getInstance().currentUser?.uid)) {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Ya perteneces al grupo")
                                .setMessage("¿Quieres salir del grupo?")
                                .setPositiveButton("Aceptar") { _, _ ->
                                    leaveGroup(group)
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        } else {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Confirmación")
                                .setMessage("¿Quieres unirte al grupo?")
                                .setPositiveButton("Aceptar") { _, _ ->
                                    joinGroup(group)
                                }
                                .setNegativeButton("Cancelar", null)
                                .show()
                        }
                    }
                }
            }
        }
    }

    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(
            outRect: Rect, view: View, parent: RecyclerView,
            state: RecyclerView.State
        ) {
            outRect.top = verticalSpaceHeight
            outRect.bottom = verticalSpaceHeight
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentSearchGroupsBinding.inflate(layoutInflater)

        setupSearchView()

        mBinding.progressBar.visibility = View.VISIBLE

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = LinearLayoutManager(context)

        val query = FirebaseFirestore.getInstance()
            .collection("groups")
            .orderBy("name")

        val options = FirestoreRecyclerOptions.Builder<Group>()
            .setQuery(query, Group::class.java)
            .build()

        mFirestoreAdapter = object : FirestoreRecyclerAdapter<Group, GroupHolder>(options) {

            private lateinit var context: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupHolder {
                context = parent.context

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_group_for_search, parent, false)

                return GroupHolder(view)
            }

            override fun onBindViewHolder(holder: GroupHolder, position: Int, model: Group) {
                val group = getItem(position)

                with(holder) {
                    setListener(group)

                    binding.tvGroupName.text =
                        group.name.toString().replaceFirstChar { it.uppercase() }
                    binding.tvDescription.text = group.description

                    if (group.private == true) {
                        binding.ivPrivate.visibility = View.VISIBLE
                    } else {
                        binding.ivPrivate.visibility = View.GONE
                    }

                    if (group.users != null && group.users!!.contains(FirebaseAuth.getInstance().currentUser?.uid)) {
                        binding.ivCheck.visibility = View.VISIBLE
                    } else {
                        binding.ivCheck.visibility = View.GONE
                    }

                    if (group.photo != null) {
                        Glide.with(context)
                            .load(group.photo)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .circleCrop()
                            .into(binding.ivGroupPhoto)
                    } else {
                        Glide.with(context)
                            .load(R.drawable.ic_group)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .circleCrop()
                            .into(binding.ivGroupPhoto)
                    }
                }
            }

            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChanged() {
                super.onDataChanged()

                notifyDataSetChanged()

                mBinding.progressBar.visibility = View.GONE
            }

            override fun onError(e: FirebaseFirestoreException) {
                super.onError(e)

                Snackbar.make(mBinding.root, "Error: ${e.message}", Snackbar.LENGTH_SHORT).show()
            }
        }

        val dividerItemDecoration = VerticalSpaceItemDecoration(16)
        mBinding.rvGroups.addItemDecoration(dividerItemDecoration)

        mBinding.rvGroups.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager
            adapter = mFirestoreAdapter
        }
    }

    override fun onStart() {
        super.onStart()

        mFirestoreAdapter.startListening()
    }

    override fun onStop() {
        super.onStop()

        mFirestoreAdapter.stopListening()
    }

    private fun setupSearchView() {
        mBinding.svSearch.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (activity as? MainActivity)?.hideBottomNav()
            } else {
                (activity as? MainActivity)?.showBottomNav()
            }
        }

        mBinding.svSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText != null) {
                    val query = FirebaseFirestore.getInstance().collection("groups")
                        .orderBy("name")
                        .startAt(newText.lowercase())
                        .endAt(newText.lowercase() + "\uf8ff")

                    val options = FirestoreRecyclerOptions.Builder<Group>()
                        .setQuery(query, Group::class.java)
                        .build()

                    mFirestoreAdapter.updateOptions(options)
                }
                return false
            }
        })
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
                Snackbar.make(mBinding.root, "Error al abandonar el grupo", Snackbar.LENGTH_SHORT).show()
            }
    }

    private fun joinGroup(group: Group) {
        val groupRef = FirebaseFirestore.getInstance().collection("groups")
        groupRef.document(group.id!!).update(
            "users",
            FieldValue.arrayUnion(FirebaseAuth.getInstance().currentUser?.uid)
        )
            .addOnSuccessListener {
                Toast.makeText(context, "Te has unido al grupo", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener {
                Snackbar.make(mBinding.root, "Error al unirse al grupo", Snackbar.LENGTH_SHORT).show()
            }
    }
}