package io.github.project_kaat.gpsdrelay.network

fun isMessageAllowedByFilter(msg : OutgoingMessage, filter : List<String>) : Boolean {
    if (msg.isGenerated) { //generated messages always pass
        return true
    }
    if (filter.isEmpty()) { //filter is disabled
        return true
    }
    else if (msg.data.startsWith("\$P") || msg.data.length < 5) { //vendor specific and malformed messages are always dropped when filter is enabled
        return false
    }
    else {
        val sentence = msg.data.substring(startIndex = 3, endIndex = 6)

        return sentence in filter
    }
}