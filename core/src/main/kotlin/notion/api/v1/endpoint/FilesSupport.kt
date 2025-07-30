package notion.api.v1.endpoint

import notion.api.v1.exception.NotionAPIError
import notion.api.v1.http.NotionHttpClient
import notion.api.v1.json.NotionJsonSerializer
import notion.api.v1.logging.NotionLogger
import notion.api.v1.model.files.FileUpload
import notion.api.v1.model.files.FileUploadPartResponse
import notion.api.v1.request.files.CompleteFileUploadRequest
import notion.api.v1.request.files.CreateFileUploadRequest
import notion.api.v1.request.files.SendFileUploadRequest
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

interface FilesSupport : EndpointsSupport {
    val httpClient: NotionHttpClient
    val jsonSerializer: NotionJsonSerializer
    val logger: NotionLogger
    val baseUrl: String

    // -----------------------------------------------
    // createFileUpload
    // -----------------------------------------------

    fun createFileUpload(
        filename: String,
        fileSize: Long,
        mimeType: String? = null,
        partSize: Long? = null,
    ): FileUpload {
        return createFileUpload(
            CreateFileUploadRequest(
                filename = filename,
                file_size = fileSize,
                mime_type = mimeType,
                part_size = partSize,
            )
        )
    }

    fun createFileUpload(request: CreateFileUploadRequest): FileUpload {
        val httpResponse = httpClient.postTextBody(
            logger = logger,
            url = "$baseUrl/file_uploads",
            body = jsonSerializer.toJsonString(request),
            headers = buildRequestHeaders(contentTypeJson())
        )
        if (httpResponse.status == 200) {
            return jsonSerializer.toFileUpload(httpResponse.body)
        } else {
            throw NotionAPIError(
                error = jsonSerializer.toError(httpResponse.body),
                httpResponse = httpResponse,
            )
        }
    }

    // -----------------------------------------------
    // sendFileUpload
    // -----------------------------------------------

    fun sendFileUpload(
        fileUploadId: String,
        file: File,
        partNumber: Int? = null,
        mimeType: String? = null,
    ): FileUploadPartResponse {
        return sendFileUpload(
            fileUploadId = fileUploadId,
            request = SendFileUploadRequest(
                file = FileInputStream(file),
                part_number = partNumber,
                mimeType = mimeType,
            )
        )
    }

    fun sendFileUpload(
        fileUploadId: String,
        inputStream: InputStream,
        partNumber: Int? = null,
        mimeType: String? = null,
    ): FileUploadPartResponse {
        return sendFileUpload(
            fileUploadId = fileUploadId,
            request = SendFileUploadRequest(
                file = inputStream,
                part_number = partNumber,
                mimeType = mimeType,
            )
        )
    }

    fun sendFileUpload(
        fileUploadId: String,
        request: SendFileUploadRequest,
    ): FileUploadPartResponse {
        val httpResponse = httpClient.postMultipartBody(
            logger = logger,
            url = "$baseUrl/file_uploads/$fileUploadId/send",
            formData = buildMultipartFormData(request),
            headers = buildRequestHeaders(emptyMap())
        )
        if (httpResponse.status == 200) {
            return jsonSerializer.toFileUploadPartResponse(httpResponse.body)
        } else {
            throw NotionAPIError(
                error = jsonSerializer.toError(httpResponse.body),
                httpResponse = httpResponse,
            )
        }
    }

    // -----------------------------------------------
    // completeFileUpload
    // -----------------------------------------------

    fun completeFileUpload(
        fileUploadId: String,
        parts: List<CompleteFileUploadRequest.FileUploadPart>,
    ): FileUpload {
        return completeFileUpload(
            fileUploadId = fileUploadId,
            request = CompleteFileUploadRequest(parts = parts)
        )
    }

    fun completeFileUpload(
        fileUploadId: String,
        request: CompleteFileUploadRequest,
    ): FileUpload {
        val httpResponse = httpClient.postTextBody(
            logger = logger,
            url = "$baseUrl/file_uploads/$fileUploadId/complete",
            body = jsonSerializer.toJsonString(request),
            headers = buildRequestHeaders(contentTypeJson())
        )
        if (httpResponse.status == 200) {
            return jsonSerializer.toFileUpload(httpResponse.body)
        } else {
            throw NotionAPIError(
                error = jsonSerializer.toError(httpResponse.body),
                httpResponse = httpResponse,
            )
        }
    }

    // -----------------------------------------------
    // Helper methods
    // -----------------------------------------------

    private fun buildMultipartFormData(request: SendFileUploadRequest): Map<String, Any> {
        val formData = mutableMapOf<String, Any>()
        formData["file"] = request.file
        request.part_number?.let { formData["part_number"] = it.toString() }
        request.mimeType?.let { formData["mimeType"] = it }
        return formData
    }
}