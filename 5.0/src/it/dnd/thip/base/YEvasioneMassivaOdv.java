package it.dnd.thip.base;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.batch.BatchRunnable;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.Column;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.CopyException;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.security.Authorizable;
import com.thera.thermfw.security.Entity;

import it.dnd.thip.base.articolo.YArticoloDatiProduz;
import it.dnd.thip.produzione.pianoPrelievi.YAzioniPianoPrelievo;
import it.thera.thip.base.articolo.ArticoloCliente;
import it.thera.thip.base.articolo.ArticoloClienteTM;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.comuniVenAcq.GestoreCalcoloCosti;
import it.thera.thip.base.comuniVenAcq.OrdineTestata;
import it.thera.thip.base.comuniVenAcq.QuantitaInUMRif;
import it.thera.thip.base.comuniVenAcq.StatoEvasione;
import it.thera.thip.base.comuniVenAcq.TipoCostoRiferimento;
import it.thera.thip.base.comuniVenAcq.TipoRiga;
import it.thera.thip.base.documenti.StatoAvanzamento;
import it.thera.thip.base.generale.PersDatiGen;
import it.thera.thip.base.prezziExtra.DocOrdRigaPrezziExtra;
import it.thera.thip.cs.DatiComuniEstesi;
import it.thera.thip.datiTecnici.modpro.AttivitaRisorsa;
import it.thera.thip.produzione.ordese.AttivitaEsecMateriale;
import it.thera.thip.produzione.ordese.AttivitaEsecProdotto;
import it.thera.thip.produzione.ordese.AttivitaEsecRisorsa;
import it.thera.thip.produzione.ordese.AttivitaEsecutiva;
import it.thera.thip.produzione.ordese.OrdineEsecutivo;
import it.thera.thip.servizi.comuniNlgCnt.UtilGener;
import it.thera.thip.vendite.documentoVE.DocumentoVenRigaPrm;
import it.thera.thip.vendite.documentoVE.DocumentoVendita;
import it.thera.thip.vendite.documentoVE.DocumentoVenditaTM;
import it.thera.thip.vendite.generaleVE.CausaleDocumentoVendita;
import it.thera.thip.vendite.generaleVE.CausaleRigaDocVen;
import it.thera.thip.vendite.generaleVE.CausaleRigaOrdVen;
import it.thera.thip.vendite.generaleVE.CondizioniCompatibilitaEvasione;
import it.thera.thip.vendite.generaleVE.PersDatiVen;
import it.thera.thip.vendite.ordineVE.GestoreEvasioneVendita;
import it.thera.thip.vendite.ordineVE.OrdineVendita;
import it.thera.thip.vendite.ordineVE.OrdineVenditaRiga;
import it.thera.thip.vendite.ordineVE.OrdineVenditaRigaPrm;
import it.thera.thip.vendite.ordineVE.OrdineVenditaRigaPrmTM;
import it.thera.thip.vendite.ordineVE.OrdineVenditaTM;
import it.thera.thip.vendite.ordineVE.OrdineVenditaTestata;
import it.thera.thip.vendite.prezziExtra.DocRigaPrezziExtraVendita;
import it.thera.thip.vendite.prezziExtra.OrdineRigaPrezziExtraVendita;

/**
 *
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 06/08/2025
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 72080    06/08/2025  DSSOF3   Prima stesura
 * 72177	20/10/2025	AGSOF3	Evado solo le righe con stato conferma evasione massiva = confermato
 * 72210	16/11/2025	AGSOF3	Evadi commenti riga ord ven
 */

public class YEvasioneMassivaOdv extends BatchRunnable implements Authorizable {

	protected int giorniOrizzonte;

	public static final String EVAS_MASSIVA_TRACE_SELECTOR = "YEMOD"; //.Selettore per trace avanzata tramite 'Abilitazione traccia'

	protected static final String STMT_ORD_VEN_RIG = "SELECT "
			+ "	* "
			+ "FROM "
			+ "	THIP.ORD_VEN_V03 V "
			+ " LEFT OUTER JOIN THIP.ORD_VEN_RIG ovr \r\n"
			+ "ON V.ID_AZIENDA = ovr.ID_AZIENDA  \r\n"
			+ "AND V.ID_ANNO_ORDINE = ovr.ID_ANNO_ORD \r\n"
			+ "AND  V.ID_NUMERO_ORD  = ovr.ID_NUMERO_ORD  \r\n"
			+ "AND  V.ID_RIGA_ORD  = ovr.ID_RIGA_ORD  \r\n"
			+ "AND  V.ID_DET_RIGA_ORD  = ovr.ID_DET_RIGA_ORD "
			+ "WHERE "
			+ "	V.ID_AZIENDA = ? "
			+ "	AND V.STATO_EVASIONE <> '"+StatoEvasione.SALDATO+"' "
			+ "	AND V.SALDO_MANUALE = '"+Column.FALSE_CHAR+"' "
			+ "	AND V.STATO_AVANZAMENTO  = '"+StatoAvanzamento.DEFINITIVO+"' "
			+ "	AND V.DATA_CONSEG_CFM IS NOT NULL"
			+ " AND V.R_CAU_RIG_ORDVEN NOT IN ('ACC')"
			+ " AND ("
			+ "        CASE"
			+ "		WHEN V.TIPO_RIGA = '"+TipoRiga.MERCE+"' "
			+ "             THEN V.QTA_RES_UM_PRM"
			+ "		ELSE V.QTA_RES_UM_VEN"
			+ "	END"
			+ "      ) > 0"
			+ "AND V.DATA_CONSEG_CFM < ( "
			+ "    SELECT DATEADD(DAY, ? +  "
			+ "                         (2 * ((? + DATEPART(WEEKDAY, GETDATE()) - 1) / 5)), GETDATE()) "
			+ ") "
			+ "AND ovr.FLAG_RIS_UTE_1 = '-' ";//72177 stato conferma evasione massiva su riga ordine = confermato
//				+ " AND ovr.ID_NUMERO_ORD IN ('OV  002106','OV  002107') AND ovr.ID_ANNO_ORD = '2026' ";
	public static CachedStatement cEstrazioneOrdVenRig = new CachedStatement(STMT_ORD_VEN_RIG);

	public int getGiorniOrizzonte() {
		return giorniOrizzonte;
	}

	public void setGiorniOrizzonte(int giorniOrizzonte) {
		this.giorniOrizzonte = giorniOrizzonte;
	}

	@Override
	protected boolean run() {
		boolean isOk = true;
		try {
			isOk = runGenerazione();
		}catch (Exception e) {
			output.println("Exc non gestita "+e.getMessage());
			isOk = false;
			e.printStackTrace(Trace.excStream);
		}
		return isOk;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected boolean runGenerazione() {
		boolean isOk = true;
		List righeOdv = estraiRigheOrdine();
		if(righeOdv.size() > 0) {
			Trace.printlnUserArea(Trace.US1, "Passo generazione DV/ODP", EVAS_MASSIVA_TRACE_SELECTOR);
			Iterator iterRighe = righeOdv.iterator();
			while(iterRighe.hasNext()) {
				OrdineVenditaRigaPrm ovr = (OrdineVenditaRigaPrm) iterRighe.next();
				OrdineVendita ovt = (OrdineVendita) ovr.getTestata();
				if(ovt.getStatoAvanzamento() == StatoAvanzamento.DEFINITIVO
						&& ovt.getStato() == DatiComuniEstesi.VALIDO 
						&& ovr.getStato() == DatiComuniEstesi.VALIDO) {
					Trace.printlnUserArea(Trace.US1, "Processo riga : "+ovr.getKey(), EVAS_MASSIVA_TRACE_SELECTOR);
					try {

						CausaleDocumentoVendita cauDocVen = YAzioniPianoPrelievo.getCausaleDocVen(ovt,
								ovt.getCliente(), null);
						if(cauDocVen != null) {
							DocumentoVendita docVen = null;
							//						if(ovt.getRaggruppamentoOrdBolla()) {
							docVen = cercaDocumentoVenditaAccorpabile(ovr, cauDocVen);
							//						}
							if(docVen == null) {
								docVen = YAzioniPianoPrelievo.creaDocumentoVendita(ovr);
							}
							DocumentoVenRigaPrm docRiga = trasformaRiga(ovr,docVen);
							docVen.getRighe().add(docRiga);
							docVen.setSalvataggioRigheForzato(true);
							/*if((docRiga.getServizioQta().getQuantitaInUMPrm() == null
								|| docRiga.getServizioQta().getQuantitaInUMPrm().compareTo(BigDecimal.ZERO) == 0)
								|| (docRiga.getServizioQta().getQuantitaInUMRif() == null
								|| docRiga.getServizioQta().getQuantitaInUMRif().compareTo(BigDecimal.ZERO) == 0)) {
							output.println("Impossibile creare la rig documento, quantita' zero, riga ordine "+ovr.getKey());
							continue;
						}*/
							int rc = 0;
							if(!docVen.isOnDB()) {
								rc = docVen.save();
							}else {
								rc = docRiga.save();
							}
							if(rc > 0) {
								if(docRiga.getStatoAvanzamento() == StatoAvanzamento.PROVVISORIO
										&& docRiga.getArticolo().getArticoloDatiProduz() instanceof YArticoloDatiProduz
										&& "MV".equals(((YArticoloDatiProduz)docRiga.getArticolo().getArticoloDatiProduz()).getIdUbicazioneVersamento())) {
									//Creo l'ordine esecutivo
									OrdineEsecutivo ordEsec = creaOrdineEsecutivo(docRiga);
									if(ordEsec != null) {
										rc = ordEsec.save();
										ordEsec.setStatoOrdineLav(OrdineEsecutivo.CONFERMATO);
										ordEsec.setStatoOrdine(OrdineEsecutivo.CONFERMATO);
										rc = ordEsec.save();
										if(rc < 0) {
											output.println("Impossibile salvare l'ordine esecutivo per la riga "+ovr.getKey()+" rc = "+rc);
										}
									}else {
										output.println("Impossibile creare l'ordine esecutivo per la riga "+ovr.getKey());
										rc = -1;
									}
								}
							}else {
								output.println("Impossibile salvare la riga documento rc = "+rc);
							}
							if(rc > 0) {
								ConnectionManager.commit();
							}else {
								ConnectionManager.rollback();
							}
						}else {
							output.println("Per l'ordine di vendita "+ovt.getNumeroDocumentoFormattato()+" non e' stata matchata nessuna causale DV");
							continue;
						}
					}catch (SQLException e) {
						e.printStackTrace(Trace.excStream);
					}
				}
			}
		}else {
			output.println("Nessuna riga ordine estratta");
		}
		return isOk;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected OrdineEsecutivo creaOrdineEsecutivo(DocumentoVenRigaPrm docRiga) {
		OrdineEsecutivo ordEsec = (OrdineEsecutivo) Factory.createObject(OrdineEsecutivo.class);
		ordEsec.setIdAzienda(docRiga.getIdAzienda());
		ordEsec.getNumeratoreHandler().setIdNumeratore(PsnDatiDnd.getCurrentPsnDatiDnd().getIdNumeratoreODP());
		ordEsec.getNumeratoreHandler().setIdSerie(PsnDatiDnd.getCurrentPsnDatiDnd().getIdSerieODP());
		ordEsec.setIdMagazzinoPrl("001");
		ordEsec.setIdMagazzinoVrs("001");
		ordEsec.setArticolo(docRiga.getArticolo());
		ordEsec.setIdVersione(docRiga.getIdVersioneRcs());
		ordEsec.setCommessa(docRiga.getCommessa());
		ordEsec.setWithModelloProduttivo("false");
		ordEsec.setStabilimento(PersDatiGen.getCurrentPersDatiGen().getStabilimento());
		ordEsec.getDescrizione().setDescrizione(docRiga.getArticolo().getDescrizioneArticoloNLS().getDescrizione());
		ordEsec.getDescrizione().setDescrizioneRidotta(docRiga.getArticolo().getDescrizioneArticoloNLS().getDescrizioneRidotta());

		//.Qta
		ordEsec.setQtaOrdinataUMPrm(docRiga.getServizioQta().getQuantitaInUMPrm());
		ordEsec.setQtaOrdinataUMSec(docRiga.getServizioQta().getQuantitaInUMSec());

		//.Date
		ordEsec.getDateRichieste().setStartDate(((OrdineVenditaRigaPrm)docRiga.getRigaOrdine()).getDataConsegnaConfermata());
		ordEsec.getDateRichieste().setEndDate(((OrdineVenditaRigaPrm)docRiga.getRigaOrdine()).getDataConsegnaConfermata());

		//.Riferimenti ordine cliente
		OrdineVendita ovt = (OrdineVendita) ((OrdineVenditaRigaPrm)docRiga.getRigaOrdine()).getTestata();
		ordEsec.setCliente(ovt.getCliente().getCliente());
		ordEsec.setAnnoOrdineCliente(ovt.getAnnoDocumento());
		ordEsec.setNumeroOrdineCliente(ovt.getNumeroDocumento());
		ordEsec.setRigaOrdineCliente(((OrdineVenditaRigaPrm)docRiga.getRigaOrdine()).getNumeroRigaDocumento());
		ordEsec.setDettaglioRigaOrdine(((OrdineVenditaRigaPrm)docRiga.getRigaOrdine()).getDettaglioRigaDocumento());

		//.Attivita esecutiva
		if(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP() != null) {
			AttivitaEsecutiva atvEsec = (AttivitaEsecutiva) Factory.createObject(AttivitaEsecutiva.class);
			atvEsec.setAzienda(ordEsec.getAzienda());
			atvEsec.setOrdineEsecutivo(ordEsec);
			atvEsec.setIdOperazione("0010");
			atvEsec.setAttivita(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP());
			atvEsec.getDescrizione().setDescrizione(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP().getDescrizione().getDescrizione());
			atvEsec.getDescrizione().setDescrizioneRidotta(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP().getDescrizione().getDescrizioneRidotta());
			atvEsec.setCentroLavoro(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP().getCentroLavoro());
			atvEsec.setCentroCosto(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP().getCentroCosto());
			atvEsec.setReparto(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP().getCentroLavoro().getReparto());
			atvEsec.setPoliticaConsAttivita(PsnDatiDnd.getCurrentPsnDatiDnd().getAttivitaODP().getPolConsAtt());
			//.Riporto le risorse da anagrafica
			if(atvEsec.getAttivita()!=null){
				List attivitaRisorsaList = atvEsec.getAttivita().getAttivitaRisorsaColl();
				for(int i=0;i<attivitaRisorsaList.size();i++)
				{
					AttivitaEsecRisorsa atvEsecRsr = (AttivitaEsecRisorsa)Factory.createObject(AttivitaEsecRisorsa.class);
					atvEsecRsr.initializeValues((AttivitaRisorsa)attivitaRisorsaList.get(i));
					atvEsec.getRisorse().add(atvEsecRsr);
				}
			}

			//.Creo il materiale congruo con quello della riga documento
			AttivitaEsecMateriale mat = (AttivitaEsecMateriale) Factory.createObject(AttivitaEsecMateriale.class);
			mat.setIdAzienda(Azienda.getAziendaCorrente());
			mat.setFather(atvEsec);
			mat.setMagazzinoPrelievo(ordEsec.getMagazzinoPrl());
			mat.setArticolo(docRiga.getArticolo());
			mat.setConfigurazione(docRiga.getConfigurazione());
			mat.setCoeffImpiego(BigDecimal.ONE);
			mat.getDescrizione().setDescrizione(docRiga.getArticolo().getDescrizioneArticoloNLS().getDescrizione());
			mat.getDescrizione().setDescrizioneRidotta(docRiga.getArticolo().getDescrizioneArticoloNLS().getDescrizioneRidotta());
			mat.setVersione(docRiga.getArticoloVersRichiesta());
			mat.setQtaRichiestaUMPrm (docRiga.getServizioQta().getQuantitaInUMPrm());
			mat.setQtaRichiestaUMSec(docRiga.getServizioQta().getQuantitaInUMSec());

			atvEsec.getMateriali().add(0,mat);

			//.Creo il prodotto congruo con quello della riga documento
			AttivitaEsecProdotto prd = (AttivitaEsecProdotto) Factory.createObject(AttivitaEsecProdotto.class);
			prd.setIdAzienda(Azienda.getAziendaCorrente());
			prd.setFather(atvEsec);
			prd.setTipoProdotto(AttivitaEsecProdotto.PRODOTTO_PRIMARIO);
			prd.setArticolo(docRiga.getArticolo());
			prd.setVersione(docRiga.getArticoloVersRichiesta());
			prd.setConfigurazione(docRiga.getConfigurazione());
			prd.getDescrizione().setDescrizione(docRiga.getArticolo().getDescrizioneArticoloNLS().getDescrizione());
			prd.getDescrizione().setDescrizioneRidotta(docRiga.getArticolo().getDescrizioneArticoloNLS().getDescrizioneRidotta());
			prd.setMagazzinoVersamento(ordEsec.getMagazzinoVrs());
			prd.setCoeffProduzione(BigDecimal.ONE);
			prd.setQtaRichiestaUMPrm (docRiga.getServizioQta().getQuantitaInUMPrm());
			prd.setQtaRichiestaUMSec(docRiga.getServizioQta().getQuantitaInUMSec());

			atvEsec.getProdotti().add(0,prd);

			atvEsec.getDatiComuniEstesi().setStato(DatiComuniEstesi.VALIDO);

			ordEsec.getAttivitaEsecutive().add(0,atvEsec);
		}else {
			return null;
		}

		ordEsec.getDatiComuniEstesi().setStato(DatiComuniEstesi.VALIDO);
		return ordEsec;
	}

	protected DocumentoVenRigaPrm trasformaRiga(OrdineVenditaRiga ordVenRig, DocumentoVendita docVen) {
		DocumentoVenRigaPrm docVenRig = (DocumentoVenRigaPrm) Factory.createObject(DocumentoVenRigaPrm.class);
		docVenRig.setIdAzienda(Azienda.getAziendaCorrente());
		docVenRig.setTestata(docVen);
		//docVenRig.setIdCauRig(docVen.getCausale().getIdCausaleRigaDocumVen());
		CausaleRigaDocVen cauRigaDoc = trovaCausaleRigaDocVen(docVen.getCausale(),(OrdineVenditaRigaPrm) ordVenRig);
		docVenRig.setCausaleRiga(cauRigaDoc);
		docVenRig.setStatoAvanzamento(StatoAvanzamento.PROVVISORIO);
		docVenRig.setIdArticolo(ordVenRig.getIdArticolo());		
		docVenRig.setIdUMSec(ordVenRig.getIdUMSec());
		docVenRig.completaBO();
		docVenRig.getServizioQta().setQuantitaInUMPrm(ordVenRig.getQuantitaResiduo().getQuantitaInUMPrm());
		docVenRig.setIdUMPrm(ordVenRig.getIdUMPrm());
		docVenRig.getServizioQta().setQuantitaInUMRif(ordVenRig.getQuantitaResiduo().getQuantitaInUMRif());
		docVenRig.setIdUMRif(ordVenRig.getIdUMRif());
		docVenRig.getServizioQta().setQuantitaInUMSec(ordVenRig.getQuantitaResiduo().getQuantitaInUMSec());
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
		docVenRig.setRAnnoOrd(ordVenRig.getAnnoDocumento());
		docVenRig.setRNumeroOrd(ordVenRig.getNumeroDocumento());
		docVenRig.setRRigaOrd(ordVenRig.getNumeroRigaDocumento());
		docVenRig.setRDetRigaOrd(ordVenRig.getDettaglioRigaDocumento());
		aggiornaAttributiDaRigaOrdine(docVenRig, ordVenRig);
		return docVenRig;
	}

	protected void aggiornaAttributiDaRigaOrdine(DocumentoVenRigaPrm docVenRig, OrdineVenditaRiga rigaOrdine) {
		// attributi valorizzati dalla riga ordine
		if (rigaOrdine != null) {
			//docVenRig.setRigaOrdine(rigaOrdine);
			docVenRig.setRAnnoOrd(rigaOrdine.getAnnoDocumento());
			docVenRig.setRNumeroOrd(rigaOrdine.getNumeroDocumento());
			docVenRig.setRRigaOrd(rigaOrdine.getNumeroRigaDocumento());
			//MG FIX 6754 inizio
			docVenRig.setSpecializzazioneRiga(rigaOrdine.getSpecializzazioneRiga());
			//MG FIX 6754 fine

			docVenRig.setSequenzaRiga(rigaOrdine.getSequenzaRiga());
			docVenRig.setTipoRiga(rigaOrdine.getTipoRiga());
			if (rigaOrdine.getTipoRiga() == TipoRiga.OMAGGIO) {
				docVenRig.setServizioCalcDatiVendita(false);
			}
			docVenRig.setMagazzino(rigaOrdine.getMagazzino());
			docVenRig.setArticolo(rigaOrdine.getArticolo());

			docVenRig.setIdCatalogoEsterno(rigaOrdine.getIdCatalogoEsterno());//Fix 34496 Inizio

			docVenRig.setArticoloVersSaldi(rigaOrdine.getArticoloVersSaldi());    

			docVenRig.setArticoloVersRichiesta(rigaOrdine.
					getArticoloVersRichiesta());
			docVenRig.setConfigurazione(rigaOrdine.getConfigurazione());
			//INI Fix 2920
			docVenRig.setDescrizioneArticoloPer(
					recuperaDescrizioneArticoloPerCLi(rigaOrdine));
			//FIN Fix 2920

			// Fix 1443
			docVenRig.setDescrizioneArticolo(rigaOrdine.
					getDescrizioneArticolo());
			// Fine fix 1443
			docVenRig.setDescrizioneExtArticolo(rigaOrdine.getDescrizioneExtArticolo());//Fix14727 RA
			docVenRig.setNota(rigaOrdine.getNota());
			docVenRig.setDocumentoMM(rigaOrdine.getDocumentoMM());
			docVenRig.setSpesa(rigaOrdine.getSpesa());
			docVenRig.setImportoPercentualeSpesa(rigaOrdine.
					getImportoPercentualeSpesa());
			docVenRig.setSpesaPercentuale(rigaOrdine.isSpesaPercentuale());
			docVenRig.setUMRifKey(rigaOrdine.getUMRifKey());
			docVenRig.setUMPrmKey(rigaOrdine.getUMPrmKey());
			docVenRig.setUMSecKey(rigaOrdine.getUMSecKey());

			//PJ fix 3741 inizio
			docVenRig.setRicalcoloQtaFattoreConv(rigaOrdine.
					isRicalcoloQtaFattoreConv());
			//PJ fix 3741 fine

			docVenRig.setCoefficienteImpiego(rigaOrdine.
					getCoefficienteImpiego());
			docVenRig.setBloccoRicalcoloQtaComp(rigaOrdine.
					isBloccoRicalcoloQtaComp());
			docVenRig.setTipoCostoRiferimento(rigaOrdine.
					getTipoCostoRiferimento());

			docVenRig.setDataConsegnaRichiesta(rigaOrdine.
					getDataConsegnaRichiesta());
			docVenRig.setDataConsegnaConfermata(rigaOrdine.
					getDataConsegnaConfermata());
			docVenRig.setSettConsegnaRichiesta(rigaOrdine.
					getSettConsegnaRichiesta());
			docVenRig.setSettConsegnaConfermata(rigaOrdine.
					getSettConsegnaConfermata());

			docVenRig.setStatoConfermaATP(rigaOrdine.getStatoConfermaATP());
			docVenRig.setDataPrevistaATP(rigaOrdine.getDataPrevistaATP());
			docVenRig.setDataTollerataATP(rigaOrdine.getDataTollerataATP());
			docVenRig.setIdListino(rigaOrdine.getIdListino());
			BigDecimal bd = GestoreEvasioneVendita.getBigDecimalZero();
			bd = rigaOrdine.getPrezzo() == null ?
					GestoreEvasioneVendita.getBigDecimalZero() : rigaOrdine.getPrezzo();
			docVenRig.setPrezzo(bd);
			bd = rigaOrdine.getPrezzoExtra() == null ?
					GestoreEvasioneVendita.getBigDecimalZero() :
						rigaOrdine.getPrezzoExtra();
			docVenRig.setPrezzoExtra(bd);
			bd = rigaOrdine.getPrezzoListino() == null ?
					GestoreEvasioneVendita.getBigDecimalZero() :
						rigaOrdine.getPrezzoListino();
			docVenRig.setPrezzoListino(bd);
			bd = rigaOrdine.getPrezzoExtraListino() == null ?
					GestoreEvasioneVendita.getBigDecimalZero() :
						rigaOrdine.getPrezzoExtraListino();
			docVenRig.setPrezzoExtraListino(bd);
			docVenRig.setRiferimentoUMPrezzo(rigaOrdine.
					getRiferimentoUMPrezzo());
			docVenRig.setTipoPrezzo(rigaOrdine.getTipoPrezzo());
			docVenRig.setBloccoRclPrzScnFatt(rigaOrdine.isBloccoRclPrzScnFatt());
			docVenRig.setProvenienzaPrezzo(rigaOrdine.getProvenienzaPrezzo());
			docVenRig.setTipoRigaListino(rigaOrdine.getTipoRigaListino());
			docVenRig.setAssoggettamentoIVA(rigaOrdine.getAssoggettamentoIVA());
			docVenRig.setResponsabileVendite(rigaOrdine.
					getResponsabileVendite());
			docVenRig.setScontoArticolo1(rigaOrdine.getScontoArticolo1());
			docVenRig.setScontoArticolo2(rigaOrdine.getScontoArticolo2());
			docVenRig.setMaggiorazione(rigaOrdine.getMaggiorazione());
			docVenRig.setSconto(rigaOrdine.getSconto());
			docVenRig.setPrcScontoIntestatario(rigaOrdine.
					getPrcScontoIntestatario());
			docVenRig.setPrcScontoModalita(rigaOrdine.getPrcScontoModalita());
			docVenRig.setScontoModalita(rigaOrdine.getScontoModalita());
			docVenRig.setAgente(rigaOrdine.getAgente());
			docVenRig.setProvvigione1Agente(rigaOrdine.getProvvigione1Agente());
			docVenRig.setProvvigione2Agente(rigaOrdine.getProvvigione2Agente());
			docVenRig.setSubagente(rigaOrdine.getSubagente());
			docVenRig.setProvvigione1Subagente(rigaOrdine.
					getProvvigione1Subagente());
			docVenRig.setProvvigione2Subagente(rigaOrdine.
					getProvvigione2Subagente());
			// 11053 LC >
			docVenRig.setDifferenzaPrezzoAgente(rigaOrdine.hasDifferenzaPrezzoAgente());
			docVenRig.setDifferenzaPrezzoSubagente(rigaOrdine.hasDifferenzaPrezzoSubagente());
			// 11053 LC <
			docVenRig.setCommessa(rigaOrdine.getCommessa());
			docVenRig.setCentroCosto(rigaOrdine.getCentroCosto());
			// Fix 8506 - Inizio
			if (rigaOrdine.getDatiCA() != null){
				docVenRig.getDatiCA().setVoceSpesaCA(rigaOrdine.getDatiCA().getVoceSpesaCA());
				docVenRig.getDatiCA().setVoceCA4(rigaOrdine.getDatiCA().getVoceCA4());
				docVenRig.getDatiCA().setVoceCA5(rigaOrdine.getDatiCA().getVoceCA5());
				docVenRig.getDatiCA().setVoceCA6(rigaOrdine.getDatiCA().getVoceCA6());
				docVenRig.getDatiCA().setVoceCA7(rigaOrdine.getDatiCA().getVoceCA7());
				docVenRig.getDatiCA().setVoceCA8(rigaOrdine.getDatiCA().getVoceCA8());
			}
			// Fix 8506 - Fine
			docVenRig.setGruppoContiAnalitica(rigaOrdine.
					getGruppoContiAnalitica());
			docVenRig.setFornitore(rigaOrdine.getFornitore());
			docVenRig.setStatoAccantonamentoPrenot(rigaOrdine.
					getStatoAccantonamentoPrenot());
			docVenRig.setPriorita(rigaOrdine.getPriorita());
			docVenRig.setStatoInvioEDI(rigaOrdine.getStatoInvioEDI());
			docVenRig.setStatoInvioDatawarehouse(rigaOrdine.
					getStatoInvioDatawarehouse());
			docVenRig.setStatoInvioLogistica(rigaOrdine.
					getStatoInvioLogistica());
			docVenRig.setStatoInvioContAnalitica(rigaOrdine.
					getStatoInvioContAnalitica());
			docVenRig.setNonFatturare(rigaOrdine.isNonFatturare()); //...FIX03015 - DZ

			//fix 2322
			docVenRig.setFlagRiservatoUtente1(rigaOrdine.
					getFlagRiservatoUtente1());
			docVenRig.setFlagRiservatoUtente2(rigaOrdine.
					getFlagRiservatoUtente2());
			docVenRig.setFlagRiservatoUtente3(rigaOrdine.
					getFlagRiservatoUtente3());
			docVenRig.setFlagRiservatoUtente4(rigaOrdine.
					getFlagRiservatoUtente4());
			docVenRig.setFlagRiservatoUtente5(rigaOrdine.
					getFlagRiservatoUtente5());
			docVenRig.setAlfanumRiservatoUtente1(rigaOrdine.
					getAlfanumRiservatoUtente1());
			docVenRig.setAlfanumRiservatoUtente2(rigaOrdine.
					getAlfanumRiservatoUtente2());
			docVenRig.setNumeroRiservatoUtente1(rigaOrdine.
					getNumeroRiservatoUtente1());
			docVenRig.setNumeroRiservatoUtente2(rigaOrdine.
					getNumeroRiservatoUtente2());
			//fine fix 2322

			// Inizio 8673
			// Inizio 3864
			docVenRig.setCostoUnitario(rigaOrdine.getCostoUnitario());
			PersDatiVen pdv = PersDatiVen.getCurrentPersDatiVen();
			if (pdv.getTipoCostoRiferimento() == TipoCostoRiferimento.COSTO_ULTIMO){
				GestoreCalcoloCosti gesCalcoloCosti = (GestoreCalcoloCosti)Factory.createObject(GestoreCalcoloCosti.class);
				gesCalcoloCosti.initialize(rigaOrdine.getIdAzienda(), rigaOrdine.getIdArticolo(), rigaOrdine.getIdVersioneSal(),
						rigaOrdine.getIdConfigurazione(), rigaOrdine.getIdMagazzino());
				gesCalcoloCosti.impostaCostoUnitario();
				docVenRig.setCostoUnitario(gesCalcoloCosti.getCostoUnitario());
			}
			// Fine 3864
			// Fine 8673

			//Fix 2333
			if (rigaOrdine.getRigaPrezziExtra() != null) {
				docVenRig.istanziaRigaPrezziExtra();
				if (docVenRig.getRigaPrezziExtra() != null) {
					DocRigaPrezziExtraVendita rigaPrezzi = (DocRigaPrezziExtraVendita)docVenRig.getRigaPrezziExtra();
					DocOrdRigaPrezziExtra rigaPrezziOrd = rigaOrdine.getRigaPrezziExtra();
					rigaPrezzi.setAnnoContrattoMateriaPrima(rigaPrezziOrd.
							getAnnoContrattoMateriaPrima());
					rigaPrezzi.setIdAzienda(rigaPrezziOrd.getIdAzienda());
					rigaPrezzi.setIdRigaCondizione(rigaPrezziOrd.getIdRigaCondizione());
					rigaPrezzi.setIdSchemaPrezzo(rigaPrezziOrd.getIdSchemaPrezzo());
					rigaPrezzi.setNumContrattoMateriaPrima(rigaPrezziOrd.
							getNumContrattoMateriaPrima());
					// fix 3016
					rigaPrezzi.setRAnnoCantiere( ( (OrdineRigaPrezziExtraVendita)
							rigaPrezziOrd).getRAnnoCantiere());
					rigaPrezzi.setRNumeroCantiere( ( (OrdineRigaPrezziExtraVendita)
							rigaPrezziOrd).getRNumeroCantiere());
					rigaPrezzi.setRRigaCantiere( ( (OrdineRigaPrezziExtraVendita)
							rigaPrezziOrd).getRRigaCantiere());
					// fine fix 3016
					//Fix 3769 BP ini...
					rigaPrezzi.setPrezzoRiferimento( ( (OrdineRigaPrezziExtraVendita)
							rigaPrezziOrd).getPrezzoRiferimento());
					//Fix 3769 BP fine.
					try {
						rigaPrezzi.getPrezziExtra().setEqual(rigaPrezziOrd.getPrezziExtra());
					}
					catch (Exception ex) {
						ex.printStackTrace();
					}
					rigaPrezzi.setAnnoOrdine(rigaOrdine.getAnnoDocumento());
					rigaPrezzi.setNumeroOrdine(rigaOrdine.getNumeroDocumento());
					rigaPrezzi.setIdRigaOrdine(rigaOrdine.getNumeroRigaDocumento());
					rigaPrezzi.setIdDetRigaOrdine(rigaOrdine.getDettaglioRigaDocumento());
				}
			}
			// Fine fix 2333
			// Fix 04324 EP ini
			if (docVenRig.getRigaOrdine() != null) {
				OrdineVenditaTestata ordVen = (OrdineVenditaTestata)docVenRig.getRigaOrdine().getTestata();
				if (ordVen.getTipoEvasioneOrdine() == OrdineTestata.SALDO_AUTOMATICO) {
					docVenRig.setRigaSaldata(true);
				}
			}
			// Fix 04324 EP fin
			// Inizio 7548
			docVenRig.setRifCommessaCli(rigaOrdine.getRifCommessaCli());
			// Fine 7548

			// fix 11120 >
			// fix 11779 >
			//docVenRig.setNumeroImballo(rigaOrdine.getNumeroImballo());
			// fix 11779 <
			docVenRig.setDifferenzaPrezzoAgente(rigaOrdine.hasDifferenzaPrezzoAgente());
			docVenRig.setDifferenzaPrezzoSubagente(rigaOrdine.hasDifferenzaPrezzoSubagente());
			// fix 11120 <

			// aggiorna QTA
			aggiornaQtaEvasione(docVenRig,rigaOrdine);

			docVenRig.setPrezzoNetto(rigaOrdine.getPrezzoNetto());//Fix PM 18662
			// fix 32086
			docVenRig.setDecimaleEDI1(rigaOrdine.getDecimaleEDI1());
			docVenRig.setDecimaleEDI2(rigaOrdine.getDecimaleEDI2());
			docVenRig.setStringaEDI1(rigaOrdine.getStringaEDI1());
			docVenRig.setStringaEDI2(rigaOrdine.getStringaEDI2());
			// fine fix 32086

			//Fix 33428 - inizio
			docVenRig.setIdAnnoRichServ(rigaOrdine.getIdAnnoRichServ());
			docVenRig.setIdNumeroRichServ(rigaOrdine.getIdNumeroRichServ());
			docVenRig.setIdBene(rigaOrdine.getIdBene());
			//Fix 33428 - fine
			//72210 <
			Entity entity = null;
			try {
				entity = Entity.elementWithKey("DocVendita",Entity.NO_LOCK);
			} catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
			}
			UtilGener.copiaCommenti(rigaOrdine.getCommentHandler(), docVenRig.getCommentHandler(), entity);
			//72210 >
		}
	}

	protected void aggiornaQtaEvasione(DocumentoVenRigaPrm docVenRig,OrdineVenditaRiga rigaOrdine) {
		QuantitaInUMRif qtaZeroInUMRif = new QuantitaInUMRif();
		qtaZeroInUMRif.azzera();
		//QuantitaInUMRif qtaAttesaIniziale = qtaZeroInUMRif;
		//QuantitaInUMRif qtaPropostaIniziale = qtaZeroInUMRif;
		QuantitaInUMRif qtaDaSpedireIniziale = qtaZeroInUMRif;

		if (docVenRig.isOnDB()) {
			// nulla
		}
		else {
			// nuova riga estratta da una riga ordine
			if (rigaOrdine != null) {
				try {
					if ( ( (DocumentoVendita)docVenRig.getTestata()).isDocumentoDiReso()) {
						qtaDaSpedireIniziale.setEqual(rigaOrdine.getQuantitaSpedita());
					}
					else {
						qtaDaSpedireIniziale.setEqual(getQtaResidua((DocumentoVendita) docVenRig.getTestata(), rigaOrdine));
					}
				}
				catch (CopyException cex) {
					cex.printStackTrace(Trace.excStream);
				}
			}
		}

		// QTA Proposta / Attesa
		if (docVenRig.getStatoAvanzamento() == StatoAvanzamento.DEFINITIVO) {
			docVenRig.setQtaAttesaEvasione(qtaDaSpedireIniziale);
		}
		else if (docVenRig.getStatoAvanzamento() ==
				StatoAvanzamento.PROVVISORIO) {
			docVenRig.setQtaPropostaEvasione(qtaDaSpedireIniziale);
		}
	}

	public QuantitaInUMRif getQtaResidua(DocumentoVendita docVen, OrdineVenditaRiga rigaOrdine) {
		QuantitaInUMRif qtaResiduo = new QuantitaInUMRif();
		qtaResiduo.azzera();
		if (rigaOrdine != null) {
			//BigDecimal zero = GestoreEvasioneVendita.getBigDecimalZero();
			try {
				if (docVen.isDocumentoDiReso()) {
					qtaResiduo.setEqual(rigaOrdine.getQuantitaSpedita());
				}
				else {
					qtaResiduo.setEqual(rigaOrdine.getQuantitaResiduo()); //fix 3134
				}
			}
			catch (CopyException cx) {
			}
		}
		return qtaResiduo;
	}

	@SuppressWarnings("rawtypes")
	private String recuperaDescrizioneArticoloPerCLi(OrdineVenditaRiga rigaOrdine) {
		List objList = null;

		try {
			String whereClause = ArticoloClienteTM.ID_AZIENDA + "='" +
					rigaOrdine.getIdAzienda() + "'"
					+ " AND " + ArticoloClienteTM.ID_ARTICOLO + "='" +
					rigaOrdine.getIdArticolo()
					+ "'" + " AND " + ArticoloClienteTM.ID_CLIENTE + "='" +
					rigaOrdine.getIdCliente() + "'";

			if (rigaOrdine.getIdConfigurazione() != null) {
				whereClause += " AND " + ArticoloClienteTM.ID_CONFIGURAZIONE + " = " +
						rigaOrdine.getIdConfigurazione();
			}
			else {
				whereClause += " AND " + ArticoloClienteTM.ID_CONFIGURAZIONE +
						" IS NULL";
			}

			objList = ArticoloCliente.retrieveList(whereClause,
					ArticoloClienteTM.ARTICOLO_CLI, false);

		}
		catch (Exception e) {
			e.printStackTrace();
		}

		String descrizione = null;
		Iterator descIte = objList.iterator();
		while (descIte.hasNext()) {
			ArticoloCliente art = (ArticoloCliente) descIte.next();
			String desc = art.getArticoloPerCliente();
			descrizione = desc;

		}

		return descrizione;

	}

	protected DocumentoVendita cercaDocumentoVenditaAccorpabile(OrdineVenditaRiga ordVenRig,CausaleDocumentoVendita cauDocVen) {
		Trace.printlnUserArea(Trace.US1, "Ricerca documento accorpabile: "+ordVenRig.getKey(), EVAS_MASSIVA_TRACE_SELECTOR);
		DocumentoVendita docVen = null;
		OrdineVendita ordVen =  (OrdineVendita) ordVenRig.getTestata();
		CondizioniCompatibilitaEvasione condComp = GestoreEvasioneVendita.get().inizializzaCondizioniCompatibilita(ordVenRig.getIdAzienda());

		String trovaDocVenAccodabile =
				//				" SELECT dvt." + DocumentoVenditaTM.ID_AZIENDA + ", dvt." + DocumentoVenditaTM.ID_ANNO_DOC + ", dvt." + DocumentoVenditaTM.ID_NUMERO_DOC
				//				+ " FROM " + DocumentoVenditaTM.TABLE_NAME +
				//				" WHERE " + DocumentoVenditaTM.ID_AZIENDA + " = '"+ordVenRig.getIdAzienda()+"' " +
				//				" AND " + DocumentoVenditaTM.R_CLIENTE + " = '"+ordVen.getIdCliente()+"' " + 
				//				" AND " + DocumentoVenditaTM.STATO_AVANZAMENTO + " = '"+cauDocVen.getStatoAvanzamento()+"' " +
				//				" AND "+DocumentoVenditaTM.COL_MAGAZZINO+" <> '"+StatoAttivita.ESEGUITO+"' " ;
				"SELECT\r\n"
				+ "	dvt.ID_AZIENDA,\r\n"
				+ "	dvt.ID_ANNO_DOC,\r\n"
				+ "	dvt.ID_NUMERO_DOC\r\n"
				+ "FROM\r\n"
				+ "	THIP.DOC_VEN_TES dvt\r\n"
				+ "LEFT OUTER JOIN THIP.ORD_VEN_TES ovt \r\n"
				+ "ON\r\n"
				+ "	dvt.ID_AZIENDA = ovt.ID_AZIENDA\r\n"
				+ "	AND dvt.R_ANNO_ORD = ovt.ID_ANNO_ORDINE\r\n"
				+ "	AND dvt.R_NUMERO_ORD = ovt.ID_NUMERO_ORD\r\n"
				+ "WHERE\r\n"
				+ "	dvt.ID_AZIENDA = '"+ordVenRig.getIdAzienda()+"'\r\n"
				+ "	AND dvt.R_CLIENTE = '"+ordVenRig.getIdCliente()+"'\r\n"
				+ "	AND dvt.STATO_AVANZAMENTO = '"+cauDocVen.getStatoAvanzamento()+"'\r\n"
				+ "	AND dvt.COL_MAGAZZINO <> '2'";
		//71994
		if(ordVenRig.getDataConsegnaConfermata() != null)
			trovaDocVenAccodabile += " AND dvt."+DocumentoVenditaTM.DATA_CONSEG_CFM+" = "+ConnectionManager.getCurrentDatabase().getLiteral(ordVenRig.getDataConsegnaConfermata())+" "; //Fix 72020 rimuovere singoli apici

		if(condComp.isGesDestinatario()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_DEN_ABT;
			if(ordVen.getIdDenAbt() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdDenAbt()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";

			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_SEQUENZA_IND;
			if(ordVen.getIdSequenzaInd() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdSequenzaInd()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";

			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.RAGIONE_SOC_DEN;
			if(ordVen.getRagioneSocaleDest() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getRagioneSocaleDest()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";

			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.INDIRIZZO_DEN;
			if(ordVen.getIndirizzoDestinatario() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIndirizzoDestinatario()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";

			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.LOCALITA_DEN;
			if(ordVen.getLocalitaDestinatario() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getLocalitaDestinatario().replace("'", "''")+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesAssIVA()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_ASSOG_IVA;
			if(ordVen.getIdAssogIva() != null)
				trovaDocVenAccodabile +=  " = '"+ordVen.getIdAssogIva()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesModalitaSpedizione()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_MOD_SPEDIZIONE;
			if(ordVen.getIdModSpedizione() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdModSpedizione()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesAgenteSubagente()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_AGENTE;
			if(ordVen.getIdAgente() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdAgente()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesBanca()) {
			if(ordVen.getIdentificativoBanca() != null && ordVen.getIdentificativoBanca().getIdABI() != null) {
				trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_ABI + " = '"+ordVen.getIdentificativoBanca().getIdABI()+"' " + 
						" AND dvt." + DocumentoVenditaTM.R_CAB + " = '"+ordVen.getIdentificativoBanca().getIdCAB()+"' ";
			}else {
				trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_ABI + " IS NULL " + 
						" AND dvt." + DocumentoVenditaTM.R_CAB + " IS NULL ";
			}
		}

		if(condComp.isGesLineaCredito()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.ID_LINEA_CREDITO;
			if(ordVen.getIdLineaCredito() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdLineaCredito()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesClienteFatturazione()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_CLIENTE_FAT;
			if(ordVen.getIdClienteFat() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdClienteFat()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesRappresentanteFiscale()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_RAPPRES_FISC;
			if(ordVen.getIdRappresentanteFisc() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdRappresentanteFisc()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesModalitaConsegna()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_MOD_CONSEGNA;
			if(ordVen.getIdModConsegna() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdModConsegna()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesCommessa()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_COMMESSA;
			if(ordVen.getIdCommessa() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdCommessa()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesValuta()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_VALUTA;
			if(ordVen.getIdValuta() != null)
				trovaDocVenAccodabile += " = '"+ordVen.getIdValuta()+"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesInizioPagamento()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.DATA_INIZIO_PAG;
			if(ordVen.getDataInizioPagamento() != null)
				trovaDocVenAccodabile += " = '"+ ConnectionManager.getCurrentDatabase().getLiteral(ordVen.getDataInizioPagamento()) +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesModalitaPagamento()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.R_MOD_PAGAMENTO;
			if(ordVen.getIdModPagamento() != null)
				trovaDocVenAccodabile += " = '"+ ordVen.getIdModPagamento() +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesScontoFineFattura()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.SCONTO_FINE_FATT;
			if(ordVen.getPrcScontoFineFattura() != null)
				trovaDocVenAccodabile += " = '"+ ordVen.getPrcScontoFineFattura() +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}

		if(condComp.isGesVettori()) {
			trovaDocVenAccodabile += " AND dvt." + DocumentoVenditaTM.SCONTO_FINE_FATT;
			if(ordVen.getPrcScontoFineFattura() != null)
				trovaDocVenAccodabile += " = '"+ ordVen.getPrcScontoFineFattura() +"' ";
			else
				trovaDocVenAccodabile += " IS NULL ";
		}		

		if(PersDatiVen.getCurrentPersDatiVen() != null) {
			if(PersDatiVen.getCurrentPersDatiVen().isAblCauVenDDT()) {
				trovaDocVenAccodabile += " AND (ovt.R_CAU_ORD_VEN = '"+ordVen.getIdCau()+"' or ovt.R_CAU_ORD_VEN IS NULL)";
			}
		}


		//raggruppamento ordine
		trovaDocVenAccodabile += "AND (ovt.RAG_ORD_BOLLA = 'Y'\r\n"
				+ "		OR \r\n"
				+ "		( \r\n"
				+ "		ovt.RAG_ORD_BOLLA = 'N'\r\n"
				+ "			AND \r\n"
				+ "		ovt.ID_ANNO_ORDINE = '"+ordVen.getAnnoDocumento()+"'\r\n"
				+ "			AND \r\n"
				+ "		ovt.ID_NUMERO_ORD = '"+ordVen.getNumeroDocumento()+"' )\r\n"
				+ " OR (ovt.RAG_ORD_BOLLA IS NULL) "
				+ "		) ";

		Trace.printlnUserArea(Trace.US1, "Ricerca documento accorpabile query: "+trovaDocVenAccodabile, EVAS_MASSIVA_TRACE_SELECTOR);
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

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected List estraiRigheOrdine() {
		List righe = new ArrayList();
		ResultSet rs = null;
		try {
			Trace.printlnUserArea(Trace.US1, cEstrazioneOrdVenRig.getStmtString(), EVAS_MASSIVA_TRACE_SELECTOR);
			PreparedStatement ps = cEstrazioneOrdVenRig.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, Azienda.getAziendaCorrente());
			ps.setInt(2, getGiorniOrizzonte());
			ps.setInt(3, getGiorniOrizzonte());
			rs = ps.executeQuery();
			while(rs.next()) {
				righe.add(OrdineVenditaRigaPrm.elementWithKey(OrdineVenditaRigaPrm.class, KeyHelper.buildObjectKey(new String[] {
						rs.getString(OrdineVenditaRigaPrmTM.ID_AZIENDA),
						rs.getString(OrdineVenditaTM.ID_ANNO_ORDINE),
						rs.getString(OrdineVenditaRigaPrmTM.ID_NUMERO_ORD),
						rs.getString(OrdineVenditaRigaPrmTM.ID_RIGA_ORD)
				}), PersistentObject.NO_LOCK));
			}
		}catch (Throwable tx) {
			tx.printStackTrace(Trace.excStream);
		}finally {
			if (rs != null) {
				try {
					rs.close();
				}catch (SQLException sql) {
					sql.printStackTrace(Trace.excStream);
				}
			}
		}
		return righe;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public CausaleRigaDocVen trovaCausaleRigaDocVen(CausaleDocumentoVendita causale, OrdineVenditaRigaPrm
			rigaOrdine) {
		CausaleRigaOrdVen cauRigaOrd = (CausaleRigaOrdVen) rigaOrdine.
				getCausaleRiga();
		List causaliRigaDoc = new ArrayList();
		// pongo come prina causale quella eventualmente selezionata come default
		List causaliRigaDocTemp = causale.getCausaliRiga();
		CausaleRigaDocVen cauRigaDocDefault = causale.getCausaleRigaDocumVen();
		int idx = 0;
		String cauRigaDocDefaultKey = "";

		if (cauRigaDocDefault != null &&
				cauRigaDocDefault.getDatiComuniEstesi().getStato() ==
				DatiComuniEstesi.VALIDO) {
			causaliRigaDoc.add(idx, cauRigaDocDefault);
			cauRigaDocDefaultKey = cauRigaDocDefault.getKey();
			idx++;
		}
		Iterator iterDocTemp = causaliRigaDocTemp.iterator();
		while (iterDocTemp.hasNext()) {
			CausaleRigaDocVen cauObj = (CausaleRigaDocVen) iterDocTemp.next();
			if (!cauObj.getKey().equals(cauRigaDocDefaultKey) &&
					cauObj.getDatiComuniEstesi().getStato() == DatiComuniEstesi.VALIDO) {
				causaliRigaDoc.add(idx, cauObj);
				idx++;
			}
		}
		// cerca la causale appropriata
		CausaleRigaDocVen cauRigaDoc = null;
		boolean isTrovata = false;
		cauRigaDoc = trovaCausaleRigaDocVen(rigaOrdine, causaliRigaDoc, true);
		if (cauRigaDoc == null)
			cauRigaDoc = trovaCausaleRigaDocVen(rigaOrdine, causaliRigaDoc, false);
		isTrovata = (cauRigaDoc != null) ? true : false;
		//Fix 16542 fine
		if (!isTrovata) {
			cauRigaDoc = null;
			GestoreEvasioneVendita.get().print(
					"causale riga doc non trovata su causale riga ord : '" +
							cauRigaOrd.getKey() + "'");
			GestoreEvasioneVendita.get().println(" con causale doc : '" +
					causale.getKey() + "'");
		}
		else {
			GestoreEvasioneVendita.get().println("causale riga doc usata ='" +
					causale.getKey() +
					"' su causale riga ord : '" +
					cauRigaOrd.getKey() + "'");
		}
		return cauRigaDoc;
	}

	//-------------------------- Fix 16542 inizio
	@SuppressWarnings("rawtypes")
	public CausaleRigaDocVen trovaCausaleRigaDocVen(OrdineVenditaRigaPrm rigaOrdine, List causaliRigaDoc, boolean conTestSuGestioneCatalogo) {
		CausaleRigaDocVen cauRigaDoc = null;
		boolean isTrovata = false;
		boolean isForzaControllo = false;
		CausaleRigaOrdVen cauRigaOrd = (CausaleRigaOrdVen) rigaOrdine.getCausaleRiga();
		Iterator iterDoc = causaliRigaDoc.iterator();
		if (cauRigaOrd != null) {
			if (rigaOrdine.getTipoRiga() == TipoRiga.SPESE_MOV_VALORE) {
				CausaleRigaDocVen cauDaSpesa = null;
				if (rigaOrdine.getSpesa() != null) {
					cauDaSpesa = (CausaleRigaDocVen) rigaOrdine.getSpesa().
							getCausaleRigaDocVen();
				}
				if (cauDaSpesa != null) {
					while (iterDoc.hasNext() && !isTrovata) {
						CausaleRigaDocVen cau = (CausaleRigaDocVen) iterDoc.next();
						if (cauDaSpesa.getKey().equals(cau.getKey())) {
							cauRigaDoc = cau;
							isTrovata = true;
						}
					}
					if (!isTrovata) {
						isForzaControllo = true;
					}
				}
				else {
					isForzaControllo = true;
				}
			}
			iterDoc = causaliRigaDoc.iterator();
			if (rigaOrdine.getTipoRiga() == TipoRiga.OMAGGIO) {
				boolean isROOmaggioSA = rigaOrdine.getCausaleRiga().isOmaggioScontoArticolo();
				while (iterDoc.hasNext() && !isTrovata) {
					CausaleRigaDocVen cau = (CausaleRigaDocVen) iterDoc.next();
					if (cau.getTipoRiga() == TipoRiga.OMAGGIO) {
						if (isROOmaggioSA == cau.isOmaggioScontoArticolo()) {
							Iterator i = cau.getCausaliRigaOrdine().iterator();
							while (i.hasNext() && !isTrovata) {
								CausaleRigaOrdVen cro = (CausaleRigaOrdVen) i.next();
								if (cro.getKey().equals(cauRigaOrd.getKey())) {
									if (!conTestSuGestioneCatalogo || (cro.isCatalogoEstPerOrdineAbl() == cau.isCatalogoEstPerOrdineAbl()) )
									{
										isTrovata = true;
										cauRigaDoc = cau;
									}
								}
							}
						}
					}
				}
			}
			else if (rigaOrdine.getTipoRiga() == TipoRiga.MERCE ||
					rigaOrdine.getTipoRiga() == TipoRiga.SERVIZIO ||
					isForzaControllo) {
				while (iterDoc.hasNext() && !isTrovata) {
					cauRigaDoc = (CausaleRigaDocVen) iterDoc.next();
					List causaliRigaOrd = cauRigaDoc.getCausaliRigaOrdine();
					Iterator iterOrd = causaliRigaOrd.iterator();
					while (iterOrd.hasNext() && !isTrovata) {
						CausaleRigaOrdVen cau = (CausaleRigaOrdVen) iterOrd.next();
						if (cau.getKey().equals(cauRigaOrd.getKey())) {
							if (!conTestSuGestioneCatalogo || (cau.isCatalogoEstPerOrdineAbl() == cauRigaDoc.isCatalogoEstPerOrdineAbl()))
							{
								isTrovata = true;
							}
						}
					}
				}
			}
		}
		if (!isTrovata && isForzaControllo) {
			Iterator iterDocUltima = causaliRigaDoc.iterator();
			while (iterDocUltima.hasNext()) {
				cauRigaDoc = (CausaleRigaDocVen) iterDocUltima.next();
				if (cauRigaDoc.getTipoRiga() == rigaOrdine.getTipoRiga()) {
					isTrovata = true;
					break;
				}
			}
		}
		return (isTrovata) ? cauRigaDoc : null;
	}

	@Override
	protected String getClassAdCollectionName() {
		return "YEvasMassODV";
	}

}
