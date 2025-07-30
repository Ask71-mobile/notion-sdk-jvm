package notion.api.v1.model.files

import com.google.gson.annotations.SerializedName

enum class FileUploadStatus {
    @SerializedName("pending") Pending,
    @SerializedName("uploaded") Uploaded,
    @SerializedName("archived") Archived,
    @SerializedName("failed") Failed,
}