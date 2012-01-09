<?
class User {
    private $id;
    private $lat;
    private $lng;
    private $lastUpdated;
    private $registrationID;
    private $name;
    private $pic;

    /**
     * Constructor
     *
     * Performs no changes to db until save()
     */
    public function __construct($id, $lat = null, $lng = null, $lastUpdated = null, $registrationID = null, $name = null, $pic = null) {
        $this->id = $id;
        $this->lat = $lat;
        $this->lng = $lng;
        $this->lastUpdated = $lastUpdated;
        $this->registrationID = $registrationID;
        $this->name = $name;
        $this->pic = $pic;
    }

    /**
     * Load lat and lng for this user
     * from the DB
     * @return True if load succeeded
     */
    public function load() {
        $stmt = db::prepareStatement('SELECT `id`, `lat`, `lng`, UNIX_TIMESTAMP(`last_updated`) AS `last_updated`, `registration_id` FROM `user` WHERE `id` = ?');
        $stmt->bind_param('i', $this->id);
        $stmt->execute();
        $stmt->bind_result($id, $lat, $lng, $lastUpdated, $registrationID);
        $stmt->fetch();

        if ($stmt->num_rows > 0) {
            $this->id = $id;
            $this->lat = $lat;
            $this->lng = $lng;
            $this->lastUpdated = $lastUpdated;
            $this->registrationID = $registrationID;
            $stmt->close();
            return true;
        } else {
            $stmt->close();
            return false;
        }
    }
  
    /**
     * Saves just lat and lng to the DB
     */
    public function saveLocation() {
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
     * Saves just registration id to the DB
     */
    public function saveRegistration() {
        $stmt = db::prepareStatement('INSERT INTO `user` (`id`, `registration_id`) VALUES(?, ?) ON DUPLICATE KEY UPDATE `registration_id`=?');
        $stmt->bind_param(
            'iss',
            $this->id,
            $this->registrationID,
            $this->registrationID
        );
        $stmt->execute();
        $stmt->close();
    }
        

    /**
     * Save any changes to this user to the DB
     */
    public function saveAll() {
        $stmt = db::prepareStatement('INSERT INTO `user` (`id`, `lat`, `lng`, `registration_id`) VALUES(?, ?, ?, ?) ON DUPLICATE KEY UPDATE `lat`=?, `lng`=?, `registration_id`=?');
        $stmt->bind_param(
            'idddd',
            $this->id,
            $this->lat,
            $this->lng,
            $this->registrationID,
            $this->lat,
            $this->lng,
            $this->registrationID
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
            $toReturn['last_updated'] = $this->lastUpdated;
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
