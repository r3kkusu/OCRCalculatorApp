package com.testapp.ocrcalculatorapp

import android.content.ContentResolver
import android.content.ContentUris
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.util.*


class TokenizerCalculator {

    private var regex = arrayOf("[0-9 .]+", "[\\*\\+\\-\\/\\×\\÷]")
    private var pSequence = arrayOf(regex[0], regex[1], regex[0])

    fun groomExpression(expression: String?) : String {
        val statements = breakStatement(expression!!);
        var groomed = "";
        statements?.iterator()?.forEach {
            groomed += it
        }

        return groomed;
    }

    fun calculate(expression: String?): Int {
        val statements = breakStatement(expression!!)
        val first = statements!![0]!!.toDouble()
        val second = statements[2]!!.toDouble()
        when (statements[1]) {
            "*", "×" -> return (first * second).toInt()
            "+" -> return (first + second).toInt()
            "-" -> return (first - second).toInt()
            "/", "÷" -> return (first / second).toInt()
        }
        return Int.MAX_VALUE
    }

    private fun breakStatement(expression: String): Array<String?>? {

        val queue: Queue<String> = LinkedList()
        var statement = String()

        // Remove white spaces
        val equation = expression.replace("\\s+".toRegex(), "")
        var pindex = 0
        var step = 0
        while (step < equation.length && pindex < pSequence.size) {
            val c = equation[step]
            if (!c.toString().matches(Regex(pSequence[pindex]))) {
                step -= if (step != 0) 1 else 0
                pindex++
                queue.add(statement)
                statement = String()
            } else {
                statement += c
            }
            step++
        }
        queue.add(statement)
        return convertArray(queue)
    }

    private fun convertArray(queue: Queue<String>): Array<String?>? {

        val statements = arrayOfNulls<String>(queue.size)
        val iterator = queue.iterator()
        var i = -1
        while (iterator.hasNext()) {
            statements[++i] = iterator.next()
        }
        return statements
    }
}