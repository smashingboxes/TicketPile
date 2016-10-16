package ticketpile.service.graphql

import graphql.Scalars
import graphql.schema.GraphQLFieldDefinition
import graphql.schema.GraphQLObjectType
import ticketpile.service.model.AddOnAdjustment
import ticketpile.service.model.DiscountAdjustment
import ticketpile.service.model.FeeAdjustment
import ticketpile.service.model.ManualAdjustment

/**
 * Mappings for [Adjustment] types to [GraphQLObjectType]s
 * 
 * Created by jonlatane on 10/15/16.
 */
val discountAdjustment: GraphQLObjectType =
        GraphQLObjectType.newObject().name("DiscountAdjustment")
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLInt)
                        .name("promotionId")
                        .dataFetcher {
                            (it.source as DiscountAdjustment<*>).discount.externalId!!
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString)
                        .name("code")
                        .dataFetcher {
                            (it.source as DiscountAdjustment<*>).discount.name
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString)
                        .name("description")
                        .dataFetcher {
                            (it.source as DiscountAdjustment<*>).discount.description
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition().type(GraphQLBigDecimal)
                        .name("amount")
                        .dataFetcher {
                            (it.source as DiscountAdjustment<*>).amount
                        }
                        .build())
                .build()



val addOnAdjustment: GraphQLObjectType =
        GraphQLObjectType.newObject().name("AddOnAdjustment")
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString)
                        .name("name")
                        .dataFetcher {
                            (it.source as AddOnAdjustment<*>).addOn.name
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString)
                        .name("selection")
                        .dataFetcher {
                            (it.source as AddOnAdjustment<*>).selection
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition().type(GraphQLBigDecimal)
                        .name("amount")
                        .dataFetcher {
                            (it.source as AddOnAdjustment<*>).amount
                        }
                        .build())
                .build()

val manualAdjustment: GraphQLObjectType =
        GraphQLObjectType.newObject().name("ManualAdjustment")
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString)
                        .name("description")
                        .dataFetcher {
                            (it.source as ManualAdjustment<*>).description
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition().type(GraphQLBigDecimal)
                        .name("amount")
                        .dataFetcher {
                            (it.source as ManualAdjustment<*>).amount
                        }
                        .build())
                .build()

val feeAdjustment : GraphQLObjectType =
        GraphQLObjectType.newObject().name("FeeAdjustment")
                .field(GraphQLFieldDefinition.newFieldDefinition().type(Scalars.GraphQLString)
                        .name("description")
                        .dataFetcher {
                            (it.source as FeeAdjustment<*>).description
                        }
                        .build())
                .field(GraphQLFieldDefinition.newFieldDefinition().type(GraphQLBigDecimal)
                        .name("amount")
                        .dataFetcher {
                            (it.source as FeeAdjustment<*>).amount
                        }
                        .build())
                .build()