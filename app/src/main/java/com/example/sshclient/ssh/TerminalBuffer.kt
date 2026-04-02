package com.example.sshclient.ssh

class TerminalBuffer(
    private val maxLines: Int = 3000
) {
    private val lines = ArrayDeque<String>(maxLines)
    private var currentLine = StringBuilder()

    @Synchronized
    fun append(raw: String): List<String> {
        for (ch in raw) {
            when (ch) {
                '\n' -> {
                    lines.addLast(currentLine.toString())
                    if (lines.size > maxLines) {
                        lines.removeFirst()
                    }
                    currentLine = StringBuilder()
                }
                '\r' -> Unit
                else -> currentLine.append(ch)
            }
        }
        return snapshot()
    }

    @Synchronized
    fun snapshot(): List<String> {
        val result = ArrayList<String>(lines.size + 1)
        result.addAll(lines)
        if (currentLine.isNotEmpty()) {
            result.add(currentLine.toString())
        }
        return result
    }

    @Synchronized
    fun clear() {
        lines.clear()
        currentLine = StringBuilder()
    }
}
