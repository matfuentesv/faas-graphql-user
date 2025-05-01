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
import graphql.schema.*;

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

        GraphQLObjectType rolType = GraphQLObjectType.newObject()
                .name("Rol")
                .field(f -> f.name("id").type(Scalars.GraphQLID))
                .field(f -> f.name("nombre").type(Scalars.GraphQLString))
                .field(f -> f.name("descripcion").type(Scalars.GraphQLString))
                .build();

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
                .field(f -> f.name("rol").type(rolType))
                .build();

        GraphQLInputObjectType userInput = GraphQLInputObjectType.newInputObject()
                .name("UserInput")
                .field(f -> f.name("id").type(Scalars.GraphQLID)) // opcional para update
                .field(f -> f.name("nombre").type(Scalars.GraphQLString))
                .field(f -> f.name("apellidoPaterno").type(Scalars.GraphQLString))
                .field(f -> f.name("apellidoMaterno").type(Scalars.GraphQLString))
                .field(f -> f.name("rut").type(Scalars.GraphQLString))
                .field(f -> f.name("direccion").type(Scalars.GraphQLString))
                .field(f -> f.name("celular").type(Scalars.GraphQLString))
                .field(f -> f.name("email").type(Scalars.GraphQLString))
                .field(f -> f.name("password").type(Scalars.GraphQLString))
                .field(f -> f.name("activo").type(Scalars.GraphQLBoolean))
                .field(f -> f.name("rol").type(GraphQLInputObjectType.newInputObject()
                        .name("RolInput")
                        .field(r -> r.name("id").type(Scalars.GraphQLID))
                        .build()))
                .build();

        // Query fetchers
        DataFetcher<List<User>> userDataFetcher = env -> userService.findAll();
        DataFetcher<User> userByIdDataFetcher = env -> {
            Long id = Long.parseLong(env.getArgument("id"));
            return userService.findUserById(id).orElse(null);
        };

        // Mutation fetchers
        DataFetcher<User> saveUserFetcher = env -> {
            Map<String, Object> input = env.getArgument("input");
            return userService.saveUser(mapUser(input));
        };

        DataFetcher<User> updateUserFetcher = env -> {
            Map<String, Object> input = env.getArgument("input");
            return userService.updateUser(mapUser(input));
        };

        DataFetcher<String> deleteUserFetcher = env -> {
            Long id = Long.parseLong(env.getArgument("id"));
            userService.deleteUser(id);
            return "Usuario eliminado con ID: " + id;
        };

        // Query type
        GraphQLObjectType queryType = GraphQLObjectType.newObject()
                .name("Query")
                .field(f -> f
                        .name("findAll")
                        .type(GraphQLList.list(userType))
                        .dataFetcher(userDataFetcher))
                .field(f -> f
                        .name("findUserById")
                        .type(userType)
                        .argument(arg -> arg.name("id").type(Scalars.GraphQLID))
                        .dataFetcher(userByIdDataFetcher))
                .build();

        // Mutation type
        GraphQLObjectType mutationType = GraphQLObjectType.newObject()
                .name("Mutation")
                .field(f -> f
                        .name("saveUser")
                        .type(userType)
                        .argument(arg -> arg.name("input").type(userInput))
                        .dataFetcher(saveUserFetcher))
                .field(f -> f
                        .name("updateUser")
                        .type(userType)
                        .argument(arg -> arg.name("input").type(userInput))
                        .dataFetcher(updateUserFetcher))
                .field(f -> f
                        .name("deleteUser")
                        .type(Scalars.GraphQLString)
                        .argument(arg -> arg.name("id").type(Scalars.GraphQLID))
                        .dataFetcher(deleteUserFetcher))
                .build();

        // Schema completo
        GraphQLSchema schema = GraphQLSchema.newSchema()
                .query(queryType)
                .mutation(mutationType)
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

    private static User mapUser(Map<String, Object> input) {
        User user = new User();
        if (input.get("id") != null)
            user.setId(Long.parseLong(input.get("id").toString()));
        user.setNombre((String) input.get("nombre"));
        user.setApellidoPaterno((String) input.get("apellidoPaterno"));
        user.setApellidoMaterno((String) input.get("apellidoMaterno"));
        user.setRut((String) input.get("rut"));
        user.setDireccion((String) input.get("direccion"));
        user.setCelular((String) input.get("celular"));
        user.setEmail((String) input.get("email"));
        user.setPassword((String) input.get("password"));
        user.setActivo((Boolean) input.get("activo"));

        if (input.get("rol") instanceof Map<?, ?> rolMap) {
            Rol rol = new Rol();
            rol.setId(Long.parseLong(rolMap.get("id").toString()));
            user.setRol(rol);
        }

        return user;
    }
}
