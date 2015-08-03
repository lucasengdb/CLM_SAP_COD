package scripts.scriptdefinition;



/* 
MA - Validated - Any - Any
Atende:
CLM.003 (Validação de campos Z),  
CLM.012 (Partes relacionadas), 
CLM.016 (Ampliação de niveis organizacionais), 
CLM.015 (Controle de Assinaturas),
CLM.014 (Aprovação societaria)
CLM.005 (Prenchimento automatico do CNPJ/CPF do Fornecedor)
CLM.053 (Controle de alterações do Questionário de Aprovação Societária)
*/
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.doc.IapiDocumentLockManager;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.VendorIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.VendorIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData2IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData2IBeanIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData4IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData4IBeanIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData5IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData5IBeanIfc;
import com.sap.odp.api.ibean.ExtensionCollectionIfc;
import com.sap.odp.api.ibean.ExtensionFieldIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanHomeIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanIfc;
import com.sap.odp.common.db.ObjectReference;
import com.sap.odp.common.platform.SessionContextIfc;
import com.sap.odp.common.types.BigText;
import com.sap.odp.common.types.Price;
import com.sap.odp.common.types.SysDatetime;
import com.sap.odp.doccommon.formatters.PriceFormatter;

import cmldummy.CLMDummyScriptDefinition;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Calendar;
import java.util.HashMap;

public class MA_Validated_Any_Any extends CLMDummyScriptDefinition{

	   ContractIBeanIfc doc;
	    // Para portar este código em "Script Definition" copiar todo codigo a partir daqui
	    // e remover o comentario da chamado do metodo "inicio"
	    //
	    //
	

		// Constantes
	    private final String LOGGER_ID = "\n[MA VALIDATED ANY ANY] ";
	    // Extension Collections
	    private final String EXTF_GERENCIA = "MA_GERENCIA";
	    private final String EXTF_AREA = "MA_AREA";
	    private final String EXTC_OUTORGADOS = "outorgados";
	    private final String EXTC_COLETA_ASS_OUTORGADOS = "coleta_assinaturas";
	    private final String EXTC_COLETA_ASS_REPRESENTANTES = "coleta_representantes";
	    private final String EXTF_EMPRESA1 = "EMPRESA_1";
	    private final String EXTF_EMPRESA2 = "EMPRESA_2";
	    private final String EXTF_EMPRESA3 = "EMPRESA_3";
	    // Outras Constantes    
	    private final String TIPO_DIRETORIA = "Diretoria";
	    private final String TIPO_GERENCIA = "Gerência";
	    private final String TIPO_AREA = "Área";
	    private final String REPRESENTS_ADVG_JURID_SOC = "Advg. Jurídico - Societário";
	    // Variaveis
	    ValueListValueIBeanIfc vlv;
	    ValueListValueIBeanHomeIfc vlvHome;
	    VendorIBeanHomeIfc vendorHome;
	    BusinessUnitIBeanIfc buDiretoria;
	    BusinessUnitIBeanIfc buGerencia;
	    BusinessUnitIBeanIfc buArea;
	    UserDefinedMasterData5IBeanHomeIfc udo5Home;
	    UserDefinedMasterData4IBeanHomeIfc udo4Home;
	    UserDefinedMasterData2IBeanHomeIfc udo2Home;
	    BusinessUnitIBeanHomeIfc buHome;
	    String schema;
	    String tabDyn_MA;

	    private final String aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX = "pergunta_";
	    private final String aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX = "resposta_";
	    private final String aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX = "soc_parceira";
	    private final String aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX = "orgao_social";
	    private final int aprovSoc_MAX_EXTFIELDS_PERGUNTA = 20;
	    private List aprovSoc_listPergSocParc;
	    private Map aprovSoc_mapaDeNumPerguntaParaOrgaoAprovador;

	    // GAP015
	    private final String VLV_DIR_PRESIDENTE = "cargo_001";
	    private final String VLV_DIR_ESTATUTARIO = "cargo_002";

	    private String ponto = "";

	    private ApplicationException getAppException(String campo, String msgId) {
	        ApplicationException ex = doc.createApplicationException(campo, msgId);
	        ex.setBundleName("tim.defdata");
	        return ex;
	    }

	    public void logInfo(String mensagem) {
	        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
	    }

	    public void logError(String mensagem) {
	        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
	    }

	    private String getNomeTabelaDinamica(int classId) throws Exception {
	        String resultado = "";
	        String sql = "SELECT FCI_SYS_DYN_CLASSES.TABLE_NAME FROM "
	                + schema + ".FCI_SYS_DYN_CLASSES, "
	                + schema + ".FCI_SYS_DYN_MEMBERS "
	                + "WHERE "
	                + "FCI_SYS_DYN_CLASSES.OBJECTID = FCI_SYS_DYN_MEMBERS.PARENT_OBJECT_ID "
	                + "AND FCI_SYS_DYN_CLASSES.ASSOC_CLASSID = " + classId + " "
	                + "AND UPPER(FCI_SYS_DYN_CLASSES.COLLECTION_NAME) IS NULL "
	                + "GROUP BY TABLE_NAME";
	        session.getDbHandle().beginTransaction();
	        session.getDbHandle().executeQuery(sql);
	        ResultSet rs = session.getDbHandle().getResultSet();
	        if (rs.next()) {
	            resultado = rs.getString("TABLE_NAME");
	        }
	        session.getDbHandle().endTransaction();
	        return resultado;
	    }

	    private String getNomeTabelaDinamica(int classId, String collectionName) throws Exception {
	        String sql;
	        String resultado = "";
	        sql = "SELECT FCI_SYS_DYN_CLASSES.TABLE_NAME FROM "
	                + schema + ".FCI_SYS_DYN_CLASSES, "
	                + schema + ".FCI_SYS_DYN_MEMBERS "
	                + "WHERE "
	                + "FCI_SYS_DYN_CLASSES.OBJECTID = FCI_SYS_DYN_MEMBERS.PARENT_OBJECT_ID "
	                + "AND FCI_SYS_DYN_CLASSES.ASSOC_CLASSID = " + classId + " "
	                + "AND UPPER(FCI_SYS_DYN_CLASSES.COLLECTION_NAME) = UPPER('" + collectionName + "') "
	                + "GROUP BY TABLE_NAME";
	        session.getDbHandle().beginTransaction();
	        session.getDbHandle().executeQuery(sql);
	        ResultSet rs = session.getDbHandle().getResultSet();
	        if (rs.next()) {
	            resultado = rs.getString("TABLE_NAME");
	        }
	        session.getDbHandle().endTransaction();
	        return resultado;
	    }

	    private String aprovSoc_leftPadIntAsString(int num, int pos, String padStr) {

	        ponto = "aprovSoc_leftPadIntAsString() - P00";
	        String s = Integer.toString(num);
	        while (s.length() < pos) {
	            s = padStr + s;
	        }
	        return s;
	    }

	    private int aprovSoc_getPadLen(String field) {
	        ponto = "aprovSoc_getPadLen() - P00";
	        int retval = 0;
	        if (field.startsWith(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX)) {
	            retval = 3;
	        } else if (field.startsWith(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX)) {
	            retval = 3;
	        } else if (field.startsWith(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX)) {
	            retval = 0;
	        } else if (field.startsWith(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX)) {
	            retval = 3;
	        } else {
	            retval = 0;
	        }
	        return retval;
	    }

	    private String aprovSoc_getPadString(String field, int n) {
	        ponto = "aprovSoc_getPadString() - P08";
	        return aprovSoc_leftPadIntAsString(n, aprovSoc_getPadLen(field), "0");
	    }

	    private ExtensionFieldIfc aprovSoc_getExtFieldNumbered(String extFieldName, int numCampoBase1) throws Exception {

	        if (numCampoBase1 == 0) {
	            throw new ApplicationException(
	                    "aprovSoc_getExtensionFieldNumber() chamada com numCampoBase1 ZERO!");
	        }

	        String extFieldNameComZeros = extFieldName
	                + aprovSoc_getPadString(extFieldName, numCampoBase1);
	        String extFieldNameSemZeros = extFieldName + numCampoBase1;
	        ExtensionFieldIfc fld;

	        fld = null;
	        try {
	            fld = doc.getExtensionField(extFieldNameSemZeros);
	        } catch (Exception e) {
	            // nada
	        }

	        if (fld == null) {
	            try {
	                fld = doc.getExtensionField(extFieldNameComZeros);
	            } catch (Exception e) {
	                // nada
	            }
	        }

	        if (fld == null) {
	            throw new ApplicationException(
	                    "Master Agreement '"
	                    + doc.getDocumentId()
	                    + "' - Nao foi achado extension field com nome "
	                    + extFieldNameComZeros + " nem com nome "
	                    + extFieldNameSemZeros);
	        }
	        return fld;
	    }

	    public ValueListTypeIBeanIfc getValueListTypeObjIdByExtId(String externalId) throws Exception {

	        ValueListTypeIBeanHomeIfc vltHome = (ValueListTypeIBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, ValueListTypeIBeanHomeIfc.sHOME_NAME);
	        List vltList = vltHome.findWhere("EXTERNAL_ID = '" + externalId + "'");
	        if (vltList.isEmpty()) {
	            throw new ApplicationException("ListType '" + externalId
	                    + "' não encontrado.");
	        }
	        ValueListTypeIBeanIfc vlt = (ValueListTypeIBeanIfc) vltList.iterator()
	                .next();

	        return vlt;
	    }

	    // Preenche automaticamente o campo "Sociedade Parceira"
	    private void gap012_preencheSociedadeParceira() throws ChainedException {

	        ponto = "base_preencheSociedadeParceira() - P00";
	        VendorIBeanIfc vendor = (VendorIBeanIfc) vendorHome.find(doc.getVendorRef());
	        if (hasValue(vendor) && hasValue(vendor.getExtensionField("VBUND").get())) {
	            String sociedadeParceira = vendor.getExtensionField("VBUND").get().toString();
	            doc.getExtensionField("MA_VBUND").set(sociedadeParceira);
	        } else {
	            doc.getExtensionField("MA_VBUND").set("N/A");
	        }
	    }

	    private boolean aprovSoc_isBlankExtFieldCorpoPergunta(ExtensionFieldIfc extFieldCorpoN) throws Exception {

	        if (!hasValue(extFieldCorpoN) || !hasValue(extFieldCorpoN.get())) {
	            return true;
	        }
	        BigText bigTextCorpo = (BigText) extFieldCorpoN.get();
	        String preview = bigTextCorpo.getTextPreview();
	        if (preview == null) {
	            return true;
	        }

	        return preview.trim().isEmpty();
	    }

	    private boolean aprovSoc_existePerguntaNumero(int numPerguntaBase_1) throws Exception {

	        ponto = "aprovSoc_extFieldExistePerguntaNumero() - P00";

	        ExtensionFieldIfc extFieldCorpoN = null;
	        try {
	            extFieldCorpoN = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, numPerguntaBase_1);
	        } catch (Exception e) {
	            // nada - exception quer dizer que nao achou extension field
	        }

	        boolean existePergunta = false;
	        if (!hasValue(extFieldCorpoN)) {
	            existePergunta = false;
	        } else {
	            existePergunta = !aprovSoc_isBlankExtFieldCorpoPergunta(extFieldCorpoN);
	        }

	        return existePergunta;
	    }

	    private boolean aprovSoc_extFieldYesNo_naoPreenchido(ExtensionFieldIfc extFieldResposta) throws Exception {

	        ponto = "aprovSoc_extFieldYesNo_naoPreenchido() - P00";
	        boolean retval = false;
	        retval = extFieldResposta == null || extFieldResposta.get() == null || !hasValue(extFieldResposta.get());

	        return retval;
	    }

	    private void aprovSoc_checarPreenchimentoObrigatorio() throws Exception {

	        ponto = "aprovSoc_chkPreencObr() - P00";
	        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
	            if (!aprovSoc_existePerguntaNumero(n + 1)) {
	                break;
	            }

	            ExtensionFieldIfc extFieldRespostaN = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);

	            if (aprovSoc_extFieldYesNo_naoPreenchido(extFieldRespostaN)) {
	                throw getAppException(null, "mensagem.gap014.todas_perguntas_respondidas_obrigatoria");
	            }
	        }
	    }

	    private Map aprovSoc_buildMapaDeNumPerguntaParaOrgaoAprovador() throws Exception {

	        ponto = "aprovSoc_buildMapaDeNumPerguntaParaOrgaoAprovador() - P00";
	        Map map = new HashMap();
	        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
	            if (!aprovSoc_existePerguntaNumero(n + 1)) {
	                break;
	            }
	            ExtensionFieldIfc extFieldOrgaoAprovN = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);
	            ObjectReferenceIfc objRefOrgaoAprovador = (ObjectReferenceIfc) extFieldOrgaoAprovN.get();
	            ValueListValueIBeanIfc vlvOrgaoAprovador = (ValueListValueIBeanIfc) vlvHome
	                    .find(objRefOrgaoAprovador);

	            map.put("" + (n + 1), vlvOrgaoAprovador);
	        }
	        return map;
	    }

	    private ValueListValueIBeanIfc aprovSoc_getVlvOrgaoSocialAprovadorDeNumPergunta(int numPergunta) {

	        ponto = "aprovSoc_getVlvOrgaoSocialAprovadorDeNumPergunta() - P00";
	        String key = "" + numPergunta;
	        return (ValueListValueIBeanIfc) aprovSoc_mapaDeNumPerguntaParaOrgaoAprovador.get(key);
	    }

	    private String aprovSoc_getTextsFromVlvOrgaoAprovador(ValueListValueIBeanIfc vlvOrgaoAprovador) {

	        ponto = "aprovSoc_getTextsFromVlvOrgaoAprovador() - P00 - vlvOrgaoAprovador="
	                + vlvOrgaoAprovador;
	        String retval = "";
	        retval = retval + vlvOrgaoAprovador.getDisplayName();

	        return retval;
	    }

	    private void aprovSoc_listarAprovacoesNecessarias() throws Exception {

	        ponto = "aprovSoc_listarAprovacoesNecessarias() - P00";
	        aprovSoc_mapaDeNumPerguntaParaOrgaoAprovador = (Map) aprovSoc_buildMapaDeNumPerguntaParaOrgaoAprovador();
	        Map semDupEmOrdem = new LinkedHashMap();
	        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
	            if (!aprovSoc_existePerguntaNumero(n + 1)) {
	                break;
	            }

	            ExtensionFieldIfc extFieldResposta = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);

	            Boolean isRespostaTrue = aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta(extFieldResposta);
	            if (isRespostaTrue) {
	                ValueListValueIBeanIfc vlvOrgaoAprovador = aprovSoc_getVlvOrgaoSocialAprovadorDeNumPergunta(n + 1);
	                semDupEmOrdem.put(aprovSoc_getTextsFromVlvOrgaoAprovador(vlvOrgaoAprovador), vlvOrgaoAprovador);
	            }
	        }

	        ExtensionCollectionIfc listaOrgaosAprovadores = doc
	                .getExtensionCollection("orgaos_sociais");

	        while (listaOrgaosAprovadores.size() > 0) {
	            listaOrgaosAprovadores.delete(listaOrgaosAprovadores.get(0));
	        }

	        int numLinAtualAprovs = 0;
	        for (Object oOrgao : semDupEmOrdem.keySet()) {
	            String strOrgao = (String) oOrgao;
	            ValueListValueIBeanIfc vlvOrgao = (ValueListValueIBeanIfc) semDupEmOrdem
	                    .get(strOrgao);
	            listaOrgaosAprovadores.add(listaOrgaosAprovadores.create());
	            listaOrgaosAprovadores.get(numLinAtualAprovs)
	                    .getExtensionField("orgao_social")
	                    .set(vlvOrgao.getLocalizedObjectReference());
	            IapiDocumentLockManager.lockField(session,
	                    listaOrgaosAprovadores.get(numLinAtualAprovs),
	                    "orgao_social");

	            numLinAtualAprovs++;
	        }
	    }

	    private String aprovSoc_getValorDoCampoNumSociedadeParceira() throws Exception {

	        ponto = "aprovSoc_getValorDoCampoNumSociedadeParceira() - P00";
	        String nomeCampoNumSocParceira = "MA_VBUND";
	        ExtensionFieldIfc extFieldNumSocParceira = doc
	                .getExtensionField(nomeCampoNumSocParceira);
	        String valorNumSocParceira = (String) extFieldNumSocParceira.get();

	        return valorNumSocParceira;
	    }

	    private boolean aprovSoc_campoNumSociedadeParceiraPreenchido() throws Exception {

	        ponto = "aprovSoc_campoNumSociedadeParceiraPreenchido() - P00";
	        String valorCampoNumSocParc = aprovSoc_getValorDoCampoNumSociedadeParceira();
	        return hasValue(valorCampoNumSocParc) && !valorCampoNumSocParc.isEmpty()
	                && !valorCampoNumSocParc.trim().equals("N/A");
	    }

	    private boolean isPerguntaVinculadaEmSociedadeParceira(int numPerguntaBase_1) {

	        ponto = "isPerguntaVinculadaEmSociedadeParceira() - P00";
	        boolean retval = false;
	        if (aprovSoc_listPergSocParc.contains("" + numPerguntaBase_1)) {
	            retval = true;
	        }
	        return retval;
	    }

	    private void aprovSoc_buildListPergSocParc() throws Exception {

	        ponto = "aprovSoc_buildListPergSocParc() - P00";
	        aprovSoc_listPergSocParc = new ArrayList();

	        String debugTextoVinculadas = "";
	        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
	            if (!aprovSoc_existePerguntaNumero(n + 1)) {
	                break;
	            }
	            ExtensionFieldIfc extFieldSocparcN = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, n + 1);
	            Boolean sociedadeParceira = (Boolean) extFieldSocparcN.get();
	            if (sociedadeParceira) {
	                aprovSoc_listPergSocParc.add("" + (n + 1));
	                debugTextoVinculadas = debugTextoVinculadas + " , " + (n + 1);
	            }
	        }
	        if ((new Date()).getTime() == 0 /*a.k.a if FALSE*/) {
	            throw new ApplicationException("Veja pontoPassei, perguntas vinculadas");
	        }
	    }

	    private void aprovSoc_questObrigSeNumSocParc() throws Exception {

	        ponto += "aprovSoc_questObrigSeNumSocParc() - P00";
	        if (!aprovSoc_campoNumSociedadeParceiraPreenchido()) {
	            return;
	        }
	        aprovSoc_buildListPergSocParc();

	        int qtdPerguntasVinculadas = 0;
	        boolean algumaPergVinculadaRespondidaSim = false;
	        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {

	            if (!aprovSoc_existePerguntaNumero(n + 1)) {
	                break;
	            }

	            ExtensionFieldIfc extFieldResposta = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);

	            boolean perguntaVinculadaSocParc = isPerguntaVinculadaEmSociedadeParceira(n + 1);
	            Boolean isRespostaSIM = aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta(extFieldResposta);

	            if (perguntaVinculadaSocParc) {
	                qtdPerguntasVinculadas++;
	                if (isRespostaSIM) {
	                    algumaPergVinculadaRespondidaSim = true;
	                }
	            }
	        }
	        if (qtdPerguntasVinculadas > 0 && !algumaPergVinculadaRespondidaSim) {
	            throw getAppException(null, "mensagem.gap014.fornecedor_soc_parceira");
	        }
	    }

	    private void aprovSoc_addGrupoJuridSocietario() throws Exception {

	        vlv = (ValueListValueIBeanIfc) vlvHome.findWhere("DISPLAY_NAME = '" + REPRESENTS_ADVG_JURID_SOC + "'")
	                .iterator().next();

	        ponto = "aprovSoc_addGrupoJuridSocietario() - P00";
	        GroupIBeanHomeIfc grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, GroupIBeanHomeIfc.sHOME_NAME);
	        GroupIBeanIfc grupo = grupoHome.findGroup("TIM.Aprovadores.Juridico_Societario");
	        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
	        CollaboratorRoleIBeanIfc roleAprovadorIfc = (CollaboratorRoleIBeanIfc) roleHome
	                .findWhere("DISPLAY_NAME = 'Aprovador'").iterator().next();
	        CollaboratorIBeanIfc collab = (CollaboratorIBeanIfc) doc.getCollaborators().create();
	        collab.setPrincipal(grupo.getObjectReference());
	        collab.setCollaboratorRole(roleAprovadorIfc.getObjectReference());
	        collab.setLockRole(true);
	        collab.setLockPrincipal(true);
	        if (!hasValue(vlv)) {
	            throw new ApplicationException("Não foi possivel encontrar o Value List Value com o "
	                    + "'Display Name Id' '" + REPRESENTS_ADVG_JURID_SOC + "' em 'Department Name'.");
	        }
	        collab.setRepresenting(vlv.getLocalizedObjectReference());
	        doc.getCollaborators().add(collab);
	    }

	    private boolean aprovSoc_existeGrupoJuridSocietario() throws Exception {

	        ponto = "aprovSoc_removeGrupoJuridSocietario() - P00";
	        GroupIBeanHomeIfc grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, GroupIBeanHomeIfc.sHOME_NAME);
	        GroupIBeanIfc grupo = grupoHome
	                .findGroup("TIM.Aprovadores.Juridico_Societario");
	        int i = 0;
	        for (i = 0; i < doc.getCollaborators().size(); i++) {
	            CollaboratorIBeanIfc collab = (CollaboratorIBeanIfc) doc
	                    .getCollaborators().get(i);
	            if (collab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.group) {
	                if (collab.getPrincipal().equals(grupo.getObjectReference())) {
	                    vlv = (ValueListValueIBeanIfc) vlvHome.findWhere("DISPLAY_NAME = '" + REPRESENTS_ADVG_JURID_SOC + "'")
	                            .iterator().next();
	                    if (!hasValue(vlv)) {
	                        throw new ApplicationException("Não foi possivel encontrar o Value List Value com o "
	                                + "'Display Name Id' '" + REPRESENTS_ADVG_JURID_SOC + "' em 'Department Name'.");
	                    }
	                    collab.setRepresenting(vlv.getLocalizedObjectReference());
	                    return true;
	                }
	            }
	        }
	        return false;
	    }

	    private Boolean aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta(ExtensionFieldIfc extFieldResposta) throws Exception {

	        ponto = "aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta() - P00";

	        ObjectReferenceIfc objRefDeResposta = (ObjectReferenceIfc) extFieldResposta.get();

	        if (!hasValue(objRefDeResposta)) {
	            return null;
	        }

	        Boolean retval = null;
	        ValueListTypeIBeanIfc vlt;
	        ValueListValueIBeanIfc vlvSim;
	        ValueListValueIBeanIfc vlvNao;

	        vlt = getValueListTypeObjIdByExtId("respostas");

	        vlvSim = vlvHome.findUniqueByNameType("sim", vlt.getTypeCode());

	        vlvNao = vlvHome.findUniqueByNameType("nao", vlt.getTypeCode());

	        if (objRefDeResposta.equals(vlvSim.getObjectReference())) {
	            retval = true;
	        } else if (objRefDeResposta.equals(vlvNao.getObjectReference())) {
	            retval = false;
	        } else {
	            retval = null;
	        }

	        return retval;
	    }

	    // CHECA SE EXISTE PELO MENOS UMA RESPOSTA SIM NO QUESTIONARIO DA APROV. SOC.
	    private boolean aprovSoc_checaSeExisteRespSim() throws Exception {
	        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
	            if (!aprovSoc_existePerguntaNumero(n + 1)) {
	                break;
	            }
	            ExtensionFieldIfc extFieldResposta = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);
	            Boolean isRespostaSIM = aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta(extFieldResposta);
	            if (isRespostaSIM) {
	                return true;
	            }
	        }
	        return false;
	    }

	    private void aprovSoc_checarSeAdicinaAprovadorGrupoJuridicoSocietario() throws Exception {

	        ponto = "aprovSoc_checarSeAdicionaAprovadorGrupoJuridicoSocietario() - P00";
	        if (aprovSoc_existeGrupoJuridSocietario()) {
	            return;
	        }
	        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
	            if (!aprovSoc_existePerguntaNumero(n + 1)) {
	                break;
	            }
	            ExtensionFieldIfc extFieldResposta = aprovSoc_getExtFieldNumbered(
	                    aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);
	            Boolean isRespostaSIM = aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta(extFieldResposta);

	            if (isRespostaSIM) {
	                aprovSoc_addGrupoJuridSocietario();
	                break;
	            }
	        }
	    }

	    private void gap014_aprovSoc() throws Exception {

	        ponto = "aprovSoc_Main() - P00";
	        if (doc.isTemplate()) {
	            return;
	        }

	        aprovSoc_checarPreenchimentoObrigatorio();
	        aprovSoc_checarSeAdicinaAprovadorGrupoJuridicoSocietario();
	        aprovSoc_questObrigSeNumSocParc();
	        if ((new Date()).getTime() == 0 /*a.k.a IF FALSE*/) {
	            throw new ApplicationException("DEBUG - Veja debug post [aprovSoc_questObrigSeNumSocParc()]");
	        }
	        // "1 e 2 na lista" 
	        aprovSoc_listarAprovacoesNecessarias();
	    }

	    // Valida o preenchimento correto em "Diretoria", "Gerência" e "Área"
	    private void gap016_validaDirGerArea() throws Exception {

	        boolean condicao = false;
	        ObjectReference objRef = null;

	        // Diretoria [######] Gerência [     ] Área [     ]
	        condicao = hasValue(doc.getBusinessUnitRef())
	                && !hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
	                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
	        if (condicao) {
	            // Verifica se o campo "Diretotia" esta preenchido corretamente
	            buDiretoria = ((BusinessUnitIBeanIfc) buHome.find((ObjectReference) doc.getBusinessUnitRef()));
	            objRef = (ObjectReference) buDiretoria.getFieldMetadata("ORG_UNIT_TYPE").get(buDiretoria);
	            if (!objRef.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
	                throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_preenchimento_incorreto");
	            }
	        }

	        // Diretoria [#####] Gerência [#####] Área [     ]
	        condicao = hasValue(doc.getBusinessUnitRef())
	                && hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
	                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
	        if (condicao) {
	            // Verifica se o campo "Gerência" esta preenchido corretamente
	            buGerencia = ((BusinessUnitIBeanIfc) buHome.find((ObjectReferenceIfc) doc.getExtensionField(EXTF_GERENCIA).get()));
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buGerencia.getFieldMetadata("ORG_UNIT_TYPE").get(buGerencia));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_GERENCIA)) {
	                throw getAppException(EXTF_GERENCIA, "mensagem.gap016.erro.gerencia_preenchimento_incorreto");
	            }
	            // Verifica se o campo "Diretoria" esta preenchido corretamente
	            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buDiretoria.getFieldMetadata("ORG_UNIT_TYPE").get(buDiretoria));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
	                throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_preenchimento_incorreto");
	            }
	            if (!doc.getBusinessUnitRef().equals(buDiretoria.getObjectReference())) {
	                throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_e_gerencia_nao_correspondem");
	            }
	            return;
	        }

	        // Diretoria [#####] Gerência [    ] Área [#####]
	        condicao = hasValue(doc.getBusinessUnitRef())
	                && !hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
	                && hasValue(doc.getExtensionField(EXTF_AREA).get());
	        if (condicao) {
	            // Verifica se o campo "Diretotia" esta preenchido corretamente
	            buDiretoria = ((BusinessUnitIBeanIfc) buHome
	                    .find((ObjectReferenceIfc) doc.getBusinessUnitRef()));
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buDiretoria.getFieldMetadata("ORG_UNIT_TYPE").get(buDiretoria));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
	                throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_preenchimento_incorreto");
	            }
	            // Verifica se o campo "Área" esta preenchido corretamente
	            buArea = ((BusinessUnitIBeanIfc) buHome
	                    .find((ObjectReferenceIfc) doc.getExtensionField(EXTF_AREA)
	                            .get()));
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buArea.getFieldMetadata("ORG_UNIT_TYPE").get(buArea));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_AREA)) {
	                throw getAppException(EXTF_AREA, "mensagem.gap016.erro.area_preenchimento_incorreto");
	            }
	            // Checando se correspondem
	            buGerencia = (BusinessUnitIBeanIfc) buHome.find(buArea.getParentObjRef());
	            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
	            if (!doc.getBusinessUnitRef().equals(buDiretoria.getObjectReference())) {
	                throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_e_area_nao_correspondem");
	            }
	            // Preenchendo campo "Gerência" de acordo com "Área"
	            doc.getExtensionField(EXTF_GERENCIA).set(buGerencia.getObjectReference());
	            return;
	        }

	        // Diretoria [#####] Gerência [#####] Área [#####]
	        condicao = hasValue(doc.getBusinessUnitRef())
	                && hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
	                && hasValue(doc.getExtensionField(EXTF_AREA).get());
	        if (condicao) {
	            // Verifica se o campo "Diretotia" esta preenchido corretamente
	            buDiretoria = ((BusinessUnitIBeanIfc) buHome
	                    .find((ObjectReferenceIfc) doc.getBusinessUnitRef()));
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buDiretoria.getFieldMetadata("ORG_UNIT_TYPE").get(buDiretoria));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
	                throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_preenchimento_incorreto");
	            }
	            // Verifica se o campo "Gerência" esta preenchido corretamente
	            buGerencia = ((BusinessUnitIBeanIfc) buHome
	                    .find((ObjectReferenceIfc) doc.getExtensionField(EXTF_GERENCIA).get()));
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buGerencia.getFieldMetadata("ORG_UNIT_TYPE").get(buGerencia));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_GERENCIA)) {
	                throw getAppException(EXTF_GERENCIA, "mensagem.gap016.erro.gerencia_preenchimento_incorreto");
	            }
	            // Verifica se o campo "Área" esta preenchido corretamente
	            buArea = ((BusinessUnitIBeanIfc) buHome.find((ObjectReferenceIfc) doc.getExtensionField(EXTF_AREA).get()));
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buArea.getFieldMetadata("ORG_UNIT_TYPE").get(buArea));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_AREA)) {
	                throw getAppException(EXTF_AREA, "mensagem.gap016.erro.area_preenchimento_incorreto");
	            }
	            // Checando se correspondem
	            if (!buDiretoria.getObjectReference().equals(buGerencia.getParentObjRef())
	                    || !buGerencia.getObjectReference().equals(buArea.getParentObjRef())) {
	                throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_gerencia_area_nao_correspondem");
	            }
	            return;
	        }

	        // Diretoria [     ] Gerência [     ] Área [     ]
	        condicao = !hasValue(doc.getBusinessUnitRef())
	                && !hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
	                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
	        if (condicao) {
	            throw getAppException(ContractIBeanIfc.sPROPID_BUSINESS_UNIT, "mensagem.gap016.erro.diretoria_gerencia_area_obrigatorio");
	        }

	        // Diretoria [     ] Gerência [     ] Área [#####]
	        condicao = !hasValue(doc.getBusinessUnitRef())
	                && !hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
	                && hasValue(doc.getExtensionField(EXTF_AREA).get());
	        if (condicao) {
	            // Verifica se o campo "Diretotia" esta preenchido corretamente
	            buArea = ((BusinessUnitIBeanIfc) buHome.find((ObjectReferenceIfc) doc.getExtensionField(EXTF_AREA).get()));
	            vlv = (ValueListValueIBeanIfc) vlvHome
	                    .find((ObjectReferenceIfc) buArea.getFieldMetadata("ORG_UNIT_TYPE").get(buArea));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_AREA)) {
	                throw getAppException(EXTF_AREA, "mensagem.gap016.erro.area_preenchimento_incorreto");
	            }

	            // Preenchendo campo "Gerência" de acordo com "Área"
	            buGerencia = (BusinessUnitIBeanIfc) buHome.find(buArea.getParentObjRef());
	            doc.getExtensionField(EXTF_GERENCIA).set(buGerencia.getObjectReference());
	            // Preenchendo campo "Diretoria" de acordo com "Gerência"
	            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
	            doc.setBusinessUnitRef(buDiretoria.getObjectReference());
	            return;
	        }

	        // Diretoria [     ] Gerência [#####] Área [     ]
	        condicao = !hasValue(doc.getBusinessUnitRef())
	                && hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
	                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
	        if (condicao) {
	            // Verifica se o campo "Diretotia" esta preenchido corretamente
	            buGerencia = ((BusinessUnitIBeanIfc) buHome.find((ObjectReferenceIfc) doc.getExtensionField(EXTF_GERENCIA).get()));
	            vlv = (ValueListValueIBeanIfc) vlvHome.find(
	                    (ObjectReferenceIfc) buGerencia.getFieldMetadata("ORG_UNIT_TYPE").get(buGerencia));
	            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_GERENCIA)) {
	                throw getAppException(EXTF_GERENCIA, "mensagem.gap016.erro.gerencia_preenchimento_incorreto");
	            }

	            // Preenchendo campo "Diretoria" de acordo com "Gerência"            
	            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
	            doc.setBusinessUnitRef(buDiretoria.getObjectReference());
	        }
	    }

	    // Preenche a coleção "Coleta de Assinaturas - Outorgados"
	    private void gap015_preencheColetaAssOutorgadosComAprovSoc(boolean existeRespSimEmAprovSoc) throws Exception {

	        String sql;
	        String whereEmpresas;
	        int objIdDiretoria;
	        int objIdCurrency;
	        String whereDepDasRespAproSoc;
	        String tabDynMA;
	        String tabDynMA_Outorgados;
	        String tabDynUDO4;
	        ResultSet rs;
	        ExtensionCollectionIfc collecColetaAssOutorgados;
	        List listOutorgados = new ArrayList();
	        UserDefinedMasterData4IBeanIfc udo4;
	        UserDefinedMasterData5IBeanIfc udo5;

	        // Campos obrigatórios não preenchidos são necessarios para continuar com esse script
	        if (!hasValue(doc.getBusinessUnitRef()) || !hasValue(doc.getCurrency())) {
	            return;
	        }

	        // Limpando a collection
	        collecColetaAssOutorgados = doc.getExtensionCollection(EXTC_COLETA_ASS_OUTORGADOS);
	        while (collecColetaAssOutorgados.size() > 0) {
	            collecColetaAssOutorgados.delete(collecColetaAssOutorgados.get(0));
	        }

	        tabDynMA = getNomeTabelaDinamica(1004);
	        tabDynMA_Outorgados = getNomeTabelaDinamica(1004, EXTC_OUTORGADOS);
	        tabDynUDO4 = getNomeTabelaDinamica(9999704);

	        objIdDiretoria = doc.getBusinessUnitRef().getObjectId();
	        objIdCurrency = doc.getCurrency().getObjectId();

	        whereEmpresas = "DYN_MA.EMPRESA_1_OBJECT_ID = "
	                + ((ObjectReferenceIfc) doc.getExtensionField(EXTF_EMPRESA1).get()).getObjectId();
	        if (hasValue(doc.getExtensionField(EXTF_EMPRESA2).get())) {
	            whereEmpresas += " OR DYN_MA.EMPRESA_1_OBJECT_ID = "
	                    + ((ObjectReferenceIfc) doc.getExtensionField(EXTF_EMPRESA2).get()).getObjectId();
	        }
	        if (hasValue(doc.getExtensionField(EXTF_EMPRESA3).get())) {
	            whereEmpresas += " OR DYN_MA.EMPRESA_1_OBJECT_ID = "
	                    + ((ObjectReferenceIfc) doc.getExtensionField(EXTF_EMPRESA3).get()).getObjectId();
	        }

	        /* Quando houver uma resposta sim em Aprov. Soc. procura somente as procurações que esteja 
	         marcado "Assinar contratos após aprovação societária", senão, procurar as procurações que 
	         esteja marcado "Assinar Contratos" e que o valor seja maior ou igual a do Contrato em questão
	         */
	        if (existeRespSimEmAprovSoc) {
	            whereDepDasRespAproSoc = "DYN_MA.PROC_PODER_ASS_APR = 1";

	        } else {
	            whereDepDasRespAproSoc = "DYN_MA.PROC_PODER_ASS = 1 AND "
	                    + "DYN_MA.PROC_PODER_VALOR_PRICE >= " + doc.getLimitValue().getPrice() + " AND "
	                    + "DYN_MA.PROC_PODER_VALOR_OBJECT_ID = " + objIdCurrency;
	        }

	        /*
	         Buscando por todas as procurações que tenham marcado o campo "Assinar contratos mediante aprovação societária". 
	         Nas procurações localizadas, localizando os outorgados que pertençam a mesma empresa e diretoria do 
	         Acordo Básico Geral em questão.
	         */
	        sql = "SELECT "
	                + "DYN_MA_OUTORGADOS.PROC_OUTORGADOS_OBJECT_ID 	AS UDO4_OBJECTID, "
	                + "MA.UNIQUE_DOC_NAME           		AS MA_ID, "
	                + "DYN_MA.PROC_PODERES_OBJECT_ID             	AS UDO5_OBJECTID "
	                + "FROM "
	                + schema + ".FCI_CONTRACT                   MA, "
	                + schema + "." + tabDynMA + "               DYN_MA, "
	                + schema + "." + tabDynMA_Outorgados + "    DYN_MA_OUTORGADOS, "
	                + schema + ".FCI_CONTRACT_TYPE              MA_TYPE, "
	                + schema + ".FCI_UDEF_MASTERDATA4           UDO4, "
	                + schema + "." + tabDynUDO4 + "             DYN_UDO4 "
	                + "WHERE "
	                + "MA.OBJECTID = DYN_MA.PARENT_OBJECT_ID AND "
	                + "MA.OBJECTID = DYN_MA_OUTORGADOS.PARENT_OBJECT_ID AND "
	                + "MA.DOC_TYPE_OBJECT_ID = MA_TYPE.OBJECTID AND "
	                + "DYN_MA_OUTORGADOS.PROC_OUTORGADOS_OBJECT_ID = UDO4.OBJECTID AND "
	                + "UDO4.OBJECTID = DYN_UDO4.PARENT_OBJECT_ID AND "
	                + "MA.INACTIVE = 0 AND "
	                + "MA_TYPE.DISPLAY_NAME = 'Procuração' AND "
	                + "MA.EXPIRATION_DATE_DATE > SYSDATE AND "
	                + whereDepDasRespAproSoc + " AND "
	                + "(" + whereEmpresas + ") AND "
	                + "DYN_UDO4.REG_OUTORGADO_DIR_OBJECT_ID = " + objIdDiretoria + " "
	                + "GROUP BY "
	                + "DYN_MA_OUTORGADOS.PROC_OUTORGADOS_OBJECT_ID, "
	                + "MA.UNIQUE_DOC_NAME, "
	                + "DYN_MA.PROC_PODERES_OBJECT_ID ";

	        try {
	            session.getDbHandle().beginTransaction();
	            session.getDbHandle().executeQuery(sql);
	            rs = session.getDbHandle().getResultSet();
	            while (rs.next()) {
	                listOutorgados.add(new String[]{
	                    rs.getString("UDO4_OBJECTID"),
	                    rs.getString("MA_ID"),
	                    rs.getString("UDO5_OBJECTID")
	                });
	            }
	        } catch (Exception e) {
	            throw new ApplicationException(e.getMessage() + "\n" + sql);
	        } finally {
	            session.getDbHandle().endTransaction();
	        }

	        if (listOutorgados.isEmpty()) {
	            // Adicionando uma linha nova à collection
	            collecColetaAssOutorgados.add(collecColetaAssOutorgados.create());
	            // Nome
	            collecColetaAssOutorgados.get(0).getExtensionField("nome").set(
	                    getAppException(null, "mensagem.gap015.outorgadoNaoEncontrado").getMessage());
	            return;
	        }

	        // Loop preenchendo as linhas na collection
	        for (int i = 0; i < listOutorgados.size(); i++) {

	            int aClassId = 9999704;
	            int aObjectId = Integer.parseInt(((String[]) listOutorgados.get(i))[0]);
	            ObjectReference objRef = new ObjectReference(aClassId, aObjectId);
	            udo4 = (UserDefinedMasterData4IBeanIfc) udo4Home.find(objRef);
	            String numeracao = i + 1 + "";
	            String nome = (String) udo4.getExtensionField("reg_outorgado_nome").get();
	            String nABProcuracao = ((String[]) listOutorgados.get(i))[1];
	            String empresa = ((ObjectReference) udo4.getExtensionField("reg_outorgado_emp")
	                    .get()).getDisplayName((SessionContextIfc) session);
	            String diretoria = ((ObjectReference) udo4.getExtensionField("reg_outorgado_dir")
	                    .get()).getDisplayName((SessionContextIfc) session);
	            aClassId = 9999705;
	            aObjectId = Integer.parseInt(((String[]) listOutorgados.get(i))[2]);
	            objRef = new ObjectReference(aClassId, aObjectId);
	            udo5 = (UserDefinedMasterData5IBeanIfc) udo5Home.find(objRef);
	            Price price = (Price) udo5.getExtensionField("valor").get();
	            String alcada = PriceFormatter.formatForDisplayWithoutCurrencyName(
	                    (SessionContextIfc) session, price);

	            // Adicionando uma linha nova à collection
	            collecColetaAssOutorgados.add(collecColetaAssOutorgados.create());

	            // Numerador
	            collecColetaAssOutorgados.get(i).getExtensionField("numerador").set(numeracao);
	            // Nome
	            collecColetaAssOutorgados.get(i).getExtensionField("nome").set(nome);
	            // 	Nº AB Procuração
	            collecColetaAssOutorgados.get(i).getExtensionField("procuracao").set(nABProcuracao);
	            // Empresa
	            collecColetaAssOutorgados.get(i).getExtensionField("empresa").set(empresa);
	            // Diretoria
	            collecColetaAssOutorgados.get(i).getExtensionField("diretoria").set(diretoria);
	            // Alçada
	            collecColetaAssOutorgados.get(i).getExtensionField("alcada_outorgado").set(alcada + " " + price.getCurrency());
	        }
	    }

	    // Preenche a coleção "Coleta de Assinaturas - Representantes"
	    private void gap015_preencheColetaAssRepresentantes(boolean existeRespSimEmAprovSoc) throws Exception {

	        String sql;
	        String whereEmpresas;
	        String tabDynUDO4 = schema + "." + getNomeTabelaDinamica(9999704);
	        String tabDynUDO2 = schema + "." + getNomeTabelaDinamica(9999702);
	        int objIdDiretoria;
	        Price price;
	        int objIdCurrency;
	        List listaRepresentantes = new ArrayList();
	        ResultSet rs;
	        UserDefinedMasterData4IBeanIfc udo4;
	        UserDefinedMasterData2IBeanIfc udo2;
	        ObjectReference objRef;
	        int classIdUdo4 = 9999704;
	        ExtensionCollectionIfc collecColetaAssRepresentantes;
	        BigDecimal valor = new BigDecimal(0);
	        int udo2ObjId = 0;
	        String alcada;

	        collecColetaAssRepresentantes = doc.getExtensionCollection(EXTC_COLETA_ASS_REPRESENTANTES);
	        // Limpando a collection
	        while (collecColetaAssRepresentantes.size() > 0) {
	            collecColetaAssRepresentantes.delete(collecColetaAssRepresentantes.get(0));
	        }

	        whereEmpresas = "DYN_UDO4.REG_OUTORGADO_EMP_OBJECT_ID = "
	                + ((ObjectReferenceIfc) doc.getExtensionField(EXTF_EMPRESA1).get()).getObjectId();

	        if (hasValue(doc.getExtensionField(EXTF_EMPRESA2).get())) {
	            whereEmpresas += " OR DYN_UDO4.REG_OUTORGADO_EMP_OBJECT_ID = "
	                    + ((ObjectReferenceIfc) doc.getExtensionField(EXTF_EMPRESA2).get()).getObjectId();
	        }
	        if (hasValue(doc.getExtensionField(EXTF_EMPRESA3).get())) {
	            whereEmpresas += " OR DYN_UDO4.REG_OUTORGADO_EMP_OBJECT_ID = "
	                    + ((ObjectReferenceIfc) doc.getExtensionField(EXTF_EMPRESA3).get()).getObjectId();
	        }

	        // Campos obrigatórios não preenchidos são necessarios para continuar com esse script
	        if (!hasValue(doc.getBusinessUnitRef()) || !hasValue(doc.getLimitValue())) {
	            return;
	        }

	        objIdDiretoria = doc.getBusinessUnitRef().getObjectId();
	        price = (Price) doc.getLimitValue();
	        objIdCurrency = doc.getCurrency().getObjectId();

	        if (existeRespSimEmAprovSoc) {
	            sql = "SELECT "
	                    + "UDO4.OBJECTID AS UDO4_OBJECTID "
	                    + "FROM "
	                    + schema + ".FCI_UDEF_MASTERDATA4 UDO4, "
	                    + tabDynUDO4 + " DYN_UDO4 "
	                    + "WHERE "
	                    + "UDO4.OBJECTID = DYN_UDO4.PARENT_OBJECT_ID AND "
	                    + "UDO4.INACTIVE = 0 AND "
	                    + "DYN_UDO4.REG_OUTORGADO_CLS_OBJECT_NAME = 'representante' AND "
	                    + "((DYN_UDO4.REG_OUTORGADO_CRG_OBJECT_NAME = '" + VLV_DIR_ESTATUTARIO + "' AND "
	                    + "(" + whereEmpresas + ") AND "
	                    + "DYN_UDO4.REG_OUTORGADO_DIR_OBJECT_ID = " + objIdDiretoria + ") OR "
	                    + "(DYN_UDO4.REG_OUTORGADO_CRG_OBJECT_NAME = '" + VLV_DIR_PRESIDENTE + "' AND "
	                    + "(" + whereEmpresas + "))) "
	                    + "ORDER BY "
	                    + "DYN_UDO4.REG_OUTORGADO_CRG_OBJECT_NAME ";
	        } else {
	            sql = "SELECT "
	                    + "UDO4.OBJECTID AS UDO4_OBJECTID, "
	                    + "UDO2.OBJECTID AS UDO2_OBJECTID, "
	                    + "DYN_UDO2.TAB_ALCADA_VALOR_PRICE AS VALOR_ALCADA "
	                    + "FROM "
	                    + schema + ".FCI_UDEF_MASTERDATA4 UDO4, "
	                    + tabDynUDO4 + " DYN_UDO4, "
	                    + schema + ".FCI_UDEF_MASTERDATA2 UDO2, "
	                    + tabDynUDO2 + " DYN_UDO2 "
	                    + "WHERE "
	                    + "UDO2.OBJECTID = DYN_UDO2.PARENT_OBJECT_ID AND "
	                    + "UDO4.OBJECTID = DYN_UDO4.PARENT_OBJECT_ID AND "
	                    + "UDO2.INACTIVE = 0 AND "
	                    + "UDO4.INACTIVE = 0 AND "
	                    + "DYN_UDO4.REG_OUTORGADO_CLS_OBJECT_NAME = 'representante' AND "
	                    + "((DYN_UDO4.REG_OUTORGADO_CRG_OBJECT_NAME = '" + VLV_DIR_PRESIDENTE + "' AND "
	                    + "(" + whereEmpresas + ") AND "
	                    + "DYN_UDO2.TAB_ALCADA_DIRETOR_OBJECT_ID = " + objIdDiretoria + " AND "
	                    + "DYN_UDO2.TAB_ALCADA_VALOR_PRICE >= " + price.getPrice() + " AND "
	                    + "DYN_UDO2.TAB_ALCADA_VALOR_OBJECT_ID = " + objIdCurrency + ") OR "
	                    + "(DYN_UDO4.REG_OUTORGADO_CRG_OBJECT_NAME = '" + VLV_DIR_ESTATUTARIO + "' AND "
	                    + "DYN_UDO4.REG_OUTORGADO_DIR_OBJECT_ID = " + objIdDiretoria + " AND "
	                    + "DYN_UDO2.TAB_ALCADA_DIRETOR_OBJECT_ID = " + objIdDiretoria + " AND "
	                    + "DYN_UDO2.TAB_ALCADA_VALOR_PRICE >= " + price.getPrice() + " AND "
	                    + "DYN_UDO2.TAB_ALCADA_VALOR_OBJECT_ID = " + objIdCurrency + " AND "
	                    + "(" + whereEmpresas + "))) "
	                    + "ORDER BY "
	                    + "DYN_UDO4.REG_OUTORGADO_CRG_OBJECT_NAME ";
	        }

	        try {
	            session.getDbHandle().beginTransaction();
	            session.getDbHandle().executeQuery(sql);
	            rs = session.getDbHandle().getResultSet();
	            // Preenchendo a List com os representantes encontrados
	            while (rs.next()) {
	                if (!existeRespSimEmAprovSoc) {
	                    // Pegando o menor valor em "VALOR_ALCADA"
	                    if (valor.compareTo(new BigDecimal(0)) == 0) {
	                        valor = rs.getBigDecimal("VALOR_ALCADA");
	                    } else if (valor.compareTo(rs.getBigDecimal("VALOR_ALCADA")) > 0) {
	                        valor = rs.getBigDecimal("VALOR_ALCADA");
	                    }
	                    udo2ObjId = rs.getInt("UDO2_OBJECTID");
	                    if (!listaRepresentantes.contains(rs.getString("UDO4_OBJECTID"))) {
	                        listaRepresentantes.add(rs.getString("UDO4_OBJECTID"));
	                    }
	                } else {
	                    listaRepresentantes.add(rs.getString("UDO4_OBJECTID"));
	                }
	            }
	        } catch (Exception e) {
	            throw new ApplicationException(e.getMessage() + "\nQuery:\n" + sql);
	        } finally {
	            session.getDbHandle().endTransaction();
	        }

	        if (listaRepresentantes.isEmpty()) {
	            // Adicionando uma linha nova à collection
	            collecColetaAssRepresentantes.add(collecColetaAssRepresentantes.create());
	            // Nome
	            collecColetaAssRepresentantes.get(0).getExtensionField("nome_representante").set(
	                    getAppException(null, "mensagem.gap015.representanteNaoEncontrado").getMessage());
	            return;
	        }

	        for (int i = 0; i < listaRepresentantes.size(); i++) {

	            objRef = new ObjectReference(classIdUdo4, Integer.parseInt(listaRepresentantes.get(i).toString()));
	            udo4 = (UserDefinedMasterData4IBeanIfc) udo4Home.find(objRef);

	            // Adicionando uma linha nova à collection
	            collecColetaAssRepresentantes.add(collecColetaAssRepresentantes.create());

	            // Numerador
	            collecColetaAssRepresentantes.get(i).getExtensionField("numerador_representante").set(String.valueOf((i + 1)));
	            // Nome
	            String nome = udo4.getDisplayName();
	            collecColetaAssRepresentantes.get(i).getExtensionField("nome_representante").set(nome);
	            // 	Cargo            
	            String cargo = ((ObjectReference) udo4.getExtensionField("reg_outorgado_crg")
	                    .get()).getDisplayName((SessionContextIfc) session);
	            collecColetaAssRepresentantes.get(i).getExtensionField("cargo_representante").set(cargo);
	            // Empresa
	            objRef = (ObjectReference) udo4.getExtensionField("reg_outorgado_emp").get();
	            String empresa = objRef.getDisplayName((SessionContextIfc) session);
	            collecColetaAssRepresentantes.get(i).getExtensionField("empresa_representante").set(empresa);
	            // Diretoria
	            objRef = (ObjectReference) udo4.getExtensionField("reg_outorgado_dir").get();
	            String diretoria = objRef.getDisplayName((SessionContextIfc) session);
	            collecColetaAssRepresentantes.get(i).getExtensionField("diretoria_representante").set(diretoria);
	            // Alçada
	            if (!existeRespSimEmAprovSoc) {
	                objRef = new ObjectReference(9999702, udo2ObjId);
	                udo2 = (UserDefinedMasterData2IBeanIfc) udo2Home.find(objRef);
	                price = (Price) udo2.getExtensionField("tab_alcada_valor").get();
	                alcada = PriceFormatter.formatForDisplayWithoutCurrencyName((SessionContextIfc) session, price);
	                collecColetaAssRepresentantes.get(i).getExtensionField("alcadas_representante").set(alcada + " " + price.getCurrency());
	            }
	        }
	    }

	    // Preenche o campo CNPJ.CPF automaticamente
	    private void gap005_preencheCNPJ_CPF() throws Exception {

	        ponto = "gap005_preencheCNPJ_CPF() 000";

	        if (hasValue(((ObjectReference) doc.getFieldMetadata("VENDOR").get(doc)))) {

	            String idCampoZ_CNPJ_CPF = "STCD_1e2";

	            String cnpj = "";
	            String cpf = "";
	            String sql = "SELECT DYN_607.STCD1, DYN_607.STCD2 "
	                    + "FROM "
	                    + session.getDbHandle().getSchemaOwner() + "." + getNomeTabelaDinamica(607) + " DYN_607 "
	                    + " WHERE "
	                    + " DYN_607.PARENT_OBJECT_ID = " + ((ObjectReference) doc.getFieldMetadata("VENDOR").get(doc)).getObjectId();
	            session.getDbHandle().beginTransaction();
	            session.getDbHandle().executeQuery(sql);
	            ponto = "gap005_preencheCNPJ_CPF() 001";
	            ResultSet rs = session.getDbHandle().getResultSet();
	            ponto = "gap005_preencheCNPJ_CPF() 002";
	            rs.next();
	            ponto = "gap005_preencheCNPJ_CPF() 003";
	            cnpj = rs.getString("STCD1");
	            ponto = "gap005_preencheCNPJ_CPF() 004";
	            cpf = rs.getString("STCD2");
	            ponto = "gap005_preencheCNPJ_CPF() 005";
	            session.getDbHandle().endTransaction();
	            ponto = "gap005_preencheCNPJ_CPF() 006";
	            if (hasValue(cnpj)) {
	                doc.getExtensionField(idCampoZ_CNPJ_CPF).set(cnpj);
	            } else if (hasValue(cpf)) {
	                doc.getExtensionField(idCampoZ_CNPJ_CPF).set(cpf);
	            } else {
	                doc.getExtensionField(idCampoZ_CNPJ_CPF).set("");
	            }
	        }
	    }

	    // Preenche Collection de histórico de mudanças do questionário Aprovação Societária
	    private void gap053_historicoMudancasAprovSoc() throws Exception {

	        boolean criandoAB;

	        // Verificando se o AB não esta sendo criado no momento
	        session.getDbHandle().beginTransaction();
	        session.getDbHandle().executeQuery("SELECT * FROM " + schema + ".FCI_CONTRACT "
	                + "WHERE OBJECTID = " + doc.getObjectReference().getObjectId());
	        criandoAB = session.getDbHandle().getResultSet().next();
	        session.getDbHandle().endTransaction();
	        if (!criandoAB) {
	            return;
	        }

	        String sql = "";
	        ResultSet rs;
	        String prefixIdCampo = "resposta_";
	        String idCampoResp = "";
	        ObjectReference objRef = null;
	        ValueListValueIBeanIfc vlvResposta = null;
	        String respostaNoBanco;
	        String respostaEmSession;
	        ExtensionCollectionIfc historicoModificacao = doc.getExtensionCollection("historicoqas");

	        int i = 1;
	        while (i < 100) {
	            // Recuperando a resposta da pergunta em questão em Session(tela do usuário)
	            if (i < 10) {
	                idCampoResp = prefixIdCampo + "00" + i;
	            } else if (i < 100) {
	                idCampoResp = prefixIdCampo + "0" + i;
	            } else {
	                idCampoResp = prefixIdCampo + i;
	            }
	            try {
	                objRef = (ObjectReference) doc.getExtensionField(idCampoResp).get();
	                vlvResposta = (ValueListValueIBeanIfc) vlvHome.find(objRef);
	            } catch (Exception e1) {
	                idCampoResp = idCampoResp.substring(1, idCampoResp.length());
	                try {
	                    objRef = (ObjectReference) doc.getExtensionField(idCampoResp).get();
	                    vlvResposta = (ValueListValueIBeanIfc) vlvHome.find(objRef);
	                } catch (Exception e2) {
	                    idCampoResp = idCampoResp.substring(1, idCampoResp.length());
	                    try {
	                        objRef = (ObjectReference) doc.getExtensionField(idCampoResp).get();
	                        vlvResposta = (ValueListValueIBeanIfc) vlvHome.find(objRef);
	                    } catch (Exception e3) {
	                        // Caso não encontrar a resposta com o id de campo mensionado até aqui, significa que 
	                        // o número de perguntas nesse AB acabou, então sai do loop
	                        break;
	                    }
	                }                
	            }
	            if (!hasValue(objRef)) {
	                break;
	            }
	            
	            respostaEmSession = vlvResposta.getDisplayName().toUpperCase();

	            // Recuperando a resposta da pergunta em questão no banco de dados 
	            session.getDbHandle().beginTransaction();
	            sql = "SELECT " + idCampoResp + "_OBJECT_NAME" + " FROM " + schema + "." + tabDyn_MA + " "
	                    + "WHERE PARENT_OBJECT_ID = " + doc.getObjectReference().getObjectId();
	            session.getDbHandle().executeQuery(sql);
	            rs = session.getDbHandle().getResultSet();
	            rs.next();
	            respostaNoBanco = rs.getString(idCampoResp + "_OBJECT_NAME").toUpperCase();
	            session.getDbHandle().endTransaction();

	            /* Comparando as respostas, se diferente gravasse na tabela de Histórico
	             de Modificação do Questionário Aprovação Societária */
	            if (!respostaNoBanco.equals(respostaEmSession)) {
	                IBeanIfc linha = historicoModificacao.create();
	                linha.getExtensionField("his_qas_usuario").set(session.getAccount().getAccountObjectReference());
	                linha.getExtensionField("his_qas_data").set(new SysDatetime(Calendar.getInstance().getTime()));
	                linha.getExtensionField("his_qas_pergunta").set("Pergunta " + i);
	                if (respostaEmSession.equals("SIM")) {
	                    respostaEmSession = "Sim";
	                } else {
	                    respostaEmSession = "Não";
	                }
	                linha.getExtensionField("his_qas_resposta").set(respostaEmSession);
	                historicoModificacao.add(linha);
	                // Bloqueando os campos da Collection
	                IapiDocumentLockManager.lockField(session, linha, "his_qas_usuario");
	                IapiDocumentLockManager.lockField(session, linha, "his_qas_data");
	                IapiDocumentLockManager.lockField(session, linha, "his_qas_pergunta");
	                IapiDocumentLockManager.lockField(session, linha, "his_qas_resposta");
	            }
	            i++;
	        }
	    }

	    private void initComponents() throws Exception {

	        buHome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);
	        udo2Home = (UserDefinedMasterData2IBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, UserDefinedMasterData2IBeanHomeIfc.sHOME_NAME);
	        udo4Home = (UserDefinedMasterData4IBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, UserDefinedMasterData4IBeanHomeIfc.sHOME_NAME);
	        udo5Home = (UserDefinedMasterData5IBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, UserDefinedMasterData5IBeanHomeIfc.sHOME_NAME);
	        vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);
	        vendorHome = (VendorIBeanHomeIfc) IBeanHomeLocator
	                .lookup(session, VendorIBeanHomeIfc.sHOME_NAME);
	        schema = session.getDbHandle().getSchemaOwner();
	        tabDyn_MA = getNomeTabelaDinamica(1004);
	    }

	    public void inicio() throws Exception {
	        ponto = "inicio() - P00";
	        if (!session.getAccount().getUserName().equalsIgnoreCase("enterprise")
	                && !session.getAccount().getUserName().equals("WORKFLOWUSER")) {
	            boolean existeRespSimAprovSoc;
	            try {
	                initComponents();

	                // Métodos que serão executados em todos os tipos de Acordo Básico
	                gap012_preencheSociedadeParceira();
	                gap016_validaDirGerArea();
	                if (!doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Procuração")) {
	                    gap005_preencheCNPJ_CPF();
	                }
	                if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Básico Geral")) {
	                    if (!doc.isTemplate()) {
	                        gap014_aprovSoc();
	                        existeRespSimAprovSoc = aprovSoc_checaSeExisteRespSim();
	                        gap015_preencheColetaAssOutorgadosComAprovSoc(existeRespSimAprovSoc);
	                        gap015_preencheColetaAssRepresentantes(existeRespSimAprovSoc);                        
	                        gap053_historicoMudancasAprovSoc();
	                    }
	                }
	                
	                if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Comercial")) {
	                    if (!doc.isTemplate()) {
	                        gap014_aprovSoc();
	                        existeRespSimAprovSoc = aprovSoc_checaSeExisteRespSim();
	                        gap015_preencheColetaAssOutorgadosComAprovSoc(existeRespSimAprovSoc);
	                        gap015_preencheColetaAssRepresentantes(existeRespSimAprovSoc);                        
	                        gap053_historicoMudancasAprovSoc();
	                    }
	                }
	                
	               

	            } catch (Exception e) {
	                if (hasValue(doc.getDocumentId()) && !doc.getDocumentId().contains("<%")) {
	                    logError("[" + doc.getDocumentId() + "] " + e.getMessage());
	                }
	                throw new ApplicationException(e.getMessage());
	            }
	        }
	    }

	   // inicio();
}
