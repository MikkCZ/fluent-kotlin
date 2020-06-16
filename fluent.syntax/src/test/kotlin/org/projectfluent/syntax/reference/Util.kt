package org.projectfluent.syntax.reference

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import org.junit.jupiter.api.Assertions.*
import org.projectfluent.syntax.ast.*

fun assertAstEquals(jsonObject: JsonObject, node: BaseNode, stack: MutableList<String> = mutableListOf()) {
    val jType = jsonObject.remove("type")
    assertEquals(jType, node::class.simpleName, stack.joinToString(">"))
    for ((key, value) in childrenOf(node)) {
        if (key == "span") {
            value?.let { assertAstEquals(jsonObject[key] as JsonObject, value as BaseNode, stack) }
            return
        }
        stack.add(key)
        val jVal = jsonObject[key]
        if (jVal == null) {
            assertNull(value, "${stack.joinToString(">")} is null in reference")
        }
        when (value) {
            null -> assertNull(jVal, "${stack.joinToString(">") }} is not null in reference")
            is BaseNode -> assertAstEquals(jVal as JsonObject, value, stack)
            is Collection<*> -> {
                assertTrue(jVal is JsonArray<*>, "${stack.joinToString(">")} is not a list")
                value.filterNot { it is Whitespace }.zip(jVal as JsonArray<*>).forEachIndexed { index, pair ->
                    val (childNode, childJson) = pair
                    stack.add(index.toString())
                    when (childNode) {
                        is BaseNode -> {
                            assertTrue(childJson is JsonObject, "${stack.joinToString(">")} has a non-object ref")
                            assertAstEquals(childJson as JsonObject, childNode, stack)
                        }
                        // Compare Fluent values as strings
                        else -> assertEquals(childJson.toString(), "$childNode")
                    }
                    stack.removeAt(stack.lastIndex)
                }
            }
            else -> assertEquals(jVal, value, "${stack.joinToString(">")} differs")
        }
        stack.removeAt(stack.lastIndex)
    }
}
