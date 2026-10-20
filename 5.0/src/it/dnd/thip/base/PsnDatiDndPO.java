package it.dnd.thip.base;

import java.sql.SQLException;
import java.util.List;
import java.util.Vector;

import com.thera.thermfw.common.BaseComponentsCollection;
import com.thera.thermfw.common.BusinessObject;
import com.thera.thermfw.common.Deletable;
import com.thera.thermfw.persist.Cacheable;
import com.thera.thermfw.persist.CopyException;
import com.thera.thermfw.persist.Copyable;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.KeyHelper;
import com.thera.thermfw.persist.OneToMany;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.persist.Proxy;
import com.thera.thermfw.persist.TableManager;
import com.thera.thermfw.security.Authorizable;
import com.thera.thermfw.security.Conflictable;

import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.base.azienda.Magazzino;
import it.thera.thip.base.generale.Serie;
import it.thera.thip.cs.EntitaAzienda;
import it.thera.thip.datiTecnici.modpro.Attivita;
import it.thera.thip.logis.fis.Ubicazione;

/**
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 16/07/2025
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 72049    16/07/2025  DSSOF3   Prima stesura
 * 72080	06/08/2025	DSSOF3   Implementazione cacheable, aggiunt campi
 * 72376	11/03/2026	DSSOF3   Aggiunta ubicazione scarico default.
 * 72702	20/10/2026	AGSOF3	 Aggiunta serie / causale doc generico riallineamento giacenze logis
 */

public abstract class PsnDatiDndPO extends EntitaAzienda implements BusinessObject, Authorizable, Deletable, Conflictable, Cacheable {

	private static PsnDatiDnd cInstance;

	protected Proxy iMagazzinoscartidp = new Proxy(it.thera.thip.base.azienda.Magazzino.class);

	//72080	Inizio
	protected Proxy iSerieODP = new Proxy(it.thera.thip.base.generale.Serie.class);
	protected Proxy iAttivitaODP = new Proxy(Attivita.class);
	//72080 Fine

	protected Proxy iUbicazioneScaricoDefault = new Proxy(Ubicazione.class); //72376

	protected OneToMany iCollegamentoMagPrlMagScr = new OneToMany(it.dnd.thip.base.CollMagPrlOperazioneMovim.class,this, 1, false);
	
	//72702 <
	protected String iNmrtDocGnrRiallLogis;
	protected String iSerieDocGnrRiallLogis;
	protected String iRCauDocGnrRiallLogis;
	//72702 >

	@SuppressWarnings("rawtypes")
	public static Vector retrieveList(String where, String orderBy, boolean optimistic)
			throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (cInstance == null)
			cInstance = (PsnDatiDnd) Factory.createObject(PsnDatiDnd.class);
		return PersistentObject.retrieveList(cInstance, where, orderBy, optimistic);
	}

	public static PsnDatiDnd elementWithKey(String key, int lockType) throws SQLException {
		return (PsnDatiDnd) PersistentObject.elementWithKey(PsnDatiDnd.class, key, lockType);
	}

	public PsnDatiDndPO() {
		setIdAzienda(Azienda.getAziendaCorrente());
	}

	public void setMagazzinoscartidp(Magazzino magazzinoscartidp) {
		String oldObjectKey = getKey();
		String idAzienda = getIdAzienda();
		if (magazzinoscartidp != null) {
			idAzienda = KeyHelper.getTokenObjectKey(magazzinoscartidp.getKey(), 1);
		}
		setIdAziendaInternal(idAzienda);
		this.iMagazzinoscartidp.setObject(magazzinoscartidp);
		setDirty();
		if (!KeyHelper.areEqual(oldObjectKey, getKey())) {
			setOnDB(false);
			iCollegamentoMagPrlMagScr.setFatherKeyChanged();
		}
	}

	public Magazzino getMagazzinoscartidp() {
		return (Magazzino) iMagazzinoscartidp.getObject();
	}

	public void setMagazzinoscartidpKey(String key) {
		String oldObjectKey = getKey();
		iMagazzinoscartidp.setKey(key);
		String idAzienda = KeyHelper.getTokenObjectKey(key, 1);
		setIdAziendaInternal(idAzienda);
		setDirty();
		if (!KeyHelper.areEqual(oldObjectKey, getKey())) {
			setOnDB(false);
			iCollegamentoMagPrlMagScr.setFatherKeyChanged();
		}
	}

	public String getMagazzinoscartidpKey() {
		return iMagazzinoscartidp.getKey();
	}

	public void setIdAzienda(String idAzienda) {
		setIdAziendaInternal(idAzienda);
		setDirty();
		setOnDB(false);
		iCollegamentoMagPrlMagScr.setFatherKeyChanged();
	}

	public String getIdAzienda() {
		String key = iAzienda.getKey();
		return key;
	}

	public void setIdMagScartiDichPrel(String idMagScartiDichPrel) {
		String key = iMagazzinoscartidp.getKey();
		iMagazzinoscartidp.setKey(KeyHelper.replaceTokenObjectKey(key, 2, idMagScartiDichPrel));
		setDirty();
	}

	public String getIdMagScartiDichPrel() {
		String key = iMagazzinoscartidp.getKey();
		String objIdMagScartiDichPrel = KeyHelper.getTokenObjectKey(key, 2);
		return objIdMagScartiDichPrel;
	}

	//72080 Inizio

	public void setSerieODP(Serie SerieODP) {
		String oldObjectKey = getKey();
		String idAzienda = getIdAzienda();
		if (SerieODP != null) {
			idAzienda = KeyHelper.getTokenObjectKey(SerieODP.getKey(), 1);
		}
		setIdAziendaInternal(idAzienda);
		this.iSerieODP.setObject(SerieODP);
		setDirty();
		if (!KeyHelper.areEqual(oldObjectKey, getKey())) {
			setOnDB(false);
		}
	}

	public Serie getSerieODP() {
		return (Serie)iSerieODP.getObject();
	}

	public void setSerieODPKey(String key) {
		String oldObjectKey = getKey();
		iSerieODP.setKey(key);
		String idAzienda = KeyHelper.getTokenObjectKey(key, 1);
		setIdAziendaInternal(idAzienda);
		setDirty();
		if (!KeyHelper.areEqual(oldObjectKey, getKey())) {
			setOnDB(false);
		}
	}

	public String getSerieODPKey() {
		return iSerieODP.getKey();
	}

	public void setIdNumeratoreODP(String IdNumeratoreODP) {
		String key = iSerieODP.getKey();
		iSerieODP.setKey(KeyHelper.replaceTokenObjectKey(key , 2, IdNumeratoreODP));
		setDirty();
	}

	public String getIdNumeratoreODP() {
		String key = iSerieODP.getKey();
		String objIdNumeratoreODP = KeyHelper.getTokenObjectKey(key,2);
		return objIdNumeratoreODP;    
	}

	public void setIdSerieODP(String IdSerieODP) {
		String key = iSerieODP.getKey();
		iSerieODP.setKey(KeyHelper.replaceTokenObjectKey(key , 3, IdSerieODP));
		setDirty();
	}

	public String getIdSerieODP() {
		String key = iSerieODP.getKey();
		String objIdSerieODP = KeyHelper.getTokenObjectKey(key,3);
		return objIdSerieODP;
	}

	public void setAttivitaODP(Attivita attivita) {
		this.iAttivitaODP.setObject(attivita);
		setDirty();
	}

	public Attivita getAttivitaODP() {
		return (Attivita) iAttivitaODP.getObject();
	}

	public void setAttivitaODPKey(String key) {
		iAttivitaODP.setKey(key);
		setDirty();
	}

	public String getAttivitaODPKey() {
		return iAttivitaODP.getKey();
	}

	public void setIdAttivitaODP(String idAttivitaODP) {
		String key = iAttivitaODP.getKey();
		iAttivitaODP.setKey(KeyHelper.replaceTokenObjectKey(key, 2, idAttivitaODP));
		setDirty();
	}

	public String getIdAttivitaODP() {
		String key = iAttivitaODP.getKey();
		String objIdAttivitaODP = KeyHelper.getTokenObjectKey(key, 2);
		return objIdAttivitaODP;
	}

	//72080 Fine

	//72376 <
	public Ubicazione getUbicazioneScaricoDefault() {
		return (Ubicazione) iUbicazioneScaricoDefault.getObject();
	}

	public void setUbicazioneScaricoDefault(Ubicazione u) {
		iUbicazioneScaricoDefault.setObject(u);
		setDirty();
	}

	public String getChiaveUbicazioneScaricoDefault(){
		return iUbicazioneScaricoDefault.getKey();
	}

	public void setChiaveUbicazioneScaricoDefault(String key){
		iUbicazioneScaricoDefault.setKey(key);
		setDirty();
	}

	public String getCodiceMagFisicoScarico(){
		String key = iUbicazioneScaricoDefault.getKey();
		return KeyHelper.getTokenObjectKey(key,1);
	}

	public void setCodiceMagFisicoScarico(String codice){
		String key = iUbicazioneScaricoDefault.getKey();
		String newKey = KeyHelper.replaceTokenObjectKey(key,1,codice);
		iUbicazioneScaricoDefault.setKey(newKey);
		setDirty();
	}

	public String getCodiceUbicazioneScarico(){
		String key = iUbicazioneScaricoDefault.getKey();
		return KeyHelper.getTokenObjectKey(key,2);
	}

	public void setCodiceUbicazioneScarico(String codice){
		String key = iUbicazioneScaricoDefault.getKey();
		String newKey = KeyHelper.replaceTokenObjectKey(key,2,codice);
		iUbicazioneScaricoDefault.setKey(newKey);
		setDirty();
	}
	//72376 >

	@SuppressWarnings("rawtypes")
	public List getCollegamentoMagPrlMagScr() {
		return getCollegamentoMagPrlMagScrInternal();
	}

	public void setEqual(Copyable obj) throws CopyException {
		super.setEqual(obj);
		PsnDatiDndPO psnDatiDndPO = (PsnDatiDndPO) obj;
		iMagazzinoscartidp.setEqual(psnDatiDndPO.iMagazzinoscartidp);
		iCollegamentoMagPrlMagScr.setEqual(psnDatiDndPO.iCollegamentoMagPrlMagScr);
		iSerieODP.setEqual(psnDatiDndPO.iSerieODP); //72080
		iAttivitaODP.setEqual(psnDatiDndPO.iAttivitaODP); //72080
	}

	public String getNmrtDocGnrRiallLogis() {
		return iNmrtDocGnrRiallLogis;
	}

	public void setNmrtDocGnrRiallLogis(String iNmrtDocGnrRiallLogis) {
		this.iNmrtDocGnrRiallLogis = iNmrtDocGnrRiallLogis;
	}

	public String getSerieDocGnrRiallLogis() {
		return iSerieDocGnrRiallLogis;
	}

	public void setSerieDocGnrRiallLogis(String iSerieDocGnrRiallLogis) {
		this.iSerieDocGnrRiallLogis = iSerieDocGnrRiallLogis;
	}

	public String getRCauDocGnrRiallLogis() {
		return iRCauDocGnrRiallLogis;
	}

	public void setRCauDocGnrRiallLogis(String iRCauDocGnrRiallLogis) {
		this.iRCauDocGnrRiallLogis = iRCauDocGnrRiallLogis;
	}

	@SuppressWarnings("rawtypes")
	public Vector checkAll(BaseComponentsCollection components) {
		Vector errors = new Vector();
		components.runAllChecks(errors);
		return errors;
	}

	public void setKey(String key) {
		setIdAzienda(key);
	}

	public String getKey() {
		return getIdAzienda();
	}

	public boolean isDeletable() {
		return checkDelete() == null;
	}

	public int saveOwnedObjects(int rc) throws SQLException {
		rc = iCollegamentoMagPrlMagScr.save(rc);
		return rc;
	}

	public int deleteOwnedObjects() throws SQLException {
		return getCollegamentoMagPrlMagScrInternal().delete();
	}

	public boolean initializeOwnedObjects(boolean result) {
		result = iCollegamentoMagPrlMagScr.initialize(result);
		return result;
	}

	public String toString() {
		return getClass().getName() + " [" + KeyHelper.formatKeyString(getKey()) + "]";
	}

	protected TableManager getTableManager() throws SQLException {
		return PsnDatiDndTM.getInstance();
	}

	protected OneToMany getCollegamentoMagPrlMagScrInternal() {
		if (iCollegamentoMagPrlMagScr.isNew())
			iCollegamentoMagPrlMagScr.retrieve();
		return iCollegamentoMagPrlMagScr;
	}

	protected void setIdAziendaInternal(String idAzienda) {
		iAzienda.setKey(idAzienda);
		String key2 = iMagazzinoscartidp.getKey();
		iMagazzinoscartidp.setKey(KeyHelper.replaceTokenObjectKey(key2, 1, idAzienda));
		iSerieODP.setKey(KeyHelper.replaceTokenObjectKey(iSerieODP.getKey(), 1, idAzienda)); //72080
		iAttivitaODP.setKey(KeyHelper.replaceTokenObjectKey(iAttivitaODP.getKey(), 1, idAzienda)); //72080
	}

}
