package com.marosseleng.distancemeasurements.ui.measurementdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.fragment.navArgs
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.marosseleng.distancemeasurements.R
import kotlinx.android.synthetic.main.fragment_measurement_detail.*
import timber.log.Timber


/**
 * @author Maroš Šeleng
 */
class MeasurementDetailFragment : Fragment() {

    private lateinit var viewModel: MeasurementDetailViewModel
    private lateinit var pointsSeries: LineDataSet
    private val args: MeasurementDetailFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_measurement_detail, container, false)
//        pointsSeries = LineDataSet(listOf(
//                Entry(0.0f, 1.0f),
//                Entry(1.0f, 2.0f),
//                Entry(2.0f, 3.0f),
//                Entry(3.0f, 4.0f),
//                Entry(4.0f, 5.0f),
//                Entry(5.0f, 6.0f),
//                Entry(6.0f, 7.0f),
//                Entry(7.0f, 8.0f),
//                Entry(8.0f, 9.0f),
//                Entry(9.0f, 10.0f),
//                Entry(10.0f, 11.0f),
//                Entry(11.0f, 12.0f),
//                Entry(12.0f, 13.0f),
//                Entry(13.0f, 14.0f),
//                Entry(14.0f, 15.0f),
//                Entry(15.0f, 16.0f),
//                Entry(16.0f, 17.0f),
//                Entry(17.0f, 18.0f),
//                Entry(18.0f, 19.0f),
//                Entry(29.0f, 20.0f)
//            ), "values")
//        view.findViewById<LineChart>(R.id.graph).run {
//            data = LineData(pointsSeries)
//            invalidate()
//        }
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        (activity as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val measurement = args.measurement

        title.text = measurement.timestamp.toString()

        val valuesAdapter = MeasuredValueAdapter()

        with(list) {
            adapter = valuesAdapter
        }

        viewModel = ViewModelProviders.of(this, MeasurementDetailViewModel.Factory(measurement))
            .get(MeasurementDetailViewModel::class.java)

        viewModel.values.observe(this, Observer {
            val isListEmpty = it.isEmpty()
            noValues.isVisible = isListEmpty
            list.isVisible = !isListEmpty
            graphWrapper.isVisible = !isListEmpty

            if (!isListEmpty) {
                valuesAdapter.items = it
            }
        })

        viewModel.graphValues.observe(this, Observer {
            val array = it.map { (fst, snd) ->
                Entry(fst.toFloat(), snd.toFloat())
            }
            Timber.d("Array: %s", array.joinToString())
            Timber.d("Array size: %d", array.size)
            graph.run {
                data = LineData(LineDataSet(array, "values"))
                invalidate()
            }
        })
    }
}
