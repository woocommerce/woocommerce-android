package com.woocommerce.android.viewmodel

import android.os.Parcelable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SavedStateHandle
import kotlinx.parcelize.Parcelize
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class LiveDataDelegateTests : BaseUnitTest() {
    @Test
    fun `when observed for the first time, then emit initial value`() {
        val livedataDelegate = LiveDataDelegate(SavedStateHandle(), ExampleData(0))

        livedataDelegate.observeForever { old, new ->
            assertThat(old).isNull()
            assertThat(new).isEqualTo(ExampleData(0))
        }
    }

    @Test
    fun `when a new value is set before observing, then the previous value will be null`() {
        val livedataDelegate = LiveDataDelegate(SavedStateHandle(), ExampleData(0))
        var dataHolder by livedataDelegate

        dataHolder = dataHolder.copy(data = 1)

        livedataDelegate.observeForever { old, new ->
            assertThat(old).isEqualTo(null)
            assertThat(new).isEqualTo(ExampleData(1))
        }
    }

    @Test
    fun `when a new value is set after observing, then emit previous value`() {
        val livedataDelegate = LiveDataDelegate(SavedStateHandle(), ExampleData(0))
        var dataHolder by livedataDelegate

        var lastData: Pair<ExampleData?, ExampleData?> = Pair(null, null)
        livedataDelegate.observeForever { old, new ->
            lastData = Pair(old, new)
        }
        dataHolder = dataHolder.copy(data = 1)

        val (old, new) = lastData
        assertThat(old).isEqualTo(ExampleData(0))
        assertThat(new).isEqualTo(ExampleData(1))
    }

    @Test
    fun `when an observer is destroyed, then previous value is still calculated correctly`() {
        val livedataDelegate = LiveDataDelegate(SavedStateHandle(), ExampleData(0))
        var dataHolder by livedataDelegate

        val lifecycleOwner = object : LifecycleOwner {
            private val lifecycle = LifecycleRegistry(this)
            override fun getLifecycle() = lifecycle
        }
        livedataDelegate.observe(lifecycleOwner) { _, _ -> }
        lifecycleOwner.lifecycle.currentState = Lifecycle.State.STARTED
        lifecycleOwner.lifecycle.currentState = Lifecycle.State.DESTROYED

        dataHolder = dataHolder.copy(data = 1)

        var lastData: Pair<ExampleData?, ExampleData?> = Pair(null, null)
        livedataDelegate.observeForever { old, new ->
            lastData = Pair(old, new)
        }

        val (old, new) = lastData
        assertThat(old).isEqualTo(ExampleData(0))
        assertThat(new).isEqualTo(ExampleData(1))
    }

    @Test
    fun `when having two observers, then previous value is calculated correctly`() {
        val livedataDelegate = LiveDataDelegate(SavedStateHandle(), ExampleData(0))
        var dataHolder by livedataDelegate

        livedataDelegate.observeForever { _, _ -> }

        var lastData: Pair<ExampleData?, ExampleData?> = Pair(null, null)
        livedataDelegate.observeForever { old, new ->
            lastData = Pair(old, new)
        }
        dataHolder = dataHolder.copy(data = 1)

        val (old, new) = lastData
        assertThat(old).isEqualTo(ExampleData(0))
        assertThat(new).isEqualTo(ExampleData(1))
    }

    @Test
    fun `when observing livedata directly, then previous value is calculated correctly`() {
        val livedataDelegate = LiveDataDelegate(SavedStateHandle(), ExampleData(0))
        var dataHolder by livedataDelegate

        livedataDelegate.liveData.observeForever { }

        var lastData: Pair<ExampleData?, ExampleData?> = Pair(null, null)
        livedataDelegate.observeForever { old, new ->
            lastData = Pair(old, new)
        }
        dataHolder = dataHolder.copy(data = 1)

        val (old, new) = lastData
        assertThat(old).isEqualTo(ExampleData(0))
        assertThat(new).isEqualTo(ExampleData(1))
    }
}

@Parcelize
data class ExampleData(val data: Int) : Parcelable
