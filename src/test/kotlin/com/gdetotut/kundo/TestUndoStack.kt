package com.gdetotut.kundo

import com.gdetotut.kundo.aux.NonTrivialClass
import com.gdetotut.kundo.aux.NonTrivialClass.Item.Type.*
import com.gdetotut.kundo.aux.Point
import com.gdetotut.kundo.aux.SimpleClass
import com.gdetotut.kundo.aux.UndoWatcher
import org.junit.Test

import java.io.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals

class TestUndoStack {

    internal lateinit var stack: UndoStack
    internal lateinit var arr: Array<Any>
    internal lateinit var subj: Serializable

    @Throws(Exception::class)
    fun <V : Serializable> initSimple(type: Class<V>, array: Array<V>) {
        arr = array as Array<Any>
        subj = SimpleClass<V>(type)
        stack = UndoStack(subj, null)
        stack.subscriber = UndoWatcher()
        for (i in array) {
            val s: SimpleClass<V> = subj as SimpleClass<V>
            val get: () -> V? = s::value::get
            val set: (V?) -> Unit = s::value::set
            stack.push(FunctionalCommand("", get, set, i, null))
        }
    }

    @Throws(IOException::class, ClassNotFoundException::class)
    fun <V : Serializable> testSimple() {

        val manager = UndoManager("", 333, stack)
        val managerBack = manager.deserialize(manager.serialize(manager, false))
        val stackBack = managerBack.stack
        // Here we can not compare stacks themselves 'cause of stack's comparison principle
        assertEquals(stack.subject, stackBack.subject)
        val objBack = stackBack.subject as SimpleClass<V>
        assertEquals(subj, objBack)
        objBack.value

        // Walk here and there
        for (i in arr.size - 1 downTo 1) {
            stackBack.undo()
            //            System.out.println(objBack.getValue());
            assertEquals(arr[i - 1], objBack.value)
        }
        for (i in 1 until arr.size) {
            stackBack.redo()
            assertEquals(arr[i], objBack.value)
        }
    }


    /**
     * Simply shows how elegant [FunctionalCommand] works
     */
    @Test
    @Throws(Exception::class)
    fun testIntegerClass() {

        val pt = Point(-30, -40)
        val stack = UndoStack(pt, null)
        val undoCommand = UndoCommand("Move point", null)
        FunctionalCommand("Change x", pt::x::get , pt::x::set, 10, undoCommand)
        FunctionalCommand("Change y", pt::y::get , pt::y::set, 20, undoCommand)
        stack.push(undoCommand)
        assertEquals(1, stack.count().toLong())
        assertEquals(10, pt.x)
        assertEquals(20, pt.y)
        stack.undo()
        assertEquals(-30, pt.x)
        assertEquals(-40, pt.y)
        assertEquals(0, stack.idx.toLong())

        var manager = UndoManager("", 4, stack)
        manager = manager.deserialize(manager.serialize(manager, true))

        val stackBack = manager.stack
        val ptBack = stackBack.subject as Point
        assertEquals(pt, ptBack)
        assertEquals(-30, ptBack.x)
        assertEquals(-40, ptBack.y)
        assertEquals(1, stackBack.count().toLong())
        assertEquals(0, stackBack.idx.toLong())

        stackBack.redo()
        // ))
        stackBack.redo()
        stackBack.redo()
        assertEquals(10, ptBack.x)
        assertEquals(20, ptBack.y)


    }

    /**
     * Create [UndoStack] with or without groups.
     */
    @Test
    fun creation() {

        run {
            // Create without group
            val subj = SimpleClass(Int::class.java)
            val stack = UndoStack(subj, null)
            stack.subscriber = UndoWatcher()
            assertEquals(true, stack.isClean)
            assertEquals(false, stack.canRedo())
            assertEquals(false, stack.canUndo())
            assertEquals(0, stack.count().toLong())
            assertEquals(true, stack.isActive)
            assertEquals(subj, stack.subject)
            assertEquals("", stack.redoText())
            assertEquals("", stack.undoText())
            assertEquals(0, stack.cleanIdx.toLong())
            assertEquals(0, stack.idx.toLong())
            assertEquals(0, stack.undoLimit.toLong())
        }

        run {
            // Create with group
            // Checks:
            //  - setActive()
            //  - active()
            val subjA = SimpleClass(Int::class.java)
            val subjB = SimpleClass(String::class.java)
            assertNotEquals(subjA, subjB)
            val group = UndoGroup()
            assertEquals(0, group.getStacks().size.toLong())

            val stackA = UndoStack(subjA, group)
            stackA.subscriber = UndoWatcher()
            assertEquals(1, group.getStacks().size.toLong())
            assertEquals(null, group.active)
            assertEquals(false, stackA.isActive)

            // Set active thru UndoStack
            stackA.isActive = true
            assertEquals(stackA, group.active)
            assertEquals(true, stackA.isActive)
            //
            stackA.isActive = false
            assertEquals(null, group.active)
            assertEquals(false, stackA.isActive)

            // Set active thru UndoGroup
            group.active = stackA
            assertEquals(stackA, group.active)
            assertEquals(true, stackA.isActive)
            //
            group.active = null
            assertEquals(null, group.active)
            assertEquals(false, stackA.isActive)

            // Second stack. Do the same
            val stackB = UndoStack(subjB, group)
            stackB.subscriber = UndoWatcher()
            assertEquals(2, group.getStacks().size.toLong())
            assertEquals(null, group.active)
            assertEquals(false, stackA.isActive)
            assertEquals(false, stackB.isActive)

            group.active = stackB
            assertEquals(stackB, group.active)
            assertEquals(false, stackA.isActive)
            assertEquals(true, stackB.isActive)

            group.active = stackA
            assertEquals(stackA, group.active)
            assertEquals(true, stackA.isActive)
            assertEquals(false, stackB.isActive)
        }

    }

    /**
     * Adding and clearing
     */
    @Test
    @Throws(Exception::class)
    fun addAndClear() {

        val scene = NonTrivialClass()
        val group = UndoGroup()
        val stack = UndoStack(scene, group)
        stack.subscriber = UndoWatcher()
        group.active = stack

        stack.push(NonTrivialClass.AddCommand(CIRCLE, scene, null))
        assertEquals(1, stack.count().toLong())
        assertEquals(1, stack.idx.toLong())
        stack.push(NonTrivialClass.AddCommand(CIRCLE, scene, null))
        assertEquals(2, stack.count().toLong())
        assertEquals(2, stack.idx.toLong())
        stack.clear()
        assertEquals(0, stack.count().toLong())
        assertEquals(0, stack.idx.toLong())

    }

    /**
     * Set and check limits:
     * - undoLimit
     * - setIndex
     */
    @Test
    @Throws(Exception::class)
    fun limits() {

        val subj = SimpleClass<Int>(Int::class.java)
        val group = UndoGroup()
        val stack = UndoStack(subj, group)
        stack.subscriber = UndoWatcher()
        stack.undoLimit = 5
        for (i in 0..9) {
            stack.push(FunctionalCommand(i.toString(), subj::value::get, subj::value::set, i, null))
        }
        assertEquals(5, stack.count().toLong())
        stack.setIndex(0)
        assertEquals(4.toInt(), subj.value)
        stack.setIndex(stack.count())
        assertEquals(9.toInt(), subj.value)
    }

    /**
     * Set and check clean:
     * - setClean
     * - isClean
     * - getCleanIdx
     */
    @Test
    @Throws(Exception::class)
    fun clean() {

        val subj = SimpleClass(Int::class.java)
        val group = UndoGroup()
        val stack = UndoStack(subj, group)
        stack.subscriber = UndoWatcher()
        for (i in 0..9) {
            stack.push(FunctionalCommand(i.toString(), subj::value::get, subj::value::set, i, null))
//            stack.push(FunctionalCommand(i.toString(), Getter<Int> { subj.getValue() }, Setter<Int> { subj.setValue() }, i, null!!))
        }
        assertEquals(10, stack.count().toLong())
        stack.setIndex(5)
        assertEquals(4.toInt(), subj.value)
        stack.setClean()
        assertEquals(5, stack.cleanIdx.toLong())
        assertEquals(true, stack.isClean)
        stack.undo()
        assertEquals(false, stack.isClean)
        stack.redo()
        assertEquals(true, stack.isClean)
        stack.redo()
        assertEquals(false, stack.isClean)
        stack.clear()
        assertEquals(0, stack.cleanIdx.toLong())

        // Now set limit, set clean, and go out of it
        stack.undoLimit = 5
        for (i in 0..4) {
            stack.push(FunctionalCommand(i.toString(), subj::value::get, subj::value::set, i, null))
//            stack.push(FunctionalCommand(i.toString(), Getter<Int> { subj.getValue() }, Setter<Int> { subj.setValue() }, i, null!!))
        }
        assertEquals(5, stack.count().toLong())
        stack.setIndex(2)
        stack.setClean()
        assertEquals(2, stack.cleanIdx.toLong())
        stack.setIndex(0)
        assertEquals(2, stack.cleanIdx.toLong())
        stack.push(FunctionalCommand(10.toString(), subj::value::get, subj::value::set, 10, null))
        assertEquals(-1, stack.cleanIdx.toLong())
        assertEquals(false, stack.isClean)
    }

    /**
     * - canUndo
     * - canRedo
     * - undoText
     * - redoText
     */
    @Test
    @Throws(Exception::class)
    fun auxProps() {
        val subj = SimpleClass(Int::class.java)
        val group = UndoGroup()
        val stack = UndoStack(subj, group)
        stack.subscriber = UndoWatcher()
        group.active = stack
        assertEquals(false, stack.canUndo())
        assertEquals(false, stack.canRedo())
        assertEquals("", stack.undoText())
        assertEquals("", stack.redoText())
        assertEquals("", group.undoText())
        assertEquals("", group.redoText())
        assertEquals(false, group.canUndo())
        assertEquals(false, group.canRedo())

        for (i in 0..2) {
            stack.push(FunctionalCommand(i.toString(), subj::value::get, subj::value::set, i, null))
        }
        assertEquals(true, stack.canUndo())
        assertEquals(false, stack.canRedo())
        assertEquals("2", stack.undoText())
        assertEquals("", stack.redoText())
        assertEquals("2", group.undoText())
        assertEquals("", group.redoText())
        assertEquals(true, group.canUndo())
        assertEquals(false, group.canRedo())

        group.undo()
        assertEquals(true, stack.canUndo())
        assertEquals(true, stack.canRedo())
        assertEquals("1", stack.undoText())
        assertEquals("2", stack.redoText())
        assertEquals("1", group.undoText())
        assertEquals("2", group.redoText())
        assertEquals(true, group.canUndo())
        assertEquals(true, group.canRedo())

        group.active!!.setIndex(0)
        assertEquals(false, stack.canUndo())
        assertEquals(true, stack.canRedo())
        assertEquals("", stack.undoText())
        assertEquals("0", stack.redoText())
        assertEquals("", group.undoText())
        assertEquals("0", group.redoText())
        assertEquals(false, group.canUndo())
        assertEquals(true, group.canRedo())
    }

    /**
     * Undo props like [Integer], [String], etc
     */
    @Test
    @Throws(Exception::class)
    fun testSimpleUndo() {

        initSimple(String::class.java, arrayOf<String>("one", "", "two"))
        testSimple<Serializable>()

        initSimple(Int::class.java, arrayOf<Int>(1, 2, 3, 10, 8))
        testSimple<Serializable>()

        initSimple(Long::class.java, arrayOf<Long>(11L, 12L, 13L, 14L, 100L))
        testSimple<Serializable>()

        initSimple(Double::class.java, arrayOf(1.1, 2.2, 3.222))
        testSimple<Serializable>()

        initSimple(Boolean::class.java, arrayOf<Boolean>(true, false, true, true))
        testSimple<Serializable>()

    }

    @Test
    @Throws(Exception::class)
    fun testNonTrivial() {
        val ntc = NonTrivialClass()
        val stack = UndoStack(ntc, null)
        stack.subscriber = UndoWatcher()
        assertEquals(0, ntc.items.size)

        run {
            stack.push(NonTrivialClass.AddCommand(CIRCLE, ntc, null))
            assertEquals(1, stack.count().toLong())
            assertEquals(1, stack.idx.toLong())
            assertEquals(1, ntc.items.size)
            //            System.out.println(ntc);

            stack.push(NonTrivialClass.AddCommand(RECT, ntc, null))
            assertEquals(2, stack.count().toLong())
            assertEquals(2, stack.idx.toLong())
            assertEquals(2, ntc.items.size)
            //            System.out.println(ntc);

            stack.undo()
            assertEquals(2, stack.count().toLong())
            assertEquals(1, stack.idx.toLong())
            assertEquals(1, ntc.items.size)
            //            System.out.println(ntc);

            stack.undo()
            assertEquals(2, stack.count().toLong())
            assertEquals(0, stack.idx.toLong())
            assertEquals(0, ntc.items.size)
            //            System.out.println(ntc);

            val manager = UndoManager("", 333, stack)
            val managerBack = manager.deserialize(manager.serialize(manager, false))
            val stackBack = managerBack.stack
            //            assertEquals(stack, stackBack);
            val objBack = stackBack.subject as NonTrivialClass
            //            assertEquals(subj, objBack);

            //            System.out.println("-------serializ -");

            assertEquals(2, stackBack.count().toLong())
            assertEquals(0, stackBack.idx.toLong())
            assertEquals(0, objBack.items.size)
            //            System.out.println(objBack);

            stackBack.redo()
            assertEquals(1, objBack.items.size)
            //            System.out.println(objBack);

            stackBack.redo()
            assertEquals(2, objBack.items.size)
            //            System.out.println(objBack);
        }


        run {
            //            System.out.println("--- Add/Del ---");
            stack.push(NonTrivialClass.AddCommand(CIRCLE, ntc, null))
            assertEquals(1, stack.count().toLong())
            assertEquals(1, stack.idx.toLong())
            assertEquals(1, ntc.items.size)
            //            System.out.println(ntc);
            stack.push(NonTrivialClass.DeleteCommand(ntc, null))
            assertEquals(2, stack.count().toLong())
            assertEquals(2, stack.idx.toLong())
            assertEquals(0, ntc.items.size)
            //            System.out.println(ntc);

            stack.undo()
            assertEquals(2, stack.count().toLong())
            assertEquals(1, stack.idx.toLong())
            assertEquals(1, ntc.items.size)
            //            System.out.println(ntc);

            stack.undo()
            assertEquals(2, stack.count().toLong())
            assertEquals(0, stack.idx.toLong())
            assertEquals(0, ntc.items.size)
            //            System.out.println(ntc);

            stack.undo()
            assertEquals(2, stack.count().toLong())
            assertEquals(0, stack.idx.toLong())
            assertEquals(0, ntc.items.size)
            //            System.out.println(ntc);

        }

        run {
            //            System.out.println("--- Add/Del/Move ---");
            stack.redo()
            assertEquals(2, stack.count().toLong())
            assertEquals(1, stack.idx.toLong())
            assertEquals(1, ntc.items.size)

            val item = (stack.subject as NonTrivialClass).items.get(0)
            var newPos = 100
            val oldPos = item.x
            item.x = newPos // Moved
            stack.push(NonTrivialClass.MovedCommand(item, oldPos, null))
            assertEquals(2, stack.count().toLong())
            assertEquals(2, stack.idx.toLong())
            assertEquals(1, ntc.items.size)

            assertEquals(newPos, item.x)
            stack.undo()
            assertEquals(oldPos, item.x)
            stack.redo()
            assertEquals(newPos, item.x)

            // Merge
            newPos = 200
            item.x = newPos // Moved again
            stack.push(NonTrivialClass.MovedCommand(item, item.x, null))
            assertEquals(2, stack.count().toLong())
            assertEquals(2, stack.idx.toLong())
            assertEquals(1, ntc.items.size)
            //            System.out.println("4: " + stack);


            // Back
            stack.undo()
            assertEquals(oldPos, item.x)
            assertEquals(2, stack.count().toLong())
            assertEquals(1, stack.idx.toLong())
            assertEquals(1, ntc.items.size)

            // Serialize
            val manager = UndoManager("", 333, stack)
            val managerBack = manager.deserialize(manager.serialize(manager, false))
            val stackBack = managerBack.stack
            val objBack = stackBack.subject as NonTrivialClass

            //            System.out.println("-------serializ -");

            assertEquals(2, stackBack.count().toLong())
            assertEquals(1, stackBack.idx.toLong())
            assertEquals(1, objBack.items.size)
            //            System.out.println(objBack);

            stackBack.redo()
            assertEquals(1, objBack.items.size)
            //            System.out.println(objBack);

        }

        run {

            val manager = UndoManager("", 333, stack)
            val str = manager.serialize(manager, false)

            val baos = ByteArrayOutputStream()
            val gzip = GZIPOutputStream(baos)
            gzip.write(str.toByteArray(charset("UTF-8")))
            gzip.close()
            //            System.out.println("Output String : " + str);
            //            System.out.println("Unzipped length : " + str.length());
            //            System.out.println("Zipped length : " + baos.size());
            //            System.out.println("Zip : " + new String(baos.toByteArray()));

            val gis = GZIPInputStream(ByteArrayInputStream(baos.toByteArray()))
            val bf = BufferedReader(InputStreamReader(gis))
            val str2 = StringBuilder()
            while (bf.ready()) {
                str2.append(bf.readLine())
            }
            val outStr = str2.toString()
            //            System.out.println("Output String : " + outStr);
            assertEquals(str, outStr)

        }


    }

    /**
     * Test for macrocommands
     */
    @Test
    @Throws(Exception::class)
    fun macro() {

        val subj = NonTrivialClass()
        val stack = UndoStack(subj, null)

        stack.push(NonTrivialClass.AddCommand(CIRCLE, subj, null))
        stack.push(NonTrivialClass.AddCommand(RECT, subj, null))
        assertEquals(2, stack.count().toLong())
        assertEquals(0, stack.cleanIdx.toLong())
        assertEquals(true, stack.canUndo())
        assertEquals(false, stack.canRedo())
        stack.undo()
        assertEquals(true, stack.canRedo())
        stack.redo()

        // Adding macrocommand not affects count of simple commands exclude moment of beginning
        stack.beginMacro("Moving")
        assertEquals(3, stack.count().toLong())
        assertEquals(false, stack.canUndo())
        stack.push(NonTrivialClass.MovedCommand(subj.items.get(0), 20, null))
        stack.push(NonTrivialClass.AddCommand(RECT, subj, null))
        stack.push(NonTrivialClass.AddCommand(RECT, subj, null))
        stack.push(NonTrivialClass.AddCommand(RECT, subj, null))
        stack.push(NonTrivialClass.AddCommand(RECT, subj, null))
        stack.push(NonTrivialClass.AddCommand(RECT, subj, null))
        stack.push(NonTrivialClass.AddCommand(RECT, subj, null))
        // Adding macrocommand not affects count
        assertEquals(3, stack.count().toLong())

        // Should has no effect inside macro process
        stack.setClean()
        assertEquals(0, stack.cleanIdx.toLong())
        assertEquals(3, stack.count().toLong())
        assertEquals(2, stack.idx.toLong())
        stack.undo()
        assertEquals(2, stack.idx.toLong())
        stack.redo()
        assertEquals(2, stack.idx.toLong())
        stack.setIndex(0)
        assertEquals(2, stack.idx.toLong())

        stack.endMacro()
        assertEquals(3, stack.idx.toLong())
        // 2 simple and 1 macro
        assertEquals(3, stack.count().toLong())
        assertEquals(8, subj.items.size)

        // Undo macro
        stack.undo()
        assertEquals(2, stack.idx.toLong())
        assertEquals(2, subj.items.size)

        // Undo macro
        stack.redo()
        assertEquals(3, stack.idx.toLong())
        assertEquals(8, subj.items.size)

        val manager = UndoManager("", 2, stack)
        val z_data = manager.serialize(manager, true)
        //        System.out.println("zipped length : " + z_data.length());
        val managerBack = manager.deserialize(z_data)
        assertEquals(manager.VERSION, managerBack.VERSION)
        assertEquals(manager.extras, managerBack.extras)
        assertEquals(manager.stack.subject, managerBack.stack.subject)
        assertEquals(NonTrivialClass::class.java, manager.stack.subject.javaClass)
        assertEquals(3, manager.stack.idx)
        assertEquals(8, (manager.stack.subject as NonTrivialClass).items.size)

        // After deserialization
        // Undo macro
        manager.stack.undo()
        assertEquals(2, manager.stack.idx)
        assertEquals(2, (manager.stack.subject as NonTrivialClass).items.size)

        // Undo macro
        manager.stack.redo()
        assertEquals(3, manager.stack.idx)
        assertEquals(8, (manager.stack.subject as NonTrivialClass).items.size)

    }


}
