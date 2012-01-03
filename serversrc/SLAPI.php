<?
require_once('DB.php');
require_once('User.php');
require_once('FacebookIntegration.php');

class SLAPI {
    private $facebookInt;
    private $fbAuthed;

    public function __construct($config) {
        // Initialise DB connection
        DB::connect($config);

        // Set up facebook integration
        $this->facebookInt = new FacebookIntegration(
            $config['fb_app_id'],
            $config['fb_app_secret'],
            (isset($_GET['access_token']) ? $_GET['access_token'] : null)
        );

        // Work out if authentication is OK
        $this->fbAuthed = $this->facebookInt->authIsOk();
    }

    public function handleAction() {
        if (!$this->fbAuthed) {
            // Not authenticated
            echo $this->authReturn(false);
            return;
        }

        if (!isset($_GET['action'])) {
            // No action parameter
            echo $this->authReturn(false);
            return;
        }
        
        $action = $_GET['action'];
        
        switch ($action) {
        case 'auth':
            echo $this->authReturn(true);
            return;
        case 'initial_fetch':
            echo $this->handleInitialFetch();
            return;
        case 'fetch':
            echo $this->handleFetch();
            return;
        case 'update_location':
            echo $this->handleUpdateLocation();
            return;
        }
    }

    private function handleInitialFetch() {
        $ownDetails = $this->facebookInt->getOwnDetails();

        if ($ownDetails === null) {
            // Session expired
            return $this->authReturn(false);
        }

        $friends = $this->facebookInt->getFriendsUsingSL();

        if ($friends === null) {
            // Session expired
            return $this->authReturn(false);
        }

        // Now have both bits of neccessary data...
        return json_encode(array(
            'auth_status' => 1,
            'own_details' => $ownDetails->toArray(),
            'friends'     => User::objectsToArrays($friends)
        ));
    }

    private function handleFetch() {
        $friends = $this->facebookInt->getFriendsUsingSL(true);

        if ($friends === null) {
            // Session expired
            return $this->authReturn(false);
        }

        // Now have both bits of neccessary data...
        return json_encode(array(
            'auth_status' => 1,
            'friends'     => User::objectsToArrays($friends)
        ));
    }

    private function handleUpdateLocation() {
        if (isset($_GET['lat'])
            && isset($_GET['lng'])) {
        
            // Lat and lng provided so update
            $user = new User(
                $this->facebookInt->getID(),
                $_GET['lat'],
                $_GET['lng']
            );

            $user->save();
        }

        return $this->authReturn(true);
    }

    private function authReturn($authed) {
        return json_encode(array(
            'auth_status' => ($authed ? 1 : 0)
        ));
    }
}
?>
