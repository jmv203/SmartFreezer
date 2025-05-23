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

    private val _wastedProductsByType = MutableLiveData<List<Pair<String, Int>>>()
    val wastedProductsByType: LiveData<List<Pair<String, Int>>> = _wastedProductsByType

    // Fechas de referencia separadas
    private val _currentWeeklyReferenceDate = MutableLiveData<Date>(Calendar.getInstance().time)
    val currentWeeklyReferenceDate: LiveData<Date> = _currentWeeklyReferenceDate

    private val _currentMonthlyReferenceDate = MutableLiveData<Date>(Calendar.getInstance().time)
    val currentMonthlyReferenceDate: LiveData<Date> = _currentMonthlyReferenceDate

    // LiveData para exponer la fecha que el Fragment debe observar para el título
    private val _displayPeriodDate = MutableLiveData<Date>()
    val displayPeriodDate: LiveData<Date> = _displayPeriodDate

    private val _selectedPeriod = MutableLiveData<String>().apply { value = "weekly" }
    val selectedPeriod: LiveData<String> = _selectedPeriod

    init {
        // Inicializar displayPeriodDate con la fecha semanal por defecto
        _displayPeriodDate.value = _currentWeeklyReferenceDate.value
    }
    private var daysOfWeek: List<String> = emptyList()

    fun setStringResources(days: List<String>) {
        daysOfWeek = days
    }

    // Carga los datos para la semana de la referenceDate
    private fun loadWeeklyData(referenceDate: Date) {
        val calendar = Calendar.getInstance().apply {
            time = referenceDate
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
            add(Calendar.DAY_OF_MONTH, diffToMonday)
        }
        val startDate = calendar.time
        calendar.add(Calendar.DAY_OF_MONTH, 6)
        val endDate = calendar.time

        _displayPeriodDate.value = referenceDate // Actualizar la fecha para el título
        loadDataBetweenDates(startDate, endDate, "weekly")
    }

    // Carga los datos para el rango de 6 meses terminando en el mes de referenceDate
    private fun loadMonthlyData(referenceDate: Date) {
        val endCal = Calendar.getInstance().apply {
            time = referenceDate
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
        }
        val startCal = Calendar.getInstance().apply {
            time = endCal.time
            add(Calendar.MONTH, -5)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        _displayPeriodDate.value = referenceDate // Actualizar la fecha para el título
        loadDataBetweenDates(startCal.time, endCal.time, "monthly")
    }

    fun setPeriod(period: String) {
        val oldPeriod = _selectedPeriod.value
        _selectedPeriod.value = period
        if (oldPeriod != period) { // Solo recargar si el periodo realmente cambió
            when (period) {
                "weekly" -> loadWeeklyData(_currentWeeklyReferenceDate.value ?: Calendar.getInstance().time)
                "monthly" -> loadMonthlyData(_currentMonthlyReferenceDate.value ?: Calendar.getInstance().time)
            }
        }
    }

    fun loadData(periodOffset: Int = 0) {
        val currentSelectedPeriod = _selectedPeriod.value ?: "weekly"
        val calendar = Calendar.getInstance()

        when (currentSelectedPeriod) {
            "monthly" -> {
                calendar.time = _currentMonthlyReferenceDate.value ?: Date()
                calendar.add(Calendar.MONTH, periodOffset * 6) // Avanza/retrocede de 6 en 6 meses
                _currentMonthlyReferenceDate.value = calendar.time
                loadMonthlyData(calendar.time)
            }
            "weekly" -> {
                calendar.time = _currentWeeklyReferenceDate.value ?: Date()
                calendar.add(Calendar.WEEK_OF_YEAR, periodOffset)
                _currentWeeklyReferenceDate.value = calendar.time
                loadWeeklyData(calendar.time)
            }
        }
    }

    private fun loadDataBetweenDates(startDate: Date, endDate: Date, period: String) {
        val currentUser = auth.currentUser
        Log.d("FirestoreQuery", "Consultando desde ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(startDate)} hasta ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(endDate)} para periodo $period")
        if (currentUser == null) {
            _wastedProductsData.value = emptyList()
            _wastedProductsByType.value = emptyList()
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
                        Log.e("FirestoreConversion", "Error converting document: ${document.id}", e)
                        null
                    }
                }
                val processedData = processData(products, period, if (period == "monthly") _currentMonthlyReferenceDate.value!! else _currentWeeklyReferenceDate.value!!)
                _wastedProductsData.postValue(processedData)

                val byType = products.groupBy { it.icon to it.name }
                    .map { (key, items) ->
                        val name = key.second
                        "$name (${items.size})" to items.size
                    }
                    .sortedByDescending { it.second }
                _wastedProductsByType.postValue(byType)
            }
            .addOnFailureListener { exception ->
                Log.e("FirestoreQuery", "Error cargando datos", exception)
                _wastedProductsData.postValue(emptyList())
                _wastedProductsByType.postValue(emptyList())
            }
    }

    private fun processData(products: List<WastedProduct>, period: String, referenceDateForMonth: Date): List<Pair<String, Int>> {
        return when (period) {
            "weekly" -> {
                val dayFormat = SimpleDateFormat("u", Locale.getDefault()) // Lunes=1, Domingo=7
                daysOfWeek.mapIndexed { index, dayName ->
                    val dayNumberInWeek = index + 1
                    dayName to products.count { product ->
                        val calProduct = Calendar.getInstance().apply { time = product.date.toDate() }

                        val productDayOfWeek = if (calProduct.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) 7 else calProduct.get(Calendar.DAY_OF_WEEK) -1
                        productDayOfWeek == dayNumberInWeek
                    }
                }
            }
            "monthly" -> {
                val currentDisplayCal = Calendar.getInstance().apply { time = referenceDateForMonth }

                (0..5).map { i ->
                    val targetCal = Calendar.getInstance().apply {
                        time = currentDisplayCal.time
                        add(Calendar.MONTH, -i)
                    }
                    val monthName = SimpleDateFormat("MMM", Locale.getDefault())
                        .format(targetCal.time)
                        .replace(".", "")

                    val count = products.count { product ->
                        val productCal = Calendar.getInstance().apply { time = product.date.toDate() }
                        productCal.get(Calendar.MONTH) == targetCal.get(Calendar.MONTH) &&
                                productCal.get(Calendar.YEAR) == targetCal.get(Calendar.YEAR)
                    }
                    monthName to count
                }.reversed()
            }
            else -> emptyList()
        }
    }
}