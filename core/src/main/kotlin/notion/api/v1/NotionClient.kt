package notion.api.v1

import java.io.Closeable
import java.io.File
import notion.api.v1.endpoint.*
import notion.api.v1.model.files.FileUpload
import notion.api.v1.model.files.FileUploadStatus
import notion.api.v1.request.files.CompleteFileUploadRequest
import notion.api.v1.exception.NotionAPIError
import notion.api.v1.http.HttpUrlConnNotionHttpClient
import notion.api.v1.http.NotionHttpClient
import notion.api.v1.json.GsonSerializer
import notion.api.v1.json.NotionJsonSerializer
import notion.api.v1.logging.NotionLogger
import notion.api.v1.logging.StdoutLogger

class NotionClient
// We don't intentionally use @JvmOverloads here. Refer to the following constructors for details.
// @JvmOverloads
constructor(
    override var token: String? = null,
    override var clientId: String? = null,
    override var clientSecret: String? = null,
    override var redirectUri: String? = null,
    override var httpClient: NotionHttpClient = defaultHttpClient,
    override var logger: NotionLogger = defaultLogger,
    override var jsonSerializer: NotionJsonSerializer = defaultJsonSerializer,
    override var baseUrl: String = defaultBaseUrl,
) :
    AutoCloseable,
    Closeable,
    DatabasesSupport,
    PagesSupport,
    BlocksSupport,
    CommentsSupport,
    SearchSupport,
    UsersSupport,
    FilesSupport,
    OAuthSupport {

  // Internal app initialization
  // This constructor is for Java and other languages
  constructor(
      token: String
  ) : this(
      token = token,
      clientId = null,
      clientSecret = null,
      redirectUri = null,
  )

  // OAuth wired app initialization
  // This constructor is for Java and other languages
  constructor(
      clientId: String,
      clientSecret: String,
      redirectUri: String
  ) : this(
      token = null,
      clientId = clientId,
      clientSecret = clientSecret,
      redirectUri = redirectUri,
  )

  // -----------------------------------------------
  // File Upload Convenience Methods
  // -----------------------------------------------

  /**
   * Upload a file to Notion (for files under 20MB)
   * @param file The file to upload
   * @return The uploaded FileUpload object with status "uploaded"
   */
  fun uploadFile(file: File): FileUpload {
    require(file.exists()) { "File does not exist: ${file.absolutePath}" }
    require(file.length() > 0) { "File is empty: ${file.absolutePath}" }
    
    // Create the file upload intent
    val fileUpload = createFileUpload(
        filename = file.name,
        fileSize = file.length(),
        mimeType = guessMimeType(file)
    )
    
    // Send the file content - for small files, this returns the completed FileUpload directly
    val mimeType = guessMimeType(file)
    val httpResponse = httpClient.postMultipartBody(
        logger = logger,
        url = "$baseUrl/file_uploads/${fileUpload.id}/send",
        formData = mapOf(
            "file" to file.inputStream(),
            "mimeType" to (mimeType ?: "application/octet-stream")
        ).filterValues { it != "application/octet-stream" || mimeType == null },
        headers = buildRequestHeaders(emptyMap())
    )
    
    if (httpResponse.status == 200) {
        // For small files, the response is a complete FileUpload object with status "uploaded"
        return jsonSerializer.toFileUpload(httpResponse.body)
    } else {
        throw NotionAPIError(
            error = jsonSerializer.toError(httpResponse.body),
            httpResponse = httpResponse,
        )
    }
  }

  /**
   * Upload a file to Notion with custom filename (for files under 20MB)
   * @param file The file to upload
   * @param filename The custom filename to use
   * @return The uploaded FileUpload object with status "uploaded"
   */
  fun uploadFile(file: File, filename: String): FileUpload {
    require(file.exists()) { "File does not exist: ${file.absolutePath}" }
    require(file.length() > 0) { "File is empty: ${file.absolutePath}" }
    require(filename.isNotBlank()) { "Filename must not be blank" }
    
    // Create the file upload intent
    val fileUpload = createFileUpload(
        filename = filename,
        fileSize = file.length(),
        mimeType = guessMimeType(file)
    )
    
    // Send the file content - for small files, this returns the completed FileUpload directly
    val mimeType = guessMimeType(file)
    val httpResponse = httpClient.postMultipartBody(
        logger = logger,
        url = "$baseUrl/file_uploads/${fileUpload.id}/send",
        formData = mapOf(
            "file" to file.inputStream(),
            "mimeType" to (mimeType ?: "application/octet-stream")
        ).filterValues { it != "application/octet-stream" || mimeType == null },
        headers = buildRequestHeaders(emptyMap())
    )
    
    if (httpResponse.status == 200) {
        // For small files, the response is a complete FileUpload object with status "uploaded"
        return jsonSerializer.toFileUpload(httpResponse.body)
    } else {
        throw NotionAPIError(
            error = jsonSerializer.toError(httpResponse.body),
            httpResponse = httpResponse,
        )
    }
  }

  private fun guessMimeType(file: File): String? {
    return when (file.extension.lowercase()) {
      "pdf" -> "application/pdf"
      "doc" -> "application/msword"
      "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
      "xls" -> "application/vnd.ms-excel"
      "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
      "ppt" -> "application/vnd.ms-powerpoint"
      "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
      "jpg", "jpeg" -> "image/jpeg"
      "png" -> "image/png"
      "gif" -> "image/gif"
      "svg" -> "image/svg+xml"
      "mp4" -> "video/mp4"
      "mp3" -> "audio/mpeg"
      "txt" -> "text/plain"
      "csv" -> "text/csv"
      "json" -> "application/json"
      "zip" -> "application/zip"
      else -> null
    }
  }

  companion object {
    @JvmStatic val defaultBaseUrl: String = "https://api.notion.com/v1"
    @JvmStatic val defaultHttpClient: NotionHttpClient = HttpUrlConnNotionHttpClient()
    @JvmStatic val defaultLogger: NotionLogger = StdoutLogger()
    @JvmStatic val defaultJsonSerializer: NotionJsonSerializer = GsonSerializer()
  }

  override fun close() {
    httpClient.close()
  }
}
