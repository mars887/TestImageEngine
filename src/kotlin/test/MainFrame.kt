package test

import java.awt.*
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.imageio.ImageIO

class MainFrame {

    val images: List<Path> =
        Files.list(Paths.get("C:\\Users\\славик\\Desktop\\MD arts\\Новая папка (2)")).toList().shuffled()

    init {
        run()
    }

    private fun run() {
        val fh = 250
        var cw = 0
        var ch = 0

        val out = BufferedImage(34000, 34000, BufferedImage.TYPE_INT_RGB)
        val g = out.graphics as Graphics2D

        val loader = Loader(images)

        val getImage = fun () : BufferedImage {
            return loader.getNext()
        }

        var i = 0

        while (true) {
            val img = getImage() ?: break
            val wiv = fh * img.width / img.height

            if (wiv + cw < out.width) {
                g.drawImage(img, cw, ch, wiv, fh, null)
                cw += wiv
                i++
            } else if (ch + fh * 2 <= out.height) {
                ch += fh
                cw = 0
                print("not ")
            } else break

            println("art $i finished __ ${ch / out.height.toFloat() * 100}")
        }

        loader.destroy()
        ImageIO.write(out, "jpeg", File("test1.jpeg"))
    }
}