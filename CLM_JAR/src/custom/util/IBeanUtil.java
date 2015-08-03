package custom.util;

import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.common.types.ObjectReferenceIfc;
import com.sap.odp.api.common.types.SimpleObjectReferenceIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData2IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData2IBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import java.util.Collection;
import org.dom4j.Element;

/**
 *
 * @author Tiago Rodrigues
 */
public class IBeanUtil {
        
    /**
     *
     * @param session
     * @param resourceId
     * @param modifiers
     * @return
     */
    public static ApplicationException getLocalizedResource(IapiSessionContextIfc session,
            String resourceId, Object[] modifiers) {
        ApplicationException aEx = new ApplicationException(session, "tim.defdata", resourceId);
        if (modifiers != null && modifiers.length > 0) {
            aEx.setMessageModifiers(modifiers);
        }
        return aEx;
    }    
    
    /**
     *
     * Gera o um Logger de informação
     * 
     * @param session
     * @param mensagem
     */
    public static void logInfo(IapiSessionContextIfc session, String mensagem) {            
        Logger.info(Logger.createLogMessage(session).setLogMessage(mensagem));
    }
    /**
     *
     * Gera o um Logger de error
     * 
     * @param session
     * @param mensagem
     */
    public static void logError(IapiSessionContextIfc session, String mensagem) {            
        Logger.error(Logger.createLogMessage(session).setLogMessage(mensagem));
    }

    public static boolean possuiValor(Object o) {

        boolean ret = true;
        if (o == null) {
            ret = false;
        } else if (o instanceof Element) {
            // 
        } else if (o instanceof Collection) {
            Collection col = (Collection) o;
            if (col.isEmpty()) {
                ret = false;
            }
        } else if (o instanceof IBeanIfc) {
            IBeanIfc i = (IBeanIfc) o;
            if (i.getDisplayName() == null || i.getDisplayName().equalsIgnoreCase("none")) {
                ret = false;
            }
        } else if (o instanceof ObjectReferenceIfc) {
            // Renato Battaglia JAN 25 2013 - 
            // checagem de SimpleObjectReferenceIfc.isSet pode ser grande mudança!
            ObjectReferenceIfc i = (ObjectReferenceIfc) o;
            if (i.getDisplayName() == null || i.getDisplayName().equalsIgnoreCase("none")) {
                ret = false;
            } else if (i.getObjectId() == null) {
                ret = false;
            }
        } else if (o instanceof SimpleObjectReferenceIfc) {
            // Renato Battaglia JAN 25 2013 - 
            // checagem de SimpleObjectReferenceIfc.isSet pode ser grande mudança!
            SimpleObjectReferenceIfc ifc = (SimpleObjectReferenceIfc) o;
            if (ifc.getObjectId() == null) {
                //renato battaglia, checava errado! se != NULL! CORRIGIDO o IF acima!
                ret = false;
            }
        }
        return ret;
    }

    /**
     * @author Renato Battaglia
     * @param session
     * @param extField
     * @param extId
     * @since 2013Fev06
     *
     * @return Uma linha de master2, conteudo de um certo extField, localizado pelo extId
     * @throws ChainedException
     */
    public static String getSingleRowMaster2ExtFieldByExtId(IapiSessionContextIfc session, String extField, String extId)
            throws ChainedException {
        String retval = "";
        UserDefinedMasterData2IBeanIfc udmd2 = null;
        UserDefinedMasterData2IBeanHomeIfc udmd2Home = (UserDefinedMasterData2IBeanHomeIfc) IBeanHomeLocator.lookup(
                session, UserDefinedMasterData2IBeanHomeIfc.sHOME_NAME);
        udmd2 = udmd2Home.findByExternalId(extId);
        if (udmd2 != null) {
            retval = (String) udmd2.getExtensionField(extField).get();
        }
  
        return retval;
    }
}
