package domain.util

interface Activatable {
    var active: Boolean

    fun activate() {
        active = true
    }

    fun deactivate() {
        active = false
    }
}
