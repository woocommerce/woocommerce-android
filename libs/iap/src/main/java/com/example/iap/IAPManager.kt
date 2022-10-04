package com.example.iap

import kotlinx.coroutines.flow.StateFlow

interface IAPManager {
    val iapPurchaseState: StateFlow<IAPPurchaseState>

    suspend fun connectToIAPService()
    suspend fun disconnectFromIAPService()
}
