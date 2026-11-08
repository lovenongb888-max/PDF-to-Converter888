package com.converter888.pdftoconverter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAppUpdate(this)

        setContent {
            var showExitDialog by remember { mutableStateOf(false) }

            MaterialTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF8E24AA), Color(0xFFD81B60))
                            )
                        )
                ) {
                    IconButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Text("❌", fontSize = 24.sp, color = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "📄 ➔ 🖼️", fontSize = 64.sp, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = "PDF to Converter888", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "แปลงไฟล์ปลอดภัย ไร้การบันทึกข้อมูล", fontSize = 14.sp, color = Color.White.copy(alpha = 0.8f))
                        Spacer(modifier = Modifier.height(48.dp))
                        Button(
                            onClick = { },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text("เลือกไฟล์ PDF", color = Color(0xFF8E24AA), fontWeight = FontWeight.Bold)
                        }
                    }

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("ปิดแอปและล้างแคช?") },
                            text = { Text("ระบบจะลบไฟล์แคชที่เก็บไว้ในเครื่องทั้งหมด แล้วทำการปิดแอปพลิเคชัน") },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        clearAppCache(this@MainActivity)
                                        finishAffinity()
                                        exitProcess(0)
                                    }
                                ) {
                                    Text("ปิดแอป")
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showExitDialog = false }) {
                                    Text("ยกเลิก")
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun clearAppCache(context: Context) {
        try {
            context.cacheDir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun checkAppUpdate(context: Context) {
        val currentVersion = "1.0.0"
        val repoUrl = "https://github.com"
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val client = OkHttpClient()
                val request = Request.Builder().url(repoUrl).build()
                val response = client.newCall(request).execute()
                val body = response.body?.string()
                
                if (!body.isNullOrEmpty()) {
                    val json = JSONObject(body)
                    val latestVersion = json.getString("tag_name").replace("v", "")
                    val downloadUrl = json.getString("html_url")
                    
                    if (latestVersion != currentVersion) {
                        launch(Dispatchers.Main) {
                            Toast.makeText(context, "พบเวอร์ชันใหม่ v$latestVersion กำลังเปิดเบราว์เซอร์เพื่อดาวน์โหลด", Toast.LENGTH_LONG).show()
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                            context.startActivity(intent)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
