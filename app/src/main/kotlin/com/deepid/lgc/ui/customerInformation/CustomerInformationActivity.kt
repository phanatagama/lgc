package com.deepid.lgc.ui.customerInformation

import android.Manifest
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.ContentValues
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.deepid.lgc.PhotoDialogFragment
import com.deepid.lgc.R
import com.deepid.lgc.data.common.toDate
import com.deepid.lgc.databinding.ActivityCustomerInformationBinding
import com.deepid.lgc.domain.model.CustomerInformation
import com.deepid.lgc.domain.model.DataImage
import com.deepid.lgc.ui.customerInformation.daum.RoadAddressSearchDialog
import com.deepid.lgc.util.IdProviderImpl
import com.deepid.lgc.util.Utils.getBitmap
import com.deepid.lgc.util.Utils.saveBitmap
import com.regula.documentreader.api.DocumentReader
import com.regula.documentreader.api.completions.IDocumentReaderCompletion
import com.regula.documentreader.api.config.ScannerConfig
import com.regula.documentreader.api.enums.DocReaderAction
import com.regula.documentreader.api.enums.Scenario
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.results.DocumentReaderResults
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.FileDescriptor
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date


class CustomerInformationActivity : AppCompatActivity() {
    var uri: Uri? = null
    private val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private var birthDate = LocalDateTime.now()
    private var issueDate = LocalDateTime.now()
    private val customerInformationViewModel: CustomerInformationViewModel by viewModel()
    private val currentScenario: String = Scenario.SCENARIO_CAPTURE
    private lateinit var binding: ActivityCustomerInformationBinding
    private val rvAdapter: CustomerPhotoAdapter by lazy {
        CustomerPhotoAdapter()
    }
    private val generateImagePlaceholder: List<DataImage> =
        (1..10).map { DataImage(IdProviderImpl().generate()) }
    private var selectedImage: DataImage = generateImagePlaceholder.first()

    private fun takePhoto() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            val filename = "${LocalDateTime.now().toString()}-LGC.jpg"

            // Get the correct media Uri for your Android build version
            val imageUri =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    MediaStore.Images.Media.getContentUri(
                        MediaStore.VOLUME_EXTERNAL_PRIMARY
                    )
                } else {
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                }
            val imageDetails = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, filename)
            }
            uri = contentResolver.insert(imageUri, imageDetails)
            takePhotoLauncher.launch(uri)
        } else {
            requestPermissions()
        }
    }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result && uri != null) {
                val bitmapResult = uriToBitmap(uri!!)
                bitmapResult?.let {
                    rvAdapter.updateList(selectedImage.copy(bitmap = rotateBitmap(it)))
                }
            }
        }
    private fun uriToBitmap(selectedFileUri: Uri): Bitmap? {
        try {
            val parcelFileDescriptor = contentResolver.openFileDescriptor(selectedFileUri, "r")
            val fileDescriptor: FileDescriptor = parcelFileDescriptor!!.fileDescriptor
            val image = BitmapFactory.decodeFileDescriptor(fileDescriptor)
            parcelFileDescriptor.close()
            return image
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return null
    }
    @SuppressLint("Range")
    fun rotateBitmap(input: Bitmap): Bitmap? {
        val orientationColumn =
            arrayOf(MediaStore.Images.Media.ORIENTATION)
        val cur: Cursor? =
            contentResolver.query(uri!!, orientationColumn, null, null, null)
        var orientation = -1
        if (cur != null && cur.moveToFirst()) {
            orientation = cur.getInt(cur.getColumnIndex(orientationColumn[0]))
        }
        Log.d("tryOrientation", orientation.toString() + "")
        val rotationMatrix = Matrix()
        rotationMatrix.setRotate(orientation.toFloat())
        cur?.close()
        return Bitmap.createBitmap(input, 0, 0, input.width, input.height, rotationMatrix, true)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        if (intent.getIntExtra(CUSTOMER_INFORMATION_TYPE, 1) == 1) {
            bindViews()
            if (intent.getIntExtra(CUSTOMER_INFORMATION_FEATURE, 1) == 1) {
                takePhoto()
            } else {
                insertOpticalImage(documentResults)
            }
        } else {
            rvAdapter.parentType = 2
            binding.btnCloseSingle.setOnClickListener {
                displayImage(false)
            }
            val customerInformationId = intent.getStringExtra(CUSTOMER_INFORMATION_ID)
            if (customerInformationId != null) {
                customerInformationViewModel.getCustomerInformationById(customerInformationId)
            }
            observe()
        }
    }

    private fun saveCustomerInformation() {
        with(binding) {
            if (!rvAdapter.currentList.any { it.bitmap != null }) {
                Toast.makeText(
                    this@CustomerInformationActivity,
                    "there is no photos",
                    Toast.LENGTH_SHORT
                ).show()
                return
            } else {
                Toast.makeText(
                    this@CustomerInformationActivity,
                    "Saving Complete",
                    Toast.LENGTH_SHORT
                ).show()
            }
            val title =
                if (titleTv.text.isNullOrEmpty()) getString(R.string.john_doe) else titleTv.text.toString()
            val detail =
                if (detailTv.text.isNullOrEmpty()) getString(R.string.customer_detail) else detailTv.text.toString()
            val address =
                if (addressTv.text.isNullOrEmpty()) getString(R.string.address_value) else addressTv.text.toString()
            val issue =
                if (issueTv.text.isNullOrEmpty()) issueDate else issueTv.text.toString().toDate()
            val birth =
                if (birthDateTv.text.isNullOrEmpty()) birthDate else birthDateTv.text.toString()
                    .toDate()

            customerInformationViewModel.addImage(rvAdapter.currentList.filter { it.bitmap != null }
                .map { it.copy(path = it.bitmap?.saveBitmap(this@CustomerInformationActivity)) })
            customerInformationViewModel.insertCustomerInformation(
                title, detail, address, issue, birth
            )
            finish()
        }

    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        requestPermissionLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        )
        { permissions ->
            // Handle Permission granted/rejected
            var permissionGranted = true
            permissions.entries.forEach {
                if (it.key in REQUIRED_PERMISSIONS && !it.value)
                    permissionGranted = false
            }
            if (!permissionGranted) {
                Toast.makeText(
                    baseContext,
                    "Permission request denied",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            } else {
                takePhoto()
            }
        }


    private fun bindViews() {
        with(binding) {
            issueTv.text = issueDate.format(formatter)
            birthDateTv.text = birthDate.format(formatter)
            birthDateTv.setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                    this@CustomerInformationActivity,
                    { _, year, month, dayOfMonth ->
                        val date = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0)
                        birthDateTv.text = date.format(formatter)
                        birthDate = date
                    },
                    birthDate.year,
                    birthDate.monthValue - 1,
                    birthDate.dayOfMonth
                ).apply { datePicker.maxDate = Date().time }
                datePickerDialog.show()
                return@setOnClickListener
            }
            addressTv.setOnClickListener {
                addressLookup()
            }
            addressTv.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) addressLookup() }
            btnSend.isEnabled = true
            btnSend.setOnClickListener {
                saveCustomerInformation()
            }

        }
    }

    private fun addressLookup() {
        val dialog = RoadAddressSearchDialog.newInstance()
        dialog.listener = object : RoadAddressSearchDialog.OnInputListener {
            override fun sendInput(input: String?) {
                input?.let {
                    binding.addressTv.setText(it)
                }
            }
        }
        dialog.show(supportFragmentManager, "RoadAddressSearchDialog")
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun bindViews(customerInformation: CustomerInformation) {
        with(binding) {
            rvAdapter.submitList(customerInformation.images)
            titleTv.setText(customerInformation.name)
            detailTv.setText(customerInformation.description)
            addressTv.setText(customerInformation.address)
            issueTv.text = customerInformation.issueDate.format(formatter)
            birthDateTv.text = customerInformation.birthDate.format(formatter)
            btnSend.text = getString(R.string.send_image)
            disableEditText(titleTv)
            disableEditText(detailTv)
            disableEditText(addressTv)
            btnSend.isEnabled = true
            btnSend.setOnClickListener {
                Toast.makeText(this@CustomerInformationActivity, "Complete", Toast.LENGTH_SHORT)
                    .show()
            }
            btnSendSingle.setOnClickListener {
                Toast.makeText(this@CustomerInformationActivity, "Complete", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun disableEditText(editText: EditText) {
        editText.isFocusable = false
        editText.isEnabled = false
        editText.isCursorVisible = false
        editText.keyListener = null
        editText.setBackgroundColor(Color.TRANSPARENT)
    }

    /** Setup recyclerview
     * 1. Setup layout manager
     * 2. Setup adapter
     * 3. Setup listener
     * 4. Setup nested scrolling
     */
    private fun setupRecyclerView(currentList: List<DataImage> = generateImagePlaceholder) {
        with(binding) {
            rvPhoto.layoutManager =
                GridLayoutManager(this@CustomerInformationActivity, 2)
            rvPhoto.adapter = rvAdapter
            rvAdapter.submitList(currentList)
            rvAdapter.listener = object : CustomerPhotoAdapter.OnItemClickListener {
                override fun onItemClickListener(view: View, dataImage: DataImage) {
                    selectedImage = dataImage
                    if (dataImage.bitmap != null || dataImage.path != null) {
                        if(rvAdapter.parentType == 1){
                            showImageDialog(
                                dataImage.bitmap ?: getBitmap(
                                    Uri.fromFile(File(dataImage.path!!)),
                                    576,
                                    768,
                                    this@CustomerInformationActivity
                                )!!
                            )
                        } else {
                            singleImage.setImageURI(Uri.fromFile(File(dataImage.path!!)))
                            displayImage(true)
                        }

                        return
                    }
                    if (documentResults == null) {
                        takePhoto()
                    } else {
                        showScanner()
                    }
                }

                override fun onItemDeleteClickListener(view: View, dataImage: DataImage) {
                    dataImage.copy(bitmap = null, path = null).let {
                        rvAdapter.updateList(it)
                    }
                }
            }
            ViewCompat.setNestedScrollingEnabled(rvPhoto, false)
        }
    }

    private fun displayImage(isSingleView: Boolean){
        with(binding){
            if(isSingleView){
                rvPhoto.visibility = View.GONE
                btnSend.visibility = View.GONE
                singleImage.visibility = View.VISIBLE
                btnSendSingle.visibility = View.VISIBLE
                btnCloseSingle.visibility = View.VISIBLE

            }else{
                rvPhoto.visibility = View.VISIBLE
                btnSend.visibility = View.VISIBLE
                singleImage.visibility = View.GONE
                btnSendSingle.visibility = View.GONE
                btnCloseSingle.visibility = View.GONE
            }
        }
    }


    private fun showImageDialog(bitmap: Bitmap) {
        PhotoDialogFragment.newInstance(bitmap).show(supportFragmentManager, "PhotoDialogFragment")
    }


    private fun insertOpticalImage(documentReaderResults: DocumentReaderResults?) {
        val uvImage = documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0,
            eRPRM_Lights.RPRM_LIGHT_UV
        )
        val rawImage = documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_PORTRAIT
        ) ?: documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE
        ) ?: documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_PORTRAIT,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0,
            eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
        ) ?: documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
        )

        val emptySlot = getEmptySlot()
        val emptyField = if (emptySlot.hasNext()) emptySlot.next() else null
        val emptyField2 = if (emptySlot.hasNext()) emptySlot.next() else null
        if (intent.getIntExtra(CUSTOMER_INFORMATION_FEATURE, 2) == 2) {
            uvImage?.let {
                if (emptyField != null) {
                    rvAdapter.updateList(emptyField.copy(bitmap = it))
                }
            }
        } else {
            if (rawImage != null && uvImage != null && emptyField != null && emptyField2 != null) {
                rvAdapter.updateList(
                    emptyField.copy(bitmap = rawImage),
                    emptyField2.copy(bitmap = uvImage)
                )
            }
        }
    }

    private fun getEmptySlot(): Iterator<DataImage> {
        return rvAdapter.currentList.filter { it.bitmap == null }.iterator()
    }

    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
            if (results != null) {
                documentResults = results
                insertOpticalImage(results)
            } else {
                Toast.makeText(this, "DocReaderSDK has been failed to identify", Toast.LENGTH_LONG)
                    .show()
            }
        } else {
            if (action == DocReaderAction.CANCEL) {
                Toast.makeText(this, "Scanning was cancelled", Toast.LENGTH_LONG).show()
            } else if (action == DocReaderAction.ERROR) {
                Toast.makeText(this, "Error:$error", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showScanner() {
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(this@CustomerInformationActivity, scannerConfig, completion)
    }

    override fun onDestroy() {
        documentResults = null
        super.onDestroy()
    }

    private fun observe() {
        customerInformationViewModel.customerInformation.observe(this) {
            bindViews(it)
        }
    }

    companion object {
        var documentResults: DocumentReaderResults? = null
        const val CUSTOMER_INFORMATION_TYPE = "CUSTOMER_INFORMATION_TYPE"
        const val CUSTOMER_INFORMATION_ID = "CUSTOMER_INFORMATION_ID"
        const val CUSTOMER_INFORMATION_FEATURE = "CUSTOMER_INFORMATION_FEATURE"
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}
