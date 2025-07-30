package notion.api.v1.http

import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.ByteBuffer
import java.time.Duration
import java.util.*
import notion.api.v1.logging.NotionLogger

// TODO: proxy support
class JavaNetHttpClient(
    connectTimeoutMillis: Int = 3_000,
    private val readTimeoutMillis: Int = 30_000,
) : NotionHttpClient {
  private val client: HttpClient =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofMillis(connectTimeoutMillis.toLong()))
          .build()

  override fun get(
      logger: NotionLogger,
      url: String,
      query: Map<String, List<String>>,
      headers: Map<String, String>
  ): NotionHttpResponse {
    val startTimeMillis = System.currentTimeMillis()
    val fullUrl = buildFullUrl(url, buildQueryString(query))
    val req =
        HttpRequest.newBuilder()
            .GET()
            .uri(URI(fullUrl))
            .timeout(Duration.ofMillis(readTimeoutMillis.toLong()))
    headers.forEach { (name, value) -> req.header(name, value) }
    val request = req.build()
    debugLogStart(logger, request.method(), fullUrl, "")
    try {
      val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
      val response =
          NotionHttpResponse(
              status = resp.statusCode(), headers = resp.headers().map(), body = resp.body())
      debugLogSuccess(logger, startTimeMillis, response)
      return response
    } catch (e: Exception) {
      warnLogFailure(logger, e)
      throw e
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
    val fullUrl = buildFullUrl(url, buildQueryString(query))
    val req =
        HttpRequest.newBuilder()
            .POST(HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
            .uri(URI(fullUrl))
            .timeout(Duration.ofMillis(readTimeoutMillis.toLong()))
    headers.forEach { (name, value) -> req.header(name, value) }
    val request = req.build()
    debugLogStart(logger, request.method(), fullUrl, body)
    try {
      val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
      val response =
          NotionHttpResponse(
              status = resp.statusCode(), headers = resp.headers().map(), body = resp.body())
      debugLogSuccess(logger, startTimeMillis, response)
      return response
    } catch (e: Exception) {
      warnLogFailure(logger, e)
      throw e
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
    val fullUrl = buildFullUrl(url, buildQueryString(query))
    val req =
        HttpRequest.newBuilder()
            .method("PATCH", HttpRequest.BodyPublishers.ofString(body, Charsets.UTF_8))
            .uri(URI(buildFullUrl(url, buildQueryString(query))))
            .timeout(Duration.ofMillis(readTimeoutMillis.toLong()))
    headers.forEach { (name, value) -> req.header(name, value) }
    val request = req.build()
    debugLogStart(logger, request.method(), fullUrl, body)
    try {
      val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
      val response =
          NotionHttpResponse(
              status = resp.statusCode(), headers = resp.headers().map(), body = resp.body())
      debugLogSuccess(logger, startTimeMillis, response)
      return response
    } catch (e: Exception) {
      warnLogFailure(logger, e)
      throw e
    }
  }

  override fun delete(
      logger: NotionLogger,
      url: String,
      query: Map<String, List<String>>,
      headers: Map<String, String>
  ): NotionHttpResponse {
    val startTimeMillis = System.currentTimeMillis()
    val fullUrl = buildFullUrl(url, buildQueryString(query))
    val req =
        HttpRequest.newBuilder()
            .DELETE()
            .uri(URI(fullUrl))
            .timeout(Duration.ofMillis(readTimeoutMillis.toLong()))
    headers.forEach { (name, value) -> req.header(name, value) }
    val request = req.build()
    debugLogStart(logger, request.method(), fullUrl, "")
    try {
      val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
      val response =
          NotionHttpResponse(
              status = resp.statusCode(), headers = resp.headers().map(), body = resp.body())
      debugLogSuccess(logger, startTimeMillis, response)
      return response
    } catch (e: Exception) {
      warnLogFailure(logger, e)
      throw e
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
    val fullUrl = buildFullUrl(url, buildQueryString(query))
    val boundary = "----formdata-notion-sdk-${UUID.randomUUID()}"
    
    val multipartHeaders = headers.toMutableMap()
    multipartHeaders["Content-Type"] = "multipart/form-data; boundary=$boundary"
    
    val bodyData = buildMultipartBody(formData, boundary)
    
    val req = HttpRequest.newBuilder()
        .POST(HttpRequest.BodyPublishers.ofByteArray(bodyData))
        .uri(URI(fullUrl))
        .timeout(Duration.ofMillis(readTimeoutMillis.toLong()))
    
    multipartHeaders.forEach { (name, value) -> req.header(name, value) }
    val request = req.build()
    debugLogStart(logger, request.method(), fullUrl, "multipart form data")
    
    try {
      val resp = client.send(request, HttpResponse.BodyHandlers.ofString())
      val response = NotionHttpResponse(
          status = resp.statusCode(), 
          headers = resp.headers().map(), 
          body = resp.body()
      )
      debugLogSuccess(logger, startTimeMillis, response)
      return response
    } catch (e: Exception) {
      warnLogFailure(logger, e)
      throw e
    }
  }

  private fun buildMultipartBody(formData: Map<String, Any>, boundary: String): ByteArray {
    val result = mutableListOf<ByteArray>()
    val mimeType = formData["mimeType"] as? String
    
    formData.forEach { (name, value) ->
      if (name != "mimeType") { // Don't write mimeType as a form field
        result.add("--$boundary\r\n".toByteArray(Charsets.UTF_8))
        
        when (value) {
          is InputStream -> {
            result.add("Content-Disposition: form-data; name=\"$name\"; filename=\"file\"\r\n".toByteArray(Charsets.UTF_8))
            val contentType = if (name == "file" && mimeType != null) mimeType else "application/octet-stream"
            result.add("Content-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
            value.use { input ->
              result.add(input.readAllBytes())
            }
          }
          else -> {
            result.add("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
            result.add(value.toString().toByteArray(Charsets.UTF_8))
          }
        }
        result.add("\r\n".toByteArray(Charsets.UTF_8))
      }
    }
    
    result.add("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
    
    // Combine all byte arrays
    val totalSize = result.sumOf { it.size }
    val combined = ByteArray(totalSize)
    var offset = 0
    for (bytes in result) {
      System.arraycopy(bytes, 0, combined, offset, bytes.size)
      offset += bytes.size
    }
    
    return combined
  }
}
