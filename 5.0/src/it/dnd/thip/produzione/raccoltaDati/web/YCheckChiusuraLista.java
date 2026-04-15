package it.dnd.thip.produzione.raccoltaDati.web;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.web.ServletEnvironment;
import com.thera.thermfw.web.servlet.BaseServlet;

import it.thera.thip.base.azienda.Azienda;

public class YCheckChiusuraLista extends BaseServlet{

	private static final long serialVersionUID = 1L;

	@Override
	protected void processAction(ServletEnvironment se) throws Exception {
		String idAzienda = Azienda.getAziendaCorrente();
		String codLista = se.getRequest().getParameter("codLista");
		List<Map<String, Object>> results = new ArrayList<>();
		String select = "SELECT\r\n"
				+ "	COALESCE(yupp.total_quantita,\r\n"
				+ "	0) AS QTA_CONFEZIONATA,\r\n"
				+ "	lr.QTA1_RIC AS QTA_RICHIESTA,\r\n"
				+ "	lr.QTA1_RIC - COALESCE(yupp.total_quantita,\r\n"
				+ "	0) AS QTA_RESIDUA,\r\n"
				+ "	lr.COD_ARTICOLO , \r\n"
				+ "	lr.DESC_MANUALE ,\r\n"
				+ "	CONCAT(RTRIM(lr.COD_LISTA), '/', lr.NUM_RIGA_HOST) AS CODICE_RIGA\r\n"
				+ "FROM\r\n"
				+ "	LOGIS.LLISTA_RIGA lr\r\n"
				+ "LEFT JOIN (\r\n"
				+ "	SELECT\r\n"
				+ "		ID_AZIENDA,\r\n"
				+ "		R_COD_LISTA,\r\n"
				+ "		R_COD_RIGA_LISTA,\r\n"
				+ "		SUM(QUANTITA) AS total_quantita\r\n"
				+ "	FROM\r\n"
				+ "		THIPPERS.YGESTIONE_UDS_PICKING_PROD\r\n"
				+ "	GROUP BY\r\n"
				+ "		ID_AZIENDA,\r\n"
				+ "		R_COD_LISTA,\r\n"
				+ "		R_COD_RIGA_LISTA\r\n"
				+ ") yupp\r\n"
				+ "ON\r\n"
				+ "	lr.COD_SOCIETA = yupp.ID_AZIENDA\r\n"
				+ "	AND lr.COD_LISTA = yupp.R_COD_LISTA\r\n"
				+ "	AND lr.CODICE = yupp.R_COD_RIGA_LISTA\r\n"
				+ "WHERE\r\n"
				+ "	lr.COD_SOCIETA = '"+idAzienda+"'\r\n"
				+ "	AND lr.COD_LISTA = '"+codLista+"';";
		try {
			CachedStatement cs = new CachedStatement(select);
			ResultSet rs = cs.executeQuery();
			while (rs.next()) {
			    Map<String, Object> row = new HashMap<>();
			    
			    row.put("qtaConfezionata", rs.getInt("QTA_CONFEZIONATA"));
			    row.put("qtaRichiesta", rs.getInt("QTA_RICHIESTA"));
			    row.put("qtaResidua", rs.getInt("QTA_RESIDUA"));
			    row.put("codArticolo", rs.getString("COD_ARTICOLO"));
			    row.put("descManuale", rs.getString("DESC_MANUALE"));
			    row.put("codiceRiga", rs.getString("CODICE_RIGA"));

			    results.add(row);
			}
		}catch(SQLException e) {
			e.printStackTrace(Trace.excStream);
		}    
		se.getResponse().setContentType("application/json");
		se.getResponse().getWriter().write(new Gson().toJson(results));
	}

}