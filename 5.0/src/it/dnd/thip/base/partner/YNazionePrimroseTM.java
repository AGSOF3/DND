package it.dnd.thip.base.partner;

import java.sql.SQLException;

import com.thera.thermfw.base.SystemParam;

import it.thera.thip.base.partner.NazionePrimroseTM;

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

public class YNazionePrimroseTM extends NazionePrimroseTM {

	public static final String R_CONTINENTE = "R_CONTINENTE";

	public static final String R_SUBCONTINENTE = "R_SUBCONTINENTE";

	public static final String ID_AZIENDA = "ID_AZIENDA";

	public static final String TABLE_NAME_EXT = SystemParam.getSchema("THIPPERS") + "";

	private static final String CLASS_NAME = it.dnd.thip.base.partner.YNazionePrimrose.class.getName();

	public YNazionePrimroseTM() throws SQLException {
		super();
	}

	protected void initialize() throws SQLException {
		super.initialize();
		setObjClassName(CLASS_NAME);
	}

	protected void initializeRelation() throws SQLException {
		super.initializeRelation();
		linkTable(TABLE_NAME_EXT);
		addAttributeOnTable("RContinente", R_CONTINENTE, TABLE_NAME_EXT);
		addAttributeOnTable("IdAzienda", ID_AZIENDA, TABLE_NAME_EXT);
		addAttributeOnTable("RSubcontinente", R_SUBCONTINENTE, TABLE_NAME_EXT);

	}

}