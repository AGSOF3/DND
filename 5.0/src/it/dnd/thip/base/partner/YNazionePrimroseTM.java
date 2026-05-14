/*
 * @(#)YNazionePrimroseTM.java
 */

/**
 * YNazionePrimroseTM
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
import com.thera.thermfw.common.*;
import java.sql.*;
import it.thera.thip.base.partner.*;
import com.thera.thermfw.base.*;

public class YNazionePrimroseTM extends NazionePrimroseTM {

  
  /**
   * Attributo R_CONTINENTE
   */
  public static final String R_CONTINENTE = "R_CONTINENTE";

  /**
   * Attributo R_SUBCONTINENTE
   */
  public static final String R_SUBCONTINENTE = "R_SUBCONTINENTE";

  /**
   * Attributo ID_AZIENDA
   */
  public static final String ID_AZIENDA = "ID_AZIENDA";

  /**
   *  TABLE_NAME
   */
  public static final String TABLE_NAME_EXT = SystemParam.getSchema("THIPPERS") + "YBBT34PT";

  /**
   *  CLASS_NAME
   */
  private static final String CLASS_NAME = it.dnd.thip.base.partner.YNazionePrimrose.class.getName();

  
  /**
   *  YNazionePrimroseTM
   * @throws SQLException
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    CodeGen     Codice generato da CodeGenerator
   *
   */
  public YNazionePrimroseTM() throws SQLException {
    super();
  }

  /**
   *  initialize
   * @throws SQLException
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    CodeGen     Codice generato da CodeGenerator
   *
   */
  protected void initialize() throws SQLException {
    super.initialize();
    setObjClassName(CLASS_NAME);
  }

  /**
   *  initializeRelation
   * @throws SQLException
   */
  /*
   * Revisions:
   * Date          Owner      Description
   * 14/05/2026    Wizard     Codice generato da Wizard
   *
   */
  protected void initializeRelation() throws SQLException {
    super.initializeRelation();
    linkTable(TABLE_NAME_EXT);
    addAttributeOnTable("RContinente", R_CONTINENTE, TABLE_NAME_EXT);
    addAttributeOnTable("IdAzienda", ID_AZIENDA, TABLE_NAME_EXT);
    addAttributeOnTable("RSubcontinente", R_SUBCONTINENTE, TABLE_NAME_EXT);
    
    

  }

}

