package it.dnd.thip.magazzino.documenti;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.thera.thermfw.ad.ClassADCollection;
import com.thera.thermfw.ad.ClassADCollectionManager;
import com.thera.thermfw.base.TimeUtils;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.batch.BatchRunnable;
import com.thera.thermfw.collector.ApiInfo;
import com.thera.thermfw.collector.BODataCollector;
import com.thera.thermfw.common.ErrorMessage;
import com.thera.thermfw.gui.cnr.OpenType;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;

import it.dnd.thip.base.PsnDatiDnd;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.documenti.StatoAvanzamento;
import it.thera.thip.base.documenti.web.DocumentoDataCollector;
import it.thera.thip.base.generale.UnitaMisura;
import it.thera.thip.logis.lgb.GestioneMagazzino;
import it.thera.thip.logis.lgb.GestioneMagazzinoTM;
import it.thera.thip.magazzino.documenti.CausaleDocumentoGen;
import it.thera.thip.magazzino.documenti.CausaleRigaDocGen;
import it.thera.thip.magazzino.documenti.DocMagGenRigaLotto;
import it.thera.thip.magazzino.documenti.DocMagGenerico;
import it.thera.thip.magazzino.documenti.DocMagGenericoRiga;
import it.thera.thip.magazzino.movimenti.CausaleMovimentoMagazzino;

/**
 * Batch di riallineamento delle giacenze Panthera rispetto alle giacenze Logis
 * per l'azienda e il magazzino selezionati.
 *
 * <p>Confronta le quantità presenti in {@code LOGIS.LSALDO} e
 * {@code THIP.SALDI_LOTTO}, raggruppate per azienda, magazzino, articolo,
 * lotto e versione. Per ogni differenza rilevata genera una riga di un
 * documento generico di magazzino.</p>
 *
 * <p>La quantità di rettifica è calcolata come giacenza Logis meno giacenza
 * Panthera. Le differenze positive utilizzano la causale di riga {@code RPL},
 * quelle negative la causale {@code RNL}; le quantità delle righe sono
 * espresse in valore assoluto.</p>
 *
 * <p>Serie e causale di testata sono recuperate dalla personalizzazione
 * {@code PsnDatiDnd}. Le righe vengono validate singolarmente e salvate
 * attraverso il salvataggio finale della testata.</p>
 *
 * <p>In assenza di differenze non viene creato alcun documento.</p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Andrea Gatta<br>
 * Date: 21/10/2026
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 72702    21/10/2026  AGSOF3   Prima stesura
 */

public class YRiallGiacLogis extends BatchRunnable {

	protected String iIdAzienda;
	protected String iIdMagazzino;

	protected static final String STMT_QTA_RETTIFICA = "WITH GIACENZE_LOGIS AS (\r\n"
			+ "    SELECT\r\n"
			+ "        COD_SOCIETA AS ID_AZIENDA,\r\n"
			+ "        COD_MAG_LOGICO AS ID_MAGAZZINO,\r\n"
			+ "        COD_ARTICOLO AS ID_ARTICOLO,\r\n"
			+ "        COALESCE(LOTTO01, '-') AS ID_LOTTO,\r\n"
			+ "        CASE\r\n"
			+ "            WHEN VERSIONE = '.' THEN '1'\r\n"
			+ "            ELSE VERSIONE\r\n"
			+ "        END AS ID_VERSIONE,\r\n"
			+ "        SUM(QTA1) AS GIACENZA_LOGIS\r\n"
			+ "    FROM\r\n"
			+ "        LOGIS.LSALDO\r\n"
			+ "    WHERE\r\n"
			+ "        COD_SOCIETA = ?\r\n"
			+ "        AND COD_MAG_LOGICO = ?\r\n"
			+ "    GROUP BY\r\n"
			+ "        COD_SOCIETA,\r\n"
			+ "        COD_MAG_LOGICO,\r\n"
			+ "        COD_ARTICOLO,\r\n"
			+ "        COALESCE(LOTTO01, '-'),\r\n"
			+ "        CASE\r\n"
			+ "            WHEN VERSIONE = '.' THEN '1'\r\n"
			+ "            ELSE VERSIONE\r\n"
			+ "        END\r\n"
			+ "),\r\n"
			+ "GIACENZE_THIP AS (\r\n"
			+ "    SELECT\r\n"
			+ "        ID_AZIENDA,\r\n"
			+ "        ID_MAGAZZINO,\r\n"
			+ "        ID_ARTICOLO,\r\n"
			+ "        ID_LOTTO,\r\n"
			+ "        ID_VERSIONE,\r\n"
			+ "        SUM(QTA_GIAC_PRM) AS GIACENZA_THIP\r\n"
			+ "    FROM\r\n"
			+ "        THIP.SALDI_LOTTO\r\n"
			+ "    WHERE\r\n"
			+ "        ID_AZIENDA = ?\r\n"
			+ "        AND ID_MAGAZZINO = ?\r\n"
			+ "    GROUP BY\r\n"
			+ "        ID_AZIENDA,\r\n"
			+ "        ID_MAGAZZINO,\r\n"
			+ "        ID_ARTICOLO,\r\n"
			+ "        ID_LOTTO,\r\n"
			+ "        ID_VERSIONE\r\n"
			+ ")\r\n"
			+ "SELECT\r\n"
			+ "    COALESCE(L.ID_AZIENDA, T.ID_AZIENDA) AS ID_AZIENDA,\r\n"
			+ "    COALESCE(L.ID_MAGAZZINO, T.ID_MAGAZZINO) AS ID_MAGAZZINO,\r\n"
			+ "    COALESCE(L.ID_ARTICOLO, T.ID_ARTICOLO) AS ID_ARTICOLO,\r\n"
			+ "    COALESCE(L.ID_LOTTO, T.ID_LOTTO) AS ID_LOTTO,\r\n"
			+ "    COALESCE(L.ID_VERSIONE, T.ID_VERSIONE) AS ID_VERSIONE,\r\n"
			+ "    COALESCE(L.GIACENZA_LOGIS, 0) AS GIACENZA_LOGIS,\r\n"
			+ "    COALESCE(T.GIACENZA_THIP, 0) AS GIACENZA_THIP,\r\n"
			+ "    COALESCE(L.GIACENZA_LOGIS, 0)\r\n"
			+ "        - COALESCE(T.GIACENZA_THIP, 0) AS QTA_RETTIFICA\r\n"
			+ "FROM\r\n"
			+ "    GIACENZE_LOGIS L\r\n"
			+ "FULL OUTER JOIN GIACENZE_THIP T ON\r\n"
			+ "    L.ID_AZIENDA = T.ID_AZIENDA\r\n"
			+ "    AND L.ID_MAGAZZINO = T.ID_MAGAZZINO\r\n"
			+ "    AND L.ID_ARTICOLO = T.ID_ARTICOLO\r\n"
			+ "    AND L.ID_LOTTO = T.ID_LOTTO\r\n"
			+ "    AND L.ID_VERSIONE = T.ID_VERSIONE\r\n"
			+ "WHERE\r\n"
			+ "    COALESCE(L.GIACENZA_LOGIS, 0) <> COALESCE(T.GIACENZA_THIP, 0)\r\n"
			+ "ORDER BY\r\n"
			+ "    ID_MAGAZZINO,\r\n"
			+ "    ID_ARTICOLO,\r\n"
			+ "    ID_LOTTO,\r\n"
			+ "    ID_VERSIONE";

	protected static CachedStatement cStmtQtaRettifica = new CachedStatement(STMT_QTA_RETTIFICA);

	public String getIdAzienda() {
		return iIdAzienda;
	}

	public void setIdAzienda(String iIdAzienda) {
		this.iIdAzienda = iIdAzienda;
	}

	public String getIdMagazzino() {
		return iIdMagazzino;
	}

	public void setIdMagazzino(String iIdMagazzino) {
		this.iIdMagazzino = iIdMagazzino;
	}

	public YRiallGiacLogis() {
		setIdAzienda(Azienda.getAziendaCorrente());
	}

	/**
	 * Esegue il confronto delle giacenze e genera il documento di rettifica.
	 *
	 * <p>Valorizza i parametri della query con l'azienda e il magazzino selezionati.
	 * Al primo record con quantità di rettifica diversa da zero crea la testata;
	 * per ogni record successivo crea e aggiunge una riga con articolo, versione,
	 * lotto, causale determinata dal segno e quantità assoluta.</p>
	 *
	 * <p>Al termine della lettura chiude il risultato della query e salva
	 * il documento completo delle righe. L'esito dell'elaborazione viene
	 * riportato nell'output del batch.</p>
	 *
	 * @return {@code true} se l'elaborazione termina correttamente, anche in
	 *         assenza di differenze; {@code false} in caso di errore
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected boolean run() {
		try {
			PsnDatiDnd psnDatiDND = PsnDatiDnd.getCurrentPsnDatiDnd();

			String idSerie = psnDatiDND.getSerieDocGnrRiallLogis();
			String idCausaleTes = psnDatiDND.getRCauDocGnrRiallLogis();
			String idCausalePositiva = null;
			String idCausaleNegativa = null;
			CausaleDocumentoGen cauTes = CausaleDocumentoGen.elementWithKey(getIdAzienda()+KeyHelper.KEY_SEPARATOR+idCausaleTes, 0);
			for(CausaleRigaDocGen cauRiga : ((List<CausaleRigaDocGen>)cauTes.getCausaliRiga())){
				if(cauRiga.getCausaleMovMagazzino().getQtaGiacenza() == CausaleMovimentoMagazzino.SOMMA)
					idCausalePositiva = cauRiga.getIdCausale();
				else if(cauRiga.getCausaleMovMagazzino().getQtaGiacenza() == CausaleMovimentoMagazzino.SOTTRAE)
					idCausaleNegativa = cauRiga.getIdCausale();
			}
			
			DocumentoDataCollector docDC = null;
			DocMagGenerico doc = null;

			PreparedStatement ps = cStmtQtaRettifica.getStatement();

			ps.setString(1, getIdAzienda());
			ps.setString(2, getIdMagazzino());
			ps.setString(3, getIdAzienda());
			ps.setString(4, getIdMagazzino());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					BigDecimal qtaRettifica =
							rs.getBigDecimal("QTA_RETTIFICA");

					if (qtaRettifica == null || qtaRettifica.signum() == 0) {
						continue;
					}

					if (doc == null) {
						docDC = creaDocGenTestata(
								idSerie,
								idCausaleTes);

						doc = (DocMagGenerico) docDC.getBo();
					}

					String idCausaleRig = qtaRettifica.signum() < 0
							? idCausaleNegativa
									: idCausalePositiva;

					DocMagGenericoRiga riga = creaDocGenRiga(
							doc,
							rs.getString("ID_ARTICOLO"),
							rs.getInt("ID_VERSIONE"),
							rs.getString("ID_LOTTO"),
							idCausaleRig,
							qtaRettifica.abs());

					doc.getRighe().add(riga);
				}
			}

			// Il ResultSet è chiuso prima del salvataggio con commit.
			if (doc != null && doc.getRighe().size() > 0) {
				doc.setSalvaRighe(true);

				docDC.setBo(doc);
				docDC.setAutoCommit(true);

				int res = docDC.save();
				if (res == BODataCollector.ERROR) {
					throw new Exception(leggiErrori(docDC));
				}

				output.println(
						"Creato correttamente il documento generico: "
								+ KeyHelper.formatKeyString(doc.getKey()));
			} else {
				output.println(
						"Nessuna differenza di giacenza da rettificare.");
			}

			return true;

		} catch (Exception e) {
			output.println(
					"Errore nella creazione del documento di rettifica: "
							+ e.getMessage());

			e.printStackTrace(Trace.excStream);
			return false;
		}
	}

	/**
	 * Crea e salva la testata del documento generico di rettifica senza salvarne
	 * le righe e senza eseguire il commit tramite il data collector.
	 *
	 * <p>Inizializza il data collector e i servizi di sicurezza per un nuovo
	 * documento. Imposta l'azienda e il magazzino selezionati, la data corrente,
	 * la serie e la causale di testata ricevute.</p>
	 *
	 * <p>Il data collector restituito viene utilizzato per il successivo
	 * salvataggio del documento completo delle righe.</p>
	 *
	 * @param idSerie identificativo della serie di numerazione del documento
	 * @param idCausaleTes identificativo della causale di testata
	 * @return data collector associato alla testata creata
	 * @throws Exception se l'inizializzazione o il salvataggio della testata
	 *                   non termina correttamente
	 */
	protected DocumentoDataCollector creaDocGenTestata(
			String idSerie,
			String idCausaleTes) throws Exception {

		DocumentoDataCollector docDC = (DocumentoDataCollector)
				createDataCollector("DocMagGenerico");

		int res = docDC.initSecurityServices(
				OpenType.NEW, true, true, true);

		if (res == BODataCollector.ERROR) {
			throw new Exception(leggiErrori(docDC));
		}

		DocMagGenerico doc = (DocMagGenerico) docDC.getBo();

		doc.setIdAzienda(getIdAzienda());

		doc.getNumeratoreHandler()
		.setDataDocumento(TimeUtils.getCurrentDate());

		doc.getNumeratoreHandler().setIdSerie(idSerie);
		doc.setIdCau(idCausaleTes);
		doc.setIdMagazzino(getIdMagazzino());

		docDC.setOnBORecursive();

		if (doc.getDataDocumento() == null) {
			doc.getNumeratoreHandler()
			.setDataDocumento(TimeUtils.getCurrentDate());
		}

		doc.setIdMagazzino(getIdMagazzino());
		doc.setSalvaRighe(false);

		docDC.loadAttValue();
		docDC.setAutoCommit(false);

		res = docDC.save();
		if (res == BODataCollector.ERROR) {
			throw new Exception(leggiErrori(docDC));
		}

		return docDC;
	}

	/**
	 * Crea e valida una riga del documento generico per una differenza di giacenza.
	 *
	 * <p>Associa la riga alla testata e imposta azienda, magazzino, data di
	 * registrazione, causale, articolo e versione. Recupera dall'articolo le
	 * unità di misura e, se prevista, calcola la quantità secondaria mediante
	 * la conversione arrotondata.</p>
	 *
	 * <p>Per gli articoli gestiti a lotti crea il dettaglio lotto con le
	 * medesime quantità della riga. Imposta lo stato di avanzamento a
	 * {@code DEFINITIVO} ed esegue i controlli del data collector.</p>
	 *
	 * <p>La riga restituita non viene salvata né aggiunta alla collezione della
	 * testata da questo metodo: entrambe le operazioni sono gestite dal chiamante.</p>
	 *
	 * @param testata documento generico a cui associare la riga
	 * @param idArticolo identificativo dell'articolo da rettificare
	 * @param idVersione versione dell'articolo da rettificare
	 * @param idLotto identificativo del lotto, obbligatorio per articoli
	 *                gestiti a lotti
	 * @param idCausaleRig causale di rettifica positiva o negativa
	 * @param quantita quantità di rettifica in unità di misura primaria,
	 *                 espressa in valore assoluto e maggiore di zero
	 * @return riga creata e validata, non ancora salvata
	 * @throws Exception se l'inizializzazione non riesce, l'articolo non esiste,
	 *                   manca l'unità di misura primaria, manca un lotto
	 *                   obbligatorio oppure la validazione della riga fallisce
	 */
	@SuppressWarnings("unchecked")
	protected DocMagGenericoRiga creaDocGenRiga(
			DocMagGenerico testata,
			String idArticolo,
			int idVersione,
			String idLotto,
			String idCausaleRig,
			BigDecimal quantita) throws Exception {

		DocumentoDataCollector rigaDC = (DocumentoDataCollector)
				createDataCollector("DocMagGenericoRiga");

		int res = rigaDC.initSecurityServices(
				OpenType.NEW, true, true, true);

		if (res == BODataCollector.ERROR) {
			throw new Exception(leggiErrori(rigaDC));
		}

		DocMagGenericoRiga riga =
				(DocMagGenericoRiga) rigaDC.getBo();

		riga.setIdAzienda(testata.getIdAzienda());
		riga.setTestata(testata);
		riga.completaBO();

		rigaDC.setOnBORecursive();

		riga.setDataRegistrazione(testata.getDataDocumento());
		riga.setIdCauRig(idCausaleRig);
		riga.setIdMagazzino(testata.getIdMagazzino());
		riga.setIdArticolo(idArticolo);
		riga.setIdVersioneRcs(idVersione);

		if (riga.getArticolo() == null) {
			throw new Exception(
					"Articolo non esistente: " + idArticolo);
		}

		UnitaMisura umPrim = riga.getArticolo().getUMPrmMag();
		UnitaMisura umSec = riga.getArticolo().getUMSecMag();

		if (umPrim == null) {
			throw new Exception(
					"Unità di misura primaria non valorizzata "
							+ "per l'articolo " + idArticolo);
		}

		riga.setIdUMPrm(umPrim.getIdUnitaMisura());

		if (umSec != null) {
			riga.setIdUMSec(umSec.getIdUnitaMisura());
		}

		riga.setOperatoreConversioneUM(
				riga.getArticolo().getOperConverUM());

		riga.getQuantita().setRicalcoloQtaFattoreConv(false);
		riga.getQuantita().setQuantitaInUMPrm(quantita);

		BigDecimal quantitaSecondaria = null;

		if (umSec != null) {
			quantitaSecondaria =
					riga.getArticolo().convertiUMArrotondate(
							quantita,
							umPrim,
							umSec,
							null);

			riga.getQuantita()
			.setQuantitaInUMSec(quantitaSecondaria);
		}

		riga.setStatoAvanzamento(StatoAvanzamento.DEFINITIVO);

		if (riga.getArticolo().isArticLotto()) {
			if (idLotto == null || idLotto.trim().isEmpty()) {
				throw new Exception(
						"Lotto non valorizzato per l'articolo "
								+ idArticolo);
			}

			DocMagGenRigaLotto rigaLotto = (DocMagGenRigaLotto)
					Factory.createObject(DocMagGenRigaLotto.class);

			rigaLotto.setFather(riga);
			rigaLotto.setIdArticolo(idArticolo);
			rigaLotto.setIdLotto(idLotto);

			rigaLotto.getQuantita()
			.setRicalcoloQtaFattoreConv(false);

			rigaLotto.getQuantita()
			.setQuantitaInUMPrm(quantita);

			if (quantitaSecondaria != null) {
				rigaLotto.getQuantita()
				.setQuantitaInUMSec(quantitaSecondaria);
			}

			riga.getRigheLotto().add(rigaLotto);
		}

		rigaDC.loadAttValue();

		res = rigaDC.check();
		if (res == BODataCollector.ERROR) {
			throw new Exception(
					"Articolo " + idArticolo
					+ ", versione " + idVersione
					+ ", lotto " + idLotto
					+ ": " + leggiErrori(rigaDC));
		}

		// Il salvataggio viene eseguito dalla testata.
		return riga;
	}
	protected BODataCollector createDataCollector(
			String className) throws Exception {

		ClassADCollection cadc =
				ClassADCollectionManager.collectionWithName(className);

		String collectorName = cadc.getBODataCollector();

		BODataCollector dc;

		if (collectorName != null) {
			dc = (BODataCollector) Factory.createObject(collectorName);
		} else {
			dc = new BODataCollector();
		}

		ApiInfo info = new ApiInfo();
		info.doNotAddNullComponentsToGroup = true;

		dc.setApiInfo(info);
		dc.initialize(cadc.getClassName(), true);

		return dc;
	}

	protected String leggiErrori(BODataCollector dc) {
		StringBuilder errori = new StringBuilder();

		for (Object objErr : dc.getErrorList().getErrors()) {
			ErrorMessage em = (ErrorMessage) objErr;

			if (errori.length() > 0) {
				errori.append("; ");
			}

			errori.append(em.getAttOrGroupLabel())
			.append(" ")
			.append(em.getLongText());
		}

		return errori.length() > 0
				? errori.toString()
						: "Errore durante la validazione o il salvataggio.";
	}

	@SuppressWarnings("unchecked")
	public ErrorMessage checkIdMagazzino() {
		if (getIdMagazzino() != null) {
			String where = GestioneMagazzinoTM.COD_SOCIETA + " = '" + getIdAzienda() + "' " + "AND "
					+ GestioneMagazzinoTM.COD_MAG_LOGICO + " = '" + getIdMagazzino() + "' ";
			List<GestioneMagazzino> mags = new ArrayList<GestioneMagazzino>();
			try {
				mags = GestioneMagazzino.retrieveList(where, "", false);
			} catch (Exception e) {
				e.printStackTrace(Trace.excStream);
			}
			if (mags.isEmpty() || mags.size() == 0)
				return new ErrorMessage("BAS0000078", "Magazzino non abilitato in logis.");
		} else
			return new ErrorMessage("BAS0000078", "Magazzino obbligatorio.");
		return null;
	}

	@Override
	protected String getClassAdCollectionName() {
		return "YRiallGiacLogis";
	}

}
