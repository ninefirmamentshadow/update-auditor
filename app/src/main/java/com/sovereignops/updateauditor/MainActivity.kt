package com.sovereignops.updateauditor

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sovereignops.updateauditor.audit.DeviceAuditor
import com.sovereignops.updateauditor.audit.PackageAuditor
import com.sovereignops.updateauditor.model.DeviceState
import com.sovereignops.updateauditor.model.PackageOrigin
import com.sovereignops.updateauditor.ui.PackageAdapter
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val packageAdapter = PackageAdapter()

    private lateinit var statusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var packageSummaryText: TextView
    private lateinit var refreshButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        deviceText = findViewById(R.id.deviceText)
        packageSummaryText = findViewById(R.id.packageSummaryText)
        refreshButton = findViewById(R.id.refreshButton)

        findViewById<RecyclerView>(R.id.packageList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = packageAdapter
        }

        refreshButton.setOnClickListener { refreshAudit() }
        refreshAudit()
    }

    override fun onDestroy() {
        executor.shutdownNow()
        super.onDestroy()
    }

    private fun refreshAudit() {
        refreshButton.isEnabled = false
        statusText.text = "Scanning local device state…"

        executor.execute {
            runCatching {
                val device = DeviceAuditor.read()
                val packages = PackageAuditor(applicationContext).read()
                device to packages
            }.onSuccess { (device, packages) ->
                if (isFinishing || isDestroyed) return@onSuccess
                runOnUiThread {
                    renderDevice(device)
                    packageAdapter.submitList(packages)

                    val firstParty = packages.count { it.origin == PackageOrigin.FIRST_PARTY }
                    val system = packages.count { it.origin == PackageOrigin.SYSTEM }
                    val user = packages.count { it.origin == PackageOrigin.USER }
                    packageSummaryText.text =
                        "Packages: ${packages.size} · first-party $firstParty · system $system · user $user"
                    statusText.text =
                        "Local observation complete. No network checks or update claims performed."
                    refreshButton.isEnabled = true
                }
            }.onFailure { error ->
                if (isFinishing || isDestroyed) return@onFailure
                runOnUiThread {
                    statusText.text = "Audit failed locally: ${error.javaClass.simpleName}"
                    refreshButton.isEnabled = true
                }
            }
        }
    }

    private fun renderDevice(device: DeviceState) {
        deviceText.text = buildString {
            appendLine("${device.manufacturer} ${device.model}")
            appendLine("device       ${device.device}")
            appendLine("product      ${device.product}")
            appendLine("Android      ${device.androidVersion} (SDK ${device.sdkInt})")
            appendLine("patch        ${device.securityPatch}")
            appendLine("build        ${device.buildId}")
            appendLine("baseband     ${device.baseband}")
            append("fingerprint  ${device.fingerprint}")
        }
    }
}
