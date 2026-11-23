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
    val webhookUrl: String?
)

data class PaystackInitResult(
    val ok: Boolean,
    val mode: String = "paystack_test_demo",
    val reference: String = "",
    val authorizationUrl: String = "",
    val amountKes: Int = CbcCurriculumRepository.MONTHLY_LICENSE_KES,
    val expiresAt: String = "",
    val message: String = "",
    val error: String = ""
)

data class PaystackVerifyResult(
    val ok: Boolean,
    val status: String = "pending",
    val receiptRef: String? = null,
    val amountKes: Int = CbcCurriculumRepository.MONTHLY_LICENSE_KES,
    val channel: String? = null,
    val childIds: List<String> = emptyList(),
    val message: String = "",
    val error: String = ""
)

object PaystackClient {
    private val SAFE_REF_REGEX = Regex("^[a-zA-Z0-9_-]{6,80}$")

    suspend fun fetchStatus(): PaystackGatewayStatus? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = (URL("${AssignmentUploadParser.UPLOAD_BASE_URL}/api/paystack/status")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Accept", "application/json")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                conn.disconnect()
                return@runCatching null
            }
            val text = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
            conn.disconnect()
            val json = JSONObject(text)
            PaystackGatewayStatus(
                configured = json.optBoolean("configured", false),
                environment = json.optString("environment", "test"),
                monthlyKes = json.optInt("monthlyKes", CbcCurriculumRepository.MONTHLY_LICENSE_KES),
                webhookUrl = json.optString("webhookUrl", "").takeIf { it.startsWith("https://") }
            )
        }.getOrNull()
    }

    suspend fun initializeCheckout(
        phoneNumber: String,
        childIds: List<String>
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
            val url = URL("${AssignmentUploadParser.UPLOAD_BASE_URL}/api/paystack/initialize")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
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
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bodyText = stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }.orEmpty()
            conn.disconnect()

            if (bodyText.isBlank()) {
                return@withContext PaystackInitResult(
                    ok = false,
                    error = "Empty response from Paystack server (HTTP $code)."
                )
            }

            val json = JSONObject(bodyText)
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
            val conn = (URL("${AssignmentUploadParser.UPLOAD_BASE_URL}/api/paystack/verify/$encodedRef")
                .openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 15000
                readTimeout = 15000
                setRequestProperty("Accept", "application/json")
            }

            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val bodyText = stream?.let { BufferedReader(InputStreamReader(it)).use { r -> r.readText() } }.orEmpty()
            conn.disconnect()

            if (bodyText.isBlank()) {
                return@withContext PaystackVerifyResult(
                    ok = false,
                    status = "pending",
                    error = "No response from Paystack verify endpoint."
                )
            }

            val json = JSONObject(bodyText)
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
