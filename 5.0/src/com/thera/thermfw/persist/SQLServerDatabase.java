package com.thera.thermfw.persist;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

import com.thera.thermfw.base.Trace;
import com.thera.thermfw.base.Utils;

/**
 * Definisce il comportamento specifico di SQLServer.
 */
/* Revisions:
 * Numbe    Date          Owner      Description
 *          15/05/2001    DM         Aggiunto metodo configureConnection()
 * 00002    18/06/2001    DM         tolto fillIfNeeded e aggiunti setString(), getCurrTimeKeywords(),
 *                                   getCurrDateKeywords(), getCurrUserKeywords() e i tre getLiteral()
 * 00068    12/10/2001    DM         Implementata handleException() e aggiunto costruttore
 * 00297    28/06/2002    ES         GetLiteral restituisce i Timestamp nel giusto formato
 * 00316    15/07/2002    EM         Implementato getURL() di Database
 * 00333    22/07/2002    DM         Aggiunta gestione dell'errore 2601
 * 00338    23/07/2002    EM         Modifica metodo getSettingTransformerClassName()
 * 00703    17/07/2003    DM         Aggiunto getTypeMapping()
 * 00953    30/10/2003    IT         Aggiunto a cvTypeMapping equivalenza LONGVARCHAR(2147483647) con LONGVARCHAR(N) e LONGVARBINARY(N); Aggiunto FOR BROWSE per Lock Pessimistico
 * 01613    12/03/2004    DM         Aggiunto a cvTypeMapping REAL(24)
 * 01733    29/03/2004    DM         Modificati getLiteral() che funzionavano solo con lingua inglese
 * 01790    28/04/2004    DM         Aggiunti i metodi getCallToXXX()
 * 02832    15/11/2004    ES         Aggiunta gestione TAG per tablespace
 * 02426    19/11/2004    DM         Corretto baco in getLiteral(Timestamp)
 * 03075    05/01/2005    DM         Aggiunto mapping per BLOB e CLOB
 * 03179    07/02/2005    ES         Aggiunto errore x attach:blob > dim. max colonna (succede solo per DB2)
 * 03393    15/03/2005    ES         Aggiunto gestione TAG <MINDATE>, <CURDATE>, <CURTIME> e <CURTS>
 * 03437    23/03/2005    DM         Modificato handleException() per non gestire e.getErrorCode() = 0
 * 03679    27/04/2005    MM         Aggiunto il metodo getDBManagerId()
 * 03788    18/05/2005    Ryo        Ottimizzazione Trace
 * 04648    23/11/2005    Ryo        Corretta la sintassi di creazione del tablespace
 * 05136    06/03/2006    DM         Gestito errCode 0, eliminata clausola FOR BROWSE, impostato timeout di default
 * 05488    05/06/2006    CB         Aggiunto getDuplicatedRowCode()
 * 07057    04/04/2007    Ryo        Implementata l'interface isColumnUppercaseable
 * 06871    26/04/2007    DM         Modificate mappature.
 * 09990    28/10/2008    DM         Aggiunto e usato getDatabaseMetaData()
 * 10936    28/07/2009    DM         Aggiunti metodi getHost() e getPort()
 * 27909    01/10/2018    RA         Aggiunto getLengthFn(), getUppercaseFn(), getLowercaseFn() ,getSubstringFn()
 * 34666    15/11/2021    AJ         Modifica nel methodo handleException
 * 40162    25/10/2023    YA         Definito metodo getStmtExistElement()
 * 41219    02/02/2024    YA         Definito metodo getFirstRowOnlyStmt()
 * 38558    22/02/2024    TA         Definito metodo getFirstNRowsOnlyStmt()
 * 44573	06/02/2025	  RA		 Aggiunto attributo e gestione traceDuplicatedRowBlocked   
 */
public class SQLServerDatabase implements Database
{
	/**
	 * Array con i nomi degli schemi di sistema del database.
	 */
	protected static final String[] dbSystemSchemaNames = {};
	protected static final Map cvTypeMapping = new HashMap();
	protected static final ThreadLocal traceDuplicatedRowBlocked = new ThreadLocal();//44573

	static
	{
		cvTypeMapping.put(new T(Types.TIMESTAMP, 23, 3), new T[]{new T(Types.DATE, 10), new T(Types.TIME, 8), new T(Types.TIMESTAMP, 26, 6)});
		// 3075 DM inizio
		// cvTypeMapping.put(new T(Types.LONGVARCHAR, 2147483647), new T[]{new T(Types.LONGVARCHAR, -1)}); //Mod.953
		// cvTypeMapping.put(new T(Types.LONGVARBINARY, 2147483647), new T[]{new T(Types.LONGVARBINARY, -1)}); //Mod.953
        // 6871 DM inizio
		// cvTypeMapping.put(new T(Types.LONGVARCHAR, 2147483647), new T[]{new T(Types.LONGVARCHAR, -1), new T(Types.CLOB, -1)});
        cvTypeMapping.put(new T(Types.LONGVARCHAR, 2147483647), new T[]{new T(Types.CLOB, -1)});
        // 6871 DM fine
		cvTypeMapping.put(new T(Types.LONGVARBINARY, 2147483647), new T[]{new T(Types.LONGVARBINARY, -1), new T(Types.BLOB, -1)});
        // 6871 DM inizio
        cvTypeMapping.put(new T(Types.CLOB, 2147483647), new T[]{new T(Types.CLOB, -1)});
        cvTypeMapping.put(new T(Types.BLOB, 2147483647), new T[]{new T(Types.LONGVARBINARY, -1), new T(Types.BLOB, -1)});
        // 6871 DM fine
		// 3075 DM fine
		// 1613 DM inizio
		cvTypeMapping.put(new T(Types.REAL, 24), new T[]{new T(Types.REAL, 7)});
		// 1613 DM fine
	}

	protected String ivHost;
	protected String ivPort;
	// 5136 DM inizio
	// Al momento valore cablato, sarebbe bello riuscire a passarlo al costruttore:
	protected int ivTimeout = 30000;
	// 5136 DM fine

	public SQLServerDatabase(String host, String port)
	{
		ivHost = host;
		ivPort = port;
	}

	/**
	 * Gestisce una SQLException, decidendo se rilanciarla o catturarla.
	 * In quest'ultimo caso viene restituito il valore adeguato di ErrorCodes.
	 * @param e l'eccezione da gestire.
	 * @return il valore adeguato di ErrorCodes corrispondente all'eccezione <code>e</code>
	 * @exception java.sql.SQLException se l'eccezione <code>e</code> non viene elaborata,
	 * viene rilanciata con una <code>throws</code>.
	 * @see com.thera.thermfw.persist.ErrorCodes
	 */
	public int handleException(SQLException e) throws SQLException
	{
		if(Trace.isLogEnabled())	// Fix 3788 Ryo
		{
			Trace.println(getClass().getName() + ".handleException():");
			Trace.println("getErrorCode() = " + e.getErrorCode() + ", getSQLState() = '" + e.getSQLState() + "'");
			Trace.println(e.getMessage()); //44573
			Trace.println(e);
		}

		int errCode;
		switch(e.getErrorCode())
		{
			case 547:
				errCode = ErrorCodes.CONSTRAINT_VIOLATION;
				//34666 inizio
				Trace.excStream.println(getClass().getName() + ".handleException():");
				Trace.excStream.println("getErrorCode() = " + e.getErrorCode() + ", getSQLState() = '" + e.getSQLState() + "'");
				Trace.excStream.println(e);
				//34666 fine
				break;
			case 2601:
			case 2627:
				errCode = ErrorCodes.DUPLICATED_ROW;
				//44573 inizio
				if(!isTraceDuplicatedRowBlocked())
				{
					Trace.excStream.println(getClass().getName() + ".handleException():");
					Trace.excStream.println("getErrorCode() = " + e.getErrorCode() + ", getSQLState() = '" + e.getSQLState() + "'");
					Trace.excStream.println(e.getMessage());
					e.printStackTrace(Trace.excStream);
                }
				//44573 fine
				break;
				//case -952: Mod. 953 Errato
				//case -911: Mod. 953 Errato
				// 5136 DM inizio
			case 0:
				// 5136 DM fine
			case -532: // Mod. 953 SQLCode corretto per Lock (FOR UPDATE)
				errCode = ErrorCodes.LOCKED_ROW;
				break;
				// 3437 DM inizio
				/*
			case 0:
			errCode = ErrorCodes.DB_NOT_FOUND;
			break;
				 */
				// 3437 DM fine
			case 18456:
				errCode = ErrorCodes.USER_PWD_UNCORRECT;
				break;
				/*
				case -952:
				errCode = ErrorCodes.OBJ_TIMEOUT;
				break;
				 */
			default:
				throw(e);
		}
		return errCode;
	}

	//Mod. 3179
	/**
	 * Metodo non ridefinito per questo database, rilancia solamente l'eccezione.
	 */
	public int handleBlobException(SQLException e) throws SQLException
	{
		throw(e);
	}
	//fine mod. 3179

	/**
	 * Restituisce una stringa che rappresenta l'istruzione SQL per definire
	 * il timestamp corrente di sistema.
	 * Restituisce <code>DEFAULT</code> per il SQLServer.
	 */
	public String getCurrTimestampKeywords()
	{
		return "CURRENT_TIMESTAMP";
	}
	public String getCurrTimeKeywords()
	{
		return getCurrTimestampKeywords();
	}
	public String getCurrDateKeywords()
	{
		return getCurrTimestampKeywords();
	}
	public String getCurrUserKeywords()
	{
		return "CURRENT_USER";
	}
	public String getForUpdateKeywords()
	{
		// 5136 DM inizio
		// return "FOR BROWSE"; //Mod.953 Uguale a FOR UPDATE di DB2
		return "";
		// 5136 DM inizio
	}

	/**
	 * Restituisce una stringa che rappresenta il nome del driver da utilizzare.
	 * Accediamo a SQLServer via JDBC-ODBC
	 */
	public String getDriverName()
	{
		return "com.microsoft.jdbc.sqlserver.SQLServerDriver";
	}

	/**
	 * Restituisce una stringa che rappresenta il prefisso al nome del database.
	 * Restituisce <code>"jdbc:odbc:"</code> per il driver JDBC-ODBC.
	 */
	public String getNamePrefix()
	{
		return "jdbc:microsoft:sqlserver://" + ivHost + ":" + ivPort;
	}

	/**
	 * Restituisce una stringa che rappresenta il suffisso al nome del database.
	 */
	public String getNameSuffix()
	{
		return ";SelectMethod=cursor;DatabaseName=";
	}

	/**
	 * Restituisce una stringa che rappresenta l'URL di collegamento al database.
	 *
	 * @param dbName Nome del database a cui ci si deve collegare
	 */
	public String getURL(String dbName) {
		return getNamePrefix() + getNameSuffix() + dbName;
	}

	/**
	 * Restituisce un array con i nomi degli schemi di sistema del database, null se se il database
	 * non supporta gli schemi.
	 */
	public String[] getDBSystemSchemaNames()
	{
		return dbSystemSchemaNames;
	}

	/**
	 * Resituisce il contenuto di una colonna di database indicata per posizione.
	 * Non so ancora come si comporta SQLServer.
	 */
	public Object getObjectFromColumn(ResultSet rs,int sqlType,int colNumber) throws SQLException {
		return rs.getObject(colNumber);
	}

	/**
	 * Resituisce il nome della classe SettingTransformer associata al tipo di database.
	 * Questo permette la gestione dell'SQL specifico dei database.
	 * @return Nome della classe erede di SettingTransformer corretta.
	 */
	public String getSettingTransformerClassName()
	{
		return "com.thera.thermfw.setting.SQLServerSettingTransformer";
	}

	/**
	 * Esegue eventuali operazioni di configurazione su una connessione appena aperta.
	 * @param c la connessione da configurare.
	 */
	public void configureConnection(Connection c) throws SQLException
	{
		// 5136 DM inizio
		Statement s = c.createStatement();
		s.executeUpdate("SET LOCK_TIMEOUT " + ivTimeout);
		c.commit();
		// 5136 DM fine
	}

	public void setString(PreparedStatement s, int pos, String value) throws SQLException
	{
		s.setString(pos, value);
	}

	public String getLiteral(Date d)
	{
		// 1733 DM inizio
		// return "'" + d + "'";
		return "CONVERT(DATETIME, '" + Utils.toISO8601Format(d) + "', 120)"; //Mod. 3393
		// 1733 DM fine
	}

	public String getLiteral(Time t)
	{
		// 1733 DM inizio
		// return "'" + t + "'";
		return "CONVERT(DATETIME, '" + t + "', 108)";
		// 1733 DM fine
	}

	public String getLiteral(Timestamp t)
	{
		int maxDec = 3;
		//Trasformo il timestamp in stringa
		String tStr = Utils.toISO8601Format(t);//Mod. 3393//t.toString();
		//Cerco la parte decimale
		int index = tStr.indexOf(".");
		//E se c'è lascio al massimo maxDec decimali
		if (index>0)
		{
			int decLen = tStr.length() - index;
			if (decLen > maxDec)
			{
				int cancLen = decLen - maxDec;
				if (cancLen <0)
					cancLen = 0;
				tStr = tStr.substring(0, tStr.length()-cancLen+1);
			}
		}
		// 1733 DM inizio
		// return "'" + tStr + "'";
		// 2426 DM inizio
		// return "CONVERT(DATETIME, '" + t + "', 121)";
		return "CONVERT(DATETIME, '" + tStr + "', 121)";
		// 2426 DM fine
		// 1733 DM fine
	}

	// 703 DM:
	public Map getTypeMapping()
	{
		return cvTypeMapping;
	}

	// 1790 DM inizio
	public String getCallToNumberToCharFn(String arg)
	{
		return "RTRIM(CAST(" + arg + " AS CHAR))";
	}

	public String getCallToDateToCharFn(String arg)
	{
		return "CONVERT(CHAR(10), " + arg + ", 120)";
	}

	public String getCallToTimeToCharFn(String arg)
	{
		return "CONVERT(CHAR(8), " + arg + ", 108)";
	}

	public String getCallToTimestampToCharFn(String arg)
	{
		return "CONVERT(CHAR(23), " + arg + ", 121)";
	}

	public String getCallToCharToDecimalFn(String arg, int precision, int scale)
	{
		StringBuffer s = new StringBuffer();
		s.append("CAST(").append(arg).append(" AS DECIMAL(").append(precision);
		if (scale > 0)
			s.append(", ").append(scale);
		return s.append("))").toString();
	}

	public String getCallToLengthFn(String arg)
	{
		return "LEN(" + arg + ")";
	}

	public String getCallToUppercaseFn(String arg)
	{
		return "UPPER(" + arg + ")";
	}

	public String getCallToLowercaseFn(String arg)
	{
		return "LOWER(" + arg + ")";
	}

	public String getCallToSubstringFn(String arg, int start, int length)
	{
		return "SUBSTRING(" + arg + ", " + start + ", " + length + ")";
	}
	// 1790 DM fine
  // 27909 inizio
  public String getLengthFn()
  {
    return "LEN";
  }

  public String getUpperFn()
  {
    return "UPPER";
  }

  public String getLowerFn()
  {
    return "LOWER";
  }

  public String getSubstringFn()
  {
    return "SUBSTRING";
	}
  // 27909 fine
	//Mod. 2807
	/**
	 * Restituisce la clausola per attribuire ad una tabella il tablespace ricevuto.
	 */
	/* Revisions:
	 * Number     Date         Owner   Description
	 * 04648      23/11/2005   Ryo     Corretta la sintassi di creazione del tablespace
	 */
	public String getTablespaceClause(String tbsName)
	{
		String clause = "";
		//Mod. 2832
		if (tbsName != null)
		{
//			clause = " ON " + tbsName;	// Fix 4648 Ryo
			clause = " ON '" + tbsName + "'";	// Fix 4648 Ryo
		}
		//fine mod. 2832
		return clause;
	}
	//fine mod. 2807

	//Mod. 3393
	/**
	 * Restituisce la data minima accettata dal database.
	 * @return data minima accettata dal database.
	 */
	public java.sql.Date getMinimumDate()
	{
		return java.sql.Date.valueOf("1753-01-01");
	}
	//fine mod. 3393

	//Fix 03679 MM
	public String getDBManagerId()
	{
		return "sqlserver";
	}

	// 3587 DM inizio
	public String getConcatOperator()
	{
		return "+";
	}
	// 3587 DM fine

	//5488 CB inizio
	public int getDuplicatedRowCode(){
		return 2627;
	}
	//5488 CB fine

	/**
	 * Metodo che verifica se su una colonna è possibile applicare il comando SQL UPPER.
	 * @param columnName
	 * @return
	 */
	/* Revisions:
	 * Fix #    Date          Owner      Description
	 * 07057    04/04/2007    Ryo        Prima versione
	 */
	public boolean isColumnUppercaseable(String schemaTableName, String columnName)
	{
		if(schemaTableName == null || columnName == null)
		{
			return false;
		}
		boolean righto = true;
		try
		{
            // 9990 DM inizio
			// DatabaseMetaData dbmd = ConnectionManager.getCurrentConnection().getMetaData();
            DatabaseMetaData dbmd = getDatabaseMetaData(ConnectionManager.getCurrentConnection());
            // 9990 DM fine
			int dot = schemaTableName.indexOf('.');
			String schemaName = null;
			String tableName = schemaTableName;
			if(dot != -1)
			{
				schemaName = schemaTableName.substring(0, dot);
				tableName = schemaTableName.substring(dot + 1, schemaTableName.length());
			}
			ResultSet columns = dbmd.getColumns(null, schemaName, tableName, columnName);
			columns.next();
			int sqlColumnType = columns.getInt("DATA_TYPE");
			columns.close();
			if(sqlColumnType == Types.CLOB)
			{
				righto = false;
			}
		}
		catch(SQLException ex)
		{
			ex.printStackTrace();
		}
		return righto;
	}

    // 9990 DM inizio
    public DatabaseMetaData getDatabaseMetaData(Connection c) throws SQLException
    {
        return c.getMetaData();
    }
    // 9990 DM fine

    // 10936 DM inizio
    public String getHost()
    {
        return ivHost;
    }

    public String getPort()
    {
        return ivPort;
    }
    // 10936 DM fine
    
    //Fix 40162 - inizio
  	@Override
  	public String getStmtExistElement(String tableName, String where) {
  		return getFirstRowOnlyStmt("SELECT * FROM " + tableName + ((where != null && !where.trim().isEmpty()) ? (" WHERE " + where) : ""));
  	}
  	//Fix 40162 - fine
  	
  	//Fix 41219 - inizio
  	@Override
  	public String getFirstRowOnlyStmt(String stmt) {
  		return stmt.replaceFirst("SELECT", "SELECT TOP 1");
  	}
    //Fix 41219 - fine
  	
	//Fix 38558 - inizio
  	public String getFirstNRowsOnlyStmt(String stmt, int n) {
  		return stmt.replaceFirst("SELECT", "SELECT TOP " + n);
  	}
    //Fix 38558 - fine
  	
  	//44573 inizio
  	public boolean isTraceDuplicatedRowBlocked()	{
  	    Boolean bol = (Boolean )traceDuplicatedRowBlocked.get();      
  	    if(bol != null )
  	        return bol.booleanValue();
  	    else
  	         return false;
  	}
  	
  	public void setTraceDuplicatedRowBlocked(boolean block) {
  	   if(block)
  	      traceDuplicatedRowBlocked .set(new Boolean(true));
  	   else
  	      traceDuplicatedRowBlocked .set(null);
  	}
  	//44573 fine
}
