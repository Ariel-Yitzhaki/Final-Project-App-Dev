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
    private var isRegisterMode = false

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

        // Restore register mode after recreation
        if (savedInstanceState != null) {
            isRegisterMode = savedInstanceState.getBoolean("isRegisterMode")
            if (isRegisterMode) {
                usernameInput.visibility = View.VISIBLE
                displayNameInput.visibility = View.VISIBLE
                primaryButton.text = getString(R.string.login_register)
                switchModeText.text = getString(R.string.login_switch_to_login)
            }
        }
    }

    // Switch between Login and Register modes
    private fun toggleMode() {
        isRegisterMode = !isRegisterMode

        if (isRegisterMode) {
            usernameInput.visibility = View.VISIBLE
            displayNameInput.visibility = View.VISIBLE
            primaryButton.text = getString(R.string.login_register)
            switchModeText.text = getString(R.string.login_switch_to_login)
        } else {
            usernameInput.visibility = View.GONE
            displayNameInput.visibility = View.GONE
            primaryButton.text = getString(R.string.login_title)
            switchModeText.text = getString(R.string.login_register_prompt)
        }
    }

    // Handle login or register based on current mode
    private fun handlePrimaryAction() {
        val email = emailInput.text.toString().trim()
        val password = passwordInput.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show()
            return
        }

        if (isRegisterMode) {
            val username = usernameInput.text.toString().trim()
            val displayName = displayNameInput.text.toString().trim()

            if (username.isEmpty() || displayName.isEmpty()) {
                Toast.makeText(this, getString(R.string.toast_email_password_required), Toast.LENGTH_SHORT).show()
                return
            }

            if (displayName.length > 25) {
                Toast.makeText(this, getString(R.string.toast_display_name_too_long), Toast.LENGTH_SHORT).show()
                return
            }

            register(email, password, username, displayName)
        } else {
            login(email, password)
        }
    }

    private fun login(email: String, password: String) {
        showLoading(true)

        lifecycleScope.launch {
            val result = authRepository.login(email, password)
            showLoading(false)

            result.fold(
                onSuccess = { goToMain() },
                onFailure = { e ->
                    Toast.makeText(this@LoginActivity, e.message ?: getString(R.string.toast_login_failed), Toast.LENGTH_SHORT).show()
                }
            )
        }
    }

    private fun register(email: String, password: String, username: String, displayName: String) {
        showLoading(true)

        lifecycleScope.launch {
            val result = authRepository.register(email, password, username, displayName)
            showLoading(false)

            result.fold(
                onSuccess = { goToMain() },
                onFailure = { e ->
                    Toast.makeText(this@LoginActivity, e.message ?: getString(R.string.toast_login_failed), Toast.LENGTH_SHORT).show()
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

    // Saves register mode so it survives rotation/recreation
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("isRegisterMode", isRegisterMode)
    }
}