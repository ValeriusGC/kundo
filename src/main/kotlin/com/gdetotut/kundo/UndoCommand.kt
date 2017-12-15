package com.gdetotut.kundo

import java.io.Serializable
import java.util.ArrayList

/**
 * The UndoCommand class is the base class of all commands stored on an UndoStack.
 */
open class UndoCommand(text: String?, parent: UndoCommand?) : Serializable {

    var text: String? = null
    var childLst: MutableList<UndoCommand>? = null

    init {
        this.text = text
        if (parent != null) {
            if (parent.childLst == null) {
                parent.childLst = ArrayList()
            }
            parent.childLst?.add(this)
        }
    }

    fun id(): Int {
        return -1
    }

    fun mergeWith(cmd: UndoCommand): Boolean {
        return false
    }

    fun childCount(): Int {
        return if(childLst == null) {
            0
        }else childLst!!.size
    }

    fun child(idx: Int): UndoCommand? {
        return if (idx < 0 || idx >= childCount()) {
            null
        } else childLst!![idx]
    }

    /**
     * Calls doRedo()  in derived classes.
     */
    fun redo() {
        doRedo()
    }

    /**
     * Calls doUndo()  in derived classes.
     */
    fun undo() {
        doUndo()
    }

    /**
     * Applies a change to the document. This function must be implemented in the derived class.
     * Calling UndoStack.push(), UndoStack.undo() or UndoStack.redo() from this function leads to  undefined behavior.
     */
    protected open fun doRedo() {
        if (childLst != null) {
            for (cmd in childLst!!) {
                cmd.redo()
            }
        }
    }

    /**
     * Reverts a change to the document. After undo() is called, the state of the document should be the same
     * as before redo() was called. This function must be implemented in the derived class.
     * Calling UndoStack.push(), UndoStack.undo() or UndoStack.redo() from this function leads to undefined behavior.
     */
    protected open fun doUndo() {
        if (childLst != null) {
            for (cmd in childLst!!) {
                cmd.undo()
            }
        }
    }

    override fun toString(): String {
        return "UndoCommand{" +
                "text='" + text + '\'' +
                '}'
    }
}
