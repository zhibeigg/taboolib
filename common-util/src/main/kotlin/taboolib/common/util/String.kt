package taboolib.common.util

/**
 * 替换字符串中的变量 {0}, {1}
 */
fun String.replaceWithOrder(vararg args: Any): String {
    if (args.isEmpty() || isEmpty()) {
        return this
    }
    val chars = toCharArray()
    val builder = StringBuilder(length)
    var i = 0
    while (i < chars.size) {
        val mark = i
        if (chars[i] == '{') {
            var num = 0
            val alias = StringBuilder()
            while (i + 1 < chars.size && chars[i + 1] != '}') {
                i++
                if (Character.isDigit(chars[i]) && alias.isEmpty()) {
                    num *= 10
                    num += chars[i] - '0'
                } else {
                    alias.append(chars[i])
                }
            }
            if (i != mark && i + 1 < chars.size && chars[i + 1] == '}') {
                i++
                if (alias.isNotEmpty()) {
                    val str = alias.toString()
                    builder.append((args.firstOrNull { it is Pair<*, *> && it.second == str } as? Pair<*, *>)?.first ?: "{$str}")
                } else {
                    builder.append(args.getOrNull(num) ?: "{$num}")
                }
            } else {
                i = mark
            }
        }
        if (mark == i) {
            builder.append(chars[i])
        }
        i++
    }
    return builder.toString()
}

/**
 * 解码 Unicode
 */
fun String.decodeUnicode(): String {
    val builder = StringBuilder()
    var i = 0
    while (i < length) {
        val c = this[i]
        if (c == '\\' && i + 1 < length && this[i + 1] == 'u') {
            val hex = substring(i + 2, i + 6)
            builder.append(hex.toInt(16).toChar())
            i += 6
        } else {
            builder.append(c)
            i++
        }
    }
    return builder.toString()
}