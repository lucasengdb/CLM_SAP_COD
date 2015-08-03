package scripts.workflow.gap018_excecao;

import cmldummy.CLMDummyWorkflow;

// Movendo para uma dase anterior
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP018_prevPhase extends CLMDummyWorkflow {

    // Constantes do Script
    private final String LOGGER_ID = "\n[Workflow 018] ";
    // Variaveis    
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
            logInfo("Documento reprovado, voltando para uma fase anterior.");
            movePhase("Draft");
        } catch (Exception e) {
            logError(e.getMessage());
            cancelProcess(e.getMessage());
        }
    }
    // inicio();
}
