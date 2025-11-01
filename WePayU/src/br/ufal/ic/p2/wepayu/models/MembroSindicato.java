package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import java.time.LocalDate;
//import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class MembroSindicato implements Serializable {
    private String id;
    private double taxaSindical;
    private List<TaxaServico> taxasDeServico;

    private LocalDate ultimaDataCobranca;

    public LocalDate getUltimaDataCobranca() { return this.ultimaDataCobranca; }
    public void setUltimaDataCobranca(LocalDate d) { this.ultimaDataCobranca = d; }

    public MembroSindicato(String id, double taxaSindical) {
        this.id = id;
        this.taxaSindical = taxaSindical;
        this.taxasDeServico = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public double getTaxaSindical() {
        return taxaSindical;
    }

    public void setTaxaSindical(double taxaSindical) {
        this.taxaSindical = taxaSindical;
    }

    public void adicionarTaxaDeServico(TaxaServico taxa) {
        this.taxasDeServico.add(taxa);
    }

    public List<TaxaServico> getTaxasDeServico() {
        return this.taxasDeServico;
    }
}
