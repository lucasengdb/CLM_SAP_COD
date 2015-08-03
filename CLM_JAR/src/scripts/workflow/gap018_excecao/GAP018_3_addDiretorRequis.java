package scripts.workflow.gap018_excecao;

import cmldummy.CLMDummyWorkflow;

// Adiciona Diretor Requisitante 
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.doc.collaboration.CollaboratorApprovalRuleType;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import java.util.ArrayList;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP018_3_addDiretorRequis extends CLMDummyWorkflow {

    // Constantes para os logs desse GAP
    private final String LOGGER_ID = "\n[Workflow 018] ";
    private final String DESCRICAO_ETAPA = "Etapa 3/5 Aprovação Diretor Requisitante.";
    // Representações dos colaboradores
    private final String REP_DIR_REQUIS = "Diretor Requisitante";
    // Variaveis
    private ArrayList aprovDirRequis = new ArrayList();
    
    String idContrato = "";
    String nomeDocCon = "";

    private void getIds() {
        idContrato = ((ContractIBeanIfc) doc.getRootParentIBean()).getDocumentId();
        nomeDocCon = doc.getDisplayName();
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
    private void addDiretoresRequisitante() throws ApplicationException, DatabaseException {

        OrderedSubordinateCollectionIfc colabs = null;
        CollaboratorIBeanIfc colab = null;
        CollaboratorApprovalRuleType ruleType = new CollaboratorApprovalRuleType(CollaboratorApprovalRuleType.ANY);
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

            // Checando se tem a "Role" de aprovador
            if (colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {

                // Recuperando "Represents" do colaborador             
                representa = colab.getRepresenting().getDisplayName();

                if (representa.equals(REP_DIR_REQUIS)) {
                    aprovDirRequis.add(colab);
                }
            }
        }

        // Faz as devidas validações se estão todos os colaboradores obrigatórios para aprovação
        if (aprovDirRequis.isEmpty()) {
            throw new ApplicationException("Acordo Básico com valor até R$ 5.000.000,00. Alçada "
                    + "de aprovação requerida: Diretores de Suprimentos e Área Requisitante.");
        } else {
            for (int i = 0; i < aprovDirRequis.size(); i++) {
                colab = (CollaboratorIBeanIfc) aprovDirRequis.get(i);
                addApprover(colab.getPrincipal(), ruleType);
                logInfo("Adicionando como aprovador \"" + colab.getPrincipal().getDisplayName() + "\"");
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
            logInfo(DESCRICAO_ETAPA);

            addDiretoresRequisitante();

        } catch (Exception e) {
            logError(e.getMessage());
            // Move para a fase anterior se houver erros
            movePhase("Draft");
            cancelProcess(e.getMessage());
        }
    }
    // inicio();
}
