package com.woocommerce.android.ui.woopos.localcatalog

object WooPosCatalogFileParserTestData {
    const val SIMPLE_PRODUCT_JSON = """
        [
            {
                "type": "simple",
                "data": {
                    "id": 123,
                    "name": "Test Product",
                    "type": "simple",
                    "price": "19.99",
                    "regular_price": "19.99",
                    "sale_price": "",
                    "sku": "TEST-SKU-001",
                    "stock_status": "instock",
                    "stock_quantity": 100,
                    "manage_stock": true,
                    "weight": "1.5",
                    "dimensions": {
                        "length": "10",
                        "width": "5",
                        "height": "3"
                    },
                    "categories": [],
                    "tags": [],
                    "attributes": [],
                    "variations": [],
                    "images": [
                        {
                            "id": 456,
                            "src": "https://example.com/image.jpg",
                            "name": "product-image",
                            "alt": "Product Image"
                        }
                    ]
                }
            }
        ]
    """

    const val VARIABLE_PRODUCT_WITH_VARIATIONS_JSON = """
        [
            {
                "type": "variable",
                "data": {
                    "id": 200,
                    "name": "Variable T-Shirt",
                    "type": "variable",
                    "price": "15.00",
                    "regular_price": "",
                    "sale_price": "",
                    "sku": "VAR-SHIRT",
                    "stock_status": "instock",
                    "manage_stock": false,
                    "categories": [],
                    "tags": [],
                    "attributes": [
                        {
                            "id": 1,
                            "name": "Size",
                            "position": 0,
                            "visible": true,
                            "variation": true,
                            "options": ["Small", "Medium", "Large"]
                        }
                    ],
                    "variations": [301, 302, 303],
                    "images": []
                }
            },
            {
                "type": "variation",
                "data": {
                    "id": 301,
                    "parent_id": 200,
                    "price": "15.00",
                    "regular_price": "15.00",
                    "sale_price": "",
                    "sku": "VAR-SHIRT-S",
                    "stock_status": "instock",
                    "stock_quantity": 50,
                    "manage_stock": true,
                    "weight": "0.2",
                    "dimensions": {
                        "length": "",
                        "width": "",
                        "height": ""
                    },
                    "attributes": [
                        {
                            "id": 1,
                            "name": "Size",
                            "option": "Small"
                        }
                    ],
                    "image": {
                        "id": 401,
                        "src": "https://example.com/shirt-small.jpg",
                        "name": "shirt-small",
                        "alt": "Small Shirt"
                    }
                }
            },
            {
                "type": "variation",
                "data": {
                    "id": 302,
                    "parent_id": 200,
                    "price": "15.00",
                    "regular_price": "15.00",
                    "sale_price": "",
                    "sku": "VAR-SHIRT-M",
                    "stock_status": "instock",
                    "stock_quantity": 75,
                    "manage_stock": true,
                    "weight": "0.25",
                    "dimensions": {
                        "length": "",
                        "width": "",
                        "height": ""
                    },
                    "attributes": [
                        {
                            "id": 1,
                            "name": "Size",
                            "option": "Medium"
                        }
                    ],
                    "image": null
                }
            }
        ]
    """

    const val MIXED_PRODUCT_TYPES_JSON = """
        [
            {
                "type": "simple",
                "data": {
                    "id": 1001,
                    "name": "Simple Coffee Mug",
                    "type": "simple",
                    "price": "9.99",
                    "regular_price": "9.99",
                    "sku": "MUG-001",
                    "stock_status": "instock",
                    "stock_quantity": 200,
                    "manage_stock": true
                }
            },
            {
                "type": "variable",
                "data": {
                    "id": 2001,
                    "name": "Variable Notebook",
                    "type": "variable",
                    "price": "5.99",
                    "sku": "NOTEBOOK-VAR",
                    "stock_status": "instock",
                    "variations": [2101, 2102]
                }
            },
            {
                "type": "variation",
                "data": {
                    "id": 2101,
                    "parent_id": 2001,
                    "price": "5.99",
                    "regular_price": "5.99",
                    "sku": "NOTEBOOK-A5",
                    "stock_status": "instock",
                    "stock_quantity": 100,
                    "manage_stock": true,
                    "attributes": [
                        {
                            "id": 1,
                            "name": "Size",
                            "option": "A5"
                        }
                    ]
                }
            },
            {
                "type": "simple",
                "data": {
                    "id": 1002,
                    "name": "Pen Set",
                    "type": "simple",
                    "price": "14.99",
                    "regular_price": "19.99",
                    "sale_price": "14.99",
                    "sku": "PEN-SET",
                    "stock_status": "instock",
                    "stock_quantity": 50,
                    "manage_stock": true
                }
            }
        ]
    """

    const val WITH_UNKNOWN_TYPE_JSON = """
        [
            {
                "type": "simple",
                "data": {
                    "id": 100,
                    "name": "Valid Product",
                    "type": "simple",
                    "price": "10.00",
                    "sku": "VALID-001",
                    "stock_status": "instock"
                }
            },
            {
                "type": "unknown_type",
                "data": {
                    "id": 999,
                    "name": "Unknown Product"
                }
            },
            {
                "type": "variation",
                "data": {
                    "id": 200,
                    "parent_id": 100,
                    "price": "10.00",
                    "sku": "VALID-VAR-001",
                    "stock_status": "instock"
                }
            }
        ]
    """

    const val EMPTY_CATALOG_JSON = "[]"

    const val MALFORMED_JSON = "{ invalid json ["

    const val MISSING_DATA_FIELD_JSON = """
        [
            {
                "type": "simple"
            },
            {
                "type": "simple",
                "data": {
                    "id": 123,
                    "name": "Valid Product",
                    "type": "simple",
                    "price": "15.00",
                    "sku": "VALID",
                    "stock_status": "instock"
                }
            }
        ]
    """

    const val MISSING_TYPE_FIELD_JSON = """
        [
            {
                "data": {
                    "id": 999,
                    "name": "No Type Product"
                }
            },
            {
                "type": "simple",
                "data": {
                    "id": 123,
                    "name": "Valid Product",
                    "type": "simple",
                    "price": "15.00",
                    "sku": "VALID",
                    "stock_status": "instock"
                }
            }
        ]
    """
}
