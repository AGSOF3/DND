package it.dnd.thip.produzione.raccoltaDati;

import java.math.BigDecimal;
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
import com.thera.thermfw.persist.Proxy;
import com.thera.thermfw.persist.TableManager;
import com.thera.thermfw.security.Authorizable;
import com.thera.thermfw.security.Conflictable;

import it.thera.thip.base.articolo.ArticoloBase;
import it.thera.thip.base.azienda.Azienda;
import it.thera.thip.cs.EntitaAzienda;

public abstract class YLogCheckChiuListaPO extends EntitaAzienda
		implements BusinessObject, Authorizable, Deletable, Conflictable {

	private static YLogCheckChiuLista cInstance;

	protected String iRifRigaLista;

	protected BigDecimal iQtaRichiesta;

	protected BigDecimal iQtaConfezionata;

	protected BigDecimal iQtaResidua;

	protected boolean iAnomalia = false;

	protected Integer iIdProgressivo;

	protected Proxy iArticolo = new Proxy(it.thera.thip.base.articolo.ArticoloBase.class);

	@SuppressWarnings("rawtypes")
	public static Vector retrieveList(String where, String orderBy, boolean optimistic)
			throws SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException {
		if (cInstance == null)
			cInstance = (YLogCheckChiuLista) Factory.createObject(YLogCheckChiuLista.class);
		return PersistentObject.retrieveList(cInstance, where, orderBy, optimistic);
	}

	public static YLogCheckChiuLista elementWithKey(String key, int lockType) throws SQLException {
		return (YLogCheckChiuLista) PersistentObject.elementWithKey(YLogCheckChiuLista.class, key, lockType);
	}

	public YLogCheckChiuListaPO() {
		setAnomalia(false);
		setIdProgressivo(new Integer(0));
		setIdAzienda(Azienda.getAziendaCorrente());
	}

	public void setRifRigaLista(String rifRigaLista) {
		this.iRifRigaLista = rifRigaLista;
		setDirty();
	}

	public String getRifRigaLista() {
		return iRifRigaLista;
	}

	public void setQtaRichiesta(BigDecimal qtaRichiesta) {
		this.iQtaRichiesta = qtaRichiesta;
		setDirty();
	}

	public BigDecimal getQtaRichiesta() {
		return iQtaRichiesta;
	}

	public void setQtaConfezionata(BigDecimal qtaConfezionata) {
		this.iQtaConfezionata = qtaConfezionata;
		setDirty();
	}

	public BigDecimal getQtaConfezionata() {
		return iQtaConfezionata;
	}

	public void setQtaResidua(BigDecimal qtaResidua) {
		this.iQtaResidua = qtaResidua;
		setDirty();
	}

	public BigDecimal getQtaResidua() {
		return iQtaResidua;
	}

	public void setAnomalia(boolean anomalia) {
		this.iAnomalia = anomalia;
		setDirty();
	}

	public boolean getAnomalia() {
		return iAnomalia;
	}

	public void setIdProgressivo(Integer idProgressivo) {
		this.iIdProgressivo = idProgressivo;
		setDirty();
		setOnDB(false);
	}

	public Integer getIdProgressivo() {
		return iIdProgressivo;
	}

	public void setArticolo(ArticoloBase articolo) {
		String oldObjectKey = getKey();
		String idAzienda = getIdAzienda();
		if (articolo != null) {
			idAzienda = KeyHelper.getTokenObjectKey(articolo.getKey(), 1);
		}
		setIdAziendaInternal(idAzienda);
		this.iArticolo.setObject(articolo);
		setDirty();
		if (!KeyHelper.areEqual(oldObjectKey, getKey())) {
			setOnDB(false);
		}
	}

	public ArticoloBase getArticolo() {
		return (ArticoloBase) iArticolo.getObject();
	}

	public void setArticoloKey(String key) {
		String oldObjectKey = getKey();
		iArticolo.setKey(key);
		String idAzienda = KeyHelper.getTokenObjectKey(key, 1);
		setIdAziendaInternal(idAzienda);
		setDirty();
		if (!KeyHelper.areEqual(oldObjectKey, getKey())) {
			setOnDB(false);
		}
	}

	public String getArticoloKey() {
		return iArticolo.getKey();
	}

	public void setIdAzienda(String idAzienda) {
		setIdAziendaInternal(idAzienda);
		setDirty();
		setOnDB(false);
	}

	public String getIdAzienda() {
		String key = iAzienda.getKey();
		return key;
	}

	public void setRArticolo(String rArticolo) {
		String key = iArticolo.getKey();
		iArticolo.setKey(KeyHelper.replaceTokenObjectKey(key, 2, rArticolo));
		setDirty();
	}

	public String getRArticolo() {
		String key = iArticolo.getKey();
		String objRArticolo = KeyHelper.getTokenObjectKey(key, 2);
		return objRArticolo;
	}

	public void setEqual(Copyable obj) throws CopyException {
		super.setEqual(obj);
		YLogCheckChiuListaPO yLogCheckChiuListaPO = (YLogCheckChiuListaPO) obj;
		iArticolo.setEqual(yLogCheckChiuListaPO.iArticolo);
	}

	@SuppressWarnings("rawtypes")
	public Vector checkAll(BaseComponentsCollection components) {
		Vector errors = new Vector();
		if (!isOnDB()) {
			setIdProgressivo(new Integer(0));
		}
		components.runAllChecks(errors);
		return errors;
	}

	public void setKey(String key) {
		setIdAzienda(KeyHelper.getTokenObjectKey(key, 1));
		setIdProgressivo(KeyHelper.stringToIntegerObj(KeyHelper.getTokenObjectKey(key, 2)));
	}

	public String getKey() {
		String idAzienda = getIdAzienda();
		Integer idProgressivo = getIdProgressivo();
		Object[] keyParts = { idAzienda, idProgressivo };
		return KeyHelper.buildObjectKey(keyParts);
	}

	public boolean isDeletable() {
		return checkDelete() == null;
	}

	public String toString() {
		return getClass().getName() + " [" + KeyHelper.formatKeyString(getKey()) + "]";
	}

	protected TableManager getTableManager() throws SQLException {
		return YLogCheckChiuListaTM.getInstance();
	}

	protected void setIdAziendaInternal(String idAzienda) {
		iAzienda.setKey(idAzienda);
		String key2 = iArticolo.getKey();
		iArticolo.setKey(KeyHelper.replaceTokenObjectKey(key2, 1, idAzienda));
	}

}