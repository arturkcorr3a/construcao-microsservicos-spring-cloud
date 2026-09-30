package com.example.clientes.service;

public class ClienteJaCadastradoException extends RuntimeException {

	public ClienteJaCadastradoException(String cpf) {
		super("Ja existe cliente com CPF " + cpf);
	}

}
