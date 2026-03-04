package it.dnd.thip.vendite.ordineVE;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.KeyHelper;

import it.thera.thip.vendite.ordineVE.OrdineVendita;

/**
*
* <p></p>
*
* <p>
* Company: Softre Solutions<br>
* Author: Andrea Gatta<br>
* Date: 04/03/2026
* </p>
*/

/*
* Revisions:
* Number	Date		Owner	Description
* 72387	04/03/2026	AGSOF3	Attributo di servizio chiave doc dgt (per magic)
*/
public class YOrdineVendita extends OrdineVendita {

	protected String iYKeyDocDgt;//aggiunto per esposizione in ws magic

	public String getYKeyDocDgt() {
		String key = "";
		ResultSet rs = null;
		try {
			String select = "SELECT ID_AZIENDA , ID_DOCUMENTO_DGT , ID_VERSIONE "
					+ "FROM THIP.DOC_DGT  "
					+ "WHERE R_TIPO_DOC_DGT  = 'ORD_VEN' "
					+ "AND ID_AZIENDA = '"+getIdAzienda()+"' "
					+ "AND R_ANNO_DOC = '"+getAnnoDocumento()+"' "
					+ "AND R_NUMERO_DOC = '"+getNumeroDocumento()+"' "
					+ "ORDER BY ID_VERSIONE DESC ";
			CachedStatement cs = new CachedStatement(select);
			rs = cs.executeQuery();
			while(rs.next()) {
				String[] c = {rs.getString(1),rs.getString(2),rs.getString(3),"1"};
				key = KeyHelper.buildObjectKey(c);
			}
			cs.free();
		}catch(SQLException e) {
			e.printStackTrace();
		}finally {
			try {
				if (rs != null) {
					rs.close();
				}
			}
			catch (Throwable t) {
				t.printStackTrace(Trace.excStream);
			}
		}
		return key;
	}

	public void setYKeyDocDgt(String iYKeyDocDgt) {
		this.iYKeyDocDgt = iYKeyDocDgt;
	}
	
}