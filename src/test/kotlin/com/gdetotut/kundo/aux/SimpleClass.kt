package com.gdetotut.kundo.aux

import java.io.Serializable

class SimpleClass<V>(private val type: Class<V>) : Serializable {

    var value: V? = null

    override fun toString(): String {
        return "SimpleClass{" +
                "value=" + value +
                '}'
    }

    override fun equals(o: Any?): Boolean {
        if (this === o) return true
        if (o == null || javaClass != o.javaClass) return false

        val that = o as SimpleClass<*>?

        return if (value != null)
            value == that!!.value
        else
            that!!.value == null && type == that.type
    }

    override fun hashCode(): Int {
        return if (value != null) value!!.hashCode() else 0
    }


}
