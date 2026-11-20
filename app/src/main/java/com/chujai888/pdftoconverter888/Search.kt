package com.chujai888.pdftoconverter888

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

/** ตำแหน่งคำที่ค้นเจอ 1 จุด พิกัดเป็นสัดส่วน 0..1 ของหน้า (ซ้าย/บน/ขวา/ล่าง) */
data class SearchHit(
    val page: Int,
    val l: Float,
    val t: Float,
    val r: Float,
    val b: Float
)

/** ค้นหาข้อความในไฟล์ที่เปิดอยู่ ทำงานในเครื่องทั้งหมด ไม่ส่งข้อมูลออกไปไหน */
object PdfSearch {

    suspend fun search(
        doc: PdfDoc,
        query: String,
        limit: Int = 2000,
        onProgress: (Int) -> Unit
    ): List<SearchHit> = withContext(Dispatchers.IO) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return@withContext emptyList<SearchHit>()

        val out = ArrayList<SearchHit>()
        PDDocument.load(doc.file).use { pd ->
            val total = pd.numberOfPages
            for (p in 0 until total) {
                ensureActive()
                val stripper = HitStripper(q, p, out, limit)
                stripper.startPage = p + 1
                stripper.endPage = p + 1
                try {
                    stripper.getText(pd)
                } catch (e: Exception) {
                    // ข้ามหน้าที่อ่านข้อความไม่ได้ แล้วค้นหน้าถัดไปต่อ
                }
                onProgress(p + 1)
                if (out.size >= limit) break
            }
        }
        out
    }

    /** รวมตัวอักษรของแต่ละบรรทัด (ข้ามคำได้) แล้วหาคำค้น พร้อมคำนวณกรอบไฮไลต์ */
    private class HitStripper(
        private val query: String,
        private val page: Int,
        private val out: MutableList<SearchHit>,
        private val limit: Int
    ) : PDFTextStripper() {

        private val line = StringBuilder()
        private val pos = ArrayList<TextPosition?>()

        override fun writeString(text: String, textPositions: MutableList<TextPosition>) {
            if (textPositions.isEmpty() || text.isEmpty()) return
            for (i in text.indices) {
                line.append(text[i])
                val idx = (i * textPositions.size / text.length).coerceAtMost(textPositions.size - 1)
                pos.add(textPositions[idx])
            }
        }

        override fun writeWordSeparator() {
            line.append(' ')
            pos.add(null)
        }

        override fun writeLineSeparator() {
            flush()
        }

        override fun endPage(page: PDPage?) {
            flush()
        }

        private fun flush() {
            if (line.isNotEmpty() && out.size < limit) findInLine()
            line.setLength(0)
            pos.clear()
        }

        private fun findInLine() {
            val hay = line.toString().lowercase()
            if (hay.length != pos.size) return // ความยาวไม่ตรงกับตำแหน่ง ข้ามเพื่อไม่ให้กรอบเพี้ยน
            var from = 0
            while (out.size < limit) {
                val i = hay.indexOf(query, from)
                if (i < 0) break
                val end = i + query.length
                var l = Float.MAX_VALUE
                var t = Float.MAX_VALUE
                var r = 0f
                var b = 0f
                var pw = 0f
                var ph = 0f
                for (k in i until end) {
                    val p = pos[k] ?: continue
                    l = min(l, p.xDirAdj)
                    r = max(r, p.xDirAdj + p.widthDirAdj)
                    t = min(t, p.yDirAdj - p.heightDir)
                    b = max(b, p.yDirAdj)
                    pw = p.pageWidth
                    ph = p.pageHeight
                }
                if (pw > 0f && ph > 0f && l < r) {
                    out.add(
                        SearchHit(
                            page = page,
                            l = (l / pw).coerceIn(0f, 1f),
                            t = (t / ph).coerceIn(0f, 1f),
                            r = (r / pw).coerceIn(0f, 1f),
                            b = (b / ph).coerceIn(0f, 1f)
                        )
                    )
                }
                from = i + query.length.coerceAtLeast(1)
            }
        }
    }
}
