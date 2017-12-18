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

    /**
     * Returns the ID of this command.
     *
     * A command ID is used in command compression. It must be an integer unique to this command's class,
     * or -1 if the command doesn't support compression.
     *
     * If the command supports compression this function must be overridden in the derived class to return the correct ID.
     * The base implementation returns -1.
     *
     * [UndoStack.push] will only try to merge two commands if they have the same ID, and the ID is not -1.
     *
     * @return Integer unique to this command's class or -1 if the command doesn't support compression.
     */
    fun id(): Int {
        return -1
    }

    /**
     * Attempts to merge this command with cmd. Returns true on success; otherwise returns false.
     *
     * If this function returns true, calling this command's redo() must have the same effect as redoing
     * both this command and cmd.
     *
     * Similarly, calling this command's [undo] must have the same effect as undoing cmd and this command.
     *
     * UndoStack will only try to merge two commands if they have the same id, and the id is not -1.
     *
     * The default implementation returns false.
     *
     * @param cmd Command to try merge with
     * @return True on success, otherwise returns false.
     */
    fun mergeWith(cmd: UndoCommand): Boolean {
        return false
    }

    /**
     * @return if child commands exist returns their count otherwise returns zero.
     */
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
     * Calls [doRedo] in derived classes.
     */
    fun redo() {
        doRedo()
    }

    /**
     * Calls [doUndo]  in derived classes.
     */
    fun undo() {
        doUndo()
    }

    /**
     * Applies a change to the document. This function must be implemented in the derived class.
     *
     * Calling [UndoStack.push], [UndoStack.undo] or [UndoStack.redo] from this function leads to  undefined behavior.
     */
    protected open fun doRedo() {
        if (childLst != null) {
            for (cmd in childLst!!) {
                cmd.redo()
            }
        }
    }

    /**
     * Reverts a change to the document. After [undo] is called, the state of the document should be the same
     * as before [redo] was called. This function must be implemented in the derived class.
     *
     * Calling [UndoStack.push], [UndoStack.undo] or [UndoStack.redo] from this function leads to  undefined behavior.
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
