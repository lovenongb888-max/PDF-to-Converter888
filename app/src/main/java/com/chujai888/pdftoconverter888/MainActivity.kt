package com.chujai888.pdftoconverter888

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PDFBoxResourceLoader.init(applicationContext)
        // เริ่มต้นสะอาดทุกครั้ง: ไม่เหลือไฟล์ค้างจากการใช้งานครั้งก่อน
        PdfEngine.clearCache(this)
        enableEdgeToEdge()
        val initial = intent?.data
        setContent { PdfApp(initialUri = initial, onExit = ::exitApp) }
    }

    private fun exitApp() {
        PdfEngine.clearCache(this)
        finishAndRemoveTask()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) PdfEngine.clearCache(this)
    }
}
