package br.ufal.ic.p2.wepayu.models;

import java.util.ArrayList;
import java.util.List;

public class EmpregadoComissionado extends EmpregadoAssalariado {
    private double taxaDeComissao;
    private List<ResultadoDeVenda> resultadosDeVenda;

    public EmpregadoComissionado(String nome, String endereco, double salarioMensal, double taxaDeComissao) {
        super(nome, endereco, salarioMensal);
        this.taxaDeComissao = taxaDeComissao;
        this.resultadosDeVenda = new ArrayList<>();
        // US9: default
        setAgendaPagamento("semanal 2 5");
    }

    public double getTaxaDeComissao() { return taxaDeComissao; }
    public void setTaxaDeComissao(double taxaDeComissao) { this.taxaDeComissao = taxaDeComissao; }

    public void adicionarResultadoDeVenda(ResultadoDeVenda venda) {
        if (this.resultadosDeVenda == null) this.resultadosDeVenda = new ArrayList<>();
        this.resultadosDeVenda.add(venda);
    }

    public List<ResultadoDeVenda> getResultadosDeVenda() { return resultadosDeVenda; }

    @Override
    public Empregado clone() {
        try {
            EmpregadoComissionado clone = (EmpregadoComissionado) super.clone();
            clone.taxaDeComissao = this.taxaDeComissao;
            clone.resultadosDeVenda = new ArrayList<>(this.resultadosDeVenda);
            clone.setAgendaPagamento(this.getAgendaPagamento());
            if (super.getMetodoPagamentoObjeto() != null) {
                clone.setMetodoPagamento(super.getMetodoPagamentoObjeto().clone());
            }
            if (super.isSindicalizado()) {
                clone.setSindicalizado(true, super.getMembroSindicato().getId(),
                                        super.getMembroSindicato().getTaxaSindical());
            }
            return clone;
        } catch (Exception e) {
            return null;    
    }
}
}
