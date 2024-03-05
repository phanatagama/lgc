package com.deepscope.deepscope.ui.customerInformation

import android.Manifest
import android.app.DatePickerDialog
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.deepscope.deepscope.R
import com.deepscope.deepscope.data.common.toDate
import com.deepscope.deepscope.data.common.toDateString
import com.deepscope.deepscope.databinding.FragmentCustomerInformationBinding
import com.deepscope.deepscope.domain.model.CustomerInformation
import com.deepscope.deepscope.domain.model.DataImage
import com.deepscope.deepscope.ui.customerInformation.daum.RoadAddressSearchDialog
import com.deepscope.deepscope.util.Utils
import com.deepscope.deepscope.util.Utils.saveBitmap
import com.deepscope.deepscope.util.launchIn
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
import timber.log.Timber
import java.io.File
import java.time.LocalDateTime
import java.util.Date

class CustomerInformationFragment : Fragment() {
    private var _binding: FragmentCustomerInformationBinding? = null
    private val binding get() = _binding!!
    private val args: CustomerInformationFragmentArgs by navArgs()
    private var uri: Uri? = null
    private var birthDate = LocalDateTime.now()
    private var issueDate = LocalDateTime.now()
    private val customerInformationViewModel: CustomerInformationViewModel by viewModel()
    private val currentScenario: String = Scenario.SCENARIO_CAPTURE
    private val generateImagePlaceholder: List<DataImage> =
        (1..10).map { DataImage() }
    private var selectedImage: DataImage = generateImagePlaceholder.first()
    private var currentUser: CustomerInformation? = null
    private val rvAdapter: CustomerPhotoAdapter by lazy {
        CustomerPhotoAdapter()
    }

    @Transient
    private val completion = IDocumentReaderCompletion { action, results, error ->
        if (action == DocReaderAction.COMPLETE
            || action == DocReaderAction.TIMEOUT
        ) {
            Timber.d("result: $results")
            results?.let {
                documentResults = results
                insertOpticalImage(results)
            } ?: run {
                showToast(getString(R.string.docreadersdk_has_been_failed_to_identify))
            }
        } else {
            when (action) {
                DocReaderAction.CANCEL -> {
                    showToast(getString(R.string.scanning_was_cancelled))
                }

                DocReaderAction.ERROR -> {
                    Timber.e(error)
                    showToast("Error: $error")
                }

                else -> {
                    showToast("Unknown action: $action")
                }
            }
        }
    }

    private fun takePhoto() {
        // Request camera permissions
        if (allPermissionsGranted()) {
            uri = Utils.getImageUri(requireActivity())
            takePhotoLauncher.launch(uri)
        } else {
            requestPermissions()
        }
    }

    private val takePhotoLauncher =
        registerForActivityResult(ActivityResultContracts.TakePicture()) { result ->
            if (result && uri != null) {
                val bitmapResult = Utils.uriToBitmap(requireActivity(), uri!!)
                bitmapResult?.let {
                    selectedImage.copy(bitmap = it, type = 1).also(rvAdapter::updateList)
                }
            }
        }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            requireActivity(), it
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
                showToast(getString(R.string.permission_request_denied))
                findNavController().navigateUp()
            } else {
                takePhoto()
            }
        }

    private fun showToast(message: String) {
        Toast.makeText(
            requireActivity(),
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * A function to show address lookup dialog
     */
    private fun addressLookup() {
        val webViewListener = RoadAddressSearchDialog.OnInputListener {
            it?.let {
                binding.addressTv.setText(it)
            }
        }
        val dialog = RoadAddressSearchDialog.Builder()
            .addListener(webViewListener)
            .build()
        dialog.show(parentFragmentManager, RoadAddressSearchDialog.TAG)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentCustomerInformationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        if (args.customerInformationType == 1) {
            bindViews()
            if (args.customerInformationFeature == 1) {
                takePhoto()
            } else {
                documentResults?.let {
                    insertOpticalImage(it)
                } ?: run {
                    showScanner()
                }
            }
        } else {
            rvAdapter.parentType = 2
            binding.btnCloseSingle.setOnClickListener {
                displayImage(false)
            }
            val customerInformationId = args.customerInformationId
            customerInformationId?.let {
                customerInformationViewModel.getCustomerInformationById(it)
            } ?: run {
                showToast("Customer Information ID is null")
                findNavController().navigateUp()
            }
            observe()
//            startAnimation()
        }
    }

    private fun observe() {
        customerInformationViewModel.customerInformation.observe(viewLifecycleOwner) {
            bindViews(it)
            currentUser = it
        }
    }

    /**
     * A function to hide recyclerview and show single image view
     */
    private fun displayImage(isSingleView: Boolean) {
        val hideViewOnImageSelected = if (isSingleView) View.GONE else View.VISIBLE
        val showViewOnImageSelected = if (isSingleView) View.VISIBLE else View.GONE
        with(binding) {
            rvPhoto.visibility = hideViewOnImageSelected
            btnSend.visibility = hideViewOnImageSelected
            singleImage.visibility = showViewOnImageSelected
            btnSendSingle.visibility = showViewOnImageSelected
            btnCloseSingle.visibility = showViewOnImageSelected
        }
    }

    private fun showScanner() {
        val scannerConfig = ScannerConfig.Builder(currentScenario).build()
        DocumentReader.Instance()
            .showScanner(requireActivity(), scannerConfig, completion)
    }

    private fun insertOpticalImage(documentReaderResults: DocumentReaderResults?) {
        val uvImage = getUvImage(documentReaderResults)
        val rawImage = getRawImage(documentReaderResults)

        val emptySlot = getEmptySlot()
        val firstSlot = if (emptySlot.hasNext()) emptySlot.next() else null
        val secondSlot = if (emptySlot.hasNext()) emptySlot.next() else null
        if (args.customerInformationFeature == 2) {
            uvImage?.let {
                firstSlot?.copy(bitmap = it, type = 2)?.also(rvAdapter::updateList)
            } ?: run {
                showToast(getString(R.string.uv_image_not_found))
            }
        } else {
            if (rawImage != null && uvImage != null && firstSlot != null && secondSlot != null) {
                rvAdapter.updateList(
                    firstSlot.copy(bitmap = rawImage, type = 1),
                    secondSlot.copy(bitmap = uvImage, type = 2),
                )
            } else {
                showToast(getString(R.string.uv_image_not_found))
            }
        }
    }

    private fun getRawImage(documentReaderResults: DocumentReaderResults?): Bitmap? =
        documentReaderResults?.getGraphicFieldImageByType(
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

    private fun getUvImage(documentReaderResults: DocumentReaderResults?): Bitmap? =
        documentReaderResults?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_DOCUMENT_IMAGE,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0,
            eRPRM_Lights.RPRM_LIGHT_UV
        )

    private fun getEmptySlot(): Iterator<DataImage> {
        return rvAdapter.currentList.filter { it.bitmap == null }.iterator()
    }

    private fun bindViews(customerInformation: CustomerInformation) {
        with(binding) {
            rvAdapter.submitList(customerInformation.images)
            titleTv.setText(customerInformation.name)
            detailTv.setText(customerInformation.description)
            addressTv.setText(customerInformation.address)
            issueTv.text = customerInformation.issueDate.toDateString()
            birthDateTv.text = customerInformation.birthDate.toDateString()
            btnSend.text = getString(R.string.send_image)
            disableEditText(titleTv)
            disableEditText(detailTv)
            disableEditText(addressTv)
            btnSend.isEnabled = false
            btnSend.setOnClickListener {
//                goToDiagnoseActivity()
            }
            btnSendSingle.setOnClickListener {
                goToDiagnoseFragment()
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

    private fun goToDiagnoseFragment() {
        if (currentUser == null) return
        val extras = FragmentNavigatorExtras(
            binding.singleImage to getString(R.string.menu_image),
            binding.titleTv to getString(R.string.customer_name),
            binding.detailTv to getString(R.string.customer_detail)
        )
        val action =
            CustomerInformationFragmentDirections.actionCustomerInformationFragmentToDiagnoseFragment(
                currentUser!!.copy(
                    images = listOf(selectedImage),
                )
            )
        return findNavController().navigate(action, extras)
    }

    private fun setupRecyclerView(currentList: List<DataImage> = generateImagePlaceholder) {
        with(binding) {
            rvPhoto.layoutManager =
                GridLayoutManager(requireActivity(), 2)
            rvPhoto.adapter = rvAdapter
            rvAdapter.submitList(currentList)
            rvAdapter.listener = object : CustomerPhotoAdapter.OnItemClickListener {
                override fun onItemClickListener(view: View, dataImage: DataImage) {
                    selectedImage = dataImage
                    if (dataImage.bitmap != null || dataImage.path != null) {
                        if (rvAdapter.parentType == 1) {
                            showImageDialog(
                                dataImage.bitmap ?: Utils.getBitmap(
                                    Uri.fromFile(File(dataImage.path!!)),
                                    576,
                                    768,
                                    requireActivity()
                                )!!
                            )
                        } else {
                            singleImage.setImageURI(Uri.fromFile(File(dataImage.path!!)))
                            displayImage(true)
                        }

                        return
                    }

                    if (args.customerInformationFeature == 1) {
                        takePhoto()
                    } else {
                        showScanner()
                    }
                }

                override fun onItemDeleteClickListener(view: View, dataImage: DataImage) {
                    if (rvAdapter.parentType == 1) {
                        dataImage.copy(bitmap = null, path = null).also(rvAdapter::updateList)
                    } else {
                        customerInformationViewModel.deleteImage(
                            args.customerInformationId!!, dataImage
                        )
                    }
                }
            }
            ViewCompat.setNestedScrollingEnabled(rvPhoto, false)
        }
    }

    private fun showImageDialog(bitmap: Bitmap) {
        PhotoDialogFragment.newInstance(bitmap)
            .show(parentFragmentManager, PhotoDialogFragment.TAG)
    }

    private fun bindViews() {
        with(binding) {
            issueTv.text = issueDate.toDateString()
            birthDateTv.text = birthDate.toDateString()
            birthDateTv.setOnClickListener { showDatePicker() }
            addressTv.setOnClickListener { addressLookup() }
            addressTv.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) addressLookup() }
            btnSend.isEnabled = true
            btnSend.setOnClickListener { saveCustomerInformation() }
        }
    }

    private fun isPhotoEmpty(): Boolean {
        return rvAdapter.currentList.all { it.bitmap == null }
    }

    private fun saveCustomerInformation() {
        with(binding) {
            if (isPhotoEmpty()) {
                showToast(getString(R.string.there_is_no_photos))
                return
            }
            btnSend.isEnabled = false
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
                .map {
                    it.copy(path = it.bitmap?.saveBitmap(requireActivity()))
                })

            customerInformationViewModel.insertCustomerInformation(
                title, detail, address, issue, birth
            ).launchIn(viewLifecycleOwner.lifecycleScope, ::onInsertComplete)
        }

    }

    private fun onInsertComplete() {
        showToast(getString(R.string.complete))
        binding.btnSend.isEnabled = true
        findNavController().navigateUp()
    }

    private fun showDatePicker() {
        val datePickerDialog = DatePickerDialog(
            requireActivity(),
            { _, year, month, dayOfMonth ->
                val date = LocalDateTime.of(year, month + 1, dayOfMonth, 0, 0)
                binding.birthDateTv.text = date.toDateString()
                birthDate = date
            },
            birthDate.year,
            birthDate.monthValue - 1,
            birthDate.dayOfMonth
        ).apply { datePicker.maxDate = Date().time }
        datePickerDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        documentResults = null
        Timber.d("[DEBUGX] onDestroyView")
    }

    companion object {
        var documentResults: DocumentReaderResults? = null

        /**
         * CUSTOMER_INFORMATION_TYPE
         * 1 -> new // the activity will be setup for new customer information (editable)
         * 2 -> existing // the activity will be setup for existing customer (read only)
         */
        const val CUSTOMER_INFORMATION_TYPE = "CUSTOMER_INFORMATION_TYPE"

        /**
         * CUSTOMER_INFORMATION_FEATURE
         * 1 -> feature-visible // the activity will be setup for normal camera
         * 2 -> feature-invisible/auto // the activity will be setup for bluetooth camera
         */
        const val CUSTOMER_INFORMATION_FEATURE = "CUSTOMER_INFORMATION_FEATURE"
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