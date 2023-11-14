package com.deepid.lgc

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.deepid.lgc.ui.defaultscanner.DefaultScannerActivity
import com.deepid.lgc.ui.scanner.ScannerUiState
import com.deepid.lgc.ui.scanner.ScannerViewModel
import com.deepid.lgc.util.BluetoothUtil
import com.deepid.lgc.util.PermissionUtil
import com.deepid.lgc.util.PermissionUtil.Companion.respondToPermissionRequest
import com.deepid.lgc.util.Utils
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.ble.BLEWrapper
import com.regula.documentreader.api.ble.BleWrapperCallback
import com.regula.documentreader.api.ble.RegulaBleService
import com.regula.documentreader.api.ble.callback.BleManagerCallback
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.exception.InitException
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false
    private var loadingDialog: AlertDialog? = null

    var etDeviceName: EditText? = null
    var btnConnect: Button? = null
    var btnScan: Button? = null
    var ivTest: ImageView? = null

    private val scannerViewModel: ScannerViewModel by viewModel()

    private val bluetoothUtil = BluetoothUtil()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        //observe()
        initFaceSDK()
        prepareDatabase()
        DocumentReader.Instance().functionality().edit().setBtDeviceName("Regula 0326").apply()
        etDeviceName?.setText(DocumentReader.Instance().functionality().btDeviceName)
        btnConnect?.setOnClickListener { view: View? ->
            if (etDeviceName?.text != null) {
                showDialog("Searching devices")
                handler.sendEmptyMessageDelayed(0, 7000)
                DocumentReader.Instance().functionality().edit()
                    .setUseAuthenticator(true)
                    .setBtDeviceName(etDeviceName?.text.toString()).apply()
                Log.d("MainActivity", "[DEBUGX] btnClicked ")
                startBluetoothService()
            }
        }
    }

    private fun initFaceSDK() {
        FaceSDK.Instance().init(this) { status: Boolean, e: InitException? ->
            if (!status) {
                Toast.makeText(
                    this@MainActivity,
                    "Init FaceSDK finished with error: " + if (e != null) e.message else "",
                    Toast.LENGTH_LONG
                ).show()
                return@init
            }
            Log.d("MainActivity", "FaceSDK init completed successfully")
        }
    }

    private fun observe() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                scannerViewModel.state.collect { uiState ->
                    handleStateChange(uiState)
                }
            }
        }
    }

    private fun getBitmap(): Bitmap {
        return assets
            .open("filename.png")
            .use(BitmapFactory::decodeStream)
    }

    private fun handleStateChange(uiState: ScannerUiState) {
        when (uiState) {
            is ScannerUiState.Init -> Unit
            is ScannerUiState.Loading -> Toast.makeText(
                this,
                "Loading: ${uiState.isLoading}",
                Toast.LENGTH_LONG
            ).show()

            is ScannerUiState.Error -> Toast.makeText(
                this,
                "Error ${uiState.message}",
                Toast.LENGTH_LONG
            ).show()

            is ScannerUiState.Success -> Toast.makeText(this, "Success", Toast.LENGTH_LONG).show()
        }

    }

    private fun prepareDatabase() {
        showDialog("preparing database")
        DocumentReader.Instance()
            .prepareDatabase(//call prepareDatabase not necessary if you have local database at assets/Regula/db.dat
                this@MainActivity,
                "FullAuth",
                object : IDocumentReaderPrepareCompletion {
                    override fun onPrepareProgressChanged(progress: Int) {
                        if (loadingDialog != null)
                            loadingDialog?.setTitle("Downloading database: $progress%")
                    }

                    override fun onPrepareCompleted(
                        status: Boolean,
                        error: DocumentReaderException?
                    ) {
                        if (status) {
                            initializeReader()
                        } else {
                            dismissDialog()
                            Toast.makeText(
                                this@MainActivity,
                                "Prepare DB failed:$error",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                })
    }

    private fun initViews() {
        etDeviceName = findViewById(R.id.ed_device)
        btnConnect = findViewById(R.id.btn_connect)
//        ivTest = findViewById(R.id.iv_test)
//        ivTest?.setImageBitmap(getBitmap())
        btnScan = findViewById(R.id.btn_scan)
        btnScan?.setOnClickListener {
            startActivity(Intent(this@MainActivity, DefaultScannerActivity::class.java))
        }
    }

    fun initializeReader() {
        val license = Utils.getLicense(this) ?: return
        showDialog("Initializing")

        DocumentReader.Instance()
            .initializeReader(this@MainActivity, DocReaderConfig(license), initCompletion)
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                btnConnect?.isEnabled = true
            } else {
                Log.e("MainActivity", "[DEBUG] INIT failed: $error ")
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
                return@IDocumentReaderInitCompletion
            }
        }

    private fun startBluetoothService() {
        if (!bluetoothUtil.isBluetoothSettingsReady(this) || isBleServiceConnected) {
            return
        }
        Log.d("MainActivity", "[DEBUGX] startBluetoothService")
        val bleIntent = Intent(this, RegulaBleService::class.java)
        startService(bleIntent)
        bindService(bleIntent, mBleConnection, 0)
    }

    private val mBleConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            isBleServiceConnected = true
            val bleService = (service as RegulaBleService.LocalBinder).service
            bleManager = bleService.bleManager
            Log.d("MainActivity", "[DEBUGX] onServiceConnected")

            if (bleManager?.isConnected == true) {
//                startActivity(Intent(this@MainActivity, SuccessfulInitActivity::class.java))
                Toast.makeText(this@MainActivity, "Bluetooth is connected", Toast.LENGTH_SHORT).show()
                return
            }
            Log.d("MainActivity", "[DEBUGX] onServiceConnected 2")

            showDialog("Searching devices")
            handler.sendEmptyMessageDelayed(0, 7000)
            bleManager.let {
                it!!.addCallback(bleManagerCallbacks)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            isBleServiceConnected = false
        }
    }
    private val handler = Handler { msg: Message? ->
        Toast.makeText(this, "Failed to connect to the torch device", Toast.LENGTH_SHORT).show()
        dismissDialog()
        false
    }

    private val bleManagerCallbacks: BleManagerCallback = object : BleWrapperCallback() {
        override fun onDeviceReady() {
            Log.d("MainActivity", "[DEBUGX] onDeviceReady")
            handler.removeMessages(0)
            bleManager!!.removeCallback(this)
//            startActivity(Intent(this@MainActivity, SuccessfulInitActivity::class.java))
            Toast.makeText(this@MainActivity, "Bluetooth is connected", Toast.LENGTH_SHORT).show()
            dismissDialog()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isBleServiceConnected) {
            unbindService(mBleConnection)
            isBleServiceConnected = false
        }
    }

    private fun dismissDialog() {
        if (loadingDialog != null) {
            loadingDialog!!.dismiss()
        }
    }

    fun showDialog(msg: String?) {
        dismissDialog()
        val builderDialog = AlertDialog.Builder(this)
        val dialogView = layoutInflater.inflate(R.layout.simple_dialog, null)
        builderDialog.setTitle(msg)
        builderDialog.setView(dialogView)
        builderDialog.setCancelable(false)
        loadingDialog = builderDialog.show()
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
                    if (bluetoothUtil.isBluetoothSettingsReady(this))
                        btnConnect?.callOnClick()
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
                if (bluetoothUtil.isBluetoothSettingsReady(this))
                    initializeReader()
            }
    }

    companion object {
        val TAG: String = this.javaClass.name
    }
}