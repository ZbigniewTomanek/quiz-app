package com.shadowtesseract.politests

import com.shadowtesseract.politests.database.engine.Tools
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class UnitTests {

    @Test
    fun photoNameTest() {
        assertEquals("25D.jpg", Tools.getPhotoNameFromAnswer("[img]025D.png[/img]"))
        assertEquals("25d.jpg", Tools.getPhotoNameFromAnswer("[img]025d.jpg[/img]"))
        assertEquals("6A.jpg", Tools.getPhotoNameFromAnswer("[img]006A.jpg[/img]"))
        assertEquals("H12B.jpg", Tools.getPhotoNameFromAnswer("[img]H12B.png[/img]"))
        assertEquals("Czas życia tego atomu wynosi[img]", Tools.getPhotoNameFromAnswer("Czas życia tego atomu wynosi[img]"))
    }

}
