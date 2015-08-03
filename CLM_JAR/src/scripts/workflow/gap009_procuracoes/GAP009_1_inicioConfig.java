package scripts.workflow.gap009_procuracoes;

import cmldummy.CLMDummyWorkflow;

// Adiciona usu�rio "Diretor de �rea" para aprova��o
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
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.common.db.NoConnectionException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tiago Rodrigues
 */
@SuppressWarnings({"unchecked", "rawtypes"})
public class GAP009_1_inicioConfig extends CLMDummyWorkflow {

    // Constantes do Script
    private final String LOGGER_ID = "\n[Workflow] ";
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
        Logger.info(Logger.createLogMessage(session).setLogMessage(
                LOGGER_ID + "[" + idContrato + "][" + nomeDocCon + "]\n"
                + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(
                LOGGER_ID + "[" + idContrato + "][" + nomeDocCon + "]\n"
                + mensagem));
    }

    private void movePrevPhase(int index) throws ApplicationException,
            DatabaseException, SQLException, NoConnectionException {

        ContractDocPhaseSubIBeanHomeIfc docPhaseHome = (ContractDocPhaseSubIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ContractDocPhaseSubIBeanHomeIfc.sHOME_NAME);
        ContractDocPhaseSubIBeanIfc docPhase = (ContractDocPhaseSubIBeanIfc) docPhaseHome
                .find(doc.getCurrentPhase());
        if (docPhase.getPrevPhaseList().size() == 0) {
            cancelProcess("N�o � possivel voltar � uma fase anterior por n�o ter nenhuma configurada para a fase atual.");
            return;
        }
        if (index >= docPhase.getPrevPhaseList().size()) {
            index = docPhase.getPrevPhaseList().size() - 1;
        }
        NextPhaseConfigSubIBeanIfc nextPhase = (NextPhaseConfigSubIBeanIfc) docPhase
                .getPrevPhaseList().get(index);
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(
                "SELECT PHASE_OBJECT_NAME FROM "
                + session.getDbHandle().getSchemaOwner()
                + ".FCI_DOC_NEXT_PHASE_SUB " + "WHERE OBJECTID = "
                + nextPhase.getObjectReference().getObjectId());
        rs = session.getDbHandle().getResultSet();
        rs.next();
        String fase = rs.getString(1);
        session.getDbHandle().endTransaction();
        ContractDocumentIBeanHomeIfc conDocHome = (ContractDocumentIBeanHomeIfc) doc
                .getIBeanHomeIfc();
        if (doc.isObjectAccessModeView()) {
            conDocHome.upgradeToEdit(doc);
        }
        conDocHome.changePhase(doc, fase);
        conDocHome.downgradeToView(doc);
        logInfo("Movido para a fase \"" + fase + "\"");
    }

    private boolean usuarioAprovador_representaDiretoria(CollaboratorIBeanIfc collaborator)
            throws ApplicationException, DatabaseException {

        ValueListValueIBeanHomeIfc representanteHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);
        ValueListValueIBeanIfc representa = (ValueListValueIBeanIfc) representanteHome
                .find(collaborator.getRepresenting());
        boolean retval = false;
        if (representa.getDisplayName().equals("Diretor da �rea")) {
            retval = true;
        }
        return retval;
    }

    private void checaExisteColaborador_aprovadorUsuario() throws ApplicationException, DatabaseException {

        CollaboratorIBeanIfc colab2 = null;
        CollaboratorIBeanIfc colab = null;
        CollaboratorRoleIBeanIfc roleAprovadorIfc;
        IBeanIfc parent;

        OrderedSubordinateCollectionIfc colabs = null;
        CollaboratorApprovalRuleType ruleType = new CollaboratorApprovalRuleType(
                CollaboratorApprovalRuleType.ANY);

        // Encontrando a "Role" "Aprovador"
        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
        roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome
                .findWhere("DISPLAY_NAME = 'Aprovador'").iterator().next();

        // Colentando da classe pai do documento de contrato
        parent = doc.getParentIBean();
        // Coletando os colaboradores no Master Agr ou Sub Agr
        if (parent instanceof ContractIBeanIfc) {
            colabs = ((ContractIBeanIfc) parent).getCollaborators();
        } else if (parent instanceof AgreementIBeanIfc) {
            colabs = ((AgreementIBeanIfc) parent).getCollaborators();
        } else {
            throw new ApplicationException("Colaboradores n�o encontrados.");
        }

        List usuariosDiretoriaAprovadores = new ArrayList();
        for (int i = 0; i < colabs.size(); i++) {
            colab = (CollaboratorIBeanIfc) colabs.get(i);
            // Recuperando "Represents" do colaborador

            // Checando se tem a "Role" de aprovador
            if (colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {
                // renbtg - atencao: exige coloaborador c role APROVADOR e que
                // seja USER (nao group!)   
                if (usuarioAprovador_representaDiretoria(colab)) {
                    usuariosDiretoriaAprovadores.add(colab);
                }
            }
        }

        if (usuariosDiretoriaAprovadores.isEmpty()) {
            throw new ApplicationException("Nenhum aprovador foi definido. Favor verificar a lista de colaboradores");
            // deve ser exibida.
        } else {
            // Adicionando os grupos encontrados para aprova��o
            for (int i = 0; i < usuariosDiretoriaAprovadores.size(); i++) {
                colab2 = (CollaboratorIBeanIfc) usuariosDiretoriaAprovadores.get(i);
                addApprover(colab2.getPrincipal(), ruleType);
                logInfo("Adicionado como aprovador o usuario \"" + colab2.getPrincipal().getDisplayName() + "\".");
            }
        }
    }

    // INICIO
    private  void inicio() throws ApplicationException, DatabaseException,
            SQLException, NoConnectionException {

        try {
            getIds();
            logInfo("Iniciando Workflow ");
            checaExisteColaborador_aprovadorUsuario();

        } catch (Exception e) {
            logError(e.getMessage());
            // Move para a fase anterior se houver erros
            movePrevPhase(0);
            cancelProcess(e.getMessage());
        }
    }
    
    public static void main(String args[]){
    	
    	GAP009_1_inicioConfig g = new GAP009_1_inicioConfig();
    	
    	try {
			g.inicio();
		} catch (ApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DatabaseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoConnectionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    
    }
    //inicio();
}
