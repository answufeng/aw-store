package com.answufeng.store.demo

import android.content.Context
import com.answufeng.store.SpMigration

class DemoRunner(private val context: Context) {

    var ipcListener: ((String) -> Unit)? = null
    var keyListener: ((String) -> Unit)? = null

    fun run(id: String, log: (String) -> Unit) {
        log("── ${sectionLabel(id)} ──")
        when (id) {
            "basics_rw" -> basicsReadWrite(log)
            "nullable" -> nullableSemantics(log)
            "complex_types" -> complexTypes(log)
            "secure" -> secureStore(log)
            "isolation" -> isolation(log)
            "multi_process" -> multiProcess(log)
            "sp_migration" -> spMigration(log)
            "listeners" -> listeners(log)
            "imperative" -> imperativeAndBatch(log)
            "get_or_put" -> getOrPut(log)
            "debug_tools" -> debugTools(log)
            "clear_all" -> clearAll(log)
            else -> log("[?] 未知演示: $id")
        }
        log("")
    }

    fun unregisterListeners(log: (String) -> Unit) {
        ipcListener?.let {
            UserStore.unregisterContentChange(it)
            ipcListener = null
        }
        keyListener?.let {
            UserStore.unregisterOnKeyChanged(it)
            keyListener = null
        }
        log("[OK] 已注销监听")
    }

    private fun sectionLabel(id: String): String = when (id) {
        "basics_rw" -> "基本读写"
        "nullable" -> "Nullable"
        "complex_types" -> "复杂类型"
        "secure" -> "加密"
        "isolation" -> "多实例"
        "multi_process" -> "多进程"
        "sp_migration" -> "SP 迁移"
        "listeners" -> "监听"
        "imperative" -> "命令式 / edit"
        "get_or_put" -> "getOrPut"
        "debug_tools" -> "调试工具"
        "clear_all" -> "清空"
        else -> id
    }

    private fun basicsReadWrite(log: (String) -> Unit) {
        UserStore.token = "abc-${System.currentTimeMillis() % 1000}"
        UserStore.userId = System.currentTimeMillis()
        UserStore.isLoggedIn = true
        UserStore.score = 95.5f
        UserStore.tags = setOf("kotlin", "android")
        UserStore.nickname = "Alice"
        UserStore.age = 25
        UserStore.ratio = 3.14159
        log("[写入] token, userId, bool, float, set, nullable, double")
        log("  token=${UserStore.token}")
        log("  userId=${UserStore.userId}")
        log("  isLoggedIn=${UserStore.isLoggedIn}")
        log("  score=${UserStore.score}")
        log("  tags=${UserStore.tags}")
        log("  nickname=${UserStore.nickname}")
        log("  age=${UserStore.age}")
        log("  ratio=${UserStore.ratio}")
    }

    private fun nullableSemantics(log: (String) -> Unit) {
        UserStore.nickname = "Bob"
        log("[nullableString] 写入后: ${UserStore.nickname}")
        UserStore.nickname = null
        log("[nullableString] 赋 null 后: ${UserStore.nickname}")

        UserStore.age = 30
        UserStore.age = null
        log("[nullableInt] 赋 null 后: ${UserStore.age}")

        NullableDefaultStore.clear()
        log("[default] 清空后 label=${NullableDefaultStore.label}")
        NullableDefaultStore.label = "已写入"
        NullableDefaultStore.label = null
        log("[default] 删键后 label=${NullableDefaultStore.label}")
    }

    private fun complexTypes(log: (String) -> Unit) {
        ParcelableStore.profile = UserProfile("Alice", 25)
        log("[Parcelable] ${ParcelableStore.profile}")

        BytesStore.binaryData = byteArrayOf(1, 2, 3)
        BytesStore.nullableBinaryData = null
        log("[Bytes] ${BytesStore.binaryData.toList()}, nullable=${BytesStore.nullableBinaryData}")

        NullableSetStore.tags = setOf("mmkv", "kotlin")
        NullableSetStore.tags = null
        log("[StringSet] nullable 删键后: ${NullableSetStore.tags}")

        JsonStore.user = UserInfo("Bob", "bob@example.com", 5)
        log("[JSON] ${JsonStore.user}")
        JsonStore.putJson("extra", UserInfo("Eve", "eve@test.com", 3))
        log("[JSON] imperative: ${JsonStore.getJson<UserInfo>("extra")}")

        ParcelableStore.clear()
        BytesStore.clear()
        NullableSetStore.clear()
        JsonStore.clear()
    }

    private fun secureStore(log: (String) -> Unit) {
        SecureStore.password = "secret123"
        log("[加密] mmapId=${SecureStore.effectiveMmapId}")
        log("  password=${SecureStore.password}")
    }

    private fun isolation(log: (String) -> Unit) {
        IsolatedStore.data = "isolated_data"
        log("[隔离] IsolatedStore=${IsolatedStore.data}")
        log("  UserStore.token 未变: ${UserStore.token}")
    }

    private fun multiProcess(log: (String) -> Unit) {
        MultiProcessStore.counter++
        log("[多进程] effectiveMmapId=${MultiProcessStore.effectiveMmapId}")
        log("  counter=${MultiProcessStore.counter}")
    }

    private fun spMigration(log: (String) -> Unit) {
        val spName = "demo_old_prefs"
        context.getSharedPreferences(spName, Context.MODE_PRIVATE).edit()
            .putString("migrated_key", "migrated_value")
            .putInt("migrated_int", 42)
            .apply()
        val result = SpMigration.migrate(context, spName)
        log("[迁移] 删除原 SP: $result")
        log("  MMKV 读取: ${UserStore.getString("migrated_key")}")

        val keepName = "demo_keep_prefs"
        context.getSharedPreferences(keepName, Context.MODE_PRIVATE).edit()
            .putString("keep_k", "keep_v")
            .commit()
        SpMigration.migrate(context, keepName, deleteAfterMigration = false)
        val stillInSp = context.getSharedPreferences(keepName, Context.MODE_PRIVATE)
            .getString("keep_k", null)
        log("[迁移] 保留原 SP: SP=$stillInSp")
        UserStore.remove("keep_k")
        context.getSharedPreferences(keepName, Context.MODE_PRIVATE).edit().clear().commit()
    }

    private fun listeners(log: (String) -> Unit) {
        unregisterListeners(log)
        ipcListener = { mmapId -> log("[跨进程] $mmapId") }
        keyListener = { key -> log("[键变更] $key") }
        UserStore.registerContentChange(listener = ipcListener!!)
        UserStore.registerOnKeyChanged(keyListener!!)
        UserStore.putString("listener_demo", "v1")
        UserStore.remove("listener_demo")
        log("[监听] 已注册；可从其它进程写入 ${UserStore.effectiveMmapId} 验证跨进程回调")
    }

    private fun imperativeAndBatch(log: (String) -> Unit) {
        UserStore.putString("api_key", "hello")
        log("[命令式] getString=${UserStore.getString("api_key")}")

        UserStore.edit {
            encode("batch_str", "batch")
            encode("batch_int", 42)
        }
        log("[edit] str=${UserStore.getString("batch_str")}, int=${UserStore.getInt("batch_int")}")

        val ttlKey = "demo_ttl"
        UserStore.edit {
            mmkv.encode(ttlKey, "60s", 60)
            markKeyChanged(ttlKey)
        }
        log("[TTL] $ttlKey=${UserStore.getString(ttlKey)}")

        UserStore.sync()
        log("[sync/async] 已 sync；token 已异步写入")
        UserStore.token = "async_ok"
        UserStore.async()

        UserStore.remove("api_key", "batch_str", "batch_int", ttlKey)
    }

    private fun getOrPut(log: (String) -> Unit) {
        UserStore.remove("gop_key")
        val v1 = UserStore.getOrPutString("gop_key") { "first" }
        val v2 = UserStore.getOrPutString("gop_key") { "second" }
        log("[getOrPutString] 1st=$v1, 2nd=$v2 (default 只执行一次)")

        JsonStore.remove("gop_json")
        val j1 = JsonStore.getOrPutJson("gop_json") { UserInfo("D", "d@e.com", 1) }
        val j2 = JsonStore.getOrPutJson("gop_json") { UserInfo("X", "x@y.z", 9) }
        log("[getOrPutJson] $j1 → $j2")

        UserStore.remove("gop_key")
        JsonStore.clear()
    }

    private fun debugTools(log: (String) -> Unit) {
        UserStore.putString("export_k", "export_v")
        UserStore.putInt("export_i", 7)
        val map = UserStore.exportToMap().filterKeys { it.startsWith("export_") }
        log("[export] $map")
        UserStore.remove("export_k", "export_i")

        log("[manage] 'token' in store: ${"token" in UserStore}")
        log("[manage] keys: ${UserStore.allKeys().take(8).joinToString()}")

        val mmkv = UserStore.mmkvInstance
        log("[mmkv] mmapID=${mmkv.mmapID()}, totalSize=${UserStore.totalSize()} bytes")
    }

    private fun clearAll(log: (String) -> Unit) {
        unregisterListeners(log)
        UserStore.clear()
        SecureStore.clear()
        IsolatedStore.clear()
        MultiProcessStore.clear()
        ParcelableStore.clear()
        BytesStore.clear()
        NullableSetStore.clear()
        NullableDefaultStore.clear()
        JsonStore.clear()
        log("[OK] 所有 Demo Store 已清空")
    }
}
