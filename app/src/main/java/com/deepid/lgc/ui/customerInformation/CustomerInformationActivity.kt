package com.deepid.lgc.ui.customerInformation

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.deepid.lgc.data.common.toDate
import com.deepid.lgc.databinding.ActivityCustomerInformationBinding
import com.deepid.lgc.domain.model.DataImage
import com.deepid.lgc.domain.model.generateImagePlaceholder
import com.deepid.lgc.util.Helpers
import com.deepid.lgc.util.Utils.saveToGallery
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
    private val currentScenario: String = Scenario.SCENARIO_FULL_AUTH
    private lateinit var binding: ActivityCustomerInformationBinding
    private var selectedImage: DataImage = generateImagePlaceholder.first()
    private val rvAdapter: PhotoAdapter by lazy {
        PhotoAdapter()
    }

    private fun takePhoto() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                Helpers.PERMISSIONS_REQUEST_CAMERA
            )
        } else {
            takePhotoLauncher.launch(null)
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
        observe()
        bindViews()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Request camera permissions
        if (documentResults == null) {
            if (allPermissionsGranted()) {
                takePhoto()
            } else {
                requestPermissions()
            }
        } else {
            insertOpticalImage(documentResults)
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
                .map { it.copy(path = it.bitmap?.saveToGallery(this@CustomerInformationActivity)) })
            customerInformationViewModel.insertCustomerInformation(
                titleTv.text.toString(),
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
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private val activityResultLauncher =
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
            setupRecyclerView()
        }
    }

    /** Setup recyclerview
     * 1. Setup layout manager
     * 2. Setup adapter
     * 3. Setup listener
     * 4. Setup nested scrolling
     */
    private fun ActivityCustomerInformationBinding.setupRecyclerView() {
        rvPhoto.layoutManager =
            GridLayoutManager(this@CustomerInformationActivity, 2)
        rvPhoto.adapter = rvAdapter
        rvAdapter.submitList(generateImagePlaceholder)
        rvAdapter.listener = object : PhotoAdapter.OnItemClickListener {
            override fun onItemClickListener(view: View, dataImage: DataImage) {
                selectedImage = dataImage
                if (dataImage.bitmap != null) {
                    Toast.makeText(
                        this@CustomerInformationActivity,
                        "Grid ${dataImage.id} was filled",
                        Toast.LENGTH_LONG
                    ).show()
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
        // TODO: observe view model LiveData/Flow here
    }

    companion object {
        var documentResults: DocumentReaderResults? = null
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