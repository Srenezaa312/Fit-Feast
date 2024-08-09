package com.example.fitfeast

import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WebViewActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        val webView: WebView = findViewById(R.id.webview)
        val closeButton: Button = findViewById(R.id.closeButton)

        closeButton.setOnClickListener {
            finish()
        }

        val url = intent.getStringExtra("EXTRA_URL") ?: ""
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }
}

