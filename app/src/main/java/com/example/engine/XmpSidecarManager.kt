package com.example.engine

import com.example.model.ColorLabel
import com.example.model.EditParams
import com.example.model.HistoryStep
import com.example.model.PhotoItem
import org.w3c.dom.Element
import java.io.File
import java.io.RandomAccessFile
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory
import org.xml.sax.InputSource

data class XmpSidecarData(
    val rating: Int,
    val isRejected: Boolean,
    val colorLabels: Set<ColorLabel>,
    val tags: List<String>,
    val params: EditParams,
    val history: List<HistoryStep>,
    val snapshotA: EditParams?,
    val snapshotB: EditParams?,
    val title: String? = null
)

/**
 * Manages authentic Darktable-compatible .xmp sidecar files for non-destructive
 * persistence of edit parameters, history stack, ratings, and color tags.
 */
object XmpSidecarManager {

    private fun colorLabelToIndex(label: ColorLabel): Int = when (label) {
        ColorLabel.RED -> 0
        ColorLabel.YELLOW -> 1
        ColorLabel.GREEN -> 2
        ColorLabel.BLUE -> 3
        ColorLabel.PURPLE -> 4
        ColorLabel.NONE -> -1
    }

    private fun indexToColorLabel(index: Int): ColorLabel = when (index) {
        0 -> ColorLabel.RED
        1 -> ColorLabel.YELLOW
        2 -> ColorLabel.GREEN
        3 -> ColorLabel.BLUE
        4 -> ColorLabel.PURPLE
        else -> ColorLabel.NONE
    }

    fun getSidecarFile(filesDir: File, photo: PhotoItem): File {
        if (photo.uriString.startsWith("file://")) {
            val filePath = photo.uriString.removePrefix("file://")
            val imageFile = File(filePath)
            val parent = imageFile.parentFile
            if (parent != null && parent.exists()) {
                return File(parent, "${imageFile.name}.xmp")
            }
        }
        val sidecarDir = File(filesDir, "xmp_sidecars").apply {
            if (!exists()) mkdirs()
        }
        return File(sidecarDir, "${photo.id}.xmp")
    }

    fun writeSidecar(filesDir: File, photo: PhotoItem): File {
        val file = getSidecarFile(filesDir, photo)
        file.parentFile?.let { if (!it.exists()) it.mkdirs() }
        val xmlContent = generateXmpXml(photo)
        file.writeText(xmlContent, Charsets.UTF_8)
        return file
    }

    fun readSidecar(filesDir: File, photo: PhotoItem): XmpSidecarData? {
        val file = getSidecarFile(filesDir, photo)
        if (!file.exists() || !file.isFile) return null
        return try {
            parseXmpXml(file.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    fun deleteSidecar(filesDir: File, photo: PhotoItem) {
        val file = getSidecarFile(filesDir, photo)
        if (file.exists()) {
            file.delete()
        }
    }

    fun generateXmpXml(photo: PhotoItem): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
        sb.append("<x:xmpmeta xmlns:x=\"adobe:ns:meta/\" x:xmptk=\"Darktable Mobile Android 1.0\">\n")
        sb.append(" <rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n")
        sb.append("  <rdf:Description rdf:about=\"\"\n")
        sb.append("    xmlns:xmp=\"http://ns.adobe.com/xap/1.0/\"\n")
        sb.append("    xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n")
        sb.append("    xmlns:darktable=\"http://darktable.sf.net/\"\n")
        sb.append("    xmp:Rating=\"${photo.rating}\"\n")
        sb.append("    darktable:xmp_version=\"5\"\n")
        sb.append("    darktable:raw_params=\"0\"\n")
        sb.append("    darktable:rejected=\"${if (photo.isRejected) 1 else 0}\"\n")
        sb.append("    darktable:history_end=\"${photo.history.size}\">\n")

        // Title
        sb.append("   <dc:title>\n")
        sb.append("    <rdf:Alt>\n")
        sb.append("     <rdf:li xml:lang=\"x-default\">${escapeXml(photo.title)}</rdf:li>\n")
        sb.append("    </rdf:Alt>\n")
        sb.append("   </dc:title>\n")

        // Tags / Subject
        if (photo.tags.isNotEmpty()) {
            sb.append("   <dc:subject>\n")
            sb.append("    <rdf:Bag>\n")
            photo.tags.forEach { tag ->
                sb.append("     <rdf:li>${escapeXml(tag)}</rdf:li>\n")
            }
            sb.append("    </rdf:Bag>\n")
            sb.append("   </dc:subject>\n")
        }

        // Color Labels (Darktable numeric indices + name attribute)
        sb.append("   <darktable:colorlabels>\n")
        sb.append("    <rdf:Seq>\n")
        photo.colorLabels.filter { it != ColorLabel.NONE }.forEach { label ->
            val idx = colorLabelToIndex(label)
            if (idx >= 0) {
                sb.append("     <rdf:li darktable:label_name=\"${label.name}\">$idx</rdf:li>\n")
            }
        }
        sb.append("    </rdf:Seq>\n")
        sb.append("   </darktable:colorlabels>\n")

        // Active Parameters
        sb.append(formatParamsElement("darktable:active_params", photo.params, "   "))

        // Snapshots
        photo.snapshotA?.let { snapA ->
            sb.append(formatParamsElement("darktable:snapshot_a", snapA, "   "))
        }
        photo.snapshotB?.let { snapB ->
            sb.append(formatParamsElement("darktable:snapshot_b", snapB, "   "))
        }

        // History Stack
        sb.append("   <darktable:history>\n")
        sb.append("    <rdf:Seq>\n")
        photo.history.forEachIndexed { idx, step ->
            sb.append("     <rdf:li\n")
            sb.append("       darktable:num=\"$idx\"\n")
            sb.append("       darktable:id=\"${escapeXml(step.id)}\"\n")
            sb.append("       darktable:operation=\"${escapeXml(step.moduleName)}\"\n")
            sb.append("       darktable:description=\"${escapeXml(step.description)}\"\n")
            sb.append("       darktable:timestamp=\"${step.timestamp}\"\n")
            sb.append("       darktable:enabled=\"1\">\n")
            sb.append(formatParamsElement("darktable:params", step.paramsSnapshot, "      "))
            sb.append("     </rdf:li>\n")
        }
        sb.append("    </rdf:Seq>\n")
        sb.append("   </darktable:history>\n")

        sb.append("  </rdf:Description>\n")
        sb.append(" </rdf:RDF>\n")
        sb.append("</x:xmpmeta>\n")
        return sb.toString()
    }

    private fun formatParamsElement(tagName: String, p: EditParams, indent: String): String {
        return buildString {
            append("$indent<$tagName\n")
            append("$indent  exposure=\"${p.exposure}\"\n")
            append("$indent  contrast=\"${p.contrast}\"\n")
            append("$indent  highlights=\"${p.highlights}\"\n")
            append("$indent  shadows=\"${p.shadows}\"\n")
            append("$indent  blackPoint=\"${p.blackPoint}\"\n")
            append("$indent  temperature=\"${p.temperature}\"\n")
            append("$indent  tint=\"${p.tint}\"\n")
            append("$indent  saturation=\"${p.saturation}\"\n")
            append("$indent  vibrance=\"${p.vibrance}\"\n")
            append("$indent  filmicEnabled=\"${p.filmicEnabled}\"\n")
            append("$indent  filmicToe=\"${p.filmicToe}\"\n")
            append("$indent  filmicShoulder=\"${p.filmicShoulder}\"\n")
            append("$indent  filmicContrast=\"${p.filmicContrast}\"\n")
            append("$indent  curveBlacks=\"${p.curveBlacks}\"\n")
            append("$indent  curveShadows=\"${p.curveShadows}\"\n")
            append("$indent  curveMidtones=\"${p.curveMidtones}\"\n")
            append("$indent  curveHighlights=\"${p.curveHighlights}\"\n")
            append("$indent  curveWhites=\"${p.curveWhites}\"\n")
            append("$indent  splitToningEnabled=\"${p.splitToningEnabled}\"\n")
            append("$indent  shadowHue=\"${p.shadowHue}\"\n")
            append("$indent  shadowSat=\"${p.shadowSat}\"\n")
            append("$indent  highlightHue=\"${p.highlightHue}\"\n")
            append("$indent  highlightSat=\"${p.highlightSat}\"\n")
            append("$indent  splitBalance=\"${p.splitBalance}\"\n")
            append("$indent  sharpness=\"${p.sharpness}\"\n")
            append("$indent  grain=\"${p.grain}\"\n")
            append("$indent  vignette=\"${p.vignette}\"\n")
            append("$indent  vignetteRadius=\"${p.vignetteRadius}\"\n")
            append("$indent  isMonochrome=\"${p.isMonochrome}\"\n")
            append("$indent  redWeight=\"${p.redWeight}\"\n")
            append("$indent  greenWeight=\"${p.greenWeight}\"\n")
            append("$indent  blueWeight=\"${p.blueWeight}\"\n")
            append("$indent  rotationAngle=\"${p.rotationAngle}\"\n")
            append("$indent  flipHorizontal=\"${p.flipHorizontal}\"\n")
            append("$indent  cropAspectRatio=\"${escapeXml(p.cropAspectRatio)}\"\n")
            append("$indent  cropLeft=\"${p.cropLeft}\"\n")
            append("$indent  cropTop=\"${p.cropTop}\"\n")
            append("$indent  cropRight=\"${p.cropRight}\"\n")
            append("$indent  cropBottom=\"${p.cropBottom}\" />\n")
        }
    }

    fun findSiblingSidecarFile(imageFile: File): File? {
        val parent = imageFile.parentFile ?: return null
        if (!parent.exists() || !parent.isDirectory) return null
        val candidates = listOf(
            "${imageFile.name}.xmp",
            "${imageFile.nameWithoutExtension}.xmp",
            "${imageFile.nameWithoutExtension}_01.${imageFile.extension}.xmp"
        ).distinct()
        for (name in candidates) {
            val file = File(parent, name)
            if (file.exists() && file.isFile && file.canRead()) {
                return file
            }
        }
        val lowerCandidates = candidates.map { it.lowercase() }
        val files = parent.listFiles() ?: return null
        for (targetLower in lowerCandidates) {
            val match = files.firstOrNull { it.isFile && it.canRead() && it.name.lowercase() == targetLower }
            if (match != null) return match
        }
        return null
    }

    fun findMatchingXmpName(imageDisplayName: String, availableXmpNames: Collection<String>): String? {
        if (availableXmpNames.isEmpty()) return null
        val cleanName = imageDisplayName.trim()
        val hasDot = cleanName.contains('.')
        val stem = if (hasDot) cleanName.substringBeforeLast(".") else cleanName
        val ext = if (hasDot) cleanName.substringAfterLast(".", "") else ""
        val priorityCandidates = buildList {
            add("$cleanName.xmp".lowercase())
            add("$stem.xmp".lowercase())
            if (ext.isNotEmpty()) {
                add("${stem}_01.$ext.xmp".lowercase())
            }
        }.distinct()
        val byLower = availableXmpNames.associateBy { it.trim().lowercase() }
        for (candidate in priorityCandidates) {
            byLower[candidate]?.let { return it }
        }
        // Fallback when provider stripped the image extension (e.g. "sunset" matching "sunset.jpg.xmp")
        if (!hasDot) {
            val stemLower = stem.lowercase()
            val prefixMatch = availableXmpNames.firstOrNull { xmpName ->
                val lower = xmpName.trim().lowercase()
                lower.startsWith("$stemLower.") && lower.endsWith(".xmp")
            }
            if (prefixMatch != null) return prefixMatch
        }
        return null
    }

    fun extractEmbeddedXmpXml(imageFile: File): String? {
        if (!imageFile.exists() || !imageFile.isFile) return null
        return try {
            val maxScan = minOf(imageFile.length(), 1024L * 1024L).toInt()
            if (maxScan <= 0) return null
            val bytes = ByteArray(maxScan)
            RandomAccessFile(imageFile, "r").use { raf ->
                raf.readFully(bytes)
            }
            val text = String(bytes, Charsets.UTF_8)
            val startIdx = text.indexOf("<x:xmpmeta")
            if (startIdx < 0) return null
            val endIdx = text.indexOf("</x:xmpmeta>", startIdx)
            if (endIdx < 0) return null
            val xml = text.substring(startIdx, endIdx + "</x:xmpmeta>".length)
            if (
                xml.contains("darktable:", ignoreCase = true) ||
                xml.contains("active_params", ignoreCase = true) ||
                xml.contains("crs:", ignoreCase = true)
            ) {
                xml
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun sanitizeXmpXml(rawXml: String): String {
        val stripped = rawXml.trimStart('\uFEFF', ' ', '\t', '\r', '\n').trimEnd('\u0000', ' ', '\t', '\r', '\n')
        val xmlStart = stripped.indexOf("<?xml")
        val xmpStart = stripped.indexOf("<x:xmpmeta")
        val rdfStart = stripped.indexOf("<rdf:RDF")
        val start = listOf(xmlStart, xmpStart, rdfStart).filter { it >= 0 }.minOrNull() ?: 0
        val sliced = if (start > 0) stripped.substring(start) else stripped
        // Strip <?xpacket ...?> processing instructions that can sit outside root tags
        return sliced.replace(Regex("<\\?xpacket[^>]*\\?>"), "").trim()
    }

    fun parseXmpXml(rawXml: String): XmpSidecarData? {
        return try {
            val xml = sanitizeXmpXml(rawXml)
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
            }
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            val descNodes = doc.getElementsByTagName("rdf:Description").let {
                if (it.length > 0) it else doc.getElementsByTagName("Description")
            }
            if (descNodes.length == 0) return null
            val descElements = (0 until descNodes.length).mapNotNull { descNodes.item(it) as? Element }
            if (descElements.isEmpty()) return null

            fun attrAcrossDescs(vararg names: String): String? {
                for (el in descElements) {
                    for (name in names) {
                        val v = el.getAttribute(name)
                        if (v.isNotEmpty()) return v
                    }
                }
                return null
            }

            fun childTextAcrossDescs(vararg names: String): String? {
                for (name in names) {
                    val nodes = doc.getElementsByTagName(name)
                    if (nodes.length > 0) {
                        val txt = nodes.item(0).textContent?.trim()
                        if (!txt.isNullOrEmpty()) return txt
                    }
                }
                return null
            }

            val rating = (attrAcrossDescs("xmp:Rating", "xap:Rating", "Rating")?.toIntOrNull())
                ?: (childTextAcrossDescs("xmp:Rating", "xap:Rating")?.toIntOrNull())
                ?: 0

            val rejectedRaw = attrAcrossDescs("darktable:rejected")
                ?: childTextAcrossDescs("darktable:rejected")
                ?: ""
            val rejected = rejectedRaw == "1" || rejectedRaw.equals("true", ignoreCase = true) || rating < 0

            // Title
            var parsedTitle: String? = null
            val titleNodes = doc.getElementsByTagName("dc:title")
            if (titleNodes.length > 0) {
                val titleEl = titleNodes.item(0) as? Element
                val liNodes = titleEl?.getElementsByTagName("rdf:li")
                if (liNodes != null && liNodes.length > 0) {
                    parsedTitle = liNodes.item(0).textContent?.trim()?.takeIf { it.isNotEmpty() }
                }
            }

            // Tags
            val tags = mutableListOf<String>()
            val subjectNodes = doc.getElementsByTagName("dc:subject")
            for (sIdx in 0 until subjectNodes.length) {
                val subjectEl = subjectNodes.item(sIdx) as? Element ?: continue
                val liNodes = subjectEl.getElementsByTagName("rdf:li")
                for (i in 0 until liNodes.length) {
                    val text = liNodes.item(i).textContent?.trim()
                    if (!text.isNullOrEmpty() && !tags.contains(text)) tags.add(text)
                }
            }

            // Color Labels (Darktable numeric indices + xmp:Label)
            val labels = mutableSetOf<ColorLabel>()
            val colorNodes = doc.getElementsByTagName("darktable:colorlabels")
            for (cIdx in 0 until colorNodes.length) {
                val colorEl = colorNodes.item(cIdx) as? Element ?: continue
                val liNodes = colorEl.getElementsByTagName("rdf:li")
                for (i in 0 until liNodes.length) {
                    val li = liNodes.item(i) as? Element ?: continue
                    val nameAttr = li.getAttribute("darktable:label_name")
                    val lbl = if (nameAttr.isNotEmpty()) {
                        try {
                            ColorLabel.valueOf(nameAttr.uppercase())
                        } catch (_: Exception) {
                            ColorLabel.NONE
                        }
                    } else {
                        val idx = li.textContent?.trim()?.toIntOrNull() ?: -1
                        indexToColorLabel(idx)
                    }
                    if (lbl != ColorLabel.NONE) labels.add(lbl)
                }
            }
            attrAcrossDescs("xmp:Label", "xap:Label", "Label")?.let { xmpLabel ->
                val mapped = when (xmpLabel.trim().lowercase()) {
                    "red" -> ColorLabel.RED
                    "yellow" -> ColorLabel.YELLOW
                    "green" -> ColorLabel.GREEN
                    "blue" -> ColorLabel.BLUE
                    "purple", "magenta" -> ColorLabel.PURPLE
                    else -> ColorLabel.NONE
                }
                if (mapped != ColorLabel.NONE) labels.add(mapped)
            }

            // Active Params (App native XML format)
            val activeParamsNodes = doc.getElementsByTagName("darktable:active_params")
            val hasExplicitActiveParams = activeParamsNodes.length > 0
            var activeParams = if (hasExplicitActiveParams) {
                parseParamsElement(activeParamsNodes.item(0) as Element)
            } else {
                EditParams()
            }

            // Snapshots
            val snapANodes = doc.getElementsByTagName("darktable:snapshot_a")
            val snapA = if (snapANodes.length > 0) parseParamsElement(snapANodes.item(0) as Element) else null

            val snapBNodes = doc.getElementsByTagName("darktable:snapshot_b")
            val snapB = if (snapBNodes.length > 0) parseParamsElement(snapBNodes.item(0) as Element) else null

            // History (supports both Darktable Mobile child <darktable:params .../> AND Desktop Darktable hex/gz attributes)
            val history = mutableListOf<HistoryStep>()
            val historyEnd = attrAcrossDescs("darktable:history_end")?.toIntOrNull()
            val historyNodes = doc.getElementsByTagName("darktable:history")
            var desktopRunningParams = EditParams()
            var decodedAnyDesktopModule = false

            if (historyNodes.length > 0) {
                val histEl = historyNodes.item(0) as Element
                val liNodes = histEl.getElementsByTagName("rdf:li")
                for (i in 0 until liNodes.length) {
                    val li = liNodes.item(i) as? Element ?: continue
                    val num = li.getAttribute("darktable:num").toIntOrNull() ?: i
                    val enabledAttr = li.getAttribute("darktable:enabled")
                    val isEnabled = enabledAttr.isEmpty() || enabledAttr == "1" || enabledAttr.equals("true", ignoreCase = true)
                    val withinHistoryEnd = historyEnd == null || historyEnd <= 0 || num < historyEnd

                    val stepId = li.getAttribute("darktable:id").ifEmpty { java.util.UUID.randomUUID().toString() }
                    val op = li.getAttribute("darktable:operation").ifEmpty { "Module" }
                    val stepDesc = li.getAttribute("darktable:description")
                    val ts = li.getAttribute("darktable:timestamp").toLongOrNull() ?: System.currentTimeMillis()
                    val paramsChildNodes = li.getElementsByTagName("darktable:params")

                    val stepParams = if (paramsChildNodes.length > 0) {
                        parseParamsElement(paramsChildNodes.item(0) as Element)
                    } else if (!hasExplicitActiveParams) {
                        val hexParams = li.getAttribute("darktable:params")
                        val modVersion = li.getAttribute("darktable:modversion").toIntOrNull() ?: 1
                        if (withinHistoryEnd) {
                            val applied = applyDesktopDarktableOperation(
                                current = desktopRunningParams,
                                operation = op,
                                enabled = isEnabled,
                                modVersion = modVersion,
                                rawParams = hexParams
                            )
                            if (applied != null) {
                                desktopRunningParams = applied
                                decodedAnyDesktopModule = true
                                applied
                            } else {
                                desktopRunningParams
                            }
                        } else {
                            desktopRunningParams
                        }
                    } else {
                        activeParams
                    }

                    history.add(
                        HistoryStep(
                            id = stepId,
                            moduleName = op,
                            description = if (stepDesc.isNotEmpty()) stepDesc else "darktable $op",
                            timestamp = ts,
                            paramsSnapshot = stepParams
                        )
                    )
                }
            }

            if (!hasExplicitActiveParams) {
                if (decodedAnyDesktopModule) {
                    activeParams = desktopRunningParams
                } else if (history.isNotEmpty()) {
                    activeParams = history.last().paramsSnapshot
                }
                // Apply Adobe Camera Raw / Lightroom crs:* attributes if present
                activeParams = parseCrsAttributes(descElements, activeParams)
            }

            XmpSidecarData(
                rating = rating.coerceIn(0, 5),
                isRejected = rejected,
                colorLabels = labels,
                tags = tags,
                params = activeParams,
                history = history,
                snapshotA = snapA,
                snapshotB = snapB,
                title = parsedTitle
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeDarktableBinaryParams(raw: String): java.nio.ByteBuffer? {
        if (raw.isBlank()) return null
        val trimmed = raw.trim()
        val bytes = try {
            if (trimmed.startsWith("gz") && trimmed.length > 4) {
                // Darktable compressed format: "gz" + 2 hex length bytes + base64 zlib payload
                val b64 = trimmed.substring(4)
                val compressed = android.util.Base64.decode(b64, android.util.Base64.DEFAULT)
                val inflater = java.util.zip.Inflater()
                inflater.setInput(compressed)
                val out = java.io.ByteArrayOutputStream()
                val buf = ByteArray(1024)
                while (!inflater.finished()) {
                    val count = inflater.inflate(buf)
                    if (count == 0) break
                    out.write(buf, 0, count)
                }
                inflater.end()
                out.toByteArray()
            } else {
                if (trimmed.length % 2 != 0) return null
                ByteArray(trimmed.length / 2) { i ->
                    trimmed.substring(i * 2, i * 2 + 2).toInt(16).toByte()
                }
            }
        } catch (_: Exception) {
            return null
        }
        return java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN)
    }

    private fun applyDesktopDarktableOperation(
        current: EditParams,
        operation: String,
        enabled: Boolean,
        modVersion: Int,
        rawParams: String
    ): EditParams? {
        val op = operation.trim().lowercase()
        val buf = decodeDarktableBinaryParams(rawParams)

        fun readFloatAt(byteOffset: Int): Float? {
            if (buf == null || byteOffset + 4 > buf.limit()) return null
            val v = buf.getFloat(byteOffset)
            return if (v.isFinite()) v else null
        }

        fun readIntAt(byteOffset: Int): Int? {
            if (buf == null || byteOffset + 4 > buf.limit()) return null
            return buf.getInt(byteOffset)
        }

        return when (op) {
            "exposure" -> {
                if (!enabled) return current.copy(exposure = 0f, blackPoint = 0f)
                // Darktable dt_iop_exposure_params_t: mode (int32 @0), black (float @4), exposure (float @8)
                val black = readFloatAt(4)
                val ev = readFloatAt(8) ?: if (modVersion <= 1) readFloatAt(0) else null
                if (ev == null && black == null) return null
                current.copy(
                    exposure = (ev ?: current.exposure).coerceIn(-3f, 3f),
                    blackPoint = if (black != null) (black * 400f).coerceIn(-50f, 50f) else current.blackPoint
                )
            }
            "monochrome", "blackandwhite" -> {
                current.copy(isMonochrome = enabled)
            }
            "channelmixerrgb" -> {
                if (!enabled) return current
                // Offset 56..67 holds grey[0..2] weights in dt_iop_channelmixerrgb_params_t
                val g0 = readFloatAt(56) ?: 0f
                val g1 = readFloatAt(60) ?: 0f
                val g2 = readFloatAt(64) ?: 0f
                val greySum = kotlin.math.abs(g0) + kotlin.math.abs(g1) + kotlin.math.abs(g2)
                if (greySum > 0.05f && greySum < 10f) {
                    val normSum = greySum.coerceAtLeast(0.001f)
                    current.copy(
                        isMonochrome = true,
                        redWeight = ((g0.coerceAtLeast(0f) / normSum) * 100f).coerceIn(0f, 100f),
                        greenWeight = ((g1.coerceAtLeast(0f) / normSum) * 100f).coerceIn(0f, 100f),
                        blueWeight = ((g2.coerceAtLeast(0f) / normSum) * 100f).coerceIn(0f, 100f)
                    )
                } else {
                    current
                }
            }
            "crop" -> {
                if (!enabled) {
                    return current.copy(cropAspectRatio = "Original", cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f)
                }
                val cx = readFloatAt(0) ?: return null
                val cy = readFloatAt(4) ?: return null
                val cw = readFloatAt(8) ?: return null
                val ch = readFloatAt(12) ?: return null
                if (cx in 0f..0.95f && cy in 0f..0.95f && cw in (cx + 0.05f)..1f && ch in (cy + 0.05f)..1f) {
                    val isFull = cx <= 0.001f && cy <= 0.001f && cw >= 0.999f && ch >= 0.999f
                    current.copy(
                        cropAspectRatio = if (isFull) "Original" else "Free",
                        cropLeft = cx,
                        cropTop = cy,
                        cropRight = cw,
                        cropBottom = ch
                    )
                } else null
            }
            "clipping" -> {
                if (!enabled) {
                    return current.copy(rotationAngle = 0f, cropAspectRatio = "Original", cropLeft = 0f, cropTop = 0f, cropRight = 1f, cropBottom = 1f)
                }
                val angle = (readFloatAt(0) ?: 0f).coerceIn(-45f, 45f)
                val cx = readFloatAt(4) ?: 0f
                val cy = readFloatAt(8) ?: 0f
                val cw = readFloatAt(12) ?: 1f
                val ch = readFloatAt(16) ?: 1f
                val validCrop = cx in 0f..0.95f && cy in 0f..0.95f && cw in (cx + 0.05f)..1f && ch in (cy + 0.05f)..1f
                val isFull = !validCrop || (cx <= 0.001f && cy <= 0.001f && cw >= 0.999f && ch >= 0.999f)
                current.copy(
                    rotationAngle = angle,
                    cropAspectRatio = if (isFull) "Original" else "Free",
                    cropLeft = if (validCrop) cx else 0f,
                    cropTop = if (validCrop) cy else 0f,
                    cropRight = if (validCrop) cw else 1f,
                    cropBottom = if (validCrop) ch else 1f
                )
            }
            "flip" -> {
                if (!enabled) return current.copy(flipHorizontal = false)
                val orientation = readIntAt(0) ?: return null
                current.copy(flipHorizontal = (orientation and 1) != 0)
            }
            "shadhi" -> {
                if (!enabled) return current.copy(shadows = 0f, highlights = 0f)
                val sh = readFloatAt(8)
                val hi = readFloatAt(16)
                if (sh == null && hi == null) return null
                current.copy(
                    shadows = (sh ?: current.shadows).coerceIn(-100f, 100f),
                    highlights = (hi ?: current.highlights).coerceIn(-100f, 100f)
                )
            }
            "vignette" -> {
                if (!enabled) return current.copy(vignette = 0f)
                val scale = readFloatAt(0)
                val brightness = readFloatAt(8)
                if (brightness == null && scale == null) return null
                current.copy(
                    vignette = if (brightness != null) (brightness * 100f).coerceIn(-100f, 100f) else current.vignette,
                    vignetteRadius = (scale ?: current.vignetteRadius).coerceIn(10f, 100f)
                )
            }
            "sharpen" -> {
                if (!enabled) return current.copy(sharpness = 0f)
                val amount = readFloatAt(4) ?: return null
                current.copy(sharpness = (10f + amount * 45f).coerceIn(0f, 100f))
            }
            "grain" -> {
                if (!enabled) return current.copy(grain = 0f)
                val strength = readFloatAt(8) ?: return null
                current.copy(grain = strength.coerceIn(0f, 100f))
            }
            "vibrance" -> {
                if (!enabled) return current.copy(vibrance = 0f)
                val amount = readFloatAt(0) ?: return null
                current.copy(vibrance = amount.coerceIn(-100f, 100f))
            }
            "velvia" -> {
                if (!enabled) return current.copy(vibrance = 0f)
                val strength = readFloatAt(0) ?: return null
                current.copy(vibrance = (strength * 100f).coerceIn(-100f, 100f))
            }
            "colisa" -> {
                if (!enabled) return current.copy(contrast = 0f, saturation = 0f)
                val cont = readFloatAt(0) ?: 0f
                val bright = readFloatAt(4) ?: 0f
                val sat = readFloatAt(8) ?: 0f
                current.copy(
                    contrast = (cont * 100f).coerceIn(-100f, 100f),
                    exposure = (current.exposure + bright * 1.5f).coerceIn(-3f, 3f),
                    saturation = (sat * 100f).coerceIn(-100f, 100f)
                )
            }
            "colorbalance" -> {
                if (!enabled) return current.copy(contrast = 0f, saturation = 0f)
                val sat = readFloatAt(52)
                val cont = readFloatAt(56)
                if (sat == null && cont == null) return null
                current.copy(
                    saturation = if (sat != null) ((sat - 1f) * 100f).coerceIn(-100f, 100f) else current.saturation,
                    contrast = if (cont != null) ((cont - 1f) * 100f).coerceIn(-100f, 100f) else current.contrast
                )
            }
            "colorbalancergb" -> {
                if (!enabled) return current.copy(contrast = 0f, saturation = 0f, vibrance = 0f)
                val satGlobal = readFloatAt(44)
                val chromaGlobal = readFloatAt(36)
                val cont = readFloatAt(64)
                val vib = readFloatAt(68)
                var updated = current
                var touched = false
                if (satGlobal != null && satGlobal in -1f..1f) {
                    updated = updated.copy(saturation = (satGlobal * 100f).coerceIn(-100f, 100f))
                    touched = true
                } else if (chromaGlobal != null && chromaGlobal in -1f..1f) {
                    updated = updated.copy(saturation = (chromaGlobal * 100f).coerceIn(-100f, 100f))
                    touched = true
                }
                if (cont != null && cont in -1f..1f) {
                    updated = updated.copy(contrast = (cont * 100f).coerceIn(-100f, 100f))
                    touched = true
                }
                if (vib != null && vib in -1f..1f) {
                    updated = updated.copy(vibrance = (vib * 100f).coerceIn(-100f, 100f))
                    touched = true
                }
                if (touched) updated else null
            }
            "filmicrgb", "filmic" -> {
                current.copy(filmicEnabled = enabled)
            }
            else -> null
        }
    }

    private fun parseCrsAttributes(descElements: List<Element>, base: EditParams): EditParams {
        fun crsAttr(vararg names: String): String? {
            for (el in descElements) {
                for (name in names) {
                    val v = el.getAttribute(name)
                    if (v.isNotEmpty()) return v
                }
            }
            return null
        }

        fun crsFloat(vararg names: String): Float? = crsAttr(*names)?.toFloatOrNull()

        var out = base
        crsFloat("crs:Exposure2012", "crs:Exposure")?.let {
            out = out.copy(exposure = it.coerceIn(-3f, 3f))
        }
        crsFloat("crs:Contrast2012", "crs:Contrast")?.let {
            out = out.copy(contrast = it.coerceIn(-100f, 100f))
        }
        crsFloat("crs:Highlights2012", "crs:Highlights")?.let {
            out = out.copy(highlights = it.coerceIn(-100f, 100f))
        }
        crsFloat("crs:Shadows2012", "crs:Shadows")?.let {
            out = out.copy(shadows = it.coerceIn(-100f, 100f))
        }
        crsFloat("crs:Blacks2012", "crs:Blacks")?.let {
            out = out.copy(blackPoint = (-it * 0.5f).coerceIn(-100f, 100f))
        }
        crsFloat("crs:Temperature")?.let {
            if (it in 1500f..20000f) {
                out = out.copy(temperature = it.coerceIn(2000f, 10000f))
            }
        }
        crsFloat("crs:Tint")?.let {
            out = out.copy(tint = it.coerceIn(-100f, 100f))
        }
        crsFloat("crs:Saturation")?.let {
            out = out.copy(saturation = it.coerceIn(-100f, 100f))
        }
        crsFloat("crs:Vibrance")?.let {
            out = out.copy(vibrance = it.coerceIn(-100f, 100f))
        }
        crsAttr("crs:ConvertToGrayscale")?.let {
            if (it.equals("true", ignoreCase = true)) {
                out = out.copy(isMonochrome = true)
            }
        }
        crsFloat("crs:Sharpness")?.let {
            out = out.copy(sharpness = it.coerceIn(0f, 100f))
        }
        crsFloat("crs:GrainAmount")?.let {
            out = out.copy(grain = it.coerceIn(0f, 100f))
        }
        crsFloat("crs:PostCropVignetteAmount", "crs:VignetteAmount")?.let {
            out = out.copy(vignette = it.coerceIn(-100f, 100f))
        }

        val hasCrop = crsAttr("crs:HasCrop")?.equals("true", ignoreCase = true) == true
        val cLeft = crsFloat("crs:CropLeft")
        val cTop = crsFloat("crs:CropTop")
        val cRight = crsFloat("crs:CropRight")
        val cBottom = crsFloat("crs:CropBottom")
        val cAngle = crsFloat("crs:CropAngle")
        if (hasCrop || cLeft != null || cTop != null || cRight != null || cBottom != null) {
            val l = (cLeft ?: 0f).coerceIn(0f, 0.95f)
            val t = (cTop ?: 0f).coerceIn(0f, 0.95f)
            val r = (cRight ?: 1f).coerceIn(l + 0.05f, 1f)
            val b = (cBottom ?: 1f).coerceIn(t + 0.05f, 1f)
            val isFull = l <= 0.001f && t <= 0.001f && r >= 0.999f && b >= 0.999f
            out = out.copy(
                cropAspectRatio = if (isFull) "Original" else "Free",
                cropLeft = l,
                cropTop = t,
                cropRight = r,
                cropBottom = b
            )
        }
        if (cAngle != null) {
            out = out.copy(rotationAngle = cAngle.coerceIn(-45f, 45f))
        }
        return out
    }

    private fun parseParamsElement(el: Element): EditParams {
        fun attrFloat(name: String, default: Float): Float =
            el.getAttribute(name).toFloatOrNull() ?: default

        fun attrBool(name: String, default: Boolean): Boolean {
            val v = el.getAttribute(name)
            return if (v.isEmpty()) default else v.toBoolean()
        }

        return EditParams(
            exposure = attrFloat("exposure", 0f),
            contrast = attrFloat("contrast", 0f),
            highlights = attrFloat("highlights", 0f),
            shadows = attrFloat("shadows", 0f),
            blackPoint = attrFloat("blackPoint", 0f),
            temperature = attrFloat("temperature", 5500f),
            tint = attrFloat("tint", 0f),
            saturation = attrFloat("saturation", 0f),
            vibrance = attrFloat("vibrance", 0f),
            filmicEnabled = attrBool("filmicEnabled", true),
            filmicToe = attrFloat("filmicToe", 35f),
            filmicShoulder = attrFloat("filmicShoulder", 40f),
            filmicContrast = attrFloat("filmicContrast", 1.15f),
            curveBlacks = attrFloat("curveBlacks", 0f),
            curveShadows = attrFloat("curveShadows", 0f),
            curveMidtones = attrFloat("curveMidtones", 0f),
            curveHighlights = attrFloat("curveHighlights", 0f),
            curveWhites = attrFloat("curveWhites", 0f),
            splitToningEnabled = attrBool("splitToningEnabled", false),
            shadowHue = attrFloat("shadowHue", 215f),
            shadowSat = attrFloat("shadowSat", 0f),
            highlightHue = attrFloat("highlightHue", 40f),
            highlightSat = attrFloat("highlightSat", 0f),
            splitBalance = attrFloat("splitBalance", 0f),
            sharpness = attrFloat("sharpness", 10f),
            grain = attrFloat("grain", 0f),
            vignette = attrFloat("vignette", 0f),
            vignetteRadius = attrFloat("vignetteRadius", 60f),
            isMonochrome = attrBool("isMonochrome", false),
            redWeight = attrFloat("redWeight", 30f),
            greenWeight = attrFloat("greenWeight", 59f),
            blueWeight = attrFloat("blueWeight", 11f),
            rotationAngle = attrFloat("rotationAngle", 0f),
            flipHorizontal = attrBool("flipHorizontal", false),
            cropAspectRatio = el.getAttribute("cropAspectRatio").ifEmpty { "Original" },
            cropLeft = attrFloat("cropLeft", 0f).coerceIn(0f, 0.9f),
            cropTop = attrFloat("cropTop", 0f).coerceIn(0f, 0.9f),
            cropRight = attrFloat("cropRight", 1f).coerceIn(0.1f, 1f),
            cropBottom = attrFloat("cropBottom", 1f).coerceIn(0.1f, 1f)
        )
    }

    private fun escapeXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
