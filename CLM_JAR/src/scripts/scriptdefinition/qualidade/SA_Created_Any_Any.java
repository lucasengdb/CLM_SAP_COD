package scripts.scriptdefinition.qualidade;

/* SA - Created - Any - Any 
Resumo:
- Cria os campos para a coleção "Delta Price Value"
- Popula as questões na aba "Aprovação Societária"
*/
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
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

import cmldummy.CLMDummyScriptDefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;


public class SA_Created_Any_Any extends CLMDummyScriptDefinition{

	 ContractIBeanIfc doc;
	 
	 // Constantes
    private final String EXTC_DETLA_PRICE_VALUE = "delta_price_value";
    private final String EXTF_PERIODO = "period_value";
    private final String VLT_PERIODS_DELTA_PRICE = "periods_delta_price";
    private final String VLV_DIV_1_ANO = "period_1";
    private final String VLV_DIV_2_ANO = "period_2";
    private final String VLV_DIV_PROX_ANOS = "period_3";
    
    private final String LOGGER_ID = "\n[SA CREATED ANY ANY] ";
    
    private String ACORDO_COMERCIAL = "Acordo Comercial";

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

    String aprovSoc_pontoPassei = "P00";

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

    private String aprovSoc_leftPadIntAsString(int num, int pos, String padStr) {
        aprovSoc_pontoPassei = "leftPadIntAsString() - P00";
        String s = new Integer(num).toString();
        aprovSoc_pontoPassei = "leftPadIntAsString() - P01";
        while (s.length() < pos) {
            aprovSoc_pontoPassei = "leftPadIntAsString() - P02";
            s = padStr + s;
            aprovSoc_pontoPassei = "leftPadIntAsString() - P03";
        }
        aprovSoc_pontoPassei = "leftPadIntAsString() - P04";
        return s;
    }

    private int aprovSoc_getPadLen(String field) {
        aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P00";
        int retval = 0;
        aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P01";
        if (field.startsWith(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX)) {
            aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P02";
            retval = 3;
        } else if (field.startsWith(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX)) {
            aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P03";
            retval = 3;
        } else if (field.startsWith(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX)) {
            aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P04";
            retval = 0;
        } else if (field.startsWith(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX)) {
            aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P05";
            retval = 3;
        } else {
            aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P06";
            retval = 0;
        }
        aprovSoc_pontoPassei = "aprovSoc_getPadLen() - P07";
        return retval;
    }

    private String aprovSoc_getPadString(String field, int n) {
        aprovSoc_pontoPassei = "aprovSoc_getPadString() - P08";
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

    private ExtensionFieldIfc aprovSoc_getExtensionFieldNumber(String extFieldName, int numCampoBase1)
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
            throw new ApplicationException("Master Agrteement/Sub Agreement de numero "
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
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P00";

        UserDefinedMasterData3IBeanHomeIfc udmd3Home = (UserDefinedMasterData3IBeanHomeIfc) IBeanHomeLocator.lookup(
                session, UserDefinedMasterData3IBeanHomeIfc.sHOME_NAME);
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P01";
        String sufixoWhere = "INACTIVE=0 and EXTERNAL_ID LIKE 'pergunta_%'"; //DAH PAU DE SQL SE USARMOS ORDER BY AQUI! 
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P01.05 - sufixoWhere = [" + sufixoWhere + "]";
        List foundStuff = udmd3Home.findWhere(
                sufixoWhere // faz com que venha do menor numero de pergunta para o maior
                );
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P02 ";
        //findWhere retorna List<iBEanIfc>, que sendo que IBeanIfc tem 
        //UserDefinedMasterData3IBeanIfc
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P03";

        SortedMap sortedMap = new TreeMap();

        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P04";
        aprovSoc_udu4_questoesAprovacaoSocietaria = new ArrayList();
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P04.10";
        for (Object o : foundStuff) {
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P05";
            IBeanIfc ifc = (IBeanIfc) o;
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P06";
            UserDefinedMasterData3IBeanIfc udmd3 = (UserDefinedMasterData3IBeanIfc) ifc;
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P07";
            BigText corpoPergunta = (BigText) udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO).get();
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P08";
            Boolean sociedadeParc = (Boolean) udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC).get();
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P09";

            Object objOrgaoAprovadorObjRef = udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR).get();
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P09.01";

            Map m = new HashMap();
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P10";
            m.put(aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO, corpoPergunta);
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P11";
            m.put(aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC, sociedadeParc);
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P12";
            m.put(aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR, objOrgaoAprovadorObjRef);
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P12.10";

            sortedMap.put(udmd3.getExternalId(), m);
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P13";
        }
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P14";
        for (Object key : sortedMap.keySet()) {
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P16";
            String strKey = (String) key;
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P17";
            Map m = (Map) sortedMap.get(strKey);
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P18";
            aprovSoc_udu4_questoesAprovacaoSocietaria.add(m);
            aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P19";
        }

        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P20";
        if (aprovSoc_udu4_questoesAprovacaoSocietaria.size() >= aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC) {
            throw new ApplicationException("Master Data 4 - erro de configuracao - limite de "
                    + aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC + " excedido.");
        }
        aprovSoc_pontoPassei = "aprovSoc_popularLista.. - P21";

        return;
    }

    private void aprovSoc_atribuirExtensioFieldsDeContrato() throws ApplicationException, DatabaseException {
        aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P00";

        for (int n = 0;
                n < (int) Math.min(aprovSoc_DOC_quantidadeDePerguntas, aprovSoc_udu4_questoesAprovacaoSocietaria.size());
                n++) {

            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P00.10";
            Map m = (Map) aprovSoc_udu4_questoesAprovacaoSocietaria.get(n);

            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P05";
            ExtensionFieldIfc extFieldCorpoN = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P06";
            ExtensionFieldIfc extFieldSociedadeParceira = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, n + 1);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P07";
            ExtensionFieldIfc extFieldOrgaoAprovador = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P07.10";

            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P09 ";
            BigText corpoPerguntaDaUDU3 = (BigText) m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P10 ";
            BigText corpoQuestaoDoContratoComoBigText = (BigText) extFieldCorpoN.get();
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P11";
            corpoQuestaoDoContratoComoBigText.setText(corpoPerguntaDaUDU3.getText(session));
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P13";
            Boolean sociedadeParceiraDaUDU3 = (Boolean) m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P13.5";
            Object objOrgaoAprovadorUDU3 = m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P14";

            extFieldSociedadeParceira.set(sociedadeParceiraDaUDU3);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P14.10";
            extFieldOrgaoAprovador.set(objOrgaoAprovadorUDU3);
            aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P14.20";
        }
        aprovSoc_pontoPassei = "aprovSoc_atribuirExt.. - P18";
    }

    private void aprovSoc_setQuantidadeDePerguntas() throws ApplicationException {
        aprovSoc_pontoPassei = "aprovSoc_setQuantidadeDeCampos.. - P02";

        aprovSoc_DOC_quantidadeDePerguntas = 0;
        for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC; n++) {
            try {
                aprovSoc_pontoPassei = "aprovSoc_setQuantidadeDeCampos.. - P05";
                aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
                aprovSoc_pontoPassei = "aprovSoc_setQuantidadeDeCampos.. - P06";
            } catch (Exception e) {
                aprovSoc_pontoPassei = "aprovSoc_setQuantidadeDeCampos.. - P07";
                break;
                //exception em obter extension field indica fim de lista de perguntas
				/*
                 * ATENCAO - ATENCAo - se DOC(MA/SubAgr) tiver mais perguntas que UDU4 as perguntas
                 * adicionais nao serao populadas!!!
                 */
            }
            aprovSoc_pontoPassei = "aprovSoc_setQuantidadeDeCampos.. - P08";
            aprovSoc_DOC_quantidadeDePerguntas++;
        }
        aprovSoc_pontoPassei = "aprovSoc_setQuantidadeDeCampos.. - P09 - aprovSoc_DOC_quantidadeDeCampos="
                + aprovSoc_DOC_quantidadeDePerguntas;

        return;
    }

    private void aprovSoc_limparValoresCampos() throws ApplicationException, DatabaseException {

        aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P00";
        ExtensionCollectionIfc extCol = doc.getExtensionCollection("orgaos_sociais");
        aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P01";
        while (extCol.size() > 0) {
            aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P02";
            extCol.delete(extCol.get(0));
            aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P03";
        }

        aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P04";
        for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
            aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P05";
            aprovSoc_limparCamposDeUmNumPergunta(n + 1);
            aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P06";
        }
        aprovSoc_pontoPassei = "aprovSoc_limparValoresCampos.. - P07";
    }

    private void aprovSoc_limparCamposDeUmNumPergunta(int numPerguntaBase1)
            throws ApplicationException, DatabaseException {

        aprovSoc_pontoPassei = "aprovSoc_limparCamposDeUmNumPergunta.. - P00" + " n = " + numPerguntaBase1;

        ExtensionFieldIfc extResp = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, numPerguntaBase1);
        ExtensionFieldIfc extSoc = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, numPerguntaBase1);
        ExtensionFieldIfc extOrgao = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, numPerguntaBase1);
        ExtensionFieldIfc extCorpo = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, numPerguntaBase1);

        extResp.set(null);
        aprovSoc_pontoPassei = "aprovSoc_desabilitarCampos.. - P12" + " n = " + numPerguntaBase1;
        extSoc.set(null);
        aprovSoc_pontoPassei = "aprovSoc_desabilitarCampos.. - P12.50" + " n = " + numPerguntaBase1;
        extOrgao.set(null);
        aprovSoc_pontoPassei = "aprovSoc_desabilitarCampos.. - P13" + " n = " + numPerguntaBase1;
        extSoc.set(null);
        aprovSoc_pontoPassei = "aprovSoc_desabilitarCampos.. - P14" + " n = " + numPerguntaBase1;
        extCorpo.set(null);
    }

    private void aprovSoc_habilitarCamposResposta() throws ApplicationException, DatabaseException {
        aprovSoc_pontoPassei = "aprovSoc_habilitarCamposResposta.. - P00";

        for (int n = 0;
                n < (int) Math.min(aprovSoc_DOC_quantidadeDePerguntas, aprovSoc_udu4_questoesAprovacaoSocietaria.size());
                n++) {
            aprovSoc_pontoPassei = "aprovSoc_habilitarCamposResposta.. - P02";
            aprovSoc_unlockExtFieldNumero(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);
        }
        aprovSoc_pontoPassei = "aprovSoc_habilitarCamposResposta.. - P04";
    }

    private void aprovSoc_desabilitarTodosCampos() throws ApplicationException, DatabaseException {
        aprovSoc_pontoPassei = "aprovSoc_desabilitarCampos.. - P00";
        IapiDocumentLockManager.lockField(session, doc, "orgaos_sociais");
        aprovSoc_pontoPassei = "aprovSoc_desabilitarCampos.. - P01";

        for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, n + 1);
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_RESPOSTASIMNAO_PREFIX, n + 1);
            aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);
        }
    }

    private void aprovSoc_avisaQueTemplateNaoSerahCopiado()
            throws ApplicationException, DatabaseException {
        aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P00";
        String msgBase = "     **** Questionario de Aprovacao Societaria nao vai ser duplicado em criacao a partir de Template ****     ";
        aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P01";
        String msgNaoCopiado = "";
        final int tamMinimo = 1;
        aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P02";
        while (msgNaoCopiado.length() < tamMinimo) {
            aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P03";
            msgNaoCopiado += msgBase;
            aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P04";
        }
        aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P05";
        for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
            aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P06";
            ExtensionFieldIfc extfCorpo = aprovSoc_getExtensionFieldNumber(aprovSoc_EXTFIELD_CORPOQUESTAO_PREFIX, n + 1);
            aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P07";
            BigText bigCorpo = (BigText) extfCorpo.get();
            aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P08";
            bigCorpo.setText(msgNaoCopiado);
            aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P09";
        }
        aprovSoc_pontoPassei = "aprovSoc_avisaQueTemplateNaoSerahCopiado P10";
    }

    public void aprovSoc_principal() throws ApplicationException, DatabaseException {

        aprovSoc_pontoPassei = "aprovSoc_principal P00";
        aprovSoc_popularListaDeQuestoesComMasterData(); //tem que ser o primeirissimo metodo chamado!!!
        aprovSoc_pontoPassei = "aprovSoc_principal P00.30";
        aprovSoc_setQuantidadeDePerguntas(); //tem que ser chamado LOGO depois de popularLista!!

        aprovSoc_pontoPassei = "aprovSoc_principal P01";

        aprovSoc_limparValoresCampos();
        aprovSoc_pontoPassei = "aprovSoc_principal P02";
        aprovSoc_desabilitarTodosCampos();
        aprovSoc_pontoPassei = "aprovSoc_principal P03";

        if (doc.isTemplate()) {
            aprovSoc_avisaQueTemplateNaoSerahCopiado();
            return;
        }
        aprovSoc_pontoPassei = "aprovSoc_principal P04";
        aprovSoc_habilitarCamposResposta();

        aprovSoc_pontoPassei = "aprovSoc_principal P05";
        aprovSoc_atribuirExtensioFieldsDeContrato();
        aprovSoc_pontoPassei = "aprovSoc_principal P06";
    }

    public void inicio() throws ApplicationException {
        try {
            gap004_createDeltaPriceValue();
            String tipoDoAcordo = ((ContractIBeanIfc) doc.getParentIBean()).getDocTypeReference().getDisplayName();
            if (tipoDoAcordo.equalsIgnoreCase("Acordo Basico de TESTES")) {
                aprovSoc_principal();
            }
            if (tipoDoAcordo.equalsIgnoreCase("Acordo Básico Geral")) {
                aprovSoc_principal();
            }
			
			
			if(tipoDoAcordo.equalsIgnoreCase(ACORDO_COMERCIAL)){
				this.aprovSoc_principal();
				
			}
        } catch (Exception e) {
            String s = "Exception "
                    + " type : " + e.getClass().getSimpleName()
                    + " in code point : [" + aprovSoc_pontoPassei + "]"
                    + " message : " + e.getMessage();
            if (s.length() > 300) {
                String x = s.substring(0, 300);
                s = x;
            }
           
            this.logError(e.getMessage());
            throw new ApplicationException(s);  //use "e" para REAL THING , ou "s" para DEBUG
        }
    }
    
    public void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }
}
