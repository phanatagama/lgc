package com.deepscope.deepscope.ui.main.fragment

import android.os.Bundle
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.deepscope.deepscope.databinding.FragmentTextfieldBinding
import com.deepscope.deepscope.ui.defaultscanner.DocumentFieldAdapter
import com.deepscope.deepscope.util.DocumentReaderResultsParcel

class TextfieldFragment : Fragment() {
    private var documentReaderResults: DocumentReaderResultsParcel? = null
    private var _binding: FragmentTextfieldBinding? = null
    private val binding get() = _binding!!
    private val rvAdapter: DocumentFieldAdapter by lazy {
        DocumentFieldAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            documentReaderResults = it.getParcelable(DOCUMENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentTextfieldBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        with(binding) {
            recyclerView.layoutManager = LinearLayoutManager(requireActivity())
            recyclerView.adapter = rvAdapter
            recyclerView.addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL))
            if(documentReaderResults?.textField?.isNotEmpty() == true){
                rvAdapter.submitList(documentReaderResults?.textField)
                hideRecyclerView(false)
            }
        }
    }

    private fun hideRecyclerView(isHide: Boolean) {
        binding.recyclerView.visibility = if (isHide) View.GONE else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val DOCUMENT = "DOCUMENT_TEXT_FIELD"

        @JvmStatic
        fun newInstance(documentReaderResultsParcel: Parcelable?) =
            TextfieldFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(DOCUMENT, documentReaderResultsParcel)
                }
            }
    }
}