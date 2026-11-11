package com.chujai888.pdftoconverter888

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.roundToInt

// ---------- สีและธีม ----------
private val Violet = Color(0xFF5B3DF5)
private val Magenta = Color(0xFFB23BFF)
private val Coral = Color(0xFFFF7A59)
private val Surface = Color(0xFFF6F4FF)
private val Ink = Color(0xFF1B1530)
private val Muted = Color(0xFF6E6A85)
private val BrandBrush = Brush.linearGradient(listOf(Violet, Magenta))

private val AppColors = lightColorScheme(
    primary = Violet,
    secondary = Coral,
    background = Surface,
    surface = Color.White,
    onSurface = Ink
)

// ---------- แอพหลัก ----------
@Composable
fun PdfApp(initialUri: Uri? = null, onExit: () -> Unit) {
    MaterialTheme(colorScheme = AppColors) {
        PdfAppContent(initialUri, onExit)
    }
}

@Composable
private fun PdfAppContent(initialUri: Uri?, onExit: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var doc by remember { mutableStateOf<PdfDoc?>(null) }
    var fileName by remember { mutableStateOf("") }
    var page by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<File?>(null) } // ไฟล์ที่รอรหัสผ่าน
    var pwWrong by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var showExit by remember { mutableStateOf(false) }
    var showConvert by remember { mutableStateOf(false) }
    var update by remember { mutableStateOf<UpdateInfo?>(null) }

    fun toast(t: String) = Toast.makeText(ctx, t, Toast.LENGTH_LONG).show()

    fun openFile(f: File) {
        scope.launch {
            busy = true
            val outcome = withContext(Dispatchers.IO) {
                try {
                    Result.success(PdfDoc(f))
                } catch (e: Throwable) {
                    Result.failure(e)
                }
            }
            val d = outcome.getOrNull()
            if (d != null) {
                doc?.close()
                doc = d
                page = 0
            } else {
                val e = outcome.exceptionOrNull()
                val needPw = e is SecurityException ||
                    withContext(Dispatchers.IO) { PdfEngine.needsPassword(f) }
                if (needPw) {
                    pending = f
                    pwWrong = false
                } else {
                    toast("เปิดไฟล์ไม่ได้ ไฟล์อาจเสียหายหรือไม่ใช่ PDF")
                }
            }
            busy = false
        }
    }

    fun openUri(uri: Uri) {
        scope.launch {
            busy = true
            val r = withContext(Dispatchers.IO) {
                runCatching { PdfEngine.importUri(ctx, uri) to PdfEngine.displayName(ctx, uri) }
            }
            busy = false
            r.onSuccess { (f, name) ->
                fileName = name
                openFile(f)
            }.onFailure { toast("เปิดไฟล์ไม่ได้") }
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) openUri(uri)
    }

    LaunchedEffect(Unit) {
        if (initialUri != null) openUri(initialUri)
        update = Updater.check(BuildConfig.VERSION_NAME)
    }

    BackHandler {
        when {
            showConvert -> showConvert = false
            doc != null -> {
                doc?.close()
                doc = null
                PdfEngine.clearCache(ctx)
            }
            else -> showExit = true
        }
    }

    Box(Modifier.fillMaxSize().background(Surface)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(onClose = { showExit = true })
            val d = doc
            if (d == null) {
                HomeScreen(onOpen = { picker.launch(arrayOf("application/pdf")) })
            } else {
                ViewerScreen(
                    doc = d,
                    fileName = fileName,
                    page = page,
                    onPage = { page = it },
                    onConvert = { showConvert = true },
                    onOpenAnother = { picker.launch(arrayOf("application/pdf")) }
                )
            }
        }

        if (busy) {
            Box(
                Modifier.fillMaxSize().background(Color(0x66000000))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White).padding(28.dp)
                ) { CircularProgressIndicator(color = Violet) }
            }
        }
    }

    val d = doc
    if (showConvert && d != null) {
        ConvertSheet(
            doc = d,
            currentPage = page,
            baseName = fileName,
            onDismiss = { showConvert = false }
        )
    }

    // รหัสผ่าน PDF (ผู้ใช้กรอกเอง ไม่เก็บบันทึก)
    val pf = pending
    if (pf != null) {
        PasswordDialog(
            wrong = pwWrong,
            onCancel = {
                pending = null
                pf.delete()
            },
            onSubmit = { pw ->
                scope.launch {
                    busy = true
                    val r = withContext(Dispatchers.IO) {
                        try {
                            val dec = PdfEngine.decrypt(ctx, pf, pw)
                            Result.success(PdfDoc(dec))
                        } catch (e: Throwable) {
                            Result.failure(e)
                        }
                    }
                    busy = false
                    val nd = r.getOrNull()
                    if (nd != null) {
                        doc?.close()
                        doc = nd
                        page = 0
                        pending = null
                    } else if (r.exceptionOrNull() is InvalidPasswordException) {
                        pwWrong = true
                    } else {
                        pending = null
                        toast("ปลดล็อกไฟล์ไม่สำเร็จ")
                    }
                }
            }
        )
    }

    if (showExit) {
        AlertDialog(
            onDismissRequest = { showExit = false },
            containerColor = Color.White,
            title = { Text("ปิดแอพและล้างแคช?", fontWeight = FontWeight.Bold) },
            text = { Text("ระบบจะลบไฟล์แคชที่เก็บไว้ในเครื่อง แล้วปิดแอพ") },
            confirmButton = {
                TextButton(onClick = {
                    showExit = false
                    doc?.close()
                    onExit()
                }) { Text("ปิดแอพ", color = Coral, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExit = false }) { Text("ยกเลิก") }
            }
        )
    }

    val u = update
    if (u != null) {
        AlertDialog(
            onDismissRequest = { update = null },
            containerColor = Color.White,
            title = { Text("มีเวอร์ชันใหม่ ${u.version}", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (u.notes.isBlank()) "แนะนำให้อัปเดตเพื่อรับฟีเจอร์และการแก้ไขล่าสุด"
                    else u.notes.take(300)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    runCatching {
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u.url)))
                    }
                    update = null
                }) { Text("อัปเดต", color = Violet, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { update = null }) { Text("ภายหลัง") } }
        )
    }
}

// ---------- โลโก้ ----------
@Composable
private fun LogoMark(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.26f))
            .background(BrandBrush),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher_foreground),
            contentDescription = "โลโก้ PDF to Converter888",
            modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = 1.55f, scaleY = 1.55f)
        )
    }
}

// ---------- แถบบน + ปุ่มปิดแอพมุมขวาบน ----------
@Composable
private fun TopBar(onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(BrandBrush)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) { LogoMark(34.dp) }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("PDF to Converter888", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text("เปิดและแปลง PDF เป็นรูปภาพ", color = Color(0xCCFFFFFF), fontSize = 12.sp)
        }
        IconButton(
            onClick = onClose,
            modifier = Modifier.size(42.dp).clip(CircleShape).background(Color(0x33FFFFFF))
        ) {
            Icon(Icons.Filled.Close, contentDescription = "ปิดแอพ", tint = Color.White)
        }
    }
}

// ---------- หน้าแรก ----------
@Composable
private fun HomeScreen(onOpen: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 28.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LogoMark(112.dp)
        Spacer(Modifier.height(18.dp))
        Text("PDF to Converter888", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Ink)
        Spacer(Modifier.height(6.dp))
        Text(
            "เปิดไฟล์ PDF แล้วแปลงเป็น PNG หรือ JPG\nเลือกคุณภาพของภาพได้ตามต้องการ",
            textAlign = TextAlign.Center,
            color = Muted,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(26.dp))
        GradientButton("เปิดไฟล์ PDF", onClick = onOpen)
        Spacer(Modifier.height(26.dp))

        FeatureCard("🔒", "ไม่ล็อกอิน ไม่เก็บข้อมูล", "ไฟล์ประมวลผลในเครื่อง และลบทิ้งเมื่อปิดแอพ")
        Spacer(Modifier.height(10.dp))
        FeatureCard("🖼️", "แปลงเป็น PNG / JPG", "เลือกความละเอียดและคุณภาพของภาพได้")
        Spacer(Modifier.height(10.dp))
        FeatureCard("🔑", "รองรับ PDF ที่มีรหัสผ่าน", "กรอกรหัสผ่านเองทุกครั้ง แอพไม่จดจำ")

        Spacer(Modifier.height(24.dp))
        Text(
            "ผู้พัฒนา: Chujai888  ·  v${BuildConfig.VERSION_NAME}",
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun FeatureCard(emoji: String, title: String, desc: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFEFEAFF)),
            contentAlignment = Alignment.Center
        ) { Text(emoji, fontSize = 22.sp) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 15.sp)
            Text(desc, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) BrandBrush else Brush.linearGradient(listOf(Color(0xFFBDB7D6), Color(0xFFBDB7D6))))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

// ---------- หน้าดู PDF ----------
@Composable
private fun ViewerScreen(
    doc: PdfDoc,
    fileName: String,
    page: Int,
    onPage: (Int) -> Unit,
    onConvert: () -> Unit,
    onOpenAnother: () -> Unit
) {
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var scale by remember(page) { mutableFloatStateOf(1f) }
    var offset by remember(page) { mutableStateOf(Offset.Zero) }

    LaunchedEffect(doc, page) {
        bmp = withContext(Dispatchers.IO) {
            runCatching { doc.render(page, 1400) }.getOrNull()
        } ?: bmp
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                fileName,
                modifier = Modifier.weight(1f),
                color = Ink,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                fontSize = 14.sp
            )
            TextButton(onClick = onOpenAnother) { Text("เปิดไฟล์อื่น", color = Violet) }
        }

        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE6E2F5))
                .clipToBounds()
                .pointerInput(page) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, 5f)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val b = bmp
            if (b != null) {
                Image(
                    bitmap = b.asImageBitmap(),
                    contentDescription = "หน้า ${page + 1}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            } else {
                CircularProgressIndicator(color = Violet)
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                RoundIconButton(enabled = page > 0, onClick = { onPage(page - 1) }) {
                    Icon(Icons.Filled.ArrowBack, "หน้าก่อนหน้า", tint = Color.White)
                }
                Text(
                    "หน้า ${page + 1} / ${doc.pageCount}",
                    fontWeight = FontWeight.SemiBold,
                    color = Ink
                )
                RoundIconButton(enabled = page < doc.pageCount - 1, onClick = { onPage(page + 1) }) {
                    Icon(Icons.Filled.ArrowForward, "หน้าถัดไป", tint = Color.White)
                }
            }
            GradientButton("แปลงเป็นรูปภาพ PNG / JPG", onClick = onConvert)
        }
    }
}

@Composable
private fun RoundIconButton(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(if (enabled) Violet else Color(0xFFBDB7D6))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

// ---------- แผงแปลงไฟล์ ----------
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConvertSheet(doc: PdfDoc, currentPage: Int, baseName: String, onDismiss: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var fmt by remember { mutableStateOf(ImgFormat.JPG) }
    var scale by remember { mutableFloatStateOf(2f) }
    var quality by remember { mutableFloatStateOf(90f) }
    var all by remember { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    var done by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<String?>(null) }

    val chipColors = FilterChipDefaults.filterChipColors(
        selectedContainerColor = Color(0xFFE9E2FF),
        selectedLabelColor = Violet
    )

    ModalBottomSheet(
        onDismissRequest = { if (!running) onDismiss() },
        containerColor = Color.White
    ) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("แปลงเป็นรูปภาพ", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Ink)

            SectionLabel("รูปแบบไฟล์")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ImgFormat.values().forEach { f ->
                    FilterChip(
                        selected = fmt == f,
                        onClick = { fmt = f },
                        label = { Text(f.label) },
                        colors = chipColors,
                        enabled = !running
                    )
                }
            }

            SectionLabel("ความละเอียด")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.5f to "ปกติ", 2f to "สูง", 3f to "สูงมาก").forEach { (s, label) ->
                    FilterChip(
                        selected = scale == s,
                        onClick = { scale = s },
                        label = { Text("$label (${(72 * s).roundToInt()} dpi)") },
                        colors = chipColors,
                        enabled = !running
                    )
                }
            }

            if (fmt == ImgFormat.JPG) {
                SectionLabel("คุณภาพภาพ JPG: ${quality.roundToInt()}%")
                Slider(
                    value = quality,
                    onValueChange = { quality = it },
                    valueRange = 40f..100f,
                    enabled = !running,
                    colors = SliderDefaults.colors(thumbColor = Violet, activeTrackColor = Violet)
                )
                Text(
                    "ยิ่งสูงภาพยิ่งคมชัด แต่ไฟล์ใหญ่ขึ้น",
                    color = Muted,
                    fontSize = 12.sp
                )
            } else {
                Text(
                    "PNG เก็บคุณภาพเต็ม ไม่สูญเสียรายละเอียด (ไฟล์จะใหญ่กว่า JPG)",
                    color = Muted,
                    fontSize = 12.sp
                )
            }

            SectionLabel("หน้าที่ต้องการแปลง")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !all,
                    onClick = { all = false },
                    label = { Text("หน้านี้ (${currentPage + 1})") },
                    colors = chipColors,
                    enabled = !running
                )
                FilterChip(
                    selected = all,
                    onClick = { all = true },
                    label = { Text("ทุกหน้า (${doc.pageCount})") },
                    colors = chipColors,
                    enabled = !running
                )
            }

            if (running) {
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                    color = Violet
                )
                Text("กำลังแปลง $done / $total", color = Muted, fontSize = 13.sp)
            }

            result?.let {
                Text(it, color = if (it.startsWith("บันทึก")) Violet else Coral, fontWeight = FontWeight.SemiBold)
            }

            GradientButton(
                text = if (running) "กำลังแปลง..." else "เริ่มแปลง",
                enabled = !running,
                onClick = {
                    scope.launch {
                        val pages = if (all) (0 until doc.pageCount).toList() else listOf(currentPage)
                        running = true
                        result = null
                        done = 0
                        total = pages.size
                        val r = withContext(Dispatchers.IO) {
                            runCatching {
                                PdfEngine.export(
                                    ctx, doc, pages, fmt, quality.roundToInt(), scale, baseName
                                ) { done = it }
                            }
                        }
                        running = false
                        result = r.fold(
                            { "บันทึกแล้ว $it รูป ที่โฟลเดอร์ Pictures/PDFtoConverter888" },
                            { "แปลงไม่สำเร็จ: ${it.message ?: "เกิดข้อผิดพลาด"}" }
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, fontWeight = FontWeight.SemiBold, color = Ink, fontSize = 14.sp)
}

// ---------- กรอกรหัสผ่าน ----------
@Composable
private fun PasswordDialog(wrong: Boolean, onCancel: () -> Unit, onSubmit: (String) -> Unit) {
    var pw by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancel,
        containerColor = Color.White,
        title = { Text("ไฟล์นี้มีรหัสผ่าน", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("กรอกรหัสผ่านเพื่อเปิดไฟล์ (แอพไม่จดจำรหัสผ่านของคุณ)", fontSize = 13.sp, color = Muted)
                OutlinedTextField(
                    value = pw,
                    onValueChange = { pw = it },
                    singleLine = true,
                    isError = wrong,
                    label = { Text("รหัสผ่าน") },
                    visualTransformation = if (show) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        TextButton(onClick = { show = !show }) { Text(if (show) "ซ่อน" else "แสดง", color = Violet) }
                    }
                )
                if (wrong) Text("รหัสผ่านไม่ถูกต้อง ลองใหม่อีกครั้ง", color = Coral, fontSize = 12.sp)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pw) }, enabled = pw.isNotEmpty()) {
                Text("เปิดไฟล์", color = Violet, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text("ยกเลิก") } }
    )
}
