<?
error_reporting(E_ALL);

require_once('config.php');
require_once('SLAPI.php');

$SLAPI = new SLAPI($config);

$SLAPI->handleAction();
?>
