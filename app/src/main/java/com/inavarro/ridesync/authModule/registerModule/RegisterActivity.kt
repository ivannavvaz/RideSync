package com.inavarro.ridesync.authModule.registerModule

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
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
import com.inavarro.ridesync.R
import com.inavarro.ridesync.common.entities.User
import com.inavarro.ridesync.databinding.ActivityRegisterBinding
import com.inavarro.ridesync.mainModule.MainActivity

class RegisterActivity : AppCompatActivity() {

    private lateinit var mBinding: ActivityRegisterBinding

    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mBinding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(mBinding.root)

        mAuth = FirebaseAuth.getInstance()

        mBinding.btnGoogle.setOnClickListener {
            signUpWithGoogle()
        }

        mBinding.btnSignUp.setOnClickListener {
            val fullName = mBinding.etFullName.text.toString().lowercase().trim()
            val email = mBinding.etEmail.text.toString().trim()
            val username = mBinding.etEmail.text.toString().lowercase().trim().substringBefore("@")
            val password = mBinding.etPassword.text.toString().trim()
            val confirmPassword = mBinding.etConfirmPassword.text.toString().trim()

            if (validateFields(fullName, email, password, confirmPassword)) {
                signUpWithEmail(fullName, email, username, password)
            }
        }
    }

    private fun validateFields(fullName: String, email: String, password: String, confirmPassword: String): Boolean {

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Snackbar.make(mBinding.root, "Completa todos los campos.", Snackbar.LENGTH_SHORT).show()
        } else {
            // Only letters and spaces
            if (!fullName.matches(Regex("^[a-zA-Z ]+\$"))) {
                Snackbar.make(mBinding.root, "Nombre completo inválido.", Snackbar.LENGTH_SHORT).show()
            } else {
                // Validate email
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Snackbar.make(mBinding.root, "Correo electrónico inválido.", Snackbar.LENGTH_SHORT).show()
                } else {
                    // Validate password
                    if (password.length < 6) {
                        Snackbar.make(
                            mBinding.root,
                            "La contraseña debe tener al menos 6 caracteres.",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    } else {
                        return if (password != confirmPassword) {
                            Snackbar.make(
                                mBinding.root,
                                "Las contraseñas no coinciden.",
                                Snackbar.LENGTH_SHORT
                            ).show()

                            false
                        } else {
                            true
                        }
                    }
                }
            }
        }

        return false
    }

    private fun signUpWithEmail(fullName: String, email: String, username: String, password: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(ContentValues.TAG, "createUserWithEmail:success")

                    val user = mAuth.currentUser
                    updateUI(user)

                    // Update username of the user
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(username)
                        .build()

                    user!!.updateProfile(profileUpdates)

                    // Insert user in Firestore
                    val userDB = User(
                        user.uid,
                        fullName,
                        email,
                        username,
                        null
                    )

                    val userRef = FirebaseFirestore.getInstance().collection("users")
                    userRef.document(user.uid).set(userDB)

                    // Go to the next activity
                    intentToMainActivity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(
                        ContentValues.TAG,
                        "createUserWithEmail:failure",
                        task.exception
                    )

                    if (task.exception is com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                        Snackbar.make(mBinding.root, "El correo electrónico ya está en uso.", Snackbar.LENGTH_SHORT).show()
                    } else {
                        updateUI(null)
                    }
                }
            }
    }

    private fun signUpWithGoogle() {
        val googleConf = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        val googleClient = GoogleSignIn.getClient(this, googleConf)
        googleClient.signOut()

        resultLauncher.launch(googleClient.signInIntent)
    }

    private var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(ContentValues.TAG, "firebaseAuthWithGoogle:" + account.id)

                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().signInWithCredential(credential)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Sign in success, update UI with the signed-in user's information
                            Log.d(ContentValues.TAG, "signInWithCredential:success")

                            val user = mAuth.currentUser
                            updateUI(user)

                            // Check if user exists in Firestore
                            val userRef = FirebaseFirestore.getInstance().collection("users")
                            userRef.document(user!!.uid).get().addOnSuccessListener { document ->
                                if (!document.exists()) {
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
                            Log.w(ContentValues.TAG, "signInWithCredential:failure", task.exception)

                            updateUI(null)
                        }
                    }
            } catch (e: ApiException) {
                Log.w(ContentValues.TAG, "Google sign in failed", e)
                updateUI(null)
            }
        }
    }

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Bienvenido.", Toast.LENGTH_SHORT).show()
        } else {
            Snackbar.make(mBinding.root, "Credenciales incorrectas.", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun intentToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}