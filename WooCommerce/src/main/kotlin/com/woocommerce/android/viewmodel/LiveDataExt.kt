package com.woocommerce.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData

fun <T, K, R> LiveData<T>.combineWith(
    liveData: LiveData<K>,
    block: (T?, K?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData.value)
    }
    result.addSource(liveData) {
        result.value = block(this.value, liveData.value)
    }
    return result
}

fun <T, K, G, R> LiveData<T>.combineWith(
    liveData1: LiveData<K>,
    liveData2: LiveData<G>,
    block: (T?, K?, G?) -> R
): LiveData<R> {
    val result = MediatorLiveData<R>()
    result.addSource(this) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    result.addSource(liveData1) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    result.addSource(liveData2) {
        result.value = block(this.value, liveData1.value, liveData2.value)
    }
    return result
}
