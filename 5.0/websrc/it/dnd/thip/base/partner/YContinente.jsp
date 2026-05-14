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
  BODataCollector YContinenteBODC = null; 
  List errors = new ArrayList(); 
  WebJSTypeList jsList = new WebJSTypeList(); 
  WebForm YContinenteForm =  
     new com.thera.thermfw.web.WebForm(request, response, "YContinenteForm", "YContinente", null, "com.thera.thermfw.web.servlet.FormActionAdapter", false, false, true, true, true, true, null, 0, true, "it/dnd/thip/base/partner/YContinente.js"); 
  YContinenteForm.setServletEnvironment(se); 
  YContinenteForm.setJSTypeList(jsList); 
  YContinenteForm.setHeader("it.thera.thip.cs.PantheraHeader.jsp"); 
  YContinenteForm.setFooter("com.thera.thermfw.common.Footer.jsp"); 
  YContinenteForm.setDeniedAttributeModeStr("hideNone"); 
  int mode = YContinenteForm.getMode(); 
  String key = YContinenteForm.getKey(); 
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
        YContinenteForm.outTraceInfo(getClass().getName()); 
        String collectorName = YContinenteForm.findBODataCollectorName(); 
                YContinenteBODC = (BODataCollector)Factory.createObject(collectorName); 
        if (YContinenteBODC instanceof WebDataCollector) 
            ((WebDataCollector)YContinenteBODC).setServletEnvironment(se); 
        YContinenteBODC.initialize("YContinente", true, 0); 
        YContinenteForm.setBODataCollector(YContinenteBODC); 
        int rcBODC = YContinenteForm.initSecurityServices(); 
        mode = YContinenteForm.getMode(); 
        if (rcBODC == BODataCollector.OK) 
        { 
           requestIsValid = true; 
           YContinenteForm.write(out); 
           if(mode != WebForm.NEW) 
              rcBODC = YContinenteBODC.retrieve(key); 
           if(rcBODC == BODataCollector.OK) 
           { 
              YContinenteForm.writeHeadElements(out); 
           // fine blocco XXX  
           // a completamento blocco di codice YYY a fine body con catch e gestione errori 
%> 
<% 
  WebMenuBar menuBar = new com.thera.thermfw.web.WebMenuBar("HM_Array1", "150", "#000000","#000000","#A5B6CE","#E4EAEF","#FFFFFF","#000000"); 
  menuBar.setParent(YContinenteForm); 
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
  myToolBarTB.setParent(YContinenteForm); 
   request.setAttribute("toolBar", myToolBarTB); 
%> 
<jsp:include page="/it/thera/thip/cs/defObjMenu.jsp" flush="true"> 
<jsp:param name="partRequest" value="toolBar"/> 
</jsp:include> 
<% 
   myToolBarTB.write(out); 
%> 
</head>
  <body onbeforeunload="<%=YContinenteForm.getBodyOnBeforeUnload()%>" onload="<%=YContinenteForm.getBodyOnLoad()%>" onunload="<%=YContinenteForm.getBodyOnUnload()%>" style="margin: 0px; overflow: hidden;"><%
   YContinenteForm.writeBodyStartElements(out); 
%> 

    <table width="100%" height="100%" cellspacing="0" cellpadding="0">
<tr>
<td style="height:0" valign="top">
<% String hdr = YContinenteForm.getCompleteHeader();
 if (hdr != null) { 
   request.setAttribute("dataCollector", YContinenteBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= hdr %>" flush="true"/> 
<% } %> 
</td>
</tr>

<tr>
<td valign="top" height="100%">
<form action="<%=YContinenteForm.getServlet()%>" method="post" name="YContinenteForm" style="height:100%"><%
  YContinenteForm.writeFormStartElements(out); 
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
  WebTextInput YContinenteIdAzienda =  
     new com.thera.thermfw.web.WebTextInput("YContinente", "IdAzienda"); 
  YContinenteIdAzienda.setParent(YContinenteForm); 
%>
<input class="<%=YContinenteIdAzienda.getClassType()%>" id="<%=YContinenteIdAzienda.getId()%>" maxlength="<%=YContinenteIdAzienda.getMaxLength()%>" name="<%=YContinenteIdAzienda.getName()%>" size="<%=YContinenteIdAzienda.getSize()%>" type="hidden"><% 
  YContinenteIdAzienda.write(out); 
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
  mytabbed.setParent(YContinenteForm); 
 mytabbed.addTab("tab1", "it.dnd.thip.base.partner.resources.YContinente", "tab1", "YContinente", null, null, null, null); 
  mytabbed.write(out); 
%>

     </td>
   </tr>
   <tr>
     <td height="100%"><div class="tabbed_pagine" id="tabbedPagine" style="position: relative; width: 100%; height: 100%;">
              <div class="tabbed_page" id="<%=mytabbed.getTabPageId("tab1")%>" style="width:100%;height:100%;overflow:auto;"><% mytabbed.startTab("tab1"); %>
                <table style="width: 100%;">
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YContinente", "IdContinente", null); 
   label.setParent(YContinenteForm); 
%><label class="<%=label.getClassType()%>" for="IdContinente"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YContinenteIdContinente =  
     new com.thera.thermfw.web.WebTextInput("YContinente", "IdContinente"); 
  YContinenteIdContinente.setParent(YContinenteForm); 
%>
<input class="<%=YContinenteIdContinente.getClassType()%>" id="<%=YContinenteIdContinente.getId()%>" maxlength="<%=YContinenteIdContinente.getMaxLength()%>" name="<%=YContinenteIdContinente.getName()%>" size="<%=YContinenteIdContinente.getSize()%>"><% 
  YContinenteIdContinente.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <%{  WebLabelCompound label = new com.thera.thermfw.web.WebLabelCompound(null, null, "YContinente", "Descrizione", null); 
   label.setParent(YContinenteForm); 
%><label class="<%=label.getClassType()%>" for="Descrizione"><%label.write(out);%></label><%}%>
                    </td>
                    <td valign="top">
                      <% 
  WebTextInput YContinenteDescrizione =  
     new com.thera.thermfw.web.WebTextInput("YContinente", "Descrizione"); 
  YContinenteDescrizione.setParent(YContinenteForm); 
%>
<input class="<%=YContinenteDescrizione.getClassType()%>" id="<%=YContinenteDescrizione.getId()%>" maxlength="<%=YContinenteDescrizione.getMaxLength()%>" name="<%=YContinenteDescrizione.getName()%>" size="<%=YContinenteDescrizione.getSize()%>"><% 
  YContinenteDescrizione.write(out); 
%>

                    </td>
                  </tr>
                  <tr>
                    <td valign="top">
                      <% 
   request.setAttribute("parentForm", YContinenteForm); 
   String CDForDatiComuniEstesi$it$thera$thip$cs$DatiComuniEstesi$jsp = "DatiComuniEstesi"; 
%>
<jsp:include page="/it/thera/thip/cs/DatiComuniEstesi.jsp" flush="true"> 
<jsp:param name="CDName" value="<%=CDForDatiComuniEstesi$it$thera$thip$cs$DatiComuniEstesi$jsp%>"/> 
</jsp:include> 
<!--<span class="subform" id="DatiComuniEstesi"></span>-->
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
  errorList.setParent(YContinenteForm); 
  errorList.write(out); 
%>
<!--<span class="errorlist"></span>-->
          </td>
        </tr>
      </table>
    <%
  YContinenteForm.writeFormEndElements(out); 
%>
</form></td>
</tr>

<tr>
<td style="height:0">
<% String ftr = YContinenteForm.getCompleteFooter();
 if (ftr != null) { 
   request.setAttribute("dataCollector", YContinenteBODC); 
   request.setAttribute("servletEnvironment", se); %>
  <jsp:include page="<%= ftr %>" flush="true"/> 
<% } %> 
</td>
</tr>
</table>


  <%
           // blocco YYY  
           // a completamento blocco di codice XXX in head 
              YContinenteForm.writeBodyEndElements(out); 
           } 
           else 
              errors.addAll(0, YContinenteBODC.getErrorList().getErrors()); 
        } 
        else 
           errors.addAll(0, YContinenteBODC.getErrorList().getErrors()); 
           if(YContinenteBODC.getConflict() != null) 
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
     if(YContinenteBODC != null && !YContinenteBODC.close(false)) 
        errors.addAll(0, YContinenteBODC.getErrorList().getErrors()); 
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
     String errorPage = YContinenteForm.getErrorPage(); 
%> 
     <jsp:include page="<%=errorPage%>" flush="true"/> 
<% 
  } 
  else 
  { 
     request.setAttribute("ConflictMessages", YContinenteBODC.getConflict()); 
     request.setAttribute("ErrorMessages", errors); 
     String conflictPage = YContinenteForm.getConflictPage(); 
%> 
     <jsp:include page="<%=conflictPage%>" flush="true"/> 
<% 
   } 
   } 
%> 
</body>
</html>
