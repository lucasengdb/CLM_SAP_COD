package scripts.scriptdefinition.qualidade;

import com.sap.eso.api.contracts.AgreementIBeanIfc;
/* 
SA - Validated - Any - Any
Atende:
CLM.012 (Partes relacionadas) 
CLM.016 (Ampliação de niveis organizacionais)
*/
import com.sap.eso.api.contracts.ContractIBeanIfc;
import java.util.List;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.VendorIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.VendorIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.common.db.ObjectReference;

import cmldummy.CLMDummyScriptDefinition;


public class SA_VALIDATED_ANY_ANY  extends CLMDummyScriptDefinition{
	 
	AgreementIBeanIfc doc;
	
	// Extension Collections
    private final String EXTF_GERENCIA = "MA_GERENCIA";
    private final String EXTF_AREA = "MA_AREA";
    // Outras Constantes
    private final String LOGGER_ID = "\n[SA VALIDATED ANY ANY] ";
    private final String TIPO_DIRETORIA = "Diretoria";
    private final String TIPO_GERENCIA = "Gerência";
    private final String TIPO_AREA = "Área";
    // Variaveis
    ValueListValueIBeanIfc vlv;
    BusinessUnitIBeanIfc buDiretoria;
    BusinessUnitIBeanIfc buGerencia;
    BusinessUnitIBeanIfc buArea;

    private String pontoDebug = "";

    private ApplicationException getAppException(String bundle, String campo, String msgId) {
        ApplicationException ex = doc.createApplicationException(campo, msgId);
        ex.setBundleName(bundle);
        return ex;
    }

    public ApplicationException getLocalizedApplicationException(
            IapiSessionContextIfc session, String resourceId, Object[] modifiers) {

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

    // Valida o preenchimento correto em "Gerência" e "Área"
    private void gap016_validaGerencia_e_Area() throws ApplicationException, DatabaseException {

        pontoDebug = "base_validaGerencia_e_Area() - P00";
        ObjectReference objRef;
        BusinessUnitIBeanHomeIfc buHome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);
        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        boolean condicao = false;

        // Diretoria [######] Gerência [     ] Área [     ]
        condicao = hasValue(doc.getBusinessUnitRef())
                && !hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
        if (condicao) {
            // Verifica se o campo "Diretotia" esta preenchido corretamente
            buDiretoria = ((BusinessUnitIBeanIfc) buHome.find((ObjectReference) doc.getBusinessUnitRef()));
            objRef = (ObjectReference) buDiretoria.getFieldMetadata("ORG_UNIT_TYPE").get(buDiretoria);
            if (!objRef.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
                throw getAppException("tim.defedata", ContractIBeanIfc.sPROPID_BUSINESS_UNIT,
                        "mensagem.gap016.erro.diretoria_preenchimento_incorreto");
            }
        }

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
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.gerencia_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Diretoria" esta preenchido corretamente
            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia
                    .getParentObjRef());
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buDiretoria.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buDiretoria));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_DIRETORIA)) {
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.diretoria_preenchimento_incorreto", null);
            }
            if (!doc.getBusinessUnitRef().equals(
                    buDiretoria.getObjectReference())) {
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.diretoria_e_gerencia_nao_correspondem", null);
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
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.diretoria_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Área" esta preenchido corretamente
            buArea = ((BusinessUnitIBeanIfc) buHome
                    .find((ObjectReferenceIfc) doc.getExtensionField(EXTF_AREA)
                            .get()));
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buArea.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buArea));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_AREA)) {
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.area_preenchimento_incorreto", null);
            }
            // Checando se correspondem
            buGerencia = (BusinessUnitIBeanIfc) buHome.find(buArea.getParentObjRef());
            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
            if (!doc.getBusinessUnitRef().equals(buDiretoria.getObjectReference())) {
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.diretoria_e_area_nao_correspondem", null);
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
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.diretoria_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Gerência" esta preenchido corretamente
            buGerencia = ((BusinessUnitIBeanIfc) buHome
                    .find((ObjectReferenceIfc) doc.getExtensionField(
                                    EXTF_GERENCIA).get()));
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buGerencia.getFieldMetadata(
                                    "ORG_UNIT_TYPE").get(buGerencia));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_GERENCIA)) {
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.gerencia_preenchimento_incorreto", null);
            }
            // Verifica se o campo "Área" esta preenchido corretamente
            buArea = ((BusinessUnitIBeanIfc) buHome.find((ObjectReferenceIfc) doc.getExtensionField(EXTF_AREA).get()));
            vlv = (ValueListValueIBeanIfc) vlvHome
                    .find((ObjectReferenceIfc) buArea.getFieldMetadata("ORG_UNIT_TYPE").get(buArea));
            if (!vlv.getDisplayName().equalsIgnoreCase(TIPO_AREA)) {
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.area_preenchimento_incorreto", null);
            }
            // Checando se correspondem
            if (!buDiretoria.getObjectReference().equals(buGerencia.getParentObjRef())
                    || !buGerencia.getObjectReference().equals(buArea.getParentObjRef())) {
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.diretoria_gerencia_area_nao_correspondem", null);
            }
            return;
        }

        // Diretoria [     ] Gerência [     ] Área [     ]
        condicao = !hasValue(doc.getBusinessUnitRef())
                && !hasValue(doc.getExtensionField(EXTF_GERENCIA).get())
                && !hasValue(doc.getExtensionField(EXTF_AREA).get());
        if (condicao) {
            throw getLocalizedApplicationException(
                    session, "mensagem.gap016.erro.diretoria_gerencia_area_obrigatorio", null);
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
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.area_preenchimento_incorreto", null);
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
                throw getLocalizedApplicationException(
                        session, "mensagem.gap016.erro.gerencia_preenchimento_incorreto", null);
            }

            // Preenchendo campo "Diretoria" de acordo com "Gerência"            
            buDiretoria = (BusinessUnitIBeanIfc) buHome.find(buGerencia.getParentObjRef());
            doc.setBusinessUnitRef(buDiretoria.getObjectReference());
        }
    }

    public void inicio() throws ChainedException {        
        if (!session.getAccount().getUserName().equalsIgnoreCase("enterprise")
                && !session.getAccount().getUserName().equals("WORKFLOWUSER")) {
            try {
                // Méthods que serão executados em toda a lista de tipos de MA
                gap016_validaGerencia_e_Area();
                gap012_preencheSociedadeParceira();
            } catch (Exception e) {
                //use "e" para REAL THING , ou "s" para DEBUG
                if (hasValue(doc.getDocumentId())) {
                    throw new ApplicationException("[" + doc.getDocumentId() + "] " + e.getMessage());
                } else {
                    throw new ApplicationException(e.getMessage());
                }
            }
        }
    }
}
