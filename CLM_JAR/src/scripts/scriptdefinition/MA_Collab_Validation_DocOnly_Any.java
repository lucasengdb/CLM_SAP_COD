package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;
import com.sap.eso.api.contracts.ContractIBeanIfc;

/*
 MA Collab Validation - DocOnly - Any 
 - Remove os colaboradores duplicados, e considera duplicados os que tenham a mesma "role"
 - Quando for colaborador adicionado pelo "Workflow Process Owner"  ser� eleiminado todos que 
 estiverem sem representa��o, se for qualquer usu�rio que esteja adicionando um colaborador 
 sem representa��o somente  o alertar� que precisa atribuir uma representa��o
 - Valida quando adicionado algum aprovador com uma representa��o que n�o corresponde a suas 
 permiss�es configuradas em seu perfil
 */
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
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
public class MA_Collab_Validation_DocOnly_Any extends CLMDummyScriptDefinition {

    ContractIBeanIfc doc;
 // Variaveis e constantes
 	private final String LOGGER_ID = "\n[COLLAB VALIDATED] ";
 	private final String REPRESENTS_ADVG_JURIDICO = "Advg. Jur�dico - Contracts and Legal Aff";
 	private final String REPRESENTS_MGR_FINAN_CSA = "Mgr. Finan. CSA";
 	private final String REPRESENTS_DIRETOR_AREA = "Diretor da �rea";
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

 				if (tipoDoc.equalsIgnoreCase("Acordo B�sico Geral")) {
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

 				if (tipoDoc.equalsIgnoreCase("Procura��o")) {
 					if (representa.getDisplayName().equals(
 							REPRESENTS_DIRETOR_AREA)) {
 						if (isUserHasRole(user, ROLE_DIRETOR_AREA)) {
 							users.add(user.getFirstName());
 						}
 					}
 				}

 				//INICIO - Valida��o do Acordo Comercial
 				if (tipoDoc.equalsIgnoreCase(ACORDO_COMERCIAL)) {
 					
 					this.logInfo("PROCESSO DE VALIDA��O NO ACORDO COMERCIAL");
 					
 					if (representa.getDisplayName().equals(
 							REPRESENTS_ADVG_JURIDICO)) {
 						 						
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
 				//FIM - Valida��o do Acordo Comercial
 			}

 		}

 		// Se encontrado algo na List 'users', ent�o foi encontrado
 		// colaboradores com suas
 		// fun��es que n�o corresponde com seu perfil
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
 			// A fun��o Aprovador n�o se aplica para o(s) usu�rio(s) {0} da
 			// lista de colaboradores
 		}
 	}

 	public void inicio() throws ChainedException {

 		this.logInfo("INICIANDO PROCESSO DE VALIDA��O DE PERFIL");
 		this.logDebug("UM TESTEE DEBUG");
 		if (!session.getAccount().getUserName().equalsIgnoreCase("enterprise")
 				&& !session.getAccount().getUserName().equals("WORKFLOWUSER")) {
 			String tipoDoc = doc.getDocTypeReference().getDisplayName();
 			try {
 				removeColabDuplicados();
 				if (tipoDoc.equalsIgnoreCase("Acordo B�sico Geral")
 						|| tipoDoc.equalsIgnoreCase("Procura��o")
 						|| tipoDoc.equalsIgnoreCase("Acordo Comercial")) {
 					gap032_validaAprovadores(tipoDoc);
 				}
 				validaColabsSemRepresenta();
 			} catch (Exception e) {
 				this.logError(e);
 				throw new ApplicationException(e.getMessage());
 			}
 		}
 		
 		this.logInfo("FINALIZANDO PROCESSO DE VALIDA��O DE PERFIL");
 	}

 	private void logError(Exception e) {

 		Logger.error(Logger.createLogMessage(session).setException(e));

 	}
 	
 	private void logDebug(String mensagem) {
		Logger.debug(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));

	}


    //inicio();
}
