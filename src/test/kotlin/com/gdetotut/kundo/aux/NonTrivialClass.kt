package com.gdetotut.kundo.aux

import com.gdetotut.kundo.UndoCommand

import java.io.Serializable
import java.util.ArrayList
import java.util.Objects

open class NonTrivialClass : Serializable {

    val items: MutableList<Item> = ArrayList()

    class Item(val type: Type) : Serializable {

        var x: Int = 0

        enum class Type {
            RECT,
            CIRCLE
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val item = o as Item?
            return x == item!!.x && type == item.type
        }

        override fun hashCode(): Int {
            return Objects.hash(x, type)
        }

        override fun toString(): String {
            return "Item{" +
                    "x=" + x +
                    ", type=" + type +
                    '}'
        }
    }

    /**
     * Command for Adding
     */
    class AddCommand(type: Item.Type, private val scene: NonTrivialClass, parent: UndoCommand?)
        : UndoCommand("", parent) {
        private val item: Item
        private val initialPos: Int

        init {
            item = Item(type)
            initialPos = this.scene.items.size * 2
            text = ConstForTest.CMD_ADD + " at " + initialPos
        }

        protected override fun doUndo() {
            scene.items.remove(item)
        }

        protected override fun doRedo() {
            scene.items.add(item)
            item.x = initialPos
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as AddCommand?
            return initialPos == that!!.initialPos && item == that.item
        }

        override fun hashCode(): Int {
            return Objects.hash(item, initialPos)
        }
    }

    /**
     * Adding
     */
    class DeleteCommand(private val scene: NonTrivialClass, parent: UndoCommand?)
        : UndoCommand("", parent) {
        private val item: Item?

        init {
            this.item = if (scene.items.size > 0) scene.items[0] else null
            text = ConstForTest.CMD_DEL + " at " + item!!.x
        }

        protected override fun doUndo() {
            if (item != null) {
                scene.items.add(item)
            }
        }

        protected override fun doRedo() {
            if (item != null) {
                scene.items.remove(item)
            }

        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as DeleteCommand?
            return scene == that!!.scene && item == that.item
        }

        override fun hashCode(): Int {
            return Objects.hash(scene, item)
        }
    }

    /**
     *
     */
    class MovedCommand(private val item: Item, private val oldPos: Int, parent: UndoCommand?)
        : UndoCommand("", parent) {
        private var newPos: Int = 0

        init {
            this.newPos = item.x
            text = ConstForTest.CMD_MOV + " to " + item.x
        }

        protected override fun doUndo() {
            item.x = oldPos
        }

        protected override fun doRedo() {
            item.x = newPos
        }

        override fun mergeWith(cmd: UndoCommand): Boolean {
            if (cmd is MovedCommand) {
                val item = (cmd as MovedCommand).item
                if (item === this.item) {
                    newPos = item.x
                    text = ConstForTest.CMD_MOV + " to " + item.x
                    return true
                }
            }
            return false
        }

        override fun id(): Int {
            return 1234
        }

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as MovedCommand?
            return oldPos == that!!.oldPos &&
                    newPos == that.newPos &&
                    item == that.item
        }

        override fun hashCode(): Int {
            return Objects.hash(item, oldPos, newPos)
        }
    }

    internal fun addItem(item: Item) {
        items.add(item)
    }

    internal fun removeItem(item: Item) {
        items.remove(item)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val aClass = o as NonTrivialClass?
        return items == aClass!!.items
    }

    override fun hashCode(): Int {
        return Objects.hash(items)
    }

    override fun toString(): String {
        return "NonTrivialClass{" +
                "items=" + items +
                '}'
    }
}
