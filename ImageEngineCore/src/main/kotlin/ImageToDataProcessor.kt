import imagescaling.ResampleFilter
import imagescaling.ResampleFilters
import java.awt.image.BufferedImage
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths

class ImageToDataProcessor(
    private val threads: Int,
    val colorHashedDepth: Int = 256,
    val resampleFilter: ResampleFilter = ResampleFilters.getLanczos3Filter(),
    val LQHashSize: Int = 32,
    val ULQHashSize: Int = 16
) {

    private val processors: Array<ImageProcessor> = Array(threads) {
        ImageProcessor(this, LQHashSize, ULQHashSize)
    }

    fun getProcessesCount(): Int {
        var c = 0
        processors.forEach {
            c += it.processesCount
        }
        return c
    }

    fun addProcess(image: BufferedImage, callback: ImageProcess.OnFinish, requireThumnail: Boolean = false) {
        val process = ImageProcess(image, callback)
        process.requireThumbnail = requireThumnail
        initProcess(process)
    }

    fun addProcess(image: Path, callback: ImageProcess.OnFinish, requireThumnail: Boolean = false) {
        val process = ImageProcess(image, callback)
        process.requireThumbnail = requireThumnail
        initProcess(process)
    }

    fun addProcess(image: String, callback: ImageProcess.OnFinish, requireThumnail: Boolean = false) {
        addProcess(Paths.get(image), callback)
    }

    fun addProcessByLink(link: String, callback: ImageProcess.OnFinish, requireThumnail: Boolean = false) {
        initProcess(
            ImageProcess(URL(link), callback)
                .also { it.requireThumbnail = requireThumnail }
        )
    }

    fun addNativeImageProcess(process: ImageProcess) = initProcess(process)

    private fun initProcess(process: ImageProcess) {
        var minId = 0
        var minCount = 100000000

        processors.forEachIndexed { i, it ->
            val count = it.processesCount
            if (count < minCount) {
                minId = i
                minCount = count
            }
        }
        processors[minId].addImageProcess(process)
    }

    fun getDefaultThumbnailMapSide() = defaultThumbnailMapSide

    fun join(timer: Int) {
        var last = System.currentTimeMillis()
        while (true) {
            var summ = 0
            processors.forEach {
                summ += it.processesCount
            }
            if (summ == 0) {
                if (System.currentTimeMillis() - last > timer) {
                    break
                }
            } else last = System.currentTimeMillis()
            Thread.sleep(100)

        }
    }

    companion object {
        const val defaultThumbnailMapSide = 600
    }

}