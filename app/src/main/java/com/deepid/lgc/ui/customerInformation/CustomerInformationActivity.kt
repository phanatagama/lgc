package com.deepid.lgc.ui.customerInformation

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.deepid.lgc.data.common.toDate
import com.deepid.lgc.databinding.ActivityCustomerInformationBinding
import com.deepid.lgc.domain.model.CustomerInformation
import com.deepid.lgc.domain.model.DataImage
import com.deepid.lgc.util.IdProviderImpl
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class CustomerInformationActivity : AppCompatActivity() {
    private val formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy")
    private var birthDate = LocalDateTime.now()
    private var issueDate = LocalDateTime.now()
    private val customerInformationViewModel: CustomerInformationViewModel by viewModel()
    private val currentScenario: String = Scenario.SCENARIO_CAPTURE
    private lateinit var binding: ActivityCustomerInformationBinding
    private val rvAdapter: PhotoAdapter by lazy {
        PhotoAdapter()
    }
    private val generateImagePlaceholder: List<DataImage> =
        (1..10).map { DataImage(IdProviderImpl().generate()) }
    private var selectedImage: DataImage = generateImagePlaceholder.first()

    private fun takePhoto() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            takePhotoLauncher.launch(null)
        } else {
            requestPermissions()
        }
    }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { result ->
            result?.let {
                rvAdapter.updateList(selectedImage.copy(bitmap = it))
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setupRecyclerView()
        if (intent.getIntExtra(CUSTOMER_INFORMATION_TYPE, 1) == 1) {
            bindViews()
            if (documentResults == null) {
                takePhoto()
            } else {
                insertOpticalImage(documentResults)
            }
        } else {
            val customerInformationId = intent.getStringExtra(CUSTOMER_INFORMATION_ID)
            if (customerInformationId != null) {
                customerInformationViewModel.getCustomerInformationById(customerInformationId)
            }
            observe()
        }
    }

    private fun saveCustomerInformation() {
        with(binding) {
            if (titleTv.text.isNullOrEmpty() || addressTv.text.isNullOrEmpty() || issueTv.text.isNullOrEmpty() || birthDateTv.text.isNullOrEmpty() || !rvAdapter.currentList.any { it.bitmap != null }) {
                Toast.makeText(
                    this@CustomerInformationActivity,
                    "Please fill all fields",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            customerInformationViewModel.addImage(rvAdapter.currentList.filter { it.bitmap != null }
                .map { it.copy(path = it.bitmap?.saveBitmap(this@CustomerInformationActivity)) })
            customerInformationViewModel.insertCustomerInformation(
                titleTv.text.toString(),
                detailTv.text.toString(),
                addressTv.text.toString(),
                issueTv.text.toString().toDate(),
                birthDateTv.text.toString().toDate(),
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
            issueTv.setOnClickListener {
                val datePickerDialog = DatePickerDialog(
                    this@CustomerInformationActivity,
                    { _, year, month, dayOfMonth ->
                        val date = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0)
                        issueTv.text = date.format(formatter)
                        issueDate = date
                    },
                    issueDate.year,
                    issueDate.monthValue - 1,
                    issueDate.dayOfMonth
                )
                datePickerDialog.show()
                return@setOnClickListener
            }
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
                )
                datePickerDialog.show()
                return@setOnClickListener
            }
            btnSend.isEnabled = true
            btnSend.setOnClickListener {
                saveCustomerInformation()
            }

        }
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
            disableEditText(titleTv)
            disableEditText(detailTv)
            disableEditText(addressTv)
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
            rvAdapter.listener = object : PhotoAdapter.OnItemClickListener {
                override fun onItemClickListener(view: View, dataImage: DataImage) {
                    selectedImage = dataImage
                    if (dataImage.bitmap != null || dataImage.path != null) {
                        return
                    }
                    if (documentResults == null) {
                        takePhoto()
                    } else {
                        showScanner()
                    }
                }
            }
            ViewCompat.setNestedScrollingEnabled(rvPhoto, false)
        }
    }

    private fun insertOpticalImage(documentReaderResults: DocumentReaderResults?) {
        val userPhoto = documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_PORTRAIT
        )
            ?: documentReaderResults?.getGraphicFieldImageByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE
            )
            // use raw image if UV image is not available
            ?: documentReaderResults?.getGraphicFieldImageByType(
                eGraphicFieldType.GF_PORTRAIT,
                eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
                0,
                eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
            )
            ?: documentReaderResults?.getGraphicFieldImageByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE,
                eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            )
        userPhoto?.let {
            rvAdapter.updateList(selectedImage.copy(bitmap = it))
        }
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