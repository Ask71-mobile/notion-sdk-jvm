package notion.api.v1.model.common

import com.google.gson.annotations.SerializedName

enum class FileType @JvmOverloads constructor(val value: String) {
  @SerializedName("external") External("external"),
  @SerializedName("file") File("file"),
  @SerializedName("file_upload") FileUpload("file_upload");

  override fun toString(): String = value
}
