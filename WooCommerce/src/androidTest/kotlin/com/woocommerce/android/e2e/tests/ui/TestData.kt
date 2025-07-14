package com.woocommerce.android.e2e.tests.ui

import com.woocommerce.android.e2e.helpers.util.OrderData
import com.woocommerce.android.e2e.helpers.util.ProductData

// Test data for Orders used in E2E (real API) tests

val pendingOrder = OrderData(
    customerName = "Samuel Ayala",
    id = 69,
    productsTotalRaw = "10.00",
    taxesRaw = "0.00",
    shippingRaw = "0.00",
    totalRaw = "10.00",
    statusRaw = "pending",
    customerNoteRaw = "Cappuccino is made on doppio, free of charge. Enjoy!"
)

val completedOrder = OrderData(
    customerName = "Samara Montes",
    id = 70,
    totalRaw = "7.00",
    statusRaw = "completed"
)

// Test data for Products used in E2E (real API) tests

val productSalad = ProductData(
    name = "Chicken Teriyaki Salad",
    stockStatusRaw = "instock",
    priceDiscountedRaw = "7",
    sku = "SLD-CHK-TRK"
)

val productCappuccino = ProductData(
    name = "Cappuccino",
    stockStatusRaw = "instock",
    variations = " • 6 variations",
    priceDiscountedRaw = "2",
    sku = "CF-CPC"
)

val productCappuccinoAlmondMedium = ProductData(
    name = productCappuccino.name + " - Medium, Almond",
    stockStatusRaw = productCappuccino.stockStatusRaw,
    priceDiscountedRaw = "3",
    sku = productCappuccino.sku + "-ALM-M"
)

val productCappuccinoAlmondLarge = ProductData(
    name = productCappuccino.name + " - Large, Almond",
    stockStatusRaw = productCappuccino.stockStatusRaw,
    priceDiscountedRaw = "4",
    sku = productCappuccino.sku + "-ALM-L"
)

val productCappuccinoCocoMedium = ProductData(
    name = productCappuccino.name,
    stockStatusRaw = productCappuccino.stockStatusRaw,
    priceDiscountedRaw = "3",
    sku = productCappuccino.sku + "-COCO-M"
)
