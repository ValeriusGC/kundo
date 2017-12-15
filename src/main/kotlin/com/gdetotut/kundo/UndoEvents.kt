package com.gdetotut.kundo

/**
 * Methods for subscribers.
 *
 */
interface UndoEvents {

    fun indexChanged(idx: Int) {}
    fun cleanChanged(clean: Boolean) {}
    fun canUndoChanged(canUndo: Boolean) {}
    fun canRedoChanged(canRedo: Boolean) {}
    fun undoTextChanged(undoText: String) {}
    fun redoTextChanged(redoText: String) {}

}
