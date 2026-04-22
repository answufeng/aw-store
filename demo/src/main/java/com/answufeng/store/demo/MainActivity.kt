package com.answufeng.store.demo

import android.os.Bundle
import android.os.Parcelable
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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
    private lateinit var toolbar: MaterialToolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

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

        log("[OK] AwStore initialized")
        log("[TIP] Tap buttons to run demos")
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        android.util.Log.d("AwStoreDemo", msg)
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun clearLog() {
        tvLog.text = "Log cleared.\n"
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
        log("[OK] Wrote basic values (incl. Double)")
    }

    private fun readValues() {
        log("[READ] Basic values")
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
        log("[NULL] nickname = ${UserStore.nickname}")
    }

    private fun setNullableIntNull() {
        UserStore.age = null
        log("[NULL] age = ${UserStore.age}")
    }

    private fun testNullableOthers() {
        UserStore.nullableTimestamp = System.currentTimeMillis()
        UserStore.nullableScore = 88.5f
        UserStore.nullableRatio = 2.71828
        UserStore.nullableEnabled = true
        log("[READ] Nullable Long/Float/Double/Boolean")
        log("  nullableTimestamp: ${UserStore.nullableTimestamp}")
        log("  nullableScore: ${UserStore.nullableScore}")
        log("  nullableRatio: ${UserStore.nullableRatio}")
        log("  nullableEnabled: ${UserStore.nullableEnabled}")
        UserStore.nullableTimestamp = null
        UserStore.nullableScore = null
        UserStore.nullableRatio = null
        UserStore.nullableEnabled = null
        log("[READ] After set null")
        log("  nullableTimestamp: ${UserStore.nullableTimestamp}")
        log("  nullableScore: ${UserStore.nullableScore}")
        log("  nullableRatio: ${UserStore.nullableRatio}")
        log("  nullableEnabled: ${UserStore.nullableEnabled}")
    }

    private fun testParcelable() {
        ParcelableStore.profile = UserProfile("Alice", 25)
        val profile = ParcelableStore.profile
        log("[Parcelable] $profile")

        val retrieved = ParcelableStore.getParcelable<UserProfile>("profile")
        log("[Parcelable] imperative getParcelable: $retrieved")
        ParcelableStore.clear()
    }

    private fun testBytes() {
        BytesStore.binaryData = byteArrayOf(0x01, 0x02, 0x03, 0x04)
        BytesStore.nullableBinaryData = byteArrayOf(0xFF.toByte(), 0xFE.toByte())
        log("[Bytes] ${BytesStore.binaryData.toList()}")
        log("[Bytes] nullable: ${BytesStore.nullableBinaryData?.toList()}")
        BytesStore.nullableBinaryData = null
        log("[Bytes] nullable after null: ${BytesStore.nullableBinaryData}")
        BytesStore.clear()
    }

    private fun testNullableStringSet() {
        NullableSetStore.tags = setOf("kotlin", "mmkv")
        log("[StringSet] nullable: ${NullableSetStore.tags}")
        NullableSetStore.tags = null
        log("[StringSet] nullable after null: ${NullableSetStore.tags}")
        NullableSetStore.clear()
    }

    private fun testJson() {
        JsonStore.user = UserInfo("Bob", "bob@example.com", 5)
        val user = JsonStore.user
        log("[JSON] $user")

        JsonStore.putJson("json_imperative", UserInfo("Eve", "eve@test.com", 3))
        val imperative = JsonStore.getJson<UserInfo>("json_imperative")
        log("[JSON] imperative putJson/getJson: $imperative")
        JsonStore.clear()
    }

    private fun testEncryptedStore() {
        SecureStore.password = "secret123"
        log("[Secure] password: ${SecureStore.password}")
    }

    private fun testIsolation() {
        IsolatedStore.data = "isolated_data"
        log("[Isolation] multiple instances")
        log("  IsolatedStore.data: ${IsolatedStore.data}")
        log("  UserStore.token (未改变): ${UserStore.token}")
    }

    private fun testMultiProcess() {
        MultiProcessStore.counter++
        log("[MultiProcess] counter: ${MultiProcessStore.counter}")
    }

    private fun testSpMigration() {
        val sp = getSharedPreferences("old_prefs", MODE_PRIVATE)
        sp.edit()
            .putString("migrated_key", "migrated_value")
            .putInt("migrated_int", 42)
            .putBoolean("migrated_bool", true)
            .apply()
        val result = SpMigration.migrate(this, "old_prefs")
        log("[Migration] SP → MMKV: $result")
    }

    private fun testContentChangeListener() {
        UserStore.registerContentChange { mmapID ->
            runOnUiThread { log("[IPC] content changed: $mmapID") }
        }
        log("[IPC] listener registered. Try writing from another process.")
    }

    private fun testOnKeyChanged() {
        UserStore.onKeyChanged { key ->
            runOnUiThread { log("[KeyChanged] $key") }
        }
        UserStore.putString("test_key", "test_value")
        UserStore.putInt("test_int", 42)
        UserStore.remove("test_key")
        log("[KeyChanged] listener registered and tested")
    }

    private fun testSync() {
        UserStore.token = "sync_value"
        UserStore.sync()
        log("[Sync] committed")
    }

    private fun testAsync() {
        UserStore.token = "async_value"
        UserStore.async()
        log("[Async] scheduled")
    }

    private fun testImperativeApi() {
        UserStore.putString("imperative_key", "hello_from_api")
        val value = UserStore.getString("imperative_key")
        log("[API] putString/getString → $value")
        UserStore.putInt("imperative_int", 100)
        log("[API] putInt/getInt → ${UserStore.getInt("imperative_int")}")
        UserStore.remove("imperative_key", "imperative_int")
        log("[API] after remove getString → ${UserStore.getString("imperative_key")}")
    }

    private fun testBatchWrite() {
        UserStore.edit {
            encode("batch_str", "batch_value")
            encode("batch_int", 999)
            encode("batch_bool", true)
        }
        log("[Batch] str=${UserStore.getString("batch_str")}, int=${UserStore.getInt("batch_int")}, bool=${UserStore.getBoolean("batch_bool")}")
        UserStore.remove("batch_str", "batch_int", "batch_bool")
    }

    private fun testGetOrPut() {
        UserStore.remove("getorput_key")
        val v1 = UserStore.getOrPutString("getorput_key") { "first_default" }
        log("[GetOrPut] getOrPutString(missing): $v1")
        val v2 = UserStore.getOrPutString("getorput_key") { "second_default" }
        log("[GetOrPut] getOrPutString(existing): $v2")
        UserStore.remove("getorput_key")

        val count = UserStore.getOrPutInt("launch_count") { 0 }
        log("[GetOrPut] launch_count=$count")
    }

    private fun testExportImport() {
        UserStore.putString("export_key", "export_value")
        UserStore.putInt("export_int", 42)
        val data = UserStore.exportToMap()
        log("[Export] $data")

        UserStore.clear()
        log("[Export] after clear: ${UserStore.getString("export_key")}")

        val count = UserStore.importFromMap(data)
        log("[Import] imported=$count, str=${UserStore.getString("export_key")}, int=${UserStore.getInt("export_int")}")
        UserStore.remove("export_key", "export_int")
    }

    private fun testMmkvInstance() {
        val mmkv = UserStore.mmkvInstance
        log("[MMKV] mmapID=${mmkv.mmapID()}, totalSize=${mmkv.totalSize()}")
    }

    private fun testManage() {
        UserStore.token = "test"
        log("[Manage]")
        log("  'token' in UserStore: ${"token" in UserStore}")
        log("  所有键: ${UserStore.allKeys().toList()}")
        UserStore.remove("token")
        log("  删除后，'token' in UserStore: ${"token" in UserStore}")
        log("  批量删除演示: remove(\"a\", \"b\")")
    }

    private fun testTotalSize() {
        log("[Size]")
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
        log("[OK] All stores cleared")
    }

    override fun onCreateOptionsMenu(menu: android.view.Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_demo_playbook -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.demo_playbook_title)
                    .setMessage(R.string.demo_playbook_message)
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }
            R.id.action_copy_log -> {
                copyLogToClipboard()
                true
            }
            R.id.action_share_log -> {
                shareLog()
                true
            }
            R.id.action_clear_log -> {
                clearLog()
                true
            }
            R.id.action_clear_all -> {
                clearAll()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copyLogToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AwStore Demo Log", tvLog.text))
        log("[OK] Log copied to clipboard")
    }

    private fun shareLog() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AwStore Demo Log")
            putExtra(Intent.EXTRA_TEXT, tvLog.text.toString())
        }
        startActivity(Intent.createChooser(intent, "Share log"))
    }
}
