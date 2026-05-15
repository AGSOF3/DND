package it.dnd.thip.base.partner;

import com.thera.thermfw.persist.CopyException;
import com.thera.thermfw.persist.Copyable;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.Proxy;

import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.partner.NazionePrimrose;

/**
* <p>
* Company: Softre Solutions<br>
* Author: Andrea Gatta<br>
* Date: 15/05/2026
* </p>
*/

/*
* Revisions:
* Number   Date        Owner    Description
* 72485	   15/05/2026  AGSOF3   Prima stesura
*/

public class YNazionePrimrose extends NazionePrimrose {

	protected Proxy iRelcontinente = new Proxy(it.dnd.thip.base.partner.YContinente.class);

	protected Proxy iRelsubcontinente = new Proxy(it.dnd.thip.base.partner.YSubcontinente.class);

	public YNazionePrimrose() {
		setIdAzienda(Azienda.getAziendaCorrente());
	}

	public void setRelcontinente(YContinente relcontinente) {
		String idAzienda = getIdAzienda();
		if (relcontinente != null) {
			idAzienda = KeyHelper.getTokenObjectKey(relcontinente.getKey(), 2);
		}
		setIdAziendaInternal(idAzienda);
		this.iRelcontinente.setObject(relcontinente);
		setDirty();
	}

	public YContinente getRelcontinente() {
		return (YContinente) iRelcontinente.getObject();
	}

	public void setRelcontinenteKey(String key) {
		iRelcontinente.setKey(key);
		String idAzienda = KeyHelper.getTokenObjectKey(key, 2);
		setIdAziendaInternal(idAzienda);
		setDirty();
	}

	public String getRelcontinenteKey() {
		return iRelcontinente.getKey();
	}

	public void setRContinente(String rContinente) {
		String key = iRelcontinente.getKey();
		iRelcontinente.setKey(KeyHelper.replaceTokenObjectKey(key, 1, rContinente));
		setDirty();
	}

	public String getRContinente() {
		String key = iRelcontinente.getKey();
		String objRContinente = KeyHelper.getTokenObjectKey(key, 1);
		return objRContinente;

	}

	public void setRelsubcontinente(YSubcontinente relsubcontinente) {
		String idAzienda = getIdAzienda();
		if (relsubcontinente != null) {
			idAzienda = KeyHelper.getTokenObjectKey(relsubcontinente.getKey(), 2);
		}
		setIdAziendaInternal(idAzienda);
		this.iRelsubcontinente.setObject(relsubcontinente);
		setDirty();
	}

	public YSubcontinente getRelsubcontinente() {
		return (YSubcontinente) iRelsubcontinente.getObject();
	}

	public void setRelsubcontinenteKey(String key) {
		iRelsubcontinente.setKey(key);
		String idAzienda = KeyHelper.getTokenObjectKey(key, 2);
		setIdAziendaInternal(idAzienda);
		setDirty();
	}

	public String getRelsubcontinenteKey() {
		return iRelsubcontinente.getKey();
	}

	public void setRSubcontinente(String rSubcontinente) {
		String key = iRelsubcontinente.getKey();
		iRelsubcontinente.setKey(KeyHelper.replaceTokenObjectKey(key, 1, rSubcontinente));
		setDirty();
	}

	public String getRSubcontinente() {
		String key = iRelsubcontinente.getKey();
		String objRSubcontinente = KeyHelper.getTokenObjectKey(key, 1);
		return objRSubcontinente;

	}

	public void setIdAzienda(String idAzienda) {
		setIdAziendaInternal(idAzienda);
		setDirty();
	}

	public String getIdAzienda() {
		String key = iRelcontinente.getKey();
		String objIdAzienda = KeyHelper.getTokenObjectKey(key, 2);
		return objIdAzienda;
	}

	public void setEqual(Copyable obj) throws CopyException {
		super.setEqual(obj);
		YNazionePrimrose yNazionePrimrose = (YNazionePrimrose) obj;
		iRelcontinente.setEqual(yNazionePrimrose.iRelcontinente);
		iRelsubcontinente.setEqual(yNazionePrimrose.iRelsubcontinente);
	}

	protected void setIdAziendaInternal(String idAzienda) {
		String key1 = iRelcontinente.getKey();
		iRelcontinente.setKey(KeyHelper.replaceTokenObjectKey(key1, 2, idAzienda));
		String key2 = iRelsubcontinente.getKey();
		iRelsubcontinente.setKey(KeyHelper.replaceTokenObjectKey(key2, 2, idAzienda));
	}

}