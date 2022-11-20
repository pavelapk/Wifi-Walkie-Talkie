package ru.pavelapk.wifi_walkie_talkie.utils

import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager

object WifiP2pUtils {

    fun statusToString(status: Int) = when (status) {
        WifiP2pDevice.AVAILABLE -> "AVAILABLE"
        WifiP2pDevice.CONNECTED -> "CONNECTED"
        WifiP2pDevice.FAILED -> "FAILED"
        WifiP2pDevice.INVITED -> "INVITED"
        WifiP2pDevice.UNAVAILABLE -> "UNAVAILABLE"
        else -> status.toString()
    }

    fun errorToString(reasonCode: Int) = when (reasonCode) {
        WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
        WifiP2pManager.ERROR -> "ERROR"
        WifiP2pManager.BUSY -> "BUSY"
        else -> reasonCode.toString()
    }

}