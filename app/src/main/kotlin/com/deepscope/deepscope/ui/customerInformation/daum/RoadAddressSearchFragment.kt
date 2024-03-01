package com.deepscope.deepscope.ui.customerInformation.daum

import android.graphics.Bitmap
import android.net.http.SslError
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.PermissionRequest
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.fragment.app.DialogFragment
import com.deepscope.deepscope.databinding.FragmentRoadAddressSearchBinding
import timber.log.Timber


class RoadAddressSearchDialog : DialogFragment() {
    private var _binding: FragmentRoadAddressSearchBinding? = null
    private val binding get() = _binding!!

    //    var navController: NavController? = null
    var webView: WebView? = null
    var handler: Handler? = null
    var listener: OnInputListener? = null

    interface OnInputListener {
        fun sendInput(input: String?)
    }

    override fun onStart() {
        super.onStart()
        if (dialog != null) {
            dialog!!.window!!.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        //binding 설정
        _binding = FragmentRoadAddressSearchBinding.inflate(inflater, container, false)

        // 핸들러를 통한 JavaScript 이벤트 반응
        handler = Handler()

        //Controller 설정
//        navController = NavHostFragment.findNavController(this)
        initWebView()
        binding.buttonBack.setOnClickListener { dismiss() }
        return binding.root
    }

    private fun initWebView() {
        // WebView 설정
        webView = binding.daumWebview

        // JavaScript 허용
        webView!!.settings.javaScriptEnabled = true

        // JavaScript의 window.open 허용
        webView!!.settings.javaScriptCanOpenWindowsAutomatically = true

        // JavaScript이벤트에 대응할 함수를 정의 한 클래스를 붙여줌
        webView!!.addJavascriptInterface(AndroidBridge(), "LgcApp")

        //DOMStorage 허용
        webView!!.settings.domStorageEnabled = true

        //ssl 인증이 없는 경우 해결을 위한 부분
        webView!!.webChromeClient = object : WebChromeClient() {
            override fun onPermissionRequest(request: PermissionRequest) {
                request.grant(request.resources)
            }
        }
        webView!!.webViewClient = object : WebViewClient() {
            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                // SSL 에러가 발생해도 계속 진행
                handler.proceed()
            }

            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                view.loadUrl(url)
                return true
            }

            // 페이지 로딩 시작시 호출
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                Timber.d("페이지 시작", url)
                binding.webProgress.visibility = View.VISIBLE
            }

            override fun onPageFinished(view: WebView, url: String) {
                binding.webProgress.visibility = View.GONE
                Timber.d("페이지 로딩", url)
                webView!!.loadUrl("javascript:sample2_execDaumPostcode();")
            }
        }

        // webview url load. php or html 파일 주소
        webView!!.loadUrl("http://127.0.0.1:3333/")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    internal inner class AndroidBridge {
        @JavascriptInterface
        @Suppress("unused")
        fun processDATA(roadAdd: String?) {
            handler?.post(Runnable {
                listener?.sendInput(roadAdd)
                dismiss()
            })
        }
    }

    companion object {
        internal val TAG = RoadAddressSearchDialog::class.java.simpleName
        fun newInstance(): RoadAddressSearchDialog {
            return RoadAddressSearchDialog()
        }
    }
}