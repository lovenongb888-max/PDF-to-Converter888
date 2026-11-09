package com.converter888.pdftoconverter

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            var showExitDialog by remember { mutableStateOf(false) }
            var selectedFileName by remember { mutableStateOf<String?>(null) }
            var selectedFileUri by remember { mutableStateOf<Uri?>(null) }

            // ใช้ Android Storage Access Framework เพื่อเปิดตัวเลือกไฟล์ของเครื่อง
            val pdfPicker = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.OpenDocument()
            ) { uri: Uri? ->
                if (uri != null) {
                    selectedFileUri = uri
                    selectedFileName = getDisplayName(uri)
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    } catch (_: SecurityException) {
                        // ผู้ให้บริการไฟล์บางรายไม่รองรับ persistable permission
                    }
                }
            }

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
                        Text("✕", fontSize = 24.sp, color = Color.White)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "PDF", fontSize = 58.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "PDF to Converter888",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "เลือกไฟล์ PDF จากอุปกรณ์ของคุณ",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                        Spacer(modifier = Modifier.height(36.dp))
                        Button(
                            onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                        ) {
                            Text(
                                "เลือกไฟล์ PDF",
                                color = Color(0xFF8E24AA),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        selectedFileName?.let { name ->
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = "ไฟล์ที่เลือก: $name",
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (showExitDialog) {
                        AlertDialog(
                            onDismissRequest = { showExitDialog = false },
                            title = { Text("ปิดแอปและล้างแคช?") },
                            text = {
                                Text("ระบบจะลบไฟล์แคชของแอป แล้วปิดแอปพลิเคชัน")
                            },
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

    private fun getDisplayName(uri: Uri): String {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameColumn >= 0 && cursor.moveToFirst()) {
                return cursor.getString(nameColumn) ?: "PDF ที่เลือก"
            }
        }
        return uri.lastPathSegment ?: "PDF ที่เลือก"
    }

    private fun clearAppCache(context: Context) {
        try {
            context.cacheDir.deleteRecursively()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
