package com.converter888.pdftoconverter

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.system.exitProcess

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // ซ่อน Action Bar ด้านบนสุดเพื่อความสวยงามเต็มหน้าจอ
        actionBar?.hide()
        
        setContent {
            PDFConverterApp(onExitApp = {
                finishAffinity()
                exitProcess(0)
            })
        }
    }
}

@Composable
fun PDFConverterApp(onExitApp: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var selectedPdfUri by remember { mutableStateOf<Uri?>(null) }
    var pdfName by remember { mutableStateOf("") }
    var pdfSize by remember { mutableStateOf("") }
    var pdfPageCount by remember { mutableStateOf(0) }
    var convertedImages by remember { mutableStateOf<List<File>>(emptyList()) }
    var isConverting by remember { mutableStateOf(false) }
    
    // สถานะความละเอียด: 0 = ปกติ (100 DPI), 1 = คมชัดสูง (200 DPI), 2 = ชัดพิเศษ (300 DPI)
    var selectedResolutionIndex by remember { mutableStateOf(1) }
    val resolutions = listOf("ปกติ (100 DPI)", "คมชัดสูง (200 DPI)", "ชัดพิเศษ (300 DPI)")

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPdfUri = it
            convertedImages = emptyList() 
            
            val cursor = context.contentResolver.query(it, null, null, null, null)
            cursor?.use { c ->
                if (c.moveToFirst()) {
                    val nameIndex = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    val sizeIndex = c.getColumnIndex(OpenableColumns.SIZE)
                    pdfName = if (nameIndex != -1) c.getString(nameIndex) else "Unknown File"
                    val sizeInBytes = if (sizeIndex != -1) c.getLong(sizeIndex) else 0L
                    pdfSize = String.format("%.2f MB", sizeInBytes / (1024.0 * 1024.0))
                }
            }
            pdfPageCount = getPdfPageCount(context, it)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF4A148C), Color(0xFFD81B60))
                )
            )
    ) {
        // ปุ่มกากบาท (X) ปิดแอป
        IconButton(
            onClick = onExitApp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 40.dp, end = 20.dp)
        ) {
            Text("✕", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (selectedPdfUri == null) {
                // หน้าแรกหลัก
                Text(
                    text = "PDF",
                    color = Color.White,
                    fontSize = 72.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PDF to Converter888",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "เลือกไฟล์ PDF จากอุปกรณ์ของคุณ",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 32.dp, vertical = 12.dp)
                ) {
                    Text(text = "เลือกไฟล์ PDF", color = Color(0xFF4A148C), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                // หน้าตัวเลือกเมื่อเลือกไฟล์เสร็จแล้ว
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("📄 รายละเอียดไฟล์ PDF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("ชื่อ: $pdfName", color = Color.White)
                        Text("ขนาด: $pdfSize", color = Color.White)
                        Text("จำนวนหน้าทั้งหมด: $pdfPageCount หน้า", color = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // ส่วนเลือกความละเอียด
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("⚙️ เลือกความละเอียดคมชัด:", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            resolutions.forEachIndexed { index, text ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { selectedResolutionIndex = index }
                                ) {
                                    RadioButton(
                                        selected = (selectedResolutionIndex == index),
                                        onClick = { selectedResolutionIndex = index },
                                        colors = RadioButtonDefaults.colors(selectedColor = Color.White, unselectedColor = Color.White.copy(alpha = 0.6f))
                                    )
                                    Text(
                                        text = text.split(" ")[0], // เอาเฉพาะคำแรก เช่น ปกติ, คมชัดสูง
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (isConverting) {
                    CircularProgressIndicator(color = Color.White)
                    Text("กำลังประมวลผลและแปลงไฟล์...", color = Color.White, modifier = Modifier.padding(top = 8.dp))
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = {
                                scope.launch {
                                    isConverting = true
                                    val scale = when(selectedResolutionIndex) {
                                        0 -> 1.0f  // 100 DPI
                                        1 -> 2.0f  // 200 DPI
                                        2 -> 3.0f  // 300 DPI
                                        else -> 2.0f
                                    }
                                    convertedImages = withContext(Dispatchers.IO) {
