package com.sovereignops.updateauditor.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.model.PackageState
import java.text.DateFormat
import java.util.Date

class PackageAdapter : ListAdapter<PackageState, PackageAdapter.PackageViewHolder>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_package, parent, false)
        return PackageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class PackageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val label: TextView = itemView.findViewById(R.id.packageLabel)
        private val packageName: TextView = itemView.findViewById(R.id.packageName)
        private val meta: TextView = itemView.findViewById(R.id.packageMeta)
        private val dateFormat = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)

        fun bind(item: PackageState) {
            label.text = "${item.label} · ${item.origin.name.replace('_', ' ')}"
            packageName.text = item.packageName
            meta.text = buildString {
                append("v${item.versionName} (${item.versionCode})")
                append(if (item.enabled) " · enabled" else " · disabled")
                if (item.debuggable) append(" · debuggable")
                if (item.systemApp) append(" · system")
                append("\ninstaller: ")
                append(item.installerPackageName ?: "unknown")
                append("\ninstalled: ")
                append(dateFormat.format(Date(item.firstInstallTime)))
                append(" · updated: ")
                append(dateFormat.format(Date(item.lastUpdateTime)))
            }
        }
    }

    private object Diff : DiffUtil.ItemCallback<PackageState>() {
        override fun areItemsTheSame(oldItem: PackageState, newItem: PackageState): Boolean =
            oldItem.packageName == newItem.packageName

        override fun areContentsTheSame(oldItem: PackageState, newItem: PackageState): Boolean =
            oldItem == newItem
    }
}
