package it.dnd.thip.magic;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Response.Status;

import org.json.JSONArray;
import org.json.JSONObject;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.collector.BODataCollector;
import com.thera.thermfw.common.BusinessObject;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.Factory;

import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.vendite.documentoVE.DocumentoVendita;
import it.thera.thip.vendite.documentoVE.DocumentoVenditaTM;
import it.thera.thip.vendite.documentoVE.FatturaVendita;
import it.thera.thip.vendite.documentoVE.FatturaVenditaTM;
import it.thera.thip.vendite.generaleVE.ws.RicercaPrezzoEcomm;
import it.thera.thip.vendite.ordineVE.OrdineVendita;
import it.thera.thip.vendite.ordineVE.OrdineVenditaTM;

/**
 * Classe di servizio per l’interfaccia Magic.
 *
 * <p>Gestisce operazioni di interrogazione per portafogli di vendita (ordini, documenti, fatture)
 * restituendo risultati in formato JSON strutturato per l’integrazione REST.</p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Andrea Gatta<br>
 * Date: 24/07/2025
 * </p>
 */

/*
 * Revisions:
 * Number	Date		Owner	Description
 * 72066    24/07/2025  AGSOF3  Prima stesura
 */

public class MagicService {

	private static MagicService instance;

	public static MagicService getInstance() {
		if (instance == null) {
			instance = (MagicService) Factory.createObject(MagicService.class);
		}
		return instance;
	}

	/**
	 * Restituisce una lista paginata di documenti di vendita (ordini, documenti o fatture)
	 * filtrata per cliente, anno e numero documento.
	 *
	 * <p>Il risultato viene restituito in formato JSON con i dati e le informazioni di paginazione.
	 * L'oggetto restituito include {@code status} e {@code response}.</p>
	 *
	 * @param portafoglio tipo di documento da ricercare: "ordini", "documenti" o "fatture"
	 * @param idCliente identificativo del cliente
	 * @param anno anno del documento (opzionale)
	 * @param numero numero del documento (opzionale)
	 * @param page pagina da recuperare
	 * @param limit numero massimo di elementi per pagina
	 * @return oggetto {@link JSONObject} con i risultati e lo stato della risposta
	 */
	@SuppressWarnings({ "unchecked"})
	public JSONObject listaPortafoglioVendite(String portafoglio, String idCliente, String anno, String numero, int page, int limit) {
		JSONObject result = new JSONObject();
		JSONObject response = new JSONObject();
		Status status = Status.OK;
		JSONObject pagination = new JSONObject();

		int totalPages = 0;
		int totalRecords = 0;

		try {
			String where = "ID_AZIENDA = '" + Azienda.getAziendaCorrente() + "' ";
			String orderBy = "ID_AZIENDA";
			int offset = (page - 1) * limit;
			JSONArray elementi = new JSONArray();
			List<BusinessObject> lista = new ArrayList<>();
			String offsetLimit = " OFFSET "+offset+" ROWS FETCH NEXT "+limit+" ROWS ONLY ";
			String classHDR = "";
			switch (portafoglio.toLowerCase()) {
			case "ordini":
				classHDR = "OrdineVendita";
				where += " AND " + OrdineVenditaTM.R_CLIENTE + " = '"+idCliente+"' ";
				if(anno != null && numero != null) {
					where += " AND " + OrdineVenditaTM.ID_ANNO_ORDINE + " = '"+anno+"' ";
					where += " AND " + OrdineVenditaTM.ID_NUMERO_ORD + " = '"+numero+"' ";
				}
				lista = OrdineVendita.retrieveList(OrdineVendita.class, where , orderBy + offsetLimit, false);
				totalRecords = retrieveTotalRecords(OrdineVenditaTM.TABLE_NAME, where);
				break;
			case "documenti":
				classHDR = "DocumentoVendita";
				where += " AND " + DocumentoVenditaTM.R_CLIENTE + " = '"+idCliente+"' ";
				if(anno != null && numero != null) {
					where += " AND " + DocumentoVenditaTM.ID_ANNO_DOC + " = '"+anno+"' ";
					where += " AND " + DocumentoVenditaTM.ID_NUMERO_DOC + " = '"+numero+"' ";
				}
				lista = DocumentoVendita.retrieveList(DocumentoVendita.class, where, orderBy + offsetLimit, false);
				totalRecords = retrieveTotalRecords(DocumentoVenditaTM.TABLE_NAME, where);
				break;
			case "fatture":
				classHDR = "FatturaVendita";
				where += " AND " + FatturaVenditaTM.R_CLIENTE_FAT + " = '"+idCliente+"' ";
				if(anno != null && numero != null) {
					where += " AND " + FatturaVenditaTM.ID_ANNO_FAT + " = '"+anno+"' ";
					where += " AND " + FatturaVenditaTM.ID_NUMERO_FAT + " = '"+numero+"' ";
				}
				lista = FatturaVendita.retrieveList(FatturaVendita.class, where, orderBy + offsetLimit, false);
				totalRecords = retrieveTotalRecords(FatturaVenditaTM.TABLE_NAME, where);
				break;
			default:
				throw new IllegalArgumentException("Portafoglio non supportato");
			}
			totalPages = (int) Math.ceil((double) totalRecords / limit);

			for (BusinessObject obj : lista) {
				BODataCollector boDC = (BODataCollector) Factory.createObject(BODataCollector.class);
				boDC.initialize(classHDR);
				boDC.setBo(obj);				
				elementi.put(boDC.data());
			}

			pagination.put("currentPage", page);
			pagination.put("totalPages", totalPages);
			pagination.put("totalItems", totalRecords);
			pagination.put("itemsPerPage", limit);

			response.put("list", elementi);
			response.put("pagination", pagination);

		} catch (Exception e) {
			status = Status.INTERNAL_SERVER_ERROR;
			response.put("error", e.getMessage());
			e.printStackTrace();
		}

		result.put("status", status);
		result.put("response", response);
		return result;
	}

	/**
	 * Calcola il numero totale di record per una determinata tabella e clausola WHERE.
	 *
	 * @param tableName nome della tabella da interrogare
	 * @param where clausola WHERE da applicare (senza il prefisso "WHERE")
	 * @return numero totale di record trovati
	 */
	protected int retrieveTotalRecords(String tableName, String where) {
		where = " WHERE " + where;
		int total = 0; 
		String select = "SELECT COUNT(1) "
				+ " FROM " 
				+ tableName
				+ where;

		CachedStatement cs = null;
		ResultSet rs = null;
		try {
			cs = new CachedStatement(select);
			rs = cs.executeQuery();
			if(rs.next()) {
				total = rs.getInt(1);
			}
		}catch(SQLException e) {
			e.printStackTrace(Trace.excStream);
		}finally {
			try {
				if(rs != null)
					rs.close();
				if(cs != null)
					cs.free();
			}catch(SQLException e) {
				e.printStackTrace(Trace.excStream);
			}
		}
		return total;
	}

	@SuppressWarnings("unchecked")
	public JSONObject listaPrezzi(JSONArray items) {

	    JSONArray resultArray = new JSONArray();

	    for (int i = 0; i < items.length(); i++) {

	        JSONObject item = items.getJSONObject(i);

	        String idArticolo = item.optString("idArticolo");
	        String idCliente = item.optString("idCliente");

	        BigDecimal prezzo = BigDecimal.ZERO;
	        String error = null;

	        try {

	            RicercaPrezzoEcomm rp = new RicercaPrezzoEcomm();
	            rp.setCompany(Azienda.getAziendaCorrente());
	            rp.setUseAuthentication(false);

	            Map<String, Object> appParams = rp.getAppParams();
	            appParams.put("codCliente", idCliente);
	            appParams.put("codArticolo", idArticolo);
	            appParams.put("codListino", "395");

	            rp.setAppParams(appParams);

	            Map<String, Object> result = rp.send();

	            Object errorsObj = result.get("errors");

	            if (errorsObj instanceof List && !((List<?>) errorsObj).isEmpty()) {

	                Map<String, Object> errorMap = (Map<String, Object>) ((List<?>) errorsObj).get(0);
	                error = errorMap.values().iterator().next().toString();

	            } else {

	                BigDecimal p = (BigDecimal) result.get("prezzo");
	                if (p != null) {
	                    prezzo = p;
	                }
	            }

	        } catch (Exception e) {
	            error = e.getMessage();
	        }

	        JSONObject prezzoObj = new JSONObject();
	        prezzoObj.put("idArticolo", idArticolo);
	        prezzoObj.put("idCliente", idCliente);
	        prezzoObj.put("prezzo", prezzo);

	        if (error != null) {
	            prezzoObj.put("error", error);
	        }

	        resultArray.put(prezzoObj);
	    }

	    JSONObject response = new JSONObject();
	    response.put("count", resultArray.length());
	    response.put("prezzi", resultArray);

	    JSONObject result = new JSONObject();
	    result.put("status", Status.OK);
	    result.put("response", response);

	    return result;
	}

}