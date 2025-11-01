package br.ufal.ic.p2.wepayu.models.payment;

/** Método de pagamento via Correios. */
public class MetodoPagamentoCorreios implements MetodoPagamento {

    private final String descricao;
    private final String endereco;

    /** Construtor sem argumentos (descricao="correios", endereco=""). */
    public MetodoPagamentoCorreios() {
        this("correios", "");
    }

    /** Construtor com apenas o endereço (descricao="correios"). */
    public MetodoPagamentoCorreios(String endereco) {
        this("correios", endereco);
    }

    /** Construtor completo. */
    public MetodoPagamentoCorreios(String descricao, String endereco) {
        this.descricao = (descricao == null || descricao.isEmpty()) ? "correios" : descricao;
        this.endereco  = endereco == null ? "" : endereco;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getEndereco() {
        return endereco;
    }
    
    @Override
    public MetodoPagamentoCorreios clone() {
        try {
            return (MetodoPagamentoCorreios) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}