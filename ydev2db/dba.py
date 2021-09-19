#!/usr/bin/env python3.9

#TODO
# Encrypt data on disk

from time import time

from   optparse import OptionParser

from p3lib.pconfig import ConfigManager
from p3lib.uio import UIO
from p3lib.helper import logTraceBack
from p3lib.database_if import DBConfig, DatabaseIF

class DBConnectionConfig(ConfigManager):
    """@brief Responsible for managing the configuration used by the ydev application."""

    DEFAULT_CONFIG_FILENAME = "dba.cfg"

    DB_HOST                 = "DB_HOST"
    DB_PORT                 = "DB_PORT"
    DB_USERNAME             = "DB_USERNAME"
    DB_PASSWORD             = "DB_PASSWORD"

    DEFAULT_CONFIG = {
        DB_HOST:                    "127.0.0.1",
        DB_PORT:                    3306,
        DB_USERNAME:                "",
        DB_PASSWORD:                ""
    }

    def __init__(self, uio, configFile):
        """@brief Constructor.
           @param uio UIO instance.
           @param configFile Config file instance."""
        super().__init__(uio, configFile, DBConnectionConfig.DEFAULT_CONFIG, addDotToFilename=False, encrypt=True)
        self._uio     = uio
        self.load()

    def configure(self):
        """@brief configure the required parameters for normal operation."""

        self.inputStr(DBConnectionConfig.DB_HOST, "Enter the address of the MYSQL database server", False)

        self.inputDecInt(DBConnectionConfig.DB_PORT, "Enter TCP port to connect to the MYSQL database server", minValue=1024, maxValue=65535)

        self.inputStr(DBConnectionConfig.DB_USERNAME, "Enter the database username", False)

        self.inputStr(DBConnectionConfig.DB_PASSWORD, "Enter the database password", False)

        self.store()

class MySQLDBClient(object):
    """@Responsible for
        - Providing an interface to view and change a database.."""
        
    TIMESTAMP               = "TIMESTAMP"
    
    @staticmethod
    def GetTableSchema(tableSchemaString):
        """@brief Get the table schema
           @param tableSchemaString The string defining the database table schema.
           @return A dictionary containing a database table schema."""
        timestampFound=False
        tableSchemaDict = {}
        elems = tableSchemaString.split(" ")
        if len(elems) > 0:
            for elem in elems:
                subElems = elem.split(":")
                if len(subElems) == 2:
                    colName = subElems[0]
                    if colName == MySQLDBClient.TIMESTAMP:
                        timestampFound=True
                    colType = subElems[1]
                    tableSchemaDict[colName] = colType
                else:
                    raise Exception("{} is an invalid table schema column.".format(elem))
            return tableSchemaDict
        else:
            raise Exception("Invalid Table schema. No elements found.")

        if not timestampFound:
            raise Exception("No {} table column defined.".format(MySQLDBClient.TIMESTAMP))

    def __init__(self, uio, options, config):
        """@brief Constructor
           @param uio A UIO instance
           @param options The command line options instance
           @param config A DBConnectionConfig instance."""  
        self._uio                   = uio
        self._options               = options
        self._config                = config
        self._ssh                   = None
        self._dataBaseIF            = None
        self._addedCount            = 0
        try:
            self._tableSchema       = self.getTableSchema()
        except:
            self._tableSchema       = ""
        self._startTime             = time()

    def _setupDBConfig(self, dbName=None):
        """@brief Setup the internal DB config
           @param dbName Optional database name."""
        self._dbConfig                      = DBConfig()
        self._dbConfig.serverAddress        = self._config.getAttr(DBConnectionConfig.DB_HOST)
        self._dbConfig.serverPort           = self._config.getAttr(DBConnectionConfig.DB_PORT)
        self._dbConfig.username             = self._config.getAttr(DBConnectionConfig.DB_USERNAME)
        self._dbConfig.password             = self._config.getAttr(DBConnectionConfig.DB_PASSWORD)
        self._dbConfig.uio                  = self._uio
        self._dbConfig.dataBaseName         = dbName
        self._dataBaseIF                    = DatabaseIF(self._dbConfig)
        
    def getTableSchema(self):
        """@return the required MYSQL table schema"""
        return MySQLDBClient.GetTableSchema(self._options.table)

    def _shutdownDBSConnection(self):
        """@brief Shutdown the connection to the DBS"""
        if self._dataBaseIF:
            self._dataBaseIF.disconnect()
            self._dataBaseIF = None

    def _connectToDBS(self):
        """@brief connect to the database server."""
        self._shutdownDBSConnection()

        self._dataBaseIF.connect()
        self._uio.info("Connected to database")

    def createDB(self):
        """@brief Create the configured database on the MYSQL server"""
        try:
            if not self._options.db:
                raise Exception("--db required.")
            self._setupDBConfig()
            self._dataBaseIF.connectNoDB()

            self._dbConfig.dataBaseName = self._options.db
            self._dataBaseIF.createDatabase()

        finally:
            self._shutdownDBSConnection()

    def deleteDB(self):
        """@brief Delete the configured database on the MYSQL server"""
        try:
            if not self._options.db:
                raise Exception("--db required.")       
            self._setupDBConfig()
            self._dbConfig.dataBaseName = self._options.db
            deleteDB = self._uio.getBoolInput("Are you sure you wish to delete the '{}' database [y/n]".format(self._dbConfig.dataBaseName))
            if deleteDB:

                self._dataBaseIF.connectNoDB()

                self._dataBaseIF.dropDatabase()

        finally:
            self._shutdownDBSConnection()

    def createTable(self):
        """@brief Create the database table configured"""
        try:
            if not self._options.db:
                raise Exception("--db required.")     

            if not self._options.table:
                raise Exception("--table required.")   
            
            if not self._options.schema:
                raise Exception("--schema required.")
              
            tableName = self._options.table
            self._setupDBConfig(dbName=self._options.db)

            self._dataBaseIF.connect()

            tableSchema = MySQLDBClient.GetTableSchema( self._options.schema )
            self._dataBaseIF.createTable(tableName, tableSchema)

        finally:
            self._shutdownDBSConnection()

    def deleteTable(self):
        """@brief Delete a database table configured"""
        try:
            if not self._options.db:
                raise Exception("--db required.")     

            if not self._options.table:
                raise Exception("--table required.")   
            
            tableName = self._options.table
            self._setupDBConfig(dbName=self._options.db)
            deleteDBTable = self._uio.getBoolInput("Are you sure you wish to delete the '{}' database table [y/n]".format(tableName))
            if deleteDBTable:

                self._dataBaseIF.connect()

                self._dataBaseIF.dropTable(tableName)

        finally:
            self._shutdownDBSConnection()

    def showDBS(self):
        """@brief List the databases."""
        try:

            self._setupDBConfig()

            self._dataBaseIF.connectNoDB()

            sql = 'SHOW DATABASES;'
            recordTuple = self._dataBaseIF.executeSQL(sql)
            for record in recordTuple:
                self._uio.info( str(record) )

        finally:
            self._shutdownDBSConnection()

    def showTables(self):
        """@brief List the databases."""
        try:
            if not self._options.db:
                raise Exception("--db required.")  
            
            self._setupDBConfig(dbName=self._options.db)

            self._dataBaseIF.connect()

            sql = 'SHOW TABLES;'
            recordTuple = self._dataBaseIF.executeSQL(sql)
            for record in recordTuple:
                self._uio.info( str(record) )

        finally:
            self._shutdownDBSConnection()

    def readTable(self):
        """@brief Read a number of records from the end of the database table."""
        try:

            if not self._options.db:
                raise Exception("--db required.")     

            if not self._options.table:
                raise Exception("--table required.")   
            
            self._setupDBConfig(dbName=self._options.db)

            self._dataBaseIF.connect()
            
            tableName = self._options.table
                
            sql = 'SELECT * FROM `{}` ORDER BY {} DESC LIMIT {}'.format(tableName, MySQLDBClient.TIMESTAMP, self._options.read_count)
            recordTuple = self._dataBaseIF.executeSQL(sql)
            for record in recordTuple:
                self._uio.info( str(record) )

        finally:
            self._shutdownDBSConnection()
            
    def executeSQL(self):
        """@brief Execute SQL command provided on the command line."""
        try:
            if not self._options.sql:
                raise Exception("--sql required.")  
            sql = self._options.sql
                        
            if self._options.db:
                self._setupDBConfig(dbName=self._options.db)
                self._dataBaseIF.connect()
            else:
                self._setupDBConfig()
                self._dataBaseIF.connectNoDB()
            

            recordTuple = self._dataBaseIF.executeSQL(sql)
            for record in recordTuple:
                self._uio.info( str(record) )

        finally:
            self._shutdownDBSConnection()
                  
            
    def showSchema(self):
        """@brief Execute SQL command provided on the command line."""
        try:
            if not self._options.db:
                raise Exception("--db required.")     

            if not self._options.table:
                raise Exception("--table required.")  
            
            tableName = self._options.table
            self._setupDBConfig(dbName=self._options.db)

            self._dataBaseIF.connect()

            sql = "DESCRIBE `{}`;".format(tableName)
            recordTuple = self._dataBaseIF.executeSQL(sql)
            for record in recordTuple:
                self._uio.info( str(record) )

        finally:
            self._shutdownDBSConnection()
            
    def showExSchema(self):
        """@brief Show an example schema so that the user can get a basic syntax for a table schema."""
        self._uio.info("LOCATION:VARCHAR(64) TIMESTAMP:TIMESTAMP VOLTS:FLOAT(5,2) AMPS:FLOAT(5,2) WATTS:FLOAT(10,2)")


def main():
    uio = UIO()
    uio.logAll(True)

    opts = OptionParser(usage="A tool for modifying MYSQL databases.")
    opts.add_option("-f",                   help="The config file for the database connection.", default=DBConnectionConfig.DEFAULT_CONFIG_FILENAME)
    opts.add_option("-c",                   help="Set the database connection configuration.", action="store_true", default=False)
    opts.add_option("--show_dbs",           help="Show all the databases on the MySQL server.", action="store_true", default=False)
    opts.add_option("--show_tables",        help="Show all the database tables for the configured database on the MySQL server.", action="store_true", default=False)
    opts.add_option("--show_table_schema",  help="Show the schema of an SQL table.", action="store_true", default=False)
    opts.add_option("--db",                 help="The name of the database to use.", default=None)
    opts.add_option("--create_db",          help="Create the configured database.", action="store_true", default=None)
    opts.add_option("--delete_db",          help="Delete the configured database.", action="store_true", default=None)
    opts.add_option("--table",              help="The name of the database table to use.", default=None)
    opts.add_option("--schema",             help="The database schema to use when creating a database table.", default=None)
    opts.add_option("--ex_schema",          help="Example database schema.", action="store_true", default=False)
    opts.add_option("--create_table",       help="Create a table in the configured database.", action="store_true", default=None)
    opts.add_option("--delete_table",       help="Delete a table from the configured database.", action="store_true", default=None)
    opts.add_option("--read",               help="Read a number of records from the end of the database table.", action="store_true", default=False)
    opts.add_option("--read_count",         help="The number of lines to read from the end of the database table (default=1).", type="int", default=1)
    opts.add_option("--sql",                help="Execute an SQL command.")
    opts.add_option("--debug",              help="Enable debugging.", action="store_true", default=False)

    try:
        (options, args) = opts.parse_args()

        dbConnectionConfig = DBConnectionConfig(uio, options.f)
        yDev2DBClient = MySQLDBClient(uio, options, dbConnectionConfig)

        uio.enableDebug(options.debug)
        if options.c:
            dbConnectionConfig.configure()

        elif options.create_db:
            yDev2DBClient.createDB()

        elif options.delete_db:
            yDev2DBClient.deleteDB()

        elif options.create_table:
            yDev2DBClient.createTable()

        elif options.delete_table:
            yDev2DBClient.deleteTable()

        elif options.show_dbs:
            yDev2DBClient.showDBS()

        elif options.show_tables:
            yDev2DBClient.showTables()
            
        elif options.read:
            yDev2DBClient.readTable()

        elif options.sql:
            yDev2DBClient.executeSQL()
            
        elif options.show_table_schema:
            yDev2DBClient.showSchema()

        elif options.ex_schema:
            yDev2DBClient.showExSchema()

        else:
            raise Exception("No action selected on command line.")

    #If the program throws a system exit exception
    except SystemExit:
        pass

    #Don't print error information if CTRL C pressed
    except KeyboardInterrupt:
        pass

    except Exception as ex:
        logTraceBack(uio)

        if options.debug:
            raise
        else:
            uio.error(str(ex))

if __name__== '__main__':
    main()
