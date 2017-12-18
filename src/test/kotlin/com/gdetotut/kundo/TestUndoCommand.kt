package com.gdetotut.kundo

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class TestUndoCommand {


    /**
     * Tests method [UndoCommand.id]
     */
    @Test
    fun id() {
        assertEquals(-1, UndoCommand("", null).id())
    }

    /**
     * Tests method [UndoCommand.mergeWith]
     */
    @Test
    fun mergeWith() {
        val cmd = UndoCommand("mergee", null)
        assertFalse(UndoCommand("", null).mergeWith(cmd))
    }

    /**
     * Tests method [UndoCommand.childCount]
     */
    @Test
    fun childCount() {
        val cmd = UndoCommand("", null)
        assertNotNull(cmd)
        assertEquals(0, cmd.childCount())

        val cmdChild = UndoCommand("sub", cmd)
        assertNotNull(cmdChild)

        assertEquals(1, cmd.childCount())
        assertEquals(0, cmdChild.childCount())
    }

    /**
     * Tests method [UndoCommand.child]
     */
    @Test
    fun child() {
        val cmd = UndoCommand("", null)
        assertNotNull(cmd)
        assertEquals(null, cmd.child(-1))
        assertEquals(null, cmd.child(0))
        assertEquals(null, cmd.child(1))

        val cmdChild = UndoCommand("sub", cmd)
        assertNotNull(cmdChild)

        assertEquals(cmdChild, cmd.child(0))
        assertEquals(null, cmdChild.child(-1))
        assertEquals(null, cmdChild.child(0))
        assertEquals(null, cmdChild.child(1))
    }

    @Test
    fun text() {
        run {
            val cmd = UndoCommand("", null)
            assertEquals("", cmd.text)
            cmd.text = "new"
            assertEquals("new", cmd.text)
        }
    }

}