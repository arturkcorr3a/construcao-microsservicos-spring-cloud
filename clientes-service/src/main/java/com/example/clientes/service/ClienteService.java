package com.example.clientes.service;

import java.util.List;
import java.util.Optional;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.stereotype.Service;

import com.example.clientes.model.Cliente;
import com.example.clientes.repository.ClienteRepository;

@Service
public class ClienteService {

	private final ClienteRepository repository;
	private final Counter cadastrosComSucesso;
	private final Counter cadastrosDuplicados;

	public ClienteService(ClienteRepository repository, MeterRegistry meterRegistry) {
		this.repository = repository;
		// Exportado no Prometheus como clientes_cadastro_total{resultado="..."}
		this.cadastrosComSucesso = Counter.builder("clientes.cadastro")
				.description("Tentativas de cadastro de clientes, por resultado")
				.tag("resultado", "sucesso")
				.register(meterRegistry);
		this.cadastrosDuplicados = Counter.builder("clientes.cadastro")
				.description("Tentativas de cadastro de clientes, por resultado")
				.tag("resultado", "duplicado")
				.register(meterRegistry);
	}

	public Cliente cadastrar(Cliente cliente) {
		if (repository.existePorCpf(cliente.getCpf())) {
			cadastrosDuplicados.increment();
			throw new ClienteJaCadastradoException(cliente.getCpf());
		}
		Cliente salvo = repository.salvar(cliente);
		cadastrosComSucesso.increment();
		return salvo;
	}

	public List<Cliente> listarTodos() {
		return repository.listarTodos();
	}

	public Optional<Cliente> buscarPorCpf(String cpf) {
		return repository.buscarPorCpf(cpf);
	}

	public List<Cliente> buscarPorNome(String nome) {
		return repository.buscarPorNome(nome);
	}

}
