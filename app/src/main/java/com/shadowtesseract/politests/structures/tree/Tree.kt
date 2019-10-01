package com.shadowtesseract.politests.structures.tree
import android.util.Log
import com.google.gson.JsonObject

class Tree
{
    var root : Node
    constructor(jsonObject: JsonObject)
    {
        Log.d("MSG", "Tworzenie drzewa")
        root = parseTree(jsonObject, null)
        root.sortAllChildrens()
        Log.d("MSG", "Stworzono drzewo")
    }

    fun print()
    {
        printListForRoot(root)
    }

    private fun printListForRoot(node : Node)
    {
        Log.d("MSG", "Root: "+node.root+" -> "+node.children)
        for (n in node.children)
            printListForRoot(n)
    }

    private fun parseTree(element: JsonObject, root:Node?) : Node
    {
        var jsonName = element.getAsJsonPrimitive("name")
        var name = jsonName.asString ?: ""
        var newNode = Node(name, root)

        if(element.has("children"))
        {
            var array = element.getAsJsonArray("children")
            (0..array.size()-1)
                    .forEach { i ->
                        newNode.addChild(parseTree(array.get(i).asJsonObject, newNode))
                    }

        }

        return newNode
    }

}

class Node(var value : Any, var root : Node?)
{
    var children : MutableList<Node> = arrayListOf()

    fun addChild(node:Node)
    {
        this.children.add(node)
    }

    override fun toString() = value.toString()

    fun getAllChildren() = children

    fun getAllChildrensNames() : MutableList<String> {
        var list : MutableList<String> = arrayListOf<String>()
        for (c in children)
            list.add(c.value.toString())
        return list
    }

    fun getNodeWithName(name: String) : Node?
    {
        for (c in children)
            if(c.value.toString() == name)
                return c
        return null
    }

    fun getNodeWithIndex(index: Int) : Node?
    {
        if(index<0 || index>=children.size)
            return null
        return children.get(index)
    }


    fun sortAllChildrens()
    {
//        Log.d("TREE", "Sortowanie "+this+" -> "+children.size)
        if(children.size == 0) return
        children.sortBy { it.toString() }
        for (ch in children)
            ch.sortAllChildrens()

    }
}