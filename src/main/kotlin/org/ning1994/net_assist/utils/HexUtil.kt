package org.ning1994.net_assist.utils

object HexUtil {
    /**
     * 字节流转成十六进制表示
     */
    fun encode(src: ByteArray): String? {
        var strHex: String
        val sb = StringBuilder("")
        for (n in src.indices) {
            strHex = Integer.toHexString(src[n].toInt() and 0xFF)
            sb.append(if (strHex.length == 1) "0$strHex" else strHex) // 每个字节由两个字符表示，位数不够，高位补0
        }
        return sb.toString().trim { it <= ' ' }
    }

    /**
     * 字符串转成字节流
     */
    fun decode(src: String): ByteArray? {
        var m: Int
        var n: Int
        val byteLen = src.length / 2 // 每两个字符描述一个字节
        val ret = ByteArray(byteLen)
        for (i in 0 until byteLen) {
            m = i * 2 + 1
            n = m + 1
            val intVal = Integer.decode("0x" + src.substring(i * 2, m) + src.substring(m, n))
            ret[i] = java.lang.Byte.valueOf(intVal.toByte())
        }
        return ret
    }
}