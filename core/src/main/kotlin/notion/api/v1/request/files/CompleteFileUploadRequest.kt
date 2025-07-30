package notion.api.v1.request.files

data class CompleteFileUploadRequest
@JvmOverloads
constructor(
    val parts: List<FileUploadPart>,
) {
    data class FileUploadPart(
        val part_number: Int,
        val etag: String,
    )
}