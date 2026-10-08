package it.dnd.thip.base.codificatore;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.thera.thermfw.base.SystemParam;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.cbs.TextNLSRow;
import com.thera.thermfw.collector.BODataCollector;
import com.thera.thermfw.gui.cnr.OpenType;
import com.thera.thermfw.persist.ConnectionManager;

import it.dnd.thip.base.articolo.YArticoloBase;
import it.thera.thip.base.articolo.ArticoloBase;
import it.thera.thip.base.articolo.ArticoloDataCollector;
import it.thera.thip.base.articolo.ArticoloDatiIdent;
import it.thera.thip.base.articolo.ArticoloDatiStabil;
import it.thera.thip.base.articolo.SezioneBaseArticolo;
import it.thera.thip.base.codificatore.Codificatore;
import it.thera.thip.base.codificatore.ValoreVariabileCodifica;
import it.thera.thip.base.codificatore.VariabileSchemaCodifica;

/**
 * 
 * <h1>Softre Solutions</h1>
 * <br>
 * @author Andrea Gatta 05/12/2024
 * <br><br>
 * <b>71726	AGSOF3	05/12/2024</b>    <p>Setto nel campo note dell'articolo la descrizione estesa
 * 											generata dal codificatore</p>
 */

/*
 * Revisions:
 * Number	Date		Owner	Description
 * 71947	06/05/2025  AGSOF3  Fatta trim che lo standard non faceva
 * 72042	10/07/2025	AGSOF3	Setto sempre la classe E = 997 
 * 72111	08/09/2025	DSSOF3	Descrizione articolo template collegato allo schema di codifca nelle info header.
 * 72661	29/09/2026	AGSOF3	substring della descrizione quando viene settata in nls
 */

public class YCodificatore extends Codificatore {

	protected String iNote;

	public String getNote() {
		return iNote;
	}

	public void setNote(String iNote) {
		this.iNote = iNote;
	}

	@Override
	public int saveArticoloBase() {
		ArticoloDataCollector artBaseBoDC = (ArticoloDataCollector)getBoDC("" + SezioneBaseArticolo.DATI_INTERAZIENDALI);
		((YArticoloBase)artBaseBoDC.getBo()).getDescrizioneArticoloNLS().setDescrizioneLunga(getNote());
		int rc = super.saveArticoloBase();
		return rc;
	}

	/**
	 * 72042
	 * In tutti gli articoli che vengono creati tramite codificatore setto la classe E = 997
	 * codice scelto da Michele (cliente)
	 */
	@Override
	public int saveAllSezioni() {
		int rc = super.saveAllSezioni();
		if(rc >= 0) {
			ArticoloDataCollector artBaseBoDC = (ArticoloDataCollector)getBoDC("" + SezioneBaseArticolo.DATI_INTERAZIENDALI);
			Timestamp ts = ((ArticoloBase)artBaseBoDC.getBo()).getTimestamp();
			for(int i = 0; i < iSezioni.length; i++) {
				BODataCollector boDC = (BODataCollector)iBoDCMap.get("" + iSezioni[i]);
				if(boDC != null && !(boDC instanceof ArticoloDataCollector)) {
					boDC.setMode(OpenType.UPDATE);
					boDC.set("IdArticolo", iArticoloBase.getIdArticolo());
					boDC.set("IdAzienda", iArticoloBase.getIdAzienda());
					SezioneBaseArticolo bo = (SezioneBaseArticolo)boDC.getBo();
					bo.setAggiornamentoDatiGlobali(false);
					try {
						if(!(bo instanceof ArticoloDatiStabil))
							bo.setTimestamp(ts);
					}
					catch(SQLException e) {
						e.printStackTrace(Trace.excStream);
					}
					if(bo instanceof ArticoloDatiIdent) {
						ArticoloDatiIdent sez0 = ((ArticoloDatiIdent)bo);//.getArticolo();
						sez0.setIdClasseE("997");//classe E "Codice x vendita"
						try {
							if(sez0.save() >= 0)
								ConnectionManager.commit();
						} catch (SQLException e) {
							e.printStackTrace(Trace.excStream);
						}
					}			
				}
			}	
		}
		return rc;
	}


	/**
	 * Sovrascrive il metodo base per applicare un {@code trim()} al valore descrizione prima di invocare la logica standard.
	 * <p>
	 * Se {@code valoreDescr} è non nullo, vengono rimossi gli spazi iniziali e finali prima di passarlo al metodo della superclasse.
	 * Questo perchè lo standard non lo fa e poi avendo tutti gli spazi valorizzati la stringa è troppo lunga per essere settata
	 * nelle descrizioni dell'articolo
	 * </p>
	 *
	 * @param templateDescrizione template della descrizione da elaborare
	 * @param seq numero di sequenza
	 * @param valoreDescr valore della descrizione da includere nel template
	 * @return la descrizione risultante generata secondo il template
	 */
	@Override
	protected String getTemplateDescrizione(String templateDescrizione, Short seq, String valoreDescr) {
		if(valoreDescr != null) 
			valoreDescr = valoreDescr.trim();
		return super.getTemplateDescrizione(templateDescrizione, seq, valoreDescr);
	}

	/**
	 * Dopo che lo standard ha costruito codice e descrizioni teoriche:
	 * 1) compilo il campo note (personalizzato) con la descrizione estesa costruita dallo std
	 * 2) compilo la descrizione estesa con la descrizione template std non troncata a 35
	 * 3) sistemo le descrizioni nls
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void proponiChiave() {
		super.proponiChiave();
		String[] teorico = yCreaCodiceTeorico();
		this.setNote(getDescrEstesa());
		String templateDescrizione = teorico[1];
		getDescrizioneNLS().setDescrizioneEstesa(templateDescrizione);
		if(templateDescrizione != null && templateDescrizione.length() > 35)
			templateDescrizione = templateDescrizione.substring(0, 35);  
		getDescrizioneNLS().setDescrizione(templateDescrizione);
		setDescr(templateDescrizione);
		//72661 <
//		if(getDescrizioneNLS().getDescrizioniNLS().size() > 0) {
//			TextNLSRow a = (TextNLSRow) getDescrizioneNLS().getDescrizioniNLS().get(0);
//			a.setText(templateDescrizione, 0);
//		}
		
		for (TextNLSRow a : (List<TextNLSRow>) getDescrizioneNLS().getDescrizioniNLS()) {
		    a.setText(templateDescrizione, 0);
		}
		//72661 >
	}

	/**
	 * Copia del metodo standard solo per ottenere la descrizione template non troncata
	 * @return
	 */
	public String[] yCreaCodiceTeorico(){

		//		fillVariabili();
		//		creaSintesiCodifica();
		Set variabili = iVariabili.keySet();
		//fix 32024 inizio
		List lstDesc=new ArrayList();
		List lstDescEstesa=new ArrayList();
		List lstDescRidotta=new ArrayList();
		lstDesc.addAll(variabili);
		lstDescEstesa.addAll(variabili);
		lstDescRidotta.addAll(variabili);

		//		Collections.sort(lstDesc, new ComparatorSeqDescr());
		//		Collections.sort(lstDescEstesa, new ComparatorSeqDescrEstesa());
		//		Collections.sort(lstDescRidotta, new ComparatorSeqDescrRidotta());
		//fix 32024 Fine

		String templateCodice = getSchemaCodifica().getTemplCodice();
		String templateDescrizione = "";
		String templateDescrEstesa = "";
		String templateDescrRidotta = "";

		//...creo una descr. dal template in ciascuna lingua definito su SchemaCodifica
		List descrizioniNLS = getSchemaCodifica().getDescrizioneCodifNLS().getDescrizioniNLS();
		for(int j = 0; j < descrizioniNLS.size(); j++) {
			TextNLSRow row = (TextNLSRow)descrizioniNLS.get(j);
			templateDescrizione = getSchemaCodifica().getDescrizioneCodifNLS().getHandler().getText("TemplDescr", row.getLanguageId());
			templateDescrEstesa = getSchemaCodifica().getDescrizioneCodifNLS().getHandler().getText("TemplDescrEstesa", row.getLanguageId());
			templateDescrRidotta = getSchemaCodifica().getDescrizioneCodifNLS().getHandler().getText("TemplDescrRidotta", row.getLanguageId());

			//...creo una descr. dal template in ciascuna lingua definito sui valoriVarCod
			//Fix 32024 inizio
			//for(Iterator i = variabili.iterator(); i.hasNext(); ) {
			for(Iterator i = lstDesc.iterator(); i.hasNext(); ) {
				VariabileSchemaCodifica variabile = (VariabileSchemaCodifica)i.next();
				ValoreVariabileCodifica valore = (ValoreVariabileCodifica)iVariabili.get(variabile);
				if(valore != null) {
					if(j == 0)
						templateCodice = getTemplateCodice(templateCodice, variabile, valore);

					String valoreDesc = valore.getDescrizioneValVarCodifNLS().getHandler().getText("ValorePerDescr", row.getLanguageId());
					//String valoreDescRid = valore.getDescrizioneValVarCodifNLS().getHandler().getText("ValorePerDescrRidotta", row.getLanguageId());//32024
					//String valoreDescEst = valore.getDescrizioneValVarCodifNLS().getHandler().getText("ValorePerDescrEstesa", row.getLanguageId());//32024

					//Fix 05099 Mz inizio
					if(valoreDesc == null) {
						valoreDesc = valore.getDescrizioneValVarCodifNLS().getValorePerDescr();
					}
					/* 32024 commentato inizio
					if(valoreDescRid == null) {
						valoreDescRid = valore.getDescrizioneValVarCodifNLS().getValorePerDescrRidotta();
					}
					if(valoreDescEst == null) {
						valoreDescEst = valore.getDescrizioneValVarCodifNLS().getValorePerDescrEstesa();
					} Fix 32024 fine*/
					//Fix 05099 Mz fine
					templateDescrizione = getTemplateDescrizione(templateDescrizione, variabile.getSeqDescr(), valoreDesc == null ? "" : valoreDesc);
					//templateDescrRidotta = getTemplateDescrizione(templateDescrRidotta, variabile.getSeqDescrRidotta(), valoreDescRid == null ? "" : valoreDescRid);//32024
					//templateDescrEstesa = getTemplateDescrizione(templateDescrEstesa, variabile.getSeqDescrEstesa(), valoreDescEst == null ? "" : valoreDescEst);//32024
				}
			}
			//Fix 32024 inizio
			for(Iterator i = lstDescEstesa.iterator(); i.hasNext(); ) {
				VariabileSchemaCodifica variabile = (VariabileSchemaCodifica)i.next();
				ValoreVariabileCodifica valore = (ValoreVariabileCodifica)iVariabili.get(variabile);
				if(valore != null) {


					String valoreDescEst = valore.getDescrizioneValVarCodifNLS().getHandler().getText("ValorePerDescrEstesa", row.getLanguageId());
					if(valoreDescEst == null) {
						valoreDescEst = valore.getDescrizioneValVarCodifNLS().getValorePerDescrEstesa();
					}
					templateDescrEstesa = getTemplateDescrizione(templateDescrEstesa, variabile.getSeqDescrEstesa(), valoreDescEst == null ? "" : valoreDescEst);
				}
			}
			for(Iterator i = lstDescRidotta.iterator(); i.hasNext(); ) {
				VariabileSchemaCodifica variabile = (VariabileSchemaCodifica)i.next();
				ValoreVariabileCodifica valore = (ValoreVariabileCodifica)iVariabili.get(variabile);
				if(valore != null) {

					String valoreDescRid = valore.getDescrizioneValVarCodifNLS().getHandler().getText("ValorePerDescrRidotta", row.getLanguageId());

					if(valoreDescRid == null) {
						valoreDescRid = valore.getDescrizioneValVarCodifNLS().getValorePerDescrRidotta();
					}
					templateDescrRidotta = getTemplateDescrizione(templateDescrRidotta, variabile.getSeqDescrRidotta(), valoreDescRid == null ? "" : valoreDescRid);
				}
			}
			//Fix 32024 Fine 
			//...se è presente qualche &n non valorizzato (perchè la var non è in stato VALIDO), lo elimino
			templateDescrizione = eliminaParametriNonValorizzati(templateDescrizione);
			templateDescrRidotta = eliminaParametriNonValorizzati(templateDescrRidotta);
			templateDescrEstesa = eliminaParametriNonValorizzati(templateDescrEstesa);

			//			if(templateDescrizione != null && templateDescrizione.length() > 35) //...FIX03395 - DZ
			//				templateDescrizione = templateDescrizione.substring(0, 35);  //Fix 09138
			if(templateDescrRidotta != null && templateDescrRidotta.length() > 15)
				templateDescrRidotta = templateDescrRidotta.substring(0, 15);  //Fix 09138
			if(templateDescrEstesa != null && templateDescrEstesa.length() > 250)  //Fix 09279
				templateDescrEstesa = templateDescrEstesa.substring(0, 250);  //Fix 09279

			//...se è valorizzata la descrizione ma non la descr ridotta, la valorizzo con i primi 15 caratteri
			if(templateDescrizione != null && (templateDescrRidotta == null || templateDescrRidotta.equals(""))) {
				int lungh = templateDescrizione.length() < 15 ? templateDescrizione.length() : 15;  //Fix 09138
				templateDescrRidotta = templateDescrizione.substring(0, lungh);
			}

			String[] texts = new String[] {templateDescrizione, templateDescrRidotta, templateDescrEstesa};
			if(templateDescrizione != null || templateDescrRidotta != null || templateDescrEstesa != null) {
				TextNLSRow rowPropostaChiaveCodif = new TextNLSRow();
				getDescrizioneNLS().getHandler().setTextsForLanguage(texts, row.getLanguageId());
			}
		}


		//		String defaultTemplateDescrizione = getDescrizioneNLS().getHandler().getText("Descrizione", SystemParam.getFrameworkLocaleString());	// Fix 10972 Ryo
		String defaultTemplateDescrizione = getDescrizioneNLS().getHandler().getText("Descrizione", SystemParam.getCompanyDefaultLocale());	// Fix 10972 Ryo
		//		String defaultTemplateDescrRidotta = getDescrizioneNLS().getHandler().getText("DescrizioneRidotta", SystemParam.getFrameworkLocaleString());	// Fix 10972 Ryo
		String defaultTemplateDescrRidotta = getDescrizioneNLS().getHandler().getText("DescrizioneRidotta", SystemParam.getCompanyDefaultLocale());	// Fix 10972 Ryo
		//		String defaultTemplateDescrEstesa = getDescrizioneNLS().getHandler().getText("DescrizioneEstesa", SystemParam.getFrameworkLocaleString());	// Fix 10972 Ryo
		String defaultTemplateDescrEstesa = getDescrizioneNLS().getHandler().getText("DescrizioneEstesa", SystemParam.getCompanyDefaultLocale());	// Fix 10972 Ryo

		if(defaultTemplateDescrRidotta == null && defaultTemplateDescrizione != null)
			defaultTemplateDescrRidotta = defaultTemplateDescrizione.length() >= 15 ? defaultTemplateDescrizione.substring(0, 15) : defaultTemplateDescrizione.substring(0, defaultTemplateDescrizione.length());
			return new String[] {templateCodice, defaultTemplateDescrizione, defaultTemplateDescrEstesa, defaultTemplateDescrRidotta};

	}
	
	//72111 Inizio
	@Override
	public String getAltreInfoHeader() {
		if(getSchemaCodifica() != null && getSchemaCodifica().getTemplate() != null) {
			return super.getAltreInfoHeader() + getSchemaCodifica().getTemplate().getDescrizioneArticoloNLS().getDescrizioneEstesa();
		}
		return super.getAltreInfoHeader();
	}
	//72111 Fine

}