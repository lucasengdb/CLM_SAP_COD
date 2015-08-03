package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;
import com.sap.eso.api.contracts.ContractIBeanIfc;

/* Master Agr - PrePublish - Any - Any
 Atende:
 GAP 003 - Campos Z do Contrato;
 GAP 018 - Publicação de Acordo Basico antes da Vigência do Doc
 */
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.ibean.AssociativeCollectionIfc;
import com.sap.odp.api.ibean.ExtensionFieldIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.common.types.SysDate;
import java.math.BigDecimal;
import java.util.Calendar;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanIfc;
import com.sap.eso.doccommon.doc.contract.configphase.ContractDocSystemPhaseEnum;
import java.util.ArrayList;

/**
 *
 * @author Tiago Rodrigues
 *
 */
public class MA_PrePublish_Any_Any extends CLMDummyScriptDefinition {

    ContractIBeanIfc doc;
    // Para portar este código em "Script Definition" copiar todo codigo a partir daqui
    // e remover o comentario da chamado do metodo "inicio"
    //
    //
    // Extension Collections
    private final String EXTC_DELTA_PRICE_VALUE = "delta_price_value";
    // Extension Fields
    private final String EXTF_OPEX = "opex_percent";
    private final String EXTF_CAPEX = "capex_percent";
    private final String EXTF_DIVISAO = "division_value";
    private final String EXTF_DELTA_PRICE = "delta_price";

    public ApplicationException getLocalizedAppException(String resourceId, Object[] modifiers) {        
        ApplicationException aEx = new ApplicationException(session, "tim.defdata", resourceId);
        if (modifiers != null && modifiers.length > 0) {
            aEx.setMessageModifiers(modifiers);
        }
        return aEx;
    }

    // Somente em "Acordo Básico"
    // CLM.003
    private void gap003_validaOPEXeCAPEX() throws ChainedException {

        BigDecimal opex = null;
        BigDecimal capex = null;
        BigDecimal zero = new BigDecimal(0);

        if (hasValue(doc.getExtensionField(EXTF_OPEX).get())) {
            opex = (BigDecimal) doc.getExtensionField(EXTF_OPEX).get();
            if (opex.compareTo(zero) == -1) {
                throw getLocalizedAppException("mensagem.gap003.erro.capex_opex_maior_igual_0", null);
            }
        } else {
            opex = new BigDecimal(0);
        }

        if (hasValue(doc.getExtensionField(EXTF_CAPEX).get())) {
            capex = (BigDecimal) doc.getExtensionField(EXTF_CAPEX).get();
            if (capex.compareTo(zero) == -1) {
                throw getLocalizedAppException("mensagem.gap003.erro.capex_opex_maior_igual_0", null);
            }
        } else {
            capex = new BigDecimal(0);
        }

        String result = capex.add(opex).toString();

        if (!result.equals("100.00")) {
            throw getLocalizedAppException("mensagem.gap003.erro.capex_opex_soma_igual_100", null);
        }
    }

    // CLM.003
    private void gap003_validaTipoNegociacao() throws ApplicationException, DatabaseException {

        if (!hasValue(doc.getExtensionField("negotiation_type").get())) {
            throw getLocalizedAppException("mensagem.gap003.erro.tipo_negociacao_preenchimento", null);
        }
    }

    // CLM.003
    private void gap003_validaCatCompra() throws ApplicationException, DatabaseException {

        if (!hasValue(doc.getExtensionField("purchasing_ctg").get())) {
            throw getLocalizedAppException("mensagem.gap003.erro.categoria_compra_preenchimento", null);
        }
    }

    // CLM.003
    private void gap003_validaDeltaPriceValue() throws ApplicationException, DatabaseException {

        Calendar dataVigencia = Calendar.getInstance();
        Calendar dataVencimento = Calendar.getInstance();

        dataVigencia.setTime(doc.getEffectiveDate().getDate());
        dataVencimento.setTime(doc.getExpirationDate().getDate());

        int anoVigencia = dataVigencia.get(Calendar.YEAR);
        int anoVencimento = dataVencimento.get(Calendar.YEAR);

        ExtensionFieldIfc divPrimeiroAno = doc.getExtensionCollection(EXTC_DELTA_PRICE_VALUE)
                .get(0).getExtensionField(EXTF_DIVISAO);
        BigDecimal valorPrimeiroAno = (BigDecimal) divPrimeiroAno.get();
        ExtensionFieldIfc divSegundoAno = doc.getExtensionCollection(EXTC_DELTA_PRICE_VALUE)
                .get(1).getExtensionField(EXTF_DIVISAO);
        BigDecimal valorSegundoAno = (BigDecimal) divSegundoAno.get();
        ExtensionFieldIfc divProximosAnos = doc.getExtensionCollection(EXTC_DELTA_PRICE_VALUE)
                .get(2).getExtensionField(EXTF_DIVISAO);
        BigDecimal valorProximosAnos = (BigDecimal) divProximosAnos.get();
        if (!hasValue(doc.getLimitValue().getPrice())) {
            throw getLocalizedAppException("mensagem.gap003.erro.deltaPrice_valorMaximo", null);
        }
        BigDecimal agrMaximum = doc.getLimitValue().getPrice();

        // Se igual a 0, torna-se obrigatório o preenchimento da linha "Divisão 1° ano"
        if ((anoVencimento - anoVigencia) == 0) {
            // Verifica se a coluna "Divisão" na primeira linha esta nulo
            if (hasValue(divPrimeiroAno.get())) {
                // Se diferente do valor do campo "Agreement Maximum"
                if (valorPrimeiroAno.compareTo(agrMaximum) != 0) {
                    throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_1_ano_somatoria", null);
                }
            } else {
                throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_1_ano_preenchimento", null);
            }
            divSegundoAno.set(null);
            divProximosAnos.set(null);
            doc.getExtensionCollection(EXTC_DELTA_PRICE_VALUE).get(1).getExtensionField(EXTF_DELTA_PRICE).set(null);
            doc.getExtensionCollection(EXTC_DELTA_PRICE_VALUE).get(2).getExtensionField(EXTF_DELTA_PRICE).set(null);
        }
        // Se igual a 1, torna-se obrigatório o preenchimento das linhas "Divisão 1° ano"
        // e "Divisão 2° Ano"
        if ((anoVencimento - anoVigencia) == 1) {
            // Verifica se a coluna "Divisão" na primeira e segunda linha estão nulos
            if (hasValue(divPrimeiroAno.get()) && hasValue(divSegundoAno.get())) {
                // Se cada valor é maior que 0
                if (valorPrimeiroAno.signum() == 1 && valorSegundoAno.signum() == 1) {
                    BigDecimal soma = valorPrimeiroAno.add(valorSegundoAno);
                    if (soma.compareTo(agrMaximum) != 0) {
                        throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_2_ano_somatoria", null);
                    }
                } else {
                    throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_2_ano_devemSerMaiorQue0", null);
                }
            } else {
                throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_2_ano_preenchimento", null);
            }
            divProximosAnos.set(null);
            doc.getExtensionCollection(EXTC_DELTA_PRICE_VALUE).get(2).getExtensionField(EXTF_DELTA_PRICE).set(null);
        }
        // Se igual ou maior a 2, torna-se obrigatório o preenchimento das linhas "Divisão 1° ano"
        // e "Divisão 2° Ano" e "Divisão nos Próximos Anos"
        if ((anoVencimento - anoVigencia) >= 2) {
            // Verifica se a coluna "Divisão" na primeira, segunda e terceira linha estão nulos
            if (hasValue(divPrimeiroAno.get()) && hasValue(divSegundoAno.get()) && hasValue(divProximosAnos.get())) {
                // Se cada valor é maior que 0
                if (valorPrimeiroAno.signum() == 1 && valorSegundoAno.signum() == 1 && valorProximosAnos.signum() == 1) {
                    BigDecimal soma = valorPrimeiroAno.add(valorSegundoAno).add(valorProximosAnos);
                    if (soma.compareTo(agrMaximum) != 0) {
                        throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_3_ano_somatoria", null);
                    }
                } else {
                    throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_3_ano_devemSerMaiorQue0", null);
                }
            } else {
                throw getLocalizedAppException("mensagem.gap003.erro.valida_divisao_3_ano_preenchimento", null);
            }
        }
    }

    // CLM.018
    private void gap018_vigenciaDocContrato() throws ApplicationException, DatabaseException {

        ContractDocPhaseSubIBeanHomeIfc docPhaseHome = (ContractDocPhaseSubIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ContractDocPhaseSubIBeanHomeIfc.sHOME_NAME);

        // Pegando todos os documentos de contrato
        AssociativeCollectionIfc docContratos = doc.getContractDocuments();
        Integer docPhaseId;
        SysDate dataInicioDocCon;
        SysDate dataFimDocCon;
        SysDate dataInicioAcordo;
        SysDate dataFimAcordo;
        Boolean podePublicar = false;
        String resourceId = "";
        ArrayList docContratosExecutados = new ArrayList();
        // Datas de inicio e vencimento do acordo basico
        dataInicioAcordo = (SysDate) doc.getEffectiveDate();
        dataFimAcordo = (SysDate) doc.getExpirationDate();

        // Coleta todos os documentos de contrato em Executado
        for (int i = 0; i < docContratos.size(); i++) {
            ContractDocumentIBeanIfc docContrato = (ContractDocumentIBeanIfc) docContratos.get(i);
            ContractDocPhaseSubIBeanIfc docPhase = (ContractDocPhaseSubIBeanIfc) docPhaseHome
                    .find(docContrato.getCurrentPhase());
            if (hasValue(docPhase.getFieldMetadata("SYSTEM_PHASE_ID").get(docPhase))) {
                docPhaseId = (Integer) docPhase.getFieldMetadata("SYSTEM_PHASE_ID").get(docPhase);
                if (docPhaseId == ContractDocSystemPhaseEnum.EXECUTED) {
                    docContratosExecutados.add(docContrato);
                }
            }
        }

        if (docContratosExecutados.size() > 0) {
            for (int i = 0; i < docContratosExecutados.size(); i++) {

                ContractDocumentIBeanIfc docContrato = (ContractDocumentIBeanIfc) docContratosExecutados.get(i);

                // Coletando as datas de inicio e vencimento do documento de contrato 
                dataInicioDocCon = (SysDate) docContrato.getFieldMetadata("EFFECTIVE_DATE").get(docContrato);
                dataFimDocCon = (SysDate) docContrato.getFieldMetadata("EXPIRATION_DATE").get(docContrato);

                if (dataInicioAcordo.compareTo(dataInicioDocCon) > 0
                        || dataFimAcordo.compareTo(dataFimDocCon) < 0) {
                    resourceId = "mensagem.gap018.documento.foradavigencia";
                } else {
                    podePublicar = true;
                }
            }
        } else {
            // Quando não houver nenhum documento em "Executed"
            resourceId = "mensagem.gap018.documento.naoexecutado";
        }

        if (!podePublicar) {
            throw getLocalizedAppException(resourceId, null);
        }
    }

    public void inicio() throws ApplicationException {
        try {
            // Se for MA ou SA do tipo "Acordo Básico Geral"
            if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Básico Geral")) {
                /* Valida se existe pelo menos um documento de contrato executado e que sua data de 
                 inicio e fim estão dentro da vigencia do acordo basico */
                gap018_vigenciaDocContrato();
                // Valida alguns campos que devem estar preenchidos corretamente para a publicação
                gap003_validaOPEXeCAPEX();
                gap003_validaTipoNegociacao();
                gap003_validaCatCompra();
                gap003_validaDeltaPriceValue();
            }
        } catch (Exception e) {
            throw new ApplicationException("[" + doc.getDocumentId() + "] " + e.getMessage());
        }
    }
    //inicio();
}
