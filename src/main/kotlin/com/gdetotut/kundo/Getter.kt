package com.gdetotut.kundo

import java.io.Serializable

@FunctionalInterface
interface Getter<out V : Serializable> : Serializable {
    fun get(): V
}
