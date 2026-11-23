package com.somatime.app.data

import android.util.Base64
import com.somatime.app.model.QuestionType
import com.somatime.app.model.QuizQuestion
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class ParsedUploadResult(
    val questions: List<QuizQuestion>,
    val warnings: List<String>
)

data class UploadedAttachmentMeta(
    val fileName: String,
    val url: String,
    val mimeType: String,
    val sizeBytes: Long
)

object AssignmentUploadParser {

    const val DEFAULT_SERVER_URL =
        "https://ais-pre-7ftuxpgldqkxiyd3355qsi-415526218247.europe-west2.run.app"

    const val DEV_SERVER_URL =
        "https://ais-dev-7ftuxpgldqkxiyd3355qsi-415526218247.europe-west2.run.app"

    @Volatile
    var UPLOAD_BASE_URL: String = DEFAULT_SERVER_URL
        private set

    fun getServerBaseUrl(): String = UPLOAD_BASE_URL

    fun normalizeServerBaseUrl(raw: String?): String? {
        val trimmed = raw?.trim()?.trimEnd('/') ?: return null
        if (trimmed.isEmpty()) return null
        return try {
            val u = URL(trimmed)
            val proto = u.protocol.lowercase(Locale.US)
            val host = u.host.lowercase(Locale.US)
            val isLocal = host == "10.0.2.2" || host == "localhost" || host == "127.0.0.1"
            if (proto == "https" || (proto == "http" && isLocal)) {
                "${proto}://${u.authority}".trimEnd('/')
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun setServerBaseUrl(raw: String?): Boolean {
        val normalized = normalizeServerBaseUrl(raw) ?: return false
        UPLOAD_BASE_URL = normalized
        return true
    }

    val SAMPLE_CSV_TEMPLATE: String = listOf(
        "type,prompt,options,answer,hint,explanation",
        "multiple_choice,\"Amina buys 3 exercise books at KES 40 each. How much does she pay?\",\"KES 80|KES 100|KES 120|KES 160\",\"KES 120\",\"Multiply 3 by 40.\",\"3 × KES 40 = KES 120.\"",
        "multiple_choice,\"Which gas do green plants take in during photosynthesis?\",\"Oxygen|Carbon dioxide|Nitrogen|Argon\",\"Carbon dioxide\",\"Plants absorb it from the air through stomata.\",\"Plants use carbon dioxide, water, and sunlight to make food.\"",
        "fill_blank,\"What is 3/4 of KES 200?\",\"\",\"150\",\"Divide 200 by 4 then multiply by 3.\",\"(200 ÷ 4) × 3 = KES 150.\""
    ).joinToString("\n")

    fun isSafeAttachmentUrl(rawUrl: String?): Boolean {
        val trimmed = rawUrl?.trim().orEmpty()
        if (trimmed.isEmpty()) return false
        if (trimmed.startsWith("/api/assignments/files/")) return true
        return try {
            val parsed = URL(trimmed)
            parsed.protocol.equals("https", ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun resolveOpenUrl(rawUrl: String?): String? {
        val trimmed = rawUrl?.trim().orEmpty()
        if (!isSafeAttachmentUrl(trimmed)) return null
        return if (trimmed.startsWith("/api/assignments/files/")) {
            "${getServerBaseUrl()}$trimmed"
        } else {
            trimmed
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 KB"
        if (bytes < 1024L) return "$bytes B"
        if (bytes < 1024L * 1024L) return "${(bytes + 512L) / 1024L} KB"
        val mb = bytes / (1024.0 * 1024.0)
        return String.format(Locale.US, "%.1f MB", mb)
    }

    fun parseUploadedQuestionFile(fileName: String, content: String): ParsedUploadResult {
        val lower = fileName.lowercase(Locale.US)
        val warnings = mutableListOf<String>()
        val trimmed = content.trim()
        if (trimmed.isEmpty()) {
            return ParsedUploadResult(emptyList(), listOf("Uploaded file is empty."))
        }

        val stamp = System.currentTimeMillis()
        fun makeId(idx: Int) = "tq-$stamp-${idx + 1}"

        // 1. JSON format
        if (lower.endsWith(".json") || trimmed.startsWith("[") || trimmed.startsWith("{")) {
            return try {
                val arr: JSONArray = if (trimmed.startsWith("[")) {
                    JSONArray(trimmed)
                } else {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("questions") ?: JSONArray()
                }
                val questions = mutableListOf<QuizQuestion>()
                val limit = minOf(arr.length(), 15)
                for (i in 0 until limit) {
                    val rec = arr.optJSONObject(i) ?: continue
                    val prompt = (rec.optString("prompt").ifBlank { rec.optString("question") })
                        .trim().take(320)
                    val answer = (rec.optString("correctAnswer").ifBlank { rec.optString("answer") })
                        .trim().take(140)
                    if (prompt.isEmpty() || answer.isEmpty()) continue

                    val rawOptsArr = rec.optJSONArray("options")
                    val rawOpts = buildList {
                        if (rawOptsArr != null) {
                            for (j in 0 until minOf(rawOptsArr.length(), 4)) {
                                val opt = rawOptsArr.optString(j).trim()
                                if (opt.isNotEmpty()) add(opt)
                            }
                        }
                    }
                    val typeField = rec.optString("type").lowercase(Locale.US)
                    val isMcq = rawOpts.size >= 2 || typeField == "multiple_choice"
                    val hint = (rec.optString("bilingualHint").ifBlank { rec.optString("hint") })
                        .ifBlank { "Read carefully before choosing." }
                        .take(220)
                    val explanation = rec.optString("explanation")
                        .ifBlank { "Correct answer: $answer" }
                        .take(260)

                    if (isMcq && rawOpts.size >= 2) {
                        val finalOpts = if (answer in rawOpts) rawOpts else (listOf(answer) + rawOpts.take(3))
                        questions.add(
                            QuizQuestion(
                                id = makeId(i),
                                type = QuestionType.MULTIPLE_CHOICE,
                                prompt = prompt,
                                hint = hint,
                                options = finalOpts,
                                correctAnswer = answer,
                                explanation = explanation
                            )
                        )
                    } else {
                        questions.add(
                            QuizQuestion(
                                id = makeId(i),
                                type = QuestionType.FILL_BLANK,
                                prompt = prompt,
                                hint = hint,
                                blankPrefix = rec.optString("blankPrefix").ifBlank { "Answer:" }.take(40),
                                blankSuffix = rec.optString("blankSuffix").take(40),
                                correctAnswer = answer,
                                acceptableAnswers = listOf(answer.lowercase(Locale.US)),
                                explanation = explanation
                            )
                        )
                    }
                }
                if (questions.isEmpty()) {
                    warnings.add("No valid questions found in JSON. Expected { prompt, options?, answer } items.")
                }
                ParsedUploadResult(questions, warnings)
            } catch (_: Exception) {
                ParsedUploadResult(emptyList(), listOf("Could not parse JSON file. Check JSON syntax."))
            }
        }

        // 2. CSV format
        if (lower.endsWith(".csv")) {
            val rows = parseCsvRows(trimmed)
            if (rows.isEmpty()) {
                return ParsedUploadResult(emptyList(), listOf("CSV file has no rows."))
            }
            val firstLower = rows.first().map { it.lowercase(Locale.US) }
            val hasHeader = firstLower.any { "prompt" in it || "question" in it } &&
                firstLower.any { "answer" in it }
            val dataRows = if (hasHeader) rows.drop(1) else rows
            val questions = mutableListOf<QuizQuestion>()

            dataRows.take(15).forEachIndexed { idx, cols ->
                var typeHint = ""
                var prompt = ""
                var optionsRaw = ""
                var answer = ""
                var hint = ""
                var explanation = ""

                if (cols.size >= 4 && cols[0].lowercase(Locale.US) in setOf("multiple_choice", "mcq", "fill_blank", "blank")) {
                    typeHint = cols[0]
                    prompt = cols.getOrElse(1) { "" }
                    optionsRaw = cols.getOrElse(2) { "" }
                    answer = cols.getOrElse(3) { "" }
                    hint = cols.getOrElse(4) { "" }
                    explanation = cols.getOrElse(5) { "" }
                } else if (cols.size >= 6 && "|" !in cols[1] && ";" !in cols[1]) {
                    prompt = cols[0]
                    optionsRaw = listOfNotNull(
                        cols.getOrNull(1)?.takeIf { it.isNotBlank() },
                        cols.getOrNull(2)?.takeIf { it.isNotBlank() },
                        cols.getOrNull(3)?.takeIf { it.isNotBlank() },
                        cols.getOrNull(4)?.takeIf { it.isNotBlank() }
                    ).joinToString("|")
                    answer = cols.getOrElse(5) { "" }
                    hint = cols.getOrElse(6) { "" }
                    explanation = cols.getOrElse(7) { "" }
                } else {
                    prompt = cols.getOrElse(0) { "" }
                    optionsRaw = cols.getOrElse(1) { "" }
                    answer = cols.getOrElse(2) { "" }
                    hint = cols.getOrElse(3) { "" }
                    explanation = cols.getOrElse(4) { "" }
                }

                prompt = prompt.trim().take(320)
                answer = answer.trim().take(140)
                if (prompt.isEmpty() || answer.isEmpty()) return@forEachIndexed

                val options = optionsRaw.split('|', ';')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(4)
                val forceBlank = "blank" in typeHint.lowercase(Locale.US)

                if (!forceBlank && options.size >= 2) {
                    val finalOpts = if (answer in options) options else (listOf(answer) + options.take(3))
                    questions.add(
                        QuizQuestion(
                            id = makeId(idx),
                            type = QuestionType.MULTIPLE_CHOICE,
                            prompt = prompt,
                            hint = hint.ifBlank { "Think through each option carefully." }.take(220),
                            options = finalOpts,
                            correctAnswer = answer,
                            explanation = explanation.ifBlank { "Correct answer: $answer" }.take(260)
                        )
                    )
                } else {
                    questions.add(
                        QuizQuestion(
                            id = makeId(idx),
                            type = QuestionType.FILL_BLANK,
                            prompt = prompt,
                            hint = hint.ifBlank { "Check spelling or units before checking." }.take(220),
                            blankPrefix = "Answer:",
                            blankSuffix = "",
                            correctAnswer = answer,
                            acceptableAnswers = listOf(answer.lowercase(Locale.US)),
                            explanation = explanation.ifBlank { "Expected answer: $answer" }.take(260)
                        )
                    )
                }
            }
            if (questions.isEmpty()) {
                warnings.add("No valid questions imported from CSV. Check columns: type,prompt,options,answer,hint,explanation.")
            }
            return ParsedUploadResult(questions, warnings)
        }

        // 3. Plain text / Q&A format
        val blocks = trimmed.split(Regex("\\r?\\n\\s*\\r?\\n+"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val questions = mutableListOf<QuizQuestion>()
        val optionRegex = Regex("^([A-Da-d])[).:\\]]\\s*(.+)$")
        val ansRegex = Regex("^answer\\s*[:=-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val hintRegex = Regex("^hint\\s*[:=-]\\s*(.+)$", RegexOption.IGNORE_CASE)
        val expRegex = Regex("^explanation\\s*[:=-]\\s*(.+)$", RegexOption.IGNORE_CASE)

        blocks.take(15).forEachIndexed { idx, block ->
            val lines = block.split(Regex("\\r?\\n")).map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) return@forEachIndexed

            var prompt = lines.first().replace(Regex("^(Q\\d*[:.)]\\s*|\\d+[.)]\\s*)", RegexOption.IGNORE_CASE), "").trim().take(320)
            val options = mutableListOf<String>()
            var answer = ""
            var hint = ""
            var explanation = ""

            for (i in 1 until lines.size) {
                val line = lines[i]
                val optMatch = optionRegex.matchEntire(line)
                if (optMatch != null) {
                    var optText = optMatch.groupValues[2].trim()
                    if (optText.endsWith("*")) {
                        optText = optText.dropLast(1).trim()
                        answer = optText
                    }
                    options.add(optText)
                    continue
                }
                val ansMatch = ansRegex.matchEntire(line)
                if (ansMatch != null) {
                    val rawAns = ansMatch.groupValues[1].trim()
                    val letterIdx = listOf("A", "B", "C", "D").indexOf(rawAns.uppercase(Locale.US))
                    answer = if (letterIdx >= 0 && letterIdx < options.size) options[letterIdx] else rawAns
                    continue
                }
                val hintMatch = hintRegex.matchEntire(line)
                if (hintMatch != null) {
                    hint = hintMatch.groupValues[1].trim()
                    continue
                }
                val expMatch = expRegex.matchEntire(line)
                if (expMatch != null) {
                    explanation = expMatch.groupValues[1].trim()
                    continue
                }
                if (options.isEmpty() && answer.isEmpty()) {
                    prompt = "$prompt $line".take(320)
                }
            }

            if (prompt.isEmpty() || answer.isEmpty()) return@forEachIndexed
            if (options.size >= 2) {
                val finalOpts = (if (answer in options) options else (listOf(answer) + options.take(3))).take(4)
                questions.add(
                    QuizQuestion(
                        id = makeId(idx),
                        type = QuestionType.MULTIPLE_CHOICE,
                        prompt = prompt,
                        hint = hint.ifBlank { "Read carefully before selecting your answer." }.take(220),
                        options = finalOpts,
                        correctAnswer = answer,
                        explanation = explanation.ifBlank { "Correct answer: $answer" }.take(260)
                    )
                )
            } else {
                questions.add(
                    QuizQuestion(
                        id = makeId(idx),
                        type = QuestionType.FILL_BLANK,
                        prompt = prompt,
                        hint = hint.ifBlank { "Type your answer in the blank field." }.take(220),
                        blankPrefix = "Answer:",
                        blankSuffix = "",
                        correctAnswer = answer,
                        acceptableAnswers = listOf(answer.lowercase(Locale.US)),
                        explanation = explanation.ifBlank { "Expected answer: $answer" }.take(260)
                    )
                )
            }
        }

        if (questions.isEmpty()) {
            warnings.add("Could not detect Q:/Answer: blocks in the text file. You can still attach it as a worksheet and add questions below.")
        }
        return ParsedUploadResult(questions, warnings)
    }

    private fun parseCsvRows(text: String): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < text.length) {
            val ch = text[i]
            val next = if (i + 1 < text.length) text[i + 1] else '\u0000'
            if (inQuotes) {
                if (ch == '"' && next == '"') {
                    cell.append('"')
                    i += 2
                    continue
                } else if (ch == '"') {
                    inQuotes = false
                } else {
                    cell.append(ch)
                }
                i += 1
                continue
            }
            when {
                ch == '"' -> inQuotes = true
                ch == ',' -> {
                    row.add(cell.toString().trim())
                    cell.clear()
                }
                ch == '\n' || ch == '\r' -> {
                    if (ch == '\r' && next == '\n') i += 1
                    row.add(cell.toString().trim())
                    if (row.any { it.isNotEmpty() }) rows.add(row)
                    row = mutableListOf()
                    cell.clear()
                }
                else -> cell.append(ch)
            }
            i += 1
        }
        row.add(cell.toString().trim())
        if (row.any { it.isNotEmpty() }) rows.add(row)
        return rows
    }

    suspend fun uploadAttachmentToServer(
        fileName: String,
        mimeType: String,
        bytes: ByteArray
    ): CloudOpResult<UploadedAttachmentMeta> = withContext(Dispatchers.IO) {
        if (bytes.isEmpty() || bytes.size > 5 * 1024 * 1024) {
            return@withContext CloudOpResult(
                ok = false,
                errorMessage = "File must be between 1 byte and 5 MB."
            )
        }
        try {
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            val payload = JSONObject()
                .put("fileName", fileName)
                .put("mimeType", mimeType)
                .put("dataBase64", base64)
                .toString()

            val conn = (URL("${getServerBaseUrl()}/api/assignments/upload").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = 15_000
                readTimeout = 20_000
                doOutput = true
                setRequestProperty("Accept", "application/json")
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(payload) }
            }
            val code = conn.responseCode
            val contentType = conn.contentType.orEmpty()
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
            } ?: ""
            conn.disconnect()

            val trimmedBody = body.trimStart()
            val isHtmlOrRedirect = code in 300..399 ||
                contentType.contains("html", ignoreCase = true) ||
                trimmedBody.startsWith("<")
            if (isHtmlOrRedirect) {
                return@withContext CloudOpResult(
                    ok = false,
                    httpCode = code,
                    errorMessage = "Server returned an HTML sign-in page instead of JSON. Use a public deployed Server URL."
                )
            }

            if (code in 200..299 && trimmedBody.startsWith("{")) {
                val json = JSONObject(trimmedBody)
                val rawUrl = json.optString("url", "")
                val resolvedUrl = resolveOpenUrl(rawUrl) ?: ""
                CloudOpResult(
                    ok = true,
                    httpCode = code,
                    data = UploadedAttachmentMeta(
                        fileName = json.optString("fileName", fileName),
                        url = resolvedUrl,
                        mimeType = json.optString("mimeType", mimeType),
                        sizeBytes = json.optLong("sizeBytes", bytes.size.toLong())
                    )
                )
            } else {
                val err = if (trimmedBody.startsWith("{")) {
                    runCatching { JSONObject(trimmedBody).optString("error") }.getOrNull()
                } else {
                    null
                }?.ifBlank { "Upload failed (HTTP $code)" } ?: "Upload failed (HTTP $code)"
                CloudOpResult(ok = false, httpCode = code, errorMessage = err)
            }
        } catch (e: Exception) {
            CloudOpResult(ok = false, httpCode = -1, errorMessage = "Upload offline (${e.javaClass.simpleName})")
        }
    }
}
