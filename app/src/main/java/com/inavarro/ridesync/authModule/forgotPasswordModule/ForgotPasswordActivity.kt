package com.inavarro.ridesync.authModule.forgotPasswordModule

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.inavarro.ridesync.R
import com.inavarro.ridesync.authModule.loginModule.LoginActivity
import com.inavarro.ridesync.databinding.ActivityForgotPasswordBinding

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityForgotPasswordBinding

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        setupToolbar()

        mAuth = FirebaseAuth.getInstance()

        mBinding.btnSend.setOnClickListener {
            val email = mBinding.etEmail.text.toString()

            if (email.isNotEmpty()) {
                mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Email enviado", Toast.LENGTH_SHORT).show()
                    }
                    }
                    .addOnFailureListener { e ->
                        Snackbar.make(mBinding.root, "Error al enviar el email", Snackbar.LENGTH_SHORT).show()
                    }
            }
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(mBinding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = ""

        mBinding.toolbar.setNavigationIcon(R.drawable.ic_arrow_back)

        mBinding.toolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()

            finish()
        }
    }
}