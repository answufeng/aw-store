package com.answufeng.store.demo

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.store.AwStore
import com.answufeng.store.AwStoreLogger
import com.answufeng.store.CryptKey
import com.answufeng.store.MmkvDelegate
import com.answufeng.store.SpMigration

/**
 * aw-store 库功能演示
 * 包含：基本存储、加密存储、多实例隔离、多进程、SP迁移等功能
 */
object UserStore : MmkvDelegate() {
    var token by string()
    var userId by long()
    var isLoggedIn by boolean()
    var score by float()
    var tags by stringSet()
    var nickname by nullableString()
    var age by nullableInt()
}

object SecureStore : MmkvDelegate(
    secureCryptKey = CryptKey.fromSecureRandom()
) {
    var password by string()
}

object IsolatedStore : MmkvDelegate(mmapId = "isolated") {
    var data by string()
}

object MultiProcessStore : MmkvDelegate(mmapId = "shared", multiProcess = true) {
    var counter by int()
}

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化存储
        AwStoreLogger.enabled = true
        if (!AwStore.isInitialized) {
            AwStore.init(this)
        }

        // 绑定视图
        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)

        // 基本存储按钮
        findViewById<Button>(R.id.btnWrite).setOnClickListener { writeValues() }
        findViewById<Button>(R.id.btnRead).setOnClickListener { readValues() }
        findViewById<Button>(R.id.btnNullableString).setOnClickListener { setNullableStringNull() }
        findViewById<Button>(R.id.btnNullableInt).setOnClickListener { setNullableIntNull() }

        // 高级功能按钮
        findViewById<Button>(R.id.btnEncrypted).setOnClickListener { testEncryptedStore() }
        findViewById<Button>(R.id.btnIsolation).setOnClickListener { testIsolation() }
        findViewById<Button>(R.id.btnMultiProcess).setOnClickListener { testMultiProcess() }
        findViewById<Button>(R.id.btnSpMigration).setOnClickListener { testSpMigration() }
        findViewById<Button>(R.id.btnContentChange).setOnClickListener { testContentChangeListener() }

        // 管理按钮
        findViewById<Button>(R.id.btnManage).setOnClickListener { testManage() }
        findViewById<Button>(R.id.btnClearAll).setOnClickListener { clearAll() }
        findViewById<Button>(R.id.btnClearLog).setOnClickListener { clearLog() }

        // 显示初始信息
        log("✅ 存储初始化完成")
        log("📊 点击按钮测试各项功能")
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        android.util.Log.d("AwStoreDemo", msg)
    }

    private fun clearLog() {
        tvLog.text = ""
        log("🗑️ 日志已清除")
    }

    private fun writeValues() {
        UserStore.token = "abc-${System.currentTimeMillis() % 1000}"
        UserStore.userId = System.currentTimeMillis()
        UserStore.isLoggedIn = true
        UserStore.score = 95.5f
        UserStore.tags = setOf("kotlin", "android")
        UserStore.nickname = "Alice"
        UserStore.age = 25
        log("✅ 基本值写入成功")
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
    }

    private fun setNullableStringNull() {
        UserStore.nickname = null
        log("🔄 可空字符串设置为null: ${UserStore.nickname}")
    }

    private fun setNullableIntNull() {
        UserStore.age = null
        log("🔄 可空整型设置为null: ${UserStore.age}")
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

    private fun testManage() {
        UserStore.token = "test"
        log("🔧 管理功能:")
        log("  包含'token': ${UserStore.contains("token")}")
        log("  所有键: ${UserStore.allKeys().toList()}")
        UserStore.remove("token")
        log("  删除后，包含'token': ${UserStore.contains("token")}")
    }

    private fun clearAll() {
        UserStore.clear()
        SecureStore.clear()
        IsolatedStore.clear()
        MultiProcessStore.clear()
        log("🗑️ 所有存储已清除")
    }
}
