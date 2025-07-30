package notion.api.v1.request.files

data class CreateFileUploadRequest
@JvmOverloads
constructor(
    val filename: String,
    val file_size: Long,
    val mime_type: String? = null,
    val part_size: Long? = null,
)