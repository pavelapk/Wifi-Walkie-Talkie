package ru.pavelapk.wifi_walkie_talkie

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.net.wifi.WpsInfo
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import ru.pavelapk.wifi_walkie_talkie.adapter.WifiPeersAdapter
import ru.pavelapk.wifi_walkie_talkie.databinding.ActivityMainBinding
import ru.pavelapk.wifi_walkie_talkie.utils.WifiP2pDeviceUtils.statusToString
import ru.pavelapk.wifi_walkie_talkie.utils.toast
import ru.pavelapk.wifi_walkie_talkie.utils.toastLong

class WiFiDirectActivity : AppCompatActivity(), WifiP2pManager.ChannelListener {
    private lateinit var binding: ActivityMainBinding

    var isWifiP2pEnabled = false
        set(value) {
            field = value
            binding.tvP2pStatus.text = "WifiP2P: $field"
            if (!field) {
                updatePeerList(listOf())
            }
        }
    private var retryChannel = false
    private val intentFilter = IntentFilter()
    private var receiver: BroadcastReceiver? = null

    private lateinit var channel: WifiP2pManager.Channel
    private lateinit var manager: WifiP2pManager
    private lateinit var wifiManager: WifiManager

    private val wifiPeersAdapter = WifiPeersAdapter { device ->
        connect(device)
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                discover()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                toastLong("Для работы программы нужно разрешение на определения ТОЧНОГО местоположения")
            }
            else -> {
                toastLong("Для работы программы нужно разрешение на определения местоположения")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Indicates a change in the Wi-Fi Direct status.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)

        // Indicates a change in the list of available peers.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)

        // Indicates the state of Wi-Fi Direct connectivity has changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)

        // Indicates this device's details have changed.
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)

        if (!initP2p()) {
            finish()
        }

        binding.recyclerPeers.adapter = wifiPeersAdapter

        binding.btnStartDiscovery.setOnClickListener {
            locationPermissionRequest.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        receiver = WiFiDirectBroadcastReceiver(manager, channel, this)
        registerReceiver(receiver, intentFilter)
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(receiver)
    }

    fun updateThisDevice(wifiP2pDevice: WifiP2pDevice) {
        with(binding) {
            tvName.text = "${wifiP2pDevice.deviceName} (${wifiP2pDevice.deviceAddress})"
            tvStatus.text =
                "${statusToString(wifiP2pDevice.status)}, ${wifiP2pDevice.primaryDeviceType}, ${wifiP2pDevice.secondaryDeviceType}"
        }
    }

    fun updatePeerList(list: List<WifiP2pDevice>) {
        wifiPeersAdapter.submitList(list)
    }

    private fun connect(device: WifiP2pDevice) {
        retryChannel = false

        val config = WifiP2pConfig().apply {
            deviceAddress = device.deviceAddress
            wps.setup = WpsInfo.PBC
        }

        manager.connect(channel, config, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                // WiFiDirectBroadcastReceiver notifies us. Ignore for now.
            }

            override fun onFailure(reason: Int) {
                toast("Connect failed. Retry.")
            }
        })
    }

    private fun discover() {
        if (!isWifiP2pEnabled) {
            toast("Enable Wifi")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                startActivity(panelIntent)
            } else {
                wifiManager.isWifiEnabled = true
            }
            return
        }

        manager.discoverPeers(channel, object : WifiP2pManager.ActionListener {
            override fun onSuccess() {
                toast("Discovery Initiated")
            }

            override fun onFailure(reasonCode: Int) {
                val error = when (reasonCode) {
                    WifiP2pManager.P2P_UNSUPPORTED -> "P2P_UNSUPPORTED"
                    WifiP2pManager.ERROR -> "ERROR"
                    WifiP2pManager.BUSY -> "BUSY"
                    else -> reasonCode.toString()
                }
                toastLong("Discovery Failed : $error")
            }
        })
    }

    private fun initP2p(): Boolean {
        // Device capability definition check
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_WIFI_DIRECT)) {
            toastLong("Wi-Fi Direct is not supported by this device.")
            return false
        }
        // Hardware capability check
        val wifiManager = getSystemService(WIFI_SERVICE) as? WifiManager
        if (wifiManager == null) {
            toastLong("Cannot get Wi-Fi system service.")
            return false
        }
        this.wifiManager = wifiManager
        if (!wifiManager.isP2pSupported && wifiManager.isWifiEnabled) {
            toastLong("Wi-Fi Direct is not supported by the hardware")
            return false
        }
        val wifiP2pManager = getSystemService(WIFI_P2P_SERVICE) as? WifiP2pManager
        if (wifiP2pManager == null) {
            toastLong("Cannot get Wi-Fi Direct system service.")
            return false
        }
        manager = wifiP2pManager

        val channel = manager.initialize(this, mainLooper, null)
        if (channel == null) {
            Log.e(TAG, "Cannot initialize Wi-Fi Direct.")
            return false
        }
        this.channel = channel

        return true
    }

    override fun onChannelDisconnected() {
        if (!retryChannel) {
            toast("Channel lost. Trying again")
            updatePeerList(listOf())
            retryChannel = true
            manager.initialize(this, mainLooper, this)
        } else {
            toastLong("Severe! Channel is probably lost premanently. Try Disable/Re-Enable P2P.")
        }
    }

    companion object {
        val TAG: String = this::class.java.simpleName
    }
}