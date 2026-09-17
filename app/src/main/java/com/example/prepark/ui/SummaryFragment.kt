package com.example.prepark.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prepark.R
import com.example.prepark.databinding.FragmentSummaryBinding
import com.google.firebase.database.FirebaseDatabase

class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // כפתור חזור
        binding.btnBackToSearch.setOnClickListener {
            findNavController().popBackStack()
        }

        val passedAddress = arguments?.getString("address")
            ?: arguments?.getString("searchedAddress")
            ?: arguments?.getString("location")
            ?: arguments?.getString("destination")

        val pendingPref = requireActivity().getSharedPreferences("PendingBooking", Context.MODE_PRIVATE)
        val prefAddress = pendingPref.getString("searchedAddress", null)
        val closestParkingName = pendingPref.getString("closestParkingName", "לא נמצא חניון קרוב")
        val distanceMeters = pendingPref.getInt("distanceMeters", 0)
        val closestParkingHours = pendingPref.getString("closestParkingHours", "")

        val searchedAddress = passedAddress ?: prefAddress ?: "שדרות רוטשילד 58, תל אביב"
        val actualParkingName = if (searchedAddress.contains("חניון")) searchedAddress else closestParkingName

        // --- הצגת הנתונים (שם חניון, מרחק, תאריכים ושעות) ---
        if (searchedAddress.contains("חניון")) {
            binding.tvDynamicLocation.text = "חניון נבחר:\n$actualParkingName"
            binding.tvDistance.visibility = View.GONE
            (binding.tvDistance.parent as? View)?.visibility = View.GONE
            binding.tvParkingHours.visibility = View.GONE
            (binding.tvParkingHours.parent as? View)?.visibility = View.GONE
        } else {
            binding.tvDynamicLocation.text = "החניון הקרוב ביותר ל$searchedAddress:\n$actualParkingName"
            binding.tvDistance.text = "$distanceMeters מטרים מהיעד"
            binding.tvDistance.visibility = View.VISIBLE
            (binding.tvDistance.parent as? View)?.visibility = View.VISIBLE
            binding.tvParkingHours.text = closestParkingHours
            binding.tvParkingHours.visibility = View.VISIBLE
            (binding.tvParkingHours.parent as? View)?.visibility = View.VISIBLE
        }

        val startTime = pendingPref.getLong("startTime", 0L)
        val endTime = pendingPref.getLong("endTime", 0L)

        if (startTime > 0L && endTime > 0L) {
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
            val startStr = timeFormat.format(java.util.Date(startTime))
            val endStr = timeFormat.format(java.util.Date(endTime))
            val dateStr = dateFormat.format(java.util.Date(startTime))
            binding.tvDynamicDateAndTimes.text = "תאריך: $dateStr\nשעות: $startStr עד $endStr"
        }

        // --- מערכת דיווחי נהגים בזמן אמת (פיירבייס) ---
        val safeParkingName = actualParkingName.toString().replace("[.#$\\[\\]]".toRegex(), "")
        val dbRef = FirebaseDatabase.getInstance().getReference("Reports").child(safeParkingName)

        dbRef.limitToLast(10).get().addOnSuccessListener { snapshot ->
            var busyCount = 0
            var freeCount = 0
            var totalReports = 0
            var latestMessage = ""

            for (child in snapshot.children) {
                val msg = child.getValue(String::class.java) ?: ""
                latestMessage = msg
                totalReports++

                if (msg.contains("מלא") || msg.contains("עמוס") || msg.contains("אין") || msg.contains("תפוס")) {
                    busyCount++
                } else if (msg.contains("פנוי") || msg.contains("יש") || msg.contains("ריק")) {
                    freeCount++
                }
            }

            if (totalReports == 0) {
                binding.tvRealTimeStatus.text = "אין עדיין דיווחים על החניון הזה ℹ️"
                binding.tvRealTimeStatus.setTextColor(Color.DKGRAY)
            } else {
                var displayText = "דיווח אחרון: \"$latestMessage\"\n"
                if (busyCount > 0) displayText += "(מתוך הדיווחים האחרונים, $busyCount נהגים דיווחו על עומס)"
                else if (freeCount > 0) displayText += "(מתוך הדיווחים האחרונים, $freeCount נהגים דיווחו שיש מקום)"

                binding.tvRealTimeStatus.text = displayText

                if (latestMessage.contains("מלא") || latestMessage.contains("עמוס") || latestMessage.contains("אין") || latestMessage.contains("תפוס")) {
                    binding.tvRealTimeStatus.setTextColor(Color.RED)
                } else if (latestMessage.contains("פנוי") || latestMessage.contains("יש") || latestMessage.contains("ריק")) {
                    binding.tvRealTimeStatus.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    binding.tvRealTimeStatus.setTextColor(Color.parseColor("#FF9800"))
                }
            }
        }.addOnFailureListener {
            binding.tvRealTimeStatus.text = "לא ניתן לטעון סטטוס תפוסה"
            binding.tvRealTimeStatus.setTextColor(Color.GRAY)
        }

        // --- לוגיקת סיום שריון בחירת תשלום (פנגו/אשראי) ---
        binding.btnReserveParking.setOnClickListener {
            if (!binding.cbTerms.isChecked) {
                Toast.makeText(requireContext(), "יש לאשר את תנאי השימוש בחניון", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (endTime > 0L) {
                // זיהוי שיטת התשלום שנבחרה
                val selectedPaymentId = binding.rgPaymentMethod.checkedRadioButtonId
                val paymentMethod = if (selectedPaymentId == R.id.rbPango) "פנגו (Pango)" else "כרטיס אשראי"

                val historyPref = requireActivity().getSharedPreferences("ParkingHistory", Context.MODE_PRIVATE)
                val currentHistory = historyPref.getString("historyList", "") ?: ""
                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                val sTime = timeFormat.format(java.util.Date(startTime))
                val eTime = timeFormat.format(java.util.Date(endTime))
                val sDate = dateFormat.format(java.util.Date(startTime))

                val newEntry = "• $actualParkingName\nתאריך: $sDate | שעות: $sTime - $eTime\n\n"
                historyPref.edit().putString("historyList", newEntry + currentHistory).apply()

                val activePref = requireActivity().getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
                val isFuture = startTime > System.currentTimeMillis() + 60000L

                activePref.edit()
                    .putBoolean("isActivated", !isFuture)
                    .putLong("startTime", startTime)
                    .putLong("endTime", endTime)
                    .putString("parkingName", actualParkingName)
                    .putString("paymentMethod", paymentMethod)
                    .apply()

                if (isFuture) {
                    Toast.makeText(requireContext(), "החניה שוריינה בהצלחה (שולם ב-$paymentMethod)!\nיש להפעיל אותה באזור האישי כשתגיעי לחניון.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "החניה הופעלה בהצלחה (שולם ב-$paymentMethod)!", Toast.LENGTH_SHORT).show()
                }
                findNavController().popBackStack()
            } else {
                Toast.makeText(requireContext(), "שגיאה: לא נבחרו זמני חניה", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}