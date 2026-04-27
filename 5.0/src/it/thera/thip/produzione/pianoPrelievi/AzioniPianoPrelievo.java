package it.thera.thip.produzione.pianoPrelievi;

import java.io.*;
import java.math.*;
import java.sql.*;
import java.util.*;

import com.thera.thermfw.ad.ClassADCollection;
import com.thera.thermfw.ad.ClassADCollectionManager;
import com.thera.thermfw.base.*;
import com.thera.thermfw.batch.BatchJob;
import com.thera.thermfw.persist.*;
import com.thera.thermfw.collector.BODataCollector;
import com.thera.thermfw.collector.SecondaryDataCollector;
import com.thera.thermfw.common.*;

import it.thera.thip.base.articolo.*;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.azienda.AziendaTM;
import it.thera.thip.base.azienda.MagazzinoTM;
import it.thera.thip.base.commessa.*;
import it.thera.thip.base.generale.*;
import it.thera.thip.magazzino.saldi.*;
import it.thera.thip.produzione.ordese.*;
import it.thera.thip.cs.*;
import it.thera.thip.datiTecnici.modpro.Attivita;
import it.thera.thip.logis.lgb.*;
import it.thera.thip.logisticaLight.*;

/**
 * AzioniPianoPrelievo
 * <br></br><b>Copyright (C) : Thera s.p.a.</b>
 * @author Laura Pezzolini 18/07/2007 at 17:36:50
 */
/*
 * Revisions:
 * Number  Date          Owner      Description
 * 07702   19/07/2007    LP         Codice generato da Wizard
 * 07875   19/09/2007    IT         Implementato metodo generazioneListePrlDaPiano()
 * 08419   13/12/2007    IT         Corretto ritorno messaggio errore
 *                       MM         Corretto aggiornamento stato trasmissione sul modello nel caso di generazione liste
 *                       LP         Correzioni varie
 *         29/01/2008    MM         inserito rollback in caso di errore cancellazione piano precedente
 * 09095   28/04/2008    MM         migliorata gestione ThipException in generazione lista
 * 13560   24/11/2010    Hammadi    se l'articolo ha movimentazione a quantità intera o l'UM primaria di magazzino è a quantità intera,
                                     la  quantità massima producibile  deve essere sempre arrotondata per difetto
 * 17440   19/02/2013    IE         Correzione del metodo forzaChiusuraListePrelievo()
 * 22443   03/11/2015    LTB        Aggiungere la gestione ubicazioni e barcode 
 * 22873   21/01/2016    LTB        Generazione di doc. produzioni nel regenerazione di piani prl.
 * 23872   12/07/2016    LTB        Gestione di errori nel gen di doc prd nel metodo forzaChiusuraListePrelievoUB
 * 24041   30/08/2016    TF         Processare gli ordini per data richiesta
 * 25856   31/05/2017    PM         Se il modello gestisce ubicazioni e barcode il controllo di compatibilità  verso la logistica avanzata non deve essere effettuato
 * 28132   26/10/2018    RA			Modifcare il metodo generazioneListePrlDaPiano affichè nel "finally" effettui rollback in ogni caso
 * 28316   27/11/2018    LTB        Aggiunto il controllo sul attivita esecutiva nel recupera della lista prelievo testata UBI
 * 28776   14/02/2019	 RA			Aggiunto filtro per ID_AZIENDA nel metodo costruisciWhere
 * 28974   01/04/2019    Mekki      Recupero della idAzienda del Modello di Prelievo nel metodo costruisciWhere
 * 29772   02/09/2019    Jackal     Fix tecnica per personalizzazioni
 * 30366   13/12/2019	 TJ			Cambia il parametro del metodo forzaChiusuraListePrelievo(...)
 * 30548   20/01/2020    RA			La generazione dei piani di prelievo non dovrebbe considerare i materiali WIP
 * 30965   16/04/2020    Bsondes    Decimali .
 * 31875   23/09/2020    Jackal     Fix tecnica per personalizzazioni
 * 34411   27/10/2021	 TJ			Generazione Liste di prelievo per i servizi
 * 35702   21/04/2022    Mekki      La generazione dei piani di prelievo non dovrebbe considerare i materiali  di un''attività di produzione esterna
 * 38139   24/03/2023    LTB        Modificare la gestione della push e pop ConnectionManager nel caso del UBI
 * 38163   21/03/2023    Mekki      Correggere getOrderBy()
 * 39402   24/07/2023    SZ      	Scale errato se il database ha le quantità a 6 decimali
 * 40182   10/11/2023	 TJ			Aggiungere la gestione di Escludi attività già presenti in altro piano 
 * 38083   13/03/2023    SBR 		Cambia in metodo generazioneListePrlDaPiano ()
 * 39017   04/07/2023    MN         Riallineamento Intellimag al 4.7.25
 * 41316   06/02/2024    SBR        Riallineamento Intellimag al 5.0
 * 42413   27/05/2024	 TJ			Restituire il dettagli dell'errore che ha impedito la generazione
 * 44367   19/12/2024	 Jackal		Aggiunti ganci per personalizzazioni
 * 45079   04/03/2025	 Jackal		Aggiunti ganci per personalizzazioni
 * 45425   04/04/2025	 Mekki		modificato lo statement TROVA_ATV_IN_ALTRO_PIANO in modo che l'alias non sia p, ma P (SQLServer)
 * 47321   13/10/2025	 Mekki		Impostare qtaMinProducibile a ZERO nel caso sia null
 * 47561   11/11/2025	 TJ			Nel caso di errori applicativi, impostare Stato Applicativo a Presenti warning
 */

public class AzioniPianoPrelievo {

  /**
   * Attributo iModelloPrelievo
   */
  protected ModelloPrelievo iModelloPrelievo;

  /**
   * Attributo iDataFineOrizzonte
   */
  protected java.sql.Date iDataFineOrizzonte;

  /**
   * Attributo iPreselezione
   */
  protected char iPreselezione;

  /**
   * Attributo iGesUbicazioneEBarcode
   */
  protected boolean iGesUbicazioneEBarcode; // Fix 22443
  
  /**
   * Attributo iEscludiAtvPresentInAltroPian
   */
  protected boolean iEscludiAtvInAltroPiano = false; //Fix 40182
  
  /**
   * Output del lavoro batch. Ha funzione puramente informativa.
   */
  protected PrintWriter output;
  
  protected BatchJob iBatchJob; //Fix 47561

  /**
   * Costanti simboliche.
   */
  public static final String PARAM_RES_FILE = "it.thera.thip.produzione.pianoPrelievi.resources.AzioniPianoPrelievi";
  public static final String RES_FILE = "it.thera.thip.produzione.pianoPrelievi.resources.PianoPrelievi";
  public static final String GENERAZIONE_LISTE = "GENERAZIONE_LISTE";
  public static final String RICALCOLO_QUANTITA = "RICALCOLO_QUANTITA";
  public static final String CHIUSURA_PIANO = "CHIUSURA_PIANO";

  /**
   * String RES_FILE
   */
  public static final BigDecimal ZERO = new BigDecimal("0");

  /**
   * Attributo iNumCommit
   */
  protected int cNumCommit = 1;

  /**
   * Attributo iStampaLogCalcoli
   */
  protected boolean cStampaLogCalcoli = false;

  /**
   * Attributo iEndOkTxt
   */
  protected String iEndOkTxt;

  /**
   * Attributo iEndErrTxt
   */
  protected String iEndErrTxt;

  /**
   * Attributo iFiltroPianoAssente
   */
  protected boolean iFiltroPianoAssente;

  /**
   * Attributo iAggiornaStatoGenLista
   */
  protected boolean iAggiornaStatoGenLista = true; //...FIX 8419

  /**
   * iUpdateModPrlConnDescr
   */
  protected static ConnectionDescriptor iUpdateModPrlConnDescr = new ConnectionDescriptor(ConnectionManager.getCurrentDBName(), ConnectionManager.getCurrentUser(), ConnectionManager.getCurrentPassword(), ConnectionManager.getCurrentDatabase());

  /**
   * TROVA_LISTE_GENERATE.
   */
  protected static final String TROVA_LISTE_GENERATE =
    "SELECT COUNT(*) FROM " + PianoPrelieviTestataTM.TABLE_NAME +
    " WHERE " + PianoPrelieviTestataTM.ID_AZIENDA + " = ?" +
    " AND " + PianoPrelieviTestataTM.ID_MODELLO_PRL + " = ?" +
    " AND " + PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_ESEGUITO + "'";

  /**
   * CachedStatement cTrovaListeGenerate.
   */
  protected static CachedStatement cTrovaListeGenerate = new CachedStatement(TROVA_LISTE_GENERATE);

//Fix 08419 MM inizio
  /**
   * TROVA_LISTE_NON_GENERATE.
   */
  protected static final String TROVA_LISTE_NON_GENERATE =
    "SELECT COUNT(*) FROM " + PianoPrelieviTestataTM.TABLE_NAME +
    " WHERE " + PianoPrelieviTestataTM.ID_AZIENDA + " = ? AND " +
                PianoPrelieviTestataTM.ID_MODELLO_PRL + " = ?  AND " +
                "(" + PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_DA_ESEG + "' OR " +
                      PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_NO + "')";

  /**
   * CachedStatement cTrovaListeGenerateOChiuse.
   */
  protected static CachedStatement cTrovaListeNonGenerate = new CachedStatement(TROVA_LISTE_NON_GENERATE);
//Fix 08419 MM fine

  /**
   * CANCELLA_PIANI_PRL.
   */
  protected static final String CANCELLA_PIANI_PRL =
    "DELETE FROM " + PianoPrelieviTestataTM.TABLE_NAME +
    " WHERE " + PianoPrelieviTestataTM.ID_AZIENDA + " = ?" +
    " AND " + PianoPrelieviTestataTM.ID_MODELLO_PRL + " = ?";

  /**
   * CachedStatement cCancellaPianiPrl.
   */
  protected static CachedStatement cCancellaPianiPrl = new CachedStatement(CANCELLA_PIANI_PRL);
  
  //37090
  /**
   * CANCELLA_PIANI_PRL_INT.
   */
  protected static final String CANCELLA_PIANI_PRL_INT =
    "DELETE FROM " + PianoPrelieviTestataTM.TABLE_NAME +
    " WHERE " + PianoPrelieviTestataTM.ID_AZIENDA + " = ?" +
    " AND " + PianoPrelieviTestataTM.ID_MODELLO_PRL + " = ?"+
    " AND " + PianoPrelieviTestataTM.STATO_INTELLIMAG + " <> '"+PianoPrelieviTestata.BLOCCATO+"'";

  /**
   * CachedStatement cCancellaPianiPrl.
   */
  protected static CachedStatement cCancellaPianiPrlInt = new CachedStatement(CANCELLA_PIANI_PRL_INT);
  //37090

  /**
   * TROVA_RIGHE_CALC_DISP.
   */
  protected static final String TROVA_RIGHE_CALC_DISP =
    "SELECT * FROM " + SystemParam.getSchema("THIP") + "PIANI_PRL_RIG_V01" +
    " WHERE " + PianoPrelieviRigaTM.ID_AZIENDA + " = ?" +
    " AND " + PianoPrelieviRigaTM.ID_MODELLO_PRL + " = ?" +
    " ORDER BY " + PianoPrelieviRigaTM.R_MAGAZZINO + "," +
    PianoPrelieviRigaTM.ID_ARTICOLO + "," +
    PianoPrelieviRigaTM.R_VERSIONE + "," +
    PianoPrelieviRigaTM.R_CONFIGURAZIONE + "," +
    PianoPrelieviRigaTM.R_COMMESSA + "," +
    PianoPrelieviRigaTM.ID_LOTTO + " DESC," +
    PianoPrelieviTestataTM.DATA_INIZIO + "," +
    PianoPrelieviTestataTM.PRIORITA;

  /**
   * CachedStatement cTrovaRigheCalcDisp.
   */
  protected static CachedStatement cTrovaRigheCalcDisp = new CachedStatement(TROVA_RIGHE_CALC_DISP);

  /**
   * TROVA_RIGHE_CALC_DISP.
   */
  protected static final String TROVA_RIGHE_CALC_MAX_PRD =
    "SELECT * FROM " + SystemParam.getSchema("THIP") + "PIANI_PRL_RIG_V01" +
    " WHERE " + PianoPrelieviRigaTM.ID_AZIENDA + " = ?" +
    " AND " + PianoPrelieviRigaTM.ID_MODELLO_PRL + " = ?" +
    " ORDER BY " + PianoPrelieviRigaTM.ID_ANNO_ORD + "," +
    PianoPrelieviRigaTM.ID_NUMERO_ORD + "," +
    PianoPrelieviRigaTM.ID_RIGA_ATTIVITA;

  /**
   * CachedStatement cTrovaRigheCalcMaxQtaPrd.
   */
  protected static CachedStatement cTrovaRigheCalcMaxQtaPrd = new CachedStatement(TROVA_RIGHE_CALC_MAX_PRD);
  
  //Fix 40182 -- Inizio
  protected static final String TROVA_ATV_IN_ALTRO_PIANO =
		    //"SELECT p." + PianoPrelieviTestataTM.ID_AZIENDA + " FROM " + PianoPrelieviTestataTM.TABLE_NAME + " p " + //Fix 45425
		    "SELECT P." + PianoPrelieviTestataTM.ID_AZIENDA + " FROM " + PianoPrelieviTestataTM.TABLE_NAME + " P " + //Fix 45425
			"LEFT OUTER JOIN " +  ModelloPrelievoTM.TABLE_NAME + " M ON P." 
		    + PianoPrelieviTestataTM.ID_AZIENDA + " = M." + ModelloPrelievoTM.ID_AZIENDA + " AND P." 
			+ PianoPrelieviTestataTM.ID_MODELLO_PRL +  " = M." + ModelloPrelievoTM.ID_MODELLO_PRL + 
		    " WHERE P." + PianoPrelieviTestataTM.ID_AZIENDA + " = ? AND P." + PianoPrelieviTestataTM.ID_ANNO_ORD + " = ? " + 
			" AND P." + PianoPrelieviTestataTM.ID_NUMERO_ORD + " = ? AND P." + PianoPrelieviTestataTM.ID_RIGA_ATTIVITA + " = ? " + 
		    " AND P." + PianoPrelieviTestataTM.ID_MODELLO_PRL + " <> ? AND P." + PianoPrelieviTestataTM.STATO_GEN_LISTA + " <> '3' " + 
			" AND P." + PianoPrelieviTestataTM.STATO + " = 'V'  AND M." + ModelloPrelievoTM.STATO_PIANO + " <> '-'  AND M." + ModelloPrelievoTM.STATO + " = 'V' ";

  /**
   * CachedStatement cTrovaListeGenerateOChiuse.
   */
  protected static CachedStatement cTrovaAtvInAltroPiano = new CachedStatement(TROVA_ATV_IN_ALTRO_PIANO);
  //Fix 40182 -- Fine

  /**
   * setModelloPrelievo
   * @param modelloPrelievo ModelloPrelievo
   */
  public void setModelloPrelievo(ModelloPrelievo modelloPrelievo) {
    iModelloPrelievo = modelloPrelievo;
  }

  /**
   * getModelloPrelievo
   * @return ModelloPrelievo
   */
  public ModelloPrelievo getModelloPrelievo() {
    return iModelloPrelievo;
  }

  /**
   * Valorizza l'attributo.
   * @param dataFineOrizzonte java.sql.Date
   */
  public void setDataFineOrizzonte(java.sql.Date dataFineOrizzonte) {
    iDataFineOrizzonte = dataFineOrizzonte;
  }

  /**
   * Restituisce l'attributo.
   * @return java.sql.Date
   */
  public java.sql.Date getDataFineOrizzonte() {
    return iDataFineOrizzonte;
  }

  /**
   * Valorizza l'attributo.
   * @param preselezione char
   */
  public void setPreselezione(char preselezione) {
    iPreselezione = preselezione;
  }

  /**
   * Restituisce l'attributo.
   * @return char
   */
  public char getPreselezione() {
    return iPreselezione;
  }

  // Fix 22443 inizio
  /**
   * Valorizza l'attributo.
   * @param gesUB boolean
   */
  public void setGesUbicazioneEBarcode(boolean gesUB) {
  	iGesUbicazioneEBarcode = gesUB;
  }

  /**
   * Restituisce l'attributo.
   * @return boolean
   */
  public boolean isGesUbicazioneEBarcode() {
    return iGesUbicazioneEBarcode;
  }
  // Fix 22443 fine
  
  /**
   * setOutput
   * @param out PrintWriter
   */
  public void setOutput(PrintWriter out) {
    output = out;
  }

  /**
   * Valorizza l'attributo.
   * @param filtroPianoAssente boolean
   */
  public void setFiltroPianoAssente(boolean filtroPianoAssente) {
    iFiltroPianoAssente = filtroPianoAssente;
  }

  /**
   * Restituisce l'attributo.
   * @return boolean
   */
  public boolean isFiltroPianoAssente() {
    return iFiltroPianoAssente;
  }

  //...FIX 8419 inizio

  /**
   * Valorizza l'attributo.
   * @param aggiornaStatoGenLista boolean
   */
  public void setAggiornaStatoGenLista(boolean aggiornaStatoGenLista) {
    iAggiornaStatoGenLista = aggiornaStatoGenLista;
  }

  /**
   * Restituisce l'attributo.
   * @return boolean
   */
  public boolean isAggiornaStatoGenLista() {
    return iAggiornaStatoGenLista;
  }

  //...FIX 8419 inizio

  /**
   * AzioniPianoPrelievo
   */
  public AzioniPianoPrelievo() {
    String numCmmPrm = ResourceLoader.getString(PARAM_RES_FILE, "NumCommit");
    String stampaLogPrm = ResourceLoader.getString(PARAM_RES_FILE, "StampaLogCalcoli");
    cNumCommit = new Integer(numCmmPrm).intValue();
    cStampaLogCalcoli = stampaLogPrm.equals("Y") ? true : false;
    iEndOkTxt = ResourceLoader.getString(RES_FILE, "EndOkTxt");
    iEndErrTxt = ResourceLoader.getString(RES_FILE, "EndErrTxt");
  }

  /**
   * trovaListeGenerateDelPiano
   * @return String
   */
  public synchronized int trovaListeGenerateDelPiano() {
    try {
      Database db = ConnectionManager.getCurrentDatabase();
      PreparedStatement ps = cTrovaListeGenerate.getStatement();
      db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
      db.setString(ps, 2, getModelloPrelievo().getIdModelloPrelievo());
      ResultSet rs = ps.executeQuery();
      int ret = (rs.next()) ? rs.getInt(1) : 0;
      rs.close();
      return ret;
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return 0;
    }
  }

//Fix 08419 MM
  /**
   * trovaListeNonGenerateGenerateDelPiano
   * @return String
   */
  protected static synchronized int trovaListeNonGenerateDelPiano(String idAzienda, String idModello) {
    ResultSet rs = null;
    int ret = 0;
    try {
      Database db = ConnectionManager.getCurrentDatabase();
      PreparedStatement ps = cTrovaListeNonGenerate.getStatement();
      db.setString(ps, 1, idAzienda);
      db.setString(ps, 2, idModello);
      rs = ps.executeQuery();
      ret = (rs.next()) ? rs.getInt(1) : 0;
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if (rs != null) {
        try {
          rs.close();
        }
        catch (SQLException  e) {
          e.printStackTrace(Trace.excStream);
        }
      }
    }
    return ret;
  }


  /**
   * chiusuraPianoPrecedente
   * @return boolean
   */
  public boolean chiusuraPianoPrecedente() {
    if(isGesUbicazioneEBarcode()) //Fix 22443
    	return chiusuraPianoPrecedenteUB(); // Fix 22443
  	
    boolean ret = true;
    //Apro nuova connessione
    try {
      ConnectionManager.pushConnection(iUpdateModPrlConnDescr);

      //...Si ricercano le eventuali Liste non “chiuse” relative al Piano Prelievi
      //...associato al Modello di input (tramite la chiave della Lista generata presente
      //...sulle righe del Piano) e si procede alla loro chiusura forzata (con cancellazione
      //...delle Missioni non completate).
      boolean okChiusuraListe = forzaChiusuraListePrelievo();
      String endChiusuraListeTxt = ResourceLoader.getString(RES_FILE, "EndChiusuraListe");
      if(okChiusuraListe) {
        ret = true;
        output.println(endChiusuraListeTxt + " " + iEndOkTxt);
      }
      //...FIX 8419 inizio
      else {
        ret = false;
        output.println(endChiusuraListeTxt + " " + iEndErrTxt);
      }
      //...FIX 8419 fine

      //...Quindi si cancellano tutte le Testate e le Righe del Piano Prelievi
      //...corrispondenti al Modello di input e si effettua commit.
      if(ret) { //...FIX 8419
        int numPiani = cancellaPianiPrelievi();

        String endChiusuraTxt = ResourceLoader.getString(RES_FILE, "EndChiusuraTxt");
        if(numPiani > 0) {
          ret = true;
          output.println(endChiusuraTxt + " " + iEndOkTxt);
        }
        else if(numPiani < 0) {
          ret = false;
          output.println(endChiusuraTxt + " " + iEndErrTxt);
        }
      }

      if (ret) {
        ConnectionManager.commit();
      }
      else//Fix 08419 MM 29/01/2008
        ConnectionManager.rollback();
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return false;
    }
    finally {
      if (iUpdateModPrlConnDescr != null)
        ConnectionManager.popConnection(iUpdateModPrlConnDescr);
    }
    return ret;
  }

  public String costruisciWhere() {
    Database db = ConnectionManager.getCurrentDatabase();
    //String where = "";//28776
  //String where = AziendaTM.ID_AZIENDA + "='" + Azienda.getAziendaCorrente() + "' ";//28776 //Fix 28974
    String where = AziendaTM.ID_AZIENDA + "='" + getModelloPrelievo().getIdAzienda() + "' ";//28776 //Fix 28974
    //...si selezionano le righe che soddisfano le condizioni di filtro espresse
    //...nel Modello di Prelievo
    String whereFiltri = getModelloPrelievo().getFiltri().getCondizioneWhere();
    if(whereFiltri != null && !whereFiltri.equals(""))
      //where = whereFiltri;//28776
      where += " AND "+ whereFiltri;//28776
    //...che hanno Articolo gestito in Logistica (controllo di compatibilità tramite
    //...apposito metodo della classe CfgLogTxArtic) --> questo controllo viene fatto dopo

    //...che hanno DataPrvImpiego del Materiale <= Data fine orizzonte
    String whereDataPrvImp = WrapperPianoPrlDatiTM.DATA_PRV_IMPIEGO + " <= " + db.getLiteral(getDataFineOrizzonte());
    if(whereDataPrvImp != null && !whereDataPrvImp.equals("")) {
      if(where != null && !where.equals(""))
        where += " AND ";
      where += whereDataPrvImp;
    }

    //...che hanno ModalitaPrelievo del Materiale = ‘Normale’ --> filtro già nella vista
    //...che hanno StatoOrdine = (‘Confermato’, ‘Schedulato’, ‘In corso’) --> filtro già nella vista

    //...che hanno StatoSchedulazione dell’Ordine coerente con TipoSchedOrdini
    //...del Modello di Prelievo
    String whereStatoSchedul = "";
    char statoSched = getModelloPrelievo().getTipoSchedOrdini();
    if(statoSched == ModelloPrelievo.TP_SCHED_SCHEDULATI)
      whereStatoSchedul = WrapperPianoPrlDatiTM.STATO_SCHEDUL + " = '" + OrdineEsecutivo.SCHEDULATO + "'";
    else
      if(statoSched == ModelloPrelievo.TP_SCHED_PROGRAMMATI)
        whereStatoSchedul = WrapperPianoPrlDatiTM.STATO_SCHEDUL + " = '" + OrdineEsecutivo.PROGRAMMATO + "'";
      else
        if(statoSched == ModelloPrelievo.TP_SCHED_SCHED_E_PGM)
          whereStatoSchedul = WrapperPianoPrlDatiTM.STATO_SCHEDUL + " IN ('" + OrdineEsecutivo.PROGRAMMATO + "','" + OrdineEsecutivo.SCHEDULATO + "')";

    if(whereStatoSchedul != null && !whereStatoSchedul.equals("")) {
      if(where != null && !where.equals(""))
        where += " AND ";
      where += whereStatoSchedul;
    }

    //...che hanno Stato di ciascuna entità in abbinamento = ‘V’ --> filtro già nella vista

    //...che hanno Quantità residua in UM Primaria del Lotto Materiale > 0
    String whereQtaResLotto = WrapperPianoPrlDatiTM.QTA_RES_PRM_LT + " > 0";
    if(where != null && !where.equals(""))
      where += " AND ";
    where += whereQtaResLotto;

    return where;
  }

  //Fix 34411 -- Inizio
  /**
   * generazioneNuovoPiano
   * @return boolean
   */
  public boolean generazioneNuovoPiano() {
	  return generazioneNuovoPianoPrd();
  }
  //Fix 34411 -- Fine
  /**
   * generazioneNuovoPianoPrd
   * @return boolean
   */
  //  public boolean generazioneNuovoPiano() { //Fix 34411
  public boolean generazioneNuovoPianoPrd() { //Fix 34411
    boolean ret = true;
    String endGenPianoTxt = ResourceLoader.getString(RES_FILE, "EndGenPianoTxt");
    try {

      //...L'abbinamento concettuale fra OrdineEsecutivo e relativo Articolo,
      //...AttivitaEsecutiva, AttivitaEsecutivaMateriali e AttivitaEsecutivaMaterialiLotti
      //...è stato fatta con la vista di database ORD_ESEC_ATV_V02 che si mappa
      //...sull'oggetto WrapperPianoPrlDati
      String where = costruisciWhere();

      //...L’ordinamento della selezione è Stabilimento/Reparto/Ordine/Attività/Materiale/Lotto
      //Fix 29772 - inizio
//      String orderBy = WrapperPianoPrlDatiTM.R_STABILIMENTO + "," +
//        WrapperPianoPrlDatiTM.R_REPARTO + "," +
//        WrapperPianoPrlDatiTM.DATA_PRV_IMPIEGO + "," +  //Fix 24041
//        WrapperPianoPrlDatiTM.ID_ANNO_ORD + "," +
//        WrapperPianoPrlDatiTM.ID_NUMERO_ORD + "," +
//        WrapperPianoPrlDatiTM.ID_RIGA_ATTIVITA + "," +
//        WrapperPianoPrlDatiTM.ID_RIGA_MATERIALE + "," +
//        WrapperPianoPrlDatiTM.ID_LOTTO;
      String orderBy = getOrderBy();
      //Fix 29772 - fine
      Vector elencoWrapper = WrapperPianoPrlDati.retrieveList(where, orderBy, false);
      riordinaElencoWrapper(elencoWrapper);

      String chiavePerRicFiltro = "";
      String primaChiaveRottura = "";
      String chiaveRotturaAttivita = "";	//Fix 29772
      CfgLogTxPianoPrelievo filtroPianoPrl = null;
      PianoPrelieviTestata testataPiano = null;
      int countTestate = 0;
      output.println("Totale attivita da processare: " + elencoWrapper.size());//72459 AGSOF3
      for(int i = 0; i < elencoWrapper.size(); i++) {
        WrapperPianoPrlDati wrPianoPrlDati = (WrapperPianoPrlDati)elencoWrapper.get(i);
        long start = System.nanoTime();//ASGOF3
        if (isRecordProcessabile(wrPianoPrlDati)) {		//Fix 31875
        	output.println("Processo attivita n.: " + i + " ("+wrPianoPrlDati.getKey()+")");//72459 AGSOF3
	        //...Filtro sugli gli articoli gestiti in Logistica (il controllo di compatibilità
	        //...avviene tramite un apposito metodo della classe CfgLogTxArtic);
	        String articoloKey = wrPianoPrlDati.getIdAzienda() + PersistentObject.KEY_SEPARATOR + wrPianoPrlDati.getRArticoloMat();
	        Articolo art = (Articolo)Factory.createObject(Articolo.class);
	        art.setKey(articoloKey);
	        boolean ext = art.retrieve();
	        //if(ext && CfgLogTxArtic.compatibile(art)) {  //Fix 25856 PM 
	        //30548 inizio
	        boolean escludiArticolo = false;
	        if (art.getTipoParte() == ArticoloDatiIdent.ARTICOLO_WIP) {
	          ParametroPsn paramPsn = ParametroPsn.getParametroPsn("std.pianiPrelievo", "escludiArticoliWIP", Azienda.getAziendaCorrente());
	          if (paramPsn != null && paramPsn.getValore().equals("Y"))
	            escludiArticolo = true;
	        }
	        //Fix 35702 inizio
	        if (!escludiArticolo) {
	        	ParametroPsn paramPsn = ParametroPsn.getParametroPsn("std.pianiPrelievo", "escludiArticoliProdEst", Azienda.getAziendaCorrente());
	    	    if (paramPsn != null && paramPsn.getValore().equals("Y")) {
	               String attivitaKey = wrPianoPrlDati.getIdAzienda() + PersistentObject.KEY_SEPARATOR + wrPianoPrlDati.getRAttivita();
	               Attivita atv = (Attivita) Factory.createObject(Attivita.class);
	               atv.setKey(attivitaKey);
	     	       boolean tmp = atv.retrieve();
	    	       if (tmp && atv.getTipoAttivita() == Attivita.PROD_ESTERNA) 
	    	          escludiArticolo = true;
	           }
	        }
	        //Fix 35702 fine
	        //if(ext && (isGesUbicazioneEBarcode() || CfgLogTxArtic.compatibile(art))) {  //Fix 25856 PM //30548
	        if(ext && (isGesUbicazioneEBarcode() || CfgLogTxArtic.compatibile(art)) && !escludiArticolo) {	
	        //30548 fine
	          String stabilimento = wrPianoPrlDati.getRStabilimento();
	          String reparto = wrPianoPrlDati.getRReparto();
	          String chiaveTmpRicFiltro = KeyHelper.buildObjectKey(new String[] {stabilimento, reparto});
	          //...A rottura di Stabilimento / Reparto, si acquisisce l’oggetto “Filtro Piano di
	          //...Prelievo” identificato attraverso il metodo di ricerca definito per tale entità
	          if(!isGesUbicazioneEBarcode()) { //Fix 22443
	          	if(!chiavePerRicFiltro.equals(chiaveTmpRicFiltro)) {
	          		chiavePerRicFiltro = chiaveTmpRicFiltro;
	          		filtroPianoPrl = CfgLogTxPianoPrelievo.trovaPianoPrelievoAbilitato(wrPianoPrlDati.getRStabilimento(), wrPianoPrlDati.getRReparto(), wrPianoPrlDati.getIdAzienda());
	          		if(filtroPianoPrl == null) {
	          			String noFiltroPianoTxt = ResourceLoader.getString(RES_FILE, "NoFiltroPianoTxt", new Object[] {wrPianoPrlDati.getRStabilimento(), wrPianoPrlDati.getRReparto()});
	          			output.println(noFiltroPianoTxt);
	          			setFiltroPianoAssente(true);
	          		}
	          	}
	
	          	if(filtroPianoPrl == null) {
	          		continue;
	          	}
	          }
	          
	          //Fix 29772 - inizio
	//          String idAnnoOrdine = wrPianoPrlDati.getIdAnnoOrd();
	//          String idNumeroOrdine = wrPianoPrlDati.getIdNumeroOrd();
	//          Integer idRigaAttivita = wrPianoPrlDati.getIdRigaAttivita();
	//          String primaChiaveRotturaTmp = KeyHelper.buildObjectKey(new Object[] {stabilimento, reparto, idAnnoOrdine, idNumeroOrdine, idRigaAttivita});
	          String primaChiaveRotturaTmp = getPrimaChiaveRotturaPianoPrelievoTestata(stabilimento, reparto, wrPianoPrlDati);
	          //Fix 29772 - fine
	
	          //...A rottura di Stabilimento/Reparto/Ordine/Attività, si genera una
	          //...testata di Piano Prelievi
	          if(!primaChiaveRottura.equals(primaChiaveRotturaTmp)) {
	            primaChiaveRottura = primaChiaveRotturaTmp;
	            if(testataPiano != null) {
	              boolean eseguiCommit = false;
	              if(countTestate % cNumCommit == 0)
	                eseguiCommit = true;
	              salvaTestataPiano(testataPiano, eseguiCommit);
	            }
	            //Fix 29772 - inizio
	            String chiaveRotturaAttivitaTmp = getChiaveRotturaAttivitaPianoPrelievoTestata(stabilimento, reparto, wrPianoPrlDati);
	            if (!chiaveRotturaAttivita.equals(chiaveRotturaAttivitaTmp)) {
	            	chiaveRotturaAttivita = chiaveRotturaAttivitaTmp;
	            	rilevaRotturaAttivita();
	            }
	            //Fix 29772 - fine
	            //Fix 40182 -- Inizio
	            if (isEscludiAtvInAltroPiano() && esisteAtvInAltroPianoPrd(wrPianoPrlDati)) 
	               testataPiano = null;
	            else { 
	            //Fix 40182 -- Fine
					 if(PersDatiGen.isGestitioIntellimag()) {//37090
		            	PianoPrelieviTestata pianoPrlTest = getPianoPrelievoTestata(wrPianoPrlDati);
		            	if(pianoPrlTest != null && pianoPrlTest.getStatoIntellimag() == PianoPrelieviTestata.BLOCCATO) {
		            		testataPiano = null;
		            	}else {
				            testataPiano = creaPianoPrelieviTestata(wrPianoPrlDati , filtroPianoPrl);
				            countTestate++;
		            	}//37090
		            }else {
			            testataPiano = creaPianoPrelieviTestata(wrPianoPrlDati , filtroPianoPrl);
			            countTestate++;
		            }
	            } //Fix 40182
	          }
	          if(testataPiano != null) //Fix 40182
	            creaPianoPrelieviRiga(testataPiano, wrPianoPrlDati);
	        }
        }
        //Fix 31875 - inizio
        else {
        	visualizzaMessaggioRecordNonProcessabile(wrPianoPrlDati);
        }
        //Fix 31875 - fine
      //72459 AGSOF3 INI
        output.println("Termine attivita n.: " + i + " ("+wrPianoPrlDati.getKey()+")");
        long end = System.nanoTime();
        double durationSeconds = (end - start) / 1_000_000_000.0;
        output.println("/// Durata: " + durationSeconds + " secondi");
        output.println("");
      //72459 AGSOF3 FIN
      }
      //...Salvo l'ultima testata creata e faccio commit
      if(testataPiano != null) {
        salvaTestataPiano(testataPiano, true);
      }

      if(countTestate > 0)
        output.println(endGenPianoTxt + " " + iEndOkTxt);

    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      output.println(endGenPianoTxt + " " + iEndErrTxt);
      return false;
    }
    return ret;
  }
  
  
  /**
   * creaPianoPrelieviTestata
   * @param wrPianoPrlDati WrapperPianoPrlDati
   * @param filtroPianoPrl CfgLogTxPianoPrelievo
   * @return PianoPrelieviTestata
   */
  public PianoPrelieviTestata creaPianoPrelieviTestata(WrapperPianoPrlDati wrPianoPrlDati , CfgLogTxPianoPrelievo filtroPianoPrl) {
    PianoPrelieviTestata pianoPrlTes = (PianoPrelieviTestata)Factory.createObject(PianoPrelieviTestata.class);
    pianoPrlTes.setIdAzienda(wrPianoPrlDati.getIdAzienda());
    //Fix 29772 - inizio
//    pianoPrlTes.setIdModelloPrl(getModelloPrelievo().getIdModelloPrelievo());
    pianoPrlTes.setIdModelloPrl(getIdModelloPrelievoDaAssegnare());
    //Fix 29772 - fine
    pianoPrlTes.setTipoModelloPrl(PianoPrelieviTestata.TP_MOD_CORRENTE);
    pianoPrlTes.setIdAnnoOrd(wrPianoPrlDati.getIdAnnoOrd());
    pianoPrlTes.setIdNumeroOrd(wrPianoPrlDati.getIdNumeroOrd());
    pianoPrlTes.setIdRigaAttivita(wrPianoPrlDati.getIdRigaAttivita());
    pianoPrlTes.setRArticolo(wrPianoPrlDati.getRArticolo());
    pianoPrlTes.setRVersione(wrPianoPrlDati.getRVersione());
    pianoPrlTes.setRConfigurazione(wrPianoPrlDati.getRConfigurazione());
    pianoPrlTes.setDescArticolo(wrPianoPrlDati.getDescrizione());
    pianoPrlTes.setRCommessa(wrPianoPrlDati.getRCommessa());
    pianoPrlTes.setNumRitorno(wrPianoPrlDati.getNumRitorno());
    pianoPrlTes.setRStabilimento(wrPianoPrlDati.getRStabilimento());
    pianoPrlTes.setRReparto(wrPianoPrlDati.getRReparto());
    pianoPrlTes.setRCentroLavoro(wrPianoPrlDati.getRCentroLavoro());
    pianoPrlTes.setROperazione(wrPianoPrlDati.getROperazione());
    pianoPrlTes.setRAttivita(wrPianoPrlDati.getRAttivita());

    //...Attributi calcolati
    Object[] dataOraInizio = calcolaData(wrPianoPrlDati, false);
    pianoPrlTes.setDataInizio((java.sql.Date)dataOraInizio[0]);
    pianoPrlTes.setOraInizio((java.sql.Time)dataOraInizio[1]);
    Object[] dataFine = calcolaData(wrPianoPrlDati, true);
    pianoPrlTes.setDataFine((java.sql.Date)dataFine[0]);
    pianoPrlTes.setPriorita(calcolaPriorita(pianoPrlTes.getOraInizio()));

    pianoPrlTes.setRCliente(wrPianoPrlDati.getRCliente());
    pianoPrlTes.setRUmPrmMag(wrPianoPrlDati.getOrdUmPrm());
    pianoPrlTes.setQtaResidua(wrPianoPrlDati.getQtaResAtv());

    //...L’attributo StatoDisponibilità di testata Piano viene impostato a ‘Non significativo’
    pianoPrlTes.setStatoDisponAtv(PianoPrelieviTestata.ST_DSP_ATV_NON_SIGNIF);

    //...L’attributo StatoGenLista viene impostato a ‘Da eseguire’ solo se Preselezione
    //...di input = ‘Tutte’ (altrimenti viene impostato a ‘No’)

    
    if(isGesUbicazioneEBarcode()) { //Fix 22443 inizio
    	pianoPrlTes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_ESEGUITO);
    	pianoPrlTes.setGesUbicazioneEBarcode(true);
    }//Fix 22443 fine
    else {
      if(getPreselezione() == ModelloPrelievo.TP_PRSL_TUTTE)
        pianoPrlTes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_DA_ESEG);
      else
        pianoPrlTes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_NO);    	
    }

    //...Dichiarante, Causale e Numeratore/Serie del Documento di Produzione vengono
    //...acquisiti dall’oggetto “Filtro Piano di Prelievo”
    // Fix 22443 inizio
    if(isGesUbicazioneEBarcode()) { 
    	ConfigurazionePianiPrelievo cfgPianiPrl = getConfigGenDocProduzione();
    	if(cfgPianiPrl != null) {
    		pianoPrlTes.setRCauDocPrd(cfgPianiPrl.getIdCauDocProduzione());
    		pianoPrlTes.setRDipendente(cfgPianiPrl.getIdDichiarante());
    		pianoPrlTes.setRNumDocPrd(cfgPianiPrl.getIdNumDocPrd());
    		pianoPrlTes.setRSerieNumDoc(cfgPianiPrl.getIdSerieNumDocPrd());
    	}
    }
    // Fix 22443 fine    
    else if(filtroPianoPrl != null) {
      pianoPrlTes.setRCauDocPrd(filtroPianoPrl.getRCauDocPrd());
      pianoPrlTes.setRDipendente(filtroPianoPrl.getRDipendente());
      pianoPrlTes.setRNumDocPrd(filtroPianoPrl.getRNumDocPrd());
      pianoPrlTes.setRSerieNumDoc(filtroPianoPrl.getRSerieNumDoc());
    }

    return pianoPrlTes;
  }

  /**
   * calcolaData
   * @param wrPianoPrlDati WrapperPianoPrlDati
   * @param isDataFine boolean
   * @return Date
   */
  public Object[] calcolaData(WrapperPianoPrlDati wrPianoPrlDati, boolean isDataFine) {
    Object[] ret = new Object[2];
    java.sql.Date dbStartDate = null;
    java.sql.Date dbEndDate = TimeUtils.getDate(9999, 12, 31);
    ConnectionDescriptor cd = ConnectionManager.getCurrentConnectionDescriptor();
    if(cd != null)
      dbStartDate = cd.getDatabase().getMinimumDate();

    java.sql.Date cmpDate = null;
    if(isDataFine)
      cmpDate = dbEndDate;
    else
      cmpDate = dbStartDate;

    //...Come Data inizio/fine di testata Piano viene assunta la prima significativa fra

    //...Data inizio/fine effettivo Attività
    java.sql.Date dataEff = null;
    if(isDataFine)
      dataEff = wrPianoPrlDati.getDataFineEff();
    else
      dataEff = wrPianoPrlDati.getDataInizioEff();
    if(dataEff != null && dataEff.compareTo(cmpDate) != 0) {
      ret[0] = dataEff;
      ret[1] = wrPianoPrlDati.getOraInizioEff();
      return ret;
    }


    //...Data inizio/fine programmato Attività
    java.sql.Date dataPgm = null;
    if(isDataFine)
      dataPgm = wrPianoPrlDati.getDataFinePgm();
    else
      dataPgm = wrPianoPrlDati.getDataInizioPgm();
    if(dataPgm != null && dataPgm.compareTo(cmpDate) != 0) {
      ret[0] = dataPgm;
      ret[1] = wrPianoPrlDati.getOraInizioPgm();
      return ret;
    }

    //...Data inizio/fine richiesta dell’Ordine
    java.sql.Date dataRcs = null;
    if(isDataFine)
      dataRcs = wrPianoPrlDati.getDataFineRcs();
    else
      dataRcs = wrPianoPrlDati.getDataInizioRcs();
    if(dataRcs != null && dataRcs.compareTo(cmpDate) != 0) {
      ret[0] = dataRcs;
      ret[1] = null;
      return ret;
    }

    return ret;
  }

  /**
   * calcolaPriorita
   * @param oraInizio Time
   * @return String
   */
  public String calcolaPriorita(java.sql.Time oraInizio) {
    String priorita = "";
    if(oraInizio != null) {
      int[] valTime = TimeUtils.getValues(oraInizio);

      //...La Priorità viene composta a partire dall’Orario di inizio nel seguente modo:
      //...la prima lettera rappresenta l’Ora (da ‘A’ a ‘X’)
      int ore = valTime[0];

      switch(ore) {
        case 0: priorita = "A"; break;
        case 1: priorita = "B"; break;
        case 2: priorita = "C"; break;
        case 3: priorita = "D"; break;
        case 4: priorita = "E"; break;
        case 5: priorita = "F"; break;
        case 6: priorita = "G"; break;
        case 7: priorita = "H"; break;
        case 8: priorita = "I"; break;
        case 9: priorita = "J"; break;
        case 10: priorita = "K"; break;
        case 11: priorita = "L"; break;
        case 12: priorita = "M"; break;
        case 13: priorita = "N"; break;
        case 14: priorita = "O"; break;
        case 15: priorita = "P"; break;
        case 16: priorita = "Q"; break;
        case 17: priorita = "R"; break;
        case 18: priorita = "S"; break;
        case 19: priorita = "T"; break;
        case 20: priorita = "U"; break;
        case 21: priorita = "V"; break;
        case 22: priorita = "W"; break;
        case 23: priorita = "X"; break;
      }

      //...la seconda cifra il periodo di 6 minuti in cui finiscono i minuti dell’Orario di inizio
      //...Esempio: l’orario 09:20 da origine alla priorità ‘J3’
      int minuti = valTime[1];
      priorita += minuti / 6;
    }
    //...se orario di inizio non è valorizzato, si assume ‘Z9'
    else
      priorita = "Z9";
    return priorita;
  }

  /**
   * creaPianoPrelieviRiga
   * @param pianoPrlTes PianoPrelieviTestata
   * @param wrPianoPrlDati WrapperPianoPrlDati
   */
  public void creaPianoPrelieviRiga(PianoPrelieviTestata pianoPrlTes, WrapperPianoPrlDati wrPianoPrlDati) {
    if(pianoPrlTes != null) {
      PianoPrelieviRiga pianoPrlRig = (PianoPrelieviRiga)Factory.createObject(PianoPrelieviRiga.class);
      pianoPrlRig.setTestata(pianoPrlTes);
      pianoPrlRig.setIdModelloPrl(pianoPrlTes.getIdModelloPrl());
      pianoPrlRig.setTipoModelloPrl(pianoPrlTes.getTipoModelloPrl());
      pianoPrlRig.setIdRigaMateriale(wrPianoPrlDati.getIdRigaMateriale());
      pianoPrlRig.setIdArticolo(wrPianoPrlDati.getRArticoloMat());
      pianoPrlRig.setIdLotto(wrPianoPrlDati.getIdLotto());
      pianoPrlRig.setRVersione(wrPianoPrlDati.getRVersioneMat());
      pianoPrlRig.setRConfigurazione(wrPianoPrlDati.getRConfigurazioneMat());
      pianoPrlRig.setRCommessa(wrPianoPrlDati.getRCommessaMat());
      pianoPrlRig.setRMagazzino(wrPianoPrlDati.getRMagazzinoMat());
      pianoPrlRig.setDataPrvImpiego(wrPianoPrlDati.getDataPrvImpiego());
      pianoPrlRig.setRUmPrmMag(wrPianoPrlDati.getMatUmPrm());
      pianoPrlRig.setRUmSecMag(wrPianoPrlDati.getMatUmSec());
      //pianoPrlRig.setQtaRichUmPrm(wrPianoPrlDati.getQtaRcsMat());
      pianoPrlRig.setQtaRichUmPrm(wrPianoPrlDati.getQtaRcsPrmLotto());
      pianoPrlRig.setQtaResUmPrm(wrPianoPrlDati.getQtaResPrmLotto());
      pianoPrlRig.setQtaResUmSec(wrPianoPrlDati.getQtaResSecLotto());
      //...L’attributo Quantità da prelevare di riga Piano viene impostato inizialmente
      //...uguale a Quantità residua
      pianoPrlRig.setQtaDaPrlUmPrm(wrPianoPrlDati.getQtaResPrmLotto());
      pianoPrlRig.setQtaDaPrlUmSec(wrPianoPrlDati.getQtaResSecLotto());

      pianoPrlRig.setCoeffImpiego(calcolaCoeffImpiego(wrPianoPrlDati));
      pianoPrlRig.setCoeffTotale(wrPianoPrlDati.getCoeffTotale());
      
      impostaPianoPrelieviRigaDatiPers(pianoPrlRig, pianoPrlTes, wrPianoPrlDati);	//Fix 29772
      
      //72161 SOF3
      if(pianoPrlRig.getQtaDaPrlUmPrm() == null || pianoPrlRig.getQtaDaPrlUmPrm() == BigDecimal.ZERO) {
    	  return;
      }
      //72161 SOF3

      pianoPrlTes.getRighe().add(pianoPrlRig);
    }
  }


  //Fix 29772 - inizio
  protected void impostaPianoPrelieviRigaDatiPers(PianoPrelieviRiga pianoPrlRig, PianoPrelieviTestata pianoPrlTes, WrapperPianoPrlDati wrPianoPrlDati) {

  }
  //Fix 29772 - fine

  
  /**
   * calcolaCoeffImpiego
   * @param wrPianoPrlDati WrapperPianoPrlDati
   * @return BigDecimal
   */
  public BigDecimal calcolaCoeffImpiego(WrapperPianoPrlDati wrPianoPrlDati) {
    BigDecimal coeffImp = null;
    BigDecimal qtaRcsLotto = wrPianoPrlDati.getQtaRcsPrmLotto();
    BigDecimal qtaRcsAtv = wrPianoPrlDati.getQtaRcsAtv();
    //...CoefficienteImpiego viene calcolato come (QtaRichiestaUMPrm di
    //...AttivitaEsecutivaMaterialiLotti/QtaRichiestaUMPrm di AttivitaEsecutiva)
    if(qtaRcsLotto != null && qtaRcsAtv != null) {
      coeffImp = qtaRcsLotto.divide(qtaRcsAtv, 6, BigDecimal.ROUND_HALF_UP);
    }
    coeffImp = coeffImp.setScale(6, BigDecimal.ROUND_HALF_UP);
    return coeffImp;
  }

  /**
   * aggionaStatoPianoSulModello
   * @param statoPiano char
   * @param dataUltimaGen Date
   * @param dataFineOriz Date
   * @param dataUltimaTras Date
   */
  public void aggiornaStatoPianoSulModello(char statoPiano, java.sql.Date dataUltimaGen, java.sql.Date dataFineOriz, java.sql.Date dataUltimaTras) {
    try {
      ConnectionManager.pushConnection(iUpdateModPrlConnDescr);
      ModelloPrelievo modPrl = getModelloPrelievo();
      modPrl.setStatoPianoPrl(statoPiano);
      if(dataUltimaGen != null)
        modPrl.setDataUltimaGenerazione(dataUltimaGen);
      if(dataFineOriz != null)
        modPrl.setDataFineOrizzonte(dataFineOriz);
      if(dataUltimaTras != null)
        modPrl.setDataUltimaTrasmissione(dataUltimaTras);

      //...FIX 8419 inizio
      //...Se il piano è stato correttamente generato imposto
      //...DataUltimaTrasmissione a null
      if(statoPiano == ModelloPrelievo.ST_PN_GENERATO) {
        modPrl.setDataUltimaTrasmissione(null);
      }
      //...FIX 8419 fine

      modPrl.save();
      ConnectionManager.commit();
    }
    catch(SQLException ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if(iUpdateModPrlConnDescr != null)
        ConnectionManager.popConnection(iUpdateModPrlConnDescr);
    }
  }

  /**
   * cancellaPianiPrelievi
   * @return String
   */
  public synchronized int cancellaPianiPrelievi() {
	  //Fix 29772 - inizio
//	  int ret = 0;
//
//	  //Fix 29772 - inizio
//	  try {
//		  //ConnectionManager.pushConnection(iUpdateModPrlConnDescr);
//		  Database db = ConnectionManager.getCurrentDatabase();
//		  PreparedStatement ps = cCancellaPianiPrl.getStatement();
//		  db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
//		  db.setString(ps, 2, getModelloPrelievo().getIdModelloPrelievo());
//		  ret = ps.executeUpdate();
//		  //ConnectionManager.commit();
//	  }
//	  catch(Exception ex) {
//		  ex.printStackTrace(Trace.excStream);
//		  return -1;
//	  }
//	  /*
//    finally {
//      if(iUpdateModPrlConnDescr != null)
//        ConnectionManager.popConnection(iUpdateModPrlConnDescr);
//    }*/
//	  //Fix 29772 - inizio
//
//	  return ret;
	  
	  return cancellaPianiPrelievi(getModelloPrelievo().getIdModelloPrelievo());
	  //Fix 29772 - fine
  }
  
  
  //Fix 29772 - inizio
  public synchronized int cancellaPianiPrelievi(String idPianoPrelievo) {
	    int ret = 0;
	    
	    try {
	      //ConnectionManager.pushConnection(iUpdateModPrlConnDescr);
	      Database db = ConnectionManager.getCurrentDatabase();
	      //37090
	      //PreparedStatement ps = cCancellaPianiPrl.getStatement();
	      PreparedStatement ps = null;
	      if(PersDatiGen.isGestitioIntellimag()) {
		       ps = cCancellaPianiPrlInt.getStatement();
	      }else {
		       ps = cCancellaPianiPrl.getStatement();
	      }
	      //37090
	      db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
	      db.setString(ps, 2, idPianoPrelievo);
	      ret = ps.executeUpdate();
	      //ConnectionManager.commit();
	    }
	    catch(Exception ex) {
	      ex.printStackTrace(Trace.excStream);
	      return -1;
	    }

	    return ret;
	  }
  //Fix 29772 - fine
  

  /**
   * salvaTestataPiano
   * @param testataPiano PianoPrelieviTestata
   * @param eseguiCommit boolean
   */
  public void salvaTestataPiano(PianoPrelieviTestata testataPiano, boolean eseguiCommit) {
    try {
      ConnectionManager.pushConnection(iUpdateModPrlConnDescr);
      int rc = testataPiano.save();
      if(rc > 0) {
        if(eseguiCommit)
          ConnectionManager.commit();
      }
      else {
        String endSalvaPianoTxt = ResourceLoader.getString(RES_FILE, "EndSalvaPianoTxt", new Object[] {testataPiano.getKey(), "" + rc});
        output.println(endSalvaPianoTxt);
      }
    }
    catch(SQLException ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if(iUpdateModPrlConnDescr != null)
        ConnectionManager.popConnection(iUpdateModPrlConnDescr);
    }
  }

  /**
   * trovaListeGenerateDelPiano
   * @return String
   */
  public synchronized ResultSet trovaRighePiani() {
//    try {
//      Database db = ConnectionManager.getCurrentDatabase();
//      PreparedStatement ps = cTrovaRigheCalcDisp.getStatement();
//      db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
//      db.setString(ps, 2, getModelloPrelievo().getIdModelloPrelievo());
//      ResultSet rs = ps.executeQuery();
//      return rs;
//    }
//    catch(Exception ex) {
//      ex.printStackTrace(Trace.excStream);
//      return null;
//    }
	  
	  return trovaRighePiani(getModelloPrelievo().getIdModelloPrelievo());
  }
  
  
  //Fix 29772 - inizio
  public synchronized ResultSet trovaRighePiani(String idModelloPrelievo) {
	  try {
		  Database db = ConnectionManager.getCurrentDatabase();
		  PreparedStatement ps = cTrovaRigheCalcDisp.getStatement();
		  db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
		  db.setString(ps, 2, idModelloPrelievo);
		  ResultSet rs = ps.executeQuery();
		  return rs;
	  }
	  catch(Exception ex) {
		  ex.printStackTrace(Trace.excStream);
		  return null;
	  }
  }
  //Fix 29772 - fine
  

  /**
   * calcoloDisponibilita
   * @return boolean
   */
  public boolean calcoloDisponibilita() {
	  //Fix 29772 - inizio
//    boolean ret = true;
//    ModelloPrelievo modPrl = getModelloPrelievo();
//    //...Se TipoQuantita del modello = (‘Residua con controllo disponibilità’,
//    //...‘Massima producibile’), si deve completare il Piano Prelievi con le
//    //...informazioni circa la disponibilità dei Materiali presenti
//    if(modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_RES_CON_CTRL ||
//      modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {
//      //...si selezionano le righe del Piano Prelievi corrispondenti al Modello corrente,
//      //...ordinandole per Magazzino, Articolo, Versione, Configurazione, Commessa
//      //...(valorizzata solo se l’Articolo gestisce i “saldi per commessa” e la Commessa
//      //...è gestita “a saldi”), Lotto (discendente in modo che i lotti effettivi vengano
//      //...presi in  considerazione prima di quello “dummy”), DataInizio, Priorità
//      ResultSet listaRighePiani = trovaRighePiani();
//      if(listaRighePiani != null) {
//        int numCalc = calcoloDisponibilita(listaRighePiani);
//        String endCalcDispTxt = ResourceLoader.getString(RES_FILE, "EndCalcDispTxt");
//        if(numCalc > 0) {
//          ret = true;
//          output.println(endCalcDispTxt + " " + iEndOkTxt);
//        }
//        else  if(numCalc < 0) {
//          ret = false;
//          output.println(endCalcDispTxt + " " + iEndErrTxt);
//        }
//      }
//    }
//    return ret;
	  
	  return calcoloDisponibilita(getModelloPrelievo());
	  //Fix 29772 - fine
  }
  
  
  //Fix 29772 - inizio
  public boolean calcoloDisponibilita(ModelloPrelievo modPrl) {
	  boolean ret = true;
	  //...Se TipoQuantita del modello = (‘Residua con controllo disponibilità’,
	  //...‘Massima producibile’), si deve completare il Piano Prelievi con le
	  //...informazioni circa la disponibilità dei Materiali presenti
	  if(modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_RES_CON_CTRL ||
			  modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {
		  //...si selezionano le righe del Piano Prelievi corrispondenti al Modello corrente,
		  //...ordinandole per Magazzino, Articolo, Versione, Configurazione, Commessa
		  //...(valorizzata solo se l’Articolo gestisce i “saldi per commessa” e la Commessa
		  //...è gestita “a saldi”), Lotto (discendente in modo che i lotti effettivi vengano
		  //...presi in  considerazione prima di quello “dummy”), DataInizio, Priorità
		  ResultSet listaRighePiani = trovaRighePiani(modPrl.getIdModelloPrelievo());
		  if(listaRighePiani != null) {
			  int numCalc = calcoloDisponibilita(listaRighePiani);
			  String endCalcDispTxt = ResourceLoader.getString(RES_FILE, "EndCalcDispTxt");
			  if(numCalc > 0) {
				  ret = true;
				  output.println(endCalcDispTxt + " " + iEndOkTxt);
			  }
			  else  if(numCalc < 0) {
				  ret = false;
				  output.println(endCalcDispTxt + " " + iEndErrTxt);
			  }
		  }
	  }
	  return ret;
  }
  //Fix 29772 - fine
  

  /**
   * calcoloDisponibilita
   * @param rs ResultSet
   * @return boolean
   */
  public int calcoloDisponibilita(ResultSet rs) {
    try {
      int numCalc = 0;
      String chiaveRottura = "";
      ArrayList righePiano = new ArrayList();
      BigDecimal giacenzaLibera = ZERO;
      while(rs.next()) {
        numCalc++;
        String idAzienda = rs.getString(PianoPrelieviRigaTM.ID_AZIENDA);
        String idMagazzino = rs.getString(PianoPrelieviRigaTM.R_MAGAZZINO);
        String idArticolo = rs.getString(PianoPrelieviRigaTM.ID_ARTICOLO);
        Integer idVersione = new Integer(rs.getInt(PianoPrelieviRigaTM.R_VERSIONE));
        Integer idConfig = new Integer(rs.getInt(PianoPrelieviRigaTM.R_CONFIGURAZIONE));
        String idCommessa = rs.getString(PianoPrelieviRigaTM.R_COMMESSA);

        String chiaveRotturaTmp = KeyHelper.buildObjectKey(new Object[] {idMagazzino, idArticolo, idVersione, idConfig, idCommessa});
        //...a rottura di Magazzino / Articolo / Versione / Configurazione / Commessa
        if(!chiaveRotturaTmp.equals(chiaveRottura)) {
          chiaveRottura = chiaveRotturaTmp;
          if(!righePiano.isEmpty())
            aggiornaDisponRighe(righePiano, giacenzaLibera);
          righePiano.clear();
          giacenzaLibera = ZERO;

          //...o si calcola GiacenzaLibera = (Giacenza + QtaEntrata – QtaUscita –
          //...QtaAccantonata) in UM primaria con il valore degli attributi del
          //...corrispondente oggetto SaldoBase se la Commessa non è significativa
          if(idCommessa == null || idCommessa.equals("")) {
            String chiaveSaldo = KeyHelper.buildObjectKey(new Object[] {idAzienda, idMagazzino, idArticolo, idVersione, idConfig, SaldoMag.OPERAZIONE_DUMMY});
            SaldoMag saldo = SaldoMag.elementWithKey(chiaveSaldo, PersistentObject.NO_LOCK);
            if(saldo != null) {
              BigDecimal giacenzaNetta = saldo.getDatiSaldo().giacenzaNetta().getQuantitaInUMPrm();
              //BigDecimal qtaEntrata = calcolaQuantitaInEntrata(saldo.getDatiSaldo());
              //BigDecimal qtaUscita = calcolaQuantitaInUscita(saldo.getDatiSaldo());
              BigDecimal qtaAccantonata = saldo.getDatiSaldo().getQtaAccantManualUMPrim();
              giacenzaLibera = giacenzaNetta.subtract(qtaAccantonata);
            }

            //...Se l'articolo gestisce i saldi, alla giacenza devo togliere
            //...la giacenza dei saldi di commessa per quell'articolo
            //...Esempio:
            //...Considero i saldi dell'articolo ART1 che gestisce i saldi di commessa
            //...e le commesse CMM1 e CMM2 che gestiscono entrambe i saldi
            //...SALDO(commessa) ART1+CMM1 --> giacenza 20
            //...SALDO(commessa) ART1+CMM2 --> giacenza 15
            //...SALDO(base) ART1 --> giacenza 50 (comprende anche quelle di commessa)
            //...Se faccio un ordine prelevando in qta 30 l'articolo ART1 con commessa
            //...CMM1 la giacenza è 20, quindi avrò disponibilità parziale.
            //...Analogamente se faccio un ordine prelevando in qta 30 l'articolo ART1
            //...con commessa CMM2 la giacenza è 15, quindi avrò disponibilità parziale.
            //...Se faccio un ordine prelevando in qta 30 l'articolo ART1 senza commessa
            //...interrogando solo il saldo base otterrei una giacenza di 50 e quindi
            //...avrei apparentemente una disponiblità completa, ma in realtà di quei
            //...50 in giacenza del saldo base 20 sono riservati alla commessa CMM1
            //...e 15 alla commessa CMM2. Il saldo reale di ART1 sarebbe quindi
            //...50 - 20 - 15 = 15, e quindi avrei ancora disponibilità parziale.
            //...Quindi poichè ART1 gestisce i saldi per commessa
            //...per sapere la sua giacenza reale devo considerare la giacenza del
            //...saldo base MENO la somma delle giacenze dei saldi di commessa per
            //...ART1 + versione + configurazione
            String artGesSaldi = rs.getString("ART_GES_SALDI");
            if(artGesSaldi != null && artGesSaldi.equals("Y")) {
              BigDecimal giacenzaRisCmm = ZERO;
              String where = SaldoMagLottoCommessaTM.ID_AZIENDA + " = '" + idAzienda + "'" +
                " AND " + SaldoMagLottoCommessaTM.ID_MAGAZZINO + " = '" + idMagazzino + "'" +
                " AND " + SaldoMagLottoCommessaTM.ID_ARTICOLO + " = '" + idArticolo + "'" +
                " AND " + SaldoMagLottoCommessaTM.ID_VERSIONE + " = " + idVersione +
                " AND " + SaldoMagLottoCommessaTM.ID_CONFIG + " = " + idConfig;
              Vector saldiCmm = SaldoMagLottoCommessa.retrieveList(where, "", false);
              for(int i = 0; i < saldiCmm.size(); i++) {
                SaldoMagLottoCommessa saldoCmm = (SaldoMagLottoCommessa)saldiCmm.get(i);
                BigDecimal giacenzaNetta = saldoCmm.getDatiSaldo().giacenzaNetta().getQuantitaInUMPrm();
                BigDecimal qtaAccantonata = saldoCmm.getDatiSaldo().getQtaAccantManualUMPrim();
                BigDecimal giacenzaLiberaCmm = giacenzaNetta.subtract(qtaAccantonata);
                giacenzaRisCmm = giacenzaRisCmm.add(giacenzaLiberaCmm);
              }

              giacenzaLibera = giacenzaLibera.subtract(giacenzaRisCmm);
            }
          }

          //...oppure dalla sommatoria di tali attributi degli oggetti SaldoCommessa
          //...relativi alla chiave di rottura
          else {
            String where = SaldoMagLottoCommessaTM.ID_AZIENDA + " = '" + idAzienda + "'" +
              " AND " + SaldoMagLottoCommessaTM.ID_MAGAZZINO + " = '" + idMagazzino + "'" +
              " AND " + SaldoMagLottoCommessaTM.ID_ARTICOLO + " = '" + idArticolo + "'" +
              " AND " + SaldoMagLottoCommessaTM.ID_VERSIONE + " = " + idVersione +
              " AND " + SaldoMagLottoCommessaTM.ID_CONFIG + " = " + idConfig +
              " AND " + SaldoMagLottoCommessaTM.ID_COMMESSA + " = '" + idCommessa + "'";
            Vector saldiCmm = SaldoMagLottoCommessa.retrieveList(where, "", false);
            for(int i = 0; i < saldiCmm.size(); i++) {
              SaldoMagLottoCommessa saldoCmm = (SaldoMagLottoCommessa)saldiCmm.get(i);
              BigDecimal giacenzaNetta = saldoCmm.getDatiSaldo().giacenzaNetta().getQuantitaInUMPrm();
              //BigDecimal qtaEntrata = calcolaQuantitaInEntrata(saldoCmm.getDatiSaldo());
              //BigDecimal qtaUscita = calcolaQuantitaInUscita(saldoCmm.getDatiSaldo());
              BigDecimal qtaAccantonata = saldoCmm.getDatiSaldo().getQtaAccantManualUMPrim();
              BigDecimal giacenzaLiberaCmm = giacenzaNetta.subtract(qtaAccantonata);
              giacenzaLibera = giacenzaLibera.add(giacenzaLiberaCmm);
            }
          }
          if(cStampaLogCalcoli)
            System.out.println("Giacenza libera " + idMagazzino.trim() + "/" + idArticolo.trim() + "/" + idVersione + "/" + idConfig + "/" + idCommessa + " --> " + giacenzaLibera);
        }

        String idModello = rs.getString(PianoPrelieviRigaTM.ID_MODELLO_PRL);
        char tipoModello = rs.getString(PianoPrelieviRigaTM.TIPO_MODELLO_PRL).charAt(0);
        String annoOrd = rs.getString(PianoPrelieviRigaTM.ID_ANNO_ORD);
        String numOrd = rs.getString(PianoPrelieviRigaTM.ID_NUMERO_ORD);
        Integer idRigaAtv = new Integer(rs.getInt(PianoPrelieviRigaTM.ID_RIGA_ATTIVITA));
        Integer idRigaMat = new Integer(rs.getInt(PianoPrelieviRigaTM.ID_RIGA_MATERIALE));
        String idLotto = rs.getString(PianoPrelieviRigaTM.ID_LOTTO);

        PianoPrelieviRiga riga = (PianoPrelieviRiga)Factory.createObject(PianoPrelieviRiga.class);
        riga.setIdAzienda(idAzienda);
        riga.setIdModelloPrl(idModello);
        riga.setTipoModelloPrl(tipoModello);
        riga.setIdAnnoOrd(annoOrd);
        riga.setIdNumeroOrd(numOrd);
        riga.setIdRigaAttivita(idRigaAtv);
        riga.setIdRigaMateriale(idRigaMat);
        riga.setIdArticolo(idArticolo);
        riga.setIdLotto(idLotto);
        boolean ret = riga.retrieve(PersistentObject.NO_LOCK);
        if(ret) {
          righePiano.add(riga);
        }

      }
      aggiornaDisponRighe(righePiano, giacenzaLibera);
      rs.close();
      return numCalc;

    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return -1;
    }
  }

  public BigDecimal calcolaQuantitaInEntrata(DatiSaldo datiSaldo) {
    BigDecimal result = ZERO;
    BigDecimal proposta = datiSaldo.getQtaPropostaEntUMPrim() == null ? ZERO : datiSaldo.getQtaPropostaEntUMPrim();
    BigDecimal transito = datiSaldo.getQtaTransitoEntUMPrim() == null ? ZERO : datiSaldo.getQtaTransitoEntUMPrim();
    BigDecimal attesa = datiSaldo.getQtaAttesaEntUMPrim() == null ? ZERO : datiSaldo.getQtaAttesaEntUMPrim();
    result = proposta.add(transito).add(attesa);
    return result;
  }

  public BigDecimal calcolaQuantitaInUscita(DatiSaldo datiSaldo) {
    BigDecimal result = ZERO;
    BigDecimal proposta = datiSaldo.getQtaPropostaUscitaUMPrim() == null ? ZERO : datiSaldo.getQtaPropostaUscitaUMPrim();
    BigDecimal transito = datiSaldo.getQtaTransitoUscitaUMPrim() == null ? ZERO : datiSaldo.getQtaTransitoUscitaUMPrim();
    BigDecimal attesa = datiSaldo.getQtaAttesaUscitaUMPrim() == null ? ZERO : datiSaldo.getQtaAttesaUscitaUMPrim();
    result = proposta.add(transito).add(attesa);
    return result;
  }

  public void aggiornaDisponRighe(List righePiano, BigDecimal giacenzaLibera) {
    try {
      ConnectionManager.pushConnection(iUpdateModPrlConnDescr);

      //...si scorrono le righe della precedente selezione relative alla chiave di
      //...rottura, si aggiorna ogni riga valorizzando
      Iterator iter = righePiano.iterator();
      while(iter.hasNext()) {
        PianoPrelieviRiga riga = (PianoPrelieviRiga)iter.next();
        BigDecimal qtaResidua = riga.getQtaResUmPrm();
        //...StatoDisponibilità = ‘Completa’ e Quantità disponibile = Quantità residua,
        //...se Quantità residua <= GiacenzaLibera
        if(qtaResidua.compareTo(giacenzaLibera) <= 0) {
          riga.setStatoDisponMat(PianoPrelieviRiga.ST_DSP_MAT_COMPLETA);
          riga.setQtaDispUmPrm(qtaResidua);
        }
        //...StatoDisponibilità = ‘Parziale’ e Quantità disponibile = GiacenzaLibera,
        //...se Quantità residua > GiacenzaLibera e GiacenzaLibera > 0
        else
          if(qtaResidua.compareTo(giacenzaLibera) > 0 && giacenzaLibera.compareTo(ZERO) > 0) {
            riga.setStatoDisponMat(PianoPrelieviRiga.ST_DSP_MAT_PARZIALE);
            riga.setQtaDispUmPrm(giacenzaLibera);
          }
          //...StatoDisponibilità = ‘Assente’ e Quantità disponibile = 0,
          //...se GiacenzaLibera <= 0
          else
            if(giacenzaLibera.compareTo(ZERO) <= 0) {
              riga.setStatoDisponMat(PianoPrelieviRiga.ST_DSP_MAT_ASSENTE);
              riga.setQtaDispUmPrm(ZERO);
            }
        //...e poi si sottrae Quantità disponibile da GiacenzaLibera
        giacenzaLibera = giacenzaLibera.subtract(riga.getQtaDispUmPrm());
        riga.save();
      }
      //...si effettua commit
      ConnectionManager.commit();
    }
    catch(SQLException ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if(iUpdateModPrlConnDescr != null)
        ConnectionManager.popConnection(iUpdateModPrlConnDescr);
    }

  }

  /**
   * trovaRigheQtaMaxPrd
   * @return String
   */
  public synchronized ResultSet trovaRigheQtaMaxPrd() {
	  //Fix 29772 - inizio
//    try {
//      Database db = ConnectionManager.getCurrentDatabase();
//      PreparedStatement ps = cTrovaRigheCalcMaxQtaPrd.getStatement();
//      db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
//      db.setString(ps, 2, getModelloPrelievo().getIdModelloPrelievo());
//      ResultSet rs = ps.executeQuery();
//      return rs;
//    }
//    catch(Exception ex) {
//      ex.printStackTrace(Trace.excStream);
//      return null;
//    }
	  
	  return trovaRigheQtaMaxPrd(getModelloPrelievo().getIdModelloPrelievo());
	  //Fix 29772 - fine
  }


  //Fix 29772 - inizio
  public synchronized ResultSet trovaRigheQtaMaxPrd(String idModelloPrelievo) {
	    try {
	      Database db = ConnectionManager.getCurrentDatabase();
	      PreparedStatement ps = cTrovaRigheCalcMaxQtaPrd.getStatement();
	      db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
	      db.setString(ps, 2, idModelloPrelievo);
	      ResultSet rs = ps.executeQuery();
	      return rs;
	    }
	    catch(Exception ex) {
	      ex.printStackTrace(Trace.excStream);
	      return null;
	    }
	  }
  //Fix 29772 - fine
  
  
  /**
   * calcoloQtaMaxProducibile
   * @return boolean
   */
  public boolean calcoloQtaMaxProducibile() {
	  //Fix 29772 - inizio
//    boolean ret = true;
//    ModelloPrelievo modPrl = getModelloPrelievo();
//    //...Se TipoQuantita del modello = (‘Residua con controllo disponibilità’,
//    //...‘Massima producibile’), si deve completare il Piano Prelievi con le
//    //...informazioni circa la disponibilità dei Materiali presenti
//    if(modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_RES_CON_CTRL ||
//      modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {
//      //...si riscorrono le righe del Piano Prelievi corrispondenti al Modello corrente,
//      //...ordinandole per Modello, Anno/Numero Ordine, Riga Attività Esecutiva
//      ResultSet listaRighePiani = trovaRigheQtaMaxPrd();
//      if(listaRighePiani != null) {
//        int count = calcoloQtaMaxProducibile(listaRighePiani);
//        String endCalcQMaxTxt = ResourceLoader.getString(RES_FILE, "EndCalcQMaxTxt");
//        if(count > 0) {
//          ret = true;
//          output.println(endCalcQMaxTxt + " " + iEndOkTxt);
//        }
//        else if(count < 0) {
//          ret = false;
//          output.println(endCalcQMaxTxt + " " + iEndErrTxt);
//        }
//      }
//    }
//    return ret;
	  
	  //Fix 34411 -- Inizio
	  if(getModelloPrelievo().getOrigine() == ModelloPrelievo.SERVIZI) {
		return calcoloQtaMaxProducibileSrv(getModelloPrelievo());  
	  } else {
	  //Fix 34411 -- Fine	  
	    return calcoloQtaMaxProducibile(getModelloPrelievo());
	  }//Fix 34411
	  //Fix 29772 - fine
  }

  
  //Fix 29772 - inizio
  public boolean calcoloQtaMaxProducibile(ModelloPrelievo modPrl) {
	  boolean ret = true;
	  //...Se TipoQuantita del modello = (‘Residua con controllo disponibilità’,
	  //...‘Massima producibile’), si deve completare il Piano Prelievi con le
	  //...informazioni circa la disponibilità dei Materiali presenti
	  if(modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_RES_CON_CTRL ||
			  modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {
		  //...si riscorrono le righe del Piano Prelievi corrispondenti al Modello corrente,
		  //...ordinandole per Modello, Anno/Numero Ordine, Riga Attività Esecutiva
		  ResultSet listaRighePiani = trovaRigheQtaMaxPrd(modPrl.getIdModelloPrelievo());
		  if(listaRighePiani != null) {
			  int count = calcoloQtaMaxProducibile(listaRighePiani);
			  String endCalcQMaxTxt = ResourceLoader.getString(RES_FILE, "EndCalcQMaxTxt");
			  if(count > 0) {
				  ret = true;
				  output.println(endCalcQMaxTxt + " " + iEndOkTxt);
			  }
			  else if(count < 0) {
				  ret = false;
				  output.println(endCalcQMaxTxt + " " + iEndErrTxt);
			  }
		  }
	  }
	  return ret;
  }
  //Fix 29772 - fine
  

  /**
   * calcoloQtaMaxProducibile
   * @param rs ResultSet
   * @return boolean
   */
  public int calcoloQtaMaxProducibile(ResultSet rs) {
    try {

      String chiaveRottura = "";
      BigDecimal qtaTeorProducibile = null;
      BigDecimal qtaTeorProdotta = null;
      PianoPrelieviTestata tes = null;
      int i = 0;
      while(rs.next()) {

        String idAzienda = rs.getString(PianoPrelieviRigaTM.ID_AZIENDA);
        String idModello = rs.getString(PianoPrelieviRigaTM.ID_MODELLO_PRL);
        char tipoModello = rs.getString(PianoPrelieviRigaTM.TIPO_MODELLO_PRL).charAt(0);
        String annoOrd = rs.getString(PianoPrelieviRigaTM.ID_ANNO_ORD);
        String numOrd = rs.getString(PianoPrelieviRigaTM.ID_NUMERO_ORD);
        Integer idRigaAtv = new Integer(rs.getInt(PianoPrelieviRigaTM.ID_RIGA_ATTIVITA));

        String chiaveRotturaTmp = KeyHelper.buildObjectKey(new Object[] {annoOrd, numOrd, idRigaAtv});

        //...a rottura di Modello / Anno/Numero Ordine / Riga Attività Esecutiva
        if(!chiaveRotturaTmp.equals(chiaveRottura)) {
          chiaveRottura = chiaveRotturaTmp;
          if(tes != null) {
            //...FIX 8419
            //if(getModelloPrelievo().getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {
              boolean eseguiCommit = false;
              if(i % cNumCommit == 0)
                eseguiCommit = true;
              if(cStampaLogCalcoli) {
                System.out.println("QUANTITA MAX PRODUCIBILE (Min. qtà teor. prodotta) --> " + qtaTeorProdotta);
                System.out.println("QUANTITA MIN PRODUCIBILE (Min. qtà teor. producibile) --> " + qtaTeorProducibile);
              }
              //...o si calcolano la  Quantità massima producibile e la Quantità minima
              //...producibile a livello Testata del Piano con il valore minore rispettivamente
              //...di Quantità teorica producibile e di Quantità teorica prodotta delle
              //...relative righe aventi CoefficienteTotale = ‘No’
              aggiornaDisponTestata(tes, qtaTeorProducibile, qtaTeorProdotta, eseguiCommit);
            //}
            qtaTeorProducibile = null;
            qtaTeorProdotta = null;
          }

          tes = (PianoPrelieviTestata)Factory.createObject(PianoPrelieviTestata.class);
          tes.setIdAzienda(idAzienda);
          tes.setIdModelloPrl(idModello);
          tes.setTipoModelloPrl(tipoModello);
          tes.setIdAnnoOrd(annoOrd);
          tes.setIdNumeroOrd(numOrd);
          tes.setIdRigaAttivita(idRigaAtv);
          boolean ret = tes.retrieve(PersistentObject.NO_LOCK);
          if(ret) {
            List righe = tes.getRighe();

            if(cStampaLogCalcoli) {
              System.out.println("");
              System.out.println("ATTIVITA' -----> " + tes.getAttivitaEsecutiva().getKey() + " (" + tes.getDataInizio() + " / " + tes.getPriorita() + ")");
            }
            for(int j = 0; j < righe.size(); j++) {
              PianoPrelieviRiga riga = (PianoPrelieviRiga)righe.get(j);

              if(cStampaLogCalcoli) {
                if(j == 0) {
                  System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------");
                  System.out.println("Articolo            | V | Config.        | Lotto          | Commessa     | CT | Coeff.Imp. | QtaRich.    | QtaResidua  | QtaDispon.  | TeorProdot. | TeorProduc. ");
                }
              }
              //...per ogni riga del Piano avente CoefficienteTotale = ‘No’, si determinano
              if(!riga.isCoeffTotale()) {
                //...FIX 8419 inizio
                BigDecimal qtaRichiesta = riga.getQtaRichUmPrm() != null ? riga.getQtaRichUmPrm() : ZERO;
                BigDecimal qtaResidua = riga.getQtaResUmPrm() != null ? riga.getQtaResUmPrm() : ZERO;
                BigDecimal qtaDisponibile = riga.getQtaDispUmPrm() != null ? riga.getQtaDispUmPrm() : ZERO;
                //...FIX 8419 fine
                BigDecimal coeffImpiego = riga.getCoeffImpiego();
                BigDecimal dividendo = qtaRichiesta.subtract(qtaResidua).add(qtaDisponibile);
                //BigDecimal qtaTeorProducibileTmp = dividendo.divide(coeffImpiego, 6, BigDecimal.ROUND_HALF_UP);//Fix 30965
                BigDecimal qtaTeorProducibileTmp = Q6Calc.get().divide(dividendo, coeffImpiego, 6, BigDecimal.ROUND_HALF_UP);//Fix 30965
                //Fix 13560 inizio
                ArticoloDatiMagaz articoloDatiMagaz = riga.getTestata().getArticolo().getArticoloDatiMagaz();
                if((articoloDatiMagaz.isQtaIntera())||(articoloDatiMagaz.getUMPrmMag() != null && articoloDatiMagaz.getUMPrmMag().getQtaIntera()))
                {
                 //qtaTeorProducibileTmp = qtaTeorProducibileTmp.setScale(0,BigDecimal.ROUND_DOWN);//Fix 30965
                  qtaTeorProducibileTmp = Q6Calc.get().setScale(qtaTeorProducibileTmp, 0,BigDecimal.ROUND_DOWN);//Fix 30965
                }//Fix 13560 fine
                //...la Quantità teorica producibile=[(Quantità richiesta–Quantità residua
                //... +Quantità disponibile)/CoefficienteImpiego];
                if(qtaTeorProducibile == null || qtaTeorProducibile.compareTo(qtaTeorProducibileTmp) > 0)
                  qtaTeorProducibile = qtaTeorProducibileTmp;

                dividendo = qtaRichiesta.subtract(qtaResidua);
                //BigDecimal qtaTeorProdottaTmp = dividendo.divide(coeffImpiego, 6, BigDecimal.ROUND_HALF_UP);//Fix 30965
                BigDecimal qtaTeorProdottaTmp = Q6Calc.get().divide(dividendo, coeffImpiego, 6, BigDecimal.ROUND_HALF_UP);//Fix 30965

                //...la Quantità teorica prodotta=[(Quantità richiesta–Quantità residua)/CoefficienteImpiego]
                if(qtaTeorProdotta == null || qtaTeorProdotta.compareTo(qtaTeorProdottaTmp) > 0)
                  qtaTeorProdotta = qtaTeorProdottaTmp;

                if(cStampaLogCalcoli) {
                  System.out.print(riga.getIdArticolo().length() > 20 ? riga.getIdArticolo().substring(0, 21) : aggiungiSpazi(riga.getIdArticolo(), 20));
                  System.out.print("| " + aggiungiSpazi(riga.getRVersione().toString(), 2));
                  System.out.print("| " + (riga.getIdEsternoConfig() != null && riga.getIdEsternoConfig().length() > 15 ? riga.getIdEsternoConfig().substring(0, 16) : aggiungiSpazi(riga.getIdEsternoConfig(), 15)));
                  System.out.print("| " + (riga.getIdLotto().length() > 15 ? riga.getIdLotto().substring(0, 16) : aggiungiSpazi(riga.getIdLotto(), 15)));
                  String cmm = commessaPerSaldi(riga);
                  System.out.print("| " + (cmm.length() > 13 ? cmm.substring(0, 14) : aggiungiSpazi(cmm, 13)));
                  System.out.print("| " + (riga.isCoeffTotale() ? "Si " : "No "));
                  System.out.print("| " + aggiungiSpazi(coeffImpiego.toString(), 11));
                  System.out.print("| " + aggiungiSpazi(qtaRichiesta.toString(), 12));
                  System.out.print("| " + aggiungiSpazi(qtaResidua.toString(), 12));
                  System.out.print("| " + aggiungiSpazi(qtaDisponibile.toString(), 12));
                  System.out.print("| " + aggiungiSpazi(qtaTeorProdottaTmp.toString(), 12));
                  System.out.println("| " + qtaTeorProducibileTmp.toString());
                }

              }
              else {

                if(cStampaLogCalcoli) {
                  System.out.print(riga.getIdArticolo().length() > 20 ? riga.getIdArticolo().substring(0, 21) : aggiungiSpazi(riga.getIdArticolo(), 20));
                  System.out.print("| " + aggiungiSpazi(riga.getRVersione().toString(), 2));
                  System.out.print("| " + (riga.getIdEsternoConfig() != null && riga.getIdEsternoConfig().length() > 15 ? riga.getIdEsternoConfig().substring(0, 16) : aggiungiSpazi(riga.getIdEsternoConfig(), 15)));
                  System.out.print("| " + (riga.getIdLotto().length() > 15 ? riga.getIdLotto().substring(0, 16) : aggiungiSpazi(riga.getIdLotto(), 15)));
                  String cmm = commessaPerSaldi(riga);
                  System.out.print("| " + (cmm.length() > 13 ? cmm.substring(0, 14) : aggiungiSpazi(cmm, 13)));
                  System.out.print("| " + (riga.isCoeffTotale() ? "Si " : "No "));
                  System.out.print("| " + aggiungiSpazi(riga.getCoeffImpiego().toString(), 11));
                  System.out.print("| " + aggiungiSpazi(riga.getQtaRichUmPrm().toString(), 12));
                  System.out.print("| " + aggiungiSpazi(riga.getQtaResUmPrm().toString(), 12));
                  System.out.print("| " + aggiungiSpazi(riga.getQtaDispUmPrm().toString(), 12));
                  System.out.print("| " + aggiungiSpazi("--", 12));
                  System.out.println("| --");
                }
              }

            }
            if(cStampaLogCalcoli)
              System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------");
          }
        }
        i++;
      }

      if(tes != null) {
        //...FIX 8419
        //if(getModelloPrelievo().getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {
          boolean eseguiCommit = false;
          if(i % cNumCommit == 0)
            eseguiCommit = true;
          //...o si calcolano la  Quantità massima producibile e la Quantità minima
          //...producibile a livello Testata del Piano con il valore minore rispettivamente
          //...di Quantità teorica producibile e di Quantità teorica prodotta delle
          //...relative righe aventi CoefficienteTotale = ‘No’

          if(cStampaLogCalcoli) {
            System.out.println("QUANTITA MAX PRODUCIBILE (Min. qtà teor. prodotta) --> " + qtaTeorProdotta);
            System.out.println("QUANTITA MIN PRODUCIBILE (Min. qtà teor. producibile) --> " + qtaTeorProducibile);
          }
          aggiornaDisponTestata(tes, qtaTeorProducibile, qtaTeorProdotta, eseguiCommit);
        //}
      }

      rs.close();
      return i;

    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return -1;
    }
  }

  /**
   * aggiornaDisponTestata
   * @param tes PianoPrelieviTestata
   * @param qtaMaxProducibile BigDecimal
   * @param qtaMinProducibile BigDecimal
   * @param eseguiCommit boolean
   */
  public void aggiornaDisponTestata(PianoPrelieviTestata tes, BigDecimal qtaMaxProducibile, BigDecimal qtaMinProducibile, boolean eseguiCommit) {
    try {
      if(eseguiCommit)
        ConnectionManager.pushConnection(iUpdateModPrlConnDescr);

      boolean areAllDispAssente = true;
      boolean areAllDispCompleta = true;
      //...se TipoQuantita del Modello = ‘Massima producibile’, si riaggiornano le righe
      //...relative impostando Quantità da prelevare =
      List righe = tes.getRighe();
      for(int j = 0; j < righe.size(); j++) {
        PianoPrelieviRiga riga = (PianoPrelieviRiga)righe.get(j);

        //...FIX 8419
        if(getModelloPrelievo().getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {

          if(cStampaLogCalcoli) {
            if(j == 0) {
              System.out.println("---------------------------------------------------------------------------------------------------------------");
              System.out.println("Articolo            | V | Config.        | Lotto          | Commessa     | QtaDaPrel. UMPrm  | QtaDaPrel. UMSec");
            }
          }

          //...[(Quantità massima producibile di Testata * CoefficienteImpiego) -
          //...(Quantità richiesta – Quantità residua)] se CoefficienteTotale = ‘No’
          //...(se il risultato è negativo, si impone 0)
          if(!riga.isCoeffTotale()) {
            BigDecimal coeffImpiego = riga.getCoeffImpiego();
            BigDecimal qtaRichiesta = riga.getQtaRichUmPrm();
            BigDecimal qtaResidua = riga.getQtaResUmPrm();
            BigDecimal qtaDaPrel = (qtaMaxProducibile.multiply(coeffImpiego)).subtract(qtaRichiesta.subtract(qtaResidua));
            if(qtaDaPrel.compareTo(ZERO) < 0)
              qtaDaPrel = ZERO;
            //qtaDaPrel = qtaDaPrel.setScale(qtaRichiesta.scale(), BigDecimal.ROUND_HALF_UP);//Fix 30965
            //qtaDaPrel = Q6Calc.get().setScale(qtaDaPrel, qtaRichiesta.scale(), BigDecimal.ROUND_HALF_UP);//Fix 30965 //Fix 39406
            qtaDaPrel = Q6Calc.get().setScale(qtaDaPrel, 2, BigDecimal.ROUND_HALF_UP);//Fix 30965 //Fix 39406
            riga.setQtaDaPrlUmPrm(controllaGestioneQta(riga, qtaDaPrel));

            //...La Quantità da prelevare in UMSecMag viene calcolata solo se l'UMSec
            //...è valorizzata, altrimenti viene impostata a zero
            riga.setQtaDaPrlUmSec(convertiQtaInUmSec(riga));

            if(cStampaLogCalcoli) {
              System.out.print(riga.getIdArticolo().length() > 20 ? riga.getIdArticolo().substring(0, 21) : aggiungiSpazi(riga.getIdArticolo(), 20));
              System.out.print("| " + aggiungiSpazi(riga.getRVersione().toString(), 2));
              System.out.print("| " + (riga.getIdEsternoConfig() != null && riga.getIdEsternoConfig().length() > 15 ? riga.getIdEsternoConfig().substring(0, 16) : aggiungiSpazi(riga.getIdEsternoConfig(), 15)));
              System.out.print("| " + (riga.getIdLotto().length() > 15 ? riga.getIdLotto().substring(0, 16) : aggiungiSpazi(riga.getIdLotto(), 15)));
              String cmm = commessaPerSaldi(riga);
              System.out.print("| " + (cmm.length() > 13 ? cmm.substring(0, 14) : aggiungiSpazi(cmm, 13)));
              System.out.print("| " + aggiungiSpazi(riga.getQtaDaPrlUmPrm().toString(), 18));
              System.out.println("| " + riga.getQtaDaPrlUmSec());
            }

          }
          //...Quantità disponibile se CoefficienteTotale = ‘Si’
          else {
            riga.setQtaDaPrlUmPrm(riga.getQtaDispUmPrm());
            riga.setQtaDaPrlUmSec(convertiQtaInUmSec(riga));

            if(cStampaLogCalcoli) {
              System.out.print(riga.getIdArticolo().length() > 20 ? riga.getIdArticolo().substring(0, 21) : aggiungiSpazi(riga.getIdArticolo(), 20));
              System.out.print("| " + aggiungiSpazi(riga.getRVersione().toString(), 2));
              System.out.print("| " + (riga.getIdEsternoConfig() != null && riga.getIdEsternoConfig().length() > 15 ? riga.getIdEsternoConfig().substring(0, 16) : aggiungiSpazi(riga.getIdEsternoConfig(), 15)));
              System.out.print("| " + (riga.getIdLotto().length() > 15 ? riga.getIdLotto().substring(0, 16) : aggiungiSpazi(riga.getIdLotto(), 15)));
              String cmm = commessaPerSaldi(riga);
              System.out.print("| " + (cmm.length() > 13 ? cmm.substring(0, 14) : aggiungiSpazi(cmm, 13)));
              System.out.print("| " + aggiungiSpazi(riga.getQtaDaPrlUmPrm().toString(), 18));
              System.out.println("| " + riga.getQtaDaPrlUmSec());
            }

          }
        }

        if(riga.getStatoDisponMat() != PianoPrelieviRiga.ST_DSP_MAT_ASSENTE)
          areAllDispAssente = false;

        if(riga.getStatoDisponMat() != PianoPrelieviRiga.ST_DSP_MAT_COMPLETA)
          areAllDispCompleta = false;

      }
      if(cStampaLogCalcoli)
        System.out.println("---------------------------------------------------------------------------------------------------------------");

      //...si aggiorna la Testata del Piano impostando:
      //...Quantità massima producibile e Quantità minima producibile con i valori determinati
      if (qtaMaxProducibile == null) //Fix 45425
         qtaMaxProducibile = ZERO; //Fix 45425
      if (qtaMinProducibile == null) //Fix 47321
         qtaMinProducibile = ZERO; //Fix 47321
      tes.setQtaMaxProd(qtaMaxProducibile);
      tes.setQtaMinProd(qtaMinProducibile);

      //...StatoDisponibilità = ‘Completamente assente’ se tutte le righe hanno
      //...StatoDisponibilità = ‘Assente’, altrimenti ‘Completa’ se tutte le righe
      //...hanno StatoDisponibilità = ‘Completa’, altrimenti ‘Parzialmente assente’
      //...se Quantità massima producibile = Quantità minima producibile, altrimenti
      //...‘Parziale’
      char statoDispAtv = PianoPrelieviTestata.ST_DSP_ATV_PARZIALE;
      if(areAllDispAssente)
        statoDispAtv = PianoPrelieviTestata.ST_DSP_ATV_COMPL_ASSENTE;
      else
        if(areAllDispCompleta)
          statoDispAtv = PianoPrelieviTestata.ST_DSP_ATV_COMPLETA;
        else
          if(qtaMaxProducibile.compareTo(qtaMinProducibile) == 0)
            statoDispAtv = PianoPrelieviTestata.ST_DSP_ATV_PAZIAL_ASSENTE;
      tes.setStatoDisponAtv(statoDispAtv);

      //...FIX 8419 inizio
      if(isAggiornaStatoGenLista()) {
        /*
               //...StatoGenLista = ‘Da eseguire’ solo se lo StatoDisponibilità è diverso
               //...da ‘Completamente assente’ ed è compatibile con Preselezione di input
               //...(altrimenti viene impostato a ‘No’)
               if(statoDispAtv != PianoPrelieviTestata.ST_DSP_ATV_COMPL_ASSENTE) {
          if(getPreselezione() == ModelloPrelievo.TP_PRSL_TUTTE)
            tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_DA_ESEG);
          else
            if(getPreselezione() == ModelloPrelievo.TP_PRSL_DISP_COMPLETA) {
              if(statoDispAtv >= PianoPrelieviTestata.ST_DSP_ATV_COMPLETA)
                tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_DA_ESEG);
          }
          else
            if(getPreselezione() == ModelloPrelievo.TP_PRSL_DA_DISP_PARZIALE) {
              if(statoDispAtv >= PianoPrelieviTestata.ST_DSP_ATV_PARZIALE)
                tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_DA_ESEG);
          }
          else
            if(getPreselezione() == ModelloPrelievo.TP_PRSL_DA_DISP_PARZ_ASS) {
              if(statoDispAtv >= PianoPrelieviTestata.ST_DSP_ATV_PAZIAL_ASSENTE)
                tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_DA_ESEG);
          }
               }
               else
          tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_NO);
         */

        //...StatoGenLista viene impostato come di seguito specificato:
        boolean isStatoCompatibile = true;
        if(getPreselezione() == ModelloPrelievo.TP_PRSL_NESSUNA) {
          isStatoCompatibile = false;
        }
        if(getPreselezione() == ModelloPrelievo.TP_PRSL_DISP_COMPLETA) {
          if(statoDispAtv < PianoPrelieviTestata.ST_DSP_ATV_COMPLETA)
            isStatoCompatibile = false;
        }
        else if(getPreselezione() == ModelloPrelievo.TP_PRSL_DA_DISP_PARZIALE) {
          if(statoDispAtv < PianoPrelieviTestata.ST_DSP_ATV_PARZIALE)
            isStatoCompatibile = false;
        }
        else if(getPreselezione() == ModelloPrelievo.TP_PRSL_DA_DISP_PARZ_ASS) {
          if(statoDispAtv < PianoPrelieviTestata.ST_DSP_ATV_PAZIAL_ASSENTE)
            isStatoCompatibile = false;
        }

        //...se StatoDisponibilita non è compatibile con Preselezione di input,
        //...si imposta ‘No’;
        if(!isStatoCompatibile)
          tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_NO);

        //...se StatoDisponibilita è compatibile con Preselezione di input:
        //...se StatoDisponibilita è diverso da (‘Completamente assente’, ‘Parzialmente assente’),
        //...si imposta ‘Da eseguire’, altrimenti, si imposta a ‘Chiuso’
        else {
          if(statoDispAtv != PianoPrelieviTestata.ST_DSP_ATV_COMPL_ASSENTE &&
            statoDispAtv != PianoPrelieviTestata.ST_DSP_ATV_PAZIAL_ASSENTE)
            tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_DA_ESEG);
          else
            tes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_CHIUSO);
        }
      }
      //...FIX 8419 fine

      int rc = tes.save();
      if(rc > 0) {
        if(eseguiCommit)
          ConnectionManager.commit();
      }
      else {
        String endSalvaPianoTxt = ResourceLoader.getString(RES_FILE, "EndSalvaPianoTxt", new Object[] {tes.getKey(), "" + rc});
        output.println(endSalvaPianoTxt);
      }
    }
    catch(SQLException ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if(eseguiCommit)
        if(iUpdateModPrlConnDescr != null)
          ConnectionManager.popConnection(iUpdateModPrlConnDescr);
    }

  }

  /**
   * convertiQtaInUmSec
   * @param riga PianoPrelieviRiga
   * @return BigDecimal
   */
  protected BigDecimal convertiQtaInUmSec(PianoPrelieviRiga riga) {
    //...La Quantità da prelevare in UMSecMag viene calcolata a partire dal
    //...corrispondente valore in UMPrmMag moltiplicandolo per il rapporto
    //...(Quantità residua di riga in UMSecMag / Quantità residua di riga in UMPrmMag)
    //...FIX 8419 inizio
    //BigDecimal qtaInUmSec = null;
    BigDecimal qtaInUmSec = ZERO;
    //...FIX 8419 fine
    if(riga.getUmSecMag() != null) {
      BigDecimal qtaInUmPrm = riga.getQtaDaPrlUmPrm();
      BigDecimal qtaResInUmPrm = riga.getQtaResUmPrm();
      BigDecimal qtaResInUmSec = riga.getQtaResUmSec();
      //BigDecimal rapporto = qtaResInUmSec.divide(qtaResInUmPrm, 6, BigDecimal.ROUND_HALF_UP);//Fix 30965
      BigDecimal rapporto = Q6Calc.get().divide(qtaResInUmSec, qtaResInUmPrm, 6, BigDecimal.ROUND_HALF_UP);//Fix 30965
      qtaInUmSec = qtaInUmPrm.multiply(rapporto);
      //qtaInUmSec = qtaInUmSec.setScale(qtaInUmPrm.scale(), BigDecimal.ROUND_HALF_UP);//Fix 30965
      //qtaInUmSec = Q6Calc.get().setScale(qtaInUmSec, qtaInUmPrm.scale(), BigDecimal.ROUND_HALF_UP);//Fix 30965 //Fix 39402
      qtaInUmSec = Q6Calc.get().setScale(qtaInUmSec, 2, BigDecimal.ROUND_HALF_UP);//Fix 39402
    }
    return qtaInUmSec;
  }

  /**
   * controllaGestioneQta
   * @param riga PianoPrelieviRiga
   * @param qtaDaPrlUmPrm BigDecimal
   * @return BigDecimal
   */
  protected BigDecimal controllaGestioneQta(PianoPrelieviRiga riga, BigDecimal qtaDaPrlUmPrm) {
    //int oldScale = qtaDaPrlUmPrm.scale();//Fix 39402
	  int oldScale = Q6Calc.get().scale(2);//Fix 39402
    if(riga.getArticolo().isQtaIntera() || riga.getUmPrmMag().getQtaIntera()) {
      //qtaDaPrlUmPrm = qtaDaPrlUmPrm.setScale(0, BigDecimal.ROUND_DOWN);//Fix 30965
      qtaDaPrlUmPrm = Q6Calc.get().setScale(qtaDaPrlUmPrm, 0, BigDecimal.ROUND_DOWN);//Fix 30965
    }
    //qtaDaPrlUmPrm = qtaDaPrlUmPrm.setScale(oldScale);//Fix 30965
    qtaDaPrlUmPrm = Q6Calc.get().setScale(qtaDaPrlUmPrm, oldScale);//Fix 30965
    return qtaDaPrlUmPrm;
  }

  /**
   * commessaPerSaldi
   * @param riga PianoPrelieviRiga
   * @return String
   */
  public String commessaPerSaldi(PianoPrelieviRiga riga) {
    Articolo articolo = riga.getArticolo();
    Commessa commessa = riga.getCommessa();
    if((articolo != null && articolo.isGesSaldiCommessa()) &&
      (commessa != null && commessa.getAggiornamentoSaldi())) {
      return commessa.getIdCommessa();
    }
    return "-";
  }

  /**
   * aggiungiSpazi
   * @param testo String
   * @param lunghezza int
   * @return String
   */
  public String aggiungiSpazi(String testo, int lunghezza) {
    if(testo == null || testo.equals(""))
      testo = "-";
    for(int i = testo.length(); i < lunghezza; i++) {
      testo += " ";
    }
    return testo;
  }

  /**
   * generazioneListePrlDaPiano
   * @param riportaDataGen boolean
   * @return boolean
   */
  public boolean generazioneListePrlDaPiano(boolean riportaDataGen) {
	  //Fix 29772 - inizio
//    if(isGesUbicazioneEBarcode()) //Fix 22443
//    	return generazioneListePrlUBDaPiano(); // Fix 22443
//    
//    boolean ret = true;
//    int countListeNoGen = 0;//liste non salvate
//    int countListeError = 0;//liste in errore
//    int countTestate = 0;//testate salvate
//    ModelloPrelievo modelloPrelievo = getModelloPrelievo();
//    if (modelloPrelievo.getStatoPianoPrl() == ModelloPrelievo.ST_PN_GENERATO ||
//        modelloPrelievo.getStatoPianoPrl() == ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO) {
//      try {
//        Vector pianiPrlDaEseguire = new Vector();
//        String where = PianoPrelieviTestataTM.ID_AZIENDA + " = '" + modelloPrelievo.getIdAzienda() + "' AND " +
//                       PianoPrelieviTestataTM.ID_MODELLO_PRL + " = '" + modelloPrelievo.getIdModelloPrelievo() + "' AND (" +
//                       PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_DA_ESEG + "' OR " +
//                       PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_NO + "') AND " +
//                       PianoPrelieviTestataTM.STATO + " = 'V'";
//        String order = PianoPrelieviTestataTM.DATA_INIZIO + " ASC, " + PianoPrelieviTestataTM.PRIORITA + " ASC";
//        Vector pianiPrl = PianoPrelieviTestata.retrieveList(where, order, true);
//        if (pianiPrl.size() > 0) {
//          for (int k = 0; k < pianiPrl.size(); k++) {
//            PianoPrelieviTestata ppT = (PianoPrelieviTestata)pianiPrl.elementAt(k);
//            if (ppT.getStatoGenLista() == PianoPrelieviTestata.ST_GEN_LST_DA_ESEG)
//              pianiPrlDaEseguire.add(ppT);
//          }
//          if (pianiPrlDaEseguire.size() > 0) {
//            //Fix 08419 MM perché impostarlo subito?
//            //aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO, null, null, null);
//            Iterator i = pianiPrlDaEseguire.iterator();
//            while (i.hasNext() && ret) {
//              PianoPrelieviTestata pianoPrlTestata = (PianoPrelieviTestata)i.next();
//              GenerazioneListaPrelievo genListaPrl = (GenerazioneListaPrelievo)Factory.createObject(GenerazioneListaPrelievo.class);
//              Vector errori = new Vector();
//              if (genListaPrl.generaListaPrelievo(pianoPrlTestata, riportaDataGen, errori)) {
//                pianoPrlTestata.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_ESEGUITO);
//                if (pianoPrlTestata.save() > 0)
//                  countTestate++;
//                else
//                  ret = false;
//                if (countTestate % cNumCommit == 0 && ret) {
//                  ConnectionManager.commit();
//                  countTestate = 0;
//                }
//              } else {
//                countListeError++;
//                if (pianoPrlTestata.getRCodLista() != null) {
//                  String chiaveTL = KeyHelper.buildObjectKey(new String[]{pianoPrlTestata.getRCodSocieta(), pianoPrlTestata.getRCodLista()});
//                  TestataLista tes = TestataLista.elementWithKey(chiaveTL, PersistentObject.NO_LOCK);
//                  if (tes != null) {
//                    pianoPrlTestata.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_ESEGUITO);
//                    if (pianoPrlTestata.save() > 0)
//                      countTestate++;
//                    else
//                      ret = false;
//                    if (countTestate % cNumCommit == 0 && ret) {
//                      ConnectionManager.commit();
//                      countTestate = 0;
//                    }
//                  } else
//                    countListeNoGen++;
//                }
//                //Fix 8419 - inizio
//                else
//                  countListeNoGen++;
//                //Fix 8419 - fine
//                String erroreGenerico = "* " + ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
//                output.println(erroreGenerico);
//                String erroreTestataPiano = "Piano Rif. " + formattaChiaveTestataLista(pianoPrlTestata);
//                for (int e = 0; e < errori.size(); e++) {
//                  ErrorMessage em = (ErrorMessage)errori.get(e);
//                  output.println(erroreTestataPiano + ": " + em.getLongText());
//                }
//              }
//            }
//            if (ret) {
//              ConnectionManager.commit();
////Fix 08419 MM inizio
////              if (pianiPrl.size() > pianiPrlDaEseguire.size())
////                aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO, null, null, TimeUtils.getCurrentDate());
////              else
////                aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_TRASMESSO, null, null, TimeUtils.getCurrentDate());
//
//              if (pianiPrlDaEseguire.size() > countListeNoGen) {//solo se almeno una lista è stata trasmessa...
//                //se non ci ci sono più liste da trasmettere... Stato TRASMESSO
//                if (trovaListeNonGenerateDelPiano(modelloPrelievo.getIdAzienda(), modelloPrelievo.getIdModelloPrelievo()) == 0)
//                  aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_TRASMESSO, null, null, TimeUtils.getCurrentDate());
//                else //altrimenti... Stato PARZIALEMENTE_TRASMESSO
//                  aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO, null, null, TimeUtils.getCurrentDate());
//              }
////Fix 08419 MM fine
//            }
//          }
//        }
//        if (countListeNoGen > 0) {
//          output.println(" ");
//          output.println(ResourceLoader.getString(RES_FILE, "WarningListeNoGen"));
//        }
//      }
////09095 MM inizio
//      catch(ThipException ex) {
//        String erroreGenerico = ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
//        output.println(erroreGenerico);
//        //09095 MM inizio
//        if (ex.getErrors() != null)
//          for (Iterator i = ex.getErrors().iterator(); i.hasNext();)
//            output.println(((ErrorMessage)i.next()).getText());
//        else
//          output.println(ex.getErrorMessage());
//        //09095 MM fine
//        ex.printStackTrace(Trace.excStream);
//        return false;
//      }
////09095 MM fine
//      catch(Exception ex) {
//        String erroreGenerico = ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
//        output.println(erroreGenerico);
//        ex.printStackTrace(Trace.excStream);
//        return false;
//      }
//      //28132 inizio
//      finally {
//        try {
//          ConnectionManager.rollback();
//        }
//        catch (SQLException e) {
//        }
//      }
//      //28132 fine      
//    } else {
//      String statoModelloErratoTxt = ResourceLoader.getString(RES_FILE, "StatoModelloErrato");
//      output.println(statoModelloErratoTxt);
//      return false;
//    }
//
//    if (countListeError > 0)
//      return false;
//
//    return ret;
	  
	  return generazioneListePrlDaPiano(riportaDataGen, getModelloPrelievo());
	  //Fix 29772 - fine
  }


  //Fix 29772 - inizio
  public boolean generazioneListePrlDaPiano(boolean riportaDataGen, ModelloPrelievo modelloPrelievo) {
	  if(isGesUbicazioneEBarcode()) //Fix 22443
		  return generazioneListePrlUBDaPiano(); // Fix 22443

	  boolean ret = true;
	  int countListeNoGen = 0;//liste non salvate
	  int countListeError = 0;//liste in errore
	  int countTestate = 0;//testate salvate
	  if (getModelloPrelievo().getStatoPianoPrl() == ModelloPrelievo.ST_PN_GENERATO ||								//TODO verificare se corretto mantenere il modello originario
			  getModelloPrelievo().getStatoPianoPrl() == ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO ||			//TODO verificare se corretto mantenere il modello originario
			  isModelloPrelievoDaVerificarePerGenListe(modelloPrelievo)) {			
		  try {
			  Vector pianiPrlDaEseguire = new Vector();
			  String where = PianoPrelieviTestataTM.ID_AZIENDA + " = '" + modelloPrelievo.getIdAzienda() + "' AND " +
					  PianoPrelieviTestataTM.ID_MODELLO_PRL + " = '" + modelloPrelievo.getIdModelloPrelievo() + "' AND (" +
					  PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_DA_ESEG + "' OR " +
					  PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_NO + "') AND " +
					  PianoPrelieviTestataTM.STATO + " = 'V'" + 
					  getWhereGenerazioneListePrlDaPianoPers();		//Fix 44367
			  String order = PianoPrelieviTestataTM.DATA_INIZIO + " ASC, " + PianoPrelieviTestataTM.PRIORITA + " ASC";
			  Vector pianiPrl = PianoPrelieviTestata.retrieveList(where, order, true);
			  if (pianiPrl.size() > 0) {
				  for (int k = 0; k < pianiPrl.size(); k++) {
					  PianoPrelieviTestata ppT = (PianoPrelieviTestata)pianiPrl.elementAt(k);
					  if (ppT.getStatoGenLista() == PianoPrelieviTestata.ST_GEN_LST_DA_ESEG)
						  pianiPrlDaEseguire.add(ppT);
				  }
				  if (pianiPrlDaEseguire.size() > 0) {
					  //Fix 08419 MM perché impostarlo subito?
					  //aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO, null, null, null);
					  Iterator i = pianiPrlDaEseguire.iterator();
					  while (i.hasNext() && ret) {
						  PianoPrelieviTestata pianoPrlTestata = (PianoPrelieviTestata)i.next();
						  GenerazioneListaPrelievo genListaPrl = (GenerazioneListaPrelievo)Factory.createObject(GenerazioneListaPrelievo.class);
						  Vector errori = new Vector();
						  if (genListaPrl.generaListaPrelievo(pianoPrlTestata, riportaDataGen, errori)) {
							  pianoPrlTestata.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_ESEGUITO);
							  pianoPrlTestata.setStatoIntellimag(PianoPrelieviTestata.TRASMESSO);//38083
							  if (pianoPrlTestata.save() > 0)
								  countTestate++;
							  else
								  ret = false;
							  if (countTestate % cNumCommit == 0 && ret) {
								  ConnectionManager.commit();
								  countTestate = 0;
							  }
						  } else {
							  countListeError++;
							  if (pianoPrlTestata.getRCodLista() != null) {
								  String chiaveTL = KeyHelper.buildObjectKey(new String[]{pianoPrlTestata.getRCodSocieta(), pianoPrlTestata.getRCodLista()});
								  TestataLista tes = TestataLista.elementWithKey(chiaveTL, PersistentObject.NO_LOCK);
								  if (tes != null) {
									  pianoPrlTestata.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_ESEGUITO);
									  if (pianoPrlTestata.save() > 0)
										  countTestate++;
									  else
										  ret = false;
									  if (countTestate % cNumCommit == 0 && ret) {
										  ConnectionManager.commit();
										  countTestate = 0;
									  }
								  } else
									  countListeNoGen++;
							  }
							  //Fix 8419 - inizio
							  else
								  countListeNoGen++;
							  //Fix 8419 - fine
							  String erroreGenerico = "* " + ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
							  output.println(erroreGenerico);
							  iBatchJob.setApplStatus(BatchJob.WITH_WARNING); //Fix 47561
							  String erroreTestataPiano = "Piano Rif. " + formattaChiaveTestataLista(pianoPrlTestata);
							  for (int e = 0; e < errori.size(); e++) {
								  ErrorMessage em = (ErrorMessage)errori.get(e);
								  output.println(erroreTestataPiano + ": " + em.getLongText());
							  }
						  }
					  }
					  if (ret) {
						  ConnectionManager.commit();
						  //Fix 08419 MM inizio
						  //	              if (pianiPrl.size() > pianiPrlDaEseguire.size())
						  //	                aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO, null, null, TimeUtils.getCurrentDate());
						  //	              else
						  //	                aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_TRASMESSO, null, null, TimeUtils.getCurrentDate());

						  if (pianiPrlDaEseguire.size() > countListeNoGen) {//solo se almeno una lista è stata trasmessa...
							  //se non ci ci sono più liste da trasmettere... Stato TRASMESSO
							  if (trovaListeNonGenerateDelPiano(getModelloPrelievo().getIdAzienda(), getModelloPrelievo().getIdModelloPrelievo()) == 0)			//TODO verificare se corretto mantenere il modello originario
								  aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_TRASMESSO, null, null, TimeUtils.getCurrentDate());
							  else //altrimenti... Stato PARZIALEMENTE_TRASMESSO
								  aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO, null, null, TimeUtils.getCurrentDate());
						  }
						  //Fix 08419 MM fine
					  }
				  }
			  }
			  if (countListeNoGen > 0) {
				  output.println(" ");
				  output.println(ResourceLoader.getString(RES_FILE, "WarningListeNoGen"));
			  }
		  }
		  //09095 MM inizio
		  catch(ThipException ex) {
			  String erroreGenerico = ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
			  output.println(erroreGenerico);
			  //09095 MM inizio
			  if (ex.getErrors() != null)
				  for (Iterator i = ex.getErrors().iterator(); i.hasNext();)
					  output.println(((ErrorMessage)i.next()).getText());
			  else
				  output.println(ex.getErrorMessage());
			  //09095 MM fine
			  ex.printStackTrace(Trace.excStream);
			  return false;
		  }
		  //09095 MM fine
		  catch(Exception ex) {
			  String erroreGenerico = ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
			  output.println(erroreGenerico);
			  ex.printStackTrace(Trace.excStream);
			  return false;
		  }
		  //28132 inizio
		  finally {
			  try {
				  ConnectionManager.rollback();
			  }
			  catch (SQLException e) {
			  }
		  }
		  //28132 fine      
	  } else {
		  String statoModelloErratoTxt = ResourceLoader.getString(RES_FILE, "StatoModelloErrato");
		  output.println(statoModelloErratoTxt);
		  return false;
	  }

	  if (countListeError > 0)
		  return false;

	  return ret;
  }
  
  
  protected boolean isModelloPrelievoDaVerificarePerGenListe(ModelloPrelievo modelloPrelievo) {
	  return true;
  }
  //Fix 29772 - fine
  
  
  public String formattaChiaveTestataLista(PianoPrelieviTestata ppt) {
    String chiave = "";
    String idAzienda = ppt.getIdAzienda();
    String idModelloPrl = ppt.getIdModelloPrl();
    Character tipoModelloPrl = new Character(ppt.getTipoModelloPrl());
    String idAnnoOrd = ppt.getIdAnnoOrd();
    String idNumeroOrd = ppt.getIdNumeroOrd();
    Integer idRigaAttivita = ppt.getIdRigaAttivita();
    chiave = idAzienda + "/" + idModelloPrl + "/" + tipoModelloPrl.toString() + "/" +
             idAnnoOrd + "/" + idNumeroOrd + "/" + idRigaAttivita;
    return chiave;
  }

  /**
   * forzaChiusuraListePrelievo
   * @return String
   */
  public synchronized boolean forzaChiusuraListePrelievo() {
	  //Fix 29772 - inizio
//    boolean ret = true;
//    try {
//      String where = PianoPrelieviTestataTM.ID_AZIENDA + " = '" + getModelloPrelievo().getIdAzienda() + "' AND " +
//                     PianoPrelieviTestataTM.ID_MODELLO_PRL + " = '" + getModelloPrelievo().getIdModelloPrelievo() + "' AND " +
//                     PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_ESEGUITO + "'";
//      Vector pianiPrl = PianoPrelieviTestata.retrieveList(where, "", true);
//      for (int i = 0; i < pianiPrl.size(); i++) {
//        PianoPrelieviTestata pianoTes = (PianoPrelieviTestata)pianiPrl.get(i);
//        String whereTS = TestataListaTM.CODICE_SOCIETA + " ='" + pianoTes.getRCodSocieta() + "' AND " +
//                         TestataListaTM.CODICE + " ='" + pianoTes.getRCodLista() + "' AND " +
////                         TestataListaTM.STATO_LISTA + " ='" + TestataLista.APERTO + "'";//Fix 17440 inizio
//                         TestataListaTM.STATO_LISTA + " IN ('" + TestataLista.APERTO + "','" + TestataLista.NUOVO + "')";//Fix 17440 fine
//        Vector elencoTL = TestataLista.retrieveList(whereTS, "", true);
//        if (elencoTL.size() == 1) {
//          TestataLista tl = (TestataLista)elencoTL.get(0);
//          Vector errori = tl.forzaChiudiNoDB();
//          //...FIX 8419 inizio
//          if (errori.size() > 0) {
//            String erroreGenerico = "* " + ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
//            output.println(erroreGenerico);
//            String erroreTestataPiano = "Piano Rif. " + formattaChiaveTestataLista(pianoTes);
//            for(int e = 0; e < errori.size(); e++) {
//              ErrorMessage em = (ErrorMessage)errori.get(e);
//              output.println(erroreTestataPiano + ": " + em.getLongText());
//            }
//            ret = false;
//          }
//          //...FIX 8419 fine
//        }
//      }
//    } catch (Exception ex) {
//      ex.printStackTrace(Trace.excStream);
//      return false;
//    }
//    return ret;
    
	  //return forzaChiusuraListePrelievo(getModelloPrelievo().getIdAzienda()); //Fix 30366
	  return forzaChiusuraListePrelievo(getModelloPrelievo().getIdModelloPrelievo()); //Fix 30366
	  //Fix 29772 - fine
  }
  
  
  //Fix 29772 - inizio
  public synchronized boolean forzaChiusuraListePrelievo(String idModelloPrelievo) {
	  boolean ret = true;
	  try {
		  String where = PianoPrelieviTestataTM.ID_AZIENDA + " = '" + getModelloPrelievo().getIdAzienda() + "' AND " +
				  PianoPrelieviTestataTM.ID_MODELLO_PRL + " = '" + idModelloPrelievo + "' AND " +
				  PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_ESEGUITO + "'" +
				  getWhereChiusuraListePrelievoPers();		//Fix 44367
		  Vector pianiPrl = PianoPrelieviTestata.retrieveList(where, "", true);
		  for (int i = 0; i < pianiPrl.size(); i++) {
			  PianoPrelieviTestata pianoTes = (PianoPrelieviTestata)pianiPrl.get(i);
			  String whereTS = TestataListaTM.CODICE_SOCIETA + " ='" + pianoTes.getRCodSocieta() + "' AND " +
					  TestataListaTM.CODICE + " ='" + pianoTes.getRCodLista() + "' AND " +
					  //	                         TestataListaTM.STATO_LISTA + " ='" + TestataLista.APERTO + "'";//Fix 17440 inizio
					  TestataListaTM.STATO_LISTA + " IN ('" + TestataLista.APERTO + "','" + TestataLista.NUOVO + "')";//Fix 17440 fine
			  Vector elencoTL = TestataLista.retrieveList(whereTS, "", true);
			  if (elencoTL.size() == 1) {
				  TestataLista tl = (TestataLista)elencoTL.get(0);
				  Vector errori = tl.forzaChiudiNoDB();
				  //...FIX 8419 inizio
				  if (errori.size() > 0) {
					  String erroreGenerico = "* " + ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
					  output.println(erroreGenerico);
					  iBatchJob.setApplStatus(BatchJob.WITH_WARNING); //Fix 47561
					  String erroreTestataPiano = "Piano Rif. " + formattaChiaveTestataLista(pianoTes);
					  for(int e = 0; e < errori.size(); e++) {
						  ErrorMessage em = (ErrorMessage)errori.get(e);
						  output.println(erroreTestataPiano + ": " + em.getLongText());
					  }
					  ret = false;
				  }
				  //...FIX 8419 fine
			  }
		  }
	  } catch (Exception ex) {
		  ex.printStackTrace(Trace.excStream);
		  return false;
	  }
	  return ret;
  }
  //Fix 29772 - fine
  
  
  // Fix 22443 inizio
  protected final String TROVA_LISTE_PIANI_DAESGUIRE_UB = 
    "SELECT DISTINCT TES.* FROM " + PianoPrelieviTestataTM.TABLE_NAME + " TES " + 
  
    " JOIN " + PianoPrelieviRigaTM.TABLE_NAME + " RIG ON " + 
        "TES." + PianoPrelieviTestataTM.ID_AZIENDA + " = " + "RIG." + PianoPrelieviRigaTM.ID_AZIENDA + " AND " + 
        "TES." + PianoPrelieviTestataTM.ID_MODELLO_PRL + " = " + "RIG." + PianoPrelieviRigaTM.ID_MODELLO_PRL + " AND " + 
        "TES." + PianoPrelieviTestataTM.TIPO_MODELLO_PRL + " = " + "RIG." + PianoPrelieviRigaTM.TIPO_MODELLO_PRL + " AND " + 
        "TES." + PianoPrelieviTestataTM.ID_ANNO_ORD + " = " + "RIG." + PianoPrelieviRigaTM.ID_ANNO_ORD + " AND " + 
        "TES." + PianoPrelieviTestataTM.ID_NUMERO_ORD + " = " + "RIG." + PianoPrelieviRigaTM.ID_NUMERO_ORD + " AND " + 
        "TES." + PianoPrelieviTestataTM.ID_RIGA_ATTIVITA + " = " + "RIG." + PianoPrelieviRigaTM.ID_RIGA_ATTIVITA + 

    " JOIN " + ArticoloTM.TABLE_NAME + " ART ON " +
        "RIG." + PianoPrelieviRigaTM.ID_AZIENDA + " = " + "ART." + ArticoloTM.ID_AZIENDA + " AND " + 
        "RIG." + PianoPrelieviRigaTM.ID_ARTICOLO + " = " + "ART." + ArticoloTM.ID_ARTICOLO + 
        
    " JOIN " + MagazzinoTM.TABLE_NAME + " MAG ON " +
        "RIG." + PianoPrelieviRigaTM.ID_AZIENDA + " = " + "MAG." + MagazzinoTM.ID_AZIENDA + " AND " + 
        "RIG." + PianoPrelieviRigaTM.R_MAGAZZINO + " = " + "MAG." + MagazzinoTM.ID_MAGAZZINO + 
        
    " WHERE " + "TES." + PianoPrelieviTestataTM.ID_AZIENDA + " = ? AND " + 
    "TES." + PianoPrelieviTestataTM.ID_MODELLO_PRL + " = ?  AND " + 
    "ART." + ArticoloDatiMagazTM.LOGISTICA_LIGHT + " = '" + Column.TRUE_CHAR + "'  AND " +     
    "MAG." + MagazzinoTM.LOGISTICA_LIGHT + " = '" + Column.TRUE_CHAR + "'  AND " + 
    "TES." + PianoPrelieviTestataTM.STATO + " = '" + DatiComuniEstesi.VALIDO + "'" +  
    " ORDER BY " + "TES." + PianoPrelieviTestataTM.DATA_INIZIO + " ASC, " + "TES." + PianoPrelieviTestataTM.PRIORITA + " ASC";
                
  protected CachedStatement cListePianiUBDaEseguire = new CachedStatement(TROVA_LISTE_PIANI_DAESGUIRE_UB);

  protected synchronized List trovaListePianiUBDaEseguire(String idAzienda, String idModello) {
  	List pianiKeys = new ArrayList();
    ResultSet rs = null;
    try {
      Database db = ConnectionManager.getCurrentDatabase();
      PreparedStatement ps = cListePianiUBDaEseguire.getStatement();
      db.setString(ps, 1, idAzienda);
      db.setString(ps, 2, idModello);
      rs = ps.executeQuery();
  		while(rs.next()) {

  			String idAziendaRS = rs.getString(PianoPrelieviTestataTM.ID_AZIENDA);
  			String idModelloRS = rs.getString(PianoPrelieviTestataTM.ID_MODELLO_PRL);  			
  			String tpModelloRS = rs.getString(PianoPrelieviTestataTM.TIPO_MODELLO_PRL);  			
  			String annoOrdRS = rs.getString(PianoPrelieviTestataTM.ID_ANNO_ORD);  			
  			String numOrdRS = rs.getString(PianoPrelieviTestataTM.ID_NUMERO_ORD);  			
  			String idRigaATVRS = rs.getString(PianoPrelieviTestataTM.ID_RIGA_ATTIVITA);  			

  			String chaivePiano = KeyHelper.buildObjectKey(new String[] {idAziendaRS, idModelloRS, tpModelloRS, annoOrdRS, numOrdRS, idRigaATVRS});  			
  			/*PianoPrelieviTestata pianoTestata = getPianoPrelieviTestata(chaivePiano);
  			if(pianoTestata != null)*/
  			pianiKeys.add(chaivePiano);
  		}
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if (rs != null) {
        try {
          rs.close();
        }
        catch (SQLException  e) {
          e.printStackTrace(Trace.excStream);
        }
      }
    }
    return pianiKeys;
  }

  
	public PianoPrelieviTestata getPianoPrelieviTestata(String chiave) {
		PianoPrelieviTestata pianoTes = null;
		try {
			pianoTes = (PianoPrelieviTestata) PianoPrelieviTestata.elementWithKey(PianoPrelieviTestata.class, chiave, PersistentObject.NO_LOCK);
		}
		catch (SQLException e) {
			e.printStackTrace(Trace.excStream);
		}
		return pianoTes;
	}
  
  
  /**
   * generazioneListePrlDaPiano
   * @param riportaDataGen boolean
   * @return boolean
   */
  public boolean generazioneListePrlUBDaPiano() {
    boolean ret = true;
    int countListeNoGen = 0;//liste non salvate
    int countTestateSalvate = 0;//testate salvate
    ModelloPrelievo modelloPrelievo = getModelloPrelievo();
    
    if (modelloPrelievo.getStatoPianoPrl() != ModelloPrelievo.ST_PN_GENERATO &&
        modelloPrelievo.getStatoPianoPrl() != ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO) {
    	String statoModelloErratoTxt = ResourceLoader.getString(RES_FILE, "StatoModelloErrato");
    	output.println(statoModelloErratoTxt);
    	return false;
    }

    if (!modelloPrelievo.isGesUbicazioneEBarcode()) {
    	String statoModelloErratoTxt = ResourceLoader.getString(RES_FILE, "StatoModelloErrato"); // to do
    	output.println(statoModelloErratoTxt);
    	return false;
    }
    
    try {
    	List pianiPrlDaEseguire = trovaListePianiUBDaEseguire(modelloPrelievo.getIdAzienda(), modelloPrelievo.getIdModelloPrelievo());
    	if (pianiPrlDaEseguire.size() == 0) {
    		output.println(" ");
    		output.println(ResourceLoader.getString(RES_FILE, "WarningListeNoGen"));
    		return false;
    	}
        
    	Iterator i = pianiPrlDaEseguire.iterator();
    	PianoPrelieviTestata pianoPrlTestata = null;
    	while (i.hasNext() && ret) {
    		pianoPrlTestata = (PianoPrelieviTestata) getPianoPrelieviTestata((String)i.next());
    		GenerazioneListaPrelievoUB genListaPrl = (GenerazioneListaPrelievoUB)Factory.createObject(GenerazioneListaPrelievoUB.class);
    		Vector errori = new Vector();
    		genListaPrl.listErrori.clear(); //Fix 42413
    		if (genListaPrl.generaListaPrelievoUB(pianoPrlTestata)) {
    			if (pianoPrlTestata.save() > 0)
    				countTestateSalvate++;
    			else
    				ret = false;
    			if (countTestateSalvate % cNumCommit == 0 && ret) {
    				ConnectionManager.commit();
    				countTestateSalvate = 0;
    			}
    		} 
    		else {
    			countListeNoGen++;
    			String erroreGenerico = "* " + ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
    			output.println(erroreGenerico);
    			iBatchJob.setApplStatus(BatchJob.WITH_WARNING); //Fix 47561
    			//Fix 42413 -- Inizio
    			if(!genListaPrl.listErrori.isEmpty()) {
    				for (int e = 0; e < genListaPrl.listErrori.size(); e++) {
        				String textError = (String)genListaPrl.listErrori.get(e);
        				output.println(textError);
        			}
    			}
    			//Fix 42413 -- Fine
    			String erroreTestataPiano = "Piano Rif. " + formattaChiaveTestataLista(pianoPrlTestata);
    			for (int e = 0; e < errori.size(); e++) {
    				ErrorMessage em = (ErrorMessage)errori.get(e);
    				output.println(erroreTestataPiano + ": " + em.getLongText());
    			}
    		}
    	}

    	ConnectionManager.commit();
    	if (pianiPrlDaEseguire.size() > countListeNoGen) {//solo se almeno una lista è stata trasmessa...
    		
    		//se non ci ci sono più liste da trasmettere... Stato TRASMESSO
    		//if (trovaListeNonGenerateDelPiano(modelloPrelievo.getIdAzienda(), modelloPrelievo.getIdModelloPrelievo()) == 0)
    		if (countListeNoGen == 0)    		
    			aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_TRASMESSO, null, null, TimeUtils.getCurrentDate());
    		else //altrimenti... Stato PARZIALEMENTE_TRASMESSO
    			aggiornaStatoPianoSulModello(ModelloPrelievo.ST_PN_PARZIALMENTE_TRASMESSO, null, null, TimeUtils.getCurrentDate());
    	}
    }
    catch(Exception ex) {
    	String erroreGenerico = ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
    	output.println(erroreGenerico);
    	ex.printStackTrace(Trace.excStream);
    	return false;
    }
    return ret;
  }  
  
  /**
   * chiusuraPianoPrecedente
   * @return boolean
   */
  public boolean chiusuraPianoPrecedenteUB() {
    boolean ret = true;
    try {
      //ConnectionManager.pushConnection(iUpdateModPrlConnDescr); //38139
    	ConnectionManager.pushConnection();    	//38139

      boolean okChiusuraListe = forzaChiusuraListePrelievoUB();
      String endChiusuraListeTxt = ResourceLoader.getString(RES_FILE, "EndChiusuraListe");
      if(okChiusuraListe) {
        ret = true;
        output.println(endChiusuraListeTxt + " " + iEndOkTxt);
      }
      else {
        ret = false;
        output.println(endChiusuraListeTxt + " " + iEndErrTxt);
      }
      if(ret) {
        int numPiani = cancellaPianiPrelievi();

        String endChiusuraTxt = ResourceLoader.getString(RES_FILE, "EndChiusuraTxt");
        if(numPiani > 0) {
          ret = true;
          output.println(endChiusuraTxt + " " + iEndOkTxt);
        }
        else if(numPiani < 0) {
          ret = false;
          output.println(endChiusuraTxt + " " + iEndErrTxt);
        }
      }

      if (ret) {
        ConnectionManager.commit();
      }
      else
        ConnectionManager.rollback();
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return false;
    }
    finally {
      //38139 inizio
      //if (iUpdateModPrlConnDescr != null)
        //ConnectionManager.popConnection(iUpdateModPrlConnDescr);
    	ConnectionManager.popConnection();
      //38139 fine
    }
    return ret;
  }  
  
  /**
   * forzaChiusuraListePrelievo
   * @return String
   */
  public synchronized boolean forzaChiusuraListePrelievoUB() {
    boolean ret = true;
    try {
      String where = PianoPrelieviTestataTM.ID_AZIENDA + " = '" + getModelloPrelievo().getIdAzienda() + "' AND " +
                     PianoPrelieviTestataTM.ID_MODELLO_PRL + " = '" + getModelloPrelievo().getIdModelloPrelievo() + "'" + /* AND " +
                     PianoPrelieviTestataTM.STATO_GEN_LISTA + " = '" + PianoPrelieviTestata.ST_GEN_LST_ESEGUITO + "'";*/
   				  	 getWhereChiusuraListePrelievoUBPers();		//Fix 45079
                     
      Vector pianiPrl = PianoPrelieviTestata.retrieveList(where, "", true);
      for (int i = 0; i < pianiPrl.size(); i++) {
      	PianoPrelieviTestata pianoTes = (PianoPrelieviTestata)pianiPrl.get(i);
        if(pianoTes.getRCodLista() == null)
        	continue;

        //28316 inizio
        /*String whereTS = ListaPrelievoTestataTM.ID_AZIENDA + " = '" + Azienda.getAziendaCorrente() + "' AND " +
        								 ListaPrelievoTestataTM.ID_LISTA_PRL + " = '" + pianoTes.getRCodLista() + "'";
        Vector elencoTL = ListaPrelievoTestata.retrieveList(whereTS, "", true);*/
        ListaPrelievoTestata listaPlTes = recuperaListaPrlTestata(pianoTes.getRCodLista());
        //LTB fine
        
        //if (listePlTes.size() == 1) {
        	//ListaPrelievoTestata tl = (ListaPrelievoTestata)listePlTes.get(0);
        if(listaPlTes != null) {
        	// 28316 fine
        	//22873 inizio
        	//Fix 34411 -- Inizio
        	if(pianoTes.getOrigine() == ModelloPrelievo.SERVIZI) {
        		Vector erroriGenDocSrv = listaPlTes.generaDocumentoServizio(null, ListaPrelievoTestata.FASE_NESSUN_ERR_FORC);
	        	if(!erroriGenDocSrv.isEmpty()) {
		            String erroreTestataPiano = "* Errore nel generazione del doc. srv del piano Rif. " + formattaChiaveTestataLista(pianoTes) + " : ";
		            output.println(erroreTestataPiano);
		            iBatchJob.setApplStatus(BatchJob.WITH_WARNING); //Fix 47561
		            for(int e = 0; e < erroriGenDocSrv.size(); e++) {
		              ErrorMessage em = (ErrorMessage)erroriGenDocSrv.get(e);
		              output.println(" - " + em.getLongText());
		            }
		            //Fix 23872 inizio            
		            char severityErrors = UtilsUbicazioneBarcode.getErrorsType(erroriGenDocSrv);
		            if (severityErrors == UtilsUbicazioneBarcode.ERRORE || severityErrors == UtilsUbicazioneBarcode.ERRORE_FORCEABILE) {
		            	ret = false;
		            	break;
		            }
	        	}
        	}
        	else {
        	//Fix 34411 -- Fine	
	        	Vector erroriGenDocPrd = listaPlTes.generaDocumentoProduzione(null, ListaPrelievoTestata.FASE_NESSUN_ERR_FORC);
	        	if(!erroriGenDocPrd.isEmpty()) {
	            String erroreTestataPiano = "* Errore nel generazione del doc. prd del piano Rif. " + formattaChiaveTestataLista(pianoTes) + " : ";
	            output.println(erroreTestataPiano);
	            iBatchJob.setApplStatus(BatchJob.WITH_WARNING); //Fix 47561
	            for(int e = 0; e < erroriGenDocPrd.size(); e++) {
	              ErrorMessage em = (ErrorMessage)erroriGenDocPrd.get(e);
	              output.println(" - " + em.getLongText());
	            }
	            //Fix 23872 inizio            
	            char severityErrors = UtilsUbicazioneBarcode.getErrorsType(erroriGenDocPrd);
	            if (severityErrors == UtilsUbicazioneBarcode.ERRORE || severityErrors == UtilsUbicazioneBarcode.ERRORE_FORCEABILE) {
	            	ret = false;
	            	break;
	            }
	            //Fix 23872 fine            
	             
	        	}
        	}//Fix 34411
        	/*if(ret) {
        		boolean lstPianoSalvato = forzaChiusuraListaPrelievo(tl);
        		if (!lstPianoSalvato) {
        			String erroreGenerico = "* " + ResourceLoader.getString(RES_FILE, "ErroreGenerazioneLista");
        			output.println(erroreGenerico);
        			ret = false;
        		}
        	}*/
        	//22873 fine        	
        }
      }
    } catch (Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return false;
    }
    return ret;
  }  
  
  //28316 inizio
  public ListaPrelievoTestata recuperaListaPrlTestata(String idLista) {
    try {
        String chiaveLista = KeyHelper.buildObjectKey(new Object[] {Azienda.getAziendaCorrente(), idLista});
        ListaPrelievoTestata listaTes = ListaPrelievoTestata.elementWithKey(chiaveLista, PersistentObject.NO_LOCK);
        //if(listaTes != null && listaTes.getAttivitaEsecutiva() != null) //Fix 34411
        if(listaTes != null && (listaTes.getAttivitaEsecutiva() != null || listaTes.getAttivitaServizio() !=null)) //Fix 34411
        	return listaTes;
        return null;
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return null;
    }
  }
  //28316 fine
  
  public boolean forzaChiusuraListaPrelievo(ListaPrelievoTestata listaTestata) {
  	listaTestata.setStatoLista(ListaPrelievoTestata.ST_COMPLETATO);
  	List righe = listaTestata.getRighe();
  	Iterator righeIt = righe.iterator();
  	while(righeIt.hasNext()) {
  		ListaPrelievoRiga riga = (ListaPrelievoRiga)righeIt.next();
  		riga.setStatoLista(ListaPrelievoTestata.ST_COMPLETATO);
  	}
  	return salvaListaPrelievo(listaTestata);	
  }
  
  public boolean salvaListaPrelievo(ListaPrelievoTestata listaTestata) {
    Vector errori = controllaListaPrelievo(listaTestata);
    if(!errori.isEmpty())
    	return false;
    
  	try {
  		int rcDoc = listaTestata.save();
  		if(rcDoc >= 0) {
  			//ConnectionManager.commit(); //22873
  			return true;
  		}
  		else {
  			//ConnectionManager.rollback(); // 22873
  			return false;  			
  		}
  	}
  	catch (SQLException e) {
  		e.printStackTrace(Trace.excStream);
  	}
		return false;
  }
  
  public Vector controllaListaPrelievo(ListaPrelievoTestata listaTestata) {
  	Vector errori = new Vector();
  	BODataCollector boDC = null;
  	try {
  		boDC = createDataCollector("ListaPrelievoTestata");
  		boDC.setBo(listaTestata);
  		int rc = boDC.check();
  		if (rc != BODataCollector.OK) {
  			return boDC.getErrorList().getErrors();
  		}
  		else {
  			SecondaryDataCollector collRighe = boDC.getSecondaryDataCollector("Righe");
  			String className = collRighe.getClassADCollectionName();
  			BODataCollector boDCRiga = createDataCollector(className);
  			for (int i = 0; i < listaTestata.getRighe().size(); i++) {
  				ListaPrelievoRiga riga = (ListaPrelievoRiga) listaTestata.getRighe().get(i);
  				boDCRiga.setBo(riga);
  				boDCRiga.setAutoCommit(false);
  				int ret = boDCRiga.check();
  				if (ret != BODataCollector.OK) 
  					errori.addAll(boDCRiga.getErrorList().getErrors());
  			}
  		}
  	}
  	catch (SQLException e) {
  		e.printStackTrace(Trace.excStream);
  	}
  	return errori;
	} 
  
  public BODataCollector createDataCollector(String className) throws SQLException{
    try {
      BODataCollector bodc = null;
      ClassADCollection classDesc = ClassADCollectionManager.collectionWithName(className);
      String collectorName = classDesc.getBODataCollector();
      if (collectorName != null) {
        bodc = (BODataCollector)Factory.createObject(collectorName);
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
  
  
  public ConfigurazionePianiPrelievo iConfigPianiPrl = null;
  public ConfigurazionePianiPrelievo getConfigGenDocProduzione() {
  	if (iConfigPianiPrl!= null )
  		return iConfigPianiPrl;
  	
  	List cfgPianoPrl;
		try {
			cfgPianoPrl = ConfigurazionePianiPrelievo.retrieveList("", "", false);
	  	if(!cfgPianoPrl.isEmpty())
	  		iConfigPianiPrl =  (ConfigurazionePianiPrelievo)cfgPianoPrl.get(0);
		}
		catch (Exception e) {
			e.printStackTrace(Trace.excStream);
		}
  	return iConfigPianiPrl;
  	/*
  	List currentUserItems = MenuUtenteLL.getUserMenuItems();
  	Iterator itemsIt = currentUserItems.iterator();
  	while(itemsIt.hasNext()) {
  		MenuUtenteItemLL itemLL = (MenuUtenteItemLL)itemsIt.next();
  		if(itemLL.getIdEntita().equals("PianoPrelievoUB") && itemLL.getIdConfigPianiPrelievo() != null)
  			return itemLL.getConfigPianiPrelievo();
  	}
    return null;
    */
  }
  //Fix 22443 fine


  //Fix 29772 - inizio
  /**
   * Inizializzazione dati per perdonalizzazioni
   * @param batch Questa istanza
   * @throws Exception
   */
  public void inizializzaDatiPers(GenerazionePianiPrlBatch batch) throws Exception {
	  
  }
  
  
  /**
   * 
   * @return Ordinamento dati
   */
  protected String getOrderBy() {
	  return 
			  WrapperPianoPrlDatiTM.R_STABILIMENTO + "," +
			  WrapperPianoPrlDatiTM.R_REPARTO + "," +
			  //WrapperPianoPrlDatiTM.DATA_PRV_IMPIEGO + "," + //Fix 38163
			  WrapperPianoPrlDatiTM.MIN_DATA_PRV_IMP + "," + //Fix 38163
			  WrapperPianoPrlDatiTM.ID_ANNO_ORD + "," +
			  WrapperPianoPrlDatiTM.ID_NUMERO_ORD + "," +
			  WrapperPianoPrlDatiTM.ID_RIGA_ATTIVITA + "," +
			  WrapperPianoPrlDatiTM.ID_RIGA_MATERIALE + "," +
			  WrapperPianoPrlDatiTM.ID_LOTTO;
  }
  
  
  /**
   * Eventuale riordino degli elementi {@link WrapperPianoPrlDati} selezionati
   * @param elencoWrapper
   * @throws Exception
   */
  protected void riordinaElencoWrapper(List elencoWrapper) throws Exception {
	  
  }
  
  
  /**
   * 
   * @param stabilimento
   * @param reparto
   * @param wrPianoPrlDati
   * @return La prima chiave di rottura per la creazione dei piani di prelievo
   * @throws Exception
   */
  protected String getPrimaChiaveRotturaPianoPrelievoTestata(String stabilimento, String reparto, WrapperPianoPrlDati wrPianoPrlDati) throws Exception {
      String idAnnoOrdine = wrPianoPrlDati.getIdAnnoOrd();
      String idNumeroOrdine = wrPianoPrlDati.getIdNumeroOrd();
      Integer idRigaAttivita = wrPianoPrlDati.getIdRigaAttivita();
      
      return KeyHelper.buildObjectKey(new Object[] {stabilimento, reparto, idAnnoOrdine, idNumeroOrdine, idRigaAttivita});
  }

  
  /**
   * 
   * @param stabilimento
   * @param reparto
   * @param wrPianoPrlDati
   * @return La chiave di rottura per attivita nella creazione dei piani di prelievo
   * @throws Exception
   */
  protected String getChiaveRotturaAttivitaPianoPrelievoTestata(String stabilimento, String reparto, WrapperPianoPrlDati wrPianoPrlDati) throws Exception {
      String idAnnoOrdine = wrPianoPrlDati.getIdAnnoOrd();
      String idNumeroOrdine = wrPianoPrlDati.getIdNumeroOrd();
      Integer idRigaAttivita = wrPianoPrlDati.getIdRigaAttivita();
      
      return KeyHelper.buildObjectKey(new Object[] {stabilimento, reparto, idAnnoOrdine, idNumeroOrdine, idRigaAttivita});
  }

  
  /**
   * Operazioni da effettuare alla rottura di attività esecutiva in creazine dei piano di prelievo
   * @throws Exception
   */
  protected void rilevaRotturaAttivita() throws Exception {
	  
  }
  
  
  /**
   * 
   * @return Il codice del modello di prelievo da assegnare ad un piano di prelievo in creazione
   */
  protected String getIdModelloPrelievoDaAssegnare() {
	  return getModelloPrelievo().getIdModelloPrelievo();
  }
  //Fix 29772 - fine

  
  //Fix 31875 - inizio
  protected boolean isRecordProcessabile(WrapperPianoPrlDati wrPianoPrlDati) {
	  return wrPianoPrlDati.isProcessabile();
  }

  
  protected void visualizzaMessaggioRecordNonProcessabile(WrapperPianoPrlDati wrPianoPrlDati) {
	  
  }
  //Fix 31875 - fine
  
  //Fix 34411 -- Inizio
  /**
   * generazioneNuovoPianoSrv
   * @return boolean
   */
  public boolean generazioneNuovoPianoSrv() {
    boolean ret = true;
    String endGenPianoTxt = ResourceLoader.getString(RES_FILE, "EndGenPianoTxt");
    try {
      String where = costruisciWhere();

      String orderBy = getOrderBy();
      Vector elencoWrapper = WrapperPianiPrlSrv.retrieveList(where, orderBy, false);
      riordinaElencoWrapper(elencoWrapper);

      String primaChiaveRottura = "";
      String chiaveRotturaAttivita = "";
      PianoPrelieviTestata testataPiano = null;
      int countTestate = 0;
      for(int i = 0; i < elencoWrapper.size(); i++) {
    	  WrapperPianiPrlSrv wrapperPianiPrlSrv = (WrapperPianiPrlSrv)elencoWrapper.get(i);

        if (isRecordProcessabile(wrapperPianiPrlSrv)) {
	        String articoloKey = wrapperPianiPrlSrv.getIdAzienda() + PersistentObject.KEY_SEPARATOR + wrapperPianiPrlSrv.getRArticoloMat();
	        Articolo art = (Articolo)Factory.createObject(Articolo.class);
	        art.setKey(articoloKey);
	        boolean ext = art.retrieve();
	        boolean escludiArticolo = false;
	        if (art.getTipoParte() == ArticoloDatiIdent.ARTICOLO_WIP) {
	          ParametroPsn paramPsn = ParametroPsn.getParametroPsn("std.pianiPrelievo", "escludiArticoliWIP", Azienda.getAziendaCorrente());
	          if (paramPsn != null && paramPsn.getValore().equals("Y"))
	            escludiArticolo = true;
	        }

	        if(ext && isGesUbicazioneEBarcode() && !escludiArticolo) {	
	          String stabilimento = wrapperPianiPrlSrv.getRStabilimento();
	          String reparto = wrapperPianiPrlSrv.getRReparto();

	          String primaChiaveRotturaTmp = getPrimaChiaveRotturaPianoPrelievoTestata(stabilimento, reparto, wrapperPianiPrlSrv);
	
	          if(!primaChiaveRottura.equals(primaChiaveRotturaTmp)) {
	            primaChiaveRottura = primaChiaveRotturaTmp;
	            if(testataPiano != null) {
	              boolean eseguiCommit = false;
	              if(countTestate % cNumCommit == 0)
	                eseguiCommit = true;
	              salvaTestataPiano(testataPiano, eseguiCommit);
	            }

	            String chiaveRotturaAttivitaTmp = getChiaveRotturaAttivitaPianoPrelievoTestata(stabilimento, reparto, wrapperPianiPrlSrv);
	            if (!chiaveRotturaAttivita.equals(chiaveRotturaAttivitaTmp)) {
	            	chiaveRotturaAttivita = chiaveRotturaAttivitaTmp;
	            	rilevaRotturaAttivita();
	            }
	            //Fix 40182 -- Inizio
	            if (isEscludiAtvInAltroPiano() && esisteAtvInAltroPianoSrv(wrapperPianiPrlSrv)) 
	               testataPiano = null;
	            else { 
	            //Fix 40182 -- Fine
	              testataPiano = creaPianoPrelieviTestata(wrapperPianiPrlSrv);
	              countTestate++;
	            }//Fix 40182
	          }
	          if(testataPiano != null) //Fix 40182
	            creaPianoPrelieviRiga(testataPiano, wrapperPianiPrlSrv);
	        }
        }
        else {
        	visualizzaMessaggioRecordNonProcessabile(wrapperPianiPrlSrv);
        }
      }
      //...Salvo l'ultima testata creata e faccio commit
      if(testataPiano != null) {
        salvaTestataPiano(testataPiano, true);
      }

      if(countTestate > 0)
        output.println(endGenPianoTxt + " " + iEndOkTxt);

    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      output.println(endGenPianoTxt + " " + iEndErrTxt);
      return false;
    }
    return ret;
  }
  
  protected boolean isRecordProcessabile(WrapperPianiPrlSrv wrapperPianiPrlSrv) {
	  return wrapperPianiPrlSrv.isProcessabile();
  }
  
  /**
   * 
   * @param stabilimento
   * @param reparto
   * @param wrapperPianiPrlSrv
   * @return La prima chiave di rottura per la creazione dei piani di prelievo
   * @throws Exception
   */
  protected String getPrimaChiaveRotturaPianoPrelievoTestata(String stabilimento, String reparto, WrapperPianiPrlSrv wrapperPianiPrlSrv) throws Exception {
      String idAnnoOrdine = wrapperPianiPrlSrv.getIdAnnoOrd();
      String idNumeroOrdine = wrapperPianiPrlSrv.getIdNumeroOrd();
      Integer idRigaAttivita = wrapperPianiPrlSrv.getIdRigaAttivita();
      
      return KeyHelper.buildObjectKey(new Object[] {stabilimento, reparto, idAnnoOrdine, idNumeroOrdine, idRigaAttivita});
  }

  
  /**
   * 
   * @param stabilimento
   * @param reparto
   * @param wrapperPianiPrlSrv
   * @return La chiave di rottura per attivita nella creazione dei piani di prelievo
   * @throws Exception
   */
  protected String getChiaveRotturaAttivitaPianoPrelievoTestata(String stabilimento, String reparto, WrapperPianiPrlSrv wrapperPianiPrlSrv) throws Exception {
      String idAnnoOrdine = wrapperPianiPrlSrv.getIdAnnoOrd();
      String idNumeroOrdine = wrapperPianiPrlSrv.getIdNumeroOrd();
      Integer idRigaAttivita = wrapperPianiPrlSrv.getIdRigaAttivita();
      
      return KeyHelper.buildObjectKey(new Object[] {stabilimento, reparto, idAnnoOrdine, idNumeroOrdine, idRigaAttivita});
  }
  
  protected void visualizzaMessaggioRecordNonProcessabile(WrapperPianiPrlSrv wrapperPianiPrlSrv) {
	  
  }
  
  /**
   * creaPianoPrelieviTestata
   * @param wrapperPianiPrlSrv WrapperPianiPrlSrv
   * @param filtroPianoPrl CfgLogTxPianoPrelievo
   * @return PianoPrelieviTestata
   */
  public PianoPrelieviTestata creaPianoPrelieviTestata(WrapperPianiPrlSrv wrapperPianiPrlSrv) {
    PianoPrelieviTestata pianoPrlTes = (PianoPrelieviTestata)Factory.createObject(PianoPrelieviTestata.class);
    pianoPrlTes.setOrigine(ModelloPrelievo.SERVIZI);
    pianoPrlTes.setIdAzienda(wrapperPianiPrlSrv.getIdAzienda());
    pianoPrlTes.setIdModelloPrl(getIdModelloPrelievoDaAssegnare());
    pianoPrlTes.setTipoModelloPrl(PianoPrelieviTestata.TP_MOD_CORRENTE);
    pianoPrlTes.setIdAnnoOrd(wrapperPianiPrlSrv.getIdAnnoOrd());
    pianoPrlTes.setIdNumeroOrd(wrapperPianiPrlSrv.getIdNumeroOrd());
    pianoPrlTes.setIdRigaAttivita(wrapperPianiPrlSrv.getIdRigaAttivita());
    pianoPrlTes.setRArticolo(wrapperPianiPrlSrv.getRArticolo());
    pianoPrlTes.setRVersione(wrapperPianiPrlSrv.getRVersione());
    pianoPrlTes.setRConfigurazione(wrapperPianiPrlSrv.getRConfigurazione());
    pianoPrlTes.setDescArticolo(wrapperPianiPrlSrv.getDescrizione());
    pianoPrlTes.setRCommessa(wrapperPianiPrlSrv.getRCommessa());
    pianoPrlTes.setNumRitorno(wrapperPianiPrlSrv.getNumRitorno());
    pianoPrlTes.setRStabilimento(wrapperPianiPrlSrv.getRStabilimento());
    pianoPrlTes.setRReparto(wrapperPianiPrlSrv.getRReparto());
    pianoPrlTes.setRCentroLavoro(wrapperPianiPrlSrv.getRCentroLavoro());
    pianoPrlTes.setROperazione(wrapperPianiPrlSrv.getROperazione());
    pianoPrlTes.setRAttivita(wrapperPianiPrlSrv.getRAttivita());

    Object[] dataOraInizio = calcolaData(wrapperPianiPrlSrv, false);
    pianoPrlTes.setDataInizio((java.sql.Date)dataOraInizio[0]);
    pianoPrlTes.setOraInizio((java.sql.Time)dataOraInizio[1]);
    Object[] dataFine = calcolaData(wrapperPianiPrlSrv, true);
    pianoPrlTes.setDataFine((java.sql.Date)dataFine[0]);
    pianoPrlTes.setPriorita(calcolaPriorita(pianoPrlTes.getOraInizio()));

    pianoPrlTes.setRCliente(wrapperPianiPrlSrv.getRCliente());
    pianoPrlTes.setRUmPrmMag(wrapperPianiPrlSrv.getOrdUmPrm());
    pianoPrlTes.setQtaResidua(wrapperPianiPrlSrv.getQtaResAtv());

    pianoPrlTes.setStatoDisponAtv(PianoPrelieviTestata.ST_DSP_ATV_NON_SIGNIF);
    
    if(isGesUbicazioneEBarcode()) {
    	pianoPrlTes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_ESEGUITO);
    	pianoPrlTes.setGesUbicazioneEBarcode(true);
    }
    else {
      if(getPreselezione() == ModelloPrelievo.TP_PRSL_TUTTE)
        pianoPrlTes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_DA_ESEG);
      else
        pianoPrlTes.setStatoGenLista(PianoPrelieviTestata.ST_GEN_LST_NO);    	
    }

    if(isGesUbicazioneEBarcode()) { 
    	ConfigurazionePianiPrelievo cfgPianiPrl = getConfigGenDocProduzione();
    	if(cfgPianiPrl != null) {
    		pianoPrlTes.setRCauDocSrv(cfgPianiPrl.getIdCauDocServizio());
    		pianoPrlTes.setRDipendenteSrv(cfgPianiPrl.getIdDichiaranteSrv());
    		pianoPrlTes.setRNumeratorDocSrv(cfgPianiPrl.getIdNumDocSrv());
    		pianoPrlTes.setRSerieNumDocSrv(cfgPianiPrl.getIdSerieNumDocSrv());
    	}
    } 
    
    return pianoPrlTes;
  }
  
  /**
   * calcolaData
   * @param wrapperPianiPrlSrv WrapperPianiPrlSrv
   * @param isDataFine boolean
   * @return Date
   */
  public Object[] calcolaData(WrapperPianiPrlSrv WrapperPianiPrlSrv, boolean isDataFine) {
    Object[] ret = new Object[2];
    java.sql.Date dbStartDate = null;
    java.sql.Date dbEndDate = TimeUtils.getDate(9999, 12, 31);
    ConnectionDescriptor cd = ConnectionManager.getCurrentConnectionDescriptor();
    if(cd != null)
      dbStartDate = cd.getDatabase().getMinimumDate();

    java.sql.Date cmpDate = null;
    if(isDataFine)
      cmpDate = dbEndDate;
    else
      cmpDate = dbStartDate;

    java.sql.Date dataEff = null;
    if(isDataFine)
      dataEff = WrapperPianiPrlSrv.getDataFineEff();
    else
      dataEff = WrapperPianiPrlSrv.getDataInizioEff();
    if(dataEff != null && dataEff.compareTo(cmpDate) != 0) {
      ret[0] = dataEff;
      ret[1] = WrapperPianiPrlSrv.getOraInizioEff();
      return ret;
    }

    java.sql.Date dataPgm = null;
    if(isDataFine)
      dataPgm = WrapperPianiPrlSrv.getDataFinePgm();
    else
      dataPgm = WrapperPianiPrlSrv.getDataInizioPgm();
    if(dataPgm != null && dataPgm.compareTo(cmpDate) != 0) {
      ret[0] = dataPgm;
      ret[1] = WrapperPianiPrlSrv.getOraInizioPgm();
      return ret;
    }

    java.sql.Date dataRcs = null;
    if(isDataFine)
      dataRcs = WrapperPianiPrlSrv.getDataFineRcs();
    else
      dataRcs = WrapperPianiPrlSrv.getDataInizioRcs();
    if(dataRcs != null && dataRcs.compareTo(cmpDate) != 0) {
      ret[0] = dataRcs;
      ret[1] = null;
      return ret;
    }

    return ret;
  }
  
  /**
   * creaPianoPrelieviRiga
   * @param pianoPrlTes PianoPrelieviTestata
   * @param wrapperPianiPrlSrv WrapperPianiPrlSrv
   */
  public void creaPianoPrelieviRiga(PianoPrelieviTestata pianoPrlTes, WrapperPianiPrlSrv wrapperPianiPrlSrv) {
    if(pianoPrlTes != null) {
      PianoPrelieviRiga pianoPrlRig = (PianoPrelieviRiga)Factory.createObject(PianoPrelieviRiga.class);
      pianoPrlRig.setOrigine(ModelloPrelievo.SERVIZI);
      pianoPrlRig.setTestata(pianoPrlTes);
      pianoPrlRig.setIdModelloPrl(pianoPrlTes.getIdModelloPrl());
      pianoPrlRig.setTipoModelloPrl(pianoPrlTes.getTipoModelloPrl());
      pianoPrlRig.setIdRigaMateriale(wrapperPianiPrlSrv.getIdRigaMateriale());
      pianoPrlRig.setIdArticolo(wrapperPianiPrlSrv.getRArticoloMat());
      pianoPrlRig.setIdLotto(wrapperPianiPrlSrv.getIdLotto());
      pianoPrlRig.setRVersione(wrapperPianiPrlSrv.getRVersioneMat());
      pianoPrlRig.setRConfigurazione(wrapperPianiPrlSrv.getRConfigurazioneMat());
      pianoPrlRig.setRCommessa(wrapperPianiPrlSrv.getRCommessaMat());
      pianoPrlRig.setRMagazzino(wrapperPianiPrlSrv.getRMagazzinoMat());
      pianoPrlRig.setDataPrvImpiego(wrapperPianiPrlSrv.getDataPrvImpiego());
      pianoPrlRig.setRUmPrmMag(wrapperPianiPrlSrv.getMatUmPrm());
      pianoPrlRig.setRUmSecMag(wrapperPianiPrlSrv.getMatUmSec());
      pianoPrlRig.setQtaRichUmPrm(wrapperPianiPrlSrv.getQtaRcsMat());
      pianoPrlRig.setQtaRichUmPrm(wrapperPianiPrlSrv.getQtaRcsPrmLotto());
      pianoPrlRig.setQtaResUmPrm(wrapperPianiPrlSrv.getQtaResPrmLotto());
      pianoPrlRig.setQtaResUmSec(wrapperPianiPrlSrv.getQtaResSecLotto());

      pianoPrlRig.setQtaDaPrlUmPrm(wrapperPianiPrlSrv.getQtaResPrmLotto());
      pianoPrlRig.setQtaDaPrlUmSec(wrapperPianiPrlSrv.getQtaResSecLotto());

      pianoPrlRig.setCoeffImpiego(calcolaCoeffImpiego(wrapperPianiPrlSrv));
      pianoPrlRig.setCoeffTotale(wrapperPianiPrlSrv.getCoeffTotale());

      pianoPrlTes.getRighe().add(pianoPrlRig);
    }
  }

  public boolean calcoloQtaMaxProducibileSrv(ModelloPrelievo modPrl) {
	  boolean ret = true;
	  if(modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_RES_CON_CTRL ||
			  modPrl.getTipoQuantita() == ModelloPrelievo.TP_QTA_MAX_PRODUCIBILE) {
		  ResultSet listaRighePiani = trovaRigheQtaMaxPrd(modPrl.getIdModelloPrelievo());
		  if(listaRighePiani != null) {
			  int count = calcoloQtaMaxProducibileSrv(listaRighePiani);
			  String endCalcQMaxTxt = ResourceLoader.getString(RES_FILE, "EndCalcQMaxTxt");
			  if(count > 0) {
				  ret = true;
				  output.println(endCalcQMaxTxt + " " + iEndOkTxt);
			  }
			  else if(count < 0) {
				  ret = false;
				  output.println(endCalcQMaxTxt + " " + iEndErrTxt);
			  }
		  }
	  }
	  return ret;
  }
  
  /**
   * calcoloQtaMaxProducibile
   * @param rs ResultSet
   * @return boolean
   */
  public int calcoloQtaMaxProducibileSrv(ResultSet rs) {
    try {
      String chiaveRottura = "";
      BigDecimal qtaTeorProducibile = null;
      BigDecimal qtaTeorProdotta = null;
      PianoPrelieviTestata tes = null;
      int i = 0;
      while(rs.next()) {

        String idAzienda = rs.getString(PianoPrelieviRigaTM.ID_AZIENDA);
        String idModello = rs.getString(PianoPrelieviRigaTM.ID_MODELLO_PRL);
        char tipoModello = rs.getString(PianoPrelieviRigaTM.TIPO_MODELLO_PRL).charAt(0);
        String annoOrd = rs.getString(PianoPrelieviRigaTM.ID_ANNO_ORD);
        String numOrd = rs.getString(PianoPrelieviRigaTM.ID_NUMERO_ORD);
        Integer idRigaAtv = new Integer(rs.getInt(PianoPrelieviRigaTM.ID_RIGA_ATTIVITA));

        String chiaveRotturaTmp = KeyHelper.buildObjectKey(new Object[] {annoOrd, numOrd, idRigaAtv});

        if(!chiaveRotturaTmp.equals(chiaveRottura)) {
          chiaveRottura = chiaveRotturaTmp;
          if(tes != null) {
              boolean eseguiCommit = false;
              if(i % cNumCommit == 0)
                eseguiCommit = true;
              if(cStampaLogCalcoli) {
                System.out.println("QUANTITA MAX PRODUCIBILE (Min. qtà teor. prodotta) --> " + qtaTeorProdotta);
                System.out.println("QUANTITA MIN PRODUCIBILE (Min. qtà teor. producibile) --> " + qtaTeorProducibile);
              }
              aggiornaDisponTestata(tes, qtaTeorProducibile, qtaTeorProdotta, eseguiCommit);
            qtaTeorProducibile = null;
            qtaTeorProdotta = null;
          }

          tes = (PianoPrelieviTestata)Factory.createObject(PianoPrelieviTestata.class);
          tes.setIdAzienda(idAzienda);
          tes.setIdModelloPrl(idModello);
          tes.setTipoModelloPrl(tipoModello);
          tes.setIdAnnoOrd(annoOrd);
          tes.setIdNumeroOrd(numOrd);
          tes.setIdRigaAttivita(idRigaAtv);
          boolean ret = tes.retrieve(PersistentObject.NO_LOCK);
          if(ret) {
            List righe = tes.getRighe();

            if(cStampaLogCalcoli) {
              System.out.println("");
              System.out.println("ATTIVITA' -----> " + tes.getAttivitaServizio().getKey() + " (" + tes.getDataInizio() + " / " + tes.getPriorita() + ")");
            }
            for(int j = 0; j < righe.size(); j++) {
              PianoPrelieviRiga riga = (PianoPrelieviRiga)righe.get(j);

              if(cStampaLogCalcoli) {
                if(j == 0) {
                  System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------");
                  System.out.println("Articolo            | V | Config.        | Lotto          | Commessa     | CT | Coeff.Imp. | QtaRich.    | QtaResidua  | QtaDispon.  | TeorProdot. | TeorProduc. ");
                }
              }
              if(!riga.isCoeffTotale()) {
                BigDecimal qtaRichiesta = riga.getQtaRichUmPrm() != null ? riga.getQtaRichUmPrm() : ZERO;
                BigDecimal qtaResidua = riga.getQtaResUmPrm() != null ? riga.getQtaResUmPrm() : ZERO;
                BigDecimal qtaDisponibile = riga.getQtaDispUmPrm() != null ? riga.getQtaDispUmPrm() : ZERO;
                BigDecimal coeffImpiego = riga.getCoeffImpiego();
                BigDecimal dividendo = qtaRichiesta.subtract(qtaResidua).add(qtaDisponibile);
                BigDecimal qtaTeorProducibileTmp = Q6Calc.get().divide(dividendo, coeffImpiego, 6, BigDecimal.ROUND_HALF_UP);
                ArticoloDatiMagaz articoloDatiMagaz = riga.getTestata().getArticolo().getArticoloDatiMagaz();
                if((articoloDatiMagaz.isQtaIntera())||(articoloDatiMagaz.getUMPrmMag() != null && articoloDatiMagaz.getUMPrmMag().getQtaIntera()))
                {
                  qtaTeorProducibileTmp = Q6Calc.get().setScale(qtaTeorProducibileTmp, 0,BigDecimal.ROUND_DOWN);
                }
                if(qtaTeorProducibile == null || qtaTeorProducibile.compareTo(qtaTeorProducibileTmp) > 0)
                  qtaTeorProducibile = qtaTeorProducibileTmp;

                dividendo = qtaRichiesta.subtract(qtaResidua);
                BigDecimal qtaTeorProdottaTmp = Q6Calc.get().divide(dividendo, coeffImpiego, 6, BigDecimal.ROUND_HALF_UP);//Fix 30965

                if(qtaTeorProdotta == null || qtaTeorProdotta.compareTo(qtaTeorProdottaTmp) > 0)
                  qtaTeorProdotta = qtaTeorProdottaTmp;

                if(cStampaLogCalcoli) {
                  System.out.print(riga.getIdArticolo().length() > 20 ? riga.getIdArticolo().substring(0, 21) : aggiungiSpazi(riga.getIdArticolo(), 20));
                  System.out.print("| " + aggiungiSpazi(riga.getRVersione().toString(), 2));
                  System.out.print("| " + (riga.getIdEsternoConfig() != null && riga.getIdEsternoConfig().length() > 15 ? riga.getIdEsternoConfig().substring(0, 16) : aggiungiSpazi(riga.getIdEsternoConfig(), 15)));
                  System.out.print("| " + (riga.getIdLotto().length() > 15 ? riga.getIdLotto().substring(0, 16) : aggiungiSpazi(riga.getIdLotto(), 15)));
                  String cmm = commessaPerSaldi(riga);
                  System.out.print("| " + (cmm.length() > 13 ? cmm.substring(0, 14) : aggiungiSpazi(cmm, 13)));
                  System.out.print("| " + (riga.isCoeffTotale() ? "Si " : "No "));
                  System.out.print("| " + aggiungiSpazi(coeffImpiego.toString(), 11));
                  System.out.print("| " + aggiungiSpazi(qtaRichiesta.toString(), 12));
                  System.out.print("| " + aggiungiSpazi(qtaResidua.toString(), 12));
                  System.out.print("| " + aggiungiSpazi(qtaDisponibile.toString(), 12));
                  System.out.print("| " + aggiungiSpazi(qtaTeorProdottaTmp.toString(), 12));
                  System.out.println("| " + qtaTeorProducibileTmp.toString());
                }

              }
              else {

                if(cStampaLogCalcoli) {
                  System.out.print(riga.getIdArticolo().length() > 20 ? riga.getIdArticolo().substring(0, 21) : aggiungiSpazi(riga.getIdArticolo(), 20));
                  System.out.print("| " + aggiungiSpazi(riga.getRVersione().toString(), 2));
                  System.out.print("| " + (riga.getIdEsternoConfig() != null && riga.getIdEsternoConfig().length() > 15 ? riga.getIdEsternoConfig().substring(0, 16) : aggiungiSpazi(riga.getIdEsternoConfig(), 15)));
                  System.out.print("| " + (riga.getIdLotto().length() > 15 ? riga.getIdLotto().substring(0, 16) : aggiungiSpazi(riga.getIdLotto(), 15)));
                  String cmm = commessaPerSaldi(riga);
                  System.out.print("| " + (cmm.length() > 13 ? cmm.substring(0, 14) : aggiungiSpazi(cmm, 13)));
                  System.out.print("| " + (riga.isCoeffTotale() ? "Si " : "No "));
                  System.out.print("| " + aggiungiSpazi(riga.getCoeffImpiego().toString(), 11));
                  System.out.print("| " + aggiungiSpazi(riga.getQtaRichUmPrm().toString(), 12));
                  System.out.print("| " + aggiungiSpazi(riga.getQtaResUmPrm().toString(), 12));
                  System.out.print("| " + aggiungiSpazi(riga.getQtaDispUmPrm().toString(), 12));
                  System.out.print("| " + aggiungiSpazi("--", 12));
                  System.out.println("| --");
                }
              }

            }
            if(cStampaLogCalcoli)
              System.out.println("-----------------------------------------------------------------------------------------------------------------------------------------------------------------");
          }
        }
        i++;
      }

      if(tes != null) {
          boolean eseguiCommit = false;
          if(i % cNumCommit == 0)
            eseguiCommit = true;
          if(cStampaLogCalcoli) {
            System.out.println("QUANTITA MAX PRODUCIBILE (Min. qtà teor. prodotta) --> " + qtaTeorProdotta);
            System.out.println("QUANTITA MIN PRODUCIBILE (Min. qtà teor. producibile) --> " + qtaTeorProducibile);
          }
          aggiornaDisponTestata(tes, qtaTeorProducibile, qtaTeorProdotta, eseguiCommit);
      }

      rs.close();
      return i;

    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
      return -1;
    }
  }
  //Fix 34411 -- Fine
  
  //Fix 40182 -- Inizio
  public void setEscludiAtvInAltroPiano(boolean escludiAtvInAltroPiano) {
	iEscludiAtvInAltroPiano = escludiAtvInAltroPiano;
  }

  public boolean isEscludiAtvInAltroPiano() {
    return iEscludiAtvInAltroPiano;
  }
  
  public boolean esisteAtvInAltroPianoPrd(WrapperPianoPrlDati wrPianoPrlDati) {
	ResultSet rs = null;
	String ret = null;
	try {
      Database db = ConnectionManager.getCurrentDatabase();
      PreparedStatement ps = cTrovaAtvInAltroPiano.getStatement();
      db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
      db.setString(ps, 2, wrPianoPrlDati.getIdAnnoOrd());
      db.setString(ps, 3, wrPianoPrlDati.getIdNumeroOrd());
      ps.setInt(4, wrPianoPrlDati.getIdRigaAttivita());
      db.setString(ps, 5, getModelloPrelievo().getIdModelloPrelievo());
      rs = ps.executeQuery();
      if(rs.next()) {
        ret = rs.getString(PianoPrelieviTestataTM.ID_AZIENDA).trim();
      }
      if(ret != null) {
    	return true;  
      }
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if (rs != null) {
        try {
          rs.close();
        }
        catch (SQLException  e) {
          e.printStackTrace(Trace.excStream);
        }
      }
    }
	return false;
  }
  
  public boolean esisteAtvInAltroPianoSrv(WrapperPianiPrlSrv wrapperPianiPrlSrv) {
	ResultSet rs = null;
	String ret = null;
	try {
      Database db = ConnectionManager.getCurrentDatabase();
      PreparedStatement ps = cTrovaAtvInAltroPiano.getStatement();
      db.setString(ps, 1, getModelloPrelievo().getIdAzienda());
      db.setString(ps, 2, wrapperPianiPrlSrv.getIdAnnoOrd());
      db.setString(ps, 3, wrapperPianiPrlSrv.getIdNumeroOrd());
      ps.setInt(4, wrapperPianiPrlSrv.getIdRigaAttivita());
      db.setString(ps, 5, getModelloPrelievo().getIdModelloPrelievo());
      rs = ps.executeQuery();
      if(rs.next()) {
        ret = rs.getString(PianoPrelieviTestataTM.ID_AZIENDA).trim();
      }
      if(ret != null) {
    	return true;  
      }
    }
    catch(Exception ex) {
      ex.printStackTrace(Trace.excStream);
    }
    finally {
      if (rs != null) {
        try {
          rs.close();
        }
        catch (SQLException  e) {
          e.printStackTrace(Trace.excStream);
        }
      }
    }
	return false;
  }
  //Fix 40182 -- Fine
  
  	// 37090 - Inizio
	//rendrere  il flag Cancel y del piano 
	
	protected Vector resetPianoPrelievoIntellimag(PianoPrelieviTestata pianoPrlTestata,boolean riportaDataGen) {
		Vector errors = new Vector();
		ErrorMessage errore = null;
		CfgLogTxPianoPrelievo filtroPianoPrl = null;
		CollegamentoThipIntellimag ctlv = (CollegamentoThipIntellimag) Factory.createObject(CollegamentoThipIntellimag.class);

		if (!GenerazioneListaPrelievo.testIntegrazionePanthLogis(TimeUtils.getCurrentDate())) {
			errors.add(new ErrorMessage("Mancante test trasmessione"));
			return errors;
		}

		filtroPianoPrl = CfgLogTxPianoPrelievo.trovaPianoPrelievoAbilitato(pianoPrlTestata.getRStabilimento(),pianoPrlTestata.getRReparto(), pianoPrlTestata.getIdAzienda());
		if (filtroPianoPrl == null) {
			errors.add(new ErrorMessage("LOGIS01054","Filtro mancante."));
			return errors;
		}
		errors = ctlv.salvaPianoPrelievoPth(pianoPrlTestata, filtroPianoPrl, true, riportaDataGen);
		return errors;
	}

	protected List<PianoPrelieviTestata> recuperaPianiPrelievo(ModelloPrelievo modelloPrelievo) {
		List<PianoPrelieviTestata> pianoPrlTestataList = new ArrayList<PianoPrelieviTestata>();
		String where = PianoPrelieviTestataTM.ID_AZIENDA + "='" + modelloPrelievo.getIdAzienda() + "' AND "
				+ PianoPrelieviTestataTM.ID_MODELLO_PRL + "='" + modelloPrelievo.getIdModelloPrelievo() + "'";
		try {
			pianoPrlTestataList = PianoPrelieviTestata.retrieveList(where, "", false);
			return pianoPrlTestataList;

		} catch (Exception e) {
			e.printStackTrace(Trace.excStream);
		}
		return pianoPrlTestataList;
	}
	
	protected PianoPrelieviTestata getPianoPrelievoTestata(WrapperPianoPrlDati wrPianoPrlDati) throws SQLException {
		String key = KeyHelper.buildObjectKey(new Object [] {wrPianoPrlDati.getIdAzienda(),getIdModelloPrelievoDaAssegnare(),PianoPrelieviTestata.TP_MOD_CORRENTE,wrPianoPrlDati.getIdAnnoOrd(),wrPianoPrlDati.getIdNumeroOrd(),wrPianoPrlDati.getIdRigaAttivita()});
		PianoPrelieviTestata pianoPrelieviTestata = PianoPrelieviTestata.elementWithKey(key, PersistentObject.NO_LOCK);
		return pianoPrelieviTestata;
	}
	//37090 - Fine
	
	
	//Fix 44367 - inizio
	protected String getWhereChiusuraListePrelievoPers() {
		return "";
	}
	
	
	protected String getWhereGenerazioneListePrlDaPianoPers() {
		return "";
	}
	//Fix 44367 - fine


	//Fix 45079 - inizio
	protected String getWhereChiusuraListePrelievoUBPers() {
		return "";
	}
	//Fix 45079 - fine
	
	//Fix 47561 -- Inizio
	public void setBatchJob(BatchJob batchJob) {
		iBatchJob = batchJob;
    }
	//Fix 47561 -- Fine

}
