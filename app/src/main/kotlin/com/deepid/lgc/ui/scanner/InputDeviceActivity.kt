package com.deepid.lgc.ui.scanner

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.deepid.lgc.R
import com.deepid.lgc.databinding.ActivityInputDeviceBinding
import com.deepid.lgc.ui.BaseRegulaSdkActivity
import com.deepid.lgc.ui.customerInformation.CustomerInformationActivity
import com.deepid.lgc.ui.result.ScanResultActivity
import com.deepid.lgc.util.BluetoothUtil
import com.deepid.lgc.util.PermissionUtil
import com.deepid.lgc.util.PermissionUtil.Companion.respondToPermissionRequest
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.ble.BLEWrapper
import com.regula.documentreader.api.ble.BleWrapperCallback
import com.regula.documentreader.api.ble.RegulaBleService
import com.regula.documentreader.api.ble.callback.BleManagerCallback
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.callback.FaceCaptureCallback
import com.regula.facesdk.model.results.FaceCaptureResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.androidx.viewmodel.ext.android.viewModel


class InputDeviceActivity : BaseRegulaSdkActivity() {
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false

    private lateinit var binding: ActivityInputDeviceBinding

    private var etDeviceName: EditText? = null
    private var btnConnect: Button? = null

    private val scannerViewModel: ScannerViewModel by viewModel()
    private val bluetoothUtil = BluetoothUtil()

    @Transient
    override val completion: IDocumentReaderCompletion =
        IDocumentReaderCompletion { action, results, error ->
            if (action == DocReaderAction.COMPLETE
                || action == DocReaderAction.TIMEOUT
            ) {
                dismissDialog()
                if (results != null) {
                    scannerViewModel.setDocumentReaderResults(results)
                    CustomerInformationActivity.documentResults = results
                }
                if (DocumentReader.Instance().functionality().isManualMultipageMode) {
                    Log.d(TAG, "[DEBUGX] MULTIPAGEMODE: ")
                    if (results?.morePagesAvailable != 0) {
                        DocumentReader.Instance().startNewPage()
                        lifecycleScope.launch {
                            delay(100)
                            withContext(Dispatchers.Main) {
//                            showScanner()
                            }
                        }
                        return@IDocumentReaderCompletion
                    } else {
                        DocumentReader.Instance().functionality().edit()
                            .setManualMultipageMode(false)
                            .apply()
                    }
                }
                if (results?.chipPage != 0) {
                    Log.d(TAG, "DEBUGX RFID IS PERFORMED: ")
                    DocumentReader.Instance()
                        .startRFIDReader(this, object : IRfidReaderCompletion() {
                            override fun onChipDetected() {
                                Log.d("Rfid", "Chip detected")
                            }

                            override fun onProgress(notification: DocumentReaderNotification) {
//                        rfidProgress(notification.code, notification.value)
                            }

                            override fun onRetryReadChip(exception: DocReaderRfidException) {
                                Log.d("Rfid", "Retry with error: " + exception.errorCode)
                            }

                            override fun onCompleted(
                                rfidAction: Int,
                                results_RFIDReader: DocumentReaderResults?,
                                error: DocumentReaderException?
                            ) {
                                if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL) {
                                    scannerViewModel.setDocumentReaderResults(
                                        results_RFIDReader ?: results
                                    )
                                    captureFace()
                                    displayResults()
                                }
                            }
                        })
                } else {
                    Log.d(TAG, "[DEBUGX] NO RFID PERFORMED ")
                    /**
                     * perform @livenessFace or @captureFace then check similarity
                     */
                    displayResults()
                }
            } else {
                dismissDialog()
                if (action == DocReaderAction.CANCEL) {
                    if (DocumentReader.Instance().functionality().isManualMultipageMode)
                        DocumentReader.Instance().functionality().edit()
                            .setManualMultipageMode(false)
                            .apply()

                    Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
                } else if (action == DocReaderAction.ERROR) {
                    Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
                }
            }
        }

    override val faceCaptureCallback: FaceCaptureCallback =
        FaceCaptureCallback { response: FaceCaptureResponse ->
            scannerViewModel.setFaceCaptureResponse(response)
            ScanResultActivity.faceCaptureResponse = response
            // ... check response.image for capture result
            if (response.image?.bitmap == null) {
                response.exception?.message?.let {
                    Toast.makeText(
                        this@InputDeviceActivity,
                        "Error: $it",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            displayResults()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityInputDeviceBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        initViews()
        //observe()
//        prepareDatabase()
//        setupFunctionality()
        etDeviceName?.setText(DocumentReader.Instance().functionality().btDeviceName)
        btnConnect?.setOnClickListener { _: View? ->
            if (etDeviceName?.text != null) {
                showDialog("Searching devices")
//                handler.sendEmptyMessageDelayed(0, 7000)
                handler(7000L).invoke()
                DocumentReader.Instance().functionality().edit()
                    .setUseAuthenticator(true)
                    .setBtDeviceName(etDeviceName?.text.toString()).apply()
                startBluetoothService()
            }
        }
    }

    override fun setupFunctionality() {
        DocumentReader.Instance().processParams().timeout = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDetect = Double.MAX_VALUE
        DocumentReader.Instance().processParams().timeoutFromFirstDocType = Double.MAX_VALUE
        DocumentReader.Instance().processParams().setLogs(true)
        DocumentReader.Instance().functionality().edit()
            .setBtDeviceName("Regula 0326")
            .setShowCaptureButton(true)
            .setShowCaptureButtonDelayFromStart(0)
            .setShowCaptureButtonDelayFromDetect(0)
            .setCaptureMode(CaptureMode.AUTO)
            .setShowTorchButton(true)
            .setDisplayMetadata(true)
            .apply()
    }


    private fun displayResults() {
        val formIntent = Intent(this, CustomerInformationActivity::class.java)
        formIntent.putExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_TYPE, 1)
        formIntent.putExtra(
            CustomerInformationActivity.CUSTOMER_INFORMATION_FEATURE,
            intent.getIntExtra(CustomerInformationActivity.CUSTOMER_INFORMATION_FEATURE, 2)
        )
        startActivity(formIntent)
    }

    private fun initViews() {
        etDeviceName = findViewById(R.id.ed_device)
        btnConnect = findViewById(R.id.btn_connect)
    }


    override val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                Log.d(TAG, "[DEBUGX] init reader DocumentSDK is complete")
                btnConnect?.isEnabled = true
            } else {
                Toast.makeText(this@InputDeviceActivity, "Init failed:$error", Toast.LENGTH_LONG)
                    .show()
                return@IDocumentReaderInitCompletion
            }
        }

    private fun startBluetoothService() {
        if (!bluetoothUtil.isBluetoothSettingsReady(this) || isBleServiceConnected) {
            return
        }
        Log.d(TAG, "[DEBUGX] startBluetoothService")
        val bleIntent = Intent(this, RegulaBleService::class.java)
        startService(bleIntent)
        bindService(bleIntent, mBleConnection, 0)
    }

    private val mBleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.d(TAG, "[DEBUGX] onServiceConnected")
            isBleServiceConnected = true
            val bleService = (service as RegulaBleService.LocalBinder).service
            bleManager = bleService.bleManager

            if (bleManager?.isConnected == true) {
                Log.d(
                    TAG,
                    "[DEBUGX] bleManager is connected, then intent to SuccessfulInitActivity"
                )
                Toast.makeText(
                    this@InputDeviceActivity,
                    "Bluetooth is connected",
                    Toast.LENGTH_SHORT
                )
                    .show()
//                startActivity(Intent(this@InputDeviceActivity, SuccessfulInitActivity::class.java))
                showScanner()
                return
            }
            Log.d(TAG, "[DEBUGX] bleManager is not connected")

            showDialog("Searching devices")
//            handler.sendEmptyMessageDelayed(0, 7000)
            handler(7000L).invoke()
            bleManager.let {
                it!!.addCallback(bleManagerCallbacks)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBleServiceConnected = false
        }
    }
//    private val handler = Handler { _: Message? ->
//        Toast.makeText(this, "Failed to connect to the torch device", Toast.LENGTH_SHORT).show()
//        dismissDialog()
//        false
//    }


    private val bleManagerCallbacks: BleManagerCallback = object : BleWrapperCallback() {
        override fun onDeviceReady() {
//            handler.removeMessages(0)
            bleManager!!.removeCallback(this)
            Toast.makeText(this@InputDeviceActivity, "Bluetooth is connected", Toast.LENGTH_SHORT)
                .show()
//            startActivity(Intent(this@InputDeviceActivity, SuccessfulInitActivity::class.java))
            dismissDialog()
            showScanner()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isBleServiceConnected) {
            unbindService(mBleConnection)
            isBleServiceConnected = false
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtil.PERMISSIONS_BLE_ACCESS) {

            if (permissions.isEmpty())
                return

            respondToPermissionRequest(this,
                permissions[0],
                grantResults,
                permissionGrantedFunc = {
                    if (bluetoothUtil.isBluetoothSettingsReady(this)) {
                        Log.d(
                            TAG,
                            "[DEBUGX] respondToPermissionRequest is Granted & BluetoothSettingReady"
                        )
                        btnConnect?.callOnClick()
                        binding.btnConnect.callOnClick()
                    }

                },
                permissionRejectedFunc = {

                })
        }
    }

    override fun onActivityResult(requestCode: Int, rc: Int, data: Intent?) {
        super.onActivityResult(requestCode, rc, data)
        var resultCode = rc
        if (requestCode == BluetoothUtil.INTENT_REQUEST_ENABLE_LOCATION)
            resultCode = if (bluetoothUtil.isLocationServiceEnabled(this)) RESULT_OK
            else requestCode
        if (requestCode == BluetoothUtil.INTENT_REQUEST_ENABLE_BLUETOOTH or BluetoothUtil.INTENT_REQUEST_ENABLE_LOCATION)
            if (resultCode == RESULT_OK) {
                if (bluetoothUtil.isBluetoothSettingsReady(this)) {
                    Log.d(
                        TAG,
                        "[DEBUGX] onActivityResult BluetoothSettingReady is True, then initializeReader"
                    )
                    initializeReader()
                }
            }
    }

    companion object {
        const val TAG: String = "InputDeviceActivity"
    }
}