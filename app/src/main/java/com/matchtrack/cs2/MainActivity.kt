package com.matchtrack.cs2

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var adView: AdView? = null
    private var adsInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(16, 14, 23)
        window.navigationBarColor = android.graphics.Color.rgb(16, 14, 23)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(android.graphics.Color.rgb(16, 14, 23))
        }
        webView = WebView(this).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = true
            settings.allowContentAccess = false
            settings.setSupportMultipleWindows(false)
            webChromeClient = WebChromeClient()
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: android.webkit.WebResourceRequest?
                ): Boolean {
                    val url = request?.url?.toString() ?: return true
                    return !url.startsWith("file:///android_asset/")
                }
            }
            setBackgroundColor(android.graphics.Color.rgb(16, 14, 23))
            loadUrl("file:///android_asset/index.html")
        }
        root.addView(webView, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        // Google's official test banner. It does not generate revenue.
        val adLabel = TextView(this).apply {
            text = "Реклама"
            textSize = 9f
            gravity = Gravity.CENTER
            setTextColor(android.graphics.Color.rgb(170, 162, 189))
            setBackgroundColor(android.graphics.Color.rgb(25, 22, 34))
        }
        root.addView(adLabel, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(14)
        ))
        setContentView(root)
        requestConsentAndLoadAds(root)
    }

    private fun requestConsentAndLoadAds(root: LinearLayout) {
        val consentInformation = UserMessagingPlatform.getConsentInformation(this)
        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(true)
            .build()

        consentInformation.requestConsentInfoUpdate(
            this,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) {
                    if (consentInformation.canRequestAds()) initializeAds(root)
                }
            },
            {
                if (consentInformation.canRequestAds()) initializeAds(root)
            }
        )
        if (consentInformation.canRequestAds()) initializeAds(root)
    }

    private fun initializeAds(root: LinearLayout) {
        if (adsInitialized || isFinishing || isDestroyed) return
        adsInitialized = true
        try {
            MobileAds.initialize(this) {
                runOnUiThread {
                    if (isFinishing || isDestroyed || adView != null) return@runOnUiThread
                    val banner = AdView(this).apply {
                        // Official Google test banner ID. Replace only after AdMob setup.
                        adUnitId = "ca-app-pub-3940256099942544/9214589741"
                        setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
                            this@MainActivity,
                            (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt()
                        ))
                    }
                    adView = banner
                    root.addView(banner, LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                    ))
                    banner.loadAd(AdRequest.Builder().build())
                }
            }
        } catch (_: Exception) {
            // App still works if ads are unavailable.
        }
    }

    override fun onBackPressed() {
        if (::webView.isInitialized && webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onDestroy() {
        adView?.destroy()
        if (::webView.isInitialized) webView.destroy()
        super.onDestroy()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
