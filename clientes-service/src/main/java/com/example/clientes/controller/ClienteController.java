package com.example.clientes.controller;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.example.clientes.model.Cliente;
import com.example.clientes.service.ClienteJaCadastradoException;
import com.example.clientes.service.ClienteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

	private final ClienteService service;

	public ClienteController(ClienteService service) {
		this.service = service;
	}

	@PostMapping
	public ResponseEntity<Cliente> cadastrar(@Valid @RequestBody Cliente cliente) {
		Cliente salvo = service.cadastrar(cliente);
		return ResponseEntity.created(URI.create("/clientes/" + salvo.getCpf())).body(salvo);
	}

	@GetMapping
	public List<Cliente> listar() {
		return service.listarTodos();
	}

	@GetMapping("/{cpf}")
	public Cliente consultarPorCpf(@PathVariable String cpf) {
		return service.buscarPorCpf(cpf)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente com CPF " + cpf + " nao encontrado"));
	}

	@GetMapping("/busca")
	public List<Cliente> consultarPorNome(@RequestParam String nome) {
		return service.buscarPorNome(nome);
	}

	@ExceptionHandler(ClienteJaCadastradoException.class)
	public ResponseEntity<ProblemDetail> tratarDuplicado(ClienteJaCadastradoException e) {
		return ResponseEntity.status(HttpStatus.CONFLICT)
				.body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage()));
	}

}
