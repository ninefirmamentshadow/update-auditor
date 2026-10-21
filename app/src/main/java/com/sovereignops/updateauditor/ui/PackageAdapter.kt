package com.sovereignops.updateauditor.ui

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.audit.SignerSummary
import com.sovereignops.updateauditor.model.PackageState
import java.text.DateFormat

/** The package rows. Tapping one opens its detail screen. */
class PackageAdapter(
    private val onClick: (PackageState) -> Unit,
) : ListAdapter<PackageState, PackageAdapter.PackageViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_package, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item)
        holder.itemView.setOnClickListener { onClick(item) }
    }

    class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.packageLabel)
        private val packageName: TextView = itemView.findViewById(R.id.packageName)
        private val meta: TextView = itemView.findViewById(R.id.packageMeta)
        private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

        fun bind(item: PackageState) {
            val res = itemView.resources
            label.text = res.getString(R.string.row_title, item.label, res.originLabel(item.origin))
            packageName.text = item.packageName
            meta.text = metaText(res, item, dateFormat)
        }
    }

    private object Diff : DiffUtil.ItemCallback<PackageState>() {
        override fun areItemsTheSame(oldItem: PackageState, newItem: PackageState): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: PackageState, newItem: PackageState): Boolean =
            oldItem == newItem
    }

    companion object {
        /** The multi-line summary under each row title. Public so tests can assert on it. */
        fun metaText(res: Resources, item: PackageState, dateFormat: DateFormat): String {
            val unknown = res.getString(R.string.value_unknown)
            val version = if (item.versionName.isBlank()) {
                res.getString(R.string.row_version_unknown, item.versionCode)
            } else {
                res.getString(R.string.row_version, item.versionName, item.versionCode)
            }
            val flags = buildList {
                add(res.getString(if (item.enabled) R.string.flag_enabled else R.string.flag_disabled))
                if (item.debuggable) add(res.getString(R.string.flag_debuggable))
                if (item.systemApp) add(res.getString(R.string.flag_system))
                if (item.updatedSystemApp) add(res.getString(R.string.flag_updated_system))
                if (item.apex) add(res.getString(R.string.flag_apex))
                if (item.removedForUser) add(res.getString(R.string.flag_removed))
            }
            return buildString {
                append(version)
                flags.forEach { append(" · ").append(it) }
                append('\n')
                append(res.getString(R.string.row_signed, res.signerLabel(SignerSummary.of(item.signers))))
                append('\n')
                append(res.getString(R.string.row_installer, item.installerPackageName ?: unknown))
                append('\n')
                append(
                    res.getString(
                        R.string.row_dates,
                        TimestampText.format(item.firstInstallTime, dateFormat, unknown),
                        TimestampText.format(item.lastUpdateTime, dateFormat, unknown),
                    ),
                )
            }
        }
    }
}
