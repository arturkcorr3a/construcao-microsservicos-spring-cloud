package com.example.pecas;

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
class PecasApiIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private MeterRegistry meterRegistry;

	private double cadastros(String resultado) {
		return meterRegistry.get("pecas.cadastro").tag("resultado", resultado).counter().count();
	}

	@Test
	@DisplayName("Fluxo completo: cadastrar, consultar por id e nome, rejeitar duplicado e contar métricas")
	void fluxoCompleto() throws Exception {
		// Arrange
		String json = """
				{"id": 100, "nome": "Engrenagem Helicoidal", "descricao": "Aço"}
				""";
		double sucessosAntes = cadastros("sucesso");
		double duplicadosAntes = cadastros("duplicado");

		// Act & Assert: cadastro
		mockMvc.perform(post("/pecas").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated());

		// consulta por id
		mockMvc.perform(get("/pecas/{id}", 100))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.nome").value("Engrenagem Helicoidal"));

		// consulta por nome (trecho, sem diferenciar maiusculas)
		mockMvc.perform(get("/pecas/busca").param("nome", "HELICOIDAL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(100));

		// listagem
		mockMvc.perform(get("/pecas"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[?(@.id == 100)]").exists());

		// duplicado
		mockMvc.perform(post("/pecas").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isConflict());

		// metricas customizadas
		assertThat(cadastros("sucesso")).isEqualTo(sucessosAntes + 1);
		assertThat(cadastros("duplicado")).isEqualTo(duplicadosAntes + 1);
	}

	@Test
	@DisplayName("Consultar id inexistente deve responder 404 com todas as camadas reais")
	void consultarInexistente_retorna404() throws Exception {
		mockMvc.perform(get("/pecas/{id}", 987654)).andExpect(status().isNotFound());
	}

}
