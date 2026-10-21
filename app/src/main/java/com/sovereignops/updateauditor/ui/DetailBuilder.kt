package com.sovereignops.updateauditor.ui

import android.content.res.Resources
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.audit.CertDigest
import com.sovereignops.updateauditor.audit.PackageFilter
import com.sovereignops.updateauditor.audit.SignerSummary
import com.sovereignops.updateauditor.model.PackageState
import java.text.DateFormat

/** What the detail screen shows for one package, as plain data the screen just lays out. */
object DetailBuilder {
    data class Row(val label: String, val value: String, val monospace: Boolean = false)
    data class Section(val title: String, val rows: List<Row>)

    fun build(res: Resources, item: PackageState, dateFormat: DateFormat): List<Section> {
        val unknown = res.getString(R.string.value_unknown)
        val notExposed = res.getString(R.string.value_not_exposed)
        fun yesNo(value: Boolean) = res.getString(if (value) R.string.value_yes else R.string.value_no)
        fun row(label: Int, value: String, mono: Boolean = false) = Row(res.getString(label), value, mono)

        val version = if (item.versionName.isBlank()) {
            res.getString(R.string.row_version_unknown, item.versionCode)
        } else {
            res.getString(R.string.row_version, item.versionName, item.versionCode)
        }

        val signing = buildList {
            add(row(R.string.detail_signing_summary, res.signerLabel(SignerSummary.of(item.signers))))
            add(row(R.string.detail_platform_signed, yesNo(item.signers.platformSigned)))
            add(row(R.string.detail_multiple_signers, yesNo(item.signers.multipleSigners)))
            add(row(R.string.detail_rotated, yesNo(item.signers.rotated)))
            if (item.signers.currentDigests.isEmpty()) {
                add(Row(res.getString(R.string.detail_current_signer, 1), res.getString(R.string.detail_no_signer)))
            }
            item.signers.currentDigests.forEachIndexed { index, digest ->
                add(Row(res.getString(R.string.detail_current_signer, index + 1), CertDigest.display(digest), monospace = true))
            }
            item.signers.pastDigests.forEachIndexed { index, digest ->
                add(Row(res.getString(R.string.detail_past_signer, index + 1), CertDigest.display(digest), monospace = true))
            }
        }

        val source = item.installSource
        return listOf(
            Section(
                res.getString(R.string.detail_section_identity),
                listOf(
                    row(R.string.detail_package_name, item.packageName, mono = true),
                    row(R.string.detail_version, version),
                ),
            ),
            Section(
                res.getString(R.string.detail_section_classification),
                listOf(
                    row(R.string.detail_origin, res.originLabel(item.origin)),
                    row(R.string.detail_why, res.originDetail(item.origin)),
                ),
            ),
            Section(res.getString(R.string.detail_section_signing), signing),
            Section(
                res.getString(R.string.detail_section_install_source),
                listOf(
                    row(R.string.detail_installer, source.installingPackage ?: notExposed, mono = true),
                    row(R.string.detail_initiating, source.initiatingPackage ?: notExposed, mono = true),
                    row(R.string.detail_originating, source.originatingPackage ?: notExposed, mono = true),
                    row(R.string.detail_update_owner, source.updateOwnerPackage ?: notExposed, mono = true),
                    row(R.string.detail_package_source, res.sourceLabel(source.packageSource)),
                    row(R.string.detail_sideloaded, yesNo(PackageFilter.isSideloaded(item))),
                ),
            ),
            Section(
                res.getString(R.string.detail_section_flags),
                listOf(
                    row(R.string.detail_enabled, yesNo(item.enabled)),
                    row(R.string.detail_debuggable, yesNo(item.debuggable)),
                    row(R.string.detail_system, yesNo(item.systemApp)),
                    row(R.string.detail_updated_system, yesNo(item.updatedSystemApp)),
                    row(R.string.detail_apex, yesNo(item.apex)),
                    row(R.string.detail_removed, yesNo(item.removedForUser)),
                ),
            ),
            Section(
                res.getString(R.string.detail_section_sdk),
                listOf(
                    row(R.string.detail_min_sdk, item.minSdk.takeIf { it > 0 }?.toString() ?: unknown),
                    row(R.string.detail_target_sdk, item.targetSdk.takeIf { it > 0 }?.toString() ?: unknown),
                    row(R.string.detail_compile_sdk, item.compileSdk?.toString() ?: unknown),
                ),
            ),
            Section(
                res.getString(R.string.detail_section_dates),
                listOf(
                    row(R.string.detail_first_install, TimestampText.format(item.firstInstallTime, dateFormat, unknown)),
                    row(R.string.detail_last_update, TimestampText.format(item.lastUpdateTime, dateFormat, unknown)),
                ),
            ),
        )
    }
}
