<?php

declare(strict_types=1);

// Punto de entrada único de la API de CuidaDosis

use CuidaDosis\ApiException;
use CuidaDosis\Auth;
use CuidaDosis\Controllers\AuthController;
use CuidaDosis\Controllers\IntakeController;
use CuidaDosis\Controllers\LinkController;
use CuidaDosis\Controllers\MedicationController;
use CuidaDosis\Database;
use CuidaDosis\Request;
use CuidaDosis\Response;
use CuidaDosis\Router;

$src = __DIR__ . '/../src';
require_once $src . '/Env.php';
require_once $src . '/ApiException.php';
require_once $src . '/Http.php';
require_once $src . '/Router.php';
require_once $src . '/Validator.php';
require_once $src . '/Database.php';
require_once $src . '/Auth.php';
require_once $src . '/Controllers/AuthController.php';
require_once $src . '/Controllers/LinkController.php';
require_once $src . '/Controllers/MedicationController.php';
require_once $src . '/Controllers/IntakeController.php';

ini_set('display_errors', '0');
$debug = false;

try {
    $config = require __DIR__ . '/../config/config.php';
    $debug = $config['debug'];

    $pdo = Database::connect($config['db']);
    $auth = new Auth($pdo, $config['token_ttl_hours']);

    $authCtl = new AuthController($pdo, $auth);
    $linkCtl = new LinkController($pdo, $auth);
    $medCtl = new MedicationController($pdo, $auth);
    $intakeCtl = new IntakeController($pdo, $auth);

    $router = new Router();
    $router->add('GET', '/', fn () => Response::json(200, [
        'status' => 'ok',
        'service' => 'CuidaDosis API',
        'time' => date('Y-m-d H:i:s'),
    ]));

    $router->add('POST', '/auth/register', [$authCtl, 'register']);
    $router->add('POST', '/auth/login', [$authCtl, 'login']);
    $router->add('POST', '/auth/logout', [$authCtl, 'logout']);
    $router->add('GET', '/me', [$authCtl, 'me']);

    $router->add('POST', '/links/invite', [$linkCtl, 'invite']);
    $router->add('POST', '/links/accept', [$linkCtl, 'accept']);
    $router->add('DELETE', '/links/active', [$linkCtl, 'unlinkCaregiver']);

    $router->add('GET', '/medications', [$medCtl, 'list']);
    $router->add('POST', '/medications', [$medCtl, 'create']);
    $router->add('GET', '/medications/{id}', [$medCtl, 'show']);
    $router->add('PUT', '/medications/{id}', [$medCtl, 'update']);
    $router->add('DELETE', '/medications/{id}', [$medCtl, 'deactivate']);

    $router->add('GET', '/intakes/today', [$intakeCtl, 'today']);
    $router->add('POST', '/intakes/{id}/take', [$intakeCtl, 'take']);
    $router->add('GET', '/caregiver/patients', [$intakeCtl, 'caregiverPatients']);
    $router->add('GET', '/caregiver/patients/{id}/intakes/today', [$intakeCtl, 'caregiverPatientToday']);
    $router->add('DELETE', '/caregiver/patients/{id}', [$linkCtl, 'unlinkPatient']);

    $router->dispatch(Request::fromGlobals());
} catch (ApiException $e) {
    Response::error($e->status, $e->getMessage(), $e->details);
} catch (\Throwable $e) {
    error_log('CuidaDosis API: ' . $e);
    $details = $debug ? ['debug' => get_class($e) . ': ' . $e->getMessage()] : [];
    Response::error(500, 'Error interno del servidor.', $details);
}
