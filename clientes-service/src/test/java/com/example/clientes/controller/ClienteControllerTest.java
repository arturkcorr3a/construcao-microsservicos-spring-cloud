package com.example.clientes.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import com.example.clientes.model.Cliente;
import com.example.clientes.service.ClienteJaCadastradoException;
import com.example.clientes.service.ClienteService;

/**
 * Teste unitario do controller isolando o framework web: os metodos sao chamados
 * diretamente, sem servlet, MockMvc ou contexto Spring. O servico e mockado.
 */
@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

	private static final String CPF = "12345678901";

	@Mock
	private ClienteService service;

	@InjectMocks
	private ClienteController controller;

	@Test
	@DisplayName("Cadastrar deve responder 201 com Location e o cliente salvo no corpo")
	void cadastrar_clienteValido_retorna201ComLocation() {
		// Arrange
		Cliente cliente = new Cliente(CPF, "Maria Silva");
		when(service.cadastrar(cliente)).thenReturn(cliente);

		// Act
		ResponseEntity<Cliente> resposta = controller.cadastrar(cliente);

		// Assert
		assertEquals(HttpStatus.CREATED, resposta.getStatusCode());
		assertEquals(URI.create("/clientes/" + CPF), resposta.getHeaders().getLocation());
		assertSame(cliente, resposta.getBody());
	}

	@Test
	@DisplayName("Cadastrar deve propagar a exceção de duplicado para o handler")
	void cadastrar_clienteDuplicado_propagaExcecao() {
		// Arrange
		Cliente cliente = new Cliente(CPF, "Maria Silva");
		when(service.cadastrar(cliente)).thenThrow(new ClienteJaCadastradoException(CPF));

		// Act & Assert
		assertThrows(ClienteJaCadastradoException.class, () -> controller.cadastrar(cliente));
	}

	@Test
	@DisplayName("O handler de duplicado deve responder 409 com a mensagem no detalhe")
	void tratarDuplicado_retorna409() {
		// Act
		ResponseEntity<ProblemDetail> resposta = controller.tratarDuplicado(new ClienteJaCadastradoException(CPF));

		// Assert
		assertEquals(HttpStatus.CONFLICT, resposta.getStatusCode());
		assertEquals(409, resposta.getBody().getStatus());
		assertTrue(resposta.getBody().getDetail().contains(CPF));
	}

	@Test
	@DisplayName("Consultar por CPF deve retornar o cliente quando ele existe")
	void consultarPorCpf_cpfExistente_retornaCliente() {
		// Arrange
		Cliente cliente = new Cliente(CPF, "Maria Silva");
		when(service.buscarPorCpf(CPF)).thenReturn(Optional.of(cliente));

		// Act
		Cliente resultado = controller.consultarPorCpf(CPF);

		// Assert
		assertSame(cliente, resultado);
	}

	@Test
	@DisplayName("Consultar por CPF deve lançar 404 quando o cliente não existe")
	void consultarPorCpf_cpfInexistente_lanca404() {
		// Arrange
		when(service.buscarPorCpf("000")).thenReturn(Optional.empty());

		// Act
		ResponseStatusException excecao = assertThrows(ResponseStatusException.class,
				() -> controller.consultarPorCpf("000"));

		// Assert
		assertEquals(HttpStatus.NOT_FOUND, excecao.getStatusCode());
	}

	@Test
	@DisplayName("Listar deve retornar os clientes do serviço")
	void listar_retornaClientesDoServico() {
		// Arrange
		List<Cliente> lista = List.of(new Cliente(CPF, "Maria Silva"));
		when(service.listarTodos()).thenReturn(lista);

		// Act
		List<Cliente> resultado = controller.listar();

		// Assert
		assertEquals(lista, resultado);
	}

	@Test
	@DisplayName("Consultar por nome deve repassar o termo ao serviço")
	void consultarPorNome_repassaTermoAoServico() {
		// Arrange
		List<Cliente> lista = List.of(new Cliente(CPF, "Maria Silva"));
		when(service.buscarPorNome("silva")).thenReturn(lista);

		// Act
		List<Cliente> resultado = controller.consultarPorNome("silva");

		// Assert
		assertEquals(lista, resultado);
		verify(service).buscarPorNome("silva");
	}

}
