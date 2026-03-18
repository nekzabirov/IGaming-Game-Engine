package domain.util

interface Orderable {
    var order: Int

    fun changeOrder(order: Int) {
        this.order = order
    }
}
