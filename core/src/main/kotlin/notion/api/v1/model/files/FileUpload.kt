package notion.api.v1.model.files

import com.google.gson.annotations.SerializedName
import notion.api.v1.model.common.ObjectType

data class FileUpload(
    @SerializedName("object") val objectType: ObjectType = ObjectType.FileUpload,
    val id: String,
    val status: FileUploadStatus,
    val filename: String,
    val file_size: Long,
    val mime_type: String?,
    @SerializedName("expiry_time") val expires_at: String,
    val upload_url: String?,
    val multipart_upload: MultipartUpload?,
) {
    data class MultipartUpload(
        val upload_id: String,
        val part_size: Long,
        val number_of_parts: Int,
        val upload_urls: Map<String, String>,
    )
}