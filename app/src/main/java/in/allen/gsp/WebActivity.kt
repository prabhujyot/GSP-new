package `in`.allen.gsp

import `in`.allen.gsp.databinding.ActivityWebBinding
import `in`.allen.gsp.utils.tag
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.databinding.DataBindingUtil

class WebActivity : AppCompatActivity() {

    private val TAG = WebActivity::class.java.name
    private lateinit var binding: ActivityWebBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_web)

        val toolbar = binding.root.findViewById<Toolbar>(R.id.myToolbar)

        setSupportActionBar(toolbar)
        toolbar.findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            onBackPressed()
        }

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                binding.progressBar.progress = newProgress
            }
        }

        binding.webView.webViewClient = object : WebViewClient() {
        }

        binding.webView.settings.javaScriptEnabled = true

        intent.getStringExtra("url").let {
            val url = intent.getStringExtra("url")
            tag("$TAG, $url")
            if (url != null) {
                binding.webView.loadUrl(url)
            }
        }
    }

}