package main.database.utils.blacklistutils

import com.google.gson.Gson
import main.database.DBFromToJson
import ImageData
import main.database.utils.filters.SameHashFilter
import imagescaling.ResampleOp
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO

class BlackListUtils {
    companion object {
        fun optimizeBlackList(blackList: BlackList) {
            optimizeBlackListImages(blackList)
            println("optimizeBlackListImages finished")
            optimizeSameImages(blackList)
            println("optimizeSameImages finished")
            saveList(blackList)
            println("saveList finished")
        }

        private fun saveList(blacklist: BlackList) {
            val json = Gson().toJson(blacklist.blacklist.map { DBFromToJson.imageDataToJID(it) }.toList())
            Files.newBufferedWriter(blacklist.jsonFilePath).use {
                it.write(json)
                it.flush()
            }
        }

        fun reInitializeBlackList(blackList: BlackList) {
            blackList.blacklist.clear()
            Files.delete(blackList.jsonFilePath)
            blackList.applyBlacklist()
        }

        fun optimizeBlackListImages(blackList: BlackList) {
            val lists = List(8) {
                mutableListOf<Path>()
            }

            Files.list(blackList.folder).toList().forEachIndexed { index, it ->
                lists[index % lists.size].add(it)
            }
            val threads = mutableListOf<MultiThreadedImageOptimizer>()
            lists.forEach {
                threads.add(MultiThreadedImageOptimizer(it))
            }
            while (threads.any { it.isAlive }) {
                Thread.sleep(100)
            }
        }

        fun optimizeSameImages(blackList: BlackList) {
            val filter = SameHashFilter(300)
            val list = HashSet<ImageData>()
            val dataGen = blackList.dataGen

            Files.list(blackList.folder).forEach {
                dataGen.addProcess(it, { data, image ->
                    data.loadedImage = null
                    data.thumbnail = null

                    synchronized(list) {
                        if (filter.fastFilter(data, list)) {
                            list.add(data)
                        } else {
                            Files.delete(it)
                            println("${list.size} same deleted - $it")
                        }
                    }
                })
            }
            dataGen.join(2000)
            blackList.blacklist.clear()
            blackList.blacklist.addAll(list)
        }
    }

    private class MultiThreadedImageOptimizer(val list: List<Path>) : Thread() {
        init {
            start()
        }

        override fun run() {
            super.run()
            val resizeTo128 = ResampleOp(128, 128)

            list.forEach {
                var orig = ImageIO.read(it.toFile())
                if (orig == null) {
                    orig = ImageIO.read(it.toFile())
                    if (orig == null) Files.delete(it)
                }
                if (orig != null) {
                    var optimized: BufferedImage? = null
                    var reWrite = false

                    if (orig.width != orig.height) {
                        val width: Int = orig.width
                        val height: Int = orig.height
                        val x: Int
                        val y: Int
                        val w: Int
                        val h: Int
                        if (width > height) {
                            y = 0
                            h = height
                            x = (width - height) / 2
                            w = height
                        } else {
                            x = 0
                            w = width
                            y = (height - width) / 2
                            h = width
                        }
                        orig = orig.getSubimage(x, y, w, h)
                        reWrite = true
                    }

                    if (orig.width > 192 || orig.height > 192) {
                        optimized = resizeTo128.filter(orig, null)
                        reWrite = true
                    }

                    if (reWrite) {
                        Files.delete(it)
                        val outi = optimized ?: orig
                        if (outi.colorModel.hasAlpha()) {
                            val newPath = File(it.toString().dropLastWhile { it != '.' } + "png")
                            ImageIO.write(optimized ?: orig, "png", newPath)
                        } else {
                            val newPath = File(it.toString().dropLastWhile { it != '.' } + "jpg")
                            ImageIO.write(optimized ?: orig, "jpg", newPath)
                        }
                        println("optimized image - $it")
                    }
                }
            }
        }
    }
}