package com.example.engine

import com.example.model.ColorLabel
import com.example.model.EditParams
import com.example.model.HistoryStep
import com.example.model.PhotoItem
import org.w3c.dom.Element
import java.io.File
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
    val snapshotB: EditParams?
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

    fun parseXmpXml(xml: String): XmpSidecarData? {
        return try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(InputSource(StringReader(xml)))
            val descList = doc.getElementsByTagName("rdf:Description")
            if (descList.length == 0) return null
            val desc = descList.item(0) as Element

            val rating = desc.getAttribute("xmp:Rating").toIntOrNull() ?: 0
            val rejected = desc.getAttribute("darktable:rejected") == "1"

            // Tags
            val tags = mutableListOf<String>()
            val subjectNodes = desc.getElementsByTagName("dc:subject")
            if (subjectNodes.length > 0) {
                val subjectEl = subjectNodes.item(0) as Element
                val liNodes = subjectEl.getElementsByTagName("rdf:li")
                for (i in 0 until liNodes.length) {
                    val text = liNodes.item(i).textContent?.trim()
                    if (!text.isNullOrEmpty()) tags.add(text)
                }
            }

            // Color Labels
            val labels = mutableSetOf<ColorLabel>()
            val colorNodes = desc.getElementsByTagName("darktable:colorlabels")
            if (colorNodes.length > 0) {
                val colorEl = colorNodes.item(0) as Element
                val liNodes = colorEl.getElementsByTagName("rdf:li")
                for (i in 0 until liNodes.length) {
                    val li = liNodes.item(i) as Element
                    val nameAttr = li.getAttribute("darktable:label_name")
                    val lbl = if (nameAttr.isNotEmpty()) {
                        try {
                            ColorLabel.valueOf(nameAttr)
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

            // Active Params
            val activeParamsNodes = desc.getElementsByTagName("darktable:active_params")
            val activeParams = if (activeParamsNodes.length > 0) {
                parseParamsElement(activeParamsNodes.item(0) as Element)
            } else {
                EditParams()
            }

            // Snapshots
            val snapANodes = desc.getElementsByTagName("darktable:snapshot_a")
            val snapA = if (snapANodes.length > 0) parseParamsElement(snapANodes.item(0) as Element) else null

            val snapBNodes = desc.getElementsByTagName("darktable:snapshot_b")
            val snapB = if (snapBNodes.length > 0) parseParamsElement(snapBNodes.item(0) as Element) else null

            // History
            val history = mutableListOf<HistoryStep>()
            val historyNodes = desc.getElementsByTagName("darktable:history")
            if (historyNodes.length > 0) {
                val histEl = historyNodes.item(0) as Element
                val liNodes = histEl.getElementsByTagName("rdf:li")
                for (i in 0 until liNodes.length) {
                    val li = liNodes.item(i) as Element
                    val stepId = li.getAttribute("darktable:id").ifEmpty { java.util.UUID.randomUUID().toString() }
                    val op = li.getAttribute("darktable:operation").ifEmpty { "Module" }
                    val stepDesc = li.getAttribute("darktable:description")
                    val ts = li.getAttribute("darktable:timestamp").toLongOrNull() ?: System.currentTimeMillis()
                    val paramsNodes = li.getElementsByTagName("darktable:params")
                    val stepParams = if (paramsNodes.length > 0) {
                        parseParamsElement(paramsNodes.item(0) as Element)
                    } else {
                        activeParams
                    }
                    history.add(
                        HistoryStep(
                            id = stepId,
                            moduleName = op,
                            description = stepDesc,
                            timestamp = ts,
                            paramsSnapshot = stepParams
                        )
                    )
                }
            }

            XmpSidecarData(
                rating = rating,
                isRejected = rejected,
                colorLabels = labels,
                tags = tags,
                params = activeParams,
                history = history,
                snapshotA = snapA,
                snapshotB = snapB
            )
        } catch (e: Exception) {
            null
        }
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
