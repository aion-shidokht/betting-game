package server;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import state.ProjectedState;
import util.QueuePopulator;

import java.net.URI;
import java.util.Optional;

public class SimpleHttpServer {

    private static final String BASE_URI;
    private static final String protocol;
    private static final Optional<String> host;
    private static final String path;
    private static final Optional<String> port;

    static {
        protocol = "http://";
        host = Optional.ofNullable(System.getenv("HOSTNAME"));
        port = Optional.ofNullable(System.getenv("PORT"));
        path = "bettingOAP";
        BASE_URI = protocol + host.orElse("localhost") + ":" + port.orElse("8025") + "/" + path + "/";
    }


    public static HttpServer startServer(ProjectedState projectedState, QueuePopulator queuePopulator) {
        // create a resource config that scans for JAX-RS resources and providers
        final ResourceConfig rc = createResourceConfig(projectedState, queuePopulator);

        // create and start a new instance of grizzly http server
        // exposing the Jersey application at BASE_URI
        return GrizzlyHttpServerFactory.createHttpServer(URI.create(BASE_URI), rc);
    }

    private static ResourceConfig createResourceConfig(ProjectedState projectedState, QueuePopulator queuePopulator) {
        ResourceConfig resourceConfig = new ResourceConfig();
        resourceConfig.packages("server");
        resourceConfig.register(new AbstractBinder() {
            @Override
            public void configure() {
                bind(projectedState).to(ProjectedState.class);
                bind(queuePopulator).to(QueuePopulator.class);
            }
        });
        return resourceConfig;
    }

    public static String getBaseUri() {
        return BASE_URI;
    }
}