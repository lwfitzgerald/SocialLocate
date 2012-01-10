<?
class C2DM {
    const AUTH_URL = 'https://www.google.com/accounts/ClientLogin';
    const SEND_URL = 'https://android.apis.google.com/c2dm/send';

    private $auth;
    private $username;
    private $password;

    public function __construct($username, $password) {
        $this->username = $username;
        $this->password = $password;

        $this->auth = $this->getAuthFromDB();

        if ($this->auth === null) {
            $this->auth = $this->authorize();
            if ($this->auth !== null) {
                $this->saveAuthToDB();
            }
        }
    }

    private function getAuthFromDB() {
        $stmt = DB::prepareStatement('SELECT `google_auth` FROM `settings`');
        $stmt->bind_result($auth);
        $stmt->execute();

        $stmt->fetch();
        $stmt->close();

        return $auth;
    }

    private function saveAuthToDB() {
        $stmt = DB::prepareStatement('UPDATE `settings` SET `google_auth`=?');
        $stmt->bind_param('s', $this->auth);
        $stmt->execute();
        $stmt->close();
    }

    private function authorize() {
        $header = array(
            'Content-type: application/x-www-form-urlencoded'
        );

        $postList = array(
          'accountType' => 'GOOGLE',
          'Email' => $this->username,
          'Passwd' => $this->password,
          'source' => 'com.inflatablegoldfish.sociallocate',
          'service' => 'ac2dm',
        );

        $post = http_build_query($postList, '&');
         
        $ch = curl_init(self::AUTH_URL);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_FAILONERROR, 1);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, 1);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $header);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $post);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);

        $ret = curl_exec($ch);
        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);

        if ($code != 200) {
            // Return null if authorize failed
            return null;
        }

        preg_match('/Auth=(.*)/', $ret, $matches);
        return $matches[1];
    }

    public function send($registrationID, $data, $attemptNo = 1) {
        $header = array(
          'Content-type: application/x-www-form-urlencoded',
          'Authorization: GoogleLogin auth='.$this->auth,
        );

        $postList = array(
          'registration_id' => $registrationID,
          'collapse_key' => 0,
        );

        foreach ($data as $key => $value){
            $postList['data.'.$key] = $value;
        }

        $post = http_build_query($postList, '&');
         
        $ch = curl_init(self::SEND_URL);
        curl_setopt($ch, CURLOPT_RETURNTRANSFER, 1);
        curl_setopt($ch, CURLOPT_FAILONERROR, 1);
        curl_setopt($ch, CURLOPT_FOLLOWLOCATION, 1);
        curl_setopt($ch, CURLOPT_POST, true);
        curl_setopt($ch, CURLOPT_HTTPHEADER, $header);
        curl_setopt($ch, CURLOPT_POSTFIELDS, $post);
        curl_setopt($ch, CURLOPT_TIMEOUT, 5);
        $ret = curl_exec($ch);

        $code = curl_getinfo($ch, CURLINFO_HTTP_CODE);
        
        if ($code == 401) {
            if ($attemptNo > 1) {
                // Failed a second time so abort
                return false;
            }

            // Google auth invalid, reauth!
            $this->auth = $this->authorize();
            
            if ($this->auth === null) {
                // Auth failed, return null
                return false;
            } else {
                $this->saveAuthToDB();

                // Resend message
                return $this->send($registrationID, $data, $attemptNo+1);
            }
        } else if ($code != 200) {
            // Fail
            return false;
        }

        if (strpos($ret, 'Error=InvalidRegistration') !== false) {
            return 'InvalidRegistration';
        }

        if (strpos($ret, 'Error=') !== false) {
            // Major error in response so return false
            return false;
        }

        return true;
    }
}
?>
