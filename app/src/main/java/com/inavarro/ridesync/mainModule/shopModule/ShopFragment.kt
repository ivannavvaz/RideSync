package com.inavarro.ridesync.mainModule.shopModule

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.Shop
import com.inavarro.ridesync.databinding.FragmentShopBinding
import com.inavarro.ridesync.mainModule.MainActivity

class ShopFragment : Fragment() {

    private lateinit var mBinding: FragmentShopBinding

    private lateinit var mShop : Shop

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        mBinding = FragmentShopBinding.inflate(layoutInflater)

        setupShopFragment()

        setupToolBar()

        setupShop()

        mBinding.btnCall.setOnClickListener {
            openPhone()
        }

        mBinding.btnVisit.setOnClickListener {
            openWeb()
        }

        mBinding.ivMap.setOnClickListener {
            openMap()
        }

        return mBinding.root
    }

    private fun setupShopFragment(){
        (activity as? MainActivity)?.hideBottomNav()
    }

    private fun setupToolBar() {
        (activity as AppCompatActivity).setSupportActionBar(mBinding.toolbar)

        mBinding.toolbar.title = ""
        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupShop(){
        // Get shop id
        val idShop = arguments?.getString("idShop")

        // Get shop data
        val shopRef = FirebaseFirestore.getInstance().collection("shops").document(idShop!!)
        shopRef.get().addOnSuccessListener {
            val shop = it.toObject(Shop::class.java)
            mShop = shop!!

            mBinding.tvName.text = shop.name
            mBinding.tvDescription.text = shop.description
            mBinding.tvPhone.text = shop.phone
            mBinding.tvEmail.text = shop.email
            mBinding.tvAddress.text = shop.address

            when (shop.type) {
                "itv" -> mBinding.tvType.text = "ITV"
                "tires" -> mBinding.tvType.text = "Neumáticos"
                "detailing" -> mBinding.tvType.text = "Detailing"
                "mechanic" -> mBinding.tvType.text = "Mecánica"
                "bodywork" -> mBinding.tvType.text = "Chapa y pintura"
                "wrap" -> mBinding.tvType.text = "Vinilado"
                "tuning" -> mBinding.tvType.text = "Tuning"
                "spareParts" -> mBinding.tvType.text = "Recambios"
                "others" -> mBinding.tvType.text = "Otros"
            }

            Glide.with(requireContext())
                .load(shop.photo)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .into(mBinding.ivPhoto)
        }
    }

    private fun openPhone() {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:${mShop.phone}")
        startActivity(intent)
    }

    private fun openWeb() {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(mShop.web)
        startActivity(intent)
    }

    private fun openMap() {
        val uri = Uri.parse("geo:0,0?q=${mShop.location?.latitude},${mShop.location?.longitude}(${mShop.name})")
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
    }
}