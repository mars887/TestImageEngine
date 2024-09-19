import java.awt.image.BufferedImage
import java.net.URL
import java.nio.file.Path
import java.nio.file.Paths
import java.util.ArrayList
import javax.imageio.ImageIO

class ImageProcess {

    private val imageLoaded: BufferedImage?
    private val imagePath: Path?
    private val imageURL: URL?
    private val callback: OnFinish
    var requireThumbnail: Boolean = false
    var thumbnailForProcessLink: URL? = null

    constructor(image: BufferedImage, callback: OnFinish) {
        imageLoaded = image
        imagePath = null
        imageURL = null
        this.callback = callback
    }

    constructor(image: String, callback: OnFinish) {
        imageLoaded = null
        imagePath = Paths.get(image)
        imageURL = null
        this.callback = callback
    }

    constructor(image: Path, callback: OnFinish) {
        imageLoaded = null
        imagePath = image
        imageURL = null
        this.callback = callback
    }

    constructor(image: URL, callback: OnFinish) {
        imageLoaded = null
        imagePath = null
        imageURL = image
        this.callback = callback
    }

    fun getImage(): BufferedImage? {
        if (imageLoaded != null) return imageLoaded
        if (imagePath != null) return ImageIO.read(imagePath.toFile())
        if (imageURL != null) return ImageIO.read(imageURL)
        return null
    }

    fun getThumbnail(): BufferedImage? {
        return if (thumbnailForProcessLink != null) ImageIO.read(thumbnailForProcessLink) else null
    }

    fun finish(
        colorMap: Array<Array<IntArray>>,
        hash: ArrayList<Int>,
        width: Int,
        height: Int,
        loadedImage: BufferedImage,
        thumbnail: BufferedImage?
    ) {
        val data = ImageData(
            IDGenerator.generateId(),
            imagePath?.toString(),
            imageURL?.toString(),
            colorMap,
            width,
            height,
            hash,
            loadedImage = imageLoaded,
            thumbnail = thumbnail
        )
        callback.invoke(data, loadedImage)
    }

    fun interface OnFinish {
        fun invoke(data: ImageData, image: BufferedImage)
    }
}