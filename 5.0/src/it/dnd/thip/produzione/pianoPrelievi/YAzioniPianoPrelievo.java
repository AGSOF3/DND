package it.dnd.thip.produzione.pianoPrelievi;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import com.thera.thermfw.base.TimeUtils;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.base.Utils;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.CopyException;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.security.Entity;

import it.dnd.thip.logis.lgb.StatoPrelievoUdcToyota;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.cliente.ClienteVendita;
import it.thera.thip.base.cliente.IdentificativoBanca;
//import it.thera.thip.base.comuniVenAcq.web.RecuperaCausaleDefaultDocOrd;
import it.thera.thip.base.documenti.StatoAvanzamento;
import it.thera.thip.base.generale.Numeratore;
import it.thera.thip.base.generale.NumeratoreException;
import it.thera.thip.base.generale.NumeratoreHandler;
import it.thera.thip.base.generale.Serie;
import it.thera.thip.produzione.ordese.OrdineEsecutivo;
import it.thera.thip.produzione.pianoPrelievi.AzioniPianoPrelievo;
import it.thera.thip.produzione.pianoPrelievi.GenerazionePianiPrlBatch;
import it.thera.thip.produzione.pianoPrelievi.PianoPrelieviRiga;
import it.thera.thip.produzione.pianoPrelievi.PianoPrelieviTestata;
import it.thera.thip.produzione.pianoPrelievi.WrapperPianoPrlDati;
import it.thera.thip.servizi.comuniNlgCnt.UtilGener;
//import it.thera.thip.produzione.pianoPrelievi.WrapperPianoPrlDatiTM; 72080
import it.thera.thip.vendite.documentoVE.DocumentoVenRigaPrm;
import it.thera.thip.vendite.documentoVE.DocumentoVenRigaPrmTM;
import it.thera.thip.vendite.documentoVE.DocumentoVendita;
import it.thera.thip.vendite.documentoVE.DocumentoVenditaTM;
import it.thera.thip.vendite.generaleVE.CausaleDocumentoVendita;
import it.thera.thip.vendite.generaleVE.CondizioniCompatibilitaEvasione;
import it.thera.thip.vendite.generaleVE.PersDatiVen;
import it.thera.thip.vendite.generaleVE.TipoReperimento;
import it.thera.thip.vendite.ordineVE.GestoreEvasioneVendita;
import it.thera.thip.vendite.ordineVE.OrdineVendita;
import it.thera.thip.vendite.ordineVE.OrdineVenditaRiga;

/**
 * Ridefinizione della classe standard {@code AzioniPianoPrelievo}
 * 
 * <p>
 * Company: Softre Solutions<br>
 * Author: Andrea Gatta<br>
 * Date: 07/03/2025
 * </p>
 */

/*
 * Revisions:
 * Number	Date		Owner	Description
 * 71868	07/03/2025	AGSOF3	Aggiungo nella where standard la condizione per cui estraggo solo gli ordini esecutivi schedulati
 * 								se in generazione piani di prelievo è stato flaggato "solo ordini programmati"
 * 			10/04/2025	AGSOF3	Corretto stato in costruisciWhere(), avevo erroneamento messo SCHEDULATO invece di PROGRAMMATO	
 * 71994	10/06/2025	DSSOF3	Gestione data consegna confermata in ricerca documento vendita e in creazione di esso.
 * 72020	30/06/2025	DSSOF3	Sistemare where accorpabilita
 * 72080	06/08/2025	DSSOF3  Remmare generazione documento vendita, rendere statici alcuni metodi che usero' in altre classi
 * 72210	16/11/2025	AGSOF3	Evado commenti ord ven in generazione doc ven	
 * 72468	15/05/2026	AGSOF3	Dalla where di estrazione dei materiali ho rimosso la data di fine orizzonte
 */

public class YAzioniPianoPrelievo extends AzioniPianoPrelievo{

	public static final String STMT_ESCLUDI_PIANI_PRELIEVO_CON_PIANO_CARICO_TOYOTA_IN_CORSO = " AND NOT EXISTS ( "
			+ "    SELECT 1 "
			+ "    FROM THIP.PIANI_PRL_RIG R "
			+ "    INNER JOIN THIPPERS.YPIANO_CARICO_TOYOTA_RIG RIG "
			+ "        ON R.ID_AZIENDA = THIP.PIANI_PRL_TES.ID_AZIENDA "
			+ "        AND R.ID_ANNO_ORD = RIG.ID_ANNO_ORDINE_RIGA_MAT "
			+ "        AND R.ID_NUMERO_ORD = RIG.ID_NUMERO_ORD_RIGA_MAT "
			+ "        AND R.ID_RIGA_ATTIVITA = RIG.ID_RIGA_ATTIVITA_RIGA_MAT "
			+ "        AND R.ID_RIGA_MATERIALE = RIG.ID_RIGA_ATTIVITA_RIGA_MAT "
			+ "    INNER JOIN THIPPERS.YPIANO_CARICO_TOYOTA_TES TES "
			+ "        ON RIG.ID_AZIENDA = TES.ID_AZIENDA "
			+ "        AND RIG.ID_ANNO_DOC = TES.ID_ANNO_DOC "
			+ "        AND RIG.ID_NUMERO_DOC = TES.ID_NUMERO_DOC "
			+ "    WHERE "
			+ "        R.ID_AZIENDA = THIP.PIANI_PRL_TES.ID_AZIENDA "
			+ "        AND R.ID_MODELLO_PRL = THIP.PIANI_PRL_TES.ID_MODELLO_PRL "
			+ "        AND R.TIPO_MODELLO_PRL = THIP.PIANI_PRL_TES.TIPO_MODELLO_PRL "
			+ "        AND R.ID_ANNO_ORD = THIP.PIANI_PRL_TES.ID_ANNO_ORD "
			+ "        AND R.ID_NUMERO_ORD = THIP.PIANI_PRL_TES.ID_NUMERO_ORD "
			+ "        AND R.ID_RIGA_ATTIVITA = THIP.PIANI_PRL_TES.ID_RIGA_ATTIVITA "
			+ "        AND TES.STATO_UDC > '"+StatoPrelievoUdcToyota.STATO_INIZIALE+"' "
			+ "        AND R.ID_MODELLO_PRL = THIP.PIANI_PRL_TES.ID_MODELLO_PRL AND R.TIPO_MODELLO_PRL = THIP.PIANI_PRL_TES.TIPO_MODELLO_PRL "
			+ ")";

	private static final String STMT_QTA_EVASA_MISSIONE_RIGA_MAT = "SELECT "
			+ "	SUM(M.QTA1_EVASA) AS EVASO_PER_RIGA_MATERIALE "
			+ "FROM "
			+ "	THIP.ORD_ESEC_ATV_V02 V "
			+ "INNER JOIN LOGIS.LLISTA_TESTA LT "
			+ "ON "
			+ "	V.ID_AZIENDA = LT.COD_SOCIETA "
			+ "	AND V.ID_ANNO_ORD = SUBSTRING(LT.RIF_PARTNER, 1, 4) "
			+ "	AND V.ID_NUMERO_ORD = SUBSTRING(LT.RIF_PARTNER, 6, 10) "
			+ "	AND V.R_OPERAZIONE = SUBSTRING(LT.RIF_PARTNER, 17, 4) "
			+ "INNER JOIN LOGIS.LLISTA_RIGA LR "
			+ "ON "
			+ "	LR.COD_SOCIETA = LT.COD_SOCIETA "
			+ "	AND LR.COD_LISTA = LT.CODICE "
			+ "	AND LR.NUM_RIGA_HOST = V.ID_RIGA_MATERIALE "
			+ "INNER JOIN LOGIS.LMISSIONE M "
			+ "ON "
			+ "	M.COD_SOCIETA = LT.COD_SOCIETA "
			+ "	AND M.COD_LISTA = LT.CODICE "
			+ "	AND M.COD_RIGA_LISTA = LR.CODICE "
			+ "WHERE "
			+ "	M.STATO_MISSIONE = 'T' "
			+ "	AND V.ID_AZIENDA = ? "
			+ "	AND V.ID_ANNO_ORD = ? "
			+ "	AND V.ID_NUMERO_ORD = ? "
			+ "	AND V.ID_RIGA_ATTIVITA = ? "
			+ "	AND V.ID_RIGA_MATERIALE = ?";
	public CachedStatement cSelezionaQtaEvasaMissioniRigaMateriale = new CachedStatement(STMT_QTA_EVASA_MISSIONE_RIGA_MAT);

	protected boolean soloOrdiniProgrammati = false;

	public boolean isSoloOrdiniProgrammati() {
		return soloOrdiniProgrammati;
	}

	public void setSoloOrdiniProgrammati(boolean soloOrdiniProgrammati) {
		this.soloOrdiniProgrammati = soloOrdiniProgrammati;
	}

	public YAzioniPianoPrelievo() {
		super();
		cCancellaPianiPrl = new CachedStatement(CANCELLA_PIANI_PRL+STMT_ESCLUDI_PIANI_PRELIEVO_CON_PIANO_CARICO_TOYOTA_IN_CORSO);
	}

	/**
	 * Setto sull'oggetto {@code YAzioniPianoPrelievo} il valore del flag personalizzato soloOrdiniProgrammati
	 * recuperato dal lancio generazione piani prelievo {@code YGenerazionePianiPrlBatch}
	 */
	@Override
	public void inizializzaDatiPers(GenerazionePianiPrlBatch batch) throws Exception {
		super.inizializzaDatiPers(batch);
		YGenerazionePianiPrlBatch yBatch = (YGenerazionePianiPrlBatch) batch;
		this.setSoloOrdiniProgrammati(yBatch.isSoloOrdiniProgrammati());
	}

	/**
	 * Costruisce la clausola WHERE rimuovendo il filtro standard
	 * su DATA_PRV_IMPIEGO generato dalla superclasse.
	 *
	 * La regex intercetta condizioni del tipo:
	 *
	 * AND DATA_PRV_IMPIEGO <= CONVERT(DATETIME, 'yyyy-MM-dd', 120)
	 *
	 * indipendentemente dalla data presente.
	 */
	@Override
	public String costruisciWhere() {
		String where = super.costruisciWhere();
		String regexData = "\\s+AND DATA_PRV_IMPIEGO <= CONVERT\\(DATETIME, '\\d{4}-\\d{2}-\\d{2}', 120\\)";
		where = where.replaceAll(regexData, "");//72468
		return where;
	}	

	/**
	 * Salva la testata del piano di prelievo e, se l'operazione ha avuto successo e si tratta
	 * di un piano associato a soli ordini programmati, gestisce la creazione (o accorpamento) 
	 * del documento di vendita e delle relative righe.
	 * <p>
	 * Il metodo segue questi passaggi:
	 * <ol>
	 *   <li>Invoca il salvataggio della testata tramite il metodo della superclasse.</li>
	 *   <li>Verifica se la testata è effettivamente salvata nel database ({@code isOnDB()}).</li>
	 *   <li>Se attivo il flag {@code isSoloOrdiniProgrammati()}, procede con:
	 *     <ul>
	 *       <li>Recupero dell'ordine esecutivo collegato alla testata.</li>
	 *       <li>Controllo della presenza di una riga d'ordine di vendita valida.</li>
	 *       <li>Verifica se la riga dell'ordine è già stata evasa tramite {@code controllaEvasioneRiga()}.</li>
	 *       <li>Se non ancora evasa, ricerca o crea il documento di vendita accorpabile.</li>
	 *       <li>Trasforma la riga d'ordine in una riga di documento di vendita.</li>
	 *       <li>Salva la riga e, in caso di successo, esegue il commit della transazione.</li>
	 *     </ul>
	 *   </li>
	 * </ol>
	 * Tutte le eccezioni vengono catturate e tracciate tramite {@code Trace.excStream}.
	 *
	 * @param testataPiano  La testata del piano di prelievo da salvare.
	 * @param eseguiCommit  {@code true} se il commit deve essere eseguito subito dopo il salvataggio.
	 */
	//@SuppressWarnings("unchecked") 72080 Remmata
	@Override
	public void salvaTestataPiano(PianoPrelieviTestata testataPiano, boolean eseguiCommit) {
		super.salvaTestataPiano(testataPiano, eseguiCommit);
		//testo se la testata piano è su db, questo è l'unico modo che ho 
		//per sapere se il salvataggio fatto nella super è andato a buon fine
		//72080 Inizio Remmata
		/*if(testataPiano.isOnDB() && isSoloOrdiniProgrammati()) {
			OrdineEsecutivo ordEsec = testataPiano.getAttivitaEsecutiva() != null ? testataPiano.getAttivitaEsecutiva().getOrdineEsecutivo() : null;
			if(ordEsec != null && ordEsec.getOrdineVenditaRiga() != null) {
				OrdineVenditaRiga ordVenRig = ordEsec.getOrdineVenditaRiga();
				try {
					boolean isRigaGiaEvasa = controllaEvasioneRiga(ordVenRig);
					if(!isRigaGiaEvasa) {
						DocumentoVendita documento = null;
						documento = cercaDocumentoVenditaAccorpabile(ordVenRig);
						if (documento == null) {
							documento = creaDocumentoVendita(ordVenRig);
						}
						DocumentoVenRigaPrm docRiga = trasformaRiga(ordVenRig,documento,ordEsec);
						documento.getRighe().add(docRiga);
						documento.setSalvataggioRigheForzato(true);
						int rc = documento.save();
						if(rc > 0) {
							ConnectionManager.commit();
						}
					}
				}catch(Exception e) {
					e.printStackTrace(Trace.excStream);
				}
			}
		}*/
		//72080 Fine remmata
	}

	/**
	 * Controlla se una riga di ordine di vendita è già stata evasa, verificando la presenza
	 * di una corrispondente riga di {@link DocumentoVenRigaPrm} nel database.
	 * <p>
	 * Il controllo avviene costruendo una clausola {@code WHERE} basata sui seguenti campi
	 * della riga d'ordine:
	 * <ul>
	 *   <li>ID azienda</li>
	 *   <li>Anno del documento</li>
	 *   <li>Numero del documento</li>
	 *   <li>Numero riga del documento</li>
	 *   <li>Dettaglio della riga</li>
	 * </ul>
	 * Se esiste almeno una riga di documento vendita che soddisfa questi criteri,
	 * la riga è considerata già evasa.
	 *
	 * @param ordVenRig La riga dell'ordine di vendita da controllare.
	 * @return {@code true} se la riga risulta già evasa, {@code false} altrimenti.
	 * @throws ClassNotFoundException Se si verifica un errore nel caricamento delle classi durante l'accesso al database.
	 * @throws InstantiationException Se si verifica un errore nell'istanza di oggetti persistenti.
	 * @throws IllegalAccessException Se l'accesso riflessivo a una classe o membro non è consentito.
	 * @throws SQLException Se si verifica un errore nell'interrogazione al database.
	 */
	@SuppressWarnings("unchecked")
	protected boolean controllaEvasioneRiga(OrdineVenditaRiga ordVenRig) throws ClassNotFoundException, InstantiationException, IllegalAccessException, SQLException {
		String where = DocumentoVenRigaPrmTM.ID_AZIENDA + " = '"+ordVenRig.getIdAzienda()+"' " + 
				" AND " + DocumentoVenRigaPrmTM.R_ANNO_ORD + " = '"+ordVenRig.getAnnoDocumento()+"' " +
				" AND " + DocumentoVenRigaPrmTM.R_NUMERO_ORD + " = '"+ordVenRig.getNumeroDocumento()+"' " +
				" AND " + DocumentoVenRigaPrmTM.R_RIGA_ORD + " = '"+ordVenRig.getNumeroRigaDocumento()+"' " +
				" AND " + DocumentoVenRigaPrmTM.R_DET_RIGA_ORD + " = '"+ordVenRig.getDettaglioRigaDocumento()+"' " ;
		List<DocumentoVenRigaPrm> righe = DocumentoVenRigaPrm.retrieveList(DocumentoVenRigaPrm.class, where, "", false);
		if(righe.size()>0)
			return true;
		return false;
	}

	/**
	 * Trasforma una riga di ordine di vendita in una riga di {@link DocumentoVenRigaPrm} 
	 * associata al documento di vendita e all'ordine esecutivo specificati.
	 * <p>
	 * Il metodo crea una nuova riga di documento di vendita e la popola con:
	 * <ul>
	 *   <li>Dati anagrafici e riferimenti (azienda, testata, causale, stato avanzamento).</li>
	 *   <li>Informazioni relative all'articolo, unità di misura e quantità proposte per l'evasione.</li>
	 *   <li>Informazioni economiche (prezzo, sconti, maggiorazioni).</li>
	 *   <li>Dati commerciali come agenti, subagenti e provvigioni.</li>
	 *   <li>Riferimento alla riga di ordine da cui è stata generata.</li>
	 * </ul>
	 *
	 * @param ordVenRig La riga dell'ordine di vendita da trasformare.
	 * @param docVen Il documento di vendita a cui associare la nuova riga.
	 * @param ordEsec L'ordine esecutivo contenente le quantità residue per l'evasione.
	 * @return Un oggetto {@link DocumentoVenRigaPrm} completamente inizializzato sulla base dei parametri forniti.
	 */
	protected DocumentoVenRigaPrm trasformaRiga(OrdineVenditaRiga ordVenRig, DocumentoVendita docVen, OrdineEsecutivo ordEsec) {
		DocumentoVenRigaPrm docVenRig = (DocumentoVenRigaPrm) Factory.createObject(DocumentoVenRigaPrm.class);
		docVenRig.setIdAzienda(Azienda.getAziendaCorrente());
		docVenRig.setTestata(docVen);
		docVenRig.setIdCauRig(docVen.getCausale().getIdCausaleRigaDocumVen());
		docVenRig.setStatoAvanzamento(StatoAvanzamento.PROVVISORIO);
		docVenRig.setIdArticolo(ordVenRig.getIdArticolo());		
		docVenRig.getQtaPropostaEvasione().setQuantitaInUMPrm(ordEsec.getQtaResiduaUMPrm());
		docVenRig.setIdUMPrm(ordVenRig.getIdUMPrm());
		docVenRig.getQtaPropostaEvasione().setQuantitaInUMRif(ordEsec.getQtaResiduaUMPrm());
		docVenRig.setIdUMRif(ordVenRig.getIdUMRif());
		docVenRig.getQtaPropostaEvasione().setQuantitaInUMSec(ordEsec.getQtaResiduaUMSec());
		docVenRig.setIdUMSec(ordVenRig.getIdUMSec());
		docVenRig.completaBO();
		docVenRig.setIdAssogIVA(ordVenRig.getIdAssogIVA());
		docVenRig.setIdListino(ordVenRig.getIdListino());
		docVenRig.setPrezzo(ordVenRig.getPrezzo());
		docVenRig.setScontoArticolo1(ordVenRig.getScontoArticolo1());
		docVenRig.setScontoArticolo2(ordVenRig.getScontoArticolo2());
		docVenRig.setMaggiorazione(ordVenRig.getMaggiorazione());
		docVenRig.setSconto(ordVenRig.getSconto());
		docVenRig.setPrcScontoIntestatario(ordVenRig.getPrcScontoIntestatario());
		docVenRig.setPrcScontoModalita(ordVenRig.getPrcScontoModalita());
		docVenRig.setIdAgente(ordVenRig.getIdAgente());
		docVenRig.setIdAgenteSub(ordVenRig.getIdSubagente());
		docVenRig.setProvvigione1Agente(ordVenRig.getProvvigione1Agente());
		docVenRig.setProvvigione2Agente(ordVenRig.getProvvigione2Agente());
		docVenRig.setProvvigione1Subagente(ordVenRig.getProvvigione1Subagente());
		docVenRig.setProvvigione2Subagente(ordVenRig.getProvvigione2Subagente());
		docVenRig.setRigaOrdine(ordVenRig);
		return docVenRig;
	}

	/**
	 * Crea e inizializza un nuovo {@link DocumentoVendita} a partire da una riga di ordine di vendita.
	 * <p>
	 * Il metodo:
	 * <ul>
	 *   <li>Recupera la data corrente.</li>
	 *   <li>Determina l'anno fiscale in base alla data e al numeratore specificato.</li>
	 *   <li>Recupera la serie associata al numeratore se necessario.</li>
	 *   <li>Imposta i dati base del documento di vendita (azienda, data, anno fiscale, serie, cliente, causale).</li>
	 *   <li>Imposta lo stato del documento come {@code PROVVISORIO}.</li>
	 * </ul>
	 * Se durante il processo mancano dati fondamentali come l'anno fiscale o la serie (quando richiesta), il metodo restituisce {@code null}.
	 *
	 * @param ordVenRig La riga dell'ordine di vendita da cui estrarre i dati per il nuovo documento.
	 * @return Un oggetto {@link DocumentoVendita} correttamente inizializzato, oppure {@code null} se non è possibile completare l'inizializzazione.
	 * @throws SQLException Se si verifica un errore nell'accesso al database durante il recupero dei dati.
	 */
	@SuppressWarnings("rawtypes")
	public static DocumentoVendita creaDocumentoVendita(OrdineVenditaRiga ordVenRig) throws SQLException {
		DocumentoVendita docVen = (DocumentoVendita) Factory.createObject(DocumentoVendita.class);
		String idNumeratore = "DOC_VEN";
		java.sql.Date data = TimeUtils.getCurrentDate();
		String annoFiscale = null;
		String idSerie = null;
		try {
			annoFiscale = NumeratoreHandler.getAnno(data,idNumeratore);
		} catch (NumeratoreException ex) {
			ex.printStackTrace(Trace.excStream);
		}
		if(annoFiscale == null) {
			return null;
		}else
		{
			annoFiscale = annoFiscale.trim();
			String keyNumeratore = KeyHelper.buildObjectKey(new String[]{Azienda.getAziendaCorrente(), idNumeratore});
			Numeratore num = (Numeratore)PersistentObject.elementWithKey(Numeratore.class, keyNumeratore, PersistentObject.NO_LOCK);
			if(num != null)
			{
				List serie = null;
				serie = num.getSerie();
				if(num.getTipoGestSerieSottoserie() == Numeratore.SOLO_SERIE || num.getTipoGestSerieSottoserie() == Numeratore.SERIE_SOTTOSERIE)
				{
					if(serie.size() <= 0)
					{
						return null;
					}
					idSerie = ((Serie)serie.get(0)).getIdSerie();
				}
			}else {
				return null;
			}
		}
		docVen.setIdAzienda(Azienda.getAziendaCorrente());
		docVen.getNumeratoreHandler().setDataDocumento(data);
		docVen.getNumeratoreHandler().setAnno(annoFiscale);
		docVen.getNumeratoreHandler().setIdSerie(idSerie);
		/*RecuperaCausaleDefaultDocOrd recuperaCausaleServlet = (RecuperaCausaleDefaultDocOrd) Factory.createObject(RecuperaCausaleDefaultDocOrd.class);
		String keyCliente = Azienda.getAziendaCorrente() + KeyHelper.KEY_SEPARATOR + ordVenRig.getIdCliente();
		String[] datiCausale = recuperaCausaleServlet.recuperaCausaleDefaultDocumento("VEN",keyCliente,keyCliente,"Cliente","DocumentoVendita");
		String idCausale = null;
		if(datiCausale != null) { //Fix 31884
			idCausale = datiCausale[0];
		}*/
		docVen.setIdCliente(ordVenRig.getIdCliente());
		//docVen.setIdCau(idCausale);
		CausaleDocumentoVendita cau = getCausaleDocVen((OrdineVendita) ordVenRig.getTestata(),((OrdineVendita)ordVenRig.getTestata()).getCliente(), null);
		docVen.setCausale(cau);
		docVen.completaBO();
		//docVen.setStatoAvanzamento(StatoAvanzamento.PROVVISORIO); 72080
		docVen.setDataConsegnaConfermata(ordVenRig.getDataConsegnaConfermata()); //71994
		aggiornaAttributiDaOrdine(docVen, (OrdineVendita) ordVenRig.getTestata());
		docVen.setDataConsegnaRichiesta(((OrdineVendita) ordVenRig.getTestata()).getDataConsegnaRichiesta());
		//72210 <
		Entity entity = null;
		try {
			entity = Entity.elementWithKey("DocVendita",Entity.NO_LOCK);
		} catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}
		UtilGener.copiaCommenti(ordVenRig.getTestata().getCommentHandler(), docVen.getCommentHandler(), entity);
		//72210 >
		return docVen;
	}

	protected static void aggiornaAttributiDaOrdine(DocumentoVendita docVen ,OrdineVendita ordine) {
		// fix 4445 16/11/05
		docVen.setNota(ordine.getNota());
		// Fine fix 4445
		// Fix 1932
		docVen.setRAnnoOrd(ordine.getAnnoDocumento());
		docVen.setRNumeroOrd(ordine.getNumeroDocumento());
		//}//Fix 16038
		// Fine fix 1932
		docVen.setNumeroOrdineIntestatario(ordine.getNumeroOrdineIntestatario());
		docVen.setDataOrdineIntestatario(ordine.getDataOrdineIntestatario());
		// Fix 2481
		docVen.setFornitoreKey(ordine.getFornitoreKey());
		docVen.setPriorita(ordine.getPriorita());

		// Fix 3016
		docVen.setRAnnoCantiere(ordine.getRAnnoCantiere());
		docVen.setRNumeroCantiere(ordine.getRNumeroCantiere());
		// fine fix 3016
		char rep = docVen.getReperimentoCondEvasione();
		if (rep == TipoReperimento.DA_ORDINE) {
			caricaDatiClienteDaOrdine(docVen,ordine);
			if (docVen.getAssoggettamentoIVA() == null) {
				PersDatiVen pdv = PersDatiVen.getCurrentPersDatiVen();
				if (pdv == null) {
					Trace.println("Impossibile creare il documento di vendita perchè PersDatiVen non è valorizzato");
					Trace.println(
							"Eccezione nel recupero di PersDatiVen in completaBO() di " +
									docVen.getClass().getName());
					return;
				}
				docVen.setAssoggettamentoIVA(pdv.getAssIVADocOrd());
			}
		}
		else if (rep == TipoReperimento.CLIENTE_FATTURAZIONE) {
			ClienteVendita cv = docVen.getClienteFatturazione();
			if (cv != null) {
				String keyCliDiv =
						KeyHelper.buildObjectKey(
								new String[] {
										Azienda.getAziendaCorrente(), cv.getIdCliente(), null
								}
								);

				docVen.caricaDatiDaCliente(cv, keyCliDiv);
				if (docVen.getAssoggettamentoIVA() == null) {
					PersDatiVen pdv = PersDatiVen.getCurrentPersDatiVen();
					if (pdv == null) {
						Trace.println("Impossibile creare il documento di vendita perchè PersDatiVen non è valorizzato");
						Trace.println(
								"Eccezione nel recupero di PersDatiVen in completaBO() di " +
										docVen.getClass().getName());
						return;
					}
					docVen.setAssoggettamentoIVA(pdv.getAssIVADocOrd());
				}
			}
		}
		docVen.setMagazzinoKey(ordine.getMagazzinoKey());
		docVen.setMagazzinoTrasferimentoKey(ordine.getMagazzinoTrasferimentoKey());
		docVen.setCommessaKey(ordine.getCommessaKey());
		docVen.setCentroCostoKey(ordine.getCentroCostoKey());
		docVen.setCambio(ordine.getCambio());
		docVen.setDataInizioTrasporto(ordine.getDataInizioTrasporto());
		docVen.setOraInizioTrasporto(ordine.getOraInizioTrasporto());
		docVen.setNumeroColli(ordine.getNumeroColli());
		docVen.setFlagRiservatoUtente1(ordine.getFlagRiservatoUtente1());
		docVen.setFlagRiservatoUtente2(ordine.getFlagRiservatoUtente2());
		docVen.setFlagRiservatoUtente3(ordine.getFlagRiservatoUtente3());
		docVen.setFlagRiservatoUtente4(ordine.getFlagRiservatoUtente4());
		docVen.setFlagRiservatoUtente5(ordine.getFlagRiservatoUtente5());
		docVen.setAlfanumRiservatoUtente1(ordine.getAlfanumRiservatoUtente1());
		docVen.setAlfanumRiservatoUtente2(ordine.getAlfanumRiservatoUtente2());
		docVen.setNumeroRiservatoUtente1(ordine.getNumeroRiservatoUtente1());
		docVen.setNumeroRiservatoUtente2(ordine.getNumeroRiservatoUtente2());
		docVen.setSpesePercPresenti(ordine.isSpesePercPresenti());
		docVen.setIdLineaCredito(ordine.getIdLineaCredito());//Fix 19385
		docVen.completaDatiDestinatarioFatturaPA();//Fix 21534
		docVen.setRaggruppamentoBolleFat(ordine.getRaggruppamentoBolleFat());//Fix 26361
		docVen.setCliRapprFiscali(ordine.getCliRapprFiscali());//Fix 36915
	}

	public static void caricaDatiClienteDaOrdine(DocumentoVendita docVen ,OrdineVendita ordine) {
		// fix 2525
		// Se il cliente intestario è diverso dal cliente da ordine i dati
		// di destinazione vengono presi dal cliente intestatario. E quindi non
		// vengono ripresi dall'ordine.
		boolean datiDestDaOrdine = true;
		if (datiDestDaOrdine) {
			// fine fix 2525
			docVen.setClienteDestinatario(ordine.getClienteDestinatario());
			docVen.setRagioneSocaleDest(ordine.getRagioneSocaleDest());
			docVen.setIndirizzoDestinatario(ordine.getIndirizzoDestinatario());
			docVen.setLocalitaDestinatario(ordine.getLocalitaDestinatario());
			docVen.setCAPDestinatario(ordine.getCAPDestinatario());
			// fix 2402
			docVen.setIdSequenzaInd(ordine.getIdSequenzaInd());
			// fine fix 2402

			// Fix 1461
			if (ordine.getClienteDestinatario() != null ||
					ordine.getRagioneSocaleDest() != null ||
					ordine.getIndirizzoDestinatario() != null ||
					ordine.getLocalitaDestinatario() != null || ordine.getCAPDestinatario() != null) {
				docVen.setIdSequenzaInd(null);
			}

			// Fine fix 1461
			docVen.setNazioneKey(ordine.getNazioneKey());
			docVen.setProvinciaKey(ordine.getProvinciaKey());
			// fix 2525
		}
		// fine fix 2525
		//Fix 27557 inizio
		/*docVen.getIdentificativoBanca().setCodificaSIA(ordine.getIdentificativoBanca().
	                                                 isCodificaSIA());
	    docVen.getIdentificativoBanca().setBancaABICABKey(ordine.
	        getIdentificativoBanca().getBancaABICABKey());
	    docVen.getIdentificativoBanca().setBancaNonABICABKey(ordine.
	        getIdentificativoBanca().getBancaNonABICABKey());
	    docVen.getIdentificativoBanca().setBancaNonABICAB(ordine.
	        getIdentificativoBanca().getBancaNonABICAB());*/
		try {
			docVen.getIdentificativoBanca().setEqual(getIdentificativoBancaDaUsare(ordine.getIdCliente(),ordine.getIdentificativoBanca(), docVen.getIdentificativoBanca(),docVen.getIdCliente()));
		} catch (CopyException e) {
			e.printStackTrace();
		}
		//Fix 27557 fine
		docVen.setIdCatVenCli(ordine.getIdCatVenCli());
		//docVen.setContoCorrente(ordine.getContoCorrente());//Fix 27557
		docVen.setContoCorrente(getValoreContoCorrenteDaUsare(ordine.getIdCliente(),ordine.getContoCorrente(),docVen.getContoCorrente(),docVen.getIdCliente()));//Fix 27557
		docVen.setCodiceIBAN(getValoreCodiceIBANDaUsare(ordine.getIdCliente(),ordine.getCodiceIBAN(),docVen.getCodiceIBAN(),docVen.getIdCliente()));//Fix 28455
		docVen.setSpesa1Key(ordine.getSpesa1Key());
		docVen.setSpesa2Key(ordine.getSpesa2Key());
		docVen.setPrcScontoIntestatario(ordine.getPrcScontoIntestatario());
		docVen.setPrcScontoModalita(ordine.getPrcScontoModalita());
		docVen.setScontoTabellareKey(ordine.getScontoTabellareKey());
		docVen.setLinguaKey(ordine.getLinguaKey());
		docVen.setZonaKey(ordine.getZonaKey());
		docVen.setMicroAreaClientiKey(ordine.getMicroAreaClientiKey());
		docVen.setListinoPrezziKey(ordine.getListinoPrezziKey());
		docVen.setValuta(ordine.getValuta());
		docVen.setModalitaPagamentoKey(ordine.getModalitaPagamentoKey());
		docVen.setDataInizioPagamento(ordine.getDataInizioPagamento());
		docVen.setAgenteKey(ordine.getAgenteKey());
		docVen.setProvvigioneAgente(ordine.getProvvigioneAgente());
		docVen.setSubagenteKey(ordine.getSubagenteKey());
		docVen.setProvvigioneSubagente(ordine.getProvvigioneSubagente());
		// 11053 LC >
		docVen.setDifferenzaPrezzoAgente(ordine.hasDifferenzaPrezzoAgente());
		docVen.setDifferenzaPrezzoSubagente(ordine.hasDifferenzaPrezzoSubagente());
		// 11053 LC <
		docVen.setTipoLiquidazionePrv(ordine.getTipoLiquidazionePrv());
		docVen.setClienteFatturazioneKey(ordine.getClienteFatturazioneKey());
		docVen.setModalitaSpedizioneKey(ordine.getModalitaSpedizioneKey());
		docVen.setDescrModalitaSpedizione(ordine.getDescrModalitaSpedizione());
		docVen.setModalitaConsegnaKey(ordine.getModalitaConsegnaKey());
		docVen.setDescrModalitaConsegna(ordine.getDescrModalitaConsegna());
		docVen.setAspettoEsterioreKey(ordine.getAspettoEsterioreKey());
		docVen.setDescrAspettoEsteriore(ordine.getDescrAspettoEsteriore());
		docVen.setCausaleTrasportoKey(ordine.getCausaleTrasportoKey());
		docVen.setDescrCausaleTrasporto(ordine.getDescrCausaleTrasporto());
		docVen.setVettore1Key(ordine.getVettore1Key());
		docVen.setDescrVettore1(ordine.getDescrVettore1());
		docVen.setVettore2Key(ordine.getVettore2Key());
		docVen.setDescrVettore2(ordine.getDescrVettore2());
		docVen.setVettore3Key(ordine.getVettore3Key());
		docVen.setDescrVettore3(ordine.getDescrVettore3());
		docVen.setCadenzaConsegnaKey(ordine.getCadenzaConsegnaKey());
		docVen.setGiroConsegneKey(ordine.getGiroConsegneKey());
		docVen.setRifDataPerPrezzoSconti(ordine.getRifDataPerPrezzoSconti());
		//MG FIX 8344
		//	    docVen.setRicalcoloPrzScontiFattura(ordine.getRicalcoloPrzScontiFattura());
		docVen.setTpRicPrzScnFat(ordine.getTpRicPrzScnFat());
		docVen.setCondizioneFatturazione(ordine.getCondizioneFatturazione());
		docVen.setPrcScontoFineFattura(ordine.getPrcScontoFineFattura());
		docVen.setPercScontoFinaleFat(ordine.getPercScontoFinaleFat()); //33971
		docVen.setScontoFinaleFat(ordine.getScontoFinaleFat()); //33971    
		//docVen.setTotScontoFinaleFat(ordine.getTotScontoFinaleFat()); //33971   ????
		docVen.setFatturazioneSospesa(ordine.getFatturazioneSospesa());
		docVen.setFatturazionePeriodica(ordine.getFatturazionePeriodica());
		docVen.setResponsabileVenditeKey(ordine.getResponsabileVenditeKey());
		docVen.setCategoriaContabileKey(ordine.getCategoriaContabileKey());
		docVen.setGruppoContiAnaliticaKey(ordine.getGruppoContiAnaliticaKey());
		docVen.setAdbSpeseIncasso(ordine.getAdbSpeseIncasso());
		docVen.setAdbSpeseBolloFatSenzaIVA(ordine.getAdbSpeseBolloFatSenzaIVA());
		docVen.setAssoggettamentoIVA(ordine.getAssoggettamentoIVA());
		docVen.setInternalTipoDestinatario(); //Fix 21534
		docVen.completaDatiDestinatarioFatturaPA();//Fix 21534
	}

	public static IdentificativoBanca getIdentificativoBancaDaUsare(String idClienteOrdine,IdentificativoBanca identBancaDaOrdine, IdentificativoBanca identBancaAttuale,
			String idCliente) {
		if(!Utils.areEqual(idClienteOrdine, idCliente))
			return identBancaAttuale;
		return identBancaDaOrdine;
	}

	public static String getValoreContoCorrenteDaUsare(String idClienteOrdine, String contoCorrenteOrdine, String contoCorrenteAttuale,
			String idCliente) {
		if(!Utils.areEqual(idClienteOrdine, idCliente))
			return contoCorrenteAttuale;
		return contoCorrenteOrdine;

	}

	public static String getValoreCodiceIBANDaUsare(String idClienteOrdine, String codiceIBANOrdine, String codiceIBANAttuale,
			String idCliente) {
		if(!Utils.areEqual(idClienteOrdine, idCliente))
			return codiceIBANAttuale;
		return codiceIBANOrdine;

	}

	public static CausaleDocumentoVendita getCausaleDocVen(OrdineVendita ordine, ClienteVendita cliente, String idDivisione) {
		if(ordine != null && ordine.getCausale().getCausaleDocVen() != null)
			return ordine.getCausale().getCausaleDocVen();

		if(idDivisione != null) {
			String[] chiaveCliente = KeyHelper.unpackObjectKey(cliente.getKey());
			String[] chiaveClienteDiv = new String[chiaveCliente.length + 1]; 
			System.arraycopy(chiaveCliente, 0, chiaveClienteDiv, 0, chiaveCliente.length);
			chiaveClienteDiv[chiaveClienteDiv.length - 1] = idDivisione;
			return cliente.getCausaleDocumento(KeyHelper.buildObjectKey(chiaveClienteDiv));
		}
		else if(cliente != null && cliente.getCausaleDocumentoVendita() != null)
			return cliente.getCausaleDocumentoVendita();

		PersDatiVen pdv = PersDatiVen.getCurrentPersDatiVen();
		if (pdv == null) {
			Trace.println("Impossibile recuperare la causale di default perchè PersDatiVen non è valorizzato");
			return null;
		}
		else
			return pdv.getCausaleDocumentoVendita();
	}

	/**
	 * In funzione della riga ordine di vendita passata cerco se esiste un documento di vendita
	 * che soddisfa le condizioni di accorpabilità:
	 * Azienda corrisponde
	 * Cliente corrisponde
	 * Stato avanzamento = PROVVISORIO
	 * @param ordVenRig
	 * @return documento di vendita su cui accorpare la riga, se trovato
	 */
	protected DocumentoVendita cercaDocumentoVenditaAccorpabile(OrdineVenditaRiga ordVenRig) {
		DocumentoVendita docVen = null;
		OrdineVendita ordVen =  (OrdineVendita) ordVenRig.getTestata();
		CondizioniCompatibilitaEvasione condComp = GestoreEvasioneVendita.get().inizializzaCondizioniCompatibilita(ordVenRig.getIdAzienda());

		String trovaDocVenAccodabile =
				" SELECT " + DocumentoVenditaTM.ID_AZIENDA + ", " + DocumentoVenditaTM.ID_ANNO_DOC + ", " + DocumentoVenditaTM.ID_NUMERO_DOC
				+ " FROM " + DocumentoVenditaTM.TABLE_NAME +
				" WHERE " + DocumentoVenditaTM.ID_AZIENDA + " = '"+ordVenRig.getIdAzienda()+"' " +
				" AND " + DocumentoVenditaTM.R_CLIENTE + " = '"+ordVen.getIdCliente()+"' " + 
				" AND " + DocumentoVenditaTM.STATO_AVANZAMENTO + " = '"+StatoAvanzamento.PROVVISORIO+"' " ;

		//71994
		if(ordVenRig.getDataConsegnaConfermata() != null)
			trovaDocVenAccodabile += " AND "+DocumentoVenditaTM.DATA_CONSEG_CFM+" = "+ConnectionManager.getCurrentDatabase().getLiteral(ordVenRig.getDataConsegnaConfermata())+" "; //Fix 72020 rimuovere singoli apici

		if(condComp.isGesDestinatario()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_DEN_ABT;
			if(ordVen.getIdDenAbt() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdDenAbt()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesAssIVA()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_ASSOG_IVA;
			if(ordVen.getIdAssogIva() != null)
				trovaDocVenAccodabile +=  " = '"+ordVen.getIdAssogIva()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesModalitaSpedizione()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_MOD_SPEDIZIONE;
			if(ordVen.getIdModSpedizione() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdModSpedizione()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesAgenteSubagente()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_AGENTE_SUB;
			if(ordVen.getIdAgente() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdAgente()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesBanca()) {
			if(ordVen.getIdentificativoBanca() != null && ordVen.getIdentificativoBanca().getIdABI() != null) {
				trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_ABI + " = '"+ordVen.getIdentificativoBanca().getIdABI()+"' " + 
						" AND " + DocumentoVenditaTM.R_CAB + " = '"+ordVen.getIdentificativoBanca().getIdCAB()+"' ";
			}else {
				trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_ABI + " IS NULL " + 
						" AND " + DocumentoVenditaTM.R_CAB + " IS NULL ";
			}
		}

		if(condComp.isGesLineaCredito()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.ID_LINEA_CREDITO;
			if(ordVen.getIdLineaCredito() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdLineaCredito()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesClienteFatturazione()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_CLIENTE_FAT;
			if(ordVen.getIdClienteFat() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdClienteFat()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesRappresentanteFiscale()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_RAPPRES_FISC;
			if(ordVen.getIdRappresentanteFisc() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdRappresentanteFisc()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesModalitaConsegna()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_MOD_CONSEGNA;
			if(ordVen.getIdModConsegna() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdModConsegna()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesCommessa()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_COMMESSA;
			if(ordVen.getIdCommessa() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdCommessa()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesValuta()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_VALUTA;
			if(ordVen.getIdValuta() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdValuta()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesInizioPagamento()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.DATA_INIZIO_PAG;
			if(ordVen.getDataInizioPagamento() != null)
				trovaDocVenAccodabile += " = '"+ ConnectionManager.getCurrentDatabase().getLiteral(ordVen.getDataInizioPagamento()) +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesModalitaPagamento()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.R_MOD_PAGAMENTO;
			if(ordVen.getIdModPagamento() != null)
				trovaDocVenAccodabile += " = '"+ ordVen.getIdModPagamento() +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesScontoFineFattura()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.SCONTO_FINE_FATT;
			if(ordVen.getPrcScontoFineFattura() != null)
				trovaDocVenAccodabile += " = '"+ ordVen.getPrcScontoFineFattura() +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesVettori()) {
			trovaDocVenAccodabile += " AND " + DocumentoVenditaTM.SCONTO_FINE_FATT;
			if(ordVen.getPrcScontoFineFattura() != null)
				trovaDocVenAccodabile += " = '"+ ordVen.getPrcScontoFineFattura() +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}		


		try {
			CachedStatement cTrovaDocVenAccodabile = new CachedStatement(trovaDocVenAccodabile);	
			ResultSet rs = cTrovaDocVenAccodabile.executeQuery();
			String key = null;
			if(rs.next()) {
				String[] c = new String[] {
						rs.getString(DocumentoVenditaTM.ID_AZIENDA),
						rs.getString(DocumentoVenditaTM.ID_ANNO_DOC),
						rs.getString(DocumentoVenditaTM.ID_NUMERO_DOC)
				};
				key = KeyHelper.buildObjectKey(c);
			}
			rs.close();
			if(key != null) {
				docVen = DocumentoVendita.elementWithKey(key, DocumentoVendita.NO_LOCK);
			}
		}catch(SQLException e) {
			e.printStackTrace(Trace.excStream);
		}
		return docVen;
	}

	@Override
	protected void impostaPianoPrelieviRigaDatiPers(PianoPrelieviRiga pianoPrlRig, PianoPrelieviTestata pianoPrlTes,
			WrapperPianoPrlDati wrPianoPrlDati) {
		super.impostaPianoPrelieviRigaDatiPers(pianoPrlRig, pianoPrlTes, wrPianoPrlDati);
		BigDecimal qtaGiaEvasa = BigDecimal.ZERO;

		PreparedStatement ps = null;
		ResultSet resultSet = null;
		try {
			ps = cSelezionaQtaEvasaMissioniRigaMateriale.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, wrPianoPrlDati.getIdAzienda());
			db.setString(ps, 2, wrPianoPrlDati.getIdAnnoOrd());
			db.setString(ps, 3, wrPianoPrlDati.getIdNumeroOrd());
			db.setString(ps, 4, wrPianoPrlDati.getIdRigaAttivita().toString());
			db.setString(ps, 5, wrPianoPrlDati.getIdRigaMateriale().toString());
			resultSet =  ps.executeQuery();
			if(resultSet.next()){
				qtaGiaEvasa = resultSet.getBigDecimal(1) != null ? resultSet.getBigDecimal(1) : BigDecimal.ZERO;
			}
		}catch (SQLException ex) {
			ex.printStackTrace(Trace.excStream);
		}

		if(qtaGiaEvasa != null) {
			pianoPrlRig.setQtaDaPrlUmPrm(pianoPrlRig.getQtaDaPrlUmPrm().subtract(qtaGiaEvasa));
		}
	}

}