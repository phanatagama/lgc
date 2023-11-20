package com.deepid.lgc.ui.main

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepid.lgc.R
import com.deepid.lgc.databinding.LayoutResultBottomsheetBinding
import com.deepid.lgc.ui.defaultscanner.DocumentFieldAdapter
import com.deepid.lgc.ui.scanner.ScannerViewModel
import com.deepid.lgc.util.DocumentReaderResultsParcel
import com.deepid.lgc.util.Helpers
import com.deepid.lgc.util.toParcelable
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.regula.documentreader.api.enums.eCheckResult
import com.regula.documentreader.api.enums.eGraphicFieldType
import com.regula.documentreader.api.enums.eRPRM_Lights
import com.regula.documentreader.api.enums.eRPRM_ResultType
import com.regula.documentreader.api.enums.eVisualFieldType
import com.regula.documentreader.api.results.DocumentReaderResults
import org.koin.androidx.viewmodel.ext.android.activityViewModel

class ResultBottomSheet : BottomSheetDialogFragment() {
    private var _binding: LayoutResultBottomsheetBinding? = null
    private val binding get() = _binding!!
    private val scannerViewModel: ScannerViewModel by activityViewModel()
    private val rvAdapter: DocumentFieldAdapter by lazy {
        DocumentFieldAdapter()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = LayoutResultBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun show(manager: FragmentManager, tag: String?) {
        try {
            val ft = manager?.beginTransaction()
            val prev: Fragment? = manager.findFragmentByTag(TAG)
            if (prev != null) {
                Log.d(TAG, "showFragment: remove prev...." + prev.javaClass.name)
                ft?.remove(prev)
            }
            manager.executePendingTransactions()
            ft?.add(this, tag)
            ft?.commitAllowingStateLoss()
        } catch (ignored: IllegalStateException) {

        }

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        observe()

    }

    private fun observe() {
        scannerViewModel.documentReaderResultLiveData.observe(viewLifecycleOwner) {
            initViews(it)
            Log.d(TAG, "[DEBUGX] observe: ${it == null}")
            Log.d(TAG, "[DEBUGX] IT RAW RES: ${it?.rawResult}")
        }
    }

    private fun initViews(results: DocumentReaderResults?) {
//        val sectionsPagerAdapter = SectionsPagerAdapter(this)
//        sectionsPagerAdapter.documentReaderResults = results?.toParcelable(requireActivity())
//        val viewPager: ViewPager2 = binding.viewPager
//        viewPager.adapter = sectionsPagerAdapter
//        val tabs: TabLayout = binding.tabs
//        TabLayoutMediator(tabs, viewPager) { tab, position ->
//            tab.text = resources.getString(TAB_TITLES[position])
//        }.attach()

        Log.d(TAG, "raw result ${results?.rawResult}")
        val statusDrawable = Helpers.drawable(
            if (results?.status?.overallStatus == eCheckResult.CH_CHECK_OK) com.regula.documentreader.api.R.drawable.reg_ok else com.regula.documentreader.api.R.drawable.reg_fail,
            requireActivity()
        )
        val name = results?.getTextFieldValueByType(eVisualFieldType.FT_SURNAME_AND_GIVEN_NAMES)
        Log.d(TAG, "debugx name from bottom sheet $name ")
        val gender = results?.getTextFieldValueByType(eVisualFieldType.FT_SEX)
        val age = results?.getTextFieldValueByType(eVisualFieldType.FT_AGE)
        val ageFieldName =
            results?.getTextFieldByType(eVisualFieldType.FT_AGE)?.getFieldName(requireActivity())
        val image = results?.getGraphicFieldImageByType(eGraphicFieldType.GF_PORTRAIT)
            ?: results?.getGraphicFieldImageByType(eGraphicFieldType.GF_DOCUMENT_IMAGE)
        val birth = results?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_BIRTH)
        val address = results?.getTextFieldValueByType(eVisualFieldType.FT_ISSUING_STATE_NAME)
        val expiry = results?.getTextFieldValueByType(eVisualFieldType.FT_DATE_OF_EXPIRY)
        val rawImage = results?.getGraphicFieldImageByType(
            eGraphicFieldType.GF_PORTRAIT,
            eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            0,
            eRPRM_Lights.RPRM_LIGHT_WHITE_FULL
        )
            ?: results?.getGraphicFieldImageByType(
                eGraphicFieldType.GF_DOCUMENT_IMAGE,
                eRPRM_ResultType.RPRM_RESULT_TYPE_RAW_IMAGE,
            )
        val documentName = if (results?.documentType?.isNotEmpty() == true) {
            Log.d(TAG, "debugx document name ${results.documentType.first().name}")
            Log.d(TAG, "debugx document documentid ${results.documentType.first().documentID}")
            Log.d(TAG, "debugx document dtypr ${results.documentType.first().dType}")
            Log.d(TAG, "debugx document countty ${results.documentType.first().dCountryName}")
            results.documentType.first().name
        } else {
            "-"
        }
        val parcelableTextField = results?.toParcelable(requireActivity()) as DocumentReaderResultsParcel?
        with(binding) {
            titleTv.text = name
            detailTv.text = if (ageFieldName != null) "$gender, ${ageFieldName}: $age" else ""
            birthDateTv.text = birth
            addressTv.text = address
            issueTv.text = expiry
            documentTv.text = documentName
            rawImageIv.setImageBitmap(rawImage)
            faceIv.setImageBitmap(image)
            checkResultIv.setImageDrawable(statusDrawable)

            // add recyclerview
            recyclerView.layoutManager = LinearLayoutManager(requireActivity())
            recyclerView.adapter = rvAdapter
            recyclerView.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL))
            if(parcelableTextField?.textField?.isNotEmpty() == true){
                rvAdapter.submitList(parcelableTextField.textField)
                hideRecyclerView(false)
            }
        }
    }
    private fun hideRecyclerView(isHide: Boolean) {
        binding.recyclerView.visibility = if (isHide) View.GONE else View.VISIBLE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = BottomSheetDialog(requireContext(), theme)
        dialog.setOnShowListener {

            val bottomSheetDialog = it as BottomSheetDialog
            val parentLayout =
                bottomSheetDialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            parentLayout?.let { it ->
                val behaviour = BottomSheetBehavior.from(it)
                setupFullHeight(it)
                behaviour.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    private fun setupFullHeight(bottomSheet: View) {
        val layoutParams = bottomSheet.layoutParams
        layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
        bottomSheet.layoutParams = layoutParams
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        @StringRes
        private val TAB_TITLES = intArrayOf(
            R.string.tabs_overall,
            R.string.tabs_text,
            R.string.tabs_image
        )
        const val TAG = "ModalBottomSheet"
        fun newInstance(): BottomSheetDialogFragment {
            return ResultBottomSheet()
        }
    }
}