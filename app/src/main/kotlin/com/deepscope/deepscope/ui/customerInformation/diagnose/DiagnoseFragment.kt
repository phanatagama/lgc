package com.deepscope.deepscope.ui.customerInformation.diagnose

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.TransitionInflater
import com.deepscope.deepscope.R
import com.deepscope.deepscope.data.common.toDateString
import com.deepscope.deepscope.databinding.FragmentDiagnoseBinding
import java.io.File

class DiagnoseFragment : Fragment() {
    private var _binding: FragmentDiagnoseBinding? = null
    private val binding get() = _binding!!
    private val args: DiagnoseFragmentArgs by navArgs()

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition =
            TransitionInflater.from(requireContext()).inflateTransition(android.R.transition.move)
        setHasOptionsMenu(true)
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = FragmentDiagnoseBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        with(binding){
            nameTv.text = args.customer.name
            detailTv.text = args.customer.description
            issueTv.text = args.customer.issueDate.toDateString()
            birthDateTv.text = args.customer.birthDate.toDateString()
            btnBack.setOnClickListener { goToMainActivity() }
            val selectedImage = args.customer.images.first()
            if (selectedImage.type == 2) functionalCosmetics()
            val imgFile = File(selectedImage.path!!)
            if (imgFile.exists()) {
                image.setImageURI(Uri.fromFile(imgFile))
            }
        }
    }

    private fun goToMainActivity() {
        val action = DiagnoseFragmentDirections.actionDiagnoseFragmentToMainFragment()
        findNavController().navigate(action)
    }
    private fun functionalCosmetics() {
        with(binding){
            topDivider.setBackgroundResource(R.color.functional_cosmetics_color)
            footDivider.setBackgroundResource(R.color.functional_cosmetics_color)
            container.setBackgroundResource(R.color.functional_cosmetics_color)
//            btnBack.setBackgroundColor(Utils.getColorResource(R.color.functional_cosmetics_color, this@DiagnoseActivity))
            subtitle.text = getString(R.string.skin_aging_diagnosis)
            diseaseTable.text = getString(R.string.skin_aging_care)
            description1.text = getString(R.string.melanon_cream_description_1)
            description2.text = getString(R.string.melanon_cream_description_2)
            receipt1.text = getString(R.string.melanon_cream)
            receipt2.text = getString(R.string.anti_wrinkle_cream)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}