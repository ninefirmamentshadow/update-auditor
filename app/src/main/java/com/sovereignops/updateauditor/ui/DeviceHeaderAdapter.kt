package com.sovereignops.updateauditor.ui

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.sovereignops.updateauditor.R
import com.sovereignops.updateauditor.model.DeviceState

/** One scrolling card at the top of the list: scan status and the device's identifiers. */
class DeviceHeaderAdapter : RecyclerView.Adapter<DeviceHeaderAdapter.HeaderViewHolder>() {
    private var status: String = ""
    private var device: DeviceState? = null

    fun submit(status: String, device: DeviceState?) {
        this.status = status
        this.device = device
        notifyItemChanged(0)
    }

    override fun getItemCount(): Int = 1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder =
        HeaderViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false))

    override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
        holder.status.text = status
        holder.device.text = device?.let { deviceText(holder.itemView.resources, it) }.orEmpty()
    }

    class HeaderViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val status: TextView = view.findViewById(R.id.statusText)
        val device: TextView = view.findViewById(R.id.deviceText)
    }

    companion object {
        private const val PAD = 12

        fun deviceText(res: Resources, device: DeviceState): String {
            val unavailable = res.getString(R.string.device_unavailable)
            fun line(label: Int, value: String) = res.getString(label).padEnd(PAD) + value.ifBlank { unavailable }
            return listOf(
                res.getString(R.string.device_title, device.manufacturer, device.model),
                line(R.string.device_label_device, device.device),
                line(R.string.device_label_product, device.product),
                line(R.string.device_label_android, res.getString(R.string.device_android_value, device.androidVersion, device.sdkInt)),
                line(R.string.device_label_patch, device.securityPatch),
                line(R.string.device_label_build, device.buildId),
                line(R.string.device_label_baseband, device.baseband),
                line(R.string.device_label_fingerprint, device.fingerprint),
            ).joinToString("\n")
        }
    }
}
