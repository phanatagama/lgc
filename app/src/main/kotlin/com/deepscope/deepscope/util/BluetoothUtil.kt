package com.deepscope.deepscope.util

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.deepscope.deepscope.R
import com.regula.documentreader.api.ble.BLEWrapper
import com.regula.documentreader.api.ble.BleWrapperCallback
import com.regula.documentreader.api.ble.RegulaBleService
import com.regula.documentreader.api.ble.callback.BleManagerCallback
import timber.log.Timber

class BluetoothUtil {
    companion object {
        const val INTENT_REQUEST_ENABLE_LOCATION = 196
        const val INTENT_REQUEST_ENABLE_BLUETOOTH = 197
    }

    fun isBluetoothEnabled(activity: Activity?): Boolean {
        return if (activity == null) {
            false
        } else {
            val bluetoothManager =
                activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            run {
                val adapter = bluetoothManager.adapter
                adapter != null && adapter.isEnabled
            }
        }
    }

    fun isLocationServiceEnabled(activity: Activity?): Boolean {
        return if (activity == null) {
            false
        } else {
            val locationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var gps_enabled = false
            var network_enabled = false
            run {
                try {
                    gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                } catch (var6: Exception) {
                    Timber.e(var6)
                }
                try {
                    network_enabled =
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (var5: Exception) {
                    Timber.e(var5)
                }
                gps_enabled || network_enabled
            }
        }
    }

    fun isPermissionDenied(activity: Activity?, permission: String?): Boolean {
        return if (activity == null) {
            false
        } else {
            com.regula.common.utils.PermissionUtil.isPermissionsDenied(
                activity,
                permission!!
            ) || !com.regula.common.utils.PermissionUtil.isPermissionGranted(
                activity,
                permission
            )
        }
    }

    @SuppressLint("MissingPermission")
    fun requestEnableBluetooth(activity: Activity?) {
        if (activity != null) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activity.startActivityForResult(enableIntent, INTENT_REQUEST_ENABLE_BLUETOOTH)
        }
    }

    fun requestEnableLocationService(activity: Activity?) {
        if (activity != null) {
            val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            activity.startActivityForResult(myIntent, INTENT_REQUEST_ENABLE_LOCATION)
        }
    }

    fun isBluetoothSettingsReady(activity: Activity?): Boolean {
        return if (!isBluetoothEnabled(activity)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && isPermissionDenied(activity, Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                ActivityCompat.requestPermissions(
                    activity!!, arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                    PermissionUtil.PERMISSIONS_BLE_ACCESS
                )
                false
            } else {
                requestEnableBluetooth(activity)
                false
            }
        } else if (!isLocationServiceEnabled(activity)) {
            requestEnableLocationService(activity)
            false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(
                        activity!!,
                        Manifest.permission.BLUETOOTH_SCAN
                    ) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.BLUETOOTH_SCAN),
                        PermissionUtil.PERMISSIONS_BLE_ACCESS
                    )
                    return false
                }
                if (ContextCompat.checkSelfPermission(
                        activity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) !=
                    PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        activity, arrayOf(Manifest.permission.BLUETOOTH_CONNECT),
                        PermissionUtil.PERMISSIONS_BLE_ACCESS
                    )
                    return false
                }
            } else if (ContextCompat.checkSelfPermission(
                    activity!!,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    PermissionUtil.PERMISSIONS_BLE_ACCESS
                )
                return false
            }
            true
        }
    }
}

class BluetoothHelper(val builder: Builder) {
    private val activity: FragmentActivity = builder.activity
    private val serviceListener: OnBluetoothServiceListener? = builder.bluetoothServiceListener
    private val requestActivityResultLauncher : ActivityResultLauncher<Intent> = builder.requestActivityResultLauncher!!
    private val requestPermissionLauncher : ActivityResultLauncher<Array<String>> = builder.requestPermissionLauncher!!
    private var bleManager: BLEWrapper? = null
    var isBleServiceConnected = false
    private val bleManagerCallbacks: BleManagerCallback = object : BleWrapperCallback() {
        override fun onDeviceReady() {
            bleManager!!.removeCallback(this)
            serviceListener?.onDeviceReady()
        }
    }
    private val mBleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Timber.d("[DEBUGX] BleConnection onServiceConnected")
            isBleServiceConnected = true
            val bleService = (service as RegulaBleService.LocalBinder).service
            bleManager = bleService.bleManager

//            handler(7000L).invoke()
            if (bleManager?.isConnected == true) {
                Timber.d("[DEBUGX] bleManager is connected, scanner will be shown")
                serviceListener?.onBluetoothServiceConnected()
                return
            }
            Timber.d("[DEBUGX] bleManager is not connected")

            serviceListener?.onShowDialog(activity.getString(R.string.searching_devices))
            bleManager.let {
                it!!.addCallback(bleManagerCallbacks)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBleServiceConnected = false
            serviceListener?.onBluetoothServiceDisconnected()
        }
    }

    interface OnBluetoothServiceListener {
        fun onBluetoothServiceConnected()
        fun onBluetoothServiceDisconnected()
        fun onDeviceReady()
        fun onBluetoothSettingsReady()
        fun onPermissionGranted()
        fun onShowPermissionDialog()
        fun onShowDialog(message: String)
    }

    class Builder(val activity: FragmentActivity) {
        var bluetoothServiceListener: OnBluetoothServiceListener? = null
        var requestPermissionLauncher : ActivityResultLauncher<Array<String>>? = null
        var requestActivityResultLauncher : ActivityResultLauncher<Intent>? = null

        fun setBluetoothServiceListener(listener: OnBluetoothServiceListener): Builder {
            bluetoothServiceListener = listener
            return this
        }

        fun setRequestPermissionLauncher(
            requestPermissionLauncher: ActivityResultLauncher<Array<String>>
        ): Builder {
            this.requestPermissionLauncher = requestPermissionLauncher
            return this
        }

        fun setRequestActivityResultLauncher(
            requestActivityResultLauncher: ActivityResultLauncher<Intent>
        ): Builder {
            this.requestActivityResultLauncher = requestActivityResultLauncher
            return this
        }

        fun build(): BluetoothHelper {
            return BluetoothHelper(this)
        }
    }

    fun startBluetoothService() {
        if (!isBluetoothSettingsReady() || isBleServiceConnected) {
            return
        }
        Timber.d("[DEBUGX] startBluetoothService")
        val bleIntent = Intent(activity, RegulaBleService::class.java)
        activity.startService(bleIntent)
        activity.bindService(bleIntent, mBleConnection, 0)
    }

    private fun isBluetoothEnabled(): Boolean {
        val bluetoothManager =
            activity.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter
        return adapter != null && adapter.isEnabled
    }

    fun isLocationServiceEnabled(): Boolean {
            val locationManager =
                activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var gps_enabled = false
            var network_enabled = false
            return run {
                try {
                    gps_enabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                } catch (var6: Exception) {
                    Timber.e(var6)
                }
                try {
                    network_enabled =
                        locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
                } catch (var5: Exception) {
                    Timber.e(var5)
                }
                gps_enabled || network_enabled
            }

    }

    private fun isPermissionDenied(permission: String?): Boolean {
        return com.regula.common.utils.PermissionUtil.isPermissionsDenied(
            activity,
            permission!!
        ) || !com.regula.common.utils.PermissionUtil.isPermissionGranted(
            activity,
            permission
        )
    }
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            activity, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun isBluetoothSettingsReady(): Boolean {
        return if (!isBluetoothEnabled()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && isPermissionDenied( Manifest.permission.BLUETOOTH_CONNECT)
            ) {
                requestPermissionLauncher.launch(
                    REQUIRED_PERMISSIONS
                )
                false
            } else {
                requestActivityResultLauncher.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                )
                false
            }
        } else if (!isLocationServiceEnabled()) {
            requestActivityResultLauncher.launch(
                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            )
            false
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!allPermissionsGranted()) {
                    requestPermissionLauncher.launch(
                        REQUIRED_PERMISSIONS
                    )
                    return false
                }
            } else if (!allPermissionsGranted()) {
                requestPermissionLauncher.launch(
                    REQUIRED_PERMISSIONS
                )
                return false
            }
            true
        }
    }
    fun unbindService() {
        if (isBleServiceConnected) {
            activity.unbindService(mBleConnection)
            isBleServiceConnected = false
        }
    }

    companion object {
        const val INTENT_REQUEST_ENABLE_LOCATION = 196
        const val INTENT_REQUEST_ENABLE_BLUETOOTH = 197
        private val REQUIRED_PERMISSIONS =
            mutableListOf<String>(
            ).apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    add(Manifest.permission.BLUETOOTH_CONNECT)
                    add(Manifest.permission.BLUETOOTH_SCAN)
                } else {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }.toTypedArray()
    }
}