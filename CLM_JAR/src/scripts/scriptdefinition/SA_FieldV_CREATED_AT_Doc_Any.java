package scripts.scriptdefinition;

import cmldummy.CLMDummyScriptDefinition;
import com.sap.eso.api.contracts.AgreementIBeanIfc;

// SA_FieldV_CREATED_AT_Doc_Any
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.doc.IapiDocumentLockManager;
import com.sap.odp.api.doccommon.masterdata.CompanyCodeIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.CompanyCodeIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;

/**
 *
 * @author Tiago Rodrigues
 */
public class SA_FieldV_CREATED_AT_Doc_Any extends CLMDummyScriptDefinition {

    AgreementIBeanIfc doc;
    //
    // Constantes e variaveis    
    private String pattern = "";

    // Substitui os TOKENS no Document Id por outro valor
    private void setDocumentId() throws ApplicationException, DatabaseException {

        if (hasValue(doc.getDocumentId())) {

            if (doc.getDocumentId().indexOf("<%ORG_UNIT%>") >= 0) {
                if (hasValue(doc.getOrganizationalUnitRef())) {
                    IBeanHomeIfc busUnitHome = IBeanHomeLocator.lookup(session, doc.getOrganizationalUnitRef());
                    BusinessUnitIBeanIfc busUnit = (BusinessUnitIBeanIfc) busUnitHome
                            .find(doc.getOrganizationalUnitRef());
                    pattern = doc.getDocumentId().replace("<%ORG_UNIT%>", busUnit.getExternalId());
                    doc.getFieldMetadata("UNIQUE_DOC_NAME").set(doc, pattern);
                } else {
                    throw doc.createApplicationException("BUSINESS_UNIT", "Informar Diretoria.");
                }
                IapiDocumentLockManager.lockField(session, doc, "BUSINESS_UNIT");
            }

            if (doc.getDocumentId().indexOf("<%CC%>") >= 0) {
                if (hasValue(doc.getExtensionField("EMPRESA_1").get())) {
                    CompanyCodeIBeanHomeIfc companyCodeHome = (CompanyCodeIBeanHomeIfc) IBeanHomeLocator
                            .lookup(session, CompanyCodeIBeanHomeIfc.sHOME_NAME);
                    CompanyCodeIBeanIfc companyCode = (CompanyCodeIBeanIfc) companyCodeHome
                            .find((ObjectReferenceIfc) doc.getExtensionField("EMPRESA_1").get());
                    pattern = doc.getDocumentId().replace("<%CC%>", companyCode.getExternalId());
                    doc.getFieldMetadata("UNIQUE_DOC_NAME").set(doc, pattern);
                } else {
                    throw doc.createApplicationException("EMPRESA_1", "Informar Company.");
                }
                IapiDocumentLockManager.lockField(session, doc, "EMPRESA_1");
            }            
        }
    }

    public void inicio() throws ApplicationException {
        try {
            setDocumentId();
        } catch (Exception e) {
            throw new ApplicationException(e);
        }
    }
    //inicio();
}
