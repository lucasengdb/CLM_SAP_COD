package scripts.calledscripts;

import cmldummy.CLMDummyScriptDefinition;
// Gera arquivo com usuários x perfil
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.api.common.log.Logger;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData3IBeanHomeIfc;
import com.sap.odp.api.doccommon.userdefined.UserDefinedMasterData3IBeanIfc;
import com.sap.odp.api.ibean.IBeanHomeLocator;
import com.sap.odp.common.db.NoConnectionException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 *
 * @author Tiago de Almeida Rodrigues
 *
 * Gera um arquivo contendo dados de usuários e seus perfis
 *
 */
public class RelatorioUser extends CLMDummyScriptDefinition {

    // Constantes do Script
    private final String LOGGER_ID = "\n[CLM_USER_PROFILES]\n";

    private void logInfo(String mensagem) {
        Logger.info(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void logError(String mensagem) {
        Logger.error(Logger.createLogMessage(session).setLogMessage(LOGGER_ID + mensagem));
    }

    private void geraUsersProfiles()
            throws SQLException, NoConnectionException, IOException, ApplicationException, DatabaseException {

        ResultSet rs;
        String sql = "";
        String login = "";
        String nomeCompleto = "";
        String matricula = "";
        String dataCriacao = "";
        String dataUltimoLogin = "";
        String perfil;
        int status;
        StringBuilder conteudoTXT = new StringBuilder();
        Calendar data = Calendar.getInstance();
        SimpleDateFormat dataFormatNomeArquivo = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat dataFormatConteudoArquivo = new SimpleDateFormat("dd/MM/yyyy");

        UserDefinedMasterData3IBeanHomeIfc udo3Home = (UserDefinedMasterData3IBeanHomeIfc) IBeanHomeLocator.lookup(
                session, UserDefinedMasterData3IBeanHomeIfc.sHOME_NAME);
        UserDefinedMasterData3IBeanIfc udo3 = (UserDefinedMasterData3IBeanIfc) udo3Home.findWhere(
                "EXTERNAL_ID = 'CLM_HOME'").iterator().next();

        if (!hasValue(udo3)) {
            logError("Não foi encontrado o registro na UDO3 \"CLM_HOME\".");
        }

        String dirHome = udo3.getDisplayName();

        String diretorio = dirHome ;
        String nomeArquivo = "CLM_USER_" + dataFormatNomeArquivo.format(data.getTime()) + ".txt";

        if (!new File(diretorio + "/" + nomeArquivo).exists()) {
            new File(diretorio + "/" + nomeArquivo).mkdirs();
        }

        File file = new File(diretorio + "/" + nomeArquivo);
        file.delete();
        FileWriter arquivo = new FileWriter(file, true);

        sql = "SELECT "
                + "FCI_UPP_USER_ACCOUNT.NAME AS LOGIN, "
                + "(FCI_UPP_USER_ACCOUNT.FIRST_NAME || ' ' || FCI_UPP_USER_ACCOUNT.LAST_NAME) AS NOME_COMPLETO, "
                + "FCI_UPP_USER_ACCOUNT.NAME AS MATRICULA, "
                + "FCI_UPP_USER_ACCOUNT.CREATED_AT AS DATA_CRIACAO, "
                + "FCI_UPP_USER_ACCOUNT.DATE_LAST_LOGGEDIN AS DATA_ULTIMO_LOGIN, "
                + "FCI_UPP_ROLE.DISPLAY_NAME AS PERFIL, "
                + "FCI_UPP_USER_ACCOUNT.INACTIVE AS STATUS "
                + "FROM SAPSR3DB.FCI_UPP_USER_ACCOUNT, SAPSR3DB.FCI_UPP_ROLE_REF, SAPSR3DB.FCI_UPP_ROLE "
                + "WHERE FCI_UPP_USER_ACCOUNT.OBJECTID = FCI_UPP_ROLE_REF.PARENT_OBJECT_ID "
                + "AND FCI_UPP_ROLE_REF.ROLE_OBJECT_ID = FCI_UPP_ROLE.OBJECTID "
                + "ORDER BY FCI_UPP_USER_ACCOUNT.NAME";

        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        rs = session.getDbHandle().getResultSet();
        while (rs.next()) {
            login = rs.getString("LOGIN").replace(";", "");
            nomeCompleto = rs.getString("NOME_COMPLETO").replace(";", "");
            matricula = rs.getString("MATRICULA").replace(";", "");
            dataCriacao = dataFormatConteudoArquivo.format(rs.getDate("DATA_CRIACAO"));
            if (hasValue(rs.getDate("DATA_ULTIMO_LOGIN"))) {
                dataUltimoLogin = dataFormatConteudoArquivo.format(rs.getDate("DATA_ULTIMO_LOGIN"));
            } else {
                dataUltimoLogin = "00/00/0000";
            }
            perfil = rs.getString("PERFIL").replace(";", "");
            status = rs.getInt("STATUS");

            if (status == 0) {
                status = 1;
            } else {
                status = 0;
            }

            conteudoTXT.append(login + ";" + nomeCompleto + ";" + matricula + ";" + dataCriacao + ";"
                    + dataUltimoLogin + ";" + perfil + ";" + status + "\n");
        }
        session.getDbHandle().endTransaction();

        if (hasValue(conteudoTXT)) {

            arquivo.write(conteudoTXT.toString());
            arquivo.close();

            logInfo("Arquivo gravado com sucesso. No diretorio \"" + dirHome + "\"");
        }
    }

    public void inicio() {

        try {
            geraUsersProfiles();
        } catch (Exception e) {
            logError(e.getMessage());
        }
    }
    //inicio();
}
