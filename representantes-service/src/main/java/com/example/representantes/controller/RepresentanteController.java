package com.example.representantes.controller;

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

import com.example.representantes.model.Representante;
import com.example.representantes.repository.RepresentanteRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/representantes")
public class RepresentanteController {

	private final RepresentanteRepository repository;
	private final Counter cadastrosComSucesso;
	private final Counter cadastrosDuplicados;

	public RepresentanteController(RepresentanteRepository repository, MeterRegistry meterRegistry) {
		this.repository = repository;
		// Exportado no Prometheus como representantes_cadastro_total{resultado="..."}
		this.cadastrosComSucesso = Counter.builder("representantes.cadastro")
				.description("Tentativas de cadastro de representantes, por resultado")
				.tag("resultado", "sucesso")
				.register(meterRegistry);
		this.cadastrosDuplicados = Counter.builder("representantes.cadastro")
				.description("Tentativas de cadastro de representantes, por resultado")
				.tag("resultado", "duplicado")
				.register(meterRegistry);
	}

	@PostMapping
	public ResponseEntity<Representante> cadastrar(@Valid @RequestBody Representante representante) {
		if (repository.existsById(representante.getCpf())) {
			cadastrosDuplicados.increment();
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe representante com CPF " + representante.getCpf());
		}
		Representante salvo = repository.save(representante);
		cadastrosComSucesso.increment();
		return ResponseEntity.created(URI.create("/representantes/" + salvo.getCpf())).body(salvo);
	}

	@GetMapping
	public List<Representante> listar() {
		return repository.findAll();
	}

	@GetMapping("/{cpf}")
	public Representante consultarPorCpf(@PathVariable String cpf) {
		return repository.findById(cpf)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Representante com CPF " + cpf + " nao encontrado"));
	}

	@GetMapping("/busca")
	public List<Representante> consultarPorNome(@RequestParam String nome) {
		return repository.findByNomeContainingIgnoreCase(nome);
	}

}
