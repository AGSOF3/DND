package it.dnd.thip.produzione.ordese.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;

import com.thera.thermfw.ad.ClassADCollection;
import com.thera.thermfw.base.Trace;
import com.thera.thermfw.common.ErrorMessage;
import com.thera.thermfw.persist.ConnectionManager;
import com.thera.thermfw.persist.PersistentObject;
import com.thera.thermfw.web.ServletEnvironment;
import com.thera.thermfw.web.WebToolBar;
import com.thera.thermfw.web.WebToolBarButton;
import com.thera.thermfw.web.servlet.GridActionAdapter;
import com.thera.thermfw.web.servlet.NavigationMapServlet;

import it.dnd.thip.produzione.ordese.YGestioneUdsPickingProd;
import it.dnd.thip.tuttoimballo.stampanti.YInterfStampanti;

/**
 * <p></p>
 *
 * <p>
 * Company: Softre Solutions<br>
 * Author: Daniele Signoroni<br>
 * Date: 10/06/2025
 * </p>
 */

/*
 * Revisions:
 * Number   Date        Owner    Description
 * 71994    10/06/2025  DSSOF3   Prima stesura
 */

public class YGestioneUdsPickingProdGridActionAdapter extends GridActionAdapter {

	private static final long serialVersionUID = 1L;

	protected static final String RES = "it.dnd.thip.produzione.ordese.resources.YGestioneUdsPickingProd";
	public static final String ACCODA_UDS = "ACCODA_RIGHE";

	public static final String STP_ETI_LOFTWARE = "STP_ETI_LOFTWARE";

	@Override
	public void modifyToolBar(WebToolBar toolBar) {
		super.modifyToolBar(toolBar);

		WebToolBarButton accodaRighe = new WebToolBarButton("AccodaUds",
				"action_submit", "new", "no", RES, "AccodaUds",
				"it/thera/thip/vendite/ordineVE/images/AccodaRigheEva.gif", ACCODA_UDS, "single", true);

		ServletEnvironment se = toolBar.getServletEnvironment();
		if(se.getRequest().getParameter("YMostraUdsAzioneGeneraUdsAutomaticamente") != null 
				&& se.getRequest().getParameter("YMostraUdsAzioneGeneraUdsAutomaticamente").equals("Y")){
			WebToolBarButton btnNew = (WebToolBarButton) toolBar.getButtons().get(0);

			toolBar.getButtons().clear();

			toolBar.addButton(btnNew);
		}else {
			accodaRighe = null;
		}

		if(accodaRighe != null)
			toolBar.addButton(accodaRighe);

		WebToolBarButton stampaEticLoftware = new WebToolBarButton("StampaLoftware",
				"action_submit", "infoArea", "no", RES, "StampaLoftware",
				"it/dnd/thip/produzione/ordese/img/tt_imb.jpeg", STP_ETI_LOFTWARE, "multiple", true);
		toolBar.addButton(stampaEticLoftware);
	}

	@Override
	protected void otherActions(ClassADCollection cadc, ServletEnvironment se) throws ServletException, IOException {
		String azione = getStringParameter(se.getRequest(), ACTION);
		if(azione.equals(ACCODA_UDS)) {
			String url = getCurrentViewSelector().getCopyObjectURL(cadc, se, getClass().getName());
			//url += "&IdCodiceRigaLista"+getStringParameter(se.getRequest(), "IdCodiceRigaLista");
			//url += "&IdCodiceLista"+getStringParameter(se.getRequest(), "IdCodiceLista");
			url += "&YAccodamentoUdsRaccoltaDati=Y";

			se.sendRequest(getServletContext(), url, false);
		}else if(azione.equals(STP_ETI_LOFTWARE)) {
			stampaEtichettaLoftware(cadc,se);
		}else {
			super.otherActions(cadc, se);
		}
	}

	@Override
	protected void callShowGrid(ServletEnvironment se) throws ServletException, IOException {
		String np = se.getRequest().getParameter(NEW_PAGE);
		String cn = se.getRequest().getParameter(CLASS_NAME);
		String sk = se.getRequest().getParameter(TH_SETTING_KEY);
		String url = se.getServletPath() + "com.thera.thermfw.web.servlet.Execute?Page=" + np + "&ClassName=" + cn + "&SettingKey=" + sk;
		String sg = se.getRequest().getParameter(TH_SHOW_GRID_NAME);
		if (sg != null && !sg.equals(""))
			url += "&ShowGridName=" + sg;
		String navigationMapName = se.getRequest().getParameter(NavigationMapServlet.NAV_MAP_NAME_PARAM);
		String navigationMapBoxName = se.getRequest().getParameter(NavigationMapServlet.NAV_MAP_BOX_NAME_PARAM);
		String navigationMapUserKey = se.getRequest().getParameter(NavigationMapServlet.NAV_MAP_BOX_USER_KEY_PARAM);

		if (navigationMapName != null){
			url += "&" + NavigationMapServlet.NAV_MAP_NAME_PARAM + "=" + navigationMapName;
		}
		if (navigationMapBoxName != null){
			url += "&" + NavigationMapServlet.NAV_MAP_BOX_NAME_PARAM + "=" + navigationMapBoxName;
		}
		if (navigationMapUserKey != null){
			url += "&" + NavigationMapServlet.NAV_MAP_BOX_USER_KEY_PARAM + "=" + navigationMapUserKey;
		}
		String MostraUdsAzioneGeneraUdsAutomaticamente = se.getRequest().getParameter("YMostraUdsAzioneGeneraUdsAutomaticamente");
		if(MostraUdsAzioneGeneraUdsAutomaticamente != null && MostraUdsAzioneGeneraUdsAutomaticamente.equals("Y")) {
			String newUrl = "";
			newUrl+="&YMostraUdsAzioneGeneraUdsAutomaticamente=Y";;
			newUrl+="&YIdTipoUds="+se.getRequest().getParameter("YIdTipoUds");
			newUrl+="&YNumeroRitorno="+se.getRequest().getParameter("YNumeroRitorno");
			newUrl+="&YIdCodiceRigaLista="+se.getRequest().getParameter("YIdCodiceRigaLista");
			newUrl+="&YIdCodiceLista="+se.getRequest().getParameter("YIdCodiceLista");
			newUrl+="&YPezziPerUds="+se.getRequest().getParameter("YPezziPerUds");

			url += newUrl;
		}
		se.sendRequest(getServletContext(), url , false);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void stampaEtichettaLoftware(ClassADCollection cadc, ServletEnvironment se) throws ServletException, IOException {
		String[] keys = se.getRequest().getParameterValues(OBJECT_KEY);
		List errorMessages = new ArrayList();
		try {
			for (int i = 0; i < keys.length; i++) {
				YGestioneUdsPickingProd uds = YGestioneUdsPickingProd.elementWithKey(keys[i], PersistentObject.NO_LOCK);
				if(uds != null) {
					YInterfStampanti stp = uds.recordLoftwareCartoni(null);
					if(stp != null) {
						int rc = stp.save();
						if(rc > 0){
							ConnectionManager.commit();
						}else {
							ConnectionManager.rollback();
							errorMessages.add(new ErrorMessage("BAS0000078","Impossibile stampare l'etichetta"));
						}
					}
				}
			}
		}catch (Exception e) {
			e.printStackTrace(Trace.excStream);
		}
		se.getRequest().setAttribute("ErrorMessages", errorMessages);
		se.sendRequest(getServletContext(), "com/thera/thermfw/common/InfoAreaHandler.jsp", false);
	}
}
