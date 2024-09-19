package main.database.utils.generators

import java.awt.Color
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentLinkedDeque

class ColorMapToImageGenerator {

    private val executors = Array(4) {
        CMTGThread()
    }

    fun addProcess(colorMap: Array<Array<IntArray>>, onFinish: OnFinish) {
        var minId = 0
        var minCount = 100000000

        executors.forEachIndexed { i, it ->
            val count = it.getProcessesCount()
            if (count < minCount) {
                minId = i
                minCount = count
            }
        }
        executors[minId].addProcess(colorMap, onFinish)
    }

    fun interrupt() {
        executors.forEach {
            it.interrupt()
        }
    }

    private class CMTGThread : Thread() {
        private val currs = ConcurrentLinkedDeque<Process>()

        init {
            start()
        }

        fun getProcessesCount() = currs.size

        fun addProcess(colorMap: Array<Array<IntArray>>, onFinish: OnFinish) {
            synchronized(currs) {
                currs.add(
                    Process(colorMap, onFinish)
                )
            }
        }

        override fun run() {
            super.run()
            while (!isInterrupted) {
                val process = currs.poll()
                if (process != null) {

                    val map = process.colorMap

                    val image = BufferedImage(map.size, map[0].size, BufferedImage.TYPE_INT_RGB)

                    for (x in map.indices) {
                        for (y in map[0].indices) {
                            image.setRGB(x,y, Color(map[x][y][0], map[x][y][1], map[x][y][2]).rgb)
                        }
                    }

                    process.onFinish.invoke(image)

                } else sleep(100)
            }
        }
    }

    fun interface OnFinish {
        fun invoke(image: BufferedImage)
    }

    private data class Process(val colorMap: Array<Array<IntArray>>, val onFinish: OnFinish)
}