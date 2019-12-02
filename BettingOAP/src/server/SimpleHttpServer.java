package server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import state.ProjectedState;
import state.UserState;
import util.QueuePopulator;

import java.net.URI;
import java.util.Optional;

public class SimpleHttpServer {

    private static final String protocol = "http://";
    private static final String path = "bettingOAP";

    public static HttpServer startServer(UserState userState, QueuePopulator queuePopulator, String host, String port) {
        String BASE_URI = getBaseUri(host, port);
        // create a resource config that scans for JAX-RS resources and providers
        final ResourceConfig rc = createResourceConfig(userState, queuePopulator);

        // create a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc, false);
    }

    private static ResourceConfig createResourceConfig(UserState userState, QueuePopulator queuePopulator) {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("server");
        resourceConfig.register(new AbstractBinder() {
            @Override
            public void configure() {
                bind(userState).to(UserState.class);
                bind(queuePopulator).to(QueuePopulator.class);
            }
        });
        return resourceConfig;
    }

    public static String getBaseUri(String host, String port) {
        return protocol + host + ":" + port + "/" + path + "/";
    }
}
