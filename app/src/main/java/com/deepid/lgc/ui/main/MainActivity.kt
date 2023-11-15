package com.deepid.lgc.ui.main

import android.Manifest
import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.lgc.R
import com.deepid.lgc.databinding.ActivityMainBinding
import com.deepid.lgc.ui.common.RecyclerAdapter
import com.deepid.lgc.ui.defaultscanner.DefaultScannerActivity
import com.deepid.lgc.ui.scanner.ScannerUiState
import com.deepid.lgc.ui.scanner.ScannerViewModel
import com.deepid.lgc.ui.scanner.SuccessfulInitActivity
import com.deepid.lgc.util.Base
import com.deepid.lgc.util.BluetoothUtil
import com.deepid.lgc.util.Helpers.Companion.PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
import com.deepid.lgc.util.Helpers.Companion.drawable
import com.deepid.lgc.util.Helpers.Companion.getBitmap
import com.deepid.lgc.util.ItemMenu
import com.deepid.lgc.util.PermissionUtil
import com.deepid.lgc.util.PermissionUtil.Companion.respondToPermissionRequest
import com.deepid.lgc.util.Utils
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.ble.BLEWrapper
import com.regula.documentreader.api.ble.BleWrapperCallback
import com.regula.documentreader.api.ble.RegulaBleService
import com.regula.documentreader.api.ble.callback.BleManagerCallback
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.completions.IDocumentReaderInitCompletion
import com.regula.documentreader.api.completions.IDocumentReaderPrepareCompletion
import com.regula.documentreader.api.completions.rfid.IRfidReaderCompletion
import com.regula.documentreader.api.config.RecognizeConfig
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.CaptureMode
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.errors.DocReaderRfidException
import com.regula.documentreader.api.errors.DocumentReaderException
import com.regula.documentreader.api.params.DocReaderConfig
import com.regula.documentreader.api.results.DocumentReaderNotification
import com.regula.documentreader.api.results.DocumentReaderResults
import com.regula.facesdk.FaceSDK
import com.regula.facesdk.exception.InitException
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : AppCompatActivity() {
    private var bleManager: BLEWrapper? = null
    private var isBleServiceConnected = false
    private var loadingDialog: AlertDialog? = null
    private var currentScenario: String = Scenario.SCENARIO_OCR
    private lateinit var binding: ActivityMainBinding
    private val rvAdapter: RecyclerAdapter by lazy {
        RecyclerAdapter(getRvData())
    }

    private fun getRvData(): List<Base> {
        val rvData = mutableListOf<Base>()
        rvData.add(
            ItemMenu(
                "Chip",
                drawable(R.drawable.image_menu_chip, this),
                onClick = { setUpBluetoothConnection() })
        )
        rvData.add(ItemMenu("Manual", drawable(R.drawable.image_menu_manual, this), onClick = {
            startActivity(Intent(this@MainActivity, DefaultScannerActivity::class.java))
        }))
        rvData.add(ItemMenu("Image", drawable(R.drawable.image_menu_image, this), false))
        return rvData
    }

    private fun setUpBluetoothConnection() {
        if (binding.contentMain.edDevice.text != null) {
            showDialog("Searching devices")
            handler.sendEmptyMessageDelayed(0, 7000)
            DocumentReader.Instance().functionality().edit()
                .setUseAuthenticator(true)
                .setBtDeviceName(binding.contentMain.edDevice.text.toString()).apply()
            Log.d(TAG, "[DEBUGX] btnClicked ")
            startBluetoothService()
        }
    }

//    var etDeviceName: EditText? = null
//    var btnConnect: Button? = null
//    var btnScan: Button? = null

    private val scannerViewModel: ScannerViewModel by viewModel()
    private val bluetoothUtil = BluetoothUtil()

    @Transient
    val imageBrowsingIntentLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if (it.resultCode == Activity.RESULT_OK) {
                it.data?.let { intent ->
                    val imageUris = ArrayList<Uri>()
                    if (intent.clipData == null) {
                        intent.data?.let { uri ->
                            imageUris.add(uri)
                        }
                    } else {
                        intent.clipData?.let { clipData ->
                            for (i in 0 until clipData.itemCount) {
                                imageUris.add(clipData.getItemAt(i).uri)
                            }
                        }
                    }
                    if (imageUris.size > 0) {
                        showDialog("Processing image")
                        if (imageUris.size == 1) {
                            getBitmap(imageUris[0], 1920, 1080, this)?.let { bitmap ->
                                val recognizeConfig =
                                    RecognizeConfig.Builder(currentScenario).setBitmap(bitmap)
                                        .build()
                                DocumentReader.Instance().recognize(recognizeConfig, completion)
                            }
                        } else {
                            val bitmaps = arrayOfNulls<Bitmap>(imageUris.size)
                            for (i in bitmaps.indices) {
                                bitmaps[i] = getBitmap(imageUris[i], 1920, 1080, this)
                            }
                            val recognizeConfig =
                                RecognizeConfig.Builder(currentScenario).setBitmaps(bitmaps).build()
                            DocumentReader.Instance().recognize(recognizeConfig, completion)
                        }
                    }
                }
            }
        }

    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
            dismissDialog()
            if (DocumentReader.Instance().functionality().isManualMultipageMode) {
                Log.d(TAG, "DEBUGX MULTIPAGEMODE: ")
                if (results?.morePagesAvailable != 0) {
                    DocumentReader.Instance().startNewPage()
                    Handler(Looper.getMainLooper()).postDelayed({
                        showScanner()
                    }, 100)
                    return@IDocumentReaderCompletion
                } else {
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()
                }
            }
            if (results?.chipPage != 0) {
                Log.d(TAG, "DEBUGX RFID IS PERFORMED: ")
                DocumentReader.Instance().startRFIDReader(this, object : IRfidReaderCompletion() {
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
                        if (rfidAction == DocReaderAction.COMPLETE || rfidAction == DocReaderAction.CANCEL)
                            ResultBottomSheet.results = results_RFIDReader!!
                        displayResults()
                        //captureFace(results_RFIDReader!!)
                    }
                })
            } else {
                Log.d(TAG, "DEBUGX NO RFID PERFORMED ")
                /**
                 * perform [livenessFace] or [captureface] then check similarity
                 */
                /**
                 * perform [livenessFace] or [captureface] then check similarity
                 */
                //  livenessFace(results)
                // captureFace(results)
                ResultBottomSheet.results = results
                displayResults()
            }
        } else {
            dismissDialog()
            if (action == DocReaderAction.CANCEL) {
                if (DocumentReader.Instance().functionality().isManualMultipageMode)
                    DocumentReader.Instance().functionality().edit().setManualMultipageMode(false)
                        .apply()

                Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initViews()
        //observe()
        initFaceSDK()
        prepareDatabase()
        DocumentReader.Instance().functionality().edit()
            .setBtDeviceName("Regula 0326")
            .setShowCaptureButton(true)
            .setShowCaptureButtonDelayFromStart(0)
            .setCaptureMode(CaptureMode.CAPTURE_FRAME)
            .apply()
        binding.contentMain.edDevice.setText(DocumentReader.Instance().functionality().btDeviceName)
        binding.contentMain.btnConnect.setOnClickListener { view: View? ->
            if (binding.contentMain.edDevice.text != null) {
                showDialog("Searching devices")
                handler.sendEmptyMessageDelayed(0, 7000)
                DocumentReader.Instance().functionality().edit()
                    .setUseAuthenticator(true)
                    .setBtDeviceName(binding.contentMain.edDevice.text.toString()).apply()
                Log.d(TAG, "[DEBUGX] btnClicked ")
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
            Log.d(TAG, "FaceSDK init completed successfully")
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

    private fun showScanner() {
        Log.d(TAG, "DEBUGX showScanner: currentscenario $currentScenario")
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this@MainActivity, scannerConfig, completion)
    }

    private fun displayResults() {
        val dialog = ResultBottomSheet.newInstance()
        dialog.show(supportFragmentManager, ResultBottomSheet.TAG)
    }

    private fun createImageBrowsingRequest() {
        val intent = Intent()
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        intent.action = Intent.ACTION_GET_CONTENT
        imageBrowsingIntentLauncher.launch(Intent.createChooser(intent, "Select Picture"))
    }

    fun recognizeImage() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE
            )
        } else
            createImageBrowsingRequest()
    }

    private fun initViews() {
//        etDeviceName = findViewById(R.id.ed_device)
//        btnConnect = findViewById(R.id.btn_connect)
//        btnScan = findViewById(R.id.btn_scan)
        with(binding.contentMain) {
            btnScan.setOnClickListener {
                startActivity(Intent(this@MainActivity, DefaultScannerActivity::class.java))
            }
            menuRv.layoutManager = LinearLayoutManager(this@MainActivity)
            menuRv.adapter = rvAdapter
            bottomNavigation.setOnItemReselectedListener { item ->
                when (item.itemId) {
                    R.id.image -> {
                        // Respond to navigation item 1 click
                        recognizeImage()

                    }

                    R.id.camera -> {
                        // Respond to navigation item 2 click
                        showScanner()
                    }

                    R.id.setting -> {
                        Toast.makeText(
                            this@MainActivity,
                            "",
                            Toast.LENGTH_LONG
                        ).show()

                    }

                    else -> Unit
                }
            }
        }

    }

    fun initializeReader() {
        val license = Utils.getLicense(this) ?: return
        showDialog("Initializing")

        DocumentReader.Instance()
            .initializeReader(this@MainActivity, DocReaderConfig(license), initCompletion)
//        DocumentReader.Instance()
//            .initializeReader(this@MainActivity, BleDeviceConfig(bleManager), initCompletion)
    }

    private val initCompletion =
        IDocumentReaderInitCompletion { result: Boolean, error: DocumentReaderException? ->
            dismissDialog()
            if (result) {
                binding.contentMain.btnConnect.isEnabled = true
            } else {
                Log.e(TAG, "[DEBUG] INIT failed: $error ")
                Toast.makeText(this@MainActivity, "Init failed:$error", Toast.LENGTH_LONG).show()
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
            isBleServiceConnected = true
            val bleService = (service as RegulaBleService.LocalBinder).service
            bleManager = bleService.bleManager
            Log.d(TAG, "[DEBUGX] onServiceConnected")

            if (bleManager?.isConnected == true) {
                startActivity(Intent(this@MainActivity, SuccessfulInitActivity::class.java))
//                Toast.makeText(this@MainActivity, "Bluetooth is connected", Toast.LENGTH_SHORT)
//                    .show()
                return
            }
            Log.d(TAG, "[DEBUGX] onServiceConnected 2")

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
            Log.d(TAG, "[DEBUGX] onDeviceReady")
            handler.removeMessages(0)
            bleManager!!.removeCallback(this)
            startActivity(Intent(this@MainActivity, SuccessfulInitActivity::class.java))
//            Toast.makeText(this@MainActivity, "Bluetooth is connected", Toast.LENGTH_SHORT).show()
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
                        binding.contentMain.btnConnect.callOnClick()
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
        const val TAG: String = "MainActivity"
    }
}
