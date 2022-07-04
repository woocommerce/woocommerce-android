@file:Suppress("ComplexMethod")

package com.woocommerce.android.extensions

import androidx.core.text.HtmlCompat
import com.woocommerce.android.util.WooLog
import org.apache.commons.io.FileUtils.byteCountToDisplaySize
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.fluxc.model.WCSSRModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

const val MISSING_VALUE = "Info not found"
const val HEADING_SSR = "### System Status Report generated via the WooCommerce Android app ### \n"
const val HEADING_WP_ENVIRONMENT = "WordPress Environment"
const val HEADING_SERVER_ENVIRONMENT = "Server Environment"
const val HEADING_DATABASE = "Database"
const val HEADING_SECURITY = "Security"
const val HEADING_ACTIVE_PLUGINS = "Active Plugins"
const val HEADING_SETTINGS = "Settings"
const val HEADING_PAGES = "WC Pages"
const val HEADING_THEME = "Theme"
const val HEADING_TEMPLATES = "Templates"
const val HEADING_STATUS_REPORT_INFORMATION = "Status report information"
const val CHECK = "✔"
const val NO_CHECK = "–"
const val PAGE_NOT_SET = "X Page not set"
const val DATABASE_SIZE_UNIT = "MB"

fun WCSSRModel.formatResult(): String {
    val sb = StringBuilder()

    sb.append(HEADING_SSR)

    // Environment
    environment?.let { it ->
        try {
            sb.append(formatEnvironmentData(JSONObject(it)))
        } catch (e: JSONException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    database?.let {
        try {
            sb.append(formatDatabaseData(JSONObject(it)))
        } catch (e: JSONException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    security?.let {
        try {
            sb.append(formatSecurityData(JSONObject(it)))
        } catch (e: JSONException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    activePlugins?.let {
        try {
            sb.append(formatActivePluginsData(JSONArray(it)))
        } catch (e: JSONException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    settings?.let {
        try {
            sb.append(formatSettingsData(JSONObject(it)))
        } catch (e: JSONException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    pages?.let {
        try {
            sb.append(formatPagesData(JSONArray(it)))
        } catch (e: JSONException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    theme?.let {
        try {
            sb.append(formatThemeData(JSONObject(it)))
        } catch (e: JSONException) {
            WooLog.e(WooLog.T.UTILS, e)
        }
    }

    sb.append(formatStatusReportInformationData())
    return sb.toString()
}

private fun formatEnvironmentData(data: JSONObject): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_WP_ENVIRONMENT))
        .append("WordPress Address (URL): ${data.optString("home_url", MISSING_VALUE)}\n")
        .append("Site Address (URL): ${data.optString("site_url", MISSING_VALUE)}\n")
        .append("WC Version: ${data.optString("version", MISSING_VALUE)}\n")
        .append("Log Directory Writable: ${checkIfTrue(data.optBoolean("log_directory_writable", false))}\n")
        .append("WP Version: ${data.optString("wp_version", MISSING_VALUE)}\n")
        .append("WP Multisite: ${checkIfTrue(data.optBoolean("wp_multisite", false))}\n")

    val memoryLimit = data.optString("wp_memory_limit", MISSING_VALUE)
    if (memoryLimit != MISSING_VALUE) {
        sb.append("WP Memory Limit: ${byteCountToDisplaySize(memoryLimit.toLong())}\n")
    }

    sb.append("WP Debug Mode: ${checkIfTrue(data.optBoolean("wp_debug_mode", false))}\n")
        .append("WP Cron: ${checkIfTrue(data.optBoolean("wp_cron", false))}\n")
        .append("Language: ${data.optString("language", MISSING_VALUE)}\n")
        .append("External object cache: ${checkIfTrue(data.optBoolean("external_object_cache", false))}\n")
        .append(formattedHeading(HEADING_SERVER_ENVIRONMENT))
        .append("Server Info: ${data.optString("server_info", MISSING_VALUE)}\n")
        .append("PHP Version: ${data.optString("php_version", MISSING_VALUE)}\n")

    val postMaxSize = data.optString("php_post_max_size", MISSING_VALUE)
    if (postMaxSize != MISSING_VALUE) {
        sb.append("PHP Post Max Size: ${byteCountToDisplaySize(postMaxSize.toLong())}\n")
    }

    sb.append("PHP Time Limit: ${data.optString("php_max_execution_time", MISSING_VALUE)} s\n")
        .append("PHP Max input Vars: ${data.optString("php_max_input_vars", MISSING_VALUE)}\n")
        .append("cURL Version: ${data.optString("curl_version", MISSING_VALUE)}\n")
        .append("Suhosin installed: ${checkIfTrue(data.optBoolean("suhosin_installed", false))}\n")
        .append("MySQL Version: ${data.optString("mysql_version_string", MISSING_VALUE)}\n")

    val maxUploadSize = data.optString("max_upload_size", MISSING_VALUE)
    if (maxUploadSize != MISSING_VALUE) {
        sb.append("PHP Post Max Size: ${byteCountToDisplaySize(maxUploadSize.toLong())}\n")
    }

    sb.append("Default Timezone: ${data.optString("default_timezone", MISSING_VALUE)}\n")
        .append("fsockopen/cURL: ${checkIfTrue(data.optBoolean("fsockopen_or_curl_enabled", false))}\n")
        .append("SoapClient: ${checkIfTrue(data.optBoolean("soapclient_enabled", false))}\n")
        .append("DOMDocument: ${checkIfTrue(data.optBoolean("domdocument_enabled", false))}\n")
        .append("GZip: ${checkIfTrue(data.optBoolean("gzip_enabled", false))}\n")
        .append("Multibye String: ${checkIfTrue(data.optBoolean("mbstring_enabled", false))}\n")
        .append("Remote Post: ${checkIfTrue(data.optBoolean("remote_post_successful", false))}\n")
        .append("Remote Get: ${checkIfTrue(data.optBoolean("remote_get_successful", false))}\n")

    return sb.toString()
}

private fun formatDatabaseData(data: JSONObject): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_DATABASE))
        .append("WC Database Version: ${data.optString("wc_database_version", MISSING_VALUE)}\n")
        .append("WC Database Prefix: ${data.optString("database_prefix", MISSING_VALUE)}\n")

    val sizeData = data.optJSONObject("database_size")
    sizeData?.let {
        val dataSize = it.optDouble("data", 0.0)
        val indexSize = it.optDouble("index", 0.0)

        val total = if (dataSize == 0.0 && indexSize == 0.0) {
            MISSING_VALUE
        } else {
            roundDoubleDecimal(dataSize + indexSize)
        }
        val size = if (dataSize == 0.0) { MISSING_VALUE } else { roundDoubleDecimal(dataSize).toString() }
        val index = if (indexSize == 0.0) { MISSING_VALUE } else { roundDoubleDecimal(indexSize).toString() }

        sb.append("Total Database Size: $total $DATABASE_SIZE_UNIT\n")
            .append("Database Data Size: $size $DATABASE_SIZE_UNIT\n")
            .append("Database Index Size: $index $DATABASE_SIZE_UNIT\n")
    }

    val tablesData = data.optJSONObject("database_tables")
    tablesData?.let { it ->
        sb.append(parseFormatTablesData(it, "woocommerce"))
            .append(parseFormatTablesData(it, "other"))
    }

    return sb.toString()
}

private fun formatSecurityData(data: JSONObject): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_SECURITY))
        .append("Secure Connection (HTTPS): ${checkIfTrue(data.optBoolean("secure_connection", false))}\n")
        .append("Hide errors from visitors: ${checkIfTrue(data.optBoolean("hide_errors", false))}\n")

    return sb.toString()
}

private fun formatActivePluginsData(data: JSONArray): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_ACTIVE_PLUGINS))

    for (i in 0 until data.length()) {
        val plugin = data.optJSONObject(i)
        plugin?.let {
            sb.append(plugin.optString("name", MISSING_VALUE))
                .append(": by " + plugin.optString("author_name", MISSING_VALUE))

            val currentVersion = plugin.optString("version", MISSING_VALUE)
            val latestVersion = plugin.optString("version_latest", MISSING_VALUE)
            sb.append(" - $currentVersion")
            if (currentVersion != MISSING_VALUE && latestVersion != MISSING_VALUE && currentVersion != latestVersion) {
                sb.append(" (update to version $latestVersion available)")
            }
            sb.append("\n")
        }
    }
    return sb.toString()
}

private fun formatSettingsData(data: JSONObject): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_SETTINGS))
        .append("API Enabled: ${checkIfTrue(data.optBoolean("api_enabled", false))}\n")
        .append("Force SSL: ${checkIfTrue(data.optBoolean("force_ssl", false))}\n")
        // Currency format: currency_name(currency_symbol)
        // Correct value example: USD($)
        // Missing value example: Info not found(Info not found)
        .append("Currency: ${data.optString("currency", MISSING_VALUE)} (")

    val currencySymbolHTML = data.optString("currency_symbol", MISSING_VALUE)
    sb.append(
        if (currencySymbolHTML == MISSING_VALUE) {
            MISSING_VALUE
        } else {
            HtmlCompat.fromHtml(currencySymbolHTML, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }
    )
        .append(")\n")
        .append("Currency Position: ${data.optString("currency_position", MISSING_VALUE)}\n")
        .append("Thousand Separator: ${data.optString("thousand_separator", MISSING_VALUE)}\n")
        .append("Decimal Separator: ${data.optString("decimal_separator", MISSING_VALUE)}\n")
        .append("Number of Decimals: ${data.optString("number_of_decimals", MISSING_VALUE)}\n")

    val productTypesTaxonomy = data.optJSONObject("taxonomies")
    productTypesTaxonomy?.let {
        sb.append(parseFormatTaxonomy(it, "Product Types") + "\n")
    }

    val productVisibilityTaxonomy = data.optJSONObject("product_visibility_terms")
    productVisibilityTaxonomy?.let {
        sb.append(parseFormatTaxonomy(it, "Product Visibility") + "\n")
    }

    sb.append("Connected to WooCommerce.com: ${checkIfTrue(data.optBoolean("woocommerce_com_connected", false))}\n")

    return sb.toString()
}

private fun formatPagesData(data: JSONArray): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_PAGES))

    for (i in 0 until data.length()) {
        val page = data.optJSONObject(i)
        page?.let {
            sb.append("${it["page_name"]}: ")
            val pageSet = it.optBoolean("page_set", false)
            sb.append(
                if (pageSet) {
                    " Page ID #${it["page_id"]}"
                } else {
                    PAGE_NOT_SET
                }
            )
                .append("\n")
        }
    }

    return sb.toString()
}

private fun formatThemeData(data: JSONObject): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_THEME))
        .append("Name: ${data.optString("name", MISSING_VALUE)}\n")

    val currentVersion = data.optString("version", MISSING_VALUE)
    val latestVersion = data.optString("version_latest", MISSING_VALUE)

    sb.append("Version: $currentVersion")
    val latestVersionExists = latestVersion != MISSING_VALUE && latestVersion != "0"
    if (latestVersionExists &&
        currentVersion != MISSING_VALUE &&
        currentVersion != latestVersion
    ) {
        sb.append(" (update to version $latestVersion available)")
    }
    sb.append("\n")
        .append("Author URL: ${data.optString("author_url", MISSING_VALUE)}\n")

    val isChildTheme = data.optBoolean("is_child_theme", false)
    if (isChildTheme) {
        sb.append("Child Theme: ${CHECK}\n")
            .append("Parent Theme Name: ${data.optString("parent_name", MISSING_VALUE)}\n")

        val parentCurrentVersion = data.optString("parent_version", MISSING_VALUE)
        val parentLatestVersion = data.optString("parent_version_latest", MISSING_VALUE)
        sb.append("Parent Theme Version: $currentVersion")
        val parentLatestVersionExists = parentLatestVersion != MISSING_VALUE && parentLatestVersion != "0"
        if (parentLatestVersionExists &&
            parentCurrentVersion != MISSING_VALUE &&
            parentCurrentVersion != parentLatestVersion
        ) {
            sb.append(" (update to version $latestVersion available)")
        }
        sb.append("\n")
            .append("Parent Theme Author URL: ${data.optString("parent_author_url", MISSING_VALUE)}\n")
    }

    sb.append("WooCommerce support: ${checkIfTrue(data.optBoolean("has_woocommerce_support", false))}\n")
        .append("WooCommerce files: ${checkIfTrue(data.optBoolean("has_woocommerce_file", false))}\n")
        .append("Outdated templates: ${checkIfTrue(data.optBoolean("has_outdated_templates", false))}\n")

    val templates = data.optJSONArray("overrides")
    templates?.let { sb.append(formatTemplatesData(it)) }

    return sb.toString()
}

private fun formatTemplatesData(data: JSONArray): String {
    val sb = StringBuilder()
    if (data.length() != 0) {
        sb.append(formattedHeading(HEADING_TEMPLATES))
            .append("Overrides: ")
        for (i in 0 until data.length()) {
            val template = data.optJSONObject(i)
            template?.let { item ->
                sb.append("${item.optString("file", MISSING_VALUE)}\n")
            }
        }
    }
    return sb.toString()
}

private fun formatStatusReportInformationData(): String {
    val sb = StringBuilder()
    sb.append(formattedHeading(HEADING_STATUS_REPORT_INFORMATION))

    val today = Calendar.getInstance()
    val ssrCreationTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.getDefault()).format(today.time)
    sb.append("Generated at: $ssrCreationTime")

    return sb.toString()
}

private fun formattedHeading(text: String): String {
    return "\n ### $text ### \n\n"
}

private fun checkIfTrue(check: Boolean) = if (check) CHECK else NO_CHECK

private fun parseFormatTablesData(tables: JSONObject, tableType: String): String {
    val sb = StringBuilder()
    val tablesByType = tables.optJSONObject(tableType)

    tablesByType?.let { it ->
        it.keys().forEach { key ->
            val tableData = it.optJSONObject(key)
            tableData?.let { data ->
                sb.append("$key: " + parseFormatSingleTableData(data))
            }
        }
    }
    return sb.toString()
}

// Example input: {"data":"0.05","index":"0.02","engine":"InnoDB"}
// Expected output: "Data: 0.05MB + Index: 0.02MB + Engine InnoDB"
private fun parseFormatSingleTableData(table: JSONObject): String {
    val data = table.optString("data", MISSING_VALUE)
    val index = table.optString("index", MISSING_VALUE)
    val engine = table.optString("engine", MISSING_VALUE)

    return "Data: ${data}MB + Index: ${index}MB + Engine $engine\n"
}

private fun parseFormatTaxonomy(taxonomies: JSONObject, taxonomyType: String): String {
    val sb = StringBuilder()
    sb.append("Taxonomies: $taxonomyType: \n")

    taxonomies.keys().forEach { key ->
        val value = taxonomies.get(key)
        sb.append("$key ($value)\n")
    }
    return sb.toString()
}

private fun roundDoubleDecimal(number: Double, scale: Int = 2, mode: RoundingMode = RoundingMode.UP): BigDecimal {
    return number.toBigDecimal().setScale(scale, mode)
}
