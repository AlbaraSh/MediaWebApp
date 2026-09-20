package com.mediawebapp.external.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.mediawebapp.config.OpenAiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiEmbeddingAdapterTest {

	private MockRestServiceServer server;
	private OpenAiEmbeddingAdapter adapter;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder();
		server = MockRestServiceServer.bindTo(builder).build();
		adapter = new OpenAiEmbeddingAdapter(
				builder, new OpenAiProperties("test-openai-key", "https://api.openai.com/v1"));
	}

	@Test
	void createEmbedding_postsToOpenAiAndParsesVector() {
		server.expect(requestTo("https://api.openai.com/v1/embeddings"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-openai-key"))
				.andRespond(withSuccess("""
						{"data":[{"embedding":[0.1,0.2,0.3]}]}
						""", MediaType.APPLICATION_JSON));

		float[] embedding = adapter.createEmbedding("Dune. Desert planet");

		server.verify();
		assertThat(embedding).containsExactly(0.1f, 0.2f, 0.3f);
	}

	@Test
	void createEmbedding_throwsWhenOpenAiFails() {
		server.expect(requestTo("https://api.openai.com/v1/embeddings"))
				.andExpect(method(HttpMethod.POST))
				.andRespond(withServerError());

		assertThatThrownBy(() -> adapter.createEmbedding("Dune"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("OpenAI");
	}

	@Test
	void createEmbedding_throwsWhenApiKeyMissing() {
		adapter = new OpenAiEmbeddingAdapter(
				RestClient.builder(), new OpenAiProperties(" ", "https://api.openai.com/v1"));

		assertThatThrownBy(() -> adapter.createEmbedding("Dune"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("not configured");
	}
}
