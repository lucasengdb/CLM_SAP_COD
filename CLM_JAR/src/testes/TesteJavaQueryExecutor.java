/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package testes;

import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.common.db.DbColumnInfo;
import com.sap.odp.common.db.DbHandle;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.db.SimpleObjectReference;
import com.sap.odp.common.platform.SessionContextIfc;
import com.sap.odp.comp.query.ChartMatrixSeries;
import com.sap.odp.comp.query.QueryDefinitionBo;
import com.sap.odp.comp.query.QueryFilterInput;
import com.sap.odp.comp.query.QueryParamValue;
import com.sap.odp.comp.query.proc.AbsJavaQueryExecutor;
import com.sap.odp.comp.query.rs.CachedResultSetIfc;
import com.sap.odp.comp.query.rs.Row;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author t3343267
 */
public class TesteJavaQueryExecutor extends AbsJavaQueryExecutor {

    private ResultSet rs;

    public TesteJavaQueryExecutor(SessionContextIfc aSession) {
        super(aSession);
    }

    // Pertence ao SchemaBrowserAllTables    
    @Override
    public CachedResultSetIfc execute(
            QueryDefinitionBo aQuery, ChartMatrixSeries aChartMatrixSeries, Map aParams,
            QueryFilterInput aFilterInput, Integer aMaxRowsReturned, SimpleObjectReference aSourceReportObjRef)
            throws ApplicationException {

        super.execute(aQuery, aChartMatrixSeries, aParams, aFilterInput, aMaxRowsReturned, aSourceReportObjRef);
        SessionContextIfc session = getSessionContext();

        try {
            initialize(session, aQuery, aChartMatrixSeries);
            DbHandle dbHandle = session.getUserTransaction();
            dbHandle.beginTransaction();
            try {
                Set currTenantsDynTablesSet = new HashSet();
                if (!session.isSystemUser()) {
                    currTenantsDynTablesSet = getCurrTenantsDynTablesSet(dbHandle);
                }
                List tables = dbHandle.getDbTables();
                Iterator iter = tables.iterator();
                getResultSet().populateInit(session, aMaxRowsReturned);
                String tableName;
                while (iter.hasNext()) {
                    tableName = (String) iter.next();
                    if ((session.isSystemUser())
                            || (!tableName.startsWith("FCI_DYN_"))
                            || (currTenantsDynTablesSet.contains(tableName))) {
                        StringBuilder linkString = new StringBuilder(
                                "/analysis/report?queryGroupName=FCI-SchemaBrowserColumns&TableName=");
                        linkString.append(tableName).append("&link_crumb=true");
                        Object[] aObjList = {tableName, linkString.toString()};
                        Row aRow = new Row(aObjList);
                        getResultSet().populateAppend(aRow);
                    }
                }
                getResultSet().populateFinish();
                return getResultSet();
            } catch (Throwable t) {
                throw t;
            } finally {
                dbHandle.endTransaction();
            }
        } catch (ApplicationException ex) {
            throw ex;
        } catch (Error ex) {
            throw ex;
        } catch (Throwable ex) {
            ApplicationException e = new ApplicationException(session, ex);
            e.setMessageId("exception.comp.query.proc.java_executor_failed");
            e.setMessageModifiers(new Object[]{aQuery.getInternalName()});
            throw e;
        }
    }

    // Pertence ao SchemaBrowserColumns    
    public CachedResultSetIfc execute2(
            QueryDefinitionBo aQuery, ChartMatrixSeries aChartMatrixSeries, Map aParams, 
            QueryFilterInput aFilterInput, Integer aMaxRowsReturned, SimpleObjectReference aSourceReportObjRef)
            throws ApplicationException {
        
        super.execute(aQuery, aChartMatrixSeries, aParams, aFilterInput, aMaxRowsReturned, aSourceReportObjRef);
        SessionContextIfc session = getSessionContext();
        try {
            initialize(session, aQuery, aChartMatrixSeries);
            DbHandle dbHandle = session.getUserTransaction();
            dbHandle.beginTransaction();
            try {
                String tableName = (String) ((QueryParamValue) aFilterInput.getQueryParamValues().get(0)).getParamValue();
                List columns = dbHandle.getDbTableColumns(tableName);
                Iterator iter = columns.iterator();
                getResultSet().populateInit(session, aMaxRowsReturned);
                int oridinalPosition = 0;
                DbColumnInfo column;
                while (iter.hasNext()) {
                    column = (DbColumnInfo) iter.next();
                    oridinalPosition++;
                    String columnName = column.getName();
                    StringBuilder typeName = new StringBuilder();
                    String tmp = column.getTypeName();
                    typeName.append(tmp);
                    if (tmp.toUpperCase().indexOf("VARCHAR") >= 0) {
                        typeName.append("(").append(column.getLength()).append(")");
                    }
                    String isNullable = column.isNullable() ? "Y" : "N";
                    String columnDef = column.getDefault();
                    Object[] aObjList = {new BigDecimal(oridinalPosition), columnName, typeName.toString(), isNullable, columnDef};
                    Row aRow = new Row(aObjList);
                    getResultSet().populateAppend(aRow);
                }
                getResultSet().populateFinish();
                return getResultSet();
            } catch (Throwable t) {
                throw t;
            } finally {
                dbHandle.endTransaction();
            }
        } catch (ApplicationException ex) {
            throw ex;
        } catch (Error ex) {
            throw ex;
        } catch (Throwable ex) {
            ApplicationException e = new ApplicationException(session, ex);
            e.setMessageId("exception.comp.query.proc.java_executor_failed");
            e.setMessageModifiers(new Object[]{aQuery.getInternalName()});
            throw e;
        }
    }

    private String getNomeTabelaDinamica(SessionContextIfc session, String campoId, String collectionId, int classId)
            throws NoConnectionException, SQLException {

        String schema = session.getDbHandle().getSchemaOwner();
        String sql;
        String resultado = "";

        sql = "SELECT FCI_SYS_DYN_CLASSES.TABLE_NAME FROM "
                + schema + ".FCI_SYS_DYN_CLASSES, "
                + schema + ".FCI_SYS_DYN_MEMBERS "
                + "WHERE "
                + "FCI_SYS_DYN_CLASSES.OBJECTID = FCI_SYS_DYN_MEMBERS.PARENT_OBJECT_ID ";

        if (collectionId == null) {
            sql += "AND UPPER(FCI_SYS_DYN_MEMBERS.ATTRIBUTE_ID) = UPPER('" + campoId + "') "
                    + "AND FCI_SYS_DYN_CLASSES.ASSOC_CLASSID = " + classId;
        } else if (campoId == null) {
            sql += "AND UPPER(FCI_SYS_DYN_CLASSES.COLLECTION_NAME) = UPPER('" + collectionId + "') "
                    + "AND FCI_SYS_DYN_CLASSES.ASSOC_CLASSID = " + classId;
        } else {
            sql += "AND UPPER(FCI_SYS_DYN_MEMBERS.ATTRIBUTE_ID) = UPPER('" + campoId + "') "
                    + "AND UPPER(FCI_SYS_DYN_CLASSES.COLLECTION_NAME) = UPPER('" + collectionId + "') "
                    + "AND FCI_SYS_DYN_CLASSES.ASSOC_CLASSID = " + classId;
        }
        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        if (rs.next()) {
            resultado = rs.getString("TABLE_NAME");
        }
        session.getDbHandle().endTransaction();

        return resultado;
    }

    private Set getCurrTenantsDynTablesSet(DbHandle dbHandle) throws NoConnectionException, SQLException, ApplicationException {

        Set result = new HashSet();
        String currContextId = getContext("extension_defn");
        dbHandle.executeQuery("SELECT T2.TABLE_NAME "
                + "FROM "
                + "<%SCHEMA%>FCI_DOC_EXT_DEFN T1, "
                + "<%SCHEMA%>FCI_SYS_DYN_CLASSES T2 "
                + "WHERE T1.EXT_OBJECT_CLASSID = T2.ASSOC_CLASSID "
                + "AND T1.BUS_UNIT_CTXT_OBJECT_ID = T2.ASSOC_CONTEXT_OBJECT_ID "
                + "AND T1.CONTEXTID = " + currContextId);

        while (dbHandle.next()) {
            String tableName = dbHandle.getString(1);
            result.add(tableName);
        }
        return result;
    }

}
