package ru.pavelapk.wifi_walkie_talkie.utils

import android.net.wifi.p2p.WifiP2pDevice

object WifiP2pDeviceUtils {

    fun statusToString(status: Int) = when (status) {
        WifiP2pDevice.AVAILABLE -> "AVAILABLE"
        WifiP2pDevice.CONNECTED -> "CONNECTED"
        WifiP2pDevice.FAILED -> "FAILED"
        WifiP2pDevice.INVITED -> "INVITED"
        WifiP2pDevice.UNAVAILABLE -> "UNAVAILABLE"
        else -> status.toString()
    }

}