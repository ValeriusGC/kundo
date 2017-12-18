package com.gdetotut.kundo

import com.gdetotut.kundo.aux.SimpleClass
import org.junit.Test

import org.junit.Assert.assertEquals

class TestUndoGroup {

    /**
     * Group should not contain two or more stacks with one object.
     */
    @Test
    fun oneGroupOneObject() {

        val subj = SimpleClass(Int::class.java)
        val group = UndoGroup()
        val stackA = UndoStack(subj, group)
        assertEquals(1, group.getStacks().size.toLong())
        val stackB = UndoStack(subj, group)
        assertEquals(1, group.getStacks().size.toLong())

        val subj2 = SimpleClass(Int::class.java)
        val stackC = UndoStack(subj2, group)
        group.add(stackC)
        assertEquals(2, group.getStacks().size.toLong())


    }

}
