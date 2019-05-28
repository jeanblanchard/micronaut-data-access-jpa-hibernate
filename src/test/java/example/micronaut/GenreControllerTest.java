package example.micronaut;

import example.micronaut.domain.Genre;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.RxHttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MicronautTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

@MicronautTest
public class GenreControllerTest {

    @Inject
    @Client("/")
    protected RxHttpClient client;

    @Test
    public void supplyAnInvalidOrderTriggersValidationFailure() {
        HttpRequest<Void> request = HttpRequest.GET("/genres/list?order=foo");
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().retrieve(request));
        assertThat(exception).hasFieldOrPropertyWithValue("status", HttpStatus.BAD_REQUEST);
    }

    @Test
    public void testFindNonExistingGenreReturns404() {
        HttpRequest<Void> request = HttpRequest.GET("/genres/99");
        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () ->
                client.toBlocking().retrieve(request));
        assertThat(exception).hasFieldOrPropertyWithValue("status", HttpStatus.NOT_FOUND);
    }

    @Test
    public void testGenreCrudOperations() {

        List<Long> genreIds = new ArrayList<>();

        HttpRequest<?> request = HttpRequest.POST("/genres", new GenreSaveCommand("DevOps")); // <3>
        HttpResponse<?> response = client.toBlocking().exchange(request);
        genreIds.add(entityId(response));

        assertThat(response).hasFieldOrPropertyWithValue("status", HttpStatus.CREATED);

        request = HttpRequest.POST("/genres", new GenreSaveCommand("Microservices")); // <3>
        response = client.toBlocking().exchange(request);

        assertThat(response).hasFieldOrPropertyWithValue("status", HttpStatus.CREATED);

        Long id = entityId(response);
        genreIds.add(id);
        request = HttpRequest.GET("/genres/"+id);

        Genre genre = client.toBlocking().retrieve(request, Genre.class); // <4>


        assertThat(genre).hasFieldOrPropertyWithValue("name", "Microservices");

        request = HttpRequest.PUT("/genres", new GenreUpdateCommand(id, "Micro-services"));
        response = client.toBlocking().exchange(request);  // <5>

        assertThat(response).hasFieldOrPropertyWithValue("status", HttpStatus.NO_CONTENT);

        request = HttpRequest.GET("/genres/" + id);
        genre = client.toBlocking().retrieve(request, Genre.class);
        assertThat(genre).hasFieldOrPropertyWithValue("name", "Micro-services");

        request = HttpRequest.GET("/genres/list");
        List<Genre> genres = client.toBlocking().retrieve(request, Argument.listOf(Genre.class));

        assertThat(genres).hasSize(2);

        request = HttpRequest.GET("/genres/list?max=1");
        genres = client.toBlocking().retrieve(request, Argument.listOf(Genre.class));

        assertThat(genres)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(g -> assertThat(g).hasFieldOrPropertyWithValue("name", "DevOps"));

        request = HttpRequest.GET("/genres/list?max=1&order=desc&sort=name");
        genres = client.toBlocking().retrieve(request, Argument.listOf(Genre.class));

        assertThat(genres)
                .hasSize(1)
                .hasOnlyOneElementSatisfying(g -> assertThat(g).hasFieldOrPropertyWithValue("name", "Micro-services"));

        request = HttpRequest.GET("/genres/list?max=1&offset=10");
        genres = client.toBlocking().retrieve(request, Argument.listOf(Genre.class));

        assertThat(genres).isEmpty();

        // cleanup:
        for (Long genreId : genreIds) {
            request = HttpRequest.DELETE("/genres/"+genreId);
            response = client.toBlocking().exchange(request);
            assertThat(response).hasFieldOrPropertyWithValue("status", HttpStatus.NO_CONTENT);
        }
    }

    protected Long entityId(HttpResponse<?> response) {
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
