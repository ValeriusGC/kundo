package com.gdetotut.kundo

import java.io.Serializable
import java.util.ArrayList

class UndoGroup : Serializable {

    var active: UndoStack? = null
        set(stack) {
            if (this.active === stack) {
                return
            }
            field = stack
        }

    private val stacks = ArrayList<UndoStack>()

    val isClean: Boolean
        get() = this.active == null || this.active!!.isClean

    fun clear() {
        for (stack in stacks) {
            stack.group = null
        }
        stacks.clear()
    }

    fun add(stack: UndoStack) {
        if (stacks.contains(stack)) {
            return
        }

        stacks.add(stack)
        if (null != stack.group) {
            stack.group!!.remove(stack)
        }
        stack.group = this

    }

    fun remove(stack: UndoStack) {
        if (!stacks.remove(stack)) {
            return
        }

        if (stack === this.active) {
            active = null
        }
        stack.group = null
    }

    fun getStacks(): List<UndoStack> {
        return stacks
    }

    fun undo() {
        if (this.active != null) {
            this.active!!.undo()
        }
    }

    fun redo() {
        if (this.active != null) {
            this.active!!.redo()
        }
    }

    fun canUndo(): Boolean {
        return this.active != null && this.active!!.canUndo()
    }

    fun canRedo(): Boolean {
        return this.active != null && this.active!!.canRedo()
    }

    fun undoText(): String? {
        return if (this.active != null) this.active!!.undoText() else ""
    }

    fun redoText(): String? {
        return if (this.active != null) this.active!!.redoText() else ""
    }

}
