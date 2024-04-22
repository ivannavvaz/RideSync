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
import com.google.android.play.core.integrity.r
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.inavarro.ridesync.R
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
            val fullName = mBinding.etFullName.text.toString().trim().lowercase()
            val email = mBinding.etEmail.text.toString().trim()
            val password = mBinding.etPassword.text.toString().trim()
            val confirmPassword = mBinding.etConfirmPassword.text.toString().trim()

            if (validateFields(fullName, email, password, confirmPassword)) {
                signUpWithEmail(email, password, fullName)
            }
        }
    }

    private fun validateFields(fullName: String, email: String, password: String, confirmPassword: String): Boolean {

        if (fullName.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Completa todos los campos.", Toast.LENGTH_SHORT).show()
        } else {
            // Only letters and spaces
            if (!fullName.matches(Regex("^[a-zA-Z ]+\$"))) {
                Toast.makeText(this, "Nombre completo inválido.", Toast.LENGTH_SHORT).show()
            } else {
                // Validate email
                if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    Toast.makeText(this, "Correo electrónico inválido.", Toast.LENGTH_SHORT).show()
                } else {
                    // Validate password
                    if (password.length < 6) {
                        Toast.makeText(
                            this,
                            "La contraseña debe tener al menos 6 caracteres.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        return if (password != confirmPassword) {
                            Toast.makeText(
                                this,
                                "Las contraseñas no coinciden.",
                                Toast.LENGTH_SHORT
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

    private fun signUpWithEmail(email: String, password: String, fullName: String) {
        mAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(ContentValues.TAG, "createUserWithEmail:success")

                    val user = mAuth.currentUser
                    updateUI(user)

                    // Update name of the user
                    val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(fullName)
                        .build()

                    user!!.updateProfile(profileUpdates)

                    // Go to the next activity
                    intentToMainActivity()
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(
                        ContentValues.TAG,
                        "createUserWithEmail:failure",
                        task.exception
                    )

                    updateUI(null)
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

    private fun updateUI(user: FirebaseUser?) {
        if (user != null) {
            Toast.makeText(this, "Bienvenido.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Credenciales incorrectas.", Toast.LENGTH_SHORT).show()
        }
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

    private fun intentToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}