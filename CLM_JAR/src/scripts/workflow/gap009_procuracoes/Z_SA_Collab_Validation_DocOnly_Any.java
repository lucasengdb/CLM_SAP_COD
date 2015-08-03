package scripts.workflow.gap009_procuracoes;

/*
 SA Collab Validation - DocOnly - Any 
 Atende: 
 - Remove os colaboradores duplicados que adicionados pelo "WORKFLOWUSER" sem representa
 */
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import cmldummy.CLMDummyWorkflow;

import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiAccountIfc;
import com.sap.odp.api.common.platform.IapiAccountLocator;
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

public class Z_SA_Collab_Validation_DocOnly_Any extends CLMDummyWorkflow {

	// Variaveis e constantes
	private final String ACORDO_COMERCIAL = "Acordo Comercial";
	private final String REPRESENTS_ADVG_JURIDICO = "Advg. Jurídico - Contracts and Legal Aff";
	private final String ROLE_ADVG_JURIDICO = "tim.clm.pr.jur_advogado";

	// Constante de Teste retirar depois
	private final String LOGGER_ID = "\n[Workflow] ";

	public ApplicationException getLocalizedApplicationException(
			String resourceId, Object[] modifiers) {

		ApplicationException aEx = new ApplicationException(session,
				"tim.defdata", resourceId);
		if (modifiers != null && modifiers.length > 0) {
			aEx.setMessageModifiers(modifiers);
		}
		return aEx;
	}

	public ApplicationException getAppException(String resourceId,
			Object[] modifiers) {

		ApplicationException aEx = new ApplicationException(session,
				"tim.defdata", resourceId);
		if (modifiers != null && modifiers.length > 0) {
			aEx.setMessageModifiers(modifiers);
		}
		return aEx;
	}

	private void removeRevisorSemRepresenta() throws ApplicationException,
			DatabaseException {

		CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
				.lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
		CollaboratorRoleIBeanIfc roleRevisorIfc = (CollaboratorRoleIBeanIfc) roleHome
				.findWhere("DISPLAY_NAME = 'Revisor'").iterator().next();
		OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();
		int i;
		for (i = 0; i < colabs.size(); i++) {
			CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
			if (colab.getCollaboratorRole().equals(
					roleRevisorIfc.getObjectReference())) {
				if (colab.getRepresenting().getDisplayName()
						.contains("DEFINIR")) {
					colabs.delete(colabs.get(i));
					i = i - 1;
				}
			}
		}
	}

	private void validaColabsSemRepresenta() throws ApplicationException,
			DatabaseException {

		OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();
		for (int i = 0; i < colabs.size(); i++) {
			CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
			if (colab.getRepresenting().getDisplayName().contains("DEFINIR")) {
				if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.user) {
					IapiAccountIfc user = IapiAccountLocator.lookup(session,
							colab.getPrincipal());
					if (!user.getUserName().equalsIgnoreCase("WORKFLOWUSER")) {
						if (!(colab.getSource().get() == CollaboratorSourceEnumType.WORKFLOW)) {
							throw getLocalizedApplicationException(
									"mensagem.gap007_e_008.erro_representa_a_definir",
									null);
						}
					}
				} else {
					throw getLocalizedApplicationException(
							"mensagem.gap007_e_008.erro_representa_a_definir",
							null);
				}
			}
		}
	}

	public void inicio() throws ApplicationException {
		if (!session.getAccount().getUserName().equalsIgnoreCase("enterprise")
				&& !session.getAccount().getUserName().equals("WORKFLOWUSER")) {
			this.logInfo("INICIO -  METODO INICIo");
			String tipoDoc = "";
			try {

				JOptionPane.showMessageDialog(null,"FUNCINOU");
				tipoDoc = doc.getDocTypeReference().getDisplayName();
				this.gap032_validaAprovadores(tipoDoc);
				if (doc.isObjectAccessModeView()) {
					doc.getIBeanHomeIfc().upgradeToEdit(doc);
				}
				if (session.getAccount().getUserName().equals("WORKFLOWUSER")) {
					removeRevisorSemRepresenta();

				} else {

					validaColabsSemRepresenta();
				}

			} catch (Exception e) {
				this.logError("Erro mensagem: "
						+ e.getMessage());
				throw new ApplicationException(e.getMessage());
			}
		}
	}

	private void gap032_validaAprovadores(String tipoDoc)
			throws ChainedException, NoConnectionException, SQLException {

		String nomes = "";
		IapiAccountIfc user = null;
		List users = new ArrayList();
		ValueListValueIBeanHomeIfc representanteHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
				.lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

		CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
				.lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);

		CollaboratorRoleIBeanIfc roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome
				.findWhere("DISPLAY_NAME = 'Aprovador'").iterator().next();
		OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();

		for (int i = 0; i < colabs.size(); i++) {

			CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);

			// Recuperando "Represents" do colaborador
			ValueListValueIBeanIfc representa = (ValueListValueIBeanIfc) representanteHome
					.find((ObjectReferenceIfc) colab.getRepresenting());

			// Se colaborador for "Aprovador" e do tipo "user"
			if (colab.getCollaboratorRole().equals(
					roleAprovadorIfc.getObjectReference())
					&& colab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.user) {
				user = IapiAccountLocator.lookup(session, colab.getPrincipal());

				if (representa.getDisplayName()
						.equals(REPRESENTS_ADVG_JURIDICO)) {
					if (isUserHasRole(user, ROLE_ADVG_JURIDICO)) {
						users.add(user.getFirstName());
					}
				}
			}
		}
		// Se encontrado algo na List 'users', então foi encontrado
		// colaboradores com suas
		// funções que não corresponde com seu perfil
		if (!users.isEmpty()) {
			this.logInfo("users.isEmpty() " + users.isEmpty());

			for (int i = 0; i < users.size(); i++) {
				if (i == 0) {
					nomes = users.get(i).toString();
				} else if ((i + 1) == users.size()) {
					nomes += " e " + users.get(i).toString();
				} else {
					nomes += ", " + users.get(i).toString();
				}
			}
			throw getAppException(
					"mensagem.gap032.erro_funcao_colaborador_invalida",
					new Object[] { nomes });
			// A função Aprovador não se aplica para o(s) usuário(s) {0} da
			// lista de colaboradores
		}
	}

	private Boolean isUserHasRole(IapiAccountIfc user, String securityProfile)
			throws NoConnectionException, SQLException {

		Boolean ret = false;
		String schema = session.getDbHandle().getSchemaOwner();
		String sql = "SELECT T3.INTERNAL_NAME FROM " + schema
				+ ".FCI_UPP_USER_ACCOUNT T1, " + schema
				+ ".FCI_UPP_ROLE_REF T2, " + schema + ".FCI_UPP_ROLE T3 "
				+ "WHERE " + "T1.OBJECTID = T2.PARENT_OBJECT_ID AND "
				+ "T2.ROLE_OBJECT_ID = T3.OBJECTID AND " + "T1.NAME = '"
				+ user.getUserName() + "' AND " + "T3.INTERNAL_NAME = '"
				+ securityProfile + "'";
		session.getDbHandle().beginTransaction();
		session.getDbHandle().executeQuery(sql);
		if (!session.getDbHandle().getResultSet().next()) {
			ret = true;
		}
		session.getDbHandle().endTransaction();

		//
		return ret;
	}

	private void logError(String mensagem) {
		Logger.error(Logger.createLogMessage(session).setLogMessage(
				LOGGER_ID + mensagem));
	}

	private void logDebug(String mensagem) {
		Logger.debug(Logger.createLogMessage(session).setLogMessage(
				LOGGER_ID + mensagem));

	}

	private void logInfo(String mensagem) {
		Logger.info(Logger.createLogMessage(session).setLogMessage(
				LOGGER_ID + mensagem));
	}
}
