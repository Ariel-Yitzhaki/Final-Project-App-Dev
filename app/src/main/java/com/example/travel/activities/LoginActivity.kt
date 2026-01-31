package com.example.travel.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.travel.R
import com.example.travel.data.AuthRepository
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var authRepository: AuthRepository
    private lateinit var emailInput: EditText
    private lateinit var usernameInput: EditText
    private lateinit var displayNameInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var primaryButton: Button
    private lateinit var switchModeText: TextView
    private lateinit var progressBar: ProgressBar
    private var isSignUpMode = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        authRepository = AuthRepository()

        // If already logged in, skip to MainActivity
        if (authRepository.getCurrentUser() != null) {
            goToMain()
            return
        }

        setContentView(R.layout.activity_login)

        // Bind views
        emailInput = findViewById(R.id.emailInput)
        usernameInput = findViewById(R.id.usernameInput)
        displayNameInput = findViewById(R.id.displayNameInput)
        passwordInput = findViewById(R.id.passwordInput)
        primaryButton = findViewById(R.id.primaryButton)
        switchModeText = findViewById(R.id.switchModeText)
        progressBar = findViewById(R.id.progressBar)

        primaryButton.setOnClickListener { handlePrimaryAction() }
        switchModeText.setOnClickListener { toggleMode() }
    }

    // Switch between Sign In and Sign Up modes
    private fun toggleMode() {
        isSignUpMode = !isSignUpMode

        if (isSignUpMode) {
            usernameInput.visibility = View.VISIBLE
            displayNameInput.visibility = View.VISIBLE
            primaryButton.text = "Sign Up"
            switchModeText.text = "Already have an account? Sign In"
        } else {
            usernameInput.visibility = View.GONE
            displayNameInput.visibility = View.GONE
            primaryButton.text = "Sign In"
            switchModeText.text = "Don't have an account? Sign Up"
        }
    }

    // Handle sign in or sign up based on current mode
    private fun handlePrimaryAction() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
            return
        }

        if (isSignUpMode) {
            val username = usernameInput.text.toString().trim()
            val displayName = displayNameInput.text.toString().trim()

            if (username.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, "All fields required", Toast.LENGTH_SHORT).show()
                return
            }

            signUp(email, password, username, displayName)
        } else {
            signIn(email, password)
        }
    }

    private fun signIn(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            val result = authRepository.signIn(email, password)
            showLoading(false)

            result.fold(
                onSuccess = { goToMain() },
                onFailure = { e ->
                    Toast.makeText(this@LoginActivity, e.message ?: "Sign in failed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun signUp(email: String, password: String, username: String, displayName: String) {
        showLoading(true)

        lifecycleScope.launch {
            val result = authRepository.signUp(email, password, username, displayName)
            showLoading(false)

            result.fold(
                onSuccess = { goToMain() },
                onFailure = { e ->
                    Toast.makeText(this@LoginActivity, e.message ?: "Sign up failed", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
        primaryButton.isEnabled = !show
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}