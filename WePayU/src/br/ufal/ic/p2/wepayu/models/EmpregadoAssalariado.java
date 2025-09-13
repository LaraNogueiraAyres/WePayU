package br.ufal.ic.p2.wepayu.models;


public class EmpregadoAssalariado extends Empregado {
    private double salarioMensal;

    public EmpregadoAssalariado(String nome, String endereco, double salarioMensal) {
        super(nome, endereco, salarioMensal);
        this.salarioMensal = salarioMensal;
    }

    public double getSalarioMensal() { return salarioMensal; }
    
    @Override
    public double getSalario() {
        return this.salarioMensal;
    }

    @Override
    public void setSalario(double salario) {
        this.salarioMensal = salario;
        super.setSalario(salario); 
    }
    @Override
    public Empregado clone() {
        try {
            EmpregadoAssalariado clone = (EmpregadoAssalariado) super.clone();
            clone.salarioMensal = this.salarioMensal;
            if (super.getMetodoPagamentoObjeto() != null) {
                clone.setMetodoPagamento(super.getMetodoPagamentoObjeto().clone());
            }
            if (super.isSindicalizado()) {
                clone.setSindicalizado(true, super.getMembroSindicato().getId(), super.getMembroSindicato().getTaxaSindical());
            }
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
