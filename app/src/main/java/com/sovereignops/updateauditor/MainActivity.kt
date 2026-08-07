package com.sovereignops.updateauditor

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
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
import com.sovereignops.updateauditor.status.ChannelStatus
import com.sovereignops.updateauditor.status.UpdateChannel
import com.sovereignops.updateauditor.status.UpdateVerdict
import com.sovereignops.updateauditor.status.VerdictEngine
import com.sovereignops.updateauditor.ui.PackageAdapter
import com.sovereignops.updateauditor.upstream.ProviderResult
import com.sovereignops.updateauditor.upstream.samsung.SamsungHistoryProvider
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private val packageAdapter = PackageAdapter()
    private val samsungProvider = SamsungHistoryProvider()

    private lateinit var statusText: TextView
    private lateinit var updateStatusText: TextView
    private lateinit var deviceText: TextView
    private lateinit var packageSummaryText: TextView
    private lateinit var refreshButton: Button
    private lateinit var checkSamsungButton: Button
    private lateinit var systemUpdateButton: Button

    private var latestSamsungStatus: ChannelStatus? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        updateStatusText = findViewById(R.id.updateStatusText)
        deviceText = findViewById(R.id.deviceText)
        packageSummaryText = findViewById(R.id.packageSummaryText)
        refreshButton = findViewById(R.id.refreshButton)
        checkSamsungButton = findViewById(R.id.checkSamsungButton)
        systemUpdateButton = findViewById(R.id.systemUpdateButton)

        findViewById<RecyclerView>(R.id.packageList).apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = packageAdapter
        }

        refreshButton.setOnClickListener { refreshAudit() }
        checkSamsungButton.setOnClickListener { checkSamsungHistory() }
        systemUpdateButton.setOnClickListener { openSystemUpdateSettings() }

        renderUpdateStatus()
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
                        "Local observation complete. Upstream checks run only when requested."
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

    private fun checkSamsungHistory() {
        checkSamsungButton.isEnabled = false
        statusText.text = "Checking Samsung official update history…"

        executor.execute {
            val device = DeviceAuditor.read()
            val channelStatus = when (val result = samsungProvider.fetch(device.model)) {
                is ProviderResult.Success -> VerdictEngine.samsungFirmware(
                    localBuild = device.firmwareBuild,
                    localSecurityPatch = device.securityPatch,
                    releases = result.value,
                    source = result.source,
                    checkedAt = result.checkedAt,
                )

                is ProviderResult.Unsupported -> ChannelStatus(
                    channel = UpdateChannel.SAMSUNG_FIRMWARE,
                    verdict = UpdateVerdict.UNSUPPORTED,
                    localValue = device.firmwareBuild,
                    detail = result.reason,
                )

                is ProviderResult.Failure -> ChannelStatus(
                    channel = UpdateChannel.SAMSUNG_FIRMWARE,
                    verdict = UpdateVerdict.ERROR,
                    localValue = device.firmwareBuild,
                    detail = result.reason,
                )
            }

            if (isFinishing || isDestroyed) return@execute
            runOnUiThread {
                latestSamsungStatus = channelStatus
                renderUpdateStatus()
                statusText.text = when (channelStatus.verdict) {
                    UpdateVerdict.ERROR -> "Samsung check failed. Local audit remains available."
                    UpdateVerdict.UNSUPPORTED -> "This Samsung model is not registered for online checks yet."
                    else -> "Samsung history check complete."
                }
                checkSamsungButton.isEnabled = true
            }
        }
    }

    private fun openSystemUpdateSettings() {
        val intent = Intent(SYSTEM_UPDATE_SETTINGS_ACTION)
        val handler = packageManager
            .queryIntentActivities(intent, PackageManager.MATCH_SYSTEM_ONLY)
            .asSequence()
            .filter { it.priority > 0 && it.activityInfo != null }
            .maxByOrNull { it.priority }

        if (handler?.activityInfo == null) {
            statusText.text = "No trusted system software-update settings handler was found."
            return
        }

        intent.setComponent(
            ComponentName(
                handler.activityInfo.packageName,
                handler.activityInfo.name,
            ),
        )

        runCatching { startActivity(intent) }
            .onFailure { statusText.text = "Could not open system update settings." }
    }

    private fun renderUpdateStatus() {
        updateStatusText.text = buildString {
            appendLine("UPDATE STATUS")
            appendLine()

            val samsung = latestSamsungStatus
            if (samsung == null) {
                appendLine("Samsung firmware       NOT CHECKED")
            } else {
                appendLine("Samsung firmware       ${samsung.verdict.displayName()}")
                appendLine("Installed              ${samsung.localValue}")
                samsung.upstreamValue?.let { appendLine("Latest source build    $it") }
                samsung.checkedAt?.let {
                    appendLine("Checked                ${CHECKED_TIME_FORMAT.format(it)}")
                }
                if (samsung.source != null) {
                    appendLine("Source                 Samsung official history")
                }
                samsung.detail?.let { appendLine("Note                   $it") }
            }

            appendLine()
            appendLine("Samsung OTA            MANUAL CHECK REQUIRED")
            appendLine("Google Play system     UNVERIFIED")
            appendLine("Play Store apps        MANUAL CHECK REQUIRED")
            appendLine("Galaxy Store apps      MANUAL CHECK REQUIRED")
            appendLine("Neo/F-Droid            UNSUPPORTED IN v0.2")
            appendLine("Obtainium              UNSUPPORTED IN v0.2")
            append("First-party apps       LOCAL ONLY")
        }
    }

    private fun renderDevice(device: DeviceState) {
        deviceText.text = buildString {
            appendLine("${device.manufacturer} ${device.model}")
            appendLine("device       ${device.device}")
            appendLine("product      ${device.product}")
            appendLine("Android      ${device.androidVersion} (SDK ${device.sdkInt})")
            appendLine("patch        ${device.securityPatch}")
            appendLine("build ID     ${device.buildId}")
            appendLine("firmware     ${device.firmwareBuild}")
            appendLine("baseband     ${device.baseband}")
            append("fingerprint  ${device.fingerprint}")
        }
    }

    private fun UpdateVerdict.displayName(): String =
        name.replace('_', ' ')

    companion object {
        private const val SYSTEM_UPDATE_SETTINGS_ACTION = "android.settings.SYSTEM_UPDATE_SETTINGS"

        private val CHECKED_TIME_FORMAT = DateTimeFormatter
            .ofPattern("h:mm a")
            .withZone(ZoneId.systemDefault())
    }
}
