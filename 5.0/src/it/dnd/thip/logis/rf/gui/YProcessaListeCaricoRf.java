package it.dnd.thip.logis.rf.gui;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import com.thera.thermfw.base.ResourceLoader;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.common.ErrorMessage;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.ErrorCodes;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.rf.driver.Driver;
import com.thera.thermfw.rf.driver.FormCreator;
import com.thera.thermfw.rf.tc.TField;
import com.thera.thermfw.rf.tc.TForm;
import com.thera.thermfw.rf.tc.TList;

import it.dnd.thip.logis.bas.StampaEtichettaPianoCarico;
import it.dnd.thip.logis.bas.YCostantiDnd;
import it.dnd.thip.logis.fis.TipoGestioneUbicazione;
import it.dnd.thip.logis.fis.YEsecuzionePianiCarico;
import it.dnd.thip.logis.lgb.StatoPrelievoRigaToyota;
import it.dnd.thip.logis.lgb.StatoPrelievoUdcToyota;
import it.dnd.thip.logis.lgb.StatoRigaToyota;
import it.dnd.thip.logis.lgb.TipoMissionePianoCaricoRigaToyota;
import it.dnd.thip.logis.lgb.YPianoCaricoToyota;
import it.dnd.thip.logis.lgb.YPianoCaricoToyotaRiga;
import it.dnd.thip.logis.lgb.YPianoCaricoToyotaTM;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.azienda.Reparto;
import it.thera.thip.logis.bas.Attributo;
import it.thera.thip.logis.bas.EventoApplicativo;
import it.thera.thip.logis.bas.Numeratore;
import it.thera.thip.logis.bas.NumeratoreMaxException;
import it.thera.thip.logis.bas.NumeratoreMaxProgrException;
import it.thera.thip.logis.bas.NumeratoreNotFoundException;
import it.thera.thip.logis.bas.NumeratoreNotValidException;
import it.thera.thip.logis.bas.ParametriLogis;
import it.thera.thip.logis.fis.AbilPostazioneTipoLista;
import it.thera.thip.logis.fis.EsecuzioneMissioni;
import it.thera.thip.logis.fis.MappaUdc;
import it.thera.thip.logis.fis.Missione;
import it.thera.thip.logis.fis.PianificazioneLista;
import it.thera.thip.logis.lgb.TestataLista;
import it.thera.thip.logis.prd.CampoTrascodificaGruppo;
import it.thera.thip.logis.prd.ConfigurazioneArticolo;
import it.thera.thip.logis.rf.gui.LogisRF;

/**
 *
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 10/04/2025
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 71923    10/04/2025  DSSOF3   Prima stesura
 * 71978	28/05/2025	DSSOF3	 Implementare la gestione del nuovo campo TipoMissione
 * 72036	14/07/2025	DSSOF3   Sistemazione pagina stampa etichetta
 * 72046	14/07/2025	DSSOF3	 Introduzione stampa etichette
 * 72452	20/04/2026	AGSOF3	 Rimosso controllo riga prelevabile 
 * 72459	27/04/2026	AGSOF3	 Faccio cancellazione missioni quando il PC viene messo in pronta per il reintegro
 */

public class YProcessaListeCaricoRf extends LogisRF {

	public YProcessaListeCaricoRf(Driver drv, String xml) {
		super(drv, xml);
		try {
			FormCreator.print(YCostantiDnd.searchXml(), this);
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
	}

	protected final static int MENU = 0;
	protected final static int PRESENTAZIONE = 1;
	protected final static int SELEZIONE = 2;
	protected final static int UDC_NR_RITORNO = 3;
	protected final static int RIPOSIZ_UDC_MAGAZZINO = 4;
	protected final static int ESECUZIONE_CARICO = 5;

	protected final static int OK = 0;
	protected final static int CONFERMA = 1;
	protected final static int PROX_MISS = 2;
	protected final static int PREC_MISS = 3;
	protected final static int NEXT_N_MISS = 4;
	protected final static int PREC_N_MISS = 5;
	protected final static int NUOVA_UDS = 6;
	protected final static int CLR = 7;
	protected final static int RIPETI = 8;
	protected final static int SALTA_MISS = 9;
	protected final static int ERR_GRAVE = 99;
	protected final static int ERRORE = 100;
	protected final static int SALTO_MISSIONI = 10;
	protected final static int MISSIONE = 7;
	protected final static int CANCELLA = 8;
	protected final static int ESITO = 11;

	protected int pagina = SELEZIONE;

	public static final String RES_FILE = "it.dnd.thip.logis.rf.resources.DndLogisRf";

	protected String formPaginaSelezioneReparto = "paginaPresentazioneReparti";

	public YEsecuzionePianiCarico esecuzionePianiCarico = null;
	public EsecuzioneMissioni esecuzioneMissioni = null;

	protected boolean flagAvanti = true;
	protected boolean flagIndietro = false;

	protected boolean naviga = true;

	protected boolean gestioneUdsInCorso = true;

	protected final static char PRELIEVO_SU_RIFERIMENTO = '0';
	protected final static char PRELIEVO_SU_UDC_REPARTO = '1';

	protected char tipoPrelievo = PRELIEVO_SU_RIFERIMENTO;

	public ErrorMessage esegui() {
		ErrorMessage err = null;
		boolean interrompi = false;
		pagina = SELEZIONE;
		while (!interrompi) {
			switch (pagina) {
			case MENU:
				interrompi = true;
				break;
			case SELEZIONE:
				esecuzionePianiCarico = (YEsecuzionePianiCarico) Factory.createObject(YEsecuzionePianiCarico.class);
				paginaSelezioneReparto();
				break;
			case UDC_NR_RITORNO:
				esecuzioneMissioni = (EsecuzioneMissioni) Factory.createObject(EsecuzioneMissioni.class);
				paginaUdcNumeroRitorno();
				break;
			case RIPOSIZ_UDC_MAGAZZINO:
				paginaRiposizionamentoUdcMagazzino();
				break;
			case ESECUZIONE_CARICO: 
				paginaEsecuzioneCarico();
				break;
			case CANCELLA:
				paginaCancella();
				break;
			default:
				try {
					messaggio(true, ResourceLoader.getString(RESOURCES,"msg0001"));
					interrompi = true;
				} catch (Exception e) {
					e.printStackTrace(Trace.excStream);
					interrompi = true;
				}
				break;
			}
		}
		return err;
	}

	public String formPaginaEsecuzioneCarico = "visualizzaMissioneUds";

	@SuppressWarnings({ })
	protected void paginaEsecuzioneCarico() {
		if(esecuzioneMissioni.getElencoMissioni().size() == 0) {
			try {
				messaggio(true, ResourceLoader.getString(RES_FILE, "noPrelieviEsecuzioneCarico"));
			} catch (Exception ex) {
				ex.printStackTrace(Trace.excStream);
			}
			pagina = UDC_NR_RITORNO;
			return;
		}
		int dim = esecuzioneMissioni.getElencoMissioni().size();
		int res = 0;                            // Esito esecuzione.
		boolean stop = false;
		boolean out = false;
		flagAvanti = naviga &&
				(esecuzioneMissioni.getElencoMissioni().size() > 1 ||
						esecuzioneMissioni.getElencoBidoneMissioni().size() > 1);
		flagIndietro = false;
		while (!stop &&
				esecuzioneMissioni.getNumMissConfermate() < dim &&
				esecuzioneMissioni.getPosMiss() >= 0 &&
				esecuzioneMissioni.getPosMiss() < dim) {
			esecuzionePianiCarico.setRigaPianoCaricoInConferma((YPianoCaricoToyotaRiga) esecuzionePianiCarico.getElencoRighe().elementAt(esecuzioneMissioni.getPosMiss()));
			esecuzioneMissioni.setMissInConferma((Missione) esecuzioneMissioni.getElencoMissioni().elementAt(esecuzioneMissioni.getPosMiss()));
			res = visualizzaMissione();   // Esegui missione.
			switch (res) {
			case SALTA_MISS:
				if (esecuzioneMissioni.getElencoBidoneMissioniSaltate().isEmpty())
					break;
				Missione mSaltata = (Missione) esecuzioneMissioni.getElencoBidoneMissioniSaltate().lastElement();
				if (cancellaSaltata(mSaltata)) {
					int num = esecuzioneMissioni.getNumMissConfermate();
					esecuzioneMissioni.setNumMissConfermate(++num);
				}
				break;
			case CONFERMA:  // Missione confermata.
				int num = esecuzioneMissioni.getNumMissConfermate();
				esecuzioneMissioni.setNumMissConfermate(++num);
				if (!cercaMissioneAvanti())       // Cerca tra le missioni successive.
					stop =  true;
				if (stop) {                       // Non trovata quindi ...
					if (cercaMissioneIndietro())    // ... cerca tra le missioni precedenti.
						stop = false;
				}
				break;
			case PROX_MISS: // Missione saltata.
				flagIndietro = naviga && true;
				if (!cercaMissioneAvanti())       // Cerca tra le missioni successive.
					stop = true;
				break;
			case PREC_MISS: // Missione saltata.
				flagAvanti = naviga && true;
				if (!cercaMissioneIndietro())     // Cerca tra le missioni precedenti.
					stop = true;
				break;
			case NEXT_N_MISS:     // Salta avanti di N missione.
				flagIndietro = naviga;
				if (!saltaAvantiMissioni())       // Cerca tra le missioni successive.
					stop = true;
				break;
			case PREC_N_MISS:     // Salta indietro di N missione.
				flagAvanti = naviga ;
				if (!saltaIndietroMissioni())     // Cerca tra le missioni precedenti.
					stop = true;
				break;
			case CLR:       // Esci.
				stop = true;
				pagina = CANCELLA;
				break;
			case ERR_GRAVE: // Torna al menù principale.
				out = true;
				stop = true;
				break;
			case RIPETI:    // Ripropone la missione.
			case ERRORE:
			default:        // Errore.
				break;                            // Ripropone la stessa missione.
			}                 // Rileggo per caricare anche le eventuali missioni aggiunte.
			dim = esecuzioneMissioni.getElencoMissioni().size();
		}
		if (out)
			pagina = MENU;
		else {
			if(pagina != CANCELLA) {
				if(dim == esecuzioneMissioni.getNumMissConfermate() && tipoPrelievo == PRELIEVO_SU_RIFERIMENTO) {
					try {
						String msg = "Sono terminati i prelievi relativi all'ordine "+esecuzionePianiCarico.getRigaPianoCaricoInConferma().getNumeroRiferimento();
						if(esecuzionePianiCarico.getRigaPianoCaricoInConferma().getCliente() != null) {
							msg += " del cliente "+esecuzionePianiCarico.getRigaPianoCaricoInConferma().getCliente().getRagioneSociale();
						}
						messaggio(false, msg);
					} catch (Exception e) {
						e.printStackTrace(Trace.excStream);
					}
					pagina = UDC_NR_RITORNO;
				}else if(dim == esecuzioneMissioni.getNumMissConfermate() && tipoPrelievo == PRELIEVO_SU_UDC_REPARTO) {
					esecuzionePianiCarico.setPianoInRiposizionamento((YPianoCaricoToyota) esecuzionePianiCarico.getElencoRighe().get(0).getTestata()); //.Me lo porto nella pagina successiva
					pagina = RIPOSIZ_UDC_MAGAZZINO;
				}
			}
		}

	}

	protected String formPaginaCancella = "ConfermaCancellazione";

	protected void paginaCancella() {
		int dim = esecuzioneMissioni.getElencoMissioni().size();
		if (esecuzioneMissioni.getNumMissConfermate() < dim) {
			if (conferma(getTForm(formPaginaCancella))) {    // Chiedi conferma.
				if (cancellazione()) {                              // Cancella ...
					try {
						ConnectionManager.rollback();
						messaggio(true, ResourceLoader.getString(RESOURCES,"operazioneKo") + " " + ResourceLoader.getString(RESOURCES, "vediPC"));
					} catch (Exception ex) {
						ex.printStackTrace(Trace.excStream);
					}
					pagina = MENU;
				} else {
					try {
						ConnectionManager.commit();
					} catch (SQLException ex) {
						ex.printStackTrace(Trace.excStream);
						pagina = CANCELLA;
						return;
					}
					pagina = UDC_NR_RITORNO;         // ... ed esce.
				}
			} else {                    // Non cancella e torna alle missioni.
				flagIndietro = false;
				esecuzioneMissioni.setPosMiss(-1);
				cercaMissioneAvanti();
				pagina = ESECUZIONE_CARICO;
			}
		} else {
			//pagina = UDC_NR_RITORNO;                   // Passerebbe direttamente alla pagina conclusiva ma
			//			ErrorMessage err = trattaLista(); //  riprova a fare il prelievo rilanciando il processo.
			//			dim = 0;
			//			if (err == null)
			//dim = esecuzioneMissioni.getElencoMissioni().size();
			//if (dim > 0)
			pagina = ESECUZIONE_CARICO;
		}
	}

	protected boolean cancellazione() {
		return esecuzioneMissioni.annullaMissioniInEsec(false);
	}

	/**
	 * Metodo chiamato dalla classi eredi.
	 */
	protected boolean cancellaSaltata(Missione m) {
		return false;
	}

	public boolean chkTastoFunc(TForm risposta){
		boolean chkTastoFunc = false;
		String bc = risposta.getTField("BarcodeTxt").getValue();
		if (bc != null &&
				!bc.equals(""))
			chkTastoFunc = true;
		if (!chkTastoFunc) {
			String qc = risposta.getTField("QtaConfermataTxt").getValue();
			if (qc != null &&
					!qc.equals("") &&
					!qc.equals("0"))
				chkTastoFunc = true;
		}
		return chkTastoFunc;

	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected int visualizzaMissione() {
		ErrorMessage errore = null;
		YPianoCaricoToyotaRiga riga = esecuzionePianiCarico.getRigaPianoCaricoInConferma();
		Missione m = esecuzioneMissioni.getMissInConferma();
		try {
			if (m.isOnDB() && riga.isOnDB()) {   // Se era su DB è possibile che ci siano stati interventi dall'esterno.
				m.retrieve(PersistentObject.NO_LOCK);
				riga.retrieve(PersistentObject.NO_LOCK);
				if (riga.getStatoRiga() != StatoRigaToyota.APERTA)
					return CONFERMA;
			}
			TestataLista tl = (TestataLista) TestataLista.elementWithKey(TestataLista.class, KeyHelper.buildObjectKey(new String[] {
					riga.getMissione().getCodiceSocieta(),riga.getMissione().getTestataRigaLista()
			}), PersistentObject.NO_LOCK);
			Vector liste = new Vector();
			liste.add(tl);
			caricaListeInEsecuzione(liste);

			//.Se e' il primo prelievo della riga, quindi stato prelievo = '0'
			//.devo mostrare una pagina dove e' possibile lanciare la stampa dell'etichetta per quella riga prelievo
			//.se l'utente conferma allora stampo, altrimenti no, torno comunque qui
			//if(riga.getStatoPrelievo() == StatoPrelievoRigaToyota.NON_PRELEVATA) {
			if(!esisteRiferimentoNonPrelevato(riga)) {
				//.quindi nel metodo che visualizza la form basta che ci sia un return in ogni caso
				stampaEtichetteRiga(riga);
			}

			TForm form = getTForm(formPaginaEsecuzioneCarico);
			settaDatiForm(form, riga);
			sendForm(form);
			TForm risposta = readInput();
			boolean chkTastoFunc = this.chkTastoFunc(risposta);
			//26.11.14 - fine
			if (risposta.getKeyPressed().equals(TForm.KEY_F2) ||
					(risposta.getKeyPressed().equals(TForm.KEY_F8) &&
							!gestioneUdsInCorso)) {
				if (chkTastoFunc)
					//Fix 15944 messaggio(false, "NON hai confermato la missione: lascia il materiale al posto oppure spostalo con la funzione dedicata..");
					messaggio(false, ResourceLoader.getString(RESOURCES, "msg0025"));
				return RIPETI;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_ESC) ||
					risposta.getKeyPressed().equals(TForm.KEY_CTL_X)) {  // Esci
				if (chkTastoFunc)
					//Fix 15944 messaggio(false, "NON hai confermato la missione: lascia il materiale al posto oppure spostalo con la funzione dedicata..");
					messaggio(false, ResourceLoader.getString(RESOURCES, "msg0025"));
				// elencoMatricole.clear();//11.06.19
				return CLR;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F7) &&
					flagIndietro) {                                      // Missione precedente.
				if (chkTastoFunc)
					//Fix 15944 messaggio(false, "NON hai confermato la missione: lascia il materiale al posto oppure spostalo con la funzione dedicata..");
					messaggio(false, ResourceLoader.getString(RESOURCES, "msg0025"));
				//elencoMatricole.clear();//11.06.19
				return PREC_MISS;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F9) &&
					flagAvanti) {                                        // Missione successiva
				if (chkTastoFunc)
					//Fix 15944 messaggio(false, "NON hai confermato la missione: lascia il materiale al posto oppure spostalo con la funzione dedicata.");
					messaggio(false, ResourceLoader.getString(RESOURCES, "msg0025"));
				esecuzioneMissioni.getElencoBidoneMissioniSaltate().removeElement(m);
				esecuzioneMissioni.getElencoBidoneMissioniSaltate().addElement(m);
				// elencoMatricole.clear();//11.06.19
				return PROX_MISS;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F4) &&
					flagIndietro) {                                      // Missione precedente.
				if (chkTastoFunc)
					//Fix 15944 messaggio(false, "NON hai confermato la missione: lascia il materiale al posto oppure spostalo con la funzione dedicata..");
					messaggio(false, ResourceLoader.getString(RESOURCES, "msg0025"));
				// elencoMatricole.clear();//11.06.19
				return PREC_N_MISS;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F6) &&
					flagAvanti) {                                         // Missione successiva
				if (chkTastoFunc)
					//Fix 15944 messaggio(false, "NON hai confermato la missione: lascia il materiale al posto oppure spostalo con la funzione dedicata..");
					messaggio(false, ResourceLoader.getString(RESOURCES, "msg0025"));
				esecuzioneMissioni.getElencoBidoneMissioniSaltate().removeElement(m);
				esecuzioneMissioni.getElencoBidoneMissioniSaltate().addElement(m);
				//elencoMatricole.clear();//11.06.19
				return NEXT_N_MISS;
			}
			//		      if (gestioneNote &&
			//		          risposta.getKeyPressed().equals(TForm.KEY_F5)) {
			//		        gestioneInfoMissione(m);                              // Gestione informazioni ulteriori.
			//		        return RIPETI;
			//		      }
			if (risposta.getKeyPressed().equals(TForm.KEY_F1) ||    // Conferma missione.
					//(risposta.getKeyPressed().equals(TForm.KEY_F8) && gestioneUdsInCorso) ||  // Spezza UdS.
					(risposta.getKeyPressed().equals(TForm.KEY_F3))) {
				String barcode = risposta.getTField("BarcodeTxt").getValue();
				String qtaConfermata = risposta.getTField("QtaConfermataTxt").getValue();
				errore = testBarcode(barcode);
				//18.03.14 - inizio
				// se non è stata inserita la qtaConfermata eventualmente la prendo dall'etichetta barcode
				if (qtaConfermata.equals("") || qtaConfermata.equals("0")){
					if (esecuzioneMissioni.getQtaConfermata() != null &&
							esecuzioneMissioni.getQtaConfermata().compareTo(new BigDecimal(0)) > 0)
						qtaConfermata = esecuzioneMissioni.getQtaConfermata().toString();
				}
				//18.03.14 - fine
				if (errore != null) {
					messaggio(true, errore.getLongText());
					return ERRORE;
				}
				m = (Missione) esecuzioneMissioni.getMissInConferma();  
				Missione mi = null;                                     //  siccome potrebbe essere cambiata
				if (m.isOnDB()) {
					String keyMiss = m.getKey();
					try {
						mi = Missione.elementWithKey(keyMiss, PersistentObject.OPTIMISTIC_LOCK);
					} catch (SQLException ex) {
						ex.printStackTrace(Trace.excStream);
						//Fix 15944 messaggio(true, "Missione persa !");
						messaggio(true, ResourceLoader.getString(RESOURCES, "msg0026"));
						return ERRORE;
					}
				} else
					mi = m;
				if (mi == null) {
					messaggio(true, ResourceLoader.getString(RESOURCES,"errChiuMiss") + " "  + ResourceLoader.getString(RESOURCES, "vediPC"));
					return ERR_GRAVE;
				}
				BigDecimal qta = new BigDecimal(0);
				/*if (esecuzioneMissioni.getflagGestCatasta() &&
						esecuzioneMissioni.getElencoSaldiCom().size() > 0) {
					boolean ripeti = false;
					ripeti = gestioneCatasta(mi,qtaConfermata);//07.07.04
					if (ripeti)
						return RIPETI;
					qta = mi.getQta1Evasa();
				} else {*/
				try {
					qta = new BigDecimal(qtaConfermata);
				} catch (Exception ex) {
					ex.printStackTrace(Trace.excStream);
					messaggio(true, ResourceLoader.getString(RESOURCES,"qtaNumero") + " " + ResourceLoader.getString(RESOURCES,"qtaOk"));
					return ERRORE;
				}
				qta = qta.multiply(esecuzioneMissioni.getCoefMovim());//29.11.04
				//}
				if (qta == null ||
						qta.compareTo(new BigDecimal(0)) < 0) {  // Il valore non può essere < 0.
					messaggio(true, ResourceLoader.getString(RESOURCES,"qtaNoNegativa") + " " + ResourceLoader.getString(RESOURCES,"qtaOk"));
					return ERRORE;
				}
				BigDecimal qtaPrm = new BigDecimal(0);
				BigDecimal qtaSec = new BigDecimal(0);

				if (qta.compareTo(new BigDecimal(0)) == 0)
					qtaPrm = new BigDecimal(0);
				else
					//05.02.08 - fine
					qtaPrm = mi.getArticolo().gestioneQta2(qta);//10.09.07
				/*while (qtaPrm == null) {    // Richiesta della qta in UM primaria
					String res = paginaQtaUM(mi,qta);//20.08.07
					//03.08.07 - fine
					if (res.equals(ESCI + ""))
						return CLR;
					if (res.equals(INDIETRO + ""))
						return RIPETI;
					try {
						qtaPrm = new BigDecimal(res);//03.08.07
					} catch (Exception ex) {
						ex.printStackTrace(Trace.excStream);
						messaggio(true, ResourceLoader.getString(RESOURCES,"qtaNumero") + " " + ResourceLoader.getString(RESOURCES,"qtaOk"));
						qtaPrm = null;//03.08.07
					}
				}*/
				//20.08.07 - inizio
				//05.02.08 - inizio
				if (qta.compareTo(new BigDecimal(0)) == 0)
					qtaSec = new BigDecimal(0);
				else
					//05.02.08 - fine
					qtaSec = mi.getArticolo().gestioneQta3(qta);//10.09.07
				/*while (qtaSec == null) { // Richiesta della qta in UM secondaria
					String res = paginaQtaUM(mi,qta);
					if (res.equals(ESCI + ""))
						return CLR;
					if (res.equals(INDIETRO + ""))
						return RIPETI;
					try {
						qtaSec = new BigDecimal(res);
					}
					catch (Exception ex) {
						ex.printStackTrace(Trace.excStream);
						messaggio(true, ResourceLoader.getString(RESOURCES, "qtaNumero") + " " + ResourceLoader.getString(RESOURCES, "qtaOk"));
						qtaSec = null;
					}
				}*/
				//20.08.07 - fine
				if (mi.getSaldo() == null) {
					//Fix 15944 messaggio(true, "Il saldo e' stato cancellato. Missione non confermabile.");
					messaggio(true, ResourceLoader.getString(RESOURCES, "msg0043"));
					return ERRORE;
				}
				if (mi.getTipoMissione() == Missione.SPOSTAMENTO &&
						mi.getCodiceMappaUdcInv() != null &&     // In caso di spostamento, se la UdC destin.
						!mi.getCodiceMappaUdcInv().equals("")) { //  non c'è più, trasferisco la sorgente.
					if (mi.getMappaUdcInv() != null) {
						try {
							if (!mi.getMappaUdcInv().retrieve(PersistentObject.NO_LOCK))
								mi.setMappaUdcInv(mi.getMappaUdc());
						} catch (SQLException ex) {
							ex.printStackTrace(Trace.excStream);
						}
					} else
						mi.setMappaUdcInv(mi.getMappaUdc());
				}
				//05.05.18 - inizio
				//in caso di prelievo parziale ricreo subito la missione per la qtà residua
				//Vector err = trattaMissione(mi, qta, qtaPrm, qtaSec);
				Vector err = new Vector();
				boolean prelievoParziale = (qta.compareTo(mi.getQta1Richiesta()) < 0);
				if (prelievoParziale){
					//09.05.19 - inizio
					//err = spezzaMissione(mi, qta, qtaPrm, qtaSec);
					//avviso che la qtà è diversa da quella richiesta, chiedo conferma per proseguire 
					//(è lo stesso controllo presente in trattaMissione(), ho dovuto anticiparlo prima della creazione della missione figlia)
					//if (qta.compareTo(mi.getQta1Richiesta().min(mi.getSaldo().getQta1())) != 0) {//14.03.23 nel caso catasta la condizione poteva essere falsa e quindi non faceva niente, negli altri casi era superfluo perché comunque siamo nel caso prelievo parziale
					boolean conf = false;
					try {
						conf = conferma(false, ResourceLoader.getString(RESOURCES, "cnf0005"));
					}
					catch (Exception ex) {
						ex.printStackTrace(Trace.excStream);
					}
					if (!conf)
						err.add(new ErrorMessage("LOGIST0039"));
					else {
						//11.06.19 - inizio
						/*if (gestioneMatricola){
							try {
								elencoMatricole = paginaMatricola(mi.getArticolo(), 
										mi.getLotto1(),
										mi.getLotto2(),
										qta,
										elencoMatricole,
										mi.getCodiceMagLogico(),//15.07.19
										esecuzioneMissioni);//09.09.20
							} catch (Exception e) {
								e.printStackTrace();
								err.add(new ErrorMessage("LOGIS01054",e.getMessage()));
								return ERRORE;
							}
							if (elencoMatricole.size() < qta.intValue()){//uscito con CTLX, le matricole acquisite vanno conservate
								return RIPETI;
							}
						}*/
						//11.06.19 - fine
						err = spezzaMissione(mi, qta, qtaPrm, qtaSec);
						riga.setMissione(mi); //.Setto sulla riga la nuova missione creata per il residuo
					}
					//}//14.03.23
				}
				//09.05.19 - fine
				else
					err = trattaMissione(mi, qta, qtaPrm, qtaSec);
				//05.05.18 - fine
				if (err.size() == 0 ||
						(err.size() == 1 && err.elementAt(0) instanceof String)) {
					//elencoMatricole.clear();//11.06.19
					if (!prelievoParziale){
						esecuzioneMissioni.setTlAbbassamento(null);  // Annullo la testata di abbassamento che avevo creato
						m.setStatoMissione(Missione.TERMINATA);
					}
					if (err.size() == 1){
						messaggio(false, new ErrorMessage((String)err.elementAt(0)).getLongText());
					}
					form.getTField("UDSTxt").setValue("");

					riga.setQuantitaPrelevataUmPrm(riga.getQuantitaPrelevataUmPrm().add(qtaPrm));
					if(!prelievoParziale)
						riga.setStatoPrelievo(StatoPrelievoRigaToyota.PRELEVATA);
					else
						riga.setStatoPrelievo(StatoPrelievoRigaToyota.PRELEVATA_PARZIALMENTE);

					int rc = riga.save();
					if(rc > 0) {
						ConnectionManager.commit();
					}else {
						ConnectionManager.rollback();
					}

					//05.05.18 - inizio
					/*if (risposta.getKeyPressed().equals(TForm.KEY_F3)){
						paginaChiudiTipoUds();
						if (esecuzioneMissioni.getTipoUds() != null)
							esecuzioneMissioni.getTestataUds().setTipoUds(esecuzioneMissioni.getTipoUds());
						paginaDimensioniUds();
					}*/
					if (prelievoParziale)
						return RIPETI;
					else
						return CONFERMA;
					//05.05.18 - fine
				} else {
					messaggio(true, ((ErrorMessage) err.elementAt(0)).getLongText());
					form.getTField("UDSTxt").setValue("");
					ConnectionManager.rollback();//05.11.21
					return ERRORE;
				}
			}
		} catch (Exception e) {
			e.printStackTrace(Trace.excStream);
			return ERR_GRAVE;
		}
		return ERRORE;
	}

	protected boolean esisteRiferimentoNonPrelevato(YPianoCaricoToyotaRiga riga) {
		String stmt = "SELECT * FROM THIPPERS.YPIANO_CARICO_TOYOTA_RIG R "
				+ "INNER JOIN THIPPERS.YPIANO_CARICO_TOYOTA_TES T "
				+ "ON R.ID_AZIENDA = T.ID_AZIENDA "
				+ "AND R.ID_ANNO_DOC = T.ID_ANNO_DOC  "
				+ "AND R.ID_NUMERO_DOC = T.ID_NUMERO_DOC "
				+ "WHERE R.ID_AZIENDA = '"+riga.getIdAzienda()+"' "
				+ "AND R.NUM_RIFERIMENTO = '"+riga.getNumeroRiferimento()+"' "
				+ "AND R.STATO_PRL_RIGA > '"+StatoPrelievoRigaToyota.NON_PRELEVATA+"' "
				+ "AND T.STATO_UDC >= '"+StatoPrelievoUdcToyota.RICEVUTA+"' AND R.TIPO_MISSIONE = '"+TipoMissionePianoCaricoRigaToyota.PRELIEVO+"' ";
		ResultSet rs = null;
		CachedStatement cs = null;
		try {
			cs = new CachedStatement(stmt);
			rs = cs.executeQuery();
			if(rs.next()) {
				return true;
			}
		}catch (Exception e) {
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
		return false;
	}

	public String formStampaEtichette = "paginaStampaEtichetta";

	protected void stampaEtichetteRiga(YPianoCaricoToyotaRiga riga) {
		TForm form = getTForm(formStampaEtichette);
		try {
			form.getTField("Label2").setValue((riga.getNumeroRiferimento() != null ? riga.getNumeroRiferimento() : ""));
			if(riga.getCliente() != null) {
				form.getTField("Label4").setVisible(false);
				form.getTField("Label4").setValue(riga.getCliente().getRagioneSociale());
			}else {
				form.getTField("Label3").setValue("");
				form.getTField("Label4").setValue("");
			}
			sendForm(form);
			TForm risposta = readInput();
			if (risposta.getKeyPressed().equals(TForm.KEY_ESC) ||
					risposta.getKeyPressed().equals(TForm.KEY_CTL_X)) {
				return;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F1)) {
				EventoApplicativo.creaEvento(riga, StampaEtichettaPianoCarico.STAMPA_ETIC_COL); //Fix 72046
			}
		} catch(Exception e) {
			e.printStackTrace(Trace.excStream);
			pagina = MENU;
		}
		return;
	}

	protected ErrorMessage testBarcode(String barcode) {
		if (barcode.trim().equals(""))
			return new ErrorMessage("LOGIS01054", ResourceLoader.getString(RESOURCES,"mancaBarcode"));
		esecuzioneMissioni.getMissInConferma().setChiaveOperazioneMovimento(KeyHelper.buildObjectKey(new String[] {esecuzioneMissioni.getMissInConferma().getCodiceMagFisico(), esecuzioneMissioni.getMissInConferma().getCodiceOperazioneMovimento()}));
		esecuzioneMissioni.getMissInConferma().setChiaveUbicazione(KeyHelper.buildObjectKey(new String[] {esecuzioneMissioni.getMissInConferma().getCodiceMagFisico(), esecuzioneMissioni.getMissInConferma().getCodiceUbicazione()}));
		esecuzioneMissioni.setCampoVariabile(barcode.trim());
		return esecuzioneMissioni.checkCampoVariabile();
	}

	@SuppressWarnings("rawtypes")
	protected Vector spezzaMissione(Missione m, BigDecimal qtaConfermata) throws Exception {
		return spezzaMissione(m, qtaConfermata, new BigDecimal(0), new BigDecimal(0));
	}

	@SuppressWarnings("rawtypes")
	protected Vector trattaMissione(Missione mi, BigDecimal qta) {
		return trattaMissione(mi, qta, new BigDecimal(0), new BigDecimal(0));
	}

	/**
	 * Esegue delle operazioni sulla missione. Nello standard la conferma.
	 * Alimenta anche l'UdS corrente, se abilitato.
	 * Ridefinire se necessario.
	 *
	 * @param mi missione in conferma.
	 * @param qta quantità effettivamente confermata.
	 *
	 * @return Vector elenco degli errori. Vuoto se tutto è OK.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Vector trattaMissione(Missione mi,
			BigDecimal qta,
			BigDecimal qtaPrm,
			BigDecimal qtaSec) {
		// Non è necessario controllare le altre qta siccome:
		//  - se sono non gestite o costanti, ovvio
		//  - se sono variabili, la richiesta calcolata teoricamente difficilmente sarà corretta con la realtà
		if (qta.compareTo(mi.getQta1Richiesta().min(mi.getSaldo().getQta1())) != 0) {
			boolean conf = false;
			if (qta.compareTo(mi.getSaldo().getQta1()) <= 0){//18.07.16 - qta diversa dalla richiesta ma non maggiore del saldo 
				try {
					//Fix 15944 conf = conferma(false, "Quantita confermata diversa da quella richiesta. Sei sicuro?" );
					conf = conferma(false, ResourceLoader.getString(RESOURCES, "cnf0005") );
				} catch (Exception ex) {
					ex.printStackTrace(Trace.excStream);
				}
			}
			//18.07.16 - inizio
			// chiedo conferma nel caso in cui la qta è maggiore del saldo
			else {
				try {
					//conf = conferma(false, "Quantita maggiore di quella del saldo. Sei sicuro?");
					conf = conferma(false, ResourceLoader.getString(RESOURCES, "msg0067") );//27.10.17
				} catch (Exception ex) {
					ex.printStackTrace(Trace.excStream);
				}
				if (conf)
					mi.setControlloGiacenza(false);
			}
			//18.07.16 - fine	
			if (!conf){
				Vector v = new Vector();
				//Fix 15944 v.addElement(new ErrorMessage("LOGIS01054", "Missione non confermata"));
				v.addElement(new ErrorMessage("LOGIST0039"));
				return v;
			}
		}
		if (gestioneUdsInCorso &&
				qta.compareTo(new BigDecimal(0)) > 0 &&
				esecuzioneMissioni.getTestataUds() != null) {
			mi.setTestataUds(esecuzioneMissioni.getTestataUds());   // Scarica su questa UdS
			//udsGenerate = true;                                     //  il materiale prelevato.
		}
		return esecuzioneMissioni.confermaMissione(mi, qta, qtaPrm, qtaSec);
	}

	/**
	 * Spezza in due la missione: la figlia viene confermata.
	 *
	 * @param m Missione madre
	 * @param qtaConfermata Q.tà in conferma sulla prima parte.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected Vector spezzaMissione(Missione m,
			BigDecimal qtaConfermata,
			BigDecimal qta2Confermata,
			BigDecimal qta3Confermata) throws Exception {
		Vector errori = new Vector();
		Missione missFiglia = (Missione) Factory.createObject(Missione.class);    // Missione figlia
		missFiglia.setCodiceMagFisico(m.getCodiceMagFisico());
		missFiglia.setCodiceOperazioneMovimento(m.getCodiceOperazioneMovimento());
		missFiglia.setTipoMissione(m.getTipoMissione());
		missFiglia.setChiaveRigaLista(m.getChiaveRigaLista());
		missFiglia.setArticolo(m.getArticolo());
		missFiglia.setSaldo(m.getSaldo());
		missFiglia.setChiavePostazione(m.getChiavePostazione());
		missFiglia.setChiaveOperatore(m.getChiaveOperatore());
		for (int i = 0; i < m.getLotto().length; i++)
			missFiglia.setLotto(m.getLotto(i),i);
		missFiglia.setDataScadenza(m.getDataScadenza());
		missFiglia.setUmBase(m.getUmBase());
		missFiglia.setUmBase1(m.getUmBase1());//10.03.11
		missFiglia.setUmBase2(m.getUmBase2());
		missFiglia.setSegno(m.getSegno());
		missFiglia.setCodiceStatistico(m.getCodiceStatistico());
		missFiglia.setConfezione(m.getConfezione());
		missFiglia.setMagLogico(m.getMagLogico());
		missFiglia.setSocieta(m.getSocieta());
		missFiglia.setPianificazioneLista(m.getPianificazioneLista());
		missFiglia.setAllestimentoDoc(m.getAllestimentoDoc());
		missFiglia.setDestinatario(m.getDestinatario());
		missFiglia.setMagLogicoInv(m.getMagLogicoInv());
		missFiglia.setCodiceUdm(m.getCodiceUdm());
		missFiglia.setCodiceUdmInverso(m.getCodiceUdmInverso());
		missFiglia.setScomparto(m.getScomparto());
		missFiglia.setSottoUbicazione(m.getSottoUbicazione());
		missFiglia.setMappaUdc(m.getMappaUdc());
		missFiglia.setChiaveUbicazione(m.getChiaveUbicazione());
		missFiglia.setChiaveUbicazioneInv(m.getChiaveUbicazioneInv());//04.03.11
		missFiglia.setQta1Richiesta(qtaConfermata);
		missFiglia.setQta2Richiesta(qta2Confermata);
		missFiglia.setQta3Richiesta(qta3Confermata);
		missFiglia.setParzializzazione(m.getParzializzazione());
		missFiglia.setNumUdm(m.getNumUdm());
		missFiglia.setSvuotamentoUdc(m.getSvuotamentoUdc());
		missFiglia.setNumEsecuzione(m.getNumEsecuzione());
		missFiglia.setSequenza(m.getSequenza());
		missFiglia.setChiaveAreaLavoro(m.getChiaveAreaLavoro());
		missFiglia.setTipoMacchina(m.getTipoMacchina());
		missFiglia.setTsEsecuzione(m.getTsEsecuzione());
		missFiglia.setStatoMissione(m.getStatoMissione());
		//Fix 15944 missFiglia.setNote("Missione frazionata");
		missFiglia.setNote(ResourceLoader.getString(RESOURCES, "txt0004")); //Fix 15944
		try {
			missFiglia.setCodice(Numeratore.getProgr(m.getMagFisico().getTipoNumeratoreMissione().getCodice()));
		} catch (SQLException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		} catch (NumeratoreMaxProgrException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		} catch (NumeratoreNotFoundException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		} catch (InstantiationException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		} catch (ClassNotFoundException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		} catch (IllegalAccessException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		} catch (NumeratoreNotValidException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		} catch (NumeratoreMaxException ex) {
			ex.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nella creazione della missione spezzata. " + ex.getMessage()));
			errori.addElement(new ErrorMessage("LOGIST0117", ex.getMessage()));
			return errori;
		}
		m.setQta1Richiesta(m.getQta1Richiesta().subtract(qtaConfermata));
		m.setQta2Richiesta(m.getQta2Richiesta().subtract(qta2Confermata));
		m.setQta3Richiesta(m.getQta3Richiesta().subtract(qta3Confermata));
		//14.03.23 - inizio
		//		if (saldoOriginale != null) {
		//			m.setSaldo(saldoOriginale);
		//			m.setUbicazione(saldoOriginale.getUbicazione());
		//			m.setMappaUdc(saldoOriginale.getMappaUdc());
		//		}
		//14.03.23 - fine
		int r = ErrorCodes.NO_ROWS_UPDATED;
		try {
			r = m.save();   // Commit fatta dalla conferma missione figlia.
		} catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nel salvataggio della missione origine."));
			errori.addElement(new ErrorMessage("LOGIST0118"));
			return errori;
		}
		if (r < ErrorCodes.NO_ROWS_UPDATED) {   // Errore nella save della miss. madre.
			try {
				ConnectionManager.rollback();
			} catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
				//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nel salvataggio della missione origine."));
				errori.addElement(new ErrorMessage("LOGIST0118"));
				return errori;
			}
			//Fix 15944 errori.addElement(new ErrorMessage("LOGIS01054", "Errore nel salvataggio della missione origine."));
			errori.addElement(new ErrorMessage("LOGIST0118"));
			return errori;
		}
		errori = trattaMissione(missFiglia, qtaConfermata, qta2Confermata, qta3Confermata);
		if (errori.size() == 0) {    // Sostiuisco la missione originaria con quella aggiornata.
			esecuzioneMissioni.getElencoMissioni().removeElementAt(esecuzioneMissioni.getPosMiss());
			esecuzioneMissioni.getElencoMissioni().add(esecuzioneMissioni.getPosMiss(), m);
		}
		return errori;
	}

	protected void settaDatiForm(TForm form, YPianoCaricoToyotaRiga riga) {
		Missione m = riga.getMissione();
		if (m == null) {
			return;
		}

		form.getTField("ArticoloMatricolato").setVisible(false);
		form.getTField("ArticoloMatricolato").setValue("");

		form.getTField("TitoloLbl").setValue(formatoPosizione());
		form.getTField("UMLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.UM.label"));
		form.getTField("ArticoloLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.Art.label"));
		form.getTField("VersioneLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.Versione.label"));
		//form.getTField("Lotto1Lbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.Lottto1.label"));
		//form.getTField("Lotto2Lbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.Lottto2.label"));
		form.getTField("UbicSaldoLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.UbicSaldo.label"));
		form.getTField("UdcLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.Udc.label"));
		form.getTField("UbicDestLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.UbicDest.label"));
		form.getTField("QtaGiacienzaLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.QtaGia.label"));
		form.getTField("QtaDisponibileLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.QtaDisp.label"));
		form.getTField("BarcodeLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.Barcode.label"));
		form.getTField("QtaRichiestaLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.QtaRich.label"));
		form.getTField("QtaConfermataLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.QtaConf.label"));
		form.getTField("UDSLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.UDS.label"));
		form.getTField("PuntoCaricoLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.PuntoCarico.label"));

		if (true){
			form.getTField("UDSLbl").setValue("");
			form.getTField("UDSTxt").setValue("");
			form.getTField("UDSTxt").setDisplay(TField.DISPLAY_OUTPUT);
		}
		/*else {
			form.getTField("UDSTxt").setDisplay(TField.DISPLAY_INPUT);
		}*/

		if (m.getArticolo().getGruppo().getAbilVersione())
			form.getTField("VersioneVal").setValue(m.getArticolo().getVersione());
		else
			form.getTField("VersioneLbl").setValue("");

		//gestioneLotto(form, m);
		//gestioneConfigurazione(form, m);
		gestionePuntoCarico(form, m);

		form.getTField("UMVal").setValue(formato(m.getArticolo().getUmBase(), 2));
		form.getTField("CodiceArticoloVal").setValue(formato(m.getArticolo().getCodice(), 17));
		String descrizione = "";
		if(riga.getArticolo().getDescrizioneArticoloNLS().getDescrizioneEstesa() != null && 
				!riga.getArticolo().getDescrizioneArticoloNLS().getDescrizioneEstesa().isEmpty()) {
			descrizione = riga.getArticolo().getDescrizioneArticoloNLS().getDescrizioneEstesa();
		}else {
			descrizione = riga.getArticolo().getDescrizioneArticoloNLS().getDescrizione();
		}
		if(descrizione.length() > 21) {
			if(descrizione.length() > 84) {
				descrizione = descrizione.substring(0,84);
			}
			form.getTField("VersioneLbl").setVisible(true);
			/*String descEx = m.getArticolo().getDescrizione();
			String firstPart = descEx.substring(0,21);
			String lastPart = descEx.substring(21,descEx.length());
			form.getTField("DescrArticoloVal").setValue(firstPart);
			form.getTField("VersioneLbl").setValue(lastPart); //.Uso questo campo per mettere gli eventuali altri chars*/

			int maxCharPerField = 21;
			String[] campi = new String[4];

			for (int i = 0; i < 4; i++) {
				int start = i * maxCharPerField;
				int end = Math.min(start + maxCharPerField, descrizione.length());
				if (start < descrizione.length()) {
					campi[i] = descrizione.substring(start, end);
				} else {
					campi[i] = "";
				}
			}

			// Imposta i valori nei rispettivi campi del form
			if(campi[0] != null && campi[0].isEmpty()) {
				form.getTField("DescrArticoloVal").setVisible(false);
				form.getTField("DescrArticoloVal").setValue("");
			}else
				form.getTField("DescrArticoloVal").setValue(campi[0]);

			if(campi[1] != null && campi[1].isEmpty()) {
				form.getTField("VersioneLbl").setVisible(false);
				form.getTField("VersioneLbl").setValue("");
			}else
				form.getTField("VersioneLbl").setValue(campi[1]);

			if(campi[2] != null && campi[2].isEmpty()) {
				form.getTField("DescrArticoloExt1").setVisible(false);
				form.getTField("DescrArticoloExt1").setValue("");
			}else
				form.getTField("DescrArticoloExt1").setValue(campi[2]);

			if(campi[3] != null && campi[3].isEmpty()) {
				form.getTField("DescrArticoloExt2").setVisible(false);
				form.getTField("DescrArticoloExt2").setValue("");
			}else
				form.getTField("DescrArticoloExt2").setValue(campi[3]);
		}else {
			form.getTField("DescrArticoloVal").setValue(descrizione);
			form.getTField("VersioneLbl").setVisible(false);
			form.getTField("DescrArticoloExt1").setVisible(false);
			form.getTField("DescrArticoloExt2").setVisible(false);
		}

		form.getTField("NumeroRiferimentoLbl").setValue(ResourceLoader.getString(RES_FILE, "visualizzaMissioneUds.NumeroRiferimento.label"));

		if(riga.getNumeroRiferimento() != null) {
			form.getTField("NumeroRiferimento").setVisible(true);
			form.getTField("NumeroRiferimentoLbl").setVisible(true);
			form.getTField("NumeroRiferimento").setValue(riga.getNumeroRiferimento());
		}else {
			form.getTField("NumeroRiferimento").setVisible(false);
			form.getTField("NumeroRiferimentoLbl").setValue("");
			form.getTField("NumeroRiferimento").setValue("");
		}

		form.getTField("RagioneSocialeCli").setValue("");

		if(riga.getCliente() != null) {
			form.getTField("RagioneSocialeCli").setVisible(true);
			form.getTField("RagioneSocialeCli").setValue(formato(riga.getCliente().getRagioneSociale(), 21));
		}

		form.getTField("UbicSaldoVal").setValue(formato(m.getSaldo().getCodiceUbicazione(), 17));
		form.getTField("UbicDestVal").setValue(formato(m.getCodiceUbicazioneInv(), 17));
		gestioneUdc(form, m);

		form.getTField("QtaGiacienzaVal").setValue(formattaBigDec(m.getSaldo().getQta1()));
		BigDecimal qtaDisp = m.getSaldo().calcolaDisponibile().add(m.getQta1Richiesta().min(m.getSaldo().getQta1()));
		form.getTField("QtaDisponibileVal").setValue(formattaBigDec(qtaDisp));
		form.getTField("QtaRichiestaVal").setValue(formattaBigDec((riga.getResiduoDaPrelevare())));

		form.getTField("BarcodeTxt").setValue("");

		form.getTField("QtaConfermataTxt").setVisible(true);
		form.getTField("QtaConfermataLbl").setVisible(true);
		form.getTField("QtaConfermataTxt").setValue("0");
		form.getTField("QtaConfermataLbl").setValue(ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.QtaConf.label"));
		form.getTField("QtaConfermataTxt").setDisplay(TField.DISPLAY_INPUT);

		form.getTField("TastiFunzioneUltimaLbl").setValue(formatoTastiFunzioneBassi());
	}

	protected void gestionePuntoCarico(TForm form, Missione m) {
		String puntoCarico = formato(m.getRigaLista().getTestataLista().getPuntoCarico(), 17);
		boolean puntoCaricoVisible = (puntoCarico.length() > 0);
		form.getTField("PuntoCaricoVal").setValue(puntoCarico);
		form.getTField("PuntoCaricoLbl").setVisible(puntoCaricoVisible);
		form.getTField("PuntoCaricoVal").setVisible(puntoCaricoVisible);
		if (!puntoCaricoVisible) {
			form.getTField("PuntoCaricoLbl").setValue("");
		}
	}

	protected void gestioneUdc(TForm form, Missione m) {
		String mappaUdc = formato(m.getCodiceMappaUdc(), 17);
		boolean mappaUdcVisible = (mappaUdc.length() > 0);
		form.getTField("UdcVal").setValue(mappaUdc);
		form.getTField("UdcLbl").setVisible(mappaUdcVisible);
		form.getTField("UdcVal").setVisible(mappaUdcVisible);
		if (!mappaUdcVisible) {
			form.getTField("UdcLbl").setValue("");
		}
	}

	protected void gestioneLotto(TForm form, Missione m) {
		String lotto = null;
		if (m.getArticolo().getGruppo().getLotti().size() > 0) {
			CampoTrascodificaGruppo campo = (CampoTrascodificaGruppo) m.getArticolo().getGruppo().getLotti().get(0);
			if (campo.getAbilitazione() && (m.getLotto1() != null)) {
				lotto = m.getLotto1().trim();
			}
		}
		lotto = formato(lotto, 17);
		form.getTField("Lotto1Val").setValue(lotto);
		boolean lotto1Visible = lotto.length() > 0;
		form.getTField("Lotto1Lbl").setVisible(lotto1Visible);
		form.getTField("Lotto1Val").setVisible(lotto1Visible);
		if (!lotto1Visible) {
			form.getTField("Lotto1Lbl").setValue("");
		}
	}

	protected void gestioneConfigurazione(TForm form, Missione m) {
		String configurazione = null;
		if (m.getArticolo().getGruppo().getLotti().size() > 1) {
			CampoTrascodificaGruppo campo = (CampoTrascodificaGruppo) m.getArticolo().getGruppo().getLotti().get(1);
			if (campo.getAbilitazione() && (m.getLotto2() != null)) {
				configurazione = m.getLotto2().trim();
				if (ParametriLogis.INSTALL_PANTHERA) {
					String idEst = ConfigurazioneArticolo.getIdEsternoConfigurazione(m.getCodiceSocieta(), m.getLotto2());
					configurazione = idEst.trim();
				}
			}
		}
		configurazione = formato(configurazione, 17);
		form.getTField("Lotto2Val").setValue(configurazione);
		boolean lotto2Visible = configurazione.length() > 0;
		form.getTField("Lotto2Lbl").setVisible(lotto2Visible);
		form.getTField("Lotto2Val").setVisible(lotto2Visible);
		if (!lotto2Visible) {
			form.getTField("Lotto2Lbl").setValue("");
		}
	}

	protected String formato(String value, int length) {
		if (value == null) {
			return "";
		}

		if (value.length() > length) {
			return value.substring(0, length);
		}

		return value;
	}

	protected String formatoPosizione() {
		int numMiss = esecuzioneMissioni.getPosMiss() + 1;
		String res = "";
		res = "> " + ResourceLoader.getString(RESOURCES, "visualizzaMissioneUds.Missione.label") + " ";
		res += numMiss + "/" + esecuzioneMissioni.getElencoMissioni().size();
		if (res.length() > 16) {
			res = res.substring(0, 16);
		}
		return res;
	}

	protected String formatoTastiFunzioneBassi() {
		String tasti = "";
		if (flagIndietro){
			tasti = "<F7";
		}

		tasti += Attributo.formatta("", 2);
		if (flagAvanti){
			if(tasti.length() > 0){
				tasti += Attributo.formatta("", 2);	
			}
			tasti += "F9>";
		}

		while (tasti.length() < 14) {
			tasti += " ";
		}

		return tasti;
	}

	/**
	 * Cerca una missione in esecuzione tra quelle precedenti a quella corrente.
	 */
	protected boolean cercaMissioneIndietro() {
		boolean trovato = false;
		// Cerca la prossima missione in esecuzione da confermare ...
		for (int i = esecuzioneMissioni.getPosMiss() - 1; i >= 0; i--) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				esecuzioneMissioni.setPosMiss(i);
				trovato = true;
				break;
			}
		}
		flagIndietro = false;
		if (!trovato)         // ... missione non trovato => stop.
			return false;
		// ... trovata. Ne cerca altre per il 'flagIndietro'.
		for (int i = esecuzioneMissioni.getPosMiss() - 1; i >= 0 && naviga; i--) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				flagIndietro = naviga && true;
				break;
			}
		}
		return true;
	}

	/**
	 * Cerca una missione in esecuzione tra quelle successive a quella corrente.
	 */
	protected boolean cercaMissioneAvanti() {
		boolean trovato = false;
		int dim = esecuzioneMissioni.getElencoMissioni().size();
		// Cerca la prossima missione in esecuzione da confermare ...
		for (int i = esecuzioneMissioni.getPosMiss() + 1; i < dim; i++) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				esecuzioneMissioni.setPosMiss(i);
				trovato = true;
				break;
			}
		}
		flagAvanti = false;
		if (!trovato)
			return false;     // ... nessuna missione in esecuzione trovata => stop.
		// ... trovata. Cerca eventuali altre missioni in esec. per il 'flagAvanti'.
		for (int i = esecuzioneMissioni.getPosMiss() + 1; i < dim && naviga; i++) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				flagAvanti = naviga && true;
				break;
			}
		}
		return true;
	}

	/**
	 * Cerca una missione in esecuzione tra quelle precedenti a quella corrente
	 * provando ad andare indietro di N missioni.
	 */
	protected boolean saltaIndietroMissioni() {
		boolean trovato = false;
		int saltate = 0;
		int lastExec = esecuzioneMissioni.getPosMiss() - 1;
		for (int i = esecuzioneMissioni.getPosMiss() - 1; i >= 0 && !trovato; i--) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				saltate++;
				lastExec = i;
				if (saltate == SALTO_MISSIONI ||
						i == 0) {
					esecuzioneMissioni.setPosMiss(i);
					trovato = true;
					break;
				}
			}
		}
		if (!trovato &&
				lastExec < esecuzioneMissioni.getPosMiss() - 1) {
			esecuzioneMissioni.setPosMiss(lastExec);
			trovato = true;
		}
		flagIndietro = false;
		if (!trovato) {         // ... missione non trovato => stop.
			return false;
		}
		// ... trovata. Ne cerca altre per il 'flagIndietro'.
		for (int i = esecuzioneMissioni.getPosMiss() - 1; i >= 0 && naviga; i--) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				flagIndietro = naviga && true;
				break;
			}
		}
		return true;
	}

	/**
	 * Cerca una missione in esecuzione tra quelle successive a quella corrente, saltando di N.
	 */
	protected boolean saltaAvantiMissioni() {
		boolean trovato = false;
		int dim = esecuzioneMissioni.getElencoMissioni().size();
		int saltate = 0;
		int lastExec = 0;
		for (int i = esecuzioneMissioni.getPosMiss() + 1; i < dim; i++) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				saltate++;
				lastExec = i;
				if (saltate == SALTO_MISSIONI ||
						i == (dim - 1)) {
					esecuzioneMissioni.setPosMiss(i);
					trovato = true;
					break;
				}
			}
		}
		if (!trovato && lastExec > 0) {
			esecuzioneMissioni.setPosMiss(lastExec);
			trovato = true;
		}
		flagAvanti = false;
		if (!trovato)
			return false;     // ... nessuna missione in esecuzione trovata => stop.
		// ... trovata. Cerca eventuali altre missioni in esec. per il 'flagAvanti'.
		for (int i = esecuzioneMissioni.getPosMiss() + 1; i < dim && naviga; i++) {
			Missione m = (Missione) esecuzioneMissioni.getElencoMissioni().elementAt(i);
			if (m.getStatoMissione() == Missione.ESECUZIONE) {
				flagAvanti = naviga && true;
				break;
			}
		}
		return true;
	}

	public String formPaginaRiposizionamentoUdcMagazzino = "paginaRiposizUdcMagazzino";

	@SuppressWarnings("rawtypes")
	protected void paginaRiposizionamentoUdcMagazzino() {
		TForm form = getTForm(formPaginaRiposizionamentoUdcMagazzino);
		try {
			form.getTField("Label3").setValue(esecuzionePianiCarico.getCodiceMappaUdc());
			sendForm(form);
			TForm risposta = readInput();
			if (risposta.getKeyPressed().equals(TForm.KEY_ESC) ||
					risposta.getKeyPressed().equals(TForm.KEY_CTL_X)) {
				pagina = SELEZIONE;
				esecuzionePianiCarico.setReparto(null); //.Se torna indietro svuoto il reparto scelto
				return;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F1)) {
				YPianoCaricoToyota pianoInRiposizionamento = esecuzionePianiCarico.getPianoInRiposizionamento();
				pianoInRiposizionamento.setStatoUdc(StatoPrelievoUdcToyota.PRONTA_PER_REINTEGRO);

				if(pianoInRiposizionamento.getStatoGestione() == TipoGestioneUbicazione.OPERATORE)
					pianoInRiposizionamento.setStatoUdc(StatoPrelievoUdcToyota.RIPOSIZIONATA);

				Iterator iterRighe = pianoInRiposizionamento.getRighe().iterator();
				while(iterRighe.hasNext()) {
					YPianoCaricoToyotaRiga riga = (YPianoCaricoToyotaRiga) iterRighe.next();
					riga.setStatoRiga(StatoRigaToyota.CHIUSA);
				}
				cancellazione();//72459 AGSOF3 quando mettiamo il piano in pronta per il reintegro pulisco le missioni
				int rc = pianoInRiposizionamento.save();
				if(rc > 0) {
					ConnectionManager.commit();
					esecuzionePianiCarico.setPianoInRiposizionamento(null); //.Svuoto il piano
					esecuzionePianiCarico.setMappaUdc(null); //.Svuoto la mappa sparata
					pagina = UDC_NR_RITORNO;
				}else {
					ConnectionManager.rollback();
					messaggio(true, "Impossibile salvare il piano di carico, riprovare");
					pagina = RIPOSIZ_UDC_MAGAZZINO;
				}
			}
		} catch(Exception e) {
			e.printStackTrace(Trace.excStream);
			pagina = MENU;
		}
		return;
	}

	public String formPaginaUdcNumeroRitorno = "paginaUdcNumeroRitorno";

	protected void paginaUdcNumeroRitorno() {
		TForm form = getTForm(formPaginaUdcNumeroRitorno);
		try {
			esecuzionePianiCarico.getElencoRighe().clear();
			form.getTField("FieldUdc").setValue("");
			form.getTField("FieldNumeroRitorno").setValue("");
			sendForm(form);
			TForm risposta = readInput();
			if (risposta.getKeyPressed().equals(TForm.KEY_ESC) ||
					risposta.getKeyPressed().equals(TForm.KEY_CTL_X)) {
				pagina = SELEZIONE;
				esecuzionePianiCarico.setPianoInRiposizionamento(null); //.Svuoto il piano
				esecuzionePianiCarico.setMappaUdc(null); //.Svuoto la mappa sparata
				return;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F1)) {
				ErrorMessage errore = testPaginaUdcNumeroRitorno(form.getTField("FieldUdc").getValue(), form.getTField("FieldNumeroRitorno").getValue());
				if(errore != null) {
					messaggio(true, errore.getLongText());
				}
			}
		} catch(Exception e) {
			e.printStackTrace(Trace.excStream);
			pagina = MENU;
		}
		return;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected ErrorMessage testPaginaUdcNumeroRitorno(String udc, String numeroRitorno) throws Exception {
		if(udc.isEmpty() && numeroRitorno.isEmpty()) {
			return new ErrorMessage("YLOGIS0003");
		}
		if(udc != null && !udc.isEmpty()) {
			MappaUdc mappa = MappaUdc.elementWithKey(udc, PersistentObject.NO_LOCK);
			if(mappa == null) {
				return new ErrorMessage("YLOGIS0001");
			}else {
				esecuzionePianiCarico.setMappaUdc(mappa); //.Porto sulla classe la mappa udc
			}
		}
		if(numeroRitorno != null && !numeroRitorno.isEmpty()) {
			/*String where = " "+AttivitaEsecutivaTM.ID_AZIENDA+" = '"+Azienda.getAziendaCorrente()+"' AND "+AttivitaEsecutivaTM.NUM_RITORNO+" = '"+numeroRitorno+"' ";
			try {
				Vector atvs = AttivitaEsecutiva.retrieveList(AttivitaEsecutiva.class, where, "", false);
				if(atvs.size() == 0) {
					return new ErrorMessage("YLOGIS0002");
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException e) {
				e.printStackTrace(Trace.excStream);
			}
			 */
			boolean esistonoRighe = esistonoRigheAperteNumeroRiferimento(numeroRitorno);
			if(!esistonoRighe) {
				return new ErrorMessage("YLOGIS0002");
			}
		}

		//.Verfico se esiste un piano di carico aperto con reparto/udc scelti
		if(udc != null && !udc.isEmpty()) {
			String where = " "+YPianoCaricoToyotaTM.ID_AZIENDA+" = '"+Azienda.getAziendaCorrente()+"' "
					+ "AND "+YPianoCaricoToyotaTM.R_REPARTO+" = '"+esecuzionePianiCarico.getReparto().getIdReparto()+"' "
					+ "AND "+YPianoCaricoToyotaTM.R_COD_MAPPA_UDC+" = '"+udc+"' AND "+YPianoCaricoToyotaTM.STATO_UDC+" = '"+StatoPrelievoUdcToyota.RICEVUTA+"' ";
			try {
				Vector piani = YPianoCaricoToyota.retrieveList(where, "", false);
				if(piani.size() == 0) {
					messaggio(false, "L'UDC "+udc+" non è presente in baia");
					pagina = UDC_NR_RITORNO;
				}else {
					Iterator iterRighe = ((YPianoCaricoToyota)piani.get(0)).getRighe().iterator();
					boolean tutteChiuse = true;
					while(iterRighe.hasNext()) {
						YPianoCaricoToyotaRiga riga = (YPianoCaricoToyotaRiga) iterRighe.next();
						if(riga.getStatoPrelievo() < StatoPrelievoRigaToyota.PRELEVATA && riga.getStatoRiga() == StatoRigaToyota.APERTA
								&& riga.getTipoMissione() == TipoMissionePianoCaricoRigaToyota.PRELIEVO) {
							tutteChiuse = false;
							if(//riga.getPrelevabile() &&
									 riga.getMissione() != null 
									&& riga.getMissione().getStatoMissione() != Missione.TERMINATA) {
								esecuzionePianiCarico.getElencoRighe().add(riga);
							}
						}
					}
					if(tutteChiuse) {
						esecuzionePianiCarico.setPianoInRiposizionamento((YPianoCaricoToyota) piani.get(0)); //.Me lo porto nella pagina successiva
						pagina = RIPOSIZ_UDC_MAGAZZINO;
					}else {
						pagina = ESECUZIONE_CARICO;
						tipoPrelievo = PRELIEVO_SU_UDC_REPARTO;
					}
				}
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException e) {
				e.printStackTrace(Trace.excStream);
			}

		}
		//.Altrimenti verifico se esistono delle righe con StatoPrelievo < 2, StatoRiga A su piani di carico con reparto/udc scelti
		else if(numeroRitorno != null && !numeroRitorno.isEmpty()) {
			List<YPianoCaricoToyotaRiga> righe = esecuzionePianiCarico.recuperaRigaPianoCaricoApertaFromNumRitorno(
					numeroRitorno);
			for (Iterator iterator = righe.iterator(); iterator.hasNext();) {
				YPianoCaricoToyotaRiga riga = (YPianoCaricoToyotaRiga) iterator.next();
				if(riga != null && riga.getPrelevabile() && riga.getMissione().getStatoMissione() != Missione.TERMINATA
						&& riga.getTipoMissione() == TipoMissionePianoCaricoRigaToyota.PRELIEVO) {
					esecuzionePianiCarico.getElencoRighe().add(riga);

				}
			}
			pagina = ESECUZIONE_CARICO;
			tipoPrelievo = PRELIEVO_SU_RIFERIMENTO;
		}else {
			messaggio(false, "Non sono presenti piani di carico aperti per il numero di ritorno sparato");
			pagina = UDC_NR_RITORNO;
		}
		//.Se la pagina e' la visualizzazione delle missioni le devo mettere in esecuzione
		if(pagina == ESECUZIONE_CARICO && esecuzionePianiCarico.getElencoRighe().size() > 0) {
			esecuzioneMissioni.getElencoMissioni().clear();
			esecuzioneMissioni.getElencoMissioniDB().clear();
			try {
				for (Iterator iterator = esecuzionePianiCarico.getElencoRighe().iterator(); iterator.hasNext();) {
					YPianoCaricoToyotaRiga riga = (YPianoCaricoToyotaRiga) iterator.next();
					Missione m = riga.getMissione();
					if(m.getStatoMissione() != Missione.ESECUZIONE) {

						m.setStatoMissione(Missione.ESECUZIONE);

						m.save();

						esecuzioneMissioni.getElencoMissioniDB().add(m);
						esecuzioneMissioni.getElencoMissioni().add(m);
					}
				}
				ConnectionManager.commit();
			}catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
				ConnectionManager.rollback();
				messaggio(true, e.getMessage());
			}
			esecuzioneMissioni.setPosMiss(0);
		}
		return null;
	}

	protected boolean esistonoRigheAperteNumeroRiferimento(String numeroRitorno) {
		String stmt = "SELECT "
				+ "	* "
				+ "FROM "
				+ "	THIPPERS.YPIANO_CARICO_TOYOTA_RIG R "
				+ "INNER JOIN THIPPERS.YPIANO_CARICO_TOYOTA_TES T ON "
				+ "	R.ID_AZIENDA = T.ID_AZIENDA "
				+ "	AND R.ID_ANNO_DOC = T.ID_ANNO_DOC "
				+ "	AND R.ID_NUMERO_DOC = T.ID_NUMERO_DOC "
				+ "WHERE "
				+ "	R.ID_AZIENDA = '"+Azienda.getAziendaCorrente()+"' "
				+ "	AND R.NUM_RIFERIMENTO = '"+numeroRitorno+"' "
				+ "	AND R.STATO_PRL_RIGA < '"+StatoPrelievoRigaToyota.PRELEVATA+"' "
				+ "	AND R.STATO_RIGA = '"+StatoRigaToyota.APERTA+"' "
				+ "	AND T.STATO_UDC = '"+StatoPrelievoUdcToyota.RICEVUTA+"' ";
		ResultSet rs = null;
		CachedStatement cs = null;
		try {
			cs = new CachedStatement(stmt);
			rs = cs.executeQuery();
			if(rs.next()) {
				return true;
			}
		}catch (Exception e) {
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
		return false;
	}

	@SuppressWarnings("rawtypes")
	protected void paginaSelezioneReparto() {
		TForm formReparti = getTForm(formPaginaSelezioneReparto);
		try {
			Vector listaReparti = caricaRepartiServitiAgv();
			caricaListaReparti(formReparti,listaReparti);
			sendForm(formReparti);
			TForm risposta = readInput();
			if (risposta.getKeyPressed().equals(TForm.KEY_ESC) ||
					risposta.getKeyPressed().equals(TForm.KEY_CTL_X)) {
				pagina = MENU;
				return;
			}
			if (risposta.getKeyPressed().equals(TForm.KEY_F1) || risposta.getKeyPressed().equals(TForm.KEY_F3)
					|| risposta.getKeyPressed().equals(TForm.KEY_F4) || risposta.getKeyPressed().equals(TForm.KEY_F6)
					|| risposta.getKeyPressed().equals(TForm.KEY_F9) || risposta.getKeyPressed().equals(TForm.KEY_F10))
				return;
			TList list = risposta.getTList("ListaReparti");
			int itemPos = list.getCurrentSelectedItem();
			list.getListSelectedItem().removeElement(new Integer(itemPos));
			list.selectCurrent();
			Reparto reparto = (Reparto) listaReparti.elementAt(itemPos - 1);
			esecuzionePianiCarico.setReparto(reparto);
			pagina = UDC_NR_RITORNO;
		} catch(Exception e) {
			e.printStackTrace(Trace.excStream);
			pagina = MENU;
		}
		return;
	}

	@SuppressWarnings("rawtypes")
	protected void caricaListaReparti(TForm form, Vector lista) {
		TList list = form.getTList("ListaReparti");
		list.resetSelected();
		Vector elencoVecchio = list.getItems();
		while (elencoVecchio.size() > 1) {
			elencoVecchio.removeElementAt(1);
		}
		for (int i = 0; i < lista.size(); i++) {
			Object obj = lista.elementAt(i);
			String item = "";
			if (obj instanceof Reparto) {
				Reparto r = (Reparto) obj;
				item = formatoReparto(form, r);
			}
			list.addItem(item);
		}
		if (lista.size() != 0) {
			list.setCurrentSelectedItem(1);
			list.setCurrentTopItem(1);
		}

	}

	protected String formatoReparto(TForm form, Reparto r) {
		String s = "";
		if(r != null) {
			s = r.getIdReparto() + "-" + r.getDescrizione().getDescrizione();
		}
		return s;
	}

	@SuppressWarnings("rawtypes")
	protected Vector caricaRepartiServitiAgv() {
		esecuzionePianiCarico.riempiElencoRepartiServitiAgv(true);
		return esecuzionePianiCarico.getElencoRepartiServitiAgv();
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected ErrorMessage caricaListeInEsecuzione(Vector lista) {
		ErrorMessage error = null;
		boolean postUds = esecuzioneMissioni.isAbilPrepUds();
		boolean gestUds = postUds;
		esecuzioneMissioni.getElencoTestate().removeAllElements();
		int i = 0;
		for (Iterator iterator = lista.iterator(); iterator.hasNext();) {
			Object obj = iterator.next();
			if (obj instanceof TestataLista) {
				TestataLista lis = (TestataLista) obj;
				esecuzioneMissioni.getElencoTestate().addElement(lis);
				if (postUds) {      // Postazione abilitata.
					boolean lisAbilUds = checkGestioneUds(lis);
					if (i == 0)             // La prima lista fissa la condizione. Le altre devono rispettare
						gestUds = lisAbilUds; //  questa. NON ammetto comportamenti misti.
					else {
						if (lisAbilUds != gestUds) {    // Errore.
							if (lisAbilUds)
								//Fix 15944 error = new ErrorMessage("LOGIS01054", "Lista " + lis.getCodice() + " con UdS gestibile in picking, diversamente dalle precedenti.");
								error = new ErrorMessage("LOGIST0097", lis.getCodice());
							else
								//Fix 15944 error = new ErrorMessage("LOGIS01054", "Lista " + lis.getCodice() + " con UdS NON gestibile in picking, diversamente dalle precedenti.");
								error = new ErrorMessage("LOGIST0098", lis.getCodice());
						}
					}
				}
			}
			if (obj instanceof PianificazioneLista) {
				gestUds = false;  // Non gestisce l'UdS in fase di prelievo, per cui si deve allestire
				//  al termine di esso. Utile per effettuare poi la ventilazione.
				if (postUds) {      // Postazione abilitata.
					boolean lisAbilUds = checkGestioneUds((PianificazioneLista) obj);
					if (i == 0)             // La prima lista fissa la condizione. Le altre devono rispettare
						gestUds = lisAbilUds; //  questa. NON ammetto comportamenti misti.
					else {
						if (lisAbilUds != gestUds) {    // Errore.
							if (lisAbilUds)
								//Fix 15944 error = new ErrorMessage("LOGIS01054", "Preparazione " + ((PianificazioneLista) obj).getCodice() + " con UdS gestibile in picking, diversamente dalle precedenti.");
								error = new ErrorMessage("LOGIST0103", String.valueOf(((PianificazioneLista) obj).getCodice()));
							else
								//Fix 15944 error = new ErrorMessage("LOGIS01054", "Preparazione " + ((PianificazioneLista) obj).getCodice() + " con UdS NON gestibile in picking, diversamente dalle precedenti.");
								error = new ErrorMessage("LOGIST0104", String.valueOf(((PianificazioneLista) obj).getCodice()));
						}
					}
				}
				esecuzioneMissioni.getElencoTestate().addElement(obj);
			}
			i++;
		}
		if (error != null)
			return error;
		esecuzioneMissioni.setAbilPrepUds(gestUds);
		if (esecuzioneMissioni.getElencoTestate().size() == 0)
			//Fix 15944 return new ErrorMessage("LOGIS01054", "Selezionare almeno una lista dall'elenco proposto.");
			return new ErrorMessage("LOGIST0105");
		return null;
	}

	protected boolean checkGestioneUds(TestataLista lista) {
		if (lista.getFlagUds() &&
				lista.getFlagPickPack()) {
			//13.03.09 - inizio
			if (esecuzioneMissioni.getPostazione().getAbilPostazioniTipoLista().size() == 0)
				return esecuzioneMissioni.getPostazione().getFlagPickPack();
			//13.03.09 - fine
			for (int i = 0; i < esecuzioneMissioni.getPostazione().getAbilPostazioniTipoLista().size(); i++) {
				AbilPostazioneTipoLista aptl = (AbilPostazioneTipoLista) esecuzioneMissioni.getPostazione().getAbilPostazioniTipoLista().get(i);
				if (aptl.getChiaveTipoLista().equals(lista.getChiaveTipoLista())) {
					return aptl.getFlagPickPack();
				}
			}
		}
		return false;
	}

	protected boolean checkGestioneUds(PianificazioneLista piano) {
		piano.riempiElencoTestata(false);
		boolean abil = false;
		for (int j = 0; j < piano.getElencoTestate().size() && !abil; j++) {
			TestataLista lista = (TestataLista) piano.getElencoTestate().elementAt(j);
			if (lista.getFlagUds() &&
					lista.getFlagPickPack()) {
				//28.01.22 - inizio
				if (esecuzioneMissioni.getPostazione().getAbilPostazioniTipoLista().size() == 0)
					return esecuzioneMissioni.getPostazione().getFlagPickPack();
				//28.01.22 - fine
				for (int i = 0; i < esecuzioneMissioni.getPostazione().getAbilPostazioniTipoLista().size(); i++) {
					AbilPostazioneTipoLista aptl = (AbilPostazioneTipoLista) esecuzioneMissioni.getPostazione().getAbilPostazioniTipoLista().get(i);
					if (aptl.getChiaveTipoLista().equals(lista.getChiaveTipoLista())) {
						abil = aptl.getFlagPickPack();
						break;
					}
				}
			}
		}
		return abil;
	}

	@Override
	public String getDefaultRESFile() {
		return RES_FILE;
	}
}
