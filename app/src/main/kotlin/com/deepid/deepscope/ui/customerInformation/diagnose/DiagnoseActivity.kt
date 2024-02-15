package com.deepid.deepscope.ui.customerInformation.diagnose

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.deepid.deepscope.databinding.ActivityDiagnoseBinding
import com.deepid.deepscope.ui.main.MainActivity
import java.io.File

class DiagnoseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDiagnoseBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDiagnoseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        intent.extras?.let {
            with(binding){
                nameTv.text = it.getString("name")
                detailTv.text = it.getString("detail")
                issueTv.text = it.getString("issue")
                birthDateTv.text = it.getString("birthDate")
                val imgFile = File(it.getString("image")!!)
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

    companion object{
        const val TAG = "DiagnoseActivity"
    }
}