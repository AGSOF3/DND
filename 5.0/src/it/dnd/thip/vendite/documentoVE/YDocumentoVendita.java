package it.dnd.thip.vendite.documentoVE;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.persist.CachedStatement;
import com.thera.thermfw.persist.KeyHelper;

import it.thera.thip.vendite.documentoVE.DocumentoVendita;

public class YDocumentoVendita extends DocumentoVendita {

	protected String iYKeyDocDgt;//aggiunto per esposizione in ws magic

	public String getYKeyDocDgt() {
		String key = "";
		ResultSet rs = null;
		try {
			String select = "SELECT ID_AZIENDA , ID_DOCUMENTO_DGT , ID_VERSIONE "
					+ "FROM THIP.DOC_DGT  "
					+ "WHERE R_TIPO_DOC_DGT  = 'DDT_VEN' "
					+ "AND ID_AZIENDA = '"+getIdAzienda()+"' "
					+ "AND R_ANNO_DOC = '"+getAnnoBolla()+"' "
					+ "AND R_NUMERO_DOC = '"+getNumeroBolla()+"' "
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