package com.gdetotut.kundo.aux

import com.gdetotut.kundo.UndoEvents

import java.io.Serializable

class UndoWatcher : UndoEvents, Serializable {

    companion object {
        private val cnt: Int = 0
    }

//    override fun indexChanged(idx: Int) {
//        super.indexChanged(idx)
//    }
//
//    override fun cleanChanged(clean: Boolean) {
//        super.cleanChanged(clean)
//    }
//
//    override fun canUndoChanged(canUndo: Boolean) {
//        super.canUndoChanged(canUndo)
//    }
//
//    override fun canRedoChanged(canRedo: Boolean) {
//        super.canRedoChanged(canRedo)
//    }
//
//    override fun undoTextChanged(undoText: String) {
//        super.undoTextChanged(undoText)
//    }
//
//    override fun redoTextChanged(redoText: String) {
//        super.redoTextChanged(redoText)
//    }

}
