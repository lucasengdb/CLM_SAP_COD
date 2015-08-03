package scripts.workflow.gap014_aprov_soc;

import cmldummy.CLMDummyWorkflow;

// Avança para próxima fase
import com.sap.eso.api.contracts.AgreementIBeanIfc;
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentTypeIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentTypeIBeanIfc;
import com.sap.eso.doccommon.doc.contract.configphase.ContractDocPhaseConfigHome;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.db.SimpleObjectReference;
import com.sap.odp.common.platform.SessionContextIfc;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP014_nextPhase extends CLMDummyWorkflow {

    // Constantes do Script
    private final String LOGGER_ID = "\n[Workflow 014] ";
    // Variaveis
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

    private String getDocPhaseConfig()
            throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        String ret = null;
        
        ContractDocumentTypeIBeanHomeIfc docTypeHome = (ContractDocumentTypeIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ContractDocumentTypeIBeanHomeIfc.sHOME_NAME);

        ContractDocumentTypeIBeanIfc docType = (ContractDocumentTypeIBeanIfc) docTypeHome.find(doc.getTypeObjRef());
        ObjectReferenceIfc configPhaseDef = (ObjectReferenceIfc) docType.getFieldMetadata("CONFIG_PHASE_DEFN").get(docType);
        ContractDocPhaseConfigHome configPhaseHome = new ContractDocPhaseConfigHome((SessionContextIfc) session);
        Integer objectId = configPhaseHome.find((SimpleObjectReference) configPhaseDef).getObjectReference().getObjectId();

        String sql = "SELECT T1.EXTERNAL_ID FROM "
                + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_CONTRACT_PHASE_CONFIG T1 "
                + "WHERE "
                + "OBJECTID = " + objectId;
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        rs.next();
        ret = rs.getString("EXTERNAL_ID");
        session.getDbHandle().endTransaction();

        return ret;
    }

    // INICIO
    public void inicio()
            throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        String docType = getDocPhaseConfig();

        try {
            getIds();
            logInfo("Workflow finalizado, movendo para próxima fase.");
            if (docType.equals("TIM.tesouraria.banco")) {
                movePhase("Impressão e Assinatura Fornecedor");
            } else {
                movePhase("Impressão");
            }
        } catch (Exception e) {
            logError(e.getMessage());
            cancelProcess(e.getMessage());
        }
    }
    // inicio();
}
