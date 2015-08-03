package cmldummy;

import com.sap.odp.api.common.platform.IapiSessionContextIfc;

/**
 *
 * @author Tiago Rodrigues
 *
 * Métodos e variaveis usadas em um Script Definition
 *
 */
public class CLMDummyScriptDefinition {

    // Serve para todos os Contextos e Eventos
    public IapiSessionContextIfc session = null;
    public String fieldValue = "";

    public Boolean hasValue(Object o) {
        return true;
    }
}
