<?
class User {
    private $id;
    private $lat;
    private $lng;
    private $name;
    private $pic;

    /**
     * Constructor
     *
     * Performs no changes to db until save()
     */
    public function __construct($id, $lat = null, $lng = null, $name = null, $pic = null) {
        $this->id = $id;
        $this->lat = $lat;
        $this->lng = $lng;
        $this->name = $name;
        $this->pic = $pic;
    }

    /**
     * Updates the location of this user
     */
    public function updateLocation($lat, $lng) {
        $this->lat = $lat;
        $this->lng = $lng;
        $this->save();
    }

    /**
     * Load lat and lng for this user
     * from the DB
     * @return True if load succeeded
     */
    public function load() {
        $stmt = db::prepareStatement('SELECT * FROM `user` WHERE `id` = ?');
        $stmt->bind_param('i', $this->id);
        $stmt->execute();
        $stmt->bind_result($id, $lat, $lng);
        $stmt->fetch();

        if ($stmt->num_rows > 0) {
            $this->id = $id;
            $this->lat = $lat;
            $this->lng = $lng;
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
        $stmt = db::prepareStatement('INSERT INTO `user` (`id`, `lat`, `lng`) VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE `lat`=?, `lng`=?');
        $stmt->bind_param(
            'idddd',
            $this->id,
            $this->lat,
            $this->lng,
            $this->lat,
            $this->lng
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
            $toReturn['lng'] = $this->lng;
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
