package com.inavarro.ridesync.authModule.loginModule

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.inavarro.ridesync.mainModule.MainActivity
import com.inavarro.ridesync.R
import com.inavarro.ridesync.authModule.registerModule.RegisterActivity
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityLoginBinding

    private lateinit var mAuth: FirebaseAuth

    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mAuth = FirebaseAuth.getInstance()

        setupSharedPreferences()

        mBinding.btnLogin.setOnClickListener {
            signIn()
        }

        mBinding.btnGoogle.setOnClickListener {
            signInWithGoogle()
        }

        mBinding.btnRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun reload() {
        // Reload the current user
        mAuth.currentUser?.reload()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = mAuth.currentUser
                updateUI(user)
            }
        }
    }

    private fun setupSharedPreferences() {
        // Get the shared preferences
        mSharedPreferences = getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
        mSharedPreferences.edit().putInt("activitiesChipSelected", R.id.chipAll).apply()
        mSharedPreferences.edit().putInt("shopsChipSelected", R.id.chipAll).apply()

        // Check if the user has remembered the session
        if (mSharedPreferences.getBoolean("session", false)) {
            val email = mSharedPreferences.getString("email", "")
            val password = mSharedPreferences.getString("password", "")
            mBinding.etEmail.setText(email)
            mBinding.etPassword.setText(password)
            mBinding.cbRemember.isChecked = true
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            // If user is logged in
            Toast.makeText(this, "Bienvenido.", Toast.LENGTH_SHORT).show()
        } else {
            // If user is not logged in
            Snackbar.make(mBinding.root, "Credenciales incorrectas.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun signIn() {

        val email = mBinding.etEmail.text.toString().trim()
        val password = mBinding.etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Snackbar.make(mBinding.root, "Completa todos los campos.", Snackbar.LENGTH_SHORT).show()
        } else {
            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")

                        val user = mAuth.currentUser
                        updateUI(user)

                        // If user has remembered the session
                        if (mBinding.cbRemember.isChecked) {
                            mSharedPreferences.edit()
                                .putString("email", email)
                                .putString("password", password)
                                .putBoolean("session", true)
                                .apply()
                        } else {
                            mSharedPreferences.edit()
                                .putBoolean("session", false)
                                .apply()
                        }

                        // Go to the next activity
                        intentToMainActivity()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)

                        updateUI(null)
                    }
                }
        }
    }

    private fun signInWithGoogle() {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, googleConf)

        // Sign out before signing in
        googleClient.signOut()

        resultLauncher.launch(googleClient.signInIntent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(TAG, "signInWithCredential:success")

                            val user = mAuth.currentUser
                            updateUI(user)

                            // Check if user exists in Firestore
                            val userRef = FirebaseFirestore.getInstance().collection("users")
                            userRef.document(user!!.uid).get().addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // Create username from email
                                    val username = user.email?.lowercase()?.substringBefore("@")

                                    // Update username of the user
                                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                        .setDisplayName(username)
                                        .build()

                                    user.updateProfile(profileUpdates)

                                    // Insert user in Firestore
                                    val userDB = User(
                                        user.uid,
                                        user.displayName,
                                        user.email,
                                        username,
                                        user.photoUrl.toString()
                                    )

                                    userRef.document(user.uid).set(userDB)
                                }
                            }

                            // Go to the next activity
                           intentToMainActivity()
                        } else {
                            // If sign in fails, display a message to the user.
                            Log.w(TAG, "signInWithCredential:failure", task.exception)

                            updateUI(null)
                        }
                    }
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                updateUI(null)
            }
        }
    }

    private fun intentToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        // Close the current activity
        finish()
    }
}