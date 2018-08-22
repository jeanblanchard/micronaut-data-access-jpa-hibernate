package example.micronaut;

import example.micronaut.domain.Genre;
import example.micronaut.genre.GenreSaveCommand;
import example.micronaut.genre.GenreUpdateCommand;
import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class GenreControllerTest {

    private static EmbeddedServer server; // <1>
    private static HttpClient client; // <2>

    @BeforeClass
    public static void setupServer() {
        server = ApplicationContext.run(EmbeddedServer.class); // <1>
        client = server.getApplicationContext().createBean(HttpClient.class, server.getURL()); // <2>
    }

    @AfterClass
    public static void stopServer() {
        if (server != null) {
            server.stop();
        }
        if (client != null) {
            client.stop();
        }
    }

    @Test
    public void testGenreCrudOperations() {
        HttpRequest request = HttpRequest.POST("/genres", new GenreSaveCommand("Microservices")); // <3>
        HttpResponse response = client.toBlocking().exchange(request);

        assertEquals(HttpStatus.CREATED, response.getStatus());

        Long id = entityId(response);
        request = HttpRequest.GET("/genres/"+id);
        Genre genre = client.toBlocking().retrieve(request, Genre.class); // <4>

        assertEquals("Microservices",  genre.getName());

        request = HttpRequest.PUT("/genres/", new GenreUpdateCommand(id, "Micro-services"));
        response = client.toBlocking().exchange(request);  // <5>

        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());

        id = entityId(response);

        request = HttpRequest.GET("/genres/"+id);
        genre = client.toBlocking().retrieve(request, Genre.class);
        assertEquals("Micro-services",  genre.getName());

        request = HttpRequest.GET("/genres");
        List<Genre> genres = client.toBlocking().retrieve(request, Argument.of(List.class, Genre.class));

        assertEquals(1, genres.size());

        // cleanup:
        request = HttpRequest.DELETE("/genres/"+id);
        response = client.toBlocking().exchange(request);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatus());
    }

    Long entityId(HttpResponse response) {
        String path = "/genres/";
        String value = response.header(HttpHeaders.LOCATION);
        if ( value == null) {
            return null;
        }
        int index = value.indexOf(path);
        if ( index != -1) {
            return Long.valueOf(value.substring(index + path.length()));
        }
        return null;
    }
}