package com.gdetotut.kundo

import java.io.Serializable
import java.util.Objects

/**
 *
 * @param <V> generic type for this class.
</V> */
class FunctionalCommand<V : Serializable> @Throws(Exception::class)
constructor(text: String, getter: Getter<V>, private val setter: Setter<V>, private val newValue: V, parent: UndoCommand)
    : UndoCommand(text, parent) {
    private val oldValue: V

    init {
        this.oldValue = getter.get()
    }

    public override fun doUndo() {
        setter.set(oldValue)
    }

    public override fun doRedo() {
        setter.set(newValue)
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
