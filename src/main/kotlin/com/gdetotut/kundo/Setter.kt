package com.gdetotut.kundo

import java.io.Serializable

@FunctionalInterface
interface Setter<in V : java.io.Serializable> : Serializable {
    fun set(v: V)
}
