package com.example.pecas.controller;

import java.net.URI;
import java.util.List;

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

import com.example.pecas.model.Peca;
import com.example.pecas.repository.PecaRepository;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/pecas")
public class PecaController {

	private final PecaRepository repository;

	public PecaController(PecaRepository repository) {
		this.repository = repository;
	}

	@PostMapping
	public ResponseEntity<Peca> cadastrar(@Valid @RequestBody Peca peca) {
		if (repository.existsById(peca.getId())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Ja existe peca com id " + peca.getId());
		}
		Peca salva = repository.save(peca);
		return ResponseEntity.created(URI.create("/pecas/" + salva.getId())).body(salva);
	}

	@GetMapping
	public List<Peca> listar() {
		return repository.findAll();
	}

	@GetMapping("/{id}")
	public Peca consultarPorId(@PathVariable Long id) {
		return repository.findById(id)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Peca " + id + " nao encontrada"));
	}

	@GetMapping("/busca")
	public List<Peca> consultarPorNome(@RequestParam String nome) {
		return repository.findByNomeContainingIgnoreCase(nome);
	}

}
