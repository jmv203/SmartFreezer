package com.example.smartfreezer.navigation

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.smartfreezer.ProfileActivity
import com.example.smartfreezer.R
import com.example.smartfreezer.SettingsActivity
import com.example.smartfreezer.databinding.FragmentWastedProductsBinding
import com.example.smartfreezer.util.CustomMarkerView
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
import java.text.SimpleDateFormat
import java.util.*
import androidx.core.view.isVisible

class WastedProductsFragment : Fragment(R.layout.fragment_wasted_products) {

    private var _binding: FragmentWastedProductsBinding? = null
    private lateinit var binding: FragmentWastedProductsBinding
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
        binding = FragmentWastedProductsBinding.bind(view)
        viewModel = ViewModelProvider(this).get(WastedProductsViewModel::class.java)

        // Sincroniza el tab al crear el fragment
        binding.tabSelectorWasted.getTabAt(1)?.select()

        binding.btnAccountWastedProducts.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettingsWastedProducts.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        setupGreeting()
        setupUI()
        setupTabLayout()
        setupCharts()
        setupObservers()

        viewModel.setPeriod("weekly")

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
                        // Ya estamos en WastedProductsFragment
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

    private fun setupUI() {
        binding.periodToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when(checkedId) {
                    R.id.btnWeekly -> viewModel.setPeriod("weekly")
                    R.id.btnMonthly -> viewModel.setPeriod("monthly")
                }
            }
        }

        binding.chartTypeToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when(checkedId) {
                    R.id.btnBarChart -> {
                        binding.barChart.visibility = View.VISIBLE
                        binding.pieChart.visibility = View.GONE
                    }
                    R.id.btnPieChart -> {
                        binding.barChart.visibility = View.GONE
                        binding.pieChart.visibility = View.VISIBLE
                        updatePieChart(viewModel.wastedProductsByType.value ?: emptyList())
                    }
                }
            }
        }

        binding.btnPrev.setOnClickListener {
            viewModel.loadData(-1) // Retroceder periodo
        }

        binding.btnNext.setOnClickListener {
            viewModel.loadData(1) // Avanzar periodo
        }
    }

    private fun setupCharts() {
        setupBarChart()
        setupPieChart()
    }

    private fun setupBarChart() {
        // Retrieve the color from the theme
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
            setExtraOffsets(10f, 10f, 10f, 10f)
            setDrawBorders(false)
            setBorderWidth(1f)
            setBorderColor(chartColor)
        }
    }

    private fun setupPieChart() {
        with(binding.pieChart) {
            description.isEnabled = false
            setExtraOffsets(20f, 0f, 20f, 0f)
            dragDecelerationFrictionCoef = 0.95f
            isDrawHoleEnabled = true
            setHoleColor(Color.TRANSPARENT)
            setTransparentCircleColor(ContextCompat.getColor(requireContext(), R.color.chart_purple))
            setTransparentCircleAlpha(20)
            holeRadius = 45f
            transparentCircleRadius = 50f
            setDrawCenterText(true)
            rotationAngle = 0f
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            animateY(1000, Easing.EaseInOutQuad)

            val typedArray = requireContext().obtainStyledAttributes(intArrayOf(R.attr.pieChartCenterTextColor))
            val centerTextColor = typedArray.getColor(0, getContrastColor()) // Default to contrast color if not found
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
                yOffset = 15f
                textSize = 12f
                textColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            }
        }
    }

    private fun getContrastColor(): Int {
        val nightModeFlags = resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return when (nightModeFlags) {
            android.content.res.Configuration.UI_MODE_NIGHT_YES ->
                ContextCompat.getColor(requireContext(), R.color.chart_text_light)
            else ->
                ContextCompat.getColor(requireContext(), R.color.chart_text_dark)
        }
    }

    private fun getPieChartColors(count: Int): List<Int> {
        val colors = mutableListOf<Int>()
        val baseColors = listOf(
            R.color.chart_purple,
            R.color.chart_blue,
            R.color.chart_green,
            R.color.chart_red,
            R.color.chart_yellow,
            R.color.colorPrimary,
            R.color.colorPrimaryDark
        )

        repeat(count) { index ->
            colors.add(ContextCompat.getColor(requireContext(), baseColors[index % baseColors.size]))
        }
        return colors
    }

    private fun setupObservers() {
        viewModel.wastedProductsData.observe(viewLifecycleOwner) { data ->
            updateBarChart(data)
            if (binding.pieChart.isVisible) {
                updatePieChart(viewModel.wastedProductsByType.value ?: emptyList())
            }
        }

        viewModel.wastedProductsByType.observe(viewLifecycleOwner) { data ->
            if (binding.pieChart.isVisible) {
                updatePieChart(data)
            }
        }

        viewModel.currentPeriod.observe(viewLifecycleOwner) { date ->
            updatePeriodTitle(date)
        }

        viewModel.selectedPeriod.observe(viewLifecycleOwner) { period ->
            viewModel.loadData()
        }
    }

    private fun updateBarChart(data: List<Pair<String, Int>>) {

        if (data.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.barChart.apply {
                clear()
                invalidate()
                visibility = View.GONE
            }
            return
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.barChart.visibility = View.VISIBLE
        }


        val entries = data.mapIndexed { index, (_, value) ->
            BarEntry(index.toFloat(), value.toFloat())
        }

        //  Retrieve the color from the theme
        val typedArray = requireContext().obtainStyledAttributes(intArrayOf(R.attr.barChartColor))
        val barColor = typedArray.getColor(0, ContextCompat.getColor(requireContext(), R.color.purpleButton)) //  Default to purpleButton if not found
        typedArray.recycle()  //  Don't forget to recycle!

        val dataSet = BarDataSet(entries, "").apply {
            color = barColor
            valueTextColor = barColor
            valueTextSize = 12f
            setDrawValues(true)
            highLightColor = barColor
            setGradientColor(barColor, barColor)
            barShadowColor = barColor
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value == 0f) "" else value.toInt().toString()
                }
            }
        }

        binding.barChart.apply {
            this.data = BarData(dataSet).apply {
                barWidth = 0.6f
                setValueTextSize(12f)
            }

            xAxis.apply {
                valueFormatter = object : ValueFormatter() {
                    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
                        return data.getOrNull(value.toInt())?.first ?: ""
                    }
                }
                labelCount = data.size
                granularity = 1f
                axisMinimum = -0.5f
                axisMaximum = data.size.toFloat() - 0.5f
            }

            marker = CustomMarkerView(requireContext(), R.layout.chart_marker_layout)
            invalidate()
        }

        val periodText = when(viewModel.selectedPeriod.value) {
            "monthly" -> getString(R.string.month)
            else -> getString(R.string.week)
        }

        val total = data.sumOf { it.second }
        binding.tvSummary.text = getString(R.string.total_wasted, total, periodText)
    }

    private fun updatePieChart(data: List<Pair<String, Int>>) {
        if (data.isEmpty()) {
            binding.tvNoData.visibility = View.VISIBLE
            binding.pieChart.apply {
                clear()
                invalidate()
                visibility = View.GONE
            }
            return
        } else {
            binding.tvNoData.visibility = View.GONE
            binding.pieChart.visibility = View.VISIBLE
        }

        val entries = data.mapIndexed { index, (name, value) ->
            PieEntry(value.toFloat(), name)
        }

        val dataSet = PieDataSet(entries, "").apply {
            sliceSpace = 0f
            selectionShift = 8f
            colors = getPieChartColors(data.size)
            valueTextColor = getContrastColor()
            valueTextSize = 14f
            valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value < 3) "" else "${value.toInt()}"
                }
            }
        }

        binding.pieChart.apply {
            this.data = PieData(dataSet)
            centerText = getString(R.string.wasted_by_type)
            setEntryLabelColor(getContrastColor())
            setEntryLabelTextSize(12f)
            animateY(1000, Easing.EaseInOutQuad)
            invalidate()
        }
    }

    private fun updatePeriodTitle(date: Date) {
        val period = viewModel.selectedPeriod.value ?: "weekly"
        val title = when (period) {
            "weekly" -> {
                val cal = Calendar.getInstance().apply {
                    time = date
                    val dayOfWeek = get(Calendar.DAY_OF_WEEK)
                    val diffToMonday = if (dayOfWeek == Calendar.SUNDAY) -6 else Calendar.MONDAY - dayOfWeek
                    add(Calendar.DAY_OF_MONTH, diffToMonday) // Ir al lunes de esa semana
                }

                val startDate = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)

                val endCal = Calendar.getInstance().apply {
                    time = cal.time
                    add(Calendar.DAY_OF_MONTH, 6) // Ir al domingo
                }

                val endDate = SimpleDateFormat("d", Locale.getDefault()).format(endCal.time)
                val monthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

                getString(R.string.week_range_format, startDate, endDate, monthYear)
            }

            "monthly" -> {
                val cal = Calendar.getInstance().apply { time = date }
                val startCal = Calendar.getInstance().apply {
                    time = date
                    add(Calendar.MONTH, -5)
                }

                val startMonth = SimpleDateFormat("MMM", Locale.getDefault()).format(startCal.time)
                val endMonth = SimpleDateFormat("MMM", Locale.getDefault()).format(cal.time)
                val year = SimpleDateFormat("yyyy", Locale.getDefault()).format(cal.time)

                getString(R.string.month_range_format, startMonth, endMonth, year)
            }

            else -> ""
        }

        binding.tvPeriodTitle.text = title
    }

}