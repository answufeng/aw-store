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
import kotlinx.parcelize.Parcelize

object UserStore : MmkvDelegate() {
    var token by string()
    var userId by long()
    var isLoggedIn by boolean()
    var score by float()
    var tags by stringSet()
    var nickname by nullableString()
    var age by nullableInt()
    var ratio by double()
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

@Parcelize
data class UserProfile(val name: String, val age: Int) : Parcelable

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
        findViewById<Button>(R.id.btnParcelable).setOnClickListener { testParcelable() }
        findViewById<Button>(R.id.btnBytes).setOnClickListener { testBytes() }
        findViewById<Button>(R.id.btnNullableStringSet).setOnClickListener { testNullableStringSet() }
        findViewById<Button>(R.id.btnEncrypted).setOnClickListener { testEncryptedStore() }
        findViewById<Button>(R.id.btnIsolation).setOnClickListener { testIsolation() }
        findViewById<Button>(R.id.btnMultiProcess).setOnClickListener { testMultiProcess() }
        findViewById<Button>(R.id.btnSpMigration).setOnClickListener { testSpMigration() }
        findViewById<Button>(R.id.btnContentChange).setOnClickListener { testContentChangeListener() }
        findViewById<Button>(R.id.btnSync).setOnClickListener { testSync() }
        findViewById<Button>(R.id.btnAsync).setOnClickListener { testAsync() }
        findViewById<Button>(R.id.btnImperativeApi).setOnClickListener { testImperativeApi() }
        findViewById<Button>(R.id.btnManage).setOnClickListener { testManage() }
        findViewById<Button>(R.id.btnTotalSize).setOnClickListener { testTotalSize() }
        findViewById<Button>(R.id.btnClearAll).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener { clearLog() }

        AwStoreLogger.enabled = true
        if (!AwStore.isInitialized) {
            AwStore.init(this)
        }

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

    private fun testParcelable() {
        ParcelableStore.profile = UserProfile("Alice", 25)
        val profile = ParcelableStore.profile
        log("📦 Parcelable存储: $profile")
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
        log("🔄 监听器已注册，尝试从其他进程写入值")
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
        log("🗑️ 所有存储已清除")
    }
}
