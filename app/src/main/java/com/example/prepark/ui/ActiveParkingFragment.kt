package com.example.prepark.ui

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prepark.databinding.FragmentActiveParkingBinding

class ActiveParkingFragment : Fragment() {

    private var _binding: FragmentActiveParkingBinding? = null
    private val binding get() = _binding!!

    private val timerHandler = Handler(Looper.getMainLooper())
    private var startTime: Long = 0
    private var endTime: Long = 0
    private var durationMillis: Long = 0
    private var isActivated = false
    private var isTimerRunning = false
    private var displayHour: String = ""

    private val timerRunnable = object : Runnable {
        override fun run() {
            if (!isTimerRunning || _binding == null) return

            val now = System.currentTimeMillis()

            if (now <= endTime) {
                val diff = endTime - now
                updateTimerText(diff)
            } else {
                binding.tvMainTimer.text = "00:00:00"
                binding.tvTimerStatus.text = "החניה הסתיימה!"
                binding.tvTimerStatus.setTextColor(Color.parseColor("#9E9E9E"))
                clearBooking()
                isTimerRunning = false
                return
            }
            if (isTimerRunning) {
                timerHandler.postDelayed(this, 1000)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentActiveParkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireContext().getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE)
        startTime = sharedPref.getLong("startTime", 0)
        endTime = sharedPref.getLong("endTime", 0)
        durationMillis = sharedPref.getLong("durationMillis", 0)
        isActivated = sharedPref.getBoolean("isActivated", false)
        displayHour = sharedPref.getString("displayHour", "") ?: ""

        if (startTime == 0L) {
            // אם אין שום נתון בזיכרון
            binding.tvTimerStatus.text = "אין חניה פעילה כרגע"
            binding.tvMainTimer.visibility = View.GONE
            binding.btnAction.visibility = View.GONE
        } else {
            binding.btnAction.visibility = View.VISIBLE
            updateUIState()
        }

        binding.btnClose.setOnClickListener {
            findNavController().popBackStack()
        }

        binding.btnAction.setOnClickListener {
            if (!isActivated) {
                // לחיצה על הכפתור "הפעל שריון חניה"
                val now = System.currentTimeMillis()
                isActivated = true
                endTime = now + durationMillis

                sharedPref.edit()
                    .putBoolean("isActivated", true)
                    .putLong("endTime", endTime)
                    .apply()

                updateUIState()
                Toast.makeText(requireContext(), "החניה הופעלה בהצלחה! השעון מתחיל לרוץ", Toast.LENGTH_SHORT).show()
            } else {
                // לחיצה על "סיום חניה / ביטול"
                stopTimer()
                clearBooking()
                Toast.makeText(requireContext(), "החניה הסתיימה!", Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }
    }

    private fun updateUIState() {
        if (!isActivated) {
            // מצב עתידי, המתנה ללקוח שילחץ על הכפתור
            binding.tvMainTimer.visibility = View.GONE
            binding.tvTimerStatus.text = "חניה משוריינת החל מהשעה $displayHour"
            binding.tvTimerStatus.setTextColor(Color.parseColor("#2196F3"))

            binding.btnAction.text = "הפעל שריון חניה"
            binding.btnAction.setBackgroundColor(Color.parseColor("#4CAF50"))
        } else {
            // מצב פעיל, השעון סופר לאחור (קורה אוטומטית בזמן אמת, או אחרי לחיצה בעתידי)
            binding.tvMainTimer.visibility = View.VISIBLE
            binding.tvTimerStatus.text = "החניה פעילה! זמן נותר:"
            binding.tvTimerStatus.setTextColor(Color.parseColor("#4CAF50"))

            binding.btnAction.text = "סיום חניה / ביטול"
            binding.btnAction.setBackgroundColor(Color.parseColor("#F44336"))

            if (!isTimerRunning) {
                isTimerRunning = true
                timerHandler.post(timerRunnable)
            }
        }
    }

    private fun clearBooking() {
        requireContext().getSharedPreferences("ActiveBooking", Context.MODE_PRIVATE).edit().clear().apply()
    }

    override fun onResume() {
        super.onResume()
        if (startTime != 0L && isActivated && !isTimerRunning) {
            isTimerRunning = true
            timerHandler.post(timerRunnable)
        }
    }

    override fun onPause() {
        super.onPause()
        stopTimer()
    }

    private fun stopTimer() {
        isTimerRunning = false
        timerHandler.removeCallbacks(timerRunnable)
    }

    private fun updateTimerText(diffInMillis: Long) {
        val seconds = (diffInMillis / 1000) % 60
        val minutes = (diffInMillis / (1000 * 60)) % 60
        val hours = (diffInMillis / (1000 * 60 * 60)) % 24
        binding.tvMainTimer.text = String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        stopTimer()
        _binding = null
    }
}