package testes;

import com.sap.eso.api.contracts.ContractIBeanIfc;
import com.sap.odp.api.common.exception.ApplicationException;
import com.sap.odp.common.db.NoConnectionException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import cmldummy.CLMDummyScriptDefinition;

/**
 *
 * @author Tiago Rodrigues
 */
public class SelectDB extends CLMDummyScriptDefinition {

    ContractIBeanIfc doc;
    //
    //
    String sql;
    String caminho;

    // QUERY TO HTML
    private void queryToHTML(String caminhoDiretorio, String sql) throws IOException, SQLException, NoConnectionException {

        session.getDbHandle().beginTransaction();
        session.getDbHandle().executeQuery(sql);
        ResultSet rs = session.getDbHandle().getResultSet();

        ResultSetMetaData md = rs.getMetaData();
        String nomeTabela = md.getTableName(1);

        //Criação de um buffer para a escrita em uma stream
        BufferedWriter strW = new BufferedWriter(
                new FileWriter(caminhoDiretorio + "/" + nomeTabela + ".html"));

        sql = sql.toUpperCase();
        sql = sql
                .replace("SELECT", "<b style='color:blue'> SELECT </b>")
                .replace(" FROM ", "<br /><b style='color:blue'> FROM </b>")
                .replace(" WHERE ", "<br /><b style='color:blue'> WHERE </b>")
                .replace(" ORDER ", "<br /><b style='color:blue'> ORDER </b>")
                .replace(" JOIN ", "<b style='color:blue'> JOIN </b>")
                .replace(" AND ", "<b style='color:blue'> AND </b>")
                .replace(" OR ", "<b style='color:blue'> OR </b>")
                .replace(" BY ", "<b style='color:blue'> BY </b>")
                .replace(" LEFT ", "<b style='color:blue'> LEFT </b>")
                .replace(" RIGTH ", "<b style='color:blue'> RIGTH </b>")
                .replace(" INNER JOIN ", "<b style='color:blue'> INNER JOIN </b>")
                .replace(" ON ", "<b style='color:blue'> ON </b>");

        // Abrindo código HTML
        strW.write("<HTML>"
                + "<head><title>" + nomeTabela + "</title></head>"
                + "<BODY style='font-family:Consolas, Arial;font-size:11px'>");

        int count = md.getColumnCount();
        int numLinhas = 0;
        strW.write("<p><B> " + nomeTabela + " </B></p>");
        strW.write("<p> " + sql + " </p>");
        strW.write("<TABLE border='1' style='font-family:Consolas, Arial;font-size:11px'>");
        strW.write("<TR style='background-color:#EEEEEE'>");
        for (int i = 1; i <= count; i++) {
            strW.write("<TD>" + md.getColumnLabel(i) + "</TD>");
        }
        strW.write("</TR>");
        while (rs.next()) {
            strW.write("<TR>");
            for (int i = 1; i <= count; i++) {
                strW.write("<TD>" + rs.getString(i) + "</TD>");
            }
            strW.write("</TR>");
            numLinhas++;
        }
        session.getDbHandle().endTransaction();
        strW.write("</TABLE>");

        strW.write("<P>" + numLinhas + " registro(s) encontrado(s).</P>");

        // Fechando código HTML
        strW.write("</BODY></HTML>");

        //Fechamos o buffer, término da gravação do arquivo
        strW.close();
    }

    private void inicio() throws ApplicationException {

        sql = "SELECT * FROM " + session.getDbHandle().getSchemaOwner() + "." + doc.getDocumentDescription();
        
        caminho = "E:/usr/sap/iface/SAP_CLM/D1C";

        try {
            queryToHTML(caminho, sql);
        } catch (Exception e) {
            throw new ApplicationException(e.getMessage());
        }
    }
}
