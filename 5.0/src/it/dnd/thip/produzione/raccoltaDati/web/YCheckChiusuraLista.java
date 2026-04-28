package it.dnd.thip.produzione.raccoltaDati.web;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.web.ServletEnvironment;
import com.thera.thermfw.web.servlet.BaseServlet;

import it.dnd.thip.produzione.raccoltaDati.YLogCheckChiuLista;
import it.thera.thip.base.azienda.Azienda;

/**
 * 
 * <p>
 * Company: Softre Solutions<br>
 * Author: Andrea Gatta<br>
 * Date: 27/04/2026
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 72457    27/04/2026  AGSOF3   Prima stesura
 */

public class YCheckChiusuraLista extends BaseServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void processAction(ServletEnvironment se) throws Exception {
		String idAzienda = Azienda.getAziendaCorrente();
		String codLista = se.getRequest().getParameter("codLista");
		List<Map<String, Object>> results = new ArrayList<>();
		String select = "SELECT\r\n" + "	COALESCE(yupp.total_quantita,\r\n" + "	0) AS QTA_CONFEZIONATA,\r\n"
				+ "	lr.QTA1_RIC AS QTA_RICHIESTA,\r\n" + "	lr.QTA1_RIC - COALESCE(yupp.total_quantita,\r\n"
				+ "	0) AS QTA_RESIDUA,\r\n" + "	lr.COD_ARTICOLO , \r\n" + "	lr.DESC_MANUALE ,\r\n"
				+ "	CONCAT(RTRIM(lr.COD_LISTA), '/', lr.NUM_RIGA_HOST) AS CODICE_RIGA, "
				+ " yupp.NUMERO_RITORNO,\r\n"
				+ "	CASE\r\n"
				+ "		WHEN yupp.NUMERO_RITORNO IS NULL\r\n"
				+ "		OR NOT EXISTS (\r\n"
				+ "		SELECT\r\n"
				+ "			1\r\n"
				+ "		FROM\r\n"
				+ "			THIP.DOC_PRD dp\r\n"
				+ "		WHERE\r\n"
				+ "			dp.NUM_RITORNO = yupp.NUMERO_RITORNO\r\n"
				+ "         )\r\n"
				+ "    THEN 'Y'\r\n"
				+ "		ELSE 'N'\r\n"
				+ "	END AS ATTIVITA_NON_DICHIARATA "
				+ "\r\n" + "FROM\r\n"
				+ "	LOGIS.LLISTA_RIGA lr\r\n" + "LEFT JOIN (\r\n" + "	SELECT\r\n" + "		ID_AZIENDA,\r\n"
				+ "		R_COD_LISTA,\r\n" + "		R_COD_RIGA_LISTA,\r\n" + "		SUM(QUANTITA) AS total_quantita,"
						+ "NUMERO_RITORNO \r\n"
				+ "	FROM\r\n" + "		THIPPERS.YGESTIONE_UDS_PICKING_PROD\r\n" + "	GROUP BY\r\n"
				+ "		ID_AZIENDA,\r\n" + "		R_COD_LISTA,\r\n" + "		R_COD_RIGA_LISTA, NUMERO_RITORNO\r\n" + ") yupp\r\n"
				+ "ON\r\n" + "	lr.COD_SOCIETA = yupp.ID_AZIENDA\r\n" + "	AND lr.COD_LISTA = yupp.R_COD_LISTA\r\n"
				+ "	AND lr.CODICE = yupp.R_COD_RIGA_LISTA\r\n" + "WHERE\r\n" + "	lr.COD_SOCIETA = '" + idAzienda
				+ "'\r\n" + "	AND lr.COD_LISTA = '" + codLista + "';";
		try {
			CachedStatement cs = new CachedStatement(select);
			ResultSet rs = cs.executeQuery();
			while (rs.next()) {
				Map<String, Object> row = new HashMap<>();

				row.put("qtaConfezionata", rs.getBigDecimal("QTA_CONFEZIONATA"));
				row.put("qtaRichiesta", rs.getBigDecimal("QTA_RICHIESTA"));
				row.put("qtaResidua", rs.getBigDecimal("QTA_RESIDUA"));
				row.put("codArticolo", rs.getString("COD_ARTICOLO"));
				row.put("descManuale", rs.getString("DESC_MANUALE"));
				row.put("codiceRiga", rs.getString("CODICE_RIGA"));
				row.put("attivitaNonDichiarata", rs.getString("ATTIVITA_NON_DICHIARATA"));
				results.add(row);
			}

			for (Map<String, Object> row : results) {
				YLogCheckChiuLista log = (YLogCheckChiuLista) Factory.createObject(YLogCheckChiuLista.class);
				log.setRifRigaLista((String) row.get("codiceRiga"));
				log.setRArticolo((String) row.get("codArticolo"));
				log.setQtaResidua((BigDecimal) row.get("qtaResidua"));
				log.setQtaConfezionata((BigDecimal) row.get("qtaConfezionata"));
				log.setQtaRichiesta((BigDecimal) row.get("qtaRichiesta"));
				BigDecimal residua = (BigDecimal) row.get("qtaResidua");

				boolean anomalia = 
				    (residua != null && residua.compareTo(BigDecimal.ZERO) != 0)
				    || "Y".equals(row.get("attivitaNonDichiarata"));
				
				log.setAnomalia(anomalia);
				log.save();
			}
			ConnectionManager.commit();
		} catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}

		se.getResponse().setContentType("application/json");
		se.getResponse().getWriter().write(new Gson().toJson(results));
	}

}