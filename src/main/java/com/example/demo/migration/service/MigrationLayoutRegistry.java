package com.example.demo.migration.service;

import com.example.demo.migration.domain.FieldType;
import com.example.demo.migration.domain.MigrationModule;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class MigrationLayoutRegistry {

    private final Map<MigrationModule, LayoutSpec> layouts = new EnumMap<>(MigrationModule.class);

    public MigrationLayoutRegistry() {
        layouts.put(MigrationModule.CLIENTES, new LayoutSpec(MigrationModule.CLIENTES, "CLIENTES", "CODIGO", List.of(
                field("CODIGO", FieldType.INTEGER, 10, true, "Codigo sequencial"),
                field("TIPOPESSOA", FieldType.TEXT, 1, true, "F ou J"),
                field("NOME", FieldType.TEXT, 100, true, "Nome ou razao social"),
                field("APELIDO", FieldType.TEXT, 100, false, "Apelido ou nome fantasia"),
                field("ENDERECO", FieldType.TEXT, 60, false, "Endereco"),
                field("COMPLEMENT", FieldType.TEXT, 60, false, "Complemento"),
                field("NUM_END", FieldType.TEXT, 60, false, "Numero"),
                field("BAIRRO", FieldType.TEXT, 60, false, "Bairro"),
                field("CIDADE", FieldType.TEXT, 50, false, "Cidade"),
                field("CEP", FieldType.TEXT, 10, false, "CEP"),
                field("TELEFONE", FieldType.TEXT, 13, false, "Telefone"),
                field("CELULAR", FieldType.TEXT, 13, false, "Celular"),
                field("INSCRICAO", FieldType.TEXT, 18, false, "Inscricao estadual"),
                field("CNPJ", FieldType.TEXT, 18, false, "CNPJ"),
                field("CPF", FieldType.TEXT, 18, false, "CPF"),
                field("IDENTIDADE", FieldType.TEXT, 18, false, "Identidade"),
                field("DATA_NASC", FieldType.DATE, 10, false, "Nascimento ou fundacao"),
                field("PAI", FieldType.TEXT, 50, false, "Pai"),
                field("MAE", FieldType.TEXT, 50, false, "Mae"),
                field("CADASTRO", FieldType.DATE, 10, false, "Data do cadastro"),
                field("LIMITE_CR1", FieldType.MONETARY, 0, false, "Limite de credito"),
                field("BLOQUEADO", FieldType.TEXT, 1, false, "S ou N"),
                field("OBS", FieldType.TEXT, 2048, false, "Observacoes"),
                field("EMAIL", FieldType.TEXT, 50, false, "Email"),
                field("CONTATO", FieldType.TEXT, 50, false, "Contato"))));

        layouts.put(MigrationModule.FORNECEDORES, new LayoutSpec(MigrationModule.FORNECEDORES, "FORNECEDORES", "CODIGO", List.of(
                field("CODIGO", FieldType.INTEGER, 10, true, "Codigo sequencial"),
                field("TIPOPESSOA", FieldType.TEXT, 1, true, "F ou J"),
                field("RAZAOSOC", FieldType.TEXT, 100, true, "Razao social"),
                field("NOME", FieldType.TEXT, 100, false, "Nome fantasia"),
                field("ENDERECO", FieldType.TEXT, 60, false, "Logradouro"),
                field("COMPLEMENT", FieldType.TEXT, 60, false, "Complemento"),
                field("NUM_END", FieldType.TEXT, 60, false, "Numero"),
                field("BAIRRO", FieldType.TEXT, 60, false, "Bairro"),
                field("CIDADE", FieldType.TEXT, 50, false, "Cidade"),
                field("CEP", FieldType.TEXT, 10, false, "CEP"),
                field("TELEFONE", FieldType.TEXT, 13, false, "Telefone"),
                field("FAX", FieldType.TEXT, 13, false, "FAX"),
                field("CNPJ", FieldType.TEXT, 18, false, "CNPJ"),
                field("TIPO_IE", FieldType.INTEGER, 1, false, "1, 2 ou 9"),
                field("INSCRICAO", FieldType.TEXT, 18, false, "Inscricao estadual"),
                field("CPF", FieldType.TEXT, 18, false, "CPF"),
                field("IDENTIDADE", FieldType.TEXT, 18, false, "Identidade"),
                field("OBS", FieldType.TEXT, 2048, false, "Observacoes"),
                field("EMAIL", FieldType.TEXT, 50, false, "Email"),
                field("CONTATO", FieldType.TEXT, 50, false, "Contato"))));

        layouts.put(MigrationModule.PRODUTOS, new LayoutSpec(MigrationModule.PRODUTOS, "PRODUTOS", "CODIGO", List.of(
                field("CODIGO", FieldType.INTEGER, 10, true, "Codigo do produto"),
                field("TIPO", FieldType.INTEGER, 2, true, "Tipo do item"),
                field("PRODUTO", FieldType.TEXT, 100, true, "Nome do produto"),
                field("UNIDADE", FieldType.TEXT, 6, true, "Sigla da unidade"),
                field("ESTOQUE", FieldType.MONETARY, 10, false, "Quantidade em estoque"),
                field("MINIMO", FieldType.MONETARY, 10, false, "Estoque minimo"),
                field("GRUPO", FieldType.INTEGER, 10, false, "Codigo do grupo"),
                field("PCO_COMPRA", FieldType.MONETARY, 10, false, "Preco de compra"),
                field("PCO_CUSTO", FieldType.MONETARY, 10, false, "Preco de custo"),
                field("MARGEM_LC", FieldType.MONETARY, 10, false, "Margem de lucro"),
                field("PCO_VENDA", FieldType.MONETARY, 10, false, "Preco de venda"),
                field("COMISSAO", FieldType.MONETARY, 10, false, "Comissao"),
                field("CODBARRA", FieldType.TEXT, 60, false, "Codigo de barras"),
                field("ORIGEM", FieldType.INTEGER, 1, false, "Origem"),
                field("CST", FieldType.TEXT, 2, false, "CST ICMS"),
                field("ICMS", FieldType.MONETARY, 4, false, "Aliquota ICMS"),
                field("NUM_FABRIC", FieldType.TEXT, 60, false, "Codigo de fabrica"),
                field("LOCALIZA", FieldType.TEXT, 50, false, "Localizacao"),
                field("APLICACAO", FieldType.TEXT, 2048, false, "Aplicacao"),
                field("COD_NCM", FieldType.INTEGER, 8, false, "NCM"),
                field("CEST", FieldType.INTEGER, 8, false, "CEST"),
                field("CST_PIS", FieldType.INTEGER, 8, false, "CST PIS"),
                field("ALIQPIS", FieldType.MONETARY, 4, false, "Aliquota PIS"),
                field("CSTCOFINS", FieldType.INTEGER, 2, false, "CST COFINS"),
                field("ALIQCOFINS", FieldType.MONETARY, 5, false, "Aliquota COFINS"),
                field("CST_IPI", FieldType.INTEGER, 2, false, "CST IPI"),
                field("IPI", FieldType.MONETARY, 4, false, "Aliquota IPI"),
                field("ALIQPISENT", FieldType.MONETARY, 4, false, "Aliquota entrada PIS"),
                field("ALICOFINSE", FieldType.MONETARY, 4, false, "Aliquota entrada COFINS"),
                field("COD_MARCA", FieldType.INTEGER, 10, false, "Codigo da marca"),
                field("PESO", FieldType.MONETARY, 10, false, "Peso bruto"),
                field("PESO_L", FieldType.MONETARY, 10, false, "Peso liquido"),
                field("APLICACAO2", FieldType.TEXT, 2048, false, "Aplicacao"),
                field("FAT_CONVER", FieldType.MONETARY, 10, false, "Fator de conversao"),
                field("FCP_ALIQ", FieldType.MONETARY, 4, false, "Aliquota FCP"),
                field("INATIVO", FieldType.TEXT, 1, false, "S ou N"))));

        layouts.put(MigrationModule.ARECEBER, new LayoutSpec(MigrationModule.ARECEBER, "ARECEBER", "CODIGO", List.of(
                field("CODIGO", FieldType.INTEGER, 10, true, "Codigo sequencial"),
                field("CLIENTES_ID", FieldType.INTEGER, 10, true, "Codigo do cliente"),
                field("DOCUMENTO", FieldType.TEXT, 10, true, "Numero do documento"),
                field("PARCELA", FieldType.TEXT, 10, false, "Parcela"),
                field("EMISSAO", FieldType.DATE, 10, true, "Data de emissao"),
                field("VENCIMENTO", FieldType.DATE, 10, true, "Data de vencimento"),
                field("VALOR", FieldType.MONETARY, 10, true, "Saldo em aberto"),
                field("OBSERVACAO", FieldType.TEXT, 100, false, "Observacoes"))));

        layouts.put(MigrationModule.APAGAR, new LayoutSpec(MigrationModule.APAGAR, "APAGAR", "CODIGO", List.of(
                field("CODIGO", FieldType.INTEGER, 10, true, "Codigo sequencial"),
                field("FORNECEDORES_ID", FieldType.INTEGER, 10, true, "Codigo do fornecedor"),
                field("DOCUMENTO", FieldType.TEXT, 10, true, "Numero do documento"),
                field("PARCELA", FieldType.TEXT, 10, false, "Parcela"),
                field("EMISSAO", FieldType.DATE, 10, true, "Data de emissao"),
                field("VENCIMENTO", FieldType.DATE, 10, true, "Data de vencimento"),
                field("VALOR", FieldType.MONETARY, 10, true, "Saldo em aberto"),
                field("OBSERVACAO", FieldType.TEXT, 100, false, "Observacoes"))));
    }

    public LayoutSpec get(MigrationModule module) {
        LayoutSpec layout = layouts.get(module);
        if (layout == null) {
            throw new IllegalArgumentException("Modulo nao suportado: " + module);
        }
        return layout;
    }

    public List<LayoutSpec> all() {
        return layouts.values().stream().toList();
    }

    public List<String> procedurePlan() {
        return List.of(
                "UTIL_ESTADOS_DISTRITOS",
                "UTIL_NCM_CEST",
                "CONFIGURAR_MIGRACAO",
                "MIGRAR_00_INICIAR",
                "MIGRAR_01_DISTRITO",
                "MIGRAR_02_CLIENTES",
                "MIGRAR_03_FORNECEDORES",
                "MIGRAR_04_UNIDADES",
                "MIGRAR_05_PRODUTOS",
                "MIGRAR_06_REF",
                "MIGRAR_07_CODFAB",
                "MIGRAR_08_ICMS",
                "MIGRAR_09_TABELA",
                "MIGRAR_10_KARDEX",
                "MIGRAR_10_KARDEX_NEGAT",
                "MIGRAR_11_SUBGRUPO",
                "MIGRAR_12_LOCALIZA",
                "MIGRAR_13_MARCAS",
                "MIGRAR_14_ARECEBER",
                "MIGRAR_15_APAGAR",
                "UTIL_RELATORIO_MIGRACAO");
    }

    private FieldSpec field(String name, FieldType type, int maxLength, boolean required, String description) {
        return new FieldSpec(name, type, maxLength, required, description);
    }
}
