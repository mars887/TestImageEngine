package main.database.utils.filelocators

import ImageData
import java.nio.file.Path

public abstract class FileLocationModule(
    var originalFilesFolder: Path,
    var normalFilesFolder: Path,
    var sameFilesFolder: Path,
    var saveFromLinks: Boolean = true
) {
    var normalFilesMode: FileLocationMode = FileLocationMode.COPY
    var sameFilesMode: FileLocationMode = FileLocationMode.COPY
    var saveCopiesFromLinks: Boolean = false

    abstract fun onSuccess(imageData: ImageData)
    abstract fun onReplace(newData: ImageData, oldData: ImageData?)
    abstract fun onFailure(data: ImageData)
}

enum class FileLocationMode {
    MOVE,
    COPY,
    NOTHING
}