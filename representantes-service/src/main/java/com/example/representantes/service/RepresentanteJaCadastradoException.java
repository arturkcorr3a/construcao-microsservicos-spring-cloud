package com.example.representantes.service;

public class RepresentanteJaCadastradoException extends RuntimeException {

	public RepresentanteJaCadastradoException(String cpf) {
		super("Ja existe representante com CPF " + cpf);
	}

}
