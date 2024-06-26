package com.inavarro.ridesync.mainModule

import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.inavarro.ridesync.BuildConfig
import com.inavarro.ridesync.R
import com.inavarro.ridesync.authModule.loginModule.LoginActivity
import com.inavarro.ridesync.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityMainBinding

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        // Initialize Firebase Auth and check if the user is signed in
        mAuth = Firebase.auth
        if (mAuth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup the Navigation Drawer
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        mBinding.bottomNavigationView.setupWithNavController(navController)
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in.
        if (mAuth.currentUser == null) {
            // Not signed in, launch the Sign In activity
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
    }

    fun hideBottomNav() {
        mBinding.bottomNavigationView.visibility = View.GONE
    }

    fun showBottomNav() {
        mBinding.bottomNavigationView.visibility = View.VISIBLE
    }

    fun hideFragmentContainerViewActivity() {
        mBinding.fragmentContainerViewActivity.visibility = View.GONE
    }

    fun showFragmentContainerViewActivity() {
        mBinding.fragmentContainerViewActivity.visibility = View.VISIBLE
    }
}