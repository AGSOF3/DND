package it.dnd.thip.produzione.raccoltaDati;

import java.sql.SQLException;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.common.ErrorMessage;
import com.thera.thermfw.common.Numerator;
import com.thera.thermfw.common.NumeratorException;

public class YLogCheckChiuLista extends YLogCheckChiuListaPO {

	public ErrorMessage checkDelete() {

		return null;
	}

	public int save() throws SQLException {
		if (!isOnDB()) {
			try {
				setIdProgressivo(new Integer(Numerator.getNextInt("YLogCheckChiuLista")));
			} catch (NumeratorException e) {
				e.printStackTrace(Trace.excStream);
			}
		}
		return super.save();
	}

}