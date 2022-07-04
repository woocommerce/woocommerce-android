package com.woocommerce.android.cardreader.internal.payments

internal enum class MetaDataKeys(val key: String) {
    STORE("paymentintent.storename"),

    /**
     * The customer's name - first name then last name separated by a space, or
     * empty if neither first name nor last name is given.
     * This key is also used by the plugin when it creates payment intents.
     */
    CUSTOMER_NAME("customer_name"),

    /**
     *  The customer's email address, or empty if not given.
     *  This key is also used by the plugin when it creates payment intents.
     */
    CUSTOMER_EMAIL("customer_email"),

    /**
     * The store URL, e.g. `https://mydomain.com`
     * This key is also used by the plugin when it creates payment intents.
     */
    SITE_URL("site_url"),

    /**
     * The order ID, e.g. 6140
     * This key is also used by the plugin when it creates payment intents.
     */
    ORDER_ID("order_id"),

    /**
     * The order key, e.g. `wc_order_0000000000000`
     * This key is also used by the plugin when it creates payment intents.
     */
    ORDER_KEY("order_key"),

    /**
     * The payment type, i.e. `single` or `recurring`
     * See also PaymentIntent.PaymentTypes
     * This key is also used by the plugin when it creates payment intents.
     */
    PAYMENT_TYPE("payment_type"),

    /**
     * Serial number of a reader which is used to collect the payment
     */
    READER_ID("reader_ID"),

    /**
     * Model name of a reader which is used to collect the payment
     */
    READER_MODEL("reader_model");

    enum class PaymentTypes(val key: String) {
        /**
         * A payment that IS NOT associated with an order containing a subscription
         * This key is also used by the plugin when it creates payment intents.
         */
        SINGLE("single"),

        /**
         * A payment that IS associated with an order containing a subscription
         * This key is also used by the plugin when it creates payment intents.
         */
        RECURRING("recurring")
    }
}
