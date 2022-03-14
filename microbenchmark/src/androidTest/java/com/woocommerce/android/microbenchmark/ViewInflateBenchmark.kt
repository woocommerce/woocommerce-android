package com.woocommerce.android.microbenchmark

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import com.woocommerce.android.benchmarkable.R

@RunWith(AndroidJUnit4::class)
class ViewInflateBenchmark {
    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun benchmarkViewInflate() {
        val context = InstrumentationRegistry.getInstrumentation().context
        val inflater = LayoutInflater.from(context)
        val root = FrameLayout(context)

        benchmarkRule.measureRepeated {
            inflater.inflate(R.layout.view_card, root, false)
        }
    }
}
