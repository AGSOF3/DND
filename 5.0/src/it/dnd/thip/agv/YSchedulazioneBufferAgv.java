package it.dnd.thip.agv;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import com.thera.thermfw.base.ResourceLoader;
import com.thera.thermfw.base.TimeUtils;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.batch.BatchJob;
import com.thera.thermfw.batch.BatchJobTM;
import com.thera.thermfw.batch.BatchOptions;
import com.thera.thermfw.batch.BatchRunnable;
import com.thera.thermfw.batch.BatchService;
import com.thera.thermfw.batch.ScheduledJob;
import com.thera.thermfw.common.ErrorMessage;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.Column;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.security.Authorizable;
import com.thera.thermfw.security.Conflictable;
import com.thera.thermfw.security.Security;
import com.thera.thermfw.web.WebElement;

import it.dnd.thip.base.azienda.YReparto;
import it.dnd.thip.logis.fis.YUbicazione;
import it.dnd.thip.logis.lgb.StatoPrelievoUdcToyota;
import it.dnd.thip.logis.lgb.YGenerazionePianiCaricoToyota;
import it.dnd.thip.logis.lgb.YPianoCaricoToyota;
import it.dnd.thip.logis.lgb.YPianoCaricoToyotaTM;

import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.profilo.ThipUser;
import it.thera.thip.cs.ThipException;

/**
 *
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 03/09/2025
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 72146    03/09/2025  DSSOF3   Prima stesura
 * 72188	30/10/2025	DSSOF3   Fix tecnica
 * 72197	07/11/2025	AGSOF3	 Saturazione completa per reparti 1Prelievo1Ripristino
 * 								 Aggiungo nei buffer solo PC la cui UDC è in scansia
 * 72248	09/12/2025	AGSOF3	 Nella popolazione buffer per la logica FIFO ora verifichiamo prima che si 
 * 								 possano passare le missioni, quando sono di prelievo potrebbero esserci le baie piene 
 * 72274	09/01/2026	AGSOF3	 All'inizio di un ciclo di buffer sblocco le eventuali baie rimaste bloccate erroneamente
 * 								 Modificato l'ordinamento della query di retrievi piani non schedulati
 */

public class YSchedulazioneBufferAgv extends BatchRunnable implements Authorizable, Conflictable {

	public static final String RES = "it.dnd.thip.agv.resources.YSchedulazioneBufferAgv";

	protected static final String SELECT_PIANI_CARICO_NON_SCHEDUL = "WITH BASE AS (\r\n"
			+ "    SELECT\r\n"
			+ "        PCT.ID_AZIENDA,\r\n"
			+ "        PCT.ID_ANNO_DOC,\r\n"
			+ "        PCT.ID_NUMERO_DOC,\r\n"
			+ "        PCT.R_REPARTO,\r\n"
			+ "        PCT.NUMERO_RITORNO_VRS,\r\n"
			+ "        PCT.TIMESTAMP_CRZ,\r\n"
			+ "        PCT.STATO_UDC,\r\n"
			+ "        PCR.ID_RIGA_DOC,\r\n"
			+ "        PCR.TIPO_MISSIONE,\r\n"
			+ "        YLU.GES_BAIA_PRELIEVO\r\n"
			+ "    FROM THIPPERS.YPIANO_CARICO_TOYOTA_TES PCT\r\n"
			+ "    LEFT JOIN LOGIS.LMAPPA_UDC LU\r\n"
			+ "        ON PCT.R_COD_MAPPA_UDC = LU.CODICE\r\n"
			+ "    LEFT JOIN THIPPERS.YLUBICAZIONE YLU\r\n"
			+ "        ON LU.COD_MAG_FISICO = YLU.COD_MAG_FISICO\r\n"
			+ "       AND LU.COD_UBICAZIONE = YLU.CODICE\r\n"
			+ "    OUTER APPLY (\r\n"
			+ "        SELECT TOP (1)\r\n"
			+ "            PCR.ID_RIGA_DOC,\r\n"
			+ "            PCR.TIPO_MISSIONE\r\n"
			+ "        FROM THIPPERS.YPIANO_CARICO_TOYOTA_RIG PCR\r\n"
			+ "        WHERE PCR.ID_AZIENDA    = PCT.ID_AZIENDA\r\n"
			+ "          AND PCR.ID_ANNO_DOC   = PCT.ID_ANNO_DOC\r\n"
			+ "          AND PCR.ID_NUMERO_DOC = PCT.ID_NUMERO_DOC\r\n"
			+ "        ORDER BY PCR.ID_RIGA_DOC\r\n"
			+ "    ) PCR\r\n"
			+ "    WHERE\r\n"
			+ "        PCT.ID_AZIENDA = ? \r\n"
			+ "        AND PCT.STATO_UDC IN ('0', '3')\r\n"
			+ "        AND PCT.STATO_GESTIONE = 'A'\r\n"
			+ "        AND EXISTS (\r\n"
			+ "            SELECT 1\r\n"
			+ "            FROM THIPPERS.YAGV_BUFFER_TES A\r\n"
			+ "            WHERE A.ID_AZIENDA = PCT.ID_AZIENDA\r\n"
			+ "              AND (PCT.R_REPARTO = A.R_REPARTO_1\r\n"
			+ "               OR  PCT.R_REPARTO = A.R_REPARTO_2)\r\n"
			+ "        )\r\n"
			+ "        AND (PCT.STATO_UDC <> '0'\r\n"
			+ "             OR YLU.GES_BAIA_PRELIEVO = 'N')\r\n"
			+ "),\r\n"
			+ "ALT AS (\r\n"
			+ "    SELECT\r\n"
			+ "        *,\r\n"
			+ "        ROW_NUMBER() OVER (\r\n"
			+ "            PARTITION BY\r\n"
			+ "                R_REPARTO,\r\n"
			+ "                NUMERO_RITORNO_VRS,\r\n"
			+ "                TIPO_MISSIONE\r\n"
			+ "            ORDER BY\r\n"
			+ "                TIMESTAMP_CRZ,\r\n"
			+ "                ID_RIGA_DOC\r\n"
			+ "        ) AS RN_TIPO\r\n"
			+ "    FROM BASE\r\n"
			+ ")\r\n"
			+ "SELECT *\r\n"
			+ "FROM ALT\r\n"
			+ "ORDER BY\r\n"
			+ "    R_REPARTO,\r\n"
			+ "    NUMERO_RITORNO_VRS,\r\n"
			+ "    RN_TIPO;";
	/*			"SELECT "
					+ "	* "
					+ "FROM "
					+ "	THIPPERS.YPIANO_CARICO_TOYOTA_TES PCT "
					+ "LEFT OUTER JOIN LOGIS.LMAPPA_UDC LU ON \r\n"
					+ "PCT.R_COD_MAPPA_UDC = LU.CODICE \r\n"
					+ "LEFT OUTER JOIN THIPPERS.YLUBICAZIONE YLU ON \r\n"
					+ "LU.COD_MAG_FISICO = YLU.COD_MAG_FISICO \r\n"
					+ "AND LU.COD_UBICAZIONE  = YLU.CODICE "
					+ "WHERE PCT.ID_AZIENDA = ? "
					+ "AND PCT.STATO_UDC IN ('"+StatoPrelievoUdcToyota.STATO_INIZIALE+"','"+StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO+"') "
					+ "AND STATO_GESTIONE = '"+TipoGestioneUbicazione.AGV+"' "
					+ "AND EXISTS ( "
					+ "        SELECT 1 "
					+ "        FROM THIPPERS.YAGV_BUFFER_TES A "
					+ "        WHERE A.ID_AZIENDA = PCT.ID_AZIENDA "
					+ "          AND (PCT.R_REPARTO = A.R_REPARTO_1 OR PCT.R_REPARTO = A.R_REPARTO_2) "
					+ "    ) "
					+ " AND (PCT.STATO_UDC <> '0' OR YLU.GES_BAIA_PRELIEVO = 'N' ) "//72197 AGOSF3 aggiungo nei buffer solo PC la cui UDC è in scansia
					+ " ORDER BY TIMESTAMP_CRZ ASC, PCT.R_REPARTO , PCT.NUMERO_RITORNO_VRS ; ";//72274 aggiunto ordinamento sul numero di ritorno 
	 */
	protected static CachedStatement cSelectPianiCaricoNonSchedul = new CachedStatement(SELECT_PIANI_CARICO_NON_SCHEDUL);

	protected static String ServSchedTerm1=ResourceLoader.getString(RES, "ServSchedTerm1");
	protected static String ServSchedTerm2=ResourceLoader.getString(RES, "ServSchedTerm2");
	protected static String ServSchedTerm3=ResourceLoader.getString(RES, "ServSchedTerm3");
	protected static String FallLett1=ResourceLoader.getString(RES, "FallLett1");
	protected static String FallLett2=ResourceLoader.getString(RES, "FallLett2");
	protected static String ProcCorr=ResourceLoader.getString(RES, "ProcCorr");
	protected static String FallSalv=ResourceLoader.getString(RES, "FallSalv");
	protected static String batchQueueId = ResourceLoader.getString(RES, "BatchQueueId");
	protected static String descriptionSchedJobId = ResourceLoader.getString(RES, "DescriptionSchedJobId");

	protected long iSleepTime;
	protected Time iExpirationTime;
	protected Time iOreInitiale;
	protected char iStato = 'V';
	protected ScheduledJob myScheduled = null;
	protected boolean iDoRun =false;
	protected boolean iFirstTime = true;
	protected String iIdAzienda;
	protected String iScheduledJobId;

	public YSchedulazioneBufferAgv() {
		setIdAzienda(Azienda.getAziendaCorrente());
	}

	public void setSleepTime(long sleepTime)
	{
		iSleepTime = sleepTime;
	}

	public long getSleepTime()
	{
		return iSleepTime;
	}

	public void setExpirationTime(Time expirationTime)
	{
		iExpirationTime = expirationTime;
	}

	public Time getExpirationTime()
	{
		return iExpirationTime;
	}

	public void setOreInitiale(Time oreInitiale)
	{
		iOreInitiale = oreInitiale;
	}

	public Time getOreInitiale()
	{
		return iOreInitiale;
	}

	public void setStato(char stato)
	{
		iStato = stato;
	}

	public char getStato()
	{
		return iStato;
	}

	public void setIdAzienda(String idAzienda)
	{
		iIdAzienda = idAzienda;
	}

	public String getIdAzienda()
	{
		return iIdAzienda;
	}

	public void setScheduledJobId(String scheduledJobId)
	{
		iScheduledJobId = scheduledJobId;
	}

	public String getScheduledJobId()
	{
		return iScheduledJobId;
	}

	@SuppressWarnings("rawtypes")
	protected void init()
	{
		if (!iFirstTime)
			return;
		iFirstTime = false;
		List l = null;
		try
		{
			String where = "RUNNER_CLASS_NAME = 'it.dnd.thip.agv.YSchedulazioneBufferAgv'" +
					" AND RTRIM(USER_ID) LIKE '%" + getIdAzienda() + "'";
			l = ScheduledJob.retrieveList(where, "", false);
			if (!l.isEmpty())
				myScheduled = (ScheduledJob) l.get(0);
			else
				createScheduledJob(); //Fix 12836
			String par = myScheduled.getParameters();
			if (par != null) //Fix 12836
			{
				par = par.replace(';', (char)18);
				getDataCollector().streamToObject(this, par);
			}
			setScheduledJobId(myScheduled.getScheduledJobId()); //Fix 12836
			setOreInitiale(myScheduled.getTime());
			setStato(myScheduled.getStatus());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public boolean retrieve(int lockType) throws SQLException {
		init();
		return true;
	}

	@SuppressWarnings("rawtypes")
	@Override
	protected boolean run() {
		init();
		//Time now = TimeUtils.getCurrentTime();
		/*if ((iOreInitiale.compareTo(now) > 0) || (iExpirationTime.compareTo(now) < 0)){
			output.println((new ErrorMessage("THIP110344")).getLongText());
			return false;
		}*/
		if (iStato != 'V'){
			output.println((new ErrorMessage("THIP110345")).getLongText());
			return false;
		}
		List l = cercaBatchJob(getIdAzienda());
		if (l != null && l.size() > 1)
		{
			output.println((new ErrorMessage("THIP110346")).getLongText());
			return true;
		}
		List lGTYT = YGenerazionePianiCaricoToyota.cercaBatchJob();
		if (lGTYT != null && lGTYT.size() > 0)
		{
			output.println((new ErrorMessage("BAS0000078","Non e' possibile eseguire il lavoro, piani di carico toyota in esecuzione")).getLongText());
			return true;
		}
		boolean isOk = true;
		try {
			while (iExpirationTime.getTime() >= TimeUtils.getCurrentTime().getTime()){
				this.checkPointImmediate(); //serve per poi stoppare nel caso di "arresto"
				if (iSleepTime == 0){
					iSleepTime = 30;
				}
				Thread.sleep(iSleepTime * 1000);
				boolean isInvioExecuting = isInvioBufferToyotaInEsecuzione();
				if(!isInvioExecuting) {
					isOk = pulisciRigheBuffer();
					pulisciBaieBloccate();//72274
					if(isOk)
						isOk = popolazioneBuffer();
					//72188
					String where = " "+YAgvBufferRigaTM.ID_AZIENDA+" = '"+getIdAzienda()+"' ";
					Vector righeBuffer = YAgvBufferRiga.retrieveList(where, "", false);
					//72188
					if(isOk && righeBuffer.size() > 0) { //..solo se ci sono delle righe...
						output.println("> Accensione invio a Toyota");
						YInvioBufferToyota invioProssimo = (YInvioBufferToyota) Factory.createObject(YInvioBufferToyota.class);
						BatchOptions batchOptions = (BatchOptions) Factory.createObject(BatchOptions.class);
						boolean ris = batchOptions.initDefaultValues(YInvioBufferToyota.class, "YInvioBufferToy", "RUN");
						if(ris) {
							invioProssimo.setBatchJob(batchOptions.getBatchJob());
							invioProssimo.setIdBuffer(1);
							invioProssimo.setSingoloBuffer(true);
							invioProssimo.setScheduledJob(batchOptions.getScheduledJob());
							invioProssimo.getBatchJob().setDescription("Invio Buffer 1");
							invioProssimo.getBatchJob().setUserDescription("Invio Buffer 1");

							if (invioProssimo.save() >= 0) {
								ConnectionManager.commit();
								BatchService.submitJob(invioProssimo.getBatchJob());
								output.println("> Invio buffer Toyota inviato al BatchService.");
							} else {
								output.println("Errore: impossibile accendere l'invio buffer a toyota");
								ConnectionManager.rollback();
							}
						}else {
							output.println("Impossibile lanciare l'invio buffer a Toyota...");
							return false;
						}
					}
					this.checkPointImmediate(); //serve per poi stoppare nel caso di "arresto"
					if (iSleepTime == 0){
						iSleepTime = 30;
					}
					Thread.sleep(iSleepTime * 1000);
				}
			}
		}catch (Exception e) {
			e.printStackTrace(Trace.excStream);
		}
		return isOk;
	}

	protected boolean isInvioBufferToyotaInEsecuzione() {
		boolean executing = false;
		String stmt = "SELECT * FROM "+BatchJobTM.TABLE_NAME+" WHERE "+BatchJobTM.RUNNER_CLASS_NAME+" = 'it.dnd.thip.agv.YInvioBufferToyota' ";
		stmt += "AND "+BatchJobTM.END_TIMESTAMP+" IS NULL ";
		ResultSet rs = null;
		CachedStatement cs = null;
		try {
			cs = new CachedStatement(stmt);
			rs = cs.executeQuery();
			if(rs.next()) {
				executing = true;
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
		return executing;
	}

	protected boolean pulisciRigheBuffer() {
		boolean isOk = true;
		String stmt = "DELETE FROM "+YAgvBufferRigaTM.TABLE_NAME+" ";
		CachedStatement cs = new CachedStatement(stmt);
		int ris;
		try {
			ris = cs.executeUpdate();
			if(ris > 0) {
				ConnectionManager.commit();
			}
		} catch (SQLException e) {
			isOk = false;
			e.printStackTrace(Trace.excStream);
		}
		return isOk;
	}

	/**
	 * Mette il flag di baia bloccata a false per le ubicazioni gestite come baie di prelievo rimaste sporche.
	 * Il caso di flag sporco è raro e non ancora identificato, succede solo in alcuni casi in cui l'agv va in errore
	 * per evitare che le ubicazioni bloccate si accumulino le sblocchiamo ad ogni esecuzione dei 7 buffer.
	 */
	protected void pulisciBaieBloccate() {
		String stmt = "UPDATE lu\r\n"
				+ "SET lu.BLOCCATA_AGV = 'N'\r\n"
				+ "FROM THIPPERS.YLUBICAZIONE lu\r\n"
				+ "WHERE\r\n"
				+ "    lu.GES_BAIA_PRELIEVO = 'Y'\r\n"
				+ "    AND lu.BLOCCATA_AGV = 'Y'\r\n"
				+ "    AND NOT EXISTS (\r\n"
				+ "        SELECT\r\n"
				+ "            1\r\n"
				+ "        FROM\r\n"
				+ "            TOYOTA.TRANSPORT PRIM\r\n"
				+ "            LEFT JOIN TOYOTA.TRANSPORT_V01 SEC1\r\n"
				+ "                ON PRIM.ID = SEC1.ID\r\n"
				+ "            JOIN THIPPERS.YLUBICAZIONE y\r\n"
				+ "                ON y.COD_MAG_FISICO = 'ST1'\r\n"
				+ "               AND y.CODICE = SEC1.ARRIVO\r\n"
				+ "        WHERE\r\n"
				+ "            PRIM.TRANSPORT_STATE NOT IN ('3', '4', '5', '6')\r\n"
				+ "            AND y.GES_BAIA_PRELIEVO = 'Y'\r\n"
				+ "            AND lu.CODICE = y.CODICE\r\n"
				+ "    )";
		CachedStatement cs = new CachedStatement(stmt);
		int ris;
		try {
			ris = cs.executeUpdate();
			if(ris > 0) {
				ConnectionManager.commit();
			}
		} catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}
	}

	@SuppressWarnings({ "rawtypes" })
	protected boolean popolazioneBuffer() throws SQLException {
		//..Sono 7 ma per sicurezza lo metto in un properties
		Integer numeroBuffer = Integer.valueOf(ResourceLoader.getString(RES, "NumeroBuffer"));

		List pianiCaricoDaSchedulare = selectPianiCaricoNonSchedulati(getIdAzienda());
		output.println(TimeUtils.getCurrentTimestamp());
		if(pianiCaricoDaSchedulare.size() == 0) {
			output.println("Nessun piano di carico da schedulare");
			return true;
		}else {
			output.println("Trovati "+pianiCaricoDaSchedulare.size()+" piani da schedulare...");
		}

		Map<String,String> pcBufferizzati = new HashMap<String,String>();

		//..Scorro i buffer per popolarli con le missioni
		for(int i = 1; i < numeroBuffer.intValue() + 1; i++) {
			try {
				String c = KeyHelper.buildObjectKey(new String[] { getIdAzienda(),String.valueOf(i)});
				YAgvBufferTestata bufferX = YAgvBufferTestata.elementWithKey(c, PersistentObject.OPTIMISTIC_LOCK);
				if(bufferX != null) {

					//..Forzo l'apertura del buffer e la pulizia di eventuali righe
					bufferX.setStatoBuffer(YAgvBufferTestata.SPENTO);
					if(bufferX.getYAgvBufferRiga().size() > 0) {
						bufferX.getYAgvBufferRiga().clear();
					}
					int rcSave = bufferX.save();
					if(rcSave > 0) {
						ConnectionManager.commit();
					}else {
						output.print("Buffer ["+c+"] impossibile salvare dopo aver spento e pulito le righe, rc ="+rcSave);
						ConnectionManager.rollback();
						return false;
					}

					//.Aggiorno da db
					bufferX.retrieve();

					YReparto reparto1 = (YReparto) bufferX.getReparto1();
					YReparto reparto2 = (YReparto) bufferX.getReparto2();

					char logicaMissioniR1 = reparto1.getLogicaMissioniToyota();
					char logicaMissioniR2 = reparto2.getLogicaMissioniToyota();

					List<YPianoCaricoToyota> pianiR1 = trovaPianiCaricoLogicaReparto(pianiCaricoDaSchedulare,reparto1,logicaMissioniR1);
					for (Iterator iterator = pianiR1.iterator(); iterator.hasNext();) {
						YPianoCaricoToyota piano = (YPianoCaricoToyota) iterator.next();
						if(piano != null && !pcBufferizzati.containsKey(piano.getIdCodiceUdc())
								&& !piano.esisteTransportInCorso()) { //72188
							YAgvBufferRiga riga = rigaAgv(bufferX, c, piano);

							bufferX.getYAgvBufferRiga().add(riga);

							pcBufferizzati.put(piano.getIdCodiceUdc(), piano.getIdCodiceUdc());
						}
					}

					List<YPianoCaricoToyota> pianiR2 = trovaPianiCaricoLogicaReparto(pianiCaricoDaSchedulare,reparto2,logicaMissioniR2);
					for (Iterator iterator = pianiR2.iterator(); iterator.hasNext();) {
						YPianoCaricoToyota piano = (YPianoCaricoToyota) iterator.next();
						if(piano != null && !pcBufferizzati.containsKey(piano.getIdCodiceUdc())
								&& !piano.esisteTransportInCorso()) { //72188
							YAgvBufferRiga riga = rigaAgv(bufferX, c, piano);

							bufferX.getYAgvBufferRiga().add(riga);

							pcBufferizzati.put(piano.getIdCodiceUdc(), piano.getIdCodiceUdc());
						}
					}

					rcSave = bufferX.save();
					if(rcSave > 0) {
						ConnectionManager.commit();
					}else if(rcSave < 0){
						output.print("Buffer ["+c+"] impossibile salvare dopo aver spento e pulito le righe, rc ="+rcSave);
						ConnectionManager.rollback();
						return false;
					}
				}
			} catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
			}
		}

		return true;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected List<YPianoCaricoToyota> trovaPianiCaricoLogicaReparto(
			List pianiCaricoDaSchedulare, 
			YReparto reparto,
			char logicaMissioni) throws SQLException {

		List<YPianoCaricoToyota> result = new ArrayList<>(2);
		if (pianiCaricoDaSchedulare == null || pianiCaricoDaSchedulare.isEmpty()) {
			return result; // lista vuota
		}

		Iterator<String> it = pianiCaricoDaSchedulare.iterator();

		//72274 AGSOF3
		//faccio questa modifica cablata, mettere la logicaMissioni fissa PRELIEVO_RIPRISTINO, anche se sul reparto c'è FIFO
		//perchè in realtà il concetto di FIFO del cliente non è first in first out, ma è un concetto quasi astratto che è
		//più in linea con il prelievo/ripristino, chiunque togliesse questa modifica verifichi bene qual è la richiesta 
		//sul FIFO e testi bene, il codice attuale nel caso FIFO sicuramente non è corretto
		logicaMissioni = LogicaMissioniToyota.PRELIEVO_RIPRISTINO;
		switch (logicaMissioni) {
		case LogicaMissioniToyota.PRELIEVO_RIPRISTINO: {
			List<YPianoCaricoToyota> prelievi = new ArrayList<>();
			List<YPianoCaricoToyota> ripristini = new ArrayList<>();

			while (it.hasNext()) {//72248
				YPianoCaricoToyota pct = YPianoCaricoToyota.elementWithKey(it.next(), PersistentObject.NO_LOCK);
				if (!matchesReparto(pct, reparto)) continue;

				int stato = pct.getStatoUdc();
				if (stato == StatoPrelievoUdcToyota.STATO_INIZIALE && prelievi.size() < 2) {
					prelievi.add(pct);
					it.remove();
				} else if (stato == StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO && ripristini.size() < 2) {
					ripristini.add(pct);
					it.remove();
				}
			}

			//72197 se ho solo prelievi o solo ripristini allora inserisco cmq 2 pc di quel tipo
			// Componi la lista finale con al massimo 2 elementi
			if (!prelievi.isEmpty() && !ripristini.isEmpty()) {
				// uno prelievo e uno ripristino
				result.add(prelievi.get(0));
				result.add(ripristini.get(0));
			} else if (!prelievi.isEmpty()) {
				// solo prelievi
				result.addAll(prelievi.subList(0, Math.min(2, prelievi.size())));
			} else if (!ripristini.isEmpty()) {
				// solo ripristini
				result.addAll(ripristini.subList(0, Math.min(2, ripristini.size())));
			}
			break;
		}

		case LogicaMissioniToyota.FIFO: {
			while (it.hasNext() && result.size() < 2) {
				YPianoCaricoToyota pct = YPianoCaricoToyota.elementWithKey(it.next(), PersistentObject.NO_LOCK);
				if (!matchesReparto(pct, reparto)) continue;

				//if (pct.getStatoUdc() == StatoPrelievoUdcToyota.STATO_INIZIALE) {

				//72248<
				//				se però la missione non è eseguibile:
				//					- è di prelievo ma non ci sono baie libere
				//				allora scarto la missione

				//				String idUbicazioneFrom = pct.getServizioCodiceUbicazioneFrom();
				//				String idUbicazioneTo = pct.getServizioCodiceUbicazioneTo();
				//				liberaBloccoAgvUbicazione(pct.getIdMagazzinoFisicoPrelievo(), idUbicazioneTo);
				//				if((idUbicazioneFrom == null || idUbicazioneFrom.isEmpty()) || (idUbicazioneTo == null || idUbicazioneTo.isEmpty())) {
				//					continue;
				//				}else {
				result.add(pct);
				it.remove(); // rimuovi appena selezionato
				//				}
				//72248 >
				//}
			}
			break;
		}

		default:
			// logica non riconosciuta -> ritorna lista vuota (o lancia eccezione, se preferisci)
			break;
		}

		return result;
	}


	protected void liberaBloccoAgvUbicazione(String idMagFisico, String idUbicazioneTo) {
		try {
			if(idUbicazioneTo != null) {
				YUbicazione ubi = (YUbicazione) YUbicazione.elementWithKey(idMagFisico + KeyHelper.KEY_SEPARATOR + idUbicazioneTo, 0); 
				if(ubi != null) {
					ConnectionManager.pushConnection();
					ubi.setBloccataAgv(false);
					ubi.save();
					ConnectionManager.commit();
				}
			}
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

	private boolean matchesReparto(YPianoCaricoToyota pct, YReparto reparto) {
		if (reparto == null) return true;
		try {
			return pct.getIdReparto().equals(reparto.getIdReparto());
		} catch (Exception e) {
			return true;
		}
	}

	public static YAgvBufferRiga rigaAgv(YAgvBufferTestata bufferT, String idAzienda, YPianoCaricoToyota pianoCaricoT) {
		YAgvBufferRiga riga = (YAgvBufferRiga) Factory.createObject(YAgvBufferRiga.class);
		riga.setFather(bufferT);
		riga.setIdAzienda(idAzienda);
		//72197 se è stato iniziale, quindi trasporto da scansia a baia
		//allora aggiorno l'ubicazione di prelievo con quella che c'è scritta nella mappa udc
		//che è per forza una ubicazione di magazzino
		if(pianoCaricoT.getStatoUdc() ==  StatoPrelievoUdcToyota.STATO_INIZIALE) {
			pianoCaricoT.setIdCodiceUbicazioneStock(pianoCaricoT.getUdc().getCodiceUbicazione());
			try {
				pianoCaricoT.save();
			} catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
			}
		}
		riga.setPianoCaricoToyota(pianoCaricoT);
		riga.setIdRigaBuffer(bufferT.getYAgvBufferRiga().size()+1);
		return riga;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List selectPianiCaricoNonSchedulati(String aziendaCorrente) throws SQLException {
		ArrayList ret = new ArrayList();
		Database db = ConnectionManager.getCurrentDatabase();
		PreparedStatement ps = cSelectPianiCaricoNonSchedul.getStatement();
		db.setString(ps, 1, aziendaCorrente);
		ResultSet rs = ps.executeQuery();
		String key = null;
		while (rs.next()){
			String idAnnoDocumento = Column.rightTrim(rs.getString(YPianoCaricoToyotaTM.ID_ANNO_DOC));
			String idNumeroDocumento = Column.rightTrim(rs.getString(YPianoCaricoToyotaTM.ID_NUMERO_DOC));
			Object[] keyParts = {aziendaCorrente, idAnnoDocumento, idNumeroDocumento};
			key = KeyHelper.buildObjectKey(keyParts);
			ret.add(key);
		}
		rs.close();
		return ret;
	}

	public int save() throws SQLException {
		iDoRun = true;
		return super.save();
	}


	@SuppressWarnings("rawtypes")
	public int save(boolean force) throws SQLException {
		int rc = 0;
		if (myScheduled != null)
		{
			String par = getDataCollector().objectToStream(this);
			par = par.replace((char)18, ';');
			myScheduled.setParameters(par);
			myScheduled.setTime(getOreInitiale());
			myScheduled.setStatus(getStato());
			rc = myScheduled.save();
		}
		if (iDoRun)
		{
			if (iOreInitiale.getTime() > TimeUtils.getCurrentTime().getTime() || iExpirationTime.getTime() < TimeUtils.getCurrentTime().getTime())
				throw new ThipException(new ErrorMessage("THIP110344"));
			if (iStato != 'V')
				throw new ThipException(new ErrorMessage("THIP110345"));

			List l = cercaBatchJob(getIdAzienda());
			if (l != null && l.size() > 1)
				throw new ThipException(new ErrorMessage("THIP110346"));

			rc = super.save(force);
		}
		iDoRun = false;
		return rc;
	}

	@SuppressWarnings("rawtypes")
	public static List cercaBatchJob(String idAzienda) {
		List l = null;
		try
		{
			String where = "RUNNER_CLASS_NAME = 'it.dnd.thip.agv.YSchedulazioneBufferAgv'";
			where += " AND STATUS = 'A'";
			where += " AND JOB_PARAMETERS LIKE '%IdAzienda=" + idAzienda + "%'";
			l = BatchJob.retrieveList(where, "", false);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return l;
	}

	public void createScheduledJob() throws SQLException {
		myScheduled = (ScheduledJob) Factory.createObject(ScheduledJob.class);
		myScheduled.setScheduledJobId(WebElement.formatStringForHTML("YPBUF_"+Azienda.getAziendaCorrente()));
		myScheduled.setDescription(descriptionSchedJobId);
		myScheduled.setBatchQueueId(batchQueueId);
		myScheduled.setRunnerClassName(Factory.getName("it.dnd.thip.agv.YSchedulazioneBufferAgv", Factory.CLASS));
		myScheduled.setJobPeriodicity(ScheduledJob.DAILY);
		if (Security.getCurrentUser() != null) {
			myScheduled.setUserId(((ThipUser) Security.getCurrentUser()).getId());
		}
	}

	@Override
	protected String getClassAdCollectionName() {
		return "YSchedulazioneBufferAgv";
	}

}