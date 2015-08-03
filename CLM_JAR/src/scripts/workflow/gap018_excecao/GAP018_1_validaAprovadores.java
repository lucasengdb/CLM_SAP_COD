package scripts.workflow.gap018_excecao;

import cmldummy.CLMDummyWorkflow;

// Valida aprovadores
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.types.PriceIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import java.math.BigDecimal;
import java.util.ArrayList;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP018_1_validaAprovadores extends CLMDummyWorkflow {

    // Constantes para os logs desse GAP
    private final String LOGGER_ID = "\n[Workflow 018] ";
    private final String DESCRICAO_ETAPA = "Etapa 1/5 Valindando se esta presente "
            + "todos os colaboradores necessarios...";
    // Representações dos colaboradores
    private final String REP_DIR_SUPRI = "Diretor Suprimentos";
    private final String REP_DIR_REQUIS = "Diretor Requisitante";
    private final String REP_PRESIDENTE = "Presidente";
    private final String REP_JURI_SOC = "Advg. Jurídico - Societário";
    // Variaveis
    private ArrayList aprovDirSupri = new ArrayList();
    private ArrayList aprovDirRequis = new ArrayList();
    private ArrayList aprovPresidentes = new ArrayList();
    private ArrayList aprovJuriSoc = new ArrayList();

    String idContrato = "";
    String nomeDocCon = "";

    private void getIds() {
        idContrato = ((ContractIBeanIfc) doc.getRootParentIBean()).getDocumentId();
        nomeDocCon = doc.getDisplayName();
    }

    public ApplicationException getLocalizedAppException(String resourceId, Object[] modifiers) {
        ApplicationException ex = doc.createApplicationException(null, resourceId);
        ex.setBundleName("tim.defdata");
        if (modifiers != null && modifiers.length > 0) {
            ex.setMessageModifiers(modifiers);
        }
        return ex;
    }

    private void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID
                + "[" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID
                + "[" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    // ADICIONA OS GRUPOS CONFORME PREFIX
    private void checaAprovadores() throws ApplicationException, DatabaseException {

        OrderedSubordinateCollectionIfc colabs = null;
        CollaboratorIBeanIfc colab = null;
        String representa = null;

        // Encontrando a "Role" "Aprovador"
        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
        CollaboratorRoleIBeanIfc roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Aprovador'").iterator().next();

        // Colentando a lista de colaboradores no Master Agreement        
        colabs = ((ContractIBeanIfc) doc.getRootParentIBean()).getCollaborators();

        for (int i = 0; i < colabs.size(); i++) {

            colab = (CollaboratorIBeanIfc) colabs.get(i);

            // Checando se tem a "Role" de aprovador e se o colaborador é um usuário
            if (colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {

                // Recuperando "Represents" do colaborador
                representa = colab.getRepresenting().getDisplayName();

                if (representa.equals(REP_DIR_SUPRI)) {
                    aprovDirSupri.add(colab);
                }
                if (representa.equals(REP_DIR_REQUIS)) {
                    aprovDirRequis.add(colab);
                }
                if (representa.equals(REP_PRESIDENTE)) {
                    aprovPresidentes.add(colab);
                }
            }

            // Checando se tem a "Role" de aprovador e se o colaborador é um grupo
            if (colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.group) {

                // Recuperando "Represents" do colaborador
                representa = colab.getRepresenting().getDisplayName();

                if (representa.equals(REP_JURI_SOC)) {
                    aprovJuriSoc.add(colab);
                }
            }
        }

        // Faz as devidas validações se estão todos os colaboradores obrigatórios para aprovação
        if (aprovDirSupri.isEmpty() || aprovDirRequis.isEmpty()) {
            throw new ApplicationException("Acordo Básico com valor até R$ 5.000.000,00. Alçada "
                    + "de aprovação requerida: Diretores de Suprimentos e Área Requisitante.");
        }

        // Colentanto o valor do contrato em "Valor Líquido do Contrato (sem IPI e ICMS)"
        PriceIfc valorContrato = ((ContractIBeanIfc) doc.getRootParentIBean()).getLimitValue();

        // Se for maior que 35 milhões
        if (valorContrato.getPrice().compareTo(new BigDecimal(30000000)) == 1) {
            if (aprovPresidentes.isEmpty() || aprovJuriSoc.isEmpty()) {
                throw getLocalizedAppException("mensagem.gap018.aprovadores.alcada.30000000", null);
            }
        }

        // Se for maior que 5 milhões
        if (valorContrato.getPrice().compareTo(new BigDecimal(5000000)) == 1) {
            if (aprovPresidentes.isEmpty()) {
                throw getLocalizedAppException("mensagem.gap018.aprovadores.alcada.5000000", null);
            }
        }
    }

    // MOVE PARA UMA FASE QUE FOR PERMITIDA PELA CONFIGURAÇÃO DE FASES
    private void movePhase(String fase) throws ApplicationException, DatabaseException {

        ContractDocumentIBeanHomeIfc conDocHome = (ContractDocumentIBeanHomeIfc) doc.getIBeanHomeIfc();
        if (doc.isObjectAccessModeView()) {
            conDocHome.upgradeToEdit(doc);
        }
        conDocHome.changePhase(doc, fase);
        conDocHome.downgradeToView(doc);
        logInfo("Movido para a fase \"" + fase + "\"");
    }

    // INICIO
    public void inicio() throws ApplicationException, DatabaseException {

        try {
            getIds();
            logInfo("Iniciando Workflow...");
            logInfo(DESCRICAO_ETAPA);
            checaAprovadores();
        } catch (Exception e) {
            logError(e.getMessage());
            // Move para a fase anterior se houver erros
            movePhase("Draft");
            cancelProcess(e.getMessage());
        }
    }
    // inicio();
}
