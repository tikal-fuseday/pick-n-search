package com.tikal.pns

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.WebView

class WebViewActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_web_view)

        webView = findViewById(R.id.webView)

        webView.getSettings().setJavaScriptEnabled(true)
        webView.loadUrl("https://cranky-visvesvaraya-5dd279.netlify.com/")
    }
}
