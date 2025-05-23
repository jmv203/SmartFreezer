package com.example.smartfreezer.navigation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.smartfreezer.ProfileActivity
import com.example.smartfreezer.R
import com.example.smartfreezer.SettingsActivity
import com.example.smartfreezer.databinding.FragmentWastedProductsBinding
import com.example.smartfreezer.util.OnInventoryTabSelectedListener
import com.example.smartfreezer.viewmodels.WastedProductsViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.*

class WastedProductsFragment : Fragment(R.layout.fragment_wasted_products) {

    private var _binding: FragmentWastedProductsBinding? = null

    private val binding get() = _binding!!
    private lateinit var viewModel: WastedProductsViewModel
    private var tabSelectedListener: OnInventoryTabSelectedListener? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnInventoryTabSelectedListener) {
            tabSelectedListener = context
        } else {
            throw ClassCastException("$context must implement OnInventoryTabSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        tabSelectedListener = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        _binding = FragmentWastedProductsBinding.bind(view)
        viewModel = ViewModelProvider(this)[WastedProductsViewModel::class.java]

        binding.tabSelectorWasted.getTabAt(1)?.select()

        // Configurar UI inicial
        binding.chartTypeToggle.check(R.id.btnBarChart)
        binding.barChart.visibility = View.VISIBLE
        binding.pieChart.visibility = View.GONE

        binding.btnAccountWastedProducts.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettingsWastedProducts.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        setupGreeting()
        setupUIListeners()
        setupTabLayout()
        setupCharts()
        setupObservers()

        // Pasar las cadenas de los días de la semana al ViewModel
        viewModel.setStringResources(
            days = listOf(
                getString(R.string.monday),
                getString(R.string.tuesday),
                getString(R.string.wednesday),
                getString(R.string.thursday),
                getString(R.string.friday),
                getString(R.string.saturday),
                getString(R.string.sunday)
            )
        )


        if (binding.periodToggle.checkedButtonId == R.id.btnWeekly) {
            viewModel.setPeriod("weekly")
        } else if (binding.periodToggle.checkedButtonId == R.id.btnMonthly) {
            viewModel.setPeriod("monthly")
        } else {

            binding.periodToggle.check(R.id.btnWeekly)
            viewModel.setPeriod("weekly")
        }
        viewModel.loadData() // Cargar datos iniciales
    }

    private fun setupTabLayout() {
        binding.tabSelectorWasted.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        if (findNavController().currentDestination?.id != R.id.inventoryFragment) {
                            findNavController().navigate(R.id.action_wastedProductsFragment_to_inventoryFragment)
                        }
                    }
                    1 -> {
                        tabSelectedListener?.onInventoryTabSelected(tab.position)
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    private fun setupGreeting() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    binding.tvGreetingWastedProducts.text = getString(R.string.hola, name)
                }
            }
    }

    private fun setupUIListeners() {

        // Inicializar gráfico bar por defecto
        binding.chartTypeToggle.check(R.id.btnBarChart)
        binding.barChart.visibility = View.VISIBLE
        binding.pieChart.visibility = View.GONE
        binding.periodToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when(checkedId) {
                    R.id.btnWeekly -> viewModel.setPeriod("weekly")
                    R.id.btnMonthly -> viewModel.setPeriod("monthly")
                }
            }
        }

        binding.chartTypeToggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                val hasBarData = viewModel.wastedProductsData.value?.isNotEmpty() == true
                val hasPieData = viewModel.wastedProductsByType.value?.isNotEmpty() == true

                when(checkedId) {
                    R.id.btnBarChart -> {
                        binding.barChart.visibility = if (hasBarData) View.VISIBLE else View.GONE
                        binding.pieChart.visibility = View.GONE
                        binding.tvNoData.isVisible = !hasBarData
                        if (hasBarData) { // Asegurar que se actualice si ya tenía datos
                            updateBarChart(viewModel.wastedProductsData.value!!)
                        }
                    }
                    R.id.btnPieChart -> {
                        binding.barChart.visibility = View.GONE
                        binding.pieChart.visibility = if (hasPieData) View.VISIBLE else View.GONE
                        binding.tvNoData.isVisible = !hasPieData
                        if (hasPieData) {
                            updatePieChart(viewModel.wastedProductsByType.value!!)
                        }
                    }
                }
            }
        }

        binding.btnPrev.setOnClickListener {
            viewModel.loadData(-1)
        }

        binding.btnNext.setOnClickListener {
            viewModel.loadData(1)
        }
    }

    private fun setupCharts() {
        setupBarChart()
        setupPieChart()
    }

    private fun setupBarChart() {
        val typedArray = requireContext().obtainStyledAttributes(intArrayOf(R.attr.barChartColor))
        val chartColor = typedArray.getColor(0, ContextCompat.getColor(requireContext(), R.color.chart_purple))
        typedArray.recycle()

        with(binding.barChart) {
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            description.isEnabled = false
            legend.isEnabled = false
            setPinchZoom(false)
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setBackgroundColor(Color.TRANSPARENT)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                axisMinimum = -0.5f
                axisLineColor = chartColor
                axisLineWidth = 1.5f
                textColor = chartColor
                textSize = 12f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
            }

            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                setDrawZeroLine(true)
                setDrawGridLines(false)
                axisLineColor = chartColor
                axisLineWidth = 1.5f
                textColor = chartColor
                textSize = 12f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
            }

            axisRight.isEnabled = false
            animateY(1000, Easing.EaseInOutQuad)
            setExtraOffsets(10f, 10f, 10f, 20f) // Aumentar offset inferior para etiquetas del eje X
            setDrawBorders(false) // Puedes habilitarlo si quieres un borde alrededor del gráfico
        }
    }

    private fun setupPieChart() {
        with(binding.pieChart) {
            description.isEnabled = false
            setExtraOffsets(20f, 5f, 20f, 5f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1000, Easing.EaseInOutQuad)

            val typedArray = requireContext().obtainStyledAttributes(intArrayOf(R.attr.pieChartCenterTextColor))
            val centerTextColor = typedArray.getColor(0, getContrastColor())
            typedArray.recycle()

            setCenterTextColor(centerTextColor)
            setCenterTextSize(14f)

            legend.apply {
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
                xEntrySpace = 10f
                yEntrySpace = 5f
                yOffset = 10f // Ajusta el offset si es necesario
                textSize = 12f
                val legendTextColorArray = requireContext().obtainStyledAttributes(intArrayOf(R.attr.colorTextSecondary))
                textColor = legendTextColorArray.getColor(0, Color.DKGRAY)
                legendTextColorArray.recycle()

            }
        }
    }

    private fun getContrastColor(): Int {
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return when (nightModeFlags) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES ->
                ContextCompat.getColor(requireContext(), R.color.chart_text_light) // Define este color
            else ->
                ContextCompat.getColor(requireContext(), R.color.chart_text_dark) // Define este color
        }
    }

    private fun getPieChartColors(count: Int): List<Int> {
        val colors = mutableListOf<Int>()
        // Intenta obtener colores del tema si es posible, o define una paleta más amplia
        val baseColorIds = listOf(
            R.color.chart_purple, R.color.chart_blue, R.color.chart_green,
            R.color.chart_red, R.color.chart_yellow, R.color.chart_orange,
            R.color.chart_teal, R.color.chart_pink
        )


        repeat(count) { index ->
            colors.add(ContextCompat.getColor(requireContext(), baseColorIds[index % baseColorIds.size]))
        }
        return colors
    }

    private fun setupObservers() {
        viewModel.wastedProductsData.observe(viewLifecycleOwner) { data ->
            val isEmpty = data.isEmpty()
            val isBarChartSelected = binding.chartTypeToggle.checkedButtonId == R.id.btnBarChart

            if (isBarChartSelected) {
                binding.barChart.isVisible = !isEmpty
                binding.tvNoData.isVisible = isEmpty
                if (!isEmpty) {
                    updateBarChart(data)
                } else {
                    binding.barChart.clear()
                    binding.barChart.invalidate() // Importante para refrescar un gráfico vacío
                    binding.tvSummary.text = "" // Limpiar resumen si no hay datos
                }
            }
        }

        viewModel.wastedProductsByType.observe(viewLifecycleOwner) { data ->
            val isEmpty = data.isEmpty()
            val isPieChartSelected = binding.chartTypeToggle.checkedButtonId == R.id.btnPieChart

            if (isPieChartSelected) {
                binding.pieChart.isVisible = !isEmpty
                binding.tvNoData.isVisible = isEmpty
                if (!isEmpty) {
                    updatePieChart(data)
                } else {
                    binding.pieChart.clear()
                    binding.pieChart.invalidate()
                    binding.tvSummary.text = "" // Limpiar el resumen cuando no hay datos
                }
            }
        }

        viewModel.displayPeriodDate.observe(viewLifecycleOwner) { date ->
            updatePeriodTitle(date)
        }

        // Observar el periodo seleccionado para actualizar el gráfico si cambia
        viewModel.selectedPeriod.observe(viewLifecycleOwner) { period ->
            val currentData = when(period) {
                "weekly" -> viewModel.wastedProductsData.value
                "monthly" -> viewModel.wastedProductsByType.value
                else -> null
            }

            if (currentData != null && binding.chartTypeToggle.checkedButtonId == R.id.btnBarChart) {
                if (currentData.isNotEmpty()) {
                    updateBarChart(currentData)
                } else {
                    binding.barChart.clear()
                    binding.barChart.invalidate()
                    binding.tvSummary.text = ""
                }
            }
        }
    }

    private fun updateBarChart(data: List<Pair<String, Int>>) {
        // La visibilidad ya se maneja en el observador
        if (data.isEmpty() && binding.chartTypeToggle.checkedButtonId == R.id.btnBarChart) {
            binding.barChart.clear()
            binding.barChart.invalidate()
            binding.tvSummary.text = ""
            return
        }


        val entries = data.mapIndexed { index, (_, value) ->
            BarEntry(index.toFloat(), value.toFloat())
        }

        val typedArray = requireContext().obtainStyledAttributes(intArrayOf(R.attr.barChartColor))
        val barColor = typedArray.getColor(0, ContextCompat.getColor(requireContext(), R.color.purpleButton))
        typedArray.recycle()

        val dataSet = BarDataSet(entries, getString(R.string.wasted_items_label)).apply { // Añadido label
            color = barColor
            valueTextColor = barColor
            valueTextSize = 11f
            setDrawValues(true)
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "" else value.toInt().toString()
                }
            }
        }

        binding.barChart.apply {
            this.data = BarData(dataSet).apply {
                barWidth = 0.6f
            }

            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return data.getOrNull(value.toInt())?.first ?: ""
                    }
                }
                labelCount = data.size.coerceAtLeast(1)
                axisMinimum = -0.5f
                axisMaximum = data.size.toFloat() - 0.5f
            }
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
        updateSummary(data)
    }

    private fun updatePieChart(data: List<Pair<String, Int>>) {
        // La visibilidad ya se maneja en el observador
        if (data.isEmpty() && binding.chartTypeToggle.checkedButtonId == R.id.btnPieChart) {
            binding.pieChart.clear()
            binding.pieChart.invalidate()
            return
        }


        val entries = data.map { (name, value) ->
            PieEntry(value.toFloat(), name)
        }

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 2f
            selectionShift = 8f
            colors = getPieChartColors(data.size)
            valueTextColor = getContrastColor()
            valueTextSize = 13f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {

                    return if (value < 1) "" else value.toInt().toString()
                }
            }
        }

        binding.pieChart.apply {
            this.data = PieData(dataSet)
            centerText = getString(R.string.wasted_by_type)
            setEntryLabelColor(getContrastColor())
            setEntryLabelTextSize(11f)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
        updateSummary(data)
    }

    private fun updatePeriodTitle(referenceDate: Date) {
        val period = viewModel.selectedPeriod.value ?: "weekly"
        val title = when (period) {
            "weekly" -> {
                val cal = Calendar.getInstance().apply {
                    time = referenceDate
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
                    add(Calendar.DAY_OF_MONTH, diffToMonday)
                }
                val startDateStr = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
                val endCal = Calendar.getInstance().apply {
                    time = cal.time
                    add(Calendar.DAY_OF_MONTH, 6)
                }
                val endDateStr = SimpleDateFormat("d", Locale.getDefault()).format(endCal.time)
                val monthFormat = SimpleDateFormat("MMM", Locale.getDefault()).apply {
                    val month = cal.get(Calendar.MONTH)
                    val symbols = DateFormatSymbols(Locale.getDefault()).apply {
                        shortMonths = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    }
                    dateFormatSymbols = symbols
                }
                val monthYearStr = monthFormat.format(cal.time) + " " + SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)
                getString(R.string.week_range_format, startDateStr, endDateStr, monthYearStr)
            }
            "monthly" -> {
                val endRangeCal = Calendar.getInstance().apply { time = referenceDate }
                val startRangeCal = Calendar.getInstance().apply {
                    time = endRangeCal.time
                    add(Calendar.MONTH, -5)
                }
                val dateFormat = SimpleDateFormat("MMM", Locale.getDefault()).apply {
                    // Asegurar abreviaciones consistentes
                    val symbols = DateFormatSymbols(Locale.getDefault()).apply {
                        shortMonths = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                    }
                    dateFormatSymbols = symbols
                }
                val startMonthStr = dateFormat.format(startRangeCal.time)
                val endMonthStr = dateFormat.format(endRangeCal.time)
                val yearStr = SimpleDateFormat("yyyy", Locale.getDefault()).format(endRangeCal.time)
                getString(R.string.month_range_format, startMonthStr, endMonthStr, yearStr)
            }
            else -> ""
        }
        binding.tvPeriodTitle.text = title
    }

    private fun updateSummary(data: List<Pair<String, Int>>) {
        val periodText = when(viewModel.selectedPeriod.value) {
            "monthly" -> getString(R.string.last_6_months)
            else -> getString(R.string.current_week)
        }
        val total = data.sumOf { it.second }
        binding.tvSummary.text = getString(R.string.total_wasted, total, periodText)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Limpiar la referencia al binding para evitar memory leaks
    }
}