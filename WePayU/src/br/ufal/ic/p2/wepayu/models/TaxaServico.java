package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.time.LocalDate;

public class TaxaServico implements Serializable {
    private LocalDate data;
    private double valor;

   private boolean cobrada;

    public TaxaServico(LocalDate data, double valor) {
        this.data = data;
        this.valor = valor;
        this.cobrada = false;
    }

    public boolean isCobrada() { return this.cobrada; }
    public void marcarCobrada() { this.cobrada = true; }

    public LocalDate getData() {
        return this.data;
    }

    public double getValor() {
        return this.valor;
    }
}
