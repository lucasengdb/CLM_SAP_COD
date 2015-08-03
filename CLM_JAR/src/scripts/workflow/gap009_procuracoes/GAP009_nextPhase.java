package scripts.workflow.gap009_procuracoes;

import cmldummy.CLMDummyWorkflow;
// Avança para próxima fase
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.configphase.ContractDocPhaseSubIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.doc.configphase.NextPhaseConfigSubIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.common.db.NoConnectionException;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP009_nextPhase extends CLMDummyWorkflow {

    // Constantes do Script
    private final String LOGGER_ID = "\n[Workflow] ";
    // Variaveis
    ResultSet rs;

    private void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }
    
    // MOVE PARA UMA POSSIVEL PROXIMA FASE
    private void moveNextPhase(int index) throws 
            ApplicationException, DatabaseException, NoConnectionException, SQLException {

        ContractDocPhaseSubIBeanHomeIfc docPhaseHome = (ContractDocPhaseSubIBeanHomeIfc) IBeanHomeLocator.lookup(
                session, ContractDocPhaseSubIBeanHomeIfc.sHOME_NAME);
        ContractDocPhaseSubIBeanIfc docPhase = (ContractDocPhaseSubIBeanIfc) docPhaseHome.find(doc.getCurrentPhase());

        if (docPhase.getNextPhaseList().size() == 0) {
            cancelProcess("Não é possivel avançar uma fase por não ter nenhuma configurada para a fase atual.");
            return;
        }

        if (index >= docPhase.getNextPhaseList().size()) {
            index = docPhase.getNextPhaseList().size() - 1;
        }

        NextPhaseConfigSubIBeanIfc nextPhase = (NextPhaseConfigSubIBeanIfc) docPhase.getNextPhaseList().get(index);

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

    // INICIO
    private void inicio() throws 
            ApplicationException, DatabaseException, NoConnectionException, SQLException {

        try {
            logInfo("Workflow do documento \"" + doc.getDisplayName()
                    + "\" finalizado, movendo para próxima fase.");
            moveNextPhase(0);

        } catch (Exception e) {
            logError(e.getMessage());            
            cancelProcess(e.getMessage());
        }
    }
    // inicio();
}
