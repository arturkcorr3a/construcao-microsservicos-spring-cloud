package com.example.representantes.persistence;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "representante")
public class RepresentanteEntity {

	@Id
	private String cpf;

	private String nome;

	protected RepresentanteEntity() {
	}

	public RepresentanteEntity(String cpf, String nome) {
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
