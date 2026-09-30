package com.example.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.micrometer.core.instrument.MeterRegistry;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Teste de integracao completo: contexto inteiro (controller + servico + JPA + H2 + metricas). */
@SpringBootTest
@AutoConfigureMockMvc
class ClientesApiIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeterRegistry meterRegistry;

	private double cadastros(String resultado) {
		return meterRegistry.get("clientes.cadastro").tag("resultado", resultado).counter().count();
	}

	@Test
	@DisplayName("Fluxo completo: cadastrar, consultar por CPF e nome, rejeitar duplicado e contar métricas")
	void fluxoCompleto() throws Exception {
		// Arrange
		String json = """
				{"cpf": "55566677788", "nome": "Beatriz Albuquerque"}
				""";
		double sucessosAntes = cadastros("sucesso");
		double duplicadosAntes = cadastros("duplicado");

		// Act & Assert: cadastro
		mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated());

		// consulta por CPF
		mockMvc.perform(get("/clientes/{cpf}", "55566677788"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Beatriz Albuquerque"));

		// consulta por nome (trecho, sem diferenciar maiusculas)
		mockMvc.perform(get("/clientes/busca").param("nome", "ALBUQUERQUE"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].cpf").value("55566677788"));

		// listagem
		mockMvc.perform(get("/clientes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.cpf == '55566677788')]").exists());

		// duplicado
		mockMvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isConflict());

		// metricas customizadas
		assertThat(cadastros("sucesso")).isEqualTo(sucessosAntes + 1);
		assertThat(cadastros("duplicado")).isEqualTo(duplicadosAntes + 1);
	}

	@Test
	@DisplayName("Consultar CPF inexistente deve responder 404 com todas as camadas reais")
	void consultarInexistente_retorna404() throws Exception {
		mockMvc.perform(get("/clientes/{cpf}", "00000000000")).andExpect(status().isNotFound());
	}

}
