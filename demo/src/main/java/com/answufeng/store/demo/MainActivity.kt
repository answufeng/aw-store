package com.answufeng.store.demo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.answufeng.store.AwStore
import com.answufeng.store.AwStoreJsonAdapter
import com.answufeng.store.AwStoreLogger

class MainActivity : AppCompatActivity() {

    private lateinit var tvLog: TextView
    private lateinit var logScrollView: ScrollView
    private lateinit var demoRunner: DemoRunner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tvLog = findViewById(R.id.tvLog)
        logScrollView = findViewById(R.id.logScrollView)
        demoRunner = DemoRunner(this)

        findViewById<android.view.View>(R.id.btnClearLog).setOnClickListener { clearLog() }

        AwStoreLogger.enabled = true
        if (!AwStore.isInitialized) {
            AwStore.init(this)
        }
        AwStoreJsonAdapter.setAdapter(GsonAdapter())

        val adapter = DemoListAdapter { item ->
            if (item.destructive) {
                confirmClear { demoRunner.run(item.id, ::appendLog) }
            } else {
                demoRunner.run(item.id, ::appendLog)
            }
        }
        findViewById<RecyclerView>(R.id.rvDemos).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            this.adapter = adapter
            itemAnimator = null
        }
        adapter.submitSections(DemoCatalog.sections)

        appendLog("[OK] AwStore 已就绪 · 选择场景开始演示")
    }

    private fun appendLog(msg: String) {
        if (tvLog.text.toString() == getString(R.string.log_placeholder)) {
            tvLog.text = ""
        }
        tvLog.append("$msg\n")
        android.util.Log.d("AwStoreDemo", msg)
        logScrollView.post { logScrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    private fun clearLog() {
        tvLog.text = getString(R.string.log_placeholder)
    }

    private fun confirmClear(onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.confirm_clear_title)
            .setMessage(R.string.confirm_clear_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> onConfirm() }
            .show()
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
            R.id.action_unregister_listeners -> {
                demoRunner.unregisterListeners(::appendLog)
                true
            }
            R.id.action_clear_all -> {
                confirmClear { demoRunner.run("clear_all", ::appendLog) }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun copyLogToClipboard() {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("AwStore Demo Log", tvLog.text))
        appendLog("[OK] 日志已复制")
    }

    private fun shareLog() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "AwStore Demo Log")
            putExtra(Intent.EXTRA_TEXT, tvLog.text.toString())
        }
        startActivity(Intent.createChooser(intent, getString(R.string.menu_share_log)))
    }
}
