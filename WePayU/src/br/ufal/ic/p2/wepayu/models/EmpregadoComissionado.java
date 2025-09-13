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
    }

    public double getTaxaDeComissao() { return taxaDeComissao; }

    public void setTaxaDeComissao(double taxaDeComissao) {
        this.taxaDeComissao = taxaDeComissao;
    }

    public void adicionarResultadoDeVenda(ResultadoDeVenda venda) {
        this.resultadosDeVenda.add(venda);
    }
    
    public List<ResultadoDeVenda> getResultadosDeVenda() {
        return resultadosDeVenda;
    }
    

}
