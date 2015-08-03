package scripts.workflow.gap023_grupos;

import cmldummy.CLMDummyWorkflow;

// Adiciona grupos para aprovação
import com.sap.eso.api.contracts.AgreementIBeanIfc;
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.doc.collaboration.CollaboratorApprovalRuleType;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.doc.configphase.NextPhaseConfigSubIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.platform.SessionContextIfc;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP023_1_addGrupos extends CLMDummyWorkflow {

    // Constantes do Script
    private final String LOGGER_ID = "\n[Workflow 023] ";
    private final String PREFIX_AA_JURIDICO = "AA. Jurídico";
    private final String PREFIX_AA_FINAC = "AA. Finan.";
    private final String FASE_AA_JURIDICO = "Análise Áreas de Apoio Jurídico";
    private final String FASE_AA_CSA = "Análise Áreas de Apoio CSA";
    // Variaveis
    private ArrayList grupos = new ArrayList();
    ResultSet rs;
    String idContrato = "";
    String nomeDocCon = "";

    private void getIds() {
        // Colentando da classe pai do documento de contrato
        IBeanIfc parent = doc.getParentIBean();
        // Coletando os colaboradores no Master Agr ou Sub Agr
        if (parent instanceof ContractIBeanIfc) {
            idContrato = ((ContractIBeanIfc) parent).getDocumentId();
        } else if (parent instanceof AgreementIBeanIfc) {
            idContrato = ((AgreementIBeanIfc) parent).getDocumentId();
        }
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
    private void addGrupos(String prefix) throws ApplicationException, DatabaseException {

        OrderedSubordinateCollectionIfc colabs = null;
        CollaboratorIBeanIfc colab = null;
        CollaboratorApprovalRuleType ruleType = new CollaboratorApprovalRuleType(CollaboratorApprovalRuleType.ANY);
        String representa = "";

        // Encontrando a "Role" "Aprovador"
        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
        CollaboratorRoleIBeanIfc roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Aprovador'").iterator().next();

        // Colentando da classe pai do documento de contrato
        IBeanIfc parent = doc.getParentIBean();
        // Coletando os colaboradores no Master Agr ou Sub Agr
        if (parent instanceof ContractIBeanIfc) {
            colabs = ((ContractIBeanIfc) parent).getCollaborators();
        } else if (parent instanceof AgreementIBeanIfc) {
            colabs = ((AgreementIBeanIfc) parent).getCollaborators();
        } else {
            throw new ApplicationException("Colaboradores não encontrados.");
        }

        for (int i = 0; i < colabs.size(); i++) {

            colab = (CollaboratorIBeanIfc) colabs.get(i);
            // Recuperando "Represents" do colaborador             
            representa = colab.getRepresenting().getExternalId((SessionContextIfc) session);

            // Checando se tem a "Role" de aprovador
            if (colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.group) {
                // Checa se este aprovador é o que representa "Mgr. Finan. CSA"
                if (representa.startsWith(prefix)) {
                    grupos.add(i);
                }
            }
        }

        if (grupos.isEmpty()) {
            throw new ApplicationException("É necessário ao menos um Grupo de área de Apoio com a "
                    + "função Aprovador. Favor verificar");
        } else {
            // Adicionando os grupos encontrados para aprovação
            for (int i = 0; i < grupos.size(); i++) {
                colab = (CollaboratorIBeanIfc) colabs.get((Integer) grupos.get(i));
                logInfo("Adicionado como aprovador o grupo \"" + colab.getPrincipal().getDisplayName() + "\".");
                addApprover(colab.getPrincipal(), ruleType);
            }
        }
    }

    // MOVE PARA UMA POSSIVEL FASE ANTERIOR
    private void movePrevPhase(int index) throws
            ApplicationException, DatabaseException, SQLException, NoConnectionException {

        ContractDocPhaseSubIBeanHomeIfc docPhaseHome = (ContractDocPhaseSubIBeanHomeIfc) IBeanHomeLocator.lookup(
                session, ContractDocPhaseSubIBeanHomeIfc.sHOME_NAME);
        ContractDocPhaseSubIBeanIfc docPhase = (ContractDocPhaseSubIBeanIfc) docPhaseHome.find(doc.getCurrentPhase());

        if (docPhase.getPrevPhaseList().size() == 0) {
            cancelProcess("Não é possivel voltar à uma fase anterior por não ter nenhuma configurada para a fase atual.");
            return;
        }

        if (index >= docPhase.getPrevPhaseList().size()) {
            index = docPhase.getPrevPhaseList().size() - 1;
        }

        NextPhaseConfigSubIBeanIfc nextPhase = (NextPhaseConfigSubIBeanIfc) docPhase.getPrevPhaseList().get(index);

        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery("SELECT PHASE_OBJECT_NAME FROM "
                + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_NEXT_PHASE_SUB "
                + "WHERE OBJECTID = " + nextPhase.getObjectReference().getObjectId());
        rs = session.getDbHandle().getResultSet();
        rs.next();
        String fase = rs.getString(1);
        session.getDbHandle().endTransaction();

        ContractDocumentIBeanHomeIfc conDocHome = (ContractDocumentIBeanHomeIfc) doc.getIBeanHomeIfc();
        if (doc.isObjectAccessModeView()) {
            conDocHome.upgradeToEdit(doc);
        }
        conDocHome.changePhase(doc, fase);
        conDocHome.downgradeToView(doc);
        logInfo("Movido para a fase \"" + fase + "\"");
    }

    // INICIO
    public void inicio() throws
            ApplicationException, DatabaseException, SQLException, NoConnectionException {

        try {
            getIds();
            logInfo("Iniciando Workflow...");

            if (doc.getCurrentPhase().getDisplayName(session).equals(FASE_AA_JURIDICO)) {
                logInfo("Etapa 1/1 Aprovação dos Grupos AA. Jurídico ");
                addGrupos(PREFIX_AA_JURIDICO);
            }
            if (doc.getCurrentPhase().getDisplayName(session).equals(FASE_AA_CSA)) {
                logInfo("Etapa 1/1 Aprovação dos Grupos AA. Finan. CSA ");
                addGrupos(PREFIX_AA_FINAC);
            }

        } catch (Exception e) {
            logError(e.getMessage());
            // Move para a fase anterior se houver erros
            movePrevPhase(0);
            cancelProcess(e.getMessage());
        }
    }
    // inicio():
}
