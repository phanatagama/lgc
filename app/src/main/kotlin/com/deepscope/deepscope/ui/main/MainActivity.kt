package com.deepscope.deepscope.ui.main

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.deepscope.deepscope.R
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.http.content.resource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty


class MainActivity : AppCompatActivity() {
    private lateinit var server: ApplicationEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initServer()
    }

    /**
     * Starts a server on port 3333
     * Serves static files from the static folder
     * All other routes are served the index.html file
     * */
    private fun initServer() {
        server = embeddedServer(Netty, 3333) {
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
            }
        }
            .start(wait = false)
    }

    override fun onDestroy() {
        server.stop(0, 0)
        super.onDestroy()
    }

    companion object {
        const val TAG: String = "MainActivity"
    }
}
