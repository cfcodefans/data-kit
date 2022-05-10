package kotlin.tests

import org.junit.Test

open class YieldTests {
    @Test
    fun testYield1() {
        val seq: Sequence<Int> = sequence {
            var i = 0
            while (i < 10) {
                yield(i)
                i += 1
            }
        }

        println(seq.toList())
    }
}