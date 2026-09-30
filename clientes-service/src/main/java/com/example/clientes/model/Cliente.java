package com.example.clientes.model;

import jakarta.validation.constraints.NotBlank;

/** Cliente do dominio. Nao depende de JPA: o mapeamento para o banco fica em {@code persistence}. */
public class Cliente {

	@NotBlank
	private String cpf;

	@NotBlank
	private String nome;

	public Cliente() {
	}

	public Cliente(String cpf, String nome) {
		this.cpf = cpf;
		this.nome = nome;
	}

	public String getCpf() {
		return cpf;
	}

	public void setCpf(String cpf) {
		this.cpf = cpf;
	}

	public String getNome() {
		return nome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

}
