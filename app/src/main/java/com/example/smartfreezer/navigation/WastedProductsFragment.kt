package com.example.smartfreezer.navigation

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.smartfreezer.R
import com.example.smartfreezer.databinding.FragmentWastedProductsBinding
import com.example.smartfreezer.util.CustomMarkerView
import com.example.smartfreezer.util.OnInventoryTabSelectedListener
import com.example.smartfreezer.viewmodels.WastedProductsViewModel
import com.github.mikephil.charting.animation.Easing
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*


class WastedProductsFragment : Fragment(R.layout.fragment_wasted_products) {

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

        setupUI()
        setupTabLayout()
        setupChart()
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
            ),
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

    private fun setupUI() {
        binding.periodToggle.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                when(checkedId) {
                    R.id.btnWeekly -> viewModel.setPeriod("weekly")
                    R.id.btnMonthly -> viewModel.setPeriod("monthly")
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

    private fun setupChart() {
        with(binding.barChart) {
            // Configuración básica
            setDrawBarShadow(false)
            setDrawValueAboveBar(true)
            description.isEnabled = false
            legend.isEnabled = false
            setPinchZoom(false)
            setDrawGridBackground(false)
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)

            // Fondo transparente
            setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.transparent))

            // Estilo del eje X
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                axisMinimum = -0.5f
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                axisLineWidth = 1.5f
                textColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                textSize = 12f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
            }

            // Estilo del eje Y izquierdo
            axisLeft.apply {
                axisMinimum = 0f
                granularity = 1f
                setDrawZeroLine(true)
                setDrawGridLines(false)
                axisLineColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
                axisLineWidth = 1.5f
                textColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
                textSize = 12f
                typeface = ResourcesCompat.getFont(requireContext(), R.font.roboto_medium)
            }

            // Deshabilitar eje derecho
            axisRight.isEnabled = false

            // Animaciones
            animateY(1000, Easing.EaseInOutQuad)

            // Estilo general
            setExtraOffsets(10f, 10f, 10f, 10f)
            setDrawBorders(false)
            setBorderWidth(1f)
            setBorderColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
        }
    }

    private fun setupObservers() {
        viewModel.wastedProductsData.observe(viewLifecycleOwner) { data ->
            updateChart(data)
        }

        viewModel.currentPeriod.observe(viewLifecycleOwner) { date ->
            updatePeriodTitle(date)
        }

        viewModel.selectedPeriod.observe(viewLifecycleOwner) { period ->
            viewModel.loadData()
        }
    }

    private fun updateChart(data: List<Pair<String, Int>>) {
        val entries = data.mapIndexed { index, (_, value) ->
            BarEntry(index.toFloat(), value.toFloat())
        }

        val dataSet = BarDataSet(entries, "").apply {
            // Estilo de las barras
            color = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
            valueTextSize = 12f
            setDrawValues(true)
            highLightColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
            setGradientColor(
                ContextCompat.getColor(requireContext(), R.color.colorPrimary),
                ContextCompat.getColor(requireContext(), R.color.colorPrimaryDark)
            )
            barShadowColor = ContextCompat.getColor(requireContext(), R.color.colorPrimary)
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

            // Añadir marcadores para mejorar la legibilidad
            var marker = CustomMarkerView(requireContext(), R.layout.chart_marker_layout)
            marker.chartView = binding.barChart

            invalidate()
        }

        // Actualizar resumen
        val total = data.sumOf { it.second }
        binding.tvSummary.text = getString(R.string.total_wasted, total)
    }

    private fun updatePeriodTitle(date: Date) {
        val period = viewModel.selectedPeriod.value ?: "weekly"
        val title = when(period) {
            "weekly" -> {
                val cal = Calendar.getInstance().apply { time = date }
                val startCal = Calendar.getInstance().apply {
                    time = date
                    add(Calendar.DAY_OF_WEEK, -6)
                }

                val startDate = SimpleDateFormat("d", Locale.getDefault()).format(startCal.time)
                val endDate = SimpleDateFormat("d", Locale.getDefault()).format(cal.time)
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