package org.h2.expression.condition

import org.h2.engine.SessionLocal
import org.h2.expression.Expression
import org.h2.value.TypeInfo
import org.h2.value.Value
import org.h2.value.ValueBoolean
import org.h2.value.ValueNull
import java.util.*

/**
 * Type predicate (IS [NOT] OF).
 */
class TypePredicate(left: Expression,
                    not: Boolean,
                    whenOperand: Boolean,
                    val typeList: Array<TypeInfo>) : SimplePredicate(left, not, whenOperand) {
    private lateinit var valueTypes: IntArray

    override fun getNotIfPossible(session: SessionLocal?): Expression? = if (whenOperand) null
    else TypePredicate(left!!, !not, false, typeList)

    override fun getWhenValue(session: SessionLocal, left: Value?): Boolean {
        if (!whenOperand) return super.getWhenValue(session, left)
        return if (left === ValueNull.INSTANCE) false
        else (Arrays.binarySearch(valueTypes, left!!.getValueType()) >= 0) xor not
    }

    override fun getValue(session: SessionLocal?): Value {
        val l = left!!.getValue(session)
        return if (l === ValueNull.INSTANCE) ValueNull.INSTANCE else ValueBoolean[(Arrays.binarySearch(valueTypes, l!!.getValueType()) >= 0) xor not]
    }

    override fun getUnenclosedSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        return getWhenSQL(left!!.getSQL(builder, sqlFlags, AUTO_PARENTHESES), sqlFlags)
    }

    override fun optimize(session: SessionLocal?): Expression? {
        val count = typeList.size

        valueTypes = IntArray(count)
        for (i in 0 until count) valueTypes[i] = typeList[i].valueType
        Arrays.sort(valueTypes)
        return super.optimize(session)
    }

    override fun getWhenSQL(builder: StringBuilder, sqlFlags: Int): StringBuilder {
        builder.append(" IS")
        if (not) builder.append(" NOT")
        builder.append(" OF (")

        for (i in typeList.indices) {
            if (i > 0) builder.append(", ")
            typeList[i].getSQL(builder, sqlFlags)
        }
        return builder.append(')')
    }
}