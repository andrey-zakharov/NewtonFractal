interface Deque<E> {

    fun addFirst(e: E)
    fun addLast(e: E)
    //fun offerLast(e: E): Boolean
    //fun removeFirst()
    //fun pollFirst() // Retrieves and removes the first element of this deque, or returns null if this deque is empty.
    /**
     * Retrieves, but does not remove, the first element of this deque.
     */
    fun first(): E
    fun last(): E

    /**
     * Retrieves and removes the first element of this deque.
     */
    fun removeFirst(): E
    fun removeLast(): E
}

class CircularFifoQueue<E>(val size: Int): Deque<E> {
    private val array = ArrayList<E>(size)
    private var current = 0 //insert mark
    val lastInserted: Int
        get() = if ( array.size < size ) array.size - 1 else (current - 1).mod(size)
    val firstInserted: Int
        get() = if ( array.size < size ) 0 else current.mod(size)

    fun print() = """${array.joinToString(", ")}\n$current"""
    private fun add(element: E): Boolean {
        if ( array.size < size ) {
            array.add(element)
        } else {
            array[current] = element
        }
        current = (current + 1).mod(size)
        return true
    }

    override fun addFirst(e: E) = add(e).let { }
    override fun addLast(e: E) = add(e).let { }
    override fun first() = array[firstInserted]
    override fun last() = array[lastInserted]
    override fun removeFirst(): E {
        TODO("Not yet implemented")
    }

    override fun removeLast(): E {
        TODO("Not yet implemented")
    }
}