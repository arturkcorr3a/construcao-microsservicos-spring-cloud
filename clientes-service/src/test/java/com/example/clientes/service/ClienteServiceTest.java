package com.example.clientes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.clientes.model.Cliente;
import com.example.clientes.repository.ClienteRepository;

/** Teste unitario do servico: repositorio mockado, sem Spring e sem banco. */
@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

	private static final String CPF = "12345678901";

	@Mock
	private ClienteRepository repository;

	private SimpleMeterRegistry meterRegistry;

	private ClienteService service;

	@BeforeEach
	void setUp() {
		// Registry em memoria (real, sem framework) para verificar as metricas;
		// por isso o servico e criado aqui em vez de usar @InjectMocks.
		meterRegistry = new SimpleMeterRegistry();
		service = new ClienteService(repository, meterRegistry);
	}

	private double cadastros(String resultado) {
		return meterRegistry.get("clientes.cadastro").tag("resultado", resultado).counter().count();
	}

	@Test
	@DisplayName("Deve salvar o cliente e contar um cadastro com sucesso quando o CPF é novo")
	void cadastrar_cpfNovo_salvaEContaSucesso() {
		// Arrange
		Cliente cliente = new Cliente(CPF, "Maria Silva");
		when(repository.existePorCpf(CPF)).thenReturn(false);
		when(repository.salvar(cliente)).thenReturn(cliente);

		// Act
		Cliente salvo = service.cadastrar(cliente);

		// Assert
		assertSame(cliente, salvo);
		verify(repository).salvar(cliente);
		assertEquals(1.0, cadastros("sucesso"));
		assertEquals(0.0, cadastros("duplicado"));
	}

	@Test
	@DisplayName("Deve rejeitar o cliente, sem salvar, e contar um duplicado quando o CPF já existe")
	void cadastrar_cpfExistente_lancaExcecaoEContaDuplicado() {
		// Arrange
		Cliente cliente = new Cliente(CPF, "Maria Silva");
		when(repository.existePorCpf(CPF)).thenReturn(true);

		// Act
		ClienteJaCadastradoException excecao = assertThrows(ClienteJaCadastradoException.class,
				() -> service.cadastrar(cliente));

		// Assert
		assertTrue(excecao.getMessage().contains(CPF));
		verify(repository, never()).salvar(any());
		assertEquals(0.0, cadastros("sucesso"));
		assertEquals(1.0, cadastros("duplicado"));
	}

	@Test
	@DisplayName("Deve retornar o cliente quando o CPF existe")
	void buscarPorCpf_cpfExistente_retornaCliente() {
		// Arrange
		Cliente cliente = new Cliente(CPF, "Maria Silva");
		when(repository.buscarPorCpf(CPF)).thenReturn(Optional.of(cliente));

		// Act
		Optional<Cliente> encontrado = service.buscarPorCpf(CPF);

		// Assert
		assertTrue(encontrado.isPresent());
		assertSame(cliente, encontrado.get());
	}

	@Test
	@DisplayName("Deve retornar vazio quando o CPF não existe")
	void buscarPorCpf_cpfInexistente_retornaVazio() {
		// Arrange
		when(repository.buscarPorCpf("000")).thenReturn(Optional.empty());

		// Act
		Optional<Cliente> encontrado = service.buscarPorCpf("000");

		// Assert
		assertTrue(encontrado.isEmpty());
	}

	@Test
	@DisplayName("Deve listar todos os clientes do repositório")
	void listarTodos_retornaClientesDoRepositorio() {
		// Arrange
		List<Cliente> lista = List.of(new Cliente(CPF, "Maria Silva"), new Cliente("98765432100", "Joao Souza"));
		when(repository.listarTodos()).thenReturn(lista);

		// Act
		List<Cliente> resultado = service.listarTodos();

		// Assert
		assertEquals(lista, resultado);
	}

	@Test
	@DisplayName("Deve delegar a busca por nome ao repositório")
	void buscarPorNome_delegaAoRepositorio() {
		// Arrange
		List<Cliente> lista = List.of(new Cliente(CPF, "Maria Silva"));
		when(repository.buscarPorNome("silva")).thenReturn(lista);

		// Act
		List<Cliente> resultado = service.buscarPorNome("silva");

		// Assert
		assertEquals(lista, resultado);
		verify(repository).buscarPorNome("silva");
	}

}
