package it.dnd.thip.base;

import java.sql.SQLException;

import com.thera.thermfw.base.SystemParam;
import com.thera.thermfw.persist.Factory;
import com.thera.thermfw.persist.TableManager;

import it.thera.thip.cs.DatiComuniEstesiTTM;

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
 * 72080	06/08/2025	DSSOF3   Aggiunta colonne
 * 72376	11/03/2026	DSSOF3	 Aggiunta colonne
 * 72702	20/10/2026	AGSOF3	 Aggiunta serie / causale doc generico riallineamento giacenze logis
 */

public class PsnDatiDndTM extends TableManager {

	public static final String ID_AZIENDA = "ID_AZIENDA";

	public static final String STATO = "STATO";

	public static final String R_UTENTE_CRZ = "R_UTENTE_CRZ";

	public static final String TIMESTAMP_CRZ = "TIMESTAMP_CRZ";

	public static final String R_UTENTE_AGG = "R_UTENTE_AGG";

	public static final String TIMESTAMP_AGG = "TIMESTAMP_AGG";

	public static final String R_MAG_SCARTI_DICH_PREL = "R_MAG_SCARTI_DICH_PREL";

	//72080 Inizio
	public static final String NUMERATORE_ORDINE_PRODUZIONE = "R_NUMERATORE_ODP";
	public static final String SERIE_ORDINE_PRODUZIONE = "R_SERIE_ODP";
	public static final String ATTIVITA_ORD_CONF_MV = "R_ATV_ORD_CONF_MV";
	//72080 Fine

	//72376 <
	public static final String ID_COD_MAG_FISICO_UB_DF_SCARICO = "R_COD_MAG_FIS_UBIC_SCARICO_DEF";
	public static final String ID_UBICAZIONE_DEFAULT_SCARICO = "R_UBIC_SCARICO_DEF";
	//72376 >

	//72702 <
	public static final String NMRT_DOC_GNR_RIALL_LOGIS = "NMRT_DOC_GNR_RIALL_LOGIS";
	public static final String SERIE_DOC_GNR_RIALL_LOGIS = "SERIE_DOC_GNR_RIALL_LOGIS";
	public static final String R_CAU_DOC_GNR_RIALL_LOGIS = "R_CAU_DOC_GNR_RIALL_LOGIS";
	//72702 >

	public static final String TABLE_NAME = SystemParam.getSchema("THIPPERS") + "PSN_DATI_DND";

	private static TableManager cInstance;

	private static final String CLASS_NAME = it.dnd.thip.base.PsnDatiDnd.class.getName();

	public synchronized static TableManager getInstance() throws SQLException {
		if (cInstance == null) {
			cInstance = (TableManager) Factory.createObject(PsnDatiDndTM.class);
		}
		return cInstance;
	}

	public PsnDatiDndTM() throws SQLException {
		super();
	}

	protected void initialize() throws SQLException {
		setTableName(TABLE_NAME);
		setObjClassName(CLASS_NAME);
		init();
	}

	protected void initializeRelation() throws SQLException {
		super.initializeRelation();
		addAttribute("IdAzienda", ID_AZIENDA);
		addAttribute("IdMagScartiDichPrel", R_MAG_SCARTI_DICH_PREL);

		//72080 Inizio
		addAttribute("IdNumeratoreODP", NUMERATORE_ORDINE_PRODUZIONE);
		addAttribute("IdSerieODP", SERIE_ORDINE_PRODUZIONE);
		addAttribute("IdAttivitaODP", ATTIVITA_ORD_CONF_MV);
		//72080 Fine

		//72376 <
		addAttribute("CodiceMagFisicoScarico", ID_COD_MAG_FISICO_UB_DF_SCARICO);
		addAttribute("CodiceUbicazioneScarico", ID_UBICAZIONE_DEFAULT_SCARICO);
		//72376 >

		//72702 <
		addAttribute("NmrtDocGnrRiallLogis", NMRT_DOC_GNR_RIALL_LOGIS);
		addAttribute("SerieDocGnrRiallLogis", SERIE_DOC_GNR_RIALL_LOGIS);
		addAttribute("RCauDocGnrRiallLogis", R_CAU_DOC_GNR_RIALL_LOGIS);
		//72702 >

		addComponent("DatiComuniEstesi", DatiComuniEstesiTTM.class);
		setKeys(ID_AZIENDA);

		setTimestampColumn("TIMESTAMP_AGG");
		((it.thera.thip.cs.DatiComuniEstesiTTM) getTransientTableManager("DatiComuniEstesi")).setExcludedColums();
	}

	private void init() throws SQLException {
		configure();
	}

}
