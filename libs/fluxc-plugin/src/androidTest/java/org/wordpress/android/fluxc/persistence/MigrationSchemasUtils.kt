package org.wordpress.android.fluxc.persistence

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.json.JSONObject
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
object MigrationSchemasUtils {
    private const val SCHEMAS_FOLDER = "org.wordpress.android.fluxc.persistence.WCAndroidDatabase"
    private const val HASH_TAG_NAME = "identityHash"
    private const val DATABASE_TAG_NAME = "database"

    fun getDBHashKey(dbVersion: Int): String = "DB_V${dbVersion}_HASH"

    fun getIdentityHash(dbVersion: Int, context: Context): String {
        val fileName = "$SCHEMAS_FOLDER/$dbVersion.json"

        // Read JSON file from assets
        val reader = context.assets.open(fileName).reader()
        val jsonString = reader.readText()
        reader.close()

        // Parse JSON string
        val jsonObject = JSONObject(jsonString)

        // Return hash
        return jsonObject.getJSONObject(DATABASE_TAG_NAME).getString(HASH_TAG_NAME)
    }

    val DB_HASHES = mapOf(
        getDBHashKey(3) to "f93440a02cd238b1abd2ab0bf6095530",
        getDBHashKey(4) to "5bf75b420b7268be1d990e841485b2ce",
        getDBHashKey(5) to "34052cc9296465672d13b43d276b7ec2",
        getDBHashKey(6) to "b24e3b32c006d07264eb0cc3427b0138",
        getDBHashKey(7) to "38081df5a9dc4a02c48a2f92c7987d00",
        getDBHashKey(8) to "f791ffbacf072bd61529514337a801e1",
        getDBHashKey(9) to "34ba60b1fb6c42ae54a6c4e216b3cdae",
        getDBHashKey(10) to "6c40c25602a91c9a88bf0691cc0f8d56",
        getDBHashKey(11) to "39ae37638a6cb9fc908153b18ee9ea9f",
        getDBHashKey(12) to "0cca8009f3a179d0f911bcc61401c7f1",
        getDBHashKey(13) to "0f27a46a235e2993401c68a3f71edcef",
        getDBHashKey(14) to "29bd4b7bb5f85a1f50b9fe9a6b2aa8eb",
        getDBHashKey(15) to "c1b4a5003f85e61f9427f4fa0edea3e1",
        getDBHashKey(16) to "152b7abad5ab1a500dc34a02e1e5a389",
        getDBHashKey(17) to "3b246a05145d072bab5f9cbae76ed922",
        getDBHashKey(18) to "d430524ce447d0c639ed7a47a35a0827",
        getDBHashKey(19) to "20424496bd0d2d7f0ec904dac725bb83",
        getDBHashKey(20) to "77e57ef8e1d85a20baf0805ee3c18c12",
        getDBHashKey(21) to "15d66924533da0996f3ad884bbfdcfab",
        getDBHashKey(22) to "a08d1da9359217298fa9560aa8f69333",
        getDBHashKey(23) to "a08d1da9359217298fa9560aa8f69333",
        getDBHashKey(24) to "f01bc4956b3c08f4ae847bff0626776b",
        getDBHashKey(25) to "6d6c9173ad3574359b4e0f4788c9541e",
        getDBHashKey(26) to "53e39092d40d7d55ad7d717d69b86547",
        getDBHashKey(27) to "f2c454ea3a1d74cf4b7ec999502708bc",
        getDBHashKey(28) to "7c63d469a9a3b1302f2d2db3e4c627ac",
        getDBHashKey(29) to "7d96824894dd52540b779abb309e98c9",
        getDBHashKey(30) to "1e3ad3f4fb750ae397a8fee0c724ed14",
        getDBHashKey(31) to "1e3ad3f4fb750ae397a8fee0c724ed14",
        getDBHashKey(32) to "892bffac2c3dc56265f35a4375208c54",
        getDBHashKey(33) to "43fd0900518251fbd271b5ffe7a0d89f",
        getDBHashKey(34) to "878b99bf2a221f0f462015ec1ff9f402",
        getDBHashKey(35) to "a01f7f90e7751f29876dda9a73c49ccc",
        getDBHashKey(36) to "2b2e3799733db41ef697c524296acc82",
        getDBHashKey(37) to "1e9b3d8837dc807d54beb93aa1655b43",
        getDBHashKey(38) to "07da421084ee1c2ed5aad680d284c02d"
    )
}
