package com.somatime.app.data

import com.somatime.app.model.ChildProfile
import com.somatime.app.model.DeviceMode
import com.somatime.app.model.LearningArea
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class CloudPairingData(
    val pairingCode: String,
    val childId: String,
    val childName: String,
    val grade: Int,
    val school: String,
    val classStream: String,
    val deviceMode: DeviceMode,
    val parentPin: String,
    val dailyCapMins: Int,
    val exchangeRateMins: Int,
    val mandatorySubjects: List<LearningArea>,
    val requireParentApproval: Boolean,
    val availableScreenSeconds: Int,
    val usedScreenTodayMins: Int,
    val learnedMinutesToday: Int,
    val isLocked: Boolean,
    val isPaired: Boolean,
    val ownerUid: String,
    val completedLessonIds: List<String>
)

data class CloudOpResult<T>(
    val ok: Boolean,
    val data: T? = null,
    val httpCode: Int = 0,
    val errorMessage: String = ""
)

object FirestoreSyncClient {

    private const val PROJECT_ID = "gen-lang-client-0447196247"
    private const val DATABASE_ID = "ai-studio-somatime-756aa707-6beb-4b24-80e0-e665ee3b8637"
    private const val API_KEY = "AIzaSyASlaaUoOUdLyVNQ41XOPsIfBk90qab1sc"
    private const val BASE_URL =
        "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/$DATABASE_ID/documents"

    private val PAIRING_CODE_REGEX = Regex("^SOMA-[A-Z0-9]{4,12}$")

    fun normalizeCode(raw: String): String {
        val cleaned = raw.trim().uppercase(Locale.US).replace(Regex("[^A-Z0-9-\\s]"), "")
        if (cleaned.isEmpty()) return ""
        val collapsed = cleaned.replace(Regex("\\s+"), "-").replace(Regex("-+"), "-")
        return when {
            collapsed.startsWith("SOMA-") -> collapsed.take(24)
            collapsed.startsWith("SOMA") && collapsed.length > 4 -> {
                val rest = collapsed.substring(4).trimStart('-')
                "SOMA-$rest".take(24)
            }
            Regex("^[A-Z0-9]{4,12}$").matches(collapsed) -> "SOMA-$collapsed".take(24)
            else -> collapsed.take(24)
        }
    }

    fun isValidCode(code: String): Boolean = PAIRING_CODE_REGEX.matches(code)

    suspend fun publishPairingSession(child: ChildProfile, parentPin: String): CloudOpResult<Boolean> =
        withContext(Dispatchers.IO) {
            val code = normalizeCode(child.pairingCode)
            if (!isValidCode(code)) {
                return@withContext CloudOpResult(
                    ok = false,
                    errorMessage = "Invalid pairing code format ($code)."
                )
            }

            val existingRes = fetchPairingRaw(code)
            if (!existingRes.ok && existingRes.httpCode != 404) {
                return@withContext CloudOpResult(
                    ok = false,
                    httpCode = existingRes.httpCode,
                    errorMessage = existingRes.errorMessage
                )
            }

            val existing = existingRes.data
            val fields = buildFieldsJson(child, parentPin, includeImmutable = existing == null)
            val body = JSONObject().put("fields", fields).toString()

            val res = if (existing == null) {
                val url = "$BASE_URL/pairings?documentId=${enc(code)}&key=${enc(API_KEY)}"
                request("POST", url, body, patchOverride = false)
            } else {
                val updateFields = listOf(
                    "childName", "grade", "school", "classStream", "deviceMode",
                    "parentPin", "dailyCapMins", "exchangeRateMins", "mandatorySubjects",
                    "requireParentApproval", "availableScreenSeconds", "usedScreenTodayMins",
                    "learnedMinutesToday", "isLocked", "isPaired", "completedLessonIds", "updatedAt"
                )
                val maskParams = updateFields.joinToString("&") { "updateMask.fieldPaths=${enc(it)}" }
                val url = "$BASE_URL/pairings/${enc(code)}?$maskParams&key=${enc(API_KEY)}"
                request("POST", url, body, patchOverride = true)
            }

            if (res.code in 200..299) {
                CloudOpResult(ok = true, data = true, httpCode = res.code)
            } else {
                CloudOpResult(
                    ok = false,
                    httpCode = res.code,
                    errorMessage = formatHttpFailure(res)
                )
            }
        }

    suspend fun fetchAndPairSession(rawCode: String): CloudOpResult<CloudPairingData> =
        withContext(Dispatchers.IO) {
            val code = normalizeCode(rawCode)
            if (!isValidCode(code)) {
                return@withContext CloudOpResult(
                    ok = false,
                    errorMessage = "Enter a valid code like SOMA-4012."
                )
            }

            val fetched = fetchPairingRaw(code)
            if (!fetched.ok || fetched.data == null) {
                return@withContext CloudOpResult(
                    ok = false,
                    httpCode = fetched.httpCode,
                    errorMessage = fetched.errorMessage
                )
            }

            val patchFields = JSONObject()
                .put("isPaired", boolVal(true))
                .put("deviceMode", strVal("own_phone"))
                .put("updatedAt", strVal(nowIso()))
            val body = JSONObject().put("fields", patchFields).toString()
            val maskParams = listOf("isPaired", "deviceMode", "updatedAt")
                .joinToString("&") { "updateMask.fieldPaths=${enc(it)}" }
            val url = "$BASE_URL/pairings/${enc(code)}?$maskParams&key=${enc(API_KEY)}"
            val patchRes = request("POST", url, body, patchOverride = true)
            if (patchRes.code !in 200..299) {
                return@withContext CloudOpResult(
                    ok = false,
                    httpCode = patchRes.code,
                    errorMessage = formatHttpFailure(patchRes)
                )
            }

            CloudOpResult(
                ok = true,
                data = fetched.data.copy(isPaired = true, deviceMode = DeviceMode.OWN_PHONE),
                httpCode = patchRes.code
            )
        }

    suspend fun fetchPairingSession(rawCode: String): CloudOpResult<CloudPairingData> =
        withContext(Dispatchers.IO) {
            val code = normalizeCode(rawCode)
            if (!isValidCode(code)) {
                return@withContext CloudOpResult(ok = false, errorMessage = "Invalid code")
            }
            fetchPairingRaw(code)
        }

    suspend fun patchLearnerSync(child: ChildProfile): CloudOpResult<Boolean> =
        withContext(Dispatchers.IO) {
            val code = normalizeCode(child.pairingCode)
            if (!isValidCode(code)) return@withContext CloudOpResult(ok = false, errorMessage = "Invalid code")

            val fields = JSONObject()
                .put("isPaired", boolVal(child.isPaired))
                .put("deviceMode", strVal(if (child.deviceMode == DeviceMode.HAND_OFF) "hand_off" else "own_phone"))
                .put("availableScreenSeconds", intVal(child.availableScreenSeconds.coerceIn(0, 28800)))
                .put("usedScreenTodayMins", intVal(child.usedScreenTodayMins.coerceIn(0, 600)))
                .put("learnedMinutesToday", intVal(child.learnedMinutesToday.coerceIn(0, 600)))
                .put("isLocked", boolVal(child.isLocked))
                .put("completedLessonIds", strArrayVal(child.completedLessonIds.take(50)))
                .put("updatedAt", strVal(nowIso()))

            val updateFields = listOf(
                "isPaired", "deviceMode", "availableScreenSeconds", "usedScreenTodayMins",
                "learnedMinutesToday", "isLocked", "completedLessonIds", "updatedAt"
            )
            val maskParams = updateFields.joinToString("&") { "updateMask.fieldPaths=${enc(it)}" }
            val url = "$BASE_URL/pairings/${enc(code)}?$maskParams&key=${enc(API_KEY)}"
            val res = request("POST", url, JSONObject().put("fields", fields).toString(), patchOverride = true)
            if (res.code in 200..299) {
                CloudOpResult(ok = true, data = true, httpCode = res.code)
            } else {
                CloudOpResult(ok = false, httpCode = res.code, errorMessage = formatHttpFailure(res))
            }
        }

    private fun fetchPairingRaw(code: String): CloudOpResult<CloudPairingData> {
        val url = "$BASE_URL/pairings/${enc(code)}?key=${enc(API_KEY)}"
        val res = request("GET", url, null, patchOverride = false)
        if (res.code !in 200..299 || res.body.isBlank()) {
            return CloudOpResult(
                ok = false,
                httpCode = res.code,
                errorMessage = formatHttpFailure(res)
            )
        }

        val root = runCatching { JSONObject(res.body) }.getOrNull()
            ?: return CloudOpResult(ok = false, httpCode = res.code, errorMessage = "Malformed cloud response.")
        val fields = root.optJSONObject("fields")
            ?: return CloudOpResult(ok = false, httpCode = res.code, errorMessage = "Missing pairing fields.")

        val pairingCode = readStr(fields, "pairingCode", code)
        val childId = readStr(fields, "childId", "child-synced")
        val childName = readStr(fields, "childName", "Learner")
        val grade = readInt(fields, "grade", 5).coerceIn(1, 12)
        val school = readStr(fields, "school", "Nairobi Primary School")
        val classStream = readStr(fields, "classStream", "Grade $grade East")
        val modeStr = readStr(fields, "deviceMode", "own_phone")
        val deviceMode = if (modeStr == "hand_off") DeviceMode.HAND_OFF else DeviceMode.OWN_PHONE
        val parentPin = readStr(fields, "parentPin", "1234").let {
            if (it.length == 4 && it.all(Char::isDigit)) it else "1234"
        }
        val dailyCapMins = readInt(fields, "dailyCapMins", 90).coerceIn(15, 300)
        val exchangeRateMins = readInt(fields, "exchangeRateMins", 15).coerceIn(5, 60)
        val mandatoryNames = readStrArray(fields, "mandatorySubjects")
        val mandatorySubjects = mandatoryNames.mapNotNull { label ->
            LearningArea.entries.firstOrNull { it.displayName == label }
        }
        val requireParentApproval = readBool(fields, "requireParentApproval", false)
        val availableScreenSeconds = readInt(fields, "availableScreenSeconds", 0).coerceIn(0, 28800)
        val usedScreenTodayMins = readInt(fields, "usedScreenTodayMins", 0).coerceIn(0, 600)
        val learnedMinutesToday = readInt(fields, "learnedMinutesToday", 0).coerceIn(0, 600)
        val isLocked = readBool(fields, "isLocked", availableScreenSeconds <= 0)
        val isPaired = readBool(fields, "isPaired", false)
        val ownerUid = readStr(fields, "ownerUid", "device_parent")
        val completedLessonIds = readStrArray(fields, "completedLessonIds")

        return CloudOpResult(
            ok = true,
            httpCode = res.code,
            data = CloudPairingData(
                pairingCode = pairingCode,
                childId = childId,
                childName = childName,
                grade = grade,
                school = school,
                classStream = classStream,
                deviceMode = deviceMode,
                parentPin = parentPin,
                dailyCapMins = dailyCapMins,
                exchangeRateMins = exchangeRateMins,
                mandatorySubjects = mandatorySubjects,
                requireParentApproval = requireParentApproval,
                availableScreenSeconds = availableScreenSeconds,
                usedScreenTodayMins = usedScreenTodayMins,
                learnedMinutesToday = learnedMinutesToday,
                isLocked = isLocked,
                isPaired = isPaired,
                ownerUid = ownerUid,
                completedLessonIds = completedLessonIds
            )
        )
    }

    private fun buildFieldsJson(
        child: ChildProfile,
        parentPin: String,
        includeImmutable: Boolean
    ): JSONObject {
        val cleanPin = parentPin.filter(Char::isDigit).take(4).let { if (it.length == 4) it else "1234" }
        val cleanChildId = child.id.replace(Regex("[^a-zA-Z0-9_-]"), "-").take(80).let {
            if (it.length >= 3) it else "child-default"
        }
        val obj = JSONObject()
        if (includeImmutable) {
            obj.put("pairingCode", strVal(normalizeCode(child.pairingCode)))
            obj.put("childId", strVal(cleanChildId))
            obj.put("ownerUid", strVal("device_parent"))
        }
        obj.put("childName", strVal(child.name.trim().ifEmpty { "Learner" }.take(80)))
        obj.put("grade", intVal(child.grade.coerceIn(1, 12)))
        obj.put("school", strVal(child.school.trim().ifEmpty { "Nairobi Primary School" }.take(120)))
        obj.put("classStream", strVal(child.classStream.trim().ifEmpty { "Grade ${child.grade} East" }.take(120)))
        obj.put("deviceMode", strVal(if (child.deviceMode == DeviceMode.HAND_OFF) "hand_off" else "own_phone"))
        obj.put("parentPin", strVal(cleanPin))
        obj.put("dailyCapMins", intVal(child.dailyCapMins.coerceIn(15, 300)))
        obj.put("exchangeRateMins", intVal(child.exchangeRateMins.coerceIn(5, 60)))
        obj.put(
            "mandatorySubjects",
            strArrayVal(child.mandatorySubjects.take(8).map { it.displayName })
        )
        obj.put("requireParentApproval", boolVal(child.requireParentApproval))
        obj.put("availableScreenSeconds", intVal(child.availableScreenSeconds.coerceIn(0, 28800)))
        obj.put("usedScreenTodayMins", intVal(child.usedScreenTodayMins.coerceIn(0, 600)))
        obj.put("learnedMinutesToday", intVal(child.learnedMinutesToday.coerceIn(0, 600)))
        obj.put("isLocked", boolVal(child.isLocked))
        obj.put("isPaired", boolVal(child.isPaired))
        obj.put("completedLessonIds", strArrayVal(child.completedLessonIds.take(50)))
        obj.put("updatedAt", strVal(nowIso()))
        return obj
    }

    private data class HttpRes(val code: Int, val body: String, val exceptionMsg: String = "")

    private fun formatHttpFailure(res: HttpRes): String {
        return when {
            res.code == -1 -> "Cannot reach Cloud Firestore (${res.exceptionMsg.ifEmpty { "check mobile data or Wi-Fi" }})."
            res.code == 404 -> "Code not found in Cloud Firestore yet. Ensure Parent phone is connected and shows 'Synced to Cloud'."
            res.code == 403 -> "Cloud permission denied (HTTP 403)."
            else -> "Cloud error (HTTP ${res.code})."
        }
    }

    private fun request(
        method: String,
        urlString: String,
        body: String?,
        patchOverride: Boolean
    ): HttpRes {
        return try {
            val conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = method
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
                if (patchOverride) {
                    setRequestProperty("X-HTTP-Method-Override", "PATCH")
                }
                if (body != null) {
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                    OutputStreamWriter(outputStream, Charsets.UTF_8).use { it.write(body) }
                }
            }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let {
                BufferedReader(InputStreamReader(it, Charsets.UTF_8)).use { r -> r.readText() }
            } ?: ""
            conn.disconnect()
            HttpRes(code, text)
        } catch (e: Exception) {
            HttpRes(-1, "", e.javaClass.simpleName)
        }
    }

    private fun strVal(v: String) = JSONObject().put("stringValue", v)
    private fun intVal(v: Int) = JSONObject().put("integerValue", v.toString())
    private fun boolVal(v: Boolean) = JSONObject().put("booleanValue", v)
    private fun strArrayVal(items: List<String>): JSONObject {
        val arr = JSONArray()
        items.forEach { arr.put(strVal(it)) }
        val inner = JSONObject()
        if (items.isNotEmpty()) inner.put("values", arr)
        return JSONObject().put("arrayValue", inner)
    }

    private fun readStr(fields: JSONObject, key: String, fallback: String): String =
        fields.optJSONObject(key)?.optString("stringValue", fallback)?.ifEmpty { fallback } ?: fallback

    private fun readInt(fields: JSONObject, key: String, fallback: Int): Int {
        val str = fields.optJSONObject(key)?.optString("integerValue", "")
        return str?.toIntOrNull() ?: fallback
    }

    private fun readBool(fields: JSONObject, key: String, fallback: Boolean): Boolean {
        val obj = fields.optJSONObject(key) ?: return fallback
        return if (obj.has("booleanValue")) obj.optBoolean("booleanValue", fallback) else fallback
    }

    private fun readStrArray(fields: JSONObject, key: String): List<String> {
        val values = fields.optJSONObject(key)
            ?.optJSONObject("arrayValue")
            ?.optJSONArray("values") ?: return emptyList()
        return buildList {
            for (i in 0 until values.length()) {
                val item = values.optJSONObject(i)?.optString("stringValue", "") ?: ""
                if (item.isNotEmpty()) add(item)
            }
        }
    }

    private fun enc(v: String): String = URLEncoder.encode(v, "UTF-8")
    private fun nowIso(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(Date())
    }
}
