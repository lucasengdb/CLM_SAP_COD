package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;
import com.sap.eso.api.contracts.ContractIBeanIfc;
/* 
 MA - Validated - Any - Any
 Atende:
 CLM.003 (Validação de campos Z), 
 CLM.012 (Partes relacionadas), 
 CLM.016 (Ampliação de niveis organizacionais), 
 CLM.015 (Controle de Assinaturas),
 CLM.014 (Aprovação societaria)
 */
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Date;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiAccountLocator;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.common.types.PriceIfc;
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
import com.sap.odp.api.ibean.ExtensionCollectionIfc;
import com.sap.odp.api.ibean.ExtensionFieldIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanHomeIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanIfc;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.db.ObjectReference;
import com.sap.odp.common.types.BigText;
import com.sap.odp.usermgmt.masterdata.UserAccountBo;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

/**
 *
 * @author Tiago Rodrigues
 *
 */
public class MA_Validated_Any_Any_Backup extends CLMDummyScriptDefinition {

    ContractIBeanIfc doc;
    // Para portar este código em "Script Definition" copiar todo codigo a partir daqui
    // e remover o comentario da chamado do metodo "inicio"
    //
    //
    // Extension Collections
    private final String EXTF_GERENCIA = "MA_GERENCIA";
    private final String EXTF_AREA = "MA_AREA";
    // Outras Constantes
    private final String LOGGER_ID = "\n[MA VALIDATED ANY ANY] ";
    private final String TIPO_DIRETORIA = "Diretoria";
    private final String TIPO_GERENCIA = "Gerência";
    private final String TIPO_AREA = "Área";
    private final String REPRESENTS_ADVG_JURID_SOC = "Advg. Jurídico - Societário";
    // Variaveis
    ValueListValueIBeanIfc vlv;
    BusinessUnitIBeanIfc buDiretoria;
    BusinessUnitIBeanIfc buGerencia;
    BusinessUnitIBeanIfc buArea;

    private final String aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX = "pergunta_";
    private final String aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX = "resposta_";
    private final String aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX = "soc_parceira";
    private final String aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX = "orgao_social";
    private final int aprovSoc_MAX_EXTFIELDS_PERGUNTA = 20;
    private List aprovSoc_listPergSocParc;
    private Map aprovSoc_mapaDeNumPerguntaParaOrgaoAprovador;
    private ResultSet rs;

    private String pontoDebug = "";

    public ApplicationException getLocalizedApplicationException(String resourceId, Object[] modifiers) {

        ApplicationException aEx = new ApplicationException(session,
                "tim.defdata", resourceId);
        if (modifiers != null && modifiers.length > 0) {
            aEx.setMessageModifiers(modifiers);
        }
        return aEx;
    }

    public void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    public void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private String aprovSoc_leftPadIntAsString(int num, int pos, String padStr) {

        pontoDebug = "aprovSoc_leftPadIntAsString() - P00";
        String s = Integer.toString(num);
        while (s.length() < pos) {
            s = padStr + s;
        }
        return s;
    }

    private int aprovSoc_getPadLen(String field) {

        pontoDebug = "aprovSoc_getPadLen() - P00";
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

        pontoDebug = "aprovSoc_getPadString() - P08";
        return aprovSoc_leftPadIntAsString(n, aprovSoc_getPadLen(field), "0");
    }

    private ExtensionFieldIfc aprovSoc_getExtFieldNumbered(String extFieldName, int numCampoBase1)
            throws ApplicationException {

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

    public ValueListTypeIBeanIfc getValueListTypeObjIdByExtId(String externalId)
            throws ApplicationException, DatabaseException {

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
    private void gap012_preencheSociedadeParceira() throws ApplicationException, DatabaseException {

        pontoDebug = "base_preencheSociedadeParceira() - P00";
        VendorIBeanHomeIfc vendorHome = (VendorIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, VendorIBeanHomeIfc.sHOME_NAME);
        VendorIBeanIfc vendor = (VendorIBeanIfc) vendorHome.find(doc.getVendorRef());
        if (hasValue(vendor) && hasValue(vendor.getExtensionField("VBUND").get())) {
            String sociedadeParceira = vendor.getExtensionField("VBUND").get().toString();
            doc.getExtensionField("MA_VBUND").set(sociedadeParceira);
        } else {
            doc.getExtensionField("MA_VBUND").set("N/A");
        }
    }

    private boolean aprovSoc_isBlankExtFieldCorpoPergunta(ExtensionFieldIfc extFieldCorpoN)
            throws ApplicationException, DatabaseException {

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

    private boolean aprovSoc_existePerguntaNumero(int numPerguntaBase_1)
            throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_extFieldExistePerguntaNumero() - P00";

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

    private boolean aprovSoc_extFieldYesNo_naoPreenchido(ExtensionFieldIfc extFieldResposta)
            throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_extFieldYesNo_naoPreenchido() - P00";
        boolean retval = false;
        retval = extFieldResposta == null || extFieldResposta.get() == null || !hasValue(extFieldResposta.get());

        return retval;
    }

    private void aprovSoc_checarPreenchimentoObrigatorio()
            throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_chkPreencObr() - P00";
        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
            if (!aprovSoc_existePerguntaNumero(n + 1)) {
                break;
            }

            ExtensionFieldIfc extFieldRespostaN = aprovSoc_getExtFieldNumbered(
                    aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);

            if (aprovSoc_extFieldYesNo_naoPreenchido(extFieldRespostaN)) {
                throw getLocalizedApplicationException("mensagem.gap014.todas_perguntas_respondidas_obrigatoria", null);
            }
        }
    }

    private Map aprovSoc_buildMapaDeNumPerguntaParaOrgaoAprovador()
            throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_buildMapaDeNumPerguntaParaOrgaoAprovador() - P00";
        Map map = new HashMap();
        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA; n++) {
            if (!aprovSoc_existePerguntaNumero(n + 1)) {
                break;
            }
            ExtensionFieldIfc extFieldOrgaoAprovN = aprovSoc_getExtFieldNumbered(
                    aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);
            ObjectReferenceIfc objRefOrgaoAprovador = (ObjectReferenceIfc) extFieldOrgaoAprovN
                    .get();
            ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                    .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);
            ValueListValueIBeanIfc vlvOrgaoAprovador = (ValueListValueIBeanIfc) vlvHome
                    .find(objRefOrgaoAprovador);

            map.put("" + (n + 1), vlvOrgaoAprovador);
        }
        return map;
    }

    private ValueListValueIBeanIfc aprovSoc_getVlvOrgaoSocialAprovadorDeNumPergunta(int numPergunta) {

        pontoDebug = "aprovSoc_getVlvOrgaoSocialAprovadorDeNumPergunta() - P00";
        String key = "" + numPergunta;
        return (ValueListValueIBeanIfc) aprovSoc_mapaDeNumPerguntaParaOrgaoAprovador.get(key);
    }

    private String aprovSoc_getTextsFromVlvOrgaoAprovador(ValueListValueIBeanIfc vlvOrgaoAprovador) {

        pontoDebug = "aprovSoc_getTextsFromVlvOrgaoAprovador() - P00 - vlvOrgaoAprovador="
                + vlvOrgaoAprovador;
        String retval = "";
        retval = retval + vlvOrgaoAprovador.getDisplayName();

        return retval;
    }

    private void aprovSoc_listarAprovacoesNecessarias() throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_listarAprovacoesNecessarias() - P00";
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

    private String aprovSoc_getValorDoCampoNumSociedadeParceira() throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_getValorDoCampoNumSociedadeParceira() - P00";
        String nomeCampoNumSocParceira = "MA_VBUND";
        ExtensionFieldIfc extFieldNumSocParceira = doc
                .getExtensionField(nomeCampoNumSocParceira);
        String valorNumSocParceira = (String) extFieldNumSocParceira.get();

        return valorNumSocParceira;
    }

    private boolean aprovSoc_campoNumSociedadeParceiraPreenchido() throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_campoNumSociedadeParceiraPreenchido() - P00";
        String valorCampoNumSocParc = aprovSoc_getValorDoCampoNumSociedadeParceira();
        return hasValue(valorCampoNumSocParc) && !valorCampoNumSocParc.isEmpty()
                && !valorCampoNumSocParc.trim().equals("N/A");
    }

    private boolean isPerguntaVinculadaEmSociedadeParceira(int numPerguntaBase_1) {

        pontoDebug = "isPerguntaVinculadaEmSociedadeParceira() - P00";
        boolean retval = false;
        if (aprovSoc_listPergSocParc.contains("" + numPerguntaBase_1)) {
            retval = true;
        }
        return retval;
    }

    private void aprovSoc_buildListPergSocParc() throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_buildListPergSocParc() - P00";
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

    private void aprovSoc_questObrigSeNumSocParc() throws ApplicationException, DatabaseException {

        pontoDebug += "aprovSoc_questObrigSeNumSocParc() - P00";
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
            throw getLocalizedApplicationException("mensagem.gap014.fornecedor_soc_parceira", null);
        }
    }

    private void aprovSoc_addGrupoJuridSocietario() throws ApplicationException, DatabaseException {

        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);
        vlv = (ValueListValueIBeanIfc) vlvHome.findWhere("DISPLAY_NAME = '" + REPRESENTS_ADVG_JURID_SOC + "'")
                .iterator().next();

        pontoDebug = "aprovSoc_addGrupoJuridSocietario() - P00";
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

    private boolean aprovSoc_existeGrupoJuridSocietario() throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_removeGrupoJuridSocietario() - P00";
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
                    ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                            .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);
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

    private Boolean aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta(ExtensionFieldIfc extFieldResposta)
            throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_pegaSimNaoDoValueListDeExtFieldDeResposta() - P00";

        String debugDados = "extFieldResposta=" + extFieldResposta;

        ObjectReferenceIfc objRefDeResposta = (ObjectReferenceIfc) extFieldResposta
                .get();
        debugDados = debugDados + " objRefDeResposta=" + objRefDeResposta
                + " objRefDeResposta.getDisplayName()="
                + objRefDeResposta.getDisplayName();

        if (objRefDeResposta == null || !hasValue(objRefDeResposta)) {
            return null;
        }
        Integer objectIdDeResposta = objRefDeResposta.getObjectId();
        debugDados = debugDados + " objectIdDeResposta=" + objectIdDeResposta;

        Boolean retval = null;
        ValueListTypeIBeanIfc vlt;
        ValueListValueIBeanIfc vlvSim;
        ValueListValueIBeanIfc vlvNao;

        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        vlt = getValueListTypeObjIdByExtId("respostas");
        debugDados = debugDados + " vlt.getDisplayName=" + vlt.getDisplayName();

        vlvSim = vlvHome.findUniqueByNameType("sim", vlt.getTypeCode());
        debugDados = debugDados + " vlvSim.getDisplayName="
                + vlvSim.getDisplayName()
                + " vlvSim.getObjectReference().getObjectId="
                + vlvSim.getObjectReference().getObjectId();
        vlvNao = vlvHome.findUniqueByNameType("nao", vlt.getTypeCode());
        debugDados = debugDados + " vlvNao.getDisplayName="
                + vlvNao.getDisplayName()
                + " vlvNao.getObjectReference().getObjectId="
                + vlvNao.getObjectReference().getObjectId();

        if (objRefDeResposta.equals(vlvSim.getObjectReference())) {
            retval = true;
        } else if (objRefDeResposta.equals(vlvNao.getObjectReference())) {
            retval = false;
        } else {
            retval = null;
        }

        return retval;
    }

    private void aprovSoc_checarSeAdicinaAprovadorGrupoJuridicoSocietario()
            throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_checarSeAdicionaAprovadorGrupoJuridicoSocietario() - P00";
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

    private String gap009_getDescricaoDiretoriaFromObjectReference(
            ObjectReferenceIfc objRefUdu4_Diretoria_pointsTo_OrganizationalUnit)
            throws ApplicationException, DatabaseException {

        pontoDebug = "poderOutorg_getDescricaoDiretoriaFromObjectReference() - P00";

        BusinessUnitIBeanHomeIfc bizzUnithome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);
        BusinessUnitIBeanIfc bizzUnit = (BusinessUnitIBeanIfc) bizzUnithome
                .find(objRefUdu4_Diretoria_pointsTo_OrganizationalUnit);
        return bizzUnit.getDisplayName();
    }

    private void gap009_expandirDadosDeOutorgados() throws ApplicationException, DatabaseException {

        pontoDebug = "poderOutorg_expandirDadosDeOutorgados() - P00";
        ExtensionCollectionIfc extColDeOutorgados = doc.getExtensionCollection("outorgados");

        for (int n = 0; n < extColDeOutorgados.size(); n++) {

            // Preenchendo: "RG, CPF, Diretoria, Endereço, Estado Civil, Nacionalidade e Profissão"
            UserDefinedMasterData4IBeanHomeIfc udu4home = (UserDefinedMasterData4IBeanHomeIfc) IBeanHomeLocator
                    .lookup(session, UserDefinedMasterData4IBeanHomeIfc.sHOME_NAME);

            IBeanIfc linha = extColDeOutorgados.get(n);
            ExtensionFieldIfc extFieldDoNome_linkUDU4 = linha.getExtensionField("proc_outorgados");

            ObjectReferenceIfc objectDeNome_linkUdu4 = (ObjectReferenceIfc) extFieldDoNome_linkUDU4.get();
            UserDefinedMasterData4IBeanIfc udu4Ifc = (UserDefinedMasterData4IBeanIfc) udu4home
                    .find(objectDeNome_linkUdu4);

            // RG
            ExtensionFieldIfc extFieldDoRG = linha.getExtensionField("proc_outorgados_rg");
            extFieldDoRG.set(udu4Ifc.getExtensionField("reg_outorgado_rg").get());
            IapiDocumentLockManager.lockField(session, linha, "proc_outorgados_rg");

            // CPF
            ExtensionFieldIfc extFieldDoCPF = linha.getExtensionField("proc_outorgados_cpf");
            extFieldDoCPF.set(udu4Ifc.getExternalId());
            IapiDocumentLockManager.lockField(session, linha, "proc_outorgados_cpf");

            // Diretoria
            ExtensionFieldIfc extFieldDiretoria = linha.getExtensionField("proc_outorgados_dir");
            ObjectReferenceIfc objRefUdu4_Diretoria_pointsTo_OrganizationalUnit = (ObjectReferenceIfc) udu4Ifc
                    .getExtensionField("reg_outorgado_dir").get();
            String textoDiretoria = gap009_getDescricaoDiretoriaFromObjectReference(
                    objRefUdu4_Diretoria_pointsTo_OrganizationalUnit);
            extFieldDiretoria.set(textoDiretoria);
            IapiDocumentLockManager.lockField(session, linha, "proc_outorgados_dir");

            // Endereço
            ExtensionFieldIfc extFieldDoEndereco = linha.getExtensionField("proc_outorgados_end");
            extFieldDoEndereco.set(udu4Ifc.getExtensionField(
                    "reg_outorgado_endereco").get());
            IapiDocumentLockManager.lockField(session, linha, "proc_outorgados_end");

            // Estado Civil
            ExtensionFieldIfc extFieldDoEstadoCivil = linha.getExtensionField("proc_outorgados_ecv");
            extFieldDoEstadoCivil.set(udu4Ifc.getExtensionField(
                    "reg_outorgado_estadocivil").get());
            IapiDocumentLockManager.lockField(session, linha, "proc_outorgados_ecv");

            // Nacionalidade
            ExtensionFieldIfc extFieldNacionalidade = linha.getExtensionField("proc_outorgados_nac");
            extFieldNacionalidade.set(udu4Ifc.getExtensionField(
                    "reg_outorgado_nacionalidade").get());
            IapiDocumentLockManager.lockField(session, linha, "proc_outorgados_nac");

            // Profissão
            ExtensionFieldIfc extFieldProfissao = linha.getExtensionField("proc_outorgados_prf");
            extFieldProfissao.set(udu4Ifc.getExtensionField(
                    "reg_outorgado_profissao").get());
            IapiDocumentLockManager.lockField(session, linha, "proc_outorgados_prf");
        }
    }

    private void gap014_aprovSoc() throws Exception {

        pontoDebug = "aprovSoc_Main() - P00";
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

    /**
     * @param int classId
     * @return Retorna o nome da Tabela Dinâmica Default
     */
    private String getNomeTabelaDinamica(int classId)
            throws NoConnectionException, SQLException {

        String schema = session.getDbHandle().getSchemaOwner();
        String sql;
        String resultado = "";

        sql = "SELECT FCI_SYS_DYN_CLASSES.TABLE_NAME FROM "
                + schema + ".FCI_SYS_DYN_CLASSES, "
                + schema + ".FCI_SYS_DYN_MEMBERS "
                + "WHERE "
                + "FCI_SYS_DYN_CLASSES.OBJECTID = FCI_SYS_DYN_MEMBERS.PARENT_OBJECT_ID "
                + "AND FCI_SYS_DYN_CLASSES.ASSOC_CLASSID = " + classId + " "
                + "AND UPPER(FCI_SYS_DYN_CLASSES.COLLECTION_NAME) IS NULL "
                + "GROUP BY TABLE_NAME";

        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        if (rs.next()) {
            resultado = rs.getString("TABLE_NAME");
        }
        session.getDbHandle().endTransaction();

        return resultado;
    }

    /**
     * @param int classId
     * @param String collectionName
     * @return Retorna o nome da Tabela Dinâmica de uma determinada coleção de campos em uma determinda tabela standard do CLM
     */
    private String getNomeTabelaDinamica(int classId, String collectionName)
            throws NoConnectionException, SQLException {

        String schema = session.getDbHandle().getSchemaOwner();
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
        rs = session.getDbHandle().getResultSet();
        if (rs.next()) {
            resultado = rs.getString("TABLE_NAME");
        }
        session.getDbHandle().endTransaction();

        return resultado;
    }

    // Preenche a coleção "Coleta de Assinaturas"
    private void gap015_preencheColetaAssinaturas()
            throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        pontoDebug = "preencheColetaAssinaturas() P001";
        List outorgadosEncontrados = new ArrayList();
        List alcadasEncontradas = new ArrayList();
        List coletaAssinaturas = new ArrayList();
        String sql;
        ExtensionCollectionIfc collColetaAssinaturas;
        String schema = session.getDbHandle().getSchemaOwner();

        String TAB_PODERES_MA = getNomeTabelaDinamica(1004, "poderes");
        String TAB_OUTORGADOS_MA = getNomeTabelaDinamica(1004, "outorgados");
        String TAB_ALCADA_UDO2 = getNomeTabelaDinamica(9999702);

        if (TAB_PODERES_MA.equals("") || TAB_OUTORGADOS_MA.equals("") || TAB_ALCADA_UDO2.equals("")) {
            throw new ApplicationException("Não foi possivel recuparar o ID das tabelas dinamicas.");
        }

        // Limpando a coleção "Coleta de Assinaturas"
        collColetaAssinaturas = doc.getExtensionCollection("coleta_assinaturas");
        while (collColetaAssinaturas.size() > 0) {
            collColetaAssinaturas.delete(collColetaAssinaturas.get(0));
        }

        UserDefinedMasterData4IBeanHomeIfc udo4Home = (UserDefinedMasterData4IBeanHomeIfc) IBeanHomeLocator
                .lookup(session, UserDefinedMasterData4IBeanHomeIfc.sHOME_NAME);
        UserDefinedMasterData2IBeanHomeIfc udo2Home = (UserDefinedMasterData2IBeanHomeIfc) IBeanHomeLocator
                .lookup(session, UserDefinedMasterData2IBeanHomeIfc.sHOME_NAME);
        BusinessUnitIBeanHomeIfc businessHome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);

        // 1° Passo 
        // - Coleto dois dados necessarios: "Tipo de contrato" em "Header" e 
        // valor do contrato em "Agreement Maximum"        
        ObjectReferenceIfc poder = (ObjectReferenceIfc) doc.getExtensionField("tipo_contrato").get();
        PriceIfc agrMax = doc.getLimitValue();

        // 2° Passo
        // - Coleto todos os MA que estejam ativos e que na coleção "Poderes" tenha em sua lista 
        // o mesmo que foi configurado em "Tipo de contrato" em "Header" no Acordo corrente
        sql = "SELECT "
                + TAB_OUTORGADOS_MA + ".PROC_OUTORGADOS_OBJECT_NAME AS OUTORGADO_NOME, "
                + TAB_OUTORGADOS_MA + ".PROC_OUTORGADOS_OBJECT_ID AS OUTORGADO_OBJECTID "
                + "FROM "
                + schema + ".FCI_CONTRACT "
                + "LEFT OUTER JOIN "
                + schema + ".FCI_CONGEN_CONTRACT_DOC "
                + "ON FCI_CONTRACT.OBJECTID = FCI_CONGEN_CONTRACT_DOC.PARENT_OBJECT_ID "
                + "LEFT OUTER JOIN "
                + schema + ".FCI_DOC_CONTRACT_PHASE  "
                + "ON FCI_CONGEN_CONTRACT_DOC.OBJECTID = FCI_DOC_CONTRACT_PHASE.PARENT_OBJECT_ID "
                + "LEFT OUTER JOIN "
                + schema + "." + TAB_OUTORGADOS_MA + " "
                + "ON FCI_CONTRACT.OBJECTID = " + TAB_OUTORGADOS_MA + ".PARENT_OBJECT_ID "
                + "LEFT OUTER JOIN "
                + schema + "." + TAB_PODERES_MA + " "
                + "ON FCI_CONTRACT.OBJECTID  = " + TAB_PODERES_MA + ".PARENT_OBJECT_ID "
                + "WHERE "
                + "    FCI_CONTRACT.DOC_TYPE_OBJECT_NAME = 'Procuração'\n"
                + "AND (FCI_DOC_CONTRACT_PHASE.DISPLAY_NAME = 'Executed' OR FCI_DOC_CONTRACT_PHASE.DISPLAY_NAME = 'Executado') "
                + "AND SYSDATE <= FCI_CONTRACT.EXPIRATION_DATE_DATE "
                + "AND " + TAB_PODERES_MA + ".PROC_PODERES_OBJECT_ID = " + poder.getObjectId() + " "
                + "GROUP BY " + TAB_OUTORGADOS_MA + ".PROC_OUTORGADOS_OBJECT_NAME, "
                + TAB_OUTORGADOS_MA + ".PROC_OUTORGADOS_OBJECT_ID "
                + "ORDER BY " + TAB_OUTORGADOS_MA + ".PROC_OUTORGADOS_OBJECT_NAME";
        pontoDebug = sql;
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        while (rs.next()) {
            ObjectReferenceIfc objRef = new ObjectReference(9999704, rs.getInt("OUTORGADO_OBJECTID"));
            outorgadosEncontrados.add(objRef);
        }
        session.getDbHandle().endTransaction();

        // 3° Passo
        // Coleto todas as Alçadas (UDO2) que o valor seja maior ou igual ao valor encontrato no 
        // Acordo corrente e que tenha o mesmo poder em Acordo corrente.
        sql = "SELECT FCI_UDEF_MASTERDATA2.OBJECTID "
                + "FROM "
                + schema + ".FCI_UDEF_MASTERDATA2, "
                + schema + "." + TAB_ALCADA_UDO2 + " "
                + "WHERE "
                + "    FCI_UDEF_MASTERDATA2.OBJECTID = " + TAB_ALCADA_UDO2 + ".PARENT_OBJECT_ID "
                + "AND " + TAB_ALCADA_UDO2 + ".TAB_ALCADA_VALOR_PRICE >= " + agrMax.getPrice() + " "
                + "AND " + TAB_ALCADA_UDO2 + ".TAB_ALCADA_PODER_OBJECT_ID = " + poder.getObjectId() + " "
                + "AND FCI_UDEF_MASTERDATA2.INACTIVE = 0 "
                + "ORDER BY " + TAB_ALCADA_UDO2 + ".TAB_ALCADA_VALOR_PRICE";
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        while (rs.next()) {
            ObjectReferenceIfc objRef = new ObjectReference(9999702, rs.getInt("OBJECTID"));
            alcadasEncontradas.add(objRef);
        }
        session.getDbHandle().endTransaction();

        // DEBUG 
        //doc.setDocumentDescription(doc.getDocumentDescription() + "Alçadas encontradas: " + alcadasEncontradas.size() + "\n");
        // FIM DEBUG
        if (!outorgadosEncontrados.isEmpty() && !alcadasEncontradas.isEmpty()) {
            for (int j = 0; j < alcadasEncontradas.size(); j++) {
                UserDefinedMasterData2IBeanIfc udo2 = (UserDefinedMasterData2IBeanIfc) udo2Home
                        .find((ObjectReferenceIfc) alcadasEncontradas.get(j));
                BusinessUnitIBeanIfc diretoriaAlcada = (BusinessUnitIBeanIfc) businessHome.find(
                        (ObjectReferenceIfc) udo2.getExtensionField("tab_alcada_diretor").get());
                for (int i = 0; i < outorgadosEncontrados.size(); i++) {
                    UserDefinedMasterData4IBeanIfc udo4 = (UserDefinedMasterData4IBeanIfc) udo4Home
                            .find((ObjectReferenceIfc) outorgadosEncontrados.get(i));
                    BusinessUnitIBeanIfc diretoriaOutorgado = (BusinessUnitIBeanIfc) businessHome.find(
                            (ObjectReferenceIfc) udo4.getExtensionField("reg_outorgado_dir").get());
                    if (diretoriaOutorgado.getExternalId().equals(diretoriaAlcada.getExternalId())) {
                        coletaAssinaturas.add(new Object[]{
                            udo4.getObjectReference().getDisplayName(),
                            diretoriaAlcada.getDisplayName(),
                            ((PriceIfc) udo2.getExtensionField("tab_alcada_valor").get()).getPrice(),
                            ((PriceIfc) udo2.getExtensionField("tab_alcada_valor").get()).getCurrency()});
                    }
                }
            }
        }

        // DEBUG 
        //doc.setDocumentDescription(doc.getDocumentDescription() + "Assinaturas encontradas: " + coletaAssinaturas.size() + "\n");
        // FIM DEBUG
        if (!coletaAssinaturas.isEmpty()) {
            for (int i = 0; i < coletaAssinaturas.size(); i++) {
                // coletaAssinaturas - Nesta coleção temos: 
                // Display Name da UDO4, Display Name da diretoria da Alçada, Valor da Alçaca e Currency em Alçada 
                collColetaAssinaturas.add(collColetaAssinaturas.create());
                Object[] obj = (Object[]) coletaAssinaturas.get(i);
                collColetaAssinaturas.get(i).getExtensionField("nome").set(obj[0]);
                collColetaAssinaturas.get(i).getExtensionField("diretoria").set(obj[1].toString());
                UserAccountBo user = (UserAccountBo) IapiAccountLocator
                        .lookup(session, session.getAccount().getAccountObjectReference());
                String priceFormatado = new DecimalFormat("###,###,###.00").format(new BigDecimal(obj[2].toString()));
                collColetaAssinaturas.get(i).getExtensionField("alcada").set(priceFormatado + " " + obj[3]);
            }
        } else {
            collColetaAssinaturas.add(collColetaAssinaturas.create());
            collColetaAssinaturas.get(0).getExtensionField("nome").set(
                    getLocalizedApplicationException("mensagem.gap015.aprovadorNaoEncontrado", null).getMessage());
        }
    }

    // Valida o preenchimento correto em "Gerência" e "Área"
    private void gap016_validaGerencia_e_Area() throws ApplicationException, DatabaseException {

        pontoDebug = "gap016_validaGerencia_e_Area() - P00";
        BusinessUnitIBeanHomeIfc buHome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);
        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        boolean condicao = false;

        //logInfo("DEBUG - Validação de Area, Gerencia e Diretoria");
        // Diretoria [#####] Gerência [#####] Área [     ]
        condicao = hasValue(doc.getBusinessUnitRef())
                && hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
        if (condicao) {
            // Verifica se o campo "Gerência" esta preenchido corretamente
            buGerencia = ((BusinessUnitIBeanIfc) buHome
                    .find((ObjectReferenceIfc) doc.getExtensionField(
                                    EXTF_GERENCIA).get()));
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buGerencia.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buGerencia));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_GERENCIA)) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.gerencia_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Diretoria" esta preenchido corretamente
            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia
                    .getParentObjRef());
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buDiretoria.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buDiretoria));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.diretoria_preenchimento_incorreto", null);
            }
            if (!doc.getBusinessUnitRef().equals(
                    buDiretoria.getObjectReference())) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.diretoria_e_gerencia_nao_correspondem", null);
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
                    .find((ObjectReferenceIfc) buDiretoria.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buDiretoria));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.diretoria_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Área" esta preenchido corretamente
            buArea = ((BusinessUnitIBeanIfc) buHome
                    .find((ObjectReferenceIfc) doc.getExtensionField(EXTF_AREA)
                            .get()));
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buArea.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buArea));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_AREA)) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.area_preenchimento_incorreto", null);
            }
            // Checando se correspondem
            buGerencia = (BusinessUnitIBeanIfc) buHome.find(buArea.getParentObjRef());
            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
            if (!doc.getBusinessUnitRef().equals(buDiretoria.getObjectReference())) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.diretoria_e_area_nao_correspondem", null);
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
                    .find((ObjectReferenceIfc) buDiretoria.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buDiretoria));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.diretoria_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Gerência" esta preenchido corretamente
            buGerencia = ((BusinessUnitIBeanIfc) buHome
                    .find((ObjectReferenceIfc) doc.getExtensionField(EXTF_GERENCIA).get()));
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buGerencia.getFieldMetadata("ORG_UNIT_TYPE").get(buGerencia));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_GERENCIA)) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.gerencia_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Área" esta preenchido corretamente
            buArea = ((BusinessUnitIBeanIfc) buHome.find((ObjectReferenceIfc) doc.getExtensionField(EXTF_AREA).get()));
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buArea.getFieldMetadata("ORG_UNIT_TYPE").get(buArea));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_AREA)) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.area_preenchimento_incorreto", null);
            }
            // Checando se correspondem
            if (!buDiretoria.getObjectReference().equals(buGerencia.getParentObjRef())
                    || !buGerencia.getObjectReference().equals(buArea.getParentObjRef())) {
                throw getLocalizedApplicationException("mensagem.gap016.erro.diretoria_gerencia_area_nao_correspondem", null);
            }
            return;
        }

        // Diretoria [     ] Gerência [     ] Área [     ]
        condicao = !hasValue(doc.getBusinessUnitRef())
                && !hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
        if (condicao) {
            throw getLocalizedApplicationException("mensagem.gap016.erro.diretoria_gerencia_area_obrigatorio", null);
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
                throw getLocalizedApplicationException("mensagem.gap016.erro.area_preenchimento_incorreto", null);
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
                throw getLocalizedApplicationException("mensagem.gap016.erro.gerencia_preenchimento_incorreto", null);
            }

            // Preenchendo campo "Diretoria" de acordo com "Gerência"            
            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
            doc.setBusinessUnitRef(buDiretoria.getObjectReference());
        }
    }

    public void inicio() throws ChainedException {

        pontoDebug = "inicio() - P00";
        try {
            // Méthods que serão executados em toda a lista de tipos de MA
            gap012_preencheSociedadeParceira();
            gap016_validaGerencia_e_Area();
            // Filtros para correta execução de alguns métodos
            if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Basico de TESTES")) {
                gap014_aprovSoc();
                gap015_preencheColetaAssinaturas();
            }
            if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Básico Geral")) {
                gap014_aprovSoc();
                //gap015_preencheColetaAssinaturas();
            }
            // Que seja do tipo "Procuração"
            if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Procuração")) {
                //gap009_expandirDadosDeOutorgados();
            }
        } catch (Exception e) {
            String s = "Exception " + " type : " + e.getClass().getSimpleName()
                    + " in code point : [" + pontoDebug + "]"
                    + " message : " + e.getMessage();
            if (s.length() >= 0) {
                if (s.length() > 300) {
                    String x = s.substring(0, 300);
                    s = x;
                }
                //use "e" para REAL THING , ou "s" para DEBUG
                if (hasValue(doc.getDocumentId()) && !doc.getDocumentId().contains("<%")) {
                    throw new ApplicationException("[" + doc.getDocumentId() + "] " + e.getMessage());
                } else {
                    throw new ApplicationException(e.getMessage());
                }
            }
        }
    }
    //inicio();
}
