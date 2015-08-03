package cmldummy;

import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanIfc;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;

/**
 *
 * @author Tiago Rodrigues
 * 
 * M�todos e variaveis usadas em um Script Definition com 
 * Contexto Document e Evento de Duplicated
 * 
 */
public class CLMDummyDocumentPrePostPhaseChange {
    
    // Serve para todos os Contextos e Eventos
    public IapiSessionContextIfc session = null;
    public ContractDocumentIBeanIfc doc = null;
    public String current_phase;        // Fase atual do documento
    public String other_phase;          // Fase a qual ser� trocada
    public Boolean phase_advancing;     // Seta como "true" se a mudan�a de fase � avan�ando e n�o retrocedendo
    
    public Boolean hasValue(Object o) {
        return true;
    }
}
