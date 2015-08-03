package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;
import com.sap.eso.api.contracts.AgreementIBeanIfc;
/*
 MA - Duplicated - Any - Any
 Resumo:
 - Cria os campos para a coleção "Delta Price Value"
 - Popula as questões na aba "Aprovação Societária"
 */
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.doc.IapiDocumentLockManager;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData3IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData3IBeanIfc;
import com.sap.odp.api.ibean.ExtensionCollectionIfc;
import com.sap.odp.api.ibean.ExtensionFieldIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.common.types.BigText;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 *
 * @author Tiago Rodrigues
 */
public class MA_Duplicated_Any_Any extends CLMDummyScriptDefinition {

    AgreementIBeanIfc doc;
    //
    //
    // Constantes
    private final String EXTC_DETLA_PRICE_VALUE = "delta_price_value";
    private final String EXTF_PERIODO = "period_value";
    private final String VLT_PERIODS_DELTA_PRICE = "periods_delta_price";
    private final String VLV_DIV_1_ANO = "period_1";
    private final String VLV_DIV_2_ANO = "period_2";
    private final String VLV_DIV_PROX_ANOS = "period_3";

    //private final String aprovSoc_USERDEF3MAST_CHAVE_PREFIX = "pergunta_";
    private final String aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO = "pergunta";
    private final String aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC = "sociedade_parceira";
    private final String aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR = "orgao_social";
    private final String aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX = "pergunta_";
    private final String aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX = "resposta_";
    private final String aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX = "soc_parceira";
    private final String aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX = "orgao_social";
    private final int aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC = 900; //TEM Q SER **MENOR** QUE 999 !
    private int aprovSoc_DOC_quantidadeDePerguntas;
    private List aprovSoc_udu4_questoesAprovacaoSocietaria;

    String pontoDebug = "P00";

    public ValueListTypeIBeanIfc getValueListTypeObjIdByExtId(String externalId)
            throws ApplicationException, DatabaseException {

        ValueListTypeIBeanHomeIfc vltHome = (ValueListTypeIBeanHomeIfc) IBeanHomeLocator.lookup(
                session, ValueListTypeIBeanHomeIfc.sHOME_NAME);
        List vltList = vltHome.findWhere("EXTERNAL_ID = '" + externalId + "'");
        if (vltList.isEmpty()) {
            throw new ApplicationException("ListType '" + externalId + "' não encontrado.");
        }
        ValueListTypeIBeanIfc vlt = (ValueListTypeIBeanIfc) vltList.iterator().next();

        return vlt;
    }

    public void gap004_createDeltaPriceValue() throws DatabaseException, ChainedException {

        ExtensionCollectionIfc collDeltaPriceValue;
        ValueListTypeIBeanIfc vlt;
        ValueListValueIBeanIfc vlv;

        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator.lookup(
                session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        collDeltaPriceValue = doc.getExtensionCollection(EXTC_DETLA_PRICE_VALUE);

        if (collDeltaPriceValue.size() == 3) {
            IapiDocumentLockManager.lockField(session, doc, EXTC_DETLA_PRICE_VALUE);
            IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(0), EXTF_PERIODO);
            IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(1), EXTF_PERIODO);
            IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(2), EXTF_PERIODO);
            return;
        }

        // Criando 3 linhas na Collection "Deslta Price Value"
        collDeltaPriceValue.add(collDeltaPriceValue.create());
        collDeltaPriceValue.add(collDeltaPriceValue.create());
        collDeltaPriceValue.add(collDeltaPriceValue.create());

        vlt = getValueListTypeObjIdByExtId(VLT_PERIODS_DELTA_PRICE);
        IapiDocumentLockManager.lockField(session, doc, EXTC_DETLA_PRICE_VALUE);

        // Setando o Value List na primeira linha e bloqueando a coluna "Periodo"      
        vlv = vlvHome.findUniqueByNameType(VLV_DIV_1_ANO, vlt.getTypeCode());
        collDeltaPriceValue.get(0).getExtensionField(EXTF_PERIODO).set(vlv.getLocalizedObjectReference());
        IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(0), EXTF_PERIODO);

        // Setando o Value List na segunda linha e bloqueando a coluna "Periodo"
        vlv = vlvHome.findUniqueByNameType(VLV_DIV_2_ANO, vlt.getTypeCode());
        collDeltaPriceValue.get(1).getExtensionField(EXTF_PERIODO).set(vlv.getLocalizedObjectReference());
        IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(1), EXTF_PERIODO);

        // Setando o Value List na terceira linha e bloqueando a coluna "Periodo"               
        vlv = vlvHome.findUniqueByNameType(VLV_DIV_PROX_ANOS, vlt.getTypeCode());
        collDeltaPriceValue.get(2).getExtensionField(EXTF_PERIODO).set(vlv.getLocalizedObjectReference());
        IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(2), EXTF_PERIODO);
    }

    private String aprovSoc_leftPadIntAsString(int num, int pos, String padStr) {
        pontoDebug = "leftPadIntAsString() - P00";
        String s = new Integer(num).toString();
        pontoDebug = "leftPadIntAsString() - P01";
        while (s.length() < pos) {
            pontoDebug = "leftPadIntAsString() - P02";
            s = padStr + s;
            pontoDebug = "leftPadIntAsString() - P03";
        }
        pontoDebug = "leftPadIntAsString() - P04";
        return s;
    }

    private int aprovSoc_getPadLen(String field) {
        pontoDebug = "aprovSoc_getPadLen() - P00";
        int retval = 0;
        pontoDebug = "aprovSoc_getPadLen() - P01";
        if (field.startsWith(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX)) {
            pontoDebug = "aprovSoc_getPadLen() - P02";
            retval = 3;
        } else if (field.startsWith(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX)) {
            pontoDebug = "aprovSoc_getPadLen() - P03";
            retval = 3;
        } else if (field.startsWith(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX)) {
            pontoDebug = "aprovSoc_getPadLen() - P04";
            retval = 0;
        } else if (field.startsWith(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX)) {
            pontoDebug = "aprovSoc_getPadLen() - P05";
            retval = 3;
        } else {
            pontoDebug = "aprovSoc_getPadLen() - P06";
            retval = 0;
        }
        pontoDebug = "aprovSoc_getPadLen() - P07";
        return retval;
    }

    private String aprovSoc_getPadString(String field, int n) {
        pontoDebug = "aprovSoc_getPadString() - P08";
        return aprovSoc_leftPadIntAsString(n, aprovSoc_getPadLen(field), "0");
    }

    private void aprovSoc_lockExtFieldNumero(String extFieldName, int numCampoBase1) throws ApplicationException {
        aprovSoc_lockUnlockExtFieldNumero(true, extFieldName, numCampoBase1);
    }

    private void aprovSoc_unlockExtFieldNumero(String extFieldName, int numCampoBase1) throws ApplicationException {
        aprovSoc_lockUnlockExtFieldNumero(false, extFieldName, numCampoBase1);
    }

    private void aprovSoc_lockUnlockExtFieldNumero(boolean mustLock, String extFieldName, int numCampoBase1)
            throws ApplicationException {

        String extFieldNameComZeros = extFieldName + aprovSoc_getPadString(extFieldName, numCampoBase1);
        String extFieldNameSemZeros = extFieldName + numCampoBase1;

        boolean okOper = true;
        try {
            if (mustLock) {
                IapiDocumentLockManager.lockField(session, doc, extFieldNameComZeros);
            } else {
                IapiDocumentLockManager.unlockField(session, doc, extFieldNameComZeros);
            }
        } catch (Exception e) {
            okOper = false;
        }
        if (!okOper) {
            okOper = true;
            try {
                if (mustLock) {
                    IapiDocumentLockManager.lockField(session, doc, extFieldNameSemZeros);
                } else {
                    IapiDocumentLockManager.unlockField(session, doc, extFieldNameSemZeros);
                }
            } catch (Exception e) {
                okOper = false;
            }
        }

        if (!okOper) {
            throw new ApplicationException("Master Agrteement/Sub Agreement de numero "
                    + doc.getDocumentId() + " - impossivel fazer lock de extensio field com nome "
                    + extFieldNameComZeros + " nem com nome " + extFieldNameSemZeros);
        }
        return;
    }

    private ExtensionFieldIfc aprovSoc_getExtFieldNumbered(String extFieldName, int numCampoBase1)
            throws ApplicationException {
        if (numCampoBase1 == 0) {
            throw new ApplicationException("aprovSoc_getExtensionFieldNumber() chamada com numCampoBase1 ZERO!");
        }

        String extFieldNameComZeros = extFieldName + aprovSoc_getPadString(extFieldName, numCampoBase1);
        String extFieldNameSemZeros = extFieldName + numCampoBase1;
        ExtensionFieldIfc fld;

        fld = null;
        try {
            fld = doc.getExtensionField(extFieldNameComZeros);
        } catch (Exception e) {
            //nada
        }
        if (fld == null) {
            try {
                fld = doc.getExtensionField(extFieldNameSemZeros);
            } catch (Exception e) {
                //nada
            }
        }

        if (fld == null) {
            throw new ApplicationException("Master Agreement de numero "
                    + doc.getDocumentId() + " - Nao foi achado extension field com nome "
                    + extFieldNameComZeros + " nem com nome " + extFieldNameSemZeros);
        }
        return fld;
    }

    /**
     * ATENCAO - todo o codigo independe de haver sequencia exata. Pode haver pergunta_012 , pergunta_18 e pergunta_23, e mesmo assim serão
     * populados os 3 primeiros EXT_FIELDs do Contract/Agreement
     *
     * @return List < Map < String, Object > > - onde List=as questoes/X socparc, Map de Nome/Valor
     * @throws DatabaseException
     * @throws ApplicationException
     */
    private void aprovSoc_popularListaDeQuestoesComMasterData() throws ApplicationException, DatabaseException {
        pontoDebug = "aprovSoc_popularLista.. - P00";

        UserDefinedMasterData3IBeanHomeIfc udmd3Home = (UserDefinedMasterData3IBeanHomeIfc) IBeanHomeLocator.lookup(
                session, UserDefinedMasterData3IBeanHomeIfc.sHOME_NAME);
        pontoDebug = "aprovSoc_popularLista.. - P01";
        String sufixoWhere = "INACTIVE=0 and EXTERNAL_ID LIKE 'pergunta_%'"; //DAH PAU DE SQL SE USARMOS ORDER BY AQUI! 
        pontoDebug = "aprovSoc_popularLista.. - P01.05 - sufixoWhere = [" + sufixoWhere + "]";
        List foundStuff = udmd3Home.findWhere(
                sufixoWhere // faz com que venha do menor numero de pergunta para o maior
                );
        pontoDebug = "aprovSoc_popularLista.. - P02 ";
        //findWhere retorna List<iBEanIfc>, que sendo que IBeanIfc tem 
        //UserDefinedMasterData3IBeanIfc
        pontoDebug = "aprovSoc_popularLista.. - P03";

        SortedMap sortedMap = new TreeMap();

        pontoDebug = "aprovSoc_popularLista.. - P04";
        aprovSoc_udu4_questoesAprovacaoSocietaria = new ArrayList();
        pontoDebug = "aprovSoc_popularLista.. - P04.10";
        for (Object o : foundStuff) {
            pontoDebug = "aprovSoc_popularLista.. - P05";
            IBeanIfc ifc = (IBeanIfc) o;
            pontoDebug = "aprovSoc_popularLista.. - P06";
            UserDefinedMasterData3IBeanIfc udmd3 = (UserDefinedMasterData3IBeanIfc) ifc;
            pontoDebug = "aprovSoc_popularLista.. - P07";
            BigText corpoPergunta = (BigText) udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO).get();
            pontoDebug = "aprovSoc_popularLista.. - P08";
            Boolean sociedadeParc = (Boolean) udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC).get();
            pontoDebug = "aprovSoc_popularLista.. - P09";

            Object objOrgaoAprovadorObjRef = udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR).get();
            pontoDebug = "aprovSoc_popularLista.. - P09.01";

            Map m = new HashMap();
            pontoDebug = "aprovSoc_popularLista.. - P10";
            m.put(aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO, corpoPergunta);
            pontoDebug = "aprovSoc_popularLista.. - P11";
            m.put(aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC, sociedadeParc);
            pontoDebug = "aprovSoc_popularLista.. - P12";
            m.put(aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR, objOrgaoAprovadorObjRef);
            pontoDebug = "aprovSoc_popularLista.. - P12.10";

            sortedMap.put(udmd3.getExternalId(), m);
            pontoDebug = "aprovSoc_popularLista.. - P13";
        }
        pontoDebug = "aprovSoc_popularLista.. - P14";
        for (Object key : sortedMap.keySet()) {
            pontoDebug = "aprovSoc_popularLista.. - P16";
            String strKey = (String) key;
            pontoDebug = "aprovSoc_popularLista.. - P17";
            Map m = (Map) sortedMap.get(strKey);
            pontoDebug = "aprovSoc_popularLista.. - P18";
            aprovSoc_udu4_questoesAprovacaoSocietaria.add(m);
            pontoDebug = "aprovSoc_popularLista.. - P19";
        }

        pontoDebug = "aprovSoc_popularLista.. - P20";
        if (aprovSoc_udu4_questoesAprovacaoSocietaria.size() >= aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC) {
            throw new ApplicationException("Master Data 4 - erro de configuracao - limite de "
                    + aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC + " excedido.");
        }
        pontoDebug = "aprovSoc_popularLista.. - P21";

        return;
    }

    private void aprovSoc_atribuirExtensioFieldsDeContrato() throws ApplicationException, DatabaseException {
        pontoDebug = "aprovSoc_atribuirExt.. - P00";

        for (int n = 0;
                n < (int) Math.min(aprovSoc_DOC_quantidadeDePerguntas, aprovSoc_udu4_questoesAprovacaoSocietaria.size());
                n++) {

            pontoDebug = "aprovSoc_atribuirExt.. - P00.10";
            Map m = (Map) aprovSoc_udu4_questoesAprovacaoSocietaria.get(n);

            pontoDebug = "aprovSoc_atribuirExt.. - P05";
            ExtensionFieldIfc extFieldCorpoN = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
            pontoDebug = "aprovSoc_atribuirExt.. - P06";
            ExtensionFieldIfc extFieldSociedadeParceira = aprovSoc_getExtFieldNumbered(
                    aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, n + 1);
            pontoDebug = "aprovSoc_atribuirExt.. - P07";
            ExtensionFieldIfc extFieldOrgaoAprovador = aprovSoc_getExtFieldNumbered(
                    aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);
            pontoDebug = "aprovSoc_atribuirExt.. - P07.10";

            pontoDebug = "aprovSoc_atribuirExt.. - P09 ";
            BigText corpoPerguntaDaUDU3 = (BigText) m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO);
            pontoDebug = "aprovSoc_atribuirExt.. - P10 ";
            BigText corpoQuestaoDoContratoComoBigText = (BigText) extFieldCorpoN.get();
            pontoDebug = "aprovSoc_atribuirExt.. - P11";
            corpoQuestaoDoContratoComoBigText.setText(corpoPerguntaDaUDU3.getText(session));
            pontoDebug = "aprovSoc_atribuirExt.. - P13";
            Boolean sociedadeParceiraDaUDU3 = (Boolean) m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC);
            pontoDebug = "aprovSoc_atribuirExt.. - P13.5";
            Object objOrgaoAprovadorUDU3 = m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR);
            pontoDebug = "aprovSoc_atribuirExt.. - P14";

            extFieldSociedadeParceira.set(sociedadeParceiraDaUDU3);
            pontoDebug = "aprovSoc_atribuirExt.. - P14.10";
            extFieldOrgaoAprovador.set(objOrgaoAprovadorUDU3);
            pontoDebug = "aprovSoc_atribuirExt.. - P14.20";
        }
        pontoDebug = "aprovSoc_atribuirExt.. - P18";
    }

    private void aprovSoc_setQuantidadeDePerguntas() throws ApplicationException {
        pontoDebug = "aprovSoc_setQuantidadeDeCampos.. - P02";

        aprovSoc_DOC_quantidadeDePerguntas = 0;
        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC; n++) {
            try {
                pontoDebug = "aprovSoc_setQuantidadeDeCampos.. - P05";
                aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
                pontoDebug = "aprovSoc_setQuantidadeDeCampos.. - P06";
            } catch (Exception e) {
                pontoDebug = "aprovSoc_setQuantidadeDeCampos.. - P07";
                break;
                //exception em obter extension field indica fim de lista de perguntas
				/*
                 * ATENCAO - ATENCAo - se DOC(MA/SubAgr) tiver mais perguntas que UDU4 as perguntas
                 * adicionais nao serao populadas!!!
                 */
            }
            pontoDebug = "aprovSoc_setQuantidadeDeCampos.. - P08";
            aprovSoc_DOC_quantidadeDePerguntas++;
        }
        pontoDebug = "aprovSoc_setQuantidadeDeCampos.. - P09 - aprovSoc_DOC_quantidadeDeCampos="
                + aprovSoc_DOC_quantidadeDePerguntas;

        return;
    }

    private void aprovSoc_limparValoresCampos() throws ApplicationException, DatabaseException {
        pontoDebug = "aprovSoc_limparValoresCampos.. - P00";
        ExtensionCollectionIfc extCol = doc.getExtensionCollection("orgaos_sociais");
        pontoDebug = "aprovSoc_limparValoresCampos.. - P01";
        while (extCol.size() > 0) {
            pontoDebug = "aprovSoc_limparValoresCampos.. - P02";
            extCol.delete(extCol.get(0));
            pontoDebug = "aprovSoc_limparValoresCampos.. - P03";
        }

        pontoDebug = "aprovSoc_limparValoresCampos.. - P04";
        for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
            pontoDebug = "aprovSoc_limparValoresCampos.. - P05";
            aprovSoc_limparCamposDeUmNumPergunta(n + 1);
            pontoDebug = "aprovSoc_limparValoresCampos.. - P06";
        }
        pontoDebug = "aprovSoc_limparValoresCampos.. - P07";
    }

    private void aprovSoc_limparCamposDeUmNumPergunta(int numPerguntaBase1) throws ApplicationException, DatabaseException {
        pontoDebug = "aprovSoc_limparCamposDeUmNumPergunta.. - P00" + " n = " + numPerguntaBase1;

        ExtensionFieldIfc extResp = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, numPerguntaBase1);
        ExtensionFieldIfc extSoc = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, numPerguntaBase1);
        ExtensionFieldIfc extOrgao = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, numPerguntaBase1);
        ExtensionFieldIfc extCorpo = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, numPerguntaBase1);

        extResp.set(null);
        pontoDebug = "aprovSoc_desabilitarCampos.. - P12" + " n = " + numPerguntaBase1;
        extSoc.set(null);
        pontoDebug = "aprovSoc_desabilitarCampos.. - P12.50" + " n = " + numPerguntaBase1;
        extOrgao.set(null);
        pontoDebug = "aprovSoc_desabilitarCampos.. - P13" + " n = " + numPerguntaBase1;
        extSoc.set(null);
        pontoDebug = "aprovSoc_desabilitarCampos.. - P14" + " n = " + numPerguntaBase1;
        extCorpo.set(null);
    }

    private void aprovSoc_habilitarCamposResposta() throws ApplicationException, DatabaseException {
        pontoDebug = "aprovSoc_habilitarCamposResposta.. - P00";

        for (int n = 0;
                n < (int) Math.min(aprovSoc_DOC_quantidadeDePerguntas, aprovSoc_udu4_questoesAprovacaoSocietaria.size());
                n++) {
            pontoDebug = "aprovSoc_habilitarCamposResposta.. - P02";
            aprovSoc_unlockExtFieldNumero(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);
        }
        pontoDebug = "aprovSoc_habilitarCamposResposta.. - P04";
    }

    private void aprovSoc_desabilitarTodosCampos() throws ApplicationException, DatabaseException {
        pontoDebug = "aprovSoc_desabilitarCampos.. - P00";
        IapiDocumentLockManager.lockField(session, doc, "orgaos_sociais");
        pontoDebug = "aprovSoc_desabilitarCampos.. - P01";

        for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, n + 1);
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);
        }
    }

    private void aprovSoc_avisaQueTemplateNaoSerahCopiado()
            throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P00";
        String msgBase = "     **** Questionario de Aprovacao Societaria nao vai ser duplicado em criacao a partir de Template ****     ";
        pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P01";
        String msgNaoCopiado = "";
        final int tamMinimo = 1;
        pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P02";
        while (msgNaoCopiado.length() < tamMinimo) {
            pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P03";
            msgNaoCopiado += msgBase;
            pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P04";
        }
        pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P05";
        for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
            pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P06";
            ExtensionFieldIfc extfCorpo = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
            pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P07";
            BigText bigCorpo = (BigText) extfCorpo.get();
            pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P08";
            bigCorpo.setText(msgNaoCopiado);
            pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P09";
        }
        pontoDebug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P10";
    }

    public void gap014_aprovSoc() throws ApplicationException, DatabaseException {

        pontoDebug = "aprovSoc_principal P00";
        aprovSoc_popularListaDeQuestoesComMasterData(); //tem que ser o primeirissimo metodo chamado!!!
        pontoDebug = "aprovSoc_principal P00.30";
        aprovSoc_setQuantidadeDePerguntas(); //tem que ser chamado LOGO depois de popularLista!!

        pontoDebug = "aprovSoc_principal P01";

        aprovSoc_limparValoresCampos();
        pontoDebug = "aprovSoc_principal P02";
        aprovSoc_desabilitarTodosCampos();
        pontoDebug = "aprovSoc_principal P03";

        if (doc.isTemplate()) {
            aprovSoc_avisaQueTemplateNaoSerahCopiado();
            return;
        }
        pontoDebug = "aprovSoc_principal P04";
        aprovSoc_habilitarCamposResposta();

        pontoDebug = "aprovSoc_principal P05";
        aprovSoc_atribuirExtensioFieldsDeContrato();
        pontoDebug = "aprovSoc_principal P06";
    }

    void inicio() throws DatabaseException, ChainedException {

        try {
            gap004_createDeltaPriceValue();
            if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Basico de TESTES")) {
                gap014_aprovSoc();
            }
            if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Básico Geral")) {
                gap014_aprovSoc();
            }
        } catch (Exception e) {
            String s = "Exception "
                    + " type : " + e.getClass().getSimpleName()
                    + " in code point : [" + pontoDebug + "]"
                    + " message : " + e.getMessage();
            if (s.length() > 300) {
                String x = s.substring(0, 300);
                s = x;
            }
            throw new ApplicationException(e);  //use "e" para REAL THING , ou "s" para DEBUG
        }
    }
    //inicio();
}
