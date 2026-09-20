package com.mediawebapp.controller;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediawebapp.exception.GlobalExceptionHandler;
import com.mediawebapp.security.TestSecurityConfig;
import com.mediawebapp.service.GenreService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(GenreController.class)
@Import({GlobalExceptionHandler.class, TestSecurityConfig.class})
class GenreControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private GenreService genreService;

	@Test
	void listCatalogGenres_returnsDistinctNames() throws Exception {
		when(genreService.listDistinctCatalogNames()).thenReturn(List.of("Action", "Drama"));

		mockMvc.perform(get("/api/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$[0]").value("Action"))
				.andExpect(jsonPath("$[1]").value("Drama"));
	}

	@Test
	void listCatalogGenres_emptyCatalog() throws Exception {
		when(genreService.listDistinctCatalogNames()).thenReturn(List.of());

		mockMvc.perform(get("/api/genres"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").isArray())
				.andExpect(jsonPath("$").isEmpty());
	}
}
