package com.gdetotut.kundo

import java.io.Serializable
import java.util.ArrayList
import java.util.Objects

class UndoStack(val subject: Serializable, group: UndoGroup?) : Serializable {

    internal var group: UndoGroup? = null

    var idx: Int = 0
        private set

    var cleanIdx: Int = 0
        private set

    private var cmdLst: MutableList<UndoCommand>? = null

    private var macroStack: MutableList<UndoCommand>? = null

    var undoLimit: Int = 0
        set(value) {
            if (cmdLst != null && cmdLst!!.size > 0) {
                System.err.println("UndoStack.undoLimit: an undo limit can only be set when the stack is empty")
            }
            if (value == this.undoLimit) {
                return
            }
            field = value
            checkUndoLimit()
        }

    var subscriber: UndoEvents? = null

    val isClean: Boolean
        get() = if (macroStack != null && !macroStack!!.isEmpty()) {
            false
        } else cleanIdx == idx

    var isActive: Boolean
        get() = group == null || group!!.active === this
        set(active) {
            if (group != null) {
                if (active) {
                    group!!.active = this
                } else if (group!!.active === this) {
                    group!!.active = null
                }
            }
        }

    init {
        group?.add(this)
    }

    fun clear() {
        if (cmdLst == null || cmdLst!!.isEmpty()) {
            return
        }

        val wasClean = isClean

        if (macroStack != null) {
            macroStack!!.clear()
        }

        for (cmd in cmdLst!!) {
            if (cmd.childLst != null) {
                cmd.childLst!!.clear()
            }
        }
        cmdLst!!.clear()
        idx = 0
        cleanIdx = 0

        if (null != subscriber) {
            subscriber!!.indexChanged(0)
            subscriber!!.canUndoChanged(false)
            subscriber!!.undoTextChanged("")
            subscriber!!.canRedoChanged(false)
            subscriber!!.redoTextChanged("")
            if (!wasClean) {
                subscriber!!.cleanChanged(true)
            }
        }
    }

    fun push(cmd: UndoCommand) {

        cmd.redo()

        val macro = macroStack != null && !macroStack!!.isEmpty()

        if (cmdLst == null) {
            cmdLst = ArrayList()
        }

        var cur: UndoCommand? = null
        var macroCmd: UndoCommand? = null
        if (macro) {
            macroCmd = macroStack!![macroStack!!.size - 1]
            if (macroCmd.childLst != null && !macroCmd.childLst!!.isEmpty()) {
                cur = macroCmd.childLst!![macroCmd.childLst!!.size - 1]
            }
        } else {
            if (idx > 0) {
                cur = cmdLst!![idx - 1]
            }
            while (idx < cmdLst!!.size) {
                cmdLst!!.removeAt(cmdLst!!.size - 1)
            }
            if (cleanIdx > idx) {
                cleanIdx = -1
            }
        }


        val canMerge = (cur != null
                && cur.id() != -1
                && cur.id() == cmd.id()
                && macro) || idx != cleanIdx

        if (canMerge && cur != null && cur.mergeWith(cmd)) {
            if (!macro && null != subscriber) {
                subscriber!!.indexChanged(idx)
                subscriber!!.canUndoChanged(canUndo())
                subscriber!!.undoTextChanged(undoText()!!)
                subscriber!!.canRedoChanged(canRedo())
                subscriber!!.redoTextChanged(redoText()!!)
            }
        } else {
            if (macro) {
                if (macroCmd!!.childLst == null) {
                    macroCmd.childLst = ArrayList()
                }
                macroCmd.childLst!!.add(cmd)
            } else {
                // And last actions
                cmdLst!!.add(cmd)
                checkUndoLimit()
                setIndex(idx + 1, false)
            }
        }
    }

    fun setClean() {
        if (macroStack != null && !macroStack!!.isEmpty()) {
            System.err.println("UndoStack.setClean: cannot set clean in the middle of a macro")
            return
        }
        setIndex(idx, true)
    }

    fun undo() {
        if (cmdLst == null || idx == 0) {
            return
        }

        if (macroStack != null && !macroStack!!.isEmpty()) {
            System.err.println("UndoStack.undo: cannot undo in the middle of a macro")
            return
        }

        val idx = this.idx - 1

        cmdLst!![idx].undo()
        setIndex(idx, false)
    }

    fun redo() {
        if (cmdLst == null || idx == cmdLst!!.size) {
            return
        }

        if (macroStack != null && !macroStack!!.isEmpty()) {
            System.err.println("UndoStack.redo(): cannot redo in the middle of a macro")
            return
        }

        cmdLst!![idx].redo()
        setIndex(idx + 1, false)
    }

    fun count(): Int {
        return if (cmdLst == null) 0 else cmdLst!!.size
    }

    fun setIndex(idx: Int) {
        var idx = idx

        if (macroStack != null && !macroStack!!.isEmpty()) {
            System.err.println("UndoStack.setIndex(): cannot set index in the middle of a macro")
            return
        }

        if (cmdLst == null) {
            return
        }

        if (idx < 0) {
            idx = 0
        } else if (idx > cmdLst!!.size) {
            idx = cmdLst!!.size
        }

        var i = this.idx
        while (i < idx) {
            cmdLst!![i++].redo()
        }
        while (i > idx) {
            cmdLst!![--i].undo()
        }

        setIndex(idx, false)
    }

    fun canUndo(): Boolean {
        return if (macroStack != null && !macroStack!!.isEmpty()) {
            false
        } else idx > 0
    }

    fun canRedo(): Boolean {
        return if (macroStack != null && !macroStack!!.isEmpty()) {
            false
        } else cmdLst != null && idx < cmdLst!!.size
    }

    fun undoText(): String? {
        if (macroStack != null && !macroStack!!.isEmpty()) {
            return ""
        }
        return if (cmdLst != null && idx > 0) cmdLst!![idx - 1].text else ""
    }

    fun redoText(): String? {
        if (macroStack != null && !macroStack!!.isEmpty()) {
            return ""
        }
        return if (cmdLst != null && idx < cmdLst!!.size) cmdLst!![idx].text else ""
    }

    fun beginMacro(text: String) {

        val cmd = UndoCommand(text, null)

        if (macroStack == null) {
            macroStack = ArrayList()
        }

        if (macroStack!!.isEmpty()) {

            while (idx < cmdLst!!.size) {
                cmdLst!!.removeAt(cmdLst!!.size - 1)
            }
            if (cleanIdx > idx) {
                cleanIdx = -1
            }
            cmdLst!!.add(cmd)

        } else {
            macroStack!![macroStack!!.size - 1].childLst!!.add(cmd)
        }

        macroStack!!.add(cmd)

        if (macroStack!!.size == 1) {
            if (subscriber != null) {
                subscriber!!.canUndoChanged(false)
                subscriber!!.undoTextChanged("")
                subscriber!!.canRedoChanged(false)
                subscriber!!.redoTextChanged("")
            }
        }
    }

    fun endMacro() {
        if (macroStack == null || macroStack!!.isEmpty()) {
            System.err.println("UndoStack.endMacro(): no matching beginMacro()")
        }
        macroStack!!.removeAt(macroStack!!.size - 1)

        if (macroStack!!.isEmpty()) {
            checkUndoLimit()
            setIndex(idx + 1, false)
        }
    }

    fun getCommand(idx: Int): UndoCommand? {
        return if (cmdLst == null || idx < 0 || idx >= cmdLst!!.size) {
            null
        } else cmdLst!![idx]
    }

    fun text(idx: Int): String? {
        return if (cmdLst == null || idx < 0 || idx >= cmdLst!!.size) {
            ""
        } else cmdLst!![idx].text
    }

    override fun toString(): String {
        return "UndoStack{" +
                "idx=" + idx +
                ", cmdLst=" + (if (cmdLst == null) "" else cmdLst) +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val stack = o as UndoStack?

        return idx == stack!!.idx && subject === stack.subject
    }

    override fun hashCode(): Int {
        return Objects.hash(idx, subject, cmdLst)
    }

    private fun setIndex(index: Int, clean: Boolean) {

        val wasClean = idx == cleanIdx

        if (this.idx != index) {
            this.idx = index
            if (null != subscriber) {
                subscriber!!.indexChanged(idx)
                subscriber!!.canUndoChanged(canUndo())
                subscriber!!.undoTextChanged(undoText()!!)
                subscriber!!.canRedoChanged(canRedo())
                subscriber!!.redoTextChanged(redoText()!!)
            }
        }

        if (clean) {
            cleanIdx = idx
        }

        val isClean = idx == cleanIdx
        if (isClean != wasClean && null != subscriber) {
            subscriber!!.cleanChanged(isClean)
        }
    }

    private fun checkUndoLimit() {

        if (this.undoLimit <= 0
                || cmdLst == null
                || this.undoLimit >= cmdLst!!.size
                || macroStack != null && macroStack!!.size > 0) {
            return
        }

        val delCnt = cmdLst!!.size - this.undoLimit
        for (i in 0 until delCnt) {
            cmdLst!!.removeAt(0)
        }

        idx -= delCnt
        if (cleanIdx != -1) {
            if (cleanIdx < delCnt) {
                cleanIdx = -1
            } else {
                cleanIdx -= delCnt
            }
        }
    }

}
