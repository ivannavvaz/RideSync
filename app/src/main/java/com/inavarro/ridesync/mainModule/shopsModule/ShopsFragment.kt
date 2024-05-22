package com.inavarro.ridesync.mainModule.shopsModule

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Shop
import com.inavarro.ridesync.databinding.FragmentShopsBinding
import com.inavarro.ridesync.databinding.ItemShopBinding
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.mainModule.shopModule.ShopFragment

class ShopsFragment : Fragment() {

    private lateinit var mBinding: FragmentShopsBinding

    private lateinit var mLayoutManager: RecyclerView.LayoutManager

    private lateinit var mFirestoreReference: CollectionReference

    private lateinit var mFirebaseAdapter: FirestoreRecyclerAdapter<Shop, ShopHolder>

    inner class ShopHolder(view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemShopBinding.bind(view)

        fun setListener(shop: Shop) {
            binding.root.setOnClickListener {
                openShop(shop.id)
            }

            binding.btnShop.setOnClickListener {
                openShop(shop.id)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentShopsBinding.inflate(layoutInflater)

        mBinding.progressBar.visibility = View.VISIBLE

        setupShopFragment()

        setupClicks()

        return mBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mLayoutManager = GridLayoutManager(context, 2)

        mFirestoreReference = FirebaseFirestore.getInstance().collection("shops")

        val options = FirestoreRecyclerOptions.Builder<Shop>()
            .setQuery(mFirestoreReference, Shop::class.java)
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
                emptyList()
            }

            override fun onError(e: FirebaseFirestoreException) {
                super.onError(e)

                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun setupShopFragment() {
        ((activity as? MainActivity)?.showBottomNav())
    }

    private fun setupClicks() {
        mBinding.chipAll.setOnClickListener {
            mFirestoreReference = FirebaseFirestore.getInstance().collection("shops")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(mFirestoreReference, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipITV.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "itv")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipTires.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "tires")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipDetailing.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "detailing")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipMechanics.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "mechanic")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipBodywork.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "bodywork")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipWrap.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "wrap")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipTuning.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "tuning")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipSpareParts.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "spareParts")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }

        mBinding.chipOthers.setOnClickListener {
            val query = FirebaseFirestore.getInstance().collection("shops")
                .whereEqualTo("type", "others")

            val options = FirestoreRecyclerOptions.Builder<Shop>()
                .setQuery(query, Shop::class.java)
                .build()

            mFirebaseAdapter.updateOptions(options)
        }
    }

    private fun emptyList() {
        if (mFirebaseAdapter.itemCount == 0) {
            mBinding.tvEmptyList.visibility = View.VISIBLE
            mBinding.ivEmptyList.visibility = View.VISIBLE
        } else {
            mBinding.tvEmptyList.visibility = View.GONE
            mBinding.ivEmptyList.visibility = View.GONE
        }
    }

    private fun openShop(shopId: String) {
        findNavController().navigate(ShopsFragmentDirections.actionShopsFragmentToShopFragment(shopId))
    }
}