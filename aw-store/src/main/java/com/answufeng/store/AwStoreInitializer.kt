package com.answufeng.store

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * 自动初始化 [AwStore] 的 ContentProvider。
 *
 * 在 AndroidManifest.xml 中声明后，无需手动在 Application.onCreate() 中调用 [AwStore.init]，
 * 库会在应用启动时自动完成初始化。
 *
 * 如需自定义初始化（如指定 rootDir），仍可在 Application.onCreate() 中手动调用 [AwStore.init]，
 * ContentProvider 不会重复初始化。
 */
class AwStoreInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        val ctx = context ?: return false
        if (!AwStore.isInitialized) {
            AwStore.init(ctx)
        }
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
