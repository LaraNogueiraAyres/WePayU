package br.ufal.ic.p2.wepayu.models;

import java.io.Serializable;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamento;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoEmMaos;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoBanco;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoCorreios;

public abstract class Empregado implements Serializable, Cloneable {
    protected String nome;
    protected String endereco;
    protected MembroSindicato membroSindicato;
    public double salario;
    private MetodoPagamento metodoPagamento;
    // US9: agenda de pagamento
    private String agendaPagamento;

    public Empregado(String nome, String endereco, double salario) {
        this.nome = nome;
        this.endereco = endereco;
        this.salario = salario;
        this.membroSindicato = null;
        this.metodoPagamento = new MetodoPagamentoEmMaos();
        this.agendaPagamento = null; // definido pelo subtipo
    }

    public String getNome() { return nome; }
    public String getEndereco() { return endereco; }
    public double getSalario() { return salario; }
    public boolean isSindicalizado() { return this.membroSindicato != null; }
    public MembroSindicato getMembroSindicato() { return this.membroSindicato; }
    public MetodoPagamento getMetodoPagamentoObjeto() { return metodoPagamento; }
    public String getAgendaPagamento() { return this.agendaPagamento; }

    public void setNome(String nome) { this.nome = nome; }
    public void setEndereco(String endereco) { this.endereco = endereco; }
    public void setSalario(double salario) { this.salario = salario; }
    public void setMetodoPagamento(MetodoPagamento metodoPagamento) { this.metodoPagamento = metodoPagamento; }
    public void setSindicalizado(boolean sindicalizado, String idSindicato, double taxaSindical) {
        if (sindicalizado) this.membroSindicato = new MembroSindicato(idSindicato, taxaSindical);
        else this.membroSindicato = null;
    }
    public void setAgendaPagamento(String agendaPagamento) { this.agendaPagamento = agendaPagamento; }

    public String getMetodoPagamento() {
        if (metodoPagamento instanceof MetodoPagamentoEmMaos) return "emMaos";
        else if (metodoPagamento instanceof MetodoPagamentoBanco) return "banco";
        else if (metodoPagamento instanceof MetodoPagamentoCorreios) return "correios";
        return null;
    }
}
