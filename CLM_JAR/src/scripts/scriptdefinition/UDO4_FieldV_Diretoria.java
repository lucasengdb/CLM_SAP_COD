package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData4IBeanIfc;

// Z - FieldV - UDO4 - Diretoria
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;

// Valida se a Diretoria informada é válida
/**
 *
 * @author Tiago Rodrigues
 */
public class UDO4_FieldV_Diretoria extends CLMDummyScriptDefinition {

    UserDefinedMasterData4IBeanIfc doc;
    // Para portar este código em "Script Definition" copiar todo codigo a partir daqui
    // e remover o comentario da chamado do metodo "inicio"
    //
    //
    //
    // Variaveis e constantes
    BusinessUnitIBeanIfc buDiretoria;
    private final String TIPO_DIRETORIA = "Diretoria";
    ValueListValueIBeanIfc vlv;

    public ApplicationException getLocalizedApplicationException(
            IapiSessionContextIfc session, String resourceId, Object[] modifiers) {

        ApplicationException aEx = new ApplicationException(session,
                "tim.defdata", resourceId);
        if (modifiers != null && modifiers.length > 0) {
            aEx.setMessageModifiers(modifiers);
        }
        return aEx;
    }

    private boolean validaDiretoria() throws ApplicationException, DatabaseException {

        BusinessUnitIBeanHomeIfc buHome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);
        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        // Verifica se o campo "Diretoria" esta preenchido corretamente  
        buDiretoria = ((BusinessUnitIBeanIfc) buHome
                .find((ObjectReferenceIfc) doc.getExtensionField("REG_OUTORGADO_DIR").get()));
        vlv = (ValueListValueIBeanIfc) vlvHome.find((ObjectReferenceIfc) buDiretoria.getFieldMetadata(
                "ORG_UNIT_TYPE").get(buDiretoria));

        return vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA);
    }

    private void gap024_validaDiretoria() throws ApplicationException, DatabaseException {

        if (!validaDiretoria()) {
            throw getLocalizedApplicationException(session, "mensagem.gap024.erro.diretoria_invalida", null);
        }
    }

    public void inicio() throws ApplicationException, DatabaseException {
        gap024_validaDiretoria();
    }
    // inicio();
}
