package com.example.deviceidlab.provider

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.util.Log
import com.example.deviceidlab.manager.DeviceIdentityManager

class DeviceIdProvider : ContentProvider() {

    companion object {
        private const val TAG = "DeviceIdProvider"
        const val AUTHORITY = "com.example.deviceidlab.provider.deviceid"
        val CONTENT_URI: Uri = Uri.parse("content://$AUTHORITY/profile")
    }

    override fun onCreate(): Boolean {
        Log.d(TAG, "DeviceIdProvider initialized")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val ctx = context ?: return MatrixCursor(emptyArray())
        val manager = DeviceIdentityManager(ctx)
        val profile = manager.getActiveProfile()

        Log.d(TAG, "IPC Query received for URI: $uri, returning profile: ${profile.name}, Android ID: ${profile.androidId}")

        val columns = arrayOf(
            "androidId", "imei", "serialNumber", "macAddress",
            "buildModel", "buildManufacturer", "buildBrand",
            "buildProduct", "buildDevice", "buildFingerprint"
        )
        val cursor = MatrixCursor(columns)
        cursor.addRow(
            arrayOf(
                profile.androidId,
                profile.imei,
                profile.serialNumber,
                profile.macAddress,
                profile.buildModel,
                profile.buildManufacturer,
                profile.buildBrand,
                profile.buildProduct,
                profile.buildDevice,
                profile.buildFingerprint
            )
        )
        return cursor
    }

    override fun getType(uri: Uri): String? = "vnd.android.cursor.dir/vnd.com.example.deviceidlab.profile"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0
}
