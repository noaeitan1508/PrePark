package com.example.prepark

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        //אתחול החיבור לשרת פיירבייסו
        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val registerButton = findViewById<Button>(R.id.registerButton)

        // כפתור התחברות
        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            goToMainActivity()
                        } else {
                            Toast.makeText(this, "שגיאה: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show()
            }
        }

        // כפתור הרשמה למשתמש חדש
        registerButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "הרשמה הצליחה!", Toast.LENGTH_SHORT).show()
                            goToMainActivity()
                        } else {
                            Toast.makeText(this, "שגיאה: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            } else {
                Toast.makeText(this, "נא למלא אימייל וסיסמה", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToMainActivity() {
        // מעבר למסך הראשי של האפליקציה לאחר התחברות מוצלחת
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish() // סוגר את מסך ההתחברות
    }
}
