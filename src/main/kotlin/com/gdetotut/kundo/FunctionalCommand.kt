package com.gdetotut.kundo

import java.io.Serializable
import java.util.Objects

class FunctionalCommand<V : Serializable> @Throws(Exception::class)

constructor(text: String, getter: () -> V?, private val setter: (V?) -> Unit,
            private val newValue: V, parent: UndoCommand?)
    : UndoCommand(text, parent) {

    private val oldValue: V? = getter()

    public override fun doUndo() {
        setter(oldValue)
    }

    public override fun doRedo() {
        setter(newValue)
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false
        val that = o as FunctionalCommand<*>?
        return oldValue == that!!.oldValue && newValue == that.newValue
    }

    override fun hashCode(): Int {
        return Objects.hash(oldValue, newValue)
    }

    override fun toString(): String {
        return "FunctionalCommand{" +
                "oldValue=" + oldValue +
                ", newValue=" + newValue +
                '}'
    }
}
