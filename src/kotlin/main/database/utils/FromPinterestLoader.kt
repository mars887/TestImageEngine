package main.database.utils

import ImageProcess
import ImageToDataProcessor
import com.google.gson.Gson
import main.database.DataBase
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path

class FromPinterestLoader {
    companion object {
        fun loadFromJsonPinData(path: Path, processor: ImageToDataProcessor, db: DataBase) {
            val gson = Gson()
            val loadedJson = Files.newBufferedReader(path)
                .lines().toList()
                .joinToString(separator = "\n")
            val pins = gson.fromJson(loadedJson, Array<PinResult>::class.java)

            pins.forEach {
                val orig = it.thumbs.last()
                val thumbnail = it.thumbs.firstOrNull { it.height > 200 && it.width > 200 }

                processor.addNativeImageProcess(
                    ImageProcess(URL(orig.url)) { data, image ->

                        db.addImageData(data, { reason, description ->
                            println("${data.filePath ?: data.link} \n $description")
                            data.loadedImage = null
                            data.thumbnail = null
                        })

                    }.also {
                        it.thumbnailForProcessLink =
                            if (thumbnail != null) URL(thumbnail.url) else null
                    }
                )
            }
        }
    }

    private data class PinResult(
        val origin: String,
        val title: String,
        val thumbs: List<ThumbImg>
    )

    private data class ThumbImg(
        val id: Int,
        val url: String,
        val width: Int,
        val height: Int
    )
}