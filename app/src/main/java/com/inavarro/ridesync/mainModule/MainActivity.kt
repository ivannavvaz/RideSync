package com.inavarro.ridesync.mainModule

import android.content.Intent
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
import com.inavarro.ridesync.R
import com.inavarro.ridesync.authModule.loginModule.LoginActivity
import com.inavarro.ridesync.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

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

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.navHostFragment) as NavHostFragment
        val navController = navHostFragment.navController
        mBinding.bottomNavigationView.setupWithNavController(navController)

        mBinding.navigationView.setNavigationItemSelectedListener(this)

        ActionBarDrawerToggle(this, mBinding.drawerLayout, mBinding.toolbar, R.string.open_drawer, R.string.close_drawer).apply {
            mBinding.drawerLayout.addDrawerListener(this)
            syncState()
        }
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

    fun getUidUser(): String {
        return mAuth.currentUser?.uid ?: ""
    }

    fun hideToolbar() {
        mBinding.toolbar.visibility = View.GONE
    }

    fun showToolbar() {
        mBinding.toolbar.visibility = View.VISIBLE
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        when (item.itemId) {
            R.id.nav_logout -> {
                signOut()
            }
            R.id.nav_edit_profile -> {
                findNavController(R.id.navHostFragment).navigate(R.id.editProfileFragment)
            }
        }

        mBinding.drawerLayout.closeDrawers()

        mBinding.drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                // Respond when the drawer's position changes
            }

            override fun onDrawerOpened(drawerView: View) {
                // Respond when the drawer is opened
            }

            override fun onDrawerClosed(drawerView: View) {
                item.isChecked = false
            }

            override fun onDrawerStateChanged(newState: Int) {
                // Respond when the drawer motion state changes
            }
        })

        return true
    }

    private fun signOut() {
        mAuth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}