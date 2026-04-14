package it.dnd.thip.tuttoimballo.api;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.common.ErrorMessage;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.rs.errors.ErrorUtils;
import com.thera.thermfw.security.Security;

import it.dnd.thip.produzione.ordese.YGestioneUdsPickingProd;
import it.dnd.thip.produzione.ordese.YGestioneUdsPickingProdTM;
import it.dnd.thip.tuttoimballo.stampanti.YInterfStampanti;
import it.thera.thip.base.azienda.Azienda;

/**
 * Servizio per gestione api per Tutto per l'imballo
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Andrea Gatta<br>
 * Date: 03/09/2025
 * </p>
 */

/*
 * Revisions:
 * Number	Date		Owner	Description
 * 72106    03/09/2025  AGSOF3  Prima stesura    
 * 72435	13/04/2026	AGSOF3	In modalita ridotta non ho inizio rilevazione quindi la query per recuperare
 * 								la uds da pesare si basa ora sulla bilancia inserita nell operatore
 */

public class TuttoImballoService {

	private static final String STMT_RECUPERA_LAST_UDS = 
			"SELECT "
					+ "	U.* "
					+ "FROM "
					+ "	THIPPERS.YGESTIONE_UDS_PICKING_PROD U "
					+ "INNER JOIN THIP.PSN_DATI_PRD_UT P ON "
					+ "	P.ID_AZIENDA = U.ID_AZIENDA "
					+ "AND LEFT(U.R_UTENTE_CRZ, CHARINDEX('_', U.R_UTENTE_CRZ + '_') - 1) = P.ID_UTENTE_LGN	 "//72435
					+ "INNER JOIN THIPPERS.YDIPENDENTI Y ON "
					+ "	P.ID_AZIENDA = Y.ID_AZIENDA "
					+ "	AND P.OPERATORE_DEF = Y.ID_DIPENDENTE "
					+ "INNER JOIN THIPPERS.YBILANCIA_TUTTO_IMBALLO B ON "
					+ "	B.ID_AZIENDA = P.ID_AZIENDA "
					+ "	AND B.ID_BILANCIA = Y.R_BILANCIA_TI "
					+ "WHERE "
					+ "	STATO_UDS = '"+YGestioneUdsPickingProd.PACKING_COMPLETATO+"' "
					+ "	AND B.IP = ? AND U.ID_AZIENDA = ? "
					//+ "ORDER BY U.TIMESTAMP_AGG ASC";
					+ "ORDER BY U.TIMESTAMP_CRZ ASC";
	public static CachedStatement cSelectLastUds = new CachedStatement(STMT_RECUPERA_LAST_UDS);

	private static TuttoImballoService instance;

	public static TuttoImballoService getInstance() {
		if (instance == null) {
			instance = (TuttoImballoService) Factory.createObject(TuttoImballoService.class);
		}
		return instance;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JSONObject riceviPeso(String ip, BigDecimal peso, int progressivo) {
		JSONObject result = new JSONObject();
		JSONObject response = new JSONObject();
		Status status = Status.OK;
		Collection<ErrorMessage> errors = new Vector<ErrorMessage>();
		try {
			Security.openSession("ADMIN_001", "SOFTRE999");
			YGestioneUdsPickingProd uds = recuperaUdsPickingProd(Azienda.getAziendaCorrente(), ip);
			if(uds != null) {
				uds.setPesoUds(peso);
				uds.setStatoUds(YGestioneUdsPickingProd.PESO_RICEVUTO);

				int rc = uds.save();
				if(rc > 0) {
					Vector udsTutte = uds.listaUdsSameCodeSameList();

					//..In caso di UDS multiarticolo devo chiudere le altre senza peso
					//..il peso l'ho messo sulla prima e poi lo portero' sulla UDS testata
					for (Iterator iterator = udsTutte.iterator(); iterator.hasNext();) {
						YGestioneUdsPickingProd udsMA = (YGestioneUdsPickingProd) iterator.next();

						udsMA.setStatoUds(YGestioneUdsPickingProd.PESO_RICEVUTO);
						udsMA.save();
					}

					udsTutte.add(uds);

					//.Lancio stampa etichetta 2 (scrivo un record nella tabella di tutto x imballo)
					YInterfStampanti interfStampanti = uds.recordLoftwareCartoni(ip);

					rc = interfStampanti.save();

					if(rc > 0) {
						response.put("RecordStampante",interfStampanti.getKey());
						ConnectionManager.commit();
					}
					else {
						errors.add(new ErrorMessage("BAS0000078","Impossibile salvare record stampente, rc = "+rc));
						ConnectionManager.rollback();
					}
				}else {
					errors.add(new ErrorMessage("BAS0000078","Impossibile salvare UDS, rc = "+rc));
				}
			}else {
				status = Status.INTERNAL_SERVER_ERROR;
				errors.add(new ErrorMessage("BAS0000089","Nessuna UDS trovata"));
			}
		}catch(Exception e) {
			status = Status.INTERNAL_SERVER_ERROR;
			ErrorMessage em = new ErrorMessage("BAS0000078",e.getMessage());
			//response.put("errors", ErrorUtils.getInstance().toJSON(em));
			errors.add(em);
			e.printStackTrace(Trace.excStream);
		} finally {
//			ConnectionManager.popConnection();
			Security.closeSession();
		}
		response.put("errors", ErrorUtils.getInstance().toJSON(errors));
		result.put("status", status);
		result.put("response", response);
		return result;
	}

	public static char calcolaCheckDigit(String codice17) {
		if (codice17 == null || codice17.length() != 17 || !codice17.matches("\\d{17}")) {
			throw new IllegalArgumentException("Servono esattamente 17 cifre.");
		}

		int somma = 0;
		for (int i = 0; i < 17; i++) {
			int cifra = codice17.charAt(i) - '0';
			int peso = (i % 2 == 0) ? 3 : 1; // 1ª,3ª,5ª... -> 3 ; 2ª,4ª... -> 1
			somma += cifra * peso;
		}

		int resto = somma % 10;
		int check = (10 - resto) % 10; // “quanto manca alla prossima decina”
		return (char) ('0' + check);
	}


	public YGestioneUdsPickingProd recuperaUdsPickingProd(String idAzienda, String ip) throws SQLException {
		YGestioneUdsPickingProd uds = null;
		ResultSet rs = null;
		try{
			PreparedStatement ps = cSelectLastUds.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, ip);
			db.setString(ps, 2, idAzienda);
			rs = ps.executeQuery();
			if (rs.next()){
				uds = (YGestioneUdsPickingProd) YGestioneUdsPickingProd.elementWithKey(YGestioneUdsPickingProd.class, KeyHelper.buildObjectKey(new String[] {
						rs.getString(YGestioneUdsPickingProdTM.ID_AZIENDA),
						rs.getString(YGestioneUdsPickingProdTM.NUMERO_RITORNO),
						rs.getString(YGestioneUdsPickingProdTM.R_UDS),
						rs.getString(YGestioneUdsPickingProdTM.R_COD_LISTA),
						rs.getString(YGestioneUdsPickingProdTM.R_COD_RIGA_LISTA),
				}), PersistentObject.NO_LOCK);
			}
		}finally{
			try{
				if(rs != null)
					rs.close();
			}catch(SQLException e){
				e.printStackTrace(Trace.excStream);
			}
		}
		return uds;
	}

}