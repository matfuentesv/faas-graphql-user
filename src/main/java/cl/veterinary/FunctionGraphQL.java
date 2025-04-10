package cl.veterinary;


import cl.veterinary.model.Rol;
import cl.veterinary.model.User;
import cl.veterinary.service.UserService;
import com.microsoft.azure.functions.*;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.Scalars;
import graphql.schema.DataFetcher;
import graphql.schema.GraphQLList;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import java.util.List;
import java.util.Map;


public class FunctionGraphQL {

    private static final GraphQL graphQL;

    static {

        final ApplicationContext context =
                new SpringApplicationBuilder(SpringAzureApp.class).run();

        final UserService userService =
                context.getBean(UserService.class);

        // Tipo GraphQL para Rol
        GraphQLObjectType userType = GraphQLObjectType.newObject()
                .name("User")
                .field(f -> f.name("id").type(Scalars.GraphQLID))
                .field(f -> f.name("nombre").type(Scalars.GraphQLString))
                .field(f -> f.name("apellidoPaterno").type(Scalars.GraphQLString))
                .field(f -> f.name("apellidoMaterno").type(Scalars.GraphQLString))
                .field(f -> f.name("rut").type(Scalars.GraphQLString))
                .field(f -> f.name("direccion").type(Scalars.GraphQLString))
                .field(f -> f.name("celular").type(Scalars.GraphQLString))
                .field(f -> f.name("email").type(Scalars.GraphQLString))
                .field(f -> f.name("password").type(Scalars.GraphQLString))
                .field(f -> f.name("activo").type(Scalars.GraphQLBoolean))
                .build();


        DataFetcher<List<User>> userDataFetcher = env -> userService.findAll();
        DataFetcher<User> userByIdDataFetcher = env -> {
            Long id = Long.parseLong(env.getArgument("id"));
            return userService.findUserById(id).orElse(null);
        };


        GraphQLObjectType queryType = GraphQLObjectType.newObject()
                .name("Query")
                .field(f -> f
                        .name("findAll")
                        .type(GraphQLList.list(userType))
                        .dataFetcher(userDataFetcher))
                .field(f -> f
                        .name("findUserById")
                        .type(userType)
                        .argument(arg -> arg
                                .name("id")
                                .type(Scalars.GraphQLID))
                        .dataFetcher(userByIdDataFetcher))
                .build();

        GraphQLSchema schema = GraphQLSchema.newSchema()
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
                .header("Content-Type", "application/json")
                .body(result)
                .build();
    }
}
