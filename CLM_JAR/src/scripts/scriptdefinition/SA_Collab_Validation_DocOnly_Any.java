package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;
import com.sap.eso.api.contracts.AgreementIBeanIfc;

/*
 SA Collab Validation - DocOnly - Any 
 - Remove os colaboradores duplicados, e considera duplicados os que tenham a mesma "role"
 - Quando for colaborador adicionado pelo "Workflow Process Owner"  ser� eleiminado todos que 
 estiverem sem representa��o, se for qualquer usu�rio que esteja adicionando um colaborador 
 sem representa��o somente  o alertar� que precisa atribuir uma representa��o
 - Valida quando adicionado algum aprovador com uma representa��o que n�o corresponde a suas 
 permiss�es configuradas em seu perfil
 */
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiAccountIfc;
import com.sap.odp.api.common.platform.IapiAccountLocator;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorSourceEnumType;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.common.db.NoConnectionException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Tiago Rodrigues
 */
public class SA_Collab_Validation_DocOnly_Any extends CLMDummyScriptDefinition {

    AgreementIBeanIfc doc;
    //
    //
    // Quando portar para o CLM copiar a partir daqui somente os met�dos e os imports
    //
    // Variaveis e constantes
    private final String LOGGER_ID = "\n[MA Collab Validation] ";
    private final String REPRESENTS_ADVG_JURIDICO = "Advg. Jur�dico - Contracts and Legal Aff";
    private final String REPRESENTS_MGR_FINAN_CSA = "Mgr. Finan. CSA";
    private final String ROLE_ADVG_JURIDICO = "tim.clm.pr.jur_advogado";
    private final String ROLE_GERENTE_FINANC = "tim.clm.pr.fin_gerente";
    
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
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void removeColabDuplicados() throws ApplicationException, DatabaseException {

        // Considera colaboradores duplicados quando o mesmo adicionado mais de uma
        // vez com a mesma role
        OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();
        int j;
        for (int i = 0; i < colabs.size(); i++) {
            CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
            for (j = i + 1; j < colabs.size(); j++) {
                CollaboratorIBeanIfc colab2 = (CollaboratorIBeanIfc) colabs.get(j);
                if (colab.getPrincipal().equals(colab2.getPrincipal())) {
                    if (colab.getCollaboratorRole().equals(colab2.getCollaboratorRole())) {
                        colabs.delete(colabs.get(j));
                        j = j - 1;
                    }
                }
            }
        }
    }

    private void removeRevisorSemRepresenta() throws ApplicationException, DatabaseException {

        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
        CollaboratorRoleIBeanIfc roleRevisorIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Revisor'").iterator().next();
        OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();
        int i;
        for (i = 0; i < colabs.size(); i++) {
            CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
            if (colab.getCollaboratorRole().equals(roleRevisorIfc.getObjectReference())) {
                if (colab.getRepresenting().getDisplayName().contains("DEFINIR")) {
                    colabs.delete(colabs.get(i));
                    i = i - 1;
                }
            }
        }
    }

    private void validaColabsSemRepresenta() throws ApplicationException, DatabaseException {

        OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();
        for (int i = 0; i < colabs.size(); i++) {
            CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
            if (colab.getRepresenting().getDisplayName().contains("DEFINIR")) {
                if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.user) {
                    IapiAccountIfc user = IapiAccountLocator.lookup(session, colab.getPrincipal());
                    if (!user.getUserName().equalsIgnoreCase("WORKFLOWUSER")) {
                        if (!(colab.getSource().get() == CollaboratorSourceEnumType.WORKFLOW)) {
                            throw getLocalizedApplicationException(
                                    session, "mensagem.gap007_e_008.erro_representa_a_definir", null);
                        }
                    }
                } else {
                    throw getLocalizedApplicationException(
                            session, "mensagem.gap007_e_008.erro_representa_a_definir", null);
                }
            }
        }
    }

    private Boolean isUserHasRole(IapiAccountIfc user, String securityProfile) throws NoConnectionException, SQLException {

        Boolean ret = false;
        String schema = session.getDbHandle().getSchemaOwner();
        String sql = "SELECT T3.INTERNAL_NAME FROM "
                + schema + ".FCI_UPP_USER_ACCOUNT T1, "
                + schema + ".FCI_UPP_ROLE_REF T2, "
                + schema + ".FCI_UPP_ROLE T3 "
                + "WHERE "
                + "T1.OBJECTID = T2.PARENT_OBJECT_ID AND "
                + "T2.ROLE_OBJECT_ID = T3.OBJECTID AND "
                + "T1.NAME = '" + user.getUserName() + "' AND "
                + "T3.INTERNAL_NAME = '" + securityProfile + "'";
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        if (!session.getDbHandle().getResultSet().next()) {
            ret = true;
        }
        session.getDbHandle().endTransaction();

        return ret;
    }

    private void gap032_validaAprovadores() throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        String nomes = "";
        IapiAccountIfc user = null;
        List users = new ArrayList();
        ValueListValueIBeanHomeIfc representanteHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);
        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
        CollaboratorRoleIBeanIfc roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Aprovador'").iterator().next();

        OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();

        for (int i = 0; i < colabs.size(); i++) {

            CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
            // Recuperando "Represents" do colaborador             
            ValueListValueIBeanIfc representa = (ValueListValueIBeanIfc) representanteHome
                    .find((ObjectReferenceIfc) colab.getRepresenting());

            if (colab.getCollaboratorRole().equals(roleAprovadorIfc.getObjectReference())
                    && colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {

                user = IapiAccountLocator.lookup(session, colab.getPrincipal());

                if (representa.getDisplayName().equals(REPRESENTS_ADVG_JURIDICO)) {
                    if (isUserHasRole(user, ROLE_ADVG_JURIDICO)) {
                        users.add(user.getFirstName());
                    }
                }
                if (representa.getDisplayName().equals(REPRESENTS_MGR_FINAN_CSA)) {
                    if (isUserHasRole(user, ROLE_GERENTE_FINANC)) {
                        users.add(user.getFirstName());
                    }
                }
            }
        }
        // Se encontrado algo na List 'users', ent�o foi encontrado colaboradores com suas 
        // fun��es que n�o corresponde com seu perfil
        if (!users.isEmpty()) {
            for (int i = 0; i < users.size(); i++) {
                if (i == 0) {
                    nomes = users.get(i).toString();
                } else {
                    nomes += ", " + users.get(i).toString();
                }
            }
            throw getLocalizedApplicationException(
                    session, "mensagem.gap032.erro_funcao_colaborador_invalida", new Object[]{nomes});
            // A fun��o Aprovador n�o se aplica para o(s) usu�rio(s) {0} da lista de colaboradores
        }
    }
    
    public void inicio() throws ApplicationException {
        try {
            if (doc.isObjectAccessModeView()) {
                doc.getIBeanHomeIfc().upgradeToEdit(doc);
            }
            removeColabDuplicados();
            gap032_validaAprovadores();
            //removeRevisorSemRepresenta();
            if (session.getAccount().getUserName().equals("WORKFLOWUSER")) {
                removeRevisorSemRepresenta();
            } else {
                validaColabsSemRepresenta();
            }
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage());
        }
    }
    
    public static void main(String arc [] ){
    	SA_Collab_Validation_DocOnly_Any c = new SA_Collab_Validation_DocOnly_Any();
    	try {
			c.inicio();
		} catch (ApplicationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
    //inicio();
}
