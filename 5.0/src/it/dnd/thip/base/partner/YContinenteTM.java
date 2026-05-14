package it.dnd.thip.base.partner;

import java.sql.SQLException;

import com.thera.thermfw.base.SystemParam;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.TableManager;

import it.thera.thip.cs.DatiComuniEstesiTTM;

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
* 72485	   06/08/2025  AGSOF3   Prima stesura
*/

public class YContinenteTM extends TableManager {

	public static final String ID_AZIENDA = "ID_AZIENDA";

	public static final String STATO = "STATO";

	public static final String R_UTENTE_CRZ = "R_UTENTE_CRZ";

	public static final String TIMESTAMP_CRZ = "TIMESTAMP_CRZ";

	public static final String R_UTENTE_AGG = "R_UTENTE_AGG";

	public static final String TIMESTAMP_AGG = "TIMESTAMP_AGG";

	public static final String ID_CONTINENTE = "ID_CONTINENTE";

	public static final String DESCRIZIONE = "DESCRIZIONE";

	public static final String TABLE_NAME = SystemParam.getSchema("THIPPERS") + "YCONTINENTE";

	private static TableManager cInstance;

	private static final String CLASS_NAME = it.dnd.thip.base.partner.YContinente.class.getName();

	public synchronized static TableManager getInstance() throws SQLException {
		if (cInstance == null) {
			cInstance = (TableManager) Factory.createObject(YContinenteTM.class);
		}
		return cInstance;
	}

	public YContinenteTM() throws SQLException {
		super();
	}

	protected void initialize() throws SQLException {
		setTableName(TABLE_NAME);
		setObjClassName(CLASS_NAME);
		init();
	}

	protected void initializeRelation() throws SQLException {
		super.initializeRelation();
		addAttribute("IdContinente", ID_CONTINENTE);
		addAttribute("Descrizione", DESCRIZIONE);
		addAttribute("IdAzienda", ID_AZIENDA);

		addComponent("DatiComuniEstesi", DatiComuniEstesiTTM.class);
		setKeys(ID_AZIENDA + "," + ID_CONTINENTE);

		setTimestampColumn("TIMESTAMP_AGG");
		((it.thera.thip.cs.DatiComuniEstesiTTM) getTransientTableManager("DatiComuniEstesi")).setExcludedColums();
	}

	private void init() throws SQLException {
		configure(ID_CONTINENTE + ", " + DESCRIZIONE + ", " + ID_AZIENDA + ", " + STATO + ", " + R_UTENTE_CRZ + ", "
				+ TIMESTAMP_CRZ + ", " + R_UTENTE_AGG + ", " + TIMESTAMP_AGG);
	}

}