package com.inavarro.ridesync.mainModule.addUsersGroupModule

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Group
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.FragmentAddUsersGroupBinding
import com.inavarro.ridesync.databinding.ItemUserBinding
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.addUsersGroupModule.adapters.AddedUsersListAdapter
import com.inavarro.ridesync.mainModule.addUsersGroupModule.adapters.OnUserRemovedListener

class AddUsersGroupFragment : Fragment(), OnUserRemovedListener {

    private lateinit var mBinding: FragmentAddUsersGroupBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mLayoutManager2: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<User, UserHolder>

    private lateinit var mAddedUsersAdapter: AddedUsersListAdapter

    private var mAddedUsersList: ArrayList<User> = ArrayList()

    private val mAddedUsersListLiveData: LiveData<ArrayList<User>> = MutableLiveData(mAddedUsersList)

    private var mAlreadyGroup: Boolean = false

    private lateinit var mGroupId: String

    inner class UserHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemUserBinding.bind(view)

        @SuppressLint("NotifyDataSetChanged")
        fun setListener(user: User) {
            binding.root.setOnClickListener {
                if (mAddedUsersList.contains(user)) {
                    binding.ivCheck.visibility = View.GONE
                    mAddedUsersList.remove(user)
                } else {
                    binding.ivCheck.visibility = View.VISIBLE
                    mAddedUsersList.add(user)
                }

                mAddedUsersAdapter.notifyDataSetChanged()
                mAddedUsersAdapter.submitList(mAddedUsersList)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentAddUsersGroupBinding.inflate(inflater, container, false)

        mAlreadyGroup = arguments?.getBoolean("alreadyGroup") ?: false

        if (!mAlreadyGroup) {
            (activity as? MainActivity)?.hideBottomNav()
        } else {
            mGroupId = arguments?.getString("groupId") ?: ""

            // Get users from group
            val query = FirebaseFirestore.getInstance()
                .collection("groups")
                .document(mGroupId)

            query.get().addOnSuccessListener { document ->
                val group = document.toObject(Group::class.java)

                if (group != null) {
                    for (userId in group.users!!) {
                        val userRef =
                            FirebaseFirestore.getInstance().collection("users").document(userId)

                        userRef.get().addOnSuccessListener { document ->
                            val user = document.toObject(User::class.java)

                            if (user != null) {
                                mAddedUsersList.add(user)
                            }
                        }.addOnCompleteListener {
                            mAddedUsersList.removeIf { user -> user.id == FirebaseAuth.getInstance().currentUser?.uid }

                            mAddedUsersAdapter.notifyDataSetChanged()
                            mAddedUsersAdapter.submitList(mAddedUsersList)

                            mFirebaseAdapter.notifyDataSetChanged()
                        }
                    }
                }
            }
        }

        setupToolBar()

        setupRecyclerViewUsersAdded()

        setupSearchView()

        mAddedUsersAdapter.submitList(mAddedUsersList)

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isAdded) {
            return
        }

        mLayoutManager = LinearLayoutManager(context)

        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("publicProfile", true)
            .whereNotEqualTo("email", FirebaseAuth.getInstance().currentUser?.email)
            .orderBy("username")

        val options = FirestoreRecyclerOptions.Builder<User>()
            .setQuery(query, User::class.java)
            .build()

        mFirebaseAdapter = object : FirestoreRecyclerAdapter<User, UserHolder>(options){

            private lateinit var context: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserHolder {
                context = parent.context

                val view = LayoutInflater.from(context)
                    .inflate(R.layout.item_user, parent, false)

                return UserHolder(view)
            }

            override fun onBindViewHolder(holder: UserHolder, position: Int, model: User) {
                val user = getItem(position)

                with(holder) {
                    setListener(user)

                    binding.tvUserName.text = user.username

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

                    if (mAddedUsersList.contains(user)) {
                        binding.ivCheck.visibility = View.VISIBLE
                    } else {
                        binding.ivCheck.visibility = View.GONE
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

        mBinding.rvUsers.apply {
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

    override fun onUserRemoved(user: User) {
        val query = FirebaseFirestore.getInstance()
            .collection("users")
            .whereEqualTo("publicProfile", true)
            .whereNotEqualTo("email", FirebaseAuth.getInstance().currentUser?.email)
            .orderBy("username")

        val options = FirestoreRecyclerOptions.Builder<User>()
            .setQuery(query, User::class.java)
            .build()

        mFirebaseAdapter.updateOptions(options)
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolBar)

        if (mAlreadyGroup) {
            mBinding.toolBar.title = "Añadir usuarios"
            mBinding.tvAddUsersTitle.visibility = View.GONE
        } else {
            mBinding.toolBar.title = "Crear grupo"
        }

        mBinding.toolBar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        mBinding.ivCheck.setOnClickListener {
            if (mAlreadyGroup) {
                mAddedUsersList.add(User(FirebaseAuth.getInstance().currentUser?.uid!!))

                val groupRef = FirebaseFirestore.getInstance().collection("groups").document(mGroupId)
                groupRef.update("users", mAddedUsersList.map { user -> user.id })
                    .addOnSuccessListener {
                        findNavController().navigateUp()
                    }
            } else {
                // Pasar lista de usuarios a array de ids
                val usersIdList = mAddedUsersList.map { user -> user.id }

                findNavController().navigate(
                    R.id.action_addUsersGroupFragment_to_createGroupFragment,
                    Bundle().apply {
                        putStringArrayList("usersIdList", ArrayList(usersIdList))
                    })
            }
        }
    }

    private fun setupRecyclerViewUsersAdded() {
        mAddedUsersAdapter = AddedUsersListAdapter(mAddedUsersList, this)

        mLayoutManager2 = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        mBinding.rvUsersAdded.apply {
            setHasFixedSize(true)
            layoutManager = mLayoutManager2
            adapter = mAddedUsersAdapter
        }
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
                    val query = FirebaseFirestore.getInstance()
                        .collection("users")
                        .whereEqualTo("publicProfile", true)
                        .whereNotEqualTo("email", FirebaseAuth.getInstance().currentUser?.email)
                        .orderBy("username")
                        .startAt(newText.lowercase())
                        .endAt(newText.lowercase() + "\uf8ff")

                    val options = FirestoreRecyclerOptions.Builder<User>()
                        .setQuery(query, User::class.java)
                        .build()

                    mFirebaseAdapter.updateOptions(options)
                }
                return false
            }
        })
    }
}