package scripts.workflow.gap014_aprov_soc;

import cmldummy.CLMDummyWorkflow;

// Movendo para uma dase anterior
import com.sap.eso.api.contracts.AgreementIBeanIfc;
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.common.db.NoConnectionException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP014_prevPhase extends CLMDummyWorkflow {

    // Constantes do Script
    private final String LOGGER_ID = "\n[Workflow 014] ";
    // Variaveis
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
                + "[" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID
                + "[" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    // MOVE PARA UMA FASE QUE FOR PERMITIDA PELA CONFIGURAÇÃO DE FASES
    private void movePhase(String fase) throws ApplicationException,
            DatabaseException, SQLException, NoConnectionException {

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
            ApplicationException, DatabaseException, NoConnectionException, SQLException {

        String faseAtual = doc.getCurrentPhase().getDisplayName(session);
        String faseAnterior = null;
        String faseMudar = null;

        try {
            getIds();
            logInfo("Documento reprovado, voltando para uma fase anterior.");
            /*	
             - Quando aplicar, se Aprovação Societária REJEITADA e etapa anterior igual a "Análise Solicitante" 
             retornar para a etapa "Análise Solicitante" = 1.
             - Quando aplicar, se Aprovação Societária REJEITADA e etapa anterior igual a "Conclusão Jurídico" 
             retornar para a etapa "Validação Solicitante" = 0
             */
            String schema = session.getDbHandle().getSchemaOwner();
            sql = "SELECT T1.PHASE_REF_OBJECT_NAME FROM "
                    + schema + ".FCI_DOC_WORKFLOW_HISTORY T1 "
                    + "WHERE "
                    + "T1.PARENT_OBJECT_ID = " + doc.getObjectReference().getObjectId() + " AND "
                    + "T1.PARENT_CLASS_ID = 2002 AND "
                    + "T1.PHASE_REF_OBJECT_NAME IS NOT NULL "
                    + "ORDER BY T1.START_DATETIME DESC ";
            session.getDbHandle().beginTransaction();
            session.getDbHandle().executeQuery(sql);
            rs = session.getDbHandle().getResultSet();
            while (rs.next()) {
                faseAnterior = rs.getString("PHASE_REF_OBJECT_NAME");
                if (!faseAtual.equals(faseAnterior)) {
                    if (faseAnterior.equals("Conclusão Jurídico")) {
                        faseMudar = "Validação Solicitante";
                        break;
                    } else if (faseAnterior.equals("Análise Solicitante")) {
                        faseMudar = "Análise Solicitante";
                        break;
                    }
                }
            }
            session.getDbHandle().endTransaction();
            logInfo("Fase anterior foi a \"" + faseAnterior + "\", voltando para \"" + faseMudar + "\"");
            movePhase(faseMudar);
        } catch (Exception e) {
            logError(e.getMessage());
            cancelProcess(e.getMessage());
        }
    }
    // inicio();
}
