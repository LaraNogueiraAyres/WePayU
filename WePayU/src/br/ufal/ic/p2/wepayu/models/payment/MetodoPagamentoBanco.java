package br.ufal.ic.p2.wepayu.models.payment;

/**
 * Classe que representa o metodo de pagamento via deposito em conta bancaria.
 */
public class MetodoPagamentoBanco implements MetodoPagamento {
    private String banco;
    private String agencia;
    private String contaCorrente;

    public MetodoPagamentoBanco(String banco, String agencia, String contaCorrente) {
        this.banco = banco;
        this.agencia = agencia;
        this.contaCorrente = contaCorrente;
    }

    public String getBanco() { return banco; }
    public String getAgencia() { return agencia; }
    public String getContaCorrente() { return contaCorrente; }

    @Override
    public MetodoPagamentoBanco clone() {
        try {
            return (MetodoPagamentoBanco) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }
}
