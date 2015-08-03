package scripts.workflow.gap014_aprov_soc;

import cmldummy.CLMDummyWorkflow;

// Adiciona o Grupo Advg. Jurid. Soc.
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanHomeIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorApprovalRuleType;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.ibean.ExtensionFieldIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanHomeIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanIfc;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.types.BigText;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author Tiago Rodrigues
 */
public class GAP014_1_addGrupoAdvgJurSoc extends CLMDummyWorkflow {

    // Variavéis e Constantes
    private final String LOGGER_ID = "\n[Workflow 014] ";
    private final String REPRESENTS_ADVG_JURID_SOC = "Advg. Jurídico - Societário";
    private final String EXTFIELD_RESPOSTAS_PREFIX = "resposta_";
    private final String EXTFIELD_PERGUNTAS_PREFIX = "pergunta_";
    private final int VLT_RESPOSTAS_TYPE_CODE = 1007;
    private final int MAX_EXTFIELDS_PERGUNTA = 20;
    String idContrato = "";
    String nomeDocCon = "";
    String debug = "";

    // COLETA ALGUMAS INFORMAÇÔES PARA OS LOGS GERADOS
    private void getIds() {
        idContrato = ((ContractIBeanIfc) doc.getRootParentIBean()).getDocumentId();
        nomeDocCon = doc.getDisplayName();
    }

    public ApplicationException getLocalizedApplicationException(
            IapiSessionContextIfc session, String resourceId, Object[] modifiers) {

        ApplicationException aEx = new ApplicationException(session,
                "tim.defdata", resourceId);
        if (modifiers != null && modifiers.length > 0) {
            aEx.setMessageModifiers(modifiers);
        }
        return aEx;
    }

    private void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID
                + "[" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID
                + "[" + idContrato + "] [" + nomeDocCon + "]\n" + mensagem));
    }

    private ValueListValueIBeanIfc getValueListValueByTypeCode(String displayNameId, int typeCode)
            throws ApplicationException, NoConnectionException, SQLException, DatabaseException {

        ValueListValueIBeanIfc vlv = null;
        String sql;
        String schema = session.getDbHandle().getSchemaOwner();
        ResultSet resultSet;

        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        sql = "SELECT T2.OBJECTID FROM "
                + schema + ".FCI_MAS_VALUE_LIST_TYPE T1, "
                + schema + ".FCI_MAS_VALUE_LIST_VALUE T2 "
                + "WHERE "
                + "T1.OBJECTID = T2.PARENT_OBJECT_ID AND "
                + "T1.TYPE_CODE = " + typeCode + " AND "
                + "T2.DISPLAY_NAME = '" + displayNameId + "'";
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        resultSet = session.getDbHandle().getResultSet();
        if (resultSet.next()) {
            vlv = (ValueListValueIBeanIfc) vlvHome.findWhere("OBJECTID = "
                    + resultSet.getInt("OBJECTID")).iterator().next();
        }
        session.getDbHandle().endTransaction();

        return vlv;
    }

    // CHECA SE EXISTE PELO MENOS UMA RESPSTA SIM NO QUESTIONARIO DE APROV. SOC.
    private boolean temRespostaSim() throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        ValueListValueIBeanIfc vlvSim;
        ExtensionFieldIfc extField = null;
        String strNum;

        vlvSim = getValueListValueByTypeCode("sim", VLT_RESPOSTAS_TYPE_CODE);

        // Somente no Acordo Básico possui documentos de contrato, então, será
        // considerado a classe pai deste contrato o "contracts.Contract"
        ContractIBeanIfc masterAgr = (ContractIBeanIfc) doc.getParentIBean();

        for (int i = 1; i <= MAX_EXTFIELDS_PERGUNTA; i++) {

            if (i < 10) {
                strNum = "00" + i;
            } else if (i < 100) {
                strNum = "0" + i;
            } else {
                strNum = Integer.toString(i);
            }

            // Coletando a pergunta e verificando se ela esta preenchida
            extField = masterAgr.getExtensionField(EXTFIELD_PERGUNTAS_PREFIX + strNum);
            BigText textoPergunta = (BigText) extField.get();
            // Verificando se a pergunta existe preenchida
            if (hasValue(textoPergunta.getTextPreview())) {
                // Colentando a resposta da pergunta encontrada  
                extField = masterAgr.getExtensionField(EXTFIELD_RESPOSTAS_PREFIX + strNum);
                // Verificando se há resposta da pergunta
                if (hasValue(extField.get())) {
                    // Verificando se a resposta é "Sim"
                    if (extField.get().equals(vlvSim.getObjectReference())) {
                        logInfo("Existe pelo menos uma resposta \"Sim\", a aprovação do grupo "
                                + "Advg. Juri. Soc. é obrigatória");
                        return true;
                    } else if (i == 16) {
                        strNum = "16";
                        extField = masterAgr.getExtensionField(EXTFIELD_RESPOSTAS_PREFIX + strNum);
                        if (extField.get().equals(vlvSim.getObjectReference())) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    // CHECA SE ESTA OK PARA O PROCESSO DE APROVAÇÂO
    private void checaAprovadores() throws ApplicationException, DatabaseException {

        OrderedSubordinateCollectionIfc colabs = null;
        CollaboratorIBeanIfc colab = null;
        CollaboratorApprovalRuleType any = new CollaboratorApprovalRuleType(CollaboratorApprovalRuleType.ANY);

        GroupIBeanHomeIfc grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, GroupIBeanHomeIfc.sHOME_NAME);
        GroupIBeanIfc grupo = grupoHome.findGroup("TIM.Aprovadores.Juridico_Societario");

        // Encontrando a "Role" "Aprovador"
        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);

        CollaboratorRoleIBeanIfc roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Aprovador'").iterator().next();

        try {
            if (temRespostaSim()) {

                colabs = ((ContractIBeanIfc) doc.getRootParentIBean()).getCollaborators();                               

                for (int i = 0; i < colabs.size(); i++) {

                    colab = (CollaboratorIBeanIfc) colabs.get(i);

                    // Procura pelo grupo "Juridico Societário" e se ele possui a role "Aprovador" e 
                    // o representa "Advg. Jurídico - Societário"
                    if (colab.getPrincipal().equals(grupo.getObjectReference())
                            && colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                            && colab.getRepresenting().getDisplayName().equals(REPRESENTS_ADVG_JURID_SOC)) {

                        addApprover(colab.getPrincipal(), any);
                        logInfo("Adicionado como aprovador o grupo 'Advg. Jurid. Soc.' "
                                + colab.getPrincipal().getDisplayName());
                    }
                }
                //throw getLocalizedApplicationException(
                //        session, "mensagem.gap014.erro.grupo.juridico.societario.nao.adicionado", null);
            }

        } catch (Exception e) {
            throw new ApplicationException("Erro durante a execução do workflow: " + e.getMessage());
        }
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

    public void inicio() throws
            ApplicationException, DatabaseException, NoConnectionException, SQLException {

        try {
            getIds();
            String currentPhase = doc.getCurrentPhase().getDisplayName(session);
            if (hasValue(currentPhase)) {
                logInfo("Etapa 1/1 - Aprovação Grupo Advg. Jurid. Soc.");
                checaAprovadores();
            } else {
                logError("Não foi possivel obter a fase atual.");
            }
        } catch (Exception e) {
            logError(e.getMessage());
            cancelProcess(e.getMessage() + " Class: " + e.getClass().getSimpleName());
            // Move para a fase anterior se houver erros 
            movePhase("Validação Solicitante");
        }
    }
    //inicio();
}
