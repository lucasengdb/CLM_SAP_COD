package cmldummy;

import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanIfc;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;

/**
 *
 * @author Tiago Rodrigues
 * 
 * Métodos e variaveis usadas em um Script Definition com 
 * Contexto Document e Evento de Duplicated
 * 
 */
public class CLMDummyDocumentPrePostPhaseChange {
    
    // Serve para todos os Contextos e Eventos
    public IapiSessionContextIfc session = null;
    public ContractDocumentIBeanIfc doc = null;
    public String current_phase;        // Fase atual do documento
    public String other_phase;          // Fase a qual será trocada
    public Boolean phase_advancing;     // Seta como "true" se a mudança de fase é avançando e não retrocedendo
    
    public Boolean hasValue(Object o) {
        return true;
    }
}
