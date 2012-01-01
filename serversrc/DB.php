<?
class DB {
    /**
     * MySQLi interface
     */
    private static $mysqli;

    /**
     * Connect to DB
     * @param $config Configuration array
     */
    public static function connect($config) {
        self::$mysqli = new mysqli($config['db_host'], $config['db_user'], $config['db_pass'], $config['db_name']);
    }

    /**
     * Prepare an SQL statement
     * @param $statement Statement string
     * @return Prepared statement
     */
    public static function prepareStatement($statement) {
        return self::$mysqli->prepare($statement);
    }
}
?>
