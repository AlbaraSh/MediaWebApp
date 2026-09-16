package com.mediawebapp.external.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mediawebapp.config.JikanProperties;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.dto.jikan.JikanAnimeDetailsResponse;
import com.mediawebapp.external.dto.jikan.JikanSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JikanAdapterTest {

	private MockRestServiceServer server;
	private JikanAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new JikanAdapter(builder, new JikanProperties("https://api.jikan.moe/v4"));
	}

	@Test
	void searchAnime_parsesResults() {
		server.expect(requestTo(startsWith("https://api.jikan.moe/v4/anime")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("q", "Cowboy%20Bebop"))
				.andExpect(queryParam("limit", "10"))
				.andRespond(withSuccess("""
						{
						  "data": [
						    {
						      "mal_id": 1,
						      "title": "Cowboy Bebop",
						      "synopsis": "The futuristic misadventures.",
						      "year": 1998,
						      "aired": { "from": "1998-04-03T00:00:00+00:00" }
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		JikanSearchResponse response = adapter.searchAnime("Cowboy Bebop");

		server.verify();
		assertThat(response.data()).hasSize(1);
		assertThat(response.data().get(0).malId()).isEqualTo(1);
		assertThat(response.data().get(0).title()).isEqualTo("Cowboy Bebop");
		assertThat(response.data().get(0).year()).isEqualTo(1998);
	}

	@Test
	void getAnime_parsesDetails() {
		server.expect(requestTo("https://api.jikan.moe/v4/anime/1"))
				.andRespond(withSuccess("""
						{
						  "data": {
						    "mal_id": 1,
						    "title": "Cowboy Bebop",
						    "synopsis": "The futuristic misadventures.",
						    "year": 1998
						  }
						}
						""", MediaType.APPLICATION_JSON));

		JikanAnimeDetailsResponse response = adapter.getAnime("1");

		server.verify();
		assertThat(response.data().title()).isEqualTo("Cowboy Bebop");
	}

	@Test
	void getAnime_whenNotFound_throwsResourceNotFound() {
		server.expect(requestTo("https://api.jikan.moe/v4/anime/0"))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> adapter.getAnime("0"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("0");
	}

	@Test
	void searchAnime_whenProviderFails_throwsExternalProviderException() {
		server.expect(requestTo(startsWith("https://api.jikan.moe/v4/anime")))
				.andRespond(withServerError());

		assertThatThrownBy(() -> adapter.searchAnime("naruto"))
				.isInstanceOf(ExternalProviderException.class)
				.hasMessage("Jikan provider is unavailable");
	}
}
