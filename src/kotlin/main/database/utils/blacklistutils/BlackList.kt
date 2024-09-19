package main.database.utils.blacklistutils

import ImageData
import ImageToDataProcessor
import com.google.gson.Gson
import main.database.DBFromToJson
import main.database.DataBase
import main.database.getFormat
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists

class BlackList(val dataBase: DataBase, val dataGen: ImageToDataProcessor, val folder: Path) {
    val blacklist = HashSet<ImageData>()
    val jsonFilePath: Path = Paths.get("$folder\\saved.json")

    fun applyBlacklist() {
        println("initializing blacklist")
        var datas: List<ImageData> = listOf()
        val gson = Gson()

        if (jsonFilePath.exists()) {

            val json = Files.newBufferedReader(jsonFilePath).lines().toList()
                .joinToString(separator = "\n")
            datas = gson.fromJson(json, Array<DBFromToJson.JsonImageData>::class.java)
                .map { DBFromToJson.JIDToImageData(it) }
        }
        blacklist.addAll(datas)
        println("saved list loaded - ${datas.size}")

        var isUpdated = false
        var ind = 0
        val savedPaths = datas.map { it.filePath }.filterNotNull().toHashSet()

        Files.list(folder).filter {
            val f = it.fileName.getFormat()
            return@filter (f == "png" || f == "jpg" || f == "jpeg") && !savedPaths.contains(it.toString())
        }.forEach {
            dataGen.addProcess(it, { data, _ ->
                data.thumbnail = null
                data.loadedImage = null
                blacklist.add(data)
                println("added to blacklist ${data.filePath}")
                isUpdated = true
            })
        }
        if (isUpdated) {
            println("adding new files")
            dataGen.join(3000)

            val json = gson.toJson(blacklist.map { DBFromToJson.imageDataToJID(it) }.toList())
            Files.newBufferedWriter(jsonFilePath).use {
                it.write(json)
                it.flush()
            }
        }
        println("blacklist initialized - ${blacklist.size} elements")
    }
}