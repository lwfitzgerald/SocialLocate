<?
require_once('config.php');
require_once('db.php');
require_once('user.php');

db::connect($config);

$user = new User(0, 0.5, 0.5);
?>
