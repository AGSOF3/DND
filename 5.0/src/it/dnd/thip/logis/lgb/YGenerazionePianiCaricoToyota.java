package it.dnd.thip.logis.lgb;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thera.thermfw.ad.ClassADCollection;
import com.thera.thermfw.ad.ClassADCollectionManager;
import com.thera.thermfw.base.TimeUtils;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.batch.BatchJob;
import com.thera.thermfw.batch.BatchRunnable;
import com.thera.thermfw.collector.BODataCollector;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.Column;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.rs.errors.ErrorUtils;
import com.thera.thermfw.security.Authorizable;

import it.dnd.thip.agv.YSchedulazioneBufferAgv;
import it.dnd.thip.base.azienda.YReparto;
//import it.dnd.thip.base.azienda.YReparto;
import it.dnd.thip.logis.fis.TipoGestioneUbicazione;
import it.dnd.thip.logis.fis.YUbicazione;
import it.dnd.thip.produzione.ordese.YAttivitaEsecutivaTM;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.documenti.web.DocumentoDataCollector;
import it.thera.thip.base.generale.IntegrazioneThipLogis;
import it.thera.thip.cs.DatiComuniEstesi;
import it.thera.thip.cs.ThipException;
import it.thera.thip.logis.fis.Missione;
import it.thera.thip.logis.fis.MissioneTM;
import it.thera.thip.produzione.ordese.AttivitaEsecMateriale;
import it.thera.thip.produzione.ordese.AttivitaEsecMaterialeTM;
import it.thera.thip.produzione.ordese.AttivitaEsecutivaTM;
import it.thera.thip.produzione.ordese.OrdineEsecutivo;
import it.thera.thip.vendite.documentoVE.DocumentoVenRigaPrm;
import it.thera.thip.vendite.documentoVE.DocumentoVenditaRigaTM;

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
 * Number   Date        Owner   Description
 * 71923    08/04/2025  DSSOF3  Prima stesura
 * 71923	29/04/2025	AGSOF3	Esclusione ubicazioni con tipo gestione pers = default
 * 72036	07/07/2025	DSSOF3	Fix tecnica
 * 72108    04/09/2025  DSSOF3	Generazione piani solo per le attivita' esecutive collegate a logistica
 * 72163	10/10/2025	AGSOF3	Ricerca stato gestione specifico per ubicaz/reparto
 * 72177	20/10/2025	AGSOF3	Set ricevuto su piani in stato gestione operatore
 * 72224	25/11/2025	DSSOF3  Rendere pubblico metodo createDataCollector
 * 72301	19/01/2026	AGSOF3	Aggiunta where per escludere completamente righe COL_LOGISTICA = 'N'
 * 72326	30/01/2026	DSSOF3	Escludere ordini esecutivi, ordini vendita e documento vendita righe non valide.
 * 72589	27/07/2026	AGSOF3	Generazione righe piano carico solo se a saldo qta richiesta atv esec materiale
 */

public class YGenerazionePianiCaricoToyota extends BatchRunnable implements Authorizable {

	public static final String STMT_PIANI_CARICO_TOYOTA_TES = "SELECT "
			+ "	PIANI_PRL_TES.ID_AZIENDA, "
			+ "	LMISSIONE.COD_MAPPA_UDC, "
			+ "	LMISSIONE.COD_MAG_FISICO, "
			+ "	LMISSIONE.COD_UBICAZIONE, "
			+ "	ORD_ESEC_ATV.R_REPARTO, "
			+ "	SUM(COALESCE(LMISSIONE.QTA1_RIC,0)), "
			+ "	SUM(LLISTA_RIGA.QTA1_RIC) "
			+ "FROM "
			+ "	THIP.PIANI_PRL_TES PIANI_PRL_TES "
			+ "INNER JOIN LOGIS.LLISTA_TESTA LLISTA_TESTA ON "
			+ "	LLISTA_TESTA.COD_SOCIETA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND LLISTA_TESTA.CODICE = PIANI_PRL_TES.R_COD_LISTA "
			+ "INNER JOIN LOGIS.LLISTA_RIGA LLISTA_RIGA ON "
			+ "	LLISTA_RIGA.COD_SOCIETA = LLISTA_TESTA.COD_SOCIETA "
			+ "	AND LLISTA_RIGA.COD_LISTA = LLISTA_TESTA.CODICE "
			+ "LEFT OUTER JOIN LOGIS.LMISSIONE LMISSIONE ON "
			+ "	LLISTA_RIGA.COD_SOCIETA = LMISSIONE.COD_SOCIETA "
			+ "	AND LLISTA_RIGA.COD_LISTA = LMISSIONE.COD_LISTA "
			+ "	AND LLISTA_RIGA.CODICE = LMISSIONE.COD_RIGA_LISTA "
			+ "INNER JOIN THIP.PIANI_PRL_RIG PIANI_PRL_RIG ON "
			+ "	PIANI_PRL_RIG.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND PIANI_PRL_RIG.ID_MODELLO_PRL = PIANI_PRL_TES.ID_MODELLO_PRL "
			+ "	AND PIANI_PRL_RIG.TIPO_MODELLO_PRL = PIANI_PRL_TES.TIPO_MODELLO_PRL "
			+ "	AND PIANI_PRL_RIG.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND PIANI_PRL_RIG.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND PIANI_PRL_RIG.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "	AND PIANI_PRL_RIG.ID_RIGA_MATERIALE = LLISTA_RIGA.NUM_RIGA_HOST "
			+ "INNER JOIN THIP.ORD_ESEC_ATV ORD_ESEC_ATV ON "
			+ "	ORD_ESEC_ATV.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND ORD_ESEC_ATV.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND ORD_ESEC_ATV.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND ORD_ESEC_ATV.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "INNER JOIN "+YAttivitaEsecutivaTM.TABLE_NAME_EXT+" YORD_ESEC_ATV ON "
			//72108
			+ "	YORD_ESEC_ATV.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND YORD_ESEC_ATV.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND YORD_ESEC_ATV.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND YORD_ESEC_ATV.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			//72108
			+ "INNER JOIN THIPPERS.YLUBICAZIONE LUBI ON "//AGSOF3 aggiunta JOIN su THIPPERS.YLUBICAZIONE
			+ "	LUBI.COD_MAG_FISICO = LMISSIONE.COD_MAG_FISICO "
			+ "	AND LUBI.CODICE = LMISSIONE.COD_UBICAZIONE "
			+ "INNER JOIN THIPPERS.YREPARTI REP ON  "
			+ "	REP.ID_AZIENDA = ORD_ESEC_ATV.ID_AZIENDA "
			+ "	AND REP.ID_REPARTO = ORD_ESEC_ATV.R_REPARTO "
			+ "WHERE "
			+ "	PIANI_PRL_TES.ID_AZIENDA = ? "
			+ "	AND LLISTA_TESTA.COD_TIPO_LISTA IN('P/PRE','P/PREC') " //Fix 72036 aggiungere IN e rimuovere parametro ?
			+ "	AND LMISSIONE.COD_MAPPA_UDC IS NOT NULL "
			+ " AND LUBI.TIPO_GESTIONE_PERS <> '"+TipoGestioneUbicazione.DEFAULT+"' " //AGSOF3 scarto le ubicazioni con tipo gestione default
			+ " AND REP.SERVITO_LOGISTICA = '"+Column.TRUE_CHAR+"' " 
			+ " AND YORD_ESEC_ATV."+YAttivitaEsecutivaTM.COL_LOGISTICA+" = '"+Column.TRUE_CHAR+"' " //72108
			+ " AND LMISSIONE."+MissioneTM.STATO_MISSIONE+" NOT IN ('"+Missione.TERMINATA+"','"+Missione.ESECUZIONE+"') "
			+ "GROUP BY "
			+ "	PIANI_PRL_TES.ID_AZIENDA, "
			+ "	LMISSIONE.COD_MAPPA_UDC, "
			+ "	LMISSIONE.COD_MAG_FISICO, "
			+ "	LMISSIONE.COD_UBICAZIONE, "
			+ "	ORD_ESEC_ATV.R_REPARTO ";
	//			+ "HAVING SUM(COALESCE(LMISSIONE.QTA1_RIC, 0)) = SUM(LLISTA_RIGA.QTA1_RIC) AND (SUM(COALESCE(LMISSIONE.QTA1_RIC, 0)) - SUM(COALESCE(LMISSIONE.QTA1_EVASA, 0))) > 0 ";
	public CachedStatement cSelectPianiCaricoTyotaTes = new CachedStatement(STMT_PIANI_CARICO_TOYOTA_TES);

	public static final String STMT_PIANO_CARICO_TOYOTA_APERTO = "SELECT "
			+ "P.ID_AZIENDA,P.ID_ANNO_DOC,P.ID_NUMERO_DOC "
			+ "FROM "
			+ "THIPPERS.YPIANO_CARICO_TOYOTA_TES P "
			+ "WHERE P.ID_AZIENDA = ? "
			+ "AND P.R_REPARTO = ? "
			+ "AND P.R_COD_MAPPA_UDC = ? "
			+ "AND P.STATO_UDC <= ? ";
	protected static CachedStatement cSelectPianoCaricoToyotaAperto = new CachedStatement(STMT_PIANO_CARICO_TOYOTA_APERTO);

	public static final String STMT_SELECT_MISSIONI_FROM_CHIAVE_LOGICA_PIANO_CARICO = "SELECT "
			+ "	LMISSIONE.COD_MAG_FISICO , "
			+ "	LMISSIONE.CODICE "
			+ "FROM "
			+ "	THIP.PIANI_PRL_TES PIANI_PRL_TES "
			+ "INNER JOIN LOGIS.LLISTA_TESTA LLISTA_TESTA ON "
			+ "	LLISTA_TESTA.COD_SOCIETA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND LLISTA_TESTA.CODICE = PIANI_PRL_TES.R_COD_LISTA "
			+ "INNER JOIN LOGIS.LLISTA_RIGA LLISTA_RIGA ON "
			+ "	LLISTA_RIGA.COD_SOCIETA = LLISTA_TESTA.COD_SOCIETA "
			+ "	AND LLISTA_RIGA.COD_LISTA = LLISTA_TESTA.CODICE "
			+ "LEFT OUTER JOIN LOGIS.LMISSIONE LMISSIONE ON "
			+ "	LLISTA_RIGA.COD_SOCIETA = LMISSIONE.COD_SOCIETA "
			+ "	AND LLISTA_RIGA.COD_LISTA = LMISSIONE.COD_LISTA "
			+ "	AND LLISTA_RIGA.CODICE = LMISSIONE.COD_RIGA_LISTA "
			+ "INNER JOIN THIP.PIANI_PRL_RIG PIANI_PRL_RIG ON "
			+ "	PIANI_PRL_RIG.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND PIANI_PRL_RIG.ID_MODELLO_PRL = PIANI_PRL_TES.ID_MODELLO_PRL "
			+ "	AND PIANI_PRL_RIG.TIPO_MODELLO_PRL = PIANI_PRL_TES.TIPO_MODELLO_PRL "
			+ "	AND PIANI_PRL_RIG.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND PIANI_PRL_RIG.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND PIANI_PRL_RIG.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "	AND PIANI_PRL_RIG.ID_RIGA_MATERIALE = LLISTA_RIGA.NUM_RIGA_HOST "
			+ "INNER JOIN THIP.ORD_ESEC_ATV ORD_ESEC_ATV ON "
			+ "	ORD_ESEC_ATV.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND ORD_ESEC_ATV.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND ORD_ESEC_ATV.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND ORD_ESEC_ATV.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "INNER JOIN THIPPERS.YORD_ESEC_ATV YORD_ESEC_ATV ON "
			+ "	YORD_ESEC_ATV.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND YORD_ESEC_ATV.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND YORD_ESEC_ATV.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND YORD_ESEC_ATV.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "WHERE "
			+ "	PIANI_PRL_TES.ID_AZIENDA = ? "
			+ "	AND LLISTA_TESTA.COD_TIPO_LISTA IN('P/PRE','P/PREC') "
			+ "	AND LMISSIONE.COD_UBICAZIONE IS NOT NULL "
			+ "	AND LMISSIONE.COD_MAPPA_UDC = ? "
			+ "	AND ORD_ESEC_ATV.R_REPARTO = ? "
			+ " AND YORD_ESEC_ATV.COL_LOGISTICA = 'Y' "//72301 aggiunta where
			+ "GROUP BY "
			+ "	LMISSIONE.COD_MAG_FISICO , "
			+ "	LMISSIONE.CODICE ";
	protected static CachedStatement cSelectMissioniChiaveLogicaPianoCarico = new CachedStatement(STMT_SELECT_MISSIONI_FROM_CHIAVE_LOGICA_PIANO_CARICO);

	public static final String STMT_SELECT_ORDESEC_ATV_MAT_FROM_MISSIONE = "SELECT "
			+ "	ORD_ESEC_ATV.ID_AZIENDA, "
			+ "	ORD_ESEC_ATV.ID_ANNO_ORD, "
			+ "	ORD_ESEC_ATV.ID_NUMERO_ORD, "
			+ "	ORD_ESEC_ATV.ID_RIGA_ATTIVITA, "
			+ "	PIANI_PRL_RIG.ID_RIGA_MATERIALE "
			+ "FROM "
			+ "	THIP.PIANI_PRL_TES PIANI_PRL_TES "
			+ "INNER JOIN LOGIS.LLISTA_TESTA LLISTA_TESTA ON "
			+ "	LLISTA_TESTA.COD_SOCIETA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND LLISTA_TESTA.CODICE = PIANI_PRL_TES.R_COD_LISTA "
			+ "INNER JOIN LOGIS.LLISTA_RIGA LLISTA_RIGA ON "
			+ "	LLISTA_RIGA.COD_SOCIETA = LLISTA_TESTA.COD_SOCIETA "
			+ "	AND LLISTA_RIGA.COD_LISTA = LLISTA_TESTA.CODICE "
			+ "LEFT OUTER JOIN LOGIS.LMISSIONE LMISSIONE ON "
			+ "	LLISTA_RIGA.COD_SOCIETA = LMISSIONE.COD_SOCIETA "
			+ "	AND LLISTA_RIGA.COD_LISTA = LMISSIONE.COD_LISTA "
			+ "	AND LLISTA_RIGA.CODICE = LMISSIONE.COD_RIGA_LISTA "
			+ "INNER JOIN THIP.PIANI_PRL_RIG PIANI_PRL_RIG ON "
			+ "	PIANI_PRL_RIG.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND PIANI_PRL_RIG.ID_MODELLO_PRL = PIANI_PRL_TES.ID_MODELLO_PRL "
			+ "	AND PIANI_PRL_RIG.TIPO_MODELLO_PRL = PIANI_PRL_TES.TIPO_MODELLO_PRL "
			+ "	AND PIANI_PRL_RIG.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND PIANI_PRL_RIG.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND PIANI_PRL_RIG.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "	AND PIANI_PRL_RIG.ID_RIGA_MATERIALE = LLISTA_RIGA.NUM_RIGA_HOST "
			+ "INNER JOIN THIP.ORD_ESEC_ATV ORD_ESEC_ATV ON "
			+ "	ORD_ESEC_ATV.ID_AZIENDA = PIANI_PRL_TES.ID_AZIENDA "
			+ "	AND ORD_ESEC_ATV.ID_ANNO_ORD = PIANI_PRL_TES.ID_ANNO_ORD "
			+ "	AND ORD_ESEC_ATV.ID_NUMERO_ORD = PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "	AND ORD_ESEC_ATV.ID_RIGA_ATTIVITA = PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "WHERE "
			+ "	PIANI_PRL_TES.ID_AZIENDA = ? "
			+ "	AND LLISTA_TESTA.COD_TIPO_LISTA IN('P/PRE','P/PREC') "
			+ "	AND LMISSIONE.COD_UBICAZIONE IS NOT NULL "
			+ "	AND LMISSIONE.COD_MAG_FISICO = ? "
			+ "	AND LMISSIONE.CODICE = ? "
			+ "GROUP BY "
			+ "	ORD_ESEC_ATV.ID_AZIENDA, "
			+ "	ORD_ESEC_ATV.ID_ANNO_ORD, "
			+ "	ORD_ESEC_ATV.ID_NUMERO_ORD, "
			+ "	ORD_ESEC_ATV.ID_RIGA_ATTIVITA, "
			+ "	PIANI_PRL_RIG.ID_RIGA_MATERIALE";
	protected static CachedStatement cSelectOrdEsecAtvMatFromMissione = new CachedStatement(STMT_SELECT_ORDESEC_ATV_MAT_FROM_MISSIONE);

	protected static final String DOC_VEN_RIG = DocumentoVenditaRigaTM.TABLE_NAME_PRINCIPALE;

	protected static final String SELECT_RIGHE_DOC_DA_RIGA_ORD =
			"SELECT " + DOC_VEN_RIG + ".ID_ANNO_DOC, " + DOC_VEN_RIG + ".ID_NUMERO_DOC, " + DOC_VEN_RIG + ".ID_RIGA_DOC" +
					" FROM " + DOC_VEN_RIG +
					" WHERE " + DOC_VEN_RIG + ".ID_AZIENDA = ? AND" +
					" " + DOC_VEN_RIG + ".R_ANNO_ORD = ? AND" +
					" " + DOC_VEN_RIG + ".R_NUMERO_ORD = ? AND" +
					" " + DOC_VEN_RIG + ".R_RIGA_ORD = ? AND" +
					" " + DOC_VEN_RIG + ".R_DET_RIGA_ORD = 0";
	protected static CachedStatement cRigheDocDaRigaOrd = new CachedStatement(SELECT_RIGHE_DOC_DA_RIGA_ORD);

	@SuppressWarnings("rawtypes")
	@Override
	protected boolean run() {
		boolean isOk = true;
		try {
			//..Se la schedulazione dei buffer e' attiva potrebbe solo causare casini
			List schBufferAgv = YSchedulazioneBufferAgv.cercaBatchJob(Azienda.getAziendaCorrente());
			if (schBufferAgv != null && schBufferAgv.size() > 0){
				output.println("Schedulazione buffer AGV attiva, non e' possibile proseguire con il lavoro");
				output.println("fermare prima il lavoro...");
				return false;
			}
			isOk = runGenerazionePianiCaricoToyota();
			if(isOk) {
				isOk = mettiStatoNuovoMissioniEsecuzione();
			}
		}catch (Exception e) {
			output.println("Exc non gestita "+e.getMessage());
			isOk = false;
			e.printStackTrace(Trace.excStream);
		}
		return isOk;
	}

	protected boolean mettiStatoNuovoMissioniEsecuzione() throws SQLException {
		boolean isOk = true;
		String delete = "UPDATE "+MissioneTM.NOME_TABELLA+" SET "+MissioneTM.STATO_MISSIONE+" = '"+Missione.NUOVA+"' "
				+ " WHERE "+MissioneTM.STATO_MISSIONE+" = '"+Missione.ESECUZIONE+"' ";
		CachedStatement cs = new CachedStatement(delete);
		int rs = cs.executeUpdate();
		if(rs > 0)
			ConnectionManager.commit();
		return isOk;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected boolean runGenerazionePianiCaricoToyota() {
		boolean ret = true;

		List<YPianoCaricoToyota> testate = estrazionePianiCaricoNuoviEsistenti();
		for (Iterator iterator = testate.iterator(); iterator.hasNext();) {
			YPianoCaricoToyota pianoCarico = (YPianoCaricoToyota) iterator.next();
			if(!pianoCarico.isOnDB()) {
				pianoCarico.getNumeratoreHandler().setAnno(String.valueOf(TimeUtils.getCurrentDate().toLocalDate().getYear()));
				pianoCarico.getNumeratoreHandler().setDataDocumento(TimeUtils.getCurrentDate());

				//..Se la gestione udc del piano è operatore metto la testata piano in ricevuta
				//...cosi che l'operatore vada a mano a prelevare
				if(pianoCarico.getStatoGestione() == TipoGestioneUbicazione.OPERATORE) {
					pianoCarico.setStatoUdc(StatoPrelievoUdcToyota.RICEVUTA);
				}

			}else {
				//.Update di tutte le righe presenti a Stato C
				Iterator iterRighe = pianoCarico.getRighe().iterator();
				while(iterRighe.hasNext()) {
					YPianoCaricoToyotaRiga riga = (YPianoCaricoToyotaRiga) iterRighe.next();
					riga.setStatoRiga(StatoRigaToyota.CHIUSA);
				}
			}

			BODataCollector boDC = null;
			try {
				boDC = createDataCollector("YPianoCaricoToyota");
				boDC.setAutoCommit(false);
				boDC.setBo(pianoCarico);
				if(!pianoCarico.isOnDB()) {
					((DocumentoDataCollector)boDC).setNuovoDocumento(true);
				}
				DocumentoDataCollector.initRighePerCheck(boDC);
				int rc1 = boDC.save();
				if (rc1 == BODataCollector.ERROR){
					output.println("Impossibile salvare il piano di carico, errori : "+ErrorUtils.getInstance().toJSON(boDC.getErrorList().getErrors()).toString());
				}else {
					try {
						List<Missione> missioni = trovaMissioniChiaveLogicaPianoCarico(pianoCarico.getIdReparto(), pianoCarico.getIdCodiceUdc());
						
						Set<String> missioniDaScartare = trovaMissioniDaScartare(missioni);//72589
						
						for (Iterator iterator2 = missioni.iterator(); iterator2.hasNext();) {
							
							Missione missione = (Missione) iterator2.next();
							
							if (missioniDaScartare.contains(missione.getKey())) {//72589
							    continue;
							}

							YPianoCaricoToyotaRiga rigaPiano = (YPianoCaricoToyotaRiga) Factory.createObject(YPianoCaricoToyotaRiga.class);
							rigaPiano.setTestata(pianoCarico);

							rigaPiano.setIdArticolo(missione.getCodiceArticolo()); //.Assumo esista in thip
							rigaPiano.setMissione(missione);

							AttivitaEsecMateriale materiale = trovaMaterialeFromMissione(missione);
							if(materiale != null) {
								rigaPiano.setAttivitaesecmateriale(materiale);
								rigaPiano.setNumRitornoAttivita(materiale.getAttivitaEsecutiva().getNumeroRitorno());

								OrdineEsecutivo ordEsec = materiale.getAttivitaEsecutiva().getOrdineEsecutivo();
								//72326 <
								if(ordEsec.getStato() != DatiComuniEstesi.VALIDO) 
									continue;
								//72326 >
								rigaPiano.setIdCliente(ordEsec.getIdCliente());

								if(ordEsec.getOrdineVenditaRiga() != null) {
									//72326 <
									if(ordEsec.getOrdineVenditaRiga().getStato() != DatiComuniEstesi.VALIDO)
										continue;
									//72326 >
									List chiaviRigheDoc = selectRigheDocDaRigheOrd(Arrays.asList(ordEsec.getOrdineVenditaRiga().getKey()));
									if(chiaviRigheDoc != null && chiaviRigheDoc.size() > 0) {
										String c1 = (String) chiaviRigheDoc.get(0);
										String[] parts = KeyHelper.unpackObjectKey(c1);
										rigaPiano.setNumeroRiferimento(IntegrazioneThipLogis.VENDITA + parts[1] + parts[2]);

										//72326 <
										for (Iterator iterator3 = chiaviRigheDoc.iterator(); iterator3.hasNext();) {
											String chiave = (String) iterator3.next();
											DocumentoVenRigaPrm docVenRigPrm = (DocumentoVenRigaPrm) DocumentoVenRigaPrm.elementWithKey(DocumentoVenRigaPrm.class, chiave, PersistentObject.NO_LOCK);
											if(docVenRigPrm != null && docVenRigPrm.getStato() != DatiComuniEstesi.VALIDO) {
												continue;
											}
										}
										//72326 >
									}
								}else {
									rigaPiano.setNumeroRiferimento(materiale.getAttivitaEsecutiva().getNumeroRitorno());
								}

								rigaPiano.setQuantitaRichiestaUmPrm(missione.getQta1Richiesta());
								rigaPiano.setQuantitaPrelevataUmPrm(BigDecimal.ZERO);

								if(pianoCarico.getStatoGestione() == TipoGestioneUbicazione.AGV || pianoCarico.getStatoGestione() == TipoGestioneUbicazione.OPERATORE) {
									rigaPiano.setPrelevabile(true);
								}else {
									rigaPiano.setPrelevabile(false);
								}
								if(rigaPiano.getNumeroRiferimento() != null)
									pianoCarico.getRighe().add(rigaPiano);
							}

						}
					} catch (SQLException e1) {
						e1.printStackTrace(Trace.excStream);
					}

					boolean tutteChiuse = true;
					Iterator iterRighePC = pianoCarico.getRighe().iterator();
					while(iterRighePC.hasNext()) {
						YPianoCaricoToyotaRiga rPC = (YPianoCaricoToyotaRiga) iterRighePC.next();
						if(rPC.getStatoRiga() == StatoRigaToyota.APERTA) {
							tutteChiuse = false;
						}
					}

					if(tutteChiuse 
							&& pianoCarico.getStatoUdc() == StatoPrelievoUdcToyota.STATO_INIZIALE
							&& pianoCarico.isOnDB()) {
						boDC.setBo(pianoCarico);
						rc1 = boDC.delete(pianoCarico.getKey());
						if (rc1 == BODataCollector.ERROR){
							ConnectionManager.rollback();
							output.println("Impossibile cancellare il piano di carico, errori : "+ErrorUtils.getInstance().toJSON(boDC.getErrorList().getErrors()).toString());
						}else {
							ConnectionManager.commit();
						}
					}else {

						boDC.setBo(pianoCarico);
						if(pianoCarico.getRighe().size() == 0) {
							ConnectionManager.rollback();
						}else {
							rc1 = boDC.save();
							if (rc1 == BODataCollector.ERROR){
								output.println("Impossibile salvare il piano di carico, errori : "+ErrorUtils.getInstance().toJSON(boDC.getErrorList().getErrors()).toString());
							}else {
								ConnectionManager.commit();
							}
						}
					}
				}
			}
			catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
			}

		}
		return ret;
	}

	@SuppressWarnings("rawtypes")
	protected List<YPianoCaricoToyota> estrazionePianiCaricoNuoviEsistenti() {
		List<YPianoCaricoToyota> testate = new ArrayList<YPianoCaricoToyota>();
		try {
			List<YPianoCaricoToyota> pianiCarico = trovaPianiCaricoToyota();
			for (Iterator iterator = pianiCarico.iterator(); iterator.hasNext();) {
				YPianoCaricoToyota pianoCarico = (YPianoCaricoToyota) iterator.next();

				//.Qui devo verificare se esiste gia' un piano con StatoUDC <= 2, allora accodo altrimenti creo con StatoUDC = 0
				YPianoCaricoToyota pianoEsistente = trovaPianoCaricoApertoChiaveLogica(pianoCarico.getIdReparto(), pianoCarico.getIdCodiceUdc());
				if(pianoEsistente != null) {
					pianoEsistente.retrieve();

					testate.add(pianoEsistente);
				}else {
					//Fix 72036 inizio rem
					//.Scarico il flag 'StatoGestione' dall'ubicazione
					/*if(pianoCarico.getUbicazioneStock() != null) {
						pianoCarico.setStatoGestione(((YUbicazione)pianoCarico.getUbicazioneStock()).getTipoGestionePers());
					}*/
					//Fix 72036 fine rem
					char statoGestione = YPianoCaricoToyota.findStatoGestione(
							((YUbicazione)pianoCarico.getUbicazioneStock()),
							((YReparto)pianoCarico.getReparto())
							);//72163
					pianoCarico.setStatoGestione(statoGestione); //Fix 72036
					//Fix 72036 remmata
					/*if(((YReparto)pianoCarico.getReparto()).isAgvNonDefault())
						pianoCarico.setStatoGestione(TipoGestioneUbicazione.MULETTISTA);*/

					testate.add(pianoCarico);
				}

			}
		} catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}
		return testate;
	}

	public static YPianoCaricoToyota trovaPianoCaricoApertoChiaveLogica(String idReparto, String codMappaUdc) throws SQLException {
		YPianoCaricoToyota piano = null;
		ResultSet rs = null;
		try{
			PreparedStatement ps = cSelectPianoCaricoToyotaAperto.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, Azienda.getAziendaCorrente());
			db.setString(ps, 2, idReparto);
			db.setString(ps, 3, codMappaUdc); 
			db.setString(ps, 4, String.valueOf(StatoPrelievoUdcToyota.RICEVUTA)); 
			rs = ps.executeQuery();
			if (rs.next()){
				piano = (YPianoCaricoToyota) Factory.createObject(YPianoCaricoToyota.class);
				piano.setKey(KeyHelper.buildObjectKey(new String[] {
						rs.getString(YPianoCaricoToyotaTM.ID_AZIENDA),
						rs.getString(YPianoCaricoToyotaTM.ID_ANNO_DOC),
						rs.getString(YPianoCaricoToyotaTM.ID_NUMERO_DOC)
				}));
			}
		}finally{
			try{
				if(rs != null)
					rs.close();
			}catch(SQLException e){
				e.printStackTrace(Trace.excStream);
			}
		}
		return piano;
	}

	public static List<Missione> trovaMissioniChiaveLogicaPianoCarico(String idReparto, String codMappaUdc) throws SQLException {
		List<Missione> missioni = new ArrayList<Missione>();
		ResultSet rs = null;
		try{
			codMappaUdc = Column.rightTrim(codMappaUdc);
			PreparedStatement ps = cSelectMissioniChiaveLogicaPianoCarico.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, Azienda.getAziendaCorrente());
			//db.setString(ps, 2, "P/PRE");
			db.setString(ps, 2, codMappaUdc);
			db.setString(ps, 3, idReparto); 
			rs = ps.executeQuery();
			while (rs.next()){
				missioni.add((Missione) Missione.elementWithKey(Missione.class, KeyHelper.buildObjectKey(new String[] {
						rs.getString(MissioneTM.COD_MAG_FISICO),
						rs.getString(MissioneTM.CODICE)
				}),PersistentObject.NO_LOCK));
			}
		}finally{
			try{
				if(rs != null)
					rs.close();
			}catch(SQLException e){
				e.printStackTrace(Trace.excStream);
			}
		}
		return missioni;
	}
	
	//72589 <
	protected Set<String> trovaMissioniDaScartare(List<Missione> missioni) throws SQLException {

	    Set<String> missioniDaScartare = new HashSet<>();

	    Map<String, List<Missione>> gruppiMissioni = new HashMap<>();

	    // Raggruppamento per CodLista + RigaLista
	    for (Missione missione : missioni) {

	        String chiave =
	                Column.rightTrim(missione.getTestataRigaLista()) + "#"
	              + missione.getCodiceRigaLista();

	        List<Missione> gruppo = gruppiMissioni.get(chiave);

	        if (gruppo == null) {
	            gruppo = new ArrayList<>();
	            gruppiMissioni.put(chiave, gruppo);
	        }

	        gruppo.add(missione);
	    }

	    // Controllo quantità
	    for (List<Missione> gruppo : gruppiMissioni.values()) {

	        BigDecimal qtaMissioni = BigDecimal.ZERO;

	        for (Missione missione : gruppo) {
	            qtaMissioni = qtaMissioni.add(missione.getQta1Richiesta());
	        }

	        AttivitaEsecMateriale materiale = trovaMaterialeFromMissione(gruppo.get(0));

	        if (materiale == null) {
	            continue;
	        }

	        BigDecimal qtaResidua = materiale.getQtaResiduaUMPrm();

	        if (qtaResidua == null) {
	            qtaResidua = BigDecimal.ZERO;
	        }

	        if (qtaMissioni.compareTo(qtaResidua) != 0) {

	            output.println("Scarto missioni lista "
	                    + gruppo.get(0).getTestataRigaLista()
	                    + " riga "
	                    + gruppo.get(0).getCodiceRigaLista()
	                    + " - Qta missioni: "
	                    + qtaMissioni
	                    + " - Qta residua materiale: "
	                    + qtaResidua);

	            for (Missione missione : gruppo) {
	                missioniDaScartare.add(missione.getKey());
	            }
	        }
	    }

	    return missioniDaScartare;
	}
	//72589 >
	
	public static AttivitaEsecMateriale trovaMaterialeFromMissione(Missione m) throws SQLException {
		AttivitaEsecMateriale materiale = null;
		ResultSet rs = null;
		try{
			PreparedStatement ps = cSelectOrdEsecAtvMatFromMissione.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, Azienda.getAziendaCorrente());
			//db.setString(ps, 2, "P/PRE");
			db.setString(ps, 2, m.getCodiceMagFisico());
			db.setString(ps, 3, String.valueOf(m.getCodice())); 
			rs = ps.executeQuery();
			if (rs.next()){
				materiale = (AttivitaEsecMateriale) AttivitaEsecMateriale.elementWithKey(AttivitaEsecMateriale.class, KeyHelper.buildObjectKey(new String[] {
						rs.getString(AttivitaEsecMaterialeTM.ID_AZIENDA),
						rs.getString(AttivitaEsecMaterialeTM.ID_ANNO_ORD),
						rs.getString(AttivitaEsecMaterialeTM.ID_NUMERO_ORD),
						rs.getString(AttivitaEsecMaterialeTM.ID_RIGA_ATTIVITA),
						rs.getString(AttivitaEsecMaterialeTM.ID_RIGA_MATERIALE)
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
		return materiale;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static List selectRigheDocDaRigheOrd(List ordVenRigKeys) throws SQLException {
		List ret = new ArrayList();
		Database db = ConnectionManager.getCurrentDatabase();
		Iterator iter = ordVenRigKeys.iterator();
		while (iter.hasNext()) {
			synchronized (cRigheDocDaRigaOrd) {
				String ordVenRigKey = (String) iter.next();
				String[] tmp = KeyHelper.unpackObjectKey(ordVenRigKey);
				String idAzienda =  tmp[0];
				String idAnnoOrd = tmp[1];
				String idNumeroOrd = tmp[2];
				String idRigaOrd = tmp[3];
				PreparedStatement ps = cRigheDocDaRigaOrd.getStatement();
				db.setString(ps, 1, idAzienda);
				db.setString(ps, 2, idAnnoOrd);
				db.setString(ps, 3, idNumeroOrd);
				db.setString(ps, 4, idRigaOrd);
				ResultSet res = ps.executeQuery();
				while (res.next()) {
					String idAnnoDoc = Column.rightTrim(res.getString(1));
					String idNumeroDoc = Column.rightTrim(res.getString(2));
					String idRigaDoc = Column.rightTrim(res.getString(3));
					String rigKey = KeyHelper.buildObjectKey(new String[] {idAzienda, idAnnoDoc, idNumeroDoc, idRigaDoc, "0"});
					ret.add(rigKey);
				}
				res.close();
			}
		}
		Collections.sort(ret);
		return ret;
	}

	public List<YPianoCaricoToyota> trovaPianiCaricoToyota() throws SQLException {
		List<YPianoCaricoToyota> piani = new ArrayList<YPianoCaricoToyota>();
		ResultSet rs = null;
		try{
			PreparedStatement ps = cSelectPianiCaricoTyotaTes.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, Azienda.getAziendaCorrente());
			//db.setString(ps, 2, "P/PRE"); fix 72036 rem
			rs = ps.executeQuery();
			while (rs.next()){
				YPianoCaricoToyota pianoCarico = (YPianoCaricoToyota) Factory.createObject(YPianoCaricoToyota.class);
				pianoCarico.setIdCodiceUdc(rs.getString(MissioneTM.COD_MAPPA_UDC));
				pianoCarico.setIdReparto(rs.getString(AttivitaEsecutivaTM.R_REPARTO));
				pianoCarico.setIdMagazzinoFisicoStock(rs.getString(MissioneTM.COD_MAG_FISICO));
				pianoCarico.setIdCodiceUbicazioneStock(rs.getString(MissioneTM.COD_UBICAZIONE));

				piani.add(pianoCarico);
			}
		}finally{
			try{
				if(rs != null)
					rs.close(); 
			}catch(SQLException e){
				e.printStackTrace(Trace.excStream);
			}
		}
		return piani;
	}

	//72224 Rendere pubblico
	/*protected*/ public static BODataCollector createDataCollector(String str) throws SQLException{
		try {
			BODataCollector bodc = null;
			ClassADCollection classDesc = ClassADCollectionManager.collectionWithName(str);
			String collectorName = classDesc.getBODataCollector();
			if (collectorName != null) {
				bodc = (BODataCollector)Factory.createObject(collectorName);
				if (bodc instanceof DocumentoDataCollector){
					((DocumentoDataCollector)bodc).setDisabilitaControlliRelazione(true);
				}
				bodc.initialize(classDesc.getClassName(), true);
			}
			else
				bodc = new BODataCollector(classDesc.getClassName(), true);

			return bodc;
		}
		catch (Exception e){
			e.printStackTrace(Trace.excStream);
			throw new ThipException(e.getMessage());
		}
	}

	@SuppressWarnings("rawtypes")
	public static List cercaBatchJob() {
		List l = null;
		try
		{
			String where = "RUNNER_CLASS_NAME = 'it.dnd.thip.logis.lgb.YGenerazionePianiCaricoToyota'";
			where += " AND STATUS = 'A'";
			l = BatchJob.retrieveList(where, "", false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return l;
	}

	@Override
	protected String getClassAdCollectionName() {
		return "YGenPianCaricoToyota";
	}

}
