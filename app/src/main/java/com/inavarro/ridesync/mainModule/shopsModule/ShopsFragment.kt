package com.inavarro.ridesync.mainModule.shopsModule

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Shop
import com.inavarro.ridesync.databinding.FragmentShopsBinding
import com.inavarro.ridesync.databinding.ItemShopBinding
import com.inavarro.ridesync.mainModule.MainActivity

class ShopsFragment : Fragment() {

    private lateinit var mBinding: FragmentShopsBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<Shop, ShopHolder>

    private lateinit var mQuery: Query

    private lateinit var mSharedPreferences: SharedPreferences

    inner class ShopHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemShopBinding.bind(view)

        fun setListener(shop: Shop) {
            binding.root.setOnClickListener {
                openShop(shop.id!!)
            }

            binding.btnShop.setOnClickListener {
                openShop(shop.id!!)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentShopsBinding.inflate(layoutInflater)

        setupShopFragment()

        setupClicks()

        mSharedPreferences = requireActivity().getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = GridLayoutManager(context, 2)

        // Select the chip that was selected before
        val chipSelected = mSharedPreferences.getInt("shopsChipSelected", R.id.chipAll)

        // Query the shop depending on the chip selected
        when (chipSelected) {
            R.id.chipAll -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
            }
            R.id.chipITV -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "itv")
            }
            R.id.chipTires -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "tires")
            }
            R.id.chipDetailing -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "detailing")
            }
            R.id.chipMechanics -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "mechanic")
            }
            R.id.chipBodywork -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "bodywork")
            }
            R.id.chipWrap -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "wrap")
            }
            R.id.chipTuning -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "tuning")
            }
            R.id.chipSpareParts -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "spareParts")
            }
            R.id.chipOthers -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
                    .whereEqualTo("type", "others")
            }
            else -> {
                mQuery = FirebaseFirestore.getInstance().collection("shops")
            }
        }

        // Check if the query has results
        mQuery.get().addOnSuccessListener {
            val numItems = it.size()
            emptyList(numItems)
        }

        val options = FirestoreRecyclerOptions.Builder<Shop>()
            .setQuery(mQuery, Shop::class.java)
            .build()

        mFirebaseAdapter = object : FirestoreRecyclerAdapter<Shop, ShopHolder>(options) {

            private lateinit var mContext: Context

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopHolder {
                mContext = parent.context

                val view = LayoutInflater.from(mContext).inflate(
                    com.inavarro.ridesync.R.layout.item_shop,
                    parent,
                    false
                )

                return ShopHolder(view)
            }

            override fun onBindViewHolder(holder: ShopHolder, position: Int, model: Shop) {
                val shop = getItem(position)

                with (holder) {
                    setListener(shop)

                    binding.tvName.text = shop.name

                    val type = shop.type
                    when (type) {
                        "itv" -> binding.tvType.text = "ITV"
                        "tires" -> binding.tvType.text = "Neumáticos"
                        "detailing" -> binding.tvType.text = "Detailing"
                        "mechanic" -> binding.tvType.text = "Mecánica"
                        "bodywork" -> binding.tvType.text = "Chapa y pintura"
                        "wrap" -> binding.tvType.text = "Vinilado"
                        "tuning" -> binding.tvType.text = "Tuning"
                        "spareParts" -> binding.tvType.text = "Recambios"
                        "others" -> binding.tvType.text = "Otros"
                    }

                    binding.tvCity.text = shop.city

                    if (shop.photo != null) {
                        Glide.with(mContext)
                            .load(shop.photo)
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .into(binding.ivPhoto)
                    } else {
                        binding.ivPhoto.setImageResource(com.inavarro.ridesync.R.drawable.ic_no_photography)
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

        mBinding.rvShop.apply {
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

    override fun onResume() {
        super.onResume()

        scrollToAndSelectSelectedChip()
    }

    private fun setupShopFragment() {
        ((activity as? MainActivity)?.showBottomNav())
        (activity as MainActivity).hideFragmentContainerViewActivity()

        mBinding.progressBar.visibility = View.VISIBLE
    }

    private fun setupClicks() {
        mBinding.chipAll.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipAll).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipITV.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipITV).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "itv")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipTires.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipTires).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "tires")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipDetailing.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipDetailing).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "detailing")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipMechanics.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipMechanics).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "mechanic")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipBodywork.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipBodywork).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "bodywork")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipWrap.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipWrap).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "wrap")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipTuning.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipTuning).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "tuning")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipSpareParts.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipSpareParts).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "spareParts")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipOthers.setOnClickListener {
            mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipOthers).apply()

            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "others")

            query.get().addOnSuccessListener {
                val numItems = it.size()
                emptyList(numItems)
            }

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
    }

    private fun openShop(shopId: String) {
        findNavController().navigate(
            ShopsFragmentDirections.actionShopsFragmentToShopFragment(shopId)
        )
    }

    private fun scrollToAndSelectSelectedChip() {
        val selectedChip = mBinding.chipGroup.findViewById<Chip>(mSharedPreferences.getInt("shopsChipSelected", R.id.chipAll))

        selectedChip.isChecked = true

        mBinding.horizontalScrollView.post {
            // Calculate the scroll position of the selected chip
            val scrollX = selectedChip.left - (mBinding.horizontalScrollView.width / 2) + (selectedChip.width / 2)

            // Scroll to the selected chip
            mBinding.horizontalScrollView.smoothScrollTo(scrollX, 0)
        }
    }

    private fun emptyList(itemCount: Int = 0) {
        // Show the empty list image and text if there are no items
        mBinding.ivEmptyList.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
        mBinding.tvEmptyList.visibility = if (itemCount == 0) View.VISIBLE else View.GONE
    }
}