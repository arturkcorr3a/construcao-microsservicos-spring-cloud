package com.example.pecas.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.pecas.model.Peca;
import com.example.pecas.service.PecaJaCadastradaException;
import com.example.pecas.service.PecaService;

/** Teste de integracao da camada web: Spring MVC real (rotas, JSON, validacao), servico mockado. */
@WebMvcTest(PecaController.class)
class PecaControllerWebMvcIT {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private PecaService service;

	@Test
	@DisplayName("POST /pecas válido deve responder 201 com Location e JSON da peça")
	void post_pecaValida_retorna201() throws Exception {
		// Arrange
		when(service.cadastrar(any(Peca.class))).thenReturn(new Peca(1L, "Parafuso", "M8"));

		// Act & Assert
		mockMvc.perform(post("/pecas").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"id": 1, "nome": "Parafuso", "descricao": "M8"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/pecas/1"))
				.andExpect(jsonPath("$.id").value(1))
				.andExpect(jsonPath("$.nome").value("Parafuso"))
				.andExpect(jsonPath("$.descricao").value("M8"));
	}

	@Test
	@DisplayName("POST /pecas sem nome deve responder 400 sem chamar o serviço")
	void post_semNome_retorna400() throws Exception {
		mockMvc.perform(post("/pecas").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"id": 1, "descricao": "M8"}
								"""))
				.andExpect(status().isBadRequest());

		verify(service, never()).cadastrar(any());
	}

	@Test
	@DisplayName("POST /pecas sem id deve responder 400")
	void post_semId_retorna400() throws Exception {
		mockMvc.perform(post("/pecas").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"nome": "Parafuso"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /pecas duplicado deve responder 409 com a mensagem")
	void post_duplicado_retorna409() throws Exception {
		// Arrange
		when(service.cadastrar(any(Peca.class))).thenThrow(new PecaJaCadastradaException(1L));

		// Act & Assert
		mockMvc.perform(post("/pecas").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"id": 1, "nome": "Parafuso"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString("id 1")));
	}

	@Test
	@DisplayName("GET /pecas/{id} deve responder 200 com a peça")
	void getPorId_existente_retorna200() throws Exception {
		// Arrange
		when(service.buscarPorId(1L)).thenReturn(Optional.of(new Peca(1L, "Parafuso", "M8")));

		// Act & Assert
		mockMvc.perform(get("/pecas/{id}", 1))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Parafuso"));
	}

	@Test
	@DisplayName("GET /pecas/{id} inexistente deve responder 404")
	void getPorId_inexistente_retorna404() throws Exception {
		// Arrange
		when(service.buscarPorId(99L)).thenReturn(Optional.empty());

		// Act & Assert
		mockMvc.perform(get("/pecas/{id}", 99)).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /pecas deve responder a lista em JSON")
	void getLista_retornaArray() throws Exception {
		// Arrange
		when(service.listarTodas()).thenReturn(List.of(new Peca(1L, "Parafuso", "M8"), new Peca(2L, "Porca", "M8")));

		// Act & Assert
		mockMvc.perform(get("/pecas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].nome").value("Porca"));
	}

	@Test
	@DisplayName("GET /pecas/busca?nome= deve repassar o termo e responder a lista")
	void getBusca_repassaTermo() throws Exception {
		// Arrange
		when(service.buscarPorNome("paraf")).thenReturn(List.of(new Peca(1L, "Parafuso", "M8")));

		// Act & Assert
		mockMvc.perform(get("/pecas/busca").param("nome", "paraf"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(1));
	}

}
