package test

import java.awt.image.BufferedImage
import java.nio.file.Path
import java.util.concurrent.ConcurrentLinkedDeque
import javax.imageio.ImageIO

class Loader(val paths: List<Path>) {

    private val queue = ConcurrentLinkedDeque<BufferedImage>()

    private val units: ArrayList<LUnit> = arrayListOf()

    init {
        val count = 6

        val callback: Loaded = object : Loaded {
            override fun put(index: Int, img: BufferedImage) {
                queue.add(img)
            }
        }

        repeat(count) { uid ->
            units.add(
                LUnit(uid, paths, count, uid, callback)
            )
        }
    }

    fun destroy() {
        units.forEach {
            it.interrupt()
        }
    }

    fun getNext(): BufferedImage {
        while (queue.isEmpty()) Thread.sleep(10)
        return queue.pop()
    }

    @FunctionalInterface
    interface Loaded {
        fun put(index: Int, img: BufferedImage)
    }
}

class LUnit(val uid: Int, val images: List<Path>, val step: Int, offset: Int, val callback: Loader.Loaded) : Thread() {

    private var curr = offset
    private var total = 0

    init {
        start()
    }

    override fun run() {
        super.run()
        while (curr < images.size && !interrupted()) {
            var img: BufferedImage?

            while (true) {
                img = ImageIO.read(images[curr].toFile())
                if (img == null) {
                    println("$uid - $curr not loaded but null")
                    curr += step
                } else {
                    println("$uid - $curr loaded")
                    curr += step
                    break
                }
            }
            callback.put(curr, img!!)
            total++
        }
        println("on $uid total loaded $total")
    }
}