package br.ufal.ic.p2.wepayu.Exception;

public class ComissaoDeveSerPositivaException extends Exception{
    public ComissaoDeveSerPositivaException(){
        super("Comissao deve ser nao-negativa.");
    }

}
