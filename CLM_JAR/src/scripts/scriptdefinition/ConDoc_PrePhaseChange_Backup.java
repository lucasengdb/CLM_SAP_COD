package scripts.scriptdefinition;


/* CON DOC PRE PHASE CHANGE
Atende:
GAP 027 - RFE
GAP 028 - Mudança de Fase por Usuário Autorizado
GAP 033 - Cria calendarios de atividades na UDMD1
GAP 034 - Campo de comentário para justificativa de rejeição das etapas de análise
*/
/* CON DOC PRE PHASE CHANGE
Atende:
GAP 027 - RFE
GAP 028 - Mudança de Fase por Usuário Autorizado
GAP 033 - Cria calendarios de atividades na UDMD1
GAP 034 - Campo de comentário para justificativa de rejeição das etapas de análise
*/

import cmldummy.CLMDummyDocumentPrePostPhaseChange;


import com.sap.eso.api.doccommon.doc.contract.DocumentVersionIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentTypeIBeanHomeIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentTypeIBeanIfc;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiAccountIfc;
import com.sap.odp.api.common.platform.IapiAccountLocator;
import com.sap.odp.api.doc.IapiDocumentLockManager;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData1IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData1IBeanIfc;
import com.sap.odp.api.ibean.ExtensionCollectionIfc;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanHomeIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanIfc;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.db.ObjectReference;
import com.sap.odp.common.types.SysDate;
import com.sap.odp.common.types.SysDatetime;
import com.sap.odp.doccommon.userdefined.UserDefinedMasterData1Bo;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Formatter;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Tiago Rodrigues
 *
 */
public class ConDoc_PrePhaseChange_Backup extends CLMDummyDocumentPrePostPhaseChange {

	  // Constantes
    private final String LOGGER_ID = "\n[PRE PHASE CHANGE]\n";
    // Constantes UDMD1
    private final String EXTC_ALERTAS = "tabela_alertas";
    private final String EXTF_FASE = "contract_doc_phase";
    private final String EXTF_REPRESENTA = "alertar";
    private final String EXTF_PERFIL_AUTORIZADO = "perfil_autorizado";
    private final String EXTF_ATIVIDADE = "atividade";
    // Campos da UDO1 para GAP033    
    private final String EXTC_MANUTENCAO = "manutencao";
    private final String EXTF_REF_MA = "campo_01";
    private final String EXTF_REF_DOC = "campo_02";
    private final String EXTF_REF_USER = "campo_03";
    private final String EXTF_FASE_UDO1 = "campo_04";
    private final String EXTF_DATA_INICIO = "campo_05";
    private final String EXTF_DATA_FIM = "campo_06";
    // GAP033
    private final String REGISTRO_GAP033 = "gap033_calendarios";
    // GAP034
    private final String EXTF_HISTORICO_JUS = "historicojus";
    private final String EXTF_JUSTIFICATIVA = "justificativa";
    private final String EXTF_DE_FASE = "de_fase";
    private final String EXTF_PARA_FASE = "para_fase";
    private final String EXTF_DATA = "datat_retrocesso";
    private final String EXTF_USUARIO = "usuario";

    // Variaveis    
    ResultSet rs;
    String sql = "";
    List listUserGAP033 = null;
    ContractIBeanIfc maAgr = null;
    UserDefinedMasterData1IBeanIfc udo1GAP33 = null;
    ExtensionCollectionIfc udo1Collection = null;

    // Variaveis que precisam ser iniciadas de imediato
    GroupIBeanHomeIfc grupoHome = null;
    UserDefinedMasterData1IBeanHomeIfc udo1Home = null;
    ContractDocumentTypeIBeanHomeIfc docTypeHome = null;
    String phaseConfidExtId = "";

    int tentativasSalvarUDMD1NumMaximo = 10;
    int tentativasSalvarUDMD1 = 0;

    // Serve para um debug
    String ponto = "";
    //String debug = "";

    private void throwAppException(String resourceId, String localMensagem, Object[] modifiers) throws ApplicationException {
        ApplicationException aEx = doc.createApplicationException(localMensagem, resourceId);
        aEx.setBundleName("tim.defdata");
        if (modifiers != null && modifiers.length > 0) {
            aEx.setMessageModifiers(modifiers);
        }
        throw aEx;
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void logWarning(String mensagem) {
        Logger.warning(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private String formatInterval(long ms) {
        long hr = TimeUnit.MILLISECONDS.toHours(ms);
        long min = TimeUnit.MILLISECONDS.toMinutes(ms) % TimeUnit.HOURS.toMinutes(1);
        long sec = TimeUnit.MILLISECONDS.toSeconds(ms) % TimeUnit.MINUTES.toSeconds(1);
        ms = TimeUnit.MILLISECONDS.toMillis(ms) % TimeUnit.SECONDS.toMillis(1);
        Object[] args = {hr, min, sec, ms};
        return new Formatter().format("%02d:%02d:%02d.%02d", args).toString();
    }

    private String removerAcentos(String stringAcentuada) {
        if (stringAcentuada == null) {
            return null;
        }
        return Normalizer.normalize(stringAcentuada, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }

    private boolean equalsIgnoreAccents(String string1, String string2) {
        if (string1 == null) {
            return string2 == null;
        }
        return removerAcentos(string1).equals(removerAcentos(string2));
    }

    // Coleta os perfis configurado para o uusário passado no parametro
    private ArrayList getUserProfiles(IapiAccountIfc user)
            throws NoConnectionException, SQLException {

        ArrayList lista = new ArrayList();

        String schema = session.getDbHandle().getSchemaOwner();
        sql = "SELECT T3.INTERNAL_NAME FROM "
                + schema + ".FCI_UPP_USER_ACCOUNT T1, "
                + schema + ".FCI_UPP_ROLE_REF T2, "
                + schema + ".FCI_UPP_ROLE T3 "
                + "WHERE "
                + "T1.OBJECTID = T2.PARENT_OBJECT_ID AND "
                + "T2.ROLE_OBJECT_ID = T3.OBJECTID AND "
                + "T1.NAME = '" + user.getUserName() + "' ";
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        while (session.getDbHandle().getResultSet().next()) {
            lista.add(session.getDbHandle().getResultSet().getString("INTERNAL_NAME"));
        }
        session.getDbHandle().endTransaction();

        return lista;
    }

    // Coleta o external id da Configuração de Fase de um documento de contrato
    private String getPhaseConfigExtId(ContractDocumentIBeanIfc conDoc) throws Exception {

        String docConPhaseConfigExtId = "";
        ContractDocumentTypeIBeanIfc docType = null;
        ObjectReferenceIfc objRef = null;

        // Coletando qual é a configuração de fase utilizada no documento de contrato em questão
        docType = (ContractDocumentTypeIBeanIfc) docTypeHome.find(conDoc.getDocTypeReference());
        objRef = (ObjectReferenceIfc) docType.getFieldMetadata("CONFIG_PHASE_DEFN").get(docType);
        try {
            session.getDbHandle().beginTransaction();
            session.getDbHandle().executeQuery("SELECT EXTERNAL_ID FROM "
                    + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_CONTRACT_PHASE_CONFIG "
                    + "WHERE OBJECTID = " + objRef.getObjectId());
            rs = session.getDbHandle().getResultSet();
            if (rs.next()) {
                docConPhaseConfigExtId = rs.getString("EXTERNAL_ID");
            } else {
                logError("Não encontrado na UDMD1 pela configuração de fase: " + conDoc.getDocTypeReference().getDisplayName());
                session.getDbHandle().endTransaction();
            }
            session.getDbHandle().endTransaction();
        } catch (Exception e) {
            throw e;
        }

        return docConPhaseConfigExtId;
    }

    // CLM.027
    private void gap027_mudancaFasePelaVersaoDoc() throws ChainedException, DatabaseException {

    	
    	this.logInfo("EXECUTOU GP027");
        /*
         1. Se versão igual a 1: a fase “Impressão” do documento de contrato poderá ser alcançada 
         pelo usuário, considerando a fase de origem conforme configuração de fases do documento de contrato. 
         2. Se versão diferente de 1: a fase “Impressão” do documento de contrato poderá ser alcançada 
         pelo usuário somente quando o documento de contrato tiver passado pela fase “Aprovação”.
         */
        // Coletando a versão mais recente do documento de contrato em questão
        DocumentVersionIBeanIfc docVersion = (DocumentVersionIBeanIfc) doc.getDocVersions().get(0);
        this.logInfo("DOCVERSION = "+docVersion);
        Integer versao = (Integer) docVersion.getFieldMetadata("VERSION").get(docVersion);
        this.logInfo("VERSAO = "+versao);

        if (versao != 1) {
        	this.logInfo("É DIFERENTE DE 1");
        	
        	this.logInfo("Fase atual = "+current_phase+" Outra fase "+other_phase);
            if (!current_phase.equalsIgnoreCase("Conclusão Jurídico")
                    && other_phase.equalsIgnoreCase("Aprovação Societária")) {
            
            	this.logInfo("Entrou no IF diferente de conclusão Juridico e aprovação Societária");
                throwAppException("mensagem.gap027.naoPodeAvancarParaAprovacaoSocietaria", null, null);
            }
        }
    }

    // CLM.028
    private void gap028_mudancaFasePorUserAutorizado()
            throws ChainedException, SQLException, NoConnectionException {

        ponto = "gap028_mudancaFasePorUserAutorizado()";

        String docConfigurablePhaseDefinition = phaseConfidExtId;
        List listUdo1Profiles = new ArrayList();
        List listUserProfiles = new ArrayList();
        List listUsuariosEncontrados = new ArrayList();
        List listUdo1Registros = new ArrayList();
        String udo1Fase = "";
        String udo1Representa = "";
        CollaboratorIBeanIfc collab = null;
        IapiAccountIfc user = null;
        String userProfile = null;
        String udo1Profile = null;

        // Coletando os perfis configurados do usuário em Session
        listUserProfiles = getUserProfiles(session.getAccount());

        // Coletando os usuários correspondente ao usuário em session 
        ContractIBeanIfc masterAgr = (ContractIBeanIfc) doc.getRootParentIBean();
        OrderedSubordinateCollectionIfc collabs = masterAgr.getCollaborators();
        for (int i = 0; i < collabs.size(); i++) {
            collab = (CollaboratorIBeanIfc) collabs.get(i);
            if (collab.getCollaboratorType().get() == CollaboratorTypeEnumType.group) {
                GroupIBeanIfc grupo = (GroupIBeanIfc) grupoHome.find(collab.getPrincipal());
                Iterator membrosGrupo = grupo.getMemberUserNamesAsCollection().iterator();
                // Verificando se o usuário em Session pertende ao grupo encontrado em Collaborators
                while (membrosGrupo.hasNext()) {
                    String userName = (String) membrosGrupo.next();
                    if (session.getAccount().getUserName().equals(userName)) {
                        user = IapiAccountLocator.lookup(session, userName);
                        listUsuariosEncontrados.add(new Object[]{user, collab.getRepresenting().getDisplayName()});
                    }
                }
            } else if (session.getAccount().getAccountObjectReference().equals(collab.getPrincipal())) {
                user = IapiAccountLocator.lookup(session, collab.getPrincipal());
                listUsuariosEncontrados.add(new Object[]{user, collab.getRepresenting().getDisplayName()});
            }
        }
        // Coleta o registro na UDO1 conforme a fase atual do documento de contrato
        UserDefinedMasterData1IBeanIfc udo1PhaseConfig = udo1Home.findByExternalId(docConfigurablePhaseDefinition);

        if (hasValue(udo1PhaseConfig)) {

            ExtensionCollectionIfc collecUdo1TabAlertas = udo1PhaseConfig.getExtensionCollection(EXTC_ALERTAS);

            // Coletando as linhas da collection encontrada na UDO1 onde na coluna fase seja igual a fase atual
            for (int i = 0; i < collecUdo1TabAlertas.size(); i++) {
                // Coletando valores das três colunas na UDO1 ("Fase", "Alertar(Representa)", "Perfil Autorizado")
                udo1Fase = ((ObjectReferenceIfc) collecUdo1TabAlertas.get(i).getExtensionField(EXTF_FASE).get()).getDisplayName();
                udo1Representa = ((ObjectReferenceIfc) collecUdo1TabAlertas.get(i).getExtensionField(EXTF_REPRESENTA).get()).getDisplayName();
                if (hasValue(collecUdo1TabAlertas.get(i).getExtensionField(EXTF_PERFIL_AUTORIZADO).get())) {
                    listUdo1Profiles = Arrays.asList(collecUdo1TabAlertas.get(i).getExtensionField(EXTF_PERFIL_AUTORIZADO).get()
                            .toString().split(";"));
                    if (equalsIgnoreAccents(current_phase, udo1Fase)) {
                        listUdo1Registros.add(new Object[]{udo1Representa, listUdo1Profiles});
                    }
                }
            }
        } else {
            throw new ApplicationException("Não encontrado na UDMD1 a configuração de "
                    + "fase: " + docConfigurablePhaseDefinition);
        }
        for (Object udo1Registro : listUdo1Registros) {
            udo1Representa = ((Object[]) udo1Registro)[0].toString();
            listUdo1Profiles = (List) ((Object[]) udo1Registro)[1];
            for (Object itemListaUsuarios : listUsuariosEncontrados) {
                user = (IapiAccountIfc) ((Object[]) itemListaUsuarios)[0];
                if (udo1Representa.equals(((Object[]) itemListaUsuarios)[1].toString())) {
                    for (Object obj : listUdo1Profiles) {
                        udo1Profile = obj.toString();
                        for (Object obj2 : listUserProfiles) {
                            userProfile = obj2.toString();
                            if (udo1Profile.equals(userProfile)) {
                                return;
                            }
                        }
                    }
                }
            }
        }
        throwAppException("mensagem.gap028.naoPossuiAutorizacaoparamudaretapa", null, null);
    }

    // CLM.033
    private void gap033_salvaRegistroUMD1() throws ChainedException, InterruptedException {
        // Se depois de coletado a UDO1 e outro usuário modificou o mesmo dará um erro 
        // ao salvar, então será uma nova tentativa recuperando a UDO1 no seu estado atual
        try {
            udo1Home.save(udo1GAP33);
            udo1Home.downgradeToView(udo1GAP33);
            logInfo("[GAP033] Criado/Atualizado calendarios em UDMD1 com sucesso.");
        } catch (ChainedException e) {
            if (tentativasSalvarUDMD1 < tentativasSalvarUDMD1NumMaximo) {
                tentativasSalvarUDMD1++;
                logInfo("[GAP033]\nFalha ao salvar UDMD1. Recarregando documento para nova tentativa "
                        + tentativasSalvarUDMD1 + "/" + tentativasSalvarUDMD1NumMaximo + ".\n" + e.getMessage());
                Thread.sleep(500); // Aguarda um tempo antes de tentar novamente
                gap033_inicio();
            } else {
                throw new ApplicationException("Foram feitas " + tentativasSalvarUDMD1NumMaximo + " tentativas sem sucesso de "
                        + "salvar o registro na UDMD1, favor verificar se há algum usuário editando a UDMD1.");
            }
        }
    }

    private void gap033_criaRegistroInicialUDMD1() throws ChainedException {

        ponto = "gap033_criaRegistroInicialUDMD1()";

        UserDefinedMasterData1IBeanIfc udo1;
        udo1 = (UserDefinedMasterData1IBeanIfc) udo1Home.create();
        udo1.setExternalId(REGISTRO_GAP033);
        udo1.setDisplayName("GAP033 Calendários");
        udo1.setDocumentDescription("Registro de atividades para usuários referentes "
                + "a mudanças de fases dos documentos de contrato.");
        udo1.setIsInactive(Boolean.TRUE);
        IapiDocumentLockManager.lockField(session, udo1, UserDefinedMasterData1Bo.sCOL_EXTERNAL_ID);
        IapiDocumentLockManager.lockField(session, udo1, UserDefinedMasterData1Bo.sCOL_DISPLAY_NAME);
        IapiDocumentLockManager.lockField(session, udo1, UserDefinedMasterData1Bo.sCOL_DOCUMENT_DESCRIPTION);
        IapiDocumentLockManager.lockField(session, udo1, EXTC_MANUTENCAO);
        ponto = "gap033_criaRegistroInicialUDMD1() 001";
        udo1Home.save(udo1);
        udo1Home.downgradeToView(udo1GAP33);
    }

    // CLM.033 Cria um calendario automaticamente na UDMD1
    private void gap033_criaCalendario(HashSet listUser) throws ChainedException {

        ponto = "gap033_criaCalendario() 000";
        String fasePosterior = other_phase;
        ObjectReference objRef = null;
        IBeanIfc udo1LinhaNova = null;
        IapiAccountIfc user = null;

        if (listUser.isEmpty()) {
            return;
        }

        for (Object obj : listUser) {

            objRef = (ObjectReference) obj;
            user = IapiAccountLocator.lookup(session, objRef);

            udo1LinhaNova = udo1Collection.create();

            udo1LinhaNova.getExtensionField(EXTF_REF_MA).set(doc.getParentIBean().getObjectReference());
            udo1LinhaNova.getExtensionField(EXTF_REF_DOC).set(doc.getObjectReference());
            udo1LinhaNova.getExtensionField(EXTF_REF_USER).set(user.getAccountObjectReference());
            udo1LinhaNova.getExtensionField(EXTF_FASE_UDO1).set(fasePosterior);
            udo1LinhaNova.getExtensionField(EXTF_DATA_INICIO).set(new SysDate(Calendar.getInstance().getTime()));

            udo1Collection.add(udo1LinhaNova);
        }
    }

    // CLM.033 Atualiza um calendario automaticamente no AB
    private void gap033_atualizaCalendario() throws ChainedException {

        ponto = "gap033_atualizaCalendario()";
        String faseAnterior = current_phase;
        IBeanIfc udo1Calendario;
        ObjectReference docRefUDO1;

        if (hasValue(faseAnterior)) {
            // Loop que atualiza todos os calendarios referentes a fase anterior
            for (int i = 0; i < udo1Collection.size(); i++) {
                udo1Calendario = udo1Collection.get(i);
                if (!hasValue(udo1Calendario.getExtensionField(EXTF_DATA_FIM).get())) {
                    if (udo1Calendario.getExtensionField(EXTF_FASE_UDO1).get().equals(faseAnterior)) {
                        docRefUDO1 = (ObjectReference) udo1Calendario.getExtensionField(EXTF_REF_DOC).get();
                        if (docRefUDO1.equals(doc.getObjectReference())) {
                            udo1Calendario.getExtensionField(EXTF_DATA_FIM).set(new SysDate(Calendar.getInstance().getTime()));
                        }
                    }
                }
            }
        }
    }

    // Busca aos colaboradores conforme a fase do documento
    private void gap033_buscaColaboradoresUDMD1()
            throws ChainedException, DatabaseException, NoConnectionException, InterruptedException, SQLException {

        ponto = "buscaColaboradoresUDMD1()";

        listUserGAP033 = new ArrayList();

        String conDocPhaseConfigExtId = phaseConfidExtId;
        CollaboratorIBeanIfc colab = null;
        GroupIBeanIfc grupoColab = null;
        OrderedSubordinateCollectionIfc colabs = null;
        IapiAccountIfc user = null;
        String udoFase;
        String udoRepresenta;
        String userRepresenta;
        String faseAtual = other_phase;

        // Localizando a tabela de alertas conforme o External Id da configuração de fase em questão
        UserDefinedMasterData1IBeanIfc udo1PhaseConfig = udo1Home.findByExternalId(conDocPhaseConfigExtId);

        if (hasValue(udo1PhaseConfig)) {

            ExtensionCollectionIfc collecUdo1 = udo1PhaseConfig.getExtensionCollection(EXTC_ALERTAS);

            // Loop para recuperar todos que deverão receber o e-mail de notificação
            for (int i = 0; i < collecUdo1.size(); i++) {

                IBeanIfc linha = collecUdo1.get(i);

                // Dados recuperados da linha corrente em UDO1
                udoFase = ((ObjectReference) linha.getExtensionField(EXTF_FASE).get()).getDisplayName();
                udoRepresenta = ((ObjectReference) linha.getExtensionField(EXTF_REPRESENTA)
                		.get()).getDisplayName();

                if (faseAtual.equals(udoFase)) {

                    ponto = "buscaColaboradoresUDMD1() 002";

                    colabs = maAgr.getCollaborators();

                    for (int j = 0; j < colabs.size(); j++) {
                        colab = (CollaboratorIBeanIfc) colabs.get(j);
                        userRepresenta = colab.getRepresenting().getDisplayName();
                        if (userRepresenta.equals(udoRepresenta)) {
                            if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.user) {
                                user = IapiAccountLocator.lookup(session, colab.getPrincipal());
                                if ((Boolean) linha.getExtensionField(EXTF_ATIVIDADE).get()) {
                                    listUserGAP033.add(user.getAccountObjectReference());
                                }
                            } else if (colab.getCollaboratorType().get() == CollaboratorTypeEnumType.group) {
                                grupoColab = (GroupIBeanIfc) grupoHome.find(colab.getPrincipal());
                                Iterator membrosGrupo = grupoColab.findGroupMembers().iterator();
                                while (membrosGrupo.hasNext()) {
                                    user = IapiAccountLocator.lookup(session, (ObjectReference) membrosGrupo.next());
                                    if ((Boolean) linha.getExtensionField(EXTF_ATIVIDADE).get()) {
                                        listUserGAP033.add(user.getAccountObjectReference());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CLM.034
    private void gap034_retrocessoFase() throws ApplicationException, DatabaseException {

        ponto = "gap034_retrocessoFase()";

        ExtensionCollectionIfc collHistoricoJus = null;
        String textoCampoJustificativa = "";

        // Se estiver voltando uma fase do documento e a fase corrente diferente de "Executed"
        if (!phase_advancing && !current_phase.equalsIgnoreCase("Executed")) {

            try {
                textoCampoJustificativa = doc.getExtensionField(EXTF_JUSTIFICATIVA).get().toString();
            } catch (Exception e) {
                throwAppException("gap034.erro.preencher.campo", EXTF_JUSTIFICATIVA, null);
            }

            if (!hasValue(textoCampoJustificativa)) {
                throwAppException("gap034.erro.preencher.campo", EXTF_JUSTIFICATIVA, null);
            } else if (textoCampoJustificativa.length() < 10) {
                throwAppException("gap034.erro.caracteres.minimo", EXTF_JUSTIFICATIVA, null);
            } else {
                collHistoricoJus = doc.getExtensionCollection(EXTF_HISTORICO_JUS);
                IBeanIfc lineAdd = collHistoricoJus.create();

                IapiDocumentLockManager.lockField(session, doc, EXTF_HISTORICO_JUS);

                lineAdd.getExtensionField(EXTF_DE_FASE).set(current_phase);
                lineAdd.getExtensionField(EXTF_PARA_FASE).set(other_phase);
                lineAdd.getExtensionField(EXTF_JUSTIFICATIVA).set(textoCampoJustificativa);
                lineAdd.getExtensionField(EXTF_DATA).set(new SysDatetime(Calendar.getInstance().getTime()));
                lineAdd.getExtensionField(EXTF_USUARIO).set(session.getAccount().getAccountObjectReference());

                IapiDocumentLockManager.lockField(session, lineAdd, EXTF_DE_FASE);
                IapiDocumentLockManager.lockField(session, lineAdd, EXTF_PARA_FASE);
                IapiDocumentLockManager.lockField(session, lineAdd, EXTF_JUSTIFICATIVA);
                IapiDocumentLockManager.lockField(session, lineAdd, EXTF_DATA);
                IapiDocumentLockManager.lockField(session, lineAdd, EXTF_USUARIO);

                collHistoricoJus.add(lineAdd);
            }
        }
        doc.getExtensionField(EXTF_JUSTIFICATIVA).set("");
    }

    // CLM.033
    private void gap033_inicio() {

        udo1GAP33 = null;
        udo1Collection = null;
        try {
            udo1GAP33 = udo1Home.findByExternalId(REGISTRO_GAP033);
            if (!hasValue(udo1GAP33)) {
                gap033_criaRegistroInicialUDMD1();
                udo1GAP33 = udo1Home.findByExternalId(REGISTRO_GAP033);
            }
            udo1Home.upgradeToEdit(udo1GAP33);
            udo1Collection = udo1GAP33.getExtensionCollection(EXTC_MANUTENCAO);
            gap033_atualizaCalendario();
            gap033_criaCalendario(new HashSet(listUserGAP033));
            gap033_salvaRegistroUMD1();

        } catch (Exception e) {
            logError(ponto + " " + e.getMessage());
        }
    }

    private void initComponents() throws Exception {

        ponto = "initComponents()";

        udo1Home = (UserDefinedMasterData1IBeanHomeIfc) IBeanHomeLocator
                .lookup(session, UserDefinedMasterData1IBeanHomeIfc.sHOME_NAME);
        grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, GroupIBeanHomeIfc.sHOME_NAME);
        docTypeHome = (ContractDocumentTypeIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ContractDocumentTypeIBeanHomeIfc.sHOME_NAME);
        phaseConfidExtId = getPhaseConfigExtId(doc);
    }

    public void inicio() throws Exception {

        Date inicioScript = Calendar.getInstance().getTime();

        try {
            ponto = "inicio()";

            initComponents();

            if (hasValue(current_phase)) {
                if (!session.getAccount().getUserName().equals("WORKFLOWUSER")) {
                    // Valida se o usuário que esta a mudar de fase é autorizado para isso.
                    gap028_mudancaFasePorUserAutorizado();
                    // Somente pode voltar uma fase caso tenha preenchido o campo de Justificativa
                    gap034_retrocessoFase();
                }
            }
            // Permite avançar uma determinada fase para outra de acordo com a versão do documento de contrato
            gap027_mudancaFasePelaVersaoDoc();

            // Coleta os colaboradores para o GAP033 e chama seus métodos mas só 
            // se o pai do documento de contrato for um Master Agreement
            if (doc.getParentIBean() instanceof ContractIBeanIfc) {
                maAgr = (ContractIBeanIfc) doc.getParentIBean();
                gap033_buscaColaboradoresUDMD1();
                gap033_inicio();
            }

        } catch (Exception e) {
            if (e instanceof ApplicationException) {
                throw e;
            } else {
                throw new ApplicationException(ponto + " " + e.getMessage());
            }
        }

        long duracao = (Calendar.getInstance().getTime().getTime() - inicioScript.getTime());
        logInfo("Duração da execução do script " + formatInterval(duracao));
    }

    
    //inicio();
}
