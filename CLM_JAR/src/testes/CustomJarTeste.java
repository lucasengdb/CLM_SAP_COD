package testes;

import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.platform.IapiTaskIfc;

/**
 *
 * @author Tiago Rodrigues
 */
public class CustomJarTeste implements IapiTaskIfc {

    private IapiSessionContextIfc session;

    public CustomJarTeste(IapiSessionContextIfc session) {
        this.session = session;
    }

    @Override
    public IapiSessionContextIfc getSessionContext() {
        return session;
    }

    @Override
    public String getDescription() {
        return "Custom Jar Teste";
    }

    @Override
    public boolean execute() throws ChainedException {
        Logger.info(Logger.createLogMessage(session).setLogMessage("Task Scheduler Executado com sucesso!"));        
        return true;
    }
    }
