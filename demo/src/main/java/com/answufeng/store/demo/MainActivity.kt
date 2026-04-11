package com.answufeng.store.demo

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.answufeng.store.BrickStore
import com.answufeng.store.MmkvDelegate
import com.answufeng.store.SpMigration

object UserStore : MmkvDelegate() {
    var token by string("token", "")
    var userId by long("user_id", 0L)
    var isLoggedIn by boolean("is_logged_in", false)
    var score by float("score", 0f)
    var tags by stringSet("tags")
}

object SecureStore : MmkvDelegate(cryptKey = "my_secret_key") {
    var password by string("password", "")
}

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BrickStore.init(this)

        tvLog = TextView(this).apply { textSize = 14f }
        val container = findViewById<LinearLayout>(R.id.container)
        container.addView(tvLog)

        container.addView(button("Write Values") {
            UserStore.token = "abc-${System.currentTimeMillis() % 1000}"
            UserStore.userId = System.currentTimeMillis()
            UserStore.isLoggedIn = true
            UserStore.score = 95.5f
            UserStore.tags = setOf("kotlin", "android")
            log("Values written")
        })

        container.addView(button("Read Values") {
            log("token: ${UserStore.token}")
            log("userId: ${UserStore.userId}")
            log("isLoggedIn: ${UserStore.isLoggedIn}")
            log("score: ${UserStore.score}")
            log("tags: ${UserStore.tags}")
        })

        container.addView(button("Encrypted Store") {
            SecureStore.password = "secret123"
            log("Encrypted password: ${SecureStore.password}")
        })

        container.addView(button("SP Migration") {
            val sp = getSharedPreferences("old_prefs", MODE_PRIVATE)
            sp.edit().putString("migrated_key", "migrated_value").apply()
            val count = SpMigration.migrate(this, "old_prefs")
            log("Migrated $count entries from SP to MMKV")
        })

        container.addView(button("Clear All") {
            UserStore.clear()
            log("UserStore cleared")
        })
    }

    private fun button(text: String, onClick: () -> Unit): Button {
        return Button(this).apply { this.text = text; setOnClickListener { onClick() } }
    }

    private fun log(msg: String) { tvLog.append("$msg\n") }
}
