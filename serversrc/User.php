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
     * Send a push notification to this user
     * @param $data Data to send in push
     * @param $C2DM C2DM reference
     * @return True if successful
     */
    public function sendPush($data, $C2DM) {
        $result = $C2DM->send($this->registrationID, $data);

        if ($result === false) {
            return false;
        }

        if ($result === 'InvalidRegistration') {
            $this->clearRegistrationID();
            return false;
        }

        return true;
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

        $this->id = $id;
        $this->lat = $lat;
        $this->lng = $lng;
        $this->lastUpdated = $lastUpdated;
        $this->registrationID = $registrationID;
        
        $stmt->close();
        return true;
    }

    /**
     * Saves just lat and lng to the DB
     */
    public function saveLocation() {
        $stmt = db::prepareStatement('INSERT INTO `user` (`id`, `lat`, `lng`, `last_updated`) VALUES(?, ?, ?, NOW()) ON DUPLICATE KEY UPDATE `lat`=?, `lng`=?, last_updated=NOW()');
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
     * Remove the registration ID for the given user ID
     */
    private function clearRegistrationID() {
        $this->registrationID = null;

        $stmt = db::prepareStatement('UPDATE `user` SET `registration_id`=NULL WHERE `id`=?');
        $stmt->bind_param('i', $this->id);
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
