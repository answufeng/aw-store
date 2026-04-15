package com.answufeng.store.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.store.AwStore
import com.answufeng.store.AwStoreLogger
import com.answufeng.store.CryptKey
import com.answufeng.store.MmkvDelegate
import com.answufeng.store.SpMigration
import com.google.android.material.card.MaterialCardView

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

        // 主布局
        val mainLayout = findViewById<LinearLayout>(R.id.mainLayout)

        // 标题
        mainLayout.addView(TextView(this).apply {
            text = "💾 aw-store 功能演示"
            textSize = 20f
            setPadding(0, 0, 0, 20)
        })

        // 基本存储卡片
        val basicCard = createCard("基本存储")
        val basicLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        basicLayout.addView(createButton("✏️ 写入基本值", ::writeValues))
        basicLayout.addView(createButton("📖 读取基本值", ::readValues))
        basicLayout.addView(createButton("🔄 可空字符串设为null", ::setNullableStringNull))
        basicLayout.addView(createButton("🔄 可空整型设为null", ::setNullableIntNull))
        basicCard.addView(basicLayout)
        mainLayout.addView(basicCard)

        // 高级功能卡片
        val advancedCard = createCard("高级功能")
        val advancedLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        advancedLayout.addView(createButton("🔒 加密存储", ::testEncryptedStore))
        advancedLayout.addView(createButton("🔄 多实例隔离", ::testIsolation))
        advancedLayout.addView(createButton("🔄 多进程支持", ::testMultiProcess))
        advancedLayout.addView(createButton("🔄 SP 迁移", ::testSpMigration))
        advancedLayout.addView(createButton("🔄 内容变化监听", ::testContentChangeListener))
        advancedCard.addView(advancedLayout)
        mainLayout.addView(advancedCard)

        // 管理功能卡片
        val manageCard = createCard("管理功能")
        val manageLayout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        manageLayout.addView(createButton("🔧 存储管理", ::testManage))
        manageLayout.addView(createButton("🗑️ 清除所有存储", ::clearAll))
        manageLayout.addView(createButton("🗑️ 清除日志", ::clearLog))
        manageCard.addView(manageLayout)
        mainLayout.addView(manageCard)

        // 日志区域
        mainLayout.addView(TextView(this).apply {
            text = "操作日志："
            textSize = 16f
            setPadding(0, 20, 0, 10)
        })

        // 初始化存储
        AwStoreLogger.enabled = true
        if (!AwStore.isInitialized) {
            AwStore.init(this)
        }

        logScrollView = findViewById(R.id.logScrollView)
        tvLog = findViewById(R.id.tvLog)

        // 显示初始信息
        log("✅ 存储初始化完成")
        log("📊 点击按钮测试各项功能")
    }

    private fun createCard(title: String): MaterialCardView {
        return MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 16)
            }
            setPadding(20, 20, 20, 20)

            addView(TextView(this@MainActivity).apply {
                text = title
                textSize = 16f
                setPadding(0, 0, 0, 12)
            })
        }
    }

    private fun createButton(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 4, 0, 4)
            }
            setOnClickListener { onClick() }
        }
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
        android.util.Log.d("AwStoreDemo", msg)
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
