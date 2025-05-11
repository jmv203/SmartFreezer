package com.example.smartfreezer.viewmodels

import android.util.Log
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

    private var daysOfWeek: List<String> = emptyList()


    fun setStringResources(
        days: List<String>,

    ) {
        daysOfWeek = days

    }

    fun loadWeeklyData(referenceDate: Date = Calendar.getInstance().time) {
        val calendar = Calendar.getInstance().apply { time = referenceDate }
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val startDate = calendar.time

        loadDataBetweenDates(startDate, referenceDate, "weekly")
    }

    fun loadMonthlyData(referenceDate: Date = Calendar.getInstance().time) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val endCal = Calendar.getInstance().apply {
            time = referenceDate
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }

        val startCal = Calendar.getInstance().apply {
            time = endCal.time
            add(Calendar.MONTH, -5)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        Log.d("DateRange", "Rango consultado: ${dateFormat.format(startCal.time)} - ${dateFormat.format(endCal.time)}")

        loadDataBetweenDates(startCal.time, endCal.time, "monthly")
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
        val calendar = Calendar.getInstance().apply { time = _currentPeriod.value ?: Date() }

        when(selectedPeriod.value) {
            "monthly" -> calendar.add(Calendar.MONTH, periodOffset * 6) // 6 meses
            else -> calendar.add(Calendar.WEEK_OF_YEAR, periodOffset) // 1 semana
        }
        _currentPeriod.value = calendar.time

        when(selectedPeriod.value) {
            "monthly" -> loadMonthlyData(calendar.time)
            else -> loadWeeklyData(calendar.time)
        }
    }

    private fun loadDataBetweenDates(startDate: Date, endDate: Date, period: String) {

        val currentUser = auth.currentUser
        Log.d("FirestoreQuery", "Consultando desde ${SimpleDateFormat("dd/MM/yyyy").format(startDate)} hasta ${SimpleDateFormat("dd/MM/yyyy").format(endDate)}")
        if (currentUser == null) {
            _wastedProductsData.value = emptyList()
            return
        }

        db.collection("users")
            .document(currentUser.uid)
            .collection("wasted_products")
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
                val dayFormat = SimpleDateFormat("u", Locale.getDefault())
                daysOfWeek.mapIndexed { index, day ->
                    val dayNumber = index + 1
                    day to products.count { product ->
                        dayFormat.format(product.date.toDate()).toInt() == dayNumber
                    }
                }
            }
            "monthly" -> {
                val currentCal = Calendar.getInstance().apply {
                    time = _currentPeriod.value ?: Date()
                    set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                }

                (0..5).map { i ->
                    val targetCal = Calendar.getInstance().apply {
                        time = currentCal.time
                        add(Calendar.MONTH, -i)
                    }

                    val monthName = SimpleDateFormat("MMM", Locale.getDefault())
                        .format(targetCal.time)
                        .replace(".", "")

                    monthName to products.count { product ->
                        val productDate = product.date.toDate()
                        val productCal = Calendar.getInstance().apply { time = productDate }
                        productCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
                                productCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
                    }
                }.reversed()
            }
            else -> emptyList()
        }
    }
}