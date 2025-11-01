# WePayU — Sistema de Folha de Pagamento (Java)

Projeto acadêmico (UFAL/IC) de um **sistema de folha de pagamento** escrito em Java, com foco em regras de negócio clássicas de payroll: cadastro de empregados (horistas, assalariados e comissionados), cartões de ponto, resultados de venda, sindicato e taxas de serviço, **agendas de pagamento** configuráveis, **cálculo da folha** com geração de arquivo de saída, além de **undo/redo** de comandos.

> Pacote base: `br.ufal.ic.p2.wepayu`  
> Formatos: datas em `D/M/AAAA` e números impressos com **vírgula** como separador decimal (pt-BR).

---

## Sumário

- [Arquitetura e principais componentes](#arquitetura-e-principais-componentes)
- [Regras de negócio](#regras-de-negócio)
  - [Tipos de empregado](#tipos-de-empregado)
  - [Cartão de ponto (horistas)](#cartão-de-ponto-horistas)
  - [Resultados de venda (comissionados)](#resultados-de-venda-comissionados)
  - [Sindicato e taxas](#sindicato-e-taxas)
  - [Agendas de pagamento](#agendas-de-pagamento)
  - [Cálculo da folha e geração de arquivo](#cálculo-da-folha-e-geração-de-arquivo)
  - [Arredondamento](#arredondamento)
- [API pública (Facade)](#api-pública-facade)
  - [Consultas de horas, vendas e taxas](#consultas-de-horas-vendas-e-taxas)
- [Erros e mensagens](#erros-e-mensagens)
- [Como rodar (sem build system)](#como-rodar-sem-build-system)
- [Testes de aceitação (EasyAccept)](#testes-de-aceitação-easyaccept)

---

## Arquitetura e principais componentes

```
WePayU
  └─src/
    └─ br/ufal/ic/p2/wepayu/
       ├─ Main.java                     # Runner para EasyAccept
        ├─ br
          ├─ ufal
            ├─ ic
              ├─ p2
                ├─ wepayu
                   ├─ Facade.java                   # Fachada de interação (API pública)
                   ├─ SistemaFolha.java             # Núcleo de regras de negócio
                   ├─ models/
                   │  ├─ Empregado.java             # Classe base
                   │  ├─ EmpregadoHorista.java
                   │  ├─ EmpregadoAssalariado.java
                   │  ├─ EmpregadoComissionado.java
                   │  ├─ CartaoDePonto.java
                   │  ├─ ResultadoDeVenda.java
                   │  ├─ MembroSindicato.java
                   │  └─ TaxaServico.java
                   ├─ payment/
                   |  │  ├─ MetodoPagamento.java       # Interface
                   |  │  ├─ MetodoPagamentoEmMaos.java
                   |  │  ├─ MetodoPagamentoCorreios.java
                   |  │  └─ MetodoPagamentoBanco.java
                   └─ Exception/                    # Exceções de domínio (validações)
```

- **`Facade`** expõe métodos de alto nível usados nos testes e por UIs externas.
- **`SistemaFolha`** contém praticamente toda a lógica: cadastro/alterações, agenda, cálculo, geração de arquivos, e pilhas de **undo/redo**.
- **Modelos** separam responsabilidades (empregados, pagamentos, sindicato, etc.).
- **EasyAccept** (em `Main.java`) executa os scripts `tests/usX.txt` (não inclusos aqui).

---

## Regras de negócio

### Tipos de empregado

- **Horista**
  - Recebe por hora trabalhada; **8h/dia** contam como horas normais; o que excede é **hora extra**.
  - Dia de pagamento: **toda sexta-feira**.
- **Assalariado**
  - Recebe **salário mensal fixo**.
  - Dia de pagamento: **último dia útil do mês** (se cair em sábado/domingo, antecipa para sexta).
- **Comissionado**
  - Recebe **fixo quinzenal** (2 semanas) **+ comissão** sobre vendas do período.
  - Dia de pagamento: **sextas alternadas**, ancoradas em **14/01/2005** (a partir dessa data, a cada 2 semanas).

### Cartão de ponto (horistas)

- `lancaCartao(empId, data, horas)` aceita `horas > 0` (ponto inválido gera exceção).
- **Horas normais** (até 8/dia) e **extras** (acima de 8/dia) são agregadas **por dia** dentro do período da folha semanal.
- Pagamento do horista na folha:
  - **horas_normais × valor_hora + horas_extras × valor_hora × 1,5**.

### Resultados de venda (comissionados)

- `lancaVenda(empId, data, valor)` aceita `valor > 0`.
- Comissão: `valor_vendas_no_período × taxa_de_comissão`.
- No dia do pagamento, comissionados recebem: **fixo quinzenal + comissão**.

### Sindicato e taxas

- Empregado pode ser **sindicalizado** (`sindicalizado=true|false`), com **ID de sindicato único** e **taxa sindical diária**.
- **Taxas de serviço** (lançadas por **ID do sindicato**) podem ser cobradas nas folhas subsequentes.
- Descontos aplicados na folha:
  - **Taxa sindical diária × nº de dias do período** pago.
  - **Taxas de serviço** cujo **`data`** esteja no período da folha (ver janelas abaixo).
  - Para horistas, há controle de **última data de cobrança** para não duplicar taxa sindical diária.

### Agendas de pagamento

Agendas reconhecidas pelo sistema (padrão inicial):
- `"semanal 5"`  → toda **sexta** (ISO `DayOfWeek`, onde **2ª=1 … domingo=7**).
- `"semanal 2 5"` → a cada **2 semanas na sexta** (quinzenal).
- `"mensal $"`  → **último dia útil** do mês.
  
Você pode **criar novas agendas** com `criarAgendaDePagamentos(descricao)`, desde que a descrição siga um destes formatos:

- `semanal D` (1–7)  
- `semanal N D` (N=1–52; D=1–7)  
- `mensal $` (último dia útil)  
- `mensal DD` (dia do mês **1–28**)

> **Janelas de apuração por tipo (para a folha do “dia D”):**
>
> - **Horista**: de **D−6** até **D** (inclusive).  
> - **Comissionado**: de **D−13** até **D** (inclusive).  
> - **Assalariado**: do **1º dia do mês** até **D** (inclusive).  
>
> Para consultas agregadas (API `get*`), o intervalo é **[início, fim)**: **inclui a data inicial e exclui a final**.

### Cálculo da folha e geração de arquivo

- `rodaFolha("D/M/AAAA")` ou `rodaFolha("D/M/AAAA", "saida.txt")`.
- Gera um arquivo texto com três seções: **HORISTAS**, **ASSALARIADOS** e **COMISSIONADOS**, contendo por empregado:
  - Totais do período, descontos e **salário líquido**, além do **método de pagamento** (ver abaixo).
- **Métodos de pagamento** suportados:
  - `emMaos` → impresso como **“Em maos”**.
  - `banco` → **`<Banco>, Ag. <Agencia> CC <Conta>`** (exige os três campos).
  - `correios` → impresso como **“Correios”** (ou “Correios, end<numero>” se o endereço terminar em número).
- Nome padrão do arquivo (se não informado): `folha-AAAA-MM-DD.txt`.

### Arredondamento

- Todos os valores monetários são **truncados** para **duas casas decimais** (**`RoundingMode.DOWN`**).  
- A impressão usa separador decimal **vírgula** (`0,00`).

---

## API pública (Facade)

> A `Facade` encaminha todas as operações para `SistemaFolha`. Métodos típicos expostos:
>
> - `encerrarSistema()` — bloqueia novos comandos após o encerramento.
> - `criarAgendaDePagamentos(descricao)`
> - `zerarSistema()` / `getNumeroDeEmpregados()`
> - `addEmpregado(id, nome, endereco, tipo, salario[, comissao])`
> - `getAtributoEmpregado(empId, atributo)`
> - `removerEmpregado(empId)`
> - `alteraEmpregado(...)` (sobrecargas para `nome`, `endereco`, `tipo`, `sindicalizado`, `metodoPagamento`, `salario`, `comissao`)
> - `lancaCartao(empId, data, horas)`
> - `lancaVenda(empId, data, valor)`
> - `lancaTaxaServico(idSindicato, data, valor)`
> - `rodaFolha(data)` / `rodaFolha(data, saida)`
> - `getEmpregadoPorNome(nome, indice)`

### Atributos de `getAtributoEmpregado` e `alteraEmpregado`

- **Leitura (`getAtributoEmpregado`)**:
  - `nome`, `endereco`, `tipo` (`horista` | `assalariado` | `comissionado`),
  - `salario`, `comissao`, `agendaPagamento`,
  - `sindicalizado` (`true`/`false`), `idSindicato`, `taxaSindical`,
  - `metodoPagamento` (`emMaos` | `banco` | `correios`),
  - `banco`, `agencia`, `contaCorrente` (somente se `metodoPagamento` = `banco`).

- **Edição (`alteraEmpregado`)** (sobrecargas):
  - `("nome", novoValor)`; `("endereco", novoValor)`
  - `("salario", novoValor)`; `("comissao", novaTaxa)`
  - `("metodoPagamento", "emMaos")`  
    `("metodoPagamento", "correios")`  
    `("metodoPagamento", "banco", banco, agencia, conta)`
  - `("sindicalizado", "true", idSindicato, taxaSindical)`  
    `("sindicalizado", "false")`
  - `("tipo", "horista", [novoSalarioHora])`  
    `("tipo", "assalariado", [novoSalarioMensal])`  
    `("tipo", "comissionado", [novoSalarioFixo], [novaTaxaComissao])`  
    > Ao mudar o **tipo**, o sistema recria o empregado com os novos parâmetros e **preserva** método de pagamento e vínculo sindical (se houver).

### Consultas de horas, vendas e taxas

- `getHorasNormaisTrabalhadas(empId, dataInicial, dataFinal)` → horas normais no intervalo **[início, fim)**.
- `getHorasExtrasTrabalhadas(empId, dataInicial, dataFinal)` → horas extras em **[início, fim)**.
- `getVendasRealizadas(empId, dataInicial, dataFinal)` → soma de vendas em **[início, fim)**.
- `getTaxasServico(empId, dataInicial, dataFinal)` → soma das taxas de serviço do sindicato em **[início, fim)**.

---

## Erros e mensagens

As validações usam exceções específicas, por exemplo:

- `NomeNaoPodeSerNuloException`, `EnderecoNaoPodeSerNuloException`
- `SalarioNaoPodeSerNuloException`, `SalarioDeveSerNumericoException`, `SalarioDeveSerNaoNegativoException`
- `ComissaoNaoPodeSerNulaException`, `ComissaoDeveSerNumericaException`, `ComissaoDeveSerNaoNegativaException`
- `IdentificacaoEmpregadoNulaException`, `EmpregadoNaoExisteException`
- `EmpregadoNaoEhHoristaException`, `EmpregadoNaoEhComissionadoException`, `EmpregadoNaoEhSindicalizadoException`
- `IdentificacaoSindicatoNulaException`, `HaOutroEmpregadoComEstaIdentificacaoDeSindicatoException`
- `ValorDeveSerPositivoException` (cartões de ponto, vendas, taxas), `MetodoPagamentoInvalidoException`
- `BancoNaoPodeSerNuloException`, `AgenciaNaoPodeSerNulaException`, `ContaCorrenteNaoPodeSerNulaException`
- `DataInvalidaException`, `DataInicialInvalidaException`, `DataFinalInvalidaException`, `DataInicialPosteriorFinalException`
- `AgendaDePagamentosJaExisteException`, `DescricaoDeAgendaInvalidaException`
- `NaoPodeDarComandoDepoisDeEncerrarSistema` (após `encerrarSistema()`)
- `NaoHaEmpregadoComEsseNomeException` (em buscas por nome)  
- Mensagens seguem o padrão definido nas classes de `Exception/`.

---

## Como rodar (sem build system)

1. **Baixe o JAR do EasyAccept** e coloque em `lib/` (ex.: `lib/easyaccept.jar`).  
   > O *Main* está preparado para rodar os scripts `tests/us1.txt` ... `tests/us10_1.txt`.
2. **Compile** (Linux/macOS):
   ```bash
   mkdir -p out
   javac -encoding UTF-8 -cp lib/easyaccept.jar -d out $(find . -name "*.java")
   ```
   No Windows (PowerShell):
   ```powershell
   md out
   dir -Recurse -Filter *.java | % { $_.FullName } | Set-Content sources.txt
   javac -encoding UTF-8 -cp "lib\easyaccept.jar" -d out @sources.txt
   ```
3. **Execute**:
   ```bash
   java -cp "out:lib/easyaccept.jar" Main
   # (Windows) java -cp "out;lib\easyaccept.jar" Main
   ```
4. **Folha**: após `rodaFolha("D/M/AAAA")`, verifique o arquivo `folha-AAAA-MM-DD.txt` criado no diretório de execução.

> **Formato de data aceito:** `D/M/AAAA` (com ou sem zeros à esquerda).  
> **Entrada numérica:** aceita **ponto** ou **vírgula** como separador decimal; a saída impressa usa **vírgula**.

---

## Testes de aceitação (EasyAccept)

- Os scripts `tests/us*.txt` são executados pelo `Main` na ordem:
  `us1`, `us1_1`, `us2`, `us2_1`, ..., `us10`, `us10_1`.
- Para rodar **um** script específico, altere o `Main.java` ou rode EasyAccept diretamente apontando para `br.ufal.ic.p2.wepayu.Facade`.

