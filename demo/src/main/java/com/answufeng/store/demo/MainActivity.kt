package com.answufeng.store.demo

import android.os.Bundle
import android.os.Parcelable
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.store.AwStore
import com.answufeng.store.AwStoreLogger
import com.answufeng.store.CryptKey
import com.answufeng.store.MmkvDelegate
import com.answufeng.store.SpMigration
import com.answufeng.store.StoreJsonAdapter
import com.answufeng.store.AwStoreJsonAdapter
import kotlinx.parcelize.Parcelize
import kotlin.reflect.KClass

object UserStore : MmkvDelegate() {
    var token by string()
    var userId by long()
    var isLoggedIn by boolean()
    var score by float()
    var tags by stringSet()
    var nickname by nullableString()
    var age by nullableInt()
    var ratio by double()
    var nullableTimestamp by nullableLong()
    var nullableScore by nullableFloat()
    var nullableRatio by nullableDouble()
    var nullableEnabled by nullableBoolean()
}

object SecureStore : MmkvDelegate(secureCryptKey = CryptKey.fromSecureRandom()) {
    var password by string()
}

object IsolatedStore : MmkvDelegate(mmapId = "isolated") {
    var data by string()
}

object MultiProcessStore : MmkvDelegate(mmapId = "shared", multiProcess = true) {
    var counter by int()
}

object ParcelableStore : MmkvDelegate(mmapId = "parcelable_demo") {
    var profile by parcelable<UserProfile>()
}

object BytesStore : MmkvDelegate(mmapId = "bytes_demo") {
    var binaryData by bytes()
    var nullableBinaryData by nullableBytes()
}

object NullableSetStore : MmkvDelegate(mmapId = "nullable_set_demo") {
    var tags by nullableStringSet()
}

object JsonStore : MmkvDelegate(mmapId = "json_demo") {
    var user by json<UserInfo>()
}

@Parcelize
data class UserProfile(val name: String, val age: Int) : Parcelable

data class UserInfo(val name: String, val email: String, val level: Int)

class GsonAdapter : StoreJsonAdapter {
    private val gson = com.google.gson.Gson()
    override fun <T : Any> toJson(value: T, clazz: KClass<T>): String = gson.toJson(value)
    override fun <T : Any> fromJson(json: String, clazz: KClass<T>): T =
        gson.fromJson(json, clazz.java)
}

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        findViewById<Button>(R.id.btnWrite).setOnClickListener { writeValues() }
        findViewById<Button>(R.id.btnRead).setOnClickListener { readValues() }
        findViewById<Button>(R.id.btnNullableString).setOnClickListener { setNullableStringNull() }
        findViewById<Button>(R.id.btnNullableInt).setOnClickListener { setNullableIntNull() }
        findViewById<Button>(R.id.btnNullableOthers).setOnClickListener { testNullableOthers() }
        findViewById<Button>(R.id.btnParcelable).setOnClickListener { testParcelable() }
        findViewById<Button>(R.id.btnBytes).setOnClickListener { testBytes() }
        findViewById<Button>(R.id.btnNullableStringSet).setOnClickListener { testNullableStringSet() }
        findViewById<Button>(R.id.btnJson).setOnClickListener { testJson() }
        findViewById<Button>(R.id.btnEncrypted).setOnClickListener { testEncryptedStore() }
        findViewById<Button>(R.id.btnIsolation).setOnClickListener { testIsolation() }
        findViewById<Button>(R.id.btnMultiProcess).setOnClickListener { testMultiProcess() }
        findViewById<Button>(R.id.btnSpMigration).setOnClickListener { testSpMigration() }
        findViewById<Button>(R.id.btnContentChange).setOnClickListener { testContentChangeListener() }
        findViewById<Button>(R.id.btnOnKeyChanged).setOnClickListener { testOnKeyChanged() }
        findViewById<Button>(R.id.btnSync).setOnClickListener { testSync() }
        findViewById<Button>(R.id.btnAsync).setOnClickListener { testAsync() }
        findViewById<Button>(R.id.btnImperativeApi).setOnClickListener { testImperativeApi() }
        findViewById<Button>(R.id.btnBatchWrite).setOnClickListener { testBatchWrite() }
        findViewById<Button>(R.id.btnGetOrPut).setOnClickListener { testGetOrPut() }
        findViewById<Button>(R.id.btnExportImport).setOnClickListener { testExportImport() }
        findViewById<Button>(R.id.btnMmkvInstance).setOnClickListener { testMmkvInstance() }
        findViewById<Button>(R.id.btnManage).setOnClickListener { testManage() }
        findViewById<Button>(R.id.btnTotalSize).setOnClickListener { testTotalSize() }
        findViewById<Button>(R.id.btnClearAll).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener { clearLog() }

        AwStoreLogger.enabled = true
        if (!AwStore.isInitialized) {
            AwStore.init(this)
        }

        AwStoreJsonAdapter.setAdapter(GsonAdapter())

        log("✅ 存储初始化完成")
        log("📊 点击按钮测试各项功能")
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        android.util.Log.d("AwStoreDemo", msg)
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun clearLog() {
        tvLog.text = "日志已清除\n"
    }

    private fun writeValues() {
        UserStore.token = "abc-${System.currentTimeMillis() % 1000}"
        UserStore.userId = System.currentTimeMillis()
        UserStore.isLoggedIn = true
        UserStore.score = 95.5f
        UserStore.tags = setOf("kotlin", "android")
        UserStore.nickname = "Alice"
        UserStore.age = 25
        UserStore.ratio = 3.14159
        log("✅ 基本值写入成功(含Double)")
    }

    private fun readValues() {
        log("📖 读取基本值:")
        log("  token: ${UserStore.token}")
        log("  userId: ${UserStore.userId}")
        log("  isLoggedIn: ${UserStore.isLoggedIn}")
        log("  score: ${UserStore.score}")
        log("  tags: ${UserStore.tags}")
        log("  nickname: ${UserStore.nickname}")
        log("  age: ${UserStore.age}")
        log("  ratio: ${UserStore.ratio}")
    }

    private fun setNullableStringNull() {
        UserStore.nickname = null
        log("🔄 可空字符串设置为null: ${UserStore.nickname}")
    }

    private fun setNullableIntNull() {
        UserStore.age = null
        log("🔄 可空整型设置为null: ${UserStore.age}")
    }

    private fun testNullableOthers() {
        UserStore.nullableTimestamp = System.currentTimeMillis()
        UserStore.nullableScore = 88.5f
        UserStore.nullableRatio = 2.71828
        UserStore.nullableEnabled = true
        log("📖 Nullable Long/Float/Double/Boolean:")
        log("  nullableTimestamp: ${UserStore.nullableTimestamp}")
        log("  nullableScore: ${UserStore.nullableScore}")
        log("  nullableRatio: ${UserStore.nullableRatio}")
        log("  nullableEnabled: ${UserStore.nullableEnabled}")
        UserStore.nullableTimestamp = null
        UserStore.nullableScore = null
        UserStore.nullableRatio = null
        UserStore.nullableEnabled = null
        log("📖 设null后:")
        log("  nullableTimestamp: ${UserStore.nullableTimestamp}")
        log("  nullableScore: ${UserStore.nullableScore}")
        log("  nullableRatio: ${UserStore.nullableRatio}")
        log("  nullableEnabled: ${UserStore.nullableEnabled}")
    }

    private fun testParcelable() {
        ParcelableStore.profile = UserProfile("Alice", 25)
        val profile = ParcelableStore.profile
        log("📦 Parcelable存储: $profile")

        val retrieved = ParcelableStore.getParcelable<UserProfile>("profile")
        log("📦 命令式API getParcelable: $retrieved")
        ParcelableStore.clear()
    }

    private fun testBytes() {
        BytesStore.binaryData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        BytesStore.nullableBinaryData = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        log("📦 ByteArray存储: ${BytesStore.binaryData.toList()}")
        log("📦 Nullable ByteArray: ${BytesStore.nullableBinaryData?.toList()}")
        BytesStore.nullableBinaryData = null
        log("📦 Nullable ByteArray设null后: ${BytesStore.nullableBinaryData}")
        BytesStore.clear()
    }

    private fun testNullableStringSet() {
        NullableSetStore.tags = setOf("kotlin", "mmkv")
        log("📦 Nullable StringSet: ${NullableSetStore.tags}")
        NullableSetStore.tags = null
        log("📦 Nullable StringSet设null后: ${NullableSetStore.tags}")
        NullableSetStore.clear()
    }

    private fun testJson() {
        JsonStore.user = UserInfo("Bob", "bob@example.com", 5)
        val user = JsonStore.user
        log("📋 JSON存储: $user")

        JsonStore.putJson("json_imperative", UserInfo("Eve", "eve@test.com", 3))
        val imperative = JsonStore.getJson<UserInfo>("json_imperative")
        log("📋 命令式API putJson/getJson: $imperative")
        JsonStore.clear()
    }

    private fun testEncryptedStore() {
        SecureStore.password = "secret123"
        log("🔒 加密存储密码: ${SecureStore.password}")
    }

    private fun testIsolation() {
        IsolatedStore.data = "isolated_data"
        log("🔄 多实例隔离:")
        log("  IsolatedStore.data: ${IsolatedStore.data}")
        log("  UserStore.token (未改变): ${UserStore.token}")
    }

    private fun testMultiProcess() {
        MultiProcessStore.counter++
        log("🔄 多进程计数器: ${MultiProcessStore.counter}")
    }

    private fun testSpMigration() {
        val sp = getSharedPreferences("old_prefs", MODE_PRIVATE)
        sp.edit()
            .putString("migrated_key", "migrated_value")
            .putInt("migrated_int", 42)
            .putBoolean("migrated_bool", true)
            .apply()
        val result = SpMigration.migrate(this, "old_prefs")
        log("🔄 SP迁移结果: $result")
    }

    private fun testContentChangeListener() {
        UserStore.registerContentChange { mmapID ->
            runOnUiThread { log("🔄 其他进程修改了内容: $mmapID") }
        }
        log("🔄 跨进程监听器已注册，尝试从其他进程写入值")
    }

    private fun testOnKeyChanged() {
        UserStore.onKeyChanged { key ->
            runOnUiThread { log("🔑 单进程键变更: $key") }
        }
        UserStore.putString("test_key", "test_value")
        UserStore.putInt("test_int", 42)
        UserStore.remove("test_key")
        log("🔑 单进程监听器已注册并测试完成")
    }

    private fun testSync() {
        UserStore.token = "sync_value"
        UserStore.sync()
        log("⏳ 同步写入完成(sync)")
    }

    private fun testAsync() {
        UserStore.token = "async_value"
        UserStore.async()
        log("⚡ 异步写入已提交(async)")
    }

    private fun testImperativeApi() {
        UserStore.putString("imperative_key", "hello_from_api")
        val value = UserStore.getString("imperative_key")
        log("📝 命令式API: putString/getString → $value")
        UserStore.putInt("imperative_int", 100)
        log("📝 命令式API: putInt/getInt → ${UserStore.getInt("imperative_int")}")
        UserStore.remove("imperative_key", "imperative_int")
        log("📝 命令式API: remove后 getString → ${UserStore.getString("imperative_key")}")
    }

    private fun testBatchWrite() {
        UserStore.edit {
            encode("batch_str", "batch_value")
            encode("batch_int", 999)
            encode("batch_bool", true)
        }
        log("📝 批量写入: str=${UserStore.getString("batch_str")}, int=${UserStore.getInt("batch_int")}, bool=${UserStore.getBoolean("batch_bool")}")
        UserStore.remove("batch_str", "batch_int", "batch_bool")
    }

    private fun testGetOrPut() {
        UserStore.remove("getorput_key")
        val v1 = UserStore.getOrPutString("getorput_key") { "first_default" }
        log("📝 getOrPutString(不存在): $v1")
        val v2 = UserStore.getOrPutString("getorput_key") { "second_default" }
        log("📝 getOrPutString(已存在): $v2")
        UserStore.remove("getorput_key")

        val count = UserStore.getOrPutInt("launch_count") { 0 }
        log("📝 getOrPutInt: launch_count=$count")
    }

    private fun testExportImport() {
        UserStore.putString("export_key", "export_value")
        UserStore.putInt("export_int", 42)
        val data = UserStore.exportToMap()
        log("📤 导出数据: $data")

        UserStore.clear()
        log("📤 清空后: ${UserStore.getString("export_key")}")

        val count = UserStore.importFromMap(data)
        log("📥 导入$count条数据后: str=${UserStore.getString("export_key")}, int=${UserStore.getInt("export_int")}")
        UserStore.remove("export_key", "export_int")
    }

    private fun testMmkvInstance() {
        val mmkv = UserStore.mmkvInstance
        log("🔧 MMKV实例: mmapID=${mmkv.mmapID()}, totalSize=${mmkv.totalSize()}")
    }

    private fun testManage() {
        UserStore.token = "test"
        log("🔧 管理功能:")
        log("  'token' in UserStore: ${"token" in UserStore}")
        log("  所有键: ${UserStore.allKeys().toList()}")
        UserStore.remove("token")
        log("  删除后，'token' in UserStore: ${"token" in UserStore}")
        log("  批量删除演示: remove(\"a\", \"b\")")
    }

    private fun testTotalSize() {
        log("📊 存储文件大小:")
        log("  UserStore: ${UserStore.totalSize()} bytes")
        log("  SecureStore: ${SecureStore.totalSize()} bytes")
    }

    private fun clearAll() {
        UserStore.clear()
        SecureStore.clear()
        IsolatedStore.clear()
        MultiProcessStore.clear()
        ParcelableStore.clear()
        BytesStore.clear()
        NullableSetStore.clear()
        JsonStore.clear()
        log("🗑️ 所有存储已清除")
    }
}
