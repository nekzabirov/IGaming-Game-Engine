package domain.util

import domain.vo.ImageMap

interface Imageable {
    var images: ImageMap

    fun updateImage(key: String, value: String) {
        images[key] = value
    }

    fun removeImage(key: String) {
        images.remove(key)
    }
}
