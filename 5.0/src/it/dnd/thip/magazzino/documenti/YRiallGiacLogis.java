package it.dnd.thip.magazzino.documenti;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.batch.BatchRunnable;

import it.thera.thip.base.azienda.Azienda;

public class YRiallGiacLogis extends BatchRunnable {

	protected String iIdAzienda;
	protected String iIdMagazzino;

	public String getIdAzienda() {
		return iIdAzienda;
	}

	public void setIdAzienda(String iIdAzienda) {
		this.iIdAzienda = iIdAzienda;
	}

	public String getIdMagazzino() {
		return iIdMagazzino;
	}

	public void setIdMagazzino(String iIdMagazzino) {
		this.iIdMagazzino = iIdMagazzino;
	}

	public YRiallGiacLogis() {
		setIdAzienda(Azienda.getAziendaCorrente());
	}

	@Override
	protected boolean run() {
		boolean ok = true;
		try {

		}catch (Exception e) {
			e.printStackTrace(Trace.excStream);
		}
		return ok;
	}

	@Override
	protected String getClassAdCollectionName() {
		return "YRiallGiacLogis";
	}

}
