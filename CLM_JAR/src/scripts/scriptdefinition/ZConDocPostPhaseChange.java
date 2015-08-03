package scripts.scriptdefinition;
/*
CON DOC POST PHASE CHANGE
Atende:
- GAP 017 - Envio de Alertas 
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
import com.sap.odp.common.db.ObjectReference;
import com.sap.odp.common.db.SimpleObjectReference;
import com.sap.odp.common.platform.SessionContextIfc;
import com.sap.odp.doccommon.util.UrlBuilder;

import cmldummy.CLMDummyDocumentPrePostPhaseChange;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import javax.mail.MessagingException;
public class ZConDocPostPhaseChange extends CLMDummyDocumentPrePostPhaseChange{
	
	// CONSTANTES    
    private final String LOGGER_ID = "\n[POST PHASE CHANGE] ";
   // private final String LOGGER_ID_DEBUG = "\n[DEBUG ENGDB] ";
    // Coleções
    private final String EXTC_ALERTAS = "tabela_alertas";
    // Campos Z (9999701) userdefined.md1
    private final String EXTF_REPRESENTA = "alertar";
    private final String EXTF_FASE = "contract_doc_phase";
    private final String EXTF_NOTIFICAR = "notificar";
    private final String ANALISE_CONTRAPARTE_SEM_ACENTO="Analise Contraparte";
    private final String ANALISE_CONTRAPARTE_COM_ACENTO="Análise Contraparte";
   
    String docConPhaseConfigExtId = "";
    IapiAccountIfc user = null;
    String representa = "";
    String udoFase = "";
    String udoRepresenta = "";
    String ponto = "";
    ResultSet rs;
    String sql = "";
    GroupIBeanHomeIfc grupoHome;
    ContractDocumentTypeIBeanHomeIfc docTypeHome;

    List listUserGAP017 = new ArrayList();

    public void logDebug(String mensagem) {
        Logger.debug(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    public void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    public void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    // Envia e-mail de alerta para usuários encontrados na UDMD1 
    private void gap017_enviarEmail(HashSet listUser)
            throws ChainedException, MalformedURLException, NoConnectionException, SQLException {

        ponto = "gap017_enviarEmail()";

        if (listUser.isEmpty()) {
            return;
        }

        // Tokens que irão no e-mail e suas descrições
        String token1;                          // ID do Contrato Pai 
        String token2;                          // Usuário que esta recebendo a mensagem
        String token3 = doc.getDisplayName() + "; na etapa " + current_phase;   // Nome do Documento de contrato e sua fase
        String token4;                          // URL do Documento de Contrato  
        String token5;                          // URL do Acordo Básico
        // Sender do e-mail
        IapiAccountIfc sender = session.getAccount();
        ponto = "gap017_enviarEmail() 001";
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
        ponto = "gap017_enviarEmail() 002";
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
        token5 = urlBuilder.getPublicProtocol() + "://" + address + ":" + port
                + "/sourcing/fsbuyer/contracts/contracts_summary,"
                + doc.getParentIBean().getObjectReference().getObjectId() + ":"
                + doc.getParentIBean().getObjectReference().getClassId()
                + "?rqaction=load&hook=contract_uirq&allow_redirect=true";

        Properties params = new Properties();
        params.put("TOKEN1", token1);           // ID do Contrato Pai
        params.put("TOKEN3", token3);           // Nome do Documento de contrato
        params.put("TOKEN4", token4);           // URL do documento de contrato
        params.put("TOKEN5", token5);           // URL do Acordo Básico
        ponto = "gap017_enviarEmail() 003";
        for (Object objRef : listUser) {
            user = IapiAccountLocator.lookup(session, (ObjectReference) objRef);
            token2 = user.getFirstName() + " " + user.getLastName();
            params.put("TOKEN2", token2);       // Usuário que esta recebendo a mensagem
            MailTypeEnumType mailTypeEnum = new MailTypeEnumType(MailTypeEnumType.ODP_CUSTOM_TEMPLATE1);
            // Enviando o e-mail 
            NotificationUtil.sendNotification(new String[]{user.getEmail()}, sender, mailTypeEnum, params, null, null);
            logDebug("[" + doc.getDisplayName() + "] [" + current_phase + "] Enviado e-mail de alerta para "
                    + user.getFullName());
        }
    }

    // Busca aos colaboradores conforme a fase do documento
    private void buscaColaboradoresUDMD1() throws
            ChainedException, NoConnectionException, SQLException, MessagingException,
            UnsupportedEncodingException, MalformedURLException {

        ponto = "buscaColaboradoresUDMD1()";
        String faseAtual = current_phase;

        CollaboratorIBeanIfc colab = null;
        GroupIBeanIfc grupoColab = null;
        OrderedSubordinateCollectionIfc colabs = null;

        // Coletando qual é a configuração de fase utilizada no documento de contrato em questão
        ContractDocumentTypeIBeanIfc docType = (ContractDocumentTypeIBeanIfc) docTypeHome.find(doc.getDocTypeReference());
        ObjectReferenceIfc objRef = (ObjectReferenceIfc) docType.getFieldMetadata("CONFIG_PHASE_DEFN").get(docType);
        session.getDbHandle().beginTransaction();
        this.logDebug("SELECT EXTERNAL_ID FROM "
                + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_CONTRACT_PHASE_CONFIG "
                + "WHERE OBJECTID = " + objRef.getObjectId());
        session.getDbHandle().executeQuery("SELECT EXTERNAL_ID FROM "
                + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_CONTRACT_PHASE_CONFIG "
                + "WHERE OBJECTID = " + objRef.getObjectId());
        rs = session.getDbHandle().getResultSet();
        if (rs.next()) {
            docConPhaseConfigExtId = rs.getString("EXTERNAL_ID");
        } else {
            logError("Não encontrado na UDMD1 a configuração para: " + docConPhaseConfigExtId);
            session.getDbHandle().endTransaction();
            return;
        }
        session.getDbHandle().endTransaction();

        // Localizando a tabela de alertas conforme o External Id da configuração de fase em questão
        UserDefinedMasterData1IBeanHomeIfc udoHome = (UserDefinedMasterData1IBeanHomeIfc) IBeanHomeLocator.lookup(
                session, UserDefinedMasterData1IBeanHomeIfc.sHOME_NAME);
        UserDefinedMasterData1IBeanIfc udo1 = udoHome.findByExternalId(docConPhaseConfigExtId);

        if (hasValue(udo1)) {

            ExtensionCollectionIfc collecUdo1 = udo1.getExtensionCollection(EXTC_ALERTAS);
            ponto = "buscaColaboradoresUDMD1() 001";

        
            // Loop para recuperar todos que deverão receber o e-mail de notificação
            for (int i = 0; i < collecUdo1.size(); i++) {

                IBeanIfc linha = collecUdo1.get(i);

                // Dados recuperados da linha corrente em UDO1
                udoFase = ((ObjectReference) linha.getExtensionField(EXTF_FASE).get()).getDisplayName();
               
                
                udoRepresenta = ((ObjectReference) linha.getExtensionField(EXTF_REPRESENTA).get()).getDisplayName();
                
                //this.logDebug("Representa = "+udoRepresenta+" Notificação = "+linha.getExtensionField(EXTF_NOTIFICAR).get());
                this.logDebug("ANTES");
                if ((Boolean) linha.getExtensionField(EXTF_NOTIFICAR).get()) {
                	
                //	this.logDebug("Entrou AQ" + faseAtual.equals(udoFase) +"\n\n FASE ATUAL = "+faseAtual+"\n\n UDOFASE = "+udoFase);
                	//Gambiarra = INICIO
                	
                	if (udoFase.equals(ANALISE_CONTRAPARTE_SEM_ACENTO)) {
                		
                		udoFase = ANALISE_CONTRAPARTE_COM_ACENTO;
						
					}
                	//Gambiarra = FIM
                	if (faseAtual.equals(udoFase)) {
                		
                		this.logDebug("ENTROU NO IF  VERIFICANDO SE A FASE ATUAL É IGUAL A UDOFASE");

                        ponto = "buscaColaboradoresUDMD1() 002";

                        // Coletando os colaboradores no Master Agr ou Sub Agr pai deste documento de contrato
                        if (doc.getParentIBean() instanceof ContractIBeanIfc) {
                        	this.logDebug("É uma instancia de ContractIBeanIfc");
                            colabs = ((ContractIBeanIfc) doc.getParentIBean()).getCollaborators();
                        } else if (doc.getParentIBean() instanceof AgreementIBeanIfc) {
                        	this.logDebug("É uma instancia de AgreementIBeanIfc");
                            colabs = ((AgreementIBeanIfc) doc.getParentIBean()).getCollaborators();
                        }

                        this.logDebug("Quantidade de Colaboradores = "+colabs.size());
                        
                        
                        for (int j = 0; j < colabs.size(); j++) {
                        	 this.logDebug("Entrou no segungo FOR");
                            colab = (CollaboratorIBeanIfc) colabs.get(j);
                            
                            representa = colab.getRepresenting().getDisplayName();
                            this.logDebug("Nome colaborador "+colab.getDisplayName()+" Representa = "+representa+", Número da Posição "+j);
                            this.logDebug("Representa do Colaborador = "+representa+", Representa udo "+udoRepresenta);
                            if (representa.equals(udoRepresenta)) {
                            	this.logDebug("O "+representa+", é igual a udoRepresenta "+udoRepresenta);
                                if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.user) {
                                	this.logDebug("Tipo do Colaborador é user");
                                	this.logDebug("Colaborador Principal = "+colab.getPrincipal());
                                    user = IapiAccountLocator.lookup(session, colab.getPrincipal());
                                    listUserGAP017.add(user.getAccountObjectReference());
                                } else if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.group) {
                                	this.logDebug("Tipo do Colaborador é Group");
                                	this.logDebug("Colaborador Principal = "+colab.getPrincipal());
                                	grupoColab = (GroupIBeanIfc) grupoHome.find(colab.getPrincipal());
                                    Iterator membrosGrupo = grupoColab.findGroupMembers().iterator();
                                    while (membrosGrupo.hasNext()) {
                                        user = IapiAccountLocator.lookup(session, (ObjectReference) membrosGrupo.next());
                                        listUserGAP017.add(user.getAccountObjectReference());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // GAP017
        gap017_enviarEmail(new HashSet(listUserGAP017));
    }
   
    public void inicio() throws ChainedException {
        try {
            ponto = "inicio()";

            grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator
                    .lookup(session, GroupIBeanHomeIfc.sHOME_NAME);
            docTypeHome = (ContractDocumentTypeIBeanHomeIfc) IBeanHomeLocator.lookup(
                    session, ContractDocumentTypeIBeanHomeIfc.sHOME_NAME);

            buscaColaboradoresUDMD1();

        } catch (Exception e) {
            //logError(ponto + " *** " + e.getMessage());
            throw new ApplicationException(e.getMessage());
        }
    }


}
