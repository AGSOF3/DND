<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN"
                      "file:///W:\PthDev\Projects\Panthera\DND\WebContent\dtd/xhtml1-transitional.dtd">
<html>
<!-- WIZGEN Therm 2.0.0 as Form - multiBrowserGen = true -->
<%=WebGenerator.writeRuntimeInfo()%>
  <head>
<%@ page contentType="text/html; charset=Cp1252"%>
<%@ page import= " 
  java.sql.*, 
  java.util.*, 
  java.lang.reflect.*, 
  javax.naming.*, 
  com.thera.thermfw.common.*, 
  com.thera.thermfw.type.*, 
  com.thera.thermfw.web.*, 
  com.thera.thermfw.security.*, 
  com.thera.thermfw.base.*, 
  com.thera.thermfw.ad.*, 
  com.thera.thermfw.persist.*, 
  com.thera.thermfw.gui.cnr.*, 
  com.thera.thermfw.setting.*, 
  com.thera.thermfw.collector.*, 
  com.thera.thermfw.batch.web.*, 
  com.thera.thermfw.batch.*, 
  com.thera.thermfw.pref.* 
"%> 
<%
  ServletEnvironment se = (ServletEnvironment)Factory.createObject("com.thera.thermfw.web.ServletEnvironment"); 
  BODataCollector YLogCheckChiuListaBODC = null; 
  List errors = new ArrayList(); 
  WebJSTypeList jsList = new WebJSTypeList(); 
  WebForm YLogCheckChiuListaForm =  
     new com.thera.thermfw.web.WebForm(request, response, "YLogCheckChiuListaForm", "YLogCheckChiuLista", null, "com.thera.thermfw.web.servlet.FormActionAdapter", false, false, true, true, true, true, null, 0, true, "it/dnd/thip/produzione/raccoltaDati/YLogCheckChiuLista.js"); 
  YLogCheckChiuListaForm.setServletEnvironment(se); 
  YLogCheckChiuListaForm.setJSTypeList(jsList); 
  YLogCheckChiuListaForm.setHeader("it.thera.thip.cs.PantheraHeader.jsp"); 
  YLogCheckChiuListaForm.setFooter("com.thera.thermfw.common.Footer.jsp"); 
  YLogCheckChiuListaForm.setDeniedAttributeModeStr("hideNone"); 
  int mode = YLogCheckChiuListaForm.getMode(); 
  String key = YLogCheckChiuListaForm.getKey(); 
  String errorMessage; 
  boolean requestIsValid = false; 
  boolean leftIsKey = false; 
  boolean conflitPresent = false; 
  String leftClass = ""; 
  try 
  {
     se.initialize(request, response); 
     if(se.begin()) 
     { 
        YLogCheckChiuListaForm.outTraceInfo(getClass().getName()); 
        String collectorName = YLogCheckChiuListaForm.findBODataCollectorName(); 
                YLogCheckChiuListaBODC = (BODataCollector)Factory.createObject(collectorName); 
        if (YLogCheckChiuListaBODC instanceof WebDataCollector) 
            ((WebDataCollector)YLogCheckChiuListaBODC).setServletEnvironment(se); 
        YLogCheckChiuListaBODC.initialize("YLogCheckChiuLista", true, 0); 
        YLogCheckChiuListaForm.setBODataCollector(YLogCheckChiuListaBODC); 
        int rcBODC = YLogCheckChiuListaForm.initSecurityServices(); 
        mode = YLogCheckChiuListaForm.getMode(); 
        if (rcBODC == BODataCollector.OK) 
        { 
           requestIsValid = true; 
           YLogCheckChiuListaForm.write(out); 
           if(mode != WebForm.NEW) 
              rcBODC = YLogCheckChiuListaBODC.retrieve(key); 
           if(rcBODC == BODataCollector.OK) 
           { 
              YLogCheckChiuListaForm.writeHeadElements(out); 
           // fine blocco XXX  
           // a completamento blocco di codice YYY a fine body con catch e gestione errori 
%> 
<% 
  WebMenuBar menuBar = new com.thera.thermfw.web.WebMenuBar("HM_Array1", "150", "#000000","#000000","#A5B6CE","#E4EAEF","#FFFFFF","#000000"); 
  menuBar.setParent(YLogCheckChiuListaForm); 
   request.setAttribute("menuBar", menuBar); 
%> 
<jsp:include page="/it/thera/thip/cs/defObjMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="menuBar"/> 
</jsp:include> 
<% 
  menuBar.write(out); 
  menuBar.writeChildren(out); 
%> 
<% 
  WebToolBar myToolBarTB = new com.thera.thermfw.web.WebToolBar("myToolBar", "24", "24", "16", "16", "#f7fbfd","#C8D6E1"); 
  myToolBarTB.setParent(YLogCheckChiuListaForm); 
   request.setAttribute("toolBar", myToolBarTB); 
%> 
<jsp:include page="/it/thera/thip/cs/defObjMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="toolBar"/> 
</jsp:include> 
<% 
   myToolBarTB.write(out); 
%> 
</head>
  <body onbeforeunload="<%=YLogCheckChiuListaForm.getBodyOnBeforeUnload()%>" onload="<%=YLogCheckChiuListaForm.getBodyOnLoad()%>" onunload="<%=YLogCheckChiuListaForm.getBodyOnUnload()%>" style="margin: 0px; overflow: hidden;"><%
   YLogCheckChiuListaForm.writeBodyStartElements(out); 
%> 

    <table width="100%" height="100%" cellspacing="0" cellpadding="0">
<tr>
<td style="height:0" valign="top">
<% String hdr = YLogCheckChiuListaForm.getCompleteHeader();
 if (hdr != null) { 
   request.setAttribute("dataCollector", YLogCheckChiuListaBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= hdr %>" flush="true"/> 
<% } %> 
</td>
</tr>

<tr>
<td valign="top" height="100%">
<form action="<%=YLogCheckChiuListaForm.getServlet()%>" method="post" name="YLogCheckChiuListaForm" style="height:100%"><%
  YLogCheckChiuListaForm.writeFormStartElements(out); 
%>

      <table cellpadding="0" cellspacing="0" height="100%" id="emptyborder" width="100%">
        <tr>
          <td style="height:0">
            <% menuBar.writeElements(out); %> 

          </td>
        </tr>
        <tr>
          <td style="height:0">
            <% myToolBarTB.writeChildren(out); %> 

          </td>
        </tr>
        <tr>
          <td>
            <% 
  WebTextInput YLogCheckChiuListaIdProgressivo =  
     new com.thera.thermfw.web.WebTextInput("YLogCheckChiuLista", "IdProgressivo"); 
  YLogCheckChiuListaIdProgressivo.setParent(YLogCheckChiuListaForm); 
%>
<input class="<%=YLogCheckChiuListaIdProgressivo.getClassType()%>" id="<%=YLogCheckChiuListaIdProgressivo.getId()%>" maxlength="<%=YLogCheckChiuListaIdProgressivo.getMaxLength()%>" name="<%=YLogCheckChiuListaIdProgressivo.getName()%>" size="<%=YLogCheckChiuListaIdProgressivo.getSize()%>" type="hidden"><% 
  YLogCheckChiuListaIdProgressivo.write(out); 
%>

          </td>
        </tr>
        <tr>
          <td height="100%">
            <!--<span class="tabbed" id="mytabbed">-->
<table width="100%" height="100%" cellpadding="0" cellspacing="0" style="padding-right:1px">
   <tr valign="top">
     <td><% 
  WebTabbed mytabbed = new com.thera.thermfw.web.WebTabbed("mytabbed", "100%", "100%"); 
  mytabbed.setParent(YLogCheckChiuListaForm); 
 mytabbed.addTab("tab2", "it.dnd.thip.produzione.raccoltaDati.resources.YLogCheckChiuLista", "tab2", "YLogCheckChiuLista", null, null, null, null); 
  mytabbed.write(out); 
%>

     </td>
   </tr>
   <tr>
     <td height="100%"><div class="tabbed_pagine" id="tabbedPagine" style="position: relative; width: 100%; height: 100%;">
              <div class="tabbed_page" id="<%=mytabbed.getTabPageId("tab2")%>" style="width:100%;height:100%;overflow:auto;"><% mytabbed.startTab("tab2"); %>
                <table style="width: 100%;">
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YLogCheckChiuLista", "RifRigaLista", null); 
   label.setParent(YLogCheckChiuListaForm); 
%><label class="<%=label.getClassType()%>" for="RifRigaLista"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YLogCheckChiuListaRifRigaLista =  
     new com.thera.thermfw.web.WebTextInput("YLogCheckChiuLista", "RifRigaLista"); 
  YLogCheckChiuListaRifRigaLista.setParent(YLogCheckChiuListaForm); 
%>
<input class="<%=YLogCheckChiuListaRifRigaLista.getClassType()%>" id="<%=YLogCheckChiuListaRifRigaLista.getId()%>" maxlength="<%=YLogCheckChiuListaRifRigaLista.getMaxLength()%>" name="<%=YLogCheckChiuListaRifRigaLista.getName()%>" size="<%=YLogCheckChiuListaRifRigaLista.getSize()%>"><% 
  YLogCheckChiuListaRifRigaLista.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YLogCheckChiuLista", "RArticolo", null); 
   label.setParent(YLogCheckChiuListaForm); 
%><label class="<%=label.getClassType()%>" for="Articolo"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebMultiSearchForm YLogCheckChiuListaArticolo =  
     new com.thera.thermfw.web.WebMultiSearchForm("YLogCheckChiuLista", "Articolo", false, false, true, 1, null, null); 
  YLogCheckChiuListaArticolo.setParent(YLogCheckChiuListaForm); 
  YLogCheckChiuListaArticolo.write(out); 
%>
<!--<span class="multisearchform" id="Articolo"></span>-->
                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YLogCheckChiuLista", "QtaRichiesta", null); 
   label.setParent(YLogCheckChiuListaForm); 
%><label class="<%=label.getClassType()%>" for="QtaRichiesta"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YLogCheckChiuListaQtaRichiesta =  
     new com.thera.thermfw.web.WebTextInput("YLogCheckChiuLista", "QtaRichiesta"); 
  YLogCheckChiuListaQtaRichiesta.setParent(YLogCheckChiuListaForm); 
%>
<input class="<%=YLogCheckChiuListaQtaRichiesta.getClassType()%>" id="<%=YLogCheckChiuListaQtaRichiesta.getId()%>" maxlength="<%=YLogCheckChiuListaQtaRichiesta.getMaxLength()%>" name="<%=YLogCheckChiuListaQtaRichiesta.getName()%>" size="<%=YLogCheckChiuListaQtaRichiesta.getSize()%>"><% 
  YLogCheckChiuListaQtaRichiesta.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YLogCheckChiuLista", "QtaConfezionata", null); 
   label.setParent(YLogCheckChiuListaForm); 
%><label class="<%=label.getClassType()%>" for="QtaConfezionata"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YLogCheckChiuListaQtaConfezionata =  
     new com.thera.thermfw.web.WebTextInput("YLogCheckChiuLista", "QtaConfezionata"); 
  YLogCheckChiuListaQtaConfezionata.setParent(YLogCheckChiuListaForm); 
%>
<input class="<%=YLogCheckChiuListaQtaConfezionata.getClassType()%>" id="<%=YLogCheckChiuListaQtaConfezionata.getId()%>" maxlength="<%=YLogCheckChiuListaQtaConfezionata.getMaxLength()%>" name="<%=YLogCheckChiuListaQtaConfezionata.getName()%>" size="<%=YLogCheckChiuListaQtaConfezionata.getSize()%>"><% 
  YLogCheckChiuListaQtaConfezionata.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YLogCheckChiuLista", "QtaResidua", null); 
   label.setParent(YLogCheckChiuListaForm); 
%><label class="<%=label.getClassType()%>" for="QtaResidua"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YLogCheckChiuListaQtaResidua =  
     new com.thera.thermfw.web.WebTextInput("YLogCheckChiuLista", "QtaResidua"); 
  YLogCheckChiuListaQtaResidua.setParent(YLogCheckChiuListaForm); 
%>
<input class="<%=YLogCheckChiuListaQtaResidua.getClassType()%>" id="<%=YLogCheckChiuListaQtaResidua.getId()%>" maxlength="<%=YLogCheckChiuListaQtaResidua.getMaxLength()%>" name="<%=YLogCheckChiuListaQtaResidua.getName()%>" size="<%=YLogCheckChiuListaQtaResidua.getSize()%>"><% 
  YLogCheckChiuListaQtaResidua.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <% 
  WebCheckBox YLogCheckChiuListaAnomalia =  
     new com.thera.thermfw.web.WebCheckBox("YLogCheckChiuLista", "Anomalia"); 
  YLogCheckChiuListaAnomalia.setParent(YLogCheckChiuListaForm); 
%>
<input id="<%=YLogCheckChiuListaAnomalia.getId()%>" name="<%=YLogCheckChiuListaAnomalia.getName()%>" type="checkbox" value="Y"><%
  YLogCheckChiuListaAnomalia.write(out); 
%>

                    </td>
                    <td valign="top">
                    </td>
                  </tr>
                </table>
              <% mytabbed.endTab(); %> 
</div>
            </div><% mytabbed.endTabbed();%> 

     </td>
   </tr>
</table><!--</span>-->
          </td>
        </tr>
        <tr>
          <td style="height:0">
            <% 
  WebErrorList errorList = new com.thera.thermfw.web.WebErrorList(); 
  errorList.setParent(YLogCheckChiuListaForm); 
  errorList.write(out); 
%>
<!--<span class="errorlist"></span>-->
          </td>
        </tr>
      </table>
    <%
  YLogCheckChiuListaForm.writeFormEndElements(out); 
%>
</form></td>
</tr>

<tr>
<td style="height:0">
<% String ftr = YLogCheckChiuListaForm.getCompleteFooter();
 if (ftr != null) { 
   request.setAttribute("dataCollector", YLogCheckChiuListaBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= ftr %>" flush="true"/> 
<% } %> 
</td>
</tr>
</table>


  <%
           // blocco YYY  
           // a completamento blocco di codice XXX in head 
              YLogCheckChiuListaForm.writeBodyEndElements(out); 
           } 
           else 
              errors.addAll(0, YLogCheckChiuListaBODC.getErrorList().getErrors()); 
        } 
        else 
           errors.addAll(0, YLogCheckChiuListaBODC.getErrorList().getErrors()); 
           if(YLogCheckChiuListaBODC.getConflict() != null) 
                conflitPresent = true; 
     } 
     else 
        errors.add(new ErrorMessage("BAS0000010")); 
  } 
  catch(NamingException e) { 
     errorMessage = e.getMessage(); 
     errors.add(new ErrorMessage("CBS000025", errorMessage));  } 
  catch(SQLException e) {
     errorMessage = e.getMessage(); 
     errors.add(new ErrorMessage("BAS0000071", errorMessage));  } 
  catch(Throwable e) {
     e.printStackTrace(Trace.excStream);
  }
  finally 
  {
     if(YLogCheckChiuListaBODC != null && !YLogCheckChiuListaBODC.close(false)) 
        errors.addAll(0, YLogCheckChiuListaBODC.getErrorList().getErrors()); 
     try 
     { 
        se.end(); 
     }
     catch(IllegalArgumentException e) { 
        e.printStackTrace(Trace.excStream); 
     } 
     catch(SQLException e) { 
        e.printStackTrace(Trace.excStream); 
     } 
  } 
  if(!errors.isEmpty())
  { 
      if(!conflitPresent)
  { 
     request.setAttribute("ErrorMessages", errors); 
     String errorPage = YLogCheckChiuListaForm.getErrorPage(); 
%> 
     <jsp:include page="<%=errorPage%>" flush="true"/> 
<% 
  } 
  else 
  { 
     request.setAttribute("ConflictMessages", YLogCheckChiuListaBODC.getConflict()); 
     request.setAttribute("ErrorMessages", errors); 
     String conflictPage = YLogCheckChiuListaForm.getConflictPage(); 
%> 
     <jsp:include page="<%=conflictPage%>" flush="true"/> 
<% 
   } 
   } 
%> 
</body>
</html>
