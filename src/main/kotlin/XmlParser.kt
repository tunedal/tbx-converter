interface XmlParser {
    sealed class Event {
        data class TagStart(
            val name: String,
            val attributes: Map<String, String> = emptyMap()
        ) : Event()
        data class TagEnd(val name: String) : Event()
        data class Text(val contents: String) : Event()
        object DocumentEnd : Event()
        data class Error(val exception: Exception) : Event()
    }

    fun parse(xml: String): Sequence<Event>
}
