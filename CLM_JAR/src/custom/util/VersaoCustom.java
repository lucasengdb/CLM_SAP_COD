package custom.util;

import com.sap.odp.api.common.log.LogMessageIfc;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;

/**
 *
 * @author Tiago Rodrigues
 * @author Renato
 * 
 * A intensão neste script é a exibição de qual versão do Custom JAR está instalado no CLM
 *
 */
public class VersaoCustom {

    private final static String versao = "\"24 de Maio 2013 - 11h00\"";
    private static final String LOGGER_CLASS = "\n[Versao Custom JAR] ";
    private IapiSessionContextIfc session;

    public VersaoCustom(IapiSessionContextIfc session) {
        this.session = session;
    }

    protected LogMessageIfc createMessage(String mensagem) {
        LogMessageIfc logMessage = Logger.createLogMessage(session);
        logMessage.setLogMessage(LOGGER_CLASS + mensagem);

        logMessage.setClass(LOGGER_CLASS);
        return logMessage;
    }

    public void mostrar() {
        Logger.warning(createMessage("A versão do custom JAR: " + versao));
    }
}
