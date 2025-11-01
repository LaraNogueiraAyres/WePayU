package br.ufal.ic.p2.wepayu.models;

import java.util.ArrayList;
import java.util.List;

public class EmpregadoHorista extends Empregado {
    private double salarioPorHora;
    public List<CartaoDePonto> cartoesDePonto;

    public EmpregadoHorista(String nome, String endereco, double salarioPorHora) {
        super(nome, endereco, salarioPorHora);
        this.salarioPorHora = salarioPorHora;
        this.cartoesDePonto = new ArrayList<>();
        // US9: default
        setAgendaPagamento("semanal 5");
    }

    public double getSalarioPorHora() { return salarioPorHora; }

    public void adicionarCartaoDePonto(CartaoDePonto cartao) {
        if (this.cartoesDePonto == null) this.cartoesDePonto = new ArrayList<>();
        this.cartoesDePonto.add(cartao);
    }

    @Override
    public Empregado clone() {
        try {
            EmpregadoHorista clone = (EmpregadoHorista) super.clone();
            clone.salarioPorHora = this.salarioPorHora;
            clone.cartoesDePonto = new ArrayList<>(this.cartoesDePonto);
            clone.setAgendaPagamento(this.getAgendaPagamento());
            if (super.getMetodoPagamentoObjeto() != null) {
                clone.setMetodoPagamento(super.getMetodoPagamentoObjeto().clone());
            }
            if (super.isSindicalizado()) {
                clone.setSindicalizado(true, super.getMembroSindicato().getId(),
                                        super.getMembroSindicato().getTaxaSindical());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
