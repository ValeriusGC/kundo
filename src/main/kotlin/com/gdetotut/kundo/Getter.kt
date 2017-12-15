package com.gdetotut.kundo

import java.io.Serializable

@FunctionalInterface
interface Getter<out V : java.io.Serializable> : Serializable {
    fun get(): V
}
