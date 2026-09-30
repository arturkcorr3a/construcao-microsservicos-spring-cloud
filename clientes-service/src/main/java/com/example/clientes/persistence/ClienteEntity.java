package com.example.clientes.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cliente")
public class ClienteEntity {

	@Id
	private String cpf;

	private String nome;

	protected ClienteEntity() {
	}

	public ClienteEntity(String cpf, String nome) {
		this.cpf = cpf;
		this.nome = nome;
	}

	public String getCpf() {
		return cpf;
	}

	public String getNome() {
		return nome;
	}

}
