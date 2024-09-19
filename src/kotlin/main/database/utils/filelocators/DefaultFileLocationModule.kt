package main.database.utils.filelocators

import ImageData
import main.database.toPath
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import javax.imageio.ImageIO

public class DefaultFileLocationModule(
    originalFilesFolder: Path,
    normalFilesFolder: Path,
    sameFilesFolder: Path,
    saveFromLinks: Boolean = true
) : FileLocationModule(originalFilesFolder, normalFilesFolder, sameFilesFolder, saveFromLinks) {

    override fun onSuccess(imageData: ImageData) {
        if (normalFilesMode == FileLocationMode.NOTHING) return
        if (imageData.filePath != null) {
            when (normalFilesMode) {
                FileLocationMode.MOVE -> imageData.filePath = Files.move(
                    imageData.filePath!!.toPath(),
                    (normalFilesFolder.toString() + "\\" + imageData.filePath!!.toPath().fileName).toPath(),
                    StandardCopyOption.ATOMIC_MOVE
                ).toString()

                FileLocationMode.COPY -> imageData.filePath = Files.copy(
                    imageData.filePath!!.toPath(),
                    (normalFilesFolder.toString() + "\\" + imageData.filePath!!.toPath().fileName).toPath()
                ).toString()

                else -> {}
            }
        } else if (imageData.link != null && saveFromLinks) {
            Files.copy(
                URL(imageData.link).openConnection().getInputStream(),
                "$normalFilesFolder\\${imageData.id}.jpg".toPath(),
                StandardCopyOption.REPLACE_EXISTING
            )
            imageData.filePath = "$normalFilesFolder\\${imageData.id}.jpg"

        } else if (imageData.loadedImage != null) {
            ImageIO.write(imageData.loadedImage, "jpg", "$normalFilesFolder\\${imageData.id}.jpg".toPath().toFile())
            imageData.filePath = "$normalFilesFolder\\${imageData.id}.jpg"
        }
    }

    override fun onReplace(newData: ImageData, oldData: ImageData?) {
        if (oldData != null && Files.exists(oldData.filePath!!.toPath())) onFailure(oldData)
        onSuccess(newData)
    }

    override fun onFailure(data: ImageData) {
        if (data.filePath == null && !saveCopiesFromLinks) return
        when (sameFilesMode) {
            FileLocationMode.MOVE -> {
                if (data.filePath == null && saveFromLinks) {
                    data.filePath = "$sameFilesFolder\\${data.id}.jpg"
                    Files.copy(
                        URL(data.link).openConnection().getInputStream(),
                        "$sameFilesFolder\\${data.id}.jpg".toPath()
                    )
                    return
                }
                if (data.filePath.toString() == originalFilesFolder.toString()) {
                    data.filePath = Files.copy(
                        data.filePath!!.toPath(),
                        "$sameFilesFolder\\${data.id}.jpg".toPath()
                    ).toString()
                } else {
                    data.filePath = Files.move(
                        data.filePath!!.toPath(),
                        "$sameFilesFolder\\${data.id}.jpg".toPath()
                    ).toString()
                }
            }

            FileLocationMode.COPY -> {
                if (data.filePath == null && saveFromLinks) {
                    data.filePath = "$sameFilesFolder\\${data.id}.jpg"
                    Files.copy(
                        URL(data.link).openConnection().getInputStream(),
                        "$sameFilesFolder\\${data.id}.jpg".toPath()
                    )
                    return
                }
                data.filePath = Files.copy(
                    data.filePath!!.toPath(),
                    "$sameFilesFolder\\${data.id}.jpg".toPath()
                ).toString()

            }


            FileLocationMode.NOTHING -> if (sameFilesMode != FileLocationMode.NOTHING)
                data.filePath?.toPath()?.let { Files.delete(it) }
        }
    }
}