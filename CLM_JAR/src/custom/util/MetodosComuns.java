package custom.util;

import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiSessionContextIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListTypeIBeanIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.ValueListValueIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.common.db.NoConnectionException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 *
 * @author Tiago Rodrigues
 */
public class MetodosComuns {

    IapiSessionContextIfc session;

    public MetodosComuns(IapiSessionContextIfc session) {
        this.session = session;
    }
    
    public void logInfo(String LOGGER_ID, String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    public void logError(String LOGGER_ID, String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    /**
     * Recupera o EXTERNAL_ID de uma Value Liste Type
     *
     * @param session
     * @param externalId
     * @return
     * @throws ApplicationException
     * @throws DatabaseException
     */
    public int getValueListTypeObjIdByExtId(IapiSessionContextIfc session, String externalId)
            throws ApplicationException, DatabaseException {

        ValueListTypeIBeanHomeIfc vltHome = (ValueListTypeIBeanHomeIfc) IBeanHomeLocator.lookup(
                session, ValueListTypeIBeanHomeIfc.sHOME_NAME);
        List vltList = vltHome.findWhere("external_id = '" + externalId + "'");
        if (vltList.isEmpty()) {
            throw new ApplicationException("ListType '" + externalId + "' não encontrado");
        }
        ValueListTypeIBeanIfc vlt = (ValueListTypeIBeanIfc) vltList.iterator().next();

        return vlt.getObjectReference().getObjectId().intValue();
    }

    /**
     * Retorna uma classe de exceção com o conteudo de um Localized Resource
     *
     * @param session
     * @param resourceId
     * @param modifiers
     * @return
     */
    public ApplicationException getLocalizedApplicationException(
            IapiSessionContextIfc session, String resourceId, Object[] modifiers) {

        ApplicationException aEx = new ApplicationException(session,
                "tim.defdata", resourceId);
        if (modifiers != null && modifiers.length > 0) {
            aEx.setMessageModifiers(modifiers);
        }
        return aEx;
    }

    /**
     * Retorna um Value List Value através de seu Display Name Id e o Type Code do Value List Type
     *
     * @param displayNameId
     * @param typeCode
     * @return
     * @throws ApplicationException
     * @throws NoConnectionException
     * @throws SQLException
     * @throws DatabaseException
     */
    public ValueListValueIBeanIfc getValueListValueByTypeCode(String displayNameId, int typeCode)
            throws ApplicationException, NoConnectionException, SQLException, DatabaseException {

        ValueListValueIBeanIfc vlv = null;
        String sql;
        String schema = session.getDbHandle().getSchemaOwner();
        ResultSet resultSet;

        ValueListValueIBeanHomeIfc vlvHome = (ValueListValueIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, ValueListValueIBeanHomeIfc.sHOME_NAME);

        sql = "SELECT T2.OBJECTID FROM "
                + schema + ".FCI_MAS_VALUE_LIST_TYPE T1, "
                + schema + ".FCI_MAS_VALUE_LIST_VALUE T2 "
                + "WHERE "
                + "T1.OBJECTID = T2.PARENT_OBJECT_ID AND "
                + "T1.TYPE_CODE = " + typeCode + " AND "
                + "T2.DISPLAY_NAME = '" + displayNameId + "'";
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        resultSet = session.getDbHandle().getResultSet();
        if (resultSet.next()) {
            vlv = (ValueListValueIBeanIfc) vlvHome.findWhere("OBJECTID = "
                    + resultSet.getInt("OBJECTID")).iterator().next();
        }
        session.getDbHandle().endTransaction();

        return vlv;
    }
}
