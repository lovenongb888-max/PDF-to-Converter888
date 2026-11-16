package com.chujai888.pdftoconverter888
import androidx.compose.runtime.withFrameNanos

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay
import kotlin.math.abs
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

    // ขณะอ่านไฟล์: ซ่อนแถบสถานะ/แถบนำทาง เต็มจอ และไม่ให้จอดับ
    val view = LocalView.current
    val reading = doc != null
    DisposableEffect(reading) {
        val window = ctx.findActivity()?.window
        val ctrl = window?.let { WindowCompat.getInsetsController(it, view) }
        if (reading) {
            ctrl?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            ctrl?.hide(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = true
        }
        onDispose {
            ctrl?.show(WindowInsetsCompat.Type.systemBars())
            view.keepScreenOn = false
        }
    }

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
        val d = doc
        if (d == null) {
            Column(Modifier.fillMaxSize()) {
                TopBar(onClose = { showExit = true })
                HomeScreen(onOpen = { picker.launch(arrayOf("application/pdf")) })
            }
        } else {
            key(d) {
                ReaderScreen(
                    doc = d,
                    onConvert = { p ->
                        page = p
                        showConvert = true
                    },
                    onOpenAnother = { picker.launch(arrayOf("application/pdf")) },
                    onClose = { showExit = true }
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

// ---------- โหมดอ่านต่อเนื่อง (เต็มจอ ไม่มีแถบบังหน้า) ----------
private val VioletLight = Color(0xFFA894FF)

private enum class ReadMode(val label: String, val bg: Color, val page: Color, val filter: ColorFilter?) {
    NORMAL("ปกติ", Color(0xFFE4E2EC), Color.White, null),
    SEPIA(
        "ถนอมสายตา", Color(0xFFD8CCB2), Color(0xFFFAEBBD),
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    0.98f, 0f, 0f, 0f, 0f,
                    0f, 0.92f, 0f, 0f, 0f,
                    0f, 0f, 0.74f, 0f, 0f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    ),
    NIGHT(
        "กลางคืน", Color(0xFF050507), Color(0xFF121212),
        ColorFilter.colorMatrix(
            ColorMatrix(
                floatArrayOf(
                    -0.7725f, 0f, 0f, 0f, 215f,
                    0f, -0.7725f, 0f, 0f, 215f,
                    0f, 0f, -0.7725f, 0f, 215f,
                    0f, 0f, 0f, 1f, 0f
                )
            )
        )
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun ReaderScreen(
    doc: PdfDoc,
    onConvert: (Int) -> Unit,
    onOpenAnother: () -> Unit,
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val hScroll = rememberScrollState()

    var zoom by remember { mutableFloatStateOf(1f) }
    var mode by remember { mutableStateOf(ReadMode.NORMAL) }
    var chrome by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var menuOpen by remember { mutableStateOf(false) }
    var gotoOpen by remember { mutableStateOf(false) }
    var badge by remember { mutableStateOf(false) }
    var viewportW by remember { mutableIntStateOf(1) }
    // ตำแหน่งที่ต้องเลื่อนไปหลังเปลี่ยนระดับซูม: [index, offset, horizontal]
    var pending by remember { mutableStateOf<IntArray?>(null) }

    // หน้าที่อยู่กลางจอ
    val current by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo
                .firstOrNull { center >= it.offset && center <= it.offset + it.size }?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    val applyZoom: (Float) -> Unit = { target ->
        val nz = target.coerceIn(1f, 4f)
        if (abs(nz - zoom) > 0.001f) {
            val ratio = nz / zoom
            val p = pending
            val idx = p?.get(0) ?: listState.firstVisibleItemIndex
            val off = p?.get(1) ?: listState.firstVisibleItemScrollOffset
            val hsv = p?.get(2) ?: hScroll.value
            pending = intArrayOf(
                idx,
                (off * ratio).roundToInt(),
                ((hsv + viewportW / 2f) * ratio - viewportW / 2f).roundToInt().coerceAtLeast(0)
            )
            zoom = nz
        }
    }
    val applyZoomState = rememberUpdatedState(applyZoom)

    // หลังซูม: รักษาตำแหน่งที่กำลังอ่านอยู่ไว้
    LaunchedEffect(zoom) {
        val p = pending ?: return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        listState.scrollToItem(p[0], p[1])
        hScroll.scrollTo(p[2])
        if (pending === p) pending = null
    }

    // เมนูลอยซ่อนเองหลัง 4 วินาที
    LaunchedEffect(chrome, tick, menuOpen, gotoOpen) {
        if (chrome && !menuOpen && !gotoOpen) {
            delay(4000)
            chrome = false
        }
    }
    // ป้ายเลขหน้าเล็กๆ ขณะเลื่อน
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            badge = true
        } else {
            delay(1200)
            badge = false
        }
    }
    LaunchedEffect(Unit) {
        Toast.makeText(ctx, "แตะหน้าจอเพื่อแสดงเมนู · แตะสองครั้งเพื่อซูม", Toast.LENGTH_LONG).show()
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(mode.bg)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .onSizeChanged { viewportW = it.width.coerceAtLeast(1) }
            // บีบสองนิ้วเพื่อซูม (นิ้วเดียวยังเลื่อนหน้าได้ตามปกติ)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.changes.count { it.pressed } > 1) {
                            val z = event.calculateZoom()
                            if (z != 1f) {
                                applyZoomState.value(zoom * z)
                                event.changes.forEach { it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { applyZoomState.value(if (zoom > 1.05f) 1f else 2.5f) },
                    onTap = {
                        chrome = !chrome
                        tick++
                    }
                )
            }
    ) {
        val vw = maxWidth
        val renderW = ((((viewportW * zoom).roundToInt()).coerceAtMost(2600) + 199) / 200) * 200

        Box(Modifier.fillMaxSize().horizontalScroll(hScroll)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.width(vw * zoom).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(doc.pageCount, key = { it }) { i ->
                    PageItem(doc, i, renderW, mode)
                }
            }
        }

        // ป้ายเลขหน้าชั่วคราวขณะเลื่อน
        AnimatedVisibility(
            visible = badge && !chrome,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 18.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) { Pill("${current + 1} / ${doc.pageCount}") }

        // ปุ่มลอยมุมขวาบน: เมนู + ปิดแอพ
        AnimatedVisibility(
            visible = chrome,
            modifier = Modifier.align(Alignment.TopEnd).padding(12.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box {
                    FloatBtn(onClick = { menuOpen = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "เมนู", tint = Color.White)
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("แปลงไฟล์เป็นรูป (PNG / JPG)") },
                            onClick = { menuOpen = false; onConvert(current) }
                        )
                        DropdownMenuItem(
                            text = { Text("ไปยังหน้า…") },
                            onClick = { menuOpen = false; gotoOpen = true }
                        )
                        DropdownMenuItem(
                            text = { Text("ซูมเข้า") },
                            onClick = { menuOpen = false; applyZoom(zoom * 1.3f) }
                        )
                        DropdownMenuItem(
                            text = { Text("ซูมออก") },
                            onClick = { menuOpen = false; applyZoom(zoom / 1.3f) }
                        )
                        DropdownMenuItem(
                            text = { Text("พอดีหน้าจอ") },
                            onClick = { menuOpen = false; applyZoom(1f) }
                        )
                        DropdownMenuItem(
                            text = {
                                val next = ReadMode.values()[(mode.ordinal + 1) % ReadMode.values().size]
                                Text("โหมดอ่าน: ${mode.label} → ${next.label}")
                            },
                            onClick = {
                                menuOpen = false
                                mode = ReadMode.values()[(mode.ordinal + 1) % ReadMode.values().size]
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("เปิดไฟล์อื่น") },
                            onClick = { menuOpen = false; onOpenAnother() }
                        )
                        DropdownMenuItem(
                            text = { Text("ปิดแอพ", color = Coral, fontWeight = FontWeight.Bold) },
                            onClick = { menuOpen = false; onClose() }
                        )
                    }
                }
                FloatBtn(onClick = onClose) {
                    Icon(Icons.Filled.Close, contentDescription = "ปิดแอพ", tint = Color.White)
                }
            }
        }

        // แถบควบคุมลอยด้านล่าง: ซูม + เลื่อนหน้า (โผล่เมื่อแตะจอ)
        AnimatedVisibility(
            visible = chrome,
            modifier = Modifier.align(Alignment.BottomCenter).padding(12.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                Modifier
                    .widthIn(max = 480.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xE61B1530))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ZoomBtn("−") { applyZoom(zoom / 1.3f); tick++ }
                        Text(
                            "${(zoom * 100).roundToInt()}%",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(60.dp)
                        )
                        ZoomBtn("+") { applyZoom(zoom * 1.3f); tick++ }
                    }
                    Text(
                        "หน้า ${current + 1} / ${doc.pageCount}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { gotoOpen = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
                if (doc.pageCount > 1) {
                    Slider(
                        value = current.toFloat(),
                        onValueChange = {
                            tick++
                            scope.launch { listState.scrollToItem(it.roundToInt()) }
                        },
                        valueRange = 0f..(doc.pageCount - 1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = VioletLight,
                            inactiveTrackColor = Color(0x44FFFFFF)
                        )
                    )
                }
            }
        }
    }

    if (gotoOpen) {
        GotoDialog(
            current = current + 1,
            count = doc.pageCount,
            onGo = { p ->
                gotoOpen = false
                scope.launch { listState.scrollToItem(p - 1) }
            },
            onDismiss = { gotoOpen = false }
        )
    }
}

@Composable
private fun PageItem(doc: PdfDoc, index: Int, renderW: Int, mode: ReadMode) {
    var bmp by remember(doc, index) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(doc, index, renderW) {
        if (bmp != null) delay(200) // รอให้หยุดซูมก่อนค่อยเรนเดอร์ใหม่
        val b = withContext(Dispatchers.IO) { runCatching { doc.render(index, renderW) }.getOrNull() }
        if (b != null) bmp = b
    }
    val (w, h) = doc.sizes[index]
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(w.toFloat() / h)
            .background(mode.page)
    ) {
        bmp?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "หน้า ${index + 1}",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds,
                colorFilter = mode.filter
            )
        }
    }
}

@Composable
private fun FloatBtn(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(46.dp)
            .clip(CircleShape)
            .background(Color(0xCC1B1530))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { content() }
}

@Composable
private fun ZoomBtn(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Text(label, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
}

@Composable
private fun Pill(text: String) {
    Text(
        text,
        color = Color.White,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(Color(0xB31B1530))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    )
}

@Composable
private fun GotoDialog(current: Int, count: Int, onGo: (Int) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val n = text.toIntOrNull()
    val valid = n != null && n in 1..count
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("ไปยังหน้า", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("กรอกเลขหน้า 1 – $count (ตอนนี้อยู่หน้า $current)", fontSize = 13.sp, color = Muted)
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.filter(Char::isDigit).take(6) },
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    label = { Text("เลขหน้า") },
                    modifier = Modifier.focusRequester(focus),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { if (valid) onGo(n!!) })
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onGo(n!!) }, enabled = valid) {
                Text("ไป", color = Violet, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("ยกเลิก") } }
    )
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
