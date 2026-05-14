/*
 * @(#)YNazionePrimrose.java
 */

/**
 * null
 *
 * <br></br><b>Copyright (C) : Thera SpA</b>
 * @author Wizard 14/05/2026 at 17:02:53
 */
/*
 * Revisions:
 * Date          Owner      Description
 * 14/05/2026    Wizard     Codice generato da Wizard
 *
 */
package it.dnd.thip.base.partner;
import com.thera.thermfw.persist.*;
import java.sql.*;
import java.util.*;
import it.thera.thip.base.partner.*;
import com.thera.thermfw.common.*;

public class YNazionePrimrose extends NazionePrimrose {

  
  /**
   * Attributo iRelcontinente
   */
  protected Proxy iRelcontinente = new Proxy(it.dnd.thip.base.partner.YContinente.class);

  /**
   * Attributo iRelsubcontinente
   */
  protected Proxy iRelsubcontinente = new Proxy(it.dnd.thip.base.partner.YSubcontinente.class);

  
  /**
   * YNazionePrimrose
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public YNazionePrimrose() {
  
    // TO DO
  }

  /**
   * Valorizza l'attributo. 
   * @param relcontinente
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setRelcontinente(YContinente relcontinente) {
    String idAzienda = getIdAzienda();
    if (relcontinente != null) {
      idAzienda = KeyHelper.getTokenObjectKey(relcontinente.getKey(), 2);
    }
    setIdAziendaInternal(idAzienda);
    this.iRelcontinente.setObject(relcontinente);
    setDirty();
  }

  /**
   * Restituisce l'attributo. 
   * @return YContinente
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public YContinente getRelcontinente() {
    return (YContinente)iRelcontinente.getObject();
  }

  /**
   * setRelcontinenteKey
   * @param key
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setRelcontinenteKey(String key) {
    iRelcontinente.setKey(key);
    String idAzienda = KeyHelper.getTokenObjectKey(key, 2);
    setIdAziendaInternal(idAzienda);
    setDirty();
  }

  /**
   * getRelcontinenteKey
   * @return String
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public String getRelcontinenteKey() {
    return iRelcontinente.getKey();
  }

  /**
   * Valorizza l'attributo. 
   * @param rContinente
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setRContinente(String rContinente) {
    String key = iRelcontinente.getKey();
    iRelcontinente.setKey(KeyHelper.replaceTokenObjectKey(key , 1, rContinente));
    setDirty();
  }

  /**
   * Restituisce l'attributo. 
   * @return String
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public String getRContinente() {
    String key = iRelcontinente.getKey();
    String objRContinente = KeyHelper.getTokenObjectKey(key,1);
    return objRContinente;
    
  }

  /**
   * Valorizza l'attributo. 
   * @param relsubcontinente
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setRelsubcontinente(YSubcontinente relsubcontinente) {
    String idAzienda = getIdAzienda();
    if (relsubcontinente != null) {
      idAzienda = KeyHelper.getTokenObjectKey(relsubcontinente.getKey(), 2);
    }
    setIdAziendaInternal(idAzienda);
    this.iRelsubcontinente.setObject(relsubcontinente);
    setDirty();
  }

  /**
   * Restituisce l'attributo. 
   * @return YSubcontinente
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public YSubcontinente getRelsubcontinente() {
    return (YSubcontinente)iRelsubcontinente.getObject();
  }

  /**
   * setRelsubcontinenteKey
   * @param key
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setRelsubcontinenteKey(String key) {
    iRelsubcontinente.setKey(key);
    String idAzienda = KeyHelper.getTokenObjectKey(key, 2);
    setIdAziendaInternal(idAzienda);
    setDirty();
  }

  /**
   * getRelsubcontinenteKey
   * @return String
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public String getRelsubcontinenteKey() {
    return iRelsubcontinente.getKey();
  }

  /**
   * Valorizza l'attributo. 
   * @param rSubcontinente
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setRSubcontinente(String rSubcontinente) {
    String key = iRelsubcontinente.getKey();
    iRelsubcontinente.setKey(KeyHelper.replaceTokenObjectKey(key , 1, rSubcontinente));
    setDirty();
  }

  /**
   * Restituisce l'attributo. 
   * @return String
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public String getRSubcontinente() {
    String key = iRelsubcontinente.getKey();
    String objRSubcontinente = KeyHelper.getTokenObjectKey(key,1);
    return objRSubcontinente;
    
  }

  /**
   * Valorizza l'attributo. 
   * @param idAzienda
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setIdAzienda(String idAzienda) {
    setIdAziendaInternal(idAzienda);
    setDirty();
  }

  /**
   * Restituisce l'attributo. 
   * @return String
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public String getIdAzienda() {
    String key = iRelcontinente.getKey();
    String objIdAzienda = KeyHelper.getTokenObjectKey(key,2);
    return objIdAzienda;
  }

  /**
   * setEqual
   * @param obj
   * @throws CopyException
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  public void setEqual(Copyable obj) throws CopyException {
    super.setEqual(obj);
    YNazionePrimrose yNazionePrimrose = (YNazionePrimrose)obj;
    iRelcontinente.setEqual(yNazionePrimrose.iRelcontinente);
    iRelsubcontinente.setEqual(yNazionePrimrose.iRelsubcontinente);
  }

  /**
   * setIdAziendaInternal
   * @param idAzienda
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  protected void setIdAziendaInternal(String idAzienda) {
    String key1 = iRelcontinente.getKey();
    iRelcontinente.setKey(KeyHelper.replaceTokenObjectKey(key1, 2, idAzienda));
    String key2 = iRelsubcontinente.getKey();
    iRelsubcontinente.setKey(KeyHelper.replaceTokenObjectKey(key2, 2, idAzienda));
  }

}

