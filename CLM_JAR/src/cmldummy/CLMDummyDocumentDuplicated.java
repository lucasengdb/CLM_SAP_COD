package cmldummy;

import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;

/**
 *
 * @author Tiago Rodrigues
 * 
 * Métodos e variaveis usadas em um Script Definition com 
 * Contexto Document e Evento de Duplicated
 * 
 */
public class CLMDummyDocumentDuplicated {
    
    // Serve para todos os Contextos e Eventos
    public IapiSessionContextIfc session = null;
    public ObjectReferenceIfc otherDoc = null;
    
    public Boolean hasValue(Object o) {
        return true;
    }
}
