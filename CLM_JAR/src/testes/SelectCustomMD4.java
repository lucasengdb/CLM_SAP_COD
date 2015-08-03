package testes;

import cmldummy.CLMDummyScriptDefinition;

// Teste
import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.api.common.exception.DatabaseException;
import com.sap.odp.common.db.NoConnectionException;
import com.sap.odp.common.platform.SessionContextIfc;
import com.sap.odp.common.types.Attachment;
import java.io.BufferedReader;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.io.File;
import java.io.FileReader;

/**
 *
 * @author Tiago Rodrigues
 */
public class SelectCustomMD4 extends CLMDummyScriptDefinition {

    ContractIBeanIfc doc;
    //
    //
    String sql = "";

    // QUERY TO HTML
    private void queryToHTML(String sql) throws
            IOException, SQLException, NoConnectionException, ApplicationException, DatabaseException {

        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        ResultSet rs = session.getDbHandle().getResultSet();

        ResultSetMetaData md = rs.getMetaData();
        String nomeTabela = md.getTableName(1);
        String dado = "";

        sql = sql
                .replace("SELECT ", "<b style='color:blue'> SELECT </b>")
                .replace(" FROM ", "<br /><b style='color:blue'> FROM </b>")
                .replace(" WHERE ", "<br /><b style='color:blue'> WHERE </b>")
                .replace(" ORDER ", "<br /><b style='color:blue'> ORDER </b>")
                .replace(" JOIN ", "<b style='color:blue'> JOIN </b>")
                .replace(" AND ", "<b style='color:blue'> AND </b>")
                .replace(" OR ", "<b style='color:blue'> OR </b>")
                .replace(" BY ", "<b style='color:blue'> BY </b>")
                .replace(" LEFT ", "<b style='color:blue'> LEFT </b>")
                .replace(" RIGTH ", "<b style='color:blue'> RIGTH </b>")
                .replace(" INNER ", "<b style='color:blue'> INNER </b>")
                .replace(" ON ", "<b style='color:blue'> ON </b>");

        // Abrindo código HTML
        StringBuilder strW = new StringBuilder();

        strW.append("<dic style='font-family:Consolas, Arial;font-size:11px'>");

        int count = md.getColumnCount();
        int numLinhas = 0;
        strW.append("<p><b> " + nomeTabela + " </b></p>");
        //strW.append("<p> " + sql + " </p>");
        strW.append("<table border='1' style='font-family:Consolas, Arial;font-size:11px'>");
        strW.append("<TR style='background-color:#EEEEEE'>");
        for (int i = 1; i <= count; i++) {
            strW.append("<TD>" + md.getColumnLabel(i) + "</TD>");
        }
        strW.append("</TR>");
        int loop = 1;
        while (rs.next()) {
            strW.append("<TR>");
            for (int i = 1; i <= count; i++) {
                if (hasValue(rs.getString(i))) {
                    dado = rs.getString(i);
                } else {
                    dado = "";
                }
                strW.append("<TD>" + dado + "</TD>");
            }
            strW.append("</TR>");
            numLinhas++;
            if (loop > 1000) {
                break;
            }
            loop++;
        }
        session.getDbHandle().endTransaction();
        strW.append("</table>");

        strW.append("<P>" + numLinhas + " registro(s) encontrado(s).</P>");
        strW.append("</div>");

        doc.getExtensionField("resultado").set(strW.toString());
    }

    public void inicio() throws ApplicationException, DatabaseException, SQLException {

        String linha = "";

        try {

            Attachment anexo = (Attachment) doc.getExtensionField("anexo").get();
            File arquivo = anexo.getFileData((SessionContextIfc) session);
            FileReader arqLeitura = new FileReader(arquivo);
            BufferedReader bufferedReader = new BufferedReader(arqLeitura);

            while ((linha = bufferedReader.readLine()) != null) {
                sql += "\n" + linha;
            }

            if (sql.length() > 0) {

                sql = sql.replace("<%SCHEMA%>", session.getDbHandle().getSchemaOwner());
                sql = sql.replace("<%RESULTS%>", "*");
                sql = sql.replace("<%ORDERBY%>", "");

                doc.getExtensionField("resultado").set(sql);

                queryToHTML(sql);
            }

            arqLeitura.close();
            bufferedReader.close();

        } catch (Exception e) {
            throw new ApplicationException(e.getMessage());
        }
    }
    //inicio();
}
