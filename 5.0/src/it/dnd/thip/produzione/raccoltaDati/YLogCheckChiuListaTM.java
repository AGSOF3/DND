package it.dnd.thip.produzione.raccoltaDati;

import java.sql.SQLException;

import com.thera.thermfw.base.SystemParam;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.TableManager;

import it.thera.thip.cs.DatiComuniEstesiTTM;

public class YLogCheckChiuListaTM extends TableManager {

	public static final String ID_AZIENDA = "ID_AZIENDA";

	public static final String STATO = "STATO";

	public static final String R_UTENTE_CRZ = "R_UTENTE_CRZ";

	public static final String TIMESTAMP_CRZ = "TIMESTAMP_CRZ";

	public static final String R_UTENTE_AGG = "R_UTENTE_AGG";

	public static final String TIMESTAMP_AGG = "TIMESTAMP_AGG";

	public static final String RIF_RIGA_LISTA = "RIF_RIGA_LISTA";

	public static final String R_ARTICOLO = "R_ARTICOLO";

	public static final String QTA_RICHIESTA = "QTA_RICHIESTA";

	public static final String QTA_CONFEZIONATA = "QTA_CONFEZIONATA";

	public static final String QTA_RESIDUA = "QTA_RESIDUA";

	public static final String ANOMALIA = "ANOMALIA";

	public static final String ID_PROGRESSIVO = "ID_PROGRESSIVO";

	public static final String TABLE_NAME = SystemParam.getSchema("THIPPERS") + "YLOG_CHECK_CHIUSURA_LISTA";

	private static TableManager cInstance;

	private static final String CLASS_NAME = it.dnd.thip.produzione.raccoltaDati.YLogCheckChiuLista.class.getName();

	public synchronized static TableManager getInstance() throws SQLException {
		if (cInstance == null) {
			cInstance = (TableManager) Factory.createObject(YLogCheckChiuListaTM.class);
		}
		return cInstance;
	}

	public YLogCheckChiuListaTM() throws SQLException {
		super();
	}

	protected void initialize() throws SQLException {
		setTableName(TABLE_NAME);
		setObjClassName(CLASS_NAME);
		init();
	}

	protected void initializeRelation() throws SQLException {
		super.initializeRelation();
		addAttribute("RifRigaLista", RIF_RIGA_LISTA);
		addAttribute("QtaRichiesta", QTA_RICHIESTA);
		addAttribute("QtaConfezionata", QTA_CONFEZIONATA);
		addAttribute("QtaResidua", QTA_RESIDUA);
		addAttribute("Anomalia", ANOMALIA);
		addAttribute("IdProgressivo", ID_PROGRESSIVO, "getIntegerObject");
		addAttribute("IdAzienda", ID_AZIENDA);
		addAttribute("RArticolo", R_ARTICOLO);

		addComponent("DatiComuniEstesi", DatiComuniEstesiTTM.class);
		setKeys(ID_AZIENDA + "," + ID_PROGRESSIVO);

		setTimestampColumn("TIMESTAMP_AGG");
		((it.thera.thip.cs.DatiComuniEstesiTTM) getTransientTableManager("DatiComuniEstesi")).setExcludedColums();
	}

	private void init() throws SQLException {
		configure(RIF_RIGA_LISTA + ", " + QTA_RICHIESTA + ", " + QTA_CONFEZIONATA + ", " + QTA_RESIDUA + ", " + ANOMALIA
				+ ", " + ID_PROGRESSIVO + ", " + ID_AZIENDA + ", " + R_ARTICOLO + ", " + STATO + ", " + R_UTENTE_CRZ
				+ ", " + TIMESTAMP_CRZ + ", " + R_UTENTE_AGG + ", " + TIMESTAMP_AGG);
	}

}