package com.example.representantes.controller;

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

import com.example.representantes.model.Representante;
import com.example.representantes.service.RepresentanteJaCadastradoException;
import com.example.representantes.service.RepresentanteService;

/** Teste de integracao da camada web: Spring MVC real (rotas, JSON, validacao), servico mockado. */
@WebMvcTest(RepresentanteController.class)
class RepresentanteControllerWebMvcIT {

	private static final String CPF = "12345678901";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private RepresentanteService service;

	@Test
	@DisplayName("POST /representantes válido deve responder 201 com Location e JSON do representante")
	void post_representanteValido_retorna201() throws Exception {
		// Arrange
		when(service.cadastrar(any(Representante.class))).thenReturn(new Representante(CPF, "Carlos Lima"));

		// Act & Assert
		mockMvc.perform(post("/representantes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": "12345678901", "nome": "Carlos Lima"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/representantes/" + CPF))
				.andExpect(jsonPath("$.cpf").value(CPF))
				.andExpect(jsonPath("$.nome").value("Carlos Lima"));
	}

	@Test
	@DisplayName("POST /representantes sem nome deve responder 400 sem chamar o serviço")
	void post_semNome_retorna400() throws Exception {
		mockMvc.perform(post("/representantes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": "12345678901"}
								"""))
				.andExpect(status().isBadRequest());

		verify(service, never()).cadastrar(any());
	}

	@Test
	@DisplayName("POST /representantes com CPF em branco deve responder 400")
	void post_cpfEmBranco_retorna400() throws Exception {
		mockMvc.perform(post("/representantes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": " ", "nome": "Carlos Lima"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /representantes duplicado deve responder 409 com a mensagem")
	void post_duplicado_retorna409() throws Exception {
		// Arrange
		when(service.cadastrar(any(Representante.class))).thenThrow(new RepresentanteJaCadastradoException(CPF));

		// Act & Assert
		mockMvc.perform(post("/representantes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": "12345678901", "nome": "Carlos Lima"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString(CPF)));
	}

	@Test
	@DisplayName("GET /representantes/{cpf} deve responder 200 com o representante")
	void getPorCpf_existente_retorna200() throws Exception {
		// Arrange
		when(service.buscarPorCpf(CPF)).thenReturn(Optional.of(new Representante(CPF, "Carlos Lima")));

		// Act & Assert
		mockMvc.perform(get("/representantes/{cpf}", CPF))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Carlos Lima"));
	}

	@Test
	@DisplayName("GET /representantes/{cpf} inexistente deve responder 404")
	void getPorCpf_inexistente_retorna404() throws Exception {
		// Arrange
		when(service.buscarPorCpf("000")).thenReturn(Optional.empty());

		// Act & Assert
		mockMvc.perform(get("/representantes/{cpf}", "000")).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /representantes deve responder a lista em JSON")
	void getLista_retornaArray() throws Exception {
		// Arrange
		when(service.listarTodos()).thenReturn(List.of(new Representante(CPF, "Carlos Lima"), new Representante("98765432100", "Ana Pereira")));

		// Act & Assert
		mockMvc.perform(get("/representantes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].nome").value("Ana Pereira"));
	}

	@Test
	@DisplayName("GET /representantes/busca?nome= deve repassar o termo e responder a lista")
	void getBusca_repassaTermo() throws Exception {
		// Arrange
		when(service.buscarPorNome("lima")).thenReturn(List.of(new Representante(CPF, "Carlos Lima")));

		// Act & Assert
		mockMvc.perform(get("/representantes/busca").param("nome", "lima"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cpf").value(CPF));
	}

}
