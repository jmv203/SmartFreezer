package com.example.smartfreezer.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.smartfreezer.models.WastedProduct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.SimpleDateFormat
import java.util.*

class WastedProductsViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val _wastedProductsData = MutableLiveData<List<Pair<String, Int>>>()
    val wastedProductsData: LiveData<List<Pair<String, Int>>> = _wastedProductsData
    private val _currentPeriod = MutableLiveData<Date>(Calendar.getInstance().time)
    val currentPeriod: LiveData<Date> = _currentPeriod
    private val _selectedPeriod = MutableLiveData<String>().apply { value = "weekly" }
    val selectedPeriod: LiveData<String> = _selectedPeriod

    fun loadWeeklyData(referenceDate: Date = Calendar.getInstance().time) {
        val calendar = Calendar.getInstance().apply { time = referenceDate }
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.time

        loadDataBetweenDates(startDate, referenceDate, "weekly")
    }

    fun loadMonthlyData(referenceDate: Date = Calendar.getInstance().time) {
        val calendar = Calendar.getInstance().apply { time = referenceDate }
        calendar.add(Calendar.MONTH, -1)
        val startDate = calendar.time

        loadDataBetweenDates(startDate, referenceDate, "monthly")
    }

    fun setPeriod(period: String) {
        _selectedPeriod.value = period
        when(period) {
            "weekly" -> loadWeeklyData()
            "monthly" -> loadMonthlyData()
            "yearly" -> loadYearlyData()
        }
    }

    private fun loadYearlyData() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        val startDate = calendar.time
        loadDataBetweenDates(startDate, Calendar.getInstance().time, "yearly")
    }

    fun loadData(periodOffset: Int = 0) {
        val calendar = Calendar.getInstance()
        when(selectedPeriod.value) {
            "monthly" -> calendar.add(Calendar.MONTH, periodOffset * 6) // Medio año
            else -> calendar.add(Calendar.WEEK_OF_YEAR, periodOffset) // Semanas
        }
        _currentPeriod.value = calendar.time

        when(selectedPeriod.value) {
            "monthly" -> loadMonthlyData(calendar.time)
            else -> loadWeeklyData(calendar.time)
        }
    }

    private fun loadDataBetweenDates(startDate: Date, endDate: Date, period: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            _wastedProductsData.value = emptyList()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .collection("wastedProducts")
            .whereGreaterThanOrEqualTo("date", startDate)
            .whereLessThanOrEqualTo("date", endDate)
            .orderBy("date", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val products = querySnapshot.documents.mapNotNull { document ->
                    try {
                        document.toObject(WastedProduct::class.java)
                    } catch (e: Exception) {
                        null
                    }
                }
                val processedData = processData(products, period)
                _wastedProductsData.postValue(processedData)
            }
            .addOnFailureListener { exception ->
                // Manejar el error adecuadamente
                _wastedProductsData.postValue(emptyList())
            }
    }

    private fun processData(products: List<WastedProduct>, period: String): List<Pair<String, Int>> {
        return when (period) {
            "weekly" -> {
                val days = listOf("L", "M", "X", "J", "V", "S", "D")
                days.map { day ->
                    day to products.count {
                        SimpleDateFormat("E", Locale.getDefault()).format(it.date.toDate()).startsWith(day)
                    }
                }
            }
            "monthly" -> {
                val months = (0..5).map { i ->
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.MONTH, -i)
                    }
                    SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
                }.reversed()

                months.map { month ->
                    month to products.count {
                        SimpleDateFormat("MMM", Locale.getDefault()).format(it.date.toDate()) == month
                    }
                }
            }
            else -> emptyList()
        }
    }
}