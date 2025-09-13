package br.ufal.ic.p2.wepayu;

import br.ufal.ic.p2.wepayu.Exception.*;
import br.ufal.ic.p2.wepayu.models.CartaoDePonto;
import br.ufal.ic.p2.wepayu.models.Empregado;
import br.ufal.ic.p2.wepayu.models.EmpregadoAssalariado;
import br.ufal.ic.p2.wepayu.models.EmpregadoComissionado;
import br.ufal.ic.p2.wepayu.models.EmpregadoHorista;
import br.ufal.ic.p2.wepayu.models.MembroSindicato;
import br.ufal.ic.p2.wepayu.models.ResultadoDeVenda;
import br.ufal.ic.p2.wepayu.models.TaxaServico;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamento;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoBanco;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoCorreios;
import br.ufal.ic.p2.wepayu.models.payment.MetodoPagamentoEmMaos;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SistemaFolha implements Serializable, Cloneable {

    private Map<String, Empregado> empregados;
    //private Stack<Map<String, Empregado>> historicoEstados = new Stack<>();
    //private static final String SISTEMA_FILE = "wepayu.dat";

    public SistemaFolha() {
        this.empregados = new HashMap<>();
    }

    public void zerarSistema() {
        this.empregados.clear();
    }

    public void addEmpregado(String id, String nome, String endereco, String tipo, String salario, String comissao) throws Exception {
        if (nome == null || nome.isEmpty()) {
            throw new NomeNaoPodeSerNuloException();
        }

        if (endereco == null || endereco.isEmpty()) {
            throw new EnderecoNaoPodeSerNuloException();
        }

        if (salario == null || salario.trim().isEmpty()) {
            throw new SalarioNaoPodeSerNuloException();
        }

        double salarioValue;
        try {
            salarioValue = Double.parseDouble(salario.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new SalarioDeveSerNumericoException();
        }

        if (salarioValue < 0) {
            throw new SalarioDeveSerNaoNegativoException();
        }

        if (tipo == null || tipo.isEmpty() || (!tipo.equals("horista") && !tipo.equals("assalariado") && !tipo.equals("comissionado"))) {
            throw new TipoInvalidoException();
        }

        if (tipo.equals("comissionado")) {
            if (comissao == null) {
                throw new TipoNaoAplicavelException();
            }
            if (comissao.trim().isEmpty()) {
                throw new ComissaoNaoPodeSerNulaException();
            }
            try {
                double comissaoValue = Double.parseDouble(comissao.replace(',', '.'));
                if (comissaoValue < 0) {
                    throw new ComissaoDeveSerNaoNegativaException();
                }
            } catch (NumberFormatException e) {
                throw new ComissaoDeveSerNumericaException();
            }
        }

        if ((tipo.equals("horista") || tipo.equals("assalariado")) && comissao != null) {
            throw new TipoNaoAplicavelException();
        }

        Empregado novoEmpregado = null;
        switch (tipo) {
            case "horista":
                novoEmpregado = new EmpregadoHorista(nome, endereco, salarioValue);
                break;
            case "assalariado":
                novoEmpregado = new EmpregadoAssalariado(nome, endereco, salarioValue);
                break;
            case "comissionado":
                novoEmpregado = new EmpregadoComissionado(nome, endereco, salarioValue, Double.parseDouble(comissao.replace(',', '.')));
                break;
        }

        this.empregados.put(id, novoEmpregado);
    }

   public String getAtributoEmpregado(String empId, String atributo) throws Exception {

        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }

        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }

        Empregado empregado = this.empregados.get(empId);
        switch (atributo) {
            case "nome":
                return empregado.getNome();
            case "endereco":
                return empregado.getEndereco();
            case "tipo":
                if (empregado instanceof EmpregadoHorista) {
                    return "horista";
                } else if (empregado instanceof EmpregadoAssalariado && !(empregado instanceof EmpregadoComissionado)) {
                    return "assalariado";
                } else if (empregado instanceof EmpregadoComissionado) {
                    return "comissionado";
                }
            case "salario":
                return String.format("%.2f", empregado.getSalario()).replace('.', ',');
            
            case "comissao":
                if (empregado instanceof EmpregadoComissionado) {
                    EmpregadoComissionado comissionado = (EmpregadoComissionado) empregado;
                    return String.format("%.2f", comissionado.getTaxaDeComissao()).replace('.', ',');
                } else {
                    throw new EmpregadoNaoEhComissionadoException();
                }
            case "sindicalizado":
                return String.valueOf(empregado.isSindicalizado());
            
            case "metodoPagamento":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoEmMaos) {
                    return "emMaos";
                } else if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    return "banco";
                } else if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoCorreios) {
                    return "correios";
                }
            case "banco":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    MetodoPagamentoBanco banco = (MetodoPagamentoBanco) empregado.getMetodoPagamentoObjeto();
                    return banco.getBanco();
                } else {
                    throw new EmpregadoNaoRecebeEmBancoException();
                }
            case "agencia":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    MetodoPagamentoBanco banco = (MetodoPagamentoBanco) empregado.getMetodoPagamentoObjeto();
                    return banco.getAgencia();
                } else {
                    throw new EmpregadoNaoRecebeEmBancoException();
                }
            case "contaCorrente":
                if (empregado.getMetodoPagamentoObjeto() instanceof MetodoPagamentoBanco) {
                    MetodoPagamentoBanco banco = (MetodoPagamentoBanco) empregado.getMetodoPagamentoObjeto();
                    return banco.getContaCorrente();
                } else {
                    throw new EmpregadoNaoRecebeEmBancoException();
                }
            case "idSindicato":
                if(empregado.isSindicalizado()) {
                    return empregado.getMembroSindicato().getId();
                } else {
                    throw new EmpregadoNaoEhSindicalizadoException();
                }
            case "taxaSindical":
                if(empregado.isSindicalizado()) {
                    return String.format("%.2f", empregado.getMembroSindicato().getTaxaSindical()).replace('.', ',');
                } else {
                    throw new EmpregadoNaoEhSindicalizadoException();
                }
            default:
                throw new AtributoNaoExisteException();
        }
    }

    public void removerEmpregado(String empId) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }   
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
    this.empregados.remove(empId);
    }

    public void lancaCartao(String empId, String data, String horas) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoEhHoristaException();
        }

        LocalDate dataCartao;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            dataCartao = LocalDate.parse(data, formatter);
        } catch (DateTimeParseException e) {
            throw new DataInvalidaException();
        }

        if (horas == null || horas.trim().isEmpty()) {
            throw new HorasDevemSerPositivasException();
        }
        double horasTrabalhadas;
        try {
            horasTrabalhadas = Double.parseDouble(horas.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new HorasDevemSerPositivasException();
        }
        if (horasTrabalhadas <= 0) {
            throw new HorasDevemSerPositivasException();
        }

        EmpregadoHorista horista = (EmpregadoHorista) empregado;
        horista.adicionarCartaoDePonto(new CartaoDePonto(dataCartao, horasTrabalhadas));
    }

    public void lancaVenda(String empId, String data, String valor) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoComissionado)) {
            throw new EmpregadoNaoEhComissionadoException();
        }

        LocalDate dataVenda;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            dataVenda = LocalDate.parse(data, formatter);
        } catch (DateTimeParseException e) {
            throw new DataInvalidaException();
        }

        if (valor == null || valor.trim().isEmpty()) {
            throw new ValorDeveSerPositivoException();
        }
        double valorVenda;
        try {
            valorVenda = Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValorDeveSerPositivoException();
        }
        if (valorVenda <= 0) {
            throw new ValorDeveSerPositivoException();
        }

        EmpregadoComissionado comissionado = (EmpregadoComissionado) empregado;
        comissionado.adicionarResultadoDeVenda(new ResultadoDeVenda(dataVenda, valorVenda));
    }

    public void lancaTaxaServico(String sindId, String data, String valor) throws Exception {
        if (sindId == null || sindId.isEmpty()) {
            throw new IdentificacaoMembroNulaException();
        }

        Empregado membroSindicato = null;
        for (Empregado emp : this.empregados.values()) {
            if (emp.isSindicalizado() && emp.getMembroSindicato().getId().equals(sindId)) {
                membroSindicato = emp;
                break;
            }
        }

        if (membroSindicato == null) {
            throw new MembroSindicatoNaoExisteException();
        }

        LocalDate dataTaxa;
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d/M/yyyy");
            dataTaxa = LocalDate.parse(data, formatter);
        } catch (DateTimeParseException e) {
            throw new DataInvalidaException();
        }

        if (valor == null || valor.trim().isEmpty()) {
            throw new ValorDeveSerPositivoException();
        }
        double valorTaxa;
        try {
            valorTaxa = Double.parseDouble(valor.replace(',', '.'));
        } catch (NumberFormatException e) {
            throw new ValorDeveSerPositivoException();
        }
        if (valorTaxa <= 0) {
            throw new ValorDeveSerPositivoException();
        }

        membroSindicato.getMembroSindicato().adicionarTaxaDeServico(new TaxaServico(dataTaxa, valorTaxa));
    }

    public void alteraEmpregado(String empId, String atributo, String valor) throws Exception {
        alteraEmpregadoInterno(empId, atributo, new String[]{valor});
    }

    public void alteraEmpregado(String empId, String atributo, String valor1, String banco, String agencia, String contaCorrente) throws Exception {
        String[] args = {valor1, banco, agencia, contaCorrente};
        alteraEmpregadoInterno(empId, atributo, args);
    }

    public void alteraEmpregado(String empId, String atributo, String valor, String arg1) throws Exception {
        String[] args = {valor, arg1};
        alteraEmpregadoInterno(empId, atributo, args);
    }
    
    public void alteraEmpregado(String empId, String atributo, String arg1, String arg2, String arg3) throws Exception {
        if (atributo.equals("sindicalizado")) {
            String[] args = {arg1, arg2, arg3};
            alteraEmpregadoInterno(empId, atributo, args);
        } else if (atributo.equals("tipo")) {
            String[] args = {arg1, arg2, arg3};
            alteraEmpregadoInterno(empId, atributo, args);
        } else {
             throw new AtributoNaoExisteException();
        }
    }
    
    private void alteraEmpregadoInterno(String empId, String atributo, String[] args) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }

        Empregado empregado = this.empregados.get(empId);
        
        switch (atributo) {
            case "nome":
                if (args.length < 1 || args[0] == null || args[0].isEmpty()) {
                    throw new NomeNaoPodeSerNuloException();
                }
                empregado.setNome(args[0]);
                break;
            case "endereco":
                if (args.length < 1 || args[0] == null || args[0].isEmpty()) {
                    throw new EnderecoNaoPodeSerNuloException();
                }
                empregado.setEndereco(args[0]);
                break;
            case "tipo":
                if (args[0] == null || args[0].isEmpty() || (!args[0].equals("horista") && !args[0].equals("assalariado") && !args[0].equals("comissionado"))) {
                    throw new TipoInvalidoException();
                }
                String novoTipo = args[0];

                double salario = empregado.getSalario();
                double comissao = 0.0;

                java.util.List<String> extraArgs = new java.util.ArrayList<>();
                if (args != null) {
                    for (int i = 1; i < args.length; i++) {
                        if (args[i] != null && !args[i].trim().isEmpty()) {
                            extraArgs.add(args[i].trim());
                        }
                    }
                }

                if (extraArgs.size() == 1) {
                    String only = extraArgs.get(0);
                    if (novoTipo.equals("comissionado")) {
                        try {
                            comissao = Double.parseDouble(only.replace(',', '.'));
                            if (comissao < 0) throw new ComissaoDeveSerNaoNegativaException();
                        } catch (NumberFormatException e) {
                            throw new ComissaoDeveSerNumericaException();
                        }
                    } else {
                        try {
                            salario = Double.parseDouble(only.replace(',', '.'));
                            if (salario < 0) throw new SalarioDeveSerNaoNegativoException();
                        } catch (NumberFormatException e) {
                            throw new SalarioDeveSerNumericoException();
                        }
                    }
                } else if (extraArgs.size() >= 2) {
                    String sSal = extraArgs.get(0);
                    String sCom = extraArgs.get(1);
                    try {
                        salario = Double.parseDouble(sSal.replace(',', '.'));
                        if (salario < 0) throw new SalarioDeveSerNaoNegativoException();
                    } catch (NumberFormatException e) {
                        throw new SalarioDeveSerNumericoException();
                    }
                    try {
                        comissao = Double.parseDouble(sCom.replace(',', '.'));
                        if (comissao < 0) throw new ComissaoDeveSerNaoNegativaException();
                    } catch (NumberFormatException e) {
                        throw new ComissaoDeveSerNumericaException();
                    }
                }

                String nome = empregado.getNome();
                String endereco = empregado.getEndereco();
                MetodoPagamento metodoPagamento = empregado.getMetodoPagamentoObjeto();
                MembroSindicato membroSindicato = empregado.getMembroSindicato();

                Empregado novoEmpregado = null;
                switch (novoTipo) {
                    case "horista":
                        novoEmpregado = new EmpregadoHorista(nome, endereco, salario);
                        break;
                    case "assalariado":
                        novoEmpregado = new EmpregadoAssalariado(nome, endereco, salario);
                        break;
                    case "comissionado":
                        novoEmpregado = new EmpregadoComissionado(nome, endereco, salario, comissao);
                        break;
                }
                
                if (metodoPagamento != null) {
                    novoEmpregado.setMetodoPagamento(metodoPagamento);
                }
                if (membroSindicato != null) {
                    novoEmpregado.setSindicalizado(true, membroSindicato.getId(), membroSindicato.getTaxaSindical());
                }

                this.empregados.put(empId, novoEmpregado);
                break;
            case "sindicalizado":
                if (args.length < 1) {
                    throw new AtributoNaoExisteException();
                }
                
                boolean sindicalizado;
                if(args[0].equals("true")) {
                    sindicalizado = true;
                } else if (args[0].equals("false")) {
                    sindicalizado = false;
                } else {
                    throw new ValorInvalidoException();
                }
                
                if (sindicalizado) {
                    if (args.length < 3) {
                        throw new AtributoNaoExisteException();
                    }
                    String idSindicato = args[1];
                    String taxaSindicalStr = args[2];
                    
                    if (idSindicato == null || idSindicato.isEmpty()) {
                        throw new IdentificacaoSindicatoNulaException();
                    }

                    for (Map.Entry<String, Empregado> entry : this.empregados.entrySet()) {
                        if (!entry.getKey().equals(empId) && entry.getValue().isSindicalizado() && entry.getValue().getMembroSindicato().getId().equals(idSindicato)) {
                            throw new HaOutroEmpregadoComEstaIdentificacaoDeSindicatoException();
                        }
                    }

                    double taxaSindical;
                    if (taxaSindicalStr == null || taxaSindicalStr.isEmpty()) {
                        throw new TaxaSindicalNaoPodeSerNulaException();
                    }
                    try {
                        taxaSindical = Double.parseDouble(taxaSindicalStr.replace(',', '.'));
                        if (taxaSindical < 0) {
                            throw new TaxaSindicalDeveSerNaoNegativaException();
                        }
                    } catch (NumberFormatException e) {
                        throw new TaxaSindicalDeveSerNumericaException();
                    }
                    empregado.setSindicalizado(true, idSindicato, taxaSindical);
                } else {
                    empregado.setSindicalizado(false, null, 0.0);
                }
                break;
            case "metodoPagamento":
                String metodo = args[0];
                switch (metodo) {
                    case "emMaos":
                        empregado.setMetodoPagamento(new MetodoPagamentoEmMaos());
                        break;
                    case "banco":
                        String banco = args[1];
                        String agencia = args[2];
                        String conta = args[3];
                        if (banco == null || banco.isEmpty()) throw new BancoNaoPodeSerNuloException();
                        if (agencia == null || agencia.isEmpty()) throw new AgenciaNaoPodeSerNulaException();
                        if (conta == null || conta.isEmpty()) throw new ContaCorrenteNaoPodeSerNulaException();
                        empregado.setMetodoPagamento(new MetodoPagamentoBanco(banco, agencia, conta));
                        break;
                    case "correios":
                        empregado.setMetodoPagamento(new MetodoPagamentoCorreios());
                        break;
                    default:
                        throw new MetodoPagamentoInvalidoException();
                }
                break;
            case "salario":
                 if (args[0] == null || args[0].trim().isEmpty()) {
                    throw new SalarioNaoPodeSerNuloException(); 
                }
                double salarioValue;
                try {
                    salarioValue = Double.parseDouble(args[0].replace(',', '.'));
                } catch (NumberFormatException e) {
                    throw new SalarioDeveSerNumericoException();
                }

                if (salarioValue < 0) {
                    throw new SalarioDeveSerNaoNegativoException();
                }
                empregado.setSalario(salarioValue);
                break;
            case "comissao":
                if (!(empregado instanceof EmpregadoComissionado)) {
                    throw new EmpregadoNaoEhComissionadoException();
                }
                if (args[0] == null || args[0].trim().isEmpty()) {
                    throw new ComissaoNaoPodeSerNulaException();
                }
                double comissaoValue;
                try {
                    comissaoValue = Double.parseDouble(args[0].replace(',', '.'));
                    if (comissaoValue < 0) {
                        throw new ComissaoDeveSerNaoNegativaException();
                    }
                } catch (NumberFormatException e) {
                    throw new ComissaoDeveSerNumericaException();
                }
                ((EmpregadoComissionado) empregado).setTaxaDeComissao(comissaoValue);
                break;
            default:
                throw new AtributoNaoExisteException();
        }
    }

    public void undo() throws Exception {
        // fazer
    }

    public void redo() throws Exception {
        // fazer
    }

    public void rodaFolha(String data) throws Exception {
    LocalDate d;
    try {
        d = parseDateFlexible(data);
    } catch (DateTimeParseException e) {
        throw new DataInvalidaException();
    }
    String nome = String.format("folha-%04d-%02d-%02d.txt", d.getYear(), d.getMonthValue(), d.getDayOfMonth());
    rodaFolha(data, nome);
}

    public void rodaFolha(String data, String saida) throws Exception {
    if (data == null || data.trim().isEmpty()) throw new DataInvalidaException();
    LocalDate currentDate;
    try {
        currentDate = parseDateFlexible(data);
    } catch (DateTimeParseException e) {
        throw new DataInvalidaException();
    }

    LocalDate comissionadoFirstPay = LocalDate.of(2005, 1, 14);

    java.util.List<Empregado> _emps = new java.util.ArrayList<>();
    for (Map.Entry<String, Empregado> e : this.empregados.entrySet()) _emps.add(e.getValue());
    _emps.sort((a,b) -> a.getNome().compareToIgnoreCase(b.getNome()));

    java.util.List<String> horistasLines = new java.util.ArrayList<>();
    double totalHoristasNormalHours = 0.0;
    double totalHoristasExtraHours = 0.0;
    double totalHoristasBruto = 0.0;
    double totalHoristasDescontos = 0.0;
    double totalHoristasLiquido = 0.0;

    java.util.List<String> assalariadosLines = new java.util.ArrayList<>();
    double totalAssalariadosBruto = 0.0;
    double totalAssalariadosDescontos = 0.0;
    double totalAssalariadosLiquido = 0.0;

    java.util.List<String> comissionadosLines = new java.util.ArrayList<>();
    double totalComissionadosBruto = 0.0;
    double totalComissionadosDescontos = 0.0;
    double totalComissionadosLiquido = 0.0;

    java.text.DecimalFormatSymbols sym = new java.text.DecimalFormatSymbols();
    sym.setDecimalSeparator(',');
    java.text.DecimalFormat df2 = new java.text.DecimalFormat("0.00");
    df2.setDecimalFormatSymbols(sym);

    for (Empregado emp : _emps) {
        LocalDate periodStart = null;
        LocalDate periodEnd = null;
        boolean isPayday = false;

        if (emp instanceof EmpregadoHorista) {
            if (currentDate.getDayOfWeek() == java.time.DayOfWeek.FRIDAY) {
                EmpregadoHorista h = (EmpregadoHorista) emp;
                LocalDate contract = null;
                for (CartaoDePonto c : h.cartoesDePonto) {
                    if (contract == null || c.getData().isBefore(contract)) contract = c.getData();
                }
                periodEnd = currentDate;
                periodStart = currentDate.minusDays(6);
                if (contract != null && !contract.isAfter(periodEnd)) isPayday = true;
            } else {
                periodEnd = currentDate;
                periodStart = currentDate.minusDays(6);
            }
        } if (emp instanceof EmpregadoComissionado) {
            periodEnd = currentDate;
            periodStart = currentDate.minusDays(13);
            if (!currentDate.isBefore(comissionadoFirstPay)) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(comissionadoFirstPay, currentDate);
                if (currentDate.getDayOfWeek() == java.time.DayOfWeek.FRIDAY && days % 14 == 0) isPayday = true;
            }
        } if (emp instanceof EmpregadoAssalariado) {
            periodEnd = currentDate;
            periodStart = currentDate.withDayOfMonth(1);
            LocalDate lastDay = currentDate.withDayOfMonth(currentDate.lengthOfMonth());
            if (currentDate.equals(lastDay)) isPayday = true;
        }

        if (!isPayday) continue;

        LocalDate periodEndExclusive = periodEnd.plusDays(1);

        double gross = 0.0;
        double deductions = 0.0;
        double normalHours = 0.0;
        double extraHours = 0.0;

        if (emp instanceof EmpregadoHorista) {
            EmpregadoHorista h = (EmpregadoHorista) emp;
            double hourly = h.getSalario();
            java.util.Map<LocalDate, Double> hoursByDay = new java.util.HashMap<>();
            for (CartaoDePonto c : h.cartoesDePonto) {
                LocalDate d2 = c.getData();
                if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                    hoursByDay.put(d2, hoursByDay.getOrDefault(d2, 0.0) + c.getHoras());
                }
            }
            for (Double hrs : hoursByDay.values()) {
                normalHours += Math.min(hrs, 8.0);
                if (hrs > 8.0) extraHours += (hrs - 8.0);
            }
            gross = normalHours * hourly + extraHours * hourly * 1.5;
        } else if (emp instanceof EmpregadoAssalariado) {
            EmpregadoAssalariado a = (EmpregadoAssalariado) emp;
            gross = a.getSalario();
        } else if (emp instanceof EmpregadoComissionado) {
                EmpregadoComissionado c = (EmpregadoComissionado) emp;
                double base = c.getSalario() * 12.0 / 26.0;
            double commissions = 0.0;
            for (ResultadoDeVenda v : c.getResultadosDeVenda()) {
                LocalDate d2 = v.getData();
                if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                    commissions += v.getValor() * c.getTaxaDeComissao();
                }
            }
            gross = base + commissions;
        }

        if (emp.isSindicalizado()) {
            if (gross > 0.0) {
                MembroSindicato ms = emp.getMembroSindicato();
                double taxaDiaria = ms.getTaxaSindical();
                long daysBetween;
                if (emp instanceof EmpregadoHorista) {
                    java.time.LocalDate lastPay = null;
                    java.time.LocalDate probe = periodEnd.minusDays(7);
                    while (probe.isAfter(java.time.LocalDate.of(1900,1,1))) {
                        java.time.LocalDate pStart = probe.minusDays(6);
                        java.time.LocalDate pEndEx = probe.plusDays(1);
                        double hourly = ((EmpregadoHorista) emp).getSalario();
                        java.util.Map<java.time.LocalDate, Double> hrs = new java.util.HashMap<>();
                        for (CartaoDePonto cp : ((EmpregadoHorista) emp).cartoesDePonto) {
                            java.time.LocalDate d2 = cp.getData();
                            if ((d2.isEqual(pStart) || d2.isAfter(pStart)) && d2.isBefore(pEndEx)) {
                                hrs.put(d2, hrs.getOrDefault(d2, 0.0) + cp.getHoras());
                            }
                        }
                        double normalPrev = 0.0, extraPrev = 0.0;
                        for (Double h : hrs.values()) {
                            normalPrev += Math.min(h, 8.0);
                            if (h > 8.0) extraPrev += (h - 8.0);
                        }
                        double grossPrev = (normalPrev * hourly) + (extraPrev * hourly * 1.5);
                        if (grossPrev > 0.0) {
                            lastPay = probe;
                            break;
                        }
                        probe = probe.minusDays(7);
                    }
                    if (lastPay == null) {
                        
                        daysBetween = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEndExclusive);
                    } else {
                        daysBetween = java.time.temporal.ChronoUnit.DAYS.between(lastPay.plusDays(1), periodEndExclusive);
                    }
                } else {
                    daysBetween = java.time.temporal.ChronoUnit.DAYS.between(periodStart, periodEndExclusive);
                }
                deductions += taxaDiaria * daysBetween;
        for (TaxaServico ts : ms.getTaxasDeServico()) {
                LocalDate d2 = ts.getData();
                if ((d2.isEqual(periodStart) || d2.isAfter(periodStart)) && d2.isBefore(periodEndExclusive)) {
                    deductions += ts.getValor();
                }
            }
            }

        double net = gross - deductions;
        if (net < 0) net = 0.0;

        String metodoStr = "";
        MetodoPagamento mp = emp.getMetodoPagamentoObjeto();
        if (mp instanceof MetodoPagamentoEmMaos) {
            metodoStr = "Em maos";
        } else if (mp instanceof MetodoPagamentoBanco) {
            MetodoPagamentoBanco mb = (MetodoPagamentoBanco) mp;
            metodoStr = String.format("%s, Ag. %s CC %s", mb.getBanco(), mb.getAgencia(), mb.getContaCorrente());
        } else if (mp instanceof MetodoPagamentoCorreios) {
            metodoStr = "Correios";
        }

        if (emp instanceof EmpregadoHorista) {
            String line = String.format("%-38s %5s %5s %13s %9s %15s %s",
                    emp.getNome(),
                    (normalHours == Math.floor(normalHours) ? String.valueOf((int) normalHours) : String.format("%.1f", normalHours).replace('.', ',')),
                    (extraHours == Math.floor(extraHours) ? String.valueOf((int) extraHours) : String.format("%.1f", extraHours).replace('.', ',')),
                    df2.format(gross),
                    df2.format(deductions),
                    df2.format(net),
                    metodoStr);
            horistasLines.add(line);

            totalHoristasNormalHours += normalHours;
            totalHoristasExtraHours += extraHours;
            totalHoristasBruto += gross;
            totalHoristasDescontos += deductions;
            totalHoristasLiquido += net;
        } else if (emp instanceof EmpregadoAssalariado) {
            String line = String.format("%-45s %13s %9s %15s %s",
                    emp.getNome(),
                    df2.format(gross),
                    df2.format(deductions),
                    df2.format(net),
                    metodoStr);
            assalariadosLines.add(line);

            totalAssalariadosBruto += gross;
            totalAssalariadosDescontos += deductions;
            totalAssalariadosLiquido += net;
        } else if (emp instanceof EmpregadoComissionado) {
            String line = String.format("%-21s %7s %8s %8s %12s %8s %14s %s",
                    emp.getNome(),
                    df2.format(0.0),
                    df2.format(0.0),
                    df2.format(0.0),
                    df2.format(gross),
                    df2.format(deductions),
                    df2.format(net),
                    metodoStr);
            comissionadosLines.add(line);

            totalComissionadosBruto += gross;
            totalComissionadosDescontos += deductions;
            totalComissionadosLiquido += net;
        }
    } 

    StringBuilder sb = new StringBuilder();
    sb.append(String.format("FOLHA DE PAGAMENTO DO DIA %04d-%02d-%02d\n", currentDate.getYear(), currentDate.getMonthValue(), currentDate.getDayOfMonth()));
    sb.append("====================================\n\n");
    sb.append("===============================================================================================================================\n");
    sb.append("===================== HORISTAS ================================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                                 Horas Extra Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("==================================== ===== ===== ============= ========= =============== ======================================\n");
    for (String l : horistasLines) {
        sb.append(l).append("\n");
    }
    sb.append("\n");
    sb.append(String.format("TOTAL HORISTAS                          %d     %d        %s     %s          %s\n\n",
            (int) totalHoristasNormalHours,
            (int) totalHoristasExtraHours,
            df2.format(totalHoristasBruto),
            df2.format(totalHoristasDescontos),
            df2.format(totalHoristasLiquido)
    ));

    sb.append("===============================================================================================================================\n");
    sb.append("===================== ASSALARIADOS ============================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                                             Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("================================================ ============= ========= =============== ======================================\n\n");
    for (String l : assalariadosLines) {
        sb.append(l).append("\n");
    }
    sb.append("\n");
    sb.append(String.format("TOTAL ASSALARIADOS                                        %s      %s            %s\n\n",
            df2.format(totalAssalariadosBruto),
            df2.format(totalAssalariadosDescontos),
            df2.format(totalAssalariadosLiquido)
    ));

    sb.append("===============================================================================================================================\n");
    sb.append("===================== COMISSIONADOS ===========================================================================================\n");
    sb.append("===============================================================================================================================\n");
    sb.append("Nome                  Fixo     Vendas   Comissao Salario Bruto Descontos Salario Liquido Metodo\n");
    sb.append("===================== ======== ======== ======== ============= ========= =============== ======================================\n\n");
    for (String l : comissionadosLines) {
        sb.append(l).append("\n");
    }
    sb.append("\n");
    sb.append(String.format("TOTAL COMISSIONADOS       %s     %s     %s          %s      %s            %s\n",
            df2.format(0.0),
            df2.format(0.0),
            df2.format(0.0),
            df2.format(totalComissionadosBruto),
            df2.format(totalComissionadosDescontos),
            df2.format(totalComissionadosLiquido)));

    double totalFolha = totalHoristasBruto + totalAssalariadosBruto + totalComissionadosBruto;
    sb.append(String.format("TOTAL FOLHA: %s\n", df2.format(totalFolha)));

    java.nio.file.Files.write(java.nio.file.Paths.get(saida), sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
    
    public String totalFolha(LocalDate data) {
        Objects.requireNonNull(data, "Data invalida.");

        if (isEndOfMonth(data)) {
            return sumBrutoAssalariados();
        }

        if (isComissionadoPayday(data)) {
            LocalDate inicioPeriodo = data.minusDays(13);
            return sumBrutoComissionados(inicioPeriodo, data);
        }

        if (data.getDayOfWeek() == DayOfWeek.FRIDAY) {
            LocalDate inicioSemana = data.minusDays(6); 
            return sumBrutoHoristas(inicioSemana, data);
        }

        return "0,00";
    }


    private boolean isEndOfMonth(LocalDate d) {
        return d.equals(d.withDayOfMonth(d.lengthOfMonth()));
    }


    private boolean isComissionadoPayday(LocalDate d) {
        if (d.getDayOfWeek() != DayOfWeek.FRIDAY) return false;
        LocalDate base = LocalDate.of(2005, 1, 1); 
        long dias = ChronoUnit.DAYS.between(base, d);
        return (dias % 14) == 13;
    }

    private String sumBrutoHoristas(LocalDate inicio, LocalDate fim) {
        double total = 0.00;
        for (Empregado e : this.empregados.values()) { 
            if (e instanceof EmpregadoHorista) {
                EmpregadoHorista h = (EmpregadoHorista) e;
                double brutoSemana = calcularBrutoHorista(h, inicio, fim);
                total += brutoSemana;
            }
        }
        return String.format("%.2f", total).replace('.', ',');
    }


    private String sumBrutoComissionados(LocalDate inicio, LocalDate fim) {
        double total = 0.00;
        for (Empregado e : this.empregados.values()) { 
            if (e instanceof EmpregadoComissionado) {
                EmpregadoComissionado c = (EmpregadoComissionado) e;
                double fixoQuinzenal = c.getSalarioMensal() * 12.0 / 26.0;
                double valorVendas = somarVendas(c, inicio, fim); 
                double comissao = valorVendas * c.getTaxaDeComissao();
                total += (fixoQuinzenal + comissao);
            }
        }
        return String.format("%.2f", total).replace('.', ',');
    }

    private String sumBrutoAssalariados() {
        double total = 0.00;
        for (Empregado e : this.empregados.values()) { 
            if (e instanceof EmpregadoAssalariado && !(e instanceof EmpregadoComissionado)) {
                EmpregadoAssalariado a = (EmpregadoAssalariado) e;
                total += a.getSalarioMensal();
            }
        }
        return String.format("%.2f", total).replace('.', ',');
    }

    private double calcularBrutoHorista(EmpregadoHorista h, LocalDate inicio, LocalDate fim) {
        double total = 0.0;
        for (CartaoDePonto cp : h.cartoesDePonto) {
            LocalDate d = cp.getData();
            if (!d.isBefore(inicio) && !d.isAfter(fim)) {
                double horas  = cp.getHoras();
                double normal = Math.min(horas, 8.0);
                double extra  = Math.max(0.0, horas - 8.0);
                double vh = h.getSalarioPorHora();     

                total += normal * vh + extra * vh * 1.5;
            }
        }
        return total;
    }

    private double somarVendas(EmpregadoComissionado c, LocalDate inicio, LocalDate fim) {
        double total = 0.0;
        for (ResultadoDeVenda v : c.getResultadosDeVenda()) {
            LocalDate d = v.getData();
            if (!d.isBefore(inicio) && !d.isAfter(fim)) {
                total += v.getValor();
            }
        }
        return total;
    }

    public String getEmpregadoPorNome(String nome, String indice) throws Exception {
        if (nome == null || nome.isEmpty()) {
            throw new NomeNaoPodeSerNuloException();
        }
        int count = 0;

        int indiceInt = Integer.parseInt(indice);
        for (Map.Entry<String, Empregado> entry : empregados.entrySet()) {
            if (nome.equals(entry.getValue().getNome())) {
                count++;
                if (count == indiceInt) {
                    return entry.getKey();
                }
            }
        }
        throw new NaoHaEmpregadoComEsseNomeException();
    }
    
    public String getHorasNormaisTrabalhadas(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoEhHoristaException();
        }

        LocalDate inicio, fim;
        try {
            inicio = parseDateFlexible(dataInicial);
        } catch (DateTimeParseException e) {
            throw new DataInicialInvalidaException();
        }
        try {
            fim = parseDateFlexible(dataFinal);
        } catch (DateTimeParseException e) {
            throw new DataFinalInvalidaException();
        }

        if (inicio.isAfter(fim)) {
            throw new DataInicialPosteriorFinalException();
        }

        EmpregadoHorista horista = (EmpregadoHorista) empregado;
        Map<LocalDate, Double> horasPorDia = new HashMap<>();
        for (CartaoDePonto cartao : horista.cartoesDePonto) {
            LocalDate data = cartao.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                horasPorDia.put(data, horasPorDia.getOrDefault(data, 0.0) + cartao.getHoras());
            }
        }
        double totalNormais = 0.0;
        for (double horasDia : horasPorDia.values()) {
            totalNormais += Math.min(horasDia, 8);
        }
        if (totalNormais == Math.floor(totalNormais)) {
            return String.valueOf((int) totalNormais);
        }
        return String.format("%.1f", totalNormais).replace('.', ',');
    }

    public String getHorasExtrasTrabalhadas(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoHorista)) {
            throw new EmpregadoNaoEhHoristaException();
        }

        LocalDate inicio, fim;
    try {
        inicio = parseDateFlexible(dataInicial);
    } catch (DateTimeParseException e) {
        throw new DataInicialInvalidaException();
    }
    try {
        fim = parseDateFlexible(dataFinal);
    } catch (DateTimeParseException e) {
        throw new DataFinalInvalidaException();
    }

    if (inicio.isAfter(fim)) {
        throw new DataInicialPosteriorFinalException();
    }

        EmpregadoHorista horista = (EmpregadoHorista) empregado;
        Map<LocalDate, Double> horasPorDia = new HashMap<>();
        for (CartaoDePonto cartao : horista.cartoesDePonto) {
            LocalDate data = cartao.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                horasPorDia.put(data, horasPorDia.getOrDefault(data, 0.0) + cartao.getHoras());
            }
        }
        double totalExtras = 0.0;
        for (double horasDia : horasPorDia.values()) {
            totalExtras += horasDia > 8 ? horasDia - 8 : 0; 
        }
        if (totalExtras == Math.floor(totalExtras)) {
            return String.valueOf((int) totalExtras);
        }
        return String.format("%.1f", totalExtras).replace('.', ',');
    }

    public String getVendasRealizadas(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!(empregado instanceof EmpregadoComissionado)) {
            throw new EmpregadoNaoEhComissionadoException();
        }

        
        LocalDate inicio, fim;
        try {
            inicio = parseDateFlexible(dataInicial);
        } catch (DateTimeParseException e) {
            throw new DataInicialInvalidaException();
        }
        try {
            fim = parseDateFlexible(dataFinal);
        } catch (DateTimeParseException e) {
            throw new DataFinalInvalidaException();
        }
        if (inicio.isAfter(fim)) {
            throw new DataInicialPosteriorFinalException();
        }

        EmpregadoComissionado comissionado = (EmpregadoComissionado) empregado;
        double totalVendas = 0.0;
        for (ResultadoDeVenda venda : comissionado.getResultadosDeVenda()) {
            LocalDate data = venda.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                totalVendas += venda.getValor();
            }
        }
        return String.format("%.2f", totalVendas).replace('.', ',');
    }

    public String getTaxasServico(String empId, String dataInicial, String dataFinal) throws Exception {
        if (empId == null || empId.isEmpty()) {
            throw new IdentificacaoEmpregadoNulaException();
        }
        if (!this.empregados.containsKey(empId)) {
            throw new EmpregadoNaoExisteException();
        }
        Empregado empregado = this.empregados.get(empId);
        if (!empregado.isSindicalizado()) {
            throw new EmpregadoNaoEhSindicalizadoException();
        }

        LocalDate inicio, fim;
        try {
            inicio = parseDateFlexible(dataInicial);;
        } catch (DateTimeParseException e) {
            throw new DataInicialInvalidaException();
        }
        try {
            fim = parseDateFlexible(dataFinal);;
        } catch (DateTimeParseException e) {
            throw new DataFinalInvalidaException();
        }
        if (inicio.isAfter(fim)) {
            throw new DataInicialPosteriorFinalException();
        }

        MembroSindicato sindicato = empregado.getMembroSindicato();
        double totalTaxas = 0.0;
        for (TaxaServico taxa : sindicato.getTaxasDeServico()) {
            LocalDate data = taxa.getData();
            if ((data.isEqual(inicio) || data.isAfter(inicio)) && data.isBefore(fim)) {
                totalTaxas += taxa.getValor();
            }
        }
        return String.format("%.2f", totalTaxas).replace('.', ',');
    }

    private LocalDate parseDateFlexible(String dateStr) throws DateTimeParseException {
        if (dateStr == null) {
            throw new DateTimeParseException("Date is null", dateStr, 0);
        }
        String s = dateStr.trim();
        String[] parts = s.split("/");
        if (parts.length != 3) {
            throw new DateTimeParseException("Invalid date format", dateStr, 0);
        }
        try {
            int day = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim());
            int year = Integer.parseInt(parts[2].trim());
            if (month < 1 || month > 12) {
                throw new DateTimeParseException("Invalid month", dateStr, 0);
            }
            YearMonth ym = YearMonth.of(year, month);
            int maxDay = ym.lengthOfMonth();
            if (day < 1 || day > maxDay) {
                throw new DateTimeParseException("Invalid day", dateStr, 0);
            }
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException e) {
            throw new DateTimeParseException("Invalid date", dateStr, 0);
        } catch (DateTimeException e) {
            throw new DateTimeParseException("Invalid date", dateStr, 0);
        }
    }
}
