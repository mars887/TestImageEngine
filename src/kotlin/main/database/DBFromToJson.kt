package main.database

import com.google.gson.Gson
import IDGenerator
import ImageData
import java.lang.StringBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.isDirectory

class DBFromToJson {
    companion object {
        fun saveToJson(database: ConcurrentHashMap<Long, ImageData>, path: Path) {
            val writer = if (path.isDirectory()) {
                Files.newBufferedWriter(Paths.get("$path/database.json"))
            } else {
                Files.newBufferedWriter(path)
            }

            val array = database.elements().toList().map { imageDataToJID(it) }.toTypedArray()

            writer.write(Gson().toJson(array))
            writer.flush()
            writer.close()
        }

        fun JsonPutToDatabase(path: Path, base: DataBase) {
            if(!Files.exists(path)) return
            val json = Files.newBufferedReader(path).lines().toList().joinToString(separator = "\n")
            val datas = Gson().fromJson(json, Array<JsonImageData>::class.java)

            datas.forEach {
                base.database.put(it.longKey, JIDToImageData(it))
                IDGenerator.putUsedId(it.longKey)
            }
        }


        fun imageDataToJID(data: ImageData): JsonImageData {
            return JsonImageData(
                data.id,
                data.filePath ?: "",
                data.link ?: "",
                colorMapArrayToString(data.colorMap),
                data.width,
                data.height,
                data.hash.joinToString(separator = ":")
            )
        }

        fun JIDToImageData(json: JsonImageData): ImageData {
            val hashList = ArrayList<Int>()
            json.hash.split(":").toList().forEach {
                hashList.add(it.toInt())
            }
            return ImageData(
                json.longKey,
                json.filePath,
                json.link,
                stringToColorMapArray(json.colorMap),
                json.width,
                json.height,
                hashList
            )
        }

        private fun colorMapArrayToString(colorMap: Array<Array<IntArray>>): String {
            val out = StringBuilder()

            colorMap.forEach { array1 ->
                array1.forEach {
                    out.append(it.joinToString(separator = ":"))
                    out.append(";")
                }
                out.append("|")
            }
            return out.toString()
        }

        private fun stringToColorMapArray(inp: String): Array<Array<IntArray>> {
            val colorMap = mutableListOf<Array<IntArray>>()

            inp.split("|").forEach {
                val line = it.split(";").filter { it.isNotBlank() }.map { arrayToIntArray(it.split(":")) }
                colorMap.add(line.toTypedArray())
            }
            return colorMap.toTypedArray()
        }

        private fun arrayToIntArray(array: List<String>): IntArray {
            return IntArray(array.size) {
                array[it].toInt()
            }
        }
    }

    data class JsonImageData(
        val longKey: Long,
        val filePath: String,
        val link: String,
        val colorMap: String,
        val width: Int,
        val height: Int,
        val hash: String,
    )
}