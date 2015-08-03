package testes;

import cmldummy.CLMDummyScriptDefinition;

// Scripts de testes
import com.sap.eso.api.contracts.AgreementIBeanIfc;
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.eso.api.doccommon.doc.contract.ContractDocumentIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import java.sql.ResultSet;
import com.sap.odp.api.common.exception.ChainedException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.common.platform.IapiAccountIfc;
import com.sap.odp.api.comp.messaging.MailTypeEnumType;
import com.sap.odp.api.doc.collaboration.CollaboratorIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanHomeIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorRoleIBeanIfc;
import com.sap.odp.api.doc.collaboration.CollaboratorTypeEnumType;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanHomeIfc;
import com.sap.odp.api.doccommon.masterdata.purchasing.BusinessUnitIBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.api.ibean.IBeanIfc;
import com.sap.odp.api.ibean.OrderedSubordinateCollectionIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanHomeIfc;
import com.sap.odp.api.usermgmt.masterdata.GroupIBeanIfc;
import com.sap.odp.api.util.NotificationUtil;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.db.ObjectReference;
import com.sap.odp.common.db.SimpleObjectReference;
import com.sap.odp.common.platform.SessionContextIfc;
import com.sap.odp.common.platform.context.SimpleUrlBuilder;
import com.sap.odp.doccommon.util.UrlBuilder;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import oracle.jdbc.driver.OracleDriver;

/**
 *
 * @author Tiago Rodrigues
 */
public class Testes extends CLMDummyScriptDefinition {

    private final String LOGGER_ID = "\n[DEBUG] ";
    ResultSet rs;

    private void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    public static int dataDiff(Date dataLow, Date dataHigh) {

        GregorianCalendar startTime = new GregorianCalendar();
        GregorianCalendar endTime = new GregorianCalendar();

        GregorianCalendar curTime = new GregorianCalendar();
        GregorianCalendar baseTime = new GregorianCalendar();

        startTime.setTime(dataLow);
        endTime.setTime(dataHigh);

        int dif_multiplier = 1;

        // Verifica a ordem de inicio das datas  
        if (dataLow.compareTo(dataHigh) < 0) {
            baseTime.setTime(dataHigh);
            curTime.setTime(dataLow);
            dif_multiplier = 1;
        } else {
            baseTime.setTime(dataLow);
            curTime.setTime(dataHigh);
            dif_multiplier = -1;
        }

        int result_years = 0;
        int result_months = 0;
        int result_days = 0;

        // Para cada mes e ano, vai de mes em mes pegar o ultimo dia para import acumulando  
        // no total de dias. Ja leva em consideracao ano bissesto  
        while (curTime.get(GregorianCalendar.YEAR) < baseTime.get(GregorianCalendar.YEAR)
                || curTime.get(GregorianCalendar.MONTH) < baseTime.get(GregorianCalendar.MONTH)) {

            int max_day = curTime.getActualMaximum(GregorianCalendar.DAY_OF_MONTH);
            result_months += max_day;
            curTime.add(GregorianCalendar.MONTH, 1);

        }

        // Marca que � um saldo negativo ou positivo  
        result_months = result_months * dif_multiplier;

        // Retirna a diferenca de dias do total dos meses  
        result_days += (endTime.get(GregorianCalendar.DAY_OF_MONTH) - startTime.get(GregorianCalendar.DAY_OF_MONTH));

        return result_years + result_months + result_days;
    }

    public static void connectOracle() throws IOException {
/*
    	BusinessUnitIBeanHomeIfc busUnitHome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
        .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);*/
    	
    	Testes t = new Testes();
    	try {
			t.testeUpdateDB();
		} catch (ApplicationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (DatabaseException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (NoConnectionException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SQLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
    	
        Connection connection = null;
        OracleDriver driver = new OracleDriver();
        Properties prop = new Properties();

        try {
            // Create a connection to the database  
            String server = "10.114.10.68";
            //String server = "10.174.230.246";
        	//server = "127.0.0.1";
            String portNumber = "1521";
            String sid = "D1C";
            String url = "jdbc:oracle:thin:@" + server + ":" + portNumber + ":" + sid;
            String username = "sys";
            String password = "SAPSR3DB";

            prop.put("user", username);
            prop.put("password", password);
            prop.put("internal_logon", "sysdba");

            System.out.println(url);
            connection = driver.connect(url, prop);

            System.out.println("Conex�o realizada com sucesso!!!");

        } catch (SQLException e) {
            System.out.println("SQLException: " + e.getMessage());
        }
    }

    public static void main(String args[]) throws IOException {
    	
    	connectOracle();
    }

    public  void testeUpdateDB() throws ApplicationException, DatabaseException, NoConnectionException, SQLException {

        ContractIBeanIfc doc = null;

        /////////////////////////////
        ContractIBeanIfc maAgr = null;

        BusinessUnitIBeanHomeIfc busUnitHome = (BusinessUnitIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, BusinessUnitIBeanHomeIfc.sHOME_NAME);
        BusinessUnitIBeanIfc busUnit = (BusinessUnitIBeanIfc) busUnitHome
                .findWhere("EXTERNAL_ID = 'QL'").iterator().next();

        ObjectReference objRef = new ObjectReference(1004, 297795585);

        maAgr = (ContractIBeanIfc) doc.getIBeanHomeIfc().find(objRef);

        if (hasValue(maAgr) && hasValue(busUnit)) {
            logInfo("MA Encontrato : " + maAgr.getDocumentId());
            maAgr.getIBeanHomeIfc().upgradeToEdit(maAgr);
            maAgr.setBusinessUnitRef(busUnit.getObjectReference());
            maAgr.getExtensionField("MA_GERENCIA").set(null);
            maAgr.getExtensionField("MA_AREA").set(null);
            maAgr.getIBeanHomeIfc().save(maAgr);
            maAgr.getIBeanHomeIfc().downgradeToView(maAgr);
            logInfo("MA : " + maAgr.getDocumentId() + " salvo com sucesso...");
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////
        try {
            session.getDbHandle().beginTransaction();
            session.getDbHandle().beginTransaction();
            String sql = "SELECT * FROM "
                    + "<%SCHEMA%>.FCI_CONTRACT T1, "
                    + "<%EXT_TABLE(contracts.Contract, coleta_assinaturas)%> T2 "
                    + "WHERE "
                    + "T1.OBJECTID = T2.PARENT_OBJECT_ID "
                    + "AND T2.PARENT_CLASS_ID = 1004 "
                    + "AND T1.CONTEXTID = <%CONTEXT(contracts.Contract)%> "
                    + "AND T1.OBJECTID = 1286078465";
            sql = "SELECT * FROM "
                    + "<%SCHEMA%>FCI_CONTRACT T1 "
                    + "WHERE "
                    + "T1.OBJECTID = 1286078465";
            session.getDbHandle().executeUpdate(sql);
            rs = session.getDbHandle().getResultSet();
            if (rs.next()) {
                logInfo("Encontrou registros.");
            } else {
                logInfo("N�o encontrou registros.");
            }
            session.getDbHandle().endTransaction();
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage());
        }

        ///////////////////////////////////////////////////////////////////////////////////////////////
        session.getDbHandle().beginTransaction();
        session.getDbHandle().beginTransaction();
        String sql = "UPDATE " + session.getDbHandle().getSchemaOwner() + ".FCI_DOC_DOC_SEC_TMPLT "
                + "SET DISPLAY_NAME = 'Ap�lice - Risk' "
                + "WHERE DISPLAY_NAME = 'Ap�lice - Rosk'; commit;";
        session.getDbHandle().executeUpdate(sql);
        session.getDbHandle().endTransaction();

        ////////////////////////////////////////////////////////////
        UrlBuilder urlBuilder = new UrlBuilder((SessionContextIfc) session);
        SimpleUrlBuilder simpleUrlBuilder = urlBuilder.setObject((SimpleObjectReference) doc.getObjectReference());

        throw new ApplicationException(" simpleUrlBuilder.toString() = " + simpleUrlBuilder.toString()
                + "\n getFromPortalParameter  " + urlBuilder.getFromPortalParameter()
                + "\n getPublicHost   " + urlBuilder.getPublicHost()
                + "\n getHook   " + urlBuilder.getHook()
                + "\n getPublicProtocol   " + urlBuilder.getPublicProtocol());

        //////////////////////////////////////
    }

    void enviarEmail() throws ChainedException, NoConnectionException, SQLException {

        // Enviando e-mails 
        Properties params = new Properties();
        params.put("TOKEN1", "ZzZz");
        params.put("TOKEN2", "Teste 1");
        params.put("TOKEN3", "Teste 2");

        String[] recipients = {"trodrigues_engineering@timbrasil.com.br"};

        IapiAccountIfc sender = session.getAccount();

        MailTypeEnumType mailTypeEnum = new MailTypeEnumType(MailTypeEnumType.ODP_CUSTOM_TEMPLATE1);

        NotificationUtil.sendNotification(recipients, sender, mailTypeEnum, params, null, null);

    }

    void addGrupoJuridSocietario() throws ApplicationException, DatabaseException {

        ContractIBeanIfc doc = null;

        GroupIBeanHomeIfc grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator.lookup(session, GroupIBeanHomeIfc.sHOME_NAME);
        GroupIBeanIfc grupo = grupoHome.findGroup("TIM.Juridico.Societario");

        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
        CollaboratorRoleIBeanIfc roleProprietarioIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Aprovador'").iterator().next();

        CollaboratorIBeanIfc collab = (CollaboratorIBeanIfc) doc.getCollaborators().create();

        collab.setPrincipal(grupo.getObjectReference());
        collab.setCollaboratorRole(roleProprietarioIfc.getObjectReference());

        doc.getCollaborators().add(collab);
    }

    void removeGrupoJuridSocietario() throws ApplicationException, DatabaseException {

        ContractIBeanIfc doc = null;

        GroupIBeanHomeIfc grupoHome = (GroupIBeanHomeIfc) IBeanHomeLocator.lookup(session, GroupIBeanHomeIfc.sHOME_NAME);
        GroupIBeanIfc grupo = grupoHome.findGroup("TIM.Juridico.Societario");

        int i = 0;
        for (i = 0; i < doc.getCollaborators().size(); i++) {

            CollaboratorIBeanIfc collab = (CollaboratorIBeanIfc) doc.getCollaborators().get(i);
            if (collab.getCollaboratorType().getValue() == CollaboratorTypeEnumType.group) {
                if (collab.getPrincipal().equals(grupo.getObjectReference())) {
                    doc.getCollaborators().delete(collab);
                    i = i - 1;
                }
            }
        }
    }

    private void adicionaAprovador() throws ApplicationException, ApplicationException, DatabaseException {

        ContractDocumentIBeanIfc doc = null;
        //
        //
        OrderedSubordinateCollectionIfc colabs;

        CollaboratorRoleIBeanHomeIfc roleHome = (CollaboratorRoleIBeanHomeIfc) IBeanHomeLocator
                .lookup(session, CollaboratorRoleIBeanHomeIfc.sHOME_NAME);
        CollaboratorRoleIBeanIfc roleProprietarioIfc = (CollaboratorRoleIBeanIfc) roleHome.findWhere(
                "DISPLAY_NAME = 'Propriet�rio'").iterator().next();

        // Colentando da classe pai do documento de contrato
        IBeanIfc parent = doc.getParentIBean();
        // Coletando os colaboradores no Master Agr ou Sub Agr
        if (parent instanceof ContractIBeanIfc) {
            colabs = ((ContractIBeanIfc) parent).getCollaborators();
        } else if (parent instanceof AgreementIBeanIfc) {
            colabs = ((AgreementIBeanIfc) parent).getCollaborators();
        } else {
            throw new ApplicationException("Colaboradores n�o encontrados.");
        }

    }
}
