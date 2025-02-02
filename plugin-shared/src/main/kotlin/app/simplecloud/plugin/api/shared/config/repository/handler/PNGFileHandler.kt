package app.simplecloud.plugin.api.shared.config.repository.handler

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class PNGFileHandler : FileHandler<BufferedImage> {

    override val fileExtension: String = ".png"

    override fun load(file: File): BufferedImage? =
        runCatching { ImageIO.read(file) }.getOrNull()


    override fun save(file: File, entity: BufferedImage) {
        ImageIO.write(entity, "png", file)
    }

    override fun validate(entity: BufferedImage): Boolean =
        entity.width > 0 && entity.height > 0

}