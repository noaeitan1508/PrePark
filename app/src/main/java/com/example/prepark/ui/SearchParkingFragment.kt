package com.example.prepark.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.prepark.MainActivity
import com.example.prepark.R
import com.example.prepark.databinding.FragmentSearchParkingBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import android.location.Location
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SearchParkingFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentSearchParkingBinding? = null
    private val binding get() = _binding!!
    private var googleMap: GoogleMap? = null

    //  מחכה שהלקוח יבחר כתובת מהחלון של גוגל, ואז מפעיל את המפה
    private val startAutocomplete = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.let {
                val place = Autocomplete.getPlaceFromIntent(it)
                val address = place.address ?: place.name ?: ""

                // מדפיס את הכתובת לשורה, ומפעיל את חישוב המרחקים
                binding.etSearchAddress.setText(address)
                searchAddressAndFindParking(address)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchParkingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // --- 1. הפעלת מנגנון המקומות של גוגל  ---
        val applicationInfo = requireContext().packageManager.getApplicationInfo(requireContext().packageName, PackageManager.GET_META_DATA)
        val apiKey = applicationInfo.metaData.getString("com.google.android.geo.API_KEY")
        if (!Places.isInitialized() && apiKey != null) {
            Places.initialize(requireContext(), apiKey)
        }

        // --- 2. הדלקת המפה ---
        binding.mapView.onCreate(savedInstanceState)
        binding.mapView.getMapAsync(this)

        binding.etSearchAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    binding.tvDynamicTitle.text = "הזן יעד או שם חניון"
                } else {
                    binding.tvDynamicTitle.text = s.toString()
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // --- לחיצה על השורה תפתח את החלון הרשמי של גוגל ---
        binding.etSearchAddress.isFocusable = false
        binding.etSearchAddress.isClickable = true
        binding.etSearchAddress.setOnClickListener {
            // הגדרתי לגוגל להביא את הכתובת, השם והקואורדינטות
            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

            // פותח את חלון ההשלמה האוטומטית
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                .setCountry("IL")
                .build(requireContext())
            startAutocomplete.launch(intent)
        }

        binding.btnOrderNow.setOnClickListener {
            val address = binding.etSearchAddress.text.toString().trim()
            if (address.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "יש להזין את כתובת היעד", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showTimePickerDialog(isFuture = false)
        }

        binding.btnFutureBooking.setOnClickListener {
            val address = binding.etSearchAddress.text.toString().trim()
            if (address.isEmpty()) {
                android.widget.Toast.makeText(requireContext(), "יש להזין את כתובת היעד", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showTimePickerDialog(isFuture = true)
        }

        binding.btnPersonalArea.setOnClickListener {
            (activity as? MainActivity)?.showPersonalAreaDialog()
        }
    }

    private fun searchAddressAndFindParking(addressName: String) {
        val geocoder = Geocoder(requireContext(), Locale("he", "IL"))
        try {
            val addresses = geocoder.getFromLocationName(addressName, 1)
            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                val destLatLng = LatLng(location.latitude, location.longitude)

                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 15f))
                googleMap?.clear()

                googleMap?.addMarker(MarkerOptions()
                    .position(destLatLng)
                    .title("היעד: $addressName")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))

                // רשימת החניונים עם שעות הפעילות שלהם
                val preparkLots = listOf(
                    Triple("חניון קניון ערים", LatLng(32.1751, 34.9079), "פעיל עד 23:00"),
                    Triple("חניון הבימה", LatLng(32.0736, 34.7732), "פעיל 24/7")
                )

                var closestLotName = ""
                var closestDistanceMeters = Float.MAX_VALUE
                var closestLatLng: LatLng? = null
                var closestLotHours = ""

                for (lot in preparkLots) {
                    val results = FloatArray(1)
                    Location.distanceBetween(
                        destLatLng.latitude, destLatLng.longitude,
                        lot.second.latitude, lot.second.longitude,
                        results
                    )
                    if (results[0] < closestDistanceMeters) {
                        closestDistanceMeters = results[0]
                        closestLotName = lot.first
                        closestLatLng = lot.second
                        closestLotHours = lot.third
                    }
                }

                closestLatLng?.let {
                    googleMap?.addMarker(MarkerOptions()
                        .position(it)
                        .title("החניון הקרוב: $closestLotName"))
                }

                val sharedPref = requireContext().getSharedPreferences("PendingBooking", Context.MODE_PRIVATE)
                sharedPref.edit()
                    .putString("closestParkingName", closestLotName)
                    .putInt("distanceMeters", closestDistanceMeters.toInt())
                    .putString("closestParkingHours", closestLotHours) // שומרים את השעות של החניון שנמצא
                    .apply()

            } else {
                android.widget.Toast.makeText(requireContext(), "לא מצאנו את הכתובת, נסה משהו אחר", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val israelCenter = LatLng(31.4117, 35.0818)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(israelCenter, 7.5f))
    }

    private fun showTimePickerDialog(isFuture: Boolean) {
        val dialog = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.bottom_sheet_time_picker, null)
        dialog.setContentView(view)

        val btnCloseBottomSheet = view.findViewById<ImageView>(R.id.btnCloseBottomSheet)
        btnCloseBottomSheet.setOnClickListener {
            dialog.dismiss()
        }

        val layoutArrivalTiming = view.findViewById<LinearLayout>(R.id.layoutArrivalTiming)
        val tvPickerTitle = view.findViewById<TextView>(R.id.tvPickerTitle)

        val pickerDay = view.findViewById<NumberPicker>(R.id.pickerDay)
        val pickerHour = view.findViewById<NumberPicker>(R.id.pickerHour)
        val pickerMinute = view.findViewById<NumberPicker>(R.id.pickerMinute)

        val pickerDurationHours = view.findViewById<NumberPicker>(R.id.pickerDurationHours)
        val pickerDurationMinutes = view.findViewById<NumberPicker>(R.id.pickerDurationMinutes)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmTime)

        pickerDurationHours.minValue = 0
        pickerDurationHours.maxValue = 24
        pickerDurationHours.value = 2

        val minuteOptions = arrayOf("00", "15", "30", "45")
        pickerDurationMinutes.minValue = 0
        pickerDurationMinutes.maxValue = minuteOptions.size - 1
        pickerDurationMinutes.displayedValues = minuteOptions

        val calendar = Calendar.getInstance()
        val daysList = mutableListOf<String>()

        if (isFuture) {
            tvPickerTitle.text = "בחר מועד הגעה ומשך חניה:"
            layoutArrivalTiming.visibility = View.VISIBLE

            val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
            daysList.add("היום (${dateFormat.format(calendar.time)})")
            for (i in 1..6) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
                daysList.add(dateFormat.format(calendar.time))
            }
            pickerDay.minValue = 0
            pickerDay.maxValue = daysList.size - 1
            pickerDay.displayedValues = daysList.toTypedArray()

            pickerHour.minValue = 0
            pickerHour.maxValue = 23
            pickerHour.value = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

            pickerMinute.minValue = 0
            pickerMinute.maxValue = 59
            pickerMinute.value = Calendar.getInstance().get(Calendar.MINUTE)
        } else {
            tvPickerTitle.text = "לכמה זמן תרצה לחנות עכשיו?"
            layoutArrivalTiming.visibility = View.GONE
        }

        btnConfirm.setOnClickListener {
            val selectedCal = Calendar.getInstance()
            var displayDateText = "עכשיו"

            if (isFuture) {
                selectedCal.add(Calendar.DAY_OF_YEAR, pickerDay.value)
                selectedCal.set(Calendar.HOUR_OF_DAY, pickerHour.value)
                selectedCal.set(Calendar.MINUTE, pickerMinute.value)
                displayDateText = "${daysList[pickerDay.value]} בשעה ${String.format("%02d:%02d", pickerHour.value, pickerMinute.value)}"
            }

            selectedCal.set(Calendar.SECOND, 0)
            val startTimeInMillis = selectedCal.timeInMillis

            if (isFuture && startTimeInMillis < System.currentTimeMillis()) {
                android.widget.Toast.makeText(requireContext(), "לא ניתן להזמין שריון לשעת עבר", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hoursToAdd = pickerDurationHours.value
            val minutesToAdd = minuteOptions[pickerDurationMinutes.value].toInt()
            val totalDurationInMillis = (hoursToAdd * 60 * 60 * 1000L) + (minutesToAdd * 60 * 1000L)
            val endTimeInMillis = startTimeInMillis + totalDurationInMillis

            if (totalDurationInMillis == 0L) {
                android.widget.Toast.makeText(requireContext(), "יש לבחור זמן חניה חוקי", android.widget.Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val searchedAddress = binding.etSearchAddress.text.toString()

            val sharedPref = requireContext().getSharedPreferences("PendingBooking", Context.MODE_PRIVATE)
            sharedPref.edit()
                .putString("searchedAddress", searchedAddress)
                .putLong("startTime", startTimeInMillis)
                .putLong("endTime", endTimeInMillis)
                .putString("displayDate", displayDateText)
                .putLong("durationTotal", totalDurationInMillis)
                .putString("durationText", "$hoursToAdd שעות ו-$minutesToAdd דקות")
                .apply()

            dialog.dismiss()
            findNavController().navigate(R.id.action_search_to_summary)
        }
        dialog.show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }
    override fun onStart() {
        super.onStart()
        binding.mapView.onStart()
    }
    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
    }
    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }
    override fun onLowMemory() {
        super.onLowMemory()
        binding.mapView.onLowMemory()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        var mapViewBundle = outState.getBundle("MapViewBundleKey")
        if (mapViewBundle == null) {
            mapViewBundle = Bundle()
            outState.putBundle("MapViewBundleKey", mapViewBundle)
        }
        binding.mapView.onSaveInstanceState(mapViewBundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.mapView.onDestroy()
        _binding = null
    }
}