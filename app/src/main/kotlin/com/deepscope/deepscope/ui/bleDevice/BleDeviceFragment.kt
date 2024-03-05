package com.deepscope.deepscope.ui.bleDevice

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.deepscope.deepscope.R
import com.deepscope.deepscope.databinding.FragmentBleDeviceBinding
import com.deepscope.deepscope.ui.common.AppDocReaderSDK
import com.deepscope.deepscope.ui.customerInformation.CustomerInformationActivity
import com.deepscope.deepscope.util.BluetoothHelper
import com.deepscope.deepscope.util.Utils
import com.deepscope.deepscope.util.Utils.REGULA_0326
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.params.Functionality
import com.regula.documentreader.api.results.DocumentReaderResults
import timber.log.Timber

class BleDeviceFragment : AppDocReaderSDK() {
    private var _binding: FragmentBleDeviceBinding? = null
    private val binding get() = _binding!!
    private val args: BleDeviceFragmentArgs by navArgs()
    private var etDeviceName: EditText? = null
    private var btnConnect: Button? = null
    private lateinit var bluetoothHelper: BluetoothHelper
    private val requestActivityResultLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (bluetoothHelper.isLocationServiceEnabled()) {
                if (result.resultCode == RESULT_OK) {
                    if (bluetoothHelper.isBluetoothSettingsReady()) {
                        Timber.d("LocationService & BluetoothService has been enabled")
                        initializeReader()
                    }
                }
            }
        }
    private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (permissionGranted) {
                if (bluetoothHelper.isBluetoothSettingsReady()) {
                    Timber.d(
                        "[DEBUGX] Bluetooth and Location permission has been granted & BluetoothSetting is ready"
                    )
                    btnConnect?.callOnClick()
                    binding.btnConnect.callOnClick()
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !requireActivity().shouldShowRequestPermissionRationale(permissions.keys.first())
            ) {
                showPermissionDialog()
            }
        }
    private val bluetoothServiceListener: BluetoothHelper.OnBluetoothServiceListener =
        object : BluetoothHelper.OnBluetoothServiceListener {
            override fun onBluetoothServiceConnected() {
                showToast(getString(R.string.bluetooth_is_connected))
                showScanner()
            }

            override fun onBluetoothServiceDisconnected() {
                showToast("BleService is disconnected")
            }

            override fun onDeviceReady() {
                showToast(getString(R.string.bluetooth_is_connected))
                dismissDialog()
                showScanner()
            }

            override fun onBluetoothSettingsReady() {
                initializeReader()
            }

            override fun onPermissionGranted() {
                btnConnect?.callOnClick()
                binding.btnConnect.callOnClick()
            }

            override fun onShowPermissionDialog() {
                showPermissionDialog()
            }

            override fun onShowDialog(message: String) {
                showDialog(message)
            }
        }

    override fun setupFunctionality() {
        documentReaderInstance.processParams().timeout = Double.MAX_VALUE
        documentReaderInstance.processParams().timeoutFromFirstDetect = Double.MAX_VALUE
        documentReaderInstance.processParams().timeoutFromFirstDocType = Double.MAX_VALUE
        documentReaderInstance.processParams().setLogs(true)
        documentReaderInstance.functionality().edit()
            .setBtDeviceName(REGULA_0326)
            .setShowCaptureButton(true)
            .setShowCaptureButtonDelayFromStart(0)
            .setShowCaptureButtonDelayFromDetect(0)
            .setCaptureMode(CaptureMode.AUTO)
            .setShowTorchButton(true)
            .setDisplayMetadata(true)
            .apply()
    }

    override fun onDocReaderInitComplete() {
        btnConnect?.isEnabled = documentReaderInstance.isReady
    }

    override fun onDocReaderCompleteScan(results: DocumentReaderResults) {
        CustomerInformationActivity.documentResults = results
        goToCustomerInformation()
    }

    private var runnable: Runnable? = null
    private fun goToCustomerInformation() {
        val directions =
            BleDeviceFragmentDirections.actionBleDeviceFragmentToCustomerInformationFragment(
                customerInformationFeature = args.customerInformationFeature,
                customerInformationType = 1,
            )
        if (!isResumed) {
            runnable = Runnable {
                findNavController().navigate(directions)
                runnable = null
            }
        } else {
            findNavController().navigate(directions)
        }
    }

    override fun onResume() {
        super.onResume()
        runnable?.run()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentBleDeviceBinding.inflate(inflater, container, false)
        Utils.resetFunctionality(Functionality())
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        prepareDatabase()
        setupFunctionality()
        initViews()
        bluetoothHelper = BluetoothHelper.Builder(requireActivity())
            .setBluetoothServiceListener(bluetoothServiceListener)
            .setRequestPermissionLauncher(requestPermissionLauncher)
            .setRequestActivityResultLauncher(requestActivityResultLauncher)
            .build()
    }

    private fun initViews(view: View) {
        etDeviceName = view.findViewById(R.id.ed_device)
        btnConnect = view.findViewById(R.id.btn_connect)
    }

    private fun initViews() {
        etDeviceName?.setText(documentReaderInstance.functionality().btDeviceName)
        btnConnect?.setOnClickListener { _: View? ->
            if (etDeviceName?.text != null) {
                showDialog(getString(R.string.searching_devices))
                documentReaderInstance.functionality().edit()
                    .setUseAuthenticator(true)
                    .setBtDeviceName(etDeviceName?.text.toString()).apply()
                bluetoothHelper.startBluetoothService()
            }
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(requireActivity())
            .setTitle("Permissions denied")
            .setMessage("Permissions denied for app. Open settings to provide permissions.")
            .setNegativeButton("cancel", null)
            .setPositiveButton(
                "Settings"
            ) { _, _ ->
                goToSettings()
            }
            .create()
            .show()
    }

    private fun goToSettings() {
        Timber.d("[DEBUGX] goToSettings")
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri = Uri.fromParts("package", requireActivity().packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()
        bluetoothHelper.unbindService()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
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

//    private val bluetoothUtil = BluetoothUtil()
//    private var bleManager: BLEWrapper? = null
//    private var isBleServiceConnected = false
//    private val mBleConnection: ServiceConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName, service: IBinder) {
//            Timber.d("[DEBUGX] BleConnection onServiceConnected")
//            isBleServiceConnected = true
//            val bleService = (service as RegulaBleService.LocalBinder).service
//            bleManager = bleService.bleManager
//
////            handler(7000L).invoke()
//            if (bleManager?.isConnected == true) {
//                Timber.d("[DEBUGX] bleManager is connected, scanner will be shown")
//                showToast(getString(R.string.bluetooth_is_connected))
//                showScanner()
//                return
//            }
//            Timber.d("[DEBUGX] bleManager is not connected")
//
//            showDialog(getString(R.string.searching_devices))
//            bleManager.let {
//                it!!.addCallback(bleManagerCallbacks)
//            }
//        }
//
//        override fun onServiceDisconnected(name: ComponentName) {
//            isBleServiceConnected = false
//            showToast("BleService is disconnected")
//        }
//    }

//    private val bleManagerCallbacks: BleManagerCallback = object : BleWrapperCallback() {
//        override fun onDeviceReady() {
//            bleManager!!.removeCallback(this)
//            showToast(getString(R.string.bluetooth_is_connected))
//            dismissDialog()
//            showScanner()
//        }
//    }

//    private fun startBluetoothService() {
//        if (!isBluetoothSettingsReady(requireActivity()) || isBleServiceConnected) {
//            return
//        }
//        Timber.d("[DEBUGX] startBluetoothService")
//        val bleIntent = Intent(requireActivity(), RegulaBleService::class.java)
//        requireActivity().startService(bleIntent)
//        requireActivity().bindService(bleIntent, mBleConnection, 0)
//    }

//    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
//        ContextCompat.checkSelfPermission(
//            requireActivity(), it
//        ) == PackageManager.PERMISSION_GRANTED
//    }

//    private fun isBluetoothSettingsReady(activity: Activity?): Boolean {
//        return if (!bluetoothUtil.isBluetoothEnabled(activity)) {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
//                && bluetoothUtil.isPermissionDenied(activity, Manifest.permission.BLUETOOTH_CONNECT)
//            ) {
//                requestPermissionLauncher.launch(
//                    REQUIRED_PERMISSIONS
//                )
//                false
//            } else {
//                requestActivityResultLauncher.launch(
//                    Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
//                )
//                false
//            }
//        } else if (!bluetoothUtil.isLocationServiceEnabled(activity)) {
//            requestActivityResultLauncher.launch(
//                Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
//            )
//            false
//        } else {
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
//                if (!allPermissionsGranted()) {
//                    requestPermissionLauncher.launch(
//                        REQUIRED_PERMISSIONS
//                    )
//                    return false
//                }
//            } else if (!allPermissionsGranted()) {
//                requestPermissionLauncher.launch(
//                    REQUIRED_PERMISSIONS
//                )
//                return false
//            }
//            true
//        }
//    }
}