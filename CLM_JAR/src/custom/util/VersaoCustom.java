package custom.util;

import com.sap.odp.api.common.log.LogMessageIfc;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;

/**
 *
 * @author Tiago Rodrigues
 * @author Renato
 * 
 * A intens�o neste script � a exibi��o de qual vers�o do Custom JAR est� instalado no CLM
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
        Logger.warning(createMessage("A vers�o do custom JAR: " + versao));
    }
}
