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

import com.mediawebapp.config.RawgProperties;
import com.mediawebapp.exception.ExternalProviderException;
import com.mediawebapp.exception.ResourceNotFoundException;
import com.mediawebapp.external.dto.rawg.RawgGame;
import com.mediawebapp.external.dto.rawg.RawgSearchResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class RawgAdapterTest {

	private MockRestServiceServer server;
	private RawgAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new RawgAdapter(builder, new RawgProperties("test-rawg-key", "https://api.rawg.io/api"));
	}

	@Test
	void searchGames_parsesResults() {
		server.expect(requestTo(startsWith("https://api.rawg.io/api/games")))
				.andExpect(method(HttpMethod.GET))
				.andExpect(queryParam("search", "GTA"))
				.andExpect(queryParam("page_size", "10"))
				.andExpect(queryParam("key", "test-rawg-key"))
				.andRespond(withSuccess("""
						{
						  "results": [
						    {
						      "id": 3498,
						      "name": "Grand Theft Auto V",
						      "released": "2013-09-17"
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		RawgSearchResponse response = adapter.searchGames("GTA");

		server.verify();
		assertThat(response.results()).hasSize(1);
		assertThat(response.results().get(0).id()).isEqualTo(3498);
		assertThat(response.results().get(0).name()).isEqualTo("Grand Theft Auto V");
		assertThat(response.results().get(0).released()).isEqualTo("2013-09-17");
		assertThat(response.results().get(0).descriptionRaw()).isNull();
	}

	@Test
	void getGame_parsesDescriptionRaw() {
		server.expect(requestTo(startsWith("https://api.rawg.io/api/games/3498")))
				.andExpect(queryParam("key", "test-rawg-key"))
				.andRespond(withSuccess("""
						{
						  "id": 3498,
						  "name": "Grand Theft Auto V",
						  "released": "2013-09-17",
						  "description": "<p>HTML should be ignored</p>",
						  "description_raw": "Plain text synopsis"
						}
						""", MediaType.APPLICATION_JSON));

		RawgGame game = adapter.getGame("3498");

		server.verify();
		assertThat(game.descriptionRaw()).isEqualTo("Plain text synopsis");
	}

	@Test
	void getGame_whenNotFound_throwsResourceNotFound() {
		server.expect(requestTo(startsWith("https://api.rawg.io/api/games/0")))
				.andRespond(withStatus(HttpStatus.NOT_FOUND));

		assertThatThrownBy(() -> adapter.getGame("0"))
				.isInstanceOf(ResourceNotFoundException.class)
				.hasMessageContaining("0");
	}

	@Test
	void searchGames_whenProviderFails_throwsExternalProviderException() {
		server.expect(requestTo(startsWith("https://api.rawg.io/api/games")))
				.andRespond(withServerError());

		assertThatThrownBy(() -> adapter.searchGames("halo"))
				.isInstanceOf(ExternalProviderException.class)
				.hasMessage("RAWG provider is unavailable");
	}
}
