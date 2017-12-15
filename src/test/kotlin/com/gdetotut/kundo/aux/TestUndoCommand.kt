package com.gdetotut.kundo.aux

import com.gdetotut.kundo.UndoCommand
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TestUndoCommand {

    @Test
    fun childCount() {
        val cmd = UndoCommand("", null)
        assertNotNull(cmd)
        assertEquals(0, cmd.childCount())

        val cmdChild = UndoCommand("sub", cmd)
        assertNotNull(cmdChild)

        assertEquals(1, cmd.childCount())
        assertEquals(cmdChild, cmd.child(0))
        assertEquals(0, cmdChild.childCount())

    }
}