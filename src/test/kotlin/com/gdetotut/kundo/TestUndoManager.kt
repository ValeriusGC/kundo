package com.gdetotut.kundo

import com.gdetotut.kundo.aux.NonTrivialClass
import com.gdetotut.kundo.aux.UndoWatcher
import org.junit.Test

import org.junit.Assert.assertEquals

class TestUndoManager {


    /**
     * Illustration for versioning.
     */
    internal class NonTrivialClass_v2 : NonTrivialClass() {

        var title: String? = null

    }

    @Test
    @Throws(Exception::class)
    fun serialize() {

        val ntc = NonTrivialClass()
        val stack = UndoStack(ntc, null)
        stack.subscriber = UndoWatcher()
        for (i in 0..999) {
            stack.push(NonTrivialClass.AddCommand(NonTrivialClass.Item.Type.CIRCLE, ntc, null))
        }
        assertEquals(1000, ntc.items.size)
        assertEquals(1000, stack.count().toLong())
        for (i in 0..999) {
            stack.push(NonTrivialClass.MovedCommand(ntc.items.get(i), 10, null))
        }
        assertEquals(1000, ntc.items.size)
        assertEquals(2000, stack.count().toLong())
        for (i in 0..999) {
            stack.push(NonTrivialClass.DeleteCommand(ntc, null))
        }
        assertEquals(0, ntc.items.size)
        assertEquals(3000, stack.count().toLong())

        var managerBack: UndoManager? = null
        run {
            // Make unzipped serialization
            val manager = UndoManager("", 2, stack)
            val data = manager.serialize(manager, false)
            //            System.out.println("1: " + data.length());
            managerBack = manager.deserialize(data)
            // Here we can't compare managers themselves 'cause of stack's comparison principle it leads at last
            // ------- assertEquals(manager, managerBack);
            assertEquals(manager.ID, managerBack!!.ID)
            assertEquals(manager.VERSION.toLong(), managerBack!!.VERSION.toLong())
            assertEquals(manager.extras, managerBack!!.extras)
            assertEquals(manager.stack.subject, managerBack!!.stack.subject)
            //~
            assertEquals(NonTrivialClass::class.java, manager.stack.subject.javaClass)
        }
        run {
            // Make zipped serialization
            val manager = UndoManager("", 2, stack)
            val z_data = manager.serialize(manager, true)
            //            System.out.println("zipped length : " + z_data.length());
            managerBack = manager.deserialize(z_data)
            // Here we can't compare managers themselves 'cause of stack's comparison principle it leads at last
            // ------- assertEquals(manager, managerBack);
            assertEquals(manager.VERSION.toLong(), managerBack!!.VERSION.toLong())
            assertEquals(manager.extras, managerBack!!.extras)
            assertEquals(manager.stack.subject, managerBack!!.stack.subject)
            //~
            assertEquals(NonTrivialClass::class.java, manager.stack.subject.javaClass)
        }

        val stackBack = managerBack!!.stack
        val ntcBack = stackBack.subject as NonTrivialClass
        stackBack.subscriber = UndoWatcher()
        // Check out
        for (i in 0..999) {
            stackBack.undo()
        }
        assertEquals(1000, ntcBack.items.size)
        for (i in 0..999) {
            stackBack.undo()
        }
        assertEquals(1000, ntcBack.items.size)
        for (i in 0..999) {
            stackBack.undo()
        }
        assertEquals(0, ntcBack.items.size)

        //============================================================================
        // Illustrate versioning
        assertEquals(NonTrivialClass::class.java, ntcBack.javaClass)
        val v2 = NonTrivialClass_v2()
        stackBack.setIndex(stackBack.count())
        v2.items.addAll(ntcBack.items)
        assertEquals(v2.items, ntcBack.items)

    }

}
