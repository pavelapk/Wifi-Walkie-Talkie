package ru.pavelapk.wifi_walkie_talkie.adapter

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.pavelapk.wifi_walkie_talkie.R
import ru.pavelapk.wifi_walkie_talkie.databinding.ItemPeerBinding
import ru.pavelapk.wifi_walkie_talkie.utils.WifiP2pUtils.statusToString

class WifiPeersAdapter(
    private val listener: WifiPeersListener
) : ListAdapter<WifiP2pDevice, WifiPeersAdapter.PeerViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PeerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_peer, parent, false)
        return PeerViewHolder(view)
    }

    override fun onBindViewHolder(holder: PeerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PeerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemPeerBinding.bind(view)

        init {
            binding.root.setOnClickListener {
                listener.onPeerClick(getItem(bindingAdapterPosition))
            }
        }

        fun bind(peer: WifiP2pDevice) = with(binding) {
            tvName.text = "${peer.deviceName} (${peer.deviceAddress})"
            tvStatus.text =
                "${statusToString(peer.status)}, ${peer.primaryDeviceType}, ${peer.secondaryDeviceType}"
        }
    }

    fun interface WifiPeersListener {
        fun onPeerClick(peer: WifiP2pDevice)
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<WifiP2pDevice>() {
            override fun areItemsTheSame(oldItem: WifiP2pDevice, newItem: WifiP2pDevice) =
                oldItem.deviceAddress == newItem.deviceAddress

            override fun areContentsTheSame(oldItem: WifiP2pDevice, newItem: WifiP2pDevice) =
                oldItem == newItem
        }
    }
}
