package notion.api.v1.http

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import notion.api.v1.http.HttpUrlConnPatchMethodWorkaround.setPatchRequestMethod
import notion.api.v1.logging.NotionLogger

// TODO: proxy support
class HttpUrlConnNotionHttpClient
@JvmOverloads
constructor(
    private val connectTimeoutMillis: Int = 3_000,
    private val readTimeoutMillis: Int = 30_000,
) : NotionHttpClient {

  override fun get(
      logger: NotionLogger,
      url: String,
      query: Map<String, List<String>>,
      headers: Map<String, String>
  ): NotionHttpResponse {
    val startTimeMillis = System.currentTimeMillis()
    val q = buildQueryString(query)
    val fullUrl = buildFullUrl(url, q)
    val conn = buildConnectionObject(fullUrl, headers)
    try {
      conn.requestMethod = "GET"
      debugLogStart(logger, conn.requestMethod, fullUrl, null)
      connect(conn).use { input ->
        val response =
            NotionHttpResponse(
                status = conn.responseCode,
                body = readResponseBody(input),
                headers = conn.headerFields)
        debugLogSuccess(logger, startTimeMillis, response)
        return response
      }
    } finally {
      disconnect(conn, logger)
    }
  }

  override fun postTextBody(
      logger: NotionLogger,
      url: String,
      query: Map<String, List<String>>,
      body: String,
      headers: Map<String, String>
  ): NotionHttpResponse {
    val startTimeMillis = System.currentTimeMillis()
    val q = buildQueryString(query)
    val fullUrl = buildFullUrl(url, q)
    val conn = buildConnectionObject(fullUrl, headers)
    try {
      conn.requestMethod = "POST"
      debugLogStart(logger, conn.requestMethod, fullUrl, body)
      setRequestBody(conn, body)
      connect(conn).use { input ->
        val response =
            NotionHttpResponse(
                status = conn.responseCode,
                body = readResponseBody(input),
                headers = conn.headerFields)
        debugLogSuccess(logger, startTimeMillis, response)
        return response
      }
    } finally {
      disconnect(conn, logger)
    }
  }

  override fun patchTextBody(
      logger: NotionLogger,
      url: String,
      query: Map<String, List<String>>,
      body: String,
      headers: Map<String, String>
  ): NotionHttpResponse {
    val startTimeMillis = System.currentTimeMillis()
    val q = buildQueryString(query)
    val fullUrl = buildFullUrl(url, q)
    val conn = buildConnectionObject(fullUrl, headers)
    try {
      setPatchRequestMethod(conn)
      debugLogStart(logger, conn.requestMethod, fullUrl, body)
      setRequestBody(conn, body)
      connect(conn).use { input ->
        val response =
            NotionHttpResponse(
                status = conn.responseCode,
                body = readResponseBody(input),
                headers = conn.headerFields)
        debugLogSuccess(logger, startTimeMillis, response)
        return response
      }
    } finally {
      disconnect(conn, logger)
    }
  }

  override fun delete(
      logger: NotionLogger,
      url: String,
      query: Map<String, List<String>>,
      headers: Map<String, String>
  ): NotionHttpResponse {
    val startTimeMillis = System.currentTimeMillis()
    val q = buildQueryString(query)
    val fullUrl = buildFullUrl(url, q)
    val conn = buildConnectionObject(fullUrl, headers)
    try {
      conn.requestMethod = "DELETE"
      debugLogStart(logger, conn.requestMethod, fullUrl, null)
      connect(conn).use { input ->
        val response =
            NotionHttpResponse(
                status = conn.responseCode,
                body = readResponseBody(input),
                headers = conn.headerFields)
        debugLogSuccess(logger, startTimeMillis, response)
        return response
      }
    } finally {
      disconnect(conn, logger)
    }
  }

  override fun postMultipartBody(
      logger: NotionLogger,
      url: String,
      query: Map<String, List<String>>,
      formData: Map<String, Any>,
      headers: Map<String, String>
  ): NotionHttpResponse {
    val startTimeMillis = System.currentTimeMillis()
    val q = buildQueryString(query)
    val fullUrl = buildFullUrl(url, q)
    val boundary = "----formdata-notion-sdk-${UUID.randomUUID()}"
    val multipartHeaders = headers.toMutableMap()
    multipartHeaders["Content-Type"] = "multipart/form-data; boundary=$boundary"
    
    val conn = buildConnectionObject(fullUrl, multipartHeaders)
    try {
      conn.requestMethod = "POST"
      debugLogStart(logger, conn.requestMethod, fullUrl, "multipart form data")
      setMultipartRequestBody(conn, formData, boundary)
      connect(conn).use { input ->
        val response =
            NotionHttpResponse(
                status = conn.responseCode,
                body = readResponseBody(input),
                headers = conn.headerFields)
        debugLogSuccess(logger, startTimeMillis, response)
        return response
      }
    } finally {
      disconnect(conn, logger)
    }
  }

  // -----------------------------------------------

  private fun buildConnectionObject(
      fullUrl: String,
      headers: Map<String, String>
  ): HttpURLConnection {
    val conn = URL(fullUrl).openConnection() as HttpURLConnection
    conn.setRequestProperty("Connection", "close")
    conn.connectTimeout = connectTimeoutMillis
    conn.readTimeout = readTimeoutMillis
    headers.forEach { (name, value) -> conn.setRequestProperty(name, value) }
    return conn
  }

  private fun setRequestBody(conn: HttpURLConnection, body: String) {
    conn.doOutput = true
    conn.outputStream.use { out -> out.write(body.toByteArray(Charsets.UTF_8)) }
  }

  private fun setMultipartRequestBody(conn: HttpURLConnection, formData: Map<String, Any>, boundary: String) {
    conn.doOutput = true
    conn.outputStream.use { out ->
      val mimeType = formData["mimeType"] as? String
      formData.forEach { (name, value) ->
        if (name != "mimeType") { // Don't write mimeType as a form field
          writeMultipartField(out, boundary, name, value, mimeType)
        }
      }
      // Write closing boundary
      out.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
    }
  }

  private fun writeMultipartField(out: OutputStream, boundary: String, name: String, value: Any, mimeType: String? = null) {
    out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
    
    when (value) {
      is InputStream -> {
        out.write("Content-Disposition: form-data; name=\"$name\"; filename=\"file\"\r\n".toByteArray(Charsets.UTF_8))
        val contentType = if (name == "file" && mimeType != null) mimeType else "application/octet-stream"
        out.write("Content-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
        value.use { input ->
          input.copyTo(out)
        }
      }
      else -> {
        out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(value.toString().toByteArray(Charsets.UTF_8))
      }
    }
    out.write("\r\n".toByteArray(Charsets.UTF_8))
  }

  private fun connect(conn: HttpURLConnection): InputStream =
      try {
        conn.connect()
        conn.inputStream
      } catch (e: IOException) {
        conn.errorStream
      }

  private fun readResponseBody(input: InputStream?): String {
    return input?.bufferedReader(Charsets.UTF_8).use { it?.readText() } ?: ""
  }

  private fun disconnect(conn: HttpURLConnection, logger: NotionLogger) {
    try {
      conn.disconnect()
    } catch (e: Exception) {
      warnLogFailure(logger, e)
    }
  }
}
