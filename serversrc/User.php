<?
class User {
    private $id;
    private $lat;
    private $long;
    private $name;
    private $pic;

    /**
     * Constructor
     *
     * Performs no changes to db until save()
     */
    public function __construct($id, $lat = null, $long = null, $name = null, $pic = null) {
        $this->id = $id;
        $this->lat = $lat;
        $this->long = $long;
        $this->name = $name;
        $this->pic = $pic;
    }

    /**
     * Updates the location of this user
     */
    public function updateLocation($lat, $long) {
        $this->lat = $lat;
        $this->long = $long;
        $this->save();
    }

    /**
     * Load lat and long for this user
     * from the DB
     * @return True if load succeeded
     */
    public function load() {
        $stmt = db::prepareStatement('SELECT * FROM `user` WHERE `id` = ?');
        $stmt->bind_param('i', $this->id);
        $stmt->execute();
        $stmt->bind_result($id, $lat, $long);
        $stmt->fetch();

        if ($stmt->num_rows > 0) {
            $this->id = $id;
            $this->lat = $lat;
            $this->long = $long;
            $stmt->close();
            return true;
        } else {
            $stmt->close();
            return false;
        }
    }
    
    /**
     * Save any changes to this user to the DB
     */
    public function save() {
        $stmt = db::prepareStatement('INSERT INTO `user` (`id`, `lat`, `long`) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `lat`=?, `long`=?');
        $stmt->bind_param(
            'idddd',
            $this->id,
            $this->lat,
            $this->long,
            $this->lat,
            $this->long
        );
        $stmt->execute();
        $stmt->close();
    }

    /**
     * Get an array representation of this user
     * @return Array representation
     */
    public function toArray() {
        $toReturn = array();

        $toReturn['id'] = $this->id;

        if ($this->lat !== null) {
            $toReturn['lat'] = $this->lat;
            $toReturn['long'] = $this->long;
        }

        if ($this->name !== null) {
            $toReturn['name'] = $this->name;
        }

        if ($this->pic !== null) {
            $toReturn['pic'] = $this->pic;
        }

        return $toReturn;
    }

    /**
     * Take an array of User objects
     * and return an array of arrays
     * representing the objects
     * @return Array of represenatative
     * arrays
     */
    public static function objectsToArrays($friendObjects) {
        $friendArrays = array();

        foreach ($friendObjects as $object) {
            array_push($friendArrays, $object->toArray());
        }

        return $friendArrays;
    }
}

?>
