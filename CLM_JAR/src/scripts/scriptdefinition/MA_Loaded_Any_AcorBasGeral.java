package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;

// Z - MA - Loaded - Any -  AcorBasGeral
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.doc.IapiDocumentLockManager;
import com.sap.odp.api.ibean.ExtensionFieldIfc;
import com.sap.odp.common.types.BigText;

/**
 *
 * @author Tiago Rodrigues
 */
public class MA_Loaded_Any_AcorBasGeral extends CLMDummyScriptDefinition {

    ContractIBeanIfc doc;
    // Para portar este código em "Script Definition" copiar todo codigo a partir daqui
    // e remover o comentario da chamado do metodo "inicio"
    //
    //
    // Variavéis e Constantes
    private final int MAX_EXTFIELDS_PERGUNTA = 900;
    private final String EXTFIELD_PERGUNTAS_PREFIX = "pergunta_";
    private final String EXTFIELD_RESPOSTAS_PREFIX = "resposta_";
    private int aprovSoc_DOC_quantidadeDePerguntas;

    private int aprovSoc_getPadLen(String field) {

        int retval = 0;
        if (field.startsWith(EXTFIELD_PERGUNTAS_PREFIX)) {
            retval = 3;
        } else if (field.startsWith(EXTFIELD_RESPOSTAS_PREFIX)) {
            retval = 3;
        }
        return retval;
    }

    private String aprovSoc_leftPadIntAsString(int num, int pos, String padStr) {

        String s = Integer.toString(num);

        while (s.length() < pos) {
            s = padStr + s;
        }
        return s;
    }

    private String aprovSoc_getPadString(String field, int n) {
        return aprovSoc_leftPadIntAsString(n, aprovSoc_getPadLen(field), "0");
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
            throw new ApplicationException("Master Agrteement/Sub Agreement de numero "
                    + doc.getDocumentId() + " - Nao foi achado extension field com nome "
                    + extFieldNameComZeros + " nem com nome " + extFieldNameSemZeros);
        }
        return fld;
    }

    private void aprovSoc_setQuantidadeDePerguntas() throws ApplicationException {

        aprovSoc_DOC_quantidadeDePerguntas = 0;
        for (int n = 0; n < MAX_EXTFIELDS_PERGUNTA; n++) {
            try {
                aprovSoc_getExtFieldNumbered(EXTFIELD_PERGUNTAS_PREFIX, n + 1);
            } catch (Exception e) {
                break;
                //exception em obter extension field indica fim de lista de perguntas
				/*
                 * ATENCAO - ATENCAo - se DOC(MA/SubAgr) tiver mais perguntas que UDU4 as perguntas
                 * adicionais nao serao populadas!!!
                 */
            }
            aprovSoc_DOC_quantidadeDePerguntas++;
        }
    }

    // CHECA A EXISTENCIA DE PERGUNTAS EM BRANCO PARA BLOQUEA-LAS
    private void gap014_checaPerguntasEmBranco() throws ApplicationException, DatabaseException {

        ExtensionFieldIfc extField = null;
        String strNum;

        aprovSoc_setQuantidadeDePerguntas();

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

    public void inicio() throws ApplicationException, DatabaseException {

        if (doc.getDocTypeReference().getDisplayName().equalsIgnoreCase("Acordo Básico Geral")) {
            if (!session.getAccount().getUserName().equals("WORKFLOWUSER")) {
                gap014_checaPerguntasEmBranco();
            }
        }
    }
    //inicio();
}
