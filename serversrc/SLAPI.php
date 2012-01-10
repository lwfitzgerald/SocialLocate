<?
require_once('DB.php');
require_once('User.php');
require_once('C2DM.php');
require_once('FacebookIntegration.php');

class SLAPI {
    private $facebookInt;
    private $fbAuthed;

    private $C2DM;

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

        if ($this->fbAuthed) {
            $this->C2DM = new C2DM($config['c2dm_user'], $config['c2dm_pass']);
        }
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
        case 'update_registration':
            echo $this->handleUpdateRegistration();
            return;
        case 'meet':
            echo $this->handleMeet();
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

            $user->saveLocation();
        }

        return $this->authReturn(true);
    }

    private function handleUpdateRegistration() {
        if (isset($_GET['registration_id'])) {
            $user = new User (
                $this->facebookInt->getID(),
                null,
                null,
                null,
                $_GET['registration_id']
            );

            $user->saveRegistration();
        }

        return $this->authReturn(true);
    }

    private function handleMeet() {
        if (isset($_GET['friend_id'])
            && isset($_GET['venue_id'])) {
            
            $user = new User($_GET['friend_id']);
            $user->load();

            $result = $user->sendPush(
                array(
                    'payload' => $_GET['friend_id'].';'.$_GET['venue_id']
                ),
                $this->C2DM
            );

            return json_encode(array(
                'auth_status' => 1,
                'meet_request_status' => ($result ? 1 : 0)
            ));
        }

        return authReturn(true);
    }

    private function authReturn($authed) {
        return json_encode(array(
            'auth_status' => ($authed ? 1 : 0)
        ));
    }
}
?>
