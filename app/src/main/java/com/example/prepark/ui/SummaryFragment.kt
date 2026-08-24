package com.example.prepark.ui

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prepark.databinding.FragmentSummaryBinding

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

        binding.btnBackToSearch.setOnClickListener {
            findNavController().popBackStack()
        }

        val passedAddress = arguments?.getString("address")
            ?: arguments?.getString("searchedAddress")
            ?: arguments?.getString("location")
            ?: arguments?.getString("destination")

        val pendingPref = requireActivity().getSharedPreferences("PendingBooking", Context.MODE_PRIVATE)
        val prefAddress = pendingPref.getString("searchedAddress", null)

        // --- שואבים את המידע שנשמר מגוגל, כולל המרחק ושעות הפעילות ---
        val closestParkingName = pendingPref.getString("closestParkingName", "לא נמצא חניון קרוב")
        val distanceMeters = pendingPref.getInt("distanceMeters", 0)
        val closestParkingHours = pendingPref.getString("closestParkingHours", "")

        val searchedAddress = passedAddress ?: prefAddress ?: "שדרות רוטשילד 58, תל אביב"

        if (searchedAddress.contains("חניון")) {
            binding.tvDynamicLocation.text = "חניון נבחר:\n$searchedAddress"

            // אם חיפשנו חניון ספציפי, אנחנו לא צריכים להראות מרחק ושעות אוטומטיות, אז מעלימים אותם
            binding.tvDistance.visibility = View.GONE
            (binding.tvDistance.parent as? View)?.visibility = View.GONE

            binding.tvParkingHours.visibility = View.GONE
            (binding.tvParkingHours.parent as? View)?.visibility = View.GONE

        } else {
            // אם זו כתובת, מציגים את שם החניון הכי קרוב
            binding.tvDynamicLocation.text = "החניון הקרוב ביותר ל$searchedAddress:\n$closestParkingName"

            // מדפיסים את המרחק במטרים ישר לתוך המסך
            binding.tvDistance.text = "$distanceMeters מטרים מהיעד"
            binding.tvDistance.visibility = View.VISIBLE
            (binding.tvDistance.parent as? View)?.visibility = View.VISIBLE

            // מדפיסים את שעות הפעילות ליד אייקון השעון שלך
            binding.tvParkingHours.text = closestParkingHours
            binding.tvParkingHours.visibility = View.VISIBLE
            (binding.tvParkingHours.parent as? View)?.visibility = View.VISIBLE
        }

        // הגדרת משתני הזמן
        val startTime = pendingPref.getLong("startTime", 0L)
        val endTime = pendingPref.getLong("endTime", 0L)

        // הצגת התאריך והשעות במסך הסיכום
        if (startTime > 0L && endTime > 0L) {
            val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
            val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

            val startStr = timeFormat.format(java.util.Date(startTime))
            val endStr = timeFormat.format(java.util.Date(endTime))
            val dateStr = dateFormat.format(java.util.Date(startTime))

            binding.tvDynamicDateAndTimes.text = "תאריך: $dateStr\nשעות: $startStr עד $endStr"
        }

        binding.btnReserveParking.setOnClickListener {

            if (!binding.cbTerms.isChecked) {
                Toast.makeText(requireContext(), "יש לאשר את תנאי השימוש בחניון", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (endTime > 0L) {
                // --- 1. שמירה לתוך היסטוריית החניות ---
                val historyPref = requireActivity().getSharedPreferences("ParkingHistory", Context.MODE_PRIVATE)
                val currentHistory = historyPref.getString("historyList", "") ?: ""

                val timeFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                val dateFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())

                val sTime = timeFormat.format(java.util.Date(startTime))
                val eTime = timeFormat.format(java.util.Date(endTime))
                val sDate = dateFormat.format(java.util.Date(startTime))

                // יוצר שורה חדשה ומוסיף אותה לראש הרשימה של ההיסטוריה
                val newEntry = "• $searchedAddress\nתאריך: $sDate | שעות: $sTime - $eTime\n\n"
                historyPref.edit().putString("historyList", newEntry + currentHistory).apply()

                // --- 2. טיפול בחניה עצמה ---
                val activePref = requireActivity().getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)

                val isFuture = startTime > System.currentTimeMillis() + 60000L

                activePref.edit()
                    .putBoolean("isActivated", !isFuture)
                    .putLong("startTime", startTime)
                    .putLong("endTime", endTime)
                    .apply()

                if (isFuture) {
                    Toast.makeText(requireContext(), "החניה שוריינה בהצלחה! יש להפעיל אותה באזור האישי כשתגיעי לחניון.", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "החניה הופעלה בהצלחה!", Toast.LENGTH_SHORT).show()
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