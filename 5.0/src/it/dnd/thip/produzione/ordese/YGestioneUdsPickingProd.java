package it.dnd.thip.produzione.ordese;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Vector;

import com.thera.thermfw.base.TimeUtils;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.common.*;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.Database;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.security.Security;
import com.thera.thermfw.security.User;

import it.dnd.thip.base.dipendente.YDipendente;
import it.dnd.thip.base.risorse.YMacchina;
import it.dnd.thip.tuttoimballo.stampanti.YInterfStampanti;
import it.thera.thip.base.articolo.ArticoloCliente;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.documenti.TipoDestinatario;
import it.thera.thip.base.generale.IntegrazioneThipLogis;
import it.thera.thip.base.profilo.ThipUser;
import it.thera.thip.base.profilo.UtenteAzienda;
import it.thera.thip.base.risorse.Risorsa;
import it.thera.thip.logis.bas.AbilUtente;
import it.thera.thip.logis.bas.ParametriLogis;
import it.thera.thip.logis.bas.Utente;
import it.thera.thip.logis.fis.OperazioneAllestimento;
import it.thera.thip.logis.fis.RigaUds;
import it.thera.thip.logis.fis.TestataUds;
import it.thera.thip.logis.fis.TipoUds;
import it.thera.thip.logis.lgb.RigaLista;
import it.thera.thip.logis.lgb.Societa;
import it.thera.thip.logis.prd.Gruppo;
import it.thera.thip.produzione.ordese.PersDatiPrdUtenteRilev;
import it.thera.thip.produzione.raccoltaDati.RilevazioneDatiProdTes;
import it.thera.thip.produzione.raccoltaDati.RilevazioneDatiProdTesTM;
import it.thera.thip.vendite.documentoVE.DocumentoVenRigaPrm;
import it.thera.thip.vendite.documentoVE.DocumentoVendita;

/**
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 02/05/2025
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 71946    02/05/2025  DSSOF3   Prima stesura
 * 72106	03/09/2025	DSSOF3	 Gestione nuovo campo IdProgressivoLista
 * 72262	18/12/2025	AGSOF3	 In etichetta cartone se presente cod art cli stampo quello
 * 72247	30/03/2026	AGSOF3	 Set articolo cliente nelle righe cartoni in stampa tutto x imballo
 * 72435	01/04/2026	AGSOF3	 Aggiunta stampante TT imballo su operatore, con priorita sulla macchina
 */

public class YGestioneUdsPickingProd extends YGestioneUdsPickingProdPO {

	//72106
	public static final String STMT_SELECT_NEXT_ID_PROGRESSIVO_LISTA = "SELECT "
			+ "	COALESCE(MAX(ID_PROGRESSIVO_LISTA),0) + 1 AS NEXT_ID_PROGRESSIVO_LISTA "
			+ "FROM "
			+ "	THIPPERS.YGESTIONE_UDS_PICKING_PROD "
			+ "WHERE "
			+ "	ID_AZIENDA = ? "
			+ "	AND R_COD_LISTA = ?";
	public static CachedStatement cNextIdProgressivoLista = new CachedStatement(STMT_SELECT_NEXT_ID_PROGRESSIVO_LISTA);
	//72106

	public ErrorMessage checkDelete() {
		if(getTestatauds() != null && getTestatauds().getStatoAllestimento() == TestataUds.CHIUSO) {
			return new ErrorMessage("BAS0000078","Non e' possibile cancellare l'oggetto, l'UDS collegata e' in stato CHIUSO");
		}
		return null;
	}

	//72106
	public synchronized static Integer nextIdProgressivoLista(String idAzienda, String codLista) throws SQLException {
		Integer id = null;
		ResultSet rs = null;
		try{
			PreparedStatement ps = cNextIdProgressivoLista.getStatement();
			Database db = ConnectionManager.getCurrentDatabase();
			db.setString(ps, 1, Azienda.getAziendaCorrente());
			db.setString(ps, 2, codLista);
			rs = ps.executeQuery();
			if (rs.next()){
				id = rs.getInt(1);
			}
		}finally{
			try{
				if(rs != null)
					rs.close();
			}catch(SQLException e){
				e.printStackTrace(Trace.excStream);
			}
		}
		return id;
	}
	//72106

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public TestataUds creaTestataUds() {
		TestataUds testata = (TestataUds) Factory.createObject(TestataUds.class);
		if(Utente.getCurrentUtente()!= null){
			if(Utente.getCurrentUtente().getCurrentMagFisico() != null)
				testata.setCodiceMagFisico(Utente.getCurrentUtente().getCurrentMagFisico().getCodice());
			testata.setOperatore(Utente.getCurrentUtente().getCurrentOperatore());
			testata.setPostazione(Utente.getCurrentUtente().getCurrentPostazione());
			if(Utente.getCurrentUtente().getCurrentPostazione() != null)
				testata.setAreaLavoro(Utente.getCurrentUtente().getCurrentPostazione().getAreaLavoro());
		}
		Vector vec = new Vector();
		try{
			vec.addAll(PersistentObject.readOnlyRetrieveList(TipoUds.class));
			//if ( vec.size() == 1 ){
			testata.setTipoUds(getTipouds());
			testata.setCodice(getIdUds());
			testata.setForma(getTipouds().getForma());
			testata.getDim().setVolumeIngombro(testata.getTipoUds().getDim().getVolumeIngombro());
			testata.getDim().setVolumeNetto(testata.getTipoUds().getDim().getVolumeNetto());
			testata.getDim().setLarghezza(testata.getTipoUds().getDim().getLarghezza());
			testata.getDim().setLunghezza(testata.getTipoUds().getDim().getLunghezza());
			testata.getDim().setProfondita(testata.getTipoUds().getDim().getProfondita());
			testata.getDim().setPesoMax(testata.getTipoUds().getDim().getPesoMax());
			testata.setTara(testata.getTipoUds().getTara());
			//}
			initSocieta(testata);
			vec = new Vector();
			vec.addAll(PersistentObject.readOnlyRetrieveList(OperazioneAllestimento.class));
			if ( vec.size() == 1 ){
				testata.setOperazioneAllestimento( (OperazioneAllestimento) vec.get(0));
			}
			if(!testata.retrieve()) {
				testata.setOnDB(false);
			}
		}catch (Exception ex) {
			ex.printStackTrace(Trace.excStream);
		}
		return testata;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public RigaUds creaRigaUds(TestataUds testata) throws SQLException {
		RigaUds riga = (RigaUds) Factory.createObject(RigaUds.class);
		riga.setCodiceSocieta(testata.getCodiceSocieta());
		if(getCodiceRigaUds() != null) {
			riga.setCodice(getCodiceRigaUds());
			riga.retrieve();
		}
		if(!riga.isOnDB()) {
			riga.setTestataUds(testata);
			Vector vec = new Vector();
			try {
				vec.addAll(PersistentObject.readOnlyRetrieveList(Gruppo.class));
				if(vec.size() == 1)
					riga.setCodiceGruppo(((Gruppo)vec.get(0)).getKey());
			}catch (Exception ex) {
				ex.printStackTrace(Trace.excStream);
			}
			riga.settaCodiceRigaUds();
			riga.setCodiceTestataLista(getIdCodiceLista());
			riga.setCodiceRigaLista(getIdCodiceRigaLista());
		}
		riga.setQta1Confermata(getQuantita());

		if(!riga.isOnDB()) {
			riga.setCodiceRigaLista(getIdCodiceRigaLista());
			riga.setCodiceArticolo(getIdArticolo());
			riga.setVersione(".");
		}
		return riga;
	}

	public void initUtente(){
		if(Utente.getCurrentUtente() != null)
			return;
		try{
			User user = Security.getCurrentUser();
			if (user != null)
				if (ParametriLogis.INSTALL_PANTHERA)
					Utente.setCurrentUtente((Utente)Utente.readOnlyElementWithKey(Utente.class, ((ThipUser) user).getUtenteAzienda().getIdUtente())); // Fix 15947
				else
					Utente.setCurrentUtente((Utente)Utente.readOnlyElementWithKey(Utente.class, user.getId())); // Fix 15947

			if(Utente.getCurrentUtente() != null){
				for (int i = 0; i < Utente.getCurrentUtente().getAbilUtenti().size() &&
						Utente.getCurrentUtente().getCurrentMagFisico() == null; i++) {
					AbilUtente a = (AbilUtente) Utente.getCurrentUtente().getAbilUtenti().
							get(i);
					if (a.getFlagDefault()) {
						Utente.getCurrentUtente().setCurrentMagFisico(a.getMagFisico());
						Utente.getCurrentUtente().setCurrentOperatore(a.getOperatore());
						Utente.getCurrentUtente().setCurrentPostazione(a.getPostazione());
					}
				}
			}
		}
		catch (Exception e){
			e.printStackTrace(Trace.excStream);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void initSocieta(TestataUds testata){
		try{
			User user = Security.getCurrentUser();
			if (user != null){
				String userAzienda = ((ThipUser)user).getUtenteAzienda().getIdAzienda();
				Vector integrazioni = new Vector();
				integrazioni.addAll(PersistentObject.readOnlyRetrieveList(IntegrazioneThipLogis.class));
				for (Iterator iter = integrazioni.iterator(); iter.hasNext(); ) {
					IntegrazioneThipLogis item = (IntegrazioneThipLogis) iter.next();
					if (item.getCodiceAziendaIC() == null || !item.getCodiceAziendaIC().equals(userAzienda))
						iter.remove();
				}
				if(integrazioni.size() > 0 )
					testata.setSocieta((Societa)Societa.readOnlyElementWithKey(Societa.class, ((IntegrazioneThipLogis)integrazioni.get(0)).getCodiceSocieta())); //Fix 15947
			}
		}catch (Exception e){
			e.printStackTrace(Trace.excStream);
		}
	}

	@SuppressWarnings("rawtypes")
	public Vector listaUdsSameCodeSameList() {
		String w = " "+YGestioneUdsPickingProdTM.ID_AZIENDA+" = '"+getIdAzienda()+"' "
				+ "AND "+YGestioneUdsPickingProdTM.R_COD_LISTA+" = '"+getIdCodiceLista()+"' "
				+ "AND "+YGestioneUdsPickingProdTM.R_UDS+" = '"+getIdUds()+"' "
				+ "AND NOT ("+YGestioneUdsPickingProdTM.R_UDS+" = '"+getIdUds()+"' "
				+ "AND "+YGestioneUdsPickingProdTM.NUMERO_RITORNO+" = '"+getNumeroRitorno()+"') ";
		Vector udsTutte = new Vector();
		try {
			udsTutte = YGestioneUdsPickingProd.retrieveList(w, " "+YGestioneUdsPickingProdTM.TIMESTAMP_CRZ+" ASC ", false);
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | SQLException e) {
			e.printStackTrace(Trace.excStream);
		}
		return udsTutte;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public YInterfStampanti recordLoftwareCartoni(String ip) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, ClassNotFoundException, InstantiationException, SQLException, NoSuchMethodException, SecurityException {
		YMacchina macchina = null;
		YInterfStampanti interfStampanti = (YInterfStampanti) Factory.createObject(YInterfStampanti.class);
		interfStampanti.setIdAzienda(getIdAzienda());
		String idCliente = null;
		RilevazioneDatiProdTes rlpt = null;

		String where = " "+RilevazioneDatiProdTesTM.ID_AZIENDA+" = '"+Azienda.getAziendaCorrente()+"'";
		where += " AND "+RilevazioneDatiProdTesTM.NUM_RITORNO+" = '"+getNumeroRitorno()+"'";
//		where += " AND "+RilevazioneDatiProdTesTM.STATO_RIL+" = '"+RilevazioneDatiProdTes.IN_CORSO+"'";

		Vector atvs = RilevazioneDatiProdTes.retrieveList(RilevazioneDatiProdTes.class, where, "", false);
		if(atvs.size() > 0) {
			rlpt = (RilevazioneDatiProdTes) atvs.get(0);
			if(rlpt.getLivelloRisorsa() == Risorsa.MATRICOLA) {
				macchina = (YMacchina) YMacchina.elementWithKey(YMacchina.class, 
						KeyHelper.buildObjectKey(new String[] {
								rlpt.getIdAzienda(),
								String.valueOf(rlpt.getTipoRisorsa()),
								String.valueOf(rlpt.getLivelloRisorsa()),
								rlpt.getIdRisorsa()
						}), PersistentObject.NO_LOCK);
				if(macchina != null)
					interfStampanti.setStampante(macchina.getIdStampante2Ti());
				//72435 <
				YDipendente operatore = (YDipendente) rlpt.getOperatore();
				if(operatore != null && operatore.getRStampante2Ti() != null) {
					interfStampanti.setStampante(operatore.getRStampante2Ti());
				}else {
					PersDatiPrdUtenteRilev utenteRilev = (PersDatiPrdUtenteRilev) Factory.createObject(PersDatiPrdUtenteRilev.class);
					utenteRilev.setIdAzienda(Azienda.getAziendaCorrente());
					UtenteAzienda ua = ((ThipUser) Security.getCurrentUser()).getUtenteAzienda();
					utenteRilev.setIdUtenteLgn(ua.getIdUtente());
					try {
						if(utenteRilev.retrieve() && utenteRilev.getOperatoreDefaultRel() != null) {
							YDipendente opeDefault = (YDipendente) utenteRilev.getOperatoreDefaultRel();
							if(opeDefault.getRStampante2Ti() != null)
								interfStampanti.setStampante(opeDefault.getRStampante2Ti());
						}
					}
					catch (SQLException ex) {
						ex.printStackTrace();
					}
				}
				//72435 >
			}
			if(rlpt.getOrdineEsecutivo().getIdCliente() != null)
				idCliente = rlpt.getOrdineEsecutivo().getIdCliente();
		}else {//se non ho la rilevazione vuol dire che sono in modalita ridotta
			try {
				YDipendente opeDefault = (YDipendente) Factory.createObject(YDipendente.class);
				String select = "SELECT y.ID_AZIENDA , y.ID_DIPENDENTE \r\n"
						+ "	FROM THIPPERS.YDIPENDENTI y \r\n"
						+ "	INNER JOIN THIPPERS.YBILANCIA_TUTTO_IMBALLO yti \r\n"
						+ "	ON y.ID_AZIENDA = yti.ID_AZIENDA \r\n"
						+ "	AND y.R_BILANCIA_TI = yti.ID_BILANCIA \r\n"
						+ "	WHERE yti.IP = '"+ip+"' ";
				CachedStatement cs = new CachedStatement(select);
				ResultSet rs = cs.executeQuery();
				if(rs.next()) {
					opeDefault.setIdAzienda(rs.getString(1));
					opeDefault.setIdDipendente(rs.getString(2));
				}
				if(opeDefault.retrieve()) {
					if(opeDefault.getRStampante2Ti() != null)
						interfStampanti.setStampante(opeDefault.getRStampante2Ti());
					macchina = (YMacchina) YMacchina.elementWithKey(YMacchina.class, 
							KeyHelper.buildObjectKey(new String[] {
									Azienda.getAziendaCorrente(),
									"M",
									"4",
									opeDefault.getNote()
							}), PersistentObject.NO_LOCK);
				}
			}catch (SQLException ex) {
				ex.printStackTrace();
			}
		}
		DocumentoVendita dv = (DocumentoVendita) Factory.createObject(DocumentoVendita.class);
		if (getIdCodiceLista().length() > 5) {
			dv.setIdAzienda(getIdAzienda());
			dv.setAnnoDocumento(getIdCodiceLista().substring(1, 5).trim());
			dv.setNumeroDocumento(getIdCodiceLista().substring(5).trim());
		}
		boolean res = false;
		try {
			res = dv.retrieve(PersistentObject.NO_LOCK);
		}
		catch (SQLException ex) {
			ex.printStackTrace(Trace.excStream);
		}
		if(res) {
			interfStampanti.setLayout(YInterfStampanti.getLayoutGerarchico(YInterfStampanti.LAYOUT_2, getIdAzienda(), getIdArticolo(), idCliente));
			if(getArticolo().getClasseA() != null) {
				interfStampanti.setCodiceArticolo(getArticolo().getIdClasseA());
			}else {
				interfStampanti.setCodiceArticolo(getArticolo().getIdArticolo());
			}
			String dsc = "";
			if(getArticolo().getDescrizioneArticoloNLS().getDescrizioneEstesa() != null) {
				dsc = getArticolo().getDescrizioneArticoloNLS().getDescrizioneEstesa().trim();
			}else {
				dsc = getArticolo().getDescrizioneArticoloNLS().getDescrizione().trim();
			}

			if(dsc != null && dsc.length() > 50) {
				dsc = dsc.substring(0,50);
			}
			if(idCliente == null && dv != null)
				idCliente = dv.getIdCliente();
			ArticoloCliente artCli = ArticoloCliente.getArticoloCliente(getIdAzienda(), idCliente, getIdArticolo(), null);
			if(artCli != null) {
				//72262 <
				if(artCli.getArticoloPerCliente() != null) {
					interfStampanti.setCodiceArticolo(artCli.getArticoloPerCliente());
				}
				//72262 >
				if(artCli.getDescrizioneEst().getDescrizioneEstesa() != null) {
					dsc = artCli.getDescrizioneEst().getDescrizioneEstesa();
				}else if(artCli.getDescrizioneEst().getDescrizione() != null) {
					dsc = artCli.getDescrizioneEst().getDescrizione();
				}
			}

			interfStampanti.setDescrizione(dsc);
			//interfStampanti.setExt1(peso.toString());
			interfStampanti.setNrCopie(1);

			interfStampanti.setCustomerRiga1(dv.getCliente().getRagioneSociale());

			String CAP = "";
			String idProvincia = "";
			String localita = "";
			String indirizzo = "";
			String ragioneSocialeDest = "";
			if (dv.getTipoDestinatario() == TipoDestinatario.NUMERO_INDIRIZZO && dv.getIndirizzo() != null) {
				localita = dv.getIndirizzo().getDatiIndirizzo().getLocalita();
				CAP = dv.getIndirizzo().getDatiIndirizzo().getCAP();
				idProvincia = dv.getIndirizzo().getDatiIndirizzo().getIdProvincia();
				ragioneSocialeDest = dv.getCliente().getRagioneSociale();
				indirizzo = dv.getIndirizzo().getDatiIndirizzo().getIndirizzo();
			}else if (dv.getTipoDestinatario() == TipoDestinatario.NUMERO_INDIRIZZO && dv.getIndirizzo() == null) {
				localita = dv.getCliente().getLocalita();
				CAP = dv.getCliente().getCAP();
				idProvincia = dv.getCliente().getIdProvincia();
				ragioneSocialeDest = dv.getCliente().getRagioneSociale();
				indirizzo = dv.getCliente().getIndirizzo();
			}
			if (dv.getTipoDestinatario() == TipoDestinatario.CLIENTE_VEN && dv.getClienteDestinatario() != null) {
				localita = dv.getClienteDestinatario().getLocalita();
				CAP = dv.getClienteDestinatario().getCAP();
				idProvincia = dv.getClienteDestinatario().getIdProvincia();
				ragioneSocialeDest = dv.getClienteDestinatario().getRagioneSociale();
				indirizzo = dv.getClienteDestinatario().getIndirizzo();
			}
			if (dv.getTipoDestinatario() == TipoDestinatario.MANUALE) {
				CAP = dv.getCAPDestinatario();
				localita = dv.getLocalitaDestinatario();
				idProvincia = dv.getIdProvinciaDen();
				indirizzo = dv.getIndirizzoDestinatario();
				ragioneSocialeDest = dv.getRagioneSocaleDest();
			}

			Vector udsTutte = listaUdsSameCodeSameList();

			udsTutte.add(this);
			int i = 1;
			for (Iterator iterator = udsTutte.iterator(); iterator.hasNext();) {
				YGestioneUdsPickingProd u = (YGestioneUdsPickingProd) iterator.next();

				RigaLista rl = (RigaLista) RigaLista.elementWithKey(RigaLista.class, KeyHelper.buildObjectKey(new String[] {
						u.getIdAzienda(), u.getIdCodiceLista(), u.getIdCodiceRigaLista().toString()
				}), PersistentObject.NO_LOCK);
				if(rl != null) {

					DocumentoVenRigaPrm docVenRig = (DocumentoVenRigaPrm) DocumentoVenRigaPrm.elementWithKey(DocumentoVenRigaPrm.class, KeyHelper.buildObjectKey(new String[] {
							dv.getIdAzienda(),dv.getAnnoDocumento(),dv.getNumeroDocumento(),rl.getNumeroRigaHost().toString()
					}), PersistentObject.NO_LOCK);

					if(docVenRig != null) {

						String setterDettCodRiga= "setDettCodRiga" + String.valueOf(i);
						String setterDettDescRiga = "setDettDescRiga" + String.valueOf(i);
						String setterDettExtRiga = "setDettExtRiga" + String.valueOf(i);
						String setterDettQtaRiga = "setDettQtaRiga" + String.valueOf(i);	

						Class c = Factory.getClass(interfStampanti.getClass());
						Method codMethod = c.getMethod(setterDettCodRiga, new Class[] { String.class });
						Method descMethod = c.getMethod(setterDettDescRiga, new Class[] { String.class });
						Method extMethod = c.getMethod(setterDettExtRiga, new Class[] { String.class });
						Method qtaMethod = c.getMethod(setterDettQtaRiga, new Class[] { String.class });

						String idArticolo = "";
						if(docVenRig.getArticolo().getClasseA() != null) {
							idArticolo = docVenRig.getArticolo().getIdClasseA();
						}else {
							idArticolo = docVenRig.getIdArticolo();
						}
						dsc = "";
						if(docVenRig.getArticolo().getDescrizioneArticoloNLS().getDescrizioneEstesa() != null) {
							dsc = docVenRig.getArticolo().getDescrizioneArticoloNLS().getDescrizioneEstesa().trim();
						}else {
							dsc = docVenRig.getArticolo().getDescrizioneArticoloNLS().getDescrizione().trim();
						}
						artCli = ArticoloCliente.getArticoloCliente(docVenRig.getIdAzienda(), idCliente, docVenRig.getIdArticolo(), null);
						if(artCli != null) {
							//72247 <
							if(artCli.getArticoloPerCliente() != null) {
								idArticolo = artCli.getArticoloPerCliente();
							}
							//72427 >
							if(artCli.getDescrizioneEst().getDescrizioneEstesa() != null) {
								dsc = artCli.getDescrizioneEst().getDescrizioneEstesa();
							}else if(artCli.getDescrizioneEst().getDescrizione() != null) {
								dsc = artCli.getDescrizioneEst().getDescrizione();
							}
						}

						if(dsc != null && dsc.length() > 50) {
							dsc = dsc.substring(0,50);
						}

						String nota = "";
						if(docVenRig.getNota() != null && !docVenRig.getNota().isEmpty()) {
							nota = docVenRig.getNota();
						}else if(docVenRig.getRigaOrdine() != null 
								&& docVenRig.getRigaOrdine().getNota() != null
								&& !docVenRig.getRigaOrdine().getNota().isEmpty()) {
							nota = docVenRig.getRigaOrdine().getNota();
						}

						codMethod.invoke(interfStampanti, new Object[] { idArticolo });
						descMethod.invoke(interfStampanti, new Object[] { dsc });
						extMethod.invoke(interfStampanti, new Object[] { nota });
						qtaMethod.invoke(interfStampanti, new Object[] { String.valueOf(u.getQuantita().intValue()) });

					}

					if(i == 6) { //.Non ho + posto per ora sul db
						break;
					}

					i++;
				}
			}

			interfStampanti.setDestCap(CAP);
			interfStampanti.setDestCitta(localita);
			interfStampanti.setDestProv(idProvincia);
			interfStampanti.setDestIndirizzo(indirizzo);
			interfStampanti.setDestRagSoc(ragioneSocialeDest);

			int id = getIdProgressivoLista();
			interfStampanti.setNrCollo(String.format("%06d", id));

			if(getPesoUds() != null)
				interfStampanti.setColloPesoLordo(getPesoUds().stripTrailingZeros().toPlainString());

			String barcodeCollo = "";
			if(macchina != null) {
				if(macchina.getIdRisorsa().equals("LIN01")) {
					barcodeCollo += "1";
				}else if(macchina.getIdRisorsa().equals("LIN02")) {
					barcodeCollo += "2";
				}

				barcodeCollo += "0000";
				barcodeCollo += dv.getNumeroDocumento().substring(4,10);
				barcodeCollo += "000";

				String idTipoUDS = getIdTipoUds().substring(1, getIdTipoUds().length());
				barcodeCollo += idTipoUDS;

				//char cd = calcolaCheckDigit(barcodeCollo);
				interfStampanti.setBarcodeIdLotto(barcodeCollo/* + cd*/);
			}

			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			interfStampanti.setData(sdf.format(TimeUtils.getCurrentDate()));
		}
		return interfStampanti;

	}
}