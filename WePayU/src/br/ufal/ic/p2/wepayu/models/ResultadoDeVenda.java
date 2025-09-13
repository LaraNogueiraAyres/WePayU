package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.time.LocalDate;


public class ResultadoDeVenda implements Serializable {
    private LocalDate data;
    private double valor;

    public ResultadoDeVenda(LocalDate data, double valor) {
        this.data = data;
        this.valor = valor;
    }

    public LocalDate getData() {
        return data;
    }

    public double getValor() {
        return valor;
    }
}