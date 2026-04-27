package it.dnd.thip.logis.fis;

import java.sql.SQLException;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.persist.CachedStatement;

import it.thera.thip.logis.fis.Missione;

/**
 *
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 20/04/2026
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 72452    20/04/2026  DSSOF3   Prima stesura
 * 72459	27/04/2026	AGSOF3	 Controllo che la missione nuova abbia il saldo
 */
public class YMissione extends Missione {

	protected static final String lock = "lock";

	protected boolean iSalvaFromPianiPrl = false;

	public boolean isSalvaFromPianiPrl() {
		return iSalvaFromPianiPrl;
	}

	public void setSalvaFromPianiPrl(boolean iSalvaFromPianiPrl) {
		this.iSalvaFromPianiPrl = iSalvaFromPianiPrl;
	}

	@Override
	public int save() throws SQLException {
		int rc =  super.save();
		if(rc > 0 && !isSalvaFromPianiPrl() && getRigaLista() != null
				&& getRigaLista().getTestataLista() != null
				&& ( getRigaLista().getTestataLista().getCodiceTipoLista().equals("P/PRE")
					||	getRigaLista().getTestataLista().getCodiceTipoLista().equals("P/PREC"))
						&& getSaldo() != null) {//72459 saldo != null
			updateMissioneInRigaPianoCarico();
		}
		return rc;
	}

	protected void updateMissioneInRigaPianoCarico() {
		CachedStatement cs = null;
		try{
			synchronized (lock) {
				String update = "	UPDATE R\r\n"
						+ "SET R.R_CODICE_MISSIONE = '"+getCodice()+"', R.R_COD_MAG_FISICO_MISS = '"+getCodiceMagFisico()+"' \r\n"
						+ "FROM THIPPERS.YPIANO_CARICO_TOYOTA_RIG AS R\r\n"
						+ "LEFT JOIN THIPPERS.YPIANO_CARICO_TOYOTA_TES AS T\r\n"
						+ "    ON T.ID_AZIENDA = R.ID_AZIENDA\r\n"
						+ "    AND T.ID_ANNO_DOC = R.ID_ANNO_DOC\r\n"
						+ "    AND T.ID_NUMERO_DOC = R.ID_NUMERO_DOC\r\n"
						+ "LEFT JOIN THIP.ORDESE_ATV_MAT AS M\r\n"
						+ "    ON M.ID_AZIENDA = R.ID_AZIENDA\r\n"
						+ "    AND M.ID_ANNO_ORD = R.ID_ANNO_ORDINE_RIGA_MAT\r\n"
						+ "    AND M.ID_NUMERO_ORD = R.ID_NUMERO_ORD_RIGA_MAT\r\n"
						+ "    AND M.ID_RIGA_ATTIVITA = R.ID_RIGA_ATTIVITA_RIGA_MAT\r\n"
						+ "    AND M.ID_RIGA_MATERIALE = R.ID_RIGA_MATERIALE\r\n"
						+ "LEFT JOIN THIP.PIANI_PRL_TES AS PT\r\n"
						+ "    ON PT.ID_AZIENDA = R.ID_AZIENDA\r\n"
						+ "    AND PT.ID_ANNO_ORD = R.ID_ANNO_ORDINE_RIGA_MAT\r\n"
						+ "    AND PT.ID_NUMERO_ORD = R.ID_NUMERO_ORD_RIGA_MAT\r\n"
						+ "    AND PT.ID_RIGA_ATTIVITA = R.ID_RIGA_ATTIVITA_RIGA_MAT\r\n"
						+ "    AND PT.STATO_GEN_LISTA = '2'\r\n"
						+ "LEFT JOIN LOGIS.LLISTA_RIGA AS LR\r\n"
						+ "    ON LR.COD_SOCIETA = R.ID_AZIENDA\r\n"
						+ "    AND LR.COD_LISTA = PT.R_COD_LISTA\r\n"
						+ "    AND LR.NUM_RIGA_HOST = R.ID_RIGA_MATERIALE\r\n"
						+ "LEFT JOIN LOGIS.LMISSIONE AS LM\r\n"
						+ "    ON LM.COD_SOCIETA = LR.COD_SOCIETA\r\n"
						+ "    AND LM.COD_LISTA = LR.COD_LISTA\r\n"
						+ "    AND LM.COD_RIGA_LISTA = LR.CODICE\r\n"
						+ "    AND LM.STATO_MISSIONE != 'T'\r\n"
						+ "WHERE\r\n"
						+ "    R.ID_AZIENDA = '"+getCodiceSocieta()+"'\r\n"
						+ "    AND R.STATO_RIGA = 'A'\r\n"
						+ "    AND LR.COD_LISTA = '"+getRigaLista().getTestataLista().getCodice()+"'\r\n"
						+ "    AND LR.CODICE = '"+getRigaLista().getCodice()+"';\r\n";
				cs = new CachedStatement(update);
				cs.executeUpdate();
			}
		}catch(Exception e) {
			e.printStackTrace(Trace.excStream);
		}finally {
			try {
				cs.free();
			} catch (SQLException e) {
				e.printStackTrace(Trace.excStream);
			}
		}
	}
}
