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
    }

    public double getSalarioPorHora() { return salarioPorHora; }

    @Override
    public double getSalario() {
        return this.salarioPorHora;
    }

    @Override
    public void setSalario(double salario) {
        this.salarioPorHora = salario;
        super.setSalario(salario);
}


    public void adicionarCartaoDePonto(CartaoDePonto cartao) {
        this.cartoesDePonto.add(cartao);
    }

    @Override
    public Empregado clone() {
        try {
            EmpregadoHorista clone = (EmpregadoHorista) super.clone();
            clone.salarioPorHora = this.salarioPorHora;
            clone.cartoesDePonto = new ArrayList<>(this.cartoesDePonto);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
