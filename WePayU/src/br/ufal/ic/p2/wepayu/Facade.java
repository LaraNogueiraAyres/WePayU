package br.ufal.ic.p2.wepayu;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;


public class Facade {

    private SistemaFolha sistema;
    private static int nextId = 1;
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("d/M/uuuu");

    public Facade() {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream("wepayu.dat"))) {
            this.sistema = (SistemaFolha) in.readObject();
        } catch (Exception e) {
            this.sistema = new SistemaFolha();
        }
    }

    public String getNumeroDeEmpregados() throws Exception {
    return sistema.getNumeroDeEmpregados();
    }

    public void criarAgendaDePagamentos (String descricao) throws Exception {
        sistema.criarAgendaDePagamentos(descricao);
    }   

    public void zerarSistema() {
        sistema.zerarSistema();
    }

    public void encerrarSistema() {
        sistema.encerrarSistema();
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream("wepayu.dat"))) {
            out.writeObject(sistema);
        } catch (Exception e) {
        
        }
    }
    
    public void undo() throws Exception {
    sistema.undo();
    }

    public void redo() throws Exception {
        sistema.redo();
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario, String comissao) throws Exception {
        String id = String.valueOf(nextId++);
        sistema.addEmpregado(id, nome, endereco, tipo, salario, comissao);
        return id;
    }

    public String criarEmpregado(String nome, String endereco, String tipo, String salario) throws Exception {
        return criarEmpregado(nome, endereco, tipo, salario, null);
    } 

    public String getAtributoEmpregado(String empId, String atributo) throws Exception {
        return sistema.getAtributoEmpregado(empId, atributo);
    }

    public void alteraEmpregado(String empId, String atributo, String valor) throws Exception {
        sistema.alteraEmpregado(empId, atributo, valor);
    }
    
    public void alteraEmpregado(String empId, String atributo, String valor, String arg1) throws Exception {
        sistema.alteraEmpregado(empId, atributo, valor, arg1); 
    }

    public void alteraEmpregado(String empId, String atributo, String valor1, String banco, String agencia, String contaCorrente) throws Exception {
        sistema.alteraEmpregado(empId, atributo, valor1, banco, agencia, contaCorrente);
    }
    
    public void alteraEmpregado(String empId, String atributo, String arg1, String arg2, String arg3) throws Exception {
        sistema.alteraEmpregado(empId, atributo, arg1, arg2, arg3);
    }

    public String totalFolha(String data) {
        LocalDate d = LocalDate.parse(data, DMY);
        return sistema.totalFolha(d); 
    }

    public String getEmpregadoPorNome(String nome, String indice) throws Exception {
        return sistema.getEmpregadoPorNome(nome, indice);
    }

     public void lancaTaxaServico(String membro, String data, String valor) throws Exception {
        sistema.lancaTaxaServico(membro, data, valor);
    }
    
    public String getTaxasServico(String empId, String dataInicial, String dataFinal) throws Exception {
        return sistema.getTaxasServico(empId, dataInicial, dataFinal);
    }

    public void removerEmpregado(String empId) throws Exception {
        sistema.removerEmpregado(empId);
    }

    public void rodaFolha(String data) throws Exception {
        sistema.rodaFolha(data);
    }

    public void rodaFolha(String data, String saida) throws Exception {
        sistema.rodaFolha(data, saida);
    }

    public void lancaCartao(String empId, String data, String horas) throws Exception {
        sistema.lancaCartao(empId, data, horas);
    }

    public void lancaVenda(String empId, String data, String valor) throws Exception {
        sistema.lancaVenda(empId, data, valor);
    }

    public String getHorasNormaisTrabalhadas(String empId, String dataInicial, String dataFinal) throws Exception {
        return sistema.getHorasNormaisTrabalhadas(empId, dataInicial, dataFinal);
    }

    public String getHorasExtrasTrabalhadas(String empId, String dataInicial, String dataFinal) throws Exception {
        return sistema.getHorasExtrasTrabalhadas(empId, dataInicial, dataFinal);
    }

    public String getVendasRealizadas(String empId, String dataInicial, String dataFinal) throws Exception {
        return sistema.getVendasRealizadas(empId, dataInicial, dataFinal);
    }
}
