SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de datos: `proyecto_poo`
--

-- --------------------------------------------------------

-- Estructura de tabla para la tabla `persona`
CREATE TABLE `persona` (
  `id_persona` int(11) NOT NULL AUTO_INCREMENT,
  `nombre` varchar(100) NOT NULL,
  `email` varchar(100) NOT NULL,
  `tipodocumento` varchar(100) NOT NULL,
  `numdocumento` varchar(100) NOT NULL,
  PRIMARY KEY (`id_persona`),
  UNIQUE KEY `idx_numdocumento` (`numdocumento`),
  UNIQUE KEY `idx_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Estructura de tabla para la tabla `rol`
CREATE TABLE `rol` (
  `id_rol` int(11) NOT NULL AUTO_INCREMENT,
  `nombre_rol` varchar(50) NOT NULL,
  `descripcion` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id_rol`),
  UNIQUE KEY `nombre_rol` (`nombre_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Estructura de tabla para la tabla `instalacion`
CREATE TABLE `instalacion` (
  `idInstalacion` int(11) NOT NULL AUTO_INCREMENT,
  `tipo` varchar(100) NOT NULL,
  `capacidadMaxima` int(11) NOT NULL,
  `aforoActual` int(11) NOT NULL,
  `nombre` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`idInstalacion`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Estructura de tabla para la tabla `usuarios`
CREATE TABLE `usuarios` (
  `idusuario` int(11) NOT NULL AUTO_INCREMENT,
  `id_persona` int(11) NOT NULL,
  `contrasena` varchar(255) NOT NULL,
  `categoria` varchar(1) DEFAULT NULL,
  `esAfiliado` tinyint(1) NOT NULL,
  `id_rol` int(11) NOT NULL DEFAULT 1,
  PRIMARY KEY (`idusuario`),
  KEY `fk_usuarios_persona` (`id_persona`),
  KEY `fk_usuarios_rol` (`id_rol`),
  CONSTRAINT `fk_usuarios_persona` FOREIGN KEY (`id_persona`) REFERENCES `persona` (`id_persona`) ON DELETE CASCADE,
  CONSTRAINT `fk_usuarios_rol` FOREIGN KEY (`id_rol`) REFERENCES `rol` (`id_rol`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Estructura de tabla para la tabla `entrenador`
CREATE TABLE `entrenador` (
  `idEntrenador` int(11) NOT NULL AUTO_INCREMENT,
  `especialidad` varchar(50) NOT NULL,
  `id_persona` int(11) NOT NULL,
  `idInstalacion` int(11) DEFAULT NULL,
  PRIMARY KEY (`idEntrenador`),
  KEY `fk_entrenador_instalacion` (`idInstalacion`),
  KEY `fk_entrenador_persona_id` (`id_persona`),
  CONSTRAINT `fk_entrenador_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`),
  CONSTRAINT `fk_entrenador_persona_id` FOREIGN KEY (`id_persona`) REFERENCES `persona` (`id_persona`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Estructura de tabla para la tabla `turno`
CREATE TABLE `turno` (
  `idTurno` int(11) NOT NULL AUTO_INCREMENT,
  `fechaHora` datetime NOT NULL,
  `duracionMinutos` int(11) NOT NULL,
  `id_usuario` int(11) NOT NULL,
  `idInstalacion` int(11) NOT NULL,
  `idEntrenador` int(11) DEFAULT NULL,
  `estado` varchar(20) DEFAULT 'RESERVADO',
  PRIMARY KEY (`idTurno`),
  KEY `fk_turno_instalacion` (`idInstalacion`),
  KEY `fk_turno_usuario` (`id_usuario`),
  KEY `fk_turno_entrenador` (`idEntrenador`),
  CONSTRAINT `fk_turno_entrenador` FOREIGN KEY (`idEntrenador`) REFERENCES `entrenador` (`idEntrenador`) ON DELETE SET NULL,
  CONSTRAINT `fk_turno_instalacion` FOREIGN KEY (`idInstalacion`) REFERENCES `instalacion` (`idInstalacion`) ON DELETE CASCADE,
  CONSTRAINT `fk_turno_usuario` FOREIGN KEY (`id_usuario`) REFERENCES `usuarios` (`idusuario`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- Datos de ejemplo para roles
INSERT INTO `rol` (`id_rol`, `nombre_rol`, `descripcion`) VALUES
(1, 'USUARIO', 'Usuario regular del sistema'),
(2, 'ADMINISTRADOR', 'Administrador del sistema'),
(3, 'ENTRENADOR', 'Entrenador del gimnasio o piscina');

COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
