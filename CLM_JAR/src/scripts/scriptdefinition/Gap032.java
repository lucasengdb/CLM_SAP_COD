package scripts.scriptdefinition;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import cmldummy.CLMDummyScriptDefinition;

import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
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

public class Gap032 extends CLMDummyScriptDefinition {

	ContractIBeanIfc doc;

	// Variaveis e constantes
	private final String LOGGER_ID = "\n[COLLAB VALIDATED] ";
	private final String REPRESENTS_ADVG_JURIDICO = "Advg. Jurídico - Contracts and Legal Aff";
	private final String REPRESENTS_MGR_FINAN_CSA = "Mgr. Finan. CSA";
	private final String REPRESENTS_DIRETOR_AREA = "Diretor da Área";
	private final String ROLE_ADVG_JURIDICO = "tim.clm.pr.jur_advogado";
	private final String ROLE_GERENTE_FINANC = "tim.clm.pr.fin_gerente";
	private final String ROLE_DIRETOR_AREA = "tim.clm.pr.diretor";
	private final String ACORDO_COMERCIAL = "Acordo Comercial";

	private void logInfo(String mensagem) {
		Logger.info(Logger.createLogMessage(session).setLogMessage(
				LOGGER_ID + mensagem));
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

	private void removeColabDuplicados() throws ChainedException {

		// Considera colaboradores duplicados quando o mesmo adicionado mais de
		// uma
		// vez com a mesma role
		OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();
		int j;
		for (int i = 0; i < colabs.size(); i++) {
			CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
			for (j = i + 1; j < colabs.size(); j++) {
				CollaboratorIBeanIfc colab2 = (CollaboratorIBeanIfc) colabs
						.get(j);
				if (colab.getPrincipal().equals(colab2.getPrincipal())) {
					if (colab.getCollaboratorRole().equals(
							colab2.getCollaboratorRole())) {
						colabs.delete(colabs.get(j));
						j = j - 1;
					}
				}
			}
		}
	}

	private void validaColabsSemRepresenta() throws ChainedException {

		OrderedSubordinateCollectionIfc colabs = doc.getCollaborators();
		for (int i = 0; i < colabs.size(); i++) {
			CollaboratorIBeanIfc colab = (CollaboratorIBeanIfc) colabs.get(i);
			if (colab.getRepresenting().getDisplayName().contains("DEFINIR")) {
				if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.user) {
					IapiAccountIfc user = IapiAccountLocator.lookup(session,
							colab.getPrincipal());
					if (!user.getUserName().equalsIgnoreCase("WORKFLOWUSER")) {
						if (!(colab.getSource().get() == CollaboratorSourceEnumType.WORKFLOW)) {
							throw getAppException(
									"mensagem.gap007_e_008.erro_representa_a_definir",
									null);
						}
					}
				} else {
					throw getAppException(
							"mensagem.gap007_e_008.erro_representa_a_definir",
							null);
				}
			}
		}
	}

	private Boolean isUserHasRole(IapiAccountIfc user, String securityProfile)
			throws NoConnectionException, SQLException {
		this.logInfo("INICIO - isUserHasRole");

		Boolean ret = false;
		String schema = session.getDbHandle().getSchemaOwner();
		this.logInfo("schema = " + schema + "\n " + user.getUserName() + "\n "
				+ securityProfile);

		String sql = "SELECT T3.INTERNAL_NAME FROM " + schema
				+ ".FCI_UPP_USER_ACCOUNT T1, " + schema
				+ ".FCI_UPP_ROLE_REF T2, " + schema + ".FCI_UPP_ROLE T3 "
				+ "WHERE " + "T1.OBJECTID = T2.PARENT_OBJECT_ID AND "
				+ "T2.ROLE_OBJECT_ID = T3.OBJECTID AND " + "T1.NAME = '"
				+ user.getUserName() + "' AND " + "T3.INTERNAL_NAME = '"
				+ securityProfile + "'";

		this.logInfo(sql);
		session.getDbHandle().beginTransaction();
		session.getDbHandle().executeQuery(sql);
		if (!session.getDbHandle().getResultSet().next()) {
			ret = true;
		}
		session.getDbHandle().endTransaction();

		return ret;
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

				if (tipoDoc.equalsIgnoreCase("Acordo Básico Geral")) {
					if (representa.getDisplayName().equals(
							REPRESENTS_ADVG_JURIDICO)) {
						if (isUserHasRole(user, ROLE_ADVG_JURIDICO)) {
							users.add(user.getFirstName());
						}
					}
					if (representa.getDisplayName().equals(
							REPRESENTS_MGR_FINAN_CSA)) {
						if (isUserHasRole(user, ROLE_GERENTE_FINANC)) {
							users.add(user.getFirstName());
						}
					}
				}

				if (tipoDoc.equalsIgnoreCase("Procuração")) {
					if (representa.getDisplayName().equals(
							REPRESENTS_DIRETOR_AREA)) {
						if (isUserHasRole(user, ROLE_DIRETOR_AREA)) {
							users.add(user.getFirstName());
						}
					}
				}

				//INICIO - Validação do Acordo Comercial
				if (tipoDoc.equalsIgnoreCase(ACORDO_COMERCIAL)) {
					
					this.logInfo("PROCESSO DE VALIDAÇÃO NO ACORDO COMERCIAL");
					
					if (representa.getDisplayName().equals(
							REPRESENTS_ADVG_JURIDICO)) {
						
						this.logInfo("Representa = "+REPRESENTS_ADVG_JURIDICO);
						
						if (isUserHasRole(user, ROLE_ADVG_JURIDICO)) {
							users.add(user.getFirstName());
						}
					}

					if (representa.getDisplayName().equals(
							REPRESENTS_MGR_FINAN_CSA)) {
						
						this.logInfo("Representa = "+REPRESENTS_MGR_FINAN_CSA);
						
						if (isUserHasRole(user, ROLE_GERENTE_FINANC)) {
							users.add(user.getFirstName());
						}
					}
				}
				//FIM - Validação do Acordo Comercial
			}

		}

		// Se encontrado algo na List 'users', então foi encontrado
		// colaboradores com suas
		// funções que não corresponde com seu perfil
		if (!users.isEmpty()) {
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

	public void inicio() throws ChainedException {

		this.logInfo("INICIANDO PROCESSO DE VALIDAÇÃO DE PERFIL");

		if (!session.getAccount().getUserName().equalsIgnoreCase("enterprise")
				&& !session.getAccount().getUserName().equals("WORKFLOWUSER")) {
			String tipoDoc = doc.getDocTypeReference().getDisplayName();
			try {
				removeColabDuplicados();
				if (tipoDoc.equalsIgnoreCase("Acordo Básico Geral")
						|| tipoDoc.equalsIgnoreCase("Procuração")
						|| tipoDoc.equalsIgnoreCase("Acordo Comercial")) {
					gap032_validaAprovadores(tipoDoc);
				}
				validaColabsSemRepresenta();
			} catch (Exception e) {
				this.logError(e);
				throw new ApplicationException(e.getMessage());
			}
		}
		
		this.logInfo("FINALIZANDO PROCESSO DE VALIDAÇÃO DE PERFIL");
	}

	private void logError(Exception e) {

		Logger.error(Logger.createLogMessage(session).setException(e));

	}

}
