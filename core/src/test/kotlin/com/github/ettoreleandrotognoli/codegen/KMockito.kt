package com.github.ettoreleandrotognoli.codegen

import org.mockito.Mockito
import kotlin.reflect.KClass

class KMockito {

    companion object {
        fun <T> any(): T {
            return Mockito.any<T>() as T
        }

        fun <T> any(type: Class<T>): T {
            return Mockito.any(type) as T
        }

        fun <T : Any> any(type: KClass<T>): T {
            return any(type.java)
        }

        fun <T : Any> mock(type: KClass<T>): T {
            return Mockito.mock(type.java)
        }

        fun <T : Any> eq(obj: T): T {
            Mockito.eq(obj)
            return obj
        }

    }

}