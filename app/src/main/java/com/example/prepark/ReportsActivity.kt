package com.example.prepark // ודאי שזה תואם לשם ה-package שלך

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class ReportsActivity : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private val reportsList = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reports)

        // שולפים איזה חניון הלקוח הזמין
        val activePref = getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
        val parkingName = activePref.getString("parkingName", "חניון כללי") ?: "חניון כללי"

        // מנקים תווים שאסור לכתוב בפיירבייס
        val safeParkingName = parkingName.replace("[.#$\\[\\]]".toRegex(), "")

        // מתחברים ספציפית לתיקייה של החניון הזה בפיירבייס!
        database = FirebaseDatabase.getInstance().getReference("Reports").child(safeParkingName)

        val reportsListView = findViewById<ListView>(R.id.reportsListView)
        val reportEditText = findViewById<EditText>(R.id.reportEditText)
        val sendButton = findViewById<Button>(R.id.sendButton)
        val backButton = findViewById<TextView>(R.id.backButton)
        val tvReportTitle = findViewById<TextView>(R.id.tvReportTitle)
        tvReportTitle.text = "דיווחי נהגים:\n$parkingName"

        // כפתור חזור החדש שלך (החץ)
        backButton.setOnClickListener {
            finish()
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, reportsList)
        reportsListView.adapter = adapter

        // קריאת הנתונים של החניון הספציפי הזה בזמן אמת
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                reportsList.clear()
                for (child in snapshot.children) {
                    val message = child.getValue(String::class.java)
                    if (message != null) {
                        reportsList.add(message)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ReportsActivity, "שגיאה בטעינת נתונים", Toast.LENGTH_SHORT).show()
            }
        })

        // שליחת דיווח לחניון הספציפי
        sendButton.setOnClickListener {
            val message = reportEditText.text.toString()
            if (message.isNotEmpty()) {
                val newReportRef = database.push()
                newReportRef.setValue(message).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        reportEditText.text.clear()
                    } else {
                        Toast.makeText(this, "שגיאה בשליחה", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}