package com.woocommerce.android.extensions

import androidx.lifecycle.MutableLiveData

fun <T> MutableLiveData<MutableList<T>>.addNewItem(item: T) {
    val oldValue = this.value ?: mutableListOf()
    oldValue.add(item)
    this.value = oldValue
}

fun <T> MutableLiveData<MutableList<T>>.containsItem(item: T): Boolean {
    val oldValue = this.value ?: mutableListOf()
    return oldValue.contains(item)
}

fun <T> MutableLiveData<MutableList<T>>.isEmpty(): Boolean {
    val oldValue = this.value ?: mutableListOf()
    return oldValue.isEmpty()
}

fun <T> MutableLiveData<MutableList<T>>.removeItem(item: T) {
    if (!this.value.isNullOrEmpty()) {
        val oldValue = this.value
        oldValue?.remove(item)
        this.value = oldValue
    } else {
        this.value = mutableListOf()
    }
}

fun <T> MutableLiveData<MutableList<T>>.getList(): MutableList<T> =
    this.value ?: mutableListOf()

fun <T> MutableLiveData<MutableList<T>>.clearList() {
    this.value?.clear()
}
