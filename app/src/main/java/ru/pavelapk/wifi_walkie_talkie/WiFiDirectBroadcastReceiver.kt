package ru.pavelapk.wifi_walkie_talkie

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.NetworkInfo
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.net.wifi.p2p.WifiP2pManager.Channel
import android.os.Parcelable
import android.util.Log
import ru.pavelapk.wifi_walkie_talkie.utils.toast

class WiFiDirectBroadcastReceiver(
    private val manager: WifiP2pManager,
    private val channel: Channel,
    private val activity: WiFiDirectActivity
) : BroadcastReceiver() {

    private val peerListListener = WifiP2pManager.PeerListListener { peerList ->
        val refreshedPeers = peerList.deviceList
        activity.updatePeerList(refreshedPeers.toList())
        if (refreshedPeers.isEmpty()) {
            Log.d(WiFiDirectActivity.TAG, "No devices found")
            return@PeerListListener
        }
    }

    private val connectionListener = WifiP2pManager.ConnectionInfoListener { info ->
        val groupOwnerAddress = info.groupOwnerAddress

        activity.toast("i am owner? ${info.isGroupOwner}, owner: ${groupOwnerAddress.hostAddress}")

        // After the group negotiation, we can determine the group owner
        // (server).
        if (info.groupFormed && info.isGroupOwner) {
            activity.startServer()
        } else if (info.groupFormed) {
            activity.startClient(groupOwnerAddress)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION -> {
                val state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1)
                activity.isWifiP2pEnabled = (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED).also {
                    if (!it) activity.goToStart()
                }

            }
            WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION -> {
                manager.requestPeers(channel, peerListListener)
                Log.d(TAG, "P2P peers changed")
            }
            WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION -> {
                val networkInfo =
                    intent.getParcelableExtra<Parcelable>(WifiP2pManager.EXTRA_NETWORK_INFO) as? NetworkInfo?

                if (networkInfo?.isConnected == true) {
                    manager.requestConnectionInfo(channel, connectionListener)
                } else {
                    activity.goToStart()
                }
            }
            WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION -> {
                (intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE) as? WifiP2pDevice)?.let {
                    activity.updateThisDevice(it)
                }
            }
        }
    }

    companion object {
        val TAG: String = this::class.java.simpleName
    }
}