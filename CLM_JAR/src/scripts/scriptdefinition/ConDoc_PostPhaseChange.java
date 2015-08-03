package scripts.scriptdefinition;

import cmldummy.CLMDummyWorkflow;

/* 
 CON DOC POST PHASE CHANGE
 GAP 017 - Envio de Alertas
 */
import com.sap.eso.api.contracts.AgreementIBeanIfc;
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentTypeIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentTypeIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiAccountIfc;
import com.sap.odp.api.common.platform.IapiAccountLocator;
import com.sap.odp.api.common.types.LocalizedObjectReferenceIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.comp.messaging.MailTypeEnumType;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData1IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData1IBeanIfc;
import com.sap.odp.api.ibean.ExtensionCollectionIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanHomeIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanIfc;
import com.sap.odp.api.util.NotificationUtil;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.db.SimpleObjectReference;
import com.sap.odp.common.platform.SessionContextIfc;
import com.sap.odp.doccommon.util.UrlBuilder;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.mail.MessagingException;

/**
 *
 * @author Tiago Rodrigues
 *
 * Atende ao(s) GAP(s):
 *
 * CLM.017 - Envio de Alertas
 *
 */
public class ConDoc_PostPhaseChange extends CLMDummyWorkflow {

    // CONSTANTES    
    private final String LOGGER_ID = "\n[POST PHASE CHANGE] ";
    // Coleções
    private final String EXTC_ALERTAS = "tabela_alertas";
    // Campos Z
    private final String EXTF_REPRESENTA = "alertar";
    private final String EXTF_FASE = "contract_doc_phase";

    String ponto = "";
    ResultSet rs;
    String sql = "";

    private void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void enviarEmail(IapiAccountIfc user, String[] recipients)
            throws ChainedException, MalformedURLException, NoConnectionException, SQLException {

        // Variavel usada para debug no CLM
        ponto = "enviarEmail";
        String token3 = doc.getDisplayName();
        String token2 = user.getFirstName() + " " + user.getLastName();
        String token1;
        String token4;

        // Coletando da classe pai do documento de contrato
        IBeanIfc parent = doc.getParentIBean();
        // Coletando o id do Master Agr ou Sub Agr pai deste documento de contrato
        if (parent instanceof ContractIBeanIfc) {
            token1 = ((ContractIBeanIfc) parent).getDocumentId();
        } else if (parent instanceof AgreementIBeanIfc) {
            token1 = ((AgreementIBeanIfc) parent).getDocumentId();
        } else {
            throw new ApplicationException("Não encontrado a classe pai do documento de contrato.");
        }

        // Recuperando o endereço e porta configurados no CLM
        session.getDbHandle().beginTransaction();
        sql = "SELECT * FROM "
                + session.getDbHandle().getSchemaOwner() + ".FCI_SYS_CLUSTER "
                + "WHERE UPPER(EXTERNAL_ID) = UPPER('TIM Cluster')";
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        if (!rs.next()) {
            throw new ApplicationException("External Id 'TIM Cluster' em 'Cluster Configuration' não encontrado.");
        }
        String address = rs.getString("PUBLIC_ADDRESS");
        String port = rs.getString("PORT");
        session.getDbHandle().endTransaction();

        // Montando a URL do documento de contrato
        UrlBuilder urlBuilder = new UrlBuilder((SessionContextIfc) session);
        urlBuilder.setObject((SimpleObjectReference) doc.getObjectReference());
        urlBuilder.setDestUser(((SessionContextIfc) session).getUserAccount(), ((SessionContextIfc) session).getLocalHostInfo());
        token4 = urlBuilder.getPublicProtocol() + "://" + address + ":" + port
                + "/sourcing/fsbuyer/contracts/contracts_summary,"
                + doc.getParentIBean().getObjectReference().getObjectId() + ":"
                + doc.getParentIBean().getObjectReference().getClassId()
                + "?rqaction=load&hook=contract_uirq&allow_redirect=true&targetObjref="
                + doc.getObjectReference().getObjectId() + ":"
                + doc.getObjectReference().getClassId();

        // Enviando e-mails 
        Properties params = new Properties();
        params.put("TOKEN1", token1);           // ID do Contrato Pai
        params.put("TOKEN2", token2);           // Usuário que esta recebendo a mensagem
        params.put("TOKEN3", token3);           // Nome do Documento de contrato
        params.put("TOKEN4", token4);           // URL do documento de contrato

        IapiAccountIfc sender = session.getAccount();

        MailTypeEnumType mailTypeEnum = new MailTypeEnumType(MailTypeEnumType.ODP_WORKFLOW_APPROVAL_STATUS_MSG);

        NotificationUtil.sendNotification(recipients, sender, mailTypeEnum, params, null, null);
    }

    // Envia alerta aos colaboradores conforme a fase do documento GAP017
    private void gap017_checaQuemRecebeAlerta() throws
            ChainedException, NoConnectionException, SQLException, MessagingException,
            UnsupportedEncodingException, MalformedURLException {

        CollaboratorIBeanIfc colab = null;
        GroupIBeanIfc grupoColab = null;
        OrderedSubordinateCollectionIfc colabs = null;
        String udoFase = null;
        String udoRepresenta = null;
        String docConPhaseConfigExtId = "";
        IapiAccountIfc user = null;
        String representa = "";
        ponto = "checaQuemRecebeAlerta 001";

        if (hasValue(current_phase)) {

            // Coletando qual é a configuração de fase utilizada no documento de contrato em questão
            ContractDocumentTypeIBeanHomeIfc docTypeHome = (ContractDocumentTypeIBeanHomeIfc) IBeanHomeLocator.lookup(
                    session, ContractDocumentTypeIBeanHomeIfc.sHOME_NAME);
            ContractDocumentTypeIBeanIfc docType = (ContractDocumentTypeIBeanIfc) docTypeHome.find(doc.getDocTypeReference());
            ObjectReferenceIfc objRef = (ObjectReferenceIfc) docType.getFieldMetadata("CONFIG_PHASE_DEFN").get(docType);
            session.getDbHandle().beginTransaction();
            session.getDbHandle().executeQuery("SELECT EXTERNAL_ID FROM "
                    + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_CONTRACT_PHASE_CONFIG "
                    + "WHERE OBJECTID = " + objRef.getObjectId());
            rs = session.getDbHandle().getResultSet();
            ponto = "checaQuemRecebeAlerta 002";
            if (rs.next()) {
                docConPhaseConfigExtId = rs.getString("EXTERNAL_ID");
            }
            session.getDbHandle().endTransaction();
            ponto = "checaQuemRecebeAlerta 003 - " + docConPhaseConfigExtId;

            // Localizando a tabela de alertas conforme o External Id da configuração de fase em questão
            UserDefinedMasterData1IBeanHomeIfc udoHome = (UserDefinedMasterData1IBeanHomeIfc) IBeanHomeLocator.lookup(
                    session, UserDefinedMasterData1IBeanHomeIfc.sHOME_NAME);
            UserDefinedMasterData1IBeanIfc udo1 = udoHome.findByExternalId(docConPhaseConfigExtId);

            if (hasValue(udo1)) {

                ExtensionCollectionIfc collecUdo = udo1.getExtensionCollection(EXTC_ALERTAS);
                ponto = "checaQuemRecebeAlerta 004";

                for (int i = 0; i < collecUdo.size(); i++) {

                    List colaboradores = new ArrayList();                    

                    // Dados recuperados da linha corrente em UDO1
                    udoFase = ((LocalizedObjectReferenceIfc) collecUdo.get(i).getExtensionField(EXTF_FASE).get())
                            .getExternalId((SessionContextIfc) session);
                    udoRepresenta = ((LocalizedObjectReferenceIfc) collecUdo.get(i).getExtensionField(EXTF_REPRESENTA).get())
                            .getExternalId((SessionContextIfc) session);

                    ponto = "checaQuemRecebeAlerta 005";
                    // Se a fase atual for igual a encontrada na UDO1 em questão
                    if (current_phase.equals(udoFase)) {

                        // Coletando da classe pai do documento de contrato
                        IBeanIfc parent = doc.getParentIBean();

                        // Coletando os colaboradores no Master Agr ou Sub Agr pai deste documento de contrato
                        if (parent instanceof ContractIBeanIfc) {
                            colabs = ((ContractIBeanIfc) parent).getCollaborators();
                        } else if (parent instanceof AgreementIBeanIfc) {
                            colabs = ((AgreementIBeanIfc) parent).getCollaborators();
                        } else {
                            throw new ApplicationException("Colaboradores não encontrados.");
                        }
                        ponto = "checaQuemRecebeAlerta 006";
                        for (int j = 0; j < colabs.size(); j++) {

                            colaboradores.clear();
                            colab = (CollaboratorIBeanIfc) colabs.get(j);

                            if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.user) {

                                representa = colab.getRepresenting().getExternalId((SessionContextIfc) session);
                                
                                ponto = "checaQuemRecebeAlerta 007";
                                
                                if (representa.equals(udoRepresenta)) {
                                    // Envia email para o colaborador encontrado
                                    user = IapiAccountLocator.lookup(session, colab.getPrincipal());
                                    logInfo("Enviando e-mail de alerta para " + user.getFullName());
                                    enviarEmail(user, new String[]{user.getEmail()});
                                }

                            } else if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.group) {

                                representa = colab.getRepresenting().getExternalId((SessionContextIfc) session);
                                
                                ponto = "checaQuemRecebeAlerta 008";

                                if (representa.equals(udoRepresenta)) {

                                    GroupIBeanHomeIfc grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator
                                            .lookup(session, GroupIBeanHomeIfc.sHOME_NAME);

                                    grupoColab = (GroupIBeanIfc) grupoHome.find(colab.getPrincipal());
                                    Iterator membrosGrupo = grupoColab.findGroupMembers().iterator();

                                    while (membrosGrupo.hasNext()) {
                                        user = IapiAccountLocator
                                                .lookup(session, (ObjectReferenceIfc) membrosGrupo.next());
                                        logInfo("Enviando e-mail de alerta para " + user.getFullName());
                                        enviarEmail(user, new String[]{user.getEmail()});
                                    }
                                }
                            }
                        }
                    }
                }

            } else {
                logInfo("Não encontrado na UDO a configuração para: " + docConPhaseConfigExtId);
            }
        }
    }

    public void inicio() throws ChainedException {
        try {
            ponto = "inicio 001";
            gap017_checaQuemRecebeAlerta();
        } catch (Exception e) {
            logError(ponto + " *** " + e.getMessage());
            throw new ApplicationException(e.getMessage());
        }
    }
    //
    //inicio();
}
