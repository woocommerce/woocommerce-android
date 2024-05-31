package com.woocommerce.android.analytics

import com.woocommerce.commons.WearAnalyticsEvent

interface IAnalyticsEvent {
    val siteless: Boolean
    val name: String
    val isPosEvent: Boolean
}

sealed class AnalyticsEvent(override val siteless: Boolean = false) : IAnalyticsEvent {
    override val name: String = this::class.simpleName!!
    override val isPosEvent: Boolean = false

    // -- General
    object APPLICATION_OPENED : AnalyticsEvent(siteless = true)
    object APPLICATION_CLOSED : AnalyticsEvent(siteless = true)
    object APPLICATION_INSTALLED : AnalyticsEvent(siteless = true)
    object APPLICATION_UPGRADED : AnalyticsEvent(siteless = true)
    object APPLICATION_VERSION_CHECK_FAILED : AnalyticsEvent(siteless = true)
    object BACK_PRESSED : AnalyticsEvent(siteless = true)
    object VIEW_SHOWN : AnalyticsEvent(siteless = true)
    object APPLICATION_STORE_SNAPSHOT : AnalyticsEvent(siteless = false)

    // -- Login
    object SIGNED_IN : AnalyticsEvent(siteless = true)
    object ACCOUNT_LOGOUT : AnalyticsEvent(siteless = true)
    object LOGIN_ACCESSED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_EXITED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_OPENED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_REQUESTED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_SUCCEEDED : AnalyticsEvent(siteless = true)
    object LOGIN_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_INSERTED_INVALID_URL : AnalyticsEvent(siteless = true)
    object LOGIN_AUTOFILL_CREDENTIALS_FILLED : AnalyticsEvent(siteless = true)
    object LOGIN_AUTOFILL_CREDENTIALS_UPDATED : AnalyticsEvent(siteless = true)
    object LOGIN_EMAIL_FORM_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_BY_EMAIL_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_PASSWORD_FORM_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_URL_FORM_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_URL_HELP_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_USERNAME_PASSWORD_FORM_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_TWO_FACTOR_FORM_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_FORGOT_PASSWORD_CLICKED : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_BUTTON_CLICK : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_BUTTON_FAILURE : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_CONNECT_SUCCESS : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_CONNECT_FAILURE : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_SUCCESS : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_FAILURE : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_2FA_NEEDED : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING : AnalyticsEvent(siteless = true)
    object LOGIN_SOCIAL_ERROR_UNKNOWN_USER : AnalyticsEvent(siteless = true)
    object LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE : AnalyticsEvent(siteless = true)
    object SIGNUP_EMAIL_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object SIGNUP_GOOGLE_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object SIGNUP_TERMS_OF_SERVICE_TAPPED : AnalyticsEvent(siteless = true)
    object SIGNUP_CANCELED : AnalyticsEvent(siteless = true)
    object SIGNUP_EMAIL_TO_LOGIN : AnalyticsEvent(siteless = true)
    object SIGNUP_MAGIC_LINK_FAILED : AnalyticsEvent(siteless = true)
    object SIGNUP_MAGIC_LINK_OPENED : AnalyticsEvent(siteless = true)
    object SIGNUP_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED : AnalyticsEvent(siteless = true)
    object SIGNUP_MAGIC_LINK_SENT : AnalyticsEvent(siteless = true)
    object SIGNUP_MAGIC_LINK_SUCCEEDED : AnalyticsEvent(siteless = true)
    object SIGNUP_SOCIAL_ACCOUNTS_NEED_CONNECTING : AnalyticsEvent(siteless = true)
    object SIGNUP_SOCIAL_BUTTON_FAILURE : AnalyticsEvent(siteless = true)
    object SIGNUP_SOCIAL_TO_LOGIN : AnalyticsEvent(siteless = true)
    object ADDED_SELF_HOSTED_SITE : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_REQUIRED_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_LEARN_MORE_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_WHAT_IS_JETPACK_HELP_SCREEN_OK_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_SITE_ADDRESS_SITE_INFO_REQUESTED : AnalyticsEvent(siteless = true)
    object LOGIN_SITE_ADDRESS_SITE_INFO_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_SITE_ADDRESS_SITE_INFO_SUCCEEDED : AnalyticsEvent(siteless = true)
    object LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_NEED_MORE_HELP_LINK_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_FIND_CONNECTED_EMAIL_HELP_SCREEN_OK_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_NO_JETPACK_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_NO_JETPACK_LOGOUT_LINK_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_NO_JETPACK_TRY_AGAIN_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_NO_JETPACK_MENU_HELP_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_NO_JETPACK_WHAT_IS_JETPACK_LINK_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_DISCOVERY_ERROR_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_DISCOVERY_ERROR_TROUBLESHOOT_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_DISCOVERY_ERROR_TRY_AGAIN_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_DISCOVERY_ERROR_SIGN_IN_WORDPRESS_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_DISCOVERY_ERROR_MENU_HELP_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_INTERCEPT_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_INTERCEPT_RETRY_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_UPDATE_TOKEN_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_FETCH_ACCOUNT_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SETTINGS_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_FETCH_SITES_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SUCCESS : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_FETCH_ACCOUNT_SETTINGS_SUCCESS : AnalyticsEvent(siteless = true)
    object LOGIN_MAGIC_LINK_FETCH_SITES_SUCCESS : AnalyticsEvent(siteless = true)
    object UNIFIED_LOGIN_STEP : AnalyticsEvent(siteless = true)
    object UNIFIED_LOGIN_FAILURE : AnalyticsEvent(siteless = true)
    object UNIFIED_LOGIN_INTERACTION : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_DISMISSED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_COMPLETED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_CONNECTION_ERROR_SHOWN : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_CONNECTION_URL_FETCH_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_CONNECT_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_CONNECT_COMPLETED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_CONNECT_DISMISSED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_CONNECTION_VERIFICATION_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_WITH_QR_CODE_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_WITH_QR_CODE_SCANNED : AnalyticsEvent(siteless = true)
    object LOGIN_PROLOGUE_STARTING_A_NEW_STORE_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_MALFORMED_APP_LOGIN_LINK : AnalyticsEvent(siteless = true)
    object LOGIN_APP_LOGIN_LINK_SUCCESS : AnalyticsEvent(siteless = true)
    object SIGNUP_LOGIN_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object SIGNUP_SUBMITTED : AnalyticsEvent(siteless = true)
    object SIGNUP_SUCCESS : AnalyticsEvent(siteless = true)
    object SIGNUP_ERROR : AnalyticsEvent(siteless = true)
    object LOGIN_SITE_CREDENTIALS_LOGIN_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_INSUFFICIENT_ROLE : AnalyticsEvent(siteless = false)
    object LOGIN_2FA_NEEDED : AnalyticsEvent(siteless = true)
    object LOGIN_USE_SECURITY_KEY_CLICKED : AnalyticsEvent(siteless = true)
    object LOGIN_SECURITY_KEY_FAILURE : AnalyticsEvent(siteless = true)
    object LOGIN_SECURITY_KEY_SUCCESS : AnalyticsEvent(siteless = true)

    // -- Site Picker
    object SITE_PICKER_STORES_SHOWN : AnalyticsEvent(siteless = true)
    object SITE_PICKER_CONTINUE_TAPPED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_HELP_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_AUTO_LOGIN_SUBMITTED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_TO_USER : AnalyticsEvent(siteless = true)
    object SITE_PICKER_AUTO_LOGIN_ERROR_NOT_WOO_STORE : AnalyticsEvent(siteless = true)
    object SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_NON_WOO_SITE_TAPPED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_NEW_TO_WOO_TAPPED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_ADD_A_STORE_TAPPED : AnalyticsEvent(siteless = true)
    object SITE_PICKER_SITE_DISCOVERY : AnalyticsEvent(siteless = true)
    object SITE_PICKER_JETPACK_TIMEOUT_ERROR_SHOWN : AnalyticsEvent(siteless = true)
    object SITE_PICKER_JETPACK_TIMEOUT_CONTACT_SUPPORT_CLICKED : AnalyticsEvent(siteless = true)

    // -- Jetpack Installation for Login
    object LOGIN_JETPACK_SITE_CREDENTIAL_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SITE_CREDENTIAL_SCREEN_DISMISSED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SITE_CREDENTIAL_INSTALL_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SITE_CREDENTIAL_RESET_PASSWORD_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SITE_CREDENTIAL_DID_SHOW_ERROR_ALERT : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SITE_CREDENTIAL_DID_FINISH_LOGIN : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_SCREEN_VIEWED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_SCREEN_DISMISSED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_INSTALL_SUCCESSFUL : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_INSTALL_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_ACTIVATION_SUCCESSFUL : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_ACTIVATION_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_FETCH_JETPACK_CONNECTION_URL_SUCCESSFUL : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_FETCH_JETPACK_CONNECTION_URL_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_CANNOT_FIND_WPCOM_USER : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_AUTHORIZED_USING_DIFFERENT_WPCOM_ACCOUNT : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_ALL_STEPS_MARKED_DONE : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_ERROR_CHECKING_JETPACK_CONNECTION : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_GO_TO_STORE_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_FETCHING_WPCOM_SITES_FAILED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_GET_SUPPORT_BUTTON_TAPPED : AnalyticsEvent(siteless = true)
    object LOGIN_JETPACK_SETUP_TRY_AGAIN_BUTTON_TAPPED : AnalyticsEvent(siteless = true)

    // -- Dashboard
    object DASHBOARD_PULLED_TO_REFRESH : AnalyticsEvent()
    object DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED : AnalyticsEvent()
    object DASHBOARD_MAIN_STATS_DATE : AnalyticsEvent()
    object DASHBOARD_MAIN_STATS_LOADED : AnalyticsEvent()
    object DASHBOARD_TOP_PERFORMERS_DATE : AnalyticsEvent()
    object DASHBOARD_TOP_PERFORMERS_LOADED : AnalyticsEvent()
    object DASHBOARD_NEW_STATS_REVERTED_BANNER_DISMISS_TAPPED : AnalyticsEvent()
    object DASHBOARD_NEW_STATS_REVERTED_BANNER_LEARN_MORE_TAPPED : AnalyticsEvent()
    object DASHBOARD_WAITING_TIME_LOADED : AnalyticsEvent()
    object DASHBOARD_SEE_MORE_ANALYTICS_TAPPED : AnalyticsEvent()
    object DASHBOARD_STORE_TIMEZONE_DIFFER_FROM_DEVICE : AnalyticsEvent()
    object USED_ANALYTICS : AnalyticsEvent()
    object STATS_UNEXPECTED_FORMAT : AnalyticsEvent()
    object DASHBOARD_STATS_CUSTOM_RANGE_ADD_BUTTON_TAPPED : AnalyticsEvent()
    object DASHBOARD_STATS_CUSTOM_RANGE_CONFIRMED : AnalyticsEvent()
    object DASHBOARD_STATS_CUSTOM_RANGE_TAB_SELECTED : AnalyticsEvent()
    object DASHBOARD_STATS_CUSTOM_RANGE_EDIT_BUTTON_TAPPED : AnalyticsEvent()
    object DASHBOARD_STATS_CUSTOM_RANGE_INTERACTED : AnalyticsEvent()
    object DYNAMIC_DASHBOARD_EDIT_LAYOUT_BUTTON_TAPPED : AnalyticsEvent()
    object DYNAMIC_DASHBOARD_HIDE_CARD_TAPPED : AnalyticsEvent()
    object DYNAMIC_DASHBOARD_EDITOR_SAVE_TAPPED : AnalyticsEvent()
    object DYNAMIC_DASHBOARD_CARD_RETRY_TAPPED : AnalyticsEvent()

    // -- Analytics Hub
    object ANALYTICS_HUB_DATE_RANGE_BUTTON_TAPPED : AnalyticsEvent()
    object ANALYTICS_HUB_DATE_RANGE_SELECTED : AnalyticsEvent()
    object ANALYTICS_HUB_PULL_TO_REFRESH_TRIGGERED : AnalyticsEvent()
    object ANALYTICS_HUB_VIEW_FULL_REPORT_TAPPED : AnalyticsEvent()
    object ANALYTICS_HUB_SETTINGS_OPENED : AnalyticsEvent()
    object ANALYTICS_HUB_SETTINGS_SAVED : AnalyticsEvent()

    // -- Orders List
    object ORDERS_LIST_FILTER : AnalyticsEvent()
    object ORDERS_LIST_SEARCH : AnalyticsEvent()
    object ORDERS_LIST_LOADED : AnalyticsEvent()
    object ORDER_LIST_LOAD_ERROR : AnalyticsEvent()
    object ORDERS_LIST_PULLED_TO_REFRESH : AnalyticsEvent()
    object ORDERS_LIST_MENU_SEARCH_TAPPED : AnalyticsEvent()
    object ORDERS_LIST_VIEW_FILTER_OPTIONS_TAPPED : AnalyticsEvent()
    object ORDER_LIST_WAITING_TIME_LOADED : AnalyticsEvent()
    object ORDER_LIST_PRODUCT_BARCODE_SCANNING_TAPPED : AnalyticsEvent()
    object ORDER_LIST_TEST_ORDER_DISPLAYED : AnalyticsEvent()
    object ORDER_LIST_TRY_TEST_ORDER_TAPPED : AnalyticsEvent()
    object ORDERS_LIST_AUTOMATIC_TIMEOUT_RETRY : AnalyticsEvent()
    object ORDERS_LIST_TOP_BANNER_TROUBLESHOOT_TAPPED : AnalyticsEvent()
    object TEST_ORDER_START_TAPPED : AnalyticsEvent()

    object FILTER_ORDERS_BY_STATUS_DIALOG_OPTION_SELECTED : AnalyticsEvent()
    object ORDER_FILTER_LIST_CLEAR_MENU_BUTTON_TAPPED : AnalyticsEvent()

    // -- Payments
    object PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED : AnalyticsEvent()
    object PAYMENTS_FLOW_COMPLETED : AnalyticsEvent()
    object PAYMENTS_FLOW_COLLECT : AnalyticsEvent()
    object PAYMENTS_FLOW_FAILED : AnalyticsEvent()
    object PAYMENTS_FLOW_CANCELED : AnalyticsEvent()

    // -- Simple Payments
    object SIMPLE_PAYMENTS_FLOW_NOTE_ADDED : AnalyticsEvent()
    object SIMPLE_PAYMENTS_FLOW_TAXES_TOGGLED : AnalyticsEvent()
    object SIMPLE_PAYMENTS_MIGRATION_SHEET_ADD_CUSTOM_AMOUNT : AnalyticsEvent()
    object SIMPLE_PAYMENTS_MIGRATION_SHEET_SHOWN : AnalyticsEvent()

    // -- Upsell Banner
    object FEATURE_CARD_SHOWN : AnalyticsEvent()
    object FEATURE_CARD_DISMISSED : AnalyticsEvent()
    object FEATURE_CARD_CTA_TAPPED : AnalyticsEvent()

    // -- Just In Time Messages
    object JITM_FETCH_SUCCESS : AnalyticsEvent()
    object JITM_FETCH_FAILURE : AnalyticsEvent()
    object JITM_DISPLAYED : AnalyticsEvent()
    object JITM_CTA_TAPPED : AnalyticsEvent()
    object JITM_DISMISS_TAPPED : AnalyticsEvent()
    object JITM_DISMISS_SUCCESS : AnalyticsEvent()
    object JITM_DISMISS_FAILURE : AnalyticsEvent()

    // -- Order Detail
    object ORDER_OPEN : AnalyticsEvent()
    object ORDER_CONTACT_ACTION : AnalyticsEvent()
    object ORDER_CONTACT_ACTION_FAILED : AnalyticsEvent()
    object ORDER_STATUS_CHANGE : AnalyticsEvent()
    object ORDER_STATUS_CHANGE_FAILED : AnalyticsEvent()
    object ORDER_STATUS_CHANGE_SUCCESS : AnalyticsEvent()
    object ORDER_DETAIL_PULLED_TO_REFRESH : AnalyticsEvent()
    object ORDER_DETAIL_ADD_NOTE_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CUSTOMER_INFO_SHOW_BILLING_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CUSTOMER_INFO_HIDE_BILLING_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CUSTOMER_INFO_EMAIL_MENU_EMAIL_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_PHONE_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_SMS_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_WHATSAPP_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_TELEGRAM_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_FULFILL_ORDER_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_PRODUCT_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_CREATE_SHIPPING_LABEL_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_DETAIL_WAITING_TIME_LOADED : AnalyticsEvent()
    object ORDER_VIEW_CUSTOM_FIELDS_TAPPED : AnalyticsEvent()
    object ORDER_DETAILS_SUBSCRIPTIONS_SHOWN : AnalyticsEvent()
    object ORDER_DETAILS_GIFT_CARD_SHOWN : AnalyticsEvent()
    object ORDER_PRODUCTS_LOADED : AnalyticsEvent()
    object ORDER_DETAIL_TRASH_TAPPED : AnalyticsEvent()
    object ORDER_DETAILS_SHIPPING_METHODS_SHOWN : AnalyticsEvent()

    // - Order detail editing
    object ORDER_DETAIL_EDIT_FLOW_STARTED : AnalyticsEvent()
    object ORDER_DETAIL_EDIT_FLOW_COMPLETED : AnalyticsEvent()
    object ORDER_DETAIL_EDIT_FLOW_FAILED : AnalyticsEvent()
    object ORDER_DETAIL_EDIT_FLOW_CANCELED : AnalyticsEvent()
    object ORDER_EDIT_BUTTON_TAPPED : AnalyticsEvent()
    object PLUGINS_NOT_SYNCED_YET : AnalyticsEvent()

    // -- Order Creation
    object ORDERS_ADD_NEW : AnalyticsEvent()
    object ORDER_PRODUCT_ADD : AnalyticsEvent()
    object ORDER_CUSTOMER_ADD : AnalyticsEvent()
    object ORDER_CUSTOMER_DELETE : AnalyticsEvent()
    object ORDER_FEE_ADD : AnalyticsEvent()
    object ORDER_FEE_UPDATE : AnalyticsEvent()
    object ORDER_SHIPPING_METHOD_ADD : AnalyticsEvent()
    object ORDER_CREATE_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_CREATION_SUCCESS : AnalyticsEvent()
    object ORDER_CREATION_FAILED : AnalyticsEvent()
    object ORDER_SYNC_FAILED : AnalyticsEvent()
    object ORDER_CREATION_CUSTOMER_SEARCH : AnalyticsEvent()
    object ORDER_CREATION_CUSTOMER_ADDED : AnalyticsEvent()
    object ORDER_CREATION_CUSTOMER_ADD_MANUALLY_TAPPED : AnalyticsEvent()
    object ORDER_PRODUCT_QUANTITY_CHANGE : AnalyticsEvent()
    object ORDER_PRODUCT_REMOVE : AnalyticsEvent()
    object ORDER_FEE_REMOVE : AnalyticsEvent()
    object ORDER_SHIPPING_METHOD_REMOVE : AnalyticsEvent()
    object ORDER_CREATION_PRODUCT_SELECTOR_ITEM_SELECTED : AnalyticsEvent()
    object ORDER_CREATION_PRODUCT_SELECTOR_ITEM_UNSELECTED : AnalyticsEvent()
    object ORDER_CREATION_PRODUCT_SELECTOR_CONFIRM_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_CREATION_PRODUCT_SELECTOR_CLEAR_SELECTION_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_CREATION_PRODUCT_BARCODE_SCANNING_TAPPED : AnalyticsEvent()
    object ORDER_CREATION_PRODUCT_SELECTOR_SEARCH_TRIGGERED : AnalyticsEvent()
    object ORDER_TAXES_HELP_BUTTON_TAPPED : AnalyticsEvent()
    object TAX_EDUCATIONAL_DIALOG_EDIT_IN_ADMIN_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_CREATION_SET_NEW_TAX_RATE_TAPPED : AnalyticsEvent()
    object TAX_RATE_SELECTOR_TAX_RATE_TAPPED : AnalyticsEvent()
    object TAX_RATE_SELECTOR_EDIT_IN_ADMIN_TAPPED : AnalyticsEvent()
    object TAX_RATE_AUTO_TAX_BOTTOM_SHEET_DISPLAYED : AnalyticsEvent()
    object TAX_RATE_AUTO_TAX_RATE_SET_NEW_RATE_FOR_ORDER_TAPPED : AnalyticsEvent()
    object TAX_RATE_AUTO_TAX_RATE_CLEAR_ADDRESS_TAPPED : AnalyticsEvent()
    object ORDER_FORM_TOTALS_PANEL_TOGGLED : AnalyticsEvent()
    object ORDER_FORM_ADD_GIFT_CARD_CTA_SHOWN : AnalyticsEvent()
    object ORDER_FORM_ADD_GIFT_CARD_CTA_TAPPED : AnalyticsEvent()
    object ORDER_FORM_GIFT_CARD_SET : AnalyticsEvent()
    object ORDER_SHIPPING_METHOD_SELECTED : AnalyticsEvent()
    object ORDER_ADD_SHIPPING_TAPPED : AnalyticsEvent()

    // -- Custom Amounts
    object ORDER_CREATION_ADD_CUSTOM_AMOUNT_TAPPED : AnalyticsEvent()
    object ORDER_CREATION_EDIT_CUSTOM_AMOUNT_TAPPED : AnalyticsEvent()
    object ORDER_CREATION_REMOVE_CUSTOM_AMOUNT_TAPPED : AnalyticsEvent()
    object ADD_CUSTOM_AMOUNT_NAME_ADDED : AnalyticsEvent()
    object ADD_CUSTOM_AMOUNT_DONE_TAPPED : AnalyticsEvent()
    object ADD_CUSTOM_AMOUNT_PERCENTAGE_ADDED : AnalyticsEvent()

    // -- Barcode Scanner
    object BARCODE_SCANNING_SUCCESS : AnalyticsEvent()
    object BARCODE_SCANNING_FAILURE : AnalyticsEvent()

    // -- Scan to Update Inventory
    object PRODUCT_LIST_PRODUCT_BARCODE_SCANNING_TAPPED : AnalyticsEvent()
    object PRODUCT_QUICK_INVENTORY_UPDATE_INCREMENT_QUANTITY_TAPPED : AnalyticsEvent()
    object PRODUCT_QUICK_INVENTORY_UPDATE_MANUAL_QUANTITY_UPDATE_TAPPED : AnalyticsEvent()
    object PRODUCT_QUICK_INVENTORY_UPDATE_DISMISSED : AnalyticsEvent()
    object PRODUCT_QUICK_INVENTORY_QUANTITY_UPDATE_SUCCESS : AnalyticsEvent()
    object PRODUCT_QUICK_INVENTORY_QUANTITY_UPDATE_FAILURE : AnalyticsEvent()
    object PRODUCT_QUICK_INVENTORY_VIEW_PRODUCT_DETAILS_TAPPED : AnalyticsEvent()
    object PRODUCT_QUICK_INVENTORY_UPDATE_BOTTOM_SHEET_SHOWN : AnalyticsEvent()

    // -- Product Search Via SKU
    object PRODUCT_SEARCH_VIA_SKU_SUCCESS : AnalyticsEvent()
    object PRODUCT_SEARCH_VIA_SKU_FAILURE : AnalyticsEvent()

    // -- Refunds
    object CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED : AnalyticsEvent()
    object CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED : AnalyticsEvent()
    object CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED : AnalyticsEvent()
    object CREATE_ORDER_REFUND_PRODUCT_AMOUNT_DIALOG_OPENED : AnalyticsEvent()
    object CREATE_ORDER_REFUND_SUMMARY_REFUND_BUTTON_TAPPED : AnalyticsEvent()
    object REFUND_CREATE : AnalyticsEvent()
    object REFUND_CREATE_SUCCESS : AnalyticsEvent()
    object REFUND_CREATE_FAILED : AnalyticsEvent()

    // -- Order Notes
    object ADD_ORDER_NOTE_ADD_BUTTON_TAPPED : AnalyticsEvent()
    object ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED : AnalyticsEvent()
    object ORDER_NOTE_ADD : AnalyticsEvent()
    object ORDER_NOTE_ADD_FAILED : AnalyticsEvent()
    object ORDER_NOTE_ADD_SUCCESS : AnalyticsEvent()

    // -- Order Shipment Tracking
    object ORDER_SHIPMENT_TRACKING_CARRIER_SELECTED : AnalyticsEvent()
    object ORDER_TRACKING_ADD : AnalyticsEvent()
    object ORDER_TRACKING_ADD_FAILED : AnalyticsEvent()
    object ORDER_TRACKING_ADD_SUCCESS : AnalyticsEvent()
    object ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_SHIPMENT_TRACKING_CUSTOM_PROVIDER_SELECTED : AnalyticsEvent()
    object ORDER_TRACKING_DELETE_SUCCESS : AnalyticsEvent()
    object ORDER_TRACKING_DELETE_FAILED : AnalyticsEvent()
    object ORDER_TRACKING_PROVIDERS_LOADED : AnalyticsEvent()
    object SHIPMENT_TRACKING_MENU_ACTION : AnalyticsEvent()

    // -- Order Coupon
    object ORDER_COUPON_ADD : AnalyticsEvent()
    object ORDER_COUPON_REMOVE : AnalyticsEvent()
    object ORDER_GO_TO_COUPON_BUTTON_TAPPED : AnalyticsEvent()

    // -- Order discount
    object ORDER_PRODUCT_DISCOUNT_ADD : AnalyticsEvent()
    object ORDER_PRODUCT_DISCOUNT_REMOVE : AnalyticsEvent()
    object ORDER_PRODUCT_DISCOUNT_ADD_BUTTON_TAPPED : AnalyticsEvent()
    object ORDER_PRODUCT_DISCOUNT_EDIT_BUTTON_TAPPED : AnalyticsEvent()

    // -- Shipping Labels
    object SHIPPING_LABEL_API_REQUEST : AnalyticsEvent()
    object SHIPPING_LABEL_PRINT_REQUESTED : AnalyticsEvent()
    object SHIPPING_LABEL_REFUND_REQUESTED : AnalyticsEvent()
    object SHIPPING_LABEL_PURCHASE_FLOW : AnalyticsEvent()
    object SHIPPING_LABEL_DISCOUNT_INFO_BUTTON_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_EDIT_ADDRESS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_EDIT_ADDRESS_USE_ADDRESS_AS_IS_BUTTON_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_EDIT_ADDRESS_OPEN_MAP_BUTTON_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_EDIT_ADDRESS_CONTACT_CUSTOMER_BUTTON_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_ADDRESS_SUGGESTIONS_USE_SELECTED_ADDRESS_BUTTON_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_ADDRESS_SUGGESTIONS_EDIT_SELECTED_ADDRESS_BUTTON_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_ADDRESS_VALIDATION_FAILED : AnalyticsEvent()
    object SHIPPING_LABEL_ADDRESS_VALIDATION_SUCCEEDED : AnalyticsEvent()
    object SHIPPING_LABEL_ORDER_FULFILL_SUCCEEDED : AnalyticsEvent()
    object SHIPPING_LABEL_ORDER_FULFILL_FAILED : AnalyticsEvent()
    object SHIPPING_LABEL_MOVE_ITEM_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_ITEM_MOVED : AnalyticsEvent()
    object SHIPPING_LABEL_ADD_PAYMENT_METHOD_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_PAYMENT_METHOD_ADDED : AnalyticsEvent()
    object SHIPPING_LABEL_ADD_PACKAGE_TAPPED : AnalyticsEvent()
    object SHIPPING_LABEL_PACKAGE_ADDED_SUCCESSFULLY : AnalyticsEvent()
    object SHIPPING_LABEL_ADD_PACKAGE_FAILED : AnalyticsEvent()
    object SHIPPING_LABEL_ORDER_IS_ELIGIBLE : AnalyticsEvent()

    // -- Card Present Payments - onboarding
    object CARD_PRESENT_ONBOARDING_LEARN_MORE_TAPPED : AnalyticsEvent()
    object CARD_PRESENT_ONBOARDING_NOT_COMPLETED : AnalyticsEvent()
    object CARD_PRESENT_ONBOARDING_COMPLETED : AnalyticsEvent()
    object CARD_PRESENT_ONBOARDING_STEP_SKIPPED : AnalyticsEvent()
    object CARD_PRESENT_ONBOARDING_CTA_TAPPED : AnalyticsEvent()
    object CARD_PRESENT_ONBOARDING_CTA_FAILED : AnalyticsEvent()
    object CARD_PRESENT_PAYMENT_GATEWAY_SELECTED : AnalyticsEvent()

    // -- Cash on Delivery - onboarding
    object ENABLE_CASH_ON_DELIVERY_SUCCESS : AnalyticsEvent()
    object ENABLE_CASH_ON_DELIVERY_FAILED : AnalyticsEvent()
    object DISABLE_CASH_ON_DELIVERY_SUCCESS : AnalyticsEvent()
    object DISABLE_CASH_ON_DELIVERY_FAILED : AnalyticsEvent()

    // -- Card Present Payments - collection
    object CARD_PRESENT_COLLECT_PAYMENT_FAILED : AnalyticsEvent()
    object CARD_PRESENT_COLLECT_PAYMENT_CANCELLED : AnalyticsEvent()
    object CARD_PRESENT_COLLECT_PAYMENT_SUCCESS : AnalyticsEvent()
    object CARD_PRESENT_PAYMENT_FAILED_CONTACT_SUPPORT_TAPPED : AnalyticsEvent()
    object CARD_PRESENT_TAP_TO_PAY_PAYMENT_FAILED_ENABLE_NFC_TAPPED : AnalyticsEvent()

    // --Card Present Payments - Interac refund
    object CARD_PRESENT_COLLECT_INTERAC_PAYMENT_SUCCESS : AnalyticsEvent()
    object CARD_PRESENT_COLLECT_INTERAC_PAYMENT_FAILED : AnalyticsEvent()
    object CARD_PRESENT_COLLECT_INTERAC_REFUND_CANCELLED : AnalyticsEvent()

    // -- Card Reader - discovery
    object CARD_READER_DISCOVERY_TAPPED : AnalyticsEvent()
    object CARD_READER_DISCOVERY_FAILED : AnalyticsEvent()
    object CARD_READER_DISCOVERY_READER_DISCOVERED : AnalyticsEvent()

    // -- Card Reader - connection
    object CARD_READER_CONNECTION_TAPPED : AnalyticsEvent()
    object CARD_READER_CONNECTION_FAILED : AnalyticsEvent()
    object CARD_READER_CONNECTION_SUCCESS : AnalyticsEvent()
    object CARD_READER_DISCONNECT_TAPPED : AnalyticsEvent()
    object CARD_READER_AUTO_CONNECTION_STARTED : AnalyticsEvent()
    object CARD_PRESENT_CONNECTION_LEARN_MORE_TAPPED : AnalyticsEvent()
    object MANAGE_CARD_READERS_AUTOMATIC_DISCONNECT_BUILT_IN_READER : AnalyticsEvent()
    object CARD_READER_AUTOMATIC_DISCONNECT : AnalyticsEvent()

    // -- Card Reader - software update
    object CARD_READER_SOFTWARE_UPDATE_STARTED : AnalyticsEvent()
    object CARD_READER_SOFTWARE_UPDATE_SUCCESS : AnalyticsEvent()
    object CARD_READER_SOFTWARE_UPDATE_FAILED : AnalyticsEvent()
    object CARD_READER_SOFTWARE_UPDATE_ALERT_SHOWN : AnalyticsEvent()
    object CARD_READER_SOFTWARE_UPDATE_ALERT_INSTALL_CLICKED : AnalyticsEvent()

    // -- Card Reader - Location
    object CARD_READER_LOCATION_SUCCESS : AnalyticsEvent()
    object CARD_READER_LOCATION_FAILURE : AnalyticsEvent()
    object CARD_READER_LOCATION_MISSING_TAPPED : AnalyticsEvent()

    // -- Card Reader - reader type selection
    object CARD_PRESENT_SELECT_READER_TYPE_BUILT_IN_TAPPED : AnalyticsEvent()
    object CARD_PRESENT_SELECT_READER_TYPE_BLUETOOTH_TAPPED : AnalyticsEvent()

    // -- Card Reader - tap to pay not available
    object CARD_PRESENT_TAP_TO_PAY_NOT_AVAILABLE : AnalyticsEvent()

    // -- Receipts
    object RECEIPT_PRINT_TAPPED : AnalyticsEvent()
    object RECEIPT_EMAIL_TAPPED : AnalyticsEvent()
    object RECEIPT_EMAIL_FAILED : AnalyticsEvent()
    object RECEIPT_PRINT_FAILED : AnalyticsEvent()
    object RECEIPT_PRINT_CANCELED : AnalyticsEvent()
    object RECEIPT_PRINT_SUCCESS : AnalyticsEvent()
    object RECEIPT_VIEW_TAPPED : AnalyticsEvent()
    object RECEIPT_URL_FETCHING_FAILS : AnalyticsEvent()

    // -- Top-level navigation
    object MAIN_MENU_SETTINGS_TAPPED : AnalyticsEvent()
    object MAIN_MENU_CONTACT_SUPPORT_TAPPED : AnalyticsEvent()
    object MAIN_TAB_DASHBOARD_SELECTED : AnalyticsEvent()
    object MAIN_TAB_DASHBOARD_RESELECTED : AnalyticsEvent()
    object MAIN_TAB_ORDERS_SELECTED : AnalyticsEvent()
    object MAIN_TAB_ORDERS_RESELECTED : AnalyticsEvent()
    object MAIN_TAB_PRODUCTS_SELECTED : AnalyticsEvent()
    object MAIN_TAB_PRODUCTS_RESELECTED : AnalyticsEvent()
    object MAIN_TAB_HUB_MENU_SELECTED : AnalyticsEvent()
    object MAIN_TAB_HUB_MENU_RESELECTED : AnalyticsEvent()

    // -- Settings
    object SETTING_CHANGE : AnalyticsEvent()
    object SETTING_CHANGE_FAILED : AnalyticsEvent()
    object SETTING_CHANGE_SUCCESS : AnalyticsEvent()
    object SETTINGS_LOGOUT_BUTTON_TAPPED : AnalyticsEvent()
    object SETTINGS_LOGOUT_CONFIRMATION_DIALOG_RESULT : AnalyticsEvent()
    object SETTINGS_BETA_FEATURES_BUTTON_TAPPED : AnalyticsEvent()
    object SETTINGS_PRIVACY_SETTINGS_BUTTON_TAPPED : AnalyticsEvent()
    object SETTINGS_FEATURE_REQUEST_BUTTON_TAPPED : AnalyticsEvent()
    object SETTINGS_ABOUT_WOOCOMMERCE_LINK_TAPPED : AnalyticsEvent()
    object SETTINGS_ABOUT_BUTTON_TAPPED : AnalyticsEvent()
    object SETTINGS_ABOUT_OPEN_SOURCE_LICENSES_LINK_TAPPED : AnalyticsEvent()
    object SETTINGS_NOTIFICATIONS_OPEN_CHANNEL_SETTINGS_BUTTON_TAPPED : AnalyticsEvent()
    object SETTINGS_WE_ARE_HIRING_BUTTON_TAPPED : AnalyticsEvent()
    object SETTINGS_IMAGE_OPTIMIZATION_TOGGLED : AnalyticsEvent()
    object SETTINGS_CARD_PRESENT_SELECT_PAYMENT_GATEWAY_TAPPED : AnalyticsEvent()
    object PRIVACY_SETTINGS_PRIVACY_POLICY_LINK_TAPPED : AnalyticsEvent()
    object PRIVACY_SETTINGS_SHARE_INFO_LINK_TAPPED : AnalyticsEvent()
    object PRIVACY_SETTINGS_THIRD_PARTY_TRACKING_INFO_LINK_TAPPED : AnalyticsEvent()
    object SETTINGS_DOMAINS_TAPPED : AnalyticsEvent()

    // -- Payments Hub
    object PAYMENTS_HUB_COLLECT_PAYMENT_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_ORDER_CARD_READER_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_CARD_READER_MANUALS_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_MANAGE_CARD_READERS_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_ONBOARDING_ERROR_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED : AnalyticsEvent()
    object PAYMENTS_HUB_CASH_ON_DELIVERY_TOGGLED_LEARN_MORE_TAPPED : AnalyticsEvent()
    object IN_PERSON_PAYMENTS_LEARN_MORE_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_TAP_TO_PAY_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_TAP_TO_PAY_FEEDBACK_TAPPED : AnalyticsEvent()
    object PAYMENTS_HUB_TAP_TO_PAY_ABOUT_TAPPED : AnalyticsEvent()

    // -- Payments Hub - Deposit Summary
    object PAYMENTS_HUB_DEPOSIT_SUMMARY_SHOWN : AnalyticsEvent()
    object PAYMENTS_HUB_DEPOSIT_SUMMARY_EXPANDED : AnalyticsEvent()
    object PAYMENTS_HUB_DEPOSIT_SUMMARY_ERROR : AnalyticsEvent()
    object PAYMENTS_HUB_DEPOSIT_SUMMARY_LEARN_MORE_CLICKED : AnalyticsEvent()
    object PAYMENTS_HUB_DEPOSIT_SUMMARY_CURRENCY_SELECTED : AnalyticsEvent()

    // -- TAP TO PAY SUMMARY
    object TAP_TO_PAY_SUMMARY_TRY_PAYMENT_TAPPED : AnalyticsEvent()
    object TAP_TO_PAY_SUMMARY_SHOWN : AnalyticsEvent()
    object CARD_PRESENT_TAP_TO_PAY_TEST_PAYMENT_REFUND_SUCCESS : AnalyticsEvent()
    object CARD_PRESENT_TAP_TO_PAY_TEST_PAYMENT_REFUND_FAILED : AnalyticsEvent()

    // -- Product list
    object PRODUCT_LIST_LOADED : AnalyticsEvent()
    object PRODUCT_LIST_LOAD_ERROR : AnalyticsEvent()
    object PRODUCT_LIST_PRODUCT_TAPPED : AnalyticsEvent()
    object PRODUCT_LIST_PULLED_TO_REFRESH : AnalyticsEvent()
    object PRODUCT_LIST_SEARCHED : AnalyticsEvent()
    object PRODUCT_LIST_MENU_SEARCH_TAPPED : AnalyticsEvent()
    object PRODUCT_LIST_VIEW_FILTER_OPTIONS_TAPPED : AnalyticsEvent()
    object PRODUCT_LIST_VIEW_SORTING_OPTIONS_TAPPED : AnalyticsEvent()
    object PRODUCT_LIST_SORTING_OPTION_SELECTED : AnalyticsEvent()
    object PRODUCT_LIST_ADD_PRODUCT_BUTTON_TAPPED : AnalyticsEvent()
    object ADD_PRODUCT_PRODUCT_TYPE_SELECTED : AnalyticsEvent()
    object PRODUCT_LIST_BULK_UPDATE_REQUESTED : AnalyticsEvent()
    object PRODUCT_LIST_BULK_UPDATE_CONFIRMED : AnalyticsEvent()
    object PRODUCT_LIST_BULK_UPDATE_SUCCESS : AnalyticsEvent()
    object PRODUCT_LIST_BULK_UPDATE_FAILURE : AnalyticsEvent()
    object PRODUCT_LIST_BULK_UPDATE_SELECT_ALL_TAPPED : AnalyticsEvent()
    object PRODUCT_FILTER_LIST_EXPLORE_BUTTON_TAPPED : AnalyticsEvent()

    // -- Product detail
    object PRODUCT_DETAIL_LOADED : AnalyticsEvent()
    object PRODUCT_DETAIL_IMAGE_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_SHARE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_UPDATE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_EXTERNAL_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_PRODUCT_VARIANTS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_PRODUCT_DESCRIPTION_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_PRICE_SETTINGS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_INVENTORY_SETTINGS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_SHIPPING_SETTINGS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_SHORT_DESCRIPTION_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_CATEGORIES_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_TAGS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_PRODUCT_TYPE_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_PRODUCT_REVIEWS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_GROUPED_PRODUCTS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_LINKED_PRODUCTS_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_VIEW_DOWNLOADABLE_FILES_TAPPED : AnalyticsEvent()
    object PRODUCT_PRICE_SETTINGS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_INVENTORY_SETTINGS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_SHIPPING_SETTINGS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_IMAGE_SETTINGS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_CATEGORY_SETTINGS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_TAG_SETTINGS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_SUBSCRIPTION_EXPIRATION_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_SUBSCRIPTION_FREE_TRIAL_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAIL_UPDATE_SUCCESS : AnalyticsEvent()
    object PRODUCT_DETAIL_UPDATE_ERROR : AnalyticsEvent()
    object ADD_PRODUCT_PUBLISH_TAPPED : AnalyticsEvent()
    object ADD_PRODUCT_SAVE_AS_DRAFT_TAPPED : AnalyticsEvent()
    object ADD_PRODUCT_SUCCESS : AnalyticsEvent()
    object ADD_PRODUCT_FAILED : AnalyticsEvent()
    object PRODUCT_IMAGE_UPLOAD_FAILED : AnalyticsEvent()
    object PRODUCT_DETAIL_PRODUCT_DELETED : AnalyticsEvent()
    object FIRST_CREATED_PRODUCT_SHOWN : AnalyticsEvent()
    object FIRST_CREATED_PRODUCT_SHARE_TAPPED : AnalyticsEvent()
    object PRODUCT_CREATED_USING_SHARED_IMAGES : AnalyticsEvent()

    // -- Product Categories
    object PRODUCT_CATEGORIES_LOADED : AnalyticsEvent()
    object PRODUCT_CATEGORIES_LOAD_FAILED : AnalyticsEvent()
    object PRODUCT_CATEGORIES_PULLED_TO_REFRESH : AnalyticsEvent()
    object PRODUCT_CATEGORY_SETTINGS_ADD_BUTTON_TAPPED : AnalyticsEvent()

    // -- Add Product Category
    object PARENT_CATEGORIES_LOADED : AnalyticsEvent()
    object PARENT_CATEGORIES_LOAD_FAILED : AnalyticsEvent()
    object PARENT_CATEGORIES_PULLED_TO_REFRESH : AnalyticsEvent()
    object ADD_PRODUCT_CATEGORY_SAVE_TAPPED : AnalyticsEvent()
    object ADD_PRODUCT_CATEGORY_DELETE_TAPPED : AnalyticsEvent()

    // -- Product Tags
    object PRODUCT_TAGS_LOADED : AnalyticsEvent()
    object PRODUCT_TAGS_LOAD_FAILED : AnalyticsEvent()
    object PRODUCT_TAGS_PULLED_TO_REFRESH : AnalyticsEvent()

    // -- Product reviews
    object PRODUCT_REVIEWS_LOADED : AnalyticsEvent()
    object PRODUCT_REVIEWS_LOAD_FAILED : AnalyticsEvent()
    object PRODUCT_REVIEWS_PULLED_TO_REFRESH : AnalyticsEvent()
    object REVIEW_REPLY_SEND : AnalyticsEvent()
    object REVIEW_REPLY_SEND_SUCCESS : AnalyticsEvent()
    object REVIEW_REPLY_SEND_FAILED : AnalyticsEvent()

    object PRODUCTS_DOWNLOADABLE_FILE : AnalyticsEvent()

    // -- Linked Products
    object LINKED_PRODUCTS : AnalyticsEvent()

    // -- Connected Products (Grouped products, Upsells, Cross-sells)
    object CONNECTED_PRODUCTS_LIST : AnalyticsEvent()

    // -- Product external link
    object PRODUCT_DETAIL_VIEW_EXTERNAL_PRODUCT_LINK_TAPPED : AnalyticsEvent()
    object EXTERNAL_PRODUCT_LINK_SETTINGS_DONE_BUTTON_TAPPED : AnalyticsEvent()

    // -- Product subscriptions
    object PRODUCT_DETAILS_VIEW_SUBSCRIPTION_EXPIRATION_TAPPED : AnalyticsEvent()
    object PRODUCT_DETAILS_VIEW_SUBSCRIPTION_FREE_TRIAL_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_SUBSCRIPTION_EXPIRATION_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_SUBSCRIPTION_FREE_TRIAL_TAPPED : AnalyticsEvent()

    // -- Product attributes
    object PRODUCT_ATTRIBUTE_EDIT_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_ATTRIBUTE_ADD_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_ATTRIBUTE_UPDATED : AnalyticsEvent()
    object PRODUCT_ATTRIBUTE_UPDATE_SUCCESS : AnalyticsEvent()
    object PRODUCT_ATTRIBUTE_UPDATE_FAILED : AnalyticsEvent()
    object PRODUCT_ATTRIBUTE_RENAME_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_ATTRIBUTE_REMOVE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_ATTRIBUTE_OPTIONS_ROW_TAPPED : AnalyticsEvent()

    // -- Product variation
    object PRODUCT_VARIATION_VIEW_VARIATION_DESCRIPTION_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_PRICE_SETTINGS_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_INVENTORY_SETTINGS_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_SHIPPING_SETTINGS_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_VARIATION_DETAIL_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_VARIATION_VISIBILITY_SWITCH_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_IMAGE_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_UPDATE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_UPDATE_SUCCESS : AnalyticsEvent()
    object PRODUCT_VARIATION_UPDATE_ERROR : AnalyticsEvent()
    object PRODUCT_VARIATION_LOADED : AnalyticsEvent()
    object PRODUCT_VARIATION_ADD_FIRST_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_ADD_MORE_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_CREATION_SUCCESS : AnalyticsEvent()
    object PRODUCT_VARIATION_CREATION_FAILED : AnalyticsEvent()
    object PRODUCT_VARIATION_REMOVE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_EDIT_ATTRIBUTE_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_EDIT_ATTRIBUTE_OPTIONS_DONE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_ATTRIBUTE_ADDED_BACK_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_DETAILS_ATTRIBUTES_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_GENERATION_REQUESTED : AnalyticsEvent()
    object PRODUCT_VARIATION_GENERATION_LIMIT_REACHED : AnalyticsEvent()
    object PRODUCT_VARIATION_GENERATION_CONFIRMED : AnalyticsEvent()
    object PRODUCT_VARIATION_GENERATION_SUCCESS : AnalyticsEvent()
    object PRODUCT_VARIATION_GENERATION_FAILURE : AnalyticsEvent()

    // -- Product Add-ons
    object PRODUCT_ADDONS_BETA_FEATURES_SWITCH_TOGGLED : AnalyticsEvent()
    object PRODUCT_ADDONS_ORDER_ADDONS_VIEWED : AnalyticsEvent()
    object PRODUCT_ADDONS_PRODUCT_DETAIL_VIEW_PRODUCT_ADDONS_TAPPED : AnalyticsEvent()
    object PRODUCT_ADDONS_ORDER_DETAIL_VIEW_PRODUCT_ADDONS_TAPPED : AnalyticsEvent()
    object PRODUCT_ADDONS_REFUND_DETAIL_VIEW_PRODUCT_ADDONS_TAPPED : AnalyticsEvent()

    object PRODUCT_DETAIL_ADD_IMAGE_TAPPED : AnalyticsEvent()
    object PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_IMAGE_SETTINGS_ADD_IMAGES_SOURCE_TAPPED : AnalyticsEvent()
    object PRODUCT_IMAGE_SETTINGS_DELETE_IMAGE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_SETTINGS_STATUS_TAPPED : AnalyticsEvent()
    object PRODUCT_SETTINGS_CATALOG_VISIBILITY_TAPPED : AnalyticsEvent()
    object PRODUCT_SETTINGS_SLUG_TAPPED : AnalyticsEvent()
    object PRODUCT_SETTINGS_PURCHASE_NOTE_TAPPED : AnalyticsEvent()
    object PRODUCT_SETTINGS_VISIBILITY_TAPPED : AnalyticsEvent()
    object PRODUCT_SETTINGS_MENU_ORDER_TAPPED : AnalyticsEvent()
    object PRODUCT_SETTINGS_REVIEWS_TOGGLED : AnalyticsEvent()
    object PRODUCT_SETTINGS_VIRTUAL_TOGGLED : AnalyticsEvent()

    // -- Product filters
    object PRODUCT_FILTER_LIST_SHOW_PRODUCTS_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_FILTER_LIST_CLEAR_MENU_BUTTON_TAPPED : AnalyticsEvent()

    // -- Product variations
    object PRODUCT_VARIANTS_PULLED_TO_REFRESH : AnalyticsEvent()
    object PRODUCT_VARIANTS_LOADED : AnalyticsEvent()
    object PRODUCT_VARIANTS_LOAD_ERROR : AnalyticsEvent()
    object PRODUCT_VARIANTS_BULK_UPDATE_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIANTS_BULK_UPDATE_REGULAR_PRICE_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIANTS_BULK_UPDATE_SALE_PRICE_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIANTS_BULK_UPDATE_REGULAR_PRICE_DONE_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIANTS_BULK_UPDATE_SALE_PRICE_DONE_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIANTS_BULK_UPDATE_STOCK_QUANTITY_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIANTS_BULK_UPDATE_STOCK_QUANTITY_DONE_TAPPED : AnalyticsEvent()

    // -- Product images
    object PRODUCT_IMAGE_ADDED : AnalyticsEvent()

    // -- Product stock status
    object PRODUCT_STOCK_STATUSES_UPDATE_DONE_TAPPED : AnalyticsEvent()

    // -- Duplicate product
    object DUPLICATE_PRODUCT_SUCCESS : AnalyticsEvent()
    object DUPLICATE_PRODUCT_FAILED : AnalyticsEvent()
    object PRODUCT_DETAIL_DUPLICATE_BUTTON_TAPPED : AnalyticsEvent()

    // -- Help & Support
    object SUPPORT_HELP_CENTER_VIEWED : AnalyticsEvent(siteless = true)
    object SUPPORT_IDENTITY_SET : AnalyticsEvent(siteless = true)
    object SUPPORT_IDENTITY_FORM_VIEWED : AnalyticsEvent(siteless = true)
    object SUPPORT_APPLICATION_LOG_VIEWED : AnalyticsEvent(siteless = true)
    object SUPPORT_SSR_COPY_BUTTON_TAPPED : AnalyticsEvent()

    // -- Support Request Form
    object SUPPORT_NEW_REQUEST_VIEWED : AnalyticsEvent()
    object SUPPORT_NEW_REQUEST_CREATED : AnalyticsEvent()
    object SUPPORT_NEW_REQUEST_FAILED : AnalyticsEvent()

    // -- Push notifications
    object PUSH_NOTIFICATION_RECEIVED : AnalyticsEvent()
    object PUSH_NOTIFICATION_TAPPED : AnalyticsEvent()
    object NEW_ORDER_PUSH_NOTIFICATION_SOUND : AnalyticsEvent()
    object NEW_ORDER_PUSH_NOTIFICATION_FIX_SHOWN : AnalyticsEvent()
    object NEW_ORDER_PUSH_NOTIFICATION_FIX_TAPPED : AnalyticsEvent()
    object NEW_ORDER_PUSH_NOTIFICATION_FIX_DISMISSED : AnalyticsEvent()

    // -- Notifications List
    object NOTIFICATION_OPEN : AnalyticsEvent()
    object NOTIFICATIONS_LOADED : AnalyticsEvent()
    object NOTIFICATIONS_LOAD_FAILED : AnalyticsEvent()

    // -- Product Review List
    object REVIEWS_LOADED : AnalyticsEvent()
    object REVIEWS_LOAD_FAILED : AnalyticsEvent()
    object REVIEWS_PRODUCTS_LOADED : AnalyticsEvent()
    object REVIEWS_PRODUCTS_LOAD_FAILED : AnalyticsEvent()
    object REVIEWS_MARK_ALL_READ : AnalyticsEvent()
    object REVIEWS_MARK_ALL_READ_SUCCESS : AnalyticsEvent()
    object REVIEWS_MARK_ALL_READ_FAILED : AnalyticsEvent()
    object REVIEWS_LIST_PULLED_TO_REFRESH : AnalyticsEvent()
    object REVIEWS_LIST_MENU_MARK_READ_BUTTON_TAPPED : AnalyticsEvent()

    // -- Product Review Detail
    object REVIEW_OPEN : AnalyticsEvent()
    object REVIEW_LOADED : AnalyticsEvent()
    object REVIEW_LOAD_FAILED : AnalyticsEvent()
    object REVIEW_PRODUCT_LOADED : AnalyticsEvent()
    object REVIEW_PRODUCT_LOAD_FAILED : AnalyticsEvent()
    object REVIEW_MARK_READ : AnalyticsEvent()
    object REVIEW_MARK_READ_SUCCESS : AnalyticsEvent()
    object REVIEW_MARK_READ_FAILED : AnalyticsEvent()
    object REVIEW_ACTION : AnalyticsEvent()
    object REVIEW_ACTION_FAILED : AnalyticsEvent()
    object REVIEW_ACTION_SUCCESS : AnalyticsEvent()
    object REVIEW_DETAIL_APPROVE_BUTTON_TAPPED : AnalyticsEvent()
    object REVIEW_DETAIL_OPEN_EXTERNAL_BUTTON_TAPPED : AnalyticsEvent()
    object REVIEW_DETAIL_SPAM_BUTTON_TAPPED : AnalyticsEvent()
    object REVIEW_DETAIL_TRASH_BUTTON_TAPPED : AnalyticsEvent()

    // -- In-App Feedback
    object APP_FEEDBACK_PROMPT : AnalyticsEvent()
    object APP_FEEDBACK_RATE_APP : AnalyticsEvent()
    object SURVEY_SCREEN : AnalyticsEvent()
    object FEATURE_FEEDBACK_BANNER : AnalyticsEvent()

    // -- Errors
    object JETPACK_TUNNEL_TIMEOUT : AnalyticsEvent()

    // -- Order status changes
    object SET_ORDER_STATUS_DIALOG_APPLY_BUTTON_TAPPED : AnalyticsEvent()

    // -- Application permissions
    object APP_PERMISSION_GRANTED : AnalyticsEvent()
    object APP_PERMISSION_DENIED : AnalyticsEvent()
    object APP_PERMISSION_RATIONALE_ACCEPTED : AnalyticsEvent()
    object APP_PERMISSION_RATIONALE_DISMISSED : AnalyticsEvent()

    // -- Encrypted logging
    object ENCRYPTED_LOGGING_UPLOAD_SUCCESSFUL : AnalyticsEvent()
    object ENCRYPTED_LOGGING_UPLOAD_FAILED : AnalyticsEvent()

    // -- What's new / feature announcements
    object FEATURE_ANNOUNCEMENT_SHOWN : AnalyticsEvent()

    // -- Jetpack CP
    object JETPACK_CP_SITES_FETCHED : AnalyticsEvent()
    object FEATURE_JETPACK_BENEFITS_BANNER : AnalyticsEvent()
    object JETPACK_INSTALL_BUTTON_TAPPED : AnalyticsEvent()
    object JETPACK_INSTALL_SUCCEEDED : AnalyticsEvent()
    object JETPACK_INSTALL_FAILED : AnalyticsEvent()
    object JETPACK_INSTALL_IN_WPADMIN_BUTTON_TAPPED : AnalyticsEvent()
    object JETPACK_INSTALL_CONTACT_SUPPORT_BUTTON_TAPPED : AnalyticsEvent()
    object JETPACK_BENEFITS_LOGIN_BUTTON_TAPPED : AnalyticsEvent()
    object JETPACK_SETUP_CONNECTION_CHECK_COMPLETED : AnalyticsEvent()
    object JETPACK_SETUP_CONNECTION_CHECK_FAILED : AnalyticsEvent()
    object JETPACK_SETUP_LOGIN_FLOW : AnalyticsEvent()
    object JETPACK_SETUP_LOGIN_COMPLETED : AnalyticsEvent()
    object JETPACK_SETUP_FLOW : AnalyticsEvent()
    object JETPACK_SETUP_COMPLETED : AnalyticsEvent()
    object JETPACK_SETUP_SYNCHRONIZATION_COMPLETED : AnalyticsEvent()

    // -- Other
    object UNFULFILLED_ORDERS_LOADED : AnalyticsEvent()
    object TOP_EARNER_PRODUCT_TAPPED : AnalyticsEvent()

    // -- Media picker
    object MEDIA_PICKER_PREVIEW_OPENED : AnalyticsEvent()
    object MEDIA_PICKER_RECENT_MEDIA_SELECTED : AnalyticsEvent()
    object MEDIA_PICKER_OPEN_GIF_LIBRARY : AnalyticsEvent()
    object MEDIA_PICKER_OPEN_DEVICE_LIBRARY : AnalyticsEvent()
    object MEDIA_PICKER_CAPTURE_PHOTO : AnalyticsEvent()
    object MEDIA_PICKER_SEARCH_TRIGGERED : AnalyticsEvent()
    object MEDIA_PICKER_SEARCH_EXPANDED : AnalyticsEvent()
    object MEDIA_PICKER_SEARCH_COLLAPSED : AnalyticsEvent()
    object MEDIA_PICKER_SHOW_PERMISSIONS_SCREEN : AnalyticsEvent()
    object MEDIA_PICKER_ITEM_SELECTED : AnalyticsEvent()
    object MEDIA_PICKER_ITEM_UNSELECTED : AnalyticsEvent()
    object MEDIA_PICKER_SELECTION_CLEARED : AnalyticsEvent()
    object MEDIA_PICKER_OPENED : AnalyticsEvent()
    object MEDIA_PICKER_OPEN_SYSTEM_PICKER : AnalyticsEvent()
    object MEDIA_PICKER_OPEN_WORDPRESS_MEDIA_LIBRARY_PICKER : AnalyticsEvent()

    // -- More Menu (aka Hub Menu)
    object HUB_MENU_SWITCH_STORE_TAPPED : AnalyticsEvent()
    object HUB_MENU_OPTION_TAPPED : AnalyticsEvent()
    object HUB_MENU_SETTINGS_TAPPED : AnalyticsEvent()

    // Shortcuts
    object SHORTCUT_PAYMENTS_TAPPED : AnalyticsEvent()
    object SHORTCUT_ORDERS_ADD_NEW : AnalyticsEvent()

    // Inbox
    object INBOX_NOTES_LOADED : AnalyticsEvent()
    object INBOX_NOTES_LOAD_FAILED : AnalyticsEvent()
    object INBOX_NOTE_ACTION : AnalyticsEvent()

    // Coupons
    object COUPONS_LOADED : AnalyticsEvent()
    object COUPONS_LOAD_FAILED : AnalyticsEvent()
    object COUPONS_LIST_SEARCH_TAPPED : AnalyticsEvent()
    object COUPON_DETAILS : AnalyticsEvent()
    object COUPON_UPDATE_INITIATED : AnalyticsEvent()
    object COUPON_UPDATE_SUCCESS : AnalyticsEvent()
    object COUPON_UPDATE_FAILED : AnalyticsEvent()
    object COUPON_DELETE_SUCCESS : AnalyticsEvent()
    object COUPON_DELETE_FAILED : AnalyticsEvent()
    object COUPON_CREATION_SUCCESS : AnalyticsEvent()
    object COUPON_CREATION_FAILED : AnalyticsEvent()
    object COUPON_CREATION_INITIATED : AnalyticsEvent()

    // Onboarding
    object LOGIN_ONBOARDING_SHOWN : AnalyticsEvent()
    object LOGIN_ONBOARDING_NEXT_BUTTON_TAPPED : AnalyticsEvent()
    object LOGIN_ONBOARDING_SKIP_BUTTON_TAPPED : AnalyticsEvent()

    // Woo Installation
    object LOGIN_WOOCOMMERCE_SETUP_BUTTON_TAPPED : AnalyticsEvent()
    object LOGIN_WOOCOMMERCE_SETUP_DISMISSED : AnalyticsEvent()
    object LOGIN_WOOCOMMERCE_SETUP_COMPLETED : AnalyticsEvent()

    // Login help scheduled notifications
    object LOCAL_NOTIFICATION_SCHEDULED : AnalyticsEvent()
    object LOCAL_NOTIFICATION_DISPLAYED : AnalyticsEvent()
    object LOCAL_NOTIFICATION_TAPPED : AnalyticsEvent()
    object LOCAL_NOTIFICATION_DISMISSED : AnalyticsEvent()

    // Widgets
    object WIDGET_TAPPED : AnalyticsEvent()

    // App links
    object UNIVERSAL_LINK_OPENED : AnalyticsEvent()
    object UNIVERSAL_LINK_FAILED : AnalyticsEvent()

    // Analytics Hub
    object ANALYTICS_HUB_WAITING_TIME_LOADED : AnalyticsEvent()

    // Domain change
    object CUSTOM_DOMAINS_STEP : AnalyticsEvent()
    object DOMAIN_CONTACT_INFO_VALIDATION_FAILED : AnalyticsEvent()
    object CUSTOM_DOMAIN_PURCHASE_SUCCESS : AnalyticsEvent()
    object CUSTOM_DOMAIN_PURCHASE_FAILED : AnalyticsEvent()

    // Application passwords login
    object APPLICATION_PASSWORDS_NEW_PASSWORD_CREATED : AnalyticsEvent()
    object APPLICATION_PASSWORDS_GENERATION_FAILED : AnalyticsEvent()
    object APPLICATION_PASSWORDS_AUTHORIZATION_WEB_VIEW_SHOWN : AnalyticsEvent()
    object APPLICATION_PASSWORDS_AUTHORIZATION_REJECTED : AnalyticsEvent()
    object APPLICATION_PASSWORDS_AUTHORIZATION_APPROVED : AnalyticsEvent()
    object APPLICATION_PASSWORDS_AUTHORIZATION_URL_NOT_AVAILABLE : AnalyticsEvent()
    object LOGIN_SITE_CREDENTIALS_INVALID_LOGIN_PAGE_DETECTED : AnalyticsEvent()
    object LOGIN_SITE_CREDENTIALS_APP_PASSWORD_EXPLANATION_DISMISSED : AnalyticsEvent()
    object LOGIN_SITE_CREDENTIALS_APP_PASSWORD_EXPLANATION_CONTACT_SUPPORT_TAPPED : AnalyticsEvent()
    object LOGIN_SITE_CREDENTIALS_APP_PASSWORD_EXPLANATION_CONTINUE_BUTTON_TAPPED : AnalyticsEvent()
    object LOGIN_SITE_CREDENTIALS_APP_PASSWORD_LOGIN_EXIT_CONFIRMATION : AnalyticsEvent()
    object LOGIN_SITE_CREDENTIALS_APP_PASSWORD_LOGIN_DISMISSED : AnalyticsEvent()

    // Free Trial
    object FREE_TRIAL_UPGRADE_NOW_TAPPED : AnalyticsEvent()
    object PLAN_UPGRADE_SUCCESS : AnalyticsEvent()
    object PLAN_UPGRADE_ABANDONED : AnalyticsEvent()
    object UPGRADES_REPORT_SUBSCRIPTION_ISSUE_TAPPED : AnalyticsEvent()

    // Store onboarding
    object STORE_ONBOARDING_SHOWN : AnalyticsEvent()
    object STORE_ONBOARDING_TASK_TAPPED : AnalyticsEvent()
    object STORE_ONBOARDING_TASK_COMPLETED : AnalyticsEvent()
    object STORE_ONBOARDING_COMPLETED : AnalyticsEvent()
    object STORE_ONBOARDING_WCPAY_BEGIN_SETUP_TAPPED : AnalyticsEvent()
    object STORE_ONBOARDING_WCPAY_TERMS_CONTINUE_TAPPED : AnalyticsEvent()

    // Quantity rules (Min/Max extension)
    object PRODUCT_DETAIL_VIEW_QUANTITY_RULES_TAPPED : AnalyticsEvent()
    object PRODUCT_VARIATION_VIEW_QUANTITY_RULES_TAPPED : AnalyticsEvent()

    // Bundled products
    object PRODUCT_DETAIL_VIEW_BUNDLED_PRODUCTS_TAPPED : AnalyticsEvent()

    // Composite Products
    object PRODUCT_DETAILS_VIEW_COMPONENTS_TAPPED : AnalyticsEvent()

    // Account
    object CLOSE_ACCOUNT_TAPPED : AnalyticsEvent()
    object CLOSE_ACCOUNT_SUCCESS : AnalyticsEvent()
    object CLOSE_ACCOUNT_FAILED : AnalyticsEvent()

    // EU Shipping Notice
    object EU_SHIPPING_NOTICE_SHOWN : AnalyticsEvent()
    object EU_SHIPPING_NOTICE_DISMISSED : AnalyticsEvent()
    object EU_SHIPPING_NOTICE_LEARN_MORE_TAPPED : AnalyticsEvent()

    // Privacy Banner
    object PRIVACY_CHOICES_BANNER_PRESENTED : AnalyticsEvent()
    object PRIVACY_CHOICES_BANNER_SETTINGS_BUTTON_TAPPED : AnalyticsEvent()
    object PRIVACY_CHOICES_BANNER_SAVE_BUTTON_TAPPED : AnalyticsEvent()

    // AI Features
    object PRODUCT_SHARING_AI_DISPLAYED : AnalyticsEvent()
    object PRODUCT_SHARING_AI_GENERATE_TAPPED : AnalyticsEvent()
    object PRODUCT_SHARING_AI_SHARE_TAPPED : AnalyticsEvent()
    object PRODUCT_SHARING_AI_DISMISSED : AnalyticsEvent()
    object PRODUCT_SHARING_AI_MESSAGE_GENERATED : AnalyticsEvent()
    object PRODUCT_SHARING_AI_MESSAGE_GENERATION_FAILED : AnalyticsEvent()

    object PRODUCT_DESCRIPTION_AI_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_DESCRIPTION_AI_GENERATE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_DESCRIPTION_AI_APPLY_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_DESCRIPTION_AI_COPY_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_DESCRIPTION_AI_GENERATION_SUCCESS : AnalyticsEvent()
    object PRODUCT_DESCRIPTION_AI_GENERATION_FAILED : AnalyticsEvent()
    object PRODUCT_AI_FEEDBACK : AnalyticsEvent()

    object PRODUCT_NAME_AI_ENTRY_POINT_TAPPED : AnalyticsEvent()
    object PRODUCT_NAME_AI_GENERATE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_NAME_AI_COPY_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_NAME_AI_APPLY_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_NAME_AI_PACKAGE_IMAGE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_NAME_AI_GENERATION_SUCCESS : AnalyticsEvent()
    object PRODUCT_NAME_AI_GENERATION_FAILED : AnalyticsEvent()

    object PRODUCT_CREATION_AI_ENTRY_POINT_DISPLAYED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_ENTRY_POINT_TAPPED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_PRODUCT_NAME_CONTINUE_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_TONE_SELECTED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_GENERATE_DETAILS_TAPPED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_GENERATE_PRODUCT_DETAILS_SUCCESS : AnalyticsEvent()
    object PRODUCT_CREATION_AI_GENERATE_PRODUCT_DETAILS_FAILED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_SAVE_AS_DRAFT_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_SAVE_AS_DRAFT_SUCCESS : AnalyticsEvent()
    object PRODUCT_CREATION_AI_SAVE_AS_DRAFT_FAILED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_SURVEY_CONFIRMATION_VIEW_DISPLAYED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_SURVEY_START_SURVEY_BUTTON_TAPPED : AnalyticsEvent()
    object PRODUCT_CREATION_AI_SURVEY_SKIP_BUTTON_TAPPED : AnalyticsEvent()

    object ADD_PRODUCT_FROM_IMAGE_DISPLAYED : AnalyticsEvent()
    object ADD_PRODUCT_FROM_IMAGE_SCAN_COMPLETED : AnalyticsEvent()
    object ADD_PRODUCT_FROM_IMAGE_SCAN_FAILED : AnalyticsEvent()
    object ADD_PRODUCT_FROM_IMAGE_DETAILS_GENERATED : AnalyticsEvent()
    object ADD_PRODUCT_FROM_IMAGE_DETAIL_GENERATION_FAILED : AnalyticsEvent()
    object ADD_PRODUCT_FROM_IMAGE_CONTINUE_BUTTON_TAPPED : AnalyticsEvent()
    object ADD_PRODUCT_FROM_IMAGE_CHANGE_PHOTO_BUTTON_TAPPED : AnalyticsEvent()
    object ADD_PRODUCT_FROM_IMAGE_REGENERATE_BUTTON_TAPPED : AnalyticsEvent()

    object AI_IDENTIFY_LANGUAGE_SUCCESS : AnalyticsEvent()
    object AI_IDENTIFY_LANGUAGE_FAILED : AnalyticsEvent()

    object ORDER_THANK_YOU_NOTE_SHOWN : AnalyticsEvent()
    object ORDER_THANK_YOU_NOTE_GENERATION_SUCCESS : AnalyticsEvent()
    object ORDER_THANK_YOU_NOTE_GENERATION_FAILED : AnalyticsEvent()
    object ORDER_THANK_YOU_NOTE_REGENERATE_TAPPED : AnalyticsEvent()
    object ORDER_THANK_YOU_NOTE_SHARE_TAPPED : AnalyticsEvent()

    // Blaze
    object BLAZE_ENTRY_POINT_DISPLAYED : AnalyticsEvent()
    object BLAZE_ENTRY_POINT_TAPPED : AnalyticsEvent()
    object BLAZE_FLOW_STARTED : AnalyticsEvent()
    object BLAZE_FLOW_CANCELED : AnalyticsEvent()
    object BLAZE_FLOW_COMPLETED : AnalyticsEvent()
    object BLAZE_FLOW_ERROR : AnalyticsEvent()
    object BLAZE_CAMPAIGN_DETAIL_SELECTED : AnalyticsEvent()
    object BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED : AnalyticsEvent()
    object BLAZE_INTRO_DISPLAYED : AnalyticsEvent()
    object BLAZE_INTRO_LEARN_MORE_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_FORM_DISPLAYED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_AD_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_CONFIRM_DETAILS_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_PAYMENT_SUBMIT_CAMPAIGN_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_ADD_PAYMENT_METHOD_WEB_VIEW_DISPLAYED : AnalyticsEvent()
    object BLAZE_CREATION_ADD_PAYMENT_METHOD_SUCCESS : AnalyticsEvent()
    object BLAZE_CAMPAIGN_CREATION_SUCCESS : AnalyticsEvent()
    object BLAZE_CAMPAIGN_CREATION_FAILED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_AD_AI_SUGGESTION_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_AD_SAVE_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_BUDGET_SAVE_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_BUDGET_SET_DURATION_APPLIED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_LANGUAGE_SAVE_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_DEVICE_SAVE_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_INTEREST_SAVE_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_LOCATION_SAVE_TAPPED : AnalyticsEvent()
    object BLAZE_CREATION_EDIT_DESTINATION_SAVE_TAPPED : AnalyticsEvent()

    // Hazmat Shipping Declaration
    object CONTAINS_HAZMAT_CHECKED : AnalyticsEvent()
    object HAZMAT_CATEGORY_SELECTOR_OPENED : AnalyticsEvent()
    object HAZMAT_CATEGORY_SELECTED : AnalyticsEvent()

    // -- Bundles
    object ORDER_FORM_BUNDLE_PRODUCT_CONFIGURE_CTA_SHOWN : AnalyticsEvent()
    object ORDER_FORM_BUNDLE_PRODUCT_CONFIGURE_CTA_TAPPED : AnalyticsEvent()
    object ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_CHANGED : AnalyticsEvent()
    object ORDER_FORM_BUNDLE_PRODUCT_CONFIGURATION_SAVE_TAPPED : AnalyticsEvent()

    // Theme picker
    object THEME_PICKER_SCREEN_DISPLAYED : AnalyticsEvent()
    object THEME_PICKER_THEME_SELECTED : AnalyticsEvent()
    object THEME_PREVIEW_SCREEN_DISPLAYED : AnalyticsEvent()
    object THEME_PREVIEW_LAYOUT_SELECTED : AnalyticsEvent()
    object THEME_PREVIEW_PAGE_SELECTED : AnalyticsEvent()
    object THEME_PREVIEW_START_WITH_THEME_BUTTON_TAPPED : AnalyticsEvent()
    object THEME_INSTALLATION_COMPLETED : AnalyticsEvent()
    object THEME_INSTALLATION_FAILED : AnalyticsEvent()

    // Connectivity Tool
    object CONNECTIVITY_TOOL_REQUEST_RESPONSE : AnalyticsEvent()
    object CONNECTIVITY_TOOL_READ_MORE_TAPPED : AnalyticsEvent()
    object CONNECTIVITY_TOOL_CONTACT_SUPPORT_TAPPED : AnalyticsEvent()

    // Woo Wear App
    object WATCH_STORE_DATA_REQUESTED : AnalyticsEvent()
    object WATCH_STORE_DATA_SUCCEEDED : AnalyticsEvent()
    object WATCH_STORE_DATA_FAILED : AnalyticsEvent()
    object WATCH_STATS_DATA_REQUESTED : AnalyticsEvent()
    object WATCH_STATS_DATA_SUCCEEDED : AnalyticsEvent()
    object WATCH_STATS_DATA_FAILED : AnalyticsEvent()
    object WATCH_ORDERS_LIST_DATA_REQUESTED : AnalyticsEvent()
    object WATCH_ORDERS_LIST_DATA_SUCCEEDED : AnalyticsEvent()
    object WATCH_ORDERS_LIST_DATA_FAILED : AnalyticsEvent()
    object WATCH_ORDER_DETAILS_DATA_REQUESTED : AnalyticsEvent()
    object WATCH_ORDER_DETAILS_DATA_SUCCEEDED : AnalyticsEvent()
    object WATCH_ORDER_DETAILS_DATA_FAILED : AnalyticsEvent()
    object WATCH_APP_OPENED : AnalyticsEvent()
    object WATCH_ORDERS_LIST_OPENED : AnalyticsEvent()
    object WATCH_ORDER_DETAILS_OPENED : AnalyticsEvent()
}

@Suppress("CyclomaticComplexMethod")
fun WearAnalyticsEvent.toAnalyticsEvent(): AnalyticsEvent? {
    return when (this) {
        WearAnalyticsEvent.WATCH_STORE_DATA_REQUESTED -> AnalyticsEvent.WATCH_STORE_DATA_REQUESTED
        WearAnalyticsEvent.WATCH_STORE_DATA_SUCCEEDED -> AnalyticsEvent.WATCH_STORE_DATA_SUCCEEDED
        WearAnalyticsEvent.WATCH_STORE_DATA_FAILED -> AnalyticsEvent.WATCH_STORE_DATA_FAILED
        WearAnalyticsEvent.WATCH_STATS_DATA_REQUESTED -> AnalyticsEvent.WATCH_STATS_DATA_REQUESTED
        WearAnalyticsEvent.WATCH_STATS_DATA_SUCCEEDED -> AnalyticsEvent.WATCH_STATS_DATA_SUCCEEDED
        WearAnalyticsEvent.WATCH_STATS_DATA_FAILED -> AnalyticsEvent.WATCH_STATS_DATA_FAILED
        WearAnalyticsEvent.WATCH_ORDERS_LIST_DATA_REQUESTED -> AnalyticsEvent.WATCH_ORDERS_LIST_DATA_REQUESTED
        WearAnalyticsEvent.WATCH_ORDERS_LIST_DATA_SUCCEEDED -> AnalyticsEvent.WATCH_ORDERS_LIST_DATA_SUCCEEDED
        WearAnalyticsEvent.WATCH_ORDERS_LIST_DATA_FAILED -> AnalyticsEvent.WATCH_ORDERS_LIST_DATA_FAILED
        WearAnalyticsEvent.WATCH_ORDER_DETAILS_DATA_REQUESTED -> AnalyticsEvent.WATCH_ORDER_DETAILS_DATA_REQUESTED
        WearAnalyticsEvent.WATCH_ORDER_DETAILS_DATA_SUCCEEDED -> AnalyticsEvent.WATCH_ORDER_DETAILS_DATA_SUCCEEDED
        WearAnalyticsEvent.WATCH_ORDER_DETAILS_DATA_FAILED -> AnalyticsEvent.WATCH_ORDER_DETAILS_DATA_FAILED
        WearAnalyticsEvent.WATCH_APP_OPENED -> AnalyticsEvent.WATCH_APP_OPENED
        WearAnalyticsEvent.WATCH_ORDERS_LIST_OPENED -> AnalyticsEvent.WATCH_ORDERS_LIST_OPENED
        WearAnalyticsEvent.WATCH_ORDER_DETAILS_OPENED -> AnalyticsEvent.WATCH_ORDER_DETAILS_OPENED
        else -> null
    }
}
