package com.shadowtesseract.politests

import junit.framework.Assert.assertEquals
import org.junit.Test
import java.util.*

class SynchronizationTest {
    class Counter {
        private var i = 0

        @Synchronized
        fun start() = i++

        @Synchronized
        fun end() = i--

        fun getI() = i
    }

    class Worker : Runnable {
        companion object {
            val counter = Counter()
            val rand = Random(System.currentTimeMillis())
        }
        override fun run() {
            counter.start()
            Thread.sleep(5 * rand.nextInt(10).toLong())
            counter.end()
        }
    }

    @Test
    fun counterTest() {
        val list = mutableListOf<Thread>()
        for (i in 0 until 100) {
            list.add(Thread(Worker()))
            list[i].start()
            list[i].join()
        }

        assertEquals(0, Worker.counter.getI())

    }
}