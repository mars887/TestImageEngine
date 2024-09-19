package main.database

import ImageData
import ImageToDataProcessor
import main.database.utils.blacklistutils.BlackList
import main.database.utils.filelocators.FileLocationModule
import main.database.utils.filters.*
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

class DataBase(var fileLocationModule: FileLocationModule, val dataGen: ImageToDataProcessor) {
    val database = ConcurrentHashMap<Long, ImageData>()
    var blacklist: BlackList? = null
    private val defFileLocationModule = fileLocationModule
    private val defImageHashFilter = SameHashFilter(200)
    private val defBlacklistFilter = DefBlackListFilter()

    // ------------------------------------------------------------------------------------------------

    fun initBlacklist(folder: Path) {
        blacklist = BlackList(this, dataGen, folder)
        blacklist!!.applyBlacklist()
    }

    fun getById(id: Long): ImageData? {
        return database[id]
    }

    fun findByPath(path: String): ImageData? {
        return findByPath(Paths.get(path))
    }

    fun findByPath(path: Path): ImageData? {
        return database.values.firstOrNull {
            it.filePath == path.toString()
        }
    }

    fun findByLink(link: String): ImageData? {
        return database.values.firstOrNull {
            it.link == link
        }
    }

    fun getAll(): MutableCollection<ImageData> {
        return database.values
    }

    fun addImageData(
        data: ImageData,
        callback: AddCallback,
        sameImageDataFilter: ImageDataFilter = defImageHashFilter,
        sameImageFindedResolver: SameImageFindedResolver = DefaultSameImageFindedResolver,
        blacklistFilter: BlacklistFilter = defBlacklistFilter,
        fileLocationModule: FileLocationModule = defFileLocationModule
    ) {

        val result = sameImageDataFilter.filter(data, database.values)
        if (blacklist != null && blacklistFilter.filter(data, blacklist!!.blacklist)) {
            callback.invoke(AddingResult.ON_BLACKLIST, "image found in black list and not added to database")
            return
        }

        when (result) {
            is NormalFilterCallback -> {
                database.put(data.id, data)
                callback.invoke(AddingResult.SUCCESS, "image successfully added to database")
                fileLocationModule.onSuccess(data)
                return
            }

            is SameImageFindedCallBack -> {
                when (sameImageFindedResolver.resolve(result.imgData, database[result.sameImgID]!!)) {
                    SameImageFindedResolverResult.NOT_ADD -> {
                        IDGenerator.removeIDFromUsed(result.sameImgID)
                        fileLocationModule.onFailure(data)
                        callback.invoke(
                            AddingResult.SAME_IMAGE_FINDED,
                            "same image found with path: ${database[result.sameImgID]?.filePath} , not added"
                        )
                    }

                    SameImageFindedResolverResult.REPLACE -> {
                        database.put(data.id, data)
                        fileLocationModule.onReplace(data, database[result.sameImgID])
                        callback.invoke(
                            AddingResult.REPLACED,
                            "same image found and ${database[result.sameImgID]?.filePath} replaced to ${data.filePath}"
                        )
                        database.remove(result.sameImgID)
                        IDGenerator.removeIDFromUsed(result.sameImgID)
                    }

                    SameImageFindedResolverResult.ADD -> {
                        database.put(data.id, data)
                        fileLocationModule.onSuccess(data)
                        callback.invoke(
                            AddingResult.SUCCESS,
                            "same image found with path: ${database[result.sameImgID]?.filePath} , added"
                        )
                    }
                }
            }

            else -> {}
        }

    }

    fun interface AddCallback {
        fun invoke(reason: AddingResult, description: String)
    }

    enum class AddingResult {
        SUCCESS,
        REPLACED,
        SAME_IMAGE_FINDED,
        ON_BLACKLIST
    }

    interface SameImageFindedResolver {
        fun resolve(newData: ImageData, currentData: ImageData): SameImageFindedResolverResult
    }

    enum class SameImageFindedResolverResult {
        NOT_ADD,
        REPLACE,
        ADD
    }

    object DefaultSameImageFindedResolver : SameImageFindedResolver {
        override fun resolve(newData: ImageData, currentData: ImageData): SameImageFindedResolverResult {
            return if ((newData.width * newData.height) / (currentData.width * currentData.height) > 1.1) {
                SameImageFindedResolverResult.REPLACE
            } else SameImageFindedResolverResult.NOT_ADD
        }
    }
}

fun Path.getFormat(): String {
    return this.toString().takeLastWhile { it != '.' }
}

fun String.toPath(): Path {
    return Paths.get(this)
}
