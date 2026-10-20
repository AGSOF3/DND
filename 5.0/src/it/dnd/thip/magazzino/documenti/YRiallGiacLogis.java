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
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;

import it.dnd.thip.base.PsnDatiDnd;
import it.thera.thip.base.articolo.Articolo;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.dipendente.web.DatiWebArticolo;
import it.thera.thip.base.documenti.StatoAvanzamento;
import it.thera.thip.base.documenti.web.DocumentoDataCollector;
import it.thera.thip.base.generale.UnitaMisura;
import it.thera.thip.logis.lgb.GestioneMagazzino;
import it.thera.thip.logis.lgb.GestioneMagazzinoTM;
import it.thera.thip.magazzino.documenti.DocMagGenRigaLotto;
import it.thera.thip.magazzino.documenti.DocMagGenerico;
import it.thera.thip.magazzino.documenti.DocMagGenericoRiga;

public class YRiallGiacLogis extends BatchRunnable {

	protected String iIdAzienda;
	protected String iIdMagazzino;

	protected static final String STMT_QTA_RETTIFICA = "WITH GIACENZE_LOGIS AS (\r\n" + "    SELECT\r\n"
			+ "        COD_SOCIETA AS ID_AZIENDA,\r\n" + "        COD_MAG_LOGICO AS ID_MAGAZZINO,\r\n"
			+ "        COD_ARTICOLO AS ID_ARTICOLO,\r\n" + "        COALESCE(LOTTO01, '-') AS ID_LOTTO,\r\n"
			+ "        CASE\r\n" + "            WHEN VERSIONE = '.' THEN '1'\r\n" + "            ELSE VERSIONE\r\n"
			+ "        END AS ID_VERSIONE,\r\n" + "        SUM(QTA1) AS GIACENZA_LOGIS\r\n" + "    FROM\r\n"
			+ "        LOGIS.LSALDO\r\n" + "    WHERE\r\n" + "        COD_SOCIETA = ?\r\n"
			+ "        AND COD_MAG_LOGICO = ?\r\n" + "    GROUP BY\r\n" + "        COD_SOCIETA,\r\n"
			+ "        COD_MAG_LOGICO,\r\n" + "        COD_ARTICOLO,\r\n" + "        COALESCE(LOTTO01, '-'),\r\n"
			+ "        CASE\r\n" + "            WHEN VERSIONE = '.' THEN '1'\r\n" + "            ELSE VERSIONE\r\n"
			+ "        END\r\n" + "),\r\n" + "GIACENZE_THIP AS (\r\n" + "    SELECT\r\n" + "        ID_AZIENDA,\r\n"
			+ "        ID_MAGAZZINO,\r\n" + "        ID_ARTICOLO,\r\n" + "        ID_LOTTO,\r\n"
			+ "        ID_VERSIONE,\r\n" + "        SUM(QTA_GIAC_PRM) AS GIACENZA_THIP\r\n" + "    FROM\r\n"
			+ "        THIP.SALDI_LOTTO\r\n" + "    WHERE\r\n" + "        ID_AZIENDA = ?\r\n"
			+ "        AND ID_MAGAZZINO = ?\r\n" + "    GROUP BY\r\n" + "        ID_AZIENDA,\r\n"
			+ "        ID_MAGAZZINO,\r\n" + "        ID_ARTICOLO,\r\n" + "        ID_LOTTO,\r\n"
			+ "        ID_VERSIONE\r\n" + ")\r\n" + "SELECT\r\n"
			+ "    COALESCE(L.ID_AZIENDA, T.ID_AZIENDA) AS ID_AZIENDA,\r\n"
			+ "    COALESCE(L.ID_MAGAZZINO, T.ID_MAGAZZINO) AS ID_MAGAZZINO,\r\n"
			+ "    COALESCE(L.ID_ARTICOLO, T.ID_ARTICOLO) AS ID_ARTICOLO,\r\n"
			+ "    COALESCE(L.ID_LOTTO, T.ID_LOTTO) AS ID_LOTTO,\r\n"
			+ "    COALESCE(L.ID_VERSIONE, T.ID_VERSIONE) AS ID_VERSIONE,\r\n"
			+ "    COALESCE(L.GIACENZA_LOGIS, 0) AS GIACENZA_LOGIS,\r\n"
			+ "    COALESCE(T.GIACENZA_THIP, 0) AS GIACENZA_THIP,\r\n" + "    COALESCE(L.GIACENZA_LOGIS, 0)\r\n"
			+ "        - COALESCE(T.GIACENZA_THIP, 0) AS QTA_RETTIFICA\r\n" + "FROM\r\n" + "    GIACENZE_LOGIS L\r\n"
			+ "FULL OUTER JOIN GIACENZE_THIP T ON\r\n" + "    L.ID_AZIENDA = T.ID_AZIENDA\r\n"
			+ "    AND L.ID_MAGAZZINO = T.ID_MAGAZZINO\r\n" + "    AND L.ID_ARTICOLO = T.ID_ARTICOLO\r\n"
			+ "    AND L.ID_LOTTO = T.ID_LOTTO\r\n" + "    AND L.ID_VERSIONE = T.ID_VERSIONE\r\n" + "WHERE\r\n"
			+ "    COALESCE(L.GIACENZA_LOGIS, 0) <> COALESCE(T.GIACENZA_THIP, 0)\r\n" + "ORDER BY\r\n"
			+ "    ID_MAGAZZINO,\r\n" + "    ID_ARTICOLO,\r\n" + "    ID_LOTTO,\r\n" + "    ID_VERSIONE";

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

	@Override
	protected boolean run() {
		try {
			PsnDatiDnd psnDatiDND = PsnDatiDnd.getCurrentPsnDatiDnd();

			String idSerie = psnDatiDND.getSerieDocGnrRiallLogis();
			String idCausaleTes = psnDatiDND.getRCauDocGnrRiallLogis();
			String idCausalePositiva = "RPL";
			String idCausaleNegativa = "RNL";

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
