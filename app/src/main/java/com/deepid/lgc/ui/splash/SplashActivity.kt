package com.deepid.lgc.ui.splash

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.deepid.lgc.databinding.ActivitySplashBinding
import com.deepid.lgc.ui.main.MainActivity
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        lifecycleScope.launch {
            initServer()
            delay(3000)
            withContext(Dispatchers.Main){
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
                finish()
            }
        }
    }
    private fun initServer() {
        embeddedServer(Netty, 3333) {
            install(ContentNegotiation) {
                gson {}
            }
            routing {
                static {
                    resource("/", "index.html")
                    resource("*", "index.html")
                    static("static") {
                        resources("static")
                    }
                }
//                get("/lah") {
//                    call.respond(mapOf("message" to "Hello, world!"))
//                }
            }
        }
            .start(wait = false)
    }
}