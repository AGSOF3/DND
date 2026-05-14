package it.dnd.thip.base.partner;

import java.sql.SQLException;
import java.util.Vector;

import com.thera.thermfw.common.BaseComponentsCollection;
import com.thera.thermfw.common.BusinessObject;
import com.thera.thermfw.common.Deletable;
import com.thera.thermfw.persist.CopyException;
import com.thera.thermfw.persist.Copyable;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.persist.TableManager;
import com.thera.thermfw.security.Authorizable;
import com.thera.thermfw.security.Conflictable;

import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.cs.EntitaAzienda;

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

public abstract class YSubcontinentePO extends EntitaAzienda
		implements BusinessObject, Authorizable, Deletable, Conflictable {

	private static YSubcontinente cInstance;

	protected String iIdSubcontinente;

	protected String iDescrizione;

	@SuppressWarnings("rawtypes")
	public static Vector retrieveList(String where, String orderBy, boolean optimistic)
			throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (cInstance == null)
			cInstance = (YSubcontinente) Factory.createObject(YSubcontinente.class);
		return PersistentObject.retrieveList(cInstance, where, orderBy, optimistic);
	}

	public static YSubcontinente elementWithKey(String key, int lockType) throws SQLException {
		return (YSubcontinente) PersistentObject.elementWithKey(YSubcontinente.class, key, lockType);
	}

	public YSubcontinentePO() {
		setIdAzienda(Azienda.getAziendaCorrente());
	}

	public void setIdSubcontinente(String idSubcontinente) {
		this.iIdSubcontinente = idSubcontinente;
		setDirty();
		setOnDB(false);
	}

	public String getIdSubcontinente() {
		return iIdSubcontinente;
	}

	public void setDescrizione(String descrizione) {
		this.iDescrizione = descrizione;
		setDirty();
	}

	public String getDescrizione() {
		return iDescrizione;
	}

	public void setIdAzienda(String idAzienda) {
		iAzienda.setKey(idAzienda);
		setDirty();
		setOnDB(false);
	}

	public String getIdAzienda() {
		String key = iAzienda.getKey();
		return key;
	}

	public void setEqual(Copyable obj) throws CopyException {
		super.setEqual(obj);
	}

	@SuppressWarnings("rawtypes")
	public Vector checkAll(BaseComponentsCollection components) {
		Vector errors = new Vector();
		components.runAllChecks(errors);
		return errors;
	}

	public void setKey(String key) {
		setIdAzienda(KeyHelper.getTokenObjectKey(key, 1));
		setIdSubcontinente(KeyHelper.getTokenObjectKey(key, 2));
	}

	public String getKey() {
		String idAzienda = getIdAzienda();
		String idSubcontinente = getIdSubcontinente();
		Object[] keyParts = { idAzienda, idSubcontinente };
		return KeyHelper.buildObjectKey(keyParts);
	}

	public boolean isDeletable() {
		return checkDelete() == null;
	}

	public String toString() {
		return getClass().getName() + " [" + KeyHelper.formatKeyString(getKey()) + "]";
	}

	protected TableManager getTableManager() throws SQLException {
		return YSubcontinenteTM.getInstance();
	}

}