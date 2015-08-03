package cmldummy;

import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanIfc;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorApprovalRuleType;

/**
 *
 * @author Tiago Rodrigues
 */
public class CLMDummyWorkflow {

    // Variavis utilizadas para o contexto Workflow
    public IapiSessionContextIfc session = null;
    public ContractDocumentIBeanIfc doc;
    public int nativeId = 0;
    public String current_phase;        // Fase atual do documento
    public String other_phase;          // Fase a qual será trocada
    public Boolean phase_advancing;     // Seta como "true" se a mudança de fase é avançando e não retrocedendo
    // Abaixo constantes que estão disponiveis para checagem do status da aprovação
    public static int PENDING = 0;      // Pendente de aprovação
    public static int APPROVED = 1;     // Aprovado
    public static int DENIED = 2;       // Reprovado
    public static int CANCELED = 3;     // Cancelado pelo método cancelProcess(String);

    public Boolean hasValue(Object o) {
        return true;
    }

    /**
     *
     * @param message Cancela o processo de Workflow
     */
    public void cancelProcess(String message) {
    }

    /**
     *
     * @return
     */
    public int getApprovalStatus() {
        return 0;
    }

    /**
     *
     * @param objRef Must be an instance of an ObjectReferenceIfc that refers to GroupIBeanIfc, or UserAccountIBeanIfc
     */
    public void addApprover(ObjectReferenceIfc objRef) {
    }

    public void addApprover(ObjectReferenceIfc objRef, CollaboratorApprovalRuleType rule) {
    }

    public void addApprover(ObjectReferenceIfc objRef, int rule) {
    }
}
