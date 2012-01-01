<?
require_once('config.php');

class FoursquareIntegration {
    private $clientID;
    private $clientSecret;
    private $lastModified;

    public function __construct($config) {
        $this->clientID = $config['fs_client_id'];
        $this->clientSecret = $config['fs_client_secret'];
        $this->lastModified = $config['fs_last_modified'];
    }

    public function getVenuesNear($lat, $long) {
        $url = 'https://api.foursquare.com/v2/venues/search?';
        $url .= "ll=$lat,$long";
        $url .= "&client_id=$this->clientID";
        $url .= "&client_secret=$this->clientSecret";
        $url .= '&intent=browse&radius=100';
        $url .= "&v=$this->lastModified";

        $json = file_get_contents($url);

        if (!$json) {
            // If cannot contact FS, return null
            return null;
        }

        $decoded = json_decode($json);

        $venues = array();

        foreach ($decoded->response->venues as $venue) {
            if (count($venue->categories) != 0
                && $venue->categories[0]->name != 'Home') {

                // Only include results which have a known
                // category and are not a "Home"

                $venues[$venue->id] = array(
                    'id'       => $venue->id,
                    'name'     => $venue->name,
                    'lat'      => $venue->location->lat,
                    'long'     => $venue->location->lng,
                    'distance' => $venue->location->distance,
                );
            }
        }

        // Sort by distance ascending
        usort($venues, array('self', 'compareVenueDistance'));

        print_r($venues);

        return $venues;
    }

    private static function compareVenueDistance($venue1, $venue2) {
        if ($venue1['distance'] == $venue2['distance']) {
            return 0;
        }
        return ($venue1['distance'] < $venue2['distance']) ? -1 : 1;
    }
}

$foursquareInt = new FoursquareIntegration($config);
?>
