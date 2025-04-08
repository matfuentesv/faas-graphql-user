package cl.veterinary;


import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("deprecation")
public class FunctionGraphQL {
    private static GraphQL graphQL;

    static {
        // Definimos el Schema GraphQL
        var bookType = GraphQLObjectType.newObject()
                .name("Book")
                .field(field -> field
                        .name("title")
                        .type(graphql.Scalars.GraphQLString))
                .field(field -> field
                        .name("author")
                        .type(graphql.Scalars.GraphQLString))
                .build();

        // Data dummy para los libros, se reemplazaría por una lista obtenida desde BD
        var books = List.of(
                Map.of("title", "Book 1", "author", "Author A"),
                Map.of("title", "Book 2", "author", "Author A"),
                Map.of("title", "Book 3", "author", "Author B")
        );

        // Simulamos obtener los libros por autor desde una BD filtrando por autor
        DataFetcher<List<Map<String, String>>> booksDataFetcher = environment -> {
            String author = environment.getArgument("author");
            return books.stream()
                    .filter(book -> book.get("author").equals(author))
                    .collect(Collectors.toList());
        };

        var queryType = GraphQLObjectType.newObject()
                .name("Query")
                .field(field -> field
                        .name("booksByAuthor")
                        .type(new GraphQLList(bookType))
                        .argument(arg -> arg
                                .name("author")
                                .type(graphql.Scalars.GraphQLString))
                        .dataFetcher(booksDataFetcher))
                .build();

        var schema = GraphQLSchema.newSchema()
                .query(queryType)
                .build();

        graphQL = GraphQL.newGraphQL(schema).build();
    }

    @FunctionName("graphql")
    public HttpResponseMessage run(
            @HttpTrigger(name = "req", methods = {HttpMethod.POST}, authLevel = AuthorizationLevel.ANONYMOUS)
            HttpRequestMessage<Map<String, Object>> request,
            final ExecutionContext context) {

        String query = (String) request.getBody().get("query");
        ExecutionInput executionInput = ExecutionInput.newExecutionInput()
                .query(query)
                .build();

        Map<String, Object> result = graphQL.execute(executionInput).toSpecification();

        return request.createResponseBuilder(HttpStatus.OK)
                .body(result)
                .build();
    }

}
