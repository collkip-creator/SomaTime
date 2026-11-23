package com.somatime.app.data

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class PaystackGatewayStatus(
    val configured: Boolean,
    val environment: String,
    val monthlyKes: Int,
    val webhookUrl: String?,
    val requiresBrowserAuth: Boolean = false
)

data class PaystackInitResult(
    val ok: Boolean,
    val mode: String = "paystack_test_demo",
    val reference: String = "",
    val authorizationUrl: String = "",
    val amountKes: Int = CbcCurriculumRepository.MONTHLY_LICENSE_KES,
    val expiresAt: String = "",
    val message: String = "",
    val error: String = "",
    val requiresBrowserAuth: Boolean = false,
    val browserCheckoutUrl: String = ""
)

data class PaystackVerifyResult(
    val ok: Boolean,
    val status: String = "pending",
    val receiptRef: String? = null,
    val amountKes: Int = CbcCurriculumRepository.MONTHLY_LICENSE_KES,
    val channel: String? = null,
    val childIds: List<String> = emptyList(),
    val message: String = "",
    val error: String = "",
    val requiresBrowserAuth: Boolean = false
)

object PaystackClient {
    private val SAFE_REF_REGEX = Regex("^[a-zA-Z0-9_-]{6,80}$")

    private fun isHtmlOrAuthRedirect(
        httpCode: Int,
        contentType: String?,
        body: String,
        location: String?
    ): Boolean {
        val loc = location.orEmpty().lowercase()
        val ct = contentType.orEmpty().lowercase()
        val trimmed = body.trimStart()
        if (httpCode in 300..399) return true
        if (ct.contains("text/html") || trimmed.startsWith("<")) return true
        if ("__cookie_check" in loc || "applet-auth-bridge" in loc || "accounts.google.com" in loc) return true
        return false
    }

    private fun parseJsonObjectSafely(raw: String): JSONObject? {
        val trimmed = raw.trimStart()
        if (!trimmed.startsWith("{")) return null
        return runCatching { JSONObject(trimmed) }.getOrNull()
    }

    fun buildAndroidBrowserCheckoutUrl(
        phoneNumber: String,
        childIds: List<String>,
        pairingCodes: List<String> = emptyList(),
        childNames: List<String> = emptyList()
    ): String {
        val base = AssignmentUploadParser.getServerBaseUrl()
        val encPhone = URLEncoder.encode(phoneNumber.trim(), "UTF-8")
        val encChildIds = URLEncoder.encode(childIds.joinToString(","), "UTF-8")
        val encCodes = URLEncoder.encode(pairingCodes.joinToString(","), "UTF-8")
        val encNames = URLEncoder.encode(childNames.joinToString(", "), "UTF-8")
        return "$base/?android_paystack=1&phone=$encPhone&childIds=$encChildIds&pairingCodes=$encCodes&childNames=$encNames"
    }

    suspend fun fetchStatus(): PaystackGatewayStatus? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("${AssignmentUploadParser.getServerBaseUrl()}/api/paystack/status")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            val contentType = conn.contentType
            val location = conn.getHeaderField("Location")
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }.orEmpty()
            conn.disconnect()

            if (isHtmlOrAuthRedirect(code, contentType, text, location)) {
                return@runCatching PaystackGatewayStatus(
                    configured = true,
                    environment = "browser_auth",
                    monthlyKes = CbcCurriculumRepository.MONTHLY_LICENSE_KES,
                    webhookUrl = null,
                    requiresBrowserAuth = true
                )
            }
            if (code !in 200..299) return@runCatching null
            val json = parseJsonObjectSafely(text) ?: return@runCatching null
            PaystackGatewayStatus(
                configured = json.optBoolean("configured", false),
                environment = json.optString("environment", "test"),
                monthlyKes = json.optInt("monthlyKes", CbcCurriculumRepository.MONTHLY_LICENSE_KES),
                webhookUrl = json.optString("webhookUrl", "").takeIf { it.startsWith("https://") },
                requiresBrowserAuth = false
            )
        }.getOrNull()
    }

    suspend fun initializeCheckout(
        phoneNumber: String,
        childIds: List<String>,
        pairingCodes: List<String> = emptyList(),
        childNames: List<String> = emptyList()
    ): PaystackInitResult = withContext(Dispatchers.IO) {
        val digits = phoneNumber.filter(Char::isDigit)
        if (digits.length < 9) {
            return@withContext PaystackInitResult(
                ok = false,
                error = "Enter a valid Kenyan M-Pesa phone number (e.g. 0712 345 678)."
            )
        }
        if (childIds.isEmpty()) {
            return@withContext PaystackInitResult(
                ok = false,
                error = "Select at least one child profile to license."
            )
        }

        try {
            val base = AssignmentUploadParser.getServerBaseUrl()
            val url = URL("$base/api/paystack/initialize")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 15000
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
            }

            val payload = JSONObject().apply {
                put("phoneNumber", phoneNumber.trim())
                put("childIds", JSONArray(childIds))
                put("client", "android")
            }

            OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val code = conn.responseCode
            val contentType = conn.contentType
            val location = conn.getHeaderField("Location")
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bodyText = stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }.orEmpty()
            conn.disconnect()

            if (isHtmlOrAuthRedirect(code, contentType, bodyText, location)) {
                val browserUrl = buildAndroidBrowserCheckoutUrl(
                    phoneNumber = phoneNumber,
                    childIds = childIds,
                    pairingCodes = pairingCodes,
                    childNames = childNames
                )
                return@withContext PaystackInitResult(
                    ok = false,
                    requiresBrowserAuth = true,
                    browserCheckoutUrl = browserUrl,
                    amountKes = childIds.size * CbcCurriculumRepository.MONTHLY_LICENSE_KES,
                    error = "The AI Studio preview server requires browser authentication before sending the M-Pesa prompt. Continue in your browser or set a public Server URL."
                )
            }

            if (bodyText.isBlank()) {
                return@withContext PaystackInitResult(
                    ok = false,
                    error = "Empty response from Paystack server (HTTP $code)."
                )
            }

            val json = parseJsonObjectSafely(bodyText)
                ?: return@withContext PaystackInitResult(
                    ok = false,
                    error = "Unexpected non-JSON response from Paystack backend (HTTP $code)."
                )

            if (code !in 200..299 || !json.optBoolean("ok", false)) {
                return@withContext PaystackInitResult(
                    ok = false,
                    error = json.optString("error", "Could not send Paystack M-Pesa prompt.")
                )
            }

            val mode = json.optString("mode", "paystack_test_demo")
            val ref = json.optString("receiptRef", "").ifBlank { json.optString("reference", "") }

            PaystackInitResult(
                ok = true,
                mode = mode,
                reference = json.optString("reference", ref),
                authorizationUrl = json.optString("authorizationUrl", ""),
                amountKes = json.optInt("amountKes", childIds.size * CbcCurriculumRepository.MONTHLY_LICENSE_KES),
                expiresAt = json.optString("expiresAt", CbcCurriculumRepository.buildLicenseExpiryIso(30)),
                message = json.optString("message", "M-Pesa prompt sent to phone.")
            )
        } catch (err: Exception) {
            PaystackInitResult(
                ok = false,
                error = err.message ?: "Unable to reach SomaTime Paystack backend."
            )
        }
    }

    suspend fun verifyTransaction(reference: String): PaystackVerifyResult = withContext(Dispatchers.IO) {
        val cleanRef = reference.trim()
        if (!SAFE_REF_REGEX.matches(cleanRef)) {
            return@withContext PaystackVerifyResult(
                ok = false,
                status = "failed",
                error = "Invalid Paystack transaction reference."
            )
        }

        try {
            val encodedRef = URLEncoder.encode(cleanRef, "UTF-8")
            val conn = (URL("${AssignmentUploadParser.getServerBaseUrl()}/api/paystack/verify/$encodedRef")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = false
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            val contentType = conn.contentType
            val location = conn.getHeaderField("Location")
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bodyText = stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }.orEmpty()
            conn.disconnect()

            if (isHtmlOrAuthRedirect(code, contentType, bodyText, location)) {
                return@withContext PaystackVerifyResult(
                    ok = false,
                    status = "pending",
                    requiresBrowserAuth = true,
                    error = "Server requires browser session to verify transaction."
                )
            }

            if (bodyText.isBlank()) {
                return@withContext PaystackVerifyResult(
                    ok = false,
                    status = "pending",
                    error = "No response from Paystack verify endpoint."
                )
            }

            val json = parseJsonObjectSafely(bodyText)
                ?: return@withContext PaystackVerifyResult(
                    ok = false,
                    status = "pending",
                    error = "Unexpected non-JSON response from Paystack verify endpoint (HTTP $code)."
                )

            if (code !in 200..299) {
                return@withContext PaystackVerifyResult(
                    ok = false,
                    status = "failed",
                    error = json.optString("error", "Paystack verification failed.")
                )
            }

            val childIdsArray = json.optJSONArray("childIds")
            val parsedChildIds = buildList {
                if (childIdsArray != null) {
                    for (i in 0 until childIdsArray.length()) {
                        val id = childIdsArray.optString(i, "")
                        if (id.isNotBlank()) add(id)
                    }
                }
            }

            PaystackVerifyResult(
                ok = json.optBoolean("ok", true),
                status = json.optString("status", "pending"),
                receiptRef = json.optString("receiptRef", "").takeIf { it.isNotBlank() },
                amountKes = json.optInt("amountKes", CbcCurriculumRepository.MONTHLY_LICENSE_KES),
                channel = json.optString("channel", "").takeIf { it.isNotBlank() },
                childIds = parsedChildIds,
                message = json.optString("message", "")
            )
        } catch (err: Exception) {
            PaystackVerifyResult(
                ok = false,
                status = "pending",
                error = err.message ?: "Unable to verify Paystack transaction right now."
            )
        }
    }
}
