package ticketpile.service.graphql

import graphql.language.FloatValue
import graphql.schema.Coercing
import graphql.schema.GraphQLScalarType
import java.math.BigDecimal

/**
 * IEEE 754 (i.e. Float/Double) is a PITA.  For instance, create a Float with value 87.55.
 * Now print it: 87.55000305175781. Lollerskates.  [GraphQLFloat] is unsuitable for real financial
 * work. [GraphQLBigDecimal] solves this problem.
 * 
 * Created by jonlatane on 10/14/16.
 */
val GraphQLBigDecimal = GraphQLScalarType("BigDecimal", "Java BigDecimal", object : Coercing {
    override fun serialize(input: Any): BigDecimal? {
        if (input is String) {
            return BigDecimal(input)
        } else if (input is BigDecimal) {
            return input
        } else if (input is Float) {
            return BigDecimal(input.toDouble())
        } else if (input is Int) {
            return BigDecimal(input)
        } else {
            return null
        }
    }

    override fun parseValue(input: Any): BigDecimal? {
        return serialize(input)
    }

    override fun parseLiteral(input: Any): Any {
        return BigDecimal((input as FloatValue).value.toDouble())
    }
})