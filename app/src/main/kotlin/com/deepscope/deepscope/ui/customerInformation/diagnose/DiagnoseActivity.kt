package com.deepscope.deepscope.ui.customerInformation.diagnose

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.deepscope.deepscope.R
import com.deepscope.deepscope.databinding.ActivityDiagnoseBinding
import com.deepscope.deepscope.ui.main.MainActivity
import com.deepscope.deepscope.util.Utils
import java.io.File

class DiagnoseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnoseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnoseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.extras?.let {
            with(binding){
                nameTv.text = it.getString(NAME)
                detailTv.text = it.getString(DETAIL)
                issueTv.text = it.getString(ISSUE)
                birthDateTv.text = it.getString(BIRTH_DATE)
                val type = it.getInt(IMAGE_TYPE, 2)
                if (type == 2) functionalCosmetics()
                val imgFile = File(it.getString(IMAGE_PATH)!!)
                if (imgFile.exists()) {
                    image.setImageURI(Uri.fromFile(imgFile))
                }
            }
        }

        bindViews()
    }

    private fun bindViews() {
        binding.btnBack.setOnClickListener {
            startActivity(Intent(this@DiagnoseActivity, MainActivity::class.java))
        }
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
    companion object{
        internal val TAG = DiagnoseActivity::class.java.simpleName
        const val NAME = "USER_NAME"
        const val ISSUE = "ISSUE"
        const val BIRTH_DATE = "BIRTH_DATE"
        const val DETAIL = "DETAIL"
        const val IMAGE_PATH = "IMAGE_PATH"
        const val IMAGE_TYPE = "IMAGE_TYPE"
    }
}