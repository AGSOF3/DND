package it.dnd.thip.logis.lgb;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Vector;

import org.json.JSONArray;
import org.json.JSONObject;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.common.*;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.Column;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.ErrorCodes;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.type.DateType;
import com.thera.thermfw.type.EnumType;

import it.dnd.thip.base.azienda.YReparto;
import it.dnd.thip.logis.fis.TipoGestioneUbicazione;
import it.dnd.thip.logis.fis.YUbicazione;
import it.dnd.thip.toyota.transport.TransportState;
import it.dnd.thip.toyota.transport.TransportTM;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.cs.ColonneFiltri;
import it.thera.thip.logis.fis.CambioUbicazioneUdc;
import it.thera.thip.logis.fis.OperazioneSpostamentoUdc;
import it.thera.thip.logis.fis.Ubicazione;
import it.thera.thip.logis.fis.UbicazioneTM;

/**
 *
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 08/04/2025
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 71923    08/04/2025  DSSOF3   Prima stesura
 * 72036	07/07/2025	DSSOF3   Aggiunta metodo tipoGestioneMissioneTabellata
 * 72163	10/10/2025	AGSOF3	 findStatoGestione
 * 72188	30/10/2025	DSSOF3   Aggiunto metodo che verificare se esiste un trasporto in esecuzione per questo piano
 * 72197	07/11/2025	AGSOF3	 Find baia in synchro e settata subito in blocco da agv
 * 72224	25/11/2025	DSSOF3   Aggiungere metodo isPianoChiuso
 * 72326	09/02/2026	DSSOF3	 Metodo movimentaUdcManuale correzione nullPointer.
 * 72459	27/04/2026	AGSOF3	 Print errori
 */

public class YPianoCaricoToyota extends YPianoCaricoToyotaPO {

	private static final String STMT_TROVA_UBIC_PRL_FREE_AGV = "SELECT "
			+ "	TOP 1 * "
			+ "FROM "
			+ "	LOGIS.LUBICAZIONE l "
			+ "INNER JOIN THIPPERS.YLUBICAZIONE y "
			+ "ON "
			+ "	l.COD_MAG_FISICO = y.COD_MAG_FISICO "
			+ "	AND l.CODICE = y.CODICE "
			+ "WHERE "
			+ "	l.COD_ZONA_PRELIEVO = ?  AND l."+UbicazioneTM.STATO_PIENO+" = '"+Ubicazione.VUOTA+"' "
			+ "	AND y.BLOCCATA_AGV = '"+com.thera.thermfw.persist.Column.FALSE_CHAR+"' "
			+ "	AND y.TIPO_GESTIONE_PERS = '"+TipoGestioneUbicazione.AGV+"' ";
	public static CachedStatement cTrovaUbicazioneFreeAgv = new CachedStatement(STMT_TROVA_UBIC_PRL_FREE_AGV);

	//72188
	private static final String ESISTE_TRANSPORT_IN_CORSO = "SELECT "
			+ "	* "
			+ "FROM "
			+ "	"+TransportTM.TABLE_NAME+" "
			+ "WHERE "
			+ "	"+TransportTM.EXTERNAL_TRANSPORT_ID+" LIKE ? "
			+ "AND "+TransportTM.TRANSPORT_STATE+" IN ('"+TransportState.ONGOING.getId()+"','"+TransportState.PENDING.getId()+"')";
	public static CachedStatement cEsisteTransportInCorso = new CachedStatement(ESISTE_TRANSPORT_IN_CORSO);
	//72188

	public ErrorMessage checkDelete() {
		if(esisteTransportInCorso()) {
			return new ErrorMessage("BAS0000078","Impossibile cancellare il piano, esiste un trasporto AGV in corso");
		}
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Vector checkAll(BaseComponentsCollection components) {
		Vector errors = super.checkAll(components);
		if(isMovimentaUdcManuale()) {
			errors.addAll(controllaCondizioniMovimentoManuale());
		}
		return errors;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Vector controllaCondizioniMovimentoManuale() {
		Vector errors = new Vector();
		if(getStatoGestione() != TipoGestioneUbicazione.MULETTISTA) {
			errors.add(new ErrorMessage("BAS0000078","Lo stato gestione deve essere Mulettista"));
		}
		if(getStatoUdc() != StatoPrelievoUdcToyota.STATO_INIZIALE 
				&& getStatoUdc() != StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO) {
			errors.add(new ErrorMessage("BAS0000078","Lo stato UDC deve essere Stato Iniziale oppure Pronta per il Reintegro"));
		}
		return errors;
	}

	@Override
	public int save() throws SQLException {
		int rc = 0;
		rc = super.save();
		if(rc > 0 && isMovimentaUdcManuale()) {
			rc = movimentaUdcManuale();
		}
		return rc;
	}

	@SuppressWarnings("rawtypes")
	public int movimentaUdcManuale() throws SQLException {
		//String barcode = getIdCodUbicazionePrelievo();
		String ubicazioneSorgente = null;
		String ubicazioneDestinazione = null;
		if(getStatoUdc() == StatoPrelievoUdcToyota.STATO_INIZIALE) {
			setStatoUdc(StatoPrelievoUdcToyota.RICEVUTA);

			Iterator iterRighe = getRighe().iterator();
			while(iterRighe.hasNext()) {
				YPianoCaricoToyotaRiga riga = (YPianoCaricoToyotaRiga) iterRighe.next();
				riga.setPrelevabile(true);
			}
			ubicazioneSorgente = getIdCodiceUbicazioneStock();
			ubicazioneDestinazione = getIdCodUbicazionePrelievo();

		}else if(getStatoUdc() == StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO) {
			//barcode = getIdCodiceUbicazioneStock();
			setStatoUdc(StatoPrelievoUdcToyota.RIPOSIZIONATA);
			setSalvaRighe(false);

			ubicazioneSorgente = getIdCodUbicazionePrelievo();
			ubicazioneDestinazione = getIdCodiceUbicazioneStock();
		}
		Vector err = new Vector();
		CambioUbicazioneUdc cambioUbicazioneUdc = (CambioUbicazioneUdc) Factory.createObject(CambioUbicazioneUdc.class);
		if(getStatoGestione() == TipoGestioneUbicazione.AGV)
			cambioUbicazioneUdc.setCodiceOperatore("AGV");
		cambioUbicazioneUdc.setUbicazioneNew(Ubicazione.elementWithKey(KeyHelper.buildObjectKey(new String[] {
				"ST1",/*barcode*/ubicazioneDestinazione
		}), PersistentObject.NO_LOCK));
		//cambioUbicazioneUdc.setUbicazioneOld(getUbicazioneStock());
		cambioUbicazioneUdc.setUbicazioneOld(Ubicazione.elementWithKey(KeyHelper.buildObjectKey(new String[] {
				"ST1",/*barcode*/ubicazioneSorgente
		}), PersistentObject.NO_LOCK));
		cambioUbicazioneUdc.setOperazioneSpostamento((OperazioneSpostamentoUdc)OperazioneSpostamentoUdc.readOnlyElementWithKey(OperazioneSpostamentoUdc.class, KeyHelper.buildObjectKey(new String[] {
				"ST1","SPOSTA"
		})));
		cambioUbicazioneUdc.setMappaUdc(getUdc());
		cambioUbicazioneUdc.settaTipoMovimento();
		if(cambioUbicazioneUdc.getUbicazioneNew() != null
				&& cambioUbicazioneUdc.getUbicazioneOld() != null) {
			err = cambioUbicazioneUdc.fine();
			if(!err.isEmpty()) {
				for(ErrorMessage em : ((Vector<ErrorMessage>)err)) {
					Trace.println("Errore in it.dnd.thip.logis.lgb.YPianoCaricoToyota.movimentaUdcManuale(): " + em.getText());//72459 AGSOF3
				}
				return ErrorCodes.GENERIC_ERROR;
			}else {
				setMovimentaUdcManuale(false);
				save();
			}
		}
		return 1;
	}

	@Override
	public int saveOwnedObjects(int rc) throws SQLException {
		if (isSalvaRighe()){
			rc = iRighe.save(rc);
		}
		return rc;
	}

	@Override
	public String getAltreInfoHeader() {
		if(isOnDB()) {
			Date dataDocumento = getDataDocumento();
			if(getDataDocumento() != null) {
				String data = dataDocumento.toString();

				if(dataDocumento != null) {
					DateType type = Factory.newObject(DateType.class);
					data = type.objectToString(dataDocumento);
					data = type.format(data);
				}
				return "(" + data + ")";
			}
		}else
			return "";
		return "";
	}

	/**
	 * Legge il tipo di missione da una tabella realizzata da Arturo Cristiano, non e' presente nel progetto
	 * @param tipoGestioneReparto
	 * @param tipoGestioneUbicazione
	 * @return
	 */
	public static char tipoGestioneMissioneTabellata(char tipoGestioneReparto, char tipoGestioneUbicazione) {
		char ret = TipoGestioneUbicazione.DEFAULT;
		String where = "SELECT * FROM THIPPERS.Y_TIPO_GES WHERE TIPO_GES_REP = '"+tipoGestioneReparto+"' AND TIPO_GES_UBI = '"+tipoGestioneUbicazione+"' ";
		CachedStatement cs = null;
		ResultSet rs = null;
		try {
			cs = new CachedStatement(where);
			rs = cs.executeQuery();
			if(rs.next()) {
				ret = rs.getString("TIPO_GES_MIS").charAt(0);
			}
		}catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}finally {
			try {
				if(rs != null) {
					rs.close();
				}
				if(cs != null) {
					cs.free();
				}
			}catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
			}
		}
		return ret;
	}

	/**
	 * Determina il tipo di gestione di una determinata ubicazione in funzione del reparto.
	 *
	 * <p>
	 * Il metodo ricerca innanzitutto una configurazione specifica nella tabella
	 * {@link YStatoGestUbiRep}. Se trovata, ne restituisce il tipo di gestione associato.
	 * In caso contrario, viene determinato il tipo di gestione tabellato in base
	 * alla combinazione di {@code tipoGestionePers} di reparto e ubicazione.
	 * </p>
	 *
	 * @param ubicazione ubicazione di magazzino da analizzare
	 * @param reparto reparto di riferimento
	 * @return il carattere identificativo del tipo di gestione
	 * @throws SQLException in caso di errori di accesso al database
	 */
	public static char findStatoGestione(YUbicazione ubicazione, YReparto reparto) throws SQLException {
		char ret = TipoGestioneUbicazione.DEFAULT;
		String[] c = {Azienda.getAziendaCorrente(),ubicazione.getCodiceMagFisico(),ubicazione.getCodice(), reparto.getIdReparto()};
		String key = KeyHelper.buildObjectKey(c);		
		YStatoGestUbiRep statoGestioneSpecifica = YStatoGestUbiRep.elementWithKey(key, NO_LOCK);
		if(statoGestioneSpecifica != null) {
			ret = statoGestioneSpecifica.getTipoGestione();
		}else {
			ret = tipoGestioneMissioneTabellata(reparto.getTipoGestionePers(), ubicazione.getTipoGestionePers());
		}		
		return ret;
	}

	public String getServizioCodiceUbicazioneFrom() {
		String idCodUbicazione = null;
		if(getStatoUdc() == StatoPrelievoUdcToyota.STATO_INIZIALE
				|| getStatoUdc() == StatoPrelievoUdcToyota.RIPOSIZIONATA) {
			idCodUbicazione = getIdCodiceUbicazioneStock();
		}else if(getStatoUdc() == StatoPrelievoUdcToyota.RICHIESTA_A_TOYOTA
				|| getStatoUdc() == StatoPrelievoUdcToyota.RICEVUTA
				|| getStatoUdc() == StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO
				|| getStatoUdc() == StatoPrelievoUdcToyota.RICHIESTA_REINTEGRO_INVIATA) {
			idCodUbicazione = getIdCodUbicazionePrelievo();
		}
		return idCodUbicazione;
	}

	public String getServizioCodiceUbicazioneTo() {
		String idCodUbicazione = null;
		if(getStatoUdc() == StatoPrelievoUdcToyota.STATO_INIZIALE
				|| getStatoUdc() == StatoPrelievoUdcToyota.RIPOSIZIONATA) {
			idCodUbicazione = getIdCodUbicazionePrelievo();
		}else if(getStatoUdc() == StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO
				|| getStatoUdc() == StatoPrelievoUdcToyota.RICEVUTA
				|| getStatoUdc() == StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO
				|| getStatoUdc() == StatoPrelievoUdcToyota.RICHIESTA_REINTEGRO_INVIATA) {
			idCodUbicazione = getIdCodiceUbicazioneStock();
		}
		//..Se l'ubicazione e' vuota allora devo cercare la prima ubicaizone NON bloccata da AGV e darla come prelievo
		if(idCodUbicazione == null) {
			idCodUbicazione = trovaUbicazioneZonaPrelievoRepartoGestitaAgvLibera(getIdReparto());
		}
		return idCodUbicazione;
	}

	//AGSOF3 metodo messo in synchro, altrimenti potenzialmente mi trova n. volte la stessa ubicazione
	public synchronized static String trovaUbicazioneZonaPrelievoRepartoGestitaAgvLibera(String idReparto) {
		String idUbicazione = null;
		String idMagFisico = null;
		ResultSet rs = null;
		try{
			PreparedStatement ps = cTrovaUbicazioneFreeAgv.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, idReparto);
			rs = ps.executeQuery();
			if (rs.next()){
				idMagFisico = rs.getString(it.thera.thip.logis.fis.UbicazioneTM.CODICE_MAG_FISICO);
				idUbicazione = Column.rightTrim(rs.getString(it.thera.thip.logis.fis.UbicazioneTM.CODICE));
			}
			//AGOSF3 prima di passare l'ubicazione (baia) la metto bloccata da agv, committo già così chiunque sia in attesa
			//di questo synchro non può trovare la stessa baia
			if(idUbicazione != null) {
				YUbicazione ubi = (YUbicazione) YUbicazione.elementWithKey(idMagFisico + KeyHelper.KEY_SEPARATOR + idUbicazione, 0); 
				if(ubi != null) {
					try {
						ConnectionManager.pushConnection();
						ubi.setBloccataAgv(true);
						ubi.save();
						ConnectionManager.commit();
					} catch (Exception e) {
						e.printStackTrace(Trace.excStream);
						try {
							ConnectionManager.rollback();
						} catch (Exception ex) {
							ex.printStackTrace(Trace.excStream);
						}
					} finally {
						//AGSOF3 la connessione viene sempre rilasciata
						try {
							ConnectionManager.popConnection();
						} catch (Exception ex) {
							ex.printStackTrace(Trace.excStream);
						}
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}finally{
			try{
				if(rs != null)
					rs.close(); 
			}catch(SQLException e){
				e.printStackTrace(Trace.excStream);
			}
		}
		return idUbicazione;
	}

	public JSONObject jsonTransport(String idUbicazioneFrom, String idUbicazioneTo, String idLoad) {
		JSONObject json = new JSONObject();

		// externalTransportId
		String externalTransportId = KeyHelper.formatKeyString(getKey())+ColonneFiltri.SEP+KeyHelper.formatKeyString(KeyHelper.buildObjectKey(new String[] {
				idUbicazioneFrom,idUbicazioneTo
		}));
		json.put("externalTransportId", externalTransportId);

		// instructions array
		JSONArray instructions = new JSONArray();

		// Prima istruzione (Pickup)
		JSONObject pickup = new JSONObject();
		pickup.put("locationName", idUbicazioneFrom);
		pickup.put("action", "Pickup");

		JSONArray pickupLoads = new JSONArray();
		JSONArray pickupInnerArray = new JSONArray();
		pickupInnerArray.put(idLoad);
		pickupLoads.put(pickupInnerArray);

		pickup.put("loads", pickupLoads);

		// Seconda istruzione (Dropoff)
		JSONObject dropoff = new JSONObject();
		dropoff.put("locationName", idUbicazioneTo);
		dropoff.put("action", "Dropoff");

		JSONArray dropoffLoads = new JSONArray();
		JSONArray dropoffInnerArray = new JSONArray();
		dropoffInnerArray.put(idLoad);
		dropoffLoads.put(dropoffInnerArray);

		dropoff.put("loads", dropoffLoads);

		// Aggiungo entrambe le istruzioni
		instructions.put(pickup);
		instructions.put(dropoff);

		json.put("instructions", instructions);

		// metadata
		JSONObject metadata = new JSONObject();
		metadata.put("flow", "AtoB");

		json.put("metadata", metadata);

		return json;
	}

	public String descrizioneStatoUdc() {
		EnumType et = EnumType.getEnumTypeInstance("YStatoUdcPrelievoToyota", EnumType.class);
		return et.descriptionFromValue(String.valueOf(getStatoUdc()));
	}

	//72188
	public boolean esisteTransportInCorso() {
		ResultSet rs = null;
		try{
			PreparedStatement ps = cEsisteTransportInCorso.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, KeyHelper.formatKeyString(getKey())+"%");
			rs = ps.executeQuery();
			if (rs.next()){
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}finally{
			try{
				if(rs != null)
					rs.close(); 
			}catch(SQLException e){
				e.printStackTrace(Trace.excStream);
			}
		}
		return false;
	}
	//72188

	//72224
	@SuppressWarnings("rawtypes")
	public boolean isPianoChiuso() {
		Iterator iterRighePC = getRighe().iterator();
		while (iterRighePC.hasNext()) {
			YPianoCaricoToyotaRiga rPC = (YPianoCaricoToyotaRiga) iterRighePC.next();
			if (rPC.getStatoRiga() == StatoRigaToyota.APERTA) {
				return false;
			}
		}
		return true;
	}
	//72224

}