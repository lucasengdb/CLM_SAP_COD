package scripts.scriptdefinition;

/* MA - Created - Any - Any
Atende:
CLM.003 - Cria os campos para a coleção "Delta Price Value"
CLM.014 - Popula as questões na aba "Aprovação Societária"
CLM.042 - Preenche automaticamente o campo de Fornecedor em AB Procuração
CLM.009 - Gestão de Procuração (Bloqueio da collection somente)
CLM.053 - Bloqueia a Collection de Histórico de Modificação do Quest. Aprov. Soc.
*/
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.doc.IapiDocumentLockManager;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.VendorIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.VendorIBeanIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData3IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData3IBeanIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData4IBeanIfc;
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

public class Z_MA_CREATED_Any_Any extends CLMDummyScriptDefinition{

	ContractIBeanIfc doc;
	//UserDefinedMasterData4IBeanIfc doc;
	
	// Constantes
	private final String EXTC_DETLA_PRICE_VALUE = "delta_price_value";
	private final String EXTF_PERIODO = "period_value";
	private final String VLT_PERIODS_DELTA_PRICE = "periods_delta_price";
	private final String VLV_DIV_1_ANO = "period_1";
	private final String VLV_DIV_2_ANO = "period_2";
	private final String VLV_DIV_PROX_ANOS = "period_3";

	// private final String aprovSoc_USERDEF3MAST_CHAVE_PREFIX = "pergunta_";
	private final String aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO = "pergunta";
	private final String aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC = "sociedade_parceira";
	private final String aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR = "orgao_social";
	private final String EXTFIELD_PERGUNTAS_PREFIX = "pergunta_";
	private final String EXTFIELD_RESPOSTAS_PREFIX = "resposta_";
	private final String aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX = "soc_parceira";
	private final String aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX = "orgao_social";
	private final int aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC = 900; // TEM Q
																		// SER
																		// **MENOR**
																		// QUE
																		// 999 !
	private int aprovSoc_DOC_quantidadeDePerguntas;
	private List aprovSoc_udu4_questoesAprovacaoSocietaria;

	// Para o GAP009
	private final String EXTF_BLOCO_PODER = "proc_poder_txt";
	private final String EXTF_ASS_CONTRATOS = "proc_poder_ass";
	private final String EXTF_ASS_CONTRATOS_SOC = "proc_poder_ass_apr";
	private final String EXTF_DIRETORIA = "proc_poder_dir";
	private final String EXTF_GERENCIA = "proc_poder_ger";
	private final String EXTF_AREA = "proc_poder_area";
	private final String EXTF_VALOR = "proc_poder_valor";

	String debug = "P00";

	public ValueListTypeIBeanIfc getValueListTypeObjIdByExtId(String externalId) throws ChainedException {

		ValueListTypeIBeanHomeIfc vltHome = (ValueListTypeIBeanHomeIfc) IBeanHomeLocator.lookup(session,
				ValueListTypeIBeanHomeIfc.sHOME_NAME);
		List vltList = vltHome.findWhere("EXTERNAL_ID = '" + externalId + "'");
		if (vltList.isEmpty()) {
			throw new ApplicationException("ListType '" + externalId + "' não encontrado.");
		}
		ValueListTypeIBeanIfc vlt = (ValueListTypeIBeanIfc) vltList.iterator().next();

		return vlt;
	}

	public void gap003_createDeltaPriceValue() throws ChainedException {

		ExtensionCollectionIfc collDeltaPriceValue;
		ValueListTypeIBeanIfc vlt;
		ValueListValueIBeanIfc vlv;

		ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator.lookup(session,
				ValueListValueIBeanHomeIfc.sHOME_NAME);

		collDeltaPriceValue = doc.getExtensionCollection(EXTC_DETLA_PRICE_VALUE);

		// Criando 3 linhas na Collection "Delta Price Value"
		collDeltaPriceValue.add(collDeltaPriceValue.create());
		collDeltaPriceValue.add(collDeltaPriceValue.create());
		collDeltaPriceValue.add(collDeltaPriceValue.create());

		vlt = getValueListTypeObjIdByExtId(VLT_PERIODS_DELTA_PRICE);
		IapiDocumentLockManager.lockField(session, doc, EXTC_DETLA_PRICE_VALUE);

		// Setando o Value List na primeira linha e bloqueando a coluna
		// "Periodo"
		vlv = vlvHome.findUniqueByNameType(VLV_DIV_1_ANO, vlt.getTypeCode());
		collDeltaPriceValue.get(0).getExtensionField(EXTF_PERIODO).set(vlv.getLocalizedObjectReference());
		IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(0), EXTF_PERIODO);

		// Setando o Value List na segunda linha e bloqueando a coluna "Periodo"
		vlv = vlvHome.findUniqueByNameType(VLV_DIV_2_ANO, vlt.getTypeCode());
		collDeltaPriceValue.get(1).getExtensionField(EXTF_PERIODO).set(vlv.getLocalizedObjectReference());
		IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(1), EXTF_PERIODO);

		// Setando o Value List na terceira linha e bloqueando a coluna
		// "Periodo"
		vlv = vlvHome.findUniqueByNameType(VLV_DIV_PROX_ANOS, vlt.getTypeCode());
		collDeltaPriceValue.get(2).getExtensionField(EXTF_PERIODO).set(vlv.getLocalizedObjectReference());
		IapiDocumentLockManager.lockField(session, collDeltaPriceValue.get(2), EXTF_PERIODO);
	}

	private String aprovSoc_leftPadIntAsString(int num, int pos, String padStr) {

		debug = "leftPadIntAsString() - P00";
		String s = Integer.toString(num);
		while (s.length() < pos) {
			s = padStr + s;
		}
		return s;
	}

	private int aprovSoc_getPadLen(String field) {

		debug = "aprovSoc_getPadLen() - P00";
		int retval;
		if (field.startsWith(EXTFIELD_PERGUNTAS_PREFIX)) {
			retval = 3;
		} else if (field.startsWith(EXTFIELD_RESPOSTAS_PREFIX)) {
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
		debug = "aprovSoc_getPadString() - P08";
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
		} catch (ApplicationException e) {
			okOper = false;
		} catch (DatabaseException e) {
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
			} catch (ApplicationException e) {
				okOper = false;
			} catch (DatabaseException e) {
				okOper = false;
			}
		}

		if (!okOper) {
			throw new ApplicationException("Master Agreement de numero " + doc.getDocumentId()
					+ " - impossivel fazer lock de extensio field com nome " + extFieldNameComZeros + " nem com nome "
					+ extFieldNameSemZeros);
		}
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
			// nada
		}
		if (fld == null) {
			try {
				fld = doc.getExtensionField(extFieldNameSemZeros);
			} catch (Exception e) {
				// nada
			}
		}

		if (fld == null) {
			throw new ApplicationException("Master Agrteement/Sub Agreement de numero " + doc.getDocumentId()
					+ " - Nao foi achado extension field com nome " + extFieldNameComZeros + " nem com nome "
					+ extFieldNameSemZeros);
		}
		return fld;
	}

	/**
	 * ATENCAO - todo o codigo independe de haver sequencia exata. Pode haver
	 * pergunta_012 , pergunta_18 e pergunta_23, e mesmo assim serão populados
	 * os 3 primeiros EXT_FIELDs do Contract/Agreement
	 *
	 * @return List < Map < String, Object > > - onde List=as questoes/X
	 *         socparc, Map de Nome/Valor
	 * @throws DatabaseException
	 * @throws ApplicationException
	 */
	private void aprovSoc_popularListaDeQuestoesComMasterData() throws ChainedException {

		debug = "aprovSoc_popularLista.. - P00";

		UserDefinedMasterData3IBeanHomeIfc udmd3Home = (UserDefinedMasterData3IBeanHomeIfc) IBeanHomeLocator
				.lookup(session, UserDefinedMasterData3IBeanHomeIfc.sHOME_NAME);

		String sufixoWhere = "INACTIVE=0 and EXTERNAL_ID LIKE 'pergunta_%'"; // DAH
																				// PAU
																				// DE
																				// SQL
																				// SE
																				// USARMOS
																				// ORDER
																				// BY
																				// AQUI!

		List foundStuff = udmd3Home.findWhere(sufixoWhere // faz com que venha
															// do menor numero
															// de pergunta para
															// o maior
		);

		SortedMap sortedMap = new TreeMap();

		debug = "aprovSoc_popularLista.. - P04";
		aprovSoc_udu4_questoesAprovacaoSocietaria = new ArrayList();
		for (Object o : foundStuff) {

			IBeanIfc ifc = (IBeanIfc) o;
			UserDefinedMasterData3IBeanIfc udmd3 = (UserDefinedMasterData3IBeanIfc) ifc;
			BigText corpoPergunta = (BigText) udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO)
					.get();
			Boolean sociedadeParc = (Boolean) udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC).get();

			Object objOrgaoAprovadorObjRef = udmd3.getExtensionField(aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR)
					.get();

			Map m = new HashMap();
			m.put(aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO, corpoPergunta);
			m.put(aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC, sociedadeParc);
			m.put(aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR, objOrgaoAprovadorObjRef);

			sortedMap.put(udmd3.getExternalId(), m);
		}
		debug = "aprovSoc_popularLista.. - P14";
		for (Object key : sortedMap.keySet()) {
			String strKey = (String) key;
			Map m = (Map) sortedMap.get(strKey);
			aprovSoc_udu4_questoesAprovacaoSocietaria.add(m);
		}

		if (aprovSoc_udu4_questoesAprovacaoSocietaria.size() >= aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC) {
			throw new ApplicationException("Master Data 4 - erro de configuracao - limite de "
					+ aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC + " excedido.");
		}
	}

	private void aprovSoc_atribuirExtensioFieldsDeContrato() throws ChainedException {

		debug = "aprovSoc_atribuirExt.. - P00";

		for (int n = 0; n < (int) Math.min(aprovSoc_DOC_quantidadeDePerguntas,
				aprovSoc_udu4_questoesAprovacaoSocietaria.size()); n++) {

			Map m = (Map) aprovSoc_udu4_questoesAprovacaoSocietaria.get(n);

			ExtensionFieldIfc extFieldCorpoN = aprovSoc_getExtFieldNumbered(EXTFIELD_PERGUNTAS_PREFIX, n + 1);
			ExtensionFieldIfc extFieldSociedadeParceira = aprovSoc_getExtFieldNumbered(
					aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, n + 1);
			ExtensionFieldIfc extFieldOrgaoAprovador = aprovSoc_getExtFieldNumbered(
					aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);

			BigText corpoPerguntaDaUDU3 = (BigText) m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_CORPOQUESTAO);
			BigText corpoQuestaoDoContratoComoBigText = (BigText) extFieldCorpoN.get();
			corpoQuestaoDoContratoComoBigText.setText(corpoPerguntaDaUDU3.getText(session));
			Boolean sociedadeParceiraDaUDU3 = (Boolean) m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_SOCPARC);
			Object objOrgaoAprovadorUDU3 = m.get(this.aprovSoc_USERDEF3MAST_EXTFIELD_ORGAOAPROVADOR);

			extFieldSociedadeParceira.set(sociedadeParceiraDaUDU3);
			extFieldOrgaoAprovador.set(objOrgaoAprovadorUDU3);
		}
		debug = "aprovSoc_atribuirExt.. - P18";
	}

	private void aprovSoc_setQuantidadeDePerguntas() throws ChainedException {

		aprovSoc_DOC_quantidadeDePerguntas = 0;
		for (int n = 0; n < aprovSoc_MAX_EXTFIELDS_PERGUNTA_SOCPARC; n++) {
			try {
				aprovSoc_getExtFieldNumbered(EXTFIELD_PERGUNTAS_PREFIX, n + 1);
			} catch (ApplicationException e) {
				break;
				// exception em obter extension field indica fim de lista de
				// perguntas
				/*
				 * ATENCAO - ATENCAo - se DOC(MA/SubAgr) tiver mais perguntas
				 * que UDU4 as perguntas adicionais nao serao populadas!!!
				 */
			}
			aprovSoc_DOC_quantidadeDePerguntas++;
		}
	}

	private void aprovSoc_limparValoresCampos() throws ChainedException {

		debug = "aprovSoc_limparValoresCampos.. - P00";
		ExtensionCollectionIfc extCol = doc.getExtensionCollection("orgaos_sociais");
		while (extCol.size() > 0) {
			extCol.delete(extCol.get(0));
		}

		for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
			aprovSoc_limparCamposDeUmNumPergunta(n + 1);
		}
	}

	private void aprovSoc_limparCamposDeUmNumPergunta(int numPerguntaBase1) throws ChainedException {
		debug = "aprovSoc_limparCamposDeUmNumPergunta.. - P00" + " n = " + numPerguntaBase1;

		ExtensionFieldIfc extResp = aprovSoc_getExtFieldNumbered(EXTFIELD_RESPOSTAS_PREFIX, numPerguntaBase1);
		ExtensionFieldIfc extSoc = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX,
				numPerguntaBase1);
		ExtensionFieldIfc extOrgao = aprovSoc_getExtFieldNumbered(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX,
				numPerguntaBase1);
		ExtensionFieldIfc extCorpo = aprovSoc_getExtFieldNumbered(EXTFIELD_PERGUNTAS_PREFIX, numPerguntaBase1);

		extResp.set(null);
		extSoc.set(null);
		extOrgao.set(null);
		extSoc.set(null);
		extCorpo.set(null);
	}

	private void aprovSoc_habilitarCamposResposta() throws ChainedException {

		debug = "aprovSoc_habilitarCamposResposta.. - P00";

		for (int n = 0; n < (int) Math.min(aprovSoc_DOC_quantidadeDePerguntas,
				aprovSoc_udu4_questoesAprovacaoSocietaria.size()); n++) {
			debug = "aprovSoc_habilitarCamposResposta.. - P02";
			aprovSoc_unlockExtFieldNumero(EXTFIELD_RESPOSTAS_PREFIX, n + 1);
		}
	}

	private void aprovSoc_desabilitarTodosCampos() throws ChainedException {

		debug = "aprovSoc_desabilitarCampos.. - P00";
		IapiDocumentLockManager.lockField(session, doc, "orgaos_sociais");

		for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
			aprovSoc_lockExtFieldNumero(EXTFIELD_PERGUNTAS_PREFIX, n + 1);
			aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_SOCIEDADEPARCEIRA_PREFIX, n + 1);
			aprovSoc_lockExtFieldNumero(EXTFIELD_RESPOSTAS_PREFIX, n + 1);
			aprovSoc_lockExtFieldNumero(aprovSoc_EXTFIELD_ORGAOSOCIALAPROVADOR_PREFIX, n + 1);
		}
	}

	private void aprovSoc_avisaQueTemplateNaoSerahCopiado() throws ChainedException {

		debug = "aprovSoc_avisaQueTemplateNaoSerahCopiado P00";
		String msgBase = "     **** Questionario de Aprovacao Societaria nao vai "
				+ "ser duplicado em criacao a partir de Template ****     ";
		String msgNaoCopiado = "";
		final int tamMinimo = 1;
		while (msgNaoCopiado.length() < tamMinimo) {
			msgNaoCopiado += msgBase;
		}
		for (int n = 0; n < aprovSoc_DOC_quantidadeDePerguntas; n++) {
			ExtensionFieldIfc extfCorpo = aprovSoc_getExtFieldNumbered(EXTFIELD_PERGUNTAS_PREFIX, n + 1);
			BigText bigCorpo = (BigText) extfCorpo.get();
			bigCorpo.setText(msgNaoCopiado);
		}
	}

	public void gap014_aprovSoc_principal() throws ChainedException {

		debug = "aprovSoc_principal P00";
		aprovSoc_popularListaDeQuestoesComMasterData(); // tem que ser o
														// primeirissimo metodo
														// chamado!!!
		aprovSoc_setQuantidadeDePerguntas(); // tem que ser chamado LOGO depois
												// de popularLista!!
		aprovSoc_limparValoresCampos();
		aprovSoc_desabilitarTodosCampos();

		if (doc.isTemplate()) {
			aprovSoc_avisaQueTemplateNaoSerahCopiado();
			return;
		}
		aprovSoc_habilitarCamposResposta();

		aprovSoc_atribuirExtensioFieldsDeContrato();
	}

	// CHECA A EXISTENCIA DE PERGUNTAS EM BRANCO PARA BLOQUEA-LAS
	private void gap014_checaPerguntasEmBranco() throws ChainedException {

		ExtensionFieldIfc extField;
		String strNum;

		for (int i = 1; i <= aprovSoc_DOC_quantidadeDePerguntas; i++) {

			if (i < 10) {
				strNum = "00" + i;
			} else if (i < 100) {
				strNum = "0" + i;
			} else {
				strNum = Integer.toString(i);
			}

			// Coletando a pergunta e verificando se ela esta preenchida
			extField = doc.getExtensionField(EXTFIELD_PERGUNTAS_PREFIX + strNum);
			BigText textoPergunta = (BigText) extField.get();
			// Verificando se a pergunta existe preenchida
			if (!hasValue(textoPergunta.getTextPreview())) {
				// Bloqueando a pergunta e resposta
				IapiDocumentLockManager.lockField(session, doc, EXTFIELD_PERGUNTAS_PREFIX + strNum);
				IapiDocumentLockManager.lockField(session, doc, EXTFIELD_RESPOSTAS_PREFIX + strNum);
				if (i == 16) {
					IapiDocumentLockManager.lockField(session, doc, EXTFIELD_RESPOSTAS_PREFIX + 16);
				}
			}
		}
	}

	// GAP 043 - Preenche automaticamente o campo de Fornecedor
	private void gap042_preencheFornecedorByExternalId(String externalId) throws ChainedException {

		// Adicionando Fornecedor
		VendorIBeanHomeIfc vendorHome = (VendorIBeanHomeIfc) IBeanHomeLocator.lookup(session,
				VendorIBeanHomeIfc.sHOME_NAME);
		VendorIBeanIfc vendor = vendorHome.findByExternalId(externalId);
		doc.getFieldMetadata(ContractIBeanIfc.sPROPID_VENDOR).set(doc, vendor.getObjectReference());
	}

	// BLOQUEIA OS CAMPOS DO BLOCO DE PODERES
	private void gap009_bloqueiaCampos() throws ChainedException {

		// Campo "Bloco de Poder"
		IapiDocumentLockManager.lockField(session, doc, EXTF_BLOCO_PODER);
		// Campo "Assinar Contratos"
		IapiDocumentLockManager.lockField(session, doc, EXTF_ASS_CONTRATOS);
		// Campo "Assinar contratos após aprovação societária"
		IapiDocumentLockManager.lockField(session, doc, EXTF_ASS_CONTRATOS_SOC);
		// Campo "Diretoria"
		IapiDocumentLockManager.lockField(session, doc, EXTF_DIRETORIA);
		// Campo "Gerência"
		IapiDocumentLockManager.lockField(session, doc, EXTF_GERENCIA);
		// Campo "Área"
		IapiDocumentLockManager.lockField(session, doc, EXTF_AREA);
		// Campo "Valor"
		IapiDocumentLockManager.lockField(session, doc, EXTF_VALOR);
	}

	private void gap053_limpaCollectionEBloqueia() throws ChainedException {
		ExtensionCollectionIfc historicoModificacao = doc.getExtensionCollection("historicoqas");
		while (historicoModificacao.size() > 0) {
			historicoModificacao.delete(historicoModificacao.get(0));
		}
		IapiDocumentLockManager.lockField(session, doc, "historicoqas");
	}

	public void inicio() throws ChainedException {

		try {
			gap003_createDeltaPriceValue();
			// Filtros para correta execução de alguns métodos em determinados
			// tipos de Acordo
			if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Basico de TESTES")) {
				gap014_aprovSoc_principal();
			}
			if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Comercial")) {
				gap014_aprovSoc_principal();
				// Este método deverá ser executado por ultimo, checa perguntas
				// em branco
				// para deixa-las em somente leitura
				gap014_checaPerguntasEmBranco();
				gap053_limpaCollectionEBloqueia();
			}
			if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Básico Geral")) {
				gap014_aprovSoc_principal();
				// Este método deverá ser executado por ultimo, checa perguntas
				// em branco
				// para deixa-las em somente leitura
				gap014_checaPerguntasEmBranco();
				gap053_limpaCollectionEBloqueia();
			}
			if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Procuração")) {
				// Adicionando o fornecedor com o external id "TIM"
				// automaticamente
				gap042_preencheFornecedorByExternalId("TIM");
				// Bloqueia a collection "Registro de Representantes" GAP009
				IapiDocumentLockManager.lockField(session, doc, "representantes");
				// Bloqueia a collection "Registro de Diretores" GAP009
				IapiDocumentLockManager.lockField(session, doc, "diretores");
				// Bloqueia os campos do bloco de poderes
				gap009_bloqueiaCampos();
			}
		} catch (ChainedException e) {
			throw new ApplicationException(e);
		}
	}

}
