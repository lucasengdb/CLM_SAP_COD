package scripts.workflow.gap008_variacoes;

import cmldummy.CLMDummyWorkflow;

// Valida e adiciona os aprovadores necessarios
import com.sap.eso.api.contracts.AgreementIBeanIfc;
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorApprovalRuleType;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.doc.configphase.NextPhaseConfigSubIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.common.db.NoConnectionException;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP008_1_addAprovadores extends CLMDummyWorkflow {

    // Constantes do Script
    private final String LOGGER_ID = "\n[Workflow 008 - Variações]";
    private final String REPRESENTS_ADVG_JURIDICO = "Advg. Jurídico - Contracts and Legal Aff";
    private final String REPRESENTS_MGR_FINAN_CSA = "Mgr. Finan. CSA";
    private final String REPRESENTS_ANALISTA_FINAN_CSA = "Anlst. Finan. CSA";
    private final String REPRESENTS_SUPRIMENTOS = "Suprimentos";
    private final String REPRESENTS_AREA_REQUISITANTE = "Área Requisitante";
    private final String REPRESENTS_AREA_SOLICITANTE = "Área Solicitante";
    // Variaveis
    private List colabAprovAdvgJuridico = new ArrayList(); // Armazena os indices dos colaboradores encontrados    
    private List colabAprovAreaRequis = new ArrayList();
    private List colabChangeHistory = new ArrayList();
    private List colabMgrFinanCSA = new ArrayList();
    private List colabAnlstFinanCSA = new ArrayList();
    private List colabProprietario = new ArrayList();
    private Boolean obrigatorioAprovAreaRequis = false;
    private Boolean obrigatorioColabMgrFinanCSA = false;
    String sql;
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
                + " [" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID
                + " [" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    private void checaAprovadores()
            throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        String msgErro = "A lista de colaboradores está incompleta "
                + "ou os papéis não foram atribuídos corretamente. Favor verificar.";

        OrderedSubordinateCollectionIfc colabs = null;
        CollaboratorIBeanIfc colab = null;
        CollaboratorApprovalRuleType all = new CollaboratorApprovalRuleType(CollaboratorApprovalRuleType.ALL);

        ValueListValueIBeanHomeIfc representanteHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);

        // Encontrando a "Role" "Aprovador"
        CollaboratorRoleIBeanIfc roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Aprovador'").iterator().next();
        // Encontrando a "Role" "Proprietário"
        CollaboratorRoleIBeanIfc roleProprietarioIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Proprietário'").iterator().next();

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

        // Checando se existe colaborador com a "Role" de aprovador e o "Represents" 
        // "Advg. Jurídico - Contracts and Legal Affairs" que é obrigatório
        for (int i = 0; i < colabs.size(); i++) {

            colab = (CollaboratorIBeanIfc) colabs.get(i);
            // Recuperando "Represents" do colaborador             
            ValueListValueIBeanIfc representa = (ValueListValueIBeanIfc) representanteHome
                    .find((ObjectReferenceIfc) colab.getRepresenting());

            // Checando se tem a "Role" de aprovador
            if (colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {
                // Coletando os colaboradores com a representação "Analista Finan CSA"
                if (representa.getDisplayName().equals(REPRESENTS_ANALISTA_FINAN_CSA)) {
                    colabAnlstFinanCSA.add(i);
                }
            }

            // Checando se tem a "Role" de aprovador
            if (colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {
                // Checa se este aprovador é o que representa "Advg. Jurídico - Contracts and Legal Aff"
                if (representa.getDisplayName().equals(REPRESENTS_ADVG_JURIDICO)) {
                    colabAprovAdvgJuridico.add(i);
                }
                // Checa se este aprovador é o que representa "Área Requisitante"
                if (representa.getDisplayName().equals(REPRESENTS_AREA_REQUISITANTE)) {
                    colabAprovAreaRequis.add(i);
                }
                // Coletando os colaboradores com a representação "Mgr. Finan CSA"
                if (representa.getDisplayName().equals(REPRESENTS_MGR_FINAN_CSA)) {
                    colabMgrFinanCSA.add(i);
                }
            }
            // Checando o colaborador com a role "Proprietário"
            if (colab.getCollaboratorRole().equals(roleProprietarioIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {
                if (representa.getDisplayName().equals(REPRESENTS_SUPRIMENTOS)) {
                    // Setando variavel como verdadeiro para a obrigatoriedade da aprovação da Área Requisitante
                    obrigatorioAprovAreaRequis = true;
                    colabProprietario.add(i);
                } else // Checando se o proprietário representa "Área Solicitante"
                if (representa.getDisplayName().equals(REPRESENTS_AREA_SOLICITANTE)) {
                    colabProprietario.add(i);
                }
            }
        }

        // Se foi encontrado colaboradores com a representação "Anlst Finan CSA", então, checamos 
        // se este usuário aparece no CHANGE HISTORY, aparecendo, torna-se obrigatório o aprovador
        // "Mgr Finan CSA" na lista de colaboradores
        session.getDbHandle().beginTransaction();
        sql = "SELECT USER_NAME_OBJECT_ID FROM "
                + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_CHG_HISTORY "
                + "WHERE PARENT_OBJECT_ID = " + doc.getObjectReference().getObjectId() + " "
                + "GROUP BY USER_NAME_OBJECT_ID";
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        while (rs.next()) {
            colabChangeHistory.add(rs.getInt("USER_NAME_OBJECT_ID"));
        }
        session.getDbHandle().endTransaction();
        for (int i = 0; i < colabAnlstFinanCSA.size(); i++) {
            colab = (CollaboratorIBeanIfc) colabs.get((Integer) colabAnlstFinanCSA.get(i));
            for (int j = 0; j < colabChangeHistory.size(); j++) {
                if (colabChangeHistory.get(j).toString().equals(colab.getPrincipal().getObjectId().toString())) {
                    obrigatorioColabMgrFinanCSA = true;
                }
            }
        }
        // Checando se foi encontrado todos colaboradores obrigatórios para o andamento do Workflow
        if (colabAprovAdvgJuridico.isEmpty()
                || (obrigatorioColabMgrFinanCSA && colabMgrFinanCSA.isEmpty())
                || colabProprietario.isEmpty()
                || (obrigatorioAprovAreaRequis && colabAprovAreaRequis.isEmpty())) {
            throw new ApplicationException(msgErro);
        }

        // Se tudo certo, adiciona os aprovadores
        // Adicionando os Advogados Juridicos
        for (int i = 0; i < colabAprovAdvgJuridico.size(); i++) {
            colab = (CollaboratorIBeanIfc) colabs.get((Integer) colabAprovAdvgJuridico.get(i));
            logInfo("Adicionado como aprovador \"Advg. Jurídico\" " + colab.getPrincipal().getDisplayName());
            addApprover(colab.getPrincipal(), all);
        }
        // Adicionando os Gerentes Financeiro CSA
        if (obrigatorioColabMgrFinanCSA) {
            for (int i = 0; i < colabMgrFinanCSA.size(); i++) {
                colab = (CollaboratorIBeanIfc) colabs.get((Integer) colabMgrFinanCSA.get(i));
                logInfo("Adicionado como aprovador \"Mgr. Finan. CSA\" " + colab.getPrincipal().getDisplayName());
                addApprover(colab.getPrincipal(), all);
            }
        }
        // Adicionando colaboradores da Área Requisitante
        if (obrigatorioAprovAreaRequis) {
            for (int i = 0; i < colabAprovAreaRequis.size(); i++) {
                colab = (CollaboratorIBeanIfc) colabs.get((Integer) colabAprovAreaRequis.get(i));
                logInfo("Adicionado como aprovador \"Área Requisitante\" " + colab.getPrincipal().getDisplayName());
                addApprover(colab.getPrincipal(), all);
            }
        }
        // Adicionando o Proprietário do contrato 
        for (int i = 0; i < colabProprietario.size(); i++) {
            colab = (CollaboratorIBeanIfc) colabs.get((Integer) colabProprietario.get(i));
            logInfo("Adicionado como aprovador o proprietário do contrato " + colab.getPrincipal().getDisplayName());
            addApprover(colab.getPrincipal(), all);
        }
    }

    // MOVE PARA UMA POSSIVEL FASE ANTERIOR
    private void movePrevPhase(int index)
            throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

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

        ContractDocumentIBeanHomeIfc contractHome = (ContractDocumentIBeanHomeIfc) doc.getIBeanHomeIfc();
        contractHome.upgradeToEdit(doc);
        // Avança para uma das fases anteriores possiveis partindo da fase atual
        contractHome.changePhase(doc, fase);
        contractHome.downgradeToView(doc);
        logInfo("Movido para a fase \"" + fase + "\"");
    }

    public void inicio()
            throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        try {
            getIds();
            logInfo("Etapa 1/1 - Adicionando Aprovadores");
            checaAprovadores();
        } catch (Exception e) {
            logError(e.getMessage());
            cancelProcess(e.getMessage());
            // Move para a fase anterior se houver erros
            movePrevPhase(1);
        }
    }
    //inicio();
}
