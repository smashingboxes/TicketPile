package ticketpile.service.graphql

import graphql.schema.GraphQLFieldDefinition.newFieldDefinition
import graphql.schema.GraphQLObjectType
import ticketpile.service.model.transformation.Weighable

/**
 * A handy dandy shortcut 
 * Created by jonlatane on 10/14/16.
 */
fun GraphQLObjectType.Builder.weighableFields(
        itemType : String = "Object"
) : GraphQLObjectType.Builder {
    return this
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("basePrice")
            .description("The base price of this $itemType, before discounts, addons, etc.")
            .dataFetcher {
                (it.source as Weighable).basePrice!!
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("discountsAmount")
            .description("The total value of all Discounts on this $itemType.")
            .dataFetcher {
                (it.source as Weighable).discountsAmount!!
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("feesAmount")
            .description("The total value of all Fees on this $itemType.")
            .dataFetcher {
                (it.source as Weighable).feesAmount!!
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("addOnsAmount")
            .description("The total value of all AddOns on this $itemType.")
            .dataFetcher {
                (it.source as Weighable).addOnsAmount!!
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("manualAdjustmentsAmount")
            .description("The total value of all ManualAdjustments on this $itemType.")
            .dataFetcher {
                (it.source as Weighable).manualAdjustmentsAmount!!
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("itemAddOnsAmount")
            .description("The total value of all AddOns on this $itemType.")
            .dataFetcher {
                (it.source as Weighable).itemAddOnsAmount!!
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("grossAmount")
            .description("The gross value of this $itemType.  May potentially be negative.  Calculated as: " +
                    "basePrice + discountsAmount + feesAmount + addOnsAmount + manualAdjustmentsAmount + itemAddOnsAmount")
            .dataFetcher {
                (it.source as Weighable).grossAmount!!
            }
            .build())
        .field(newFieldDefinition().type(GraphQLBigDecimal)
            .name("totalAmount")
            .description("The total value of this $itemType, which is never negative. " +
                    "Calculated as max(grossAmount, 0).")
            .dataFetcher {
                (it.source as Weighable).totalAmount!!
            }
            .build())
}