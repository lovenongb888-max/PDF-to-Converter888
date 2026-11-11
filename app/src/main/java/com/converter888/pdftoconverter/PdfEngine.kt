package com.chujai888.pdftoconverter888

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.provider.OpenableColumns
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.InvalidPasswordException
import java.io.Closeable
import java.io.File
import kotlin.math.roundToInt

enum class ImgFormat(val ext: String, val mime: String, val label: String) {
    PNG("png", "image/png", "PNG"),
    JPG("jpg", "image/jpeg", "JPG")
}

/** เอกสาร PDF ที่เปิดอยู่ (อ่านจากไฟล์ชั่วคราวใน cache เท่านั้น) */
class PdfDoc(val file: File) : Closeable {
    private val pfd: ParcelFileDescriptor =
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    private val renderer: PdfRenderer

    init {
        try {
            renderer = PdfRenderer(pfd)
        } catch (e: Throwable) {
            pfd.close()
            throw e
        }
    }

    val pageCount: Int = renderer.pageCount

    @Volatile
    private var closed = false

    @Synchronized
    fun pageWidthPt(index: Int): Int {
        check(!closed) { "document closed" }
        val page = renderer.openPage(index)
        try {
            return page.width
        } finally {
            page.close()
        }
    }

    /** เรนเดอร์หน้าเป็น Bitmap พื้นขาว กว้าง [targetWidth] พิกเซล (จำกัดด้านยาวสุด [maxDim]) */
    @Synchronized
    fun render(index: Int, targetWidth: Int, maxDim: Int = 6000): Bitmap {
        check(!closed) { "document closed" }
        val page = renderer.openPage(index)
        try {
            var w = targetWidth.coerceAtLeast(100)
            var h = (w * page.height.toFloat() / page.width).roundToInt().coerceAtLeast(1)
            val longest = maxOf(w, h)
            if (longest > maxDim) {
                val f = maxDim.toFloat() / longest
                w = (w * f).roundToInt().coerceAtLeast(1)
                h = (h * f).roundToInt().coerceAtLeast(1)
            }
            val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            bmp.eraseColor(Color.WHITE)
            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bmp
        } finally {
            page.close()
        }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        runCatching { renderer.close() }
        runCatching { pfd.close() }
    }
}

object PdfEngine {

    private fun workDir(ctx: Context): File = File(ctx.cacheDir, "work").apply { mkdirs() }

    /** ลบไฟล์แคชทั้งหมดที่แอพเก็บไว้ในเครื่อง */
    fun clearCache(ctx: Context) {
        runCatching { ctx.cacheDir.listFiles()?.forEach { it.deleteRecursively() } }
        runCatching { ctx.externalCacheDir?.listFiles()?.forEach { it.deleteRecursively() } }
    }

    fun displayName(ctx: Context, uri: Uri): String {
        runCatching {
            ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) {
                        val n = c.getString(0)
                        if (!n.isNullOrBlank()) return n
                    }
                }
        }
        return uri.lastPathSegment?.substringAfterLast('/') ?: "document.pdf"
    }

    /** คัดลอกไฟล์ที่ผู้ใช้เลือกเข้า cache ชั่วคราว (จะถูกลบเมื่อปิดแอพ) */
    fun importUri(ctx: Context, uri: Uri): File {
        val out = File(workDir(ctx), "src_${System.currentTimeMillis()}.pdf")
        try {
            val input = ctx.contentResolver.openInputStream(uri)
                ?: throw IllegalStateException("เปิดไฟล์ไม่ได้")
            input.use { i -> out.outputStream().use { o -> i.copyTo(o) } }
            if (!out.isFile || out.length() == 0L) {
                throw IllegalStateException("ไฟล์ PDF ว่างเปล่าหรืออ่านไม่ได้")
            }
            return out
        } catch (e: Throwable) {
            out.delete()
            throw e
        }
    }

    /** ตรวจว่าไฟล์ต้องใช้รหัสผ่านหรือไม่ */
    fun needsPassword(file: File): Boolean = try {
        PDDocument.load(file).close()
        false
    } catch (e: InvalidPasswordException) {
        true
    } catch (e: Throwable) {
        false
    }

    /** ปลดรหัสผ่านด้วยรหัสที่ผู้ใช้กรอก แล้วเก็บสำเนาที่ปลดล็อกแล้วไว้ใน cache ชั่วคราว
     *  โยน InvalidPasswordException ถ้ารหัสไม่ถูกต้อง */
    fun decrypt(ctx: Context, src: File, password: String): File {
        val doc = PDDocument.load(src, password)
        try {
            doc.setAllSecurityToBeRemoved(true)
            val out = File(workDir(ctx), "dec_${System.currentTimeMillis()}.pdf")
            doc.save(out)
            return out
        } finally {
            doc.close()
        }
    }

    /** แปลงหน้าที่เลือกเป็นรูป แล้วบันทึกที่ Pictures/PDFtoConverter888 คืนจำนวนรูปที่บันทึก */
    fun export(
        ctx: Context,
        doc: PdfDoc,
        pages: List<Int>,
        fmt: ImgFormat,
        quality: Int,
        scale: Float,
        baseName: String,
        onProgress: (Int) -> Unit
    ): Int {
        var count = 0
        for (p in pages) {
            val w = (doc.pageWidthPt(p) * scale).roundToInt()
            val bmp = doc.render(p, w)
            try {
                save(ctx, bmp, fmt, quality, "${safeName(baseName)}_p${p + 1}")
            } finally {
                bmp.recycle()
            }
            count++
            onProgress(count)
        }
        return count
    }

    private fun safeName(name: String): String =
        name.removeSuffix(".pdf").removeSuffix(".PDF")
            .replace(Regex("[^\\w\\-\\u0E00-\\u0E7F ]"), "_")
            .trim()
            .ifEmpty { "document" }

    private fun save(ctx: Context, bmp: Bitmap, fmt: ImgFormat, quality: Int, name: String) {
        val resolver = ctx.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.${fmt.ext}")
            put(MediaStore.Images.Media.MIME_TYPE, fmt.mime)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                Environment.DIRECTORY_PICTURES + "/PDFtoConverter888"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: throw IllegalStateException("บันทึกรูปไม่สำเร็จ")
        try {
            val out = resolver.openOutputStream(uri)
                ?: throw IllegalStateException("บันทึกรูปไม่สำเร็จ")
            out.use {
                val cf = if (fmt == ImgFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                val encoded = bmp.compress(cf, if (fmt == ImgFormat.PNG) 100 else quality, it)
                check(encoded) { "เข้ารหัสรูปภาพไม่สำเร็จ" }
            }
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        } catch (e: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw e
        }
    }
}
