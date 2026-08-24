package com.example.prepark.ui

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prepark.R

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnBack = view.findViewById<TextView>(R.id.btnBack)
        btnBack.setOnClickListener {
            findNavController().popBackStack()
        }

        // שואב את ההיסטוריה האמיתית מהזיכרון של האפליקציה
        val tvHistoryList = view.findViewById<TextView>(R.id.tvHistoryList)
        val historyPref = requireActivity().getSharedPreferences("ParkingHistory", Context.MODE_PRIVATE)
        val historyData = historyPref.getString("historyList", "")

        if (historyData.isNullOrBlank()) {
            tvHistoryList.text = "אין היסטוריית חניות כרגע."
        } else {
            tvHistoryList.text = historyData
        }
    }
}