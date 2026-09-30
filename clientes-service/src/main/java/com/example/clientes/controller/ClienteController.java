package com.example.clientes.controller;

import java.net.URI;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.clientes.model.Cliente;
import com.example.clientes.repository.ClienteRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

	private final ClienteRepository repository;
	private final Counter cadastrosComSucesso;
	private final Counter cadastrosDuplicados;

	public ClienteController(ClienteRepository repository, MeterRegistry meterRegistry) {
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

	@PostMapping
	public ResponseEntity<Cliente> cadastrar(@Valid @RequestBody Cliente cliente) {
		if (repository.existsById(cliente.getCpf())) {
			cadastrosDuplicados.increment();
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe cliente com CPF " + cliente.getCpf());
		}
		Cliente salvo = repository.save(cliente);
		cadastrosComSucesso.increment();
		return ResponseEntity.created(URI.create("/clientes/" + salvo.getCpf())).body(salvo);
	}

	@GetMapping
	public List<Cliente> listar() {
		return repository.findAll();
	}

	@GetMapping("/{cpf}")
	public Cliente consultarPorCpf(@PathVariable String cpf) {
		return repository.findById(cpf)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente com CPF " + cpf + " nao encontrado"));
	}

	@GetMapping("/busca")
	public List<Cliente> consultarPorNome(@RequestParam String nome) {
		return repository.findByNomeContainingIgnoreCase(nome);
	}

}
