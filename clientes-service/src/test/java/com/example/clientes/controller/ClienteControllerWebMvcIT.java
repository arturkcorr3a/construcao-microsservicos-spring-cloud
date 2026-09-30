package com.example.clientes.controller;

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

import com.example.clientes.model.Cliente;
import com.example.clientes.service.ClienteJaCadastradoException;
import com.example.clientes.service.ClienteService;

/** Teste de integracao da camada web: Spring MVC real (rotas, JSON, validacao), servico mockado. */
@WebMvcTest(ClienteController.class)
class ClienteControllerWebMvcIT {

	private static final String CPF = "12345678901";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ClienteService service;

	@Test
	@DisplayName("POST /clientes válido deve responder 201 com Location e JSON do cliente")
	void post_clienteValido_retorna201() throws Exception {
		// Arrange
		when(service.cadastrar(any(Cliente.class))).thenReturn(new Cliente(CPF, "Maria Silva"));

		// Act & Assert
		mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": "12345678901", "nome": "Maria Silva"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", "/clientes/" + CPF))
				.andExpect(jsonPath("$.cpf").value(CPF))
				.andExpect(jsonPath("$.nome").value("Maria Silva"));
	}

	@Test
	@DisplayName("POST /clientes sem nome deve responder 400 sem chamar o serviço")
	void post_semNome_retorna400() throws Exception {
		mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": "12345678901"}
								"""))
				.andExpect(status().isBadRequest());

		verify(service, never()).cadastrar(any());
	}

	@Test
	@DisplayName("POST /clientes com CPF em branco deve responder 400")
	void post_cpfEmBranco_retorna400() throws Exception {
		mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": " ", "nome": "Maria Silva"}
								"""))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("POST /clientes duplicado deve responder 409 com a mensagem")
	void post_duplicado_retorna409() throws Exception {
		// Arrange
		when(service.cadastrar(any(Cliente.class))).thenThrow(new ClienteJaCadastradoException(CPF));

		// Act & Assert
		mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"cpf": "12345678901", "nome": "Maria Silva"}
								"""))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.detail", containsString(CPF)));
	}

	@Test
	@DisplayName("GET /clientes/{cpf} deve responder 200 com o cliente")
	void getPorCpf_existente_retorna200() throws Exception {
		// Arrange
		when(service.buscarPorCpf(CPF)).thenReturn(Optional.of(new Cliente(CPF, "Maria Silva")));

		// Act & Assert
		mockMvc.perform(get("/clientes/{cpf}", CPF))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Maria Silva"));
	}

	@Test
	@DisplayName("GET /clientes/{cpf} inexistente deve responder 404")
	void getPorCpf_inexistente_retorna404() throws Exception {
		// Arrange
		when(service.buscarPorCpf("000")).thenReturn(Optional.empty());

		// Act & Assert
		mockMvc.perform(get("/clientes/{cpf}", "000")).andExpect(status().isNotFound());
	}

	@Test
	@DisplayName("GET /clientes deve responder a lista em JSON")
	void getLista_retornaArray() throws Exception {
		// Arrange
		when(service.listarTodos()).thenReturn(List.of(new Cliente(CPF, "Maria Silva"), new Cliente("98765432100", "Joao Souza")));

		// Act & Assert
		mockMvc.perform(get("/clientes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.length()").value(2))
				.andExpect(jsonPath("$[1].nome").value("Joao Souza"));
	}

	@Test
	@DisplayName("GET /clientes/busca?nome= deve repassar o termo e responder a lista")
	void getBusca_repassaTermo() throws Exception {
		// Arrange
		when(service.buscarPorNome("silva")).thenReturn(List.of(new Cliente(CPF, "Maria Silva")));

		// Act & Assert
		mockMvc.perform(get("/clientes/busca").param("nome", "silva"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cpf").value(CPF));
	}

}
