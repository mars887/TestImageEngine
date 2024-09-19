import java.awt.image.BufferedImage
import java.util.ArrayList

class ImageData(
    val id: Long,
    var filePath: String?,
    val link: String?,
    val colorMap: Array<Array<IntArray>>,
    val width: Int,
    val height: Int,
    val hash: ArrayList<Int>,
    var loadedImage: BufferedImage? = null,
    var thumbnail: BufferedImage? = null
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ImageData

        return (id == other.id || filePath == other.filePath || link == other.link)
    }

    override fun hashCode(): Int {
        return id.hashCode()
    }
}