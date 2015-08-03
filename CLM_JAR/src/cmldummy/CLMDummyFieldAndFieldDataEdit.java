package cmldummy;

import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.ibean.metadata.IBeanFieldMdIfc;

/**
 *
 * @author Tiago Rodrigues
 */
public class CLMDummyFieldAndFieldDataEdit {

    public IapiSessionContextIfc session = null;
    // Reference to the IBeanFieldMdIfc for field in question    
    public IBeanFieldMdIfc field = null;
    // Reference to value of field in question. In the case of Field Data Edit, this is the old value of the field.
    public ObjectReferenceIfc fieldValue = null;
    public ObjectReferenceIfc newFieldValue = null;

    public Boolean hasValue(Object o) {
        return true;
    }

    public Object getFieldValue() {
        Object obj = null;
        return obj;
    }
}
