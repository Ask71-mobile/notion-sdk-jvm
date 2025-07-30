package notion.api.v1.request.files

import java.io.InputStream

data class SendFileUploadRequest
@JvmOverloads
constructor(
    val file: InputStream,
    val part_number: Int? = null,
    val mimeType: String? = null,
)