package main

import ImageToDataProcessor
import main.database.DBFromToJson
import main.database.DataBase
import main.database.utils.blacklistutils.BlackList
import main.database.utils.blacklistutils.BlackListUtils
import main.database.utils.filelocators.DefaultFileLocationModule
import main.database.utils.filelocators.FileLocationMode
import main.database.utils.generators.ColorMapToImageGenerator
import java.nio.file.Files
import java.nio.file.Paths

class App(private val prop: Properties) {


    private val origPath =
        Paths.get("C:\\Users\\славик\\Desktop\\alldrones")
    private val normPath = Paths.get("C:\\Users\\славик\\Desktop\\origin")
    private val copyPath = Paths.get("C:\\Users\\славик\\Desktop\\copy")

    private val dataGen = ImageToDataProcessor(
        12,
        LQHashSize = 24,
        ULQHashSize = 16
    )

    private val base = DataBase(
        DefaultFileLocationModule(
            origPath,
            normPath,
            copyPath
        ).apply {
            normalFilesMode = FileLocationMode.MOVE
            sameFilesMode = FileLocationMode.MOVE
            saveFromLinks = false
            saveCopiesFromLinks = false
        },
        dataGen
    )
    //val cmgen = ColorMapToImageGenerator()

    init {
        run()
    }

    private fun run() {
        val savedPath = Paths.get("C:\\Users\\славик\\Desktop\\saved.json")

        base.blacklist = BlackList(base, dataGen, Paths.get("C:\\Users\\славик\\Desktop\\blacklist"))
        BlackListUtils.optimizeBlackList(base.blacklist!!)
        //base.blacklist!!.applyBlacklist()

        try {
            DBFromToJson.JsonPutToDatabase(savedPath, base)
        } catch (_: Exception) {

        }


        Files.list(origPath).forEach {
            dataGen.addProcess(it, { data, img ->
                base.addImageData(data, { reason, description ->
                    println("${data.filePath ?: data.link} \n $description")
                    data.loadedImage = null
                })
            })
        }

//        FromPinterestLoader.loadFromJsonPinData(
//            Paths.get("C:\\Users\\славик\\Desktop\\loaderTest\\testpinsjson.json"),
//            dataGen,
//            base
//        )

        dataGen.join(2000)

        DBFromToJson.saveToJson(base.database, savedPath)
        println("end")
    }
}