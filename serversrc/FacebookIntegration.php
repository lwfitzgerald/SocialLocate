<?
require_once('facebook-lib/facebook.php');

class FacebookIntegration {
    private $facebook;
    private $userID;

    /**
     * Create facebook connection and validate
     * access token
     * @param $fb_app_id FB app id
     * @param $fb_app_secret FB app secret
     * @param $access_token Access token
     * to attempt to authenticate with. If not
     * provided, use access_token from previous
     * session
     */
    public function __construct($fb_app_id, $fb_app_secret, $access_token) {
        $this->facebook = new Facebook(array(
            'appId'  => $fb_app_id,
            'secret' => $fb_app_secret
        ));

        if ($access_token !== null) {
            // Setting new access token
            $this->facebook->setAccessToken($access_token);
        }

        $this->validateAccessToken();
    }

    /**
     * Attempts to query the API to get
     * the user id associated with this
     * access token. User ID set to 0 if
     * fails
     */
    private function validateAccessToken() {
        $this->userID = $this->facebook->getUser();
    }

    /**
     * Returns if authentication was
     * successful
     * @return True if successful
     */
    public function authIsOk() {
        return $this->userID !== 0;
    }

    /**
     * Get the user's Facebook ID
     * @return Facebook user ID
     */
    public function getID() {
        return $this->userID;
    }

    public function getOwnDetails() {
        $query = 'SELECT uid, name, pic_square FROM user WHERE uid=me()';
        try {
            $own_details = $this->facebook->api(array(
                'method' => 'fql.query',
                'query'  => $query
            ));
        } catch (FacebookApiException $e) {
            return null;
        }

        return new User(
            $own_details[0]['uid'],
            null,
            null,
            $own_details[0]['name'],
            $own_details[0]['pic_square']
        );
    }

    private function getFriends($light) {
        if (!$light) {
            $query = 'SELECT uid, name, pic_square FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1=me()) ORDER BY name';
        } else {
            $query = 'SELECT uid FROM user WHERE uid IN (SELECT uid2 FROM friend WHERE uid1=me()) ORDER BY name';
        }

        try {
            $friends = $this->facebook->api(array(
                'method' => 'fql.query',
                'query' => $query
            ));
        } catch (FacebookApiException $e) {
            return null;
        }

        return $friends;
    }

    /**
     * Get friends of FB auth'd user
     */
    public function getFriendsUsingSL($light = false) {
        $friends = $this->getFriends($light);

        $friend_map = array();

        foreach ($friends as $friend) {
            if (!$light) {
                $friend_map[$friend['uid']] = array(
                    'id' => $friend['uid'],
                    'name' => $friend['name'],
                    'pic' => $friend['pic_square']
                );
            } else {
                $friend_map[$friend['uid']] = array(
                    'id' => $friend['uid']
                );
            }
        }

        $in_values = '';
        $sl_friends = array();

        if (!empty($friend_map)) {
            foreach ($friend_map as $friend) {
                $in_values .= $friend['id'] . ',';
            }
            
            // Chop off extra ','
            $in_values = substr($in_values, 0, strlen($in_values)-1-1);

            $stmt = DB::prepareStatement("SELECT * FROM `user` WHERE `id` IN ($in_values)");

            $stmt->bind_result($friend_id, $lat, $long);
            $stmt->execute();

            while ($stmt->fetch()) {
                if (!$light) {
                    $sl_friend = new User(
                        $friend_id,
                        $lat,
                        $long,
                        $friend_map[$friend_id]['name'],
                        $friend_map[$friend_id]['pic']
                    );
                } else {
                    $sl_friend = new User($friend_id, $lat, $long);
                }

                $sl_friends[$friend_id] = $sl_friend;
            }

            $stmt->close();
        }

        return $sl_friends;
    }
}
?>
