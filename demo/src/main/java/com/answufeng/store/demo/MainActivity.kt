package com.answufeng.store.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.store.AwStore
import com.answufeng.store.AwStoreLogger
import com.answufeng.store.MmkvDelegate
import com.answufeng.store.SpMigration

object UserStore : MmkvDelegate() {
    var token by string("token", "")
    var userId by long("user_id", 0L)
    var isLoggedIn by boolean("is_logged_in", false)
    var score by float("score", 0f)
    var tags by stringSet("tags")
    var nickname by nullableString("nickname")
}

object SecureStore : MmkvDelegate(cryptKey = "my_secret_key") {
    var password by string("password", "")
}

object IsolatedStore : MmkvDelegate(mmapId = "isolated") {
    var data by string("data", "")
}

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AwStoreLogger.enabled = true
        AwStore.init(this)

        tvLog = findViewById(R.id.tvLog)
        val container = findViewById<LinearLayout>(R.id.container)

        container.addView(button("Write Values") {
            UserStore.token = "abc-${System.currentTimeMillis() % 1000}"
            UserStore.userId = System.currentTimeMillis()
            UserStore.isLoggedIn = true
            UserStore.score = 95.5f
            UserStore.tags = setOf("kotlin", "android")
            UserStore.nickname = "Alice"
            log("Values written")
        })

        container.addView(button("Read Values") {
            log("token: ${UserStore.token}")
            log("userId: ${UserStore.userId}")
            log("isLoggedIn: ${UserStore.isLoggedIn}")
            log("score: ${UserStore.score}")
            log("tags: ${UserStore.tags}")
            log("nickname: ${UserStore.nickname}")
        })

        container.addView(button("Nullable String (set null)") {
            UserStore.nickname = null
            log("nickname after null: ${UserStore.nickname}")
        })

        container.addView(button("Encrypted Store") {
            SecureStore.password = "secret123"
            log("Encrypted password: ${SecureStore.password}")
        })

        container.addView(button("Multi-Instance Isolation") {
            IsolatedStore.data = "isolated_data"
            log("IsolatedStore.data: ${IsolatedStore.data}")
            log("UserStore.token (unchanged): ${UserStore.token}")
        })

        container.addView(button("Contains / AllKeys / Remove") {
            UserStore.token = "test"
            log("contains 'token': ${UserStore.contains("token")}")
            log("allKeys: ${UserStore.allKeys().toList()}")
            UserStore.remove("token")
            log("after remove, contains 'token': ${UserStore.contains("token")}")
        })

        container.addView(button("SP Migration") {
            val sp = getSharedPreferences("old_prefs", MODE_PRIVATE)
            sp.edit()
                .putString("migrated_key", "migrated_value")
                .putInt("migrated_int", 42)
                .putBoolean("migrated_bool", true)
                .apply()
            val result = SpMigration.migrate(this, "old_prefs")
            log("Migration: $result")
        })

        container.addView(button("Content Change Listener") {
            UserStore.registerContentChange { mmapID ->
                runOnUiThread { log("Content changed by other process: $mmapID") }
            }
            log("Listener registered, try writing values from another process")
        })

        container.addView(button("Clear All") {
            UserStore.clear()
            SecureStore.clear()
            IsolatedStore.clear()
            log("All stores cleared")
        })

        container.addView(button("Clear Log") {
            tvLog.text = ""
        })
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply {
            this.text = text
            setOnClickListener { onClick() }
        }
    }

    private fun log(msg: String) {
        tvLog.append("$msg\n")
        val scrollView = tvLog.parent as? ScrollView
        scrollView?.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }
}
