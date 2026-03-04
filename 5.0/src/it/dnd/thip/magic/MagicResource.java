package it.dnd.thip.magic;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.JSONObject;

import com.thera.thermfw.rs.BaseResource;


/**
 * Resource per la gestione dell'integrazione tra Panthera e Magic.
 *
 * <p>Espone endpoint REST per l'accesso a dati di portafoglio (ordini, documenti e fatture) tramite JAX-RS,
 * integrandosi con il servizio {@link MagicService}.</p>
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

@Path("/dnd")
public class MagicResource extends BaseResource {

	private MagicService service = MagicService.getInstance();
	
	/**
	 * Endpoint REST per ottenere una lista paginata di elementi del portafoglio vendite.
	 * <p>
	 * Il tipo di portafoglio può essere: {@code ordini}, {@code documenti}, o {@code fatture}.<br>
	 * I parametri anno e numero sono facoltativi e utilizzati per filtrare ulteriormente i risultati.
	 * </p>
	 *
	 * @param nomePortafoglio tipo di portafoglio da interrogare
	 * @param idCliente codice cliente su cui filtrare
	 * @param anno (opzionale) anno del documento
	 * @param numero (opzionale) numero del documento
	 * @param page numero di pagina da restituire (default: 1)
	 * @param limit numero massimo di elementi per pagina (default: 10)
	 * @return risposta HTTP contenente un JSON con i dati e lo stato dell'operazione
	 */
	@Path("/portafogli/list")
	@GET
	public Response listaProdotti(
			@QueryParam("portafoglio") String nomePortafoglio,
			@QueryParam("cliente") String idCliente,
			@QueryParam("anno") String anno,
			@QueryParam("numero") String numero,
			@QueryParam("page") @DefaultValue("1") int page,
			@QueryParam("limit") @DefaultValue("10") int limit) {
	
		JSONObject result = service.listaPortafoglioVendite(nomePortafoglio,idCliente,anno,numero, page,limit);
		Status status = (Status) result.get("status");
		JSONObject response = result.getJSONObject("response");
		return buildResponse(status, response);
	}
	
}